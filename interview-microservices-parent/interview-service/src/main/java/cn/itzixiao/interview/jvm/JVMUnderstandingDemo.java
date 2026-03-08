package cn.itzixiao.interview.jvm;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM 核心概念理解示例
 *
 * JVM 内存结构：
 * ┌─────────────────────────────────────────────────────────────┐
 * │                        堆内存 (Heap)                         │
 * │  ┌─────────────┬─────────────┬─────────────┬─────────────┐  │
 * │  │   新生代     │   Eden区    │  Survivor0  │  Survivor1  │  │
 * │  │  (Young)    │   (8/10)    │   (1/10)    │   (1/10)    │  │
 * │  └─────────────┴─────────────┴─────────────┴─────────────┘  │
 * │  ┌─────────────────────────────────────────────────────────┐│
 * │  │                   老年代 (Old/Tenured)                   ││
 * │  └─────────────────────────────────────────────────────────┘│
 * └─────────────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────────────┐
 * │                      元空间 (Metaspace)                      │
 * │              (JDK8+ 替代永久代，使用本地内存)                  │
 * └─────────────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────────────┐
 * │  虚拟机栈 (VM Stack)  │  本地方法栈 (Native Stack)            │
 * │  - 栈帧：局部变量表、操作数栈、动态链接、方法返回地址           │
 * └─────────────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────────────┐
 * │              程序计数器 (PC Register)                         │
 * │              记录当前线程执行的字节码行号                      │
 * └─────────────────────────────────────────────────────────────┘
 * ┌─────────────────────────────────────────────────────────────┐
 * │              直接内存 (Direct Memory) - NIO使用               │
 * └─────────────────────────────────────────────────────────────┘
 */
public class JVMUnderstandingDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== JVM 核心概念理解示例 ==========\n");

        // 1. JVM 内存结构演示
        demonstrateMemoryStructure();

        // 2. 堆内存分配与GC演示
        demonstrateHeapAndGC();

        // 3. 栈内存与栈溢出演示
        demonstrateStackMemory();

        // 4. 元空间/方法区演示
        demonstrateMetaspace();

        // 5. 对象内存布局演示
        demonstrateObjectMemoryLayout();

        // 6. 类加载机制演示
        demonstrateClassLoading();

        // 7. JVM 参数与监控
        demonstrateJVMMonitoring();
    }

    /**
     * 1. JVM 内存结构说明
     */
    private static void demonstrateMemoryStructure() {
        System.out.println("【1. JVM 内存结构】\n");

        String structure =
            "运行时数据区：\n" +
            "┌─────────────────────────────────────────────────────────────┐\n" +
            "│  线程共享区域                                                │\n" +
            "│  ┌───────────────────────────────────────────────────────┐  │\n" +
            "│  │  堆 (Heap) - 所有对象实例分配区域                        │  │\n" +
            "│  │  -Xms 初始堆大小  -Xmx 最大堆大小                        │  │\n" +
            "│  │  - 新生代 (Young): Eden + Survivor0 + Survivor1         │  │\n" +
            "│  │  - 老年代 (Old): 存放长期存活对象                         │  │\n" +
            "│  └───────────────────────────────────────────────────────┘  │\n" +
            "│  ┌───────────────────────────────────────────────────────┐  │\n" +
            "│  │  元空间 (Metaspace) - 类元数据、常量池、静态变量          │  │\n" +
            "│  │  -XX:MetaspaceSize  -XX:MaxMetaspaceSize                │  │\n" +
            "│  │  (JDK8之前是永久代 PermGen，在堆内存中)                   │  │\n" +
            "│  └───────────────────────────────────────────────────────┘  │\n" +
            "├─────────────────────────────────────────────────────────────┤\n" +
            "│  线程私有区域                                                │\n" +
            "│  ┌───────────────────────────────────────────────────────┐  │\n" +
            "│  │  虚拟机栈 (VM Stack) - 每个线程私有                       │  │\n" +
            "│  │  -Xss 栈大小                                           │  │\n" +
            "│  │  - 存储栈帧：局部变量表、操作数栈、动态链接、返回地址      │  │\n" +
            "│  └───────────────────────────────────────────────────────┘  │\n" +
            "│  ┌───────────────────────────────────────────────────────┐  │\n" +
            "│  │  本地方法栈 (Native Stack) - JNI调用使用                  │  │\n" +
            "│  └───────────────────────────────────────────────────────┘  │\n" +
            "│  ┌───────────────────────────────────────────────────────┐  │\n" +
            "│  │  程序计数器 (PC Register) - 记录当前执行位置              │  │\n" +
            "│  └───────────────────────────────────────────────────────┘  │\n" +
            "└─────────────────────────────────────────────────────────────┘\n" +
            "直接内存 (Direct Memory)：\n" +
            "  - NIO 使用，不受 JVM 堆大小限制\n" +
            "  - -XX:MaxDirectMemorySize 设置上限\n";

        System.out.println(structure);
    }

    /**
     * 2. 堆内存分配与GC演示
     */
    private static void demonstrateHeapAndGC() throws InterruptedException {
        System.out.println("【2. 堆内存分配与垃圾回收】\n");

        // 获取内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();

        System.out.println("堆内存信息：");
        System.out.println("  初始大小: " + formatBytes(heapUsage.getInit()));
        System.out.println("  已使用: " + formatBytes(heapUsage.getUsed()));
        System.out.println("  已提交: " + formatBytes(heapUsage.getCommitted()));
        System.out.println("  最大值: " + formatBytes(heapUsage.getMax()));

        System.out.println("\n对象分配与晋升过程：");
        System.out.println("1. 新对象首先在 Eden 区分配");
        System.out.println("2. Eden 满时触发 Minor GC，存活对象进入 Survivor 区");
        System.out.println("3. Survivor 区对象每熬过一次 GC，年龄 +1");
        System.out.println("4. 年龄达到阈值（默认15），晋升到老年代");
        System.out.println("5. 老年代满时触发 Full GC\n");

        // 演示对象分配
        System.out.println("模拟对象分配（观察内存变化）：");
        List<byte[]> list = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            // 分配 10MB 内存
            byte[] bytes = new byte[10 * 1024 * 1024];
            list.add(bytes);

            heapUsage = memoryMXBean.getHeapMemoryUsage();
            System.out.println("  分配第 " + (i + 1) + " 个 10MB 对象，堆已使用: " +
                formatBytes(heapUsage.getUsed()));

            Thread.sleep(100);
        }

        list.clear();
        System.gc(); // 建议GC（不强制）
        Thread.sleep(500);

        heapUsage = memoryMXBean.getHeapMemoryUsage();
        System.out.println("  清空引用并GC后，堆已使用: " + formatBytes(heapUsage.getUsed()));
        System.out.println();
    }

    /**
     * 3. 栈内存演示
     */
    private static void demonstrateStackMemory() {
        System.out.println("【3. 栈内存与栈帧】\n");

        System.out.println("栈帧结构（每个方法调用创建一个栈帧）：");
        System.out.println("┌─────────────────────────┐");
        System.out.println("│  局部变量表 - 方法参数和局部变量  │");
        System.out.println("├─────────────────────────┤");
        System.out.println("│  操作数栈 - 字节码指令工作区     │");
        System.out.println("├─────────────────────────┤");
        System.out.println("│  动态链接 - 指向运行时常量池     │");
        System.out.println("├─────────────────────────┤");
        System.out.println("│  方法返回地址 - 调用者位置       │");
        System.out.println("└─────────────────────────┘\n");

        // 演示栈深度
        System.out.println("计算当前栈深度：");
        try {
            calculateStackDepth(0);
        } catch (StackOverflowError e) {
            System.out.println("  栈溢出！这是栈内存的限制。\n");
        }
    }

    private static void calculateStackDepth(int depth) {
        if (depth % 1000 == 0) {
            System.out.println("  当前递归深度: " + depth);
        }
        calculateStackDepth(depth + 1);
    }

    /**
     * 4. 元空间/方法区演示
     */
    private static void demonstrateMetaspace() {
        System.out.println("【4. 元空间 (Metaspace)】\n");

        System.out.println("存储内容：");
        System.out.println("  - 类型信息（类名、修饰符、父类、接口）");
        System.out.println("  - 常量池（运行时常量池、字符串常量池）");
        System.out.println("  - 字段信息、方法信息");
        System.out.println("  - 静态变量（JDK8+ 移到堆中）");
        System.out.println("  - 即时编译器编译后的代码缓存\n");

        System.out.println("JDK8 变化：");
        System.out.println("  永久代 (PermGen) → 元空间 (Metaspace)");
        System.out.println("  - 永久代在 JVM 堆内存中，大小固定，容易溢出");
        System.out.println("  - 元空间使用本地内存，默认无上限（受限于物理内存）");
        System.out.println("  - 字符串常量池移到堆中\n");

        // 字符串常量池演示
        System.out.println("字符串常量池演示：");
        String s1 = "abc";
        String s2 = "abc";
        String s3 = new String("abc");
        String s4 = s3.intern();

        System.out.println("  s1 == s2: " + (s1 == s2) + " (常量池复用)");
        System.out.println("  s1 == s3: " + (s1 == s3) + " (new 创建新对象)");
        System.out.println("  s1 == s4: " + (s1 == s4) + " (intern 返回常量池引用)\n");
    }

    /**
     * 5. 对象内存布局演示
     */
    private static void demonstrateObjectMemoryLayout() {
        System.out.println("【5. 对象内存布局】\n");

        System.out.println("普通对象在堆中的布局（64位 JVM，开启压缩指针）：");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│  对象头 (Header) - 12 bytes         │");
        System.out.println("│  - Mark Word: 8 bytes               │");
        System.out.println("│    * 哈希码、GC年龄、锁状态标志      │");
        System.out.println("│  - Class Pointer: 4 bytes           │");
        System.out.println("│    * 指向类元数据的指针              │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│  实例数据 (Instance Data)           │");
        System.out.println("│  - 字段内容，按类型对齐              │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│  对齐填充 (Padding)                 │");
        System.out.println("│  - 8字节对齐                        │");
        System.out.println("└─────────────────────────────────────┘\n");

        System.out.println("Mark Word 结构（64位）：");
        System.out.println("┌────────────────────────────────────────────────────────┐");
        System.out.println("│  无锁状态：                                             │");
        System.out.println("│  [unused:25|hash:31|unused:1|age:4|biased_lock:1|lock:2]│");
        System.out.println("│  biased_lock=0, lock=01 表示无锁                       │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│  偏向锁状态：                                           │");
        System.out.println("│  [thread:54|epoch:2|unused:1|age:4|biased_lock:1|lock:2]│");
        System.out.println("│  biased_lock=1, lock=01 表示偏向锁                     │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│  轻量级锁：                                             │");
        System.out.println("│  [ptr_to_lock_record:62|lock:02]                        │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│  重量级锁：                                             │");
        System.out.println("│  [ptr_to_heavyweight_monitor:62|lock:10]                │");
        System.out.println("├────────────────────────────────────────────────────────┤");
        System.out.println("│  GC标记：                                               │");
        System.out.println("│  [unused:62|lock:11]                                    │");
        System.out.println("└────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 6. 类加载机制演示
     */
    private static void demonstrateClassLoading() {
        System.out.println("【6. 类加载机制】\n");

        System.out.println("类加载过程：");
        System.out.println("┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐");
        System.out.println("│  加载    │ → │  验证    │ → │  准备    │ → │  解析    │ → │  初始化  │");
        System.out.println("└─────────┘   └─────────┘   └─────────┘   └─────────┘   └─────────┘");
        System.out.println("  加载：读取.class文件，生成Class对象");
        System.out.println("  验证：文件格式、元数据、字节码、符号引用验证");
        System.out.println("  准备：为类变量分配内存并设置零值");
        System.out.println("  解析：将符号引用转为直接引用");
        System.out.println("  初始化：执行<clinit>()方法\n");

        System.out.println("双亲委派模型：");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│           自定义类加载器                 │");
        System.out.println("│         (Application ClassLoader)       │");
        System.out.println("│                   ↑ 委派                │");
        System.out.println("│         扩展类加载器                     │");
        System.out.println("│         (Extension ClassLoader)         │");
        System.out.println("│                   ↑ 委派                │");
        System.out.println("│         启动类加载器                     │");
        System.out.println("│         (Bootstrap ClassLoader)         │");
        System.out.println("│              ↑ 加载核心类                │");
        System.out.println("│         [jre/lib/rt.jar]                │");
        System.out.println("└─────────────────────────────────────────┘\n");

        // 演示类加载器
        System.out.println("当前类加载器层次：");
        ClassLoader classLoader = JVMUnderstandingDemo.class.getClassLoader();
        while (classLoader != null) {
            System.out.println("  " + classLoader.getClass().getName());
            classLoader = classLoader.getParent();
        }
        System.out.println("  null (Bootstrap ClassLoader - 由C++实现)\n");
    }

    /**
     * 7. JVM 监控与参数
     */
    private static void demonstrateJVMMonitoring() {
        System.out.println("【7. JVM 参数与监控】\n");

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

        System.out.println("JVM 信息：");
        System.out.println("  JVM 名称: " + runtimeMXBean.getVmName());
        System.out.println("  JVM 版本: " + runtimeMXBean.getVmVersion());
        System.out.println("  JVM 厂商: " + runtimeMXBean.getVmVendor());

        System.out.println("\n常用 JVM 参数：");
        System.out.println("堆内存设置：");
        System.out.println("  -Xms512m          初始堆大小");
        System.out.println("  -Xmx2g            最大堆大小");
        System.out.println("  -Xmn256m          新生代大小");
        System.out.println("  -XX:NewRatio=2    老年代/新生代比例");
        System.out.println("  -XX:SurvivorRatio=8  Eden/Survivor比例");

        System.out.println("\n元空间设置：");
        System.out.println("  -XX:MetaspaceSize=128m      初始元空间大小");
        System.out.println("  -XX:MaxMetaspaceSize=256m   最大元空间大小");

        System.out.println("\nGC 设置：");
        System.out.println("  -XX:+UseSerialGC        串行GC");
        System.out.println("  -XX:+UseParallelGC      并行GC");
        System.out.println("  -XX:+UseConcMarkSweepGC CMS GC");
        System.out.println("  -XX:+UseG1GC            G1 GC");
        System.out.println("  -XX:+PrintGCDetails     打印GC详情");
        System.out.println("  -XX:+PrintGCDateStamps  打印GC时间戳");
        System.out.println("  -Xloggc:gc.log          GC日志文件");

        System.out.println("\nOOM 相关：");
        System.out.println("  -XX:+HeapDumpOnOutOfMemoryError    OOM时生成堆转储");
        System.out.println("  -XX:HeapDumpPath=/path/to/dump     堆转储路径");

        System.out.println("\n当前启动参数：");
        List<String> inputArguments = runtimeMXBean.getInputArguments();
        for (String arg : inputArguments) {
            System.out.println("  " + arg);
        }
    }

    /**
     * 格式化字节大小
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}
