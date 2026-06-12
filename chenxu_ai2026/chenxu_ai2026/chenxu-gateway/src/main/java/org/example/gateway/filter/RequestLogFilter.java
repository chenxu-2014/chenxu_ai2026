package org.example.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 请求日志全局过滤器 — 统一网关日志处理
 * - 记录每个请求的方法、路径、来源 IP
 * - 记录响应状态码和耗时
 * - 使用 SLF4J 输出，可对接 ELK/SkyWalking 等日志平台
 */
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
        String path = request.getURI().getPath();
        String ip = request.getRemoteAddress() != null
                ? request.getRemoteAddress().getHostString()
                : "unknown";

        long start = System.currentTimeMillis();

        // 请求进入日志
        log.info("[网关] → {} {} | 来源IP: {}", method, path, ip);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long elapsed = System.currentTimeMillis() - start;
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;
            // 响应完成日志（含耗时）
            log.info("[网关] ← {} {} | 状态: {} | 耗时: {}ms", method, path, status, elapsed);
        }));
    }

    @Override
    public int getOrder() {
        // 日志在鉴权之后执行
        return -50;
    }
}
