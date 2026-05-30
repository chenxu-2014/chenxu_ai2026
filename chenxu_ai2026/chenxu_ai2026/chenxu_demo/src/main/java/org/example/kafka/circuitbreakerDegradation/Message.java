package org.example.kafka.circuitbreakerDegradation;

public class Message {

    /**
     * 消息ID
     */
    long id;

    /**
     * 电表ID
     */
    String meterId;

    /**
     * 采集值
     */
    String value;

    /**
     * 事件时间
     */
    long eventTime;

    /**
     * Kafka partition
     */
    int partition;

    /**
     * Kafka offset
     */
    long offset;

    /**
     * 重试次数
     */
    int retryCount;

    /**
     * 错误信息
     */
    String errorMsg;

    /**
     * 是否核心数据
     */
    boolean core;
    public Message(){

    }
    public Message(long id,
                   String meterId,
                   String value,
                   long eventTime) {

        this.id = id;
        this.meterId = meterId;
        this.value = value;
        this.eventTime = eventTime;
    }

    @Override
    public String toString() {

        return "Message{" +
                "id=" + id +
                ", meterId=" + meterId +
                ", retryCount=" + retryCount +
                '}';
    }
}