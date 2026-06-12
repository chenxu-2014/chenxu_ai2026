package org.example.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sentinel 统一兜底处理器 — 供 @SentinelResource 的 fallback / blockHandler 引用
 * <p>
 * 方法签名规范：
 * <ul>
 *   <li>blockHandler：返回值 + 原参数 + BlockException（必须 static）</li>
 *   <li>fallback：返回值 + 原参数 + Throwable（必须 static）</li>
 * </ul>
 */
public class SentinelBlockHandler {

    private static final Logger log = LoggerFactory.getLogger(SentinelBlockHandler.class);

    // ==================== loadAllToRedis 兜底 ====================

    /** 流控 / 熔断触发时返回友好提示（blockHandler 处理 BlockException） */
    public static String loadAllToRedisBlock(BlockException e) {
        if (e instanceof FlowException) {
            log.warn("[Sentinel 流控] loadAllToRedis 被限流，当前已有任务在执行");
            return "{\"code\":429,\"msg\":\"全量加载任务已在执行中，请稍后重试\"}";
        }
        if (e instanceof DegradeException) {
            log.warn("[Sentinel 熔断] loadAllToRedis 已熔断，慢调用比例过高");
            return "{\"code\":503,\"msg\":\"数据加载服务暂时不可用（熔断中），请稍后重试\"}";
        }
        return "{\"code\":429,\"msg\":\"请求被 Sentinel 拦截\"}";
    }

    /** 业务异常兜底（fallback 处理所有 Throwable） */
    public static String loadAllToRedisFallback(Throwable t) {
        log.error("[Sentinel Fallback] loadAllToRedis 执行异常", t);
        return "{\"code\":500,\"msg\":\"数据加载失败: " + t.getMessage() + "\"}";
    }

    // ==================== kafkaSend 兜底 ====================

    /** Kafka 发送被流控 / 熔断 */
    public static void kafkaSendBlock(String key, String value, BlockException e) {
        if (e instanceof FlowException) {
            log.warn("[Sentinel 流控] Kafka 发送被限流, key={}, QPS 超 100", key);
        } else if (e instanceof DegradeException) {
            log.warn("[Sentinel 熔断] Kafka 发送已熔断, key={}", key);
        }
        // 兜底策略：直接丢弃消息（生产环境可改为写入死信队列 / 本地日志文件）
    }

    /** Kafka 发送异常兜底 */
    public static void kafkaSendFallback(String key, String value, Throwable t) {
        log.error("[Sentinel Fallback] Kafka 发送异常, key={}, error={}", key, t.getMessage());
    }
}
