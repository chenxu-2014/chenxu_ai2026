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
 * 请求日志全局过滤器 — 记录每个请求的方法、路径、IP、耗时
 * <p>
 * 日志采集已下沉到 chenxu_demo 服务层（{@code ApiLogInterceptor}），
 * Gateway 仅保留 SLF4J 控制台日志。
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

        log.info("[网关] → {} {} | 来源IP: {}", method, path, ip);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long elapsed = System.currentTimeMillis() - start;
            int status = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;
            log.info("[网关] ← {} {} | 状态: {} | 耗时: {}ms", method, path, status, elapsed);
        }));
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
