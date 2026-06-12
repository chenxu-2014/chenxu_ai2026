package org.example.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 交易链路埋点消息消费者 — 消费 TracingTopic 中的埋点消息
 * <p>
 * 实际项目中可对接 ELK / ClickHouse / 实时数仓进行链路分析和告警。
 * 此处打印到日志，配合 traceId 在 Zipkin 中查看完整调用链。
 */
@Component
public class TracingMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(TracingMessageConsumer.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 消费 TracingTopic 消息，还原交易链路步骤
     * <p>
     * 通过 traceId 可关联 Zipkin 分布式追踪页面，实现"业务埋点 ↔ 调用链"双向关联。
     */
    @KafkaListener(topics = "${tracing.topic:TracingTopic}", groupId = "tracing-consumer-group")
    public void onTracingMessage(String message) {
        try {
            TransactionStep step = objectMapper.readValue(message, TransactionStep.class);
            log.info("[追踪消费] ← TracingTopic | traceId={} | step={}-{} | orderId={} | userId={} | amount={} | {}",
                    step.getTraceId(), step.getStep(), step.getStatus(),
                    step.getOrderId(), step.getUserId(), step.getAmount(), step.getDetail());
        } catch (Exception e) {
            log.error("[追踪消费] 消息解析失败: {}", e.getMessage());
        }
    }
}
