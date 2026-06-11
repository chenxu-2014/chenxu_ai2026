package org.example.thread.aqs;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * ====================================================================
 * CyclicBarrier（循环屏障） —— 多线程分段计算后汇总
 * ====================================================================
 *
 * 【CyclicBarrier 是什么】
 *   一组线程互相等待，所有线程都到达屏障点后，再一起继续执行。
 *   "Cyclic" 表示可重复使用：屏障被触发后自动重置，可以用于下一轮等待。
 *
 * 【与 CountDownLatch 的核心区别】
 *   ┌─────────────────────┬──────────────────────┬──────────────────────┐
 *   │       特性          │   CountDownLatch     │   CyclicBarrier      │
 *   ├─────────────────────┼──────────────────────┼──────────────────────┤
 *   │   重用性            │   一次性（不可重置） │   可循环使用         │
 *   │   等待方            │   等待方和计数方分离 │   所有线程互等       │
 *   │   屏障动作          │   无内置支持         │   支持 barrierAction │
 *   │   触发时机          │   countDown 到 0     │   所有线程 await 到齐│
 *   └─────────────────────┴──────────────────────┴──────────────────────┘
 *
 * 【AQS 实现原理】
 *   CyclicBarrier 内部使用 ReentrantLock + Condition（不是直接用 AQS）：
 *   - lock.lock() 保护 count 的修改
 *   - condition.await() 让未到齐的线程阻塞等待
 *   - 最后一个到达的线程执行 barrierAction，然后 condition.signalAll() 唤醒所有
 *
 * 【典型应用场景】
 *   1. 分段并行计算：各线程计算子任务，到齐后汇总（本例）
 *   2. 多阶段任务：所有线程完成第一阶段后，再一起进入第二阶段
 *   3. 并行测试：所有线程准备就绪后同时开始
 *
 * 【BrokenBarrierException】
 *   当某个线程在 await() 期间被中断，或屏障被 reset()，其他等待线程会收到此异常。
 *   表示屏障已被破坏，本轮等待无效。
 */
public class CyclicBarrierDemo {

    /** 分段线程数（屏障需要等待的线程数量） */
    private static final int THREAD_COUNT = 4;

    /** 计算目标：1 + 2 + ... + 100 */
    private static final int TOTAL_NUMBER = 100;

    /** 每个线程的分段计算结果（线程安全：每个线程只写自己的槽位） */
    private static int[] partialSums = new int[THREAD_COUNT];

    /** 最终汇总结果 */
    private static int finalSum = 0;

    public static void main(String[] args) {
        /**
         * 创建 CyclicBarrier
         *
         * 参数1：THREAD_COUNT —— 需要 THREAD_COUNT 个线程都到达才触发屏障
         * 参数2：barrierAction —— 所有线程到达后，由最后一个到达的线程执行此任务
         *
         * 【执行顺序】
         *   线程1 await → 阻塞
         *   线程2 await → 阻塞
         *   线程3 await → 阻塞
         *   线程4 await → 最后一个到达 → 执行 barrierAction → 唤醒线程1/2/3
         *   → 所有线程从 await() 返回，继续执行后续代码
         */
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT, () -> {
            // barrierAction：汇总所有分段结果（由最后一个到达的线程执行）
            for (int sum : partialSums) {
                finalSum += sum;
            }
            System.out.println("所有分段计算完成，最终累加和 = " + finalSum);
        });

        // 启动 THREAD_COUNT 个计算线程
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                // ====== 第一阶段：计算分段和 ======
                int start = threadId * (TOTAL_NUMBER / THREAD_COUNT) + 1;
                int end = (threadId + 1) * (TOTAL_NUMBER / THREAD_COUNT);
                // 最后一个线程处理余数（本例 100/4=25 正好整除，此行不会触发）
                if (threadId == THREAD_COUNT - 1) end = TOTAL_NUMBER;

                int sum = 0;
                for (int j = start; j <= end; j++) {
                    sum += j;
                }
                partialSums[threadId] = sum; // 写入自己的槽位（无并发冲突）
                System.out.printf("%s 计算了 [%d, %d] 的和 = %d%n",
                        Thread.currentThread().getName(), start, end, sum);

                // ====== 第二阶段：等待所有线程计算完成 ======
                try {
                    // 到达屏障点，阻塞等待其他线程
                    // AQS 内部：ReentrantLock 保护 count，count-- 后不为 0 则 condition.await()
                    // 最后一个线程：count=0 → 执行 barrierAction → signalAll() 唤醒其他线程
                    barrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }

                // ====== 第三阶段：屏障打开后继续执行 ======
                // 所有线程从 await() 返回后，可以继续执行后续任务
                // CyclicBarrier 的 "Cyclic" 体现在：可以再次调用 barrier.await() 进入下一轮
                System.out.printf("%s 继续执行后续任务%n", Thread.currentThread().getName());
            }, "Calculator-" + i).start();
        }

        // 【CyclicBarrier 可重用性演示】
        // 如果需要再做一轮计算，只需让所有线程再次调用 barrier.await() 即可
        // 屏障会自动重置，无需像 CountDownLatch 那样重新创建对象
    }
}
