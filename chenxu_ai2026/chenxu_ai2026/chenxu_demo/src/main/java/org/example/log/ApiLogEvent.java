package org.example.log;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;

/**
 * API 日志事件 — 网关/服务产生的结构化日志，经 Kafka 中转后写入 DB
 * <p>
 * 参考 {@link org.example.transaction.TransactionStep} 的消息体设计模式：
 * POJO + 静态工厂 + Jackson 序列化，适合在 Kafka 中传输。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiLogEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private String traceId;      // 分布式链路 traceId
    private String method;       // GET / POST / PUT / DELETE
    private String path;         // 请求路径，如 /meter/count
    private String sourceIp;     // 来源 IP
    private int    statusCode;   // HTTP 状态码
    private long   elapsedMs;    // 请求耗时（毫秒）
    private String userId;       // JWT 解析出的用户 ID（如有）
    private String serviceName;  // chenxu-gateway / chenxu-demo
    private long   timestamp;    // 请求时间（epoch ms）
    private String errorMsg;     // 异常信息（如有）

    public ApiLogEvent() {}

    /** 静态工厂 — 自动填充时间戳 */
    public static ApiLogEvent of(String traceId, String method, String path, String sourceIp,
                                  int statusCode, long elapsedMs, String userId,
                                  String serviceName, String errorMsg) {
        ApiLogEvent e = new ApiLogEvent();
        e.traceId = traceId;
        e.method = method;
        e.path = path;
        e.sourceIp = sourceIp;
        e.statusCode = statusCode;
        e.elapsedMs = elapsedMs;
        e.userId = userId;
        e.serviceName = serviceName;
        e.timestamp = Instant.now().toEpochMilli();
        e.errorMsg = errorMsg;
        return e;
    }

    /** 序列化为 JSON */
    public String toJson() {
        try { return MAPPER.writeValueAsString(this); }
        catch (JsonProcessingException ex) { return "{}"; }
    }

    /** 从 JSON 反序列化 */
    public static ApiLogEvent fromJson(String json) {
        try { return MAPPER.readValue(json, ApiLogEvent.class); }
        catch (JsonProcessingException ex) { return null; }
    }

    // ========== Getters & Setters ==========

    public String getTraceId()    { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public String getMethod()     { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath()       { return path; }
    public void setPath(String path) { this.path = path; }

    public String getSourceIp()   { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }

    public int getStatusCode()    { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public long getElapsedMs()    { return elapsedMs; }
    public void setElapsedMs(long elapsedMs) { this.elapsedMs = elapsedMs; }

    public String getUserId()     { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getServiceName(){ return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public long getTimestamp()    { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getErrorMsg()   { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}
