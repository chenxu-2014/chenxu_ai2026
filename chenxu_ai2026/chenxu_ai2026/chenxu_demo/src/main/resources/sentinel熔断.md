测试 1：流控（最简单）

快速连续访问 3 次，第 3 次被限流：

# 走网关 (8080)，快速连续执行 3 次
for i in 1 2 3; do curl -s http://localhost:8080/sentinel/test/flow && echo; done

预期输出：
OK — 第 1 次请求通过
OK — 第 2 次请求通过
被限流了！QPS 超过阈值(2)，请稍后重试 — FlowException    ← 第3次被拦截

测试 2：异常比例熔断

先连续触发异常，再请求正常路径——断路器已打开：

# 连续 3 次异常请求（触发熔断条件：异常比例 > 50%）
for i in 1 2 3; do curl -s "http://localhost:8080/sentinel/test/degrade?error=true" && echo; done

# 再发正常请求——已被熔断
curl -s http://localhost:8080/sentinel/test/degrade && echo

预期输出：
业务异常: 模拟业务异常
业务异常: 模拟业务异常
业务异常: 模拟业务异常
已熔断！断路器打开中，请 10 秒后重试    ← 正常请求也被拦截

等 10 秒熔断窗口过后再试，恢复正常。

测试 3：慢调用熔断

# 连续 3 次慢调用（每次 500ms，超过 200ms 阈值）
for i in 1 2 3; do time curl -s http://localhost:8080/sentinel/test/slow && echo; done

# 第 4 次——熔断
curl -s http://localhost:8080/sentinel/test/slow && echo

直接调 8081 也可以（绕过网关，只测服务层 Sentinel）

curl -s http://localhost:8081/sentinel/test/flow