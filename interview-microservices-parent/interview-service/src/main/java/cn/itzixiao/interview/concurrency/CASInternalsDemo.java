package cn.itzixiao.interview.concurrency;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * CAS (Compare And Swap) 底层原理详解
 * <p>
 * CAS 核心思想：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  比较内存位置的当前值和预期值，如果相等则更新为新值            │
 * │  整个操作是原子的，由硬件指令保证                              │
 * │                                                             │
 * │  伪代码：                                                    │
 * │  boolean compareAndSwap(V, A, B) {                          │
 * │      if (V == A) {  // V: 内存值, A: 预期值                 │
 * │          V = B;     // B: 新值                              │
 * │          return true;                                       │
 * │      }                                                      │
 * │      return false;                                          │
 * │  }                                                          │
 * └─────────────────────────────────────────────────────────────┘
 */
@SuppressWarnings("restriction")
public class CASInternalsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== CAS 底层原理详解 ==========\n");

        // 1. CAS 硬件基础
        demonstrateHardwareBasis();

        // 2. Unsafe 类详解
        demonstrateUnsafe();

        // 3. 原子类实现
        demonstrateAtomicClasses();

        // 4. CAS 问题与解决
        demonstrateCASProblems();

        // 5. 性能优化
        demonstratePerformanceOptimization();
    }

    /**
     * 1. CAS 硬件基础
     */
    private static void demonstrateHardwareBasis() {
        System.out.println("【1. CAS 硬件基础】\n");

        System.out.println("CPU 指令支持：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  x86 架构：cmpxchg 指令                                     │");
        System.out.println("│  - compare and exchange                                     │");
        System.out.println("│  - 单核：直接执行                                           │");
        System.out.println("│  - 多核：需要加 lock 前缀（lock cmpxchg）                   │");
        System.out.println("│    lock 前缀作用：锁定内存总线，保证操作原子性               │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  ARM 架构：LDREX + STREX 指令对                             │");
        System.out.println("│  - Load-Exclusive 和 Store-Exclusive                       │");
        System.out.println("│  - 通过独占监视器实现原子性                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("MESI 缓存一致性协议：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  CPU 缓存行状态：                                            │");
        System.out.println("│  - M (Modified)：已修改，与内存不一致，独占                  │");
        System.out.println("│  - E (Exclusive)：独占，与内存一致                          │");
        System.out.println("│  - S (Shared)：共享，多个 CPU 缓存相同数据                  │");
        System.out.println("│  - I (Invalid)：无效，需要重新从内存加载                    │");
        System.out.println("│                                                             │");
        System.out.println("│  lock 前缀作用：                                             │");
        System.out.println("│  1. 锁定内存总线（老 CPU）或缓存行（新 CPU）                 │");
        System.out.println("│  2. 将当前 CPU 缓存行写回内存                               │");
        System.out.println("│  3. 使其他 CPU 对应缓存行失效                               │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. Unsafe 类详解
     */
    private static void demonstrateUnsafe() throws Exception {
        System.out.println("【2. Unsafe 类 - CAS 的 Java 实现】\n");

        System.out.println("Unsafe 类特点：");
        System.out.println("- 位于 sun.misc 包下，不推荐使用（JDK9+ 限制访问）");
        System.out.println("- 提供底层内存操作、CAS、线程调度等功能");
        System.out.println("- 标准 API 无法完成的工作，Unsafe 可以做到\n");

        System.out.println("Unsafe 核心 CAS 方法：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 比较并交换对象字段                                       │");
        System.out.println("│  boolean compareAndSwapObject(Object o, long offset,        │");
        System.out.println("│                               Object expected,              │");
        System.out.println("│                               Object x)                     │");
        System.out.println("│                                                             │");
        System.out.println("│  // 比较并交换 int                                          │");
        System.out.println("│  boolean compareAndSwapInt(Object o, long offset,           │");
        System.out.println("│                            int expected,                    │");
        System.out.println("│                            int x)                           │");
        System.out.println("│                                                             │");
        System.out.println("│  // 比较并交换 long                                         │");
        System.out.println("│  boolean compareAndSwapLong(Object o, long offset,          │");
        System.out.println("│                             long expected,                  │");
        System.out.println("│                             long x)                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("获取字段偏移量：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 获取对象字段的内存偏移量                                 │");
        System.out.println("│  long objectFieldOffset(Field f)                            │");
        System.out.println("│                                                             │");
        System.out.println("│  // 示例                                                    │");
        System.out.println("│  AtomicInteger ai = new AtomicInteger(0);                   │");
        System.out.println("│  Unsafe unsafe = getUnsafe();                               │");
        System.out.println("│  long valueOffset = unsafe.objectFieldOffset(               │");
        System.out.println("│      AtomicInteger.class.getDeclaredField(\"value\"));       │");
        System.out.println("│  // valueOffset 就是 value 字段在对象中的内存位置             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        // 演示 Unsafe 使用
        System.out.println("Unsafe CAS 操作演示：");

        // 通过反射获取 Unsafe 实例（仅演示，生产环境不推荐）
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);

        // 创建一个简单的类用于测试
        class TestObject {
            private int value = 0;
        }

        TestObject obj = new TestObject();
        Field valueField = TestObject.class.getDeclaredField("value");
        long offset = unsafe.objectFieldOffset(valueField);

        System.out.println("初始值: " + unsafe.getInt(obj, offset));

        // CAS 操作
        boolean success = unsafe.compareAndSwapInt(obj, offset, 0, 100);
        System.out.println("CAS(0, 100): " + success + ", 当前值: " + unsafe.getInt(obj, offset));

        success = unsafe.compareAndSwapInt(obj, offset, 0, 200);
        System.out.println("CAS(0, 200): " + success + " (预期0但实际是100，失败)");

        success = unsafe.compareAndSwapInt(obj, offset, 100, 200);
        System.out.println("CAS(100, 200): " + success + ", 当前值: " + unsafe.getInt(obj, offset));
        System.out.println();
    }

    /**
     * 3. 原子类实现
     */
    private static void demonstrateAtomicClasses() {
        System.out.println("【3. 原子类实现原理】\n");

        System.out.println("AtomicInteger 核心代码：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  public class AtomicInteger extends Number                  │");
        System.out.println("│          implements java.io.Serializable {                  │");
        System.out.println("│                                                             │");
        System.out.println("│      private volatile int value;  // volatile 保证可见性    │");
        System.out.println("│                                                             │");
        System.out.println("│      private static final Unsafe unsafe = Unsafe.getUnsafe();│");
        System.out.println("│      private static final long valueOffset;                 │");
        System.out.println("│                                                             │");
        System.out.println("│      static {                                               │");
        System.out.println("│          try {                                              │");
        System.out.println("│              valueOffset = unsafe.objectFieldOffset(        │");
        System.out.println("│                  AtomicInteger.class.getDeclaredField(\"value\"));│");
        System.out.println("│          } catch (Exception ex) { throw new Error(ex); }    │");
        System.out.println("│      }                                                      │");
        System.out.println("│                                                             │");
        System.out.println("│      public final int incrementAndGet() {                   │");
        System.out.println("│          return unsafe.getAndAddInt(this, valueOffset, 1) + 1;│");
        System.out.println("│      }                                                      │");
        System.out.println("│                                                             │");
        System.out.println("│      public final boolean compareAndSet(int expect, int update) {│");
        System.out.println("│          return unsafe.compareAndSwapInt(this, valueOffset, │");
        System.out.println("│                                        expect, update);     │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("getAndAddInt 的 JDK8 实现：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  public final int getAndAddInt(Object o, long offset, int delta) {│");
        System.out.println("│      int v;                                                 │");
        System.out.println("│      do {                                                   │");
        System.out.println("│          v = getIntVolatile(o, offset);  // 获取当前值      │");
        System.out.println("│      } while (!compareAndSwapInt(o, offset, v, v + delta)); │");
        System.out.println("│      // CAS 失败则重试（自旋）                               │");
        System.out.println("│      return v;                                              │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("AtomicLong 的 LongAdder 优化（JDK8）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  问题：高并发下 AtomicLong 成为热点                          │");
        System.out.println("│        所有线程竞争同一个变量的 CAS                           │");
        System.out.println("│                                                             │");
        System.out.println("│  解决：分散热点（类似 ConcurrentHashMap 的计数器）           │");
        System.out.println("│                                                             │");
        System.out.println("│  LongAdder 结构：                                            │");
        System.out.println("│  - base：基础值，低并发时使用                                │");
        System.out.println("│  - Cell[] cells：高并发时分散到多个单元                      │");
        System.out.println("│  - 每个线程通过 ThreadLocalRandom 选择 Cell                  │");
        System.out.println("│                                                             │");
        System.out.println("│  最终值 = base + sum(cells)                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 4. CAS 问题与解决
     */
    private static void demonstrateCASProblems() {
        System.out.println("【4. CAS 的问题与解决】\n");

        System.out.println("问题1：ABA 问题");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  场景：                                                      │");
        System.out.println("│  1. 线程1读取值为 A                                          │");
        System.out.println("│  2. 线程2将 A 改为 B，再改回 A                               │");
        System.out.println("│  3. 线程1 CAS 时发现值还是 A，操作成功                       │");
        System.out.println("│  4. 但实际上值已经经历过变化，可能引发问题                   │");
        System.out.println("│                                                             │");
        System.out.println("│  示例（链表操作）：                                          │");
        System.out.println("│  初始: A → B → C                                            │");
        System.out.println("│  线程1: 想删除 A，使 B 成为头节点                            │");
        System.out.println("│  线程2: 删除 B，链表变为 A → C，再插入新 B，链表变为 A → B' → C│");
        System.out.println("│  线程1: CAS 成功，但 B 已经不是原来的 B，C 丢失              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("ABA 解决方案：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. AtomicStampedReference（版本号）                         │");
        System.out.println("│     - 每个值带一个 stamp（版本号）                           │");
        System.out.println("│     - 修改时同时检查值和版本号                               │");
        System.out.println("│     - 即使值相同，版本号不同也会失败                         │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. AtomicMarkableReference（标记位）                        │");
        System.out.println("│     - 每个值带一个 boolean 标记                              │");
        System.out.println("│     - 用于标记对象是否被删除                                 │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. 使用传统的互斥锁                                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        // 演示 AtomicStampedReference
        System.out.println("AtomicStampedReference 演示：");
        AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>("A", 0);

        int[] stampHolder = new int[1];
        String value = stampedRef.get(stampHolder);
        int stamp = stampHolder[0];
        System.out.println("初始值: " + value + ", 版本: " + stamp);

        // A → B
        stampedRef.compareAndSet("A", "B", stamp, stamp + 1);
        System.out.println("A→B: " + stampedRef.getReference() + ", 版本: " + stampedRef.getStamp());

        // B → A
        stampedRef.compareAndSet("B", "A", stamp + 1, stamp + 2);
        System.out.println("B→A: " + stampedRef.getReference() + ", 版本: " + stampedRef.getStamp());

        // 用旧版本号尝试更新（失败）
        boolean success = stampedRef.compareAndSet("A", "C", stamp, stamp + 1);
        System.out.println("用旧版本号 CAS: " + success + " (版本号不同，失败)\n");

        System.out.println("问题2：自旋开销");
        System.out.println("- 高并发下 CAS 失败率高，CPU 空转");
        System.out.println("- 解决：自适应自旋、指数退避、转换为重量级锁\n");

        System.out.println("问题3：只能保证单个变量原子性");
        System.out.println("- 多个变量需要同时更新时无法使用 CAS");
        System.out.println("- 解决：使用 synchronized、Lock、或者将多个变量封装成一个对象\n");
    }

    /**
     * 5. 性能优化
     */
    private static void demonstratePerformanceOptimization() {
        System.out.println("【5. CAS 性能优化】\n");

        System.out.println("缓存行填充（Cache Line Padding）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  问题：伪共享（False Sharing）                               │");
        System.out.println("│  - CPU 缓存行通常为 64 字节                                  │");
        System.out.println("│  - 不同变量在同一缓存行，一个线程修改会导致其他线程缓存失效   │");
        System.out.println("│                                                             │");
        System.out.println("│  解决：@Contended 注解（JDK8）或手动填充                     │");
        System.out.println("│  ┌─────────────────────────────────────────────────────┐   │");
        System.out.println("│  │  @sun.misc.Contended                                │   │");
        System.out.println("│  │  static final class Cell {                          │   │");
        System.out.println("│  │      volatile long value;                           │   │");
        System.out.println("│  │  }                                                  │   │");
        System.out.println("│  │  // JVM 会在 value 前后填充 128 字节，避免伪共享    │   │");
        System.out.println("│  └─────────────────────────────────────────────────────┘   │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("减少 CAS 冲突：");
        System.out.println("1. 分散热点（LongAdder、ConcurrentHashMap 计数器）");
        System.out.println("2. 批量操作，减少 CAS 次数");
        System.out.println("3. 根据场景选择：低并发用 AtomicLong，高并发用 LongAdder\n");

        System.out.println("自适应自旋：");
        System.out.println("- 根据历史成功率调整自旋次数");
        System.out.println("- 成功率高：增加自旋次数");
        System.out.println("- 成功率低：减少自旋次数或直接阻塞\n");

        System.out.println("CAS vs synchronized 选择：");
        System.out.println("┌─────────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│   场景          │   推荐方案          │   原因              │");
        System.out.println("├─────────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│  低竞争         │   CAS               │  无锁，开销小       │");
        System.out.println("│  高竞争         │   synchronized/Lock │  避免自旋浪费 CPU   │");
        System.out.println("│  单个变量       │   CAS               │  简单高效           │");
        System.out.println("│  多个变量       │   synchronized/Lock │  保证原子性         │");
        System.out.println("│  需要可重入     │   Lock              │  CAS 不直接支持     │");
        System.out.println("└─────────────────┴─────────────────────┴─────────────────────┘\n");
    }
}
