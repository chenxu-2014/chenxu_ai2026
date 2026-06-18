package org.example.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * API 日志 Kafka 生产者 — 将结构化日志事件发送到 api-logs Topic
 * <p>
 * 参考 {@link org.example.transaction.TracingMessageProducer} 的异步发送模式：
 * Jackson 序列化 → kafkaTemplate.send() → whenComplete 回调日志
 */
@Component
public class ApiLogProducer {

    private static final Logger log = LoggerFactory.getLogger(ApiLogProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public ApiLogProducer(KafkaTemplate<String, String> kafkaTemplate,
                          @Value("${log.kafka.topic:api-logs}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    /** 异步发送日志事件到 Kafka（fire-and-forget，不阻塞） */
    public void send(ApiLogEvent event) {
        if (event == null) return;
        String json = event.toJson();
        String key = event.getTraceId() != null ? event.getTraceId() : event.getPath();
        kafkaTemplate.send(topic, key, json).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[ApiLogProducer] 发送失败 | path={} | error={}", event.getPath(), ex.getMessage());
            } else if (log.isDebugEnabled()) {
                log.debug("[ApiLogProducer] 发送成功 | path={} | offset={}",
                        event.getPath(), result.getRecordMetadata().offset());
            }
        });
    }
}
