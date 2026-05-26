package org.example.thread.aqs;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * 模拟多线程分段计算 1~N 的累加和，最后汇总
 */
//适用问题：需要多个线程分别完成子任务，然后所有线程到达屏障点后再一起继续执行，且屏障可以重复使用（与 CountDownLatch 一次性的区别）。
//        AQS 实现：内部使用 ReentrantLock + Condition 实现屏障等待/唤醒机制。
public class CyclicBarrierDemo {
    private static final int THREAD_COUNT = 4;   // 分段线程数
    private static final int TOTAL_NUMBER = 100; // 计算 1+2+...+100
    private static int[] partialSums = new int[THREAD_COUNT]; // 每段计算结果
    private static int finalSum = 0;

    public static void main(String[] args) {
        // 创建 CyclicBarrier，当所有线程都到达后，执行汇总任务（Runnable）
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT, () -> {
            // 汇总线程（最后一个到达的线程会自动执行此任务）
            for (int sum : partialSums) {
                finalSum += sum;
            }
            System.out.println("所有分段计算完成，最终累加和 = " + finalSum);
        });

        // 启动 THREAD_COUNT 个线程，每个线程负责一段数字的累加
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                int start = threadId * (TOTAL_NUMBER / THREAD_COUNT) + 1;
                int end = (threadId + 1) * (TOTAL_NUMBER / THREAD_COUNT);
                // 处理最后可能除不尽的情况（本例 100/4=25 正好整除）
                if (threadId == THREAD_COUNT - 1) end = TOTAL_NUMBER;

                int sum = 0;
                for (int j = start; j <= end; j++) {
                    sum += j;
                }
                partialSums[threadId] = sum;
                System.out.printf("%s 计算了 [%d, %d] 的和 = %d%n",
                        Thread.currentThread().getName(), start, end, sum);

                try {
                    barrier.await(); // 等待其他线程
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }

                // 屏障之后的动作（例如所有线程可以继续执行后续，但本例无需）
                System.out.printf("%s 继续执行后续任务%n", Thread.currentThread().getName());
            }, "Calculator-" + i).start();
        }

        // 演示 CyclicBarrier 可重用性：我们可以再次使用同一个 barrier 对象做第二轮计算
        // 为了简洁，这里省略第二次重用，但原理是调用 barrier.reset() 或再次 await()
    }
}
