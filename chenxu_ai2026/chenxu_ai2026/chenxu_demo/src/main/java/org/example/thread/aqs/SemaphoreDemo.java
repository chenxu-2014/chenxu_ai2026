package org.example.thread.aqs;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * ====================================================================
 * Semaphore（信号量）使用演示
 * ====================================================================
 *
 * 【Semaphore 是什么】
 *   Semaphore 是 JDK 提供的共享锁实现，内部基于 AQS 共享模式（与 MySharedLock 原理一致）。
 *   它维护一组"许可"（permits），线程必须先 acquire 获取许可才能执行，执行完 release 归还许可。
 *
 * 【核心 API】
 *   acquire()          —— 获取许可（阻塞式，许可不足时线程会被挂起）
 *   acquire(n)         —— 获取 n 个许可
 *   tryAcquire()       —— 尝试获取许可（非阻塞，返回 boolean）
 *   tryAcquire(timeout) —— 尝试获取许可，最多等待 timeout 时间
 *   release()          —— 归还许可（会唤醒等待的线程）
 *   availablePermits() —— 查询当前剩余许可数
 *
 * 【公平 vs 非公平】
 *   new Semaphore(permits)            —— 非公平（默认），新线程可能插队
 *   new Semaphore(permits, true)      —— 公平，按 FIFO 顺序获取许可
 *   公平模式性能较低（需要维护队列顺序），非公平模式吞吐量更高
 *
 * 【典型应用场景】
 *   1. 数据库连接池：限制最大连接数
 *   2. 限流：限制并发请求数（如 QPS 限流）
 *   3. 资源池：控制对有限资源的并发访问
 *   4. 生产者-消费者：控制缓冲区访问
 *
 * 【AQS 内部原理】
 *   Semaphore 的 state = 剩余许可数
 *   acquire() → AQS.acquireShared(1) → tryAcquireShared(1) → CAS state-1
 *   release() → AQS.releaseShared(1) → tryReleaseShared(1) → CAS state+1 → 唤醒等待线程
 */
public class SemaphoreDemo {

    /**
     * 信号量实例：最多允许 1 个线程同时访问资源
     *
     * 注意：注释说"最多允许 3 个"，但实际 permits=1，等价于互斥锁。
     * 如果要演示并发效果，应改为 new Semaphore(3)
     */
    private static final Semaphore semaphore = new Semaphore(1);

    public static void main(String[] args) {
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    // 获取许可（阻塞式）
                    // permits=1 时，同一时刻只有 1 个线程能通过，其余阻塞等待
                    // AQS 内部：CAS 将 state 从 1→0，成功则放行；失败则入队 park
                    semaphore.acquire();

                    System.out.println(Thread.currentThread().getName() + " 获得了许可，开始执行任务 " + taskId);
                    TimeUnit.SECONDS.sleep(2); // 模拟任务执行耗时

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 归还许可（必须在 finally 中，防止异常导致许可泄漏）
                    // AQS 内部：CAS 将 state 从 0→1，然后唤醒队列中等待的线程
                    semaphore.release();
                    System.out.println(Thread.currentThread().getName() + " 释放了许可，剩余许可：" + semaphore.availablePermits());
                }
            }, "Thread-" + i).start();
        }
    }
}
