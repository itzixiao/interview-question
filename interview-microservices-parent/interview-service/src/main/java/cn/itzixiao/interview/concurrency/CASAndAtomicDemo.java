package cn.itzixiao.interview.concurrency;

import java.util.concurrent.atomic.*;
import java.util.concurrent.CountDownLatch;

/**
 * CAS 与原子类详解
 *
 * CAS (Compare And Swap) - 乐观锁实现
 * ┌─────────────────────────────────────────────────────────────┐
 * │  CAS 操作包含三个操作数：                                      │
 * │  1. 内存位置 V（变量的内存地址）                               │
 * │  2. 预期值 A（旧值）                                          │
 * │  3. 新值 B                                                    │
 * │                                                             │
 * │  操作逻辑：                                                   │
 * │  if V == A:                                                 │
 * │      V = B (交换成功)                                        │
 * │      return true                                            │
 * │  else:                                                      │
 * │      return false (交换失败，需要重试)                        │
 * │                                                             │
 * │  底层实现：CPU 指令 (cmpxchg)                                 │
 * └─────────────────────────────────────────────────────────────┘
 */
public class CASAndAtomicDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== CAS 与原子类详解 ==========\n");

        // 1. CAS 原理演示
        demonstrateCASPrinciple();

        // 2. 原子基本类型
        demonstrateAtomicBasic();

        // 3. 原子引用类型
        demonstrateAtomicReference();

        // 4. 原子数组和字段更新器
        demonstrateAtomicArrayAndUpdater();

        // 5. 原子累加器（JDK8 优化）
        demonstrateLongAdder();

        // 6. ABA 问题与解决
        demonstrateABAProblem();
    }

    /**
     * 1. CAS 原理演示
     */
    private static void demonstrateCASPrinciple() throws InterruptedException {
        System.out.println("【1. CAS 原理详解】\n");

        System.out.println("CAS 特点：");
        System.out.println("- 无锁算法，非阻塞");
        System.out.println("- 原子性由硬件指令保证");
        System.out.println("- 可能出现 ABA 问题");
        System.out.println("- 自旋消耗 CPU\n");

        // 模拟 CAS 操作
        SimulatedCAS cas = new SimulatedCAS();

        System.out.println("CAS 操作模拟：");
        System.out.println("初始值: " + cas.get());

        boolean success1 = cas.compareAndSet(0, 1);
        System.out.println("CAS(0, 1): " + success1 + ", 当前值: " + cas.get());

        boolean success2 = cas.compareAndSet(0, 2);
        System.out.println("CAS(0, 2): " + success2 + " (预期0但实际是1，失败)");

        boolean success3 = cas.compareAndSet(1, 2);
        System.out.println("CAS(1, 2): " + success3 + ", 当前值: " + cas.get());

        System.out.println("\nCAS 与 synchronized 对比：");
        System.out.println("┌─────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│   特性      │   CAS               │   synchronized      │");
        System.out.println("├─────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│  实现方式   │ 乐观锁，自旋重试     │ 悲观锁，线程阻塞     │");
        System.out.println("│  适用场景   │ 低竞争，短操作       │ 高竞争，长操作       │");
        System.out.println("│  开销       │ CPU 自旋             │ 线程上下文切换       │");
        System.out.println("│  公平性     │ 不保证               │ 可配置               │");
        System.out.println("│  可重入     │ 不支持               │ 支持                 │");
        System.out.println("└─────────────┴─────────────────────┴─────────────────────┘\n");
    }

    /**
     * 2. 原子基本类型
     */
    private static void demonstrateAtomicBasic() throws InterruptedException {
        System.out.println("【2. 原子基本类型】\n");

        // AtomicInteger
        System.out.println("AtomicInteger 演示：");
        AtomicInteger atomicInt = new AtomicInteger(0);

        System.out.println("初始值: " + atomicInt.get());
        System.out.println("getAndIncrement(): " + atomicInt.getAndIncrement() + " (先获取后自增)");
        System.out.println("incrementAndGet(): " + atomicInt.incrementAndGet() + " (先自增后获取)");
        System.out.println("addAndGet(5): " + atomicInt.addAndGet(5));
        System.out.println("compareAndSet(7, 10): " + atomicInt.compareAndSet(7, 10));
        System.out.println("当前值: " + atomicInt.get());

        // 线程安全测试
        System.out.println("\n多线程安全测试（10个线程各自增1000次）：");
        AtomicInteger counter = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("最终结果: " + counter.get() + " (预期: 10000)\n");

        // 其他原子类型
        System.out.println("其他原子基本类型：");
        System.out.println("- AtomicLong: long 类型的原子操作");
        System.out.println("- AtomicBoolean: boolean 类型的原子操作");
        System.out.println("- AtomicReference<V>: 引用类型的原子操作\n");
    }

    /**
     * 3. 原子引用类型
     */
    private static void demonstrateAtomicReference() {
        System.out.println("【3. 原子引用类型】\n");

        // AtomicReference
        System.out.println("AtomicReference 演示：");
        AtomicReference<User> userRef = new AtomicReference<>(new User("张三", 20));

        System.out.println("当前用户: " + userRef.get());

        User oldUser = userRef.get();
        User newUser = new User("李四", 25);

        boolean success = userRef.compareAndSet(oldUser, newUser);
        System.out.println("CAS 更新: " + success);
        System.out.println("更新后用户: " + userRef.get());

        // 注意：引用类型的 CAS 比较的是引用地址
        System.out.println("\n注意：引用类型 CAS 比较的是引用地址，不是内容");
        User anotherUser = new User("李四", 25);
        boolean fail = userRef.compareAndSet(anotherUser, new User("王五", 30));
        System.out.println("用新对象 CAS: " + fail + " (引用不同，即使内容相同)\n");

        // AtomicStampedReference - 解决 ABA 问题
        System.out.println("AtomicStampedReference（带版本号）：");
        AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>("A", 0);

        int[] stampHolder = new int[1];
        String value = stampedRef.get(stampHolder);
        int stamp = stampHolder[0];

        System.out.println("初始值: " + value + ", 版本: " + stamp);

        // 更新
        stampedRef.compareAndSet("A", "B", stamp, stamp + 1);
        System.out.println("A→B: " + stampedRef.getReference() + ", 版本: " + stampedRef.getStamp());

        stampedRef.compareAndSet("B", "A", stamp + 1, stamp + 2);
        System.out.println("B→A: " + stampedRef.getReference() + ", 版本: " + stampedRef.getStamp());

        System.out.println("\n即使值变回 A，版本号不同，CAS 会失败，解决 ABA 问题\n");
    }

    /**
     * 4. 原子数组和字段更新器
     */
    private static void demonstrateAtomicArrayAndUpdater() {
        System.out.println("【4. 原子数组和字段更新器】\n");

        // AtomicIntegerArray
        System.out.println("AtomicIntegerArray 演示：");
        AtomicIntegerArray array = new AtomicIntegerArray(5);

        System.out.println("初始数组: " + array);
        array.set(0, 10);
        array.addAndGet(1, 5);
        array.incrementAndGet(2);

        System.out.println("设置后: " + array);
        System.out.println("getAndAdd(0, 3): " + array.getAndAdd(0, 3));
        System.out.println("最终数组: " + array);

        // AtomicReferenceArray
        System.out.println("\nAtomicReferenceArray 演示：");
        AtomicReferenceArray<String> strArray = new AtomicReferenceArray<>(3);
        strArray.set(0, "Hello");
        strArray.set(1, "World");
        System.out.println("字符串数组: [" + strArray.get(0) + ", " + strArray.get(1) + "]\n");

        // 字段更新器
        System.out.println("AtomicIntegerFieldUpdater 演示：");
        System.out.println("用途：对已有类的字段进行原子操作，无需修改类定义\n");

        AtomicIntegerFieldUpdater<VolatileData> updater =
                AtomicIntegerFieldUpdater.newUpdater(VolatileData.class, "value");

        VolatileData data = new VolatileData();
        System.out.println("初始值: " + data.getValue());

        updater.incrementAndGet(data);
        System.out.println("自增后: " + data.getValue());

        updater.addAndGet(data, 10);
        System.out.println("加10后: " + data.getValue() + "\n");
    }

    /**
     * 5. LongAdder - JDK8 优化
     */
    private static void demonstrateLongAdder() throws InterruptedException {
        System.out.println("【5. LongAdder - 高并发优化】\n");

        System.out.println("LongAdder 原理：");
        System.out.println("- 内部维护一个 base 值和一个 Cell 数组");
        System.out.println("- 无竞争时直接操作 base");
        System.out.println("- 有竞争时各线程分散到不同 Cell");
        System.out.println("- 获取结果时累加所有 Cell 和 base\n");

        System.out.println("AtomicLong vs LongAdder 性能对比：");

        // 测试 AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long start1 = System.currentTimeMillis();
        CountDownLatch latch1 = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000000; j++) {
                    atomicLong.incrementAndGet();
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("AtomicLong: " + time1 + "ms, 结果: " + atomicLong.get());

        // 测试 LongAdder
        LongAdder longAdder = new LongAdder();
        long start2 = System.currentTimeMillis();
        CountDownLatch latch2 = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000000; j++) {
                    longAdder.increment();
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("LongAdder: " + time2 + "ms, 结果: " + longAdder.sum());

        System.out.println("\n结论：高并发下 LongAdder 性能显著优于 AtomicLong\n");

        // DoubleAdder
        System.out.println("DoubleAdder 和 LongAccumulator：");
        DoubleAdder doubleAdder = new DoubleAdder();
        doubleAdder.add(1.5);
        doubleAdder.add(2.5);
        System.out.println("DoubleAdder sum: " + doubleAdder.sum());

        // LongAccumulator - 自定义累加函数
        LongAccumulator accumulator = new LongAccumulator(Long::max, 0);
        accumulator.accumulate(5);
        accumulator.accumulate(10);
        accumulator.accumulate(3);
        System.out.println("LongAccumulator max: " + accumulator.get() + "\n");
    }

    /**
     * 6. ABA 问题与解决
     */
    private static void demonstrateABAProblem() {
        System.out.println("【6. ABA 问题与解决】\n");

        System.out.println("ABA 问题描述：");
        System.out.println("1. 线程1读取值为 A");
        System.out.println("2. 线程2将 A 改为 B，再改回 A");
        System.out.println("3. 线程1 CAS 时发现值还是 A，操作成功");
        System.out.println("4. 但实际上值已经经历过变化，可能引发问题\n");

        System.out.println("ABA 问题示例（链表操作）：");
        System.out.println("初始: A → B → C");
        System.out.println("线程1: 想删除 A，使 B 成为头节点");
        System.out.println("线程2: 删除 B，链表变为 A → C，再插入新 B，链表变为 A → B' → C");
        System.out.println("线程1: CAS 成功，但 B 已经不是原来的 B，可能导致 C 丢失\n");

        System.out.println("解决方案：");
        System.out.println("1. AtomicStampedReference - 增加版本号");
        System.out.println("2. AtomicMarkableReference - 增加标记位");
        System.out.println("3. 使用传统的互斥锁\n");

        System.out.println("AtomicStampedReference 使用要点：");
        System.out.println("- 每次更新时版本号 +1");
        System.out.println("- CAS 时同时检查值和版本号");
        System.out.println("- 即使值相同，版本号不同也会失败\n");
    }

    // ==================== 辅助类 ====================

    static class SimulatedCAS {
        private volatile int value;

        public synchronized int get() {
            return value;
        }

        public synchronized boolean compareAndSet(int expected, int newValue) {
            if (value == expected) {
                value = newValue;
                return true;
            }
            return false;
        }
    }

    static class User {
        private final String name;
        private final int age;

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + "}";
        }
    }

    static class VolatileData {
        public volatile int value = 0;

        public int getValue() {
            return value;
        }
    }
}
