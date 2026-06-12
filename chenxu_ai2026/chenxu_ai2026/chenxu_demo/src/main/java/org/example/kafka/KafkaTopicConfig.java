package org.example.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RefreshScope
public class KafkaTopicConfig {

    @Value("${kafka.topic}")
    private String topic;

    @Value("${kafka.partitions}")
    private int partitions;

    @Value("${tracing.topic:TracingTopic}")
    private String tracingTopic;

    @Bean
    public NewTopic hashParTopic() {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(1)
                .build();
    }

    // ========== 交易链路埋点 Topic — 下单/库存/支付的追踪消息 ==========
    @Bean
    public NewTopic tracingTopic() {
        return TopicBuilder.name(tracingTopic)
                .partitions(3)                  // 3 个分区，按 traceId 哈希保证同一链路有序
                .replicas(1)
                .build();
    }
}