package org.example.designpattern.Singleton;

public enum EnumSingleton {
    INSTANCE;

    // 可以添加方法
    public void doSomething() {
        System.out.println("Doing something...");
    }
}