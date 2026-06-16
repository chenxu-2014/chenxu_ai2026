package org.example.datacompare.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.datacompare.entity.UserDiffResult;

import java.util.List;

/**
 * c_user_diff 表 Mapper — 差异结果写入。
 */
@Mapper
public interface UserDiffMapper {

    /** 批量插入差异记录 */
    int insertBatch(@org.apache.ibatis.annotations.Param("list") List<UserDiffResult> list);

    /** 按批次号查询差异总数 */
    long countByBatchId(@org.apache.ibatis.annotations.Param("batchId") String batchId);

    /** 按批次号查询差异明细 */
    List<UserDiffResult> selectByBatchId(@org.apache.ibatis.annotations.Param("batchId") String batchId);

    /** 清除指定批次的旧数据（用于重新比对） */
    int deleteByBatchId(@org.apache.ibatis.annotations.Param("batchId") String batchId);
}
