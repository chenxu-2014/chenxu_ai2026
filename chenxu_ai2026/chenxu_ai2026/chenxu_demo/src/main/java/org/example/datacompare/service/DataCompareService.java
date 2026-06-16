package org.example.datacompare.service;

import java.util.Map;

/**
 * 数据比对服务接口。
 * <p>
 * 比对两个系统的用户档案数据（c_user vs c_cons），找出差异并落库。
 * <p>
 * 提供两种比对模式：
 * <ul>
 *   <li>{@link #compare(int)} — 普通模式，适用于 5000w 以下数据量</li>
 *   <li>{@link #compareWithBuckets(int, int)} — Hash 分桶模式，适用于 5 亿以上数据量，
 *       通过 CRC32(user_id) % bucketCount 将数据分桶，逐桶处理避免 OOM</li>
 * </ul>
 */
public interface DataCompareService {

    /**
     * 普通模式：游标分页全量比对。
     *
     * @param pageSize 分页大小（默认 5000）
     * @return 汇总结果
     */
    Map<String, Object> compare(int pageSize);

    /**
     * Hash 分桶模式：将数据按 user_id 哈希分成 bucketCount 个桶，逐桶比对。
     * <p>
     * 每个桶只加载该桶内的数据到内存，桶处理完后释放再处理下一个桶。
     * 例如 5 亿条数据分 100 桶，每桶约 500 万条，HashSet 内存 ~500MB，可控。
     *
     * @param pageSize    每批拉取条数（默认 5000）
     * @param bucketCount 分桶数（建议 50~200，默认 100）
     * @return 汇总结果（含各桶统计明细）
     */
    Map<String, Object> compareWithBuckets(int pageSize, int bucketCount);
}
