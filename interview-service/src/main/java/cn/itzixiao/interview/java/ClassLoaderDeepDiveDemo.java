package cn.itzixiao.interview.java;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Java 类加载机制深入理解
 *
 * 类加载过程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 加载（Loading）                                          │
 * │     - 通过全限定名获取二进制字节流                            │
 * │     - 将字节流转化为方法区的运行时数据结构                    │
 * │     - 在堆中生成 Class 对象，作为方法区数据的访问入口         │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 验证（Verification）                                     │
 * │     - 文件格式验证                                           │
 * │     - 元数据验证                                             │
 * │     - 字节码验证                                             │
 * │     - 符号引用验证                                           │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 准备（Preparation）                                      │
 * │     - 为类变量分配内存并设置零值                              │
 * │     - 例如：static int a = 123; 准备阶段 a = 0               │
 * ├─────────────────────────────────────────────────────────────┤
 * │  4. 解析（Resolution）                                       │
 * │     - 将常量池中的符号引用替换为直接引用                      │
 * │     - 类、接口、字段、方法解析                                │
 * ├─────────────────────────────────────────────────────────────┤
 * │  5. 初始化（Initialization）                                 │
 * │     - 执行 <clinit>() 方法                                   │
 * │     - 类变量赋值和静态代码块执行                              │
 * │     - 例如：static int a = 123; 初始化阶段 a = 123           │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 双亲委派模型：
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     自定义类加载器                            │
 * │                          ↑                                   │
 * │                     应用程序类加载器（AppClassLoader）        │
 * │                          ↑                                   │
 * │                     扩展类加载器（ExtClassLoader）            │
 * │                          ↑                                   │
 * │                     启动类加载器（BootstrapClassLoader）      │
 * └─────────────────────────────────────────────────────────────┘
 */
public class ClassLoaderDeepDiveDemo {

    /**
     * 1. 类加载器层次结构
     */
    public void classLoaderHierarchy() {
        System.out.println("========== 类加载器层次结构 ==========\n");

        // 获取系统类加载器（AppClassLoader）
        ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
        System.out.println("【应用程序类加载器】AppClassLoader");
        System.out.println("  加载路径: classpath 下的类");
        System.out.println("  ClassLoader: " + appClassLoader);
        System.out.println("  加载类示例: " + this.getClass().getClassLoader() + "\n");

        // 获取扩展类加载器（ExtClassLoader）
        ClassLoader extClassLoader = appClassLoader.getParent();
        System.out.println("【扩展类加载器】ExtClassLoader");
        System.out.println("  加载路径: $JAVA_HOME/lib/ext 或 java.ext.dirs");
        System.out.println("  ClassLoader: " + extClassLoader + "\n");

        // 获取启动类加载器（BootstrapClassLoader）
        ClassLoader bootstrapClassLoader = extClassLoader.getParent();
        System.out.println("【启动类加载器】BootstrapClassLoader");
        System.out.println("  加载路径: $JAVA_HOME/lib（rt.jar 等核心类库）");
        System.out.println("  ClassLoader: " + bootstrapClassLoader);
        System.out.println("  （C++ 实现，Java 代码中获取为 null）");
        System.out.println("  加载类示例: " + String.class.getClassLoader() + "\n");

        // 验证 String 类由 BootstrapClassLoader 加载
        System.out.println("【验证】");
        System.out.println("  String.class.getClassLoader(): " + String.class.getClassLoader());
        System.out.println("  自定义类.getClassLoader(): " + this.getClass().getClassLoader() + "\n");
    }

    /**
     * 2. 双亲委派模型演示
     */
    public void parentDelegationModel() throws ClassNotFoundException {
        System.out.println("========== 双亲委派模型 ==========\n");

        System.out.println("【委派流程】");
        System.out.println("  1. 收到类加载请求");
        System.out.println("  2. 委托给父类加载器");
        System.out.println("  3. 父类加载器继续向上委托");
        System.out.println("  4. 到达顶层（Bootstrap）");
        System.out.println("  5. 从顶层开始尝试加载");
        System.out.println("  6. 父类无法加载，子类才尝试加载\n");

        System.out.println("【优点】");
        System.out.println("  1. 避免类的重复加载");
        System.out.println("  2. 保证核心 API 不被篡改（安全性）\n");

        // 自定义类加载器演示
        System.out.println("【自定义类加载器演示】");
        CustomClassLoader customLoader = new CustomClassLoader();

        // 加载 String 类（应该由 BootstrapClassLoader 加载）
        Class<?> stringClass = customLoader.loadClass("java.lang.String");
        System.out.println("  加载 String 类: " + stringClass.getClassLoader());

        // 加载自定义类
        Class<?> customClass = customLoader.loadClass(
                "cn.itzixiao.interview.java.ClassLoaderDeepDiveDemo");
        System.out.println("  加载自定义类: " + customClass.getClassLoader() + "\n");
    }

    /**
     * 3. 类加载时机
     */
    public void classLoadingTiming() {
        System.out.println("========== 类加载时机 ==========\n");

        System.out.println("【主动引用（会触发初始化）】");
        System.out.println("  1. new 实例化对象");
        System.out.println("  2. 访问静态变量（final 常量除外）");
        System.out.println("  3. 调用静态方法");
        System.out.println("  4. 反射调用");
        System.out.println("  5. 初始化子类时，父类先初始化");
        System.out.println("  6. main() 方法所在类\n");

        System.out.println("【被动引用（不会触发初始化）】");
        System.out.println("  1. 通过子类引用父类的静态字段");
        System.out.println("  2. 定义数组类型（如 SuperClass[]）");
        System.out.println("  3. 引用常量（编译期存入常量池）\n");

        // 演示被动引用
        System.out.println("【演示】被动引用");
        System.out.println("  访问 ChildClass.parentStaticField:");
        System.out.println("  " + ChildClass.parentStaticField);
        System.out.println("  （ParentClass 未初始化！）\n");
    }

    /**
     * 4. 类加载过程详解
     */
    public void classLoadingProcess() {
        System.out.println("========== 类加载过程详解 ==========\n");

        System.out.println("【1. 加载（Loading）】");
        System.out.println("  - 获取二进制字节流（文件、网络、动态生成等）");
        System.out.println("  - 转化为方法区运行时数据结构");
        System.out.println("  - 生成 Class 对象\n");

        System.out.println("【2. 验证（Verification）】");
        System.out.println("  - 文件格式验证：魔数、版本号等");
        System.out.println("  - 元数据验证：语义分析");
        System.out.println("  - 字节码验证：控制流、数据流分析");
        System.out.println("  - 符号引用验证：能否找到对应类\n");

        System.out.println("【3. 准备（Preparation）】");
        System.out.println("  - 为类变量分配内存");
        System.out.println("  - 设置零值（0、null、false）");
        System.out.println("  - 例：static int a = 123; 准备阶段 a = 0\n");

        System.out.println("【4. 解析（Resolution）】");
        System.out.println("  - 符号引用 → 直接引用");
        System.out.println("  - 类/接口解析、字段解析、方法解析\n");

        System.out.println("【5. 初始化（Initialization）】");
        System.out.println("  - 执行 <clinit>() 方法");
        System.out.println("  - 类变量赋值");
        System.out.println("  - 静态代码块执行");
        System.out.println("  - 例：static int a = 123; 初始化阶段 a = 123\n");

        // 演示初始化顺序
        System.out.println("【演示】初始化顺序");
        new InitializationOrder();
    }

    /**
     * 5. 打破双亲委派模型
     */
    public void breakParentDelegation() throws Exception {
        System.out.println("\n========== 打破双亲委派模型 ==========\n");

        System.out.println("【场景】");
        System.out.println("  1. JDBC：SPI 机制，由启动类加载器加载的类需要加载厂商实现");
        System.out.println("  2. Tomcat：Web 应用隔离，不同应用使用不同类加载器");
        System.out.println("  3. OSGi：模块化热部署\n");

        System.out.println("【Tomcat 类加载器结构】");
        System.out.println("  Bootstrap");
        System.out.println("     ↑");
        System.out.println("  Common (Shared)");
        System.out.println("   /    \\");
        System.out.println("Catalina  Shared");
        System.out.println("            ↑");
        System.out.println("        WebApp1  WebApp2\n");

        // 自定义类加载器（打破双亲委派）
        System.out.println("【自定义类加载器】打破双亲委派");
        BreakDelegationClassLoader loader = new BreakDelegationClassLoader();

        // 先尝试自己加载
        Class<?> clazz = loader.loadClassWithoutDelegation(
                "cn.itzixiao.interview.java.ClassLoaderDeepDiveDemo");
        System.out.println("  加载类: " + clazz);
        System.out.println("  类加载器: " + clazz.getClassLoader() + "\n");
    }

    /**
     * 6. 线程上下文类加载器
     */
    public void threadContextClassLoader() {
        System.out.println("========== 线程上下文类加载器 ==========\n");

        System.out.println("【作用】");
        System.out.println("  - 解决 SPI（Service Provider Interface）问题");
        System.out.println("  - 父类加载器请求子类加载器完成类加载\n");

        System.out.println("【SPI 机制问题】");
        System.out.println("  - JDBC 接口由 BootstrapClassLoader 加载");
        System.out.println("  - MySQL 驱动实现由 AppClassLoader 加载");
        System.out.println("  - BootstrapClassLoader 无法加载 MySQL 驱动\n");

        System.out.println("【解决方案】");
        System.out.println("  Thread.currentThread().getContextClassLoader()");
        System.out.println("  默认是 AppClassLoader\n");

        // 获取和设置线程上下文类加载器
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        System.out.println("【当前线程上下文类加载器】");
        System.out.println("  " + contextClassLoader + "\n");

        // 使用 ServiceLoader 加载 SPI 实现
        System.out.println("【ServiceLoader 加载 SPI】");
        java.util.ServiceLoader<java.sql.Driver> drivers = java.util.ServiceLoader.load(java.sql.Driver.class);
        System.out.println("  加载的驱动:");
        for (java.sql.Driver driver : drivers) {
            System.out.println("    " + driver.getClass().getName());
        }
        System.out.println();
    }

    /**
     * 7. 类卸载
     */
    public void classUnloading() {
        System.out.println("========== 类卸载 ==========\n");

        System.out.println("【类卸载条件】");
        System.out.println("  1. 该类所有实例已被回收");
        System.out.println("  2. 加载该类的 ClassLoader 已被回收");
        System.out.println("  3. 该类对应的 Class 对象没有被引用\n");

        System.out.println("【典型场景】");
        System.out.println("  - Tomcat 热部署：卸载旧 WebApp 类");
        System.out.println("  - OSGi 模块卸载\n");

        System.out.println("【注意】");
        System.out.println("  - 由 BootstrapClassLoader 加载的类不会被卸载");
        System.out.println("  - 自定义类加载器加载的类可以卸载\n");
    }

    // ==================== 自定义类加载器 ====================

    /**
     * 自定义类加载器（遵循双亲委派）
     */
    static class CustomClassLoader extends ClassLoader {

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            System.out.println("  CustomClassLoader 尝试加载: " + name);

            // 先委托给父类加载器
            return super.loadClass(name);
        }
    }

    /**
     * 打破双亲委派的类加载器
     */
    static class BreakDelegationClassLoader extends ClassLoader {

        public Class<?> loadClassWithoutDelegation(String name) throws ClassNotFoundException {
            // 先尝试自己加载
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                try {
                    clazz = findClass(name);
                } catch (ClassNotFoundException e) {
                    // 自己加载失败，再委托给父类
                    clazz = super.loadClass(name);
                }
            }
            return clazz;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // 这里可以实现自定义的类加载逻辑
            // 例如：从网络、数据库、加密文件加载类
            throw new ClassNotFoundException("自定义加载失败: " + name);
        }
    }

    // ==================== 测试用的类 ====================

    static class ParentClass {
        static {
            System.out.println("  ParentClass 初始化");
        }
        public static final int parentStaticField = 123;
    }

    static class ChildClass extends ParentClass {
        static {
            System.out.println("  ChildClass 初始化");
        }
    }

    static class InitializationOrder {
        static {
            System.out.println("  1. 静态代码块执行");
        }

        static int staticVar = initializeStaticVar();

        static int initializeStaticVar() {
            System.out.println("  2. 静态变量初始化: staticVar = 10");
            return 10;
        }

        {
            System.out.println("  3. 实例代码块执行");
        }

        int instanceVar = initializeInstanceVar();

        int initializeInstanceVar() {
            System.out.println("  4. 实例变量初始化: instanceVar = 20");
            return 20;
        }

        public InitializationOrder() {
            System.out.println("  5. 构造方法执行");
        }
    }

    public static void main(String[] args) throws Exception {
        ClassLoaderDeepDiveDemo demo = new ClassLoaderDeepDiveDemo();
        demo.classLoaderHierarchy();
        demo.parentDelegationModel();
        demo.classLoadingTiming();
        demo.classLoadingProcess();
        demo.breakParentDelegation();
        demo.threadContextClassLoader();
        demo.classUnloading();
    }
}
