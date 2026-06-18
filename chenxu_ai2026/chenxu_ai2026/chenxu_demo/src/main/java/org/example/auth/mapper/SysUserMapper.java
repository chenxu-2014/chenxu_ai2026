package org.example.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.auth.entity.SysUser;

/**
 * 系统用户 Mapper
 */
@Mapper
public interface SysUserMapper {

    /** 根据用户名查询用户 */
    SysUser selectByUsername(String username);

    /** 新增用户 */
    int insert(SysUser user);
}
