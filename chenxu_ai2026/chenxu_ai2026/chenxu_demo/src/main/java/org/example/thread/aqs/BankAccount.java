package org.example.thread.aqs;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 银行账户类（使用 ReentrantLock 保证线程安全）
 */
//场景一：ReentrantLock —— 解决银行账户并发转账
//        适用问题：多线程对同一账户进行存款/取款/转账，需要保证金额操作的原子性，且支持重入（同一个线程多次获取锁）。
//        AQS 实现：独占模式 + 可重入 + 等待队列。
public class BankAccount {
    private final ReentrantLock lock = new ReentrantLock(); // 可重入锁
    private double balance;                                 // 账户余额

    public BankAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    /**
     * 存款（可被同一线程多次调用）
     */
    public void deposit(double amount) {
        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("存款金额必须大于0");
            }
            balance += amount;
            System.out.printf("%s 存入 %.2f，当前余额：%.2f%n",
                    Thread.currentThread().getName(), amount, balance);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 取款
     */
    public void withdraw(double amount) {
        lock.lock();
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("取款金额必须大于0");
            }
            if (balance < amount) {
                System.out.printf("%s 取款 %.2f 失败，余额不足！当前余额：%.2f%n",
                        Thread.currentThread().getName(), amount, balance);
                return;
            }
            balance -= amount;
            System.out.printf("%s 取出 %.2f，当前余额：%.2f%n",
                    Thread.currentThread().getName(), amount, balance);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 转账（演示锁的重入：transfer 调用了 withdraw 和 deposit，会再次获取同一把锁）
     */
    public void transfer(BankAccount target, double amount) {
        lock.lock();          // 先锁住自己账户
        try {
            if (balance < amount) {
                System.out.printf("%s 转账 %.2f 失败，余额不足%n",
                        Thread.currentThread().getName(), amount);
                return;
            }
            // 扣减自己账户（会再次获取当前账户的锁——重入）
            this.withdraw(amount);
            // 增加对方账户（注意：为避免死锁，一般需按顺序加锁，此处简化）
            target.deposit(amount);
            System.out.printf("%s 转账 %.2f 成功%n", Thread.currentThread().getName(), amount);
        } finally {
            lock.unlock();
        }
    }

    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    // 测试并发转账
    public static void main(String[] args) throws InterruptedException {
        BankAccount accountA = new BankAccount(1000);
        BankAccount accountB = new BankAccount(500);

        Runnable transferTask = () -> {
            for (int i = 0; i < 5; i++) {
                accountA.transfer(accountB, 100);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        Thread t1 = new Thread(transferTask, "用户1");
        Thread t2 = new Thread(transferTask, "用户2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("最终账户A余额：" + accountA.getBalance());
        System.out.println("最终账户B余额：" + accountB.getBalance());
    }
}
