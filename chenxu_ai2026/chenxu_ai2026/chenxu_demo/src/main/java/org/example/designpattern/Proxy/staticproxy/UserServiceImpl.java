package org.example.designpattern.Proxy.staticproxy;

import org.example.designpattern.Proxy.UserService;

// 真实对象
public class UserServiceImpl implements UserService {
    @Override
    public void addUser(String name) {
        System.out.println("添加用户: " + name);
        // 模拟业务逻辑
        // try {
        //     Thread.sleep(1000);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }
    }

    @Override
    public void deleteUser(String id) {
        System.out.println("删除用户，ID为: " + id);
    }
}