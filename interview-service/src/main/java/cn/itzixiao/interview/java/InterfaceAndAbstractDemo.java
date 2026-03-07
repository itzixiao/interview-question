package cn.itzixiao.interview.java;

/**
 * =====================================================================================
 * 接口与抽象类详解
 * =====================================================================================
 * 
 * 一、接口（Interface）
 * -------------------------------------------------------------------------------------
 * 接口是一种抽象类型，是抽象方法的集合。接口以 interface 关键字声明。
 * 
 * 二、抽象类（Abstract Class）
 * -------------------------------------------------------------------------------------
 * 抽象类是不能被实例化的类，用 abstract 关键字修饰，可以包含抽象方法和具体方法。
 * 
 * 三、核心区别
 * -------------------------------------------------------------------------------------
 * | 特性           | 接口                          | 抽象类                      |
 * |----------------|-------------------------------|-----------------------------|
 * | 关键字         | interface                     | abstract class              |
 * | 多继承         | 一个类可实现多个接口          | 一个类只能继承一个抽象类    |
 * | 成员变量       | 只能是 public static final    | 可以有各种类型的成员变量    |
 * | 方法           | JDK8前全是抽象方法            | 可以有抽象和具体方法        |
 * | 构造方法       | 不能有                        | 可以有                      |
 * | 设计理念       | 定义行为契约                  | 代码复用+模板设计           |
 * 
 * 四、JDK8/9 接口新特性
 * -------------------------------------------------------------------------------------
 * JDK 8: default 方法（默认方法）、static 方法（静态方法）
 * JDK 9: private 方法（私有方法）
 */
public class InterfaceAndAbstractDemo {

    public static void main(String[] args) {
        System.out.println("========== 接口与抽象类详解 ==========\n");

        // 1. 接口演示
        demonstrateInterface();

        // 2. 抽象类演示
        demonstrateAbstractClass();

        // 3. 接口多实现演示
        demonstrateMultipleInterfaces();

        // 4. 高频面试题
        showInterviewQuestions();
    }

    /**
     * 接口演示
     */
    private static void demonstrateInterface() {
        System.out.println("【一、接口演示】\n");

        // 创建实现类对象
        Dog dog = new Dog();
        Cat cat = new Cat();

        // 调用接口方法
        dog.eat();
        dog.sleep();
        cat.eat();
        cat.sleep();

        // 调用默认方法
        System.out.println("\n调用接口默认方法：");
        dog.defaultMethod();
        cat.defaultMethod();

        // 调用静态方法
        System.out.println("\n调用接口静态方法：");
        AnimalInterface.staticMethod();

        System.out.println("\n接口特点：");
        System.out.println("  1. 所有方法默认是 public abstract（JDK8前）");
        System.out.println("  2. 所有变量默认是 public static final");
        System.out.println("  3. JDK8 支持 default 和 static 方法");
        System.out.println("  4. JDK9 支持 private 方法");
        System.out.println("  5. 不能实例化，没有构造方法");
    }

    /**
     * 抽象类演示
     */
    private static void demonstrateAbstractClass() {
        System.out.println("\n【二、抽象类演示】\n");

        // 创建具体子类
        Circle circle = new Circle(5.0);
        Rectangle rectangle = new Rectangle(4.0, 6.0);

        // 调用抽象方法（子类实现）
        System.out.println("圆形面积: " + circle.calculateArea());
        System.out.println("矩形面积: " + rectangle.calculateArea());

        // 调用具体方法（父类实现）
        System.out.println("\n调用抽象类具体方法：");
        circle.display();
        rectangle.display();

        // 调用抽象类构造方法
        System.out.println("\n抽象类特点：");
        System.out.println("  1. 用 abstract 修饰，不能实例化");
        System.out.println("  2. 可以有抽象方法和具体方法");
        System.out.println("  3. 可以有构造方法（供子类调用）");
        System.out.println("  4. 可以有各种访问修饰符的成员变量");
        System.out.println("  5. 子类必须实现所有抽象方法（除非子类也是抽象类）");
    }

    /**
     * 接口多实现演示
     */
    private static void demonstrateMultipleInterfaces() {
        System.out.println("\n【三、接口多实现演示】\n");

        SmartPhone phone = new SmartPhone();
        phone.call();           // Phone 接口方法
        phone.takePhoto();      // Camera 接口方法
        phone.playMusic();      // MusicPlayer 接口方法
        phone.charge();         // 默认方法

        System.out.println("\n多实现特点：");
        System.out.println("  1. 一个类可以实现多个接口");
        System.out.println("  2. 解决了 Java 单继承的限制");
        System.out.println("  3. 实现类必须实现所有接口的抽象方法");
    }

    /**
     * 高频面试题
     */
    private static void showInterviewQuestions() {
        System.out.println("\n【四、高频面试题】\n");

        System.out.println("Q1: 接口和抽象类的区别？");
        System.out.println("答案：");
        System.out.println("  1. 继承关系：类只能单继承抽象类，但可实现多个接口");
        System.out.println("  2. 成员变量：接口只能是 public static final，抽象类无限制");
        System.out.println("  3. 方法实现：接口JDK8前只能有抽象方法，抽象类可以有具体方法");
        System.out.println("  4. 构造方法：接口没有，抽象类可以有");
        System.out.println("  5. 设计理念：接口是行为契约，抽象类是代码复用");

        System.out.println("\nQ2: 什么时候用接口？什么时候用抽象类？");
        System.out.println("答案：");
        System.out.println("  用接口：");
        System.out.println("    - 需要定义行为规范，不关心具体实现");
        System.out.println("    - 需要多继承场景");
        System.out.println("    - 功能之间没有关联性");
        System.out.println("  用抽象类：");
        System.out.println("    - 需要代码复用，子类有公共逻辑");
        System.out.println("    - 需要定义成员变量");
        System.out.println("    - 子类之间有层次关系（is-a 关系）");

        System.out.println("\nQ3: JDK8 接口新增的 default 方法有什么用？");
        System.out.println("答案：");
        System.out.println("  1. 向后兼容：为现有接口添加新方法而不破坏实现类");
        System.out.println("  2. 代码复用：提供默认实现，减少重复代码");
        System.out.println("  示例：Collection 接口的 forEach、stream 等方法");

        System.out.println("\nQ4: 一个类实现多个接口，接口有相同的默认方法怎么办？");
        System.out.println("答案：");
        System.out.println("  实现类必须重写该方法，解决冲突");
        System.out.println("  可以使用 InterfaceName.super.method() 指定调用哪个接口的方法");

        System.out.println("\nQ5: 抽象类可以没有抽象方法吗？");
        System.out.println("答案：");
        System.out.println("  可以。抽象类可以没有抽象方法");
        System.out.println("  但有抽象方法的类必须是抽象类");
        System.out.println("  用途：防止直接实例化，强制通过子类使用");

        System.out.println("\nQ6: 接口可以继承接口吗？抽象类可以实现接口吗？");
        System.out.println("答案：");
        System.out.println("  接口可以继承接口（支持多继承）");
        System.out.println("  抽象类可以实现接口");
        System.out.println("  抽象类可以继承具体类");
        System.out.println("  抽象类可以继承抽象类");

        System.out.println("\nQ7: 普通类和抽象类有什么区别？");
        System.out.println("答案：");
        System.out.println("  普通类：可以实例化，不能有抽象方法");
        System.out.println("  抽象类：不能实例化，可以有抽象方法");

        System.out.println("\nQ8: Java 为什么要设计单继承？");
        System.out.println("答案：");
        System.out.println("  1. 避免钻石问题：多个父类有相同方法时的二义性");
        System.out.println("  2. 简化设计：降低复杂度");
        System.out.println("  3. 接口多实现弥补了单继承的不足");
    }
}

/**
 * =====================================================================================
 * 一、接口示例
 * =====================================================================================
 */
interface AnimalInterface {
    
    // 常量：默认 public static final
    String TYPE = "动物";

    // 抽象方法：默认 public abstract
    void eat();
    void sleep();

    // JDK8 新特性：默认方法（有具体实现）
    default void defaultMethod() {
        System.out.println("    [接口默认方法] 这是 JDK8 新增的 default 方法");
        // JDK9+ 可以调用私有方法，JDK8 不支持
        // privateMethod();
    }

    // JDK8 新特性：静态方法
    static void staticMethod() {
        System.out.println("    [接口静态方法] 这是 JDK8 新增的 static 方法");
    }

    // JDK9 新特性：私有方法（JDK8 不支持，注释掉）
    // private void privateMethod() {
    //     System.out.println("    [接口私有方法] 这是 JDK9 新增的 private 方法");
    // }
}

/**
 * 接口实现类：狗
 */
class Dog implements AnimalInterface {
    @Override
    public void eat() {
        System.out.println("    [狗] 吃骨头");
    }

    @Override
    public void sleep() {
        System.out.println("    [狗] 睡觉");
    }
}

/**
 * 接口实现类：猫
 */
class Cat implements AnimalInterface {
    @Override
    public void eat() {
        System.out.println("    [猫] 吃鱼");
    }

    @Override
    public void sleep() {
        System.out.println("    [猫] 睡觉");
    }
}

/**
 * =====================================================================================
 * 二、抽象类示例
 * =====================================================================================
 */
abstract class Shape {
    // 成员变量：可以是各种访问修饰符
    protected String color;
    private String name;

    // 构造方法：供子类调用
    public Shape(String name) {
        this.name = name;
        this.color = "白色";
        System.out.println("    [抽象类构造] 创建图形: " + name);
    }

    // 抽象方法：子类必须实现
    public abstract double calculateArea();

    // 具体方法：子类可以直接使用
    public void display() {
        System.out.println("    [图形信息] 名称: " + name + ", 颜色: " + color + 
                ", 面积: " + calculateArea());
    }

    public void setColor(String color) {
        this.color = color;
    }
}

/**
 * 抽象类子类：圆形
 */
class Circle extends Shape {
    private double radius;

    public Circle(double radius) {
        super("圆形");  // 调用抽象类构造方法
        this.radius = radius;
    }

    @Override
    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}

/**
 * 抽象类子类：矩形
 */
class Rectangle extends Shape {
    private double width;
    private double height;

    public Rectangle(double width, double height) {
        super("矩形");
        this.width = width;
        this.height = height;
    }

    @Override
    public double calculateArea() {
        return width * height;
    }
}

/**
 * =====================================================================================
 * 三、接口多实现示例
 * =====================================================================================
 */

// 接口1：手机功能
interface Phone {
    void call();
    
    default void charge() {
        System.out.println("    [充电] 使用默认充电方式");
    }
}

// 接口2：相机功能
interface Camera {
    void takePhoto();
    
    default void charge() {
        System.out.println("    [充电] 使用相机充电方式");
    }
}

// 接口3：音乐播放功能
interface MusicPlayer {
    void playMusic();
}

/**
 * 多实现类：智能手机
 * 实现了三个接口
 */
class SmartPhone implements Phone, Camera, MusicPlayer {
    
    @Override
    public void call() {
        System.out.println("    [手机] 打电话");
    }

    @Override
    public void takePhoto() {
        System.out.println("    [相机] 拍照");
    }

    @Override
    public void playMusic() {
        System.out.println("    [音乐] 播放音乐");
    }

    /**
     * 两个接口有相同的默认方法，必须重写
     */
    @Override
    public void charge() {
        System.out.println("    [智能手机] 使用快充");
        // 可以指定调用哪个接口的默认方法
        // Phone.super.charge();
        // Camera.super.charge();
    }
}
