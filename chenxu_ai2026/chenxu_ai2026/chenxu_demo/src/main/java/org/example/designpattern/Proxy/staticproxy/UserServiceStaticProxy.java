package org.example.designpattern.Proxy.staticproxy;

import org.example.designpattern.Proxy.UserService;

// 代理类，同样实现接口
public class UserServiceStaticProxy implements UserService {

    // 持有目标对象的引用
    private UserService target;

    public UserServiceStaticProxy(UserService target) {
        this.target = target;
    }

    @Override
    public void addUser(String name) {
        // 【前置增强】- 代理添加的功能
        long startTime = System.currentTimeMillis();
        System.out.println("--- 开始执行 addUser 方法 ---");

        // 调用目标对象的方法
        target.addUser(name);

        // 【后置增强】- 代理添加的功能
        long endTime = System.currentTimeMillis();
        System.out.println("--- addUser 方法执行完成，耗时: " + (endTime - startTime) + "ms ---");
    }

    @Override
    public void deleteUser(String id) {
        // 类似 addUser 的逻辑，可以添加日志、事务等
        System.out.println("--- 开始执行 deleteUser 方法 ---");
        target.deleteUser(id);
        System.out.println("--- deleteUser 方法执行完成 ---");
    }
}