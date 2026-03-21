package cn.itzixiao.interview.cache.memcached.lock;

import cn.itzixiao.interview.cache.memcached.service.MemcachedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Memcached 的分布式锁实现
 * 
 * <h3>实现原理：</h3>
 * <ol>
 *   <li>使用 add 操作实现加锁（原子性保证）</li>
 *   <li>使用 delete 操作释放锁</li>
 *   <li>锁持有者标识防止误删</li>
 * </ol>
 * 
 * <h3>注意事项：</h3>
 * <ul>
 *   <li>Memcached 不支持 Redis 的过期续期机制</li>
 *   <li>需要合理设置锁的过期时间</li>
 *   <li>不支持可重入锁</li>
 *   <li>释放锁时 get + delete 不是原子操作，存在极小概率误删</li>
 * </ul>
 * 
 * <h3>使用示例：</h3>
 * <pre>
 * {@code
 * // 方式1：手动加锁/解锁
 * String lockValue = distributedLock.tryLock("order:123");
 * if (lockValue != null) {
 *     try {
 *         // 执行业务逻辑
 *         processOrder();
 *     } finally {
 *         distributedLock.unlock("order:123", lockValue);
 *     }
 * }
 * 
 * // 方式2：自动管理锁
 * Result result = distributedLock.executeWithLock("order:123", 30, () -> {
 *     return processOrder();
 * });
 * }
 * </pre>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "memcached.enabled", havingValue = "true")
public class MemcachedDistributedLock {

    private final MemcachedService memcachedService;
    
    /**
     * 锁的默认过期时间：30秒
     * 防止业务执行时间过长导致锁提前过期
     */
    private static final int DEFAULT_LOCK_EXPIRE = 30;
    
    /**
     * 获取锁的重试间隔：100毫秒
     */
    private static final long RETRY_INTERVAL_MS = 100;

    public MemcachedDistributedLock(MemcachedService memcachedService) {
        this.memcachedService = memcachedService;
    }

    // ==================== 加锁操作 ====================

    /**
     * 尝试获取分布式锁
     * 
     * <p>使用 add 操作实现，仅当 key 不存在时才能添加成功</p>
     * <p>这是原子操作，不需要额外的同步机制</p>
     * 
     * @param lockKey      锁的 key，建议使用业务前缀，如 "lock:order:123"
     * @param expireSeconds 锁的过期时间（秒）
     * @return 锁持有者标识，用于释放锁；获取失败返回 null
     */
    public String tryLock(String lockKey, int expireSeconds) {
        // 生成唯一的锁持有者标识
        // 用于防止误删其他线程持有的锁
        // UUID 保证全局唯一
        String lockValue = UUID.randomUUID().toString();
        
        // add 操作是原子的，只有 key 不存在时才会成功
        // 这比 Redis 的 SET NX EX 更简单直接
        boolean success = memcachedService.add(lockKey, lockValue, expireSeconds);
        
        if (success) {
            log.debug("Successfully acquired lock, key: {}, value: {}", 
                lockKey, lockValue);
            return lockValue;
        }
        
        log.debug("Failed to acquire lock, key: {}", lockKey);
        return null;
    }

    /**
     * 尝试获取锁，使用默认过期时间
     * 
     * @param lockKey 锁的 key
     * @return 锁持有者标识，获取失败返回 null
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_EXPIRE);
    }

    /**
     * 阻塞式获取锁
     * 
     * <p>在指定时间内不断重试，直到获取锁或超时</p>
     * <p>适用于必须获取锁才能执行的场景</p>
     * 
     * @param lockKey      锁的 key
     * @param expireSeconds 锁的过期时间（秒）
     * @param waitTime     最大等待时间
     * @param timeUnit     时间单位
     * @return 锁持有者标识，超时返回 null
     */
    public String lock(String lockKey, int expireSeconds, 
            long waitTime, TimeUnit timeUnit) {
        
        long startTime = System.currentTimeMillis();
        long waitMillis = timeUnit.toMillis(waitTime);
        
        while (true) {
            // 尝试获取锁
            String lockValue = tryLock(lockKey, expireSeconds);
            if (lockValue != null) {
                return lockValue;
            }
            
            // 检查是否超时
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= waitMillis) {
                log.warn("Lock acquisition timeout, key: {}, waitTime: {}ms", 
                    lockKey, waitMillis);
                return null;
            }
            
            // 等待一段时间后重试
            // 使用指数退避可以减少竞争，但这里使用固定间隔简化实现
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Lock acquisition interrupted, key: {}", lockKey, e);
                return null;
            }
        }
    }

    /**
     * 阻塞式获取锁，使用默认过期时间
     * 
     * @param lockKey  锁的 key
     * @param waitTime 最大等待时间
     * @param timeUnit 时间单位
     * @return 锁持有者标识
     */
    public String lock(String lockKey, long waitTime, TimeUnit timeUnit) {
        return lock(lockKey, DEFAULT_LOCK_EXPIRE, waitTime, timeUnit);
    }

    // ==================== 解锁操作 ====================

    /**
     * 释放锁
     * 
     * <p>需要验证锁持有者标识，防止误删其他线程的锁</p>
     * 
     * <h3>注意：</h3>
     * <p>这里的 get 和 delete 不是原子操作</p>
     * <p>在高并发场景下可能出现问题：</p>
     * <ol>
     *   <li>线程 A 获取锁，执行业务</li>
     *   <li>锁过期，线程 B 获取锁</li>
     *   <li>线程 A 执行 get 获取到 B 的锁值（此时 A 的锁已过期）</li>
     *   <li>线程 A 判断值不匹配，不会误删</li>
     * </ol>
     * <p>但如果 A 的锁刚好过期，B 还没获取到锁，此时 A 执行 get 返回 null</p>
     * <p>这种情况下 A 不会误删，所以总体是安全的</p>
     * 
     * @param lockKey  锁的 key
     * @param lockValue 锁持有者标识（获取锁时返回的值）
     * @return 是否成功释放
     */
    public boolean unlock(String lockKey, String lockValue) {
        if (lockValue == null) {
            log.warn("Lock value is null, cannot unlock, key: {}", lockKey);
            return false;
        }
        
        // 先获取当前锁的值，验证是否是自己持有的锁
        String currentValue = memcachedService.get(lockKey);
        
        if (lockValue.equals(currentValue)) {
            // 是自己持有的锁，可以删除
            boolean deleted = memcachedService.delete(lockKey);
            if (deleted) {
                log.debug("Successfully released lock, key: {}", lockKey);
            } else {
                log.warn("Failed to delete lock, key: {}", lockKey);
            }
            return deleted;
        }
        
        // 锁已被其他线程持有或已过期
        log.warn("Lock not owned by current thread, key: {}, expected: {}, actual: {}", 
            lockKey, lockValue, currentValue);
        return false;
    }

    // ==================== 便捷方法 ====================

    /**
     * 使用分布式锁执行任务（无返回值）
     * 
     * <p>自动管理锁的获取和释放</p>
     * 
     * @param lockKey  锁的 key
     * @param task     要执行的任务
     * @return 是否成功获取锁并执行任务
     */
    public boolean executeWithLock(String lockKey, Runnable task) {
        String lockValue = tryLock(lockKey);
        if (lockValue == null) {
            log.warn("Failed to acquire lock for task, key: {}", lockKey);
            return false;
        }
        
        try {
            task.run();
            return true;
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 使用分布式锁执行任务（有返回值）
     * 
     * <p>自动管理锁的获取和释放</p>
     * 
     * @param lockKey  锁的 key
     * @param task     要执行的任务
     * @param <T>      返回值类型
     * @return 任务执行结果，获取锁失败返回 null
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> task) {
        String lockValue = tryLock(lockKey);
        if (lockValue == null) {
            log.warn("Failed to acquire lock for task, key: {}", lockKey);
            return null;
        }
        
        try {
            return task.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 使用分布式锁执行任务（带超时）
     * 
     * @param lockKey      锁的 key
     * @param expireSeconds 锁的过期时间
     * @param waitTime     最大等待时间
     * @param timeUnit     时间单位
     * @param task         要执行的任务
     * @param <T>          返回值类型
     * @return 任务执行结果，获取锁失败返回 null
     */
    public <T> T executeWithLock(String lockKey, int expireSeconds,
            long waitTime, TimeUnit timeUnit, Supplier<T> task) {
        
        String lockValue = lock(lockKey, expireSeconds, waitTime, timeUnit);
        if (lockValue == null) {
            log.warn("Failed to acquire lock within timeout, key: {}", lockKey);
            return null;
        }
        
        try {
            return task.get();
        } finally {
            unlock(lockKey, lockValue);
        }
    }

    /**
     * 检查锁是否被持有
     * 
     * @param lockKey 锁的 key
     * @return true 表示锁被持有，false 表示锁未被持有
     */
    public boolean isLocked(String lockKey) {
        return memcachedService.get(lockKey) != null;
    }
}
