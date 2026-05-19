package org.example.designpattern.Singleton;

public class SingletonDemo {
//    单例模式详解
//      单例模式是一种创建型设计模式，它确保一个类只有一个实例，并提供一个全局访问点来获取该实例。
//    单例模式的核心特点
//      1私有构造函数：防止外部通过new关键字创建实例
//      2静态私有实例变量：保存类的唯一实例
//      3静态公共方法：提供全局访问点来获取实例
    public static void main(String[] args) {
        EagerSingleton einstince=EagerSingleton.getInstance();//实现简单，线程安全但因为不是懒加载，如果实例很大且从未使用，会造成资源浪费
        EagerSingleton einstince1=EagerSingleton.getInstance();
        System.out.println("EagerSingleton==="+(einstince==einstince1));


        EnumSingleton enumInstince=EnumSingleton.INSTANCE;
        EnumSingleton enumInstince1=EnumSingleton.INSTANCE;
        System.out.println("EnumSingleton==="+(enumInstince==enumInstince1));

        LazySingleton linstince=LazySingleton.getInstance();//线程不安全，多线程环境下可能创建多个实例
        LazySingleton linstince1=LazySingleton.getInstance();
        System.out.println("LazySingleton==="+(linstince==linstince1));

        SynchronizedSingleton sinstince=SynchronizedSingleton.getInstance();//每次获取实例都需要同步，性能较差 线程安全
        SynchronizedSingleton sinstince1=SynchronizedSingleton.getInstance();
        System.out.println("SynchronizedSingleton==="+(sinstince==sinstince1));

        DoubleCheckedSingleton dinstince=DoubleCheckedSingleton.getInstance();// 双重检查锁：线程安全 性能好 但是实现复杂
        DoubleCheckedSingleton dinstince1=DoubleCheckedSingleton.getInstance();
        System.out.println("DoubleCheckedSingleton==="+(dinstince==dinstince1));


//            线程安全（由JVM类加载机制保证） 静态内部类
//            懒加载（只有在调用getInstance()时才会加载SingletonHolder类）
//            实现简单，无需同
//            但是无法传递参数初始化
        InnerClassSingleton iinstince=InnerClassSingleton.getInstance();
        InnerClassSingleton iinstince1=InnerClassSingleton.getInstance();
        System.out.println("InnerClassSingleton==="+(iinstince==iinstince1));


    }

}
