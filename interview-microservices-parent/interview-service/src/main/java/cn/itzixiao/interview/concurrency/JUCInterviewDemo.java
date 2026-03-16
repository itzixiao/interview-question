package cn.itzixiao.interview.concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Java 并发编程核心知识点详解
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │                         Java 并发编程知识体系                                 │
 * ├──────────────────────────────────────────────────────────────────────────────┤
 * │                                                                              │
 * │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                       │
 * │  │   线程基础  │    │   锁机制    │    │   JMM       │                       │
 * │  │   Thread    │    │ synchronized│    │ volatile    │                       │
 * │  │ Runnable    │    │ ReentrantLock│   │ 原子性      │                       │
 * │  │ Callable    │    │ ReadWriteLock│   │ 可见性      │                       │
 * │  └──────┬──────┘    │ 乐观/悲观锁  │    │ 有序性      │                       │
 * │         │          └──────┬──────┘    └──────┬──────┘                       │
 * │         │                  │                  │                              │
 * │  ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐                       │
 * │  │  线程池     │    │  并发工具   │    │  ThreadLocal │                      │
 * │  │ Executor    │    │ CountDownLatch│  │ Inheritable  │                      │
 * │  │ ThreadPool  │    │ CyclicBarrier │  │ ThreadLocal  │                      │
 * │  │ Future      │    │ Semaphore    │    │              │                      │
 * │  │ CompletableFuture│            │    │              │                      │
 * │  └──────┬──────┘    └─────────────┘    └─────────────┘                       │
 * │         │                                                                     │
 * │  ┌──────▼──────┐                                                              │
 * │  │  虚拟线程   │                                                              │
 * │  │ (Project Loom)│                                                           │
 * │  └─────────────┘                                                              │
 * └──────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author itzixiao
 */
public class JUCInterviewDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Java 并发编程核心知识点详解                             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");

        // 第一部分：线程基础
        demonstrateThreadBasics();

        // 第二部分：JMM 与 volatile
        demonstrateJMMAndVolatile();

        // 第三部分：死锁
        demonstrateDeadlock();

        // 第四部分：synchronized 关键字
        demonstrateSynchronized();

        // 第五部分：ReentrantLock
        demonstrateReentrantLock();

        // 第六部分：ReentrantReadWriteLock
        demonstrateReadWriteLock();

        // 第七部分：乐观锁与悲观锁
        demonstrateOptimisticAndPessimisticLock();

        // 第八部分：ThreadLocal
        demonstrateThreadLocal();

        // 第九部分：线程池
        demonstrateThreadPool();

        // 第十部分：Future 与 CompletableFuture
        demonstrateFuture();

        // 第十一部分：虚拟线程
        demonstrateVirtualThread();

        // 第十二部分：高频面试题
        printInterviewQuestions();
    }

    // ==================== 第一部分：线程基础 ====================

    private static void demonstrateThreadBasics() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第一部分：线程基础                                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【1.1 创建线程的四种方式】\n");

        System.out.println("方式一：继承 Thread 类");
        Thread thread1 = new Thread() {
            @Override
            public void run() {
                System.out.println("Thread-1: " + Thread.currentThread().getName());
            }
        };
        thread1.start();

        System.out.println("\n方式二：实现 Runnable 接口");
        Thread thread2 = new Thread(() ->
                System.out.println("Thread-2: " + Thread.currentThread().getName())
        );
        thread2.start();

        System.out.println("\n方式三：实现 Callable 接口（有返回值）");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(() -> {
            return "Callable 返回结果";
        });
        try {
            System.out.println("Thread-3: " + future.get());
        } catch (Exception e) {
            e.printStackTrace();
        }
        executor.shutdown();

        System.out.println("\n方式四：使用线程池（推荐）");
        ExecutorService threadPool = Executors.newFixedThreadPool(2);
        threadPool.submit(() ->
                System.out.println("Thread-4: " + Thread.currentThread().getName())
        );
        threadPool.shutdown();

        System.out.println("\n【1.2 线程状态】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  NEW           # 新建状态，未启动                                       │");
        System.out.println("│  RUNNABLE      # 可运行状态（包括就绪和运行中）                         │");
        System.out.println("│  BLOCKED       # 阻塞状态，等待获取锁                                    │");
        System.out.println("│  WAITING       # 无限期等待，等待其他线程唤醒                            │");
        System.out.println("│  TIMED_WAITING # 限期等待，指定等待时间                                 │");
        System.out.println("│  TERMINATED    # 终止状态，线程执行完成                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【1.3 常用方法】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  start()          # 启动线程                                            │");
        System.out.println("│  run()            # 线程体（直接调用只是普通方法）                      │");
        System.out.println("│  sleep(ms)        # 线程休眠，不释放锁                                 │");
        System.out.println("│  wait()           # 对象等待，释放锁                                   │");
        System.out.println("│  notify()         # 唤醒一个等待线程                                   │");
        System.out.println("│  notifyAll()      # 唤醒所有等待线程                                   │");
        System.out.println("│  join()           # 等待线程终止                                        │");
        System.out.println("│  yield()          # 让出 CPU 时间片                                       │");
        System.out.println("│  interrupt()      # 中断线程                                           │");
        System.out.println("│  isAlive()        # 判断线程是否存活                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第二部分：JMM 与 volatile ====================

    private static void demonstrateJMMAndVolatile() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   第二部分：JMM 与 volatile 关键字                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【2.1 JMM 三大特性】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 原子性（Atomicity）                                                   │");
        System.out.println("│     - 操作不可分割，要么全部成功，要么全部失败                           │");
        System.out.println("│     - synchronized、Lock 保证原子性                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│  2. 可见性（Visibility）                                                  │");
        System.out.println("│     - 一个线程修改共享变量，其他线程立即可见                             │");
        System.out.println("│     - volatile、synchronized、Lock 保证可见性                           │");
        System.out.println("│                                                                        │");
        System.out.println("│  3. 有序性（Ordering）                                                    │");
        System.out.println("│     - 程序执行顺序按照代码先后顺序                                     │");
        System.out.println("│     - volatile 禁止指令重排序，happens-before 原则                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【2.2 volatile 关键字】\n");

        VolatileCounter counter = new VolatileCounter();

        // 线程 1 修改值
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            counter.setValue(100);
            System.out.println("Writer 修改值为：" + counter.getValue());
        });

        // 线程 2 读取值
        Thread reader = new Thread(() -> {
            while (counter.getValue() == 0) {
                // 空循环，等待值变化
            }
            System.out.println("Reader 发现值变化：" + counter.getValue());
        });

        writer.start();
        reader.start();

        try {
            writer.join();
            reader.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\nvolatile 应用场景：");
        System.out.println("1. 状态标记量（如：volatile boolean flag）");
        System.out.println("2. 单例模式双重检查锁定（DCL）");
        System.out.println("3. 多任务完成标志");
        System.out.println("注意：volatile 不保证原子性！i++ 操作仍需要同步\n");
    }

    // 辅助类：volatile 计数器
    static class VolatileCounter {
        private volatile int value = 0;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    // ==================== 第三部分：死锁 ====================

    private static void demonstrateDeadlock() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第三部分：死锁详解                                  ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【3.1 死锁产生的四个必要条件】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 互斥条件：资源一次只能被一个线程占用                                │");
        System.out.println("│  2. 请求与保持：已持有资源，又申请新资源                                │");
        System.out.println("│  3. 不剥夺：已获得的资源不能被强制剥夺                                │");
        System.out.println("│  4. 循环等待：多个线程形成头尾相接的循环链                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        Object lock1 = new Object();
        Object lock2 = new Object();

        System.out.println("【3.2 死锁示例】\n");

        // 注意：实际运行时注释掉，避免程序卡死
        /*
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("T1: 持有 lock1");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                System.out.println("T1: 等待 lock2");
                synchronized (lock2) {
                    System.out.println("T1: 获得 lock2");
                }
            }
        });

        Thread t2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("T2: 持有 lock2");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                System.out.println("T2: 等待 lock1");
                synchronized (lock1) {
                    System.out.println("T2: 获得 lock1");
                }
            }
        });

        t1.start();
        t2.start();
        */

        System.out.println("（示例代码已注释，避免程序卡死）\n");

        System.out.println("【3.3 死锁预防策略】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 破坏请求与保持：一次性申请所有资源                                  │");
        System.out.println("│  2. 破坏不剥夺：申请不到时主动释放已持有资源                            │");
        System.out.println("│  3. 破坏循环等待：按顺序申请资源（如：都先申请 lock1）                   │");
        System.out.println("│  4. 使用定时锁：tryLock(timeout）超时放弃                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【3.4 查看死锁方法】\n");
        System.out.println("1. jps -l 查看进程 ID");
        System.out.println("2. jstack <pid> 查看线程堆栈");
        System.out.println("3. 寻找 Found one Java-level deadlock 提示\n");
    }

    // ==================== 第四部分：synchronized 关键字 ====================

    private static void demonstrateSynchronized() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第四部分：synchronized 关键字                             ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【4.1 synchronized 三种用法】\n");

        SynchronizedExample example = new SynchronizedExample();

        System.out.println("用法一：修饰实例方法（锁当前对象实例）");
        Thread t1 = new Thread(() -> {
            example.method1();
        });
        t1.start();

        System.out.println("用法二：修饰静态方法（锁当前类的 Class 对象）");
        Thread t2 = new Thread(() -> {
            SynchronizedExample.method2();
        });
        t2.start();

        System.out.println("用法三：修饰代码块（锁指定对象）");
        Thread t3 = new Thread(() -> {
            example.method3();
        });
        t3.start();

        try {
            t1.join();
            t2.join();
            t3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n【4.2 synchronized 原理】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  JVM 基于进入和退出 Monitor 对象来实现                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  方法同步：                                                            │");
        System.out.println("│  - ACC_SYNCHRONIZED 标志位                                             │");
        System.out.println("│  - 方法调用时检查标志位，自动获取/释放 monitor                          │");
        System.out.println("│                                                                        │");
        System.out.println("│  代码块同步：                                                          │");
        System.out.println("│  - monitorenter 指令（进入同步块）                                     │");
        System.out.println("│  - monitorexit 指令（退出同步块）                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【4.3 synchronized 升级过程（JDK 1.6+）】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  无锁 → 偏向锁 → 轻量级锁 → 重量级锁                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  偏向锁：                                                                │");
        System.out.println("│  - 只有一个线程访问，无竞争                                             │");
        System.out.println("│  - 偏向第一个获取它的线程                                               │");
        System.out.println("│                                                                        │");
        System.out.println("│  轻量级锁（自旋锁）：                                                     │");
        System.out.println("│  - 有竞争但时间短                                                       │");
        System.out.println("│  - CAS 自旋，不立即阻塞                                                 │");
        System.out.println("│                                                                        │");
        System.out.println("│  重量级锁：                                                              │");
        System.out.println("│  - 竞争激烈，自旋失败                                                   │");
        System.out.println("│  - 依赖底层 mutex lock 实现，线程阻塞                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // 辅助类：synchronized 示例
    static class SynchronizedExample {
        public synchronized void method1() {
            System.out.println("method1: " + Thread.currentThread().getName());
        }

        public static synchronized void method2() {
            System.out.println("method2: " + Thread.currentThread().getName());
        }

        public void method3() {
            synchronized (this) {
                System.out.println("method3: " + Thread.currentThread().getName());
            }
        }
    }

    // ==================== 第五部分：ReentrantLock ====================

    private static void demonstrateReentrantLock() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   第五部分：ReentrantLock 详解                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        ReentrantLockExample lockExample = new ReentrantLockExample();

        System.out.println("【5.1 ReentrantLock 基本使用】\n");

        Thread t1 = new Thread(() -> {
            lockExample.increment();
        });

        Thread t2 = new Thread(() -> {
            lockExample.increment();
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("最终 count = " + lockExample.getCount());

        System.out.println("\n【5.2 ReentrantLock vs synchronized】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ReentrantLock 特性：                                                    │");
        System.out.println("│  1. 可中断：lockInterruptibly() 响应中断                                │");
        System.out.println("│  2. 可超时：tryLock(timeout) 超时放弃                                   │");
        System.out.println("│  3. 公平锁：可设置公平策略（默认非公平）                                │");
        System.out.println("│  4. 多条件变量：newCondition() 支持多个 Condition                       │");
        System.out.println("│  5. 手动加锁释放：lock()/unlock() 必须在 finally 中释放                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【5.3 AQS 原理】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  AbstractQueuedSynchronizer（AQS）是 Lock 的核心实现框架               │");
        System.out.println("│                                                                        │");
        System.out.println("│  核心组件：                                                              │");
        System.out.println("│  - state：同步状态（volatile int）                                     │");
        System.out.println("│  - CLH 队列：双向链表，存储等待线程                                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  工作流程：                                                              │");
        System.out.println("│  1. 尝试获取锁（CAS 修改 state）                                         │");
        System.out.println("│  2. 失败则加入队列尾部                                                  │");
        System.out.println("│  3. 被唤醒后再次尝试获取                                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // 辅助类：ReentrantLock 示例
    static class ReentrantLockExample {
        private final ReentrantLock lock = new ReentrantLock();
        private int count = 0;

        public void increment() {
            lock.lock();
            try {
                count++;
                System.out.println(Thread.currentThread().getName() + ": count = " + count);
            } finally {
                lock.unlock();
            }
        }

        public int getCount() {
            return count;
        }
    }

    // ==================== 第六部分：ReentrantReadWriteLock ====================

    private static void demonstrateReadWriteLock() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              第六部分：ReentrantReadWriteLock 详解                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        ReadWriteLockExample rwLockExample = new ReadWriteLockExample();

        System.out.println("【6.1 读写锁特性】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  - 读锁（共享锁）：多个线程可同时读                                     │");
        System.out.println("│  - 写锁（排他锁）：写时独占，其他读写都阻塞                             │");
        System.out.println("│  - 适用场景：读多写少                                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        // 多个读线程
        for (int i = 1; i <= 3; i++) {
            final int id = i;
            new Thread(() -> {
                String value = rwLockExample.read("Thread-" + id);
                System.out.println(value);
            }, "Read-" + i).start();
        }

        // 写线程
        new Thread(() -> {
            rwLockExample.write("新数据", "Write-1");
        }).start();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n【6.2 锁降级】\n");
        System.out.println("ReentrantReadWriteLock 支持锁降级（写锁→读锁），不支持锁升级\n");
    }

    // 辅助类：读写锁示例
    static class ReadWriteLockExample {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private String data = "初始数据";

        public String read(String threadName) {
            lock.readLock().lock();
            try {
                System.out.println(threadName + " 读取：" + data);
                return data;
            } finally {
                lock.readLock().unlock();
            }
        }

        public void write(String newData, String threadName) {
            lock.writeLock().lock();
            try {
                System.out.println(threadName + " 写入：" + newData);
                this.data = newData;
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    // ==================== 第七部分：乐观锁与悲观锁 ====================

    private static void demonstrateOptimisticAndPessimisticLock() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第七部分：乐观锁与悲观锁                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【7.1 概念对比】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  悲观锁（Pessimistic Lock）：                                           │");
        System.out.println("│  - 假设最坏情况，每次操作都认为会被修改                                 │");
        System.out.println("│  - 实现：synchronized、ReentrantLock                                    │");
        System.out.println("│  - 适用：写多读少，竞争激烈                                             │");
        System.out.println("│                                                                        │");
        System.out.println("│  乐观锁（Optimistic Lock）：                                            │");
        System.out.println("│  - 假设最好情况，每次操作都认为不会被修改                               │");
        System.out.println("│  - 实现：CAS（Compare And Swap）                                       │");
        System.out.println("│  - 适用：读多写少，竞争不激烈                                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【7.2 CAS 原理】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  CAS(V, A, B)：                                                         │");
        System.out.println("│  - V: 内存中的值                                                        │");
        System.out.println("│  - A: 预期原值                                                          │");
        System.out.println("│  - B: 新值                                                              │");
        System.out.println("│                                                                        │");
        System.out.println("│  流程：                                                                 │");
        System.out.println("│  1. 比较 V 和 A 是否相等                                                 │");
        System.out.println("│  2. 相等则将 V 更新为 B                                                  │");
        System.out.println("│  3. 不相等则重试（自旋）                                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【7.3 AtomicInteger 示例】\n");
        AtomicInteger atomicInt = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                atomicInt.incrementAndGet();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                atomicInt.incrementAndGet();
            }
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("atomicInt = " + atomicInt.get() + "（预期：2000）\n");

        System.out.println("【7.4 CAS 问题】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. ABA 问题：值从 A→B→A，CAS 认为没变                                   │");
        System.out.println("│     解决：AtomicStampedReference（添加版本号）                          │");
        System.out.println("│                                                                        │");
        System.out.println("│  2. 自旋时间长：竞争激烈时长时间自旋浪费 CPU                            │");
        System.out.println("│     解决：自旋次数限制                                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  3. 只能保证单个变量原子性                                              │");
        System.out.println("│     解决：锁                                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第八部分：ThreadLocal ====================

    private static void demonstrateThreadLocal() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    第八部分：ThreadLocal 详解                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        ThreadLocal<String> threadLocal = ThreadLocal.withInitial(() -> "default");

        System.out.println("【8.1 ThreadLocal 原理】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  每个 Thread 内部维护 ThreadLocalMap                                   │");
        System.out.println("│  Map<ThreadLocal, Object> 存储线程私有数据                              │");
        System.out.println("│                                                                        │");
        System.out.println("│  get() 流程：                                                           │");
        System.out.println("│  1. 获取当前线程                                                       │");
        System.out.println("│  2. 获取线程内部的 ThreadLocalMap                                      │");
        System.out.println("│  3. 以 ThreadLocal 为 key 获取 value                                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        Thread t1 = new Thread(() -> {
            threadLocal.set("Thread-1 的值");
            System.out.println("Thread-1: " + threadLocal.get());
            threadLocal.remove(); // 防止内存泄漏
        });

        Thread t2 = new Thread(() -> {
            threadLocal.set("Thread-2 的值");
            System.out.println("Thread-2: " + threadLocal.get());
            threadLocal.remove(); // 防止内存泄漏
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\n【8.2 应用场景】\n");
        System.out.println("1. 数据库连接管理（每个线程独立 Connection）");
        System.out.println("2. Session 管理（Web 请求链路传递用户信息）");
        System.out.println("3. SimpleDateFormat 线程安全封装");
        System.out.println("4. MDC 日志追踪（TraceID 传递）\n");

        System.out.println("【8.3 内存泄漏问题】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ThreadLocalMap 的 key 是弱引用，value 是强引用                          │");
        System.out.println("│  如果 ThreadLocal 被回收，key 变为 null，但 value 仍存在                 │");
        System.out.println("│                                                                        │");
        System.out.println("│  解决方案：                                                             │");
        System.out.println("│  使用完必须调用 remove() 清理！                                         │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第九部分：线程池 ====================

    private static void demonstrateThreadPool() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第九部分：线程池详解                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【9.1 ThreadPoolExecutor 七大参数】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. corePoolSize       核心线程数（最小线程数）                         │");
        System.out.println("│  2. maximumPoolSize    最大线程数                                      │");
        System.out.println("│  3. keepAliveTime      空闲线程存活时间                                │");
        System.out.println("│  4. unit               时间单位                                        │");
        System.out.println("│  5. workQueue          任务队列（BlockingQueue）                       │");
        System.out.println("│  6. threadFactory      线程工厂（自定义线程名）                         │");
        System.out.println("│  7. handler            拒绝策略                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【9.2 工作流程】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 提交任务 → 核心线程未满 → 创建核心线程执行                          │");
        System.out.println("│  2. 核心线程已满 → 加入任务队列等待                                     │");
        System.out.println("│  3. 队列已满 → 创建非核心线程（不超过 max）                            │");
        System.out.println("│  4. 线程数达上限 → 触发拒绝策略                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【9.3 四种拒绝策略】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  AbortPolicy（默认）    抛出异常                                        │");
        System.out.println("│  CallerRunsPolicy       调用者线程执行                                  │");
        System.out.println("│  DiscardPolicy          直接丢弃                                       │");
        System.out.println("│  DiscardOldestPolicy    丢弃最老任务                                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【9.4 正确使用线程池】\n");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                              // 核心线程数
                5,                              // 最大线程数
                60L,                            // 空闲存活时间
                TimeUnit.SECONDS,               // 时间单位
                new ArrayBlockingQueue<>(10),   // 有界队列
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );

        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task-" + taskId + " by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();

        System.out.println("\n【9.5 不推荐使用 Executors 创建】\n");
        System.out.println("原因：");
        System.out.println("1. FixedThreadPool/SingleThreadPool: 队列长度 Integer.MAX_VALUE，可能 OOM");
        System.out.println("2. CachedThreadPool: 允许线程数 Integer.MAX_VALUE，可能 OOM");
        System.out.println("建议：使用 ThreadPoolExecutor 构造函数显式指定参数\n");
    }

    // ==================== 第十部分：Future 与 CompletableFuture ====================

    private static void demonstrateFuture() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║              第十部分：Future 与 CompletableFuture 详解                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【10.1 Future 局限性】\n");
        System.out.println("- get() 阻塞，无法异步回调");
        System.out.println("- 无法组合多个任务\n");

        System.out.println("【10.2 CompletableFuture 强大功能】\n");

        // 异步执行
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            return "异步结果";
        });

        // 链式调用
        CompletableFuture<Void> future2 = CompletableFuture.supplyAsync(() -> "Hello")
                .thenApply(s -> s + " World")
                .thenAccept(System.out::println)
                .thenRun(() -> System.out.println("执行完成"));

        // 组合多个任务
        CompletableFuture<Integer> future3 = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> future4 = CompletableFuture.supplyAsync(() -> 20);

        CompletableFuture<Integer> combined = future3.thenCombine(future4, (a, b) -> a + b);
        System.out.println("组合结果：" + combined.join());

        // 任意一个完成
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future3, future4);
        System.out.println("任意一个完成：" + anyOf.join());

        // 所有都完成
        CompletableFuture<Void> allOf = CompletableFuture.allOf(future3, future4);
        allOf.join();
        System.out.println("所有都完成\n");

        System.out.println("【10.3 常用方法】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  supplyAsync/supplyAsync         异步执行有返回值                      │");
        System.out.println("│  runAsync/runAsync               异步执行无返回值                      │");
        System.out.println("│  thenApply                     转换结果                                │");
        System.out.println("│  thenAccept                    消费结果                                │");
        System.out.println("│  thenRun                       执行动作                                │");
        System.out.println("│  thenCombine                   组合两个 Future                         │");
        System.out.println("│  thenCompose                   扁平化组合                              │");
        System.out.println("│  allOf/anyOf                   所有/任意一个完成                       │");
        System.out.println("│  exceptionally                 异常处理                                │");
        System.out.println("│  whenComplete                  完成回调（无论成功失败）                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第十一部分：虚拟线程 ====================

    private static void demonstrateVirtualThread() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     第十一部分：虚拟线程（Project Loom）                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【11.1 虚拟线程 vs 平台线程】\n");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  平台线程（Platform Thread）：                                          │");
        System.out.println("│  - 一对一映射到操作系统内核线程                                         │");
        System.out.println("│  - 重量级，栈内存 1MB，创建成本高                                        │");
        System.out.println("│  - 数量有限（几千个）                                                   │");
        System.out.println("│                                                                        │");
        System.out.println("│  虚拟线程（Virtual Thread）：                                           │");
        System.out.println("│  - JVM 调度的轻量级线程，多对一映射到平台线程                           │");
        System.out.println("│  - 轻量级，栈内存动态扩展，创建成本极低                                 │");
        System.out.println("│  - 数量巨大（百万级）                                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        // JDK 21+ 特性（如果运行在 JDK 21 以下，请注释掉虚拟线程部分）
        System.out.println("【11.2 创建虚拟线程（JDK 21+ 特性）】\n");

        // 注意：虚拟线程是 JDK 21 特性，以下代码需要在 JDK 21+ 环境运行
        // 方式一：直接启动
        /*
        try {
            Thread virtualThread1 = Thread.ofVirtual().start(() -> {
                System.out.println("虚拟线程：" + Thread.currentThread().getName());
            });
            
            // 方式二：工厂创建
            ThreadFactory factory = Thread.ofVirtual().factory();
            Thread virtualThread2 = factory.newThread(() -> {
                System.out.println("工厂创建的虚拟线程");
            });
            virtualThread2.start();
            
            // 方式三：ExecutorService
            try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                executor.submit(() -> {
                    System.out.println("线程池中的虚拟线程");
                });
            }
            
            System.out.println("虚拟线程是虚拟的：" + virtualThread1.isVirtual());
        } catch (Exception e) {
            System.out.println("当前 JDK 版本不支持虚拟线程（需要 JDK 21+）");
        }
        */
        System.out.println("注：虚拟线程代码已注释，需在 JDK 21+ 环境运行");
        System.out.println("示例代码请参考源码注释部分");

        System.out.println("\n【11.3 适用场景】\n");
        System.out.println("✅ 适合：IO 密集型任务（高并发网络服务）");
        System.out.println("❌ 不适合：CPU 密集型任务（计算密集）\n");

        System.out.println("【11.4 优势】\n");
        System.out.println("1. 简化并发编程模型（一个请求一个线程）");
        System.out.println("2. 提高吞吐量（减少上下文切换）");
        System.out.println("3. 降低资源消耗（栈内存小）");
        System.out.println("4. 便于调试（保留堆栈信息）\n");
    }

    // ==================== 第十二部分：高频面试题 ====================

    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第十二部分：高频面试题                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        // 线程基础
        System.out.println("==================== 线程基础面试题 ====================\n");

        System.out.println("【问题 1】创建线程的方式有哪些？");
        System.out.println("答：");
        System.out.println("1. 继承 Thread 类");
        System.out.println("2. 实现 Runnable 接口");
        System.out.println("3. 实现 Callable 接口（有返回值）");
        System.out.println("4. 使用线程池（推荐）\n");

        System.out.println("【问题 2】sleep() 和 wait() 的区别？");
        System.out.println("答：");
        System.out.println("- 归属不同：sleep() 属于 Thread，wait() 属于 Object");
        System.out.println("- 锁释放：sleep() 不释放锁，wait() 释放锁");
        System.out.println("- 使用范围：sleep() 任何地方，wait() 必须在同步块中");
        System.out.println("- 唤醒方式：sleep() 时间到自动醒，wait() 需要 notify()\n");

        System.out.println("【问题 3】run() 和 start() 的区别？");
        System.out.println("答：");
        System.out.println("- start(): 启动新线程，JVM 调用 run() 方法");
        System.out.println("- run(): 普通方法调用，在当前线程执行\n");

        System.out.println("【问题 4】线程的生命周期（状态）？");
        System.out.println("答：NEW → RUNNABLE → BLOCKED → WAITING → TIMED_WAITING → TERMINATED\n");

        // JMM 与 volatile
        System.out.println("==================== JMM 与 volatile 面试题 ====================\n");

        System.out.println("【问题 5】Java 内存模型（JMM）是什么？");
        System.out.println("答：");
        System.out.println("JMM 定义了线程和主内存之间的抽象关系：");
        System.out.println("- 主内存：所有变量存储在主内存");
        System.out.println("- 工作内存：每个线程有自己的工作内存");
        System.out.println("- 线程不能直接访问主内存，需通过工作内存\n");

        System.out.println("【问题 6】volatile 关键字的作用？");
        System.out.println("答：");
        System.out.println("1. 保证可见性：一个线程修改，其他线程立即可见");
        System.out.println("2. 禁止指令重排序：happens-before 原则");
        System.out.println("3. 不保证原子性：i++ 操作仍需要同步");
        System.out.println("应用场景：状态标记、单例 DCL、多任务完成标志\n");

        System.out.println("【问题 7】什么是原子性、可见性、有序性？");
        System.out.println("答：");
        System.out.println("- 原子性：操作不可分割，要么全成功要么全失败");
        System.out.println("- 可见性：一个线程修改，其他线程立即可见");
        System.out.println("- 有序性：程序按代码顺序执行，禁止重排序\n");

        // synchronized
        System.out.println("==================== synchronized 面试题 ====================\n");

        System.out.println("【问题 8】synchronized 的原理？");
        System.out.println("答：");
        System.out.println("- 基于 Monitor 对象（对象头中的锁记录）");
        System.out.println("- 方法同步：ACC_SYNCHRONIZED 标志位");
        System.out.println("- 代码块同步：monitorenter/monitorexit 指令\n");

        System.out.println("【问题 9】synchronized 锁升级过程？");
        System.out.println("答：无锁 → 偏向锁 → 轻量级锁（自旋） → 重量级锁（阻塞）\n");

        System.out.println("【问题 10】synchronized 和 ReentrantLock 的区别？");
        System.out.println("答：");
        System.out.println("相同点：都可重入、互斥、保证可见性");
        System.out.println("不同点：");
        System.out.println("- synchronized：JVM 层面，自动加锁释放，不支持公平锁");
        System.out.println("- ReentrantLock：API 层面，手动加锁释放，支持公平锁、可中断、多条件变量\n");

        // 锁机制
        System.out.println("==================== 锁机制面试题 ====================\n");

        System.out.println("【问题 11】乐观锁和悲观锁的区别？");
        System.out.println("答：");
        System.out.println("- 悲观锁：假设最坏情况，每次操作都认为会被修改（synchronized、ReentrantLock）");
        System.out.println("- 乐观锁：假设最好情况，每次操作都认为不会被修改（CAS）");
        System.out.println("- 适用场景：悲观锁写多读少，乐观锁读多写少\n");

        System.out.println("【问题 12】CAS 是什么？有什么问题？");
        System.out.println("答：");
        System.out.println("CAS(Compare And Swap)：比较并交换，包含三个操作数 V(内存值)、A(预期值)、B(新值)");
        System.out.println("问题：");
        System.out.println("1. ABA 问题：添加版本号解决（AtomicStampedReference）");
        System.out.println("2. 自旋时间长：竞争激烈时浪费 CPU");
        System.out.println("3. 只能保证单个变量原子性\n");

        System.out.println("【问题 13】ReentrantReadWriteLock 的应用场景？");
        System.out.println("答：读多写少的场景，如缓存系统\n");

        // ThreadLocal
        System.out.println("==================== ThreadLocal 面试题 ====================\n");

        System.out.println("【问题 14】ThreadLocal 的原理？");
        System.out.println("答：");
        System.out.println("- 每个 Thread 维护 ThreadLocalMap");
        System.out.println("- 以 ThreadLocal 为 key，存储线程私有数据");
        System.out.println("- get() 时从当前线程的 Map 中获取\n");

        System.out.println("【问题 15】ThreadLocal 内存泄漏问题？");
        System.out.println("答：");
        System.out.println("- ThreadLocalMap 的 key 是弱引用，value 是强引用");
        System.out.println("- ThreadLocal 被回收后，key 为 null，value 仍存在");
        System.out.println("- 解决：使用完必须调用 remove()\n");

        // 线程池
        System.out.println("==================== 线程池面试题 ====================\n");

        System.out.println("【问题 16】线程池的七个参数？");
        System.out.println("答：");
        System.out.println("1. corePoolSize：核心线程数");
        System.out.println("2. maximumPoolSize：最大线程数");
        System.out.println("3. keepAliveTime：空闲存活时间");
        System.out.println("4. unit：时间单位");
        System.out.println("5. workQueue：任务队列");
        System.out.println("6. threadFactory：线程工厂");
        System.out.println("7. handler：拒绝策略\n");

        System.out.println("【问题 17】线程池的工作流程？");
        System.out.println("答：");
        System.out.println("1. 核心线程未满 → 创建核心线程");
        System.out.println("2. 核心已满 → 加入队列");
        System.out.println("3. 队列已满 → 创建非核心线程");
        System.out.println("4. 线程达上限 → 触发拒绝策略\n");

        System.out.println("【问题 18】有哪些拒绝策略？");
        System.out.println("答：");
        System.out.println("1. AbortPolicy：抛异常（默认）");
        System.out.println("2. CallerRunsPolicy：调用者执行");
        System.out.println("3. DiscardPolicy：直接丢弃");
        System.out.println("4. DiscardOldestPolicy：丢弃最老任务\n");

        System.out.println("【问题 19】为什么不推荐使用 Executors 创建线程池？");
        System.out.println("答：");
        System.out.println("1. FixedThreadPool：队列长度 Integer.MAX_VALUE，可能 OOM");
        System.out.println("2. CachedThreadPool：允许线程数 Integer.MAX_VALUE，可能 OOM");
        System.out.println("建议：使用 ThreadPoolExecutor 显式指定参数\n");

        // Future 与虚拟线程
        System.out.println("==================== Future 与虚拟线程面试题 ====================\n");

        System.out.println("【问题 20】CompletableFuture 的优势？");
        System.out.println("答：");
        System.out.println("1. 异步非阻塞");
        System.out.println("2. 链式调用");
        System.out.println("3. 组合多个任务");
        System.out.println("4. 异常处理\n");

        System.out.println("【问题 21】虚拟线程和平台线程的区别？");
        System.out.println("答：");
        System.out.println("- 平台线程：一对一映射内核线程，重量级，栈内存 1MB");
        System.out.println("- 虚拟线程：JVM 调度轻量级线程，多对一映射，栈内存动态，百万级并发\n");

        System.out.println("==========================================================================\n");
    }
}
