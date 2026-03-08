package cn.itzixiao.interview.redis.advanced;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式缓存实现详解
 *
 * 缓存策略：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. Cache Aside (旁路缓存) - 最常用                          │
 * │     - 读：先读缓存，未命中读数据库，再写入缓存                │
 * │     - 写：先更新数据库，再删除缓存                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. Read/Write Through (读写穿透)                            │
 * │     - 读写都经过缓存，缓存与数据库同步                        │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. Write Behind (异步回写)                                  │
 * │     - 先写缓存，异步批量写入数据库                            │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 缓存更新策略：
 * - 过期时间 (TTL)：设置合理的过期时间
 * - 主动更新：数据变更时更新缓存
 * - 延迟双删：删除缓存 → 更新数据库 → 延迟删除缓存
 */
public class RedisDistributedCacheDemo {

    private StringRedisTemplate redisTemplate;

    /**
     * Cache Aside 模式 - 查询数据
     */
    public String getUserById(String userId) {
        String cacheKey = "user:" + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 1. 先查缓存
        String userJson = ops.get(cacheKey);
        if (userJson != null) {
            System.out.println("【缓存命中】从 Redis 获取用户: " + userId);
            return userJson;
        }

        // 2. 缓存未命中，查数据库（模拟）
        System.out.println("【缓存未命中】从数据库查询用户: " + userId);
        userJson = queryUserFromDB(userId);

        // 3. 写入缓存，设置过期时间
        if (userJson != null) {
            ops.set(cacheKey, userJson, 30, TimeUnit.MINUTES);
            System.out.println("【写入缓存】用户数据已缓存，TTL=30分钟");
        }

        return userJson;
    }

    /**
     * Cache Aside 模式 - 更新数据（先更新数据库，再删缓存）
     */
    public void updateUser(String userId, String userJson) {
        String cacheKey = "user:" + userId;

        // 1. 先更新数据库
        System.out.println("【更新数据库】用户: " + userId);
        updateUserInDB(userId, userJson);

        // 2. 再删除缓存（不是更新缓存，避免并发问题）
        redisTemplate.delete(cacheKey);
        System.out.println("【删除缓存】清除用户缓存: " + cacheKey);
    }

    /**
     * 延迟双删策略 - 解决数据库与缓存不一致
     */
    public void updateUserWithDoubleDelete(String userId, String userJson) throws InterruptedException {
        String cacheKey = "user:" + userId;

        // 1. 先删除缓存
        redisTemplate.delete(cacheKey);
        System.out.println("【延迟双删】第一次删除缓存");

        // 2. 更新数据库
        updateUserInDB(userId, userJson);

        // 3. 延迟一段时间（确保数据库主从同步完成）
        Thread.sleep(500);

        // 4. 再次删除缓存
        redisTemplate.delete(cacheKey);
        System.out.println("【延迟双删】第二次删除缓存");
    }

    /**
     * 缓存空值 - 解决缓存穿透
     */
    public String getUserWithNullCache(String userId) {
        String cacheKey = "user:" + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        String userJson = ops.get(cacheKey);

        // 判断是否是空值缓存
        if ("NULL".equals(userJson)) {
            System.out.println("【空值缓存】用户不存在，直接返回null");
            return null;
        }

        if (userJson != null) {
            return userJson;
        }

        // 查数据库
        userJson = queryUserFromDB(userId);

        if (userJson != null) {
            ops.set(cacheKey, userJson, 30, TimeUnit.MINUTES);
        } else {
            // 缓存空值，设置较短的过期时间
            ops.set(cacheKey, "NULL", 5, TimeUnit.MINUTES);
            System.out.println("【空值缓存】用户不存在，缓存空值5分钟");
        }

        return userJson;
    }

    /**
     * 互斥锁 - 解决缓存击穿
     */
    public String getUserWithMutex(String userId) {
        String cacheKey = "user:" + userId;
        String lockKey = "lock:user:" + userId;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 1. 先查缓存
        String userJson = ops.get(cacheKey);
        if (userJson != null) {
            return userJson;
        }

        // 2. 缓存未命中，尝试获取锁
        Boolean locked = ops.setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查
                userJson = ops.get(cacheKey);
                if (userJson != null) {
                    return userJson;
                }

                // 查询数据库
                userJson = queryUserFromDB(userId);
                if (userJson != null) {
                    ops.set(cacheKey, userJson, 30, TimeUnit.MINUTES);
                }
            } finally {
                // 释放锁
                redisTemplate.delete(lockKey);
            }
        } else {
            // 获取锁失败，短暂等待后重试
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUserWithMutex(userId);
        }

        return userJson;
    }

    /**
     * 多级缓存 - 本地缓存 + Redis
     */
    private final com.google.common.cache.Cache<String, String> localCache =
            com.google.common.cache.CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(1, TimeUnit.MINUTES)
                    .build();

    public String getUserWithMultiLevelCache(String userId) {
        String cacheKey = "user:" + userId;

        // 1. 查本地缓存
        String userJson = localCache.getIfPresent(cacheKey);
        if (userJson != null) {
            System.out.println("【本地缓存命中】");
            return userJson;
        }

        // 2. 查 Redis
        ValueOperations<String, String> ops = redisTemplate.opsForValue();
        userJson = ops.get(cacheKey);
        if (userJson != null) {
            System.out.println("【Redis缓存命中】");
            localCache.put(cacheKey, userJson);
            return userJson;
        }

        // 3. 查数据库
        System.out.println("【数据库查询】");
        userJson = queryUserFromDB(userId);
        if (userJson != null) {
            ops.set(cacheKey, userJson, 30, TimeUnit.MINUTES);
            localCache.put(cacheKey, userJson);
        }

        return userJson;
    }

    // 模拟数据库操作
    private String queryUserFromDB(String userId) {
        return "{\"id\":\"" + userId + "\",\"name\":\"User" + userId + "\"}";
    }

    private void updateUserInDB(String userId, String userJson) {
        System.out.println("数据库更新用户: " + userId);
    }
}
