package org.example.log;

import org.apache.ibatis.annotations.Mapper;
import org.example.log.entity.ApiLog;

/**
 * API 日志 MyBatis Mapper
 */
@Mapper
public interface ApiLogMapper {

    /** 插入一条 API 访问日志 */
    int insert(ApiLog apiLog);
}
