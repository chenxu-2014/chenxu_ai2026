package org.example.log;

import brave.Span;
import brave.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * API 请求日志拦截器 — 在服务层采集请求上下文并发送到 Kafka
 * <p>
 * 相比 Gateway 层采集的优势：
 * <ul>
 *   <li>Servlet 线程模型可直接使用 KafkaTemplate（无需 WebClient 中转）</li>
 *   <li>可获取 Brave Tracer 的 traceId / spanId</li>
 *   <li>可获取 JwtInterceptor 注入的 userId</li>
 *   <li>afterCompletion 保证无论成功/异常都能拿到 HTTP 状态码</li>
 * </ul>
 */
@Component
public class ApiLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiLogInterceptor.class);
    private static final String START_TIME_ATTR = "apiLogStartTime";

    private final ApiLogProducer apiLogProducer;
    private final Tracer tracer;

    public ApiLogInterceptor(ApiLogProducer apiLogProducer, Tracer tracer) {
        this.apiLogProducer = apiLogProducer;
        this.tracer = tracer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : 0;

        ApiLogEvent event = ApiLogEvent.of(
                currentTraceId(),                                    // traceId
                request.getMethod(),                                 // GET/POST/...
                request.getRequestURI(),                             // /meter/count
                request.getRemoteAddr(),                             // 来源 IP
                response.getStatus(),                                // HTTP 状态码
                elapsed,                                             // 耗时 ms
                (String) request.getAttribute("userId"),             // JwtInterceptor 注入
                "chenxu-demo",                                       // 服务名
                ex != null ? ex.getMessage() : ""                    // 异常信息
        );

        // 异步发送到 Kafka（不阻塞响应）
        apiLogProducer.send(event);

        if (log.isDebugEnabled()) {
            log.debug("[ApiLog] {} {} | status={} | {}ms | traceId={}",
                    event.getMethod(), event.getPath(),
                    event.getStatusCode(), event.getElapsedMs(), event.getTraceId());
        }
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceIdString() : "";
    }
}
