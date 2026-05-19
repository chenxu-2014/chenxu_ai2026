package org.example.designpattern.ChainOfResponsibility;

public class DepartmentManager extends Handler {
    @Override
    public void handleRequest(LeaveRequest request) {
        if (request.getLeaveDays() > 2 && request.getLeaveDays() <= 5) {
            System.out.printf("部门经理批准 %s 的请假申请，天数：%d天%n",
                    request.getEmployeeName(), request.getLeaveDays());
        } else {
            // 自己无权处理，交给下一个处理者（总经理）
            if (nextHandler != null) {
                nextHandler.handleRequest(request);
            }
        }
    }
}