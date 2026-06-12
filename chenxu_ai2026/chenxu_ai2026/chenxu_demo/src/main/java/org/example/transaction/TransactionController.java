package org.example.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 交易链路演示 Controller — 下单 → 扣库存 → 支付
 * <p>
 * 核心流程：
 * <pre>
 * POST /order/execute?userId=U001&productId=PROD_001&amount=99.9
 *   → TransactionService.executeOrder()
 *     → ① 下单（创建子 Span ORDER）
 *     → ② 扣库存（创建子 Span INVENTORY）
 *     → ③ 支付（创建子 Span PAYMENT）
 *     → 每个步骤都发送埋点消息到 TracingTopic
 * </pre>
 * <p>
 * 通过 traceId，可在 Zipkin 中查看完整调用瀑布图，
 * 同时 TracingTopic 消费端的日志可做业务审计。
 */
@RestController
@RequestMapping("/order")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    /**
     * 执行完整交易链路
     *
     * @param userId    用户 ID（如 U001）
     * @param productId 商品 ID（如 PROD_001，预置库存 100；PROD_002 库存 50）
     * @param amount    订单金额
     */
    @PostMapping("/execute")
    public Map<String, Object> executeOrder(
            @RequestParam(defaultValue = "U001") String userId,
            @RequestParam(defaultValue = "PROD_001") String productId,
            @RequestParam(defaultValue = "99.9") double amount) {
        return transactionService.executeOrder(userId, productId, amount);
    }
}
