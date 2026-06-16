# Kafka 背压+熔断+降级 & 1000w 数据加载到 Redis 完整 Demo

> 涵盖两次提交的核心内容：
> - `e5cb060` — Kafka 背压 + 熔断 + 降级 全链路演示
> - `996a2f3` — C_METER 1000w 数据全量加载到 Redis 完整 Demo

---

# 第一部分：Kafka 背压 + 熔断 + 降级

## 1. 概述

本 Demo 模拟了一个工业级的 **Kafka 消费 → 解析 → 转换 → 入库** 数据处理管道，纯 Java 实现（无需真实 Kafka/DB），在管道中集成了 **8 种容错机制**。

提供两个版本：

| 文件 | 行数 | 说明 |
|------|------|------|
| `FullKafkaBackpressureDemo.java` | 715 行 | 完整版：8 种机制 + 3 阶段线程池 |
| `KafkaBackpressureDemo.java` | 203 行 | 精简版：核心背压 + 熔断，无解析/转换/重试/DLQ |

文件路径：
```
chenxu_demo/src/main/java/org/example/kafka/circuitbreakerDegradation/
  ├─ FullKafkaBackpressureDemo.java   # 完整版（推荐学习）
  ├─ KafkaBackpressureDemo.java       # 精简版（快速理解核心）
  └─ Message.java                     # 消息实体
```

## 2. 整体数据流向

```
Kafka Consumer
    │  pollSize 条/次（默认100，熔断时降为10）
    ▼
[parseQueue]  LinkedBlockingQueue(5000)   ← 有界队列，满时 put() 阻塞 = 背压
    │
    ▼  Parse Workers × 4（CPU 密集型，模拟 JSON 解析 5ms）
[transformQueue]  LinkedBlockingQueue(5000)
    │
    ▼  Transform Workers × 4（模拟字段转换/数据清洗 10ms）
[dbQueue]  LinkedBlockingQueue(5000)     ← 最关键队列，积压量触发熔断
    │
    ▼  DB Workers × 8（I/O 密集型，drainTo 批量取 100 条写库）
    ├── 成功 → 完成
    ├── IllegalArgumentException（JSON格式错误等不可恢复）→ [dlqQueue]  死信队列
    └── 其他异常（网络超时等可恢复）→ [retryQueue]  重试队列
                                        │
                                        ▼  Retry Worker × 1（指数退避 + Jitter）
                                        ├── retry ≤ 5 次 → 重新投入 dbQueue
                                        └── retry > 5 次 → 转入 [dlqQueue]
```

## 3. 8 种容错机制详解

### 3.1 背压（Backpressure）

**核心原理**：通过 `LinkedBlockingQueue<>(5000)` 有界队列连接各阶段。当队列满时，`put()` 方法阻塞上游生产者线程，自动减缓生产速度，防止内存溢出（OOM）。

```java
// 有界队列：容量 5000，满时自动阻塞上游
private static final BlockingQueue<Message> dbQueue =
        new LinkedBlockingQueue<>(5000);

// 背压体现：队列满时 put() 阻塞，Kafka Consumer 自动减速
dbQueue.put(msg);
```

**关键点**：
- 不是丢弃消息，而是让上游等待——消息不丢失
- 无需手动控制速率，队列容量天然形成"限流阀"
- 5000 的容量提供了缓冲余量，应对瞬时流量尖峰

### 3.2 熔断器（Circuit Breaker）

**三态模型**：`CLOSED（正常）→ OPEN（熔断）→ HALF_OPEN（半开）→ CLOSED`

```
    dbQueue积压<3000 && RT<2000ms
    ┌──────────────────────────────┐
    │         CLOSED（正常）         │
    │   pollSize=100, 正常消费      │
    └──────────────────────────────┘
              │ dbQueue>3000 或 RT>2000ms
              ▼
    ┌──────────────────────────────┐
    │         OPEN（熔断）           │
    │   paused=true, pollSize=10   │
    │   消费暂停, 小流量试探         │
    └──────────────────────────────┘
              │ DB Worker 检测到 RT<500ms
              ▼
    ┌──────────────────────────────┐
    │       HALF_OPEN（半开）        │
    │   halfOpen=true, 试探写入     │
    │   成功且RT<500 → 关闭熔断      │
    │   失败 → 重新OPEN              │
    └──────────────────────────────┘
```

**触发条件**（Monitor 线程每 2 秒检查）：
- `dbQueue.size() > 3000` — 队列积压超过 60%
- `avgDbRt > 2000ms` — 数据库响应过慢

**熔断动作**：
```java
paused.set(true);        // Kafka Consumer 检测到后暂停消费
pollSize = 10;           // 动态限流：拉取速率降为 1/10
```

**恢复机制**（DB Worker 在半开状态下检测）：
```java
if (halfOpen.get() && rt < 500) {
    halfOpen.set(false);
    pollSize = 100;      // 恢复正常消费速率
}
```

### 3.3 线程池隔离（Thread Pool Isolation）

三个阶段使用独立的 `ExecutorService`，防止某个阶段异常拖垮整个链路：

```java
// 解析：CPU 密集型 → 4 线程（接近 CPU 核心数）
private static final ExecutorService parseExecutor =
        Executors.newFixedThreadPool(4);

// 转换：CPU 密集型 → 4 线程
private static final ExecutorService transformExecutor =
        Executors.newFixedThreadPool(4);

// DB 写入：I/O 密集型 → 8 线程（大部分时间在等待DB响应）
private static final ExecutorService dbExecutor =
        Executors.newFixedThreadPool(8);
```

### 3.4 批量写库（Batch Write）

DB Worker 使用 `take() + drainTo()` 模式，每次最多取 100 条批量写入：

```java
List<Message> batch = new ArrayList<>();
Message first = dbQueue.take();       // 阻塞获取第一条
batch.add(first);
dbQueue.drainTo(batch, 99);           // 非阻塞再取最多 99 条
// → 每批最多 100 条，减少 DB 连接/事务开销
```

### 3.5 重试 + 指数退避 + Jitter

独立的 `RetryWorker` 单线程处理重试：

```
delay = 2^retryCount × 1000ms + random(0~500ms)

第 1 次重试: 等待 2^1 = 2    秒 + 0~500ms
第 2 次重试: 等待 2^2 = 4    秒 + 0~500ms
第 3 次重试: 等待 2^3 = 8    秒 + 0~500ms
第 4 次重试: 等待 2^4 = 16   秒 + 0~500ms
第 5 次重试: 等待 2^5 = 32   秒 + 0~500ms
超过 5 次 → 转入死信队列（DLQ）
```

**Jitter 的作用**：多个消息同时失败时，随机抖动将重试时间打散，避免"惊群效应"（Thundering Herd）——即所有消息在同一时刻重试，瞬时流量尖峰再次压垮下游。

```java
long delay = (long) Math.pow(2, retry) * 1000 + random.nextInt(500);
Thread.sleep(delay);
```

### 3.6 死信队列（DLQ）

不可恢复的消息进入 DLQ，等待人工排查后重新投递：

```java
// 不可恢复场景 1：数据格式异常
if (msg.id % 333 == 0) {
    throw new IllegalArgumentException("JSON格式异常");
}
// → catch (IllegalArgumentException e) → dlqQueue.offer(badMsg)

// 不可恢复场景 2：重试次数耗尽
if (retry > 5) {
    dlqQueue.offer(msg);
}
```

### 3.7 动态限流（Dynamic Rate Limiting）

`pollSize` 在正常和熔断状态间动态调整，实现"软恢复"：

```
正常状态: pollSize = 100
熔断触发: pollSize = 10   （流量降为 1/10）
恢复成功: pollSize = 100   （恢复正常）
```

这比"完全停止再恢复"更平滑，避免恢复时流量突增造成二次熔断。

### 3.8 最终一致性

消息不会丢失——无论走哪条路径，最终都会被处理：

```
正常路径:     Kafka → 解析 → 转换 → DB 写入 → 成功
可恢复异常:   Kafka → ... → DB 失败 → 重试(最多5次) → 成功
不可恢复异常: Kafka → ... → DLQ → 人工修复 → 重新投递
```

## 4. Monitor 监控线程

每 2 秒采集一次全链路指标：

```java
System.out.println("parseQueue="    + parseQueue.size());
System.out.println("transformQueue=" + transformQueue.size());
System.out.println("dbQueue="       + dbQueue.size());
System.out.println("retryQueue="    + retryQueue.size());
System.out.println("dlqQueue="      + dlqQueue.size());
System.out.println("avgDbRt="       + avgDbRt.get());
System.out.println("pollSize="      + pollSize);
System.out.println("paused="        + paused.get());
System.out.println("halfOpen="      + halfOpen.get());
```

## 5. 如何运行

直接执行 `main()` 方法，无需任何外部依赖：

```bash
# 完整版（推荐）
java org.example.kafka.circuitbreakerDegradation.FullKafkaBackpressureDemo

# 精简版（快速理解核心）
java org.example.kafka.circuitbreakerDegradation.KafkaBackpressureDemo
```

控制台会实时输出各队列积压量、DB RT、熔断状态等指标。

## 6. Sentinel 生产级保护

在 Spring 的真实 Kafka 发送路径上，`KafkaMessageProducer` 受 Sentinel 保护：

```java
@Component
@RefreshScope   // Nacos 动态刷新 topic 等配置
public class KafkaMessageProducer {

    @SentinelResource(
            value = "kafkaSend",                      // 资源名
            blockHandlerClass = SentinelBlockHandler.class,
            blockHandler = "kafkaSendBlock",           // 限流兜底 → 429
            fallbackClass = SentinelBlockHandler.class,
            fallback = "kafkaSendFallback"             // 异常兜底 → 500
    )
    public void send(String key, String value) { ... }
}
```

Sentinel 规则（`SentinelRulesInit.java`）：
- **流控**：QPS ≤ 100（超出返回 429）
- **熔断**：异常比例 > 30% 时触发（慢调用比例 > 50% → 30s 熔断）

`@RefreshScope` 说明：该注解使 Bean 在 Nacos 配置变更时重新创建，`@Value("${kafka.topic}")` 等配置项可热刷新。

---

# 第二部分：1000w 数据全量加载到 Redis

## 1. 概述

将 MySQL `C_METER` 表中 **1000 万行** 数据全量加载到 Redis Hash 结构，通过 **流式游标** 避免 OOM，通过 **Redis Pipeline** 减少网络 RTT，通过 **Sentinel** 防止并发重复执行。

文件路径：
```
chenxu_demo/src/main/java/org/example/redis/load/
  ├─ CmMeter.java               # 实体类
  ├─ CmMeterMapper.java         # MyBatis Mapper 接口
  ├─ CmMeterService.java        # Service 接口
  ├─ CmMeterServiceImpl.java    # 核心实现（流式游标 + Pipeline 批量写入）
  ├─ CmMeterController.java     # REST 接口
  └─ CmMeterDataGenerator.java  # 多线程造假数据工具
```

MyBatis XML：`chenxu_demo/src/main/resources/mapper/CmMeterMapper.xml`
DDL：`chenxu_demo/src/main/resources/c_meter.sql`

## 2. 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        准备工作                                   │
│  POST /meter/generate                                            │
│    → CmMeterDataGenerator.main()                                 │
│    → 10线程 × 100w条/线程 = 1000w 行                              │
│    → JDBC rewriteBatchedStatements 批量插入                       │
│    → 预计 1~2 分钟                                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      全量加载（核心）                               │
│  POST /meter/load                                                │
│    → CmMeterServiceImpl.loadAllToRedis()                         │
│    → MyBatis 流式游标（fetchSize = Integer.MIN_VALUE）            │
│    → ResultHandler 逐行回调                                       │
│    → 攒批 5000 条 → Redis Pipeline hMSet                          │
│    → 每 10w 条打印进度                                            │
│    → 预计 3~6 分钟                                                │
│                                                                   │
│  @SentinelResource("loadAllToRedis")                             │
│    ├─ 流控: 并发线程数 ≤ 1（防重复执行）                            │
│    └─ 熔断: 慢调用比例 > 50% → 30s 熔断                            │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        验证结果                                   │
│  GET /meter/count  → redisTemplate.keys("meter:*").size()        │
│  GET /meter/getkey/{id}  → redisTemplate.opsForHash().entries()  │
└─────────────────────────────────────────────────────────────────┘
```

## 3. 关键设计点

### 3.1 流式游标：避免 OOM

MyBatis 流式读取的关键配置：

```java
// CmMeterMapper.java
@Options(fetchSize = Integer.MIN_VALUE)  // MySQL 驱动识别为流式模式
@ResultType(CmMeter.class)
void streamAll(ResultHandler<CmMeter> handler);
```

```xml
<!-- CmMeterMapper.xml -->
<select id="streamAll" resultMap="BaseResultMap" fetchSize="-2147483648">
    SELECT id, meter_id, meter_port, COMM_NO, asset_no, meter_address FROM C_METER
</select>
```

**原理**：`fetchSize = Integer.MIN_VALUE` 告诉 MySQL JDBC 驱动使用游标模式，一次只从数据库读取一行到内存。如果不这样做，1000w 行一次性加载会导致 OOM。

### 3.2 Pipeline 批量写入：减少网络 RTT

```java
// 每攒够 5000 条，通过 Pipeline 一次性发送到 Redis
redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (CmMeter meter : batch) {
        byte[] key = s.serialize("meter:" + meter.getMeterId());
        Map<byte[], byte[]> fieldMap = new LinkedHashMap<>();
        fieldMap.put(s.serialize("meter_id"),      s.serialize(meter.getMeterId()));
        fieldMap.put(s.serialize("meter_port"),    s.serialize(meter.getMeterPort()));
        fieldMap.put(s.serialize("comm_no"),       s.serialize(meter.getCommNo()));
        fieldMap.put(s.serialize("asset_no"),      s.serialize(meter.getAssetNo()));
        fieldMap.put(s.serialize("meter_address"), s.serialize(meter.getMeterAddress()));
        connection.hashCommands().hMSet(key, fieldMap);
    }
    return null;
});
```

**原理**：Pipeline 将多次 `hMSet` 命令打包在一次网络往返中发送给 Redis，将 5000 次 RTT 减少为 1 次。1000w 条数据从 1000w 次 RTT 减少为 2000 次（10000000÷5000）。

### 3.3 攒批与进度日志

```java
// 流式回调中攒批
cmMeterMapper.streamAll(ctx -> {
    CmMeter meter = ctx.getResultObject();
    batch.add(meter);

    if (batch.size() >= BATCH_SIZE) {
        flushBatch(new ArrayList<>(batch));  // 拷贝防并发修改
        long done = loadedCount.addAndGet(batch.size());
        batch.clear();

        // 每 10w 条打印进度
        if (done - lastLogMark.get() >= 100_000) {
            lastLogMark.set(done);
            log.info("进度: {}/{} ({}%)", done, total, ...);
        }
    }
});
// 处理不足 5000 条的尾部数据
if (!batch.isEmpty()) {
    flushBatch(batch);
}
```

### 3.4 Redis 存储结构

每条 C_METER 记录存储为一个 Redis Hash：

```
Key:   meter:00012345
Hash:  { meter_id → "00012345", meter_port → "0123",
         comm_no → "000456", asset_no → "0000789012",
         meter_address → "北京朝阳区建外大街..." }
```

选择 Hash 而非 String JSON 的原因：
- 可以按字段独立读写，不用整体序列化/反序列化
- 内存更省（Redis Hash 在字段少时使用 ziplist 编码）
- 支持 `HINCRBY`、`HSETNX` 等原子操作

## 4. 假数据生成器（CmMeterDataGenerator）

### 线程模型

```
main 线程
  ├─ Thread-0: meter_id 00000001 ~ 01000000（第      1 ~  100w 条）
  ├─ Thread-1: meter_id 01000001 ~ 02000000（第  100w+1 ~ 200w 条）
  ├─ ...
  └─ Thread-9: meter_id 09000001 ~ 10000000（第  900w+1 ~ 1000w 条）
```

### 性能优化

```java
// 1. MySQL JDBC 批量改写：多条 INSERT 合并为一条
//    INSERT INTO ... VALUES (...), (...), (...)
//    性能提升 5~10 倍
String URL = "jdbc:mysql://...?rewriteBatchedStatements=true";

// 2. 手动事务控制：每 5000 条 commit 一次
conn.setAutoCommit(false);
ps.executeBatch();
conn.commit();

// 3. PreparedStatement 预编译 + addBatch
for (int i = 0; i < batch; i++) {
    fillRow(ps);
    ps.addBatch();
}

// 4. CountDownLatch 等待所有线程完成
CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
// 每个线程完成后: latch.countDown();
latch.await();  // 主线程等待所有子线程完成
```

### 全局自增序号

```java
private static final AtomicLong SEQ = new AtomicLong(0);

// 每个线程调用 incrementAndGet() 获取唯一序号
long seq = SEQ.incrementAndGet();
ps.setString(1, String.format("%08d", seq % 100_000_000));
```

使用 `AtomicLong` 保证 10 个线程之间的序号不重复。

## 5. Sentinel 防护

`CmMeterServiceImpl.loadAllToRedis()` 上使用 `@SentinelResource`：

```java
@SentinelResource(
    value = SentinelRulesInit.RESOURCE_LOAD_ALL,  // "loadAllToRedis"
    blockHandlerClass = SentinelBlockHandler.class,
    blockHandler = "loadAllToRedisBlock",           // FlowException → 429
    fallbackClass = SentinelBlockHandler.class,
    fallback = "loadAllToRedisFallback"             // 其他异常 → 500
)
public String loadAllToRedis() { ... }
```

Sentinel 规则（`SentinelRulesInit.java`）：

| 规则类型 | 参数 | 效果 |
|----------|------|------|
| 流控（FlowRule） | 并发线程数 ≤ 1 | 防止多个全量加载任务同时执行 |
| 熔断（DegradeRule） | 慢调用比例 > 50%，count=200ms | 30s 窗口内慢调用超 50% → 熔断 30s |

blockHandler 返回示例：
```json
{"code":429, "msg":"全量加载任务已在执行中，请稍后再试（Sentinel 并发流控）"}
{"code":503, "msg":"数据加载服务暂时不可用（熔断中），请稍后再试"}
```

## 6. REST 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/meter/generate` | POST | 异步生成 1000w 条假数据到 MySQL |
| `/meter/load` | POST | 同步全量加载到 Redis（阻塞 3~6 分钟） |
| `/meter/count` | GET | 查询 Redis 中 meter 记录数 |
| `/meter/getkey/{key}` | GET | 根据 meter_id 查询单条记录 |

## 7. 依赖一览

```xml
<!-- MyBatis 流式游标 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>

<!-- Redis Pipeline -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Sentinel 熔断限流 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

---

# 第三部分：注解速查

| 注解/技术 | 位置 | 用途 |
|-----------|------|------|
| `@SentinelResource` | `KafkaMessageProducer.send()`、`CmMeterServiceImpl.loadAllToRedis()` | 声明 Sentinel 资源，配置 blockHandler/fallback |
| `@RefreshScope` | `KafkaMessageProducer`、`KafkaTopicConfig` | Nacos 配置变更时动态刷新 Bean |
| `@PostConstruct` | `SentinelRulesInit.init()` | 应用启动时自动加载 Sentinel 规则 |
| `LinkedBlockingQueue(5000)` | `FullKafkaBackpressureDemo` | 有界队列实现背压 |
| `AtomicBoolean` | `paused`、`halfOpen` | 熔断状态标志，保证多线程可见性 |
| `AtomicLong` | `avgDbRt`、`SEQ` | 原子计数器，无锁线程安全 |
| `volatile` | `pollSize` | 保证 Monitor 和 Consumer 线程间可见性 |
| `ExecutorService` | parseExecutor / transformExecutor / dbExecutor | 线程池隔离，防止级联故障 |
| `CountDownLatch` | `CmMeterDataGenerator` | 等待所有线程完成 |
| `BlockingQueue.drainTo()` | DB Worker | 批量取消息，减少开销 |
| `executePipelined()` | `CmMeterServiceImpl.flushBatch()` | Redis Pipeline 减少网络 RTT |
| `@Options(fetchSize = Integer.MIN_VALUE)` | `CmMeterMapper.streamAll()` | MyBatis 流式游标，避免 OOM |
| `rewriteBatchedStatements=true` | JDBC URL | MySQL 批量 INSERT 改写 |

---

# 第四部分：快速启动

```bash
# 1. 运行 Kafka 全链路 Demo（纯 Java，无需外部依赖）
#    直接运行 FullKafkaBackpressureDemo.main()

# 2. 生成假数据
curl -X POST http://localhost:8080/meter/generate
# 返回: "数据生成已在后台启动，请观察控制台日志"

# 3. 等待数据生成完成后，全量加载到 Redis
curl -X POST http://localhost:8080/meter/load
# 返回: "加载完成! 共 10000000 条记录, 耗时 245000 ms (245.00 秒)..."

# 4. 验证
curl http://localhost:8080/meter/count
# 返回: "Redis 中 meter 记录数: 10000000"

curl http://localhost:8080/meter/getkey/00012345
# 返回各字段值
```
