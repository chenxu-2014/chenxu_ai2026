package org.example.redis.RedisDistributedLock;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

public class RedissonLockDemo {
    public static void main(String[] args) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.249.129:6379");
        config.useSingleServer().setPassword("snCJadmin-1");
        RedissonClient redisson = Redisson.create(config);
        RLock lock = redisson.getLock("order:123:lock");

        try {
            // 加锁，默认看门狗每30秒自动续期（业务未完成时）
            lock.lock();
            System.out.println("获取锁成功，执行业务...");
            Thread.sleep(5000);  // 模拟长业务，锁会自动续期
            System.out.println("业务完成");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            redisson.shutdown();
        }
    }
}