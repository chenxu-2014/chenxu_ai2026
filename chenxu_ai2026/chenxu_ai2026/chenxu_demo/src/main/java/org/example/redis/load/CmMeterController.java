package org.example.redis.load;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * C_METER 数据管理 Controller。
 * <p>
 * 对外暴露 3 个 REST 接口，用于触发数据生成、全量加载到 Redis、以及查看加载进度。
 * <p>
 * 接口说明：
 * <pre>
 *   POST /meter/generate  → 多线程造假数据（1000w 条），异步执行
 *   POST /meter/load      → 流式读取 DB 并全量写入 Redis
 *   GET  /meter/count     → 查看 Redis 中已加载的 meter 记录数
 * </pre>
 * <p>
 * 典型使用流程：
 * <pre>
 *   1. POST /meter/generate  → 生成测试数据到 MySQL
 *   2. POST /meter/load      → 将数据从 MySQL 同步到 Redis
 *   3. GET  /meter/count     → 确认 Redis 中的数据量
 * </pre>
 */
@RestController
@RequestMapping("/meter")
public class CmMeterController {

    @Autowired
    private CmMeterService cmMeterService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 触发全量加载：将 C_METER 表的所有数据写入 Redis。
     * <p>
     * 这是一个同步接口，会阻塞直到全部加载完成。
     * 1000w 数据量预计耗时 3~6 分钟，期间 HTTP 连接保持打开。
     * 如果客户端有超时限制，建议把超时设大（如 10 分钟）。
     * <p>
     * 请求示例：
     * <pre>curl -X POST http://localhost:8080/meter/load</pre>
     *
     * @return 加载结果摘要（总条数、耗时、平均吞吐量）
     */
    @PostMapping("/load")
    public String load() {
        return cmMeterService.loadAllToRedis();
    }

    /**
     * 查看 Redis 中已加载的 meter 记录数。
     * <p>
     * 注意：{@code keys("meter:*")} 在数据量大时会阻塞 Redis，
     * 1000w 个 Key 的 KEYS 命令可能耗时数十秒。
     * 生产环境建议用 {@code SCAN} 命令代替，或使用 {@code DBSIZE} 做粗略估算。
     * <p>
     * 请求示例：
     * <pre>curl http://localhost:8080/meter/count</pre>
     *
     * @return Redis 中以 "meter:" 开头的 Key 的数量
     */
    @GetMapping("/count")
    public String count() {
        Set<String> keys = redisTemplate.keys("meter:*");
        long count = keys != null ? keys.size() : 0;
        return "Redis 中 meter 记录数: " + count;
    }

    /**
     * 触发多线程造假数据（1000w 条，约 10 个线程）。
     * <p>
     * 异步执行：接口立即返回，数据生成在后台线程中进行。
     * 生成进度请观察控制台日志，每个线程每 10 万条会打印一次进度。
     * <p>
     * 请求示例：
     * <pre>curl -X POST http://localhost:8080/meter/generate</pre>
     *
     * @return 提示信息
     */
    @PostMapping("/generate")
    public String generate() {
        new Thread(() -> {
            try {
                CmMeterDataGenerator.main(new String[0]);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // 恢复中断标志
                throw new RuntimeException(e);
            }
        }).start();
        return "数据生成已在后台启动，请观察控制台日志";
    }

    /**
     * 根据 meter_id 从 Redis 中查询一条仪表数据。
     * <p>
     * 全 Key 格式为 "meter:{meter_id}"，传参时只需传 meter_id 部分。
     * 例如：GET /meter/getkey/00012345 → 查询 Key="meter:00012345"
     * <p>
     * 读写路径统一使用 StringRedisSerializer，因此可以直接用 opsForHash() 高阶 API。
     */
    @GetMapping("/getkey/{key}")
    public String getMeterKey(@PathVariable String key) {
        String redisKey = "meter:" + key;

        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        if (entries.isEmpty()) {
            return "Key [" + redisKey + "] 不存在或没有数据";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Key: ").append(redisKey).append("\n");
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append("\n");
        }

        return sb.toString();
    }
}
