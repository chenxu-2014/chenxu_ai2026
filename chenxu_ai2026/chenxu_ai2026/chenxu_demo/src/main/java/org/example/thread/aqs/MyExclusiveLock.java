package org.example.thread.aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 自定义独占锁（不可重入）
 * 使用 AQS 实现，state=0 表示未锁定，state=1 表示已锁定
 */
public class MyExclusiveLock {

    // 同步器内部类
    private static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 尝试获取锁（独占模式）
         * @param arg 获取次数（本例固定为 1）
         * @return true 表示获取成功，false 表示失败
         */
        @Override
        protected boolean tryAcquire(int arg) {
            // 期望从 0 改成 1，如果成功则表示获得锁
            if (compareAndSetState(0, 1)) {
                // 设置当前拥有锁的线程（用于重入判断，本例不重入，但记录一下有助于调试）
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * 尝试释放锁（独占模式）
         * @param arg 释放次数（固定为 1）
         * @return true 表示释放成功
         */
        @Override
        protected boolean tryRelease(int arg) {
            // 必须由持有锁的线程调用
            if (getState() == 0) {
                throw new IllegalMonitorStateException("锁已经释放，不能重复释放");
            }
            // 清除拥有者线程
            setExclusiveOwnerThread(null);
            // 将 state 设回 0（这里不需要 CAS，因为独占模式下只有当前线程才会修改 state）
            setState(0);
            return true;
        }

        /**
         * 是否被当前线程独占（用于 Condition 时必要，本例未使用 Condition）
         */
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1 && getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    // 聚合同步器对象
    private final Sync sync = new Sync();

    // 对外暴露的加锁方法
    public void lock() {
        // AQS 的模板方法：acquire 内部会调用 tryAcquire，失败则将线程加入队列并阻塞
        sync.acquire(1);
    }

    // 对外暴露的解锁方法
    public void unlock() {
        sync.release(1);
    }

    // 尝试加锁（非阻塞）
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    // 测试：用自定义锁实现线程安全的计数
    public static void main(String[] args) throws InterruptedException {
        MyExclusiveLock lock = new MyExclusiveLock();
        Counter counter = new Counter();

        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                lock.lock();
                try {
                    counter.increment();
                } finally {
                    lock.unlock();
                }
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终计数: " + counter.get()); // 期望 2000
    }

    static class Counter {
        private int count = 0;
        public void increment() { count++; }
        public int get() { return count; }
    }
}