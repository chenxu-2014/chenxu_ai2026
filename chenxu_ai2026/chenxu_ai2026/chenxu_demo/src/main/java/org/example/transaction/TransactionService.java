package org.example.transaction;

import brave.Span;
import brave.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 交易链路演示服务 — 顺序执行：下单 → 扣库存 → 支付
 * <p>
 * 每个步骤：
 * <ol>
 *   <li>创建子 Span（Zipkin 中可看到独立的 ORDER / INVENTORY / PAYMENT 片段）</li>
 *   <li>发送埋点消息到 TracingTopic（含 traceId + 业务上下文）</li>
 *   <li>模拟业务处理（库存不足 10% 概率触发）</li>
 * </ol>
 * <p>
 * 使用 Brave 原生 API（brave.Tracer），避免 Micrometer Tracing 自动配置兼容性问题。
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    @Autowired
    private Tracer tracer;                          // brave.Tracer（Brave 原生，非 Micrometer）

    @Autowired
    private TracingMessageProducer tracingProducer;

    // 模拟库存
    private final ConcurrentHashMap<String, AtomicInteger> inventory = new ConcurrentHashMap<>();

    public TransactionService() {
        inventory.put("PROD_001", new AtomicInteger(100));
        inventory.put("PROD_002", new AtomicInteger(50));
    }

    /**
     * 执行完整交易链路：下单 → 扣库存 → 支付
     */
    public Map<String, Object> executeOrder(String userId, String productId, double amount) {
        String orderId = "ORD_" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> result = new HashMap<>();

        log.info("══════ 交易链路开始 ══════ traceId={} orderId={} userId={} productId={} amount={}",
                currentTraceId(), orderId, userId, productId, amount);

        // ① 下单
        String step1Result = createOrder(orderId, userId, productId, amount);
        result.put("order", step1Result);
        if (!"SUCCESS".equals(step1Result)) {
            result.put("status", "FAILED_AT_ORDER");
            return result;
        }

        // ② 扣库存
        String step2Result = deductInventory(orderId, productId);
        result.put("inventory", step2Result);
        if (!"SUCCESS".equals(step2Result)) {
            result.put("status", "FAILED_AT_INVENTORY");
            return result;
        }

        // ③ 支付
        String step3Result = processPayment(orderId, userId, amount);
        result.put("payment", step3Result);

        result.put("status", "SUCCESS".equals(step3Result) ? "COMPLETED" : "FAILED_AT_PAYMENT");
        result.put("orderId", orderId);
        result.put("traceId", currentTraceId());

        log.info("══════ 交易链路结束 ══════ traceId={} orderId={} status={}",
                currentTraceId(), orderId, result.get("status"));
        return result;
    }

    // ==================== 下单 ====================
    private String createOrder(String orderId, String userId, String productId, double amount) {
        log.info("[下单] orderId={} userId={} productId={} amount={}", orderId, userId, productId, amount);

        Span orderSpan = tracer.nextSpan().name("ORDER").start();   // 创建子 Span
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(orderSpan)) {
            sendTrace("ORDER", "SUCCESS", orderId, userId, productId, amount, "订单创建成功");
            return "SUCCESS";
        } finally {
            orderSpan.finish();
        }
    }

    // ==================== 扣库存 ====================
    private String deductInventory(String orderId, String productId) {
        log.info("[扣库存] orderId={} productId={}", orderId, productId);

        Span inventorySpan = tracer.nextSpan().name("INVENTORY").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(inventorySpan)) {
            AtomicInteger stock = inventory.get(productId);
            // 10% 概率模拟库存不足
            if (stock == null || Math.random() < 0.1) {
                sendTrace("INVENTORY", "FAILED", orderId, null, productId, 0, "库存不足");
                return "FAILED: 库存不足";
            }
            int remaining = stock.decrementAndGet();
            sendTrace("INVENTORY", "SUCCESS", orderId, null, productId, 0,
                    "扣库存成功, 剩余: " + remaining);
            return "SUCCESS";
        } finally {
            inventorySpan.finish();
        }
    }

    // ==================== 支付 ====================
    private String processPayment(String orderId, String userId, double amount) {
        log.info("[支付] orderId={} userId={} amount={}", orderId, userId, amount);

        Span paymentSpan = tracer.nextSpan().name("PAYMENT").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(paymentSpan)) {
            sendTrace("PAYMENT", "SUCCESS", orderId, userId, null, amount, "支付成功");
            return "SUCCESS";
        } finally {
            paymentSpan.finish();
        }
    }

    // ==================== 埋点工具方法 ====================

    /** 发送一条埋点到 TracingTopic */
    private void sendTrace(String step, String status, String orderId,
                           String userId, String productId, double amount, String detail) {
        TransactionStep ts = TransactionStep.of(
                currentTraceId(), currentSpanId(),
                step, status, orderId, userId, productId, amount, detail);
        tracingProducer.send(ts);
    }

    /** 获取当前 traceId */
    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceIdString() : "N/A";
    }

    /** 获取当前 spanId */
    private String currentSpanId() {
        Span span = tracer.currentSpan();
        return span != null ? span.context().spanIdString() : "N/A";
    }
}
