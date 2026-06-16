package org.example.datacompare.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.datacompare.dto.CUserDto;

import java.util.List;

/**
 * c_user 表 Mapper — 系统A用户档案查询。
 */
@Mapper
public interface CUserMapper {

    /** 总记录数 */
    long count();

    /** 游标分页查询（WHERE id > #{cursor} ORDER BY id LIMIT #{size}），避免深分页 OOM */
    List<CUserDto> selectByCursor(@Param("cursor") long cursor, @Param("size") int size);

    /** 按 userId 列表批量查询 */
    List<CUserDto> selectByUserIds(@Param("userIds") List<String> userIds);

    /** 获取所有 userId（用于反向比对：找出c_cons有但c_user没有的记录） */
    List<String> selectAllUserIds();

    // ========== Hash 分桶方法（>>>5亿数据场景，逐桶处理避免 OOM）==========

    /** 指定桶内的记录数 */
    long countByBucket(@Param("bucket") int bucket, @Param("bucketCount") int bucketCount);

    /** 游标分页 + Hash 分桶：WHERE MOD(CRC32(user_id), bucketCount)=bucket AND id>cursor */
    List<CUserDto> selectByBucketAndCursor(@Param("bucket") int bucket,
                                           @Param("bucketCount") int bucketCount,
                                           @Param("cursor") long cursor,
                                           @Param("size") int size);

    /** 指定桶内所有 userId（桶内反向比对用） */
    List<String> selectUserIdsByBucket(@Param("bucket") int bucket,
                                       @Param("bucketCount") int bucketCount);
}
