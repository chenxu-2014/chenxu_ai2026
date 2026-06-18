package org.example.auth.interceptor;

import org.example.log.ApiLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册全局拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final ApiLogInterceptor apiLogInterceptor;

    public WebMvcConfig(JwtInterceptor jwtInterceptor, ApiLogInterceptor apiLogInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.apiLogInterceptor = apiLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 鉴权拦截器 — 白名单放行，其余校验 Token
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/error");

        // API 日志拦截器 — 所有请求（含白名单）都记录
        registry.addInterceptor(apiLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/", "/error");
    }
}
