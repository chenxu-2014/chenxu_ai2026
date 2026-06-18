package org.example.auth.controller;

import org.example.auth.entity.SysUser;
import org.example.auth.mapper.SysUserMapper;
import org.example.auth.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器：登录 / 注册
 */
@RestController
public class AuthController {

    private final SysUserMapper sysUserMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(SysUserMapper sysUserMapper, JwtUtil jwtUtil) {
        this.sysUserMapper = sysUserMapper;
        this.jwtUtil = jwtUtil;
    }

    /** 注册 */
    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestParam String username,
                                         @RequestParam String password,
                                         @RequestParam(required = false, defaultValue = "") String nickname) {
        Map<String, Object> result = new HashMap<>();
        // 检查用户名是否已存在
        SysUser exist = sysUserMapper.selectByUsername(username);
        if (exist != null) {
            result.put("code", 400);
            result.put("msg", "用户名已存在");
            return result;
        }
        // BCrypt 加密密码
        String encodedPwd = passwordEncoder.encode(password);
        SysUser user = new SysUser(username, encodedPwd,
                nickname.isEmpty() ? username : nickname);
        sysUserMapper.insert(user);

        result.put("code", 200);
        result.put("msg", "注册成功");
        result.put("data", Map.of("userId", user.getId()));
        return result;
    }

    /** 登录 */
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestParam String username,
                                      @RequestParam String password) {
        Map<String, Object> result = new HashMap<>();
        // 查用户
        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            result.put("code", 400);
            result.put("msg", "用户名或密码错误");
            return result;
        }
        // 校验密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            result.put("code", 400);
            result.put("msg", "用户名或密码错误");
            return result;
        }
        // 生成 Token
        String token = jwtUtil.generate(String.valueOf(user.getId()));

        result.put("code", 200);
        result.put("msg", "登录成功");
        result.put("data", Map.of(
                "token", token,
                "userId", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() != null ? user.getNickname() : user.getUsername()
        ));
        return result;
    }
}
