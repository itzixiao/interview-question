package cn.itzixiao.interview.concurrency;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * 高并发线程安全问题解决方案详解 - 教学型示例
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        线程安全问题产生的根本原因                            │
 * │                                                                             │
 * │  1. 原子性问题：多步操作被打断，如 count++（读-改-写）                       │
 * │  2. 可见性问题：线程修改后，其他线程看不到最新值（CPU缓存）                  │
 * │  3. 有序性问题：指令重排序导致执行顺序与代码顺序不一致                       │
 * │                                                                             │
 * │  ┌──────────────────────────────────────────────────────────────────┐       │
 * │  │                   Java 内存模型 (JMM)                            │       │
 * │  │                                                                  │       │
 * │  │   线程1         线程2         线程3                              │       │
 * │  │   ┌────┐       ┌────┐       ┌────┐                              │       │
 * │  │   │工作内存│   │工作内存│   │工作内存│ ← 每个线程有自己的工作内存    │       │
 * │  │   └──┬─┘       └──┬─┘       └──┬─┘                              │       │
 * │  │      ↓            ↓            ↓                                │       │
 * │  │   ┌────────────────────────────────────┐                        │       │
 * │  │   │            主内存                   │ ← 所有共享变量存在主内存  │       │
 * │  │   └────────────────────────────────────┘                        │       │
 * │  └──────────────────────────────────────────────────────────────────┘       │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                          解决方案总览                                        │
 * │                                                                             │
 * │  ┌───────────────┬──────────────────────────────────────────────────┐       │
 * │  │  方案         │  特点                                            │       │
 * │  ├───────────────┼──────────────────────────────────────────────────┤       │
 * │  │  synchronized │  悲观锁，自动获取释放，可重入，JDK6后性能优化     │       │
 * │  │  Lock         │  悲观锁，手动控制，可中断，可超时，可公平         │       │
 * │  │  volatile     │  保证可见性和有序性，不保证原子性                 │       │
 * │  │  原子类       │  CAS无锁算法，适合简单操作                        │       │
 * │  │  并发容器     │  线程安全的集合类，如ConcurrentHashMap            │       │
 * │  │  ThreadLocal  │  线程隔离，每个线程独立副本                       │       │
 * │  │  不可变对象   │  天然线程安全，如String、final字段                │       │
 * │  └───────────────┴──────────────────────────────────────────────────┘       │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
public class ThreadSafeSolutionDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                                                                   █");
        System.out.println("█             高并发线程安全问题解决方案详解                         █");
        System.out.println("█                                                                   █");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println();

        // 第一部分：问题演示
        demonstrateThreadSafetyProblem();

        // 第二部分：解决方案
        solution1_Synchronized();
        solution2_Lock();
        solution3_Volatile();
        solution4_AtomicClass();
        solution5_ConcurrentCollections();
        solution6_ThreadLocal();
        solution7_ImmutableObject();

        // 第三部分：综合对比
        compareAllSolutions();

        // 第四部分：高频面试题
        interviewQuestions();

        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                        全部演示完成                               █");
        System.out.println("███████████████████████████████████████████████████████████████████");
    }

    // ==================== 第一部分：问题演示 ====================

    /**
     * 演示线程安全问题
     */
    private static void demonstrateThreadSafetyProblem() throws InterruptedException {
        System.out.println("========== 线程安全问题演示 ==========\n");

        System.out.println("【问题1：原子性问题】");
        System.out.println("count++ 不是原子操作，包含三步：读取 → 修改 → 写入\n");

        UnsafeCounter unsafeCounter = new UnsafeCounter();
        CountDownLatch latch1 = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    unsafeCounter.increment();
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();

        System.out.println("  预期结果：10000");
        System.out.println("  实际结果：" + unsafeCounter.getCount());
        System.out.println("  结论：多线程下 count++ 会丢失更新\n");

        // 可见性问题
        System.out.println("【问题2：可见性问题】");
        System.out.println("一个线程修改变量，其他线程可能看不到最新值\n");

        VisibilityProblem visibility = new VisibilityProblem();

        Thread reader = new Thread(() -> {
            int localValue = visibility.value;
            while (localValue == 0) {
                // 死循环：看不到其他线程的修改
                localValue = visibility.value;
            }
            System.out.println("  读线程：看到 value = " + localValue);
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            visibility.value = 1;
            System.out.println("  写线程：设置 value = 1");
        });

        reader.start();
        writer.start();

        // 等待最多2秒
        reader.join(2000);
        if (reader.isAlive()) {
            System.out.println("  结论：读线程无法看到修改（可见性问题）");
            reader.interrupt();
        }
        writer.join();
        System.out.println();

        // 有序性问题
        System.out.println("【问题3：有序性问题】");
        System.out.println("指令重排序可能导致意外结果\n");

        System.out.println("  示例代码：");
        System.out.println("  int a = 0, b = 0;");
        System.out.println("  // 线程1           // 线程2");
        System.out.println("  a = 1;             if (b == 1)");
        System.out.println("  b = 1;                print(a); // 可能输出0！\n");

        System.out.println("  原因：JIT 或 CPU 可能重排序为 b=1 先执行\n");
    }

    // ==================== 第二部分：解决方案 ====================

    /**
     * 解决方案1：synchronized
     */
    private static void solution1_Synchronized() throws InterruptedException {
        System.out.println("========== 解决方案1：synchronized ==========\n");

        System.out.println("【synchronized 特点】");
        System.out.println("  - 悲观锁，互斥访问");
        System.out.println("  - 自动获取和释放锁");
        System.out.println("  - 可重入（同一线程可多次获取）");
        System.out.println("  - JDK6+ 优化：偏向锁 → 轻量级锁 → 重量级锁\n");

        System.out.println("【三种使用方式】");

        // 方式1：同步实例方法
        System.out.println("  1. 同步实例方法（锁 this）");
        SyncMethodCounter counter1 = new SyncMethodCounter();
        runCounterTest(counter1::increment, counter1::getCount);

        // 方式2：同步静态方法
        System.out.println("  2. 同步静态方法（锁 Class）");
        SyncStaticCounter.reset();
        runStaticCounterTest();

        // 方式3：同步代码块
        System.out.println("  3. 同步代码块（锁指定对象）");
        SyncBlockCounter counter3 = new SyncBlockCounter();
        runCounterTest(counter3::increment, counter3::getCount);

        // 锁升级过程
        System.out.println("【synchronized 锁升级过程】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  无锁 → 偏向锁 → 轻量级锁 → 重量级锁                             │");
        System.out.println("├─────────────────────────────────────────────────────────────────┤");
        System.out.println("│  偏向锁：只有一个线程访问，Mark Word 记录线程ID，无需CAS        │");
        System.out.println("│  轻量级锁：多个线程竞争，CAS 自旋获取锁（消耗CPU）              │");
        System.out.println("│  重量级锁：自旋失败，线程阻塞（需要操作系统介入）              │");
        System.out.println("│  注意：锁只能升级，不能降级                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 解决方案2：Lock
     */
    private static void solution2_Lock() throws InterruptedException {
        System.out.println("========== 解决方案2：Lock ==========\n");

        System.out.println("【Lock 相比 synchronized 的优势】");
        System.out.println("  - 可中断：lockInterruptibly()");
        System.out.println("  - 可超时：tryLock(timeout)");
        System.out.println("  - 可公平：new ReentrantLock(true)");
        System.out.println("  - 多条件：多个 Condition\n");

        // ReentrantLock 演示
        System.out.println("【ReentrantLock 演示】");
        ReentrantLockCounter lockCounter = new ReentrantLockCounter();
        runCounterTest(lockCounter::increment, lockCounter::getCount);

        // tryLock 超时演示
        System.out.println("【tryLock 超时获取锁】");
        ReentrantLock lock = new ReentrantLock();

        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                System.out.println("  线程1：获取锁，持有2秒...");
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
                System.out.println("  线程1：释放锁");
            }
        });

        Thread waiter = new Thread(() -> {
            try {
                System.out.println("  线程2：尝试获取锁（最多等1秒）");
                if (lock.tryLock(1, TimeUnit.SECONDS)) {
                    try {
                        System.out.println("  线程2：获取到锁");
                    } finally {
                        lock.unlock();
                    }
                } else {
                    System.out.println("  线程2：获取锁超时，执行其他逻辑");
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

        // 读写锁
        System.out.println("【ReadWriteLock 读写锁】");
        System.out.println("  读读不互斥，读写互斥，写写互斥");
        System.out.println("  适用场景：读多写少\n");

        ReadWriteLockCache cache = new ReadWriteLockCache();
        cache.put("key", "value");
        System.out.println("  写入: key=value");
        System.out.println("  读取: " + cache.get("key") + "\n");
    }

    /**
     * 解决方案3：volatile
     */
    private static void solution3_Volatile() throws InterruptedException {
        System.out.println("========== 解决方案3：volatile ==========\n");

        System.out.println("【volatile 的作用】");
        System.out.println("  1. 保证可见性：修改立即刷新到主内存");
        System.out.println("  2. 禁止重排序：通过内存屏障实现\n");

        System.out.println("【注意】volatile 不保证原子性！\n");

        // 可见性演示
        System.out.println("【可见性演示】");
        VolatileVisibility visibility = new VolatileVisibility();

        Thread reader = new Thread(() -> {
            while (!visibility.flag) {
                // 等待 flag 变为 true
            }
            System.out.println("  读线程：看到 flag = " + visibility.flag);
        });

        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("  写线程：设置 flag = true");
            visibility.flag = true;
        });

        reader.start();
        writer.start();
        reader.join(2000);
        writer.join();
        System.out.println("  结论：volatile 保证了可见性\n");

        // 非原子性演示
        System.out.println("【volatile 不保证原子性】");
        VolatileCounter volatileCounter = new VolatileCounter();
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    volatileCounter.increment();
                }
                latch.countDown();
            }).start();
        }
        latch.await();

        System.out.println("  预期结果：10000");
        System.out.println("  实际结果：" + volatileCounter.getCount());
        System.out.println("  结论：volatile 无法保证 count++ 的原子性\n");

        // 适用场景
        System.out.println("【volatile 适用场景】");
        System.out.println("  1. 状态标志：停止标志、初始化完成标志");
        System.out.println("  2. 单例模式的双重检查锁定（DCL）");
        System.out.println("  3. 配合 CAS 使用\n");
    }

    /**
     * 解决方案4：原子类
     */
    private static void solution4_AtomicClass() throws InterruptedException {
        System.out.println("========== 解决方案4：原子类 ==========\n");

        System.out.println("【原子类分类】");
        System.out.println("  基本类型：AtomicInteger, AtomicLong, AtomicBoolean");
        System.out.println("  引用类型：AtomicReference, AtomicStampedReference");
        System.out.println("  数组类型：AtomicIntegerArray, AtomicLongArray");
        System.out.println("  累加器：LongAdder, DoubleAdder（高并发优化）\n");

        // AtomicInteger 演示
        System.out.println("【AtomicInteger 演示】");
        AtomicInteger atomicInt = new AtomicInteger(0);
        CountDownLatch latch1 = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    atomicInt.incrementAndGet();
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();

        System.out.println("  预期结果：10000");
        System.out.println("  实际结果：" + atomicInt.get());
        System.out.println("  结论：原子类保证了线程安全\n");

        // CAS 原理
        System.out.println("【CAS 原理】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  CAS (Compare And Swap) 比较并交换                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  三个参数：内存值 V，预期值 A，新值 B                            │");
        System.out.println("│  操作：if (V == A) then V = B else 重试                         │");
        System.out.println("│                                                                 │");
        System.out.println("│  特点：                                                         │");
        System.out.println("│  - 无锁算法，非阻塞                                             │");
        System.out.println("│  - CPU 指令级别保证原子性                                       │");
        System.out.println("│  - 可能出现 ABA 问题（用版本号解决）                            │");
        System.out.println("│  - 自旋消耗 CPU（高竞争时用 LongAdder）                         │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // LongAdder vs AtomicLong
        System.out.println("【LongAdder vs AtomicLong 性能对比】");
        int threadCount = 10;
        int loopCount = 1000000;

        // AtomicLong 测试
        AtomicLong atomicLong = new AtomicLong(0);
        CountDownLatch latch2 = new CountDownLatch(threadCount);
        long start1 = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    atomicLong.incrementAndGet();
                }
                latch2.countDown();
            }).start();
        }
        latch2.await();
        long time1 = System.currentTimeMillis() - start1;

        // LongAdder 测试
        LongAdder longAdder = new LongAdder();
        CountDownLatch latch3 = new CountDownLatch(threadCount);
        long start2 = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < loopCount; j++) {
                    longAdder.increment();
                }
                latch3.countDown();
            }).start();
        }
        latch3.await();
        long time2 = System.currentTimeMillis() - start2;

        System.out.println("  AtomicLong: " + time1 + "ms, 结果: " + atomicLong.get());
        System.out.println("  LongAdder:  " + time2 + "ms, 结果: " + longAdder.sum());
        System.out.println("  结论：高并发下 LongAdder 性能更好\n");
    }

    /**
     * 解决方案5：并发容器
     */
    private static void solution5_ConcurrentCollections() throws InterruptedException {
        System.out.println("========== 解决方案5：并发容器 ==========\n");

        System.out.println("【并发容器分类】");
        System.out.println("┌───────────────────┬──────────────────────────────────────────┐");
        System.out.println("│  容器              │  特点                                    │");
        System.out.println("├───────────────────┼──────────────────────────────────────────┤");
        System.out.println("│  ConcurrentHashMap │  分段锁（JDK7）/ CAS+synchronized（JDK8）│");
        System.out.println("│  CopyOnWriteList   │  写时复制，读无锁，适合读多写少          │");
        System.out.println("│  ConcurrentLinkedQueue │  无锁队列，CAS实现                   │");
        System.out.println("│  BlockingQueue     │  阻塞队列，生产者消费者模式              │");
        System.out.println("│  ConcurrentSkipListMap │  跳表实现，有序，无锁                │");
        System.out.println("└───────────────────┴──────────────────────────────────────────┘\n");

        // ConcurrentHashMap 演示
        System.out.println("【ConcurrentHashMap 演示】");
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        CountDownLatch latch1 = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            final int index = i;
            new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    concurrentMap.put("key-" + index + "-" + j, j);
                }
                latch1.countDown();
            }).start();
        }
        latch1.await();

        System.out.println("  写入完成，map 大小: " + concurrentMap.size());
        System.out.println("  预期: 1000, 实际: " + concurrentMap.size() + "\n");

        // CopyOnWriteArrayList 演示
        System.out.println("【CopyOnWriteArrayList 演示】");
        System.out.println("  写时复制：每次修改都复制整个数组");
        System.out.println("  适用场景：读多写少，如监听器列表\n");

        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        cowList.add("A");
        cowList.add("B");
        cowList.add("C");

        // 遍历过程中修改不会抛异常
        System.out.println("  遍历时修改（不会抛 ConcurrentModificationException）:");
        for (String item : cowList) {
            System.out.println("    读取: " + item);
            if ("A".equals(item)) {
                cowList.add("D");
                System.out.println("    添加: D");
            }
        }
        System.out.println("  最终列表: " + cowList + "\n");

        // BlockingQueue 演示
        System.out.println("【BlockingQueue 阻塞队列】");
        System.out.println("  生产者消费者模式的最佳实现\n");

        ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(3);

        // 生产者
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 5; i++) {
                    String item = "Item-" + i;
                    queue.put(item);  // 队列满时阻塞
                    System.out.println("  生产: " + item + ", 队列大小: " + queue.size());
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // 消费者
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(500);  // 延迟启动
                for (int i = 1; i <= 5; i++) {
                    String item = queue.take();  // 队列空时阻塞
                    System.out.println("  消费: " + item + ", 队列大小: " + queue.size());
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println();
    }

    /**
     * 解决方案6：ThreadLocal
     */
    private static void solution6_ThreadLocal() throws InterruptedException {
        System.out.println("========== 解决方案6：ThreadLocal ==========\n");

        System.out.println("【ThreadLocal 原理】");
        System.out.println("  每个线程内部有一个 ThreadLocalMap");
        System.out.println("  ThreadLocal 作为 key，线程私有变量作为 value");
        System.out.println("  实现线程隔离，天然线程安全\n");

        System.out.println("【ThreadLocal 演示】");
        ThreadLocal<String> threadLocal = new ThreadLocal<>();

        Thread t1 = new Thread(() -> {
            threadLocal.set("线程1的值");
            System.out.println("  线程1 设置: " + threadLocal.get());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("  线程1 读取: " + threadLocal.get());
            threadLocal.remove();  // 避免内存泄漏
        });

        Thread t2 = new Thread(() -> {
            threadLocal.set("线程2的值");
            System.out.println("  线程2 设置: " + threadLocal.get());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("  线程2 读取: " + threadLocal.get());
            threadLocal.remove();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\n【ThreadLocal 适用场景】");
        System.out.println("  1. 数据库连接（Connection）");
        System.out.println("  2. Session 信息");
        System.out.println("  3. 用户信息、请求上下文");
        System.out.println("  4. SimpleDateFormat（线程不安全）\n");

        System.out.println("【内存泄漏问题】");
        System.out.println("  ThreadLocalMap 的 key 是弱引用");
        System.out.println("  如果 ThreadLocal 对象被回收，key 变为 null");
        System.out.println("  但 value 仍然存在，造成内存泄漏");
        System.out.println("  解决：使用完毕后调用 remove()\n");
    }

    /**
     * 解决方案7：不可变对象
     */
    private static void solution7_ImmutableObject() {
        System.out.println("========== 解决方案7：不可变对象 ==========\n");

        System.out.println("【不可变对象的特点】");
        System.out.println("  1. 对象创建后状态不能改变");
        System.out.println("  2. 所有字段都是 final");
        System.out.println("  3. 没有 setter 方法");
        System.out.println("  4. 不提供修改内部状态的方法");
        System.out.println("  5. 天然线程安全，无需同步\n");

        System.out.println("【JDK 中的不可变对象】");
        System.out.println("  - String（字符串池）");
        System.out.println("  - Integer/Long 等包装类");
        System.out.println("  - BigDecimal/BigInteger\n");

        // 不可变对象演示
        System.out.println("【不可变对象演示】");
        ImmutableUser user = new ImmutableUser("张三", 25);
        System.out.println("  创建用户: " + user);

        // user.setName("李四");  // 编译错误，没有 setter
        ImmutableUser newUser = user.withName("李四");
        System.out.println("  修改名字后新对象: " + newUser);
        System.out.println("  原对象不变: " + user);
        System.out.println();

        System.out.println("【如何创建不可变类】");
        System.out.println("  1. 类声明为 final（防止子类覆盖）");
        System.out.println("  2. 所有字段声明为 private final");
        System.out.println("  3. 不提供 setter 方法");
        System.out.println("  4. 构造函数中进行深拷贝");
        System.out.println("  5. getter 返回副本（如果是可变对象）\n");
    }

    // ==================== 第三部分：综合对比 ====================

    /**
     * 综合对比各种方案
     */
    private static void compareAllSolutions() {
        System.out.println("========== 解决方案综合对比 ==========\n");

        System.out.println("┌───────────────┬───────────┬───────────┬───────────┬───────────────────┐");
        System.out.println("│  方案         │  原子性   │  可见性   │  有序性   │  适用场景          │");
        System.out.println("├───────────────┼───────────┼───────────┼───────────┼───────────────────┤");
        System.out.println("│  synchronized │  ✓        │  ✓        │  ✓        │  通用，临界区保护  │");
        System.out.println("│  Lock         │  ✓        │  ✓        │  ✓        │  需要高级特性      │");
        System.out.println("│  volatile     │  ✗        │  ✓        │  ✓        │  状态标志          │");
        System.out.println("│  原子类       │  ✓        │  ✓        │  ✓        │  计数器，CAS操作   │");
        System.out.println("│  并发容器     │  ✓        │  ✓        │  ✓        │  并发集合          │");
        System.out.println("│  ThreadLocal  │  隔离     │  隔离     │  隔离     │  线程私有数据      │");
        System.out.println("│  不可变对象   │  天然安全 │  天然安全 │  天然安全 │  值对象            │");
        System.out.println("└───────────────┴───────────┴───────────┴───────────┴───────────────────┘\n");

        System.out.println("【选择建议】");
        System.out.println("  1. 简单计数器：AtomicInteger 或 LongAdder");
        System.out.println("  2. 复杂操作：synchronized 或 Lock");
        System.out.println("  3. 状态标志：volatile");
        System.out.println("  4. 并发集合：ConcurrentHashMap");
        System.out.println("  5. 线程隔离：ThreadLocal");
        System.out.println("  6. 值对象：不可变对象\n");
    }

    // ==================== 第四部分：高频面试题 ====================

    /**
     * 高频面试题
     */
    private static void interviewQuestions() {
        System.out.println("==================== 高频面试题 ====================\n");

        // Q1
        System.out.println("【面试题1】synchronized 和 Lock 有什么区别？\n");
        System.out.println("┌─────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│   特性      │   synchronized      │   Lock              │");
        System.out.println("├─────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│  实现层面   │ JVM 内置            │ Java API            │");
        System.out.println("│  释放方式   │ 自动释放            │ 必须手动 unlock     │");
        System.out.println("│  可中断     │ 不可中断            │ lockInterruptibly   │");
        System.out.println("│  超时获取   │ 不支持              │ tryLock(timeout)    │");
        System.out.println("│  公平性     │ 只有非公平          │ 可配置              │");
        System.out.println("│  条件队列   │ 单个(wait/notify)   │ 多个 Condition      │");
        System.out.println("│  锁绑定     │ 一个对象一把锁      │ 可绑定多个条件      │");
        System.out.println("└─────────────┴─────────────────────┴─────────────────────┘\n");

        // Q2
        System.out.println("【面试题2】volatile 能保证线程安全吗？\n");
        System.out.println("  不能完全保证。volatile 只能保证可见性和有序性，不能保证原子性。");
        System.out.println("  ");
        System.out.println("  例如：volatile int count; count++ 不是线程安全的");
        System.out.println("  因为 count++ 包含三步操作：读取、修改、写入");
        System.out.println("  ");
        System.out.println("  volatile 适用场景：");
        System.out.println("  - 状态标志（停止标志、初始化完成标志）");
        System.out.println("  - 单例模式的双重检查锁定（DCL）\n");

        // Q3
        System.out.println("【面试题3】CAS 是什么？有什么问题？\n");
        System.out.println("  CAS (Compare And Swap) 是一种乐观锁实现。");
        System.out.println("  ");
        System.out.println("  原理：比较内存值与预期值，相等则更新，否则重试");
        System.out.println("  ");
        System.out.println("  问题：");
        System.out.println("  1. ABA 问题：值从 A→B→A，CAS 误认为没变");
        System.out.println("     解决：AtomicStampedReference（版本号）");
        System.out.println("  2. 自旋消耗 CPU：高竞争时不断重试");
        System.out.println("     解决：LongAdder（分散竞争）");
        System.out.println("  3. 只能保证单个变量原子性");
        System.out.println("     解决：AtomicReference 或加锁\n");

        // Q4
        System.out.println("【面试题4】什么是死锁？如何避免？\n");
        System.out.println("  死锁：两个或多个线程互相等待对方持有的资源。");
        System.out.println("  ");
        System.out.println("  四个必要条件：");
        System.out.println("  1. 互斥条件：资源只能被一个线程占用");
        System.out.println("  2. 请求与保持：持有资源同时请求新资源");
        System.out.println("  3. 不剥夺条件：已获得的资源不能被强制剥夺");
        System.out.println("  4. 循环等待：线程间形成资源请求环路");
        System.out.println("  ");
        System.out.println("  避免方法：");
        System.out.println("  1. 破坏循环等待：按固定顺序获取锁");
        System.out.println("  2. 使用 tryLock 超时机制");
        System.out.println("  3. 使用并发工具类替代手动加锁\n");

        // Q5
        System.out.println("【面试题5】synchronized 的锁升级过程？\n");
        System.out.println("  无锁 → 偏向锁 → 轻量级锁 → 重量级锁");
        System.out.println("  ");
        System.out.println("  1. 偏向锁：只有一个线程访问");
        System.out.println("     - Mark Word 记录线程 ID");
        System.out.println("     - 下次同线程访问无需 CAS");
        System.out.println("  ");
        System.out.println("  2. 轻量级锁：多个线程交替访问（无竞争）");
        System.out.println("     - CAS 自旋获取锁");
        System.out.println("     - 不会阻塞线程");
        System.out.println("  ");
        System.out.println("  3. 重量级锁：多个线程同时竞争");
        System.out.println("     - 自旋失败后升级");
        System.out.println("     - 线程阻塞，需要操作系统调度");
        System.out.println("  ");
        System.out.println("  注意：锁只能升级，不能降级\n");

        // Q6
        System.out.println("【面试题6】ThreadLocal 会导致内存泄漏吗？\n");
        System.out.println("  可能会。");
        System.out.println("  ");
        System.out.println("  原因：");
        System.out.println("  - ThreadLocalMap 的 key 是弱引用（ThreadLocal）");
        System.out.println("  - value 是强引用");
        System.out.println("  - 如果 ThreadLocal 被回收，key 变为 null");
        System.out.println("  - 但 value 仍然存在，无法被回收");
        System.out.println("  ");
        System.out.println("  解决方案：");
        System.out.println("  - 使用完毕后调用 remove()");
        System.out.println("  - 尤其在线程池中，线程复用时必须清理\n");

        // Q7
        System.out.println("【面试题7】ConcurrentHashMap 是如何保证线程安全的？\n");
        System.out.println("  JDK 7：分段锁（Segment）");
        System.out.println("  - 默认 16 个 Segment");
        System.out.println("  - 每个 Segment 是一个小的 HashMap");
        System.out.println("  - 不同 Segment 可并发操作");
        System.out.println("  ");
        System.out.println("  JDK 8：CAS + synchronized");
        System.out.println("  - 取消 Segment，直接用 Node 数组");
        System.out.println("  - 空桶：CAS 插入");
        System.out.println("  - 非空桶：synchronized 锁住头节点");
        System.out.println("  - 锁粒度更细，并发度更高\n");

        // Q8
        System.out.println("【面试题8】什么是可见性？为什么会有可见性问题？\n");
        System.out.println("  可见性：一个线程修改变量后，其他线程能立即看到最新值。");
        System.out.println("  ");
        System.out.println("  为什么会有问题：");
        System.out.println("  - 每个线程有自己的工作内存（CPU缓存）");
        System.out.println("  - 线程先从主内存读取变量到工作内存");
        System.out.println("  - 修改后可能不会立即刷新到主内存");
        System.out.println("  - 其他线程看到的还是旧值");
        System.out.println("  ");
        System.out.println("  解决方案：");
        System.out.println("  - volatile：强制刷新到主内存");
        System.out.println("  - synchronized：加锁时从主内存读，解锁时刷新");
        System.out.println("  - final：不可变，天然可见\n");

        // Q9
        System.out.println("【面试题9】LongAdder 为什么比 AtomicLong 快？\n");
        System.out.println("  AtomicLong 问题：");
        System.out.println("  - 所有线程竞争同一个变量");
        System.out.println("  - 高并发时 CAS 失败率高，不断重试");
        System.out.println("  ");
        System.out.println("  LongAdder 优化：");
        System.out.println("  - 内部维护一个 base 和 Cell 数组");
        System.out.println("  - 无竞争时直接操作 base");
        System.out.println("  - 有竞争时分散到不同 Cell");
        System.out.println("  - 获取结果时累加 base + 所有 Cell");
        System.out.println("  - 减少了竞争，提高了吞吐量\n");

        // Q10
        System.out.println("【面试题10】如何选择合适的线程安全方案？\n");
        System.out.println("  1. 简单计数器");
        System.out.println("     → AtomicInteger（低并发）");
        System.out.println("     → LongAdder（高并发）");
        System.out.println("  ");
        System.out.println("  2. 复杂临界区");
        System.out.println("     → synchronized（简单场景）");
        System.out.println("     → Lock（需要超时、中断等高级特性）");
        System.out.println("  ");
        System.out.println("  3. 状态标志");
        System.out.println("     → volatile");
        System.out.println("  ");
        System.out.println("  4. 并发集合");
        System.out.println("     → ConcurrentHashMap（读写都多）");
        System.out.println("     → CopyOnWriteArrayList（读多写少）");
        System.out.println("  ");
        System.out.println("  5. 线程隔离数据");
        System.out.println("     → ThreadLocal");
        System.out.println("  ");
        System.out.println("  6. 值对象");
        System.out.println("     → 不可变对象\n");
    }

    // ==================== 辅助类 ====================

    // 不安全的计数器
    static class UnsafeCounter {
        private int count = 0;

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    // 可见性问题演示
    static class VisibilityProblem {
        int value = 0;  // 没有 volatile
    }

    // volatile 可见性
    static class VolatileVisibility {
        volatile boolean flag = false;
    }

    // volatile 计数器（不安全）
    static class VolatileCounter {
        private volatile int count = 0;

        public void increment() {
            count++;
        }

        public int getCount() {
            return count;
        }
    }

    // synchronized 方法计数器
    static class SyncMethodCounter {
        private int count = 0;

        public synchronized void increment() {
            count++;
        }

        public synchronized int getCount() {
            return count;
        }
    }

    // synchronized 静态方法计数器
    static class SyncStaticCounter {
        private static int count = 0;

        public static synchronized void increment() {
            count++;
        }

        public static synchronized int getCount() {
            return count;
        }

        public static void reset() {
            count = 0;
        }
    }

    // synchronized 代码块计数器
    static class SyncBlockCounter {
        private int count = 0;
        private final Object lock = new Object();

        public void increment() {
            synchronized (lock) {
                count++;
            }
        }

        public int getCount() {
            synchronized (lock) {
                return count;
            }
        }
    }

    // ReentrantLock 计数器
    static class ReentrantLockCounter {
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

    // 读写锁缓存
    static class ReadWriteLockCache {
        private final Map<String, String> cache = new HashMap<>();
        private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

        public String get(String key) {
            rwLock.readLock().lock();
            try {
                return cache.get(key);
            } finally {
                rwLock.readLock().unlock();
            }
        }

        public void put(String key, String value) {
            rwLock.writeLock().lock();
            try {
                cache.put(key, value);
            } finally {
                rwLock.writeLock().unlock();
            }
        }
    }

    // 不可变对象
    static final class ImmutableUser {
        private final String name;
        private final int age;

        public ImmutableUser(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        // 返回新对象，而非修改
        public ImmutableUser withName(String newName) {
            return new ImmutableUser(newName, this.age);
        }

        public ImmutableUser withAge(int newAge) {
            return new ImmutableUser(this.name, newAge);
        }

        @Override
        public String toString() {
            return "ImmutableUser{name='" + name + "', age=" + age + "}";
        }
    }

    // 辅助方法：运行计数器测试
    private static void runCounterTest(Runnable incrementAction, java.util.function.IntSupplier getCount) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    incrementAction.run();
                }
                latch.countDown();
            }).start();
        }
        latch.await();

        System.out.println("     预期: 10000, 实际: " + getCount.getAsInt() + "\n");
    }

    // 辅助方法：运行静态计数器测试
    private static void runStaticCounterTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(10);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    SyncStaticCounter.increment();
                }
                latch.countDown();
            }).start();
        }
        latch.await();

        System.out.println("     预期: 10000, 实际: " + SyncStaticCounter.getCount() + "\n");
    }
}
