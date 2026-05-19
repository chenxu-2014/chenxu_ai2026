package org.example.designpattern.ChainOfResponsibility;

public class LeaveRequest {
    private String employeeName;
    private int leaveDays;

    public LeaveRequest(String employeeName, int leaveDays) {
        this.employeeName = employeeName;
        this.leaveDays = leaveDays;
    }

    // Getter 方法
    public String getEmployeeName() {
        return employeeName;
    }

    public int getLeaveDays() {
        return leaveDays;
    }
}