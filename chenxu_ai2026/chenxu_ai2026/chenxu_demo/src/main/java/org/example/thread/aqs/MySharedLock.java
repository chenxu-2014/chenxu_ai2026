package org.example.thread.aqs;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 自定义共享锁（类似 Semaphore）
 * 允许最多 permits 个线程同时进入临界区
 */
public class MySharedLock {

    // 同步器内部类
    private static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 构造方法，设置初始许可数量
         * @param permits 最大并发数
         */
        public Sync(int permits) {
            if (permits <= 0) {
                throw new IllegalArgumentException("permits must be positive");
            }
            setState(permits);  // state 表示当前剩余许可数
        }

        /**
         * 尝试获取共享锁
         * @param arg 本次需要获取的许可数（本例固定为 1）
         * @return 负数表示获取失败，0 或正数表示成功，且返回值表示剩余许可数（用于传播）
         */
        @Override
        protected int tryAcquireShared(int arg) {
            for (;;) {
                int available = getState();
                int remaining = available - arg;
                // 剩余许可不足，直接返回负数（失败）
                if (remaining < 0) {
                    return remaining;
                }
                // CAS 尝试更新剩余许可数
                if (compareAndSetState(available, remaining)) {
                    return remaining;  // 返回剩余许可数（>=0 表示成功）
                }
            }
        }

        /**
         * 尝试释放共享锁
         * @param arg 释放的许可数（固定为 1）
         * @return true 表示释放成功（可能唤醒等待的线程）
         */
        @Override
        protected boolean tryReleaseShared(int arg) {
            for (;;) {
                int current = getState();
                int next = current + arg;
                if (next < 0) { // 溢出保护（本例基本不会溢出，但保留防御）
                    throw new Error("Maximum permit count exceeded");
                }
                if (compareAndSetState(current, next)) {
                    return true;  // 释放成功，由 AQS 决定是否唤醒等待线程
                }
            }
        }
    }

    private final Sync sync;

    public MySharedLock(int permits) {
        sync = new Sync(permits);
    }

    // 获取许可（阻塞）
    public void acquire() {
        sync.acquireShared(1);
    }

    // 释放许可
    public void release() {
        sync.releaseShared(1);
    }

    // 尝试获取许可（非阻塞）
    public boolean tryAcquire() {
        return sync.tryAcquireShared(1) >= 0;
    }

    // 测试：控制最多 3 个线程同时打印
    public static void main(String[] args) {
        MySharedLock lock = new MySharedLock(3); // 最多 3 个并发

        Runnable task = () -> {
            lock.acquire();
            try {
                System.out.println(Thread.currentThread().getName() + " 获得许可，执行任务...");
                Thread.sleep(1000); // 模拟耗时操作
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.release();
                System.out.println(Thread.currentThread().getName() + " 释放许可");
            }
        };

        for (int i = 0; i < 10; i++) {
            new Thread(task, "Thread-" + i).start();
        }
    }
}