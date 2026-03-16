package cn.itzixiao.interview.jvm;

import java.util.ArrayList;
import java.util.List;

/**
 * 垃圾回收器理解与对比
 * <p>
 * 垃圾回收算法基础：
 * 1. 标记-清除 (Mark-Sweep)：标记存活对象，清除未标记，产生碎片
 * 2. 复制 (Copying)：内存分两半，存活对象复制到另一半，无碎片但浪费空间
 * 3. 标记-整理 (Mark-Compact)：标记存活对象，向一端移动，清理边界外内存
 * 4. 分代收集：新生代用复制，老年代用标记-清除或标记-整理
 * <p>
 * 垃圾回收器对比：
 * ┌─────────────┬─────────────┬─────────────┬─────────────┬─────────────┐
 * │   收集器     │   算法      │   适用区域   │   线程      │   特点      │
 * ├─────────────┼─────────────┼─────────────┼─────────────┼─────────────┤
 * │  Serial     │ 复制/标记整理│ 新生代/老年代│ 单线程      │ 简单高效    │
 * │  ParNew     │ 复制        │ 新生代      │ 多线程      │ Serial多线程版│
 * │  Parallel   │ 复制/标记整理│ 新生代/老年代│ 多线程      │ 吞吐优先    │
 * │  CMS        │ 标记清除    │ 老年代      │ 多线程      │ 低延迟      │
 * │  G1         │ 复制/标记整理│ 整堆        │ 多线程      │ 平衡吞吐延迟 │
 * │  ZGC        │ 复制        │ 整堆        │ 多线程      │ 超低延迟    │
 * └─────────────┴─────────────┴─────────────┴─────────────┴─────────────┘
 */
public class GarbageCollectorDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 垃圾回收器理解与对比 ==========\n");

        // 1. GC 基础概念
        demonstrateGCConcepts();

        // 2. 各收集器详解
        demonstrateCollectors();

        // 3. GC 调优示例
        demonstrateGCTuning();

        // 4. 内存泄漏示例
        demonstrateMemoryLeak();
    }

    /**
     * 1. GC 基础概念
     */
    private static void demonstrateGCConcepts() {
        System.out.println("【1. 垃圾回收基础概念】\n");

        System.out.println("判断对象是否存活：");
        System.out.println("1. 引用计数法");
        System.out.println("   - 每个对象维护引用计数器");
        System.out.println("   - 计数为0时回收");
        System.out.println("   - 缺点：无法解决循环引用问题\n");

        System.out.println("2. 可达性分析算法（JVM使用）");
        System.out.println("   - 从 GC Roots 开始向下搜索");
        System.out.println("   - 不可达的对象标记为垃圾");
        System.out.println("   - GC Roots 包括：");
        System.out.println("     * 虚拟机栈中引用的对象");
        System.out.println("     * 方法区中类静态属性引用的对象");
        System.out.println("     * 方法区中常量引用的对象");
        System.out.println("     * 本地方法栈中 JNI 引用的对象\n");

        System.out.println("引用类型（强度递减）：");
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│  强引用 (Strong Reference)                               │");
        System.out.println("│  Object obj = new Object();                              │");
        System.out.println("│  永不回收，即使OOM也不回收                               │");
        System.out.println("├─────────────────────────────────────────────────────────┤");
        System.out.println("│  软引用 (Soft Reference)                                 │");
        System.out.println("│  SoftReference<Object> ref = new SoftReference<>(obj);   │");
        System.out.println("│  内存不足时回收，适合缓存                                │");
        System.out.println("├─────────────────────────────────────────────────────────┤");
        System.out.println("│  弱引用 (Weak Reference)                                 │");
        System.out.println("│  WeakReference<Object> ref = new WeakReference<>(obj);   │");
        System.out.println("│  下次GC时回收，比软引用更弱                              │");
        System.out.println("├─────────────────────────────────────────────────────────┤");
        System.out.println("│  虚引用 (Phantom Reference)                              │");
        System.out.println("│  PhantomReference<Object> ref = ...                      │");
        System.out.println("│  无法获取对象，仅用于回收通知                            │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. 各收集器详解
     */
    private static void demonstrateCollectors() {
        System.out.println("【2. 垃圾回收器详解】\n");

        // Serial 收集器
        System.out.println("1. Serial / Serial Old 收集器");
        System.out.println("   算法：新生代复制，老年代标记-整理");
        System.out.println("   特点：单线程，STW（Stop The World）");
        System.out.println("   适用：客户端模式，内存小的环境");
        System.out.println("   参数：-XX:+UseSerialGC\n");

        System.out.println("   执行过程：");
        System.out.println("   应用程序 ──────→ GC ──────→ 应用程序");
        System.out.println("            ↑ STW ↑\n");

        // ParNew 收集器
        System.out.println("2. ParNew 收集器");
        System.out.println("   算法：复制算法");
        System.out.println("   特点：Serial 的多线程版本，与 CMS 配合");
        System.out.println("   适用：服务端新生代收集");
        System.out.println("   参数：-XX:+UseParNewGC\n");

        // Parallel 收集器
        System.out.println("3. Parallel Scavenge / Parallel Old 收集器");
        System.out.println("   算法：复制 / 标记-整理");
        System.out.println("   特点：吞吐量优先，自适应调节");
        System.out.println("   适用：后台计算，不需要太多交互");
        System.out.println("   参数：-XX:+UseParallelGC");
        System.out.println("        -XX:MaxGCPauseMillis=100  最大GC停顿时间");
        System.out.println("        -XX:GCTimeRatio=99        GC时间占比 1/(1+99)\n");

        // CMS 收集器
        System.out.println("4. CMS (Concurrent Mark Sweep) 收集器");
        System.out.println("   算法：标记-清除");
        System.out.println("   目标：最短停顿时间");
        System.out.println("   适用：互联网应用，B/S系统");
        System.out.println("   参数：-XX:+UseConcMarkSweepGC\n");

        System.out.println("   执行阶段：");
        System.out.println("   ┌─────────┬─────────┬─────────┬─────────┬─────────┐");
        System.out.println("   │ 初始标记 │ 并发标记 │ 重新标记 │ 并发清除 │ 并发重置 │");
        System.out.println("   │  (STW)  │ (并发)  │  (STW)  │ (并发)  │ (并发)  │");
        System.out.println("   └─────────┴─────────┴─────────┴─────────┴─────────┘");
        System.out.println("   缺点：");
        System.out.println("   - 对 CPU 敏感（并发阶段占用线程）");
        System.out.println("   - 无法处理浮动垃圾（Concurrent Mode Failure）");
        System.out.println("   - 标记-清除产生内存碎片\n");

        // G1 收集器
        System.out.println("5. G1 (Garbage First) 收集器");
        System.out.println("   算法：复制 + 标记-整理");
        System.out.println("   特点：整堆管理，分区回收，可预测停顿");
        System.out.println("   适用：大堆内存（6G以上），需要平衡吞吐和延迟");
        System.out.println("   参数：-XX:+UseG1GC");
        System.out.println("        -XX:MaxGCPauseMillis=200\n");

        System.out.println("   内存布局：");
        System.out.println("   ┌────┬────┬────┬────┬────┬────┬────┬────┬────┬────┐");
        System.out.println("   │ E  │ S0 │ S1 │ O  │ O  │ O  │ O  │ O  │ H  │ O  │ ...");
        System.out.println("   └────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘");
        System.out.println("   E=Eden, S=Survivor, O=Old, H=Humongous(大对象)\n");

        System.out.println("   回收过程：");
        System.out.println("   1. 初始标记 (STW) - 标记 GC Roots 直接关联");
        System.out.println("   2. 并发标记 - 从 GC Roots 追踪可达对象");
        System.out.println("   3. 最终标记 (STW) - 处理 SATB 队列");
        System.out.println("   4. 筛选回收 (STW) - 按回收价值排序，优先回收高价值区\n");

        // ZGC
        System.out.println("6. ZGC / Shenandoah 收集器（JDK11+）");
        System.out.println("   目标：超低延迟 (< 10ms)，支持 TB 级堆");
        System.out.println("   技术：染色指针、读屏障、并发整理");
        System.out.println("   参数：-XX:+UseZGC\n");

        System.out.println("   染色指针（Colored Pointers）：");
        System.out.println("   64位指针高4位存储元数据：");
        System.out.println("   ┌────────┬────────────────────────────────────────┐");
        System.out.println("   │ 0000   │ 0000 ... 实际地址 (44位，支持16TB)      │");
        System.out.println("   │ Finalizable │ Remapped │ Marked1 │ Marked0    │");
        System.out.println("   └────────┴────────────────────────────────────────┘\n");
    }

    /**
     * 3. GC 调优示例
     */
    private static void demonstrateGCTuning() {
        System.out.println("【3. GC 调优思路】\n");

        System.out.println("调优目标选择：");
        System.out.println("1. 吞吐量优先：科学计算、批处理");
        System.out.println("   - 使用 Parallel GC");
        System.out.println("   - 增大新生代，减少 GC 频率\n");

        System.out.println("2. 低延迟优先：Web应用、交易系统");
        System.out.println("   - 使用 CMS 或 G1");
        System.out.println("   - 控制堆大小，缩短停顿时间\n");

        System.out.println("3. 大内存优先：大数据、内存计算");
        System.out.println("   - 使用 G1 或 ZGC");
        System.out.println("   - 避免 Full GC\n");

        System.out.println("常用调优参数：");
        System.out.println("# 基础配置");
        System.out.println("-Xms8g -Xmx8g                    # 固定堆大小，避免动态调整");
        System.out.println("-Xmn3g                           # 新生代大小");
        System.out.println("-XX:MetaspaceSize=256m           # 元空间初始大小");
        System.out.println(" ");
        System.out.println("# GC 选择");
        System.out.println("-XX:+UseG1GC                     # 使用 G1");
        System.out.println("-XX:MaxGCPauseMillis=200         # 目标最大停顿时间");
        System.out.println(" ");
        System.out.println("# GC 日志（JDK9+ 统一日志）");
        System.out.println("-Xlog:gc*:file=gc.log:time:filecount=5,filesize=100m");
        System.out.println(" ");
        System.out.println("# OOM 处理");
        System.out.println("-XX:+HeapDumpOnOutOfMemoryError");
        System.out.println("-XX:HeapDumpPath=/var/log/heapdump.hprof\n");

        System.out.println("调优步骤：");
        System.out.println("1. 监控：jstat, jvisualvm, GC日志");
        System.out.println("2. 分析：GC频率、停顿时间、内存分配速率");
        System.out.println("3. 调整：根据目标选择合适的收集器和参数");
        System.out.println("4. 验证：持续监控，对比调优效果\n");
    }

    /**
     * 4. 内存泄漏示例
     */
    private static void demonstrateMemoryLeak() throws InterruptedException {
        System.out.println("【4. 内存泄漏示例】\n");

        // 设置较小的堆内存，快速触发 OOM
        // java -Xmx100m -Xms100m -XX:+PrintGC -XX:+PrintGCDetails -cp interview-service/target/classes cn.itzixiao.interview.jvm.GarbageCollectorDemo

        System.out.println("常见内存泄漏场景：");
        System.out.println("1. 静态集合类持有对象引用");
        System.out.println("2. 未关闭的资源（连接、流）");
        System.out.println("3. 监听器未移除");
        System.out.println("4. ThreadLocal 未 remove");
        System.out.println("5. 内部类持有外部类引用\n");

        // 模拟内存泄漏
        System.out.println("模拟静态集合导致的内存泄漏：");
        System.out.println("（观察内存增长，当内存不足时会抛出 OOM）\n");

        // 放开注释运行内存泄漏模拟
        // 运行前请设置较小的堆内存：-Xmx50m -Xms50m
        staticListLeakSimulation();

        System.out.println("检测工具：");
        System.out.println("1. jmap -dump:format=b,file=heap.hprof <pid>");
        System.out.println("2. jvisualvm / jconsole - 可视化监控");
        System.out.println("3. MAT (Memory Analyzer Tool) - 分析堆转储");
        System.out.println("4. Arthas - 实时诊断\n");

        System.out.println("排查步骤：");
        System.out.println("1. 确认泄漏：老年代持续增长，Full GC 无法回收");
        System.out.println("2. 生成堆转储：jmap -dump 或 -XX:+HeapDumpOnOutOfMemoryError");
        System.out.println("3. 分析 dominator_tree：找到占用最大的对象");
        System.out.println("4. 定位引用链：找到 GC Roots 路径");
        System.out.println("5. 修复代码：释放引用、使用弱引用、及时清理\n");
    }

    // 静态集合，模拟内存泄漏
    private static final List<byte[]> STATIC_LIST = new ArrayList<>();

    private static void staticListLeakSimulation() throws InterruptedException {
        System.out.println("========== 内存泄漏模拟开始 ==========");
        System.out.println("运行参数建议：-Xmx100m -Xms100m -XX:+PrintGC -XX:+PrintGCDetails");
        System.out.println("监控方式：");
        System.out.println("  1. jvisualvm 或 jconsole 连接进程查看内存曲线");
        System.out.println("  2. jstat -gcutil <pid> 1000 实时查看 GC 情况");
        System.out.println("  3. 观察 GC 日志，Full GC 频率增加但内存无法回收");
        System.out.println("  4. 按 Ctrl+C 可提前终止程序");
        System.out.println("========================================\n");

        // 等待用户连接 jconsole
        System.out.println("等待 30 秒，请用 jconsole 连接此进程...");
        System.out.println("连接后可以点击 \"内存\" 标签页观察堆内存曲线\n");
        for (int i = 30; i > 0; i--) {
            System.out.print("\r倒计时: " + i + " 秒...");
            Thread.sleep(1000);
        }
        System.out.println("\r\n开始内存泄漏模拟！\n");

        Runtime runtime = Runtime.getRuntime();

        try {
            // 持续添加对象，直到 OOM 或手动停止
            int count = 0;
            while (true) {
                // 每次添加 256KB，让程序运行更长时间便于观察
                STATIC_LIST.add(new byte[256 * 1024]);
                count++;

                // 每 50 个对象（约 12.5MB）打印一次内存状态
                if (count % 50 == 0) {
                    long maxMemory = runtime.maxMemory() / 1024 / 1024;
                    long totalMemory = runtime.totalMemory() / 1024 / 1024;
                    long freeMemory = runtime.freeMemory() / 1024 / 1024;
                    long usedMemory = totalMemory - freeMemory;
                    long listSizeMB = STATIC_LIST.size() * 256 / 1024;

                    System.out.printf("[%04d] 列表大小: %d MB | 已用: %d MB | 空闲: %d MB | 最大: %d MB%n",
                            count, listSizeMB, usedMemory, freeMemory, maxMemory);
                }

                // 每 10 个对象暂停一下，控制速度
                if (count % 10 == 0) {
                    Thread.sleep(200);
                }
            }
        } catch (OutOfMemoryError e) {
            System.out.println("\n========== OOM 发生！==========");
            System.out.println("原因：静态集合持有对象引用，GC 无法回收");
            System.out.println("最终列表大小: " + STATIC_LIST.size() * 256 / 1024 + " MB");
            System.out.println("解决方案：");
            System.out.println("  1. 避免静态集合长期持有对象");
            System.out.println("  2. 使用 WeakHashMap 或软引用");
            System.out.println("  3. 定期清理不需要的数据");
            System.out.println("================================");

            // OOM 后暂停 5 秒，方便查看结果
            Thread.sleep(5000);
        }
    }
}
