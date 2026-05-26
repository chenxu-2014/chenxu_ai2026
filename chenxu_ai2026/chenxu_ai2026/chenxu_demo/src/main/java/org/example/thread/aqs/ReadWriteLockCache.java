package org.example.thread.aqs;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 通用缓存类（读多写少场景，使用读写锁提升并发性能）
 * @param <K> 键类型
 * @param <V> 值类型
 */
//场景二：ReentrantReadWriteLock —— 实现高性能线程安全缓存
//        适用问题：读多写少场景，多个读线程可以同时访问，但写线程必须独占。
//        AQS 实现：内部维护两个 AQS 子类（读锁共享模式 + 写锁独占模式）。
public class ReadWriteLockCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();        // 非线程安全的底层存储
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    /**
     * 读操作：多个读线程可同时执行
     */
    public V get(K key) {
        readLock.lock();
        try {
            System.out.printf("[读] %s 尝试读取 key=%s%n", Thread.currentThread().getName(), key);
            V value = cache.get(key);
            System.out.printf("[读] %s 读取完成 value=%s%n", Thread.currentThread().getName(), value);
            return value;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * 写操作：独占，同一时间只有一个线程能写
     */
    public void put(K key, V value) {
        writeLock.lock();
        try {
            System.out.printf("[写] %s 开始写入 key=%s, value=%s%n",
                    Thread.currentThread().getName(), key, value);
            cache.put(key, value);
            Thread.sleep(500); // 模拟写耗时
            System.out.printf("[写] %s 写入完成%n", Thread.currentThread().getName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 清除所有缓存（写操作）
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

    // 测试：5个读线程 + 2个写线程
    public static void main(String[] args) {
        ReadWriteLockCache<String, String> cache = new ReadWriteLockCache<>();

        // 写线程1
        new Thread(() -> cache.put("name", "张三"), "Writer-1").start();
        // 写线程2
        new Thread(() -> cache.put("city", "北京"), "Writer-2").start();

        // 多个读线程
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    cache.get("name");
                    cache.get("city");
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Reader-" + i).start();
        }
    }
}