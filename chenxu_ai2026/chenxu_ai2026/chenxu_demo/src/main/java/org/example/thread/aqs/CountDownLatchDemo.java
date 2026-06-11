package org.example.thread.aqs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ====================================================================
 * CountDownLatch 基础用法 —— 主线程等待多个子线程完成
 * ====================================================================
 *
 * 【CountDownLatch 是什么】
 *   一个一次性的同步屏障。初始化时指定计数值 count，线程可以：
 *   - await()：阻塞等待，直到 count 降为 0
 *   - countDown()：将 count 减 1（通常在子线程完成任务后调用）
 *
 * 【典型场景】
 *   1. 主线程等待所有子线程初始化完成后再继续（本例）
 *   2. 并行计算：多个线程分别计算子任务，主线程汇总结果
 *   3. 门闩：所有条件满足后才放行
 *
 * 【AQS 内部原理】
 *   CountDownLatch 使用 AQS 共享模式：
 *   - state = count（计数器初始值）
 *   - countDown() → tryReleaseShared()：CAS 将 state - 1
 *     - 如果减到 0 → doReleaseShared() 唤醒所有等待线程
 *   - await() → tryAcquireShared()：检查 state 是否为 0
 *     - state != 0 → 返回 -1 → 线程入队阻塞
 *     - state == 0 → 返回 1 → 直接放行
 *
 * 【重要特性】
 *   - 一次性：count 降到 0 后不可重置（需要可重用请用 CyclicBarrier）
 *   - countDown() 不会阻塞：只是 CAS 减 1，非常轻量
 *   - await(timeout)：支持超时，避免无限等待
 *
 * 【CountDownLatch vs CyclicBarrier】
 *   ┌─────────────────────┬──────────────────────┬──────────────────────┐
 *   │       特性          │   CountDownLatch     │   CyclicBarrier      │
 *   ├─────────────────────┼──────────────────────┼──────────────────────┤
 *   │   重用性            │   一次性             │   可重复使用         │
 *   │   等待方            │   一个或多个线程     │   所有参与线程       │
 *   │   计数方向          │   递减到 0           │   递增到 N           │
 *   │   触发动作          │   无（可外部处理）   │   支持 barrierAction │
 *   │   AQS 模式          │   共享模式           │   ReentrantLock+Cond │
 *   └─────────────────────┴──────────────────────┴──────────────────────┘
 */
public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        int taskCount = 7;

        // 创建倒计数门闩，初始计数 = taskCount
        // AQS 内部：setState(taskCount)
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行任务 " + taskId);
                    TimeUnit.SECONDS.sleep((long) (Math.random() * 3)); // 模拟随机耗时
                    System.out.println(Thread.currentThread().getName() + " 完成任务 " + taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 任务完成，计数减一
                    // AQS 内部：CAS 将 state - 1
                    // 当 state 从 1 → 0 时，唤醒所有在 await() 上阻塞的线程
                    latch.countDown();
                }
            }, "工作线程-" + i).start();
        }

        // 主线程阻塞等待，直到 latch 的计数器降为 0
        // AQS 内部：tryAcquireShared 检查 state==0？
        //   - 不为 0 → 线程入队 park 等待
        //   - 为 0 → 直接返回
        System.out.println("主线程等待所有子任务完成...");
        latch.await();
        System.out.println("所有子任务已完成，主线程继续执行");
    }
}
