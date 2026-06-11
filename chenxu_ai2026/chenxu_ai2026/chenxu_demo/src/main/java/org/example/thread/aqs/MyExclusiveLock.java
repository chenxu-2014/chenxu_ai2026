package org.example.thread.aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * ====================================================================
 * 自定义独占锁（不可重入） —— 基于 AQS 的最简实现
 * ====================================================================
 *
 * 【AQS 核心原理】
 * AQS (AbstractQueuedSynchronizer) 是 Java 并发包的基石，ReentrantLock、CountDownLatch、
 * Semaphore、ReadWriteLock 等同步器全部基于 AQS 实现。
 *
 * AQS 维护两个核心东西：
 *   1. volatile int state  —— 同步状态（含义由子类定义）
 *   2. CLH 变体的双向队列  —— 存放获取锁失败后被阻塞的线程
 *
 * 子类只需重写 tryAcquire / tryRelease（独占模式）或 tryAcquireShared / tryReleaseShared（共享模式），
 * 即可实现各种同步器。AQS 负责排队、阻塞、唤醒等"脏活"。
 *
 * 【本类的 state 语义】
 *   state = 0  → 锁空闲
 *   state = 1  → 锁被某线程持有
 *
 * 【独占模式 vs 共享模式】
 *   独占模式：同一时刻只有一个线程能持有锁（如 ReentrantLock）
 *   共享模式：同一时刻可有多个线程持有锁（如 Semaphore、ReadWriteLock 的读锁）
 *
 * 【本锁特点】
 *   - 不可重入：同一线程再次 lock() 会死锁（因为 tryAcquire 不判断 owner）
 *   - 不支持公平/非公平切换（直接 CAS 抢锁，等价于非公平）
 *   - 目的：用最少代码展示 AQS 独占模式的核心骨架
 */
public class MyExclusiveLock {

    /**
     * ================================================================
     * 同步器内部类 —— 真正实现锁语义的核心
     * ================================================================
     * 继承 AQS，重写 tryAcquire / tryRelease / isHeldExclusively 三个模板方法。
     * AQS 的 acquire() 方法调用链：
     *   lock() → sync.acquire(1)
     *     → tryAcquire(1)  // 子类实现，尝试获取锁
     *     → 如果失败：addWaiter() 入队 → acquireQueued() 自旋+park 阻塞
     *     → 如果成功：直接返回
     *
     * AQS 的 release() 方法调用链：
     *   unlock() → sync.release(1)
     *     → tryRelease(1)  // 子类实现，尝试释放锁
     *     → 如果成功：unparkSuccessor() 唤醒队列中下一个等待线程
     */
    private static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 尝试获取锁（独占模式）
         *
         * 【AQS 调用时机】当外部调用 lock() / acquire() 时，AQS 模板方法会调用此方法
         *
         * 【实现逻辑】
         *   1. CAS 将 state 从 0 改为 1（原子操作，保证线程安全）
         *   2. 成功 → 设置独占线程为当前线程，返回 true（获取锁成功）
         *   3. 失败 → 返回 false（获取锁失败，AQS 会将当前线程入队并阻塞）
         *
         * 【为什么不用 synchronized？】
         *   compareAndSetState() 底层是 Unsafe.compareAndSwapInt()，是 CPU 级别的原子指令，
         *   比 synchronized 更轻量，适合"低竞争"场景的快速路径。
         *
         * @param arg 获取次数（本例固定为 1，AQS 模板方法传入）
         * @return true 表示获取成功，false 表示失败
         */
        @Override
        protected boolean tryAcquire(int arg) {
            // CAS：期望 state 从 0 → 1，成功则拿到锁
            if (compareAndSetState(0, 1)) {
                // 记录当前持有锁的线程（用于 isHeldExclusively 判断和 Condition 支持）
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
            // 【不可重入的关键】这里没有判断 "当前线程是否已经是 owner"，
            // 如果是可重入锁（如 ReentrantLock），需要：
            //   if (current == getExclusiveOwnerThread()) {
            //       setState(state + 1);  // 重入计数 +1
            //       return true;
            //   }
        }

        /**
         * 尝试释放锁（独占模式）
         *
         * 【AQS 调用时机】当外部调用 unlock() / release() 时，AQS 模板方法会调用此方法
         *
         * 【实现逻辑】
         *   1. 检查 state 是否为 0（为 0 说明锁没被持有，释放不合法）
         *   2. 清除 owner 线程记录
         *   3. 将 state 设回 0
         *   4. 返回 true → AQS 会唤醒队列中下一个等待的线程
         *
         * 【为什么不需要 CAS？】
         *   独占模式下，只有持有锁的线程才能调用 release()，不存在并发修改 state 的情况，
         *   所以直接 setState(0) 即可，无需 CAS。
         *
         * @param arg 释放次数（固定为 1）
         * @return true 表示释放成功
         * @throws IllegalMonitorStateException 如果锁未被持有却尝试释放
         */
        @Override
        protected boolean tryRelease(int arg) {
            // 防御性检查：锁已经处于空闲状态，不能重复释放
            if (getState() == 0) {
                throw new IllegalMonitorStateException("锁已经释放，不能重复释放");
            }
            // 清除 owner 记录（必须在 setState 之前，否则 isHeldExclusively 可能误判）
            setExclusiveOwnerThread(null);
            // state 归零，表示锁已释放
            setState(0);
            return true;
            // 【可重入锁的释放】如果是 ReentrantLock，需要：
            //   setState(state - 1);
            //   if (state == 0) { setExclusiveOwnerThread(null); return true; }
            //   return false;  // 还没完全释放（重入层数 > 0）
        }

        /**
         * 判断锁是否被当前线程独占持有
         *
         * 【使用场景】
         *   1. Condition.await() 内部需要调用此方法校验
         *   2. AQS 的某些模板方法（如 release() 的安全检查）会用到
         *
         * @return true 表示当前线程持有此锁
         */
        @Override
        protected boolean isHeldExclusively() {
            // state=1 且 owner 是当前线程 → 当前线程持有锁
            return getState() == 1 && getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    /** 持有同步器实例（组合模式，将 AQS 能力封装为自定义锁） */
    private final Sync sync = new Sync();

    /**
     * 加锁（阻塞式）
     *
     * 【调用链】lock() → AQS.acquire(1) → tryAcquire(1)
     *   - tryAcquire 成功 → 直接返回
     *   - tryAcquire 失败 → AQS 将线程加入 CLH 队列 → park 阻塞
     *     → 前驱线程释放锁时 unpark 唤醒 → 重新 tryAcquire
     */
    public void lock() {
        sync.acquire(1);
    }

    /**
     * 解锁
     *
     * 【调用链】unlock() → AQS.release(1) → tryRelease(1)
     *   - tryRelease 成功 → AQS 唤醒队列中下一个等待线程
     */
    public void unlock() {
        sync.release(1);
    }

    /**
     * 尝试加锁（非阻塞，立即返回）
     *
     * @return true 表示获取成功，false 表示锁已被占用（不会阻塞，不会入队）
     */
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    /**
     * 测试入口：两个线程各累加 1000 次，用自定义锁保证线程安全
     * 期望结果：最终计数 = 2000
     */
    public static void main(String[] args) throws InterruptedException {
        MyExclusiveLock lock = new MyExclusiveLock();
        Counter counter = new Counter();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                lock.lock();
                try {
                    counter.increment();
                } finally {
                    // 必须在 finally 中释放锁，防止异常导致死锁
                    lock.unlock();
                }
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();  // 等待 t1 完成
        t2.join();  // 等待 t2 完成

        System.out.println("最终计数: " + counter.get()); // 期望 2000
    }

    /**
     * 非线程安全的计数器（依赖外部锁保护）
     */
    static class Counter {
        private int count = 0;
        public void increment() { count++; }
        public int get() { return count; }
    }
}