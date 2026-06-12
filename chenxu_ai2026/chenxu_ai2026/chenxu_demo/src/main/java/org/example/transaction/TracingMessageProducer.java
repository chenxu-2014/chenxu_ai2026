package org.example.transaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 交易链路埋点消息生产者 — 每个交易步骤的埋点数据发送到 TracingTopic
 * <p>
 * 消息包含 traceId / spanId，下游消费者可据此还原完整调用链。
 */
@Component
public class TracingMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(TracingMessageProducer.class);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${tracing.topic:TracingTopic}")
    private String tracingTopic;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 发送交易步骤埋点到 TracingTopic */
    public void send(TransactionStep step) {
        try {
            String json = objectMapper.writeValueAsString(step);
            // 用 traceId 作为 Kafka Key，保证同一调用链的消息进入同一分区（有序）
            kafkaTemplate.send(tracingTopic, step.getTraceId(), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[追踪埋点] 发送失败: step={}, orderId={}, error={}",
                                    step.getStep(), step.getOrderId(), ex.getMessage());
                        } else {
                            log.info("[追踪埋点] → TracingTopic | traceId={} | step={} | orderId={} | offset={}",
                                    step.getTraceId(), step.getStep(), step.getOrderId(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("[追踪埋点] JSON 序列化失败: {}", e.getMessage());
        }
    }
}
