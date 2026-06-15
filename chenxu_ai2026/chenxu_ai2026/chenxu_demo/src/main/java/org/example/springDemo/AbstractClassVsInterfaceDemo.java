package org.example.springDemo;

/**
 * 演示抽象类与接口的区别
 * 场景：动物世界，有狗、鸟、飞机（无关动物，但都能飞）
 */


/**
 * 总结抽象类 vs 接口的区别（对应代码中的注释编号）：
 *
 * 1. 成员变量：抽象类可以有实例变量、类变量；----接口只能有 public static final 常量。
 * 2. 构造方法：抽象类可以有构造方法（子类调用初始化）；----接口不能有构造方法。
 * 3. 普通方法：抽象类可以有非抽象方法；----接口在Java8前只能有抽象方法，Java8后有default/static方法。
 * 4. 访问修饰符：抽象类方法可以使用任意修饰符（private/protected/public）；----接口方法默认public，不能用private/protected。
 * 5. 继承/实现：类只能单继承抽象类；----类可以实现多个接口。
 * 6. 设计理念：抽象类表达"is-a"（是什么），---接口表达"can-do"（能做什么）。
 * 7. 使用场景：需要共享代码、公共状态时用抽象类；-----定义契约、能力、多角色时用接口。
 * 8. 版本演进：Java8后接口功能增强，但依然不能持有状态（实例变量）；抽象类依然保留构造器和状态管理能力。
 */

public class AbstractClassVsInterfaceDemo {

    public static void main(String[] args) {
        // 1. 抽象类：体现 "is-a" 关系（狗是一种动物）
        Dog dog = new Dog("旺财");
        dog.eat();      // 继承的普通方法
        dog.sleep();    // 抽象方法的实现
        dog.bark();     // 子类特有方法

        // 2. 接口：体现 "can-do" 能力（鸟、飞机都能飞，但本质不同）
        Flyable bird = new Bird("小燕子");
        Flyable plane = new Plane("波音747");
        bird.fly();   // 接口方法调用
        bird.takeOff();
        plane.fly();
        plane.takeOff();
        Flyable.showType();

        // 3. 接口的多实现能力：一个人可以同时具备多种能力
        Person p = new Person("张三");
        p.eat();      // 继承自抽象类
        p.study();    // 实现接口1
        p.work();     // 实现接口2

        // 4. 抽象类与接口配合：模板方法模式
        System.out.println("=== 模板方法演示 ===");
        AbstractDataParser csvParser = new CsvDataParser();
        csvParser.parseAndProcess();  // 调用模板方法
    }
}

// ==================== 1. 抽象类（体现 is-a） ====================
abstract class AbstractAnimal {
    // 区别1：抽象类可以有成员变量（状态）
    protected String name;

    // 区别2：抽象类可以有构造方法（子类必须调用）
    public AbstractAnimal(String name) {
        this.name = name;
        System.out.println("抽象类构造器：初始化动物名字 " + name);
    }

    // 区别3：抽象类可以有普通方法（已实现，子类直接继承使用）
    public void eat() {
        System.out.println(name + " 正在吃东西（抽象类的普通方法）");
    }

    // 区别4：抽象类可以有抽象方法（子类必须实现，除非子类也是抽象类）
    public abstract void sleep();

    // 区别5：抽象类可以有 private 方法、protected 方法（权限灵活）
    protected void breathe() {
        System.out.println(name + " 在呼吸（protected方法）");
    }
}

// 具体子类继承抽象类（单继承，只能extends一个父类）
class Dog extends AbstractAnimal {
    public Dog(String name) {
        super(name);  // 必须调用父类构造器
    }

    @Override
    public void sleep() {
        System.out.println(name + " 趴着睡觉（实现抽象方法）");
    }

    // 子类特有方法
    public void bark() {
        System.out.println(name + " 汪汪叫");
    }
}

// ==================== 2. 接口（体现 can-do） ====================
interface Flyable {
    // 区别6：接口中成员变量默认是 public static final（常量）
    int MAX_SPEED = 1000;  // 等价于 public static final int MAX_SPEED = 1000;

    // 区别7：接口中方法默认是 public abstract（Java 8之前）
    void fly();  // 抽象方法

    // 区别8：Java 8 后接口可以有 default 方法（提供默认实现）
    default void takeOff() {
        System.out.println("准备起飞（接口 default 方法）");
    }

    // 区别9：Java 8 后接口可以有静态方法
    static void showType() {
        System.out.println("我是飞行能力接口（接口静态方法）");
    }
}

// 接口可以多实现，同时实现多个接口
class Bird extends AbstractAnimal implements Flyable {
    public Bird(String name) {
        super(name);
    }

    @Override
    public void sleep() {
        System.out.println(name + " 站着睡觉");
    }

    @Override
    public void fly() {
        System.out.println(name + " 扑腾翅膀飞行，最高速度 " + MAX_SPEED);
    }

    // 可以重写default方法（可选）
    @Override
    public void takeOff() {
        System.out.println(name + " 蹬腿起飞（重写default方法）");
    }
}

// 完全不相关的类（不是动物）也可以通过实现接口获得能力
class Plane implements Flyable {
    private String model;

    public Plane(String model) {
        this.model = model;
    }

    @Override
    public void fly() {
        System.out.println(model + " 喷气飞行，速度可达 " + MAX_SPEED + " km/h");
    }
    // 不重写 takeOff，则使用接口的 default 实现
}

// ==================== 3. 接口的多实现 vs 抽象类的单继承 ====================
interface Studyable {
    void study();
}

interface Workable {
    void work();
}

// 区别10：一个类可以实现多个接口（多实现），弥补单继承的不足
abstract class Human {
    protected String name;
    public Human(String name) { this.name = name; }
    public abstract void eat();
}

class Person extends Human implements Studyable, Workable {
    public Person(String name) { super(name); }

    @Override
    public void eat() {
        System.out.println(name + " 用筷子吃饭");
    }

    @Override
    public void study() {
        System.out.println(name + " 正在学习Java");
    }

    @Override
    public void work() {
        System.out.println(name + " 正在写代码");
    }
}

// ==================== 4. 抽象类 + 接口配合使用（模板方法模式） ====================
// 抽象类定义骨架，接口定义扩展能力
interface DataParser {
    void parse();
}

abstract class AbstractDataParser implements DataParser {
    // 模板方法：定义了算法骨架
    public final void parseAndProcess() {
        readData();      // 公共步骤
        parse();         // 不同实现（接口方法）
        processResult(); // 公共步骤
        logComplete();   // 公共步骤
    }

    private void readData() {
        System.out.println("读取数据（抽象类私有方法）");
    }

    protected void processResult() {
        System.out.println("处理解析结果（抽象类保护方法）");
    }

    private void logComplete() {
        System.out.println("记录日志完成");
    }
}

class CsvDataParser extends AbstractDataParser {
    @Override
    public void parse() {
        System.out.println("解析CSV格式数据");
    }
}
