package org.example.thread_cx;

public class InterruptExample {
//    public static void main(String[] args) throws InterruptedException {
//        Thread worker = new Thread(() -> {
//            try {
//                System.out.println("while worker.currentThread.isInterrupted()+"+Thread.currentThread().isInterrupted());
//
//                while (!Thread.currentThread().isInterrupted()) {
//                    System.out.println("Working..");
//                    Thread.sleep(1000); // 模拟工作
//                }
//                System.out.println("while worker.currentThread.isInterrupted()+"+Thread.currentThread().isInterrupted());
//
//                System.out.println("Thread is interrupted, exiting..");
//            } catch (InterruptedException e) {
//                // 捕获InterruptedException，通常表示线程应该退出
//                System.out.println("Thread was interrupted during sleep");
//                System.out.println("worker.currentThread.isInterrupted()+"+Thread.currentThread().isInterrupted());
//
//                // 清除中断状态（如果需要的话）
//                Thread.interrupted(); // 这会清除中断状态
//                System.out.println("worker.currentThread.isInterrupted()+"+Thread.currentThread().isInterrupted());
////                System.out.println("Thread.interrupted()"+Thread.interrupted());
//            }
//        });
//
//        worker.start();
//
//        // 主线程休眠3秒，然后中断worker线程
//        Thread.sleep(3000);
//        worker.interrupt();
//
//    }

//
    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            int i=0;
            while (true) {
                boolean interrupted = Thread.currentThread().isInterrupted();
                if (interrupted){

                    System.out.println("被打断了, 退出循环");
                    System.out.println("worker.currentThread.isInterrupted()+"+Thread.currentThread().isInterrupted());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                    }
                    //Thread.interrupted();
                    System.out.println("worker.currentThread.isInterrupted()+"+Thread.currentThread().isInterrupted());
                    i++;
                    System.out.println(i++);
                    break;
//                    if(i==2){
//                        System.out.println("i==========="+i); break;}

                }else {
                   System.out.println("t1.currentThread.isInterrupted()+"+Thread.currentThread().isInterrupted());
                   System.out.println("沒有打断正常执行--");
                }
            }
        }, "t1");
        t1.setName("线程名:sss");
        t1.start();
        t1.sleep(1);
        System.out.println("=============="+t1.getName());
        System.out.println("开始interrupt");
        t1.interrupt();

    }
}
