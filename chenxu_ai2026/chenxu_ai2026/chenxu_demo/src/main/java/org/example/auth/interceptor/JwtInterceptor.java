package org.example.auth.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.auth.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * JWT 鉴权拦截器
 * - 白名单路径直接放行（登录、注册、健康检查）
 * - 校验 Authorization: Bearer <token>
 * - 校验通过后将 userId 写入 request attribute
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    /** 白名单：不需要 Token 的路径前缀 */
    private static final List<String> WHITELIST = List.of(
            "/auth/login",
            "/auth/register",
            "/public/",
            "/actuator/"
    );

    public JwtInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String path = request.getRequestURI();

        // 白名单放行
        if (isWhitelisted(path)) {
            return true;
        }

        // 提取 Token
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"缺少 Authorization 头或格式错误\"}");
            return false;
        }

        String token = authHeader.substring(7);
        Claims claims = jwtUtil.validate(token);
        if (claims == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token 无效或已过期\"}");
            return false;
        }

        // 鉴权通过，注入 userId
        request.setAttribute("userId", jwtUtil.getUserId(claims));
        return true;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }
}
