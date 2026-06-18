package org.example.log;

import org.example.log.entity.ApiLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * API 日志 Kafka 消费者 — 从 api-logs Topic 消费并写入 MySQL
 * <p>
 * 参考 {@link org.example.transaction.TracingMessageConsumer} 的消费模式：
 * 接收 JSON 字符串 → Jackson 反序列化 → 业务处理（此处为写库）
 */
@Component
public class ApiLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(ApiLogConsumer.class);

    private final ApiLogMapper apiLogMapper;

    public ApiLogConsumer(ApiLogMapper apiLogMapper) {
        this.apiLogMapper = apiLogMapper;
    }

    @KafkaListener(topics = "${log.kafka.topic:api-logs}", groupId = "api-log-consumer-group")
    public void onApiLogMessage(String message) {
        try {
            ApiLogEvent event = ApiLogEvent.fromJson(message);
            if (event == null) {
                log.warn("[ApiLogConsumer] 消息解析失败，跳过: {}", message);
                return;
            }
            // 转换为 DB 实体
            ApiLog entity = toEntity(event);
            int rows = apiLogMapper.insert(entity);
            if (rows > 0) {
                log.debug("[ApiLogConsumer] 入库成功 | {} {} | status={} | elapsed={}ms",
                        event.getMethod(), event.getPath(), event.getStatusCode(), event.getElapsedMs());
            }
        } catch (Exception e) {
            // 消费异常不影响 offset 提交，确保不会丢消息
            log.error("[ApiLogConsumer] 消费异常: {}", e.getMessage(), e);
        }
    }

    /** ApiLogEvent → ApiLog 实体转换 */
    private ApiLog toEntity(ApiLogEvent event) {
        ApiLog entity = new ApiLog();
        entity.setTraceId(event.getTraceId() != null ? event.getTraceId() : "");
        entity.setMethod(event.getMethod() != null ? event.getMethod() : "");
        entity.setPath(event.getPath() != null ? event.getPath() : "");
        entity.setSourceIp(event.getSourceIp() != null ? event.getSourceIp() : "");
        entity.setStatusCode(event.getStatusCode());
        entity.setElapsedMs(event.getElapsedMs());
        entity.setUserId(event.getUserId() != null ? event.getUserId() : "");
        entity.setServiceName(event.getServiceName() != null ? event.getServiceName() : "");
        entity.setErrorMsg(event.getErrorMsg() != null ? event.getErrorMsg() : "");
        return entity;
    }
}
