package org.example.thread.aqs;

import java.util.concurrent.CountDownLatch;

/**
 * ====================================================================
 * CountDownLatch 进阶用法 —— "发令枪"模式（同时起步）
 * ====================================================================
 *
 * 【与 CountDownLatchDemo 的区别】
 *   CountDownLatchDemo：主线程等子线程（一次性门闩，countDown 到 0 才放行）
 *   本类：两个 CountDownLatch 组合使用
 *     - startSignal（计数=1）：所有线程先 await，主线程一声令下 countDown，全部同时启动
 *     - doneSignal（计数=N）：所有线程完成后，主线程才继续
 *
 * 【设计模式】
 *   这是 CountDownLatch 的经典 "起跑门" 模式：
 *   所有线程就绪 → 等待发令 → 同时起步 → 各自执行 → 全部完成后汇总
 *
 * 【AQS 原理】
 *   CountDownLatch 内部使用 AQS 共享模式：
 *   - state = count（倒计数器初始值）
 *   - countDown() → tryReleaseShared() → CAS 将 state - 1
 *   - await() → tryAcquireShared() → 检查 state 是否为 0
 *     - 不为 0 → 入队阻塞
 *     - 为 0 → 直接通过（门闩已打开）
 *   - 当 state 从 1 变为 0 时，AQS 唤醒所有 await 中的线程（共享传播）
 *
 * 【CountDownLatch vs CyclicBarrier】
 *   CountDownLatch：一次性，count 到 0 后不能重置
 *   CyclicBarrier：可重用，所有线程到达后自动重置
 *   CountDownLatch 更灵活（可以用作"发令枪"等非对称场景）
 */
public class CountDownLatchStartTogether {

    public static void main(String[] args) throws InterruptedException {
        int threadNum = 5;

        // 起跑信号：计数=1，所有 Runner 线程 await 这个信号
        // 主线程调用 startSignal.countDown() 后，所有 Runner 同时被唤醒
        CountDownLatch startSignal = new CountDownLatch(1);

        // 完成信号：计数=threadNum，每个 Runner 执行完后 countDown
        // 主线程 await 这个信号，等所有 Runner 都完成后才继续
        CountDownLatch doneSignal = new CountDownLatch(threadNum);

        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                try {
                    // ====== 第一阶段：等待发令 ======
                    // 所有线程在此阻塞，等待主线程发出 startSignal
                    // AQS 内部：tryAcquireShared 发现 state=1（未到0），线程入队 park
                    startSignal.await();

                    // ====== 第二阶段：执行任务 ======
                    System.out.println(Thread.currentThread().getName() + " 开始运行");
                    Thread.sleep(1000); // 模拟业务处理

                    // ====== 第三阶段：标记完成 ======
                    // CAS 将 doneSignal 的 state - 1
                    // 最后一个线程 countDown 后 state=0，唤醒主线程
                    doneSignal.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Runner-" + i).start();
        }

        // 主线程：模拟准备工作
        System.out.println("准备... 3 2 1 跑！");
        Thread.sleep(2000); // 模拟准备时间（实际场景可能是初始化资源等）

        // 发出起跑信号：state 从 1 → 0，AQS 唤醒所有等待在 startSignal 上的 Runner 线程
        // 所有 Runner 几乎同时从 await() 返回，实现"同时起步"效果
        startSignal.countDown();

        // 主线程等待所有 Runner 完成：阻塞直到 doneSignal 的 state 降为 0
        doneSignal.await();
        System.out.println("所有选手完成比赛");
    }
}