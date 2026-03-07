package cn.itzixiao.interview.redis.advanced;

import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 与 Redisson 分布式锁详解
 *
 * 本示例对比展示了 Redis 原生分布式锁和 Redisson 分布式锁的实现方式
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        Redis 分布式锁演进                                    │
 * │                                                                             │
 * │   方式1：Redis 原生命令（SET NX EX + Lua 脚本）                              │
 * │   - 优点：简单、不依赖额外框架                                               │
 * │   - 缺点：不可重入、无自动续期、需要自己实现                                 │
 * │                                                                             │
 * │   方式2：Redisson 框架                                                       │
 * │   - 优点：可重入、自动续期、多种锁类型、API 简洁                             │
 * │   - 缺点：引入额外依赖                                                       │
 * │                                                                             │
 * │   Redisson 锁类型：                                                          │
 * │   ┌───────────────────────────────────────────────────────────────────────┐│
 * │   │  1. 可重入锁（Reentrant Lock）                                          ││
 * │   │     - 同一线程可以多次获取锁                                           ││
 * │   │     - 支持看门狗自动续期                                               ││
 * │   │                                                                       ││
 * │   │  2. 公平锁（Fair Lock）                                                ││
 * │   │     - 按请求锁的顺序获取锁                                             ││
 * │   │     - 防止饥饿                                                         ││
 * │   │                                                                       ││
 * │   │  3. 联锁（MultiLock）                                                  ││
 * │   │     - 同时获取多个锁，防止死锁                                         ││
 * │   │                                                                       ││
 * │   │  4. 红锁（RedLock）                                                    ││
 * │   │     - 在多个 Redis 节点上加锁                                          ││
 * │   │     - 提高可靠性                                                       ││
 * │   │                                                                       ││
 * │   │  5. 读写锁（ReadWriteLock）                                            ││
 * │   │     - 读读共享，读写互斥，写写互斥                                     ││
 * │   │                                                                       ││
 * │   │  6. 信号量（Semaphore）                                                ││
 * │   │     - 控制同时访问的线程数量                                           ││
 * │   │                                                                       ││
 * │   │  7. 闭锁（CountDownLatch）                                             ││
 * │   │     - 等待多个线程完成                                                 ││
 * │   └───────────────────────────────────────────────────────────────────────┘│
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
public class RedissonDistributedLockDemo {

    private static RedissonClient redisson;

    public static void main(String[] args) throws Exception {
        System.out.println("========== Redis 与 Redisson 分布式锁详解 ==========\n");

        // 初始化 Redisson
        initRedisson();

        // ========== 第一部分：Redis 原生分布式锁 ==========
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          第一部分：Redis 原生分布式锁                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // 1. Redis 原生分布式锁原理
        demonstrateRedisNativeLock();

        // 2. Redis 原生分布式锁实现（模拟 Jedis）
        demonstrateRedisNativeLockImplementation();

        // 3. Redis 原生分布式锁的问题
        demonstrateRedisNativeLockProblems();

        // ========== 第二部分：Redisson 分布式锁 ==========
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          第二部分：Redisson 分布式锁                        ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // 4. 可重入锁
        demonstrateReentrantLock();

        // 5. 公平锁
        demonstrateFairLock();

        // 6. 读写锁
        demonstrateReadWriteLock();

        // 7. 信号量
        demonstrateSemaphore();

        // 8. 闭锁
        demonstrateCountDownLatch();

        // 9. 联锁
        demonstrateMultiLock();

        // 10. 红锁
        demonstrateRedLock();

        // ========== 第三部分：对比与最佳实践 ==========
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          第三部分：对比与最佳实践                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // 11. Redis vs Redisson 对比
        demonstrateLockComparison();

        // 12. 最佳实践
        demonstrateBestPractices();

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

    // ==================== 第一部分：Redis 原生分布式锁 ====================

    /**
     * 1. Redis 原生分布式锁原理
     */
    private static void demonstrateRedisNativeLock() {
        System.out.println("【1. Redis 原生分布式锁原理】\n");

        System.out.println("分布式锁的核心要求：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 互斥性：同一时刻只有一个客户端能持有锁                           │");
        System.out.println("│  2. 安全性：锁只能被持有者释放，不能误删                             │");
        System.out.println("│  3. 死锁避免：设置过期时间，防止死锁                                 │");
        System.out.println("│  4. 容错性：Redis 宕机后，锁能正确释放                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("Redis 分布式锁演进过程：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  阶段1：SETNX + EXPIRE（不推荐）                                    │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  SETNX lock:order:123 true                                          │");
        System.out.println("│  EXPIRE lock:order:123 30                                           │");
        System.out.println("│  问题：SETNX 和 EXPIRE 不是原子操作                                 │");
        System.out.println("│        SETNX 成功，但 EXPIRE 执行前进程崩溃 → 死锁                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  阶段2：SET key value NX EX（Redis 2.6.12+）                        │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  SET lock:order:123 randomValue NX EX 30                            │");
        System.out.println("│  优点：原子操作，避免死锁                                           │");
        System.out.println("│                                                                     │");
        System.out.println("│  阶段3：Lua 脚本保证释放锁原子性                                    │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  // 释放锁时，先判断再删除                                          │");
        System.out.println("│  if redis.call('get', KEYS[1]) == ARGV[1] then                      │");
        System.out.println("│      return redis.call('del', KEYS[1])                              │");
        System.out.println("│  else                                                               │");
        System.out.println("│      return 0                                                       │");
        System.out.println("│  end                                                                │");
        System.out.println("│  作用：防止误删其他线程的锁                                         │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. Redis 原生分布式锁实现（模拟 Jedis）
     */
    private static void demonstrateRedisNativeLockImplementation() {
        System.out.println("【2. Redis 原生分布式锁实现】\n");

        System.out.println("完整的 Redis 原生分布式锁实现代码：\n");

        System.out.println("// ============ 加锁代码 ============");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  /**                                                                │");
        System.out.println("│   * 加锁（使用 SET NX EX 原子命令）                                 │");
        System.out.println("│   */                                                                │");
        System.out.println("│  public boolean tryLock(String lockKey, String requestId,           │");
        System.out.println("│                         int expireTime, TimeUnit unit) {            │");
        System.out.println("│      // SET key value NX EX seconds                                 │");
        System.out.println("│      // NX: 不存在才设置                                            │");
        System.out.println("│      // EX: 设置过期时间（秒）                                      │");
        System.out.println("│      String result = jedis.set(lockKey, requestId,                  │");
        System.out.println("│                              SetParams.setParams().nx().ex(expireTime));│");
        System.out.println("│      return \"OK\".equals(result);                                   │");
        System.out.println("│  }                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("// ============ 解锁代码（Lua 脚本）============");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  /**                                                                │");
        System.out.println("│   * 解锁（使用 Lua 脚本保证原子性）                                 │");
        System.out.println("│   */                                                                │");
        System.out.println("│  public boolean unlock(String lockKey, String requestId) {          │");
        System.out.println("│      String script =                                                │");
        System.out.println("│          \"if redis.call('get', KEYS[1]) == ARGV[1] then \" +       │");
        System.out.println("│          \"    return redis.call('del', KEYS[1]) \" +                │");
        System.out.println("│          \"else \" +                                                │");
        System.out.println("│          \"    return 0 \" +                                        │");
        System.out.println("│          \"end\";                                                   │");
        System.out.println("│                                                                     │");
        System.out.println("│      Object result = jedis.eval(script,                             │");
        System.out.println("│          Collections.singletonList(lockKey),                        │");
        System.out.println("│          Collections.singletonList(requestId));                     │");
        System.out.println("│      return Long.valueOf(1).equals(result);                         │");
        System.out.println("│  }                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("// ============ 完整使用示例 ============");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  String lockKey = \"lock:order:\" + orderId;                         │");
        System.out.println("│  String requestId = UUID.randomUUID().toString();                   │");
        System.out.println("│                                                                     │");
        System.out.println("│  try {                                                              │");
        System.out.println("│      // 尝试获取锁                                                  │");
        System.out.println("│      if (tryLock(lockKey, requestId, 30, TimeUnit.SECONDS)) {       │");
        System.out.println("│          // 执行业务逻辑                                            │");
        System.out.println("│          processOrder(orderId);                                     │");
        System.out.println("│      } else {                                                       │");
        System.out.println("│          // 获取锁失败                                              │");
        System.out.println("│          throw new BusinessException(\"系统繁忙，请稍后重试\");     │");
        System.out.println("│      }                                                              │");
        System.out.println("│  } finally {                                                        │");
        System.out.println("│      // 释放锁                                                      │");
        System.out.println("│      unlock(lockKey, requestId);                                    │");
        System.out.println("│  }                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("关键点说明：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. requestId 使用 UUID，标识锁的持有者                             │");
        System.out.println("│  2. 加锁使用 SET NX EX 原子命令                                     │");
        System.out.println("│  3. 解锁使用 Lua 脚本，保证「判断+删除」原子性                      │");
        System.out.println("│  4. finally 中确保释放锁                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 3. Redis 原生分布式锁的问题
     */
    private static void demonstrateRedisNativeLockProblems() {
        System.out.println("【3. Redis 原生分布式锁的问题】\n");

        System.out.println("问题1：不可重入");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  场景：                                                              │");
        System.out.println("│  public void methodA() {                                            │");
        System.out.println("│      lock.lock();                                                   │");
        System.out.println("│      try {                                                          │");
        System.out.println("│          methodB();  // 调用 methodB，也需要同一把锁                │");
        System.out.println("│      } finally {                                                    │");
        System.out.println("│          lock.unlock();                                             │");
        System.out.println("│      }                                                              │");
        System.out.println("│  }                                                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  public void methodB() {                                            │");
        System.out.println("│      lock.lock();   // 同一线程再次获取锁 → 死锁！                  │");
        System.out.println("│      // ...                                                         │");
        System.out.println("│  }                                                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  解决：Redisson 使用 Hash 结构存储锁，支持重入计数                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("问题2：锁过期时间难以设置");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  场景：                                                              │");
        System.out.println("│  - 设置 10 秒过期，但业务执行需要 15 秒 → 锁提前释放               │");
        System.out.println("│  - 设置 60 秒过期，但业务执行只需要 1 秒 → 意外崩溃时等待太久       │");
        System.out.println("│                                                                     │");
        System.out.println("│  解决：Redisson 看门狗机制自动续期                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("问题3：主从切换导致锁丢失");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  场景：                                                              │");
        System.out.println("│  1. 客户端 A 在 Master 获取锁                                       │");
        System.out.println("│  2. Master 宕机，锁还没同步到 Slave                                 │");
        System.out.println("│  3. Slave 升级为 Master                                             │");
        System.out.println("│  4. 客户端 B 在新 Master 也能获取锁                                 │");
        System.out.println("│  结果：两个客户端同时持有锁                                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  解决：Redisson RedLock 或使用 ZooKeeper                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("问题4：无法实现阻塞等待");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  原生实现：                                                          │");
        System.out.println("│  - 需要自己实现自旋等待                                             │");
        System.out.println("│  - 需要处理超时                                                     │");
        System.out.println("│  - 需要处理中断                                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  解决：Redisson 内置阻塞等待支持                                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 模拟 Redis 原生锁实现（用于演示）====================

    /**
     * 模拟 Redis 原生分布式锁
     */
    static class SimpleRedisDistributedLock {
        private final ConcurrentHashMap<String, LockInfo> lockStore;

        public SimpleRedisDistributedLock(ConcurrentHashMap<String, LockInfo> lockStore) {
            this.lockStore = lockStore;
        }

        /**
         * 加锁（模拟 SET NX EX）
         */
        public boolean tryLock(String lockKey, String requestId, int expireSeconds) {
            long now = System.currentTimeMillis();
            long expireTime = now + expireSeconds * 1000;

            LockInfo newLock = new LockInfo(requestId, expireTime);
            LockInfo existing = lockStore.putIfAbsent(lockKey, newLock);

            if (existing == null) {
                return true; // 加锁成功
            }

            // 检查是否过期
            if (existing.expireTime < now) {
                // 锁已过期，尝试获取
                if (lockStore.replace(lockKey, existing, newLock)) {
                    return true;
                }
            }

            return false; // 加锁失败
        }

        /**
         * 解锁（模拟 Lua 脚本）
         */
        public boolean unlock(String lockKey, String requestId) {
            LockInfo lockInfo = lockStore.get(lockKey);
            if (lockInfo != null && lockInfo.requestId.equals(requestId)) {
                lockStore.remove(lockKey);
                return true;
            }
            return false; // 不是自己的锁，不能释放
        }

        static class LockInfo {
            String requestId;
            long expireTime;

            LockInfo(String requestId, long expireTime) {
                this.requestId = requestId;
                this.expireTime = expireTime;
            }
        }
    }

    // ==================== 第二部分：Redisson 分布式锁 ====================

    /**
     * 4. 可重入锁
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
     * 5. 公平锁
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
     * 6. 读写锁
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
     * 7. 信号量
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
     * 8. 闭锁
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
     * 9. 联锁
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

    /**
     * 10. 红锁（RedLock）
     */
    private static void demonstrateRedLock() throws InterruptedException {
        System.out.println("【7. 红锁（RedLock）】\n");

        System.out.println("什么是 RedLock？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  RedLock 是 Redis 作者 Antirez 提出的分布式锁算法                   │");
        System.out.println("│  用于解决单节点 Redis 主从切换导致锁丢失的问题                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  原理：                                                              │");
        System.out.println("│  1. 获取当前时间戳                                                   │");
        System.out.println("│  2. 依次向 N 个 Redis 节点请求加锁                                   │");
        System.out.println("│  3. 计算获取锁成功消耗的时间                                         │");
        System.out.println("│  4. 如果在多数节点（N/2+1）获取成功，且消耗时间 < 锁过期时间，则成功 │");
        System.out.println("│  5. 如果失败，向所有节点发送解锁请求                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("Redisson RedLock 使用示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 创建多个 RedissonClient（连接不同的 Redis 节点）                 │");
        System.out.println("│  RedissonClient client1 = Redisson.create(config1);                 │");
        System.out.println("│  RedissonClient client2 = Redisson.create(config2);                 │");
        System.out.println("│  RedissonClient client3 = Redisson.create(config3);                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 获取锁对象                                                       │");
        System.out.println("│  RLock lock1 = client1.getLock(\"myLock\");                          │");
        System.out.println("│  RLock lock2 = client2.getLock(\"myLock\");                          │");
        System.out.println("│  RLock lock3 = client3.getLock(\"myLock\");                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 创建 RedLock                                                    │");
        System.out.println("│  RLock redLock = redisson.getRedLock(lock1, lock2, lock3);          │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 使用                                                            │");
        System.out.println("│  try {                                                              │");
        System.out.println("│      if (redLock.tryLock(10, 30, TimeUnit.SECONDS)) {               │");
        System.out.println("│          // 执行业务                                                │");
        System.out.println("│      }                                                              │");
        System.out.println("│  } finally {                                                        │");
        System.out.println("│      redLock.unlock();                                              │");
        System.out.println("│  }                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("RedLock 的争议：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  反对观点（Martin Kleppmann）：                                     │");
        System.out.println("│  - RedLock 依赖系统时钟，时钟跳跃可能导致问题                       │");
        System.out.println("│  - 网络分区场景下仍可能出现问题                                     │");
        System.out.println("│  - 建议使用 fencing token 方案                                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  建议：                                                              │");
        System.out.println("│  - 对于极高可靠性要求的场景，考虑使用 ZooKeeper 或 etcd             │");
        System.out.println("│  - 一般场景下，单节点 Redis + Redisson 足够使用                     │");
        System.out.println("│  - 业务层做好幂等设计，锁只是辅助                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第三部分：对比与最佳实践 ====================

    /**
     * 11. Redis vs Redisson 对比
     */
    private static void demonstrateLockComparison() {
        System.out.println("【11. Redis 原生锁 vs Redisson 锁对比】\n");

        System.out.println("功能对比：");
        System.out.println("┌────────────────┬─────────────────┬─────────────────┐");
        System.out.println("│      功能       │   Redis 原生    │    Redisson     │");
        System.out.println("├────────────────┼─────────────────┼─────────────────┤");
        System.out.println("│  可重入         │      不支持     │      支持       │");
        System.out.println("│  自动续期       │      不支持     │   看门狗机制   │");
        System.out.println("│  阻塞等待       │   需自己实现   │      支持       │");
        System.out.println("│  公平锁         │      不支持     │      支持       │");
        System.out.println("│  读写锁         │      不支持     │      支持       │");
        System.out.println("│  联锁           │      不支持     │      支持       │");
        System.out.println("│  红锁           │      不支持     │      支持       │");
        System.out.println("│  信号量         │      不支持     │      支持       │");
        System.out.println("│  闭锁           │      不支持     │      支持       │");
        System.out.println("│  API 简洁度     │      较复杂    │      简洁      │");
        System.out.println("│  额外依赖       │        无      │   需引入依赖   │");
        System.out.println("└────────────────┴─────────────────┴─────────────────┘\n");

        System.out.println("使用场景建议：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  使用 Redis 原生锁的场景：                                          │");
        System.out.println("│  - 项目已使用 Jedis/Lettuce，不想引入额外依赖                       │");
        System.out.println("│  - 锁使用场景简单，不需要重入                                       │");
        System.out.println("│  - 对锁的可靠性要求不高                                             │");
        System.out.println("│                                                                     │");
        System.out.println("│  使用 Redisson 的场景：                                             │");
        System.out.println("│  - 需要可重入锁                                                     │");
        System.out.println("│  - 需要自动续期（看门狗）                                           │");
        System.out.println("│  - 需要公平锁、读写锁等高级功能                                     │");
        System.out.println("│  - 需要简洁的 API                                                   │");
        System.out.println("│  - 推荐大多数场景使用                                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 12. 最佳实践
     */
    private static void demonstrateBestPractices() {
        System.out.println("【12. 分布式锁最佳实践】\n");

        System.out.println("1. 锁的粒度要小");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  粗粒度：lock:order        → 锁住所有订单操作                       │");
        System.out.println("│  细粒度：lock:order:12345  → 只锁住订单12345的操作                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  建议：锁的粒度尽可能细，减少竞争，提高并发度                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("2. 锁内业务要短");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  锁内不要执行：                                                     │");
        System.out.println("│  - 网络请求（如调用第三方 API）                                     │");
        System.out.println("│  - 复杂计算                                                        │");
        System.out.println("│  - 文件 I/O                                                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  建议：只包裹必要的临界区代码                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("3. 必须使用 finally 释放锁");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 正确                                                            │");
        System.out.println("│  RLock lock = redisson.getLock(\"myLock\");                          │");
        System.out.println("│  try {                                                              │");
        System.out.println("│      // 业务逻辑                                                   │");
        System.out.println("│  } finally {                                                        │");
        System.out.println("│      lock.unlock();  // 确保释放                                   │");
        System.out.println("│  }                                                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 错误（异常时锁不会释放）                                        │");
        System.out.println("│  RLock lock = redisson.getLock(\"myLock\");                          │");
        System.out.println("│  // 业务逻辑（可能抛异常）                                          │");
        System.out.println("│  lock.unlock();                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("4. 使用看门狗而非手动设置过期时间");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 推荐：使用看门狗自动续期                                        │");
        System.out.println("│  lock.lock();  // 不指定过期时间，启动看门狗                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 不推荐：手动设置过期时间                                        │");
        System.out.println("│  lock.lock(10, TimeUnit.SECONDS);  // 业务超时会丢锁                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("5. 业务层做好幂等设计");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  分布式锁不是万能的：                                               │");
        System.out.println("│  - 主从切换可能丢锁                                                 │");
        System.out.println("│  - 时钟跳跃可能导致问题                                             │");
        System.out.println("│  - 网络分区可能导致多个客户端获取锁                                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  建议：                                                              │");
        System.out.println("│  - 业务层使用唯一 ID 做幂等                                         │");
        System.out.println("│  - 数据库层面使用唯一索引                                           │");
        System.out.println("│  - 分布式锁只是辅助手段                                             │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("6. 监控锁的使用情况");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  需要监控的指标：                                                   │");
        System.out.println("│  - 锁获取失败次数                                                   │");
        System.out.println("│  - 锁持有时间分布                                                   │");
        System.out.println("│  - 等待锁的时间分布                                                 │");
        System.out.println("│  - 死锁告警                                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("========== Redis 与 Redisson 分布式锁详解完成 ==========\n");
    }
}
