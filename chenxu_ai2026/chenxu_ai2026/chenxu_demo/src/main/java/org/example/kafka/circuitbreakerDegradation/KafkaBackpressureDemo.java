package org.example.kafka.circuitbreakerDegradation;


import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class KafkaBackpressureDemo {

    /**
     * 模拟 Kafka -> DB 的缓冲队列
     */
    private static final BlockingQueue<String> dbQueue =
            new LinkedBlockingQueue<>(2000);

    /**
     * DB写线程池
     */
    private static final ExecutorService dbExecutor =
            Executors.newFixedThreadPool(8);

    /**
     * 是否暂停消费（熔断）
     */
    private static final AtomicBoolean paused =
            new AtomicBoolean(false);

    /**
     * DB平均RT
     */
    private static final AtomicLong avgDbRt =
            new AtomicLong(0);

    /**
     * 模拟Kafka消费速度
     */
    private static volatile int pollSize = 100;

    public static void main(String[] args) {

        // 启动DB消费者
        startDbWorkers();

        // 启动监控线程
        startMonitor();

        // 模拟Kafka消费
        simulateKafkaConsumer();
    }

    /**
     * 模拟Kafka Consumer
     */
    private static void simulateKafkaConsumer() {

        int msgId = 0;

        while (true) {

            try {

                // 熔断：暂停消费
                if (paused.get()) {
                    System.out.println(">>> Kafka消费已暂停...");
                    Thread.sleep(3000);
                    continue;
                }

                // 模拟poll
                for (int i = 0; i < pollSize; i++) {

                    String msg = "message-" + (++msgId);

                    // 放入DB队列
                    dbQueue.put(msg);
                }

                System.out.println(
                        "poll " + pollSize +
                                " 条消息, 当前queue=" + dbQueue.size());

                Thread.sleep(500);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * DB线程池
     */
    private static void startDbWorkers() {

        for (int i = 0; i < 8; i++) {

            dbExecutor.submit(() -> {

                Random random = new Random();

                while (true) {

                    try {

                        String msg = dbQueue.take();

                        long start = System.currentTimeMillis();

                        /**
                         * 模拟数据库RT抖动
                         *
                         * 80%概率：正常
                         * 20%概率：数据库慢
                         */
                        if (random.nextInt(10) < 2) {

                            // DB抖动
                            Thread.sleep(3000);

                        } else {

                            // 正常RT
                            Thread.sleep(50);
                        }

                        long rt =
                                System.currentTimeMillis() - start;

                        avgDbRt.set(rt);

                        System.out.println(
                                Thread.currentThread().getName()
                                        + " 写库成功: "
                                        + msg
                                        + " RT="
                                        + rt + "ms");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 监控线程
     */
    private static void startMonitor() {

        new Thread(() -> {

            while (true) {

                try {

                    int queueSize = dbQueue.size();

                    long rt = avgDbRt.get();

                    System.out.println(
                            "\n===== MONITOR =====");
                    System.out.println(
                            "queueSize=" + queueSize);
                    System.out.println(
                            "avgDbRt=" + rt);
                    System.out.println(
                            "pollSize=" + pollSize);
                    System.out.println(
                            "paused=" + paused.get());

                    /**
                     * 熔断条件
                     */
                    if (queueSize > 1000 || rt > 2000) {

                        paused.set(true);

                        // 动态限流
                        pollSize = 10;

                        System.out.println(
                                ">>> 触发熔断!!!");

                    } else {

                        paused.set(false);

                        pollSize = 100;
                    }

                    Thread.sleep(2000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();
    }
}

