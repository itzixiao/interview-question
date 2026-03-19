package cn.itzixiao.interview.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 线程同步机制详解
 * <p>
 * 锁的分类：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  悲观锁 vs 乐观锁                                            │
 * │  - 悲观锁：synchronized, Lock（先加锁再操作）                 │
 * │  - 乐观锁：CAS（Compare And Swap，先操作再验证）              │
 * ├─────────────────────────────────────────────────────────────┤
 * │  公平锁 vs 非公平锁                                          │
 * │  - 公平锁：按请求顺序获取锁（ReentrantLock(true)）            │
 * │  - 非公平锁：允许插队，吞吐量更高（默认）                      │
 * ├─────────────────────────────────────────────────────────────┤
 * │  可重入锁 vs 不可重入锁                                       │
 * │  - 可重入锁：同一线程可多次获取同一把锁（synchronized, Lock）  │
 * │  - 不可重入锁：同一线程不能重复获取                           │
 * ├─────────────────────────────────────────────────────────────┤
 * │  独占锁 vs 共享锁                                            │
 * │  - 独占锁：只有一个线程能访问（写锁）                          │
 * │  - 共享锁：多个线程可同时访问（读锁）                          │
 * └─────────────────────────────────────────────────────────────┘
 */
public class SynchronizationDemo {

    private int count = 0;
    private final Object lock = new Object();
    private final ReentrantLock reentrantLock = new ReentrantLock();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    public static void main(String[] args) throws Exception {
        System.out.println("========== 线程同步机制详解 ==========\n");

        SynchronizationDemo demo = new SynchronizationDemo();

        // 1. synchronized 关键字
        demo.demonstrateSynchronized();

        // 2. Lock 接口
        demo.demonstrateLock();

        // 3. 读写锁
        demo.demonstrateReadWriteLock();

        // 4. 死锁演示与避免
        demonstrateDeadlock();

        // 5. volatile 关键字
        demonstrateVolatile();
    }

    /**
     * 1. synchronized 关键字详解
     */
    private void demonstrateSynchronized() throws InterruptedException {
        System.out.println("【1. synchronized 关键字】\n");

        System.out.println("synchronized 的三种使用方式：");
        System.out.println("1. 同步实例方法：锁当前对象（this）");
        System.out.println("2. 同步静态方法：锁类的 Class 对象");
        System.out.println("3. 同步代码块：锁指定对象\n");

        // 演示线程安全问题
        System.out.println("线程安全问题演示（无同步）：");
        count = 0;
        Thread[] threads = new Thread[10];

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    count++; // 非原子操作
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }
        System.out.println("预期结果: 10000, 实际结果: " + count + " （线程不安全）\n");

        // 使用 synchronized 解决
        System.out.println("使用 synchronized 同步方法：");
        count = 0;
        SynchronizedCounter counter = new SynchronizedCounter();

        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }
        System.out.println("预期结果: 10000, 实际结果: " + counter.getCount() + " （线程安全）\n");

        // synchronized 原理
        System.out.println("synchronized 底层原理：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  Java 对象头中的 Mark Word 存储锁信息                         │");
        System.out.println("│                                                             │");
        System.out.println("│  锁升级过程（JDK6 优化）：                                    │");
        System.out.println("│  无锁 → 偏向锁 → 轻量级锁 → 重量级锁                          │");
        System.out.println("│                                                             │");
        System.out.println("│  1. 偏向锁：只有一个线程访问，Mark Word 记录线程ID            │");
        System.out.println("│  2. 轻量级锁：多个线程竞争，CAS 自旋获取锁                    │");
        System.out.println("│  3. 重量级锁：自旋失败，线程阻塞，操作系统调度                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. Lock 接口详解
     */
    private void demonstrateLock() throws InterruptedException {
        System.out.println("【2. Lock 接口】\n");

        System.out.println("Lock vs synchronized 对比：");
        System.out.println("┌─────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│   特性      │   synchronized      │   Lock              │");
        System.out.println("├─────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│  获取方式   │ 自动获取/释放        │ 手动 lock/unlock    │");
        System.out.println("│  可中断     │ 不可中断            │ 可中断 lockInterruptibly │");
        System.out.println("│  超时获取   │ 不支持              │ 支持 tryLock(timeout) │");
        System.out.println("│  公平锁     │ 非公平              │ 可配置公平/非公平    │");
        System.out.println("│  条件变量   │ 一个（wait/notify）  │ 多个 Condition      │");
        System.out.println("│  性能       │ JDK6+ 优化后接近     │ 竞争激烈时略优      │");
        System.out.println("└─────────────┴─────────────────────┴─────────────────────┘\n");

        // ReentrantLock 演示
        System.out.println("ReentrantLock 演示：");
        LockCounter lockCounter = new LockCounter();
        Thread[] threads = new Thread[5];

        for (int i = 0; i < 5; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    lockCounter.increment();
                }
            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }
        System.out.println("计数结果: " + lockCounter.getCount() + "\n");

        // tryLock 超时演示
        System.out.println("tryLock 超时获取锁演示：");
        ReentrantLock tryLock = new ReentrantLock();

        Thread holder = new Thread(() -> {
            tryLock.lock();
            System.out.println("  线程1：获取锁，持有5秒");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                tryLock.unlock();
                System.out.println("  线程1：释放锁");
            }
        });

        Thread waiter = new Thread(() -> {
            System.out.println("  线程2：尝试获取锁（最多等待2秒）");
            try {
                if (tryLock.tryLock(2, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("  线程2：成功获取锁");
                    } finally {
                        tryLock.unlock();
                    }
                } else {
                    System.out.println("  线程2：获取锁超时，放弃");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        holder.start();
        Thread.sleep(100);
        waiter.start();
        holder.join();
        waiter.join();
        System.out.println();
    }

    /**
     * 3. 读写锁
     */
    private void demonstrateReadWriteLock() throws InterruptedException {
        System.out.println("【3. 读写锁 (ReadWriteLock)】\n");

        System.out.println("读写锁规则：");
        System.out.println("- 读读不互斥：多个读线程可同时访问");
        System.out.println("- 读写互斥：读时不能写，写时不能读");
        System.out.println("- 写写互斥：多个写线程互斥\n");

        ReadWriteLockData data = new ReadWriteLockData();

        // 创建多个读线程
        Thread[] readers = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final int index = i;
            readers[i] = new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    System.out.println("  读线程" + index + " 第" + j + "次读取: " + data.read());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }, "Reader-" + i);
        }

        // 创建写线程
        Thread writer = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                data.write(i);
                System.out.println("  写线程写入: " + i);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Writer");

        // 启动所有线程
        for (Thread reader : readers) {
            reader.start();
        }
        writer.start();

        for (Thread reader : readers) {
            reader.join();
        }
        writer.join();
        System.out.println();
    }

    /**
     * 4. 死锁演示与避免
     */
    private static void demonstrateDeadlock() throws InterruptedException {
        System.out.println("【4. 死锁与避免】\n");

        System.out.println("死锁产生的四个必要条件：");
        System.out.println("1. 互斥条件：资源一次只能被一个线程占用");
        System.out.println("2. 请求与保持：线程持有资源同时请求新资源");
        System.out.println("3. 不剥夺条件：已获得的资源不能被强制剥夺");
        System.out.println("4. 循环等待条件：线程之间形成资源请求环路\n");

        // 死锁演示
        System.out.println("死锁演示（观察后程序会卡住）：");
        Object lockA = new Object();
        Object lockB = new Object();

        Thread thread1 = new Thread(() -> {
            synchronized (lockA) {
                System.out.println("  线程1：获取锁A");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("  线程1：等待锁B...");
                synchronized (lockB) {
                    System.out.println("  线程1：获取锁B");
                }
            }
        }, "Deadlock-1");

        Thread thread2 = new Thread(() -> {
            synchronized (lockB) {
                System.out.println("  线程2：获取锁B");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("  线程2：等待锁A...");
                synchronized (lockA) {
                    System.out.println("  线程2：获取锁A");
                }
            }
        }, "Deadlock-2");

        thread1.start();
        thread2.start();

        // 等待2秒后中断
        Thread.sleep(2000);
        System.out.println("  检测到死锁，中断线程...\n");
        thread1.interrupt();
        thread2.interrupt();

        System.out.println("死锁避免策略：");
        System.out.println("1. 破坏请求与保持：一次性申请所有资源");
        System.out.println("2. 破坏循环等待：按固定顺序申请资源");
        System.out.println("3. 使用 tryLock 超时机制");
        System.out.println("4. 使用并发工具类替代手动加锁\n");
    }

    /**
     * 5. volatile 关键字与内存屏障源码级详解
     */
    private static void demonstrateVolatile() throws InterruptedException {
        System.out.println("【5. volatile 关键字 - JMM 与内存屏障源码级详解】\n");

        // JMM 详细说明
        System.out.println("======================================================================");
        System.out.println("【Java 内存模型（JMM）详解】");
        System.out.println("======================================================================\n");

        System.out.println("1. JMM 是什么？");
        System.out.println("   JMM（Java Memory Model）是 Java 虚拟机规范定义的一套内存模型，");
        System.out.println("   用于规范多线程环境下共享变量的访问规则，确保并发程序的正确性。\n");

        System.out.println("2. JMM 的核心抽象结构：");
        System.out.println("   ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("   │                      主内存（Main Memory）                   │");
        System.out.println("   │                   所有共享变量的存储位置                      │");
        System.out.println("   └──────────────┬──────────────────────────────┬───────────────┘");
        System.out.println("                  │                              │");
        System.out.println("          read/load│                              │store/write");
        System.out.println("                  │                              │");
        System.out.println("   ┌───────────────▼──────────────┐  ┌───────────▼────────────────┐");
        System.out.println("   │      工作内存（线程1）        │  │      工作内存（线程2）      │");
        System.out.println("   │  ┌────────────────────────┐  │  │  ┌──────────────────────┐  │");
        System.out.println("   │  │  共享变量副本 x = ?    │  │  │  │  共享变量副本 x = ?  │  │");
        System.out.println("   │  │  共享变量副本 y = ?    │  │  │  │  共享变量副本 y = ?  │  │");
        System.out.println("   │  └────────────────────────┘  │  │  └──────────────────────┘  │");
        System.out.println("   │        use/assign            │  │        use/assign          │");
        System.out.println("   └──────────────────────────────┘  └────────────────────────────┘\n");

        System.out.println("3. JMM 定义的 8 种内存交互操作：");
        System.out.println("   ┌────────────┬────────────────────────────────────────────────────┐");
        System.out.println("   │   操作     │                    说明                             │");
        System.out.println("   ├────────────┼────────────────────────────────────────────────────┤");
        System.out.println("   │   lock     │  作用于主内存，将变量标识为线程独占（互斥）          │");
        System.out.println("   │   unlock   │  作用于主内存，释放变量的锁定状态                    │");
        System.out.println("   │   read     │  作用于主内存，将变量值传输到工作内存                │");
        System.out.println("   │   load     │  作用于工作内存，将 read 的值放入变量副本            │");
        System.out.println("   │   use      │  作用于工作内存，将变量值传递给执行引擎              │");
        System.out.println("   │   assign   │  作用于工作内存，将执行引擎值赋给变量副本            │");
        System.out.println("   │   store    │  作用于工作内存，将变量值传送到主内存                │");
        System.out.println("   │   write    │  作用于主内存，将 store 的值写入变量                 │");
        System.out.println("   └────────────┴────────────────────────────────────────────────────┘\n");

        System.out.println("4. JMM 的三大特性：");
        System.out.println("   ┌───────────┬─────────────────────────────────────────────────────┐");
        System.out.println("   │   特性    │                    说明                              │");
        System.out.println("   ├───────────┼─────────────────────────────────────────────────────┤");
        System.out.println("   │  原子性   │  操作不可分割，要么全成功要么全失败                   │");
        System.out.println("   │           │  保证：synchronized、Lock、原子类                    │");
        System.out.println("   ├───────────┼─────────────────────────────────────────────────────┤");
        System.out.println("   │  可见性   │  一个线程修改，其他线程立即可见                       │");
        System.out.println("   │           │  保证：volatile、synchronized、Lock、final           │");
        System.out.println("   ├───────────┼─────────────────────────────────────────────────────┤");
        System.out.println("   │  有序性   │  程序按代码顺序执行，禁止指令重排序                   │");
        System.out.println("   │           │  保证：volatile（禁止重排）、synchronized            │");
        System.out.println("   └───────────┴─────────────────────────────────────────────────────┘\n");

        System.out.println("5. happens-before 规则（JMM 核心规则）：");
        System.out.println("   如果操作 A happens-before 操作 B，那么 A 的结果对 B 可见。\n");
        System.out.println("   ┌────────────────────────────────────────────────────────────────┐");
        System.out.println("   │  规则1：程序顺序规则                                            │");
        System.out.println("   │         同一个线程中，前面的操作 happens-before 后面的操作       │");
        System.out.println("   │                                                                │");
        System.out.println("   │  规则2：volatile 变量规则                                       │");
        System.out.println("   │         volatile 写 happens-before volatile 读                 │");
        System.out.println("   │                                                                │");
        System.out.println("   │  规则3：锁规则                                                  │");
        System.out.println("   │         unlock happens-before 后续对同一锁的 lock               │");
        System.out.println("   │                                                                │");
        System.out.println("   │  规则4：传递性                                                  │");
        System.out.println("   │         A happens-before B，B happens-before C，则 A hb C      │");
        System.out.println("   │                                                                │");
        System.out.println("   │  规则5：线程启动规则                                            │");
        System.out.println("   │         Thread.start() happens-before 线程内所有操作            │");
        System.out.println("   │                                                                │");
        System.out.println("   │  规则6：线程终止规则                                            │");
        System.out.println("   │         线程内所有操作 happens-before 线程终止检测              │");
        System.out.println("   │                                                                │");
        System.out.println("   │  规则7：中断规则                                                │");
        System.out.println("   │         interrupt() happens-before 检测到中断                  │");
        System.out.println("   │                                                                │");
        System.out.println("   │  规则8：对象终结规则                                            │");
        System.out.println("   │         构造函数执行 happens-before finalize()                 │");
        System.out.println("   └────────────────────────────────────────────────────────────────┘\n");

        System.out.println("======================================================================");
        System.out.println("【volatile 关键字详解】");
        System.out.println("======================================================================\n");

        System.out.println("volatile 的作用：");
        System.out.println("1. 保证可见性：一个线程修改，其他线程立即可见");
        System.out.println("2. 禁止指令重排序：通过内存屏障实现\n");

        System.out.println("volatile 不能保证原子性！");
        System.out.println("例如：count++ 不是原子操作（读取-修改-写入）\n");

        // 可见性演示
        System.out.println("可见性演示：");
        VolatileExample example = new VolatileExample();

        Thread reader = new Thread(() -> {
            System.out.println("  读线程：等待 flag 变为 true");
            while (!example.flag) {
                // 忙等待
            }
            System.out.println("  读线程：检测到 flag = " + example.flag);
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(1000);
                System.out.println("  写线程：设置 flag = true");
                example.flag = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        reader.start();
        writer.start();
        reader.join();
        writer.join();

        // 内存屏障源码级详解
        System.out.println("\n======================================================================");
        System.out.println("【内存屏障（Memory Barrier）源码级详解】");
        System.out.println("======================================================================\n");

        // 1. 四种内存屏障类型
        System.out.println("1. 四种内存屏障类型（JVM 规范定义）：\n");
        System.out.println("┌─────────────┬────────────────────────────────────────────────────┐");
        System.out.println("│  屏障类型    │  作用描述                                           │");
        System.out.println("├─────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│  LoadLoad   │  确保 Load1 在 Load2 之前执行（禁止读-读重排序）      │");
        System.out.println("│             │  指令序列: Load1; LoadLoad; Load2                   │");
        System.out.println("├─────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│  StoreStore │  确保 Store1 在 Store2 之前执行（禁止写-写重排序）    │");
        System.out.println("│             │  指令序列: Store1; StoreStore; Store2               │");
        System.out.println("├─────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│  LoadStore  │  确保 Load1 在 Store2 之前执行（禁止读-写重排序）     │");
        System.out.println("│             │  指令序列: Load1; LoadStore; Store2                 │");
        System.out.println("├─────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│  StoreLoad  │  确保 Store1 在 Load2 之前执行（禁止写-读重排序）     │");
        System.out.println("│             │  指令序列: Store1; StoreLoad; Load2                 │");
        System.out.println("│             │  【开销最大】：会使该屏障之前的所有内存访问指令都完成  │");
        System.out.println("└─────────────┴────────────────────────────────────────────────────┘\n");

        // 2. volatile 内存屏障插入策略
        System.out.println("2. volatile 内存屏障插入策略（JVM 编译期处理）：\n");
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【volatile 写操作】 - JVM 在编译时插入屏障                           │");
        System.out.println("│                                                                      │");
        System.out.println("│      [普通写操作]                                                     │");
        System.out.println("│          ↓                                                           │");
        System.out.println("│      StoreStore 屏障  ← 阻止普通写与 volatile 写重排序                │");
        System.out.println("│          ↓                                                           │");
        System.out.println("│      volatile 写  ← 将值刷新到主内存                                  │");
        System.out.println("│          ↓                                                           │");
        System.out.println("│      StoreLoad 屏障   ← 阻止 volatile 写与后续读/写重排序（开销最大）   │");
        System.out.println("│                                                                      │");
        System.out.println("│  【volatile 读操作】                                                  │");
        System.out.println("│                                                                      │");
        System.out.println("│      LoadLoad 屏障    ← 阻止 volatile 读与普通读重排序                │");
        System.out.println("│          ↓                                                           │");
        System.out.println("│      LoadStore 屏障   ← 阻止 volatile 读与普通写重排序                │");
        System.out.println("│          ↓                                                           │");
        System.out.println("│      volatile 读  ← 从主内存读取最新值                                │");
        System.out.println("│          ↓                                                           │");
        System.out.println("│      [后续操作]                                                       │");
        System.out.println("└──────────────────────────────────────────────────────────────────────┘\n");

        // 3. HotSpot JVM 源码层面的实现
        System.out.println("3. HotSpot JVM 源码层面实现（openjdk/hotspot）：\n");
        System.out.println("【JVM 编译器（C2）处理 volatile 的位置】");
        System.out.println("文件: hotspot/share/opto/parse2.cpp");
        System.out.println("方法: Parse::do_put_xxx() 和 Parse::do_get_xxx()\n");
        System.out.println("```cpp");
        System.out.println("// 当解析 volatile 字段写操作时，C2 编译器会调用:");
        System.out.println("void Parse::do_put_xxx() {");
        System.out.println("    // ... 前置代码 ...");
        System.out.println("    if (is_volatile) {");
        System.out.println("        // 插入内存屏障节点 (MemBarRelease)");
        System.out.println("        // 对应: StoreStore + StoreLoad 屏障");
        System.out.println("        insert_mem_bar(Op_MemBarRelease);");
        System.out.println("    }");
        System.out.println("    // 执行实际的写操作");
        System.out.println("    store_to_memory(...);");
        System.out.println("    if (is_volatile) {");
        System.out.println("        // 插入内存屏障节点 (MemBarVolatile)");
        System.out.println("        insert_mem_bar(Op_MemBarVolatile);");
        System.out.println("    }");
        System.out.println("}");
        System.out.println("```\n");

        System.out.println("【JVM 内存屏障到 CPU 指令的映射】");
        System.out.println("文件: hotspot/os_cpu/xxx/orderAccess.hpp\n");
        System.out.println("```cpp");
        System.out.println("// x86 架构实现（orderAccess_linux_x86.hpp）");
        System.out.println("// x86 本身内存模型较强，volatile 写只需要 lock 前缀指令");
        System.out.println("inline void OrderAccess::storeload()  { fence(); }");
        System.out.println("inline void OrderAccess::fence() {");
        System.out.println("    // lock 前缀指令会触发 CPU 缓存一致性协议");
        System.out.println("    // 将 Store Buffer 中的数据刷新到主内存");
        System.out.println("    __asm__ volatile (\"lock; addl $0,0(%%rsp)\" ::: \"memory\");");
        System.out.println("}");
        System.out.println("```\n");

        // 4. 字节码层面的体现
        System.out.println("4. 字节码层面的体现（可通过 javap -v 查看）：\n");
        System.out.println("```java");
        System.out.println("// Java 源码");
        System.out.println("volatile int x;");
        System.out.println("x = 42;  // volatile 写");
        System.out.println("int y = x;  // volatile 读");
        System.out.println("```\n");
        System.out.println("```");
        System.out.println("// javap -v 输出的字节码");
        System.out.println("// volatile 写 - 使用 putfield 指令，access_flags 带 ACC_VOLATILE");
        System.out.println("putfield      #2    // Field x:I (flags: ACC_VOLATILE)");
        System.out.println("");
        System.out.println("// volatile 读 - 使用 getfield 指令，access_flags 带 ACC_VOLATILE");
        System.out.println("getfield      #2    // Field x:I (flags: ACC_VOLATILE)");
        System.out.println("```\n");
        System.out.println("说明：字节码本身不直接体现内存屏障，ACC_VOLATILE 标志位告诉 JVM");
        System.out.println("      在解释执行或 JIT 编译时需要插入相应的内存屏障指令。\n");

        // 5. CPU 指令层面的实现
        System.out.println("5. CPU 指令层面实现（不同架构差异）：\n");
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  x86/x64 架构（强内存模型）                                           │");
        System.out.println("│  ─────────────────────────                                           │");
        System.out.println("│  volatile 写：使用 lock 前缀指令                                       │");
        System.out.println("│  lock addl $0, 0(%%rsp)  ← lock 前缀触发缓存一致性协议                  │");
        System.out.println("│                          ← 将 Store Buffer 数据刷新到内存              │");
        System.out.println("│                                                                      │");
        System.out.println("│  volatile 读：普通 mov 指令即可（x86 读操作本身有强一致性）             │");
        System.out.println("│  movl (%rsi), %eax                                                  │");
        System.out.println("├──────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  ARM 架构（弱内存模型）                                               │");
        System.out.println("│  ─────────────────────                                               │");
        System.out.println("│  volatile 写：使用 dmb ish 指令（数据内存屏障）                         │");
        System.out.println("│  dmb ish  ← Inner Shareable 域的数据内存屏障                          │");
        System.out.println("│                                                                      │");
        System.out.println("│  volatile 读：使用 dmb ish 指令                                       │");
        System.out.println("│  dmb ish                                                            │");
        System.out.println("├──────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  RISC-V 架构（弱内存模型）                                            │");
        System.out.println("│  ────────────────────────                                            │");
        System.out.println("│  volatile 写：使用 fence rw,w 指令                                    │");
        System.out.println("│  fence rw,w  ← 前面读写操作必须在后面写操作前完成                       │");
        System.out.println("│                                                                      │");
        System.out.println("│  volatile 读：使用 fence r,rw 指令                                    │");
        System.out.println("│  fence r,rw  ← 前面读操作必须在后面读写操作前完成                       │");
        System.out.println("└──────────────────────────────────────────────────────────────────────┘\n");

        // 6. 如何验证内存屏障
        System.out.println("6. 如何验证内存屏障的存在：\n");
        System.out.println("【方法1：使用 hsdis 查看汇编代码】");
        System.out.println("```bash");
        System.out.println("# 1. 下载 hsdis 插件（与 JDK 版本匹配）");
        System.out.println("# 2. 设置 JVM 参数打印汇编");
        System.out.println("java -XX:+UnlockDiagnosticVMOptions \\");
        System.out.println("     -XX:+PrintAssembly \\");
        System.out.println("     -XX:CompileCommand=print,*YourClass.yourVolatileMethod \\");
        System.out.println("     YourClass");
        System.out.println("```\n");
        System.out.println("【方法2：使用 JITWatch 工具】");
        System.out.println("```bash");
        System.out.println("# 1. 添加 JVM 参数生成日志");
        System.out.println("java -XX:+LogCompilation -XX:+PrintInlining YourClass");
        System.out.println("# 2. 使用 JITWatch 分析 hotspot_pid*.log");
        System.out.println("```\n");
        System.out.println("【方法3：观察可见性效果】");
        System.out.println("```java");
        System.out.println("// 去掉 volatile 关键字，程序可能无限循环");
        System.out.println("// 加上 volatile 关键字，程序一定能正常结束");
        System.out.println("volatile boolean flag = false;");
        System.out.println("// 读线程");
        System.out.println("while (!flag) { }  // 没有 volatile 可能永远看不到变化");
        System.out.println("```\n");

        // 7. 内存屏障与 happens-before
        System.out.println("7. 内存屏障与 happens-before 关系：\n");
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  happens-before 规则（JMM 抽象层）    →   内存屏障实现（JVM 底层）      │");
        System.out.println("├──────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  volatile 写 happens-before volatile 读                              │");
        System.out.println("│      ↓                                                               │");
        System.out.println("│  volatile 写 + StoreLoad 屏障 + volatile 读                          │");
        System.out.println("├──────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  程序顺序规则：前面操作 happens-before 后面操作                         │");
        System.out.println("│      ↓                                                               │");
        System.out.println("│  LoadLoad / StoreStore / LoadStore 屏障保证不重排序                    │");
        System.out.println("└──────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("8. 总结：内存屏障的本质\n");
        System.out.println("┌──────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  【本质】内存屏障是 JVM/HotSpot 插入的 CPU 指令，用于：                 │");
        System.out.println("│                                                                      │");
        System.out.println("│  1. 禁止指令重排序：编译器优化和 CPU 乱序执行都受限制                   │");
        System.out.println("│  2. 保证可见性：强制刷新 CPU 缓存到主内存（写屏障）                     │");
        System.out.println("│                强制从主内存读取最新值（读屏障）                         │");
        System.out.println("│                                                                      │");
        System.out.println("│  【关键理解】                                                         │");
        System.out.println("│  - Java 代码中的 volatile 关键字                                       │");
        System.out.println("│      ↓ 编译                                                          │");
        System.out.println("│  - 字节码中的 ACC_VOLATILE 标志位                                      │");
        System.out.println("│      ↓ JVM 解释执行 / C2 JIT 编译                                     │");
        System.out.println("│  - 内存屏障节点 (MemBarNode) 插入到 IR 图                              │");
        System.out.println("│      ↓ 代码生成                                                      │");
        System.out.println("│  - CPU 特定指令（lock/dmb/fence）                                      │");
        System.out.println("│      ↓ 执行                                                          │");
        System.out.println("│  - 缓存一致性协议（MESI）保证多核可见性                                 │");
        System.out.println("└──────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 辅助类定义 ====================

    static class SynchronizedCounter {
        private int count = 0;

        public synchronized void increment() {
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }

    static class LockCounter {
        private int count = 0;
        private final ReentrantLock lock = new ReentrantLock();

        public void increment() {
            lock.lock();
            try {
                count++;
            } finally {
                lock.unlock();
            }
        }

        public int getCount() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }
    }

    static class ReadWriteLockData {
        private int data = 0;
        private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

        public int read() {
            rwLock.readLock().lock();
            try {
                try {
                    Thread.sleep(50); // 模拟读取耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return data;
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void write(int value) {
            rwLock.writeLock().lock();
            try {
                try {
                    Thread.sleep(100); // 模拟写入耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                this.data = value;
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    static class VolatileExample {
        volatile boolean flag = false;
    }
}
