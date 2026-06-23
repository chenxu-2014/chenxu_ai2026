package org.example.thread_cx;

public class JoinExample {
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            try {
                System.out.println("线程t1开始执行");
                Thread.sleep(2000);
                System.out.println("线程t1执行结束");
            } catch (InterruptedException e) {
                System.out.println("线程t1InterruptedException============");
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {

            try {
                System.out.println("线程t2开始执行");
                Thread.sleep(1000);
                System.out.println("线程t2执行结束");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        });

        t1.start();
        t2.start();

        System.out.println("主线程等待t1执行完毕");
        t1.join(); //如果t1.join() 则是等待t1执行完毕 在执行别的。如果是t2.join() 则是t2执行结束再执行别的。也可以说在join的过程中是阻塞的 只能等待他执行结束。
        System.out.println("t1执行完毕，主线程继续");

        //t1.join();  // 等待t2执行完毕，确保线程全部结束

        System.out.println("所有线程执行结束，主线程退出");
    }
}
