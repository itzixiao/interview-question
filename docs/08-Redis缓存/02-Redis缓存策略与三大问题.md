# Redis缓存策略与三大问题

## 一、缓存更新策略

### 1.1 Cache Aside（旁路缓存）⭐推荐

**读操作**：
```
1. 先读缓存
2. 缓存未命中，读数据库
3. 将数据写入缓存
4. 返回结果
```

**写操作**：
```
1. 先更新数据库
2. 再删除缓存（推荐删除而非更新：避免并发更新导致缓存脏数据，删除后下次读取自动重建）
```

**优点**：
- 实现简单
- 容错性好
- 缓存不会脏读

**缺点**：
- 存在短暂不一致（写操作删除缓存前，读操作可能读取旧数据）
- 高并发下可能丢更新（极端场景：读请求未命中缓存 → 查询旧数据 → 写请求更新 DB 并删除缓存 → 读请求将旧数据写入缓存）

**解决方案**：延迟双删、加分布式锁、Canal 监听 Binlog

**代码示例**：
```java
// 读缓存
public User getUserById(Long id) {
    String key = "user:" + id;
    User user = redisTemplate.get(key);
    
    if (user == null) {
        // 缓存未命中，查询数据库
        user = userMapper.selectById(id);
        
        if (user != null) {
            // 写入缓存（设置过期时间）
            redisTemplate.setex(key, 3600, user);
        } else {
            // 缓存空值，防止穿透
            redisTemplate.setex(key, 300, null);
        }
    }
    
    return user;
}
```

---

### 1.2 Read/Write Through（读写穿透）

**原理**：
- 读写都经过缓存
- 缓存系统负责与数据库同步
- 应用层无需关心缓存逻辑

**优点**：
- 应用层代码简洁
- 缓存与数据库强一致

**缺点**：
- 实现复杂
- 需要缓存层支持（如 Spring Cache）

---

### 1.3 Write Behind（异步回写）

**原理**：
- 先写缓存
- 缓存系统异步批量写入数据库

**优点**：
- 写性能高
- 合并多次写操作

**缺点**：
- 数据安全性低（缓存宕机丢失数据）
- 实现复杂

**适用场景**：
- 对数据一致性要求不高
- 高频写操作（如计数器）

---

## 二、缓存三大问题

### 2.1 缓存穿透 🔴

**定义**：查询不存在的数据，请求绕过缓存直达数据库。

```
用户请求 → 缓存（未命中）→ 数据库（查询为空）
                ↓
         大量请求打到数据库
```

**产生原因**：
- 查询的 key 不存在
- 恶意攻击（故意查询不存在的 ID）

**解决方案**：

#### 方案 1：缓存空值

```java
public User getUserById(Long id) {
    String key = "user:" + id;
    User user = redisTemplate.get(key);
    
    if (user == null) {
        // 查询数据库
        user = userMapper.selectById(id);
        
        if (user == null) {
            // 缓存空值（设置短过期时间，避免内存浪费）
            redisTemplate.setex(key, 300, null); // 直接缓存 null 而非空对象，更节省内存
            return null;
        }
        
        // 缓存正常数据
        redisTemplate.setex(key, 3600, user);
    }
    
    return user; // 修正：返回正确的 user 对象
}
```

**优点**：实现简单  
**缺点**：占用内存，可能存在短期不一致

---

#### 方案 2：布隆过滤器

**使用流程**：先查布隆过滤器，再查缓存/数据库；数据写入时同步添加。

```java
@Component
public class BloomFilterUtil {
    
    @Autowired
    private RedissonClient redissonClient;
    
    private RBloomFilter<String> bloomFilter;
    
    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("userFilter");
        // 初始化：期望插入 100 万个元素，误判率 3%
        bloomFilter.tryInit(1000000L, 0.03);
    }
    
    /**
     * 检查用户是否存在
     */
    public boolean contains(Long userId) {
        return bloomFilter.contains("user:" + userId);
    }
    
    /**
     * 添加用户（数据写入时调用）
     */
    public void add(Long userId) {
        bloomFilter.add("user:" + userId);
    }
}

// 使用示例
@Service
public class UserService {
    
    @Autowired
    private BloomFilterUtil bloomFilterUtil;
    
    public User getUserById(Long id) {
        // 1. 布隆过滤器快速判断是否存在
        if (!bloomFilterUtil.contains(id)) {
            return null; // 肯定不存在
        }
        
        // 2. 查缓存
        String key = "user:" + id;
        User user = redisTemplate.get(key);
        if (user != null) {
            return user;
        }
        
        // 3. 查数据库
        user = userMapper.selectById(id);
        if (user != null) {
            redisTemplate.setex(key, 3600, user);
        }
        
        return user;
    }
}
```

**优点**：节省内存，过滤效果好  
**缺点**：有误判率，需要额外维护

---

#### 方案 3：参数校验

```java
@GetMapping("/user/{id}")
public Result<User> getUser(@PathVariable Long id) {
    // 入口校验：ID 必须为正数
    if (id == null || id <= 0) {
        throw new BusinessException("用户 ID 不合法");
    }
    
    User user = userService.getUserById(id);
    return Result.success(user);
}
```

**优点**：简单有效，从源头拦截  
**缺点**：只能拦截明显非法参数

---

### 2.2 缓存击穿 🟡

**定义**：热点 key 过期瞬间，大量并发请求打到数据库。

```
热点 key 过期
    ↓
大量并发请求 → 缓存（失效）→ 数据库
                ↓
         数据库压力剧增
```

**产生原因**：
- 热点 key 过期
- 高并发访问

**解决方案**：

#### 方案 1：互斥锁（推荐）

```java
public User getUserById(Long id) {
    String key = "user:" + id;
    User user = redisTemplate.get(key);
    
    if (user == null) {
        // 尝试获取分布式锁
        String lockKey = "lock:user:" + id;
        if (tryLock(lockKey)) {
            try {
                // 双重检查
                user = redisTemplate.get(key);
                if (user == null) {
                    // 查询数据库
                    user = userMapper.selectById(id);
                    
                    if (user != null) {
                        // 写入缓存
                        redisTemplate.setex(key, 3600, user);
                    } else {
                        // 缓存空值
                        redisTemplate.setex(key, 300, new User());
                    }
                }
            } finally {
                unlock(lockKey);
            }
        } else {
            // 获取锁失败，等待后重试
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return getUserById(id);  // 递归重试
        }
    }
    
    return user;
}

// 分布式锁实现（带 ThreadLocal 保存锁值）
private ThreadLocal<String> lockValueHolder = new ThreadLocal<>();

private boolean tryLock(String key) {
    String lockValue = UUID.randomUUID().toString(); // 生成唯一值
    String script = 
        "if redis.call('setnx', KEYS[1], ARGV[1]) == 1 then " +
        "   redis.call('expire', KEYS[1], ARGV[2]); " +
        "   return 1; " +
        "else " +
        "   return 0; " +
        "end";
    
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(key),
        lockValue, // 传入唯一值
        "30"
    );
    
    if (result != null && result == 1) {
        // 保存锁值到 ThreadLocal，用于解锁
        lockValueHolder.set(lockValue);
        return true;
    }
    return false;
}

private void unlock(String key) {
    String lockValue = lockValueHolder.get();
    if (lockValue == null) return;
    
    String script = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "   return redis.call('del', KEYS[1]); " +
        "else " +
        "   return 0; " +
        "end";
    
    redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(key),
        lockValue // 使用加锁时的唯一值
    );
    lockValueHolder.remove(); // 清空 ThreadLocal，防止内存泄漏
}
```

**优点**：保证只有一个线程重建缓存  
**缺点**：实现复杂，有死锁风险

---

#### 方案 2：逻辑过期

**适用场景**：秒杀、热点商品等极致性能场景，允许短期不一致。

```java
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CacheWrapper<T> {
    private T data;           // 实际数据
    private long expireTime;  // 逻辑过期时间
}

public User getUserById(Long id) {
    String key = "user:" + id;
    CacheWrapper<User> wrapper = redisTemplate.get(key);
    
    if (wrapper == null) {
        // 缓存未命中，重建（加锁）
        return rebuildCache(id);
    }
    
    // 检查是否逻辑过期
    if (System.currentTimeMillis() > wrapper.getExpireTime()) {
        // 已过期，异步重建（不阻塞请求）
        rebuildCacheAsync(id);
    }
    
    return wrapper.getData();
}

private User rebuildCache(Long id) {
    String lockKey = "lock:user:" + id;
    
    // 尝试获取锁
    if (!tryLock(lockKey)) {
        // 获取锁失败，重试或返回兜底数据
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return rebuildCache(id); // 递归重试
    }
    
    try {
        // 双重检查：防止锁等待期间缓存已被重建
        CacheWrapper<User> wrapper = redisTemplate.get("user:" + id);
        if (wrapper != null && System.currentTimeMillis() <= wrapper.getExpireTime()) {
            return wrapper.getData();
        }
        
        // 查询数据库重建缓存
        User user = userMapper.selectById(id);
        CacheWrapper<User> newWrapper = new CacheWrapper<>(user, 
            System.currentTimeMillis() + 7200000);  // 2 小时后过期
        redisTemplate.set("user:" + id, newWrapper);
        return user;
    } finally {
        unlock(lockKey); // 释放锁
    }
}

private void rebuildCacheAsync(Long id) {
    // 使用线程池异步重建
    executorService.submit(() -> {
        String lockKey = "lock:user:" + id;
        if (tryLock(lockKey)) {
            try {
                rebuildCache(id);
            } finally {
                unlock(lockKey);
            }
        }
    });
}
```

**优点**：无需阻塞请求，性能高（适合秒杀、热点商品等极致性能场景）  
**缺点**：实现复杂，存在短期不一致（过期后仍返回旧数据，直到异步重建完成）

---

#### 方案 3：热点 Key 永不过期

```java
// 对热点数据不设置过期时间
if (isHotKey(id)) {
    redisTemplate.set("user:" + id, user);  // 永不过期
} else {
    redisTemplate.setex("user:" + id, 3600, user);  // 普通数据 1 小时过期
}

private boolean isHotKey(Long id) {
    // 根据访问频率判断
    return hotKeySet.contains(id);
}
```

**优点**：简单直接  
**缺点**：需要识别热点 Key，数据更新需手动处理

---

### 2.3 缓存雪崩 🟠

**定义**：大量 key 同时过期，或 Redis 宕机，导致请求全部打到数据库。

```
情况 1：大量 key 同时过期
用户请求 → 缓存（大面积失效）→ 数据库
                ↓
         数据库崩溃

情况 2:Redis 宕机
用户请求 → Redis（不可用）→ 数据库
                ↓
         数据库崩溃
```

**产生原因**：
- 大量 key 设置相同的过期时间
- Redis 节点故障

**解决方案**：

#### 方案 1：随机过期时间

```java
// 基础过期时间 + 随机值（单位：秒）
int baseExpire = 3600;  // 1 小时
int randomExpire = ThreadLocalRandom.current().nextInt(300);  // 0-5 分钟（300 秒）
int expireTime = baseExpire + randomExpire;

redisTemplate.opsForValue().set("user:" + id, user, expireTime, TimeUnit.SECONDS); // 明确时间单位
```

**优点**：简单有效  
**缺点**：效果有限

---

#### 方案 2：多级缓存

@Component
public class MultiLevelCache {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private UserMapper userMapper; // 补充注入
    
    // 本地缓存（Caffeine）
    private Cache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    // 分布式锁方法（同前文）
    private ThreadLocal<String> lockValueHolder = new ThreadLocal<>();
    
    private boolean tryLock(String key) {
        // 实现同前文互斥锁
        return true; // 简化示例
    }
    
    private void unlock(String key) {
        // 实现同前文
    }
    
    public User getUserById(Long id) {
        String key = "user:" + id;
        
        // 1. 查本地缓存
        User user = (User) localCache.getIfPresent(key);
        if (user != null) {
            return user;
        }
        
        // 2. 查 Redis
        user = (User) redisTemplate.opsForValue().get(key);
        if (user != null) {
            // 回填本地缓存
            localCache.put(key, user);
            return user;
        }
        
        // 3. 查数据库（加锁避免击穿）
        String lockKey = "lock:user:" + id;
        if (tryLock(lockKey)) {
            try {
                // 双重检查
                user = (User) redisTemplate.opsForValue().get(key);
                if (user != null) {
                    localCache.put(key, user);
                    return user;
                }
                
                user = userMapper.selectById(id);
                if (user != null) {
                    // 填充两级缓存
                    redisTemplate.opsForValue().set(key, user, 3600, TimeUnit.SECONDS);
                    localCache.put(key, user);
                }
            } finally {
                unlock(lockKey);
            }
        } else {
            // 兜底：返回本地缓存或默认值
            return (User) localCache.getIfPresent(key);
        }
        
        return user;
    }
}
```

**依赖引入**（pom.xml）：
```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>
```

**优点**：分散风险，提高可用性  
**缺点**：实现复杂，占用内存

---

#### 方案 3：熔断降级

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @SentinelResource(value = "getUserById", 
                      blockHandler = "handleBlockException",
                      fallback = "handleFallback")
    public User getUserById(Long id) {
        String key = "user:" + id;
        
        // 查缓存
        User user = (User) redisTemplate.opsForValue().get(key);
        if (user != null) {
            return user;
        }
        
        // 查数据库
        user = userMapper.selectById(id);
        if (user != null) {
            redisTemplate.opsForValue().set(key, user, 3600, TimeUnit.SECONDS);
        }
        
        return user;
    }
    
    // 限流降级
    public User handleBlockException(Long id, BlockException ex) {
        log.warn("触发限流降级：{}", id, ex);
        return getLocalCache(id);
    }
    
    // 异常降级
    public User handleFallback(Long id, Throwable ex) {
        log.error("服务异常降级：{}", id, ex);
        return getLocalCache(id);
    }
    
    private User getLocalCache(Long id) {
        // 返回本地缓存或默认值
        return new User(id, "default");
    }
}
```

**优点**：保护数据库，提高系统韧性  
**缺点**：需要引入 Sentinel/Hystrix

---

#### 方案 4：高可用架构

```bash
# Redis Cluster + Sentinel 配置
# 6 个节点（3 主 3 从）
192.168.1.10:7000  # Master 1
192.168.1.11:7001  # Master 2
192.168.1.12:7002  # Master 3
192.168.1.10:7003  # Slave 1
192.168.1.11:7004  # Slave 2
192.168.1.12:7005  # Slave 3
```

**优点**：从根本上避免单点故障  
**缺点**：运维复杂，成本高

---

## 三、缓存一致性

### 3.1 延迟双删

**流程**：
```
1. 删除缓存
2. 更新数据库
3. 延迟 **大于主从复制延迟 + 业务操作耗时**（如 1-3s）
4. 再次删除缓存
```

**目的**：删除读取缓存时写入的旧数据

**代码示例**：
```java
public void updateUser(User user) {
    String key = "user:" + user.getId();
    
    // 1. 删除缓存
    redisTemplate.delete(key);
    
    // 2. 更新数据库
    userMapper.updateById(user);
    
    // 3. 延迟删除（延迟时间建议：Redis主从同步延迟 + 业务最大耗时）
    CompletableFuture.runAsync(() -> {
        try {
            Thread.sleep(1000); // 推荐 1-3s，根据实际环境调整
            redisTemplate.delete(key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });
}
```

**优点**：简单易实现  
**缺点**：延迟时间难以精确控制

---

### 3.2  Canal 监听 Binlog

**原理**：
- 通过 Canal 监听 MySQL Binlog
- 解析后自动删除/更新 Redis缓存

**优点**：解耦业务代码，保证最终一致性  
**缺点**：架构复杂，需要额外维护 Canal

**Sentinel 熔断降级配置示例**（application.yml）：
```yaml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080
      rules:
        flow:
          - resource: getUserById
            grade: QPS
            count: 1000  # QPS 阈值
        degrade:
          - resource: getUserById
            grade: EXCEPTION_RATIO
            count: 0.5   # 异常率 50% 触发降级
            timeWindow: 10 # 降级时间 10 秒
```

---

## 四、高频面试题

### 问题 1：什么是缓存穿透？如何解决？

**参考答案**：

**定义**：查询不存在的数据，请求绕过缓存直达数据库。

**解决方案**：
1. **缓存空值**：设置短过期时间
2. **布隆过滤器**：在缓存前加一层过滤
3. **参数校验**：入口处拦截非法请求

---

### 问题 2：什么是缓存击穿？如何解决？

**参考答案**：

**定义**：热点 key 过期瞬间，大量并发请求打到数据库。

**解决方案**：
1. **互斥锁**：只允许一个线程重建缓存
2. **逻辑过期**：缓存永不过期，后台异步更新
3. **热点 Key 永不过期**：对热点数据不设置过期时间

---

### 问题 3：什么是缓存雪崩？如何解决？

**参考答案**：

**定义**：大量 key 同时过期，或 Redis 宕机，导致请求全部打到数据库。

**解决方案**：
1. **随机过期时间**：在基础过期时间上加随机值
2. **多级缓存**：本地缓存 + Redis
3. **熔断降级**：使用 Sentinel/Hystrix
4. **高可用架构**：Redis Cluster + Sentinel

---

### 问题 4：如何保证缓存与数据库的一致性？

**参考答案**：

**Cache Aside 模式（推荐）**：
- **读**：先读缓存，未命中读 DB，写入缓存（数据库为空时缓存 null）
- **写**：先更新 DB，再删除缓存

**为什么是删除而不是更新缓存？**
- 更新缓存可能导致并发覆盖（线程 A 更新 DB，线程 B 同时更新 DB，线程 B 先更新缓存，线程 A 后更新缓存，导致缓存脏数据）
- 删除更简单，下次读取时重建

**风险**：高并发下仍可能出现短暂不一致（读请求在写请求删除缓存前读取了旧数据），可通过延迟双删/Canal 弥补。

**高级方案**：
- 延迟双删（延迟时间 > 主从复制延迟 + 业务耗时）
- Canal 监听 Binlog（解耦业务代码）

---

## 五、最佳实践

### 5.1 缓存设计原则

1. **明确缓存目的**：不是所有数据都需要缓存
2. **合理设置过期时间**：避免永不过期或过短
3. **控制缓存粒度**：不要过大或过小
4. **考虑并发场景**：击穿、雪崩、穿透
5. **监控告警**：
   - 核心指标：缓存命中率（Hit Rate = 命中数 / 总请求数）、内存使用率、过期 key 数
   - 告警阈值：命中率 < 80% 告警，内存使用率 > 90% 告警
   - 工具：Redis CLI（`INFO stats`）、Prometheus + Grafana

### 5.2 缓存使用建议

```java
// ✅ 推荐做法
// 1. 设置合理的过期时间
redisTemplate.setex(key, 3600 + random.nextInt(300), value);

// 2. 使用互斥锁处理热点 Key
if (value == null && tryLock(lockKey)) {
    // 重建缓存
}

// 3. 批量操作使用 Pipeline
redisTemplate.executePipelined((RedisCallback<?>) connection -> {
    // 批量操作
});

// ❌ 不推荐
// 1. 永不过期（除非特殊场景）
redisTemplate.set(key, value);

// 2. 大 Key 存储
redisTemplate.set(key, largeObject);  // 对象不要超过 10KB

// 3. 不加锁处理并发
if (value == null) {
    value = db.query();  // 多线程同时查询
    redisTemplate.set(key, value);
}
```

---

## 六、总结

| 问题 | 定义 | 解决方案 |
|------|------|----------|
| 缓存穿透 | 查询不存在的数据 | 缓存空值、布隆过滤器、参数校验 |
| 缓存击穿 | 热点 key 过期 | 互斥锁、逻辑过期、热点 Key 永不过期 |
| 缓存雪崩 | 大量 key 同时过期/Redis 宕机 | 随机过期时间、多级缓存、熔断降级、高可用 |
| 缓存一致性 | 缓存与数据库数据不一致 | Cache Aside、延迟双删、Canal 监听 |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15
