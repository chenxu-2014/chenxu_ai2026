package org.example.thread.aqs;

import java.util.concurrent.CountDownLatch;

// 场景2 发令枪效果
public class CountDownLatchStartTogether {

    public static void main(String[] args) throws InterruptedException {
        int threadNum = 5;
        CountDownLatch startSignal = new CountDownLatch(1);   // 起跑信号
        CountDownLatch doneSignal = new CountDownLatch(threadNum); // 完成信号

        for (int i = 0; i < threadNum; i++) {
            new Thread(() -> {
                try {
                    startSignal.await();   // 阻塞，等待起跑信号
                    System.out.println(Thread.currentThread().getName() + " 开始运行");
                    // 模拟业务处理
                    Thread.sleep(1000);
                    doneSignal.countDown(); // 标记自己完成
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Runner-" + i).start();
        }

        System.out.println("准备... 3 2 1 跑！");
        Thread.sleep(2000); // 模拟准备时间
        startSignal.countDown(); // 发出起跑信号，所有等待的 Runner 同时启动

        doneSignal.await(); // 等待所有 Runner 完成
        System.out.println("所有选手完成比赛");
    }
}