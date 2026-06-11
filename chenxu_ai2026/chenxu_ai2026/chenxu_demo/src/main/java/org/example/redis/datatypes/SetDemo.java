package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Component;

/**
 * Redis Set（集合）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * intset（整数集合）+ hashtable 两种编码，元素少且为整数时用 intset 节省内存。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 标签系统 —— 给文章/商品打标签，按标签筛选
 *   ├─────── 共同好友/关注 —— SINTER 求交集发现共同关注
 *   ├─────── 抽奖系统 —— SRANDMEMBER 随机抽取，SPOP 不重复抽取
 *   ├─────── 去重 —— SADD 自动去重，快速判断元素是否存在（SISMEMBER O(1)）
 *   └─────── 推荐系统 —— SDIFF 发现"你可能感兴趣"的人
 * </pre>
 */
@Component
public class SetDemo {

    private static final Logger log = LoggerFactory.getLogger(SetDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:set:";

    /**
     * 标签系统。给文章打标签，支持按标签检索、统计、推荐。
     * 场景：博客、电商的商品标签、知识库分类。
     */
    public String tagsSystem() {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();

        // 给文章添加多个标签（SADD）
        ops.add(KEY_PREFIX + "article:3001", "Java", "Redis", "架构设计", "高并发");
        ops.add(KEY_PREFIX + "article:3002", "Java", "Spring Boot", "微服务");
        ops.add(KEY_PREFIX + "article:3003", "Redis", "缓存", "系统设计");

        StringBuilder sb = new StringBuilder("【标签系统】\n");
        sb.append("  文章 3001 标签: ").append(ops.members(KEY_PREFIX + "article:3001")).append("\n");
        sb.append("  文章 3002 标签: ").append(ops.members(KEY_PREFIX + "article:3002")).append("\n");
        sb.append("  文章 3003 标签: ").append(ops.members(KEY_PREFIX + "article:3003")).append("\n");

        // 计算标签"Java"关联的文章数
        sb.append("  标签【Java】关联文章数: ").append(ops.size(KEY_PREFIX + "article:3001")).append("\n");

        // 判断元素是否存在（O(1)）
        boolean hasRedis = Boolean.TRUE.equals(ops.isMember(KEY_PREFIX + "article:3001", "Redis"));
        sb.append("  文章 3001 是否包含【Redis】: ").append(hasRedis);
        return sb.toString();
    }

    /**
     * 共同好友/关注（集合运算 SINTER / SUNION / SDIFF）。
     * 场景：社交平台发现共同关注的人、好友推荐。
     */
    public String mutualFollow() {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();

        // 用户 A 关注了：张三、李四、王五、赵六
        ops.add(KEY_PREFIX + "follow:userA", "张三", "李四", "王五", "赵六");
        // 用户 B 关注了：李四、赵六、钱七、孙八
        ops.add(KEY_PREFIX + "follow:userB", "李四", "赵六", "钱七", "孙八");

        // 交集：共同关注
        java.util.Set<Object> intersect = ops.intersect(KEY_PREFIX + "follow:userA", KEY_PREFIX + "follow:userB");
        // 并集：所有关注的人
        java.util.Set<Object> union = ops.union(KEY_PREFIX + "follow:userA", KEY_PREFIX + "follow:userB");
        // 差集：A 关注了但 B 没关注的（推荐 B 关注）
        java.util.Set<Object> diff = ops.difference(KEY_PREFIX + "follow:userA", KEY_PREFIX + "follow:userB");

        StringBuilder sb = new StringBuilder("【共同好友/关注】\n");
        sb.append("  A 关注: [张三, 李四, 王五, 赵六]\n");
        sb.append("  B 关注: [李四, 赵六, 钱七, 孙八]\n");
        sb.append("  共同关注（交集）: ").append(intersect).append("\n");
        sb.append("  所有关注（并集）: ").append(union).append("\n");
        sb.append("  推荐 B 关注（差集）: ").append(diff);
        return sb.toString();
    }

    /**
     * 抽奖系统（SRANDMEMBER / SPOP）。
     * 场景：活动抽奖，SRANDMEMBER 允许重复中奖，SPOP 不重复中奖。
     */
    public String lottery() {
        SetOperations<String, Object> ops = redisTemplate.opsForSet();

        // 10 个用户参与抽奖
        ops.add(KEY_PREFIX + "lottery:activity:2025",
                "user01", "user02", "user03", "user04", "user05",
                "user06", "user07", "user08", "user09", "user10");

        // SRANDMEMBER: 随机抽取 3 个（不删除）
        java.util.List<Object> winners = ops.randomMembers(KEY_PREFIX + "lottery:activity:2025", 3);

        // SPOP: 随机弹出 2 个（从集合中移除）
        java.util.List<Object> lotteryWinners = ops.pop(KEY_PREFIX + "lottery:activity:2025", 2);

        StringBuilder sb = new StringBuilder("【抽奖系统】\n");
        sb.append("  参与人数: 10\n");
        sb.append("  幸运观众（SRANDMEMBER）: ").append(winners).append("\n");
        sb.append("  真正中奖者（SPOP，移出集合）: ").append(lotteryWinners).append("\n");
        sb.append("  剩余可抽奖人数: ").append(ops.size(KEY_PREFIX + "lottery:activity:2025"));
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis Set 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】intset + hashtable 双编码\n");
        sb.append("元素少且为整数时用 intset 节省内存，否则用 hashtable。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 标签系统 —— SADD 打标签，按标签检索\n");
        sb.append("  2. 共同好友/关注 —— SINTER 交集发现共同关注\n");
        sb.append("  3. 抽奖系统 —— SRANDMEMBER 随机抽 / SPOP 不重复抽\n");
        sb.append("  4. 去重 —— SADD 自动去重\n");
        sb.append("  5. 推荐系统 —— SDIFF 发现你可能感兴趣的人\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(tagsSystem()).append("\n\n");
        sb.append("  ").append(mutualFollow()).append("\n\n");
        sb.append("  ").append(lottery()).append("\n");
        return sb.toString();
    }
}
