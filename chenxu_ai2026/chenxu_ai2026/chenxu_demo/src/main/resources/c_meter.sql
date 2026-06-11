-- C_METER 建表语句
-- meter_id 8位 / meter_port 4位 / COMM_NO 6位 / asset_no 10位 / meter_address 16位(文字)

CREATE TABLE C_METER (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '自增主键',
    meter_id VARCHAR(10) NOT NULL COMMENT '仪表编号, 8位数字',
    meter_port VARCHAR(8) DEFAULT '' COMMENT '端口号, 4位数字',
    COMM_NO VARCHAR(10) DEFAULT '' COMMENT '通信编号, 6位数字',
    asset_no VARCHAR(16) DEFAULT '' COMMENT '资产编号, 10位数字',
    meter_address VARCHAR(68) DEFAULT '' COMMENT '仪表地址, 16个中文字符(UTF-8 一个中文3字节, 预留48)',
    INDEX idx_meter_id (meter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仪表信息表';
