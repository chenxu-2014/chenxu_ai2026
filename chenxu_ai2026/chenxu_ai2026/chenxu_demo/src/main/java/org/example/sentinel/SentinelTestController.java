package org.example.sentinel;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sentinel 熔断限流测试端点
 * <p>
 * 每个端点都配置了低阈值，方便手动验证：
 * <ul>
 *   <li>{@code /sentinel/test/flow} — 流控测试（QPS=2，快速点 3 次即可触发）</li>
 *   <li>{@code /sentinel/test/degrade} — 熔断测试（异常比例 > 50%，连续触发异常即可熔断）</li>
 *   <li>{@code /sentinel/test/slow} — 慢调用熔断测试（模拟 500ms 响应，连续调用可触发）</li>
 * </ul>
 */
@RestController
@RequestMapping("/sentinel/test")
public class SentinelTestController {

    private static final Logger log = LoggerFactory.getLogger(SentinelTestController.class);
    private final AtomicInteger counter = new AtomicInteger(0);

    // ========== 流控测试：QPS = 2，快速请求 3 次可触发限流 ==========
    @GetMapping("/flow")
    @SentinelResource(
            value = "sentinelFlowTest",
            blockHandler = "flowBlockHandler"
    )
    public String testFlow() {
        return "OK — 第 " + counter.incrementAndGet() + " 次请求通过";
    }

    /** 流控兜底：被限流时返回此结果 */
    public String flowBlockHandler(BlockException e) {
        log.warn("[Sentinel 测试] 流控触发！QPS 超过 2");
        return "被限流了！QPS 超过阈值(2)，请稍后重试 — " + e.getClass().getSimpleName();
    }

    // ========== 熔断测试：异常比例 > 50% 触发熔断 ==========
    @GetMapping("/degrade")
    @SentinelResource(
            value = "sentinelDegradeTest",
            fallback = "degradeFallback"
    )
    public String testDegrade(@RequestParam(defaultValue = "false") boolean error) {
        if (error) {
            throw new RuntimeException("模拟业务异常");
        }
        return "OK — 正常响应";
    }

    /** 熔断兜底：熔断打开或业务异常时返回此结果 */
    public String degradeFallback(boolean error, Throwable t) {
        if (t instanceof BlockException) {
            log.warn("[Sentinel 测试] 熔断触发！断路器已打开");
            return "已熔断！断路器打开中，请 10 秒后重试";
        }
        log.warn("[Sentinel 测试] 业务异常: {}", t.getMessage());
        return "业务异常: " + t.getMessage();
    }

    // ========== 慢调用熔断测试：响应 500ms，快速连续调用触发慢调用比例熔断 ==========
    @GetMapping("/slow")
    @SentinelResource(
            value = "sentinelSlowTest",
            blockHandler = "slowBlockHandler"
    )
    public String testSlow() throws InterruptedException {
        Thread.sleep(500); // 模拟慢调用（超过 200ms 阈值）
        return "OK — 慢调用返回（500ms）";
    }

    public String slowBlockHandler(BlockException e) {
        log.warn("[Sentinel 测试] 慢调用熔断触发！");
        return "熔断了！慢调用比例过高 — " + e.getClass().getSimpleName();
    }
}
