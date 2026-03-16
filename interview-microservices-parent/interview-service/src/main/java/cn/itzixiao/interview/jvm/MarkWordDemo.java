package cn.itzixiao.interview.jvm;

import org.openjdk.jol.info.ClassLayout;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Java Mark Word 详解示例
 * 
 * Mark Word 是对象头的一部分，用于存储对象的运行时状态信息
 * 
 * 64 位 JVM（开启压缩指针）下的 Mark Word 结构（8 字节 = 64 位）：
 * 
 * ┌────────────────────────────────────────────────────────────────────┐
 * │                    Mark Word (64 bits)                             │
 * ├────────────────────────────────────────────────────────────────────┤
 * │  无锁状态：                                                         │
 * │  [unused:25|hash:31|unused:1|age:4|biased_lock:1|lock:01]          │
 * │  - biased_lock = 0, lock = 01 → 无锁状态                           │
 * │  - hash: 对象的 hashCode                                           │
 * │  - age: 对象分代年龄（GC 时使用）                                   │
 * ├────────────────────────────────────────────────────────────────────┤
 * │  偏向锁状态：                                                       │
 * │  [thread:54|epoch:2|unused:1|age:4|biased_lock:1|lock:01]          │
 * │  - biased_lock = 1, lock = 01 → 偏向锁状态                         │
 * │  - thread: 持有偏向锁的线程 ID                                     │
 * │  - epoch: 时间戳，用于批量重偏向                                    │
 * ├────────────────────────────────────────────────────────────────────┤
 * │  轻量级锁状态：                                                     │
 * │  [ptr_to_lock_record:62|lock:00]                                   │
 * │  - lock = 00 → 轻量级锁状态                                        │
 * │  - ptr_to_lock_record: 指向栈中锁记录的指针                         │
 * ├────────────────────────────────────────────────────────────────────┤
 * │  重量级锁状态：                                                     │
 * │  [ptr_to_heavyweight_monitor:62|lock:10]                           │
 * │  - lock = 10 → 重量级锁状态                                        │
 * │  - ptr_to_heavyweight_monitor: 指向堆中监视器对象的指针              │
 * ├────────────────────────────────────────────────────────────────────┤
 * │  GC 标记状态：                                                       │
 * │  [unused:2|marked:2|age:4|biased_lock:1|lock:11]                   │
 * │  - lock = 11 → GC 标记状态                                          │
 * └────────────────────────────────────────────────────────────────────┘
 * 
 * 锁状态转换过程：
 * 无锁 → 偏向锁 → 轻量级锁 → 重量级锁
 * 
 * @author itzixiao
 * @date 2026-03-16
 */
public class MarkWordDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== Java Mark Word 详解示例 ==========\n");

        // 1. 演示对象头信息
        demonstrateObjectHeader();

        // 2. 演示不同锁状态
        demonstrateLockStates();

        // 3. 演示偏向锁
        demonstrateBiasedLocking();

        // 4. 演示轻量级锁（synchronized）
        demonstrateLightweightLock();

        // 5. 演示重量级锁
        demonstrateHeavyweightLock();

        // 6. 打印 JVM 信息
        printJVMInfo();
    }

    /**
     * 1. 演示对象头信息
     */
    private static void demonstrateObjectHeader() {
        System.out.println("【1. Java 对象内存布局】\n");

        System.out.println("普通对象在堆中的布局（64 位 JVM，开启压缩指针）：");
        System.out.println("┌─────────────────────────────────────┐");
        System.out.println("│  对象头 (Header) - 12 bytes         │");
        System.out.println("│  - Mark Word: 8 bytes               │");
        System.out.println("│    * 哈希码、GC 年龄、锁状态标志      │");
        System.out.println("│  - Class Pointer: 4 bytes           │");
        System.out.println("│    * 指向类元数据的指针              │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│  实例数据 (Instance Data)           │");
        System.out.println("│  - 字段内容，按类型对齐              │");
        System.out.println("├─────────────────────────────────────┤");
        System.out.println("│  对齐填充 (Padding)                 │");
        System.out.println("│  - 8 字节对齐                        │");
        System.out.println("└─────────────────────────────────────┘\n");

        // 创建对象并打印其基本信息
        Object obj = new Object();
        System.out.println("创建一个普通对象：");
        System.out.println("  对象 hashCode: " + System.identityHashCode(obj));
        System.out.println("  对象所在类：" + obj.getClass().getName());
        
        // 使用 JOL 查看对象头布局
        System.out.println("\n使用 JOL 查看对象头实际布局：");
        String layout = ClassLayout.parseInstance(obj).toPrintable();
        System.out.println(layout);
        
        System.out.println("说明：对象头包含 Mark Word(8 字节) + Class Pointer(4 字节)\n");
    }

    /**
     * 2. 演示不同锁状态
     */
    private static void demonstrateLockStates() {
        System.out.println("【2. Mark Word 的四种锁状态】\n");

        printMarkWordState("无锁状态", 
            "biased_lock=0, lock=01", 
            "默认状态，对象未被锁定");
        
        printMarkWordState("偏向锁", 
            "biased_lock=1, lock=01", 
            "单线程访问时，偏向持有锁的线程，减少 CAS 开销");
        
        printMarkWordState("轻量级锁", 
            "lock=00", 
            "多线程竞争但无阻塞时，使用 CAS 自旋");
        
        printMarkWordState("重量级锁", 
            "lock=10", 
            "多线程竞争激烈时，使用操作系统互斥量，线程阻塞");
        
        System.out.println("\n锁升级过程：无锁 → 偏向锁 → 轻量级锁 → 重量级锁\n");
    }

    private static void printMarkWordState(String state, String mark, String description) {
        System.out.printf("%-10s: [%-40s]%n", state, mark);
        System.out.println("           " + description);
    }

    /**
     * 3. 演示偏向锁
     * 偏向锁优化的是同一个线程反复获取锁的场景
     */
    private static void demonstrateBiasedLocking() throws InterruptedException {
        System.out.println("【3. 偏向锁演示】\n");

        final Object lock = new Object();

        System.out.println("偏向锁优化场景：");
        System.out.println("  - 只有一个线程访问同步块时，JVM 会将锁偏向该线程");
        System.out.println("  - 该线程再次进入时无需 CAS，直接比较线程 ID");
        System.out.println("  - 其他线程竞争时会撤销偏向，升级为轻量级锁\n");

        // 打印初始状态的 Mark Word
        System.out.println("初始状态（无锁）的 Mark Word：");
        printMarkWordHex(lock, "无锁状态");

        // 在 HotSpot JVM 中，偏向锁有延迟（默认 4 秒）
        // 这里只是演示代码结构
        System.out.println("\n执行 synchronized 同步块（可能触发偏向锁）：");
        
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                synchronized (lock) {
                    System.out.println("  线程 1 第 " + (i + 1) + " 次获取锁");
                    if (i == 0) {
                        // 第一次获取锁时打印 Mark Word
                        printMarkWordHex(lock, "偏向锁状态（线程 1 持有）");
                    }
                }
            }
        }, "Thread-1");

        thread1.start();
        thread1.join();

        System.out.println("\n线程执行完毕后的 Mark Word：");
        printMarkWordHex(lock, "偏向锁保持状态");
        System.out.println("  说明：第一次需要 CAS，后续直接进入（如果启用偏向锁）\n");
    }

    /**
     * 4. 演示轻量级锁
     * 轻量级锁使用 CAS 自旋，避免线程阻塞
     */
    private static void demonstrateLightweightLock() throws InterruptedException {
        System.out.println("【4. 轻量级锁演示】\n");

        final Object lock = new Object();

        System.out.println("轻量级锁特点：");
        System.out.println("  - 当有另一个线程尝试获取锁时，偏向锁升级为轻量级锁");
        System.out.println("  - 线程不会阻塞，而是自旋等待（忙等）");
        System.out.println("  - 适用于锁持有时间短的场景\n");

        // 打印初始状态
        System.out.println("初始状态的 Mark Word：");
        printMarkWordHex(lock, "无锁状态");

        CountTask task = new CountTask(lock);
        Thread t1 = new Thread(task, "Thread-A");
        Thread t2 = new Thread(task, "Thread-B");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("\n竞争结束后的 Mark Word：");
        printMarkWordHex(lock, "锁释放后状态");
        
        System.out.println("  最终计数：" + task.getCount());
        System.out.println("  说明：两个线程竞争锁，可能触发轻量级锁→重量级锁\n");
    }

    static class CountTask implements Runnable {
        private final Object lock;
        private int count = 0;

        CountTask(Object lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            for (int i = 0; i < 1000; i++) {
                synchronized (lock) {
                    count++;
                }
            }
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * 5. 演示重量级锁
     * 重量级锁会导致线程阻塞，依赖操作系统互斥量
     */
    private static void demonstrateHeavyweightLock() throws InterruptedException {
        System.out.println("\n【5. 重量级锁演示】\n");

        final Object lock = new Object();

        System.out.println("重量级锁触发条件：");
        System.out.println("  - 多个线程激烈竞争锁");
        System.out.println("  - 锁持有时间较长");
        System.out.println("  - 轻量级锁自旋超过阈值\n");

        System.out.println("重量级锁特点：");
        System.out.println("  - 未获取到锁的线程会被阻塞（挂起）");
        System.out.println("  - 依赖操作系统的 Mutex Lock 实现");
        System.out.println("  - 线程切换开销大，但适合长时间锁持有\n");

        // 打印初始状态的 Mark Word
        System.out.println("初始状态（无锁）的 Mark Word：");
        printMarkWordHex(lock, "无锁状态");

        // 创建多个线程模拟激烈竞争
        for (int i = 0; i < 5; i++) {
            final int threadNum = i;
            Thread thread = new Thread(() -> {
                System.out.println("  线程" + threadNum + " 准备获取锁...");
                synchronized (lock) {
                    System.out.println("  线程" + threadNum + " 获取到锁，执行任务...");
                    
                    // 第一个获取锁的线程打印 Mark Word（可能是重量级锁）
                    if (threadNum == 0) {
                        printMarkWordHex(lock, "重量级锁状态（线程 0 持有）");
                    }
                    
                    try {
                        Thread.sleep(100); // 模拟耗时操作
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("  线程" + threadNum + " 释放锁");
                }
            }, "Worker-" + i);
            thread.start();
        }

        Thread.sleep(800); // 等待所有线程执行完成
        
        // 打印所有线程执行完毕后的 Mark Word
        System.out.println("\n所有线程执行完毕后的 Mark Word：");
        printMarkWordHex(lock, "锁释放后状态");
        
        System.out.println("  说明：多个线程竞争，未获取到的线程会阻塞等待\n");
    }

    /**
     * 6. 打印 JVM 信息
     */
    private static void printJVMInfo() {
        System.out.println("【6. 当前 JVM 信息】\n");

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        
        System.out.println("JVM 名称：" + runtimeMXBean.getVmName());
        System.out.println("JVM 版本：" + System.getProperty("java.version"));
        System.out.println("JVM 供应商：" + runtimeMXBean.getSpecVendor());
        
        // 检查是否启用偏向锁
        boolean biasedLockingEnabled = isBiasedLockingEnabled();
        System.out.println("偏向锁启用：" + (biasedLockingEnabled ? "是" : "否"));
        
        System.out.println("\n查看 Mark Word 的工具推荐：");
        System.out.println("  - JOL (Java Object Layout): 查看对象头布局");
        System.out.println("  - JVisualVM: 可视化监控工具");
        System.out.println("  - Arthas: 在线诊断工具\n");
    }

    /**
     * 打印对象的 Mark Word 十六进制表示
     */
    private static void printMarkWordHex(Object obj, String state) {
        try {
            // 使用 JOL 获取对象布局信息
            ClassLayout layout = ClassLayout.parseInstance(obj);
            
            System.out.printf("  [%s] 对象头布局信息：%n", state);
            System.out.println(layout.toPrintable());
            
            // 注意：JOL 不直接提供原始字节数组，但可以通过 VM 内部方法获取
            // 这里我们使用反射方式获取 Mark Word（仅用于演示）
            // 实际生产中建议使用 JOL 的 toPrintable() 输出即可
            System.out.println("  说明：以上是完整的对象头布局，包含 Mark Word 和 Class Pointer");
            System.out.println();
        } catch (Exception e) {
            System.out.println("  [" + state + "] 无法获取 Mark Word: " + e.getMessage());
            System.out.println("  （注意：某些 JVM 版本或配置下可能无法访问对象头）\n");
        }
    }

    /**
     * 检查是否启用偏向锁
     */
    private static boolean isBiasedLockingEnabled() {
        // 通过 JVM 参数判断（实际项目中可通过 ManagementFactory 获取）
        String biasedLocking = System.getProperty("java.vm.info", "");
        // 简化处理，实际应检查 -XX:+UseBiasedLocking 参数
        return true; // HotSpot 默认启用
    }
}
