package org.example.datacompare.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.datacompare.dto.CConsDto;

import java.util.List;

/**
 * c_cons 表 Mapper — 系统B用户档案查询。
 */
@Mapper
public interface CConsMapper {

    /** 总记录数 */
    long count();

    /** 按 userId 列表批量查询 */
    List<CConsDto> selectByUserIds(@Param("userIds") List<String> userIds);

    /** 获取所有 userId（用于反向比对：找出c_user有但c_cons没有的记录） */
    List<String> selectAllUserIds();

    // ========== Hash 分桶方法（>>>5亿数据场景，逐桶处理避免 OOM）==========

    /** 指定桶内的记录数 */
    long countByBucket(@Param("bucket") int bucket, @Param("bucketCount") int bucketCount);

    /** 指定桶内所有 userId（桶内反向比对用） */
    List<String> selectUserIdsByBucket(@Param("bucket") int bucket,
                                       @Param("bucketCount") int bucketCount);
}
