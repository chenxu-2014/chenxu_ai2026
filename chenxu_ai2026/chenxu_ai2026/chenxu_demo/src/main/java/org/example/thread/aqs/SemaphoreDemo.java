package org.example.thread.aqs;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class SemaphoreDemo {

    // 模拟一个最多同时允许 3 个线程访问的资源
    private static final Semaphore semaphore = new Semaphore(1);

    public static void main(String[] args) {
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    // 获取许可（如果不够则阻塞）
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName() + " 获得了许可，开始执行任务 " + taskId);
                    // 模拟任务执行
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // 释放许可
                    semaphore.release();
                    System.out.println(Thread.currentThread().getName() + " 释放了许可，剩余许可：" + semaphore.availablePermits());
                }
            }, "Thread-" + i).start();
        }
    }
}
