package org.example.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 日志采集控制器 — 接收来自 Gateway 的结构化日志，转发到 Kafka
 * <p>
 * Gateway 的 RequestLogFilter 通过反应式 WebClient 调用此接口，
 * 将请求/响应上下文以 ApiLogEvent 格式提交，再由 ApiLogProducer 写入 Kafka。
 */
@RestController
@RequestMapping("/api/log")
public class LogCollectController {

    private static final Logger log = LoggerFactory.getLogger(LogCollectController.class);

    private final ApiLogProducer apiLogProducer;

    public LogCollectController(ApiLogProducer apiLogProducer) {
        this.apiLogProducer = apiLogProducer;
    }

    /** 接收单条 API 日志 */
    @PostMapping("/collect")
    public Map<String, Object> collect(@RequestBody ApiLogEvent event) {
        apiLogProducer.send(event);
        return Map.of("code", 200, "msg", "ok");
    }

    /** 批量接收（降低 Gateway 侧网络开销） */
    @PostMapping("/collect/batch")
    public Map<String, Object> collectBatch(@RequestBody ApiLogEvent[] events) {
        for (ApiLogEvent event : events) {
            apiLogProducer.send(event);
        }
        return Map.of("code", 200, "msg", "ok", "count", events.length);
    }
}
