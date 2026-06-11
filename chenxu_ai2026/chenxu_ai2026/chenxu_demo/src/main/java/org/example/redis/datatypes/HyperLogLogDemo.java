package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HyperLogLogOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis HyperLogLog（基数统计）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * 概率数据结构，基于伯努利试验的 LogLog 算法改进版。
 * 固定使用约 12 KB 内存即可统计 2^64 个元素的基数，标准误差约 0.81%。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 日活用户（UV） —— 每天一个 key，PFADD 记录用户 ID
 *   ├─────── 月度/周度活跃 —— PFMERGE 合并多天数据，再 PFCOUNT
 *   ├─────── 搜索词去重统计 —— 搜索热词 UV
 *   ├─────── 页面 PV/UV —— PV 用计数器，UV 用 HyperLogLog
 *   └─────── IP 去重统计 —— 百万级 IP 去重仅需 12 KB
 * </pre>
 *
 * 注意：HyperLogLog 不存储元素本身，只能 COUNT，不能查询具体元素。
 * 误差约 0.81%，对 UV 统计来说精度足够。
 */
@Component
public class HyperLogLogDemo {

    private static final Logger log = LoggerFactory.getLogger(HyperLogLogDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:hll:";

    /**
     * 日活用户（UV）统计。
     * 场景：统计今天有多少不同的用户访问了首页。
     * 如果用 Set 存 1000w 用户 ID，内存约 500 MB+；
     * 用 HyperLogLog 只需 12 KB，省了 40000 倍。
     */
    public String dailyUV() {
        HyperLogLogOperations<String, Object> ops = redisTemplate.opsForHyperLogLog();
        String key = KEY_PREFIX + "uv:index:2025-06-11";

        // 模拟 10w 次访问，实际共有 50000 个不同用户
        for (int i = 1; i <= 100000; i++) {
            // 取模制造重复用户：只有 50000 个不同用户
            String userId = "user:" + (i % 50000);
            ops.add(key, userId);
        }

        Long estimatedUV = ops.size(key);

        StringBuilder sb = new StringBuilder("【日活用户 UV 统计】\n");
        sb.append("  模拟 100000 次访问，实际不同用户 50000\n");
        sb.append("  HyperLogLog 估算 UV: ").append(estimatedUV).append("\n");
        sb.append("  实际 UV: 50000\n");
        sb.append("  误差率: ")
                .append(String.format("%.2f", Math.abs(estimatedUV - 50000) * 100.0 / 50000))
                .append("%\n");
        sb.append("  内存占用: 约 12 KB（如果用 Set 存需 ~2 MB）");
        return sb.toString();
    }

    /**
     * 月度活跃合并（PFMERGE）。
     * 场景：将 30 天的日 UV 合并成月 UV，不需要额外存储。
     */
    public String monthlyMergeUV() {
        HyperLogLogOperations<String, Object> ops = redisTemplate.opsForHyperLogLog();

        // 模拟 3 天的 UV 数据，每天访问的用户 ID 不同
        ops.add(KEY_PREFIX + "uv:day1", "user:1", "user:2", "user:3", "user:4", "user:5");
        ops.add(KEY_PREFIX + "uv:day2", "user:3", "user:4", "user:5", "user:6", "user:7");
        ops.add(KEY_PREFIX + "uv:day3", "user:1", "user:5", "user:8", "user:9", "user:10");

        // PFMERGE: 合并 3 天数据，去重统计
        String mergeKey = KEY_PREFIX + "uv:merge:week1";
        ops.union(mergeKey, KEY_PREFIX + "uv:day1", KEY_PREFIX + "uv:day2", KEY_PREFIX + "uv:day3");

        Long mergedUV = ops.size(mergeKey);

        StringBuilder sb = new StringBuilder("【月度活跃合并 —— PFMERGE】\n");
        sb.append("  第 1 天 UV: 5 (user1~5)\n");
        sb.append("  第 2 天 UV: 5 (user3~7)\n");
        sb.append("  第 3 天 UV: 5 (user1,5,8,9,10)\n");
        sb.append("  实际 3 天去重 UV: 10\n");
        sb.append("  PFMERGE 估算合并 UV: ").append(mergedUV).append("\n");
        sb.append("  合并消耗内存: 仍只占 12 KB");
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis HyperLogLog 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】概率数据结构，伯努利试验 + LogLog 算法\n");
        sb.append("固定 12 KB 内存统计 2^64 个元素基数，标准误差约 0.81%。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 日活用户 UV —— PFADD + PFCOUNT，12 KB 代替几百 MB\n");
        sb.append("  2. 月度活跃合并 —— PFMERGE 合并多天数据\n");
        sb.append("  3. 搜索词去重统计 —— 搜索热词 UV\n");
        sb.append("  4. IP 去重 —— 百万级 IP 去重仅需 12 KB\n");
        sb.append("  注意：不存储元素本身，只能 COUNT 不能查询具体元素\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(dailyUV()).append("\n\n");
        sb.append("  ").append(monthlyMergeUV()).append("\n");
        return sb.toString();
    }
}
