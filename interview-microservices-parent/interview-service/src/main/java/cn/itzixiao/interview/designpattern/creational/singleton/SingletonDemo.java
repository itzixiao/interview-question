package cn.itzixiao.interview.designpattern.creational.singleton;

/**
 * =====================================================================================
 * 单例模式（Singleton Pattern）
 * =====================================================================================
 * <p>
 * 一、定义
 * -------------------------------------------------------------------------------------
 * 确保一个类只有一个实例，并提供一个全局访问点。
 * <p>
 * 二、核心思想
 * -------------------------------------------------------------------------------------
 * 1. 私有构造函数：防止外部通过 new 创建实例
 * 2. 私有静态实例：保存唯一的实例
 * 3. 公有静态方法：提供全局访问点
 * <p>
 * 三、应用场景
 * -------------------------------------------------------------------------------------
 * - 配置管理器（Configuration Manager）
 * - 日志记录器（Logger）
 * - 数据库连接池（Database Connection Pool）
 * - 线程池（Thread Pool）
 * - 缓存（Cache）
 * <p>
 * 四、实现方式对比
 * -------------------------------------------------------------------------------------
 * | 实现方式           | 线程安全 | 懒加载 | 性能 | 推荐度 |
 * |--------------------|----------|--------|------|--------|
 * | 饿汉式             | 是       | 否     | 高   | ★★★    |
 * | 懒汉式（同步方法） | 是       | 是     | 低   | ★★     |
 * | 双重检查锁         | 是       | 是     | 高   | ★★★★  |
 * | 静态内部类         | 是       | 是     | 高   | ★★★★★ |
 * | 枚举               | 是       | 否     | 高   | ★★★★★ |
 */
public class SingletonDemo {

    public static void main(String[] args) {
        System.out.println("========== 单例模式（Singleton Pattern）==========\n");

        // 1. 饿汉式
        System.out.println("【1. 饿汉式】");
        EagerSingleton eager1 = EagerSingleton.getInstance();
        EagerSingleton eager2 = EagerSingleton.getInstance();
        System.out.println("  是否同一实例: " + (eager1 == eager2));
        System.out.println("  特点: 类加载时就创建实例，线程安全，但可能造成资源浪费\n");

        // 2. 懒汉式（同步方法）
        System.out.println("【2. 懒汉式（同步方法）】");
        LazySingleton lazy1 = LazySingleton.getInstance();
        LazySingleton lazy2 = LazySingleton.getInstance();
        System.out.println("  是否同一实例: " + (lazy1 == lazy2));
        System.out.println("  特点: 懒加载，但 synchronized 导致性能较差\n");

        // 3. 双重检查锁
        System.out.println("【3. 双重检查锁（Double-Checked Locking）】");
        DoubleCheckedLockingSingleton dcl1 = DoubleCheckedLockingSingleton.getInstance();
        DoubleCheckedLockingSingleton dcl2 = DoubleCheckedLockingSingleton.getInstance();
        System.out.println("  是否同一实例: " + (dcl1 == dcl2));
        System.out.println("  特点: 懒加载 + 线程安全 + 高性能，volatile 防止指令重排序\n");

        // 4. 静态内部类（推荐）
        System.out.println("【4. 静态内部类（推荐方式）】");
        StaticInnerClassSingleton inner1 = StaticInnerClassSingleton.getInstance();
        StaticInnerClassSingleton inner2 = StaticInnerClassSingleton.getInstance();
        System.out.println("  是否同一实例: " + (inner1 == inner2));
        System.out.println("  特点: 利用类加载机制保证线程安全，懒加载，代码简洁\n");

        // 5. 枚举（最佳实践）
        System.out.println("【5. 枚举单例（最佳实践）】");
        EnumSingleton enum1 = EnumSingleton.INSTANCE;
        EnumSingleton enum2 = EnumSingleton.INSTANCE;
        System.out.println("  是否同一实例: " + (enum1 == enum2));
        System.out.println("  特点: 天然防止反射攻击和序列化破坏，代码最简洁\n");

        // 6. 防止反射破坏单例测试
        System.out.println("【6. 单例的安全性分析】");
        System.out.println("  反射攻击: 枚举单例天然免疫，其他方式需要额外处理");
        System.out.println("  序列化破坏: 枚举单例天然免疫，其他方式需实现 readResolve() 方法");
        System.out.println();

        System.out.println("【选择建议】");
        System.out.println("  - 不需要懒加载: 枚举单例（最佳）");
        System.out.println("  - 需要懒加载: 静态内部类（推荐）");
        System.out.println("  - 需要延迟初始化参数: 双重检查锁");
    }
}

/**
 * =====================================================================================
 * 1. 饿汉式单例
 * =====================================================================================
 * <p>
 * 原理：类加载时就创建实例
 * 优点：实现简单，无线程安全问题
 * 缺点：类加载时就初始化，可能造成资源浪费
 */
class EagerSingleton {

    // 类加载时就创建实例（final 保证不可变）
    private static final EagerSingleton INSTANCE = new EagerSingleton();

    // 私有构造函数，防止外部创建实例
    private EagerSingleton() {
        System.out.println("    [饿汉式] 实例已创建");
    }

    // 提供全局访问点
    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}

/**
 * =====================================================================================
 * 2. 懒汉式单例（同步方法）
 * =====================================================================================
 * <p>
 * 原理：首次使用时才创建实例
 * 优点：懒加载，节约资源
 * 缺点：synchronized 锁住整个方法，性能差
 */
class LazySingleton {

    private static LazySingleton instance;

    private LazySingleton() {
        System.out.println("    [懒汉式] 实例已创建");
    }

    // synchronized 保证线程安全，但性能较差
    public static synchronized LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }
}

/**
 * =====================================================================================
 * 3. 双重检查锁单例（Double-Checked Locking）
 * =====================================================================================
 * <p>
 * 原理：两次检查 + volatile + synchronized
 * <p>
 * 关键点：
 * 1. volatile 防止指令重排序
 * - instance = new Singleton() 不是原子操作，分为三步：
 * a. 分配内存空间
 * b. 初始化对象
 * c. 将 instance 指向分配的内存地址
 * - 指令重排序可能导致 b 和 c 交换顺序，使其他线程获得未初始化的对象
 * <p>
 * 2. 两次检查
 * - 第一次检查：避免不必要的同步
 * - 第二次检查：确保只创建一个实例
 * <p>
 * 优点：懒加载 + 线程安全 + 高性能
 */
class DoubleCheckedLockingSingleton {

    // volatile 防止指令重排序，保证可见性
    private static volatile DoubleCheckedLockingSingleton instance;

    private DoubleCheckedLockingSingleton() {
        System.out.println("    [双重检查锁] 实例已创建");
    }

    public static DoubleCheckedLockingSingleton getInstance() {
        // 第一次检查：避免不必要的同步（性能优化）
        if (instance == null) {
            synchronized (DoubleCheckedLockingSingleton.class) {
                // 第二次检查：确保只创建一个实例
                if (instance == null) {
                    instance = new DoubleCheckedLockingSingleton();
                }
            }
        }
        return instance;
    }
}

/**
 * =====================================================================================
 * 4. 静态内部类单例（推荐方式）
 * =====================================================================================
 * <p>
 * 原理：利用 Java 类加载机制保证线程安全
 * <p>
 * 关键点：
 * 1. 外部类加载时，内部类不会加载
 * 2. 调用 getInstance() 时，JVM 才加载内部类
 * 3. JVM 在类加载时会进行同步，保证线程安全
 * <p>
 * 优点：
 * - 懒加载：首次使用时才加载内部类
 * - 线程安全：JVM 保证类加载的线程安全
 * - 代码简洁：无需 synchronized 和 volatile
 */
class StaticInnerClassSingleton {

    // 私有构造函数
    private StaticInnerClassSingleton() {
        System.out.println("    [静态内部类] 实例已创建");
    }

    // 静态内部类，持有外部类的实例
    private static class Holder {
        private static final StaticInnerClassSingleton INSTANCE = new StaticInnerClassSingleton();
    }

    // 获取实例时才会加载 Holder 类
    public static StaticInnerClassSingleton getInstance() {
        return Holder.INSTANCE;
    }
}

/**
 * =====================================================================================
 * 5. 枚举单例（最佳实践）
 * =====================================================================================
 * <p>
 * 原理：利用 Java 枚举类型的特性
 * <p>
 * 优点：
 * 1. 天然线程安全：枚举实例在类加载时创建，JVM 保证线程安全
 * 2. 防止反射攻击：Constructor#newInstance() 对枚举类型会抛出异常
 * 3. 防止序列化破坏：枚举的序列化机制保证不会创建新实例
 * 4. 代码最简洁
 * <p>
 * 推荐：这是《Effective Java》作者 Josh Bloch 推荐的方式
 */
enum EnumSingleton {

    INSTANCE;

    // 枚举的构造函数默认是私有的
    EnumSingleton() {
        System.out.println("    [枚举单例] 实例已创建");
    }

    // 可以添加业务方法
    public void doSomething() {
        System.out.println("    执行业务逻辑");
    }
}
