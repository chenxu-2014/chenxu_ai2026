package org.example.datacompare.service;

import org.example.datacompare.dto.CConsDto;
import org.example.datacompare.dto.CUserDto;
import org.example.datacompare.entity.UserDiffResult;
import org.example.datacompare.mapper.CConsMapper;
import org.example.datacompare.mapper.CUserMapper;
import org.example.datacompare.mapper.UserDiffMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据比对服务实现。
 * <p>
 * 比对策略：游标分页拉取 → 主键 HashMap → 分批比对 → 差异落库
 * <p>
 * 使用游标分页（WHERE id > #{cursor} ORDER BY id LIMIT #{size}），
 * 避免深分页 LIMIT offset,size 导致的全表扫描和 OOM。
 * <p>
 * 流程：
 * <ol>
 *   <li>COUNT c_user 获取总记录数</li>
 *   <li>游标逐批拉取 c_user（cursor 从 0 开始，每次取 pageSize 条）</li>
 *   <li>按当前批的 userId 列表批量查询 c_cons</li>
 *   <li>构建 HashMap&lt;userId, CUserDto&gt;</li>
 *   <li>逐条比对：c_user 有、c_cons 无 → INSERT 差异</li>
 *   <li>逐字段比对：值不同 → UPDATE 差异</li>
 *   <li>全部批次完成后，反向查找：c_cons 有、c_user 无 → DELETE 差异</li>
 *   <li>返回汇总统计</li>
 * </ol>
 * <p>
 * 性能预估（5000w 数据，pageSize=5000）：
 * <pre>
 *   总批数: 10000 批
 *   每批查询 c_user（游标走主键索引，固定开销）: ~5ms
 *   每批 IN 查询 c_cons（走 idx_user_id）: ~10ms
 *   每批内存比对 5000 条: ~5ms
 *   每批批量 INSERT 差异: ~10ms
 *   每批合计: ~30ms × 10000 = ~300 秒（约 5 分钟）
 * </pre>
 */
@Service
public class DataCompareServiceImpl implements DataCompareService {

    private static final Logger log = LoggerFactory.getLogger(DataCompareServiceImpl.class);

    /** INSERT 类型差异的伪字段名（c_user 有、c_cons 无） */
    private static final String FIELD_INSERT = "__INSERT__";

    /** DELETE 类型差异的伪字段名（c_cons 有、c_user 无） */
    private static final String FIELD_DELETE = "__DELETE__";

    @Autowired
    private CUserMapper cUserMapper;

    @Autowired
    private CConsMapper cConsMapper;

    @Autowired
    private UserDiffMapper userDiffMapper;

    @Override
    public Map<String, Object> compare(int pageSize) {
        String batchId = "BATCH_" + System.currentTimeMillis();
        long startTime = System.currentTimeMillis();

        long totalUser = cUserMapper.count();
        long totalCons = cConsMapper.count();
        long totalPages = (totalUser + pageSize - 1) / pageSize;

        log.info("══════ 数据比对开始 ══════ batchId={} c_user={}w c_cons={}w pageSize={} totalPages={}",
                batchId, totalUser / 10000, totalCons / 10000, pageSize, totalPages);

        // 清除同批次旧数据（支持重新比对）
        userDiffMapper.deleteByBatchId(batchId);

        // 统计计数器
        long insertCount = 0;  // c_user 有、c_cons 无
        long updateCount = 0;  // 字段值不同
        long deleteCount = 0;  // c_cons 有、c_user 无

        // 收集所有 c_user 的 userId，用于最后的反向比对
        Set<String> allUserIds = new HashSet<>();

        // ========== 游标逐批比对 ==========
        long cursor = 0;
        int batchIndex = 0;

        while (true) {
            // ① 游标分页拉取 c_user（WHERE id > cursor ORDER BY id LIMIT size）
            List<CUserDto> userList = cUserMapper.selectByCursor(cursor, pageSize);
            if (userList.isEmpty()) break;

            // 记录本批最后一个 id 作为下一次游标
            cursor = userList.get(userList.size() - 1).getId();

            // ② 收集 userId 列表
            List<String> userIds = new ArrayList<>(userList.size());
            for (CUserDto u : userList) {
                userIds.add(u.getUserId());
                allUserIds.add(u.getUserId());
            }
            List<CConsDto> consList = cConsMapper.selectByUserIds(userIds);
            Map<String, CConsDto> consMap = consList.stream()
                    .collect(Collectors.toMap(CConsDto::getUserId, c -> c, (a, b) -> a));

            // ④ 逐条比对
            List<UserDiffResult> batchDiffs = new ArrayList<>();

            for (CUserDto user : userList) {
                CConsDto cons = consMap.get(user.getUserId());

                if (cons == null) {
                    // c_user 有、c_cons 无 → INSERT 差异
                    batchDiffs.add(new UserDiffResult(
                            batchId, user.getUserId(), "INSERT",
                            FIELD_INSERT,
                            "c_user(id=" + user.getId() + ")", "null"));
                    insertCount++;
                } else {
                    // 逐字段比对
                    List<UserDiffResult> fieldDiffs = compareFields(batchId, user, cons);
                    if (!fieldDiffs.isEmpty()) {
                        batchDiffs.addAll(fieldDiffs);
                        updateCount += fieldDiffs.size();
                    }
                }
            }

            // ⑤ 批量写入差异到 c_user_diff
            if (!batchDiffs.isEmpty()) {
                userDiffMapper.insertBatch(batchDiffs);
            }

            // 进度日志
            batchIndex++;
            if (batchIndex % 200 == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                long processed = (long) batchIndex * pageSize;
                double percent = processed * 100.0 / totalUser;
                log.info("进度: cursor={} processed={}w/{}w ({}%)  insert={} update={}  耗时={}s",
                        cursor, processed / 10000, totalUser / 10000,
                        String.format("%.1f", Math.min(percent, 100.0)),
                        insertCount, updateCount, elapsed / 1000);
            }
        }

        // ⑥ 反向比对：找出 c_cons 有但 c_user 没有的记录
        log.info("开始反向比对（c_cons → c_user）...");
        List<String> consAllUserIds = cConsMapper.selectAllUserIds();
        List<UserDiffResult> deleteDiffs = new ArrayList<>();

        for (String consUserId : consAllUserIds) {
            if (!allUserIds.contains(consUserId)) {
                deleteDiffs.add(new UserDiffResult(
                        batchId, consUserId, "DELETE",
                        FIELD_DELETE,
                        "null", "c_cons(user_id=" + consUserId + ")"));
                deleteCount++;
            }
        }

        if (!deleteDiffs.isEmpty()) {
            userDiffMapper.insertBatch(deleteDiffs);
        }

        // ========== 汇总统计 ==========
        long elapsed = System.currentTimeMillis() - startTime;
        long totalDiffs = insertCount + updateCount + deleteCount;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batchId);
        result.put("totalUser", totalUser);
        result.put("totalCons", totalCons);
        result.put("totalPages", totalPages);
        result.put("pageSize", pageSize);
        result.put("insertCount", insertCount);     // c_user 有、c_cons 无
        result.put("updateCount", updateCount);     // 字段值不同
        result.put("deleteCount", deleteCount);     // c_cons 有、c_user 无
        result.put("totalDiffs", totalDiffs);
        result.put("elapsedMs", elapsed);
        result.put("elapsedSec", elapsed / 1000.0);

        log.info("══════ 数据比对完成 ══════ batchId={} insert={} update={} delete={} totalDiffs={} elapsed={}s",
                batchId, insertCount, updateCount, deleteCount, totalDiffs, elapsed / 1000);

        return result;
    }

    // ================================================================
    // Hash 分桶模式：>>>5 亿数据场景，逐桶处理避免 OOM
    // ================================================================

    /**
     * Hash 分桶比对。
     * <p>
     * 核心原理：
     * <pre>
     *   对 user_id 做 CRC32 哈希取模，将全量数据均匀分成 bucketCount 个桶，
     *   每次只处理一个桶，该桶的 allUserIds HashSet 大小 = 总量 ÷ bucketCount。
     *
     *   例：5 亿条数据 / 100 桶 = 500 万条/桶 → allUserIds ~500MB → 可控
     *      5 亿条数据 / 200 桶 = 250 万条/桶 → allUserIds ~250MB → 更安全
     *
     *   数据流：
     *   FOR bucket IN 0..buckets-1:
     *     ① 游标拉取 c_user (WHERE CRC32(user_id)%N = bucket)
     *     ② IN 查询匹配的 c_cons
     *     ③ 逐字段比对 → INSERT / UPDATE 差异
     *     ④ 桶内反向比对：c_cons 有、c_user 无 → DELETE 差异
     *     ⑤ 释放本桶 Set → GC → 下一桶
     * </pre>
     *
     * @param pageSize    每批拉取条数
     * @param bucketCount 分桶数（建议 50~200）
     * @return 汇总结果
     */
    @Override
    public Map<String, Object> compareWithBuckets(int pageSize, int bucketCount) {
        String batchId = "BATCH_" + System.currentTimeMillis();
        long startTime = System.currentTimeMillis();

        long totalUser = cUserMapper.count();
        long totalCons = cConsMapper.count();

        log.info("══════ Hash分桶比对开始 ══════ batchId={} c_user={}w c_cons={}w buckets={} pageSize={}",
                batchId, totalUser / 10000, totalCons / 10000, bucketCount, pageSize);

        userDiffMapper.deleteByBatchId(batchId);

        long totalInsert = 0, totalUpdate = 0, totalDelete = 0;
        List<Map<String, Object>> bucketDetails = new ArrayList<>();

        // ========== 逐桶处理 ==========
        for (int bucket = 0; bucket < bucketCount; bucket++) {
            long bucketStart = System.currentTimeMillis();
            long bucketUserCount = cUserMapper.countByBucket(bucket, bucketCount);
            long bucketConsCount = cConsMapper.countByBucket(bucket, bucketCount);

            if (bucketUserCount == 0 && bucketConsCount == 0) continue;

            log.info("  Bucket {}/{} 开始  c_user={}w c_cons={}w",
                    bucket, bucketCount, bucketUserCount / 10000, bucketConsCount / 10000);

            long bucketInsert = 0, bucketUpdate = 0, bucketDelete = 0;

            // 桶内 allUserIds —— 用完即清，不会累积到下一桶
            Set<String> allUserIds = new HashSet<>((int) Math.min(bucketUserCount, Integer.MAX_VALUE));

            // --- ① 游标分页拉取 c_user（桶内） ---
            long cursor = 0;
            int batchIndex = 0;

            while (true) {
                List<CUserDto> userList = cUserMapper.selectByBucketAndCursor(
                        bucket, bucketCount, cursor, pageSize);
                if (userList.isEmpty()) break;

                cursor = userList.get(userList.size() - 1).getId();

                // 收集 userId
                List<String> userIds = new ArrayList<>(userList.size());
                for (CUserDto u : userList) {
                    userIds.add(u.getUserId());
                    allUserIds.add(u.getUserId());
                }

                // 批量查 c_cons
                List<CConsDto> consList = cConsMapper.selectByUserIds(userIds);
                Map<String, CConsDto> consMap = consList.stream()
                        .collect(Collectors.toMap(CConsDto::getUserId, c -> c, (a, b) -> a));

                // 逐条比对
                List<UserDiffResult> batchDiffs = new ArrayList<>();

                for (CUserDto user : userList) {
                    CConsDto cons = consMap.get(user.getUserId());
                    if (cons == null) {
                        batchDiffs.add(new UserDiffResult(
                                batchId, user.getUserId(), "INSERT",
                                FIELD_INSERT,
                                "c_user(id=" + user.getId() + ")", "null"));
                        bucketInsert++;
                    } else {
                        List<UserDiffResult> fieldDiffs = compareFields(batchId, user, cons);
                        if (!fieldDiffs.isEmpty()) {
                            batchDiffs.addAll(fieldDiffs);
                            bucketUpdate += fieldDiffs.size();
                        }
                    }
                }

                if (!batchDiffs.isEmpty()) {
                    userDiffMapper.insertBatch(batchDiffs);
                }

                batchIndex++;
                if (batchIndex % 200 == 0) {
                    log.info("  Bucket {} cursor={} done={}w insert={} update={}",
                            bucket, cursor, (batchIndex * pageSize) / 10000,
                            bucketInsert, bucketUpdate);
                }
            }

            // --- ② 桶内反向比对：c_cons 有、c_user 无 → DELETE ---
            List<String> consUserIds = cConsMapper.selectUserIdsByBucket(bucket, bucketCount);
            List<UserDiffResult> deleteDiffs = new ArrayList<>();

            for (String uid : consUserIds) {
                if (!allUserIds.contains(uid)) {
                    deleteDiffs.add(new UserDiffResult(
                            batchId, uid, "DELETE", FIELD_DELETE,
                            "null", "c_cons(user_id=" + uid + ")"));
                    bucketDelete++;
                }
            }

            if (!deleteDiffs.isEmpty()) {
                userDiffMapper.insertBatch(deleteDiffs);
            }

            // --- ③ 释放本桶内存 ---
            allUserIds.clear();

            long bucketElapsed = System.currentTimeMillis() - bucketStart;
            totalInsert += bucketInsert;
            totalUpdate += bucketUpdate;
            totalDelete += bucketDelete;

            Map<String, Object> bucketDetail = new LinkedHashMap<>();
            bucketDetail.put("bucket", bucket);
            bucketDetail.put("userCount", bucketUserCount);
            bucketDetail.put("consCount", bucketConsCount);
            bucketDetail.put("insert", bucketInsert);
            bucketDetail.put("update", bucketUpdate);
            bucketDetail.put("delete", bucketDelete);
            bucketDetail.put("elapsedSec", bucketElapsed / 1000.0);
            bucketDetails.add(bucketDetail);

            log.info("  Bucket {}/{} 完成  insert={} update={} delete={} 耗时={}s",
                    bucket, bucketCount, bucketInsert, bucketUpdate, bucketDelete, bucketElapsed / 1000);
        }

        // ========== 汇总 ==========
        long elapsed = System.currentTimeMillis() - startTime;
        long totalDiffs = totalInsert + totalUpdate + totalDelete;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batchId);
        result.put("totalUser", totalUser);
        result.put("totalCons", totalCons);
        result.put("bucketCount", bucketCount);
        result.put("pageSize", pageSize);
        result.put("insertCount", totalInsert);
        result.put("updateCount", totalUpdate);
        result.put("deleteCount", totalDelete);
        result.put("totalDiffs", totalDiffs);
        result.put("elapsedMs", elapsed);
        result.put("elapsedSec", elapsed / 1000.0);
        result.put("buckets", bucketDetails);

        log.info("══════ Hash分桶比对完成 ══════ batchId={} buckets={} insert={} update={} delete={} totalDiff={} elapsed={}s",
                batchId, bucketCount, totalInsert, totalUpdate, totalDelete, totalDiffs, elapsed / 1000);

        return result;
    }

    /**
     * 逐字段比对两个 DTO（c_user vs c_cons）。
     * <p>
     * 若字段值不一致，生成一条 diff_type=UPDATE 的差异记录。
     * 排除 id、create_time、update_time 等非业务字段。
     *
     * @return 差异记录列表（空列表表示完全一致）
     */
    private List<UserDiffResult> compareFields(String batchId, CUserDto user, CConsDto cons) {
        List<UserDiffResult> diffs = new ArrayList<>();

        // 逐字段比对，按字段名生成差异
        diffField(diffs, batchId, user.getUserId(), "user_name",  user.getUserName(),  cons.getUserName());
        diffField(diffs, batchId, user.getUserId(), "phone",      user.getPhone(),      cons.getPhone());
        diffField(diffs, batchId, user.getUserId(), "email",      user.getEmail(),      cons.getEmail());
        diffField(diffs, batchId, user.getUserId(), "id_card",    user.getIdCard(),     cons.getIdCard());
        diffField(diffs, batchId, user.getUserId(), "gender",     user.getGender(),     cons.getGender());
        diffField(diffs, batchId, user.getUserId(), "birth_date", user.getBirthDate(),  cons.getBirthDate());
        diffField(diffs, batchId, user.getUserId(), "address",    user.getAddress(),    cons.getAddress());
        diffField(diffs, batchId, user.getUserId(), "status",     user.getStatus(),     cons.getStatus());

        return diffs;
    }

    /** 比对单个字段，null-safe */
    private void diffField(List<UserDiffResult> diffs, String batchId, String userId,
                           String fieldName, Object valueA, Object valueB) {
        String strA = valueA == null ? "" : valueA.toString();
        String strB = valueB == null ? "" : valueB.toString();
        if (!strA.equals(strB)) {
            diffs.add(new UserDiffResult(batchId, userId, "UPDATE", fieldName, strA, strB));
        }
    }
}
