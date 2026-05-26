package org.example.redis.RedisDistributedLock;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.params.SetParams;

import java.util.Collections;
import java.util.UUID;

/**
 * Redis 分布式锁 Demo（基于 Jedis）
 * 核心：
 * 1. 加锁：SET key random_value NX PX expireMillis
 * 2. 解锁：Lua 脚本，先 get 校验随机值，再 del
 * 3. 重试：自旋 + 短暂休眠
 */
public class RedisDistributedLockDemo {

    private final Jedis jedis;
    private final String lockKey;
    private final String requestId;   // 唯一标识，用于安全释放锁
    private final int expireMillis;   // 锁过期时间（毫秒）

    /**
     * @param jedis        Redis 客户端
     * @param lockKey      锁的键名
     * @param expireMillis 锁持有时间（毫秒），建议业务预估时间的 2~3 倍
     */
    public RedisDistributedLockDemo(Jedis jedis, String lockKey, int expireMillis) {
        this.jedis = jedis;
        this.lockKey = lockKey;
        this.requestId = UUID.randomUUID().toString();  // 每个锁持有者唯一ID
        this.expireMillis = expireMillis;
    }

    /**
     * 尝试加锁（非阻塞）
     * @return true 成功，false 失败
     */
    public boolean tryLock() {
        // SET key value NX PX milliseconds
        SetParams params = SetParams.setParams().nx().px(expireMillis);
        String result = jedis.set(lockKey, requestId, params);
        return "OK".equals(result);
    }

    /**
     * 阻塞加锁，直到成功或超时
     * @param timeoutMillis 最长等待时间（毫秒），0 表示一直等待
     * @param retryInterval 重试间隔（毫秒）
     * @return true 加锁成功，false 超时失败
     * @throws InterruptedException
     */
    public boolean lock(long timeoutMillis, long retryInterval) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (true) {
            if (tryLock()) {
                return true;
            }
            // 检查是否超时
            if (timeoutMillis > 0 && (System.currentTimeMillis() - start) >= timeoutMillis) {
                return false;
            }
            Thread.sleep(retryInterval);
        }
    }

    /**
     * 释放锁（Lua 脚本保证原子性）
     * @return true 释放成功，false 释放失败（锁已过期或不属于自己）
     */
    public boolean unlock() {
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else " +
                        "return 0 " +
                        "end";
        Object result = jedis.eval(script, Collections.singletonList(lockKey), Collections.singletonList(requestId));
        return Long.valueOf(1L).equals(result);
    }

    // ==================== 使用示例 ====================
    public static void main(String[] args) {

        Jedis jedis = new Jedis("192.168.249.129", 6379);  // 根据实际情况修改
        RedisDistributedLockDemo lock = new RedisDistributedLockDemo(jedis, "order:123:lock", 30000);

        try {
            // 尝试加锁（等待最多5秒，每次重试间隔100ms）
            if (lock.lock(5000, 100)) {
                System.out.println("成功获取分布式锁，开始执行业务...");
                // 模拟业务处理
                Thread.sleep(2000);
                System.out.println("业务处理完成。");
            } else {
                System.out.println("获取锁超时，放弃执行。");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.unlock()) {
                System.out.println("锁已释放。");
            } else {
                System.out.println("释放锁失败（可能锁已过期或不属于当前线程）。");
            }
            jedis.close();
        }
    }
}