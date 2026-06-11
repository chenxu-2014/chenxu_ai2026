package org.example.redis.load;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;

import java.util.List;

/**
 * C_METER 表的 MyBatis Mapper 接口。
 * <p>
 * {@code @Mapper} 注解让 MyBatis 在启动时自动扫描并生成代理实现类，
 * 不需要额外配置 {@code @MapperScan}（避免把同包下的 Service 接口也误扫为 Mapper）。
 * <p>
 * 方法列表：
 * <pre>
 *   count()       → 查全表总行数，用于估算进度
 *   selectByPage  → 传统分页查询（小数据量用，不推荐用于千万级）
 *   streamAll     → 流式游标读取（推荐用于千万级全量导出，无深分页问题）
 * </pre>
 */
@Mapper
public interface CmMeterMapper {

    /**
     * 查询 C_METER 表总记录数。
     * <p>
     * 注意：InnoDB 下 COUNT(*) 需要全表扫描，1000w 行大约耗时 2~5 秒。
     * 如果对精度要求不高，可以用 {@code SHOW TABLE STATUS} 取近似值。
     *
     * @return 表总行数
     */
    long count();

    /**
     * 【不推荐用于千万级数据】传统 LIMIT 分页查询。
     * <p>
     * 缺点：当 offset 很大时（如 900w），MySQL 需要扫描并丢弃前 900w 行再返回后 N 行，
     * 每页耗时递增，最后一页可能数秒甚至数十秒。这是经典的"MySQL 深分页"问题。
     * <p>
     * 保留此方法作为备用，实际加载请使用 {@link #streamAll(ResultHandler)} 流式读取。
     *
     * @param offset 起始偏移量（从 0 开始）
     * @param limit  每页记录数
     * @return 当前页的 CmMeter 列表
     */
    List<CmMeter> selectByPage(@Param("offset") long offset, @Param("limit") int limit);

    /**
     * 【推荐】流式游标读取 C_METER 全表数据。
     * <p>
     * 原理：
     * <ol>
     *   <li>{@code fetchSize = Integer.MIN_VALUE} 触发 MySQL JDBC 驱动的
     *       "streaming ResultSet" 模式 —— 逐行从服务端拉取，不在客户端内存中缓存全量结果集</li>
     *   <li>MyBatis 每读取一行，就回调一次 {@link ResultHandler#handleResult}</li>
     *   <li>调用方在回调中积累到一定数量（如 5000 条），就批量写入 Redis Pipeline</li>
     * </ol>
     * <p>
     * 优点：
     * <ul>
     *   <li>不会 OOM —— 不需要一次把 1000w 行全部加载到堆内存</li>
     *   <li>没有深分页问题 —— 不依赖 LIMIT offset，每一行都是常数时间拉取</li>
     *   <li>全程只有 1 个 DB 查询，1 个连接，连接数友好</li>
     * </ul>
     * <p>
     * 注意事项：
     * <ul>
     *   <li>流式读取期间，DB 连接一直被占用，直到 ResultSet 遍历结束或 close</li>
     *   <li>不要在回调中执行耗时的同步操作（如调外部 HTTP 接口），否则连接会被长时间占用</li>
     *   <li>如果 MySQL 有 net_write_timeout / net_read_timeout 限制，确保总处理时间在超时范围内</li>
     * </ul>
     *
     * @param handler 每读取一行就回调一次的处理器，由调用方实现批量攒批逻辑
     */
    @Options(fetchSize = Integer.MIN_VALUE)
    void streamAll(ResultHandler<CmMeter> handler);
}
