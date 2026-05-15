package org.example.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaTestController {

    @Autowired
    private KafkaMessageProducer producer;

    /**
     * 发送测试消息: GET /kafka/send?key=order001&value=支付完成
     */
    @GetMapping("/kafka/send")
    public String send(@RequestParam String key, @RequestParam String value) {
        producer.send(key, value);
        return "消息已发送: key=" + key + ", value=" + value;
    }
}