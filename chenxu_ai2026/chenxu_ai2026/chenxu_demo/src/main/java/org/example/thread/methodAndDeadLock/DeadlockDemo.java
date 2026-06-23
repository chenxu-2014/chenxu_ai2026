package org.example.thread_cx;

public class DeadlockDemo {
        private static final Object resource1 = new Object();
        private static final Object resource2 = new Object();

        public static void main(String[] args) throws InterruptedException {
            Thread threadA = new Thread(() -> {
                synchronized (resource1) {
                    System.out.println("Thread A acquired resource1");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (resource2) {
                        System.out.println("Thread A acquired resource2");
                    }
                }
            });

            Thread threadB = new Thread(() -> {
                synchronized (resource2) {
                    System.out.println("Thread B acquired resource2");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    synchronized (resource1) {
                        System.out.println("Thread B acquired resource1");
                    }
                }
            });
            threadA.start();
            threadB.start();
        }

}
