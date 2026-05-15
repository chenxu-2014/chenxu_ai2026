package org.example.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageConsumer {

    @KafkaListener(topics = "${kafka.topic}", groupId = "cx-hash-group")
    public void onMessage(ConsumerRecord<String, String> record) {
        System.out.println("[Consumer] 收到消息: key=" + record.key()
                + ", value=" + record.value()
                + ", partition=" + record.partition()
                + ", offset=" + record.offset()
                + ", topic=" + record.topic());
    }
}