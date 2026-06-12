package org.example.redis.load;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.example.sentinel.SentinelBlockHandler;
import org.example.sentinel.SentinelRulesInit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * C_METER 数据加载服务实现。
 * <p>
 * 核心设计思路：
 * <ol>
 *   <li>使用 MyBatis 流式游标（ResultHandler）逐行读取，避免将 1000w 行全部加载到内存</li>
 *   <li>在回调中攒批 —— 每积累 {@value #BATCH_SIZE} 条就通过 Redis Pipeline 批量写入</li>
 *   <li>Pipeline 将多次写入命令打包在一次网络往返中发送给 Redis，大幅减少 RTT 开销</li>
 * </ol>
 * <p>
 * Sentinel 防护：loadAllToRedis 是重量级操作（1000w 行全量加载），通过
 * {@code @SentinelResource} 实现了并发线程数流控（最多 1 个线程）和慢调用熔断。
 */
@Service
public class CmMeterServiceImpl implements CmMeterService {

    private static final Logger log = LoggerFactory.getLogger(CmMeterServiceImpl.class);

    /** 每攒够 BATCH_SIZE 条数据就通过 Pipeline 写一次 Redis */
    private static final int BATCH_SIZE = 5000;

    /** Redis Key 前缀，最终 Key 格式为 meter:00012345 */
    private static final String REDIS_KEY_PREFIX = "meter:";

    @Autowired
    private CmMeterMapper cmMeterMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 执行全量加载。
     * <p>
     * 步骤：
     * <pre>
     *   1. COUNT(*) 获取总行数（用于进度日志）
     *   2. 调用 streamAll 启动流式读取
     *   3. ResultHandler 回调中攒批 → 每 5000 条 flush 一次
     *   4. 流结束后 flush 尾部不足 5000 的剩余数据
     *   5. 汇总耗时、吞吐量，返回给调用方
     * </pre>
     *
     * @return 加载结果摘要
     */
    @Override
    // ========== Sentinel 熔断限流：并发线程数流控 + 慢调用熔断 ==========
    @SentinelResource(
            value = SentinelRulesInit.RESOURCE_LOAD_ALL,
            blockHandlerClass = SentinelBlockHandler.class,
            blockHandler = "loadAllToRedisBlock",
            fallbackClass = SentinelBlockHandler.class,
            fallback = "loadAllToRedisFallback"
    )
    public String loadAllToRedis() {
        // 1. 查总记录数，仅用于进度展示，不参与业务逻辑
        long total = cmMeterMapper.count();
        log.info("C_METER 表总记录数: {}", total);
        long startTime = System.currentTimeMillis();

        // 2. 准备攒批容器（初始容量 5000，避免扩容开销）
        List<CmMeter> batch = new ArrayList<>(BATCH_SIZE);

        // 3. 两个原子计数器
        //    loadedCount ：已加载总数（用于最终统计）
        //    lastLogMark ：上次打日志时的已加载数（用于控制每 10w 条打一次日志）
        AtomicLong loadedCount = new AtomicLong(0);
        AtomicLong lastLogMark = new AtomicLong(0);

        // 4. 启动流式游标读取
        //    ResultHandler 的 handleResult 每行触发一次，此方法阻塞直到全部行读完
        cmMeterMapper.streamAll(ctx -> {
            CmMeter meter = ctx.getResultObject();   // 当前行映射为 CmMeter 对象
            batch.add(meter);                         // 加入攒批容器

            // 攒够 5000 条 → 批量写入 Redis
            if (batch.size() >= BATCH_SIZE) {
                // new ArrayList<> 拷贝一份，防止 flush 期间 batch 被后续行修改
                flushBatch(new ArrayList<>(batch));
                long done = loadedCount.addAndGet(batch.size());
                batch.clear();   // 清空容器，开始下一轮攒批

                // 每 10 万条打一次进度日志（用差值判断，避免频繁 String.format）
                if (done - lastLogMark.get() >= 100_000) {
                    lastLogMark.set(done);
                    double percent = done * 100.0 / total;
                    log.info("进度: {}/{} ({}%)", done, total, String.format("%.1f", percent));
                }
            }
        });

        // 5. 处理最后的"尾巴"——不足 5000 条的数据
        if (!batch.isEmpty()) {
            flushBatch(batch);
            loadedCount.addAndGet(batch.size());
        }

        // 6. 汇总统计
        long count = loadedCount.get();
        long elapsed = System.currentTimeMillis() - startTime;
        String result = String.format(
                "加载完成! 共 %d 条记录, 耗时 %d ms (%.2f 秒), 平均 %.0f 条/秒",
                count, elapsed, elapsed / 1000.0,
                count * 1000.0 / Math.max(elapsed, 1));   // 避免除零
        log.info(result);
        return result;
    }

    /**
     * 将一批 CmMeter 通过 Redis Pipeline 批量写入。
     * <p>
     * 写入使用 StringRedisSerializer 统一序列化 Key 和 Value，
     * 读取时用 opsForHash().entries() 即可正确反序列化，读写路径一致。
     */
    private void flushBatch(List<CmMeter> batch) {
        StringRedisSerializer s = new StringRedisSerializer();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (CmMeter meter : batch) {
                byte[] key = s.serialize(REDIS_KEY_PREFIX + meter.getMeterId());

                Map<byte[], byte[]> fieldMap = new LinkedHashMap<>();
                fieldMap.put(s.serialize("meter_id"),      s.serialize(meter.getMeterId()));
                fieldMap.put(s.serialize("meter_port"),    s.serialize(meter.getMeterPort()));
                fieldMap.put(s.serialize("comm_no"),       s.serialize(meter.getCommNo()));
                fieldMap.put(s.serialize("asset_no"),      s.serialize(meter.getAssetNo()));
                fieldMap.put(s.serialize("meter_address"), s.serialize(meter.getMeterAddress()));

                connection.hashCommands().hMSet(key, fieldMap);
            }
            return null;
        });
    }
}
