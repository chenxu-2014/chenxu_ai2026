package org.example.redis.datatypes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Redis Stream（流）数据类型 Demo。
 *
 * <h3>底层结构</h3>
 * 类似于 Kafka 的日志结构：消息按到达顺序追加到 Stream 中，
 * 每条消息有唯一 ID（时间戳-序列号），支持消费者组（Consumer Group）。
 *
 * <h3>使用场景</h3>
 * <pre>
 *   ├─────── 消息队列 —— 相比 List 模式，Stream 支持 ACK、回溯消费、持久化
 *   ├─────── 事件溯源 —— 订单状态变更事件、审计日志
 *   ├─────── 通知系统 —— 多消费者订阅不同消息
 *   ├─────── 数据同步 —— CDC（Change Data Capture）事件流
 *   └─────── 实时分析 —— 日志/指标收集，多消费者消费
 * </pre>
 *
 * Stream vs List 消息队列：
 *   List: 消费者 RPOP 后消息即被删除，无法回溯，无 ACK 机制
 *   Stream: 消息持久化，消费者组管理 offset，确认机制不丢消息
 */
@Component
public class StreamDemo {

    private static final Logger log = LoggerFactory.getLogger(StreamDemo.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_PREFIX = "demo:stream:";

    /**
     * 订单事件流 —— 消息生产与消费。
     * 场景：电商订单的全生命周期状态变更事件流。
     * XADD 追加事件，XREAD 从指定位置读取消息。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String orderEventStream() {
        String streamKey = KEY_PREFIX + "order:events";
        String groupName = "order-process-group";

        // 生产者：发送订单事件
        RecordId msgId1 = redisTemplate.opsForStream().add(
                ObjectRecord.create(streamKey, Map.of(
                        "orderId", "ORD20250611001",
                        "userId", "1001",
                        "event", "order_created",
                        "amount", "2999.00",
                        "timestamp", String.valueOf(System.currentTimeMillis())
                )));

        RecordId msgId2 = redisTemplate.opsForStream().add(
                ObjectRecord.create(streamKey, Map.of(
                        "orderId", "ORD20250611001",
                        "userId", "1001",
                        "event", "payment_success",
                        "amount", "2999.00",
                        "timestamp", String.valueOf(System.currentTimeMillis())
                )));

        // 创建消费者组（如果已存在则忽略）
        try {
            redisTemplate.opsForStream().createGroup(streamKey, groupName);
        } catch (Exception e) {
            // 组已存在忽略
        }

        // 消费者：从最近未 ACK 的消息开始读
        List messages = null;
        try {
            messages = redisTemplate.opsForStream().read(
                    MapRecord.class,
                    Consumer.from(groupName, "consumer-1"),
                    StreamReadOptions.empty().count(10).block(Duration.ofSeconds(1)),
                    StreamOffset.create(streamKey, ReadOffset.lastConsumed())
            );
        } catch (Exception e) {
            // Stream 为空时忽略
        }

        // XLEN: 查看 Stream 中消息总数
        Long streamLen = redisTemplate.opsForStream().size(streamKey);

        StringBuilder sb = new StringBuilder("【订单事件流 —— Stream】\n\n");
        sb.append("【生产者】\n");
        sb.append("  XADD 消息 1 ID: ").append(msgId1).append("\n");
        sb.append("  XADD 消息 2 ID: ").append(msgId2).append("\n");
        sb.append("  Stream 总消息数: ").append(streamLen).append("\n\n");
        sb.append("【消费者组: ").append(groupName).append("】\n");
        if (messages != null && !messages.isEmpty()) {
            for (Object msg : messages) {
                MapRecord<String, Object, Object> record = (MapRecord<String, Object, Object>) msg;
                sb.append("  XREAD 消息 ID: ").append(record.getId())
                        .append(" -> ").append(record.getValue()).append("\n");
            }
        }
        sb.append("\n与 List 消息队列的区别：\n");
        sb.append("  List: 无 ACK、消息消费即删除、无法回溯\n");
        sb.append("  Stream: 持久化、ACK 确认、消费者组、消息可回溯\n");
        sb.append("  Stream 消息可靠性更高，适合对消息可靠有要求的场景");
        return sb.toString();
    }

    /**
     * 通知系统 —— 多消费者独立消费。
     * 场景：系统通知/告警，多个消费者独立处理（一个发邮件、一个存数据库）。
     * 使用不同的消费者组实现"广播"语义。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public String notificationSystem() {
        String streamKey = KEY_PREFIX + "notifications";

        // 发送通知事件
        RecordId msgId = redisTemplate.opsForStream().add(
                ObjectRecord.create(streamKey, Map.of(
                        "type", "SYSTEM_ALERT",
                        "level", "WARN",
                        "title", "CPU 使用率超过 90%",
                        "message", "服务器 CPU 使用率 92%，请关注",
                        "timestamp", String.valueOf(System.currentTimeMillis())
                ))
        );

        StringBuilder sb = new StringBuilder("【通知系统 —— Stream】\n");
        sb.append("  告警消息已发送，ID: ").append(msgId).append("\n");
        sb.append("  不同消费者组可独立消费：\n");
        sb.append("    组1 - email-group: 发送邮件通知\n");
        sb.append("    组2 - sms-group:   发送短信通知\n");
        sb.append("    组3 - metrics-group: 存储到监控系统\n");
        sb.append("  每个组内可多消费者分摊压力");
        return sb.toString();
    }

    public String demoAll() {
        StringBuilder sb = new StringBuilder("========== Redis Stream 数据类型 Demo ==========\n\n");
        sb.append("【底层结构】日志结构（类 Kafka），消息按顺序追加\n");
        sb.append("消息 ID = 时间戳-序列号，支持消费者组和心跳 ACK。\n\n");
        sb.append("【使用场景】\n");
        sb.append("  1. 消息队列 —— 相比 List，支持 ACK / 回溯 / 消费者组\n");
        sb.append("  2. 事件溯源 —— 订单生命周期事件，可回溯重放\n");
        sb.append("  3. 通知/告警系统 —— 多消费者独立消费同一份消息\n");
        sb.append("  4. 实时日志/指标 —— CDC 变更数据捕获\n\n");
        sb.append("【数据演示】\n");
        sb.append("  ").append(orderEventStream()).append("\n\n");
        sb.append("  ").append(notificationSystem()).append("\n");
        return sb.toString();
    }
}
