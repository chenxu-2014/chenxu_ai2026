package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis Sorted Set（有序集合 / ZSet）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * 跳表（SkipList）+ 哈希表（HashTable）。
 * 跳表实现 O(log N) 的插入/删除/范围查询，哈希表实现 O(1) 的元素查找分值。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 排行榜 —— 实时积分/热度/销售额排行，ZREVRANGE 取 TopN
 *   ├─────── 延迟队列 —— score 存储时间戳，ZRANGEBYSCORE 取到期任务
 *   ├─────── 滑动窗口限流 —— score 存时间戳，ZREMRANGEBYSCORE 清理过期窗口
 *   ├─────── 范围查询 —— ZRANGEBYSCORE 按分数区间查询数据
 *   └─────── 自动补全 —— score 存权重，前缀补全
 * </pre>
 */
@Component
public class SortedSetDemo {

    private static final Logger log = LoggerFactory.getLogger(SortedSetDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:zset:";

    /**
     * 实时排行榜（ZINCRBY / ZREVRANGE）。
     * 场景：游戏积分榜、直播人气榜、商品销量榜、热搜排行。
     * ZINCRBY 原子增减分值，ZREVRANGE 按分值降序取 TopN。
     */
    public String leaderboard() {
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        String key = KEY_PREFIX + "leaderboard:live:room888";

        // 主播收到礼物，分值（人气值）实时增长
        ops.incrementScore(key, "主播-李佳琦", 15200);
        ops.incrementScore(key, "主播-薇娅", 14800);
        ops.incrementScore(key, "主播-小杨哥", 23000);
        ops.incrementScore(key, "主播-刘畊宏", 9800);
        ops.incrementScore(key, "主播-董宇辉", 18500);
        ops.incrementScore(key, "主播-广东夫妇", 12600);

        // 模拟李佳琦再次收到大额礼物，分值增加
        ops.incrementScore(key, "主播-李佳琦", 8000);

        // ZREVRANGE: 按分值降序取 Top3（带分值）
        Set<ZSetOperations.TypedTuple<Object>> top3 = ops.reverseRangeWithScores(key, 0, 2);

        StringBuilder sb = new StringBuilder("【实时排行榜】\n");
        sb.append("  主播人气排名（降序）:\n");
        int rank = 1;
        for (ZSetOperations.TypedTuple<Object> tuple : top3) {
            sb.append("    ").append(rank++).append(". ")
                    .append(tuple.getValue())
                    .append(" (人气值: ").append(String.format("%.0f", tuple.getScore())).append(")\n");
        }
        sb.append("  参与排行的主播: ").append(ops.size(key)).append(" 人\n");

        // 查询某主播的排名和分数
        Double liScore = ops.score(key, "主播-李佳琦");
        Long liRank = ops.reverseRank(key, "主播-李佳琦");
        sb.append("  李佳琦当前排名: 第").append((liRank != null ? liRank + 1 : 0)).append("名，人气值: ")
                .append(String.format("%.0f", liScore != null ? liScore : 0));
        return sb.toString();
    }

    /**
     * 延迟队列（ZRANGEBYSCORE 轮询到期任务）。
     * 场景：订单超时自动取消、定时任务调度、优惠券过期提醒。
     * 实现：score 存任务执行的时间戳，消费者轮询 ZRANGEBYSCORE 0 nowTimestamp。
     */
    public String delayQueue() {
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        String key = KEY_PREFIX + "delay:order:timeout";

        long now = System.currentTimeMillis() / 1000;  // 当前秒级时间戳

        // 添加 3 个延时任务，score = 到期时间戳
        ops.add(key, "order:10001:待支付", now + 30);   // 30 秒后到期
        ops.add(key, "order:10002:待支付", now + 60);   // 60 秒后到期
        ops.add(key, "order:10003:待支付", now + 120);  // 120 秒后到期

        // 模拟时间流逝，查询当前到期的任务（score <= now 的）
        Set<Object> expired = ops.rangeByScore(key, 0, now + 35);  // 假设过去了 35 秒

        // 清理已处理的任务
        if (expired != null) {
            ops.remove(key, expired.toArray());
        }

        StringBuilder sb = new StringBuilder("【延迟队列】\n");
        sb.append("  已添加 3 个延迟任务（30s/60s/120s 到期）\n");
        sb.append("  当前到期任务（35 秒后）: ").append(expired).append("\n");
        sb.append("  剩余未到期任务数: ").append(ops.size(key));
        return sb.toString();
    }

    /**
     * 商品销售排行榜（当天实时）。
     * 场景：电商大促期间实时销量排行。
     */
    public String salesRanking() {
        ZSetOperations<String, Object> ops = redisTemplate.opsForZSet();
        String key = KEY_PREFIX + "sales:daily:2025-06-11";

        // 模拟当天销量数据
        ops.add(key, "iPhone 16 Pro Max", 342);
        ops.add(key, "华为 Mate 70", 289);
        ops.add(key, "AirPods Pro 3", 521);
        ops.add(key, "MacBook Air M4", 167);
        ops.add(key, "小米 SU7 防晒膜", 893);

        // ZREVRANGEBYSCORE: 按销量降序
        Set<ZSetOperations.TypedTuple<Object>> ranking = ops.reverseRangeByScoreWithScores(key, 0, 10000);

        StringBuilder sb = new StringBuilder("【商品销量排行榜（当天）】\n");
        int rank = 1;
        if (ranking != null) {
            for (ZSetOperations.TypedTuple<Object> tuple : ranking) {
                sb.append("  ").append(rank++).append(". ")
                        .append(tuple.getValue())
                        .append(" — 销量: ").append(String.format("%.0f", tuple.getScore())).append("\n");
            }
        }
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis Sorted Set (ZSet) 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】SkipList + HashTable\n");
        sb.append("插入/删除/范围查询 O(log N)，查找分值 O(1)。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 实时排行榜 —— ZINCRBY 增减分值，ZREVRANGE 取 TopN\n");
        sb.append("  2. 延迟队列 —— score 存时间戳，到期任务 ZRANGEBYSCORE\n");
        sb.append("  3. 商品销售排行 —— 当天销量实时排序\n");
        sb.append("  4. 滑动窗口限流 —— score 存时间戳清理过期窗口\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(leaderboard()).append("\n\n");
        sb.append("  ").append(delayQueue()).append("\n\n");
        sb.append("  ").append(salesRanking()).append("\n");
        return sb.toString();
    }
}
