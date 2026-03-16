package cn.itzixiao.interview.designpattern.structural.bridge;

/**
 * =====================================================================================
 * 桥接模式（Bridge Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 将抽象部分与它的实现部分分离，使它们都可以独立地变化。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 抽象（Abstraction）：定义抽象接口，持有实现接口的引用
 * 2. 扩展抽象（Refined Abstraction）：扩展抽象接口
 * 3. 实现（Implementor）：定义实现接口
 * 4. 具体实现（Concrete Implementor）：实现实现接口
 * <p>
 * 三、为什么需要桥接模式？
 * -------------------------------------------------------------------------------------
 * 问题场景：形状和颜色的组合
 * - 如果用继承：Circle-Red, Circle-Blue, Square-Red, Square-Blue...
 * - 类的数量 = 形状数 × 颜色数（类爆炸）
 * <p>
 * 使用桥接模式：
 * - 抽象：形状（Shape）
 * - 实现：颜色（Color）
 * - 类的数量 = 形状数 + 颜色数（解耦）
 * <p>
 * 四、桥接模式 vs 适配器模式
 * -------------------------------------------------------------------------------------
 * | 特性       | 桥接模式                   | 适配器模式             |
 * |------------|----------------------------|------------------------|
 * | 目的       | 分离抽象和实现             | 接口转换               |
 * | 时机       | 设计阶段                   | 维护阶段               |
 * | 关系       | 抽象和实现预先分离         | 解决已有接口不兼容     |
 * <p>
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - JDBC 驱动：JDBC API（抽象）与各种数据库驱动（实现）
 * - Java AWT：组件（抽象）与平台实现（实现）
 * - 日志框架：SLF4J（抽象）与 Logback/Log4j（实现）
 */
public class BridgeDemo {

    public static void main(String[] args) {
        System.out.println("========== 桥接模式（Bridge Pattern）==========\n");

        System.out.println("【场景：形状与颜色的组合】\n");

        // 圆形 + 红色
        System.out.println("1. 红色圆形：");
        Color red = new RedColor();
        Shape redCircle = new Circle(red);
        redCircle.draw();

        // 圆形 + 蓝色
        System.out.println("\n2. 蓝色圆形：");
        Color blue = new BlueColor();
        Shape blueCircle = new Circle(blue);
        blueCircle.draw();

        // 矩形 + 红色
        System.out.println("\n3. 红色矩形：");
        Shape redRectangle = new Rectangle(red);
        redRectangle.draw();

        // 矩形 + 蓝色
        System.out.println("\n4. 蓝色矩形：");
        Shape blueRectangle = new Rectangle(blue);
        blueRectangle.draw();

        System.out.println("\n【类数量分析】：");
        System.out.println("  - 不使用桥接：形状(2) × 颜色(2) = 4 个类");
        System.out.println("  - 使用桥接：形状(2) + 颜色(2) = 4 个类");
        System.out.println("  - 如果新增形状或颜色，桥接模式优势更明显");
        System.out.println("  - 例如：3种形状 + 4种颜色 = 3+4=7 个类（桥接）vs 12 个类（继承）");

        System.out.println("\n【模式分析】：");
        System.out.println("  - 分离抽象和实现，两者独立变化");
        System.out.println("  - 使用组合替代继承，避免类爆炸");
        System.out.println("  - 符合开闭原则：扩展新形状或颜色无需修改现有代码");
        System.out.println("  - 符合单一职责原则：抽象和实现各司其职");
    }
}

/**
 * =====================================================================================
 * 实现接口（Implementor）
 * =====================================================================================
 * 定义实现的接口，提供基本操作
 */
interface Color {
    /**
     * 应用颜色
     *
     * @param shapeType 形状类型
     */
    void applyColor(String shapeType);
}

/**
 * =====================================================================================
 * 具体实现：红色
 * =====================================================================================
 */
class RedColor implements Color {
    @Override
    public void applyColor(String shapeType) {
        System.out.println("    绘制红色" + shapeType + "，边框为红色，填充为浅红色");
    }
}

/**
 * =====================================================================================
 * 具体实现：蓝色
 * =====================================================================================
 */
class BlueColor implements Color {
    @Override
    public void applyColor(String shapeType) {
        System.out.println("    绘制蓝色" + shapeType + "，边框为蓝色，填充为浅蓝色");
    }
}

/**
 * =====================================================================================
 * 抽象（Abstraction）
 * =====================================================================================
 * 定义抽象接口，持有实现接口的引用（桥接点）
 */
abstract class Shape {
    // 桥接：持有实现接口的引用
    protected Color color;

    public Shape(Color color) {
        this.color = color;
    }

    /**
     * 绘制形状
     */
    abstract void draw();
}

/**
 * =====================================================================================
 * 扩展抽象：圆形
 * =====================================================================================
 */
class Circle extends Shape {

    public Circle(Color color) {
        super(color);
    }

    @Override
    void draw() {
        System.out.println("    创建圆形...");
        color.applyColor("圆形");
    }
}

/**
 * =====================================================================================
 * 扩展抽象：矩形
 * =====================================================================================
 */
class Rectangle extends Shape {

    public Rectangle(Color color) {
        super(color);
    }

    @Override
    void draw() {
        System.out.println("    创建矩形...");
        color.applyColor("矩形");
    }
}
