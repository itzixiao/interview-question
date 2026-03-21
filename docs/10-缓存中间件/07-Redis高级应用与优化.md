# Redis高级应用与优化

## 一、限流算法与实现

### 1.1 计数器算法

**原理**：固定时间窗口内计数，超过阈值则拒绝

**Redis 实现**：

```java
@Component
public class CounterRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 固定窗口计数限流
     */
    public boolean tryAcquire(String key, int limit, long windowSize) {
        String redisKey = "rate:limit:" + key;
        long now = System.currentTimeMillis();
        long windowStart = now - (now % windowSize);
        
        // 窗口标识
        String windowKey = redisKey + ":" + windowStart;
        
        // 核心修正：使用 setIfAbsent + increment 保证原子性
        // 1. 初始化窗口（不存在则设置，过期时间=窗口大小 +1 秒避免提前过期）
        redisTemplate.opsForValue().setIfAbsent(windowKey, "0", windowSize + 1000, TimeUnit.MILLISECONDS);
        // 2. 原子递增（避免并发问题）
        Long count = redisTemplate.opsForValue().increment(windowKey);
        
        // 缓解临界问题：叠加前一个窗口的计数（可选，牺牲部分精度降低突发风险）
        // long prevWindowStart = windowStart - windowSize;
        // String prevWindowKey = redisKey + ":" + prevWindowStart;
        // Long prevCount = redisTemplate.opsForValue().get(prevWindowKey) != null ? 
        //     Long.parseLong(redisTemplate.opsForValue().get(prevWindowKey).toString()) : 0;
        // return count + prevCount <= limit;
        
        return count != null && count <= limit;
    }
}
```

**优点**：实现简单  
**缺点**：临界问题（窗口切换时突发流量），可通过叠加前一窗口计数缓解

---

### 1.2 滑动窗口算法

**原理**：将时间窗口划分为多个小格子，每个格子独立计数

**Redis 实现（Lua 脚本原子化）**：

```java
@Component
public class SlidingWindowRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final int GRID_SIZE = 10;  // 10 个小格子
    
    public boolean tryAcquire(String key, int limit, long windowSize) {
        String redisKey = "rate:sliding:" + key;
        long now = System.currentTimeMillis();
        long gridSize = windowSize / GRID_SIZE;
        long currentGrid = now / gridSize;
        
        // 核心修正：使用 Lua 脚本原子化统计 + 新增（仅 1 次网络往返）
        String script = 
            "local total = 0 " +
            "for i = 0, ARGV[2]-1 do " +
            "   local gridKey = KEYS[1] .. ':' .. (ARGV[1] - i) " +
            "   local val = redis.call('get', gridKey) " +
            "   if val then total = total + tonumber(val) end " +
            "end " +
            "if total >= ARGV[3] then return 0 end " +
            "local currentGridKey = KEYS[1] .. ':' .. ARGV[1] " +
            "redis.call('incr', currentGridKey) " +
            "redis.call('expire', currentGridKey, ARGV[4]) " +
            "return 1";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(redisKey),
            String.valueOf(currentGrid),    // ARGV[1]：当前格子
            String.valueOf(GRID_SIZE),      // ARGV[2]：格子数
            String.valueOf(limit),          // ARGV[3]：限额
            String.valueOf(gridSize * 2)    // ARGV[4]：过期时间
        );
        
        return result != null && result == 1;
    }
}
```

**优点**：更精确，避免临界问题  
**缺点**：实现复杂，占用内存多

---

### 1.3 令牌桶算法

**原理**：固定速率产生令牌，请求获取令牌，无令牌则拒绝

**Redis 实现（Lua 脚本，含浮点精度处理）**：

```lua
-- KEYS[1]: 限流 key
-- ARGV[1]: 桶容量
-- ARGV[2]: 令牌产生速率（个/秒）
-- ARGV[3]: 当前时间戳（毫秒）
-- ARGV[4]: 请求令牌数

local key = KEYS[1]
local capacity = tonumber(ARGV[1])      -- 桶容量
local rate = tonumber(ARGV[2])          -- 速率
local now = tonumber(ARGV[3])           -- 当前时间
local requested = tonumber(ARGV[4])     -- 请求数量

-- 获取当前令牌数和时间戳
local last_tokens = tonumber(redis.call('hget', key, 'tokens')) or capacity
local last_time = tonumber(redis.call('hget', key, 'time')) or now

-- 核心修正：浮点精度处理（乘以 1000 转为整数计算）
local delta_time = math.max(0, now - last_time)
local new_tokens = last_tokens + (delta_time * rate) / 1000
new_tokens = math.min(capacity, math.floor(new_tokens * 1000) / 1000)  -- 保留 3 位小数

if new_tokens >= requested then
    -- 有足够令牌
    redis.call('hset', key, 'tokens', new_tokens - requested)
    redis.call('hset', key, 'time', now)
    -- 核心修正：设置 Hash 过期时间（1 小时），避免无效 key 永久占用内存
    redis.call('expire', key, 3600)
    return 1
else
    -- 令牌不足
    return 0
end
```

**Java 调用**：

```java
@Component
public class TokenBucketRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String SCRIPT = 
        "local key = KEYS[1] " +
        "local capacity = tonumber(ARGV[1]) " +
        "local rate = tonumber(ARGV[2]) " +
        "local now = tonumber(ARGV[3]) " +
        "local requested = tonumber(ARGV[4]) " +
        "local last_tokens = tonumber(redis.call('hget', key, 'tokens')) or capacity " +
        "local last_time = tonumber(redis.call('hget', key, 'time')) or now " +
        "local delta_time = math.max(0, now - last_time) " +
        "local new_tokens = last_tokens + (delta_time * rate) / 1000 " +
        "new_tokens = math.min(capacity, math.floor(new_tokens * 1000) / 1000) " +
        "if new_tokens >= requested then " +
        "    redis.call('hset', key, 'tokens', new_tokens - requested) " +
        "    redis.call('hset', key, 'time', now) " +
        "    redis.call('expire', key, 3600) " +
        "    return 1 " +
        "else return 0 end";
    
    public boolean tryAcquire(String key, int capacity, int rate) {
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(SCRIPT, Long.class),
            Collections.singletonList("token:bucket:" + key),
            String.valueOf(capacity),
            String.valueOf(rate),
            String.valueOf(System.currentTimeMillis()),
            "1"
        );
        
        return result != null && result == 1;
    }
}
```

**优点**：允许突发流量  
**缺点**：实现复杂，需处理浮点精度

---

### 1.4 漏桶算法

**原理**：请求流入漏桶，固定速率流出，桶满则溢出

**Redis 实现（Lua 脚本原子化）**：

```java
@Component
public class LeakyBucketRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * @param key 限流 key
     * @param capacity 桶容量
     * @param rate 流出速率（次/秒）
     */
    public boolean tryAcquire(String key, int capacity, int rate) {
        String redisKey = "leaky:bucket:" + key;
        
        // 核心修正：Lua 脚本原子化计算，避免并发安全问题
        String script = 
            "local now = tonumber(ARGV[1]) " +
            "local capacity = tonumber(ARGV[2]) " +
            "local rate = tonumber(ARGV[3]) " +
            "local last_time = tonumber(redis.call('hget', KEYS[1], 'time') or now) " +
            "local water = tonumber(redis.call('hget', KEYS[1], 'water') or 0) " +
            "local delta_time = now - last_time " +
            "local out_water = math.floor(delta_time * rate / 1000) " +
            "local current_water = math.max(0, water - out_water) " +
            "if current_water + 1 > capacity then return 0 end " +
            "redis.call('hset', KEYS[1], 'time', now) " +
            "redis.call('hset', KEYS[1], 'water', current_water + 1) " +
            "redis.call('expire', KEYS[1], 60) " +
            "return 1";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(redisKey),
            String.valueOf(System.currentTimeMillis()),
            String.valueOf(capacity),
            String.valueOf(rate)
        );
        
        return result != null && result == 1;
    }
}
```

**优点**：平滑限流  
**缺点**：无法处理突发流量

---

## 二、延时队列

### 2.1 Sorted Set 实现

**原理**：score = 执行时间戳，zrange 获取到期任务

**实现代码**：

```java
@Component
public class DelayQueue {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 添加延时任务
     */
    public void addTask(String queueName, String taskId, long delayTime) {
        long executeTime = System.currentTimeMillis() + delayTime;
        redisTemplate.opsForZSet().add(queueName, taskId, executeTime);
    }
    
    /**
     * 获取到期任务
     */
    public Set<String> getDueTasks(String queueName) {
        long now = System.currentTimeMillis();
        return redisTemplate.opsForZSet().rangeByScore(queueName, 0, now);
    }
    
    /**
     * 消费任务（轮询方式，Lua 脚本原子化）
     */
    public String pollTask(String queueName) {
        long now = System.currentTimeMillis();
        
        // 核心修正：Lua 脚本原子化获取 + 删除（避免重复消费）
        String script = 
            "local tasks = redis.call('zrangebyscore', KEYS[1], 0, ARGV[1], 'LIMIT', 0, 1) " +
            "if #tasks == 0 then return nil end " +
            "local taskId = tasks[1] " +
            "if redis.call('zrem', KEYS[1], taskId) == 1 then " +
            "   return taskId " +
            "else " +
            "   return nil " +
            "end";
        
        return (String) redisTemplate.execute(
            new DefaultRedisScript<>(script, String.class),
            Collections.singletonList(queueName),
            String.valueOf(now)
        );
    }
}
```

**消费者示例**：

```java
@Component
public class DelayQueueConsumer {
    
    @Autowired
    private DelayQueue delayQueue;
    
    @Autowired
    private TaskService taskService;
    
    @Scheduled(fixedDelay = 100)  // 100ms 检查一次（提升精度）
    public void consume() {
        String queueName = "delay:queue";
        int count = 0;
        
        while (true) {
            String taskId = delayQueue.pollTask(queueName);
            
            if (taskId == null) {
                break;  // 无任务
            }
            
            // 处理任务
            taskService.execute(taskId);
            
            // 限制单次消费数量，避免线程阻塞
            if (++count >= 100) break;
        }
    }
}
```

---

### 2.2 Redisson 实现

**使用 RDelayedQueue（异步消费）**：

```java
@Component
public class RedissonDelayQueue {
    
    @Autowired
    private RedissonClient redissonClient;
    
    // 核心修正：DelayedQueue 依赖 BlockingQueue
    private final RBlockingQueue<String> blockingQueue = 
        redissonClient.getBlockingQueue("order:delay:queue");
    private final RDelayedQueue<String> delayedQueue = 
        redissonClient.getDelayedQueue(blockingQueue);
    
    // 核心修正：异步添加任务
    public void sendOrder(String orderId, long delaySeconds) {
        CompletableFuture.runAsync(() -> 
            delayedQueue.offer(orderId, delaySeconds, TimeUnit.SECONDS)
        );
    }
    
    // 核心修正：异步消费，避免阻塞主线程
    @PostConstruct
    public void startConsume() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String orderId = blockingQueue.take();
                    processOrder(orderId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    // 异常处理：重试/记录
                    log.error("处理延时任务失败", e);
                }
            }
        }, "delay-queue-consumer").start();
    }
    
    private void processOrder(String orderId) {
        // 业务逻辑
    }
}
```

**优点**：简单易用，支持多种队列类型  
**缺点**：依赖 Redisson 框架，需异步化处理

---

## 三、分布式幂等

### 3.1 Token 机制

**流程**：

```
1. 客户端请求接口前，先获取幂等 Token
2. 请求时携带 Token 和业务 ID
3. 服务端使用 Lua 脚本原子校验并删除 Token（验证 businessId+防重放）
4. Token 只能使用一次
```

**实现代码**：

```java
@Service
public class IdempotentService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 获取幂等 Token（存储 businessId）
     */
    public String getIdempotentToken(String businessId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = "idempotent:token:" + token;
        
        // 核心修正：存储 Token + 业务 ID，过期时间 5 分钟
        redisTemplate.opsForHash().put(key, "businessId", businessId);
        redisTemplate.opsForHash().put(key, "createTime", System.currentTimeMillis());
        redisTemplate.expire(key, 300, TimeUnit.SECONDS);
        
        return token;
    }
    
    /**
     * 校验并删除 Token（Lua 脚本保证原子性 + 验证 businessId+防重放）
     */
    public boolean checkAndRemoveToken(String token, String businessId) {
        String key = "idempotent:token:" + token;
        
        String script = 
            "local bid = redis.call('hget', KEYS[1], 'businessId') " +
            "local createTime = redis.call('hget', KEYS[1], 'createTime') " +
            "if not bid or bid ~= ARGV[1] then return 0 end " +
            "-- 防重放：Token 创建超过 5 分钟失效（兜底）" +
            "if tonumber(createTime) + 300000 < tonumber(ARGV[2]) then return 0 end " +
            "redis.call('del', KEYS[1]) " +
            "return 1";
        
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(key),
            businessId,
            String.valueOf(System.currentTimeMillis())
        );
        
        return result != null && result == 1;
    }
}
```

**使用示例**：

```java
@RestController
@RequestMapping("/order")
public class OrderController {
    
    @Autowired
    private IdempotentService idempotentService;
    
    @Autowired
    private OrderService orderService;
    
    @PostMapping("/create")
    public Result<Order> createOrder(
            @RequestBody OrderRequest request,
            @RequestHeader("X-Idempotent-Token") String token) {
        
        // 核心修正：校验 Token + 业务 ID
        if (!idempotentService.checkAndRemoveToken(token, request.getBusinessId())) {
            return Result.error("重复请求或 Token 已失效");
        }
        
        // 创建订单
        Order order = orderService.create(request);
        return Result.success(order);
    }
    
    /**
     * 获取幂等 Token 接口
     */
    @GetMapping("/idempotent/token")
    public Result<String> getIdempotentToken(@RequestParam String businessId) {
        String token = idempotentService.getIdempotentToken(businessId);
        return Result.success(token);
    }
}
```

---

### 3.2 唯一索引防重

**原理**：数据库唯一索引 + Redis 前置校验

```java
@Service
public class OrderService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Transactional
    public Order create(OrderRequest request) {
        String requestId = request.getRequestId();
        String key = "order:request:" + requestId;
        
        // 核心修正：过期时间 = 事务最大执行时间 + 缓冲时间（如 5 分钟）
        Boolean absent = redisTemplate.opsForValue()
            .setIfAbsent(key, "processing", 300, TimeUnit.SECONDS);
        
        if (Boolean.FALSE.equals(absent)) {
            throw new BusinessException("重复请求");
        }
        
        try {
            // 数据库唯一索引兜底
            Order order = convert(request);
            orderMapper.insert(order);
            
            // 核心修正：同步删除（事务提交后），避免异步丢失
            redisTemplate.delete(key);
            
            return order;
        } catch (DuplicateKeyException e) {
            // 核心修正：清理锁
            redisTemplate.delete(key);
            throw new BusinessException("订单已存在");
        }
    }
}
```

---

## 四、性能优化

### 4.1 大 Key 问题

**发现大 Key**：

```bash
# 命令行工具
redis-cli --bigkeys

# 查看 key 占用内存
MEMORY USAGE user:1001
```

**解决方案**：

#### 方案 1：拆分大 Key

```java
// ❌ 错误：大 Hash 包含所有字段
HSET user:1001 field1 value1 field2 value2 ... field1000 value1000

// ✅ 正确：拆分成多个小 Hash
HSET user:1001:base name age gender
HSET user:1001:profile address bio avatar
HSET user:1001:settings theme language notifications
```

---

#### 方案 2：异步删除

```java
// ❌ 错误：同步删除大 Key（阻塞）
redisTemplate.delete("large:key");

// ✅ 正确：使用 UNLINK 异步删除（含返回值处理）
public void deleteLargeKey(String key) {
    String script = "return redis.call('unlink', KEYS[1])";
    // 核心修正：指定返回类型为 Long，处理删除失败情况
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(key)
    );
    if (result == null || result == 0) {
        log.warn("大 Key 删除失败：{}", key);
    }
}
```

---

### 4.2 热点 Key 问题

**发现热点 Key**：

```bash
# 开启监控
redis-cli --hotkeys

# 或使用 monitor 命令（生产环境慎用）
MONITOR
```

**解决方案**：

#### 方案 1：多级缓存

```java
@Component
public class MultiLevelCache {
    
    // 本地缓存（Caffeine）
    private Cache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public Object getHotKey(String key) {
        // 1. 本地缓存
        Object value = localCache.getIfPresent(key);
        if (value != null) {
            return value;
        }
        
        // 2. Redis缓存
        value = redisTemplate.opsForValue().get(key);
        if (value != null) {
            localCache.put(key, value);  // 回填本地缓存
            return value;
        }
        
        return null;
    }
}
```

---

#### 方案 2：副本分散

```java
// 写入副本（Lua 脚本原子化，保证一致性）
public void setHotKeyReplica(String hotKey, Object value, int replicaCount) {
    String script = 
        "for i = 0, ARGV[2]-1 do " +
        "   local replicaKey = KEYS[1] .. ':replica:' .. i " +
        "   redis.call('set', replicaKey, ARGV[1]) " +
        "   redis.call('expire', replicaKey, ARGV[3]) " +
        "end " +
        "return 1";
    
    redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(hotKey),
        JSON.toJSONString(value),
        String.valueOf(replicaCount),
        String.valueOf(3600) // 过期时间
    );
}

// 读取副本
public Object getHotKeyReplica(String hotKey, int replicaCount) {
    int randomIndex = ThreadLocalRandom.current().nextInt(replicaCount);
    String replicaKey = hotKey + ":replica:" + randomIndex;
    return redisTemplate.opsForValue().get(replicaKey);
}
```

---

### 4.3 Pipeline 批量操作

**使用示例**：

```java
// ❌ 错误：多次网络往返
for (int i = 0; i < 1000; i++) {
    redisTemplate.opsForValue().set("key:" + i, "value:" + i);
}

// ✅ 正确：Pipeline 一次性发送
redisTemplate.executePipelined((RedisCallback<?>) connection -> {
    for (int i = 0; i < 1000; i++) {
        connection.set(("key:" + i).getBytes(), 
                      ("value:" + i).getBytes());
    }
    return null;
});
```

---

### 4.4 内存淘汰策略

**配置方式**：

```bash
# redis.conf
maxmemory 4gb
maxmemory-policy allkeys-lru
```

**策略选择**：

| 淘汰策略类型           | 策略名称                   | 核心逻辑                                  |
|------------------|------------------------|---------------------------------------|
| **不淘汰数据**        | `noeviction`           | 内存满时拒绝写入，返回错误，默认策略                    |
| **从所有键中淘汰**      | `allkeys-lru`          | 从所有键中选择最近最少使用的键淘汰                     |
| **从所有键中淘汰**      | `allkeys-random`       | 从所有键中随机选择键淘汰                          |
| **从所有键中淘汰**      | `allkeys-lfu`          | 从所有键中选择最近最不频繁使用的键淘汰（Redis 4.0+）       |
| **仅从设过期时间的键中淘汰** | `volatile-lru`         | 从设置了过期时间的键中选择最近最少使用的键淘汰               |
| **仅从设过期时间的键中淘汰** | `volatile-random`      | 从设置了过期时间的键中随机选择键淘汰                    |
| **仅从设过期时间的键中淘汰** | `volatile-lfu`         | 从设置了过期时间的键中选择最近最不频繁使用的键淘汰（Redis 4.0+） |
| **仅从设过期时间的键中淘汰** | `volatile-ttl`         | 从设置了过期时间的键中选择剩余 TTL 最短的键淘汰            |
| **仅从设过期时间的键中淘汰** | `volatile-justexpired` | 仅淘汰已过期的键，若没有则不淘汰                      |

**使用建议**：

| 场景           | 推荐策略           | 说明              |
|--------------|----------------|-----------------|
| **缓存场景（推荐）** | `allkeys-lru`  | 自动淘汰冷数据，保留热数据   |
| **热点数据缓存**   | `allkeys-lfu`  | 基于访问频率，避免偶发访问误判 |
| **部分数据可过期**  | `volatile-lru` | 只淘汰有过期时间的数据     |
| **TTL 分级场景** | `volatile-ttl` | 优先淘汰即将过期的数据     |
| **数据极其重要**   | `noeviction`   | 宁可报错也不丢失数据      |

---

## 五、高频面试题

### 问题 1:Redis 如何实现限流？常见算法有哪些？

**参考答案**：

**常见限流算法**：

| 算法   | 原理       | 特点        |
|------|----------|-----------|
| 计数器  | 固定窗口计数   | 简单，但有临界问题 |
| 滑动窗口 | 多个小格子    | 更精确，占用内存多 |
| 令牌桶  | 固定速率产生令牌 | 允许突发流量    |
| 漏桶   | 固定速率处理   | 平滑限流，无法突发 |

**推荐**：令牌桶算法（平衡性能和灵活性）

---

### 问题 2:Redis 如何实现延时队列？

**参考答案**：

**Sorted Set 实现**：

- score = 执行时间戳
- zrange 获取到期任务
- zrem 删除已处理任务

**Redisson 实现**：

- RDelayedQueue
- 内部使用 DelayedQueue + BlockingQueue

---

### 问题 3:Redis 如何实现分布式幂等？

**参考答案**：

**Token 机制**：

1. 请求前获取幂等 Token
2. 请求时携带 Token
3. 服务端 Lua 脚本原子校验并删除

**唯一索引**：

- Redis 前置校验
- 数据库唯一索引兜底

---

### 问题 4：如何解决 Redis 大 Key 问题？

**参考答案**：

**发现方式**：

- `redis-cli --bigkeys`
- `MEMORY USAGE key`

**解决方案**：

1. **拆分**：大 Hash 拆成多个小 Hash
2. **异步删除**：使用 `UNLINK` 代替 `DEL`
3. **批量操作**：分批处理，避免一次性操作
4. **压缩**：对 value 进行压缩

---

### 问题 5：如何处理热点 Key 问题？

**参考答案**：

**发现方式**：

- `redis-cli --hotkeys`
- 监控系统分析

**解决方案**：

1. **多级缓存**：本地缓存 + Redis
2. **副本分散**：创建多个副本，随机读取
3. **永不过期**：热点 Key 不设置过期时间
4. **提前预热**：活动开始前加载到缓存

---

## 六、最佳实践

### 6.1 Key 设计规范

```bash
# ✅ 推荐
user:1001:info
order:20240101:detail
seckill:product:1001:stock

# ❌ 不推荐
user_1001
u1001
key1
```

### 6.2 使用规范

1. **设置过期时间**：避免脏数据永久存在
2. **控制 Key 大小**：不超过 1KB
3. **控制 Value 大小**：不超过 10KB
4. **批量操作**：使用 Pipeline/MGET
5. **禁用危险命令**：KEYS、FLUSHALL、FLUSHDB

### 6.3 监控告警

```bash
# 关键指标
- 内存使用率 > 80%
- QPS 突增/突降
- 响应时间 > 10ms
- 连接数 > 80%
- 主从延迟 > 10 秒
- 缓存命中率 < 90%
```

---

## 七、总结

| 应用场景 | 实现方案                             |
|------|----------------------------------|
| 限流   | 计数器、滑动窗口、令牌桶、漏桶                  |
| 延时队列 | Sorted Set、Redisson DelayedQueue |
| 幂等   | Token 机制、唯一索引                    |
| 性能优化 | 拆分大 Key、多级缓存、Pipeline            |
| 内存管理 | 合理设置淘汰策略、定期清理                    |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15
