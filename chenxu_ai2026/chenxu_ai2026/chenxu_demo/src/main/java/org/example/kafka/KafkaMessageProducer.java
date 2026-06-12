package org.example.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RefreshScope
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

    /**
     * 发送同一业务键（如订单号）的多条消息，保证这些消息落在同一分区且严格有序。
     * @param businessKey  用于决定分区的键（相同 key → 同一分区）
     * @param messageSeq   消息内容（例如带序列号的消息体）
     */
    public void sendOrdered(String businessKey, String messageSeq) {
        // 使用相同的 key 保证进入同一分区
        ProducerRecord<String, String> record = new ProducerRecord<>("order-topic", businessKey, messageSeq);

        // 异步发送，但配置已保证顺序（幂等性 + max.in.flight=5）
        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                System.out.printf("消息发送成功: key=%s, value=%s, offset=%d%n",
                        businessKey, messageSeq, result.getRecordMetadata().offset());
            } else {
                System.err.printf("消息发送失败: key=%s, value=%s, 错误=%s%n",
                        businessKey, messageSeq, ex.getMessage());
            }
        });
    }

    /**
     * 模拟连续发送同一业务的 10 条消息（验证顺序）
     */
    public void sendBatchOrdered() {
        String orderId = "ORDER_12345";
        for (int i = 1; i <= 10; i++) {
            sendOrdered(orderId, "step_" + i + "_data");
        }
    }
}