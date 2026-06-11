package org.example.thread.aqs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ====================================================================
 * ReentrantReadWriteLock 实战 —— 高性能线程安全缓存
 * ====================================================================
 *
 * 【读写锁的核心思想】
 *   读操作（共享）：多个线程可以同时读，互不阻塞
 *   写操作（独占）：同一时刻只能有一个线程写，且写时不能读
 *   适用于"读多写少"场景，比纯互斥锁（ReentrantLock）吞吐量高很多
 *
 * 【读写锁的互斥关系】
 *   ┌──────┬───────┬───────┐
 *   │      │  读锁 │  写锁 │
 *   ├──────┼───────┼───────┤
 *   │ 读锁 │  兼容 │  互斥 │
 *   ├──────┼───────┼───────┤
 *   │ 写锁 │  互斥 │  互斥 │
 *   └──────┴───────┴───────┘
 *   - 读读兼容：多个读线程可同时持有读锁
 *   - 读写互斥：有线程持有读锁时，写锁必须等待
 *   - 写写互斥：写锁是独占的
 *
 * 【AQS 内部实现】
 *   ReentrantReadWriteLock 巧妙地用一个 int (state) 同时表示读锁和写锁状态：
 *   - 高 16 位：读锁持有次数（共享计数）
 *   - 低 16 位：写锁重入次数（独占计数）
 *   - 内部维护两个 AQS 子类：
 *     - Sync（共享模式）→ 读锁
 *     - Sync（独占模式）→ 写锁
 *
 * 【锁降级】
 *   持有写锁的线程可以继续获取读锁（锁降级），但反过来不行（读锁不能升级为写锁）。
 *   锁降级的典型场景：先写入数据，再读取验证，最后释放写锁（保留读锁防止其他写线程插入）。
 *
 * 【ReentrantReadWriteLock vs StampedLock】
 *   ReentrantReadWriteLock：悲观读锁，读时完全阻塞写
 *   StampedLock（JDK8+）：支持乐观读（不加锁），读多写少场景性能更优
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public class ReadWriteLockCache<K, V> {

    /** 非线程安全的底层存储（由读写锁保护） */
    private final Map<K, V> cache = new HashMap<>();

    /** 读写锁实例（内部维护读锁和写锁） */
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    /**
     * 读锁（共享模式）
     * 多个线程可同时持有，用于保护读操作
     * AQS 内部：高 16 位记录读锁持有次数
     */
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();

    /**
     * 写锁（独占模式）
     * 同一时刻只有一个线程能持有，用于保护写操作
     * AQS 内部：低 16 位记录写锁重入次数
     */
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    /**
     * 读操作：多个读线程可同时执行
     *
     * 【并发行为】
     *   - 无写锁时：所有读线程同时进入，无阻塞
     *   - 有写锁时：读线程阻塞等待写锁释放
     *   - 读锁之间不互斥：多个线程可同时 get()
     */
    public V get(K key) {
        readLock.lock();  // 获取读锁（共享模式，AQS 高 16 位 +1）
        try {
            System.out.printf("[读] %s 尝试读取 key=%s%n", Thread.currentThread().getName(), key);
            V value = cache.get(key);
            System.out.printf("[读] %s 读取完成 value=%s%n", Thread.currentThread().getName(), value);
            return value;
        } finally {
            readLock.unlock();  // 释放读锁（AQS 高 16 位 -1）
        }
    }

    /**
     * 写操作：独占，同一时间只有一个线程能写
     *
     * 【并发行为】
     *   - 写锁与所有其他锁（读锁、写锁）互斥
     *   - 写入时，所有读线程和写线程都会被阻塞
     *   - 写锁支持重入：同一线程可多次 lock()，需要对应次数的 unlock()
     */
    public void put(K key, V value) {
        writeLock.lock();  // 获取写锁（独占模式，AQS 低 16 位 +1）
        try {
            System.out.printf("[写] %s 开始写入 key=%s, value=%s%n",
                    Thread.currentThread().getName(), key, value);
            cache.put(key, value);
            Thread.sleep(500); // 模拟写耗时（实际场景可能是 DB 写入等）
            System.out.printf("[写] %s 写入完成%n", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();  // 释放写锁
        }
    }

    /**
     * 清除所有缓存（写操作，独占）
     */
    public void clear() {
        writeLock.lock();
        try {
            cache.clear();
            System.out.printf("[写] %s 清空缓存%n", Thread.currentThread().getName());
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 测试入口：5 个读线程 + 2 个写线程并发操作缓存
     *
     * 观察要点：
     *   1. 读线程之间不会互相阻塞（会看到读操作交替输出）
     *   2. 写线程会阻塞所有其他线程（读和写都会等待）
     *   3. 写操作之间也是互斥的
     */
    public static void main(String[] args) {
        ReadWriteLockCache<String, String> cache = new ReadWriteLockCache<>();

        // 写线程1：写入 name=张三
        new Thread(() -> cache.put("name", "张三"), "Writer-1").start();
        // 写线程2：写入 city=北京
        new Thread(() -> cache.put("city", "北京"), "Writer-2").start();

        // 5 个读线程：并发读取缓存
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    cache.get("name");
                    cache.get("city");
                    try {
                        Thread.sleep(100); // 模拟读间隔
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Reader-" + i).start();
        }
    }
}