package cn.itzixiao.interview.redis.advanced;

import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式锁详解
 *
 * Redisson 是 Redis 的 Java 客户端，提供了丰富的分布式功能
 *
 * 锁的类型：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 可重入锁（Reentrant Lock）                               │
 * │     - 同一线程可以多次获取锁                                 │
 * │     - 支持看门狗自动续期                                     │
 * │                                                             │
 * │  2. 公平锁（Fair Lock）                                      │
 * │     - 按请求锁的顺序获取锁                                   │
 * │     - 防止饥饿                                               │
 * │                                                             │
 * │  3. 联锁（MultiLock）                                        │
 * │     - 同时获取多个锁，防止死锁                               │
 * │                                                             │
 * │  4. 红锁（RedLock）                                          │
 * │     - 在多个 Redis 节点上加锁                                │
 * │     - 提高可靠性                                             │
 * │                                                             │
 * │  5. 读写锁（ReadWriteLock）                                  │
 * │     - 读读共享，读写互斥，写写互斥                           │
 * │                                                             │
 * │  6. 信号量（Semaphore）                                      │
 * │     - 控制同时访问的线程数量                                 │
 * │                                                             │
 * │  7. 闭锁（CountDownLatch）                                   │
 * │     - 等待多个线程完成                                       │
 * └─────────────────────────────────────────────────────────────┘
 */
public class RedissonDistributedLockDemo {

    private static RedissonClient redisson;

    public static void main(String[] args) throws Exception {
        System.out.println("========== Redisson 分布式锁详解 ==========\n");

        // 初始化 Redisson
        initRedisson();

        // 1. 可重入锁
        demonstrateReentrantLock();

        // 2. 公平锁
        demonstrateFairLock();

        // 3. 读写锁
        demonstrateReadWriteLock();

        // 4. 信号量
        demonstrateSemaphore();

        // 5. 闭锁
        demonstrateCountDownLatch();

        // 6. 联锁
        demonstrateMultiLock();

        // 关闭 Redisson
        redisson.shutdown();
    }

    private static void initRedisson() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379")
                .setConnectionMinimumIdleSize(10)
                .setConnectionPoolSize(64);

        redisson = Redisson.create(config);
        System.out.println("Redisson 连接成功\n");
    }

    /**
     * 1. 可重入锁
     */
    private static void demonstrateReentrantLock() throws InterruptedException {
        System.out.println("【1. 可重入锁（Reentrant Lock）】\n");

        System.out.println("特点：");
        System.out.println("- 同一线程可以多次获取锁（重入）");
        System.out.println("- 支持看门狗自动续期");
        System.out.println("- 默认非公平锁\n");

        RLock lock = redisson.getLock("myLock");

        // 方式1：手动设置过期时间（不会自动续期）
        System.out.println("方式1：手动设置过期时间");
        boolean isLocked = lock.tryLock(3, 10, TimeUnit.SECONDS);
        if (isLocked) {
            try {
                System.out.println("  获取锁成功，执行业务逻辑...");
                // 模拟业务执行
                Thread.sleep(2000);
            } finally {
                lock.unlock();
                System.out.println("  释放锁");
            }
        }
        System.out.println();

        // 方式2：使用看门狗自动续期（推荐）
        System.out.println("方式2：看门狗自动续期");
        System.out.println("  - 不指定过期时间时，默认30秒");
        System.out.println("  - 看门狗每10秒检查一次，续期到30秒");
        System.out.println("  - 业务执行完自动停止续期\n");

        // 模拟重入
        System.out.println("演示锁重入：");
        lock.lock();
        try {
            System.out.println("  第一次获取锁");
            lock.lock();  // 同一线程可以再次获取
            try {
                System.out.println("  第二次获取锁（重入）");
            } finally {
                lock.unlock();
                System.out.println("  释放第二层锁");
            }
        } finally {
            lock.unlock();
            System.out.println("  释放第一层锁\n");
        }
    }

    /**
     * 2. 公平锁
     */
    private static void demonstrateFairLock() throws InterruptedException {
        System.out.println("【2. 公平锁（Fair Lock）】\n");

        System.out.println("特点：");
        System.out.println("- 按请求锁的顺序获取锁");
        System.out.println("- 先请求的线程先获得锁");
        System.out.println("- 吞吐量较低，但避免饥饿\n");

        RLock fairLock = redisson.getFairLock("myFairLock");

        Runnable task = () -> {
            String threadName = Thread.currentThread().getName();
            try {
                System.out.println("  " + threadName + " 请求锁");
                fairLock.lock();
                System.out.println("  " + threadName + " 获取锁");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                fairLock.unlock();
                System.out.println("  " + threadName + " 释放锁");
            }
        };

        // 启动多个线程测试公平性
        for (int i = 0; i < 3; i++) {
            new Thread(task, "Thread-" + i).start();
            Thread.sleep(100); // 确保按顺序请求
        }

        Thread.sleep(3000);
        System.out.println();
    }

    /**
     * 3. 读写锁
     */
    private static void demonstrateReadWriteLock() throws InterruptedException {
        System.out.println("【3. 读写锁（ReadWriteLock）】\n");

        System.out.println("特点：");
        System.out.println("- 读读不互斥：多个读线程可同时访问");
        System.out.println("- 读写互斥：读时不能写，写时不能读");
        System.out.println("- 写写互斥：多个写线程互斥\n");

        RReadWriteLock rwLock = redisson.getReadWriteLock("myRWLock");
        RLock readLock = rwLock.readLock();
        RLock writeLock = rwLock.writeLock();

        // 读线程
        Runnable readTask = () -> {
            String threadName = Thread.currentThread().getName();
            readLock.lock();
            try {
                System.out.println("  " + threadName + " 获取读锁，读取数据...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                readLock.unlock();
                System.out.println("  " + threadName + " 释放读锁");
            }
        };

        // 写线程
        Runnable writeTask = () -> {
            String threadName = Thread.currentThread().getName();
            writeLock.lock();
            try {
                System.out.println("  " + threadName + " 获取写锁，写入数据...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                writeLock.unlock();
                System.out.println("  " + threadName + " 释放写锁");
            }
        };

        System.out.println("启动2个读线程和1个写线程：");
        new Thread(readTask, "Read-1").start();
        new Thread(readTask, "Read-2").start();
        Thread.sleep(100);
        new Thread(writeTask, "Write-1").start();

        Thread.sleep(3000);
        System.out.println();
    }

    /**
     * 4. 信号量
     */
    private static void demonstrateSemaphore() throws InterruptedException {
        System.out.println("【4. 信号量（Semaphore）】\n");

        System.out.println("特点：");
        System.out.println("- 控制同时访问的线程数量");
        System.out.println("- 类似限流功能\n");

        RSemaphore semaphore = redisson.getSemaphore("mySemaphore");
        // 设置可用许可数为3
        semaphore.trySetPermits(3);

        Runnable task = () -> {
            String threadName = Thread.currentThread().getName();
            try {
                System.out.println("  " + threadName + " 尝试获取许可");
                semaphore.acquire();
                System.out.println("  " + threadName + " 获取许可成功，剩余: " + semaphore.availablePermits());
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                semaphore.release();
                System.out.println("  " + threadName + " 释放许可，剩余: " + semaphore.availablePermits());
            }
        };

        System.out.println("启动5个线程，但只有3个许可：");
        for (int i = 0; i < 5; i++) {
            new Thread(task, "Thread-" + i).start();
            Thread.sleep(100);
        }

        Thread.sleep(4000);
        System.out.println();
    }

    /**
     * 5. 闭锁
     */
    private static void demonstrateCountDownLatch() throws InterruptedException {
        System.out.println("【5. 闭锁（CountDownLatch）】\n");

        System.out.println("特点：");
        System.out.println("- 等待多个线程完成");
        System.out.println("- 类似 Java 的 CountDownLatch\n");

        RCountDownLatch latch = redisson.getCountDownLatch("myLatch");
        // 设置计数为3
        latch.trySetCount(3);

        Runnable worker = () -> {
            String threadName = Thread.currentThread().getName();
            try {
                System.out.println("  " + threadName + " 开始工作...");
                Thread.sleep(1000);
                System.out.println("  " + threadName + " 完成工作");
                latch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        System.out.println("主线程等待3个工作线程完成...");
        for (int i = 0; i < 3; i++) {
            new Thread(worker, "Worker-" + i).start();
        }

        latch.await();
        System.out.println("所有工作线程完成，主线程继续执行\n");
    }

    /**
     * 6. 联锁
     */
    private static void demonstrateMultiLock() throws InterruptedException {
        System.out.println("【6. 联锁（MultiLock）】\n");

        System.out.println("特点：");
        System.out.println("- 同时获取多个锁");
        System.out.println("- 防止死锁（要么都获取，要么都不获取）");
        System.out.println("- 类似数据库的多行锁\n");

        RLock lock1 = redisson.getLock("lock1");
        RLock lock2 = redisson.getLock("lock2");
        RLock lock3 = redisson.getLock("lock3");

        // 创建联锁
        RLock multiLock = redisson.getMultiLock(lock1, lock2, lock3);

        System.out.println("尝试同时获取3个锁...");
        boolean locked = multiLock.tryLock(3, 10, TimeUnit.SECONDS);
        if (locked) {
            try {
                System.out.println("  成功获取所有锁，执行业务逻辑...");
            } finally {
                multiLock.unlock();
                System.out.println("  释放所有锁");
            }
        } else {
            System.out.println("  获取锁失败");
        }

        System.out.println();
    }
}
