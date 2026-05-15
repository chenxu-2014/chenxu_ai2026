package org.example.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic}")
    private String topic;

    @Value("${kafka.partitions}")
    private int partitions;

    @Bean
    public NewTopic hashParTopic() {
        return TopicBuilder.name(topic)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}