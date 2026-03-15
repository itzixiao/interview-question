# Redis 分布式锁详解

## 一、分布式锁概述

### 1.1 为什么需要分布式锁？

在分布式系统中，多个进程/线程可能同时访问共享资源，需要一种机制来保证同一时刻只有一个进程能访问该资源。

**典型场景**：
- 秒杀系统：防止超卖
- 定时任务：避免重复执行
- 库存扣减：保证数据一致性
- 订单处理：防止重复下单

---

### 1.2 分布式锁实现方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **Redis** | 性能好、实现简单 | 主从切换可能丢锁（可通过 RedLock 改进） | 对性能要求高，允许短暂不一致 |
| **ZooKeeper** | 可靠性高、强一致 | 性能较低、实现复杂 | 对一致性要求高 |
| **数据库** | 简单、易理解 | 性能差、单点故障 | 低频场景 |
| **etcd** | 强一致、高可用 | 运维复杂 | 云原生场景 |

---

## 二、Redis 分布式锁的演进

### 2.1 基础版本：SETNX + EXPIRE

**实现原理**：
```bash
# 加锁
SETNX lock:key value
EXPIRE lock:key 30

# 解锁
DEL lock:key
```

**Java 代码示例**：
```java
public boolean tryLock(String key, String value, long expireTime) {
    // 尝试获取锁（指定超时时间，避免阻塞）
    Boolean result = redisTemplate.opsForValue()
        .setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);
    
    // 处理 null：Redis 连接异常时返回 null
    return Boolean.TRUE.equals(result);
}

// 解锁方法补充锁归属校验（避免误删）
public boolean unlock(String key, String value) {
    String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(key),
        value
    );
    return result != null && result == 1;
}
```

**存在的问题**：
1. **非原子操作**：SETNX 和 EXPIRE 分开执行，中间可能宕机
2. **误删锁**：A 线程的锁过期后，B 线程获取锁，A 线程恢复后删除了 B 的锁
3. **不可重入**：同一线程无法多次获取同一把锁
4. **无阻塞等待**：获取失败后需自己实现重试

---

### 2.2 改进版本：SET NX EX（原子操作）

**实现原理**：
```bash
# Redis 2.6.12+ 支持
SET lock:key unique_value NX EX 30
```

**参数说明**：
- `NX`：Not Exists，只有不存在时才设置
- `EX 30`：过期时间 30 秒
- `unique_value`：唯一标识（UUID + 线程 ID）

**Java 代码示例**：
```java
public boolean tryLock(String key, String value, long expireTime) {
    // Redis 2.6.12+ 支持 SET NX EX，RedisTemplate 正确写法
    return Boolean.TRUE.equals(
        redisTemplate.opsForValue()
            .set(key, value, expireTime, TimeUnit.SECONDS, 
                 RedisStringCommands.SetOption.ifAbsent()) // 等价于 NX
    );
}

// 解锁必须用 Lua脚本，避免误删
public boolean unlock(String key, String value) {
    String script = """
        if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
        else
            return 0
        end
        """;
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList(key),
        value
    );
    return result != null && result == 1;
}
```

**仍存在的问题**：
1. **锁过期业务未完成**：业务执行时间超过 30 秒，锁自动释放
2. **不可重入**
3. **主从切换丢锁**：主节点写入锁后宕机，从节点未同步

---

### 2.3 完善版本：Redisson（推荐）⭐

**Redisson 特点**：
- ✅ 可重入（记录线程标识和重入次数）
- ✅ 自动续期（看门狗机制）
- ✅ 支持阻塞等待
- ✅ 支持公平锁、读写锁、红锁

**Maven 依赖**：
```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.23.5</version>
</dependency>
```

**基本使用**：
```java
@Autowired
private RedissonClient redissonClient;

public void doBusiness() {
    RLock lock = redissonClient.getLock("myLock");
    
    // 方式 1：阻塞式加锁（启用看门狗）
    lock.lock(); // 无返回值，阻塞直到获取锁
    try {
        // 执行业务逻辑
        businessLogic();
    } finally {
        lock.unlock();
    }
    
    // 方式 2：非阻塞式加锁（推荐）
    /*
    if (lock.tryLock(10, TimeUnit.SECONDS)) { // 等待 10 秒，获取后无过期时间（看门狗）
        try {
            businessLogic();
        } finally {
            lock.unlock();
        }
    } else {
        throw new BusinessException("获取锁失败");
    }
    */
}
```

---

## 三、Redisson 核心机制

### 3.1 可重入原理

**数据结构**：
```lua
{
  "lock_key": {
    "redisson_lock__channel:{lock_key}:threadId:counter": 1  -- 实际存储结构
    -- 简化理解："thread_unique_id": 1（线程唯一标识：重入次数）
  }
}
```
> 补充：Redisson 实际存储的 Field 包含客户端 ID + 线程 ID，确保分布式环境下线程唯一。

**实现逻辑**：
```java
// 伪代码
if (lock.exists() && lock.hexists(threadId)) {
    // 同一线程，重入次数 +1
    lock.hincr(threadId, 1);
    renewExpire();
    return true;
}
```

---

### 3.2 看门狗机制（WatchDog）⭐⭐⭐

**问题背景**：
- 业务执行时间超过锁过期时间 → 锁自动释放 → 其他线程进入 → 并发问题

**看门狗解决方案**：
```
获取锁成功
    ↓
启动看门狗线程
    ↓
每隔 10 秒检查（过期时间的 1/3）
    ↓
业务未完成 → 自动续期到 30 秒
    ↓
业务完成 → 停止看门狗，释放锁
```

**配置参数**：
```java
Config config = new Config();
config.useSingleServer()
    .setAddress("redis://127.0.0.1:6379");

// 看门狗相关配置
config.setLockWatchdogTimeout(30000);  // 锁的默认过期时间（默认 30 秒），续期时重置为该值
config.setTimeout(3000);  // Redis 命令执行超时时间（毫秒）

RedissonClient redisson = Redisson.create(config);
```
> 补充说明：看门狗的**续期间隔**是 `lockWatchdogTimeout / 3`（默认 10 秒），而非配置的 30 秒。

**重要提示**：
```java
// ✅ 正确：不指定过期时间，启用看门狗（默认 30 秒，每 10 秒续期）
RLock lock = redisson.getLock("myLock");
lock.lock();  // 看门狗自动续期

// ❌ 错误 1：指定过期时间，禁用看门狗
lock.tryLock(10, 30, TimeUnit.SECONDS);  // 30 秒后锁过期，看门狗不工作

// ❌ 错误 2：lock() 带过期时间，禁用看门狗
lock.lock(30, TimeUnit.SECONDS); // 30 秒后锁过期，无续期
```

---

### 3.3 锁释放机制

**Lua 脚本保证原子性**：
```lua
-- 判断是否是当前线程
if redis.call('hexists', KEYS[1], ARGV[1]) == 1 then
    -- 重入次数 -1
    local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1)
    
    if counter == 0 then
        -- 重入次数为 0，删除锁
        redis.call('del', KEYS[1])
        return 1
    else
        -- 更新过期时间
        redis.call('pexpire', KEYS[1], ARGV[2])
        return 0
    end
end

return 0
```

**Java 代码**：
```java
public void unlock(RLock lock) {
    try {
        lock.unlock();
    } catch (IllegalMonitorStateException e) {
        // 锁已过期或不属于当前线程
        log.warn("锁已过期：{}", e.getMessage());
    }
}
```

---

## 四、分布式锁的高级应用

### 4.1 公平锁

**特点**：按照请求顺序依次获取锁

**使用示例**：
```java
RLock fairLock = redisson.getFairLock("fairLock");

if (fairLock.lock()) {
    try {
        // 业务逻辑
    } finally {
        fairLock.unlock();
    }
}
```

**底层实现**：
- 使用 LinkedList 存储等待线程
- 按 FIFO 顺序唤醒

---

### 4.2 读写锁

**特点**：
- 读锁共享（多个线程可同时读）
- 写锁独占（写时禁止读写）

**使用示例**：
```java
RReadWriteLock rwLock = redisson.getReadWriteLock("rwLock");

// 读锁
RLock readLock = rwLock.readLock();
readLock.lock();
try {
    // 读取数据
} finally {
    readLock.unlock();
}

// 写锁
RLock writeLock = rwLock.writeLock();
writeLock.lock();
try {
    // 写入数据
} finally {
    writeLock.unlock();
}
```

---

### 4.3 联锁（MultiLock）

**场景**：同时获取多把锁，要么都成功，要么都失败

**使用示例**：
```java
RLock lock1 = redisson.getLock("lock1");
RLock lock2 = redisson.getLock("lock2");
RLock lock3 = redisson.getLock("lock3");

RedissonMultiLock multiLock = new RedissonMultiLock(lock1, lock2, lock3);

if (multiLock.lock()) {
    try {
        // 执行业务
    } finally {
        multiLock.unlock();
    }
}
```

---

### 4.4 红锁（RedLock）

**场景**：多 Redis 节点部署，提高可靠性

**算法原理**：
```
1. 向 N 个独立的 Redis 节点申请锁
2. 计算总耗时
3. 多数节点（N/2+1）成功且耗时 < 过期时间，则获取锁成功
4. 释放时向所有节点发送释放命令
```

**使用示例**：
```java
// 创建多个 Redis 客户端（连接不同节点）
RedissonClient client1 = Redisson.create(config1); // 节点 1
RedissonClient client2 = Redisson.create(config2); // 节点 2
RedissonClient client3 = Redisson.create(config3); // 节点 3

// 所有节点使用相同的锁 key
RLock lock1 = client1.getLock("myRedLock");
RLock lock2 = client2.getLock("myRedLock");
RLock lock3 = client3.getLock("myRedLock");

// 创建红锁
RedissonRedLock redLock = new RedissonRedLock(lock1, lock2, lock3);

// 加锁：等待 100 毫秒，锁过期时间 30 秒（禁用看门狗）
if (redLock.tryLock(100, 30, TimeUnit.SECONDS)) {
    try {
        // 执行业务
    } finally {
        redLock.unlock();
    }
}
```

**争议与建议**：
- Martin Kleppmann 指出 RedLock 存在以下风险：
  1. 时钟漂移：节点时间不一致导致锁提前过期；
  2. 网络延迟：部分节点加锁成功但响应超时；
  3. 脑裂：主从切换后，旧主节点仍持有锁；
- Redis 作者 antirez 认为这些场景概率极低，RedLock 可满足大部分场景；
- **建议**：
  - 普通场景：使用单机 Redisson 锁 + 主从复制；
  - 高可靠场景：使用 RedLock（3-5 个独立节点）；
  - 极致一致性场景：使用 ZooKeeper/etcd。

---

## 五、分布式锁实战场景

### 5.1 秒杀系统防超卖

```java
@Service
public class SeckillService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Autowired
    private ProductMapper productMapper;
    
    public void seckill(Long productId, Long userId) {
        RLock lock = redissonClient.getLock("seckill:lock:" + productId);
        
        if (lock.tryLock(10, TimeUnit.SECONDS)) {
            try {
                // 分布式锁在外层，事务在内部
                doSeckill(productId, userId);
            } finally {
                lock.unlock();
            }
        } else {
            throw new BusinessException("系统繁忙，请稍后再试");
        }
    }
    
    @Transactional // 事务放在内部方法
    public void doSeckill(Long productId, Long userId) {
        // 1. 查询库存（加行锁：for update）
        Product product = productMapper.selectByIdForUpdate(productId);
        if (product.getStock() <= 0) {
            throw new BusinessException("库存不足");
        }
        
        // 2. 扣减库存（乐观锁防超卖）
        int affected = productMapper.decreaseStock(productId);
        if (affected == 0) {
            throw new BusinessException("库存不足");
        }
        
        // 3. 创建订单
        Order order = new Order();
        order.setProductId(productId);
        order.setUserId(userId);
        orderMapper.insert(order);
    }
}
```

**关键修正**：
1. 分布式锁在外层，避免事务未提交时锁释放；
2. 查询库存加行锁 `select ... for update`，或扣减库存用乐观锁 `where stock > 0`；
3. 扣减库存后检查影响行数，防止超卖。

---

### 5.2 定时任务防重复

```java
@Component
public class ScheduledTask {
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点执行
    public void dailyReport() {
        RLock lock = redissonClient.getLock("task:dailyReport");
        
        if (lock.tryLock()) {
            try {
                log.info("开始执行日报任务");
                
                // 执行业务逻辑
                generateDailyReport();
                
                log.info("日报任务执行完成");
            } catch (Exception e) {
                log.error("日报任务执行失败", e);
            } finally {
                lock.unlock();
            }
        } else {
            log.info("任务正在执行，跳过本次调度");
        }
    }
}
```

---

### 5.3 接口幂等性保证

```java
@RestController
@RequestMapping("/order")
public class OrderController {
    
    @Autowired
    private RedissonClient redissonClient;
    
    @PostMapping("/create")
    public Result<Order> createOrder(@RequestBody OrderRequest request) {
        // 使用 requestId 作为锁 key
        String lockKey = "order:create:" + request.getRequestId();
        RLock lock = redissonClient.getLock(lockKey);
        
        if (lock.tryLock()) {
            try {
                // 1. 检查是否已处理
                if (isProcessed(request.getRequestId())) {
                    return Result.success(getProcessedOrder(request.getRequestId()));
                }
                
                // 2. 创建订单
                Order order = orderService.create(request);
                
                // 3. 标记已处理
                markAsProcessed(request.getRequestId(), order.getId());
                
                return Result.success(order);
            } finally {
                lock.unlock();
            }
        } else {
            return Result.error("请求处理中，请勿重复提交");
        }
    }
}
```

---

## 六、高频面试题

### 问题 1：如何用 Redis 实现分布式锁？

**参考答案**：

**基础实现**：
```bash
SET lock:key unique_value NX EX 30
```

**存在问题**：
1. 不可重入
2. 锁过期业务未完成
3. 主从切换丢锁

**推荐方案**：使用 Redisson 框架
- 可重入
- 看门狗自动续期
- 支持阻塞等待

---

### 问题 2：如何解决锁超时导致的死锁问题？

> **问题 2：如何解决锁过期导致的业务未完成问题？**
> 
> **参考答案**：
> **方案 1：看门狗机制（推荐）**
> - 不指定锁过期时间，Redisson 自动启动看门狗线程；
> - 每隔 10 秒检查业务是否完成，未完成则将锁续期到 30 秒；
> - 业务完成或服务宕机后，看门狗停止，锁最终过期释放。
> 
> **方案 2：设置合理的过期时间**
> - 根据业务最大执行时间设置（如预估 10 秒，设置 30 秒）；
> - 预留缓冲时间，避免业务未完成锁已释放。
> 
> **方案 3：finally 块释放锁**
> ```java
> lock.lock();
> try {
>     // 业务逻辑
> } finally {
>     lock.unlock();  // 确保锁被释放，避免死锁
> }
> ```

---

### 问题 3：Redisson 的看门狗机制是如何工作的？

**参考答案**：

**工作流程**：
1. 获取锁成功后，启动看门狗线程
2. 每隔 10 秒（过期时间的 1/3）检查锁是否仍被持有
3. 如果业务未完成，自动将锁续期到 30 秒
4. 业务完成或服务宕机，看门狗停止，锁最终过期释放

**注意事项**：
- 只有不指定过期时间时，看门狗才生效
- 看门狗只适用于单机锁，不适用于红锁

---

### 问题 4：什么是 RedLock 算法？适用场景？

**参考答案**：

**原理**：
1. 向 N 个独立的 Redis 节点申请锁
2. 计算获取锁的总耗时
3. 多数节点（N/2+1）成功且耗时 < 过期时间，则获取锁
4. 释放时向所有节点发送释放命令

**适用场景**：
- 对可靠性要求较高的分布式场景
- 多数据中心部署

**争议**：
- 在时钟漂移、网络延迟等场景下存在安全性问题
- 对一致性要求极高的场景建议使用 ZooKeeper

---

### 问题 5：Redis 锁如何保证可重入性？

**参考答案**：

**实现原理**：
- 使用 Hash 结构存储锁信息
- Key：锁标识
- Field：线程 ID
- Value：重入次数

**判断逻辑**：
```lua
if redis.call('hexists', KEYS[1], ARGV[1]) == 1 then
    -- 同一线程，重入次数 +1
    redis.call('hincrby', KEYS[1], ARGV[1], 1)
    redis.call('pexpire', KEYS[1], ARGV[2])
    return 1
end
```

---

## 七、最佳实践

### 7.1 使用建议

1. **优先使用 Redisson**：避免自己实现的坑（如原子性、续期、可重入）；
2. **启用看门狗**：非特殊场景不指定过期时间，依赖看门狗续期；
3. **锁粒度要小**：按业务 ID 加锁（如 `seckill:lock:productId`），避免全局锁；
4. **锁内业务要短**：避免在锁内执行 IO 操作（如 RPC、数据库慢查询）；
5. **必须 finally 释放**：即使业务抛出异常，也要保证锁释放；
6. **加锁失败降级**：获取锁失败时返回友好提示，而非直接抛异常；
7. **避免长时间占用锁**：锁内业务超时则主动释放，防止阻塞其他请求。

### 7.2 错误示范

```java
// ❌ 错误 1：锁过期，业务未完成
lock.tryLock(10, 30, TimeUnit.SECONDS);  // 30 秒后锁过期

// ❌ 错误 2：未在 finally 中释放锁
lock.lock();
// 业务逻辑（抛出异常时锁未释放）

// ❌ 错误 3：锁粒度过大
synchronized (globalLock) {
    // 包含大量无关操作
}

// ❌ 错误 4：误删其他线程的锁
redisTemplate.delete(lockKey);  // 未校验锁归属
```

### 7.3 正确示范

```java
// ✅ 正确 1：使用看门狗
RLock lock = redisson.getLock("myLock");
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();
}

// ✅ 正确 2：锁粒度适中
RLock lock = redisson.getLock("order:" + orderId);
lock.lock();
try {
    // 只处理单个订单
} finally {
    lock.unlock();
}

// ✅ 正确 3：带超时时间的尝试（Redisson 正确用法）
RLock lock = redisson.getLock("myLock");
// 参数 1：等待时间（10 秒），参数 2：锁过期时间（-1 表示启用看门狗）
if (lock.tryLock(10, -1, TimeUnit.SECONDS)) {
    try {
        // 业务逻辑
    } finally {
        if (lock.isHeldByCurrentThread()) { // 检查当前线程持有锁
            lock.unlock();
        }
    }
} else {
    // 获取锁失败，降级处理
    throw new BusinessException("系统繁忙，请稍后再试");
}
```

---

## 八、总结

| 知识点 | 核心内容 |
|--------|----------|
| 锁演进 | SETNX → SET NX EX → Redisson |
| 看门狗 | 自动续期，解决锁过期问题 |
| 可重入 | 记录线程标识和重入次数 |
| 高级锁 | 公平锁、读写锁、联锁、红锁 |
| 实战场景 | 秒杀、定时任务、幂等性 |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15

---

## 附录：Redisson 配置示例

**application.yml**：
```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 
redisson:
  lock-watchdog-timeout: 30000 # 看门狗默认过期时间
```

**Lua脚本中文注释**：
```lua
-- 判断是否是当前线程
if redis.call('hexists', KEYS[1], ARGV[1]) == 1 then
    -- 重入次数 -1
    local counter = redis.call('hincrby', KEYS[1], ARGV[1], -1)
    
    if counter == 0 then
        -- 重入次数为 0，删除锁
        redis.call('del', KEYS[1])
        return 1
    else
        -- 更新过期时间
        redis.call('pexpire', KEYS[1], ARGV[2])
        return 0
    end
end

return 0
```
