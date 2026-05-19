package org.example.designpattern.ChainOfResponsibility;
//优点：
//        1降低耦合度：请求发送者无需知道谁处理了请求，甚至不知道链的结构。
//        2动态组合职责：可以在运行时动态地改变链内处理者的先后次序，也可以增加或删除处理者，非常灵活。
//        3符合开闭原则：可以轻松地增加新的具体处理者来扩展功能，而无需修改现有代码。
//缺点：
//        1请求不一定被处理：如果链配置不当，请求可能到达链的末端都无法被处理。
//        2性能问题：链过长可能会影响性能，特别是在递归调用深度很大时。
//        3调试困难：请求的传递被隐藏在了链中，调试时可能不太直观，不容易跟踪请求的处理过程。
//规则：
//        ●请假天数 <= 2天，项目经理审批。
//        ●请假天数 2 < days <= 5天，部门经理审批。
//        ●请假天数 > 5天，总经理审批。
public class ChainOfResponsibilityDemo {
    public static void main(String[] args) {
        // 1. 创建处理者
        Handler projectManager = new ProjectManager();
        Handler departmentManager = new DepartmentManager();
        Handler generalManager = new GeneralManager();

        // 2. 组装责任链：项目经理 -> 部门经理 -> 总经理
        projectManager.setNextHandler(departmentManager);
        departmentManager.setNextHandler(generalManager);
        // 注意：generalManager 是链的末端，没有下一个处理者

        // 3. 创建测试请求
        LeaveRequest request1 = new LeaveRequest("张三", 1);
        LeaveRequest request2 = new LeaveRequest("李四", 4);
        LeaveRequest request3 = new LeaveRequest("王五", 7);
        LeaveRequest request4 = new LeaveRequest("赵六", 0); // 边缘情况

        // 4. 将请求提交给链的起始节点（项目经理）
        System.out.println("===== 张三请假1天 =====");
        projectManager.handleRequest(request1);

        System.out.println("\n===== 李四请假4天 =====");
        projectManager.handleRequest(request2);

        System.out.println("\n===== 王五请假7天 =====");
        projectManager.handleRequest(request3);

        System.out.println("\n===== 赵六请假0天 =====");
        projectManager.handleRequest(request4);
    }
}