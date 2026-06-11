package org.example.redis.load;

import java.io.Serializable;

/**
 * C_METER 表对应的实体类。
 * <p>
 * 表结构（共 6 列）：
 * <pre>
 *   id            BIGINT AUTO_INCREMENT  自增主键（仅 DB 使用，不加载到 Redis）
 *   meter_id      VARCHAR(8)            仪表编号，8 位数字，作为 Redis Hash 的 Key
 *   meter_port    VARCHAR(4)            端口号，4 位数字
 *   COMM_NO       VARCHAR(6)            通信编号，6 位数字
 *   asset_no      VARCHAR(10)           资产编号，10 位数字
 *   meter_address VARCHAR(48)           仪表地址，16 个中文字符
 * </pre>
 * <p>
 * 实现 {@link Serializable} 是为了：
 * 1. Redis 序列化 / 反序列化时可以走 Java 原生序列化机制
 * 2. 如果将来需要在网络间传输（如 RPC 调用），也能正常传递
 * <p>
 * Java 字段命名与 DB 列名的映射关系在 CmMeterMapper.xml 的 ResultMap 中定义。
 * 注意：DB 字段 {@code COMM_NO} 是全大写，Java 字段是驼峰 {@code commNo}。
 */
public class CmMeter implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 仪表编号，8 位数字，如 "00000001" */
    private String meterId;

    /** 端口号，4 位数字，如 "0001" */
    private String meterPort;

    /** 通信编号，6 位数字，如 "000001" */
    private String commNo;

    /** 资产编号，10 位数字，如 "0000000001" */
    private String assetNo;

    /** 仪表地址，16 个中文字符，如 "北京市朝阳区建国路88号" */
    private String meterAddress;

    // ======================== Getter / Setter ========================

    public String getMeterId() { return meterId; }
    public void setMeterId(String meterId) { this.meterId = meterId; }

    public String getMeterPort() { return meterPort; }
    public void setMeterPort(String meterPort) { this.meterPort = meterPort; }

    public String getCommNo() { return commNo; }
    public void setCommNo(String commNo) { this.commNo = commNo; }

    public String getAssetNo() { return assetNo; }
    public void setAssetNo(String assetNo) { this.assetNo = assetNo; }

    public String getMeterAddress() { return meterAddress; }
    public void setMeterAddress(String meterAddress) { this.meterAddress = meterAddress; }

    @Override
    public String toString() {
        return "CmMeter{meterId='" + meterId + "', meterPort='" + meterPort +
               "', commNo='" + commNo + "', assetNo='" + assetNo +
               "', meterAddress='" + meterAddress + "'}";
    }
}
