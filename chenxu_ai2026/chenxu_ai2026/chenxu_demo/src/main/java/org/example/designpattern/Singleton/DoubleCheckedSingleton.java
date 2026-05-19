package org.example.designpattern.Singleton;

public class DoubleCheckedSingleton {
    // 使用volatile防止指令重排序
    private static volatile DoubleCheckedSingleton instance;

    private DoubleCheckedSingleton() {}

    public static DoubleCheckedSingleton getInstance() {
        if (instance == null) { // 第一次检查
            synchronized (DoubleCheckedSingleton.class) {
                if (instance == null) { // 第二次检查
                    instance = new DoubleCheckedSingleton();
                }
            }
        }
        return instance;
    }
}