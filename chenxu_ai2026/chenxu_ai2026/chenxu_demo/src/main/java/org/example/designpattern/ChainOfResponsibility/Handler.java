package org.example.designpattern.ChainOfResponsibility;

public abstract class Handler {
    // 责任链上的下一个处理者
    protected Handler nextHandler;

    public void setNextHandler(Handler nextHandler) {
        this.nextHandler = nextHandler;
    }

    /**
     * 处理请假请求
     * @param request 请假请求对象
     */
    public abstract void handleRequest(LeaveRequest request);
}
