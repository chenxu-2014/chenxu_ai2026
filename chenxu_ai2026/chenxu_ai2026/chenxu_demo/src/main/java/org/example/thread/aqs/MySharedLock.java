package org.example.thread.aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * ====================================================================
 * 自定义共享锁（类似 Semaphore） —— 基于 AQS 共享模式实现
 * ====================================================================
 *
 * 【AQS 共享模式 vs 独占模式】
 *   独占模式（MyExclusiveLock）：同一时刻只有 1 个线程能持有锁
 *   共享模式（本类）：同一时刻可有 N 个线程同时持有锁
 *
 * 【本类的 state 语义】
 *   state = 剩余许可数（permits）
 *   每次 acquire → state - 1（许可消耗）
 *   每次 release → state + 1（许可归还）
 *   state = 0 时，后续 acquire 将阻塞等待
 *
 * 【与 Semaphore 的关系】
 *   java.util.concurrent.Semaphore 内部就是用 AQS 共享模式实现的，
 *   本类是最简化版本，帮助理解 Semaphore 的底层原理。
 *
 * 【共享模式的"传播"机制】
 *   tryAcquireShared 的返回值有特殊含义：
 *     - 负数 → 获取失败，线程入队阻塞
 *     - 0 → 获取成功，但不传播（后续等待线程不会被唤醒）
 *     - 正数 → 获取成功，且传播（AQS 会唤醒队列中后续的共享等待线程）
 *   这就是"共享"的关键：一个线程释放后，可以连续唤醒多个等待的共享线程。
 *
 * 【典型应用场景】
 *   - 数据库连接池（限制最大连接数）
 *   - 限流器（限制并发请求数）
 *   - 资源池（如线程池、对象池）
 */
public class MySharedLock {

    /**
     * ================================================================
     * 同步器内部类 —— 共享模式的核心实现
     * ================================================================
     *
     * AQS 共享模式的 acquire 调用链：
     *   acquireShared(1) → tryAcquireShared(1)
     *     → 返回 >= 0 → 获取成功
     *     → 返回 < 0 → doAcquireShared() 入队 → 自旋+park 阻塞
     *       → 前驱释放时 → setHeadAndPropagate() → unpark 唤醒
     *
     * AQS 共享模式的 release 调用链：
     *   releaseShared(1) → tryReleaseShared(1)
     *     → 返回 true → doReleaseShared() 唤醒队列中等待的共享线程
     *     → 被唤醒的线程再次 tryAcquireShared → 成功则继续传播唤醒下一个
     */
    private static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 构造方法，设置初始许可数量
         *
         * @param permits 最大并发数（必须 > 0）
         * @throws IllegalArgumentException 如果 permits <= 0
         */
        public Sync(int permits) {
            if (permits <= 0) {
                throw new IllegalArgumentException("permits must be positive");
            }
            // state 表示当前剩余许可数（初始值 = permits）
            setState(permits);
        }

        /**
         * 尝试获取共享锁（CAS 自旋方式）
         *
         * 【返回值语义（AQS 规范）】
         *   - 负数 → 获取失败，AQS 将线程加入等待队列并阻塞
         *   - 0 → 获取成功，但不传播唤醒（后续共享线程继续等待）
         *   - 正数 → 获取成功，且传播唤醒（AQS 会唤醒后续共享等待线程）
         *
         * 【为什么用 for(;;) 自旋？】
         *   CAS 可能因并发竞争而失败（多个线程同时 compareAndSetState），
         *   失败后需要重试，所以用无限循环不断尝试，直到成功或发现许可不足。
         *
         * @param arg 本次需要获取的许可数（本例固定为 1）
         * @return 剩余许可数（>=0 表示成功，<0 表示失败）
         */
        @Override
        protected int tryAcquireShared(int arg) {
            for (;;) {
                int available = getState();       // 当前剩余许可数
                int remaining = available - arg;  // 获取后的剩余数
                // 剩余不足 → 返回负数，AQS 会阻塞当前线程
                if (remaining < 0) {
                    return remaining;
                }
                // CAS 原子更新：available → remaining
                // 成功 → 返回 remaining（>=0，获取成功）
                // 失败 → 其他线程抢先修改了 state，重新循环再试
                if (compareAndSetState(available, remaining)) {
                    return remaining;
                }
            }
        }

        /**
         * 尝试释放共享锁（CAS 自旋方式）
         *
         * 【释放后 AQS 的行为】
         *   返回 true 后，AQS 调用 doReleaseShared()：
         *   1. 从等待队列头部取出一个共享模式的等待线程
         *   2. unpark 唤醒它
         *   3. 被唤醒的线程再次 tryAcquireShared → 成功后可能继续传播唤醒下一个
         *   这就是"共享传播链"：一次释放可能连续唤醒多个等待线程
         *
         * @param arg 释放的许可数（固定为 1）
         * @return true 表示释放成功
         * @throws Error 如果许可数溢出（理论上不会发生）
         */
        @Override
        protected boolean tryReleaseShared(int arg) {
            for (;;) {
                int current = getState();        // 当前许可数
                int next = current + arg;        // 归还后的许可数
                // 溢出保护：int 溢出会变负数
                if (next < 0) {
                    throw new Error("Maximum permit count exceeded");
                }
                // CAS 原子更新：current → next
                if (compareAndSetState(current, next)) {
                    return true;  // 释放成功，AQS 将唤醒等待线程
                }
                // CAS 失败 → 重试
            }
        }
    }

    /** 持有同步器实例 */
    private final Sync sync;

    /**
     * 创建共享锁
     *
     * @param permits 最大并发许可数（同时允许多少个线程持有锁）
     */
    public MySharedLock(int permits) {
        sync = new Sync(permits);
    }

    /**
     * 获取许可（阻塞式）
     *
     * 【调用链】acquire() → AQS.acquireShared(1) → tryAcquireShared(1)
     *   - 返回 >= 0 → 直接返回（获取成功）
     *   - 返回 < 0 → doAcquireShared() 入队阻塞，等待被唤醒
     */
    public void acquire() {
        sync.acquireShared(1);
    }

    /**
     * 释放许可
     *
     * 【调用链】release() → AQS.releaseShared(1) → tryReleaseShared(1)
     *   - 返回 true → doReleaseShared() 唤醒等待线程（可能传播唤醒多个）
     */
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * 尝试获取许可（非阻塞，立即返回）
     *
     * @return true 表示获取成功，false 表示许可不足（不会阻塞）
     */
    public boolean tryAcquire() {
        return sync.tryAcquireShared(1) >= 0;
    }

    /**
     * 测试入口：10 个线程竞争 3 个许可，同时最多 3 个线程执行任务
     * 观察输出：任意时刻最多 3 个线程同时持有许可
     */
    public static void main(String[] args) {
        MySharedLock lock = new MySharedLock(3); // 最多 3 个并发

        Runnable task = () -> {
            lock.acquire();  // 获取许可（可能阻塞）
            try {
                System.out.println(Thread.currentThread().getName() + " 获得许可，执行任务...");
                Thread.sleep(1000); // 模拟耗时操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.release();  // 归还许可（可能唤醒其他等待线程）
                System.out.println(Thread.currentThread().getName() + " 释放许可");
            }
        };

        for (int i = 0; i < 10; i++) {
            new Thread(task, "Thread-" + i).start();
        }
    }
}