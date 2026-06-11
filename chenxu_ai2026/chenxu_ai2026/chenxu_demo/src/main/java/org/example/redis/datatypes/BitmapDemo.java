package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Redis Bitmap（位图）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * Bitmap 不是独立的数据结构，底层是 String（SDS），按位操作（SETBIT/GETBIT）。
 * 一个字节（8 bit）最多可以表示 8 个状态，10 亿个 bit 仅需约 119 MB 内存。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 用户签到 —— 每个 bit 表示一天，一年仅需 365 bit = 46 字节
 *   ├─────── 日活/月活统计 —— 每个用户 id 映射到一个 bit 位置
 *   ├─────── 布隆过滤器 —— 分布式缓存防穿透
 *   ├─────── 在线状态 —— 每个用户一个 bit，0 离线 1 在线
 *   └─────── 权限控制 —— 按位保存权限掩码，快速判断权限
 * </pre>
 */
@Component
public class BitmapDemo {

    private static final Logger log = LoggerFactory.getLogger(BitmapDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:bitmap:";

    /**
     * 用户签到（SETBIT / GETBIT / BITCOUNT / BITFIELD）。
     * 场景：积分商城签到功能，每月一个 key。
     * key = sign:userId:202506   → 2025 年 6 月签到记录
     * offset = 日期（1-30/31）
     * BITCOUNT 统计当月签到总天数。
     */
    public String userCheckIn() {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String key = KEY_PREFIX + "sign:user:1001:" + month;

        // 模拟 6 月签到数据（6月1日~6月11日，其中 6/5、6/8 未签到）
        int[] signedDays = {1, 2, 3, 4, 6, 7, 9, 10, 11};
        for (int day : signedDays) {
            // offset = day-1，bit 值 = 1（已签到）
            redisTemplate.opsForValue().setBit(key, day - 1, true);
        }

        // 查询今天是否已签到
        int today = LocalDate.now().getDayOfMonth();
        boolean checkedInToday = Boolean.TRUE.equals(ops.getBit(key, today - 1));

        // BITCOUNT: 统计这个月已签到的天数
        Long totalDays = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>)
                        connection -> connection.stringCommands().bitCount(key.getBytes())
        );

        StringBuilder sb = new StringBuilder("【用户签到 —— " + month + "】\n");
        sb.append("  签到日历（0=未签，1=已签）:\n    ");
        for (int day = 1; day <= 11; day++) {
            Boolean signed = ops.getBit(key, day - 1);
            sb.append(Boolean.TRUE.equals(signed) ? "1" : "0");
            if (day % 7 == 0) sb.append(" ");
        }
        sb.append("\n  本月累计签到: ").append(totalDays).append(" 天\n");
        sb.append("  今日（6月" + today + "日）是否签到: ").append(checkedInToday ? "已签到" : "未签到");
        return sb.toString();
    }

    /**
     * 日活跃用户统计（DAU）。
     * 场景：统计每天/周/月的活跃用户数。
     * 每个用户 ID 映射到一个 bit offset，BITOP AND/OR 做周/月活统计。
     * 节省内存：1000w 日活用户只需约 1.19 MB。
     */
    public String dailyActiveUsers() {
        String key = KEY_PREFIX + "dau:2025-06-11";
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        // 模拟 100 个用户中，30 个活跃用户（用户 ID 1~100）
        int[] activeUserIds = {3, 7, 12, 15, 18, 22, 25, 28, 31, 33, 36, 39,
                42, 45, 48, 51, 55, 58, 62, 65, 68, 71, 74, 77, 80, 83, 86, 89, 95, 99};
        for (int uid : activeUserIds) {
            ops.setBit(key, uid, true);
        }

        // BITCOUNT: 统计日活数
        Long dau = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Long>)
                        connection -> connection.stringCommands().bitCount(key.getBytes())
        );

        // 检查某个用户是否活跃
        boolean userActive = Boolean.TRUE.equals(ops.getBit(key, 33));
        boolean userNotActive = Boolean.TRUE.equals(ops.getBit(key, 50));

        StringBuilder sb = new StringBuilder("【日活跃用户统计 DAU】\n");
        sb.append("  模拟用户池: 100 人，日活数: ").append(dau).append("\n");
        sb.append("  用户 33 是否活跃: ").append(userActive ? "是" : "否").append("\n");
        sb.append("  用户 50 是否活跃: ").append(userNotActive ? "是" : "否");
        return sb.toString();
    }

    /**
     * 布隆过滤器（Bloom Filter 简易模拟）。
     * 场景：缓存穿透防护（百万级数据判存在），内存占用极低。
     * 注意：实际生产推荐使用 Redisson 的 RBloomFilter，或 Guava 的 BloomFilter。
     * 这里用 SETBIT 模拟多个 hash 位，仅作概念演示。
     */
    public String bloomFilterSimple() {
        String key = KEY_PREFIX + "bloom:spam:phone";
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();

        // 用 3 个 hash 位模拟布隆过滤器（实际生产会更多）
        // 将黑名单手机号写入: hash 到 bit 位置 5, 18, 30
        int[] hashPositions = {5, 18, 30};
        for (int pos : hashPositions) {
            ops.setBit(key, pos, true);
        }

        // 判断一个手机号是否在黑名单中：同样的 hash 算法判所有位是否为 1
        boolean mayBeInBlocklist = true;
        for (int pos : hashPositions) {
            if (!Boolean.TRUE.equals(ops.getBit(key, pos))) {
                mayBeInBlocklist = false;
                break;
            }
        }

        StringBuilder sb = new StringBuilder("【布隆过滤器（模拟）】\n");
        sb.append("  黑名单已加载到 Bitmap\n");
        sb.append("  目标手机号是否可能在黑名单: ").append(mayBeInBlocklist);
        sb.append("（注意：布隆过滤器有极小误判率，但绝不漏判）");
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis Bitmap 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】基于 String（SDS）的位操作\n");
        sb.append("一个字节（8 bit）表示 8 个状态，10 亿 bit ≈ 119 MB。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 用户签到 —— 每个 bit 表示一天，一年仅 46 字节\n");
        sb.append("  2. DAU/MAU 统计 —— 1000w 活跃用户仅需 1.19 MB\n");
        sb.append("  3. 布隆过滤器 —— 缓存穿透防护\n");
        sb.append("  4. 在线状态 / 权限掩码 —— 紧凑的布尔状态存储\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(userCheckIn()).append("\n\n");
        sb.append("  ").append(dailyActiveUsers()).append("\n\n");
        sb.append("  ").append(bloomFilterSimple()).append("\n");
        return sb.toString();
    }
}
