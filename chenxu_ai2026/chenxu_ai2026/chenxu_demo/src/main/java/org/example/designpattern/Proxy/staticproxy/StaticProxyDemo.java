package org.example.designpattern.Proxy.staticproxy;

import org.example.designpattern.Proxy.UserService;

public class StaticProxyDemo {
    public static void main(String[] args) {
        // 创建真实对象
        UserService realService = new UserServiceImpl();

        // 创建代理对象，并传入真实对象
        UserService proxy = new UserServiceStaticProxy(realService);

        // 通过代理对象调用方法
        proxy.addUser("张三");
        System.out.println();
        proxy.deleteUser("123");
    }
}