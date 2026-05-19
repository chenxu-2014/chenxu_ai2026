package org.example.designpattern.Proxy.dynamicproxy;

import org.example.designpattern.Proxy.UserService;
import org.example.designpattern.Proxy.staticproxy.UserServiceImpl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class JdkDynamicProxyDemo {
    public static void main(String[] args) {

        // 1. 创建真实目标对象
        UserService realService = new UserServiceImpl();

        // 2. 创建 InvocationHandler，并传入目标对象
        InvocationHandler handler = new LoggingInvocationHandler(realService);

        // 3. 动态创建代理对象
        // 参数：类加载器、代理类要实现的接口列表、调用处理器
        UserService proxy = (UserService) Proxy.newProxyInstance(
                realService.getClass().getClassLoader(), // 使用目标类的类加载器
                realService.getClass().getInterfaces(),  // 获取目标类实现的所有接口
                handler                                  // 自定义的调用处理器
        );

        // 4. 通过代理对象调用方法
        proxy.addUser("李四（JDK Proxy）");
        System.out.println();
        proxy.deleteUser("456");

        // 可选：查看代理类的名字
        System.out.println("代理对象的类型: " + proxy.getClass().getName());
    }
}
