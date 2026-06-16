-- ============================================================
-- 系统A用户档案表（数据比对源表1）
-- ============================================================
CREATE TABLE c_user (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    user_id     VARCHAR(32)  NOT NULL COMMENT '用户唯一标识',
    user_name   VARCHAR(64)  DEFAULT '' COMMENT '姓名',
    phone       VARCHAR(20)  DEFAULT '' COMMENT '手机号',
    email       VARCHAR(128) DEFAULT '' COMMENT '邮箱',
    id_card     VARCHAR(18)  DEFAULT '' COMMENT '身份证',
    gender      TINYINT      DEFAULT 0 COMMENT '性别: 0未知, 1男, 2女',
    birth_date  DATE         DEFAULT NULL COMMENT '出生日期',
    address     VARCHAR(256) DEFAULT '' COMMENT '地址',
    status      TINYINT      DEFAULT 0 COMMENT '状态: 0禁用, 1正常, 2冻结',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统A-用户档案表';
