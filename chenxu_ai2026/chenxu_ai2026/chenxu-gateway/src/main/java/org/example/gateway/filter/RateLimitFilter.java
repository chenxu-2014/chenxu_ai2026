package org.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流全局过滤器 — 网关层统一限流
 * - 基于 IP 的简单令牌桶 / 固定窗口计数器
 * - 每 IP 每秒最多 20 个请求
 * - 超限返回 429 Too Many Requests
 *
 * TODO: Step 4 接入 Sentinel 后，本过滤器替换为 Sentinel 网关限流，
 *       届时可使用 QPS/并发线程数/热点参数等多种策略，规则可持久化到 Nacos
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // 每秒最大请求数
    private static final int MAX_RPS = 20;

    // IP → 当前时间窗口 + 计数
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getHostString()
                : "unknown";

        long now = System.currentTimeMillis() / 1000; // 当前秒

        WindowCounter counter = counters.compute(ip, (k, v) -> {
            if (v == null || v.second != now) {
                return new WindowCounter(now);
            }
            return v;
        });

        int count = counter.count.incrementAndGet();

        if (count > MAX_RPS) {
            log.warn("[限流] IP:{} 超过限制 {}/s，当前请求数: {}", ip, MAX_RPS, count);
            return tooManyRequests(exchange, ip);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange, String ip) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"code\":429,\"msg\":\"请求过于频繁，请稍后重试\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    /** 时间窗口计数器 */
    private static class WindowCounter {
        final long second;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter(long second) {
            this.second = second;
        }
    }

    @Override
    public int getOrder() {
        // 限流在鉴权之后、日志之前执行
        return -80;
    }
}
