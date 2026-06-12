package org.example.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sentinel 规则初始化 — 应用启动时自动加载默认的流控与熔断规则
 * <p>
 * 规则说明：
 * <ul>
 *   <li>流控规则 (FlowRule)：限制 QPS / 并发线程数，超出则快速失败或排队等待</li>
 *   <li>熔断规则 (DegradeRule)：慢调用比例 / 异常比例超过阈值时熔断，快速失败而不是持续压垮下游</li>
 * </ul>
 * <p>
 * 后续接入 Sentinel Dashboard 后，规则可动态推送，本类中的硬编码规则仅作为兜底默认值。
 */
@Component
public class SentinelRulesInit {

    private static final Logger log = LoggerFactory.getLogger(SentinelRulesInit.class);

    // ========== 资源名称（需与 @SentinelResource 的 value 一致）==========
    public static final String RESOURCE_LOAD_ALL = "loadAllToRedis";     // 全量加载 1000w 数据到 Redis
    public static final String RESOURCE_KAFKA_SEND = "kafkaSend";       // Kafka 消息发送
    // ---------- 测试端点（低阈值，方便手动验证）----------
    public static final String RESOURCE_FLOW_TEST = "sentinelFlowTest";     // 流控测试 QPS=2
    public static final String RESOURCE_DEGRADE_TEST = "sentinelDegradeTest"; // 异常比例熔断测试
    public static final String RESOURCE_SLOW_TEST = "sentinelSlowTest";    // 慢调用熔断测试

    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
        log.info("Sentinel 流控 & 熔断规则初始化完成");
    }

    // ========== 流控规则 — 限制 QPS ==========
    private void initFlowRules() {
        // loadAllToRedis：同一时间只允许 1 个线程执行（避免并发全量加载打爆内存和 Redis）
        FlowRule loadRule = new FlowRule(RESOURCE_LOAD_ALL)
                .setGrade(RuleConstant.FLOW_GRADE_THREAD)   // 按并发线程数限流（而非 QPS）
                .setCount(1);                                 // 最多 1 个线程同时执行

        // kafkaSend：限制 QPS 为 100，防止下游 Kafka 被突发流量打爆
        FlowRule kafkaRule = new FlowRule(RESOURCE_KAFKA_SEND)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)       // 按 QPS 限流
                .setCount(100);                               // 每秒最多 100 次

        // ----- 测试端点：故意设低阈值，方便手动验证 -----
        FlowRule flowTestRule = new FlowRule(RESOURCE_FLOW_TEST)
                .setGrade(RuleConstant.FLOW_GRADE_QPS)       // 按 QPS 限流
                .setCount(2);                                 // 每秒最多 2 次，快速点 3 次即触发

        FlowRuleManager.loadRules(List.of(loadRule, kafkaRule, flowTestRule));
    }

    // ========== 熔断规则 — 慢调用 / 异常比例熔断 ==========
    private void initDegradeRules() {
        // loadAllToRedis：慢调用比例超过 50% 且 10s 内 >= 5 个请求时，熔断 30 秒
        DegradeRule loadDegrade = new DegradeRule(RESOURCE_LOAD_ALL)
                .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType())  // 按慢调用比例熔断
                .setCount(200)                          // 响应时间超过 200ms 算慢调用
                .setTimeWindow(30)                      // 熔断时长 30 秒
                .setMinRequestAmount(5)                 // 最小请求数（10s 统计窗口内）
                .setSlowRatioThreshold(0.5)             // 慢调用比例阈值 50%
                .setStatIntervalMs(10000);              // 统计窗口 10 秒

        // kafkaSend：异常率达到 30% 且 10s 内 >= 10 个请求时，熔断 20 秒
        // 注意：由于 send 是异步方法（CompletableFuture），异常不会传播到 Sentinel，
        //       此处规则作为演示。实际项目中需使用 SphU.asyncEntry 包裹异步调用。
        DegradeRule kafkaDegrade = new DegradeRule(RESOURCE_KAFKA_SEND)
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())  // 按异常比例熔断
                .setCount(0.3)                          // 异常比例 30%
                .setTimeWindow(20)                      // 熔断时长 20 秒
                .setMinRequestAmount(10)                // 最小请求数
                .setStatIntervalMs(10000);              // 统计窗口 10 秒

        // ----- 测试端点熔断规则 -----
        // sentinelDegradeTest：异常比例 > 50% 且在 10s 内 >= 3 个请求时，熔断 10 秒
        DegradeRule degradeTestRule = new DegradeRule(RESOURCE_DEGRADE_TEST)
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                .setCount(0.5)                          // 异常比例 50%
                .setTimeWindow(10)                      // 熔断时长 10 秒
                .setMinRequestAmount(3)                 // 最小请求数
                .setStatIntervalMs(10000);              // 统计窗口 10 秒

        // sentinelSlowTest：慢调用比例 > 50%（>200ms）且 10s 内 >= 3 个请求时，熔断 10 秒
        DegradeRule slowTestRule = new DegradeRule(RESOURCE_SLOW_TEST)
                .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType())
                .setCount(200)                          // 响应超过 200ms 算慢调用
                .setTimeWindow(10)                      // 熔断时长 10 秒
                .setMinRequestAmount(3)                 // 最小请求数
                .setSlowRatioThreshold(0.5)             // 慢调用比例 50%
                .setStatIntervalMs(10000);              // 统计窗口 10 秒

        DegradeRuleManager.loadRules(List.of(loadDegrade, kafkaDegrade, degradeTestRule, slowTestRule));
    }
}
