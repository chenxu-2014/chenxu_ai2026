package org.example.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaMessageProducer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topic}")
    private String topic;

    /**
     * 发送消息，使用一致hash分区保证相同key的消息进入同一分区
     * @param key   业务key（如订单ID、用户ID），相同key保证顺序
     * @param value 消息内容
     */
    public void send(String key, String value) {
        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, value);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.println("[Producer] 发送成功: key=" + key
                        + ", partition=" + result.getRecordMetadata().partition()
                        + ", offset=" + result.getRecordMetadata().offset());
            } else {
                System.err.println("[Producer] 发送失败: key=" + key + ", error=" + ex.getMessage());
            }
        });
    }
}