package org.example.redis.load;

/**
 * C_METER 数据加载服务接口。
 * <p>
 * 定义将 C_METER 表数据加载到 Redis 的核心能力。
 * 业务语义：把数据库中 1000w 条仪表数据全量同步到 Redis，
 * 供下游业务（如实时抄表、仪表定位）高速读取。
 */
public interface CmMeterService {

    /**
     * 将 C_METER 表全量数据加载到 Redis。
     * <p>
     * 执行流程：
     * <ol>
     *   <li>查询 C_METER 表总行数（用于日志进度）</li>
     *   <li>使用 MyBatis 流式游标逐行读取全表数据</li>
     *   <li>每攒够 5000 条，通过 Redis Pipeline 批量写入，减少网络 RTT</li>
     *   <li>每 10 万条打印一次进度日志</li>
     *   <li>返回加载耗时和吞吐量统计</li>
     * </ol>
     * <p>
     * Redis 存储格式：
     * <pre>
     *   Key:   meter:{meter_id}          (例: meter:00012345)
     *   Type:  Hash
     *   Fields: meter_id, meter_port, comm_no, asset_no, meter_address
     * </pre>
     * <p>
     * 性能预估（参考值）：
     * <ul>
     *   <li>流式读取 1000w 行：约 60~120 秒</li>
     *   <li>Redis Pipeline 批量写入 1000w 条 Hash：约 120~240 秒</li>
     *   <li>整体耗时：约 3~6 分钟（取决于网络延迟和 Redis 性能）</li>
     * </ul>
     *
     * @return 加载结果摘要，格式如 "加载完成! 共 10000000 条记录, 耗时 245000 ms (245.00 秒), 平均 40816 条/秒"
     */
    String loadAllToRedis();
}
