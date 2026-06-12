package org.example.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成 Token、校验 Token、提取 Claims
 * 用于网关层的统一鉴权
 */
public class JwtUtil {

    // 签名密钥（生产环境应从配置中心读取）
    private static final String SECRET = "chenxu-gateway-jwt-secret-key-2026-shiyue";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // Token 有效期：2 小时
    private static final long EXPIRE_MS = 2 * 60 * 60 * 1000;

    /** 生成 JWT Token */
    public static String generate(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_MS))
                .signWith(KEY)
                .compact();
    }

    /** 校验并解析 Token，返回 Claims；校验失败返回 null */
    public static Claims validate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    /** 从 Claims 中提取用户 ID */
    public static String getUserId(Claims claims) {
        return claims.getSubject();
    }
}
