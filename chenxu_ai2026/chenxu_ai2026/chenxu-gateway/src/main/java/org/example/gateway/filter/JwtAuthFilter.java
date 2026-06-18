package org.example.gateway.filter;

import io.jsonwebtoken.Claims;
import org.example.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT 鉴权全局过滤器 — 统一网关入口拦截
 * - 校验请求头 Authorization: Bearer <token>
 * - 白名单路径直接放行（登录、健康检查、公开接口）
 * - Token 无效或过期返回 401
 * - 校验通过后将用户信息写入请求头，传递给下游服务
 */
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    // ========== JWT 鉴权白名单：这些路径不需要 Token ==========
    private static final List<String> WHITELIST = List.of(
            "/auth/login",          // 登录接口
            "/auth/register",       // 注册接口
            "/auth/",               // 认证相关路径
            "/public/",             // 公开资源
            "/actuator/",           // 健康检查
            "/meter/",              // 电表数据 Demo
            "/compare/",            // 数据比对 Demo
            "/kafka/",              // Kafka 消息测试 Demo
            "/sendOrdered",         // Kafka 有序消息 Demo
            "/redis/",              // Redis 数据类型 Demo
            "/sentinel/",           // Sentinel 测试端点
            "/order/"               // 交易链路演示端点
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径直接放行，不校验 Token
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 从请求头提取 Token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少 Authorization 头或格式错误");
        }

        String token = authHeader.substring(7);
        Claims claims = JwtUtil.validate(token);
        if (claims == null) {
            return unauthorized(exchange, "Token 无效或已过期");
        }

        // 鉴权通过，将用户信息写入请求头传递给下游服务
        ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id", JwtUtil.getUserId(claims))
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /** 返回 401 未授权响应 */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":401,\"msg\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        // 鉴权优先级最高
        return -100;
    }
}
