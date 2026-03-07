package cn.itzixiao.interview.java;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Java 类加载机制深入理解 - 教学型详解
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        Java 类加载机制总览                                  │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                          类加载过程                                  │   │
 * │  │                                                                     │   │
 * │  │   加载(Loading) → 验证(Verification) → 准备(Preparation)          │   │
 * │  │        ↓                                                           │   │
 * │  │   解析(Resolution) → 初始化(Initialization) → 使用 → 卸载           │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                       类加载器层次结构                                │   │
 * │  │                                                                     │   │
 * │  │         应用程序类加载器 (Application ClassLoader)                   │   │
 * │  │                        ↑ 委托                                      │   │
 * │  │          扩展类加载器 (Extension ClassLoader)                       │   │
 * │  │                        ↑ 委托                                      │   │
 * │  │          启动类加载器 (Bootstrap ClassLoader)                       │   │
 * │  │                                                                     │   │
 * │  │  类加载器          加载路径                     实现语言             │   │
 * │  │  Bootstrap        $JAVA_HOME/lib              C++                  │   │
 * │  │  Extension        $JAVA_HOME/lib/ext          Java                 │   │
 * │  │  Application      classpath                   Java                 │   │
 * │  │  Custom           自定义                       Java                 │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                       双亲委派模型                                    │   │
 * │  │                                                                     │   │
 * │  │  工作流程：                                                          │   │
 * │  │  1. 收到类加载请求                                                  │   │
 * │  │  2. 委托给父类加载器                                                │   │
 * │  │  3. 父类加载器继续向上委托，直到 Bootstrap                       │   │
 * │  │  4. 从顶层开始尝试加载                                              │   │
 * │  │  5. 父类无法加载，子类才尝试加载                                  │   │
 * │  │                                                                     │   │
 * │  │  优点：                                                              │   │
 * │  │  - 避免类的重复加载                                                  │   │
 * │  │  - 保证核心 API 不被篡改（安全性）                                  │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                     打破双亲委派的场景                                │   │
 * │  │                                                                     │   │
 * │  │  1. JDBC：SPI 机制，启动类加载器加载的类需要加载厂商实现           │   │
 * │  │  2. Tomcat：Web 应用隔离，不同应用使用不同类加载器                 │   │
 * │  │  3. OSGi：模块化热部署                                             │   │
 * │  │  4. Spring Boot DevTools：热重载                                    │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────────────────────┘
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

    /**
     * 8. 类加载过程详解 - 深入版
     */
    public void classLoadingProcessDeep() {
        System.out.println("========== 类加载过程详解 ==========\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                  类加载过程内存变化                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  加载阶段：                                                    │");
        System.out.println("│  - .class 文件 → 二进制字节流 → 方法区的类信息              │");
        System.out.println("│  - 在堆中创建 Class 对象（访问方法区的入口）                │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  验证阶段：                                                    │");
        System.out.println("│  - 文件格式：魔数(0xCAFEBABE)、版本号、常量池等            │");
        System.out.println("│  - 元数据：类是否有父类、是否实现接口、访问性                  │");
        System.out.println("│  - 字节码：控制流、数据流分析，确保不会危害 VM             │");
        System.out.println("│  - 符号引用：能否找到对应的类、方法、字段                    │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  准备阶段：                                                    │");
        System.out.println("│  - 为类变量（static）分配内存                                 │");
        System.out.println("│  - 设置初始零值：                                             │");
        System.out.println("│      int → 0, long → 0L, boolean → false               │");
        System.out.println("│      引用类型 → null                                         │");
        System.out.println("│  - 注意：final static 的常量在这里就赋值了                    │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  解析阶段：                                                    │");
        System.out.println("│  - 符号引用 → 直接引用                                      │");
        System.out.println("│  - 符号引用：类的全限定名（字符串）                          │");
        System.out.println("│  - 直接引用：类在内存中的实际地址（指针）                    │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  初始化阶段：                                                  │");
        System.out.println("│  - 执行 <clinit>() 方法                                      │");
        System.out.println("│  - 类变量真正赋值                                            │");
        System.out.println("│  - 执行静态代码块                                              │");
        System.out.println("│  - 多线程环境下 JVM 保证只执行一次                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 9. 初始化时机详解
     */
    public void initializationTimingDeep() {
        System.out.println("========== 初始化时机详解 ==========\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│              主动引用（会触发初始化）                           │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. new 实例化对象                                            │");
        System.out.println("│  2. 访问静态变量（final 常量除外）                              │");
        System.out.println("│  3. 调用静态方法                                              │");
        System.out.println("│  4. 反射调用（Class.forName）                                 │");
        System.out.println("│  5. 初始化子类时，父类先初始化                                │");
        System.out.println("│  6. main() 方法所在类                                         │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│              被动引用（不会触发初始化）                         │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. 通过子类引用父类的静态字段                                │");
        System.out.println("│  2. 定义数组类型（如 SuperClass[]）                            │");
        System.out.println("│  3. 引用常量（编译期存入常量池）                               │");
        System.out.println("│  4. ClassLoader.loadClass() 只加载不初始化                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // 演示
        System.out.println("【代码演示】");
        System.out.println("  // 被动引用：通过子类访问父类静态字段");
        System.out.println("  System.out.println(ChildClass.parentStaticField);");
        System.out.println("  // 结果：只会初始化父类，不会初始化子类\n");

        System.out.println("  // 演示执行:");
        System.out.println("  ChildClass.parentStaticField = " + ChildClass.parentStaticField);
        System.out.println();
    }

    // ==================== 高频面试题演示 ====================

    /**
     * 10. 高频面试题 - 什么是类加载器？有哪几种？
     */
    public void interviewQuestion1() {
        System.out.println("==================== 高频面试题 ====================");
        System.out.println();
        System.out.println("【面试题 1】什么是类加载器？有哪几种？\n");

        System.out.println("【答案】");
        System.out.println("类加载器负责将 .class 文件加载到 JVM 内存中，生成 Class 对象\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  类加载器             加载路径                  实现语言   │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Bootstrap           $JAVA_HOME/lib           C++       │");
        System.out.println("│  （启动类加载器）      rt.jar, resources.jar              │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Extension           $JAVA_HOME/lib/ext       Java      │");
        System.out.println("│  （扩展类加载器）      java.ext.dirs                       │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Application         classpath                Java      │");
        System.out.println("│  （应用程序类加载器）  java.class.path                     │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Custom              自定义                    Java      │");
        System.out.println("│  （自定义类加载器）                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【代码验证】");
        System.out.println("  String.class 的加载器: " + String.class.getClassLoader());
        System.out.println("  (显示 null 因为 Bootstrap 是 C++ 实现)\n");
        System.out.println("  本类的加载器: " + this.getClass().getClassLoader() + "\n");
    }

    /**
     * 11. 高频面试题 - 双亲委派模型是什么？为什么要用？
     */
    public void interviewQuestion2() {
        System.out.println("【面试题 2】双亲委派模型是什么？为什么要用？\n");

        System.out.println("【答案】");
        System.out.println("双亲委派：收到加载请求时，先委托父加载器加载，父加载器无法加载时才自己加载\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                      工作流程                                │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. 收到加载请求                                              │");
        System.out.println("│  2. 检查该类是否已加载                                        │");
        System.out.println("│  3. 委托给父类加载器（递归向上）                              │");
        System.out.println("│  4. 到达 Bootstrap，开始尝试加载                              │");
        System.out.println("│  5. 父类加载失败，子类才尝试加载                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    为什么要使用双亲委派？                       │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. 避免重复加载：父加载器加载过的类，子加载器不会再加载    │");
        System.out.println("│  2. 保证安全性：防止核心 API 被篡改                           │");
        System.out.println("│     例如：自定义 java.lang.String 不会被加载             │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // 源码展示
        System.out.println("【核心源码】");
        System.out.println("  protected Class<?> loadClass(String name, boolean resolve) {");
        System.out.println("      // 1. 检查是否已加载");
        System.out.println("      Class<?> c = findLoadedClass(name);");
        System.out.println("      if (c == null) {");
        System.out.println("          // 2. 委托父类加载");
        System.out.println("          if (parent != null) {");
        System.out.println("              c = parent.loadClass(name, false);");
        System.out.println("          } else {");
        System.out.println("              c = findBootstrapClassOrNull(name);");
        System.out.println("          }");
        System.out.println("          // 3. 父类加载失败，自己加载");
        System.out.println("          if (c == null) {");
        System.out.println("              c = findClass(name);");
        System.out.println("          }");
        System.out.println("      }");
        System.out.println("      return c;");
        System.out.println("  }\n");
    }

    /**
     * 12. 高频面试题 - 如何打破双亲委派模型？
     */
    public void interviewQuestion3() {
        System.out.println("【面试题 3】如何打破双亲委派模型？有哪些场景？\n");

        System.out.println("【答案】重写 loadClass() 方法，先自己加载，失败再委托\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                  打破双亲委派的典型场景                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. JDBC SPI 机制                                           │");
        System.out.println("│     - 接口在 rt.jar (Bootstrap 加载)                        │");
        System.out.println("│     - 实现在厂商 jar (App 加载)                            │");
        System.out.println("│     - 解决：线程上下文类加载器 (TCCL)                       │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. Tomcat Web 应用隔离                                    │");
        System.out.println("│     - 每个 Web 应用有独立的类加载器                        │");
        System.out.println("│     - 同一个类可以加载不同版本                              │");
        System.out.println("│     - 实现应用间隔离                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. OSGi 模块化                                             │");
        System.out.println("│     - 模块热插拔                                            │");
        System.out.println("│     - 模块间依赖管理                                        │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  4. Spring Boot DevTools                                    │");
        System.out.println("│     - 实现热重载                                            │");
        System.out.println("│     - RestartClassLoader 替换旧类                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // Tomcat 类加载器结构
        System.out.println("【Tomcat 类加载器结构】");
        System.out.println("                Bootstrap");
        System.out.println("                   ↑");
        System.out.println("                System");
        System.out.println("                   ↑");
        System.out.println("                Common");
        System.out.println("               /      \\");
        System.out.println("          Catalina   Shared");
        System.out.println("                        ↑");
        System.out.println("              WebApp1  WebApp2  WebApp3\n");
    }

    /**
     * 13. 高频面试题 - Class.forName 与 ClassLoader.loadClass 的区别
     */
    public void interviewQuestion4() throws Exception {
        System.out.println("【面试题 4】Class.forName() 与 ClassLoader.loadClass() 的区别？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  方式                        是否初始化      用途              │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Class.forName(name)        ✔             JDBC 加载驱动     │");
        System.out.println("│  Class.forName(name,false,) ✖             延迟初始化       │");
        System.out.println("│  ClassLoader.loadClass()    ✖             Spring 懒加载    │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【代码演示】");
        System.out.println("  // JDBC 加载驱动 - 需要执行 static 块注册驱动");
        System.out.println("  Class.forName(\"com.mysql.cj.jdbc.Driver\");");
        System.out.println();
        System.out.println("  // Spring 懒加载 - 不需要立即初始化");
        System.out.println("  classLoader.loadClass(\"com.example.MyBean\");\n");

        // 实际演示
        System.out.println("【实际演示】");
        System.out.println("  使用 ClassLoader.loadClass 加载测试类:");
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        // 此时不会执行 static 块
        System.out.println("  加载完成，注意观察 static 块是否执行\n");
    }

    /**
     * 14. 高频面试题 - 线程上下文类加载器是什么？
     */
    public void interviewQuestion5() {
        System.out.println("【面试题 5】线程上下文类加载器 (TCCL) 是什么？为什么需要它？\n");

        System.out.println("【答案】");
        System.out.println("TCCL 是绑定在线程上的类加载器，用于解决 SPI 问题\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    SPI 问题场景                              │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  JDBC 接口: java.sql.Driver                                 │");
        System.out.println("│    - 在 rt.jar 中                                          │");
        System.out.println("│    - 由 Bootstrap ClassLoader 加载                         │");
        System.out.println("│                                                             │");
        System.out.println("│  MySQL 实现: com.mysql.cj.jdbc.Driver                       │");
        System.out.println("│    - 在 classpath 中                                       │");
        System.out.println("│    - 由 Application ClassLoader 加载                       │");
        System.out.println("│                                                             │");
        System.out.println("│  问题：Bootstrap 无法加载 Application 下的类                │");
        System.out.println("│  解决：通过 TCCL 获取 Application ClassLoader               │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【代码演示】");
        System.out.println("  // 获取线程上下文类加载器");
        System.out.println("  ClassLoader tccl = Thread.currentThread().getContextClassLoader();");
        System.out.println();
        System.out.println("  // 设置线程上下文类加载器");
        System.out.println("  Thread.currentThread().setContextClassLoader(customLoader);");
        System.out.println();

        // 实际演示
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        System.out.println("【实际值】");
        System.out.println("  当前线程的 TCCL: " + tccl + "\n");
    }

    /**
     * 15. 高频面试题 - 类的卸载条件是什么？
     */
    public void interviewQuestion6() {
        System.out.println("【面试题 6】类的卸载条件是什么？Bootstrap 加载的类会被卸载吗？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    类卸载的三个条件                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  1. 该类的所有实例已被 GC 回收                              │");
        System.out.println("│  2. 加载该类的 ClassLoader 已被 GC 回收                    │");
        System.out.println("│  3. 该类的 Class 对象没有被引用                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【重要结论】");
        System.out.println("  - Bootstrap ClassLoader 加载的类不会被卸载");
        System.out.println("    （JVM 生命周期内始终存在）");
        System.out.println("  - 自定义类加载器加载的类可以被卸载\n");

        System.out.println("【应用场景】");
        System.out.println("  - Tomcat 热部署：重新部署时卸载旧的 WebApp 类");
        System.out.println("  - OSGi 模块卸载：停止模块时卸载相关类");
        System.out.println("  - 动态代理：代理类不再使用时可被卸载\n");
    }

    /**
     * 16. 高频面试题 - 类加载过程中的准备和初始化有什么区别？
     */
    public void interviewQuestion7() {
        System.out.println("【面试题 7】准备阶段和初始化阶段有什么区别？\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  阶段          操作                    示例                  │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  准备          分配内存+零值            static int a = 0    │");
        System.out.println("│  初始化        真正赋值                static int a = 123  │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【特殊情况】");
        System.out.println("  // final static 的编译期常量，在准备阶段就赋值");
        System.out.println("  public static final int CONST = 100; // 准备阶段 CONST = 100");
        System.out.println();
        System.out.println("  // final static 但需要计算，在初始化阶段赋值");
        System.out.println("  public static final int CALC = getValue(); // 初始化时赋值\n");

        System.out.println("【零值表】");
        System.out.println("  int: 0,  long: 0L,  short: 0");
        System.out.println("  float: 0.0f,  double: 0.0d");
        System.out.println("  boolean: false,  char: '\\u0000'");
        System.out.println("  引用类型: null\n");
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

    /**
     * 热重载类加载器示例
     */
    static class HotSwapClassLoader extends ClassLoader {
        private String classPath;

        public HotSwapClassLoader(String classPath) {
            this.classPath = classPath;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] classData = loadClassData(name);
            if (classData == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, classData, 0, classData.length);
        }

        private byte[] loadClassData(String className) {
            String path = classPath + "/" + className.replace('.', '/') + ".class";
            try {
                return Files.readAllBytes(Paths.get(path));
            } catch (IOException e) {
                return null;
            }
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
        
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                                                                   █");
        System.out.println("█                Java 类加载机制深入讲解                          █");
        System.out.println("█                                                                   █");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println();
        
        // 基础部分
        demo.classLoaderHierarchy();
        demo.parentDelegationModel();
        demo.classLoadingTiming();
        demo.classLoadingProcess();
        demo.breakParentDelegation();
        demo.threadContextClassLoader();
        demo.classUnloading();
        
        // 进阶部分
        demo.classLoadingProcessDeep();
        demo.initializationTimingDeep();
        
        // 高频面试题
        demo.interviewQuestion1();
        demo.interviewQuestion2();
        demo.interviewQuestion3();
        demo.interviewQuestion4();
        demo.interviewQuestion5();
        demo.interviewQuestion6();
        demo.interviewQuestion7();
        
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                        全部演示完成                             █");
        System.out.println("███████████████████████████████████████████████████████████████████");
    }
}
