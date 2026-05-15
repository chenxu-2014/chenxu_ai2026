package com.chenxu.thread_cx;

public class sleepJoinExample {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("Worker is working: " + i);
                try {
                    // 模拟耗时任务
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // 处理中断异常
                }
            }
            System.out.println("Worker finished");
        });
        Thread worker1 = new Thread(() -> {
            for (int i = 1; i <= 5; i++) {
                System.out.println("Worker1 is working: " + i);
                try {
                    // 模拟耗时任务
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // 处理中断异常
                }
            }
            System.out.println("Worker1 finished");
        });

        worker.start();worker.join();
        worker1.start();
        // 主线程等待worker线程完成


        System.out.println("Main thread continues after worker thread has finished");
    }
}
