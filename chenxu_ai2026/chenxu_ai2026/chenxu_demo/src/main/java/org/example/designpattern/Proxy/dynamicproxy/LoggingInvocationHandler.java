package org.example.designpattern.Proxy.dynamicproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class LoggingInvocationHandler implements InvocationHandler {

    // 目标对象（被代理的对象）
    private final Object target;

    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }

    /**
     * @param proxy  代理对象本身（通常不太使用）
     * @param method 正在被调用的方法对象
     * @param args   调用方法时传入的参数
     * @return       方法的返回值
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 【前置增强】
        long startTime = System.currentTimeMillis();
        System.out.println("【JDK Proxy】开始执行方法: " + method.getName());

        // 核心：利用反射调用目标对象的真实方法
        Object result = method.invoke(target, args);

        // 【后置增强】
        long endTime = System.currentTimeMillis();
        System.out.println("【JDK Proxy】方法 " + method.getName() + " 执行完成，耗时: " + (endTime - startTime) + "ms");

        return result; // 返回方法执行结果
    }
}
