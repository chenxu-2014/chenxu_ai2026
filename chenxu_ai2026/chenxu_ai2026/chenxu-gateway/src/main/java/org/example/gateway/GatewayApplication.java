package org.example.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 统一网关入口
 * - 请求路由：将外部请求根据路径规则转发到对应的微服务
 * - JWT 鉴权：校验请求头中的 Token，拦截未授权请求
 * - 限流：基于 RequestRateLimiter 网关过滤器控制请求频率
 * - 日志处理：记录每个请求的路径、耗时、响应状态
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
