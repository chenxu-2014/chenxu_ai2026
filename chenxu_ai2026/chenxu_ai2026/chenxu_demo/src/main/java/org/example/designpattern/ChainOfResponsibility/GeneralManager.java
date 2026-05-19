package org.example.designpattern.ChainOfResponsibility;

public class GeneralManager extends Handler {
    @Override
    public void handleRequest(LeaveRequest request) {
        if (request.getLeaveDays() > 5) {
            System.out.printf("总经理批准 %s 的请假申请，天数：%d天%n",
                    request.getEmployeeName(), request.getLeaveDays());
        } else {
            // 理论上，他是最终节点，但也可以选择抛出异常或做其他处理
            System.out.println("请假天数过长，不予批准！");
            // 或者，如果链还没结束，可以继续传递（虽然这个例子中他是最后的）
            // if (nextHandler != null) {
            //     nextHandler.handleRequest(request);
            // }
        }
    }
}