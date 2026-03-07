package cn.itzixiao.interview.designpattern;

/**
 * =====================================================================================
 * 23种设计模式总览
 * =====================================================================================
 * 
 * 设计模式分为三大类：
 * 
 * 一、创建型模式（Creational Patterns）- 5种
 * -------------------------------------------------------------------------------------
 * 关注对象的创建过程，将对象的创建和使用分离
 * 
 * 1. 单例模式（Singleton）：确保一个类只有一个实例
 * 2. 工厂方法模式（Factory Method）：定义创建对象的接口，让子类决定实例化
 * 3. 抽象工厂模式（Abstract Factory）：创建一系列相关对象的接口
 * 4. 建造者模式（Builder）：将复杂对象的构建与表示分离
 * 5. 原型模式（Prototype）：通过复制现有对象创建新对象
 * 
 * 二、结构型模式（Structural Patterns）- 7种
 * -------------------------------------------------------------------------------------
 * 关注类和对象的组合，形成更大的结构
 * 
 * 1. 适配器模式（Adapter）：将一个类的接口转换成另一个接口
 * 2. 桥接模式（Bridge）：将抽象与实现分离
 * 3. 组合模式（Composite）：将对象组合成树形结构
 * 4. 装饰器模式（Decorator）：动态地给对象添加功能
 * 5. 外观模式（Facade）：为子系统提供统一接口
 * 6. 享元模式（Flyweight）：共享细粒度对象
 * 7. 代理模式（Proxy）：控制对对象的访问
 * 
 * 三、行为型模式（Behavioral Patterns）- 11种
 * -------------------------------------------------------------------------------------
 * 关注对象之间的通信和职责分配
 * 
 * 1. 策略模式（Strategy）：定义算法族，使它们可以相互替换
 * 2. 模板方法模式（Template Method）：定义算法骨架，子类实现具体步骤
 * 3. 观察者模式（Observer）：定义一对多的依赖关系
 * 4. 迭代器模式（Iterator）：顺序访问聚合对象中的元素
 * 5. 责任链模式（Chain of Responsibility）：将请求沿链传递
 * 6. 命令模式（Command）：将请求封装为对象
 * 7. 备忘录模式（Memento）：保存和恢复对象状态
 * 8. 状态模式（State）：允许对象在状态改变时改变行为
 * 9. 访问者模式（Visitor）：在不改变类结构的情况下定义新操作
 * 10. 中介者模式（Mediator）：用中介对象封装对象交互
 * 11. 解释器模式（Interpreter）：定义语言的文法解释器
 * 
 * 四、设计原则（SOLID）
 * -------------------------------------------------------------------------------------
 * - 单一职责原则（SRP）：一个类只负责一项职责
 * - 开闭原则（OCP）：对扩展开放，对修改关闭
 * - 里氏替换原则（LSP）：子类可以替换父类
 * - 接口隔离原则（ISP）：接口要小而专一
 * - 依赖倒置原则（DIP）：依赖抽象不依赖具体
 */
public class DesignPatternDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║               23种设计模式完整示例代码                           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");

        printDesignPatternOverview();

        System.out.println("\n【运行方式】：");
        System.out.println("  每个模式都有独立的包和演示类，可单独运行：");
        System.out.println("  - 创建型模式包：cn.itzixiao.interview.designpattern.creational.*");
        System.out.println("  - 结构型模式包：cn.itzixiao.interview.designpattern.structural.*");
        System.out.println("  - 行为型模式包：cn.itzixiao.interview.designpattern.behavioral.*");
        System.out.println("\n  例如：运行 SingletonDemo.java 演示单例模式");
    }

    private static void printDesignPatternOverview() {
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        创建型模式（5种）                             │");
        System.out.println("├───────────────────┬─────────────────────────────────────────────────┤");
        System.out.println("│ 单例模式          │ 确保一个类只有一个实例                          │");
        System.out.println("│ 工厂方法模式      │ 子类决定实例化哪个类                            │");
        System.out.println("│ 抽象工厂模式      │ 创建一系列相关对象的接口                        │");
        System.out.println("│ 建造者模式        │ 分离复杂对象的构建与表示                        │");
        System.out.println("│ 原型模式          │ 通过复制创建新对象                              │");
        System.out.println("└───────────────────┴─────────────────────────────────────────────────┘");

        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        结构型模式（7种）                             │");
        System.out.println("├───────────────────┬─────────────────────────────────────────────────┤");
        System.out.println("│ 适配器模式        │ 转换接口使其兼容                                │");
        System.out.println("│ 桥接模式          │ 分离抽象与实现                                  │");
        System.out.println("│ 组合模式          │ 树形结构的部分-整体层次                          │");
        System.out.println("│ 装饰器模式        │ 动态添加功能                                    │");
        System.out.println("│ 外观模式          │ 为子系统提供统一接口                            │");
        System.out.println("│ 享元模式          │ 共享细粒度对象                                  │");
        System.out.println("│ 代理模式          │ 控制对象访问                                    │");
        System.out.println("└───────────────────┴─────────────────────────────────────────────────┘");

        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                        行为型模式（11种）                            │");
        System.out.println("├───────────────────┬─────────────────────────────────────────────────┤");
        System.out.println("│ 策略模式          │ 定义可互换的算法族                              │");
        System.out.println("│ 模板方法模式      │ 定义算法骨架，子类实现细节                      │");
        System.out.println("│ 观察者模式        │ 一对多的依赖关系                                │");
        System.out.println("│ 迭代器模式        │ 顺序访问聚合元素                                │");
        System.out.println("│ 责任链模式        │ 沿链传递请求                                    │");
        System.out.println("│ 命令模式          │ 将请求封装为对象                                │");
        System.out.println("│ 备忘录模式        │ 保存和恢复对象状态                              │");
        System.out.println("│ 状态模式          │ 状态改变时改变行为                              │");
        System.out.println("│ 访问者模式        │ 定义新的操作而不改变类结构                      │");
        System.out.println("│ 中介者模式        │ 封装对象间的交互                                │");
        System.out.println("│ 解释器模式        │ 定义语言的文法解释器                            │");
        System.out.println("└───────────────────┴─────────────────────────────────────────────────┘");
    }
}
