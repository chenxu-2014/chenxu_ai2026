package org.example.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel 网关层熔断限流规则初始化
 * <p>
 * 说明：
 * <ul>
 *   <li>{@code SentinelGatewayFilter} 和 {@code SentinelGatewayBlockExceptionHandler}
 *       已由 {@code SentinelSCGAutoConfiguration} 自动注册，此处不再重复定义</li>
 *   <li>本类仅负责初始化默认的 API 分组定义和网关流控规则</li>
 *   <li>规则可在 Sentinel Dashboard 中动态调整，无需重启</li>
 * </ul>
 */
@Configuration
public class SentinelGatewayConfig {

    private static final Logger log = LoggerFactory.getLogger(SentinelGatewayConfig.class);

    // ========== API 分组定义 ==========
    private static final String API_METER = "meter_api";
    private static final String API_KAFKA = "kafka_api";
    private static final String API_REDIS = "redis_api";

    @PostConstruct
    public void init() {
        initApiDefinitions();
        initGatewayRules();
        log.info("Sentinel 网关限流规则初始化完成（Filter/Handler 由 SentinelSCGAutoConfiguration 自动配置）");
    }

    /** API 定义：将路径按业务域分组，每个分组可独立设置流控阈值 */
    private void initApiDefinitions() {
        Set<ApiDefinition> definitions = new HashSet<>();

        definitions.add(new ApiDefinition(API_METER)
                .setPredicateItems(new HashSet<>(Set.of(
                        new ApiPathPredicateItem()
                                .setPattern("/meter/load")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT),
                        new ApiPathPredicateItem()
                                .setPattern("/meter/generate")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_EXACT)
                ))));

        definitions.add(new ApiDefinition(API_KAFKA)
                .setPredicateItems(new HashSet<>(Set.of(
                        new ApiPathPredicateItem()
                                .setPattern("/kafka/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                ))));

        definitions.add(new ApiDefinition(API_REDIS)
                .setPredicateItems(new HashSet<>(Set.of(
                        new ApiPathPredicateItem()
                                .setPattern("/redis/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                ))));

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    /** 网关流控规则：按 API 分组限流 */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        rules.add(new GatewayFlowRule(API_METER)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(1)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule(API_KAFKA)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(50)
                .setIntervalSec(1));

        rules.add(new GatewayFlowRule(API_REDIS)
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(200)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
    }
}
