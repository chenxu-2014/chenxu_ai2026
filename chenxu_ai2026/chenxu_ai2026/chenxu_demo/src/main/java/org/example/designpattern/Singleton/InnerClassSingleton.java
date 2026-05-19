package org.example.designpattern.Singleton;

public class InnerClassSingleton {
    private InnerClassSingleton() {}

    // 静态内部类
    private static class SingletonHolder {
        private static final InnerClassSingleton INSTANCE = new InnerClassSingleton();
    }

    public static InnerClassSingleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}