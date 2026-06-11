package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis String（字符串）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * SDS（Simple Dynamic String），O(1) 获取长度，预分配空间减少内存重新分配。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 缓存对象 —— JSON 序列化后存储大对象，如配置、用户信息等
 *   ├─────── 计数器 —— INCR/DECR 实现点赞数、访问量、库存扣减
 *   ├─────── 分布式锁 —— SET NX EX 原子指令，防止缓存击穿/重复操作
 *   ├─────── 共享 Session —— 微服务多实例共享用户登录态
 *   ├─────── 限流 —— INCR + EXPIRE 实现接口限流滑动窗口
 *   ├─────── 验证码 —— SET key code EX 60 短时验证码
 *   └─────── ID 生成器 —— INCR 生成全局唯一递增 ID
 * </pre>
 */
@Component
public class StringDemo {

    private static final Logger log = LoggerFactory.getLogger(StringDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:string:";

    // ==================== 缓存对象 ====================

    /**
     * 缓存用户基本信息。
     * 场景：避免每次请求都查询数据库，适合读多写少、偶尔变更的数据。
     */
    public String cacheObject() {
        String key = KEY_PREFIX + "user:1001";
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        String cached = (String) ops.get(key);
        if (cached != null) {
            return "【缓存命中】" + cached;
        }

        String userJson = "{\"id\":1001,\"name\":\"张三\",\"dept\":\"研发部\",\"role\":\"高级工程师\"}";
        ops.set(key, userJson, Duration.ofMinutes(5));
        return "【缓存设置】" + userJson + "(TTL: 5min)";
    }

    // ==================== 计数器 ====================

    /**
     * 文章点赞计数器（INCR/DECR）。
     * 场景：文章详情页点赞、视频播放量、商品浏览量，高并发下避免直接操作 DB。
     * 优点：原子操作，单线程无竞态，QPS 可达 10w+。
     */
    public String counter() {
        String key = KEY_PREFIX + "article:like:2048";
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        Long likes = ops.increment(key);          // INCR: 原子 +1
        ops.increment(key, 2);                     // INCRBY: 原子 +N
        ops.decrement(key);                        // DECR: 原子 -1
        String val = ops.get(key) + "";

        redisTemplate.expire(key, Duration.ofDays(7));
        return "【计数器】文章 2048 当前点赞数: " + val + "(TTL: 7 天)";
    }

    // ==================== 分布式锁 ====================

    /**
     * 分布式锁（SET NX EX 原子指令）。
     * 场景：秒杀防超卖、定时任务防重复执行、幂等性控制。
     * 注意：生产环境推荐使用 Redisson 封装的锁，自动续期，避免锁过期问题。
     */
    public String distributedLock() {
        String lockKey = KEY_PREFIX + "lock:seckill:1001";
        String requestId = Thread.currentThread().getName() + "-" + System.currentTimeMillis();
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        Boolean locked = ops.setIfAbsent(lockKey, requestId, Duration.ofSeconds(10));

        if (Boolean.TRUE.equals(locked)) {
            try {
                String stockKey = KEY_PREFIX + "stock:seckill:1001";
                Long stock = ops.decrement(stockKey);
                return "【分布式锁】获取锁成功，剩余库存: " + stock + "(锁key: " + lockKey + ")";
            } finally {
                // Lua 脚本保证释放锁的原子性
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(
                        new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                        java.util.Collections.singletonList(lockKey),
                        requestId
                );
            }
        }
        return "【分布式锁】获取锁失败，有其他线程正在处理";
    }

    // ==================== 限流 ====================

    /**
     * 接口限流（滑动窗口简易实现）。
     * 场景：API 调用频率控制，防止恶意刷接口。
     * 思路：INCR 当前秒的 key，达到阈值则限流。
     */
    public String rateLimit() {
        String key = KEY_PREFIX + "ratelimit:api:/user/query";
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        long count = ops.increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(1));
        }
        long threshold = 5;
        if (count > threshold) {
            return "【限流】请求已被限流，当前 " + count + "/" + threshold + "(阈值/秒)";
        }
        return "【限流】请求放行，当前 " + count + "/" + threshold + "(阈值/秒)";
    }

    // ==================== 验证码 ====================

    /**
     * 手机验证码（SET EX 自动过期）。
     * 场景：注册/登录验证码，6 位数字，60 秒有效期。
     */
    public String verificationCode() {
        String phone = "138****8888";
        String key = KEY_PREFIX + "sms:code:" + phone;
        String code = String.valueOf((int) ((Math.random() * 900000) + 100000));

        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(key, code, Duration.ofSeconds(60));

        return "【验证码】手机 " + phone + " 的验证码: " + code + "(60 秒内有效)";
    }

    // ==================== 所有场景汇总 ====================

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis String 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】SDS（Simple Dynamic String）\n");
        sb.append("O(1) 长度获取，预分配空间，二进制安全。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 缓存对象 —— JSON 序列化后缓存大对象，减少 DB 压力\n");
        sb.append("  2. 计数器   —— INCR/DECR 原子操作，点赞、访问量、库存\n");
        sb.append("  3. 分布式锁 —— SET NX EX 原子指令，防重入\n");
        sb.append("  4. 限流     —— INCR + EXPIRE 滑动窗口限流\n");
        sb.append("  5. 验证码   —— SET EX 自动过期短信验证码\n");
        sb.append("  6. 共享Session —— 微服务统一登录态\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(cacheObject()).append("\n");
        sb.append("  ").append(counter()).append("\n");
        sb.append("  ").append(distributedLock()).append("\n");
        sb.append("  ").append(rateLimit()).append("\n");
        sb.append("  ").append(verificationCode()).append("\n");
        return sb.toString();
    }
}
