package org.example.datacompare.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 差异记录实体（对应 c_user_diff 表）。
 * <p>
 * 每条记录表示一个用户的某个字段在两个系统间的差异。
 */
public class UserDiffResult implements Serializable {

    private Long id;
    private String batchId;
    private String userId;
    /** INSERT(仅A有), DELETE(仅B有), UPDATE(字段值不同) */
    private String diffType;
    private String fieldName;
    private String valueA;
    private String valueB;
    private LocalDateTime diffTime;

    public UserDiffResult() {}

    public UserDiffResult(String batchId, String userId, String diffType,
                          String fieldName, String valueA, String valueB) {
        this.batchId = batchId;
        this.userId = userId;
        this.diffType = diffType;
        this.fieldName = fieldName;
        this.valueA = valueA;
        this.valueB = valueB;
        this.diffTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDiffType() { return diffType; }
    public void setDiffType(String diffType) { this.diffType = diffType; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getValueA() { return valueA; }
    public void setValueA(String valueA) { this.valueA = valueA; }

    public String getValueB() { return valueB; }
    public void setValueB(String valueB) { this.valueB = valueB; }

    public LocalDateTime getDiffTime() { return diffTime; }
    public void setDiffTime(LocalDateTime diffTime) { this.diffTime = diffTime; }
}
