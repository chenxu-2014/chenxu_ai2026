package org.example.designpattern.Singleton;

public class EagerSingleton {
    // 类加载时就创建实例
    private static final EagerSingleton instance = new EagerSingleton();

    // 私有构造函数
    private EagerSingleton() {
        // 防止通过反射创建实例
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    // 全局访问点
    public static EagerSingleton getInstance() {
        return instance;
    }
}
