package org.example.thread.aqs;

import java.util.concurrent.locks.ReentrantLock;

/**
 * ====================================================================
 * ReentrantLock 实战 —— 银行账户并发转账
 * ====================================================================
 *
 * 【ReentrantLock 是什么】
 *   JDK 提供的可重入互斥锁，是 synchronized 的增强版。
 *   内部基于 AQS 独占模式实现（与 MyExclusiveLock 原理一致，但功能更完善）。
 *
 * 【ReentrantLock vs synchronized】
 *   ┌─────────────────────┬──────────────────────┬──────────────────────┐
 *   │       特性          │   ReentrantLock      │   synchronized       │
 *   ├─────────────────────┼──────────────────────┼──────────────────────┤
 *   │   加锁/解锁         │   手动 lock/unlock   │   自动（进入/退出）  │
 *   │   可重入            │   是                 │   是                 │
 *   │   公平锁            │   支持               │   不支持             │
 *   │   可中断            │   lockInterruptibly  │   不支持             │
 *   │   超时获取          │   tryLock(timeout)   │   不支持             │
 *   │   条件变量          │   多个 Condition     │   只有 wait/notify   │
 *   │   性能              │   高（JDK6+ 差距小） │   高（JDK6+ 优化后）│
 *   └─────────────────────┴──────────────────────┴──────────────────────┘
 *
 * 【AQS 内部原理】
 *   ReentrantLock 使用 AQS 独占模式：
 *   - state = 0 → 锁空闲
 *   - state = N → 锁被持有，重入 N 次
 *   - lock() → AQS.acquire(1) → tryAcquire(1)：CAS 设 state 0→1，失败则入队阻塞
 *   - unlock() → AQS.release(1) → tryRelease(1)：state-1，减到 0 则释放锁并唤醒下一个
 *
 * 【可重入的含义】
 *   同一线程可以多次获取同一把锁（每获取一次 state+1，释放一次 state-1）。
 *   本例中 transfer() 持有锁后调用 withdraw() 和 deposit()，它们内部也会 lock()，
 *   如果锁不可重入，这里就会死锁。
 *
 * 【公平锁 vs 非公平锁】
 *   new ReentrantLock()       → 非公平（默认），新线程可能插队，吞吐量高
 *   new ReentrantLock(true)   → 公平，严格 FIFO，吞吐量低
 *   非公平锁的优势：减少线程切换开销，刚释放锁的线程可能立刻再次获取（CPU 缓存热）
 *
 * 【死锁预防】
 *   多个账户互转时，如果线程1锁A→锁B，线程2锁B→锁A，会死锁。
 *   解决方案：按固定顺序加锁（如按账户 ID 排序），本例为简化未实现。
 */
public class BankAccount {

    /** 可重入锁（默认非公平模式） */
    private final ReentrantLock lock = new ReentrantLock();

    /** 账户余额（由 lock 保护，保证线程安全） */
    private double balance;

    public BankAccount(double initialBalance) {
        this.balance = initialBalance;
    }

    /**
     * 存款
     *
     * 【线程安全】lock.lock() 保证同一时刻只有一个线程执行存款操作
     * 【可重入】deposit 可以被同一线程的其他方法（如 transfer）调用，不会死锁
     *
     * @param amount 存款金额（必须 > 0）
     * @throws IllegalArgumentException 如果金额 <= 0
     */
    public void deposit(double amount) {
        lock.lock();  // AQS: CAS state 0→1，失败则入队 park
        try {
            if (amount <= 0) {
                throw new IllegalArgumentException("存款金额必须大于0");
            }
            balance += amount;
            System.out.printf("%s 存入 %.2f，当前余额：%.2f%n",
                    Thread.currentThread().getName(), amount, balance);
        } finally {
            // 必须在 finally 中释放锁，防止异常导致锁泄漏
            lock.unlock();  // AQS: state-1，减到 0 则唤醒队列中下一个线程
        }
    }

    /**
     * 取款
     *
     * 【线程安全】同 deposit，由 lock 保护
     * 【余额不足处理】打印提示并返回（不抛异常，业务层面处理）
     *
     * @param amount 取款金额（必须 > 0）
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
     * 转账（从当前账户转到目标账户）
     *
     * 【可重入演示】
     *   transfer() → lock.lock()  → state = 1（第一次获取）
     *     → this.withdraw() → lock.lock() → state = 2（重入！同一线程再次获取）
     *     → this.withdraw() → lock.unlock() → state = 1
     *     → target.deposit() → lock.lock() → state = 2（再次重入）
     *     → target.deposit() → lock.unlock() → state = 1
     *   → lock.unlock() → state = 0（完全释放，唤醒下一个等待线程）
     *
     * 【死锁风险提醒】
     *   如果 A→B 和 B→A 同时发生，且各自锁住自己的账户后等待对方，就会死锁。
     *   生产环境中应按账户 ID 排序加锁，或使用 tryLock(timeout) 避免无限等待。
     *
     * @param target 目标账户
     * @param amount 转账金额
     */
    public void transfer(BankAccount target, double amount) {
        lock.lock();  // 锁住自己账户（state: 0→1）
        try {
            if (balance < amount) {
                System.out.printf("%s 转账 %.2f 失败，余额不足%n",
                        Thread.currentThread().getName(), amount);
                return;
            }
            // 扣减自己账户（内部会再次 lock → state: 1→2，可重入）
            this.withdraw(amount);
            // 增加对方账户（注意：这里获取的是 target 的锁，不是自己的）
            target.deposit(amount);
            System.out.printf("%s 转账 %.2f 成功%n", Thread.currentThread().getName(), amount);
        } finally {
            lock.unlock();  // 释放锁（state: 1→0，完全释放）
        }
    }

    /**
     * 查询余额（也需要加锁，保证读到最新值）
     *
     * 【为什么读也要加锁？】
     *   balance 不是 volatile 的，不加锁可能读到 CPU 缓存中的旧值。
     *   加锁保证了 happens-before 关系：写操作对后续读操作可见。
     */
    public double getBalance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 测试并发转账
     *
     * 场景：账户A(1000) 向 账户B(500) 转账，两个用户各转 5 次，每次 100
     * 期望：总金额守恒 = 1000 + 500 = 1500
     *   转账 10 次 × 100 = 1000 从 A 到 B
     *   最终：A = 0, B = 1500
     */
    public static void main(String[] args) throws InterruptedException {
        BankAccount accountA = new BankAccount(1000);
        BankAccount accountB = new BankAccount(500);

        Runnable transferTask = () -> {
            for (int i = 0; i < 5; i++) {
                accountA.transfer(accountB, 100);
                try {
                    Thread.sleep(10); // 模拟业务间隔
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };

        Thread t1 = new Thread(transferTask, "用户1");
        Thread t2 = new Thread(transferTask, "用户2");
        t1.start();
        t2.start();
        t1.join();  // 等待用户1完成
        t2.join();  // 等待用户2完成

        // 验证总金额守恒
        System.out.println("最终账户A余额：" + accountA.getBalance());
        System.out.println("最终账户B余额：" + accountB.getBalance());
        System.out.println("总金额：" + (accountA.getBalance() + accountB.getBalance()));
    }
}
