package cn.itzixiao.interview.redis.advanced;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Redis 限流实现详解
 *
 * 限流算法对比：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 计数器算法 (Counter)                                     │
 * │     - 简单，但临界问题严重                                    │
 * │     - 适合简单场景                                            │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 滑动窗口算法 (Sliding Window)                            │
 * │     - 精确，但内存消耗大                                      │
 * │     - 适合精度要求高的场景                                    │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 漏桶算法 (Leaky Bucket)                                  │
 * │     - 流量平滑，可应对突发流量                                │
 * │     - 适合需要匀速处理的场景                                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  4. 令牌桶算法 (Token Bucket)                                │
 * │     - 允许突发流量，平均速率限制                              │
 * │     - 适合大多数场景（最常用）                                │
 * └─────────────────────────────────────────────────────────────┘
 */
public class RedisRateLimiterDemo {

    private StringRedisTemplate redisTemplate;

    /**
     * 1. 固定窗口计数器限流
     *
     * 问题：临界突发问题
     * 时间: |-------窗口1-------|-------窗口2-------|
     * 请求:              100个              100个
     * 在窗口交界处可能瞬间有200个请求通过
     */
    public boolean fixedWindowRateLimit(String key, int limit, int windowSeconds) {
        String redisKey = "rate:limit:fixed:" + key;

        // 获取当前计数
        Long count = redisTemplate.opsForValue().increment(redisKey);

        // 第一次访问，设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }

        return count != null && count <= limit;
    }

    /**
     * 2. 滑动窗口限流（基于 Redis ZSet）
     *
     * 精确统计时间窗口内的请求数
     */
    public boolean slidingWindowRateLimit(String key, int limit, int windowSeconds) {
        String redisKey = "rate:limit:sliding:" + key;

        long now = System.currentTimeMillis();
        long windowStart = now - windowSeconds * 1000;

        // Lua 脚本保证原子性
        String luaScript =
                "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1]) " +  // 移除窗口外的记录
                        "local current = redis.call('ZCARD', KEYS[1]) " +       // 统计当前窗口内记录数
                        "if current < tonumber(ARGV[2]) then " +
                        "   redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3]) " +   // 添加当前请求
                        "   redis.call('EXPIRE', KEYS[1], ARGV[4]) " +
                        "   return 1 " +
                        "else " +
                        "   return 0 " +
                        "end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(redisKey),
                String.valueOf(windowStart),      // ARGV[1]: 窗口起始时间
                String.valueOf(limit),             // ARGV[2]: 限制次数
                String.valueOf(now),               // ARGV[3]: 当前时间戳
                String.valueOf(windowSeconds)      // ARGV[4]: 过期时间
        );

        return result != null && result == 1;
    }

    /**
     * 3. 令牌桶限流（最常用）
     *
     * 原理：
     * - 桶容量固定，以固定速率产生令牌
     * - 请求需要获取令牌才能执行
     * - 桶满时不再产生令牌
     * - 允许突发流量（桶内令牌数）
     */
    public boolean tokenBucketRateLimit(String key, int capacity, double rate) {
        String redisKey = "rate:limit:token:" + key;

        // Lua 脚本实现令牌桶
        String luaScript =
                "local key = KEYS[1] " +
                        "local capacity = tonumber(ARGV[1]) " +
                        "local rate = tonumber(ARGV[2]) " +
                        "local now = tonumber(ARGV[3]) " +
                        "local requested = tonumber(ARGV[4]) " +

                        // 获取当前状态
                        "local bucket = redis.call('HMGET', key, 'tokens', 'last_time') " +
                        "local tokens = tonumber(bucket[1]) or capacity " +
                        "local last_time = tonumber(bucket[2]) or now " +

                        // 计算新增令牌
                        "local delta = math.max(0, now - last_time) " +
                        "local new_tokens = math.min(capacity, tokens + delta * rate) " +

                        // 判断是否允许请求
                        "local allowed = new_tokens >= requested " +
                        "local remaining = new_tokens " +

                        "if allowed then " +
                        "   remaining = new_tokens - requested " +
                        "end " +

                        // 更新状态
                        "redis.call('HMSET', key, 'tokens', remaining, 'last_time', now) " +
                        "redis.call('EXPIRE', key, 60) " +

                        "return allowed and 1 or 0";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        long now = System.currentTimeMillis() / 1000;

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(redisKey),
                String.valueOf(capacity),   // 桶容量
                String.valueOf(rate),       // 令牌产生速率（个/秒）
                String.valueOf(now),        // 当前时间
                "1"                          // 请求令牌数
        );

        return result != null && result == 1;
    }

    /**
     * 4. 漏桶限流
     *
     * 原理：
     * - 请求先进入桶（队列）
     * - 桶以固定速率漏出（处理）
     * - 桶满时拒绝新请求
     * - 流量平滑，无突发
     */
    public boolean leakyBucketRateLimit(String key, int capacity, int ratePerSecond) {
        String redisKey = "rate:limit:leaky:" + key;

        String luaScript =
                "local key = KEYS[1] " +
                        "local capacity = tonumber(ARGV[1]) " +
                        "local rate = tonumber(ARGV[2]) " +
                        "local now = tonumber(ARGV[3]) " +

                        // 获取当前水量和上次漏水时间
                        "local bucket = redis.call('HMGET', key, 'water', 'last_leak') " +
                        "local water = tonumber(bucket[1]) or 0 " +
                        "local last_leak = tonumber(bucket[2]) or now " +

                        // 计算漏出的水量
                        "local leaked = (now - last_leak) * rate " +
                        "local new_water = math.max(0, water - leaked) " +

                        // 判断是否可放入桶中
                        "local allowed = new_water < capacity " +

                        "if allowed then " +
                        "   new_water = new_water + 1 " +
                        "end " +

                        // 更新状态
                        "redis.call('HMSET', key, 'water', new_water, 'last_leak', now) " +
                        "redis.call('EXPIRE', key, 60) " +

                        "return allowed and 1 or 0";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        long now = System.currentTimeMillis() / 1000;

        Long result = redisTemplate.execute(
                redisScript,
                Collections.singletonList(redisKey),
                String.valueOf(capacity),
                String.valueOf(ratePerSecond),
                String.valueOf(now)
        );

        return result != null && result == 1;
    }

    /**
     * 5. 分布式限流 - 基于 Redisson
     */
    public void redissonRateLimiterDemo() {
        // 使用 Redisson 提供的 RRateLimiter
        org.redisson.api.RRateLimiter rateLimiter =
                org.redisson.Redisson.create().getRateLimiter("myRateLimiter");

        // 初始化：每10秒产生5个令牌
        rateLimiter.trySetRate(
                org.redisson.api.RateType.OVERALL,  // 全局限流
                5,                                   // 速率
                10,                                  // 速率间隔
                org.redisson.api.RateIntervalUnit.SECONDS
        );

        // 尝试获取1个令牌
        boolean acquired = rateLimiter.tryAcquire(1, 3, TimeUnit.SECONDS);

        if (acquired) {
            System.out.println("【限流通过】执行业务");
        } else {
            System.out.println("【限流拒绝】请求过于频繁");
        }
    }

    /**
     * 6. 多级限流（用户级 + 接口级 + 全局）
     */
    public boolean multiLevelRateLimit(String userId, String api, String ip) {
        // 1. 用户级限流：每个用户每分钟100次
        if (!slidingWindowRateLimit("user:" + userId, 100, 60)) {
            System.out.println("【限流】用户级别超限");
            return false;
        }

        // 2. 接口级限流：每个接口每秒1000次
        if (!tokenBucketRateLimit("api:" + api, 1000, 1000)) {
            System.out.println("【限流】接口级别超限");
            return false;
        }

        // 3. IP级限流：每个IP每分钟50次
        if (!fixedWindowRateLimit("ip:" + ip, 50, 60)) {
            System.out.println("【限流】IP级别超限");
            return false;
        }

        // 4. 全局限流：每秒10000次
        if (!tokenBucketRateLimit("global", 10000, 10000)) {
            System.out.println("【限流】全局级别超限");
            return false;
        }

        return true;
    }

    /**
     * 限流注解实现思路
     */
    public @interface RateLimit {
        String key() default "";
        int limit() default 100;
        int window() default 60;
        String message() default "请求过于频繁，请稍后再试";
    }

    /**
     * 使用示例
     */
    @RateLimit(key = "order:create", limit = 10, window = 60)
    public void createOrder() {
        System.out.println("创建订单");
    }
}
