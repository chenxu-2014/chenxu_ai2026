package org.example.thread;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

//线程池运行：当任务进来 创建核心线程-核心线程满了-任务进阻塞队列-阻塞队列任务满了-创建最大线程数-最大线程数满了触发拒绝策略
//拒绝策略有四种，或者自定义拒绝策略：实现RejectedExecutionHandler类（可以将任务发到kafka或者数据库等后续再处理）
// 1.abortpolicy 抛异常
// 2.callerrunspolicy 哪里来的去哪里
// 3.discardpolicy 直接丢弃
// 4.discardoldestpolicy 丢弃最老的，尝试提交当前的任务
public class threadPool {
    public static void main(String[] args) {
        // 核心线程数1，最大线程数2，队列容量1（满后触发拒绝）
        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                1, 2, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1),
                new ThreadPoolExecutor.CallerRunsPolicy()//主线程自己跑这个任务。好处是任务不会丢失，同时能降低新任务的提交速度 任务太多会影响主流程
                //new CustomRejectedPolicy() 自定义拒绝策略，实现RejectedExecutionHandler类 将任务发到kafka或者数据库等后续再处理
        );

        // 提交5个任务，模拟触发拒绝
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            pool.execute(() -> {
                System.out.println(Thread.currentThread().getName() + " 执行任务 " + taskId);
                try { Thread.sleep(100); } catch (InterruptedException e) {}
            });
        }
        pool.shutdown();
    }


    /**
     * 自定义拒绝策略：记录到日志 + 放入MQ（演示打印）
     */
    static class CustomRejectedPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            // 可以获取任务信息，这里简单打印
            System.out.println("[自定义拒绝] 任务被拒绝，时间: " + System.currentTimeMillis() +
                    ", 活跃线程数: " + executor.getActiveCount() +
                    ", 队列大小: " + executor.getQueue().size());
            // 实际生产：发送到死信队列或记录DB
            // mqTemplate.send("dead-letter-queue", taskData);
            // 也可以转为异步重试或告警
        }
    }
}
