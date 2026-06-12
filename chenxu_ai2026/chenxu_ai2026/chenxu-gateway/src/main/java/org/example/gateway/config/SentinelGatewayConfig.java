package org.example.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Sentinel 网关层熔断限流配置 — 替换此前简单的 RateLimitFilter
 * <p>
 * 三大核心能力：
 * <ol>
 *   <li><b>网关流控 (GatewayFlowRule)</b>：按 API 分组 / 路径控制整体 QPS，超限返回 429</li>
 *   <li><b>API 定义 (ApiDefinition)</b>：将一组路径聚合为一个逻辑 API，统一设置流控规则</li>
 *   <li><b>统一异常处理 (GatewayCallbackManager)</b>：流控/熔断触发时返回 JSON 错误响应</li>
 * </ol>
 * <p>
 * 规则可在 Sentinel Dashboard 中动态调整，无需重启应用。
 */
@Configuration
public class SentinelGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(SentinelGatewayConfig.class);

    // ========== API 分组定义 — 不同业务域不同流控阈值 ==========
    private static final String API_METER = "meter_api";   // 数据加载相关（重操作，低 QPS）
    private static final String API_KAFKA = "kafka_api";   // Kafka 消息相关（中 QPS）
    private static final String API_REDIS = "redis_api";   // Redis 数据类型演示（高 QPS）

    private final List<ViewResolver> viewResolvers;
    private final ServerCodecConfigurer serverCodecConfigurer;

    public SentinelGatewayConfig(
            ObjectProvider<List<ViewResolver>> viewResolversProvider,
            ServerCodecConfigurer serverCodecConfigurer) {
        this.viewResolvers = viewResolversProvider.getIfAvailable(Collections::emptyList);
        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    // ========== Sentinel 网关过滤器 — 在请求进入路由前进行流控检查 ==========
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)      // 最高优先级，早于 JwtAuthFilter
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    // ========== 网关限流异常处理器 — 配合 scg.fallback 配置返回统一 JSON ==========
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
        return new SentinelGatewayBlockExceptionHandler(viewResolvers, serverCodecConfigurer);
    }

    // ========== 自定义限流/熔断响应 + 初始化默认规则 ==========
    @PostConstruct
    public void init() {
        initBlockHandler();
        initApiDefinitions();
        initGatewayRules();
        log.info("Sentinel 网关限流规则 & BlockHandler 初始化完成");
    }

    /** 自定义 BlockHandler：流控/熔断时返回统一 JSON 错误响应 */
    private void initBlockHandler() {
        // application.yml 中 scg.fallback 已配置了 response-status + response-body，
        // SentinelGatewayBlockExceptionHandler 会自动读取该配置，无需额外编程。
        // 如需更复杂的响应逻辑（如根据不同的 BlockException 类型返回不同消息），
        // 可通过 GatewayCallbackManager.setBlockHandler() 自定义。
        log.info("Sentinel 网关 Fallback 使用 YAML 配置: 429 + JSON");
    }

    /** API 定义：将路径按业务域分组，每个分组可独立设置流控阈值 */
    private void initApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();

        // meter_api：数据加载相关接口 — 重操作，低 QPS
        ApiDefinition meterApi = new ApiDefinition(API_METER)
                .setPredicateItems(new HashSet<>(Set.of(
                        new ApiPathPredicateItem()
                                .setPattern("/meter/load")              // POST 全量加载
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT),
                        new ApiPathPredicateItem()
                                .setPattern("/meter/generate")          // POST 数据生成
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT)
                )));
        definitions.add(meterApi);

        // kafka_api：Kafka 消息接口 — 中 QPS
        ApiDefinition kafkaApi = new ApiDefinition(API_KAFKA)
                .setPredicateItems(new HashSet<>(Set.of(
                        new ApiPathPredicateItem()
                                .setPattern("/kafka/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                )));
        definitions.add(kafkaApi);

        // redis_api：Redis 演示接口 — 高 QPS（读多写少）
        ApiDefinition redisApi = new ApiDefinition(API_REDIS)
                .setPredicateItems(new HashSet<>(Set.of(
                        new ApiPathPredicateItem()
                                .setPattern("/redis/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                )));
        definitions.add(redisApi);

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    /** 网关流控规则：按 API 分组限流 */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // meter_api：每秒最多 1 个请求（全量加载是重操作）
        rules.add(new GatewayFlowRule(API_METER)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(1)
                .setIntervalSec(1));

        // kafka_api：每秒最多 50 个请求
        rules.add(new GatewayFlowRule(API_KAFKA)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(50)
                .setIntervalSec(1));

        // redis_api：每秒最多 200 个请求（读操作，高吞吐）
        rules.add(new GatewayFlowRule(API_REDIS)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(200)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
    }
}
