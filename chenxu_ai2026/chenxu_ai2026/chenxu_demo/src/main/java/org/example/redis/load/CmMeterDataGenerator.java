package org.example.redis.load;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * C_METER 多线程造假数据工具。
 * <p>
 * 用途：向 C_METER 表批量插入 1000w 条模拟数据，用于测试 Redis 全量加载流程。
 * <p>
 * 线程模型：
 * <pre>
 *   main 线程
 *     ├─ Thread-0: 写入第      1 ~ 100w 条
 *     ├─ Thread-1: 写入第 100w+1 ~ 200w 条
 *     ├─ Thread-2: 写入第 200w+1 ~ 300w 条
 *     ├─ ...
 *     └─ Thread-9: 写入第 900w+1 ~ 1000w 条
 * </pre>
 * 使用 {@link CountDownLatch} 确保主线程在所有子线程完成后才打印汇总统计。
 * <p>
 * 数据规则：
 * <pre>
 *   meter_id      8 位数字，全局自增序号 % 1亿，左补零（如 "00000001"）
 *   meter_port    4 位随机数字（如 "0123"）
 *   COMM_NO       6 位随机数字（如 "000456"）
 *   asset_no      10 位随机数字（如 "0000789012"）
 *   meter_address 16 个随机中文字符（如 "北京朝阳区建外大街光华路..."）
 * </pre>
 * <p>
 * 性能优化：
 * <ul>
 *   <li>{@code rewriteBatchedStatements=true}：MySQL JDBC 驱动会把多条 INSERT 改写为
 *       {@code INSERT INTO ... VALUES (...), (...), (...)} 大幅减少网络往返和 SQL 解析开销</li>
 *   <li>手动事务：每 5000 条 commit 一次，避免一个事务太大导致 undo log 膨胀</li>
 *   <li>{@code PreparedStatement}：预编译 SQL，避免每次执行都解析 SQL 文本</li>
 * </ul>
 * <p>
 * 预估耗时：10 线程 × 100w 条/线程，单线程约 30~60 秒，整体约 1~2 分钟。
 * <p>
 * 使用方式：
 * <ul>
 *   <li>直接运行 main() 方法</li>
 *   <li>或通过 {@code POST /meter/generate} 接口触发</li>
 * </ul>
 */
public class CmMeterDataGenerator {

    /** 目标总记录数：1000 万 */
    private static final int TOTAL_RECORDS = 10_000_000;

    /** 并发线程数，每个线程负责 100w 条 */
    private static final int THREAD_COUNT = 10;

    /** 每个线程写入的记录数 */
    private static final int RECORDS_PER_THREAD = TOTAL_RECORDS / THREAD_COUNT;

    /** JDBC 批量提交大小：每 5000 条 executeBatch + commit 一次 */
    private static final int BATCH_SIZE = 5000;

    /**
     * 全局自增序号，跨线程共享。
     * 使用 AtomicLong 保证线程安全，每个线程调用 incrementAndGet() 获取唯一序号。
     * 序号对 1 亿取模后格式化为 8 位数字，超过 1 亿时回绕（实际 1000w 数据不会回绕）。
     */
    private static final AtomicLong SEQ = new AtomicLong(0);

    /**
     * JDBC 连接参数说明：
     * <pre>
     *   rewriteBatchedStatements=true  → MySQL JDBC 批量改写（关键！性能可提升 5~10 倍）
     *   useSSL=false                   → 内网环境关闭 SSL，减少握手开销
     *   serverTimezone=Asia/Shanghai   → 时区设置，避免时间字段偏差
     *   characterEncoding=utf-8        → 字符编码，确保中文不乱码
     * </pre>
     */
    private static final String URL = "jdbc:mysql://192.168.249.129:3306/austin"
            + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf-8&rewriteBatchedStatements=true";
    private static final String USER = "root";
    private static final String PASSWORD = "snCJadmin-1";

    /** 预编译 INSERT SQL，所有线程共用同一模板 */
    private static final String INSERT_SQL =
            "INSERT INTO C_METER (meter_id, meter_port, COMM_NO, asset_no, meter_address) VALUES (?,?,?,?,?)";

    /** 随机数生成器，每个线程复用（Random 是线程安全的） */
    private static final Random RAND = new Random();

    /**
     * 程序入口。
     *
     * @param args 命令行参数（暂未使用）
     */
    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        System.out.println("开始生成 " + TOTAL_RECORDS / 10000 + "w 条数据，线程数: " + THREAD_COUNT);

        // 固定大小线程池：10 个工作线程
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);

        // 倒计时门闩：初始值为线程数，每个线程完成后减 1，归零后主线程继续
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // 启动 10 个线程，每个线程写入自己负责的那一段数据
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadNo = t;
            pool.submit(() -> {
                try {
                    insertBatch(threadNo, RECORDS_PER_THREAD);
                } finally {
                    latch.countDown();   // 无论成功失败都要 countDown，否则主线程永远阻塞
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        pool.shutdown();

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("全部完成! 总耗时 %.1f 秒, 平均 %.0f 条/秒%n",
                elapsed / 1000.0, TOTAL_RECORDS * 1000.0 / elapsed);
    }

    /**
     * 单个线程的批量插入逻辑。
     * <p>
     * 流程：
     * <pre>
     *   循环直到写完本线程配额:
     *     1. 构造 5000 条数据加入 PreparedStatement 的 batch
     *     2. executeBatch() 发送批量 INSERT 到 MySQL
     *     3. commit() 提交事务
     *     4. 每 10 万条打印进度
     * </pre>
     *
     * @param threadNo 线程编号（仅用于日志）
     * @param total    本线程需要写入的总记录数（100w）
     */
    private static void insertBatch(int threadNo, int total) {
        int done = 0;   // 本线程已完成数

        // try-with-resources 确保 Connection 和 PreparedStatement 被正确关闭
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            // 关闭自动提交 → 手动事务控制，每 BATCH_SIZE 条 commit 一次
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
                while (done < total) {
                    // 最后一轮可能不足 BATCH_SIZE，取较小值
                    int batch = Math.min(BATCH_SIZE, total - done);

                    // 填充 batch 条数据到 PreparedStatement
                    for (int i = 0; i < batch; i++) {
                        fillRow(ps);
                        ps.addBatch();   // 加入批次队列，尚未发送
                    }

                    // ====== 一次网络往返发送 batch 条 INSERT ======
                    ps.executeBatch();   // 执行批次
                    conn.commit();       // 提交事务

                    done += batch;

                    // 每 10 万条打印进度
                    if (done % 100_000 == 0 || done == total) {
                        System.out.printf("[线程-%d] 进度: %d/%d (%.1f%%)%n",
                                threadNo, done, total, done * 100.0 / total);
                    }
                }
            }
        } catch (SQLException e) {
            // 某个线程写入失败不会影响其他线程
            System.err.printf("[线程-%d] 出错: %s%n", threadNo, e.getMessage());
            e.printStackTrace();
        }
        System.out.printf("[线程-%d] 完成, 共写入 %d 条%n", threadNo, done);
    }

    /**
     * 填充一行随机数据到 PreparedStatement。
     * <p>
     * 参数位置：
     * <pre>
     *   ps.setString(1, ...) → meter_id       (8 位数字)
     *   ps.setString(2, ...) → meter_port     (4 位数字)
     *   ps.setString(3, ...) → COMM_NO        (6 位数字)
     *   ps.setString(4, ...) → asset_no       (10 位数字)
     *   ps.setString(5, ...) → meter_address  (16 个中文字符)
     * </pre>
     */
    private static void fillRow(PreparedStatement ps) throws SQLException {
        // 原子自增 + 对 1 亿取模 + 左补零 → 始终 8 位
        long seq = SEQ.incrementAndGet();
        ps.setString(1, String.format("%08d", seq % 100_000_000));
        // meter_port: 4 位随机数字 [0000, 9999]
        ps.setString(2, String.format("%04d", RAND.nextInt(10000)));
        // COMM_NO: 6 位随机数字 [000000, 999999]
        ps.setString(3, String.format("%06d", RAND.nextInt(1000000)));
        // asset_no: 10 位随机数字 [0000000000, 9999999999]
        // Math.abs 防止 nextLong() 返回负数取模后仍是负数
        ps.setString(4, String.format("%010d", Math.abs(RAND.nextLong()) % 10_000_000_000L));
        // meter_address: 16 个随机中文字符
        ps.setString(5, randomChinese(16));
    }

    /**
     * 生成 n 个随机中文字符。
     * <p>
     * 中文 Unicode 范围：{@code 一 (一)} ~ {@code 龥 (龥)}
     * 共 20902 个常用汉字，覆盖绝大多数中文场景。
     * <p>
     * 注意：这里生成的是随机字符序列，不一定是通顺的词语。
     * 如果需要语义化的地址，可以维护一个地址库随机拼接。
     *
     * @param n 需要生成的汉字数量
     * @return 长度为 n 的随机中文字符串
     */
    private static String randomChinese(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            // 在 [0x4E00, 0x9FA5] 区间内随机取一个码点
            char c = (char) (0x4E00 + RAND.nextInt(0x9FA5 - 0x4E00 + 1));
            sb.append(c);
        }
        return sb.toString();
    }
}
