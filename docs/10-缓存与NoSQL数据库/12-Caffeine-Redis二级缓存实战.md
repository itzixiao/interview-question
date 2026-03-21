# Caffeine + Redis 二级缓存实战

> 📚 本文档详细讲解 Caffeine + Redis 二级缓存架构设计与实现，包括缓存一致性、性能优化、实战案例等核心知识点。

## 目录

- [一、二级缓存架构概述](#一二级缓存架构概述)
- [二、Caffeine 本地缓存详解](#二caffeine-本地缓存详解)
- [三、二级缓存架构设计](#三二级缓存架构设计)
- [四、核心代码实现](#四核心代码实现)
- [五、缓存一致性方案](#五缓存一致性方案)
- [六、性能优化与监控](#六性能优化与监控)
- [七、实战案例](#七实战案例)
- [八、常见问题](#八常见问题)

---

## 一、二级缓存架构概述

### 1.1 为什么需要二级缓存

```
┌─────────────────────────────────────────────────────────────────┐
│                     二级缓存架构优势                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  L1 (Caffeine)          L2 (Redis)           DB                 │
│  ┌─────────┐           ┌─────────┐           ┌─────────┐        │
│  │ 本地缓存  │ ← 未命中 → │ 远程缓存 │ ← 未命中 → │  数据库   │       │
│  │  <1ms   │           │  <5ms   │           │  >10ms  │        │
│  │  无网络  │           │  一次网络 │           │  磁盘IO  │       │
│  └─────────┘           └─────────┘           └─────────┘        │
│                                                                  │
│  优势：                                                           │
│  ✅ L1 命中：极致性能，无网络开销                                     │
│  ✅ L2 命中：避免数据库压力，跨进程共享                                │
│  ✅ 分层设计：平衡性能与一致性                                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 适用场景

| 场景    | 说明        | 示例        |
|-------|-----------|-----------|
| 热点数据  | 访问频率极高的数据 | 商品详情、用户信息 |
| 配置数据  | 变动较少的数据   | 系统配置、字典数据 |
| 计算密集型 | 查询成本高的数据  | 统计报表、聚合结果 |
| 读多写少  | 读取远大于写入   | 新闻内容、商品分类 |

### 1.3 不适用场景

- ❌ **强一致性要求**：金融交易、库存扣减
- ❌ **频繁变更数据**：实时价格、秒杀库存
- ❌ **数据量过大**：超出本地内存容量

---

## 二、Caffeine 本地缓存详解

### 2.1 Caffeine 核心特性

```java
/**
 * Caffeine 特点：
 * 1. 基于 W-TinyLFU 淘汰算法，命中率更高
 * 2. 支持异步加载、刷新
 * 3. 提供丰富的统计信息
 * 4. 线程安全，性能优异
 */
```

### 2.2 基础配置

```java

@Configuration
public class CaffeineConfig {

    @Bean
    public Cache<String, Object> caffeineCache() {
        return Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(10000)
                // 写入后过期时间
                .expireAfterWrite(10, TimeUnit.MINUTES)
                // 访问后过期时间
                .expireAfterAccess(5, TimeUnit.MINUTES)
                // 刷新时间（异步刷新）
                .refreshAfterWrite(1, TimeUnit.MINUTES)
                // 开启统计
                .recordStats()
                // 淘汰监听器
                .removalListener((key, value, cause) ->
                        log.debug("Cache removed: key={}, cause={}", key, cause))
                .build();
    }
}
```

### 2.3 异步加载配置

```java

@Bean
public LoadingCache<String, User> userCache() {
    return Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            // 异步加载
            .buildAsync((key, executor) ->
                    CompletableFuture.supplyAsync(
                            () -> userService.loadUser(key), executor))
            .synchronous();
}
```

---

## 三、二级缓存架构设计

### 3.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      二级缓存架构图                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│   Application                                                   │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │              CacheManager (缓存管理器)                   │  │
│   │  ┌─────────────────────────────────────────────────┐   │  │
│   │  │           L1: Caffeine (本地缓存)                │   │  │
│   │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐          │   │  │
│   │  │  │ 热点数据 │   │ 配置数据 │  │ 用户数据 │          │   │  │
│   │  │  └─────────┘  └─────────┘  └─────────┘          │   │  │
│   │  └─────────────────────────────────────────────────┘   │  │
│   │                         ↓ 未命中                        │  │
│   │  ┌─────────────────────────────────────────────────┐   │  │
│   │  │           L2: Redis (远程缓存)                   │   │  │
│   │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐          │   │  │
│   │  │  │ 热点数据 │   │ 配置数据 │  │ 用户数据 │           │   │  │
│   │  │  └─────────┘  └─────────┘  └─────────┘          │   │  │
│   │  └─────────────────────────────────────────────────┘   │  │
│   └─────────────────────────────────────────────────────────┘  │
│                              ↓ 未命中                           │
│   ┌─────────────────────────────────────────────────────────┐  │
│   │                    Database (数据库)                     │  │
│   └─────────────────────────────────────────────────────────┘  │
│                                                                │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 缓存层级对比

| 特性   | L1 (Caffeine) | L2 (Redis) | DB       |
|------|---------------|------------|----------|
| 访问速度 | ~1ms          | ~5-10ms    | ~10-50ms |
| 容量   | 内存限制（MB级）     | 内存限制（GB级）  | 磁盘（TB级）  |
| 共享性  | 进程内独享         | 多进程共享      | 全局共享     |
| 一致性  | 弱一致性          | 最终一致性      | 强一致性     |
| 适用数据 | 热点数据          | 共享数据       | 全量数据     |

---

## 四、核心代码实现

### 4.1 缓存管理器实现

```java

@Component
@Slf4j
public class TieredCacheManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Cache<String, Object> caffeineCache;

    private static final String CACHE_PREFIX = "tiered:";

    /**
     * 获取缓存（L1 → L2 → DB）
     */
    public <T> T get(String key, Class<T> type, Supplier<T> dbLoader) {
        // 1. 先查 L1 (Caffeine)
        Object value = caffeineCache.getIfPresent(key);
        if (value != null) {
            log.debug("L1 hit: key={}", key);
            return (T) value;
        }

        // 2. 再查 L2 (Redis)
        String redisKey = CACHE_PREFIX + key;
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        if (redisValue != null) {
            log.debug("L2 hit: key={}", key);
            T result = JSON.parseObject(redisValue, type);
            // 回填 L1
            caffeineCache.put(key, result);
            return result;
        }

        // 3. 查询数据库
        log.debug("DB load: key={}", key);
        T result = dbLoader.get();
        if (result != null) {
            // 写入 L2
            redisTemplate.opsForValue().set(redisKey,
                    JSON.toJSONString(result),
                    Duration.ofMinutes(30));
            // 写入 L1
            caffeineCache.put(key, result);
        }
        return result;
    }

    /**
     * 设置缓存
     */
    public void set(String key, Object value, Duration redisTtl) {
        // 写入 L1
        caffeineCache.put(key, value);
        // 写入 L2
        redisTemplate.opsForValue().set(
                CACHE_PREFIX + key,
                JSON.toJSONString(value),
                redisTtl);
    }

    /**
     * 删除缓存
     */
    public void evict(String key) {
        // 删除 L1
        caffeineCache.invalidate(key);
        // 删除 L2
        redisTemplate.delete(CACHE_PREFIX + key);
    }
}
```

### 4.2 Spring Cache 集成

```java

@Configuration
@EnableCaching
public class TieredCacheConfiguration {

    @Bean
    public CacheManager tieredCacheManager(
            StringRedisTemplate redisTemplate,
            Cache<String, Object> caffeineCache) {

        return new TieredCacheManager(redisTemplate, caffeineCache);
    }

    /**
     * 自定义 Cache 实现
     */
    public static class TieredCache implements Cache {

        private final String name;
        private final Cache<String, Object> caffeineCache;
        private final StringRedisTemplate redisTemplate;

        public TieredCache(String name,
                           Cache<String, Object> caffeineCache,
                           StringRedisTemplate redisTemplate) {
            this.name = name;
            this.caffeineCache = caffeineCache;
            this.redisTemplate = redisTemplate;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        public ValueWrapper get(Object key) {
            String cacheKey = key.toString();

            // L1
            Object value = caffeineCache.getIfPresent(cacheKey);
            if (value != null) {
                return () -> value;
            }

            // L2
            String redisKey = name + ":" + cacheKey;
            String redisValue = redisTemplate.opsForValue().get(redisKey);
            if (redisValue != null) {
                Object result = JSON.parse(redisValue);
                caffeineCache.put(cacheKey, result);
                return () -> result;
            }
            return null;
        }

        @Override
        public void put(Object key, Object value) {
            String cacheKey = key.toString();
            caffeineCache.put(cacheKey, value);
            redisTemplate.opsForValue().set(
                    name + ":" + cacheKey,
                    JSON.toJSONString(value),
                    Duration.ofMinutes(30));
        }

        @Override
        public void evict(Object key) {
            String cacheKey = key.toString();
            caffeineCache.invalidate(cacheKey);
            redisTemplate.delete(name + ":" + cacheKey);
        }

        @Override
        public void clear() {
            caffeineCache.invalidateAll();
            // Redis 批量删除
            Set<String> keys = redisTemplate.keys(name + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }
}
```

### 4.3 注解式缓存使用

```java

@Service
@Slf4j
public class ProductService {

    @Autowired
    private ProductMapper productMapper;

    /**
     * 查询商品（使用二级缓存）
     */
    @TieredCacheable(
            cacheNames = "product",
            key = "#id",
            caffeineTtl = 10,  // L1: 10分钟
            redisTtl = 30      // L2: 30分钟
    )
    public Product getProduct(Long id) {
        log.info("Load product from DB: id={}", id);
        return productMapper.selectById(id);
    }

    /**
     * 更新商品（清除缓存）
     */
    @TieredCacheEvict(
            cacheNames = "product",
            key = "#product.id"
    )
    public void updateProduct(Product product) {
        productMapper.updateById(product);
    }

    /**
     * 批量查询（优化版本）
     */
    @TieredCacheable(
            cacheNames = "product:batch",
            key = "#ids.hashCode()"
    )
    public List<Product> getProducts(List<Long> ids) {
        return productMapper.selectBatchIds(ids);
    }
}

/**
 * 自定义二级缓存注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface TieredCacheable {
    String[] cacheNames() default {};

    String key() default "";

    long caffeineTtl() default 10;  // 分钟

    long redisTtl() default 30;     // 分钟
}
```

---

## 五、缓存一致性方案

### 5.1 更新策略

```
┌─────────────────────────────────────────────────────────────────┐
│                     缓存更新策略对比                              │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Cache Aside（旁路缓存）- 推荐                                   │
│                                                                  │
│     读：Cache → DB → 回填 Cache                                   │
│     写：更新 DB → 删除 Cache                                       │
│                                                                  │
│     优点：简单、容错性好                                            │
│     缺点：短暂不一致                                               │
│                                                                  │
│  2. Read/Write Through                                           │
│                                                                  │
│     读写都经过 Cache，Cache 负责同步 DB                              │
│                                                                  │
│     优点：对业务透明                                                │
│     缺点：实现复杂                                                 │
│                                                                  │
│  3. Write Behind（异步写）                                         │
│                                                                  │
│     先写 Cache，异步批量写 DB                                       │
│                                                                  │
│     优点：写性能极高                                                │
│     缺点：可能丢数据                                                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 一致性实现

```java

@Component
@Slf4j
public class CacheConsistencyManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Cache<String, Object> caffeineCache;

    /**
     * 延迟双删策略
     */
    public void delayedDoubleDelete(String key) {
        // 1. 先删 L1
        caffeineCache.invalidate(key);

        // 2. 删 L2
        redisTemplate.delete("tiered:" + key);

        // 3. 延迟再次删除（防止脏数据）
        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    caffeineCache.invalidate(key);
                    redisTemplate.delete("tiered:" + key);
                    log.debug("Delayed delete: key={}", key);
                });
    }

    /**
     * 基于 Canal 的订阅更新
     */
    @CanalListener(table = "product")
    public void onProductChange(CanalMessage message) {
        String id = message.getData().get("id");
        String key = "product:" + id;

        // 删除 L1
        caffeineCache.invalidate(key);

        // 发布消息通知其他节点
        redisTemplate.convertAndSend("cache:evict", key);
    }

    /**
     * 监听 Redis 消息，同步删除 L1
     */
    @RedisListener(channel = "cache:evict")
    public void onCacheEvictMessage(String key) {
        caffeineCache.invalidate(key);
        log.debug("Received evict message: key={}", key);
    }
}
```

### 5.3 分布式一致性

```java
/**
 * Redisson 分布式锁保证缓存更新安全
 */
@Component
public class DistributedCacheLock {

    @Autowired
    private RedissonClient redissonClient;

    public <T> T updateWithLock(String key, Supplier<T> updater) {
        RLock lock = redissonClient.getLock("cache:lock:" + key);

        try {
            // 尝试获取锁，最多等待 3 秒
            boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("获取缓存锁失败");
            }

            return updater.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

---

## 六、性能优化与监控

### 6.1 性能优化策略

```java

@Configuration
public class CacheOptimizationConfig {

    /**
     * 优化 1: 异步预热
     */
    @PostConstruct
    public void preheatCache() {
        CompletableFuture.runAsync(() -> {
            // 预热热点数据
            List<Product> hotProducts = productService.getHotProducts();
            hotProducts.forEach(p ->
                    cacheManager.set("product:" + p.getId(), p, Duration.ofHours(1)));
        });
    }

    /**
     * 优化 2: 批量加载
     */
    public Map<String, Object> batchGet(Set<String> keys) {
        // 批量从 L1 获取
        Map<String, Object> l1Result = caffeineCache.getAllPresent(keys);

        // 剩余 key 批量从 L2 获取
        Set<String> remainingKeys = keys.stream()
                .filter(k -> !l1Result.containsKey(k))
                .map(k -> "tiered:" + k)
                .collect(Collectors.toSet());

        if (!remainingKeys.isEmpty()) {
            List<String> l2Values = redisTemplate.opsForValue()
                    .multiGet(remainingKeys);
            // 回填 L1
        }

        return l1Result;
    }

    /**
     * 优化 3: 压缩存储
     */
    public void setCompressed(String key, Object value) {
        try {
            byte[] compressed = Snappy.compress(
                    JSON.toJSONString(value).getBytes());
            redisTemplate.opsForValue().set(
                    "tiered:" + key,
                    Base64.getEncoder().encodeToString(compressed));
        } catch (IOException e) {
            log.error("压缩失败", e);
        }
    }
}
```

### 6.2 监控指标

```java

@Component
public class CacheMetrics {

    @Autowired
    private Cache<String, Object> caffeineCache;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Caffeine 统计信息
     */
    public Map<String, Object> getCaffeineStats() {
        CacheStats stats = caffeineCache.stats();

        return Map.of(
                "hitCount", stats.hitCount(),
                "missCount", stats.missCount(),
                "hitRate", String.format("%.2f%%", stats.hitRate() * 100),
                "evictionCount", stats.evictionCount(),
                "loadSuccessCount", stats.loadSuccessCount(),
                "totalLoadTime", stats.totalLoadTime()
        );
    }

    /**
     * Redis 缓存统计
     */
    public Map<String, Object> getRedisStats() {
        Properties info = redisTemplate.execute(
                (RedisCallback<Properties>)
                        conn -> conn.serverCommands().info("memory"));

        return Map.of(
                "usedMemory", info.getProperty("used_memory_human"),
                "usedMemoryPeak", info.getProperty("used_memory_peak_human"),
                "keyCount", redisTemplate.execute(
                        conn -> conn.serverCommands().dbSize())
        );
    }

    /**
     * 缓存命中率报表
     */
    @Scheduled(fixedRate = 60000) // 每分钟输出
    public void printCacheReport() {
        CacheStats stats = caffeineCache.stats();
        log.info("【缓存统计】L1命中率: {}%, 请求数: {}, 淘汰数: {}",
                String.format("%.2f", stats.hitRate() * 100),
                stats.requestCount(),
                stats.evictionCount());
    }
}
```

---

## 七、实战案例

### 7.1 电商商品详情缓存

```java

@Service
@Slf4j
public class ProductDetailService {

    @Autowired
    private TieredCacheManager cacheManager;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductDetailConverter converter;

    /**
     * 获取商品详情（多级缓存）
     */
    public ProductDetailVO getProductDetail(Long productId) {
        String cacheKey = "product:detail:" + productId;

        return cacheManager.get(cacheKey, ProductDetailVO.class, () -> {
            // 1. 查询商品基础信息
            Product product = productMapper.selectById(productId);
            if (product == null) {
                return null;
            }

            // 2. 查询商品规格
            List<Sku> skus = skuMapper.selectByProductId(productId);

            // 3. 查询商品评价统计
            ReviewStats reviewStats = reviewService.getStats(productId);

            // 4. 组装 VO
            return converter.toDetailVO(product, skus, reviewStats);
        });
    }

    /**
     * 库存扣减（强一致性场景）
     */
    public boolean deductStock(Long skuId, Integer quantity) {
        String cacheKey = "sku:stock:" + skuId;

        // 1. 先删缓存
        cacheManager.evict(cacheKey);

        // 2. 更新数据库
        boolean success = skuMapper.deductStock(skuId, quantity) > 0;

        // 3. 延迟双删
        if (success) {
            CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
                    .execute(() -> cacheManager.evict(cacheKey));
        }

        return success;
    }
}
```

### 7.2 配置中心缓存

```java

@Service
public class ConfigService {

    @Autowired
    private TieredCacheManager cacheManager;

    @Autowired
    private ConfigMapper configMapper;

    /**
     * 获取配置（长缓存时间）
     */
    public String getConfig(String key) {
        String cacheKey = "config:" + key;

        return cacheManager.get(cacheKey, String.class, () -> {
            Config config = configMapper.selectByKey(key);
            return config != null ? config.getValue() : null;
        });
    }

    /**
     * 配置变更监听
     */
    @EventListener
    public void onConfigChange(ConfigChangeEvent event) {
        String key = "config:" + event.getKey();
        cacheManager.evict(key);

        // 广播通知其他节点
        redisTemplate.convertAndSend("config:changed", event.getKey());
    }
}
```

---

## 八、常见问题

### Q1: 如何避免缓存雪崩？

```java
/**
 * 解决方案：随机过期时间 + 熔断降级
 */
public Object getWithProtection(String key, Supplier<Object> loader) {
    try {
        return cacheManager.get(key, Object.class, loader);
    } catch (Exception e) {
        log.error("缓存异常，降级处理", e);
        // 降级：直接查库或返回默认值
        return loader.get();
    }
}

// 设置随机过期时间，避免同时失效
Duration ttl = Duration.ofMinutes(30 + RandomUtil.randomInt(10));
```

### Q2: 大对象如何处理？

```java
/**
 * 解决方案：压缩 + 分片
 */
public void setLargeObject(String key, Object value) {
    String json = JSON.toJSONString(value);

    if (json.length() > 1024 * 1024) { // 1MB
        // 1. 压缩
        byte[] compressed = Snappy.compress(json.getBytes());
        // 2. 分片存储
        List<byte[]> chunks = Lists.partition(
                Bytes.asList(compressed), 512 * 1024);

        for (int i = 0; i < chunks.size(); i++) {
            redisTemplate.opsForValue().set(
                    key + ":chunk:" + i,
                    Base64.getEncoder().encodeToString(Bytes.toArray(chunks.get(i))),
                    Duration.ofHours(1));
        }
    }
}
```

### Q3: 缓存穿透如何处理？

```java
/**
 * 解决方案：布隆过滤器 + 空值缓存
 */
public Object getWithBloomFilter(String key, Supplier<Object> loader) {
    // 1. 布隆过滤器检查
    if (!bloomFilter.mightContain(key)) {
        return null; // 一定不存在
    }

    // 2. 查询缓存
    Object value = cacheManager.get(key, Object.class, loader);

    // 3. 空值缓存（防止重复查库）
    if (value == null) {
        cacheManager.set(key, CacheConstant.EMPTY_VALUE, Duration.ofMinutes(5));
    }

    return value;
}
```

---

## 参考资料

- [Caffeine 官方文档](https://github.com/ben-manes/caffeine/wiki)
- [Redis 官方文档](https://redis.io/documentation)
- [Spring Cache 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)

---

**文档版本**: v1.0  
**最后更新**: 2026-03-21  
**作者**: itzixiao
