package org.example.thread.aqs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
//场景1 主线程等待多个子线程任务完成
public class CountDownLatchDemo {

    public static void main(String[] args) throws InterruptedException {
        int taskCount = 7;
        CountDownLatch latch = new CountDownLatch(taskCount);

        for (int i = 1; i <= taskCount; i++) {
            final int taskId = i;
            new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + " 开始执行任务 " + taskId);
                    TimeUnit.SECONDS.sleep((long) (Math.random() * 3)); // 模拟耗时
                    System.out.println(Thread.currentThread().getName() + " 完成任务 " + taskId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // 每个任务完成时计数减一
                }
            }, "工作线程-" + i).start();
        }

        System.out.println("主线程等待所有子任务完成...");
        latch.await(); // 阻塞直到计数器为0
        System.out.println("所有子任务已完成，主线程继续执行");
    }
}
