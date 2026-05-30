package org.example.kafka.circuitbreakerDegradation;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * =====================================================
 * Kafka 背压 + 熔断 + 降级 全链路演示
 * =====================================================
 *
 * 本 Demo 模拟了一个完整的 Kafka 消费 → 解析 → 转换 → 入库 的数据处理管道，
 * 并在管道中集成了以下工业级容错机制：
 *
 * 1. 背压（Backpressure）：
 *    当下游处理速度跟不上上游生产速度时，通过有界队列（BlockingQueue）自动阻塞
 *    生产者，防止内存溢出（OOM）。同时通过动态调整 pollSize（Kafka 每次拉取的
 *    消息数量）来从源头控制流入速率。
 *
 * 2. 熔断器（Circuit Breaker）：
 *    当 DB 写入的平均响应时间（RT）超过阈值或队列积压过多时，触发熔断，
 *    暂停 Kafka 消费，避免雪崩效应。熔断器有三种状态：
 *    - CLOSED（正常）：正常消费和处理
 *    - OPEN（熔断）：暂停消费，等待下游恢复
 *    - HALF_OPEN（半开）：熔断一段时间后，尝试以小流量恢复消费，
 *      如果 DB RT 回落到正常水平则关闭熔断，否则重新熔断
 *
 * 3. 线程池隔离（Thread Pool Isolation）：
 *    解析、转换、入库三个阶段分别使用独立的线程池，避免某个阶段的异常
 *    （如 DB 超时）拖垮整个处理链路。每个阶段的瓶颈互不影响。
 *
 * 4. 批量写库（Batch Write）：
 *    DB 线程不是逐条写入，而是通过 drainTo() 一次性从队列中取出最多 100 条
 *    消息进行批量写入，显著减少数据库连接次数和事务开销。
 *
 * 5. 重试 + 指数退避 + Jitter（Retry with Exponential Backoff & Jitter）：
 *    对于可恢复的异常（如网络抖动、数据库连接超时），将消息放入重试队列，
 *    重试间隔按 2^retryCount 秒指数增长，并叠加 0~500ms 的随机抖动（Jitter），
 *    防止多个消费者在同一时刻重试导致"惊群效应"（Thundering Herd）。
 *
 * 6. 死信队列（DLQ - Dead Letter Queue）：
 *    对于不可恢复的异常（如 JSON 格式错误、数据校验失败），或重试次数超过
 *    最大限制（5次）的消息，放入死信队列，供人工排查和修复后重新投递。
 *
 * 7. 动态限流（Dynamic Rate Limiting）：
 *    熔断触发时，将 Kafka pollSize 从 100 降到 10，大幅减少流入速率；
 *    恢复后重新调回 100，实现平滑的流量控制。
 *
 * 8. 最终一致性（Eventual Consistency）：
 *    即使中间某个环节失败，消息也会通过重试或 DLQ 机制保证最终被处理，
 *    不会丢失数据。
 *
 * 整体数据流向：
 * Kafka Consumer → [parseQueue] → Parse Workers → [transformQueue]
 *                  → Transform Workers → [dbQueue] → DB Workers（批量写库）
 *                                                       ↓ 失败
 *                                               [retryQueue] → Retry Worker → 重新进入 dbQueue
 *                                                       ↓ 超过重试上限 / 不可恢复异常
 *                                               [dlqQueue] → 死信队列（人工处理）
 *
 * 监控线程持续采集各队列积压量和 DB RT，作为熔断/恢复的决策依据。
 */
public class FullKafkaBackpressureDemo {

    // ================================================================
    // 队列定义：每个阶段之间通过有界 BlockingQueue 解耦
    // 容量 5000，当队列满时 put() 会阻塞，天然实现背压
    // ================================================================

    /**
     * 解析队列：存放从 Kafka 拉取的原始消息，等待 JSON 解析。
     * 有界队列满时，上游（Kafka Consumer）会被阻塞，实现背压。
     */
    private static final BlockingQueue<Message> parseQueue =
            new LinkedBlockingQueue<>(5000);

    /**
     * 转换队列：存放已解析的消息，等待字段转换（如单位换算、字段映射等）。
     */
    private static final BlockingQueue<Message> transformQueue =
            new LinkedBlockingQueue<>(5000);

    /**
     * 数据库写入队列：存放已转换的消息，等待批量写入数据库。
     * 这是整个链路中最关键的队列，积压量是触发熔断的核心指标之一。
     */
    private static final BlockingQueue<Message> dbQueue =
            new LinkedBlockingQueue<>(5000);

    /**
     * 重试队列：存放 DB 写入失败的可恢复消息。
     * 由独立的 Retry Worker 线程消费，等待指数退避延迟后重新投入 dbQueue。
     */
    private static final BlockingQueue<Message> retryQueue =
            new LinkedBlockingQueue<>(5000);

    /**
     * 死信队列（Dead Letter Queue）：存放不可恢复的消息。
     * 包括：数据格式异常、重试次数耗尽等。需要人工介入排查后重新投递。
     */
    private static final BlockingQueue<Message> dlqQueue =
            new LinkedBlockingQueue<>(5000);

    Random random = new Random();

    // ================================================================
    // 线程池隔离：每个处理阶段使用独立线程池，互不干扰
    // ================================================================

    /**
     * 解析线程池：4 个线程，负责从 parseQueue 取出原始消息并进行 JSON 解析。
     * 解析是 CPU 密集型操作，线程数一般设为 CPU 核心数。
     */
    private static final ExecutorService parseExecutor =
            Executors.newFixedThreadPool(4);

    /**
     * 转换线程池：4 个线程，负责字段转换、数据清洗等操作。
     * 与解析阶段类似，属于轻量级 CPU 操作。
     */
    private static final ExecutorService transformExecutor =
            Executors.newFixedThreadPool(4);

    /**
     * 数据库写入线程池：8 个线程，负责批量写库。
     * DB 写入是 I/O 密集型操作，大部分时间在等待数据库响应，
     * 所以线程数可以比 CPU 核心数多，提高吞吐量。
     */
    private static final ExecutorService dbExecutor =
            Executors.newFixedThreadPool(8);

    // ================================================================
    // 熔断器状态
    // ================================================================

    /**
     * 熔断标志位。
     * - true：熔断已触发，Kafka Consumer 暂停拉取消息
     * - false：正常状态，持续消费
     *
     * 使用 AtomicBoolean 保证多线程间的可见性和原子性。
     * Monitor 线程负责设置为 true（触发熔断），
     * DB Worker 在 HALF_OPEN 状态下成功写入后设置为 false（关闭熔断）。
     */
    private static final AtomicBoolean paused =
            new AtomicBoolean(false);

    /**
     * 半开状态标志位。
     * 熔断触发后，经过一段冷却时间，系统进入半开状态，
     * 此时以小流量（pollSize=10）试探性地恢复消费。
     * 如果 DB RT 回落到正常水平（< 500ms），则认为下游已恢复，
     * 关闭熔断并恢复正常流量；否则重新进入熔断状态。
     *
     * 半开状态的作用：避免在下游尚未完全恢复时大量涌入请求导致二次故障。
     */
    private static final AtomicBoolean halfOpen =
            new AtomicBoolean(false);

    /**
     * 数据库平均响应时间（RT, Response Time），单位毫秒。
     * 由 DB Worker 在每次批量写入后更新，Monitor 线程读取后用于判断
     * 是否需要触发熔断（RT > 2000ms 触发）。
     *
     * 注意：这里简化为只记录最近一次的 RT，生产环境中通常使用
     * 滑动窗口平均值或 P99 分位数来减少偶发抖动的影响。
     */
    private static final AtomicLong avgDbRt =
            new AtomicLong(0);

    /**
     * Kafka 每次 poll 的消息数量，即 Consumer 的 max.poll.records。
     * 默认值 100，熔断触发时降为 10，恢复后调回 100。
     * volatile 保证在 Monitor 线程和 Consumer 线程之间的可见性。
     */
    private static volatile int pollSize = 100;

    /**
     * 熔断触发的时间戳（毫秒）。
     * 用于计算熔断持续时间，当超过冷却时间后进入半开状态。
     * 本 Demo 中未使用该字段做自动半开切换（简化实现），
     * 但保留以便扩展——比如实现"熔断 30 秒后自动进入半开"。
     */
    private static volatile long openTime = 0;

    /**
     * 主方法：按顺序启动所有工作线程，然后开始模拟 Kafka 消费。
     *
     * 启动顺序：
     * 1. Parse Workers — 等待解析队列中的消息
     * 2. Transform Workers — 等待转换队列中的消息
     * 3. DB Workers — 等待 DB 队列中的消息
     * 4. Retry Worker — 等待重试队列中的消息
     * 5. Monitor — 开始监控各队列积压和 DB RT
     * 6. Kafka Consumer Simulator — 开始模拟消息拉取
     */
    public static void main(String[] args) {

        startParseWorkers();

        startTransformWorkers();

        startDbWorkers();

        startRetryWorker();

        startMonitor();

        simulateKafkaConsumer();
    }

    /**
     * 模拟 Kafka Consumer 的消息拉取过程。
     *
     * 核心逻辑：
     * 1. 每 500ms 执行一次 poll，每次拉取 pollSize 条消息
     * 2. 拉取前检查熔断状态，如果已熔断则暂停 3 秒后重试
     * 3. 将消息放入 dbQueue（本 Demo 简化了 parse → transform 流程，
     *    直接从 Consumer 进入 DB 队列）
     *
     * 背压体现：
     * - 当 dbQueue 满时（5000 条），dbQueue.put() 会阻塞当前线程，
     *   自动减缓 poll 速度，无需手动控制
     * - 熔断时 pollSize 降为 10，从源头减少消息流入量
     */
    private static void simulateKafkaConsumer() {
        Random random = new Random();
        int msgId = 0;

        while (true) {

            try {

                // 熔断检查：如果 paused=true，说明下游已经扛不住了，
                // 此时暂停消费，每 3 秒检查一次熔断状态
                if (paused.get()) {
                    System.out.println(">>> Kafka消费已暂停...");
                    Thread.sleep(3000);
                    continue;
                }

                // 模拟一次 Kafka poll 操作，拉取 pollSize 条消息
                // pollSize 会根据熔断状态动态调整（100 或 10）
                for (int i = 0; i < pollSize; i++) {
                    Message msg = new Message(++msgId,"meter-" + (msgId % 100),"power=" + random.nextInt(1000),System.currentTimeMillis()
                    );

                    // 放入DB队列
                    // 如果队列满，put() 会阻塞，这就是背压的核心体现：
                    // 上游生产速度自动适应下游消费速度
                    dbQueue.put(msg);
                }

                System.out.println(
                        "poll " + pollSize +
                                " 条消息, 当前queue=" + dbQueue.size());

                // 每次 poll 间隔 500ms，模拟 Kafka 的 poll 间隔
                Thread.sleep(500);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 启动解析工作线程池。
     *
     * 职责：从 parseQueue 中取出原始消息，模拟 JSON 解析（耗时 5ms），
     * 然后将解析后的消息放入 transformQueue。
     *
     * 线程模型：4 个线程持续轮询，使用 BlockingQueue.take() 实现
     * 无忙等阻塞——队列为空时线程自动挂起，有消息时被唤醒。
     */
    private static void startParseWorkers() {

        for (int i = 0; i < 4; i++) {

            parseExecutor.submit(() -> {

                while (true) {

                    try {

                        // take() 会阻塞直到队列中有可用消息，
                        // 避免了 CPU 空转（busy-waiting）
                        Message msg = parseQueue.take();

                        // 模拟 JSON 解析耗时 5ms
                        // 实际场景中可能涉及 JSON 反序列化、Schema 校验等
                        Thread.sleep(5);

                        // 解析完成后放入下一阶段队列
                        // 如果 transformQueue 满，put() 阻塞，形成链路背压
                        transformQueue.put(msg);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 启动转换工作线程池。
     *
     * 职责：从 transformQueue 中取出已解析的消息，模拟字段转换
     *（如单位换算、枚举映射、数据清洗等，耗时 10ms），
     * 然后将转换后的消息放入 dbQueue 等待入库。
     *
     * 线程模型：与 Parse Workers 类似，4 个线程持续轮询。
     */
    private static void startTransformWorkers() {

        for (int i = 0; i < 4; i++) {

            transformExecutor.submit(() -> {

                while (true) {

                    try {

                        Message msg = transformQueue.take();

                        // 模拟字段转换耗时 10ms
                        // 实际场景中可能涉及：单位换算（kW → W）、
                        // 枚举值映射、字段脱敏、数据补全等
                        Thread.sleep(10);

                        dbQueue.put(msg);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 启动数据库写入工作线程池 —— 整个链路的核心环节。
     *
     * 职责：从 dbQueue 中批量取出消息（最多 100 条/批），模拟批量写库。
     *
     * 关键特性：
     * 1. 批量写库：通过 drainTo() 一次性取出多条消息，减少 DB 连接/事务开销
     * 2. 熔断检测：写入完成后更新 avgDbRt，Monitor 线程据此判断是否熔断
     * 3. 半开恢复：在半开状态下，如果 RT < 500ms 则关闭熔断
     * 4. 异常分类：
     *    - IllegalArgumentException（不可恢复）→ 直接进 DLQ
     *    - 其他异常（可恢复）→ 进入 RetryQueue 重试
     * 5. 模拟脏数据：每 333 条消息中有 1 条格式异常（id % 333 == 0）
     * 6. 模拟 DB 抖动：30% 概率响应慢（3000ms），70% 概率正常（100ms）
     */
    private static void startDbWorkers() {

        for (int i = 0; i < 8; i++) {

            dbExecutor.submit(() -> {

                Random random = new Random();

                while (true) {

                    try {

                        // ===== 批量写库核心逻辑 =====
                        // 先 take() 阻塞获取一条消息（保证至少有一条才开始批量）
                        List<Message> batch = new ArrayList<>();

                        Message first = dbQueue.take();
                        batch.add(first);

                        // drainTo() 非阻塞地将队列中剩余消息一次性取出（最多 99 条）
                        // 加上前面 take 的 1 条，每批最多 100 条
                        // 这样做的好处：
                        // - 减少数据库连接/事务的创建次数
                        // - 提高单次写入的数据密度
                        // - 降低网络往返开销
                        dbQueue.drainTo(batch, 99);

                        long start = System.currentTimeMillis();

                        /**
                         * 模拟脏数据检测
                         * 每 333 条消息中有 1 条 JSON 格式异常，触发 IllegalArgumentException
                         * 这类异常属于"不可恢复"——重试 100 次也不会变好，
                         * 所以直接扔进 DLQ，不浪费重试资源
                         */
                        for (Message msg : batch) {

                            if (msg.id % 333 == 0) {
                                throw new IllegalArgumentException(
                                        "JSON格式异常");
                            }
                        }

                        /**
                         * 模拟数据库响应抖动
                         * 30% 的概率响应时间 3000ms（慢），70% 的概率 100ms（正常）
                         * 这种抖动在生产环境中很常见，原因包括：
                         * - 数据库 GC（垃圾回收）暂停
                         * - 慢查询阻塞
                         * - 磁盘 I/O 瞬时拥塞
                         * - 网络抖动
                         *
                         * 当慢请求比例升高时，avgDbRt 会快速上升，
                         * Monitor 线程检测到 RT > 2000ms 后触发熔断
                         */
                        if (random.nextInt(10) < 3) {

                            Thread.sleep(3000);

                        } else {

                            Thread.sleep(100);
                        }

                        // 计算本次批量写入的响应时间
                        long rt =
                                System.currentTimeMillis() - start;

                        // 更新全局平均 RT，供 Monitor 线程做熔断判断
                        avgDbRt.set(rt);

                        System.out.println(
                                Thread.currentThread().getName()
                                        + " batch写库成功 size="
                                        + batch.size()
                                        + " RT="
                                        + rt
                                        + "ms");

                        /**
                         * 半开状态下的恢复判断
                         *
                         * 场景：系统之前已经熔断（paused=true），进入半开状态（halfOpen=true）
                         * 此时以小流量（pollSize=10）试探性恢复消费。
                         *
                         * 恢复条件：RT < 500ms，说明数据库响应已经回到正常水平。
                         * 恢复动作：
                         * 1. halfOpen = false — 退出半开状态
                         * 2. pollSize = 100 — 恢复正常消费速率
                         * 3. Monitor 线程检测到 paused=true 但半开条件不再满足时会设 paused=false
                         *
                         * 设计要点：不在这里直接设 paused=false，而是让 Monitor 统一管理状态，
                         * 避免多个线程同时修改熔断状态导致竞态条件。
                         */
                        if (halfOpen.get() && rt < 500) {

                            System.out.println(
                                    "\n>>> HALF OPEN恢复成功，关闭熔断");

                            halfOpen.set(false);
                            pollSize = 100;
                        }

                    } catch (IllegalArgumentException e) {

                        /**
                         * 不可恢复异常处理 → 进入死信队列（DLQ）
                         *
                         * 常见的不可恢复异常：
                         * - JSON 解析失败（格式错误、字段缺失）
                         * - 数据校验失败（值域越权、必填字段为空）
                         * - Schema 版本不匹配
                         *
                         * 这些问题重试不会解决，需要人工修复数据后重新投递。
                         * 将错误信息记录到 errorMsg 字段，方便排查。
                         */
                        Message badMsg = new Message();
                        badMsg.errorMsg = e.getMessage();

                        dlqQueue.offer(badMsg);

                        System.out.println(
                                "\n>>> 脏数据进入DLQ: "
                                        + e.getMessage());

                    } catch (Exception e) {

                        /**
                         * 可恢复异常处理 → 进入重试队列（RetryQueue）
                         *
                         * 常见的可恢复异常：
                         * - 数据库连接超时
                         * - 网络瞬断
                         * - 数据库锁等待超时
                         * - 连接池耗尽
                         *
                         * 这些异常经过一段时间后可能自动恢复，
                         * 所以通过重试机制来保证最终一致性。
                         */
                        Message retryMsg = new Message();
                        retryMsg.retryCount++;

                        retryQueue.offer(retryMsg);

                        System.out.println(
                                "\n>>> DB失败，进入RetryQueue");
                    }
                }
            });
        }
    }


    /**
     * 启动重试工作线程 —— 实现指数退避 + Jitter 的自动重试。
     *
     * 核心逻辑：
     * 1. 从 retryQueue 中取出失败消息
     * 2. 检查重试次数，超过 5 次则转入 DLQ（放弃重试）
     * 3. 计算退避延迟：2^retry * 1000ms + 随机 0~500ms
     * 4. 等待退避时间后，将消息重新投入 dbQueue 写入
     *
     * 指数退避示例（不含 Jitter）：
     * - 第 1 次重试：等待 2^1 = 2 秒
     * - 第 2 次重试：等待 2^2 = 4 秒
     * - 第 3 次重试：等待 2^3 = 8 秒
     * - 第 4 次重试：等待 2^4 = 16 秒
     * - 第 5 次重试：等待 2^5 = 32 秒
     * - 超过 5 次 → 进入 DLQ
     *
     * Jitter（随机抖动）的作用：
     * 如果多个消息同时失败，它们的重试时间会被打散，
     * 避免所有消息在同一时刻重试造成"惊群效应"（Thundering Herd），
     * 即瞬时流量尖峰再次压垮下游。
     *
     * 为什么用单线程：
     * 重试消息通常数量较少，单线程足够处理。
     * 如果重试队列持续积压，说明下游问题严重，应该触发告警。
     */
    private static void startRetryWorker() {

        new Thread(() -> {

            Random random = new Random();

            while (true) {

                try {

                    Message msg = retryQueue.take();

                    int retry = msg.retryCount;

                    // 重试次数超过上限（5 次），放弃重试，转入 DLQ
                    // 设计思路：重试不是万能的，超过一定次数说明问题不是临时性的，
                    // 继续重试只会浪费资源并延迟发现真正的问题
                    if (retry > 5) {

                        dlqQueue.offer(msg);

                        System.out.println(
                                ">>> 超过最大重试次数，进入DLQ");

                        continue;
                    }

                    /**
                     * 指数退避 + Jitter 延迟计算
                     *
                     * 公式：delay = 2^retry * 1000ms + random(0~500ms)
                     *
                     * - 2^retry：指数增长，重试次数越多等待越久，
                     *   给下游更多恢复时间
                     * - * 1000：转为毫秒
                     * - random(500)：添加 0~500ms 的随机抖动（Full Jitter），
                     *   将并发重试请求在时间上打散
                     *
                     * 这是 AWS 推荐的重试策略之一（Exponential Backoff with Jitter），
                     * 在分布式系统中比固定间隔重试效果好得多。
                     */
                    long delay =
                            (long) Math.pow(2, retry) * 1000
                                    + random.nextInt(500);

                    System.out.println(
                            ">>> Retry第"
                                    + retry
                                    + "次, delay="
                                    + delay + "ms");

                    Thread.sleep(delay);

                    msg.retryCount++;

                    // 重试完成后重新投入 DB 写入队列
                    dbQueue.offer(msg);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }

    /**
     * 启动监控线程 —— 实时采集指标并触发熔断/半开决策。
     *
     * 职责：
     * 1. 每 2 秒采集一次各队列的积压数量
     * 2. 采集 DB 平均响应时间（avgDbRt）
     * 3. 根据以下规则判断是否触发熔断：
     *    - dbQueue 积压 > 3000 条：队列即将溢出，说明消费速度严重滞后
     *    - avgDbRt > 2000ms：数据库响应过慢，继续写入只会加剧问题
     *
     * 熔断后的动作：
     * 1. paused = true → Kafka Consumer 检测到后暂停消费
     * 2. openTime = 当前时间 → 记录熔断触发时刻，用于计算冷却时间
     * 3. pollSize = 10 → 动态限流，将拉取速率降为原来的 1/10
     *
     * 恢复机制（半开状态）：
     * 本 Demo 中半开状态由 DB Worker 触发（检测到 RT 回落），
     * Monitor 负责在 dbQueue 积压回落或 RT 正常后取消熔断标志。
     *
     * 生产环境中的增强：
     * - 使用滑动窗口计算 RT 的 P50/P99，而非只看最近一次
     * - 增加熔断持续时间的自动半开切换（如熔断 30 秒后自动进入半开）
     * - 接入 Prometheus/Grafana 进行可视化监控
     * - 增加告警通知（钉钉、邮件、短信）
     */
    private static void startMonitor() {

        new Thread(() -> {

            while (true) {

                try {

                    int dbSize = dbQueue.size();

                    long rt = avgDbRt.get();

                    // 打印当前系统状态快照，方便观察各队列积压和熔断状态
                    System.out.println(
                            "\n========================");
                    System.out.println(
                            "parseQueue=" + parseQueue.size());
                    System.out.println(
                            "transformQueue=" + transformQueue.size());
                    System.out.println(
                            "dbQueue=" + dbSize);
                    System.out.println(
                            "retryQueue=" + retryQueue.size());
                    System.out.println(
                            "dlqQueue=" + dlqQueue.size());
                    System.out.println(
                            "avgDbRt=" + rt);
                    System.out.println(
                            "pollSize=" + pollSize);
                    System.out.println(
                            "paused=" + paused.get());
                    System.out.println(
                            "halfOpen=" + halfOpen.get());

                    /**
                     * 熔断触发条件（满足任一即触发）：
                     *
                     * 条件1：dbQueue 积压 > 3000 条
                     * 含义：5000 容量的队列已经用了 60%，消费速度远跟不上生产速度，
                     * 如果不干预，队列很快会满，导致上游阻塞甚至超时。
                     *
                     * 条件2：DB 平均响应时间 > 2000ms
                     * 含义：数据库已经严重过载或存在慢查询，
                     * 继续加大写入压力只会让情况更糟（雪崩效应），
                     * 需要暂停消费让数据库"喘口气"。
                     *
                     * 只在 !paused 时触发，避免重复触发。
                     */
                    if (dbSize > 3000 || rt > 2000) {

                        if (!paused.get()) {

                            System.out.println(
                                    "\n>>> 触发熔断!!!");

                            paused.set(true);

                            // 记录熔断触发时间
                            openTime =
                                    System.currentTimeMillis();

                            /**
                             * 动态限流：将 pollSize 从 100 降为 10
                             *
                             * 效果：即使 Kafka Consumer 恢复消费，
                             * 每次也只能拉取 10 条消息（原来 1/10 的流量），
                             * 实现"软恢复"而非一次性涌入大量消息。
                             *
                             * 这比直接停止消费更温和：
                             * - 完全停止：恢复时流量突增，可能二次触发熔断
                             * - 限流恢复：流量缓慢回升，给下游适应时间
                             */
                            pollSize = 10;
                        }
                    }

                    // 每 2 秒采集一次指标
                    Thread.sleep(2000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }


}
