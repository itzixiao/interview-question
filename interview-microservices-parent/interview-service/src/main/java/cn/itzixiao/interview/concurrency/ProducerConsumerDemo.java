package cn.itzixiao.interview.concurrency;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * =====================================================================================
 * ReentrantLock 实现生产者消费者模式
 * =====================================================================================
 * <p>
 * 一、生产者消费者模式概述
 * -------------------------------------------------------------------------------------
 * 生产者消费者模式是一种经典的并发协作模式：
 * - 生产者：负责生产数据，放入缓冲区
 * - 消费者：从缓冲区取出数据进行消费
 * - 缓冲区：存储数据的容器，通常是有界队列
 * <p>
 * 核心问题：
 * 1. 缓冲区满时，生产者需要等待
 * 2. 缓冲区空时，消费者需要等待
 * 3. 需要正确处理线程间的通信
 * <p>
 * 二、为什么用 ReentrantLock 而不是 synchronized？
 * -------------------------------------------------------------------------------------
 * | 特性                | synchronized    | ReentrantLock           |
 * |---------------------|-----------------|-------------------------|
 * | 锁获取方式           | JVM 自动管理     | 手动 lock/unlock        |
 * | 条件变量             | 单个 wait/notify | 多个 Condition          |
 * | 公平性               | 非公平           | 可选公平/非公平          |
 * | 可中断               | 不支持           | 支持lockInterruptibly   |
 * | 超时获取             | 不支持           | 支持tryLock(timeout)    |
 * <p>
 * ReentrantLock 的优势：
 * - 多个 Condition：生产者和消费者可以使用不同的条件队列，避免"惊群效应"
 * - 灵活性高：可以精细控制锁的获取和释放
 * - 公平锁：可以避免线程饥饿
 */
public class ProducerConsumerDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== ReentrantLock 实现生产者消费者 ==========\n");

        // 1. 基本实现演示
        demonstrateBasicProducerConsumer();

        // 2. 多生产者多消费者
        demonstrateMultipleProducersConsumers();

        // 3. 对比 synchronized 实现
        demonstrateSynchronizedVersion();

        // 4. LinkedBlockingQueue 实现（推荐方式）
        demonstrateLinkedBlockingQueue();

        // 5. 高频面试题
        showInterviewQuestions();
    }

    /**
     * 1. 基本实现演示
     */
    private static void demonstrateBasicProducerConsumer() throws InterruptedException {
        System.out.println("【一、基本实现演示】\n");

        BoundedBuffer<String> buffer = new BoundedBuffer<>(5);

        Thread producer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    String item = "商品-" + i;
                    buffer.put(item);
                    System.out.println("  [生产者] 生产: " + item + " | 缓冲区大小: " + buffer.size());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    String item = buffer.take();
                    System.out.println("  [消费者] 消费: " + item + " | 缓冲区大小: " + buffer.size());
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Consumer");

        producer.start();
        Thread.sleep(50);
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("\n演示完成！\n");
    }

    /**
     * 2. 多生产者多消费者
     */
    private static void demonstrateMultipleProducersConsumers() throws InterruptedException {
        System.out.println("【二、多生产者多消费者】\n");

        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(3);

        Thread[] producers = new Thread[3];
        for (int p = 0; p < 3; p++) {
            final int producerId = p;
            producers[p] = new Thread(() -> {
                for (int i = 0; i < 5; i++) {
                    try {
                        int item = producerId * 100 + i;
                        buffer.put(item);
                        System.out.println("  [生产者-" + producerId + "] 生产: " + item);
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Producer-" + p);
        }

        Thread[] consumers = new Thread[2];
        for (int c = 0; c < 2; c++) {
            final int consumerId = c;
            consumers[c] = new Thread(() -> {
                for (int i = 0; i < 7; i++) {
                    try {
                        Integer item = buffer.take();
                        System.out.println("    [消费者-" + consumerId + "] 消费: " + item);
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }, "Consumer-" + c);
        }

        for (Thread p : producers) p.start();
        for (Thread c : consumers) c.start();

        for (Thread p : producers) p.join();
        for (Thread c : consumers) c.join();

        System.out.println("\n多生产者多消费者演示完成！\n");
    }

    /**
     * 3. 对比 synchronized 实现
     */
    private static void demonstrateSynchronizedVersion() {
        System.out.println("【三、ReentrantLock vs synchronized 对比】\n");

        System.out.println("synchronized 实现方式：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  synchronized (lock) {                                      │");
        System.out.println("│      while (queue.isFull()) {                               │");
        System.out.println("│          lock.wait();  // 只有一个等待队列                   │");
        System.out.println("│      }                                                      │");
        System.out.println("│      queue.add(item);                                       │");
        System.out.println("│      lock.notifyAll();  // 唤醒所有线程（效率低）            │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("ReentrantLock 实现方式：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  lock.lock();                                               │");
        System.out.println("│  try {                                                      │");
        System.out.println("│      while (queue.isFull()) {                               │");
        System.out.println("│          notFull.await();  // 生产者独立等待队列             │");
        System.out.println("│      }                                                      │");
        System.out.println("│      queue.add(item);                                       │");
        System.out.println("│      notEmpty.signal();  // 精准唤醒消费者                  │");
        System.out.println("│  } finally {                                                │");
        System.out.println("│      lock.unlock();                                         │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("ReentrantLock 的优势：");
        System.out.println("  1. 多条件变量：生产者和消费者分别等待，避免相互干扰");
        System.out.println("  2. 精准唤醒：signal() 只唤醒特定类型的线程");
        System.out.println("  3. 避免惊群：不会唤醒所有线程再让大部分重新等待");
        System.out.println("  4. 公平性：可选择公平锁，避免线程饥饿\n");
    }

    /**
     * 4. LinkedBlockingQueue 实现（推荐方式）
     * <p>
     * LinkedBlockingQueue 是 Java 并发包提供的阻塞队列实现：
     * - 内部使用 ReentrantLock + Condition 实现
     * - 提供了现成的 put() 和 take() 阻塞方法
     * - 无需手动处理锁和条件变量
     * - 是生产者消费者模式的最佳实践
     */
    private static void demonstrateLinkedBlockingQueue() throws InterruptedException {
        System.out.println("【四、LinkedBlockingQueue 实现（推荐）】\n");

        // 创建有界阻塞队列（容量为 5）
        BlockingQueue<String> queue = new LinkedBlockingQueue<>(5);

        System.out.println("LinkedBlockingQueue 特点：");
        System.out.println("  - 基于链表的阻塞队列，可选有界或无界");
        System.out.println("  - 内部使用两把锁（putLock、takeLock），读写分离，并发度高");
        System.out.println("  - put(e)：队列满时阻塞，直到有空间");
        System.out.println("  - take()：队列空时阻塞，直到有元素");
        System.out.println("  - offer(e, timeout, unit)：带超时的插入");
        System.out.println("  - poll(timeout, unit)：带超时的获取\n");

        // 生产者线程
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    String item = "数据-" + i;
                    queue.put(item);  // 阻塞式放入
                    System.out.println("  [生产者] 放入: " + item + " | 队列大小: " + queue.size());
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Producer");

        // 消费者线程
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    String item = queue.take();  // 阻塞式取出
                    System.out.println("  [消费者] 取出: " + item + " | 队列大小: " + queue.size());
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Consumer");

        producer.start();
        Thread.sleep(50);
        consumer.start();

        producer.join();
        consumer.join();

        System.out.println("\nLinkedBlockingQueue 演示完成！\n");

        // 对比三种实现方式
        System.out.println("【三种实现方式对比】\n");
        System.out.println("┌─────────────────┬─────────────────┬─────────────────┬─────────────────┐");
        System.out.println("│     特性        │  synchronized   │  ReentrantLock  │ LinkedBlockingQueue │");
        System.out.println("├─────────────────┼─────────────────┼─────────────────┼─────────────────┤");
        System.out.println("│  代码复杂度     │      中         │      高         │      低         │");
        System.out.println("│  灵活性         │      低         │      高         │      中         │");
        System.out.println("│  性能           │      中         │      高         │      高         │");
        System.out.println("│  推荐度         │      ★★        │      ★★★       │      ★★★★★   │");
        System.out.println("│  使用场景       │    简单场景     │   需要精细控制   │   大多数场景    │");
        System.out.println("└─────────────────┴─────────────────┴─────────────────┴─────────────────┘\n");

        System.out.println("LinkedBlockingQueue 源码要点：");
        System.out.println("  1. 两把锁分离：putLock 和 takeLock，生产和消费可以并行");
        System.out.println("  2. 两个条件：notEmpty 和 notFull，精准唤醒");
        System.out.println("  3. 原子计数：AtomicInteger count，保证计数原子性");
        System.out.println("  4. 内存一致性：使用 full GC 屏障保证可见性\n");
    }

    /**
     * 5. 高频面试题
     */
    private static void showInterviewQuestions() {
        System.out.println("【五、高频面试题】\n");

        System.out.println("Q1: 生产者消费者模式的核心是什么？");
        System.out.println("答案：");
        System.out.println("  - 互斥：缓冲区是共享资源，需要互斥访问");
        System.out.println("  - 同步：生产者和消费者需要协调工作");
        System.out.println("  - 通信：缓冲区状态变化时需要通知对方\n");

        System.out.println("Q2: 为什么用 while 而不是 if 判断条件？");
        System.out.println("答案：");
        System.out.println("  防止虚假唤醒（Spurious Wakeup）：");
        System.out.println("  - 线程可能在没有收到 signal/notify 的情况下被唤醒");
        System.out.println("  - 使用 while 可以在被唤醒后重新检查条件");
        System.out.println("  - 如果用 if，虚假唤醒会导致逻辑错误\n");

        System.out.println("Q3: Condition 的 signal 和 signalAll 有什么区别？");
        System.out.println("答案：");
        System.out.println("  - signal()：唤醒一个等待线程（效率高）");
        System.out.println("  - signalAll()：唤醒所有等待线程（更安全但效率低）");
        System.out.println("  - ReentrantLock 可以用多个 Condition，signal 更精准\n");

        System.out.println("Q4: ReentrantLock 的公平锁和非公平锁有什么区别？");
        System.out.println("答案：");
        System.out.println("  非公平锁（默认）：");
        System.out.println("    - 获取锁时不排队，直接尝试获取");
        System.out.println("    - 性能高，但可能导致线程饥饿");
        System.out.println("  公平锁：");
        System.out.println("    - 按照请求顺序获取锁");
        System.out.println("    - 不会饥饿，但性能略低");
        System.out.println("    - 创建方式：new ReentrantLock(true)\n");

        System.out.println("Q5: 生产者消费者模式有哪些实际应用？");
        System.out.println("答案：");
        System.out.println("  1. 线程池：任务队列 + 工作线程");
        System.out.println("  2. 消息队列：Kafka、RabbitMQ");
        System.out.println("  3. 数据库连接池：连接的生产和复用");
        System.out.println("  4. 日志系统：异步日志写入\n");

        System.out.println("Q6: 如何避免死锁？");
        System.out.println("答案：");
        System.out.println("  1. 确保锁的获取和释放成对出现（try-finally）");
        System.out.println("  2. 避免嵌套锁");
        System.out.println("  3. 使用 tryLock 设置超时");
        System.out.println("  4. 使用更高级的并发工具（BlockingQueue）\n");

        System.out.println("Q7: LinkedBlockingQueue 和 ArrayBlockingQueue 有什么区别？");
        System.out.println("答案：");
        System.out.println("  LinkedBlockingQueue：");
        System.out.println("    - 基于链表，可无界也可有界");
        System.out.println("    - 两把锁（putLock、takeLock），读写分离");
        System.out.println("    - 吞吐量更高");
        System.out.println("  ArrayBlockingQueue：");
        System.out.println("    - 基于数组，必须有界");
        System.out.println("    - 一把锁，读写互斥");
        System.out.println("    - 内存占用更小\n");

        System.out.println("Q8: 为什么推荐使用 BlockingQueue？");
        System.out.println("答案：");
        System.out.println("  1. 代码简洁：无需手动处理锁和条件变量");
        System.out.println("  2. 功能完整：提供阻塞、超时、非阻塞等多种操作");
        System.out.println("  3. 经过充分测试：JDK 标准库，稳定可靠");
        System.out.println("  4. 性能优化：内部实现经过高度优化\n");
    }

    /**
     * =====================================================================================
     * 有界缓冲区实现（核心代码）
     * =====================================================================================
     */
    static class BoundedBuffer<T> {
        private final Queue<T> queue;
        private final int capacity;
        private final ReentrantLock lock;
        private final Condition notFull;   // 缓冲区未满条件
        private final Condition notEmpty;  // 缓冲区非空条件

        public BoundedBuffer(int capacity) {
            this.capacity = capacity;
            this.queue = new LinkedList<>();
            this.lock = new ReentrantLock(true); // 使用公平锁
            this.notFull = lock.newCondition();   // 生产者等待条件
            this.notEmpty = lock.newCondition();  // 消费者等待条件
        }

        /**
         * 生产者：放入元素
         */
        public void put(T item) throws InterruptedException {
            lock.lock();
            try {
                while (queue.size() == capacity) {
                    notFull.await(); // 缓冲区已满，等待
                }
                queue.offer(item);
                notEmpty.signal(); // 唤醒消费者
            } finally {
                lock.unlock();
            }
        }

        /**
         * 消费者：取出元素
         */
        public T take() throws InterruptedException {
            lock.lock();
            try {
                while (queue.isEmpty()) {
                    notEmpty.await(); // 缓冲区为空，等待
                }
                T item = queue.poll();
                notFull.signal(); // 唤醒生产者
                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return queue.size();
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 对比：使用 synchronized 实现的版本
     */
    static class SynchronizedBuffer<T> {
        private final Queue<T> queue;
        private final int capacity;
        private final Object lock = new Object();

        public SynchronizedBuffer(int capacity) {
            this.capacity = capacity;
            this.queue = new LinkedList<>();
        }

        public void put(T item) throws InterruptedException {
            synchronized (lock) {
                while (queue.size() == capacity) {
                    lock.wait();
                }
                queue.offer(item);
                lock.notifyAll();
            }
        }

        public T take() throws InterruptedException {
            synchronized (lock) {
                while (queue.isEmpty()) {
                    lock.wait();
                }
                T item = queue.poll();
                lock.notifyAll();
                return item;
            }
        }
    }
}
