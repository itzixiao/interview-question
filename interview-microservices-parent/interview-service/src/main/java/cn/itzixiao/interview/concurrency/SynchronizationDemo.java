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
     * 5. volatile 关键字
     */
    private static void demonstrateVolatile() throws InterruptedException {
        System.out.println("【5. volatile 关键字】\n");

        System.out.println("volatile 的作用：");
        System.out.println("1. 保证可见性：一个线程修改，其他线程立即可见");
        System.out.println("2. 禁止指令重排序：保证程序执行顺序\n");

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

        System.out.println("\n内存屏障（Memory Barrier）：");
        System.out.println("volatile 写操作：在写之后插入 StoreStore + StoreLoad 屏障");
        System.out.println("volatile 读操作：在读之前插入 LoadLoad + LoadStore 屏障");
        System.out.println("防止编译器和 CPU 对指令进行重排序\n");
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
