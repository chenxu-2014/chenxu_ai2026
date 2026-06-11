package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Redis Hash（哈希）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * ZipList（压缩列表）+ Hashtable 双编码。
 * 字段数少且值小时用 ZipList 节省内存，超过阈值自动转为 Hashtable。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 对象存储 —— 用户信息/商品详情/配置项，每个 field 对应一个属性
 *   ├─────── 购物车 —— user_id 为 key，商品 sku 为 field，数量为 value
 *   ├─────── 计数器分组 —— 多个计数器共享一个 key，节省内存
 *   ├─────── 配置中心 —— 应用配置按模块分组，HGETALL 一次性读取
 *   └─────── Session 存储 —— Web 应用用户会话属性
 * </pre>
 */
@Component
public class HashDemo {

    private static final Logger log = LoggerFactory.getLogger(HashDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:hash:";

    /**
     * 对象存储（HSET / HGET / HGETALL）。
     * 场景：用户信息、商品详情等对象数据。相比 String 存整体 JSON，
     * Hash 支持单独读写某个字段，省带宽，适合频繁修改部分字段的场景。
     *
     * 对比 String 存 JSON 和 Hash 存字段：
     *   String: SET user:1001 "{...完整JSON...}"    → 只改一个字段也要传整个 JSON
     *   Hash  : HSET user:1001 name "张三"           → 只改一个字段，省带宽
     */
    public String objectStorage() {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        String key = KEY_PREFIX + "user:1001";

        // HSET：设置单个字段
        ops.put(key, "name", "张三");
        ops.put(key, "dept", "研发部");
        ops.put(key, "title", "高级架构师");
        ops.put(key, "email", "zhangsan@example.com");
        ops.put(key, "phone", "138****8888");

        // HGET：读取单个字段
        String name = (String) ops.get(key, "name");

        // HGETALL：读取所有字段（注意生产环境大 hash 慎用，阻塞 Redis）
        Map<Object, Object> all = ops.entries(key);

        // HLEN：字段数量
        Long fields = ops.size(key);

        // HEXISTS：判断字段是否存在
        boolean hasEmail = Boolean.TRUE.equals(ops.hasKey(key, "email"));

        // HDEL：删除字段
        ops.delete(key, "phone");

        StringBuilder sb = new StringBuilder("【Hash 对象存储 —— 用户信息】\n");
        sb.append("  HGET name: ").append(name).append("\n");
        sb.append("  总字段数: ").append(fields).append("\n");
        sb.append("  email 是否存在: ").append(hasEmail).append("\n");
        sb.append("  所有字段（删除 phone 后）:\n");
        for (Map.Entry<Object, Object> e : all.entrySet()) {
            if (!"phone".equals(e.getKey())) {
                sb.append("    ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 购物车。
     * 场景：电商未登录状态下，临时存储用户购物车数据。
     * key = cart:userId, field = skuId, value = 数量。
     * 支持 HINCRBY 增减数量。
     */
    public String shoppingCart() {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        String cartKey = KEY_PREFIX + "cart:user:8888";

        // 添加商品到购物车
        ops.put(cartKey, "sku:1001", "2");   // 2 件 T 恤
        ops.put(cartKey, "sku:1002", "1");   // 1 条牛仔裤
        ops.put(cartKey, "sku:1003", "3");   // 3 双袜子

        // 修改数量：再买 1 件 T 恤
        ops.increment(cartKey, "sku:1001", 1);

        // 取出购物车全部内容
        Map<Object, Object> cart = ops.entries(cartKey);

        long totalItems = 0;
        StringBuilder sb = new StringBuilder("【购物车】\n");
        for (Map.Entry<Object, Object> e : cart.entrySet()) {
            int qty = Integer.parseInt(e.getValue().toString());
            totalItems += qty;
            sb.append("  ").append(e.getKey()).append(" × ").append(qty).append("\n");
        }
        sb.append("  购物车共 ").append(cart.size()).append(" 种商品，").append(totalItems).append(" 件");
        return sb.toString();
    }

    /**
     * 配置中心（应用配置分组存储）。
     * 场景：按模块存储应用配置项，运行时动态读取和更新。
     */
    public String configCenter() {
        HashOperations<String, Object, Object> ops = redisTemplate.opsForHash();
        String key = KEY_PREFIX + "config:app:payment";

        ops.put(key, "max_retry", "3");
        ops.put(key, "timeout_ms", "5000");
        ops.put(key, "callback_url", "https://api.example.com/payment/callback");
        ops.put(key, "rate_limit", "100");

        // 读取某个配置
        String callbackUrl = (String) ops.get(key, "callback_url");

        // HKEYS 获取所有字段名（轻量，比 HGETALL 省带宽）
        java.util.Set<Object> configKeys = ops.keys(key);

        StringBuilder sb = new StringBuilder("【配置中心 —— 支付模块】\n");
        sb.append("  配置项列表: ").append(configKeys).append("\n");
        sb.append("  callback_url = ").append(callbackUrl).append("\n");
        sb.append("  共 ").append(ops.size(key)).append(" 项配置");
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis Hash 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】ZipList + Hashtable 双编码\n");
        sb.append("字段少且值小时用 ZipList 节省内存，超阈值转 Hashtable。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 对象存储 —— HSET 存对象属性，HGET 读单个字段省带宽\n");
        sb.append("  2. 购物车 —— key=用户ID，field=SKU，value=数量\n");
        sb.append("  3. 配置中心 —— 按模块分组存储配置项\n");
        sb.append("  4. Session 存储 —— Web 会话属性字段化存储\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(objectStorage()).append("\n\n");
        sb.append("  ").append(shoppingCart()).append("\n\n");
        sb.append("  ").append(configCenter()).append("\n");
        return sb.toString();
    }
}
