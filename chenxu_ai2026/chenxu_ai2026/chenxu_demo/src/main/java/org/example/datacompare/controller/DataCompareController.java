package org.example.datacompare.controller;

import org.example.datacompare.mapper.UserDiffMapper;
import org.example.datacompare.service.DataCompareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数据比对 Controller。
 * <p>
 * 对外暴露数据比对触发、进度查询、结果查看接口。
 * <p>
 * 接口说明：
 * <pre>
 *   POST /compare/run?pageSize=5000  → 触发全量比对（同步，耗时较长）
 *   GET  /compare/result/{batchId}   → 查询指定批次的比对结果摘要
 * </pre>
 * <p>
 * 典型使用流程：
 * <pre>
 *   1. POST /compare/run  → 普通模式（游标分页，适合 < 5000w）
 *   2. POST /compare/runWithBuckets → Hash分桶模式（适合 > 5亿，避免OOM）
 *   3. GET  /compare/result/{batchId} → 查看差异数量
 * </pre>
 */
@RestController
@RequestMapping("/compare")
public class DataCompareController {

    @Autowired
    private DataCompareService dataCompareService;

    @Autowired
    private UserDiffMapper userDiffMapper;

    /**
     * 触发全量比对。
     * <p>
     * 这是一个同步接口，会阻塞直到全部比对完成。
     * 对于 5000w 级别数据，预计耗时 5~10 分钟。
     * <p>
     * 请求示例：
     * <pre>curl -X POST "http://localhost:8081/compare/run?pageSize=5000"</pre>
     *
     * @param pageSize 分页大小（默认 5000）
     * @return 比对结果摘要（batchId、各类型差异数、耗时等）
     */
    @PostMapping("/run")
    public Map<String, Object> run(@RequestParam(defaultValue = "5000") int pageSize) {
        return dataCompareService.compare(pageSize);
    }

    /**
     * Hash 分桶模式比对（>>>5 亿数据适用）。
     * <p>
     * 通过 CRC32(user_id) % bucketCount 将数据均匀分桶，逐桶处理，
     * 每桶只加载该桶内的 userId 到内存，避免全量 HashSet OOM。
     * <p>
     * 内存估算：5 亿 / 100 桶 = 500 万/桶 → ~500MB HashSet
     * <p>
     * 请求示例：
     * <pre>curl -X POST "http://localhost:8081/compare/runWithBuckets?pageSize=5000&bucketCount=100"</pre>
     *
     * @param pageSize    每批拉取条数（默认 5000）
     * @param bucketCount 分桶数（默认 100，建议 50~200）
     * @return 汇总结果（含每桶明细）
     */
    @PostMapping("/runWithBuckets")
    public Map<String, Object> runWithBuckets(
            @RequestParam(defaultValue = "5000") int pageSize,
            @RequestParam(defaultValue = "100") int bucketCount) {
        return dataCompareService.compareWithBuckets(pageSize, bucketCount);
    }

    /**
     * 查询指定批次的比对结果摘要。
     * <p>
     * 请求示例：
     * <pre>curl http://localhost:8081/compare/result/BATCH_1718500000000</pre>
     */
    @GetMapping("/result/{batchId}")
    public Map<String, Object> result(@PathVariable String batchId) {
        long count = userDiffMapper.countByBatchId(batchId);
        return Map.of(
                "batchId", batchId,
                "totalDiffs", count
        );
    }
}
