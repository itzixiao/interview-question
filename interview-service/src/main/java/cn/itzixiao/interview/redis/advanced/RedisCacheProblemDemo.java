package cn.itzixiao.interview.redis.advanced;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存三大问题详解：穿透、击穿、雪崩
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  缓存穿透 (Cache Penetration)                                │
 * │  - 查询不存在的数据，缓存和数据库都没有                        │
 * │  - 恶意攻击可能导致数据库压力过大                            │
 * │  - 解决：布隆过滤器、缓存空值、参数校验                        │
 * ├─────────────────────────────────────────────────────────────┤
 * │  缓存击穿 (Cache Breakdown)                                  │
 * │  - 热点key过期，大量请求同时打到数据库                        │
 * │  - 解决：互斥锁、逻辑过期、热点key永不过期                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  缓存雪崩 (Cache Avalanche)                                  │
 * │  - 大量key同时过期或Redis宕机                                │
 * │  - 解决：随机过期时间、多级缓存、熔断降级                      │
 * └─────────────────────────────────────────────────────────────┘
 */
public class RedisCacheProblemDemo {

    private StringRedisTemplate redisTemplate;

    // ==================== 1. 缓存穿透解决方案 ====================

    /**
     * 方案1：缓存空值
     *
     * 优点：实现简单
     * 缺点：额外内存消耗，可能造成短期不一致
     */
    public String cachePenetrationSolution1(String key) {
        String cacheKey = "data:" + key;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 1. 查缓存
        String value = ops.get(cacheKey);

        // 判断是否为空值标记
        if ("NULL".equals(value)) {
            System.out.println("【缓存空值】key=" + key + " 不存在");
            return null;
        }

        if (value != null) {
            return value;
        }

        // 2. 查数据库
        value = queryDatabase(key);

        if (value != null) {
            // 缓存真实数据
            ops.set(cacheKey, value, 30, TimeUnit.MINUTES);
        } else {
            // 缓存空值，设置较短的过期时间
            ops.set(cacheKey, "NULL", 5, TimeUnit.MINUTES);
            System.out.println("【缓存空值】key=" + key + " 缓存空值5分钟");
        }

        return value;
    }

    /**
     * 方案2：布隆过滤器
     *
     * 优点：内存占用少，没有多余缓存
     * 缺点：有一定误判率，实现复杂
     */
    public static class BloomFilterSolution {

        private org.redisson.api.RBloomFilter<String> bloomFilter;
        private StringRedisTemplate redisTemplate;

        /**
         * 初始化布隆过滤器
         */
        public void initBloomFilter() {
            bloomFilter = org.redisson.Redisson.create()
                    .getBloomFilter("data:bloom");

            // 预期插入100万个元素，误判率0.01
            bloomFilter.tryInit(1000000L, 0.01);

            // 加载已有数据到过滤器
            loadExistingData();
        }

        private void loadExistingData() {
            // 从数据库加载所有有效key
            System.out.println("【布隆过滤器】加载现有数据...");
            bloomFilter.add("user:1001");
            bloomFilter.add("user:1002");
            bloomFilter.add("user:1003");
        }

        /**
         * 查询数据
         */
        public String getData(String key) {
            // 1. 布隆过滤器检查
            if (!bloomFilter.contains(key)) {
                System.out.println("【布隆过滤器】key=" + key + " 肯定不存在");
                return null;
            }

            // 2. 查缓存
            String cacheKey = "data:" + key;
            String value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                return value;
            }

            // 3. 查数据库（布隆过滤器说可能存在，才查数据库）
            value = queryDatabase(key);
            if (value != null) {
                redisTemplate.opsForValue().set(cacheKey, value, 30, TimeUnit.MINUTES);
            }

            return value;
        }
    }

    /**
     * 方案3：参数校验 + 限流
     */
    public String cachePenetrationSolution3(String key) {
        // 1. 参数校验
        if (!isValidKey(key)) {
            System.out.println("【参数校验】非法key: " + key);
            return null;
        }

        // 2. 对可疑IP限流
        String ipKey = "limit:ip:" + getClientIp();
        Long count = redisTemplate.opsForValue().increment(ipKey);
        if (count == 1) {
            redisTemplate.expire(ipKey, 1, TimeUnit.MINUTES);
        }
        if (count > 100) {
            System.out.println("【限流】访问过于频繁");
            return null;
        }

        // 3. 正常查询流程
        return cachePenetrationSolution1(key);
    }

    // ==================== 2. 缓存击穿解决方案 ====================

    /**
     * 方案1：互斥锁（Mutex Lock）
     *
     * 保证只有一个线程去重建缓存
     */
    public String cacheBreakdownSolution1(String key) {
        String cacheKey = "hot:data:" + key;
        String lockKey = "lock:" + key;
        ValueOperations<String, String> ops = redisTemplate.opsForValue();

        // 1. 查缓存
        String value = ops.get(cacheKey);
        if (value != null) {
            return value;
        }

        // 2. 缓存未命中，尝试获取锁
        Boolean locked = ops.setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双重检查
                value = ops.get(cacheKey);
                if (value != null) {
                    return value;
                }

                // 3. 查数据库并重建缓存
                System.out.println("【互斥锁】获取锁，重建缓存: " + key);
                value = queryDatabase(key);
                if (value != null) {
                    ops.set(cacheKey, value, 30, TimeUnit.MINUTES);
                }
            } finally {
                // 释放锁
                redisTemplate.delete(lockKey);
            }
        } else {
            // 获取锁失败，短暂等待后重试
            System.out.println("【互斥锁】获取锁失败，等待重试: " + key);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return cacheBreakdownSolution1(key);
        }

        return value;
    }

    /**
     * 方案2：逻辑过期（Logical Expiration）
     *
     * 不设置TTL，通过逻辑时间判断是否过期
     * 过期后异步重建，无需等待
     */
    public static class LogicalExpireData {
        private String data;
        private long expireTime;  // 逻辑过期时间

        public LogicalExpireData(String data, long expireTime) {
            this.data = data;
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public String getData() { return data; }
        public long getExpireTime() { return expireTime; }
    }

    public String cacheBreakdownSolution2(String key) {
        String cacheKey = "hot:logical:" + key;

        // 1. 查缓存
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json == null) {
            return null;
        }

        // 2. 解析逻辑过期时间
        LogicalExpireData expireData = parseLogicalData(json);

        // 3. 判断是否逻辑过期
        if (!expireData.isExpired()) {
            return expireData.getData();
        }

        // 4. 已过期，尝试获取锁重建
        String lockKey = "lock:logical:" + key;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

        if (Boolean.TRUE.equals(locked)) {
            // 异步重建缓存
            new Thread(() -> {
                try {
                    System.out.println("【逻辑过期】异步重建缓存: " + key);
                    String newData = queryDatabase(key);
                    saveWithLogicalExpire(cacheKey, newData, 30);
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }).start();
        }

        // 5. 返回过期数据（保证可用性）
        System.out.println("【逻辑过期】返回过期数据: " + key);
        return expireData.getData();
    }

    private void saveWithLogicalExpire(String key, String data, long expireMinutes) {
        long expireTime = System.currentTimeMillis() + expireMinutes * 60 * 1000;
        LogicalExpireData expireData = new LogicalExpireData(data, expireTime);
        String json = toJson(expireData);
        redisTemplate.opsForValue().set(key, json);
    }

    /**
     * 方案3：热点Key永不过期
     *
     * 通过定时任务异步更新缓存
     */
    public String cacheBreakdownSolution3(String key) {
        String cacheKey = "hot:never:" + key;

        // 1. 查缓存（永不过期）
        String value = redisTemplate.opsForValue().get(cacheKey);
        if (value != null) {
            return value;
        }

        // 2. 异步加载（实际应由定时任务完成）
        value = queryDatabase(key);
        if (value != null) {
            redisTemplate.opsForValue().set(cacheKey, value);
        }

        return value;
    }

    // ==================== 3. 缓存雪崩解决方案 ====================

    /**
     * 方案1：随机过期时间
     *
     * 避免大量key同时过期
     */
    public void cacheAvalancheSolution1() {
        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            String key = "data:" + i;
            String value = "value" + i;

            // 基础过期时间 + 随机偏移（0-300秒）
            int baseExpire = 1800;  // 30分钟
            int randomOffset = random.nextInt(300);
            int expireTime = baseExpire + randomOffset;

            redisTemplate.opsForValue().set(key, value, expireTime, TimeUnit.SECONDS);
        }

        System.out.println("【随机过期】100个key设置随机过期时间");
    }

    /**
     * 方案2：多级缓存
     *
     * Caffeine（本地）+ Redis（远程）+ 数据库
     */
    public static class MultiLevelCache {

        private com.github.benmanes.caffeine.cache.Cache<String, String> localCache;
        private StringRedisTemplate redisTemplate;

        public MultiLevelCache() {
            this.localCache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(1, TimeUnit.MINUTES)
                    .build();
        }

        public String get(String key) {
            // 1. 查本地缓存
            String value = localCache.getIfPresent(key);
            if (value != null) {
                System.out.println("【多级缓存】本地缓存命中: " + key);
                return value;
            }

            // 2. 查 Redis
            value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                System.out.println("【多级缓存】Redis命中: " + key);
                localCache.put(key, value);
                return value;
            }

            // 3. 查数据库
            System.out.println("【多级缓存】查询数据库: " + key);
            value = queryDatabase(key);
            if (value != null) {
                redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
                localCache.put(key, value);
            }

            return value;
        }
    }

    /**
     * 方案3：熔断降级
     *
     * Redis 故障时，直接返回默认值或本地数据
     */
    public String cacheAvalancheSolution3(String key) {
        String cacheKey = "data:" + key;

        try {
            // 尝试从 Redis 获取
            String value = redisTemplate.opsForValue().get(cacheKey);
            if (value != null) {
                return value;
            }
        } catch (Exception e) {
            // Redis 故障，触发降级
            System.out.println("【熔断降级】Redis故障，返回默认值: " + key);
            return getDefaultValue(key);
        }

        // 查数据库
        return queryDatabase(key);
    }

    /**
     * 方案4：高可用架构
     *
     * Redis Cluster + 哨兵 + 主从复制
     */
    public void cacheAvalancheSolution4() {
        System.out.println("【高可用架构】");
        System.out.println("  - Redis Cluster：数据分片，水平扩展");
        System.out.println("  - 哨兵模式：自动故障转移");
        System.out.println("  - 主从复制：读写分离，数据备份");
        System.out.println("  - 持久化：RDB + AOF，防止数据丢失");
    }

    // ==================== 辅助方法 ====================

    private static String queryDatabase(String key) {
        // 模拟数据库查询
        System.out.println("【查询数据库】key=" + key);
        return "data_" + key;
    }

    private boolean isValidKey(String key) {
        return key != null && key.matches("^[a-zA-Z0-9:]+$");
    }

    private String getClientIp() {
        return "127.0.0.1";
    }

    private static LogicalExpireData parseLogicalData(String json) {
        // 简化解析
        return new LogicalExpireData(json, System.currentTimeMillis() + 3600000);
    }

    private static String toJson(LogicalExpireData data) {
        return "{\"data\":\"" + data.getData() + "\",\"expireTime\":" + data.getExpireTime() + "}";
    }

    private String getDefaultValue(String key) {
        return "default_" + key;
    }

    /**
     * 总结对比
     */
    public static void printSummary() {
        System.out.println("\n========== 缓存问题解决方案总结 ==========\n");

        System.out.println("【缓存穿透】");
        System.out.println("  问题：查询不存在数据，绕过缓存直达数据库");
        System.out.println("  解决：");
        System.out.println("    1. 缓存空值（简单，有内存开销）");
        System.out.println("    2. 布隆过滤器（高效，无内存浪费）");
        System.out.println("    3. 参数校验 + 限流（防止恶意攻击）\n");

        System.out.println("【缓存击穿】");
        System.out.println("  问题：热点key过期，大量请求打崩数据库");
        System.out.println("  解决：");
        System.out.println("    1. 互斥锁（保证单线程重建）");
        System.out.println("    2. 逻辑过期（永不过期，异步更新）");
        System.out.println("    3. 热点key永不过期（定时更新）\n");

        System.out.println("【缓存雪崩】");
        System.out.println("  问题：大量key同时过期或Redis宕机");
        System.out.println("  解决：");
        System.out.println("    1. 随机过期时间（打散过期点）");
        System.out.println("    2. 多级缓存（本地+远程）");
        System.out.println("    3. 熔断降级（故障时保护数据库）");
        System.out.println("    4. 高可用架构（Cluster+哨兵）\n");
    }
}
