package cn.itzixiao.interview.redis;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis 分布式锁详解
 * <p>
 * 分布式锁要求：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 互斥性：同一时刻只有一个客户端能持有锁                     │
 * │  2. 安全性：锁只能被持有者释放，不能误删                       │
 * │  3. 死锁避免：设置过期时间，防止死锁                          │
 * │  4. 容错性：Redis 宕机后，锁能正确释放                        │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * Redis 分布式锁演进：
 * ┌─────────┐ → ┌─────────┐ → ┌─────────┐ → ┌─────────┐
 * │  SETNX  │   │ SET EX  │   │ Lua脚本 │   │ RedLock │
 * │ + EXPIRE│   │  NX     │   │ 原子操作│   │ 多节点  │
 * └─────────┘   └─────────┘   └─────────┘   └─────────┘
 */
public class DistributedLockDemo {

    // 模拟 Redis
    private final ConcurrentHashMap<String, LockValue> redis = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("========== Redis 分布式锁详解 ==========\n");

        DistributedLockDemo demo = new DistributedLockDemo();

        // 1. 分布式锁基础
        demo.demonstrateLockBasics();

        // 2. 锁的演进
        demo.demonstrateLockEvolution();

        // 3. Redisson 锁原理
        demo.demonstrateRedissonLock();

        // 4. 锁的问题与解决
        demo.demonstrateLockProblems();

        // 5. 分布式锁对比
        demo.demonstrateLockComparison();
    }

    /**
     * 1. 分布式锁基础
     */
    private void demonstrateLockBasics() {
        System.out.println("【1. 分布式锁基础】\n");

        System.out.println("为什么需要分布式锁？");
        System.out.println("- 单体应用：synchronized、ReentrantLock 即可");
        System.out.println("- 分布式系统：多个 JVM 进程，需要跨进程的锁机制\n");

        System.out.println("分布式锁的实现方式：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  数据库                                                       │");
        System.out.println("│  - 唯一索引：插入成功获得锁，删除释放锁                        │");
        System.out.println("│  - 缺点：性能差，无过期机制，不可重入                          │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  Redis                                                        │");
        System.out.println("│  - SET key value NX EX seconds                                │");
        System.out.println("│  - 优点：性能高，支持过期                                     │");
        System.out.println("│  - 缺点：主从切换可能丢锁，不可重入（需Redisson）              │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  ZooKeeper                                                    │");
        System.out.println("│  - 临时顺序节点：创建成功获得锁，断开连接自动释放               │");
        System.out.println("│  - 优点：可靠性高，天然支持可重入、排队等待                    │");
        System.out.println("│  - 缺点：性能略低，部署复杂                                   │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  etcd                                                         │");
        System.out.println("│  - 基于 Raft 的一致性存储                                     │");
        System.out.println("│  - 优点：强一致性，支持租约自动过期                           │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. 锁的演进
     */
    private void demonstrateLockEvolution() {
        System.out.println("【2. Redis 分布式锁的演进】\n");

        System.out.println("阶段1：SETNX + EXPIRE（不推荐）");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  SETNX lock:order:123 true              │");
        System.out.println("│  if (result == 1) {                     │");
        System.out.println("│      EXPIRE lock:order:123 30           │");
        System.out.println("│      // 执行业务逻辑                     │");
        System.out.println("│      DEL lock:order:123                 │");
        System.out.println("│  }                                      │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("问题：SETNX 和 EXPIRE 不是原子操作");
        System.out.println("- SETNX 成功，但 EXPIRE 执行前进程崩溃 → 死锁\n");

        System.out.println("阶段2：SET key value NX EX（Redis 2.6.12+）");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  SET lock:order:123 randomValue NX EX 30│");
        System.out.println("│  // 原子操作：设置成功返回OK            │");
        System.out.println("│  // 设置失败返回nil                     │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("优点：原子操作，避免死锁\n");

        System.out.println("阶段3：Lua 脚本保证释放锁原子性");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  // 释放锁时，先判断再删除               │");
        System.out.println("│  if redis.call('get', KEYS[1]) == ARGV[1]│");
        System.out.println("│  then                                     │");
        System.out.println("│      return redis.call('del', KEYS[1])   │");
        System.out.println("│  else                                     │");
        System.out.println("│      return 0                             │");
        System.out.println("│  end                                      │");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("作用：防止误删其他线程的锁\n");

        System.out.println("阶段4：RedLock 算法（Redis 作者提出）");
        System.out.println("- 在 N 个独立的 Redis 节点上获取锁");
        System.out.println("- 当在大多数节点（N/2+1）上获取成功，且耗时小于锁过期时间，才认为获取成功");
        System.out.println("- 释放时向所有节点发送释放命令\n");
    }

    /**
     * 3. Redisson 锁原理
     */
    private void demonstrateRedissonLock() {
        System.out.println("【3. Redisson 分布式锁原理】\n");

        System.out.println("Redisson 是什么？");
        System.out.println("- Redis 的 Java 客户端，提供了丰富的分布式功能");
        System.out.println("- 分布式锁是其核心功能之一\n");

        System.out.println("Redisson 锁的特点：");
        System.out.println("1. 可重入：同一线程可以多次获取锁");
        System.out.println("2. 看门狗：自动续期，防止业务执行中锁过期");
        System.out.println("3. 阻塞等待：支持 tryLock 超时等待");
        System.out.println("4. 公平锁：按请求顺序获取锁\n");

        System.out.println("看门狗机制（Watch Dog）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  获取锁成功                                                  │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  启动看门狗线程（守护线程）                                   │");
        System.out.println("│       │                                                     │");
        System.out.println("│       ↓ 每 10 秒检查（锁过期时间的 1/3）                      │");
        System.out.println("│  业务是否完成？                                              │");
        System.out.println("│  ├─ 否 → 续期锁（重置过期时间为 30 秒）                       │");
        System.out.println("│  └─ 是 → 停止看门狗，释放锁                                  │");
        System.out.println("│                                                             │");
        System.out.println("│  注意：只有没有指定过期时间时才会启动看门狗                   │");
        System.out.println("│        lock.lock();  // 看门狗续期                          │");
        System.out.println("│        lock.lock(10, TimeUnit.SECONDS);  // 不会续期         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Redisson 可重入实现：");
        System.out.println("- 使用 Hash 结构存储锁：hset lock_name threadId count");
        System.out.println("- 重入时 count + 1，释放时 count - 1");
        System.out.println("- count = 0 时删除锁\n");

        System.out.println("Redisson 使用示例：");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  RLock lock = redisson.getLock(\"myLock\");│");
        System.out.println("│  try {                                  │");
        System.out.println("│      // 尝试获取锁，最多等待10秒         │");
        System.out.println("│      // 锁30秒后自动释放                 │");
        System.out.println("│      boolean isLock = lock.tryLock(10, 30, TimeUnit.SECONDS);│");
        System.out.println("│      if (isLock) {                      │");
        System.out.println("│          // 执行业务逻辑                 │");
        System.out.println("│      }                                  │");
        System.out.println("│  } finally {                            │");
        System.out.println("│      lock.unlock();                     │");
        System.out.println("│  }                                      │");
        System.out.println("└─────────────────────────────────────────┘\n");
    }

    /**
     * 4. 锁的问题与解决
     */
    private void demonstrateLockProblems() {
        System.out.println("【4. 分布式锁的问题与解决】\n");

        System.out.println("问题1：锁过期时间设置多长？");
        System.out.println("- 太短：业务没执行完，锁就释放了");
        System.out.println("- 太长：如果进程崩溃，其他线程要等很久");
        System.out.println("解决：使用看门狗自动续期\n");

        System.out.println("问题2：主从切换导致锁丢失");
        System.out.println("场景：");
        System.out.println("1. 客户端A在 Master 获取锁");
        System.out.println("2. Master 宕机，锁还没同步到 Slave");
        System.out.println("3. Slave 升级为 Master");
        System.out.println("4. 客户端B在新 Master 也能获取锁");
        System.out.println("结果：两个客户端同时持有锁\n");

        System.out.println("解决方案：");
        System.out.println("1. RedLock 算法：多节点部署");
        System.out.println("2. ZooKeeper/etcd：CP 系统，保证一致性");
        System.out.println("3. 业务幂等：锁只是兜底，业务层也要幂等\n");

        System.out.println("问题3：锁的粒度");
        System.out.println("- 粗粒度：lock:order 影响所有订单");
        System.out.println("- 细粒度：lock:order:123 只影响订单123");
        System.out.println("建议：锁的粒度尽可能细，提高并发度\n");

        System.out.println("问题4：锁的性能");
        System.out.println("- 获取锁需要网络请求（RTT）");
        System.out.println("- 高并发场景可能成为瓶颈");
        System.out.println("优化：");
        System.out.println("- 本地缓存 + 分布式锁（减少锁竞争）");
        System.out.println("- 分段锁（类似 ConcurrentHashMap）\n");
    }

    /**
     * 5. 分布式锁对比
     */
    private void demonstrateLockComparison() {
        System.out.println("【5. 分布式锁实现对比】\n");

        System.out.println("┌───────────┬───────────┬───────────┬───────────┬───────────┐");
        System.out.println("│   特性    │   MySQL   │   Redis   │ ZooKeeper │   etcd    │");
        System.out.println("├───────────┼───────────┼───────────┼───────────┼───────────┤");
        System.out.println("│  性能     │    低     │    高     │   中      │    高     │");
        System.out.println("│  可靠性   │    中     │    中     │   高      │    高     │");
        System.out.println("│  实现难度 │    低     │    低     │   中      │    中     │");
        System.out.println("│  可重入   │   不支持  │  需实现   │   支持    │   需实现  │");
        System.out.println("│  阻塞等待 │   不支持  │  需实现   │   支持    │   需实现  │");
        System.out.println("│  自动续期 │   不支持  │  需实现   │   支持    │   支持    │");
        System.out.println("│  主从切换 │    无     │  可能丢锁 │   无      │    无     │");
        System.out.println("└───────────┴───────────┴───────────┴───────────┴───────────┘\n");

        System.out.println("选型建议：");
        System.out.println("- 追求性能，允许偶尔异常：Redis + Redisson");
        System.out.println("- 追求可靠性，性能要求不高：ZooKeeper");
        System.out.println("- 云原生环境：etcd");
        System.out.println("- 简单场景，已有 MySQL：数据库唯一索引\n");

        System.out.println("Redis 分布式锁最佳实践：");
        System.out.println("1. 使用 SET key value NX EX 原子命令");
        System.out.println("2. value 使用唯一标识（UUID + 线程ID），防止误删");
        System.out.println("3. 释放锁使用 Lua 脚本，保证原子性");
        System.out.println("4. 使用 Redisson 处理可重入、看门狗续期");
        System.out.println("5. 锁粒度要细，减少竞争");
        System.out.println("6. 业务层做好幂等，锁只是辅助\n");
    }

    // ==================== 锁实现演示 ====================

    /**
     * 简单的 Redis 锁实现（演示用）
     */
    static class SimpleRedisLock {
        private final ConcurrentHashMap<String, LockValue> redis;

        public SimpleRedisLock(ConcurrentHashMap<String, LockValue> redis) {
            this.redis = redis;
        }

        /**
         * 获取锁
         */
        public boolean tryLock(String lockKey, String requestId, int expireSeconds) {
            LockValue lockValue = new LockValue(requestId, System.currentTimeMillis() + expireSeconds * 1000);
            // SETNX 语义：如果不存在则设置
            LockValue existing = redis.putIfAbsent(lockKey, lockValue);
            if (existing == null) {
                return true; // 获取锁成功
            }
            // 检查是否过期（简单实现，实际用 Redis 的过期机制）
            if (existing.expireTime < System.currentTimeMillis()) {
                LockValue old = redis.put(lockKey, lockValue);
                return old == existing || old == null;
            }
            return false; // 锁被占用
        }

        /**
         * 释放锁（使用 Lua 脚本思想）
         */
        public boolean unlock(String lockKey, String requestId) {
            LockValue lockValue = redis.get(lockKey);
            if (lockValue != null && lockValue.requestId.equals(requestId)) {
                redis.remove(lockKey);
                return true;
            }
            return false; // 不是自己的锁，不能释放
        }
    }

    static class LockValue {
        String requestId;
        long expireTime;

        public LockValue(String requestId, long expireTime) {
            this.requestId = requestId;
            this.expireTime = expireTime;
        }
    }
}
