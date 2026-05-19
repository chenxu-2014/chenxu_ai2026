package org.example.designpattern.ChainOfResponsibility;

public class ProjectManager extends Handler {
    @Override
    public void handleRequest(LeaveRequest request) {
        if (request.getLeaveDays() <= 2) {
            System.out.printf("项目经理批准 %s 的请假申请，天数：%d天%n",
                    request.getEmployeeName(), request.getLeaveDays());
        } else {
            // 自己无权处理，交给下一个处理者（部门经理）
            if (nextHandler != null) {
                nextHandler.handleRequest(request);
            }
        }
    }
}