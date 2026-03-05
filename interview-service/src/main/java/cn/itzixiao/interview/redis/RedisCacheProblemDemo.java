package cn.itzixiao.interview.redis;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redis 缓存问题详解
 *
 * 三大缓存问题：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 缓存穿透 (Cache Penetration)                             │
 * │     - 查询不存在的数据，缓存和数据库都没有                     │
 * │     - 大量请求直接打到数据库                                  │
 * │     - 解决：布隆过滤器、缓存空值、参数校验                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. 缓存击穿 (Cache Breakdown)                               │
 * │     - 热点key过期，大量请求同时访问数据库                      │
 * │     - 解决：互斥锁、逻辑过期、热点key永不过期                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 缓存雪崩 (Cache Avalanche)                               │
 * │     - 大量key同时过期，或Redis宕机                            │
 * │     - 解决：过期时间加随机值、多级缓存、熔断降级               │
 * └─────────────────────────────────────────────────────────────┘
 */
public class RedisCacheProblemDemo {

    // 模拟 Redis 缓存
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    // 模拟数据库
    private final Map<String, Object> database = new ConcurrentHashMap<>();
    // 布隆过滤器（简化版）
    private final Map<String, Boolean> bloomFilter = new ConcurrentHashMap<>();

    public RedisCacheProblemDemo() {
        // 初始化数据库数据
        database.put("user:1", "User1");
        database.put("user:2", "User2");
        database.put("user:3", "User3");
        database.put("hot:product", "Hot Product Data");

        // 初始化布隆过滤器
        database.keySet().forEach(key -> bloomFilter.put(key, true));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("========== Redis 缓存问题详解 ==========\n");

        RedisCacheProblemDemo demo = new RedisCacheProblemDemo();

        // 1. 缓存穿透
        demo.demonstrateCachePenetration();

        // 2. 缓存击穿
        demo.demonstrateCacheBreakdown();

        // 3. 缓存雪崩
        demo.demonstrateCacheAvalanche();

        // 4. 缓存一致性
        demo.demonstrateCacheConsistency();
    }

    /**
     * 1. 缓存穿透演示
     */
    private void demonstrateCachePenetration() throws InterruptedException {
        System.out.println("【1. 缓存穿透 (Cache Penetration)】\n");

        System.out.println("问题描述：");
        System.out.println("- 查询一个不存在的数据（如 id=-1）");
        System.out.println("- 缓存中没有，数据库也没有");
        System.out.println("- 每次请求都直接访问数据库\n");

        // 模拟攻击：查询不存在的数据
        System.out.println("模拟攻击（查询 user:9999，不存在）：");
        for (int i = 0; i < 5; i++) {
            String result = getDataWithoutProtection("user:9999");
            System.out.println("  请求 " + (i + 1) + ": " + (result != null ? result : "null") +
                " (直接访问数据库)");
        }
        System.out.println();

        // 解决方案1：缓存空值
        System.out.println("解决方案1：缓存空值");
        System.out.println("- 数据库查询为空时，缓存一个空值（设置较短过期时间）");
        System.out.println("- 后续相同请求直接从缓存返回空值\n");

        for (int i = 0; i < 5; i++) {
            String result = getDataWithNullCache("user:8888");
            System.out.println("  请求 " + (i + 1) + ": " +
                ("NULL".equals(result) ? "空值缓存" : result));
        }
        System.out.println();

        // 解决方案2：布隆过滤器
        System.out.println("解决方案2：布隆过滤器");
        System.out.println("- 查询前先用布隆过滤器判断key是否存在");
        System.out.println("- 不存在直接返回，不访问缓存和数据库\n");

        String[] testKeys = {"user:1", "user:9999", "user:2", "user:8888"};
        for (String key : testKeys) {
            String result = getDataWithBloomFilter(key);
            System.out.println("  查询 " + key + ": " + result);
        }
        System.out.println();
    }

    /**
     * 2. 缓存击穿演示
     */
    private void demonstrateCacheBreakdown() throws InterruptedException {
        System.out.println("【2. 缓存击穿 (Cache Breakdown)】\n");

        System.out.println("问题描述：");
        System.out.println("- 热点key在高并发下过期");
        System.out.println("- 大量请求同时访问数据库重建缓存");
        System.out.println("- 数据库压力瞬间增大\n");

        // 模拟热点key过期
        String hotKey = "hot:product";
        cache.put(hotKey, database.get(hotKey));
        System.out.println("设置热点key: " + hotKey);

        // 模拟key过期
        cache.remove(hotKey);
        System.out.println("热点key过期！\n");

        // 模拟多个线程同时查询（无保护）
        System.out.println("无保护，10个线程同时查询：");
        CountDownLatch latch = new CountDownLatch(10);
        AtomicInteger dbQueryCount = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                String result = getHotDataWithoutLock(hotKey, dbQueryCount);
                latch.countDown();
            }).start();
        }
        latch.await();
        System.out.println("数据库被访问次数: " + dbQueryCount.get() + "\n");

        // 清除缓存，重新测试
        cache.remove(hotKey);

        // 解决方案：互斥锁
        System.out.println("解决方案：互斥锁");
        System.out.println("- 只有一个线程去数据库查询并重建缓存");
        System.out.println("- 其他线程等待或重试\n");

        CountDownLatch latch2 = new CountDownLatch(10);
        AtomicInteger dbQueryCount2 = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                String result = getHotDataWithLock(hotKey, dbQueryCount2);
                latch2.countDown();
            }).start();
        }
        latch2.await();
        System.out.println("数据库被访问次数: " + dbQueryCount2.get() + "\n");
    }

    /**
     * 3. 缓存雪崩演示
     */
    private void demonstrateCacheAvalanche() throws InterruptedException {
        System.out.println("【3. 缓存雪崩 (Cache Avalanche)】\n");

        System.out.println("问题描述：");
        System.out.println("- 大量key同时过期");
        System.out.println("- 或者Redis宕机");
        System.out.println("- 所有请求直接打到数据库\n");

        // 模拟大量key同时设置相同过期时间
        System.out.println("模拟：100个key同时设置30秒过期");
        System.out.println("问题：30秒后，这100个key同时失效\n");

        // 解决方案1：过期时间加随机值
        System.out.println("解决方案1：过期时间加随机值");
        System.out.println("- 基础过期时间 + 随机偏移量");
        System.out.println("- 避免大量key同时过期\n");

        for (int i = 0; i < 5; i++) {
            int expireTime = getRandomExpireTime(30);
            System.out.println("  key" + i + " 过期时间: " + expireTime + "秒");
        }
        System.out.println();

        // 解决方案2：多级缓存
        System.out.println("解决方案2：多级缓存");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  应用层                                  │");
        System.out.println("│  ┌─────────┐  ┌─────────┐  ┌─────────┐ │");
        System.out.println("│  │ 本地缓存 │→│ Redis   │→│ 数据库   │ │");
        System.out.println("│  │ Caffeine│  │ 缓存    │  │         │ │");
        System.out.println("│  └─────────┘  └─────────┘  └─────────┘ │");
        System.out.println("│       ↑ 降级保护                          │");
        System.out.println("│   Redis宕机时，直接查数据库或返回默认值    │");
        System.out.println("└─────────────────────────────────────────┘\n");

        // 解决方案3：熔断降级
        System.out.println("解决方案3：熔断降级（Sentinel/Hystrix）");
        System.out.println("- 数据库压力过大时，熔断部分请求");
        System.out.println("- 返回默认值或错误提示\n");
    }

    /**
     * 4. 缓存一致性
     */
    private void demonstrateCacheConsistency() {
        System.out.println("【4. 缓存一致性问题】\n");

        System.out.println("问题：数据库和缓存数据不一致\n");

        System.out.println("更新策略对比：");
        System.out.println("┌─────────────┬─────────────────────────────────────────────┐");
        System.out.println("│  Cache Aside│  先更新数据库，再删除缓存（推荐）            │");
        System.out.println("│  (旁路缓存) │  - 读：先读缓存，miss则读库，再写缓存        │");
        System.out.println("│             │  - 写：先写库，再删缓存                      │");
        System.out.println("├─────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  Read/Write │  先更新缓存，再异步写数据库                   │");
        System.out.println("│  Through    │  - 数据一致性好，但实现复杂                   │");
        System.out.println("├─────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  Write      │  只更新缓存，由缓存异步写数据库               │");
        System.out.println("│  Behind     │  - 性能最好，但可能丢数据                     │");
        System.out.println("└─────────────┴─────────────────────────────────────────────┘\n");

        System.out.println("Cache Aside 为什么是删除缓存而不是更新缓存？");
        System.out.println("1. 并发场景下，更新缓存可能导致脏数据");
        System.out.println("2. 删除缓存更简单，下次读取时重建");
        System.out.println("3. 懒加载思想，避免写入无效的缓存\n");

        System.out.println("并发问题示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  线程A（写）          │  线程B（写）          │  线程C（读） │");
        System.out.println("├───────────────────────┼───────────────────────┼─────────────┤");
        System.out.println("│  更新数据库 value=1   │                       │             │");
        System.out.println("│                       │  更新数据库 value=2   │             │");
        System.out.println("│                       │  更新缓存 value=2     │             │");
        System.out.println("│  更新缓存 value=1     │                       │             │");
        System.out.println("│                       │                       │  读缓存=1   │");
        System.out.println("│                       │                       │  （脏数据）  │");
        System.out.println("└───────────────────────┴───────────────────────┴─────────────┘\n");

        System.out.println("解决方案：");
        System.out.println("1. 延迟双删：删缓存→写数据库→延迟→再删缓存");
        System.out.println("2. 消息队列：写数据库→发消息→异步删缓存");
        System.out.println("3. Canal 订阅 binlog：监听数据库变更，同步删缓存");
        System.out.println("4. 设置合理的过期时间，最终一致性\n");
    }

    // ==================== 辅助方法 ====================

    // 无保护的查询
    private String getDataWithoutProtection(String key) {
        String value = (String) cache.get(key);
        if (value == null) {
            // 模拟数据库查询
            value = (String) database.get(key);
        }
        return value;
    }

    // 缓存空值
    private String getDataWithNullCache(String key) {
        String value = (String) cache.get(key);
        if (value != null) {
            return "NULL".equals(value) ? null : value;
        }

        value = (String) database.get(key);
        if (value == null) {
            // 缓存空值，设置5分钟过期
            cache.put(key, "NULL");
        } else {
            cache.put(key, value);
        }
        return value;
    }

    // 布隆过滤器
    private String getDataWithBloomFilter(String key) {
        // 布隆过滤器判断
        if (!bloomFilter.containsKey(key)) {
            return "[布隆过滤器拦截]";
        }

        String value = (String) cache.get(key);
        if (value == null) {
            value = (String) database.get(key);
            if (value != null) {
                cache.put(key, value);
            }
        }
        return value != null ? value : "null";
    }

    // 无锁查询热点数据
    private String getHotDataWithoutLock(String key, AtomicInteger dbCount) {
        String value = (String) cache.get(key);
        if (value == null) {
            // 模拟数据库查询
            dbCount.incrementAndGet();
            try {
                Thread.sleep(100); // 模拟查询耗时
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            value = (String) database.get(key);
            if (value != null) {
                cache.put(key, value);
            }
        }
        return value;
    }

    // 带锁查询热点数据
    private String getHotDataWithLock(String key, AtomicInteger dbCount) {
        String value = (String) cache.get(key);
        if (value != null) {
            return value;
        }

        // 模拟获取分布式锁
        synchronized (this) {
            // 双重检查
            value = (String) cache.get(key);
            if (value != null) {
                return value;
            }

            // 只有一个线程执行数据库查询
            dbCount.incrementAndGet();
            try {
                Thread.sleep(100); // 模拟查询耗时
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            value = (String) database.get(key);
            if (value != null) {
                cache.put(key, value);
            }
        }
        return value;
    }

    // 随机过期时间
    private int getRandomExpireTime(int baseSeconds) {
        return baseSeconds + new Random().nextInt(300); // 基础时间 + 0-300秒随机
    }
}

// 使用 java.util.concurrent.atomic.AtomicInteger
