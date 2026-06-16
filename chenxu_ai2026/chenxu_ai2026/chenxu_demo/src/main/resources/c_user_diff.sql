-- ============================================================
-- 差异记录表（存储每次比对的差异结果）
-- ============================================================
CREATE TABLE c_user_diff (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    batch_id    VARCHAR(32)  NOT NULL COMMENT '比对批次号（同一次比对共用同一个batch_id）',
    user_id     VARCHAR(32)  NOT NULL COMMENT '存在差异的用户标识',
    diff_type   VARCHAR(16)  NOT NULL COMMENT '差异类型: INSERT(仅A有), DELETE(仅B有), UPDATE(字段值不同)',
    field_name  VARCHAR(64)  DEFAULT '' COMMENT '差异字段名（UPDATE类型时记录具体字段）',
    value_a     VARCHAR(512) DEFAULT '' COMMENT 'c_user(系统A)中的值',
    value_b     VARCHAR(512) DEFAULT '' COMMENT 'c_cons(系统B)中的值',
    diff_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '比对时间',
    INDEX idx_batch_id (batch_id),
    INDEX idx_user_id (user_id),
    INDEX idx_diff_type (diff_type),
    INDEX idx_diff_time (diff_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户档案差异记录表';
