package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis List（列表）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * QuickList（旧版是 LinkedList + ZipList 的组合），双向链表 + 压缩列表混合结构。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 消息队列 —— LPUSH + BRPOP 实现生产者-消费者模式
 *   ├─────── 最新消息列表 —— LPUSH + LTRIM 只保留最新的 N 条
 *   ├─────── 时间线/Feed 流 —— 用户发帖时 LPUSH 到关注者的 List
 *   ├─────── 栈 —— LPUSH + LPOP 实现后进先出
 *   └─────── 队列 —— LPUSH + RPOP 实现先进先出
 * </pre>
 *
 * 注意：如果消息可靠性要求高（不丢消息），请使用 Redis Stream 或 Kafka。
 */
@Component
public class ListDemo {

    private static final Logger log = LoggerFactory.getLogger(ListDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:list:";

    /**
     * 消息队列（生产者-消费者）。
     * 场景：异步处理耗时任务，如发送邮件、生成报表、处理图片。
     * LPUSH 从左侧插入，RPOP 从右侧取出，形成 FIFO 队列。
     */
    public String messageQueue() {
        String queueKey = KEY_PREFIX + "queue:email";
        ListOperations<String, Object> ops = redisTemplate.opsForList();

        // 生产者: LPUSH 投递 3 条消息
        ops.leftPush(queueKey, "email:user1001@example.com:欢迎注册");
        ops.leftPush(queueKey, "email:user1002@example.com:密码重置");
        ops.leftPush(queueKey, "email:user1003@example.com:订单确认");

        // 消费者: RPOP 消费消息
        StringBuilder sb = new StringBuilder("【消息队列】\n");
        sb.append("Queue 长度: ").append(ops.size(queueKey)).append("\n");
        Object msg;
        int count = 0;
        while ((msg = ops.rightPop(queueKey)) != null) {
            sb.append("  消费消息 ").append(++count).append(": ").append(msg).append("\n");
        }
        sb.append("队列已消费完毕，当前长度: ").append(ops.size(queueKey));
        return sb.toString();
    }

    /**
     * 最新消息列表（LPUSH + LTRIM）。
     * 场景：新闻资讯轮播、公告列表、实时日志展示。
     * LTRIM 只保留前 N 条，避免 List 无限增长。
     */
    public String latestNews() {
        String key = KEY_PREFIX + "news:hot";
        ListOperations<String, Object> ops = redisTemplate.opsForList();

        // 模拟 10 条新闻持续插入
        for (int i = 1; i <= 10; i++) {
            ops.leftPush(key, "新闻#" + i + ": Redis " + i + "." + i + " 版本发布");
        }
        // 只保留最新的 5 条（左侧为最新）
        ops.trim(key, 0, 4);

        StringBuilder sb = new StringBuilder("【最新消息列表（仅保留最新 5 条）】\n");
        for (int i = 0; i < ops.size(key); i++) {
            sb.append("  ").append(ops.index(key, i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 浏览历史记录（LPUSH + LTRIM 模拟栈）。
     * 场景：用户最近浏览的商品记录，最多保留 50 条。
     */
    public String browseHistory() {
        String key = KEY_PREFIX + "history:user:9527";
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        redisTemplate.delete(key);  // 先清空，重新演示

        // 用户浏览商品，每次 LPUSH 到"栈顶"
        for (int i = 1; i <= 10; i++) {
            ops.leftPush(key, "商品ID:SKU_" + String.format("%05d", i));
        }
        // 只保留最近 5 条
        ops.trim(key, 0, 4);

        StringBuilder sb = new StringBuilder("【浏览历史（最近 5 条、后进先出）】\n");
        for (int i = 0; i < ops.size(key); i++) {
            sb.append("  ").append(ops.index(key, i)).append("\n");
        }
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis List 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】QuickList（双向链表 + ZipList 压缩列表）\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 消息队列 —— LPUSH + BRPOP 生产者-消费者模式\n");
        sb.append("  2. 最新消息列表 —— LPUSH + LTRIM 保留最近 N 条\n");
        sb.append("  3. 浏览历史记录 —— 后进先出（栈），最多保留 50 条\n");
        sb.append("  4. Feed 流/时间线 —— 用户发帖时 LPUSH 到粉丝 List\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(messageQueue()).append("\n\n");
        sb.append("  ").append(latestNews()).append("\n\n");
        sb.append("  ").append(browseHistory()).append("\n");
        return sb.toString();
    }
}
