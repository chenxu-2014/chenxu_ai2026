-- API 请求日志表
CREATE TABLE IF NOT EXISTS sys_api_log (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    trace_id     VARCHAR(64)  DEFAULT '' COMMENT '分布式链路 traceId',
    method       VARCHAR(10)  NOT NULL COMMENT '请求方法 GET/POST/PUT/DELETE',
    path         VARCHAR(256) NOT NULL COMMENT '请求路径',
    source_ip    VARCHAR(64)  DEFAULT '' COMMENT '来源 IP',
    status_code  INT          DEFAULT 0 COMMENT 'HTTP 状态码',
    elapsed_ms   BIGINT       DEFAULT 0 COMMENT '耗时(毫秒)',
    user_id      VARCHAR(32)  DEFAULT '' COMMENT '用户ID(从JWT解析)',
    service_name VARCHAR(32)  DEFAULT '' COMMENT '服务名 gateway/demo',
    error_msg    VARCHAR(512) DEFAULT '' COMMENT '错误信息',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_trace_id (trace_id),
    INDEX idx_path (path),
    INDEX idx_status_code (status_code),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API请求日志表';
