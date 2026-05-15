package com.chenxu.deepseek;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ProducerConsumerDemo {

    public static void main(String[] args) {
        // 创建容量为10的阻塞队列
        BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(10);

        // 创建生产者和消费者
        Thread producer = new Thread(new Producer(queue), "Producer");
        Thread consumer = new Thread(new Consumer(queue), "Consumer");

        // 启动线程
        producer.start();
        consumer.start();
    }

    // 生产者
    static class Producer implements Runnable {
        private final BlockingQueue<Integer> queue;

        public Producer(BlockingQueue<Integer> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= 10; i++) {
                    // 生产数据
                    System.out.println(Thread.currentThread().getName() + " 生产: " + i);
                    queue.put(i); // 如果队列满则阻塞
                    Thread.sleep(200); // 模拟生产耗时
                }
                // 发送结束信号
                queue.put(-1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // 消费者
    static class Consumer implements Runnable {
        private final BlockingQueue<Integer> queue;

        public Consumer(BlockingQueue<Integer> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    // 消费数据，最多等待2秒
                    Integer data = queue.poll(2, TimeUnit.SECONDS);
                    if (data == null) {
                        System.out.println(Thread.currentThread().getName() + " 等待超时，退出");
                        break;
                    }
                    if (data == -1) {
                        System.out.println(Thread.currentThread().getName() + " 收到结束信号，退出");
                        break;
                    }
                    System.out.println(Thread.currentThread().getName() + " 消费: " + data);
                    Thread.sleep(500); // 模拟消费耗时
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}