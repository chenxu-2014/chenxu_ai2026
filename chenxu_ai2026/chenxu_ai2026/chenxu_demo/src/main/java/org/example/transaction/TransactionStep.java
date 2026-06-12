package org.example.transaction;

import java.time.Instant;

/**
 * 交易链路埋点实体 — 每个交易步骤都生成一条埋点消息发送到 TracingTopic
 * <p>
 * 包含 traceId/spanId 关联分布式链路追踪，以及业务上下文（订单号、金额等）
 */
public class TransactionStep {

    private String traceId;          // 分布式调用链 ID（关联 Zipkin）
    private String spanId;           // 当前 Span ID
    private String step;             // 步骤名：ORDER / INVENTORY / PAYMENT
    private String status;           // 状态：PENDING / SUCCESS / FAILED
    private String orderId;          // 订单号
    private String userId;           // 用户 ID
    private String productId;        // 商品 ID
    private double amount;           // 金额
    private long timestamp;          // 时间戳（毫秒）
    private String detail;           // 详情描述

    public TransactionStep() {}

    public static TransactionStep of(String traceId, String spanId, String step, String status,
                                      String orderId, String userId, String productId,
                                      double amount, String detail) {
        TransactionStep ts = new TransactionStep();
        ts.traceId = traceId;
        ts.spanId = spanId;
        ts.step = step;
        ts.status = status;
        ts.orderId = orderId;
        ts.userId = userId;
        ts.productId = productId;
        ts.amount = amount;
        ts.timestamp = Instant.now().toEpochMilli();
        ts.detail = detail;
        return ts;
    }

    // ========== Getters（序列化需要）==========
    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getStep() { return step; }
    public String getStatus() { return status; }
    public String getOrderId() { return orderId; }
    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public double getAmount() { return amount; }
    public long getTimestamp() { return timestamp; }
    public String getDetail() { return detail; }

    public void setTraceId(String traceId) { this.traceId = traceId; }
    public void setSpanId(String spanId) { this.spanId = spanId; }
    public void setStep(String step) { this.step = step; }
    public void setStatus(String status) { this.status = status; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setProductId(String productId) { this.productId = productId; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public void setDetail(String detail) { this.detail = detail; }
}
