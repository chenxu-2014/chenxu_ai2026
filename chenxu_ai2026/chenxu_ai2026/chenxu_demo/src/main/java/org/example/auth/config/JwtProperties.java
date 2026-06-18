package org.example.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性 — 从 application.yml 读取
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（HMAC-SHA），长度至少 256 bits */
    private String secret = "chenxu-demo-jwt-secret-key-2026";

    /** Token 有效期（毫秒），默认 2 小时 */
    private long expireMs = 2 * 60 * 60 * 1000;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpireMs() {
        return expireMs;
    }

    public void setExpireMs(long expireMs) {
        this.expireMs = expireMs;
    }
}
