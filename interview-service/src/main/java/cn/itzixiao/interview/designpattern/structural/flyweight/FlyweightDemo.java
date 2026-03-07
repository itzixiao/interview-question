package cn.itzixiao.interview.designpattern.structural.flyweight;

import java.util.HashMap;
import java.util.Map;

/**
 * =====================================================================================
 * 享元模式（Flyweight Pattern）
 * =====================================================================================
 * 
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 运用共享技术有效地支持大量细粒度的对象。
 * 
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 内部状态（Intrinsic State）：可共享的状态，存储在享元对象中
 * 2. 外部状态（Extrinsic State）：不可共享的状态，由客户端维护
 * 3. 享元工厂（Flyweight Factory）：创建和管理享元对象
 * 
 * 三、为什么需要享元模式？
 * -------------------------------------------------------------------------------------
 * 问题：假设需要绘制 100 万个圆，每个圆有颜色、位置、大小属性
 * 
 * 不使用享元模式：
 * - 每个圆创建一个对象，包含所有属性
 * - 内存占用 = 100万 × (颜色 + 位置 + 大小) = 巨大
 * 
 * 使用享元模式：
 * - 颜色作为内部状态（共享）：假设只有 5 种颜色，创建 5 个享元对象
 * - 位置、大小作为外部状态（不共享）：客户端传入
 * - 内存占用 = 5 × 颜色 + 100万 × (位置 + 大小的引用) = 显著减少
 * 
 * 四、享元模式结构
 * -------------------------------------------------------------------------------------
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  Client                                                             │
 * │     │                                                               │
 * │     ↓                                                               │
 * │  FlyweightFactory ──────→ Flyweight Pool（享元池）                  │
 * │     │                       ├── Flyweight(红)                       │
 * │     │                       ├── Flyweight(绿)                       │
 * │     │                       └── Flyweight(蓝)                       │
 * │     │                                                               │
 * │     └─────→ Flyweight（享元接口）                                   │
 * │                 │                                                   │
 * │                 └─── ConcreteFlyweight（具体享元）                  │
 * │                       - intrinsicState（内部状态，共享）            │
 * │                       - operation(extrinsicState)                  │
 * └─────────────────────────────────────────────────────────────────────┘
 * 
 * 五、应用场景
 * -------------------------------------------------------------------------------------
 * - Integer.valueOf()：-128 到 127 的缓存
 * - String 常量池：字符串常量共享
 * - 线程池：线程的复用
 * - 连接池：数据库连接的复用
 */
public class FlyweightDemo {

    public static void main(String[] args) {
        System.out.println("========== 享元模式（Flyweight Pattern）==========\n");

        System.out.println("【场景：围棋棋子】");
        System.out.println("棋盘上有大量棋子，但颜色只有黑白两种\n");

        // 创建棋子工厂
        ChessPieceFactory factory = new ChessPieceFactory();

        // 模拟棋盘上的多个棋子
        System.out.println("【创建棋子】：");

        // 黑子
        ChessPiece black1 = factory.getChessPiece("黑");
        black1.display(10, 20);  // 外部状态：位置

        ChessPiece black2 = factory.getChessPiece("黑");
        black2.display(15, 25);

        // 白子
        ChessPiece white1 = factory.getChessPiece("白");
        white1.display(10, 21);

        ChessPiece white2 = factory.getChessPiece("白");
        white2.display(15, 26);

        // 再次获取黑子，验证共享
        ChessPiece black3 = factory.getChessPiece("黑");
        black3.display(20, 30);

        System.out.println("\n【享元对象数量】：");
        System.out.println("  享元池中对象数量: " + factory.getPoolSize());
        System.out.println("  黑子和白子各创建一个享元对象，实现共享");

        System.out.println("\n【对象引用对比】：");
        System.out.println("  black1 == black2: " + (black1 == black2));
        System.out.println("  black1 == black3: " + (black1 == black3));
        System.out.println("  black1 == white1: " + (black1 == white1));

        // Java 中的享元模式
        System.out.println("\n【Java 中的享元模式】：");
        System.out.println("1. Integer 缓存：");
        Integer i1 = Integer.valueOf(100);  // 在缓存范围内
        Integer i2 = Integer.valueOf(100);
        System.out.println("    Integer.valueOf(100) == Integer.valueOf(100): " + (i1 == i2));

        Integer i3 = Integer.valueOf(200);  // 超出缓存范围
        Integer i4 = Integer.valueOf(200);
        System.out.println("    Integer.valueOf(200) == Integer.valueOf(200): " + (i3 == i4));

        System.out.println("\n2. String 常量池：");
        String s1 = "hello";
        String s2 = "hello";
        System.out.println("    \"hello\" == \"hello\": " + (s1 == s2));

        System.out.println("\n【模式分析】：");
        System.out.println("  - 共享细粒度对象，减少内存占用");
        System.out.println("  - 区分内部状态（共享）和外部状态（不共享）");
        System.out.println("  - 享元工厂管理享元池，实现对象复用");
        System.out.println("  - 适用于大量相似对象的场景");
    }
}

/**
 * =====================================================================================
 * 享元接口
 * =====================================================================================
 */
interface ChessPiece {
    /**
     * 显示棋子
     * @param x 外部状态：x坐标
     * @param y 外部状态：y坐标
     */
    void display(int x, int y);

    /**
     * 获取颜色
     */
    String getColor();
}

/**
 * =====================================================================================
 * 具体享元
 * =====================================================================================
 * 内部状态：颜色（color）
 * 外部状态：位置（x, y），由客户端传入
 */
class ConcreteChessPiece implements ChessPiece {
    // 内部状态：可共享
    private String color;

    public ConcreteChessPiece(String color) {
        this.color = color;
        System.out.println("    创建" + color + "子享元对象");
    }

    @Override
    public void display(int x, int y) {
        System.out.println("    " + color + "子位置: (" + x + ", " + y + ")");
    }

    @Override
    public String getColor() {
        return color;
    }
}

/**
 * =====================================================================================
 * 享元工厂
 * =====================================================================================
 * 管理享元池，确保享元对象被共享
 */
class ChessPieceFactory {
    // 享元池：存储已创建的享元对象
    private Map<String, ChessPiece> pool = new HashMap<>();

    /**
     * 获取享元对象
     * 如果已存在则返回，否则创建新对象
     */
    public ChessPiece getChessPiece(String color) {
        ChessPiece piece = pool.get(color);
        if (piece == null) {
            piece = new ConcreteChessPiece(color);
            pool.put(color, piece);
        }
        return piece;
    }

    /**
     * 获取享元池大小
     */
    public int getPoolSize() {
        return pool.size();
    }
}
