# Redis 高级应用 - 分布式锁与限流

## 分布式锁

### 为什么需要分布式锁

在分布式系统中，多个服务实例可能同时操作共享资源，需要一种跨 JVM 的互斥机制来保证数据一致性。

### Redis 实现分布式锁的原理

```
┌─────────────────────────────────────────────────────────────┐
│  服务A          Redis           服务B                        │
│    │              │               │                          │
│    │  SETNX lock  │               │                          │
│    │─────────────>│               │                          │
│    │  OK          │               │                          │
│    │<─────────────│               │                          │
│    │              │               │  SETNX lock              │
│    │  执行业务    │               │─────────────>            │
│    │  逻辑        │               │  nil (失败)              │
│    │              │               │<─────────────            │
│    │  DEL lock    │               │  等待或重试              │
│    │─────────────>│               │                          │
└─────────────────────────────────────────────────────────────┘
```

### 基本实现

```java
public class DistributedLock {
    
    /**
     * 获取锁
     */
    public boolean tryLock(String lockKey, String requestId, int expireTime) {
        // SET key value NX EX seconds
        String result = jedis.set(lockKey, requestId, "NX", "EX", expireTime);
        return "OK".equals(result);
    }
    
    /**
     * 释放锁（使用 Lua 脚本保证原子性）
     */
    public boolean releaseLock(String lockKey, String requestId) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                       "return redis.call('del', KEYS[1]) else return 0 end";
        Object result = jedis.eval(script, 
            Collections.singletonList(lockKey), 
            Collections.singletonList(requestId));
        return Long.valueOf(1).equals(result);
    }
}
```

### Redisson 分布式锁

Redisson 是 Redis 的 Java 客户端，提供了更完善的分布式锁实现。

```java
// 获取锁
RLock lock = redisson.getLock("myLock");

// 加锁（自动续期）
lock.lock();
try {
    // 执行业务逻辑
} finally {
    lock.unlock();
}

// 尝试获取锁（带超时时间）
boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
```

### Redisson 锁的特点

| 特性 | 说明 |
|------|------|
| **可重入** | 同一线程可以多次获取锁 |
| **自动续期** | 看门狗机制，自动延长锁有效期 |
| **阻塞等待** | 支持等待获取锁 |
| **超时释放** | 防止死锁 |

## 限流

### 限流算法

#### 1. 计数器算法

```
时间窗口: 0----1----2----3----4----5----6 (秒)
请求数:   5    8    3    7    0    4    6
          └────┘
          窗口内最多 10 个请求
```

```java
public class CounterRateLimiter {
    
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        String currentKey = key + ":" + (System.currentTimeMillis() / 1000 / windowSeconds);
        Long count = jedis.incr(currentKey);
        
        if (count == 1) {
            jedis.expire(currentKey, windowSeconds);
        }
        
        return count <= limit;
    }
}
```

#### 2. 滑动窗口算法

更精确的限流，将时间窗口划分为多个小窗口。

#### 3. 令牌桶算法

```
     令牌产生速率: 10/秒
           │
           ▼
    ┌─────────────┐
    │   令牌桶    │  容量: 100
    │  ○ ○ ○ ○ ○  │
    └──────┬──────┘
           │
           ▼
        请求处理
```

```java
public class TokenBucketRateLimiter {
    
    public boolean tryAcquire(String key, int capacity, int rate) {
        String script = 
            "local capacity = tonumber(ARGV[1]);" +
            "local rate = tonumber(ARGV[2]);" +
            "local now = tonumber(ARGV[3]);" +
            "local key = KEYS[1];" +
            
            "local lastTime = redis.call('hget', key, 'lastTime');" +
            "local tokens = redis.call('hget', key, 'tokens');" +
            
            "if lastTime == false then" +
            "    redis.call('hmset', key, 'tokens', capacity-1, 'lastTime', now);" +
            "    redis.call('expire', key, 60);" +
            "    return 1;" +
            "end" +
            
            "local timePassed = now - tonumber(lastTime);" +
            "local newTokens = math.min(capacity, tonumber(tokens) + timePassed * rate);" +
            
            "if newTokens >= 1 then" +
            "    redis.call('hmset', key, 'tokens', newTokens-1, 'lastTime', now);" +
            "    return 1;" +
            "else" +
            "    redis.call('hset', key, 'lastTime', now);" +
            "    return 0;" +
            "end";
            
        Object result = jedis.eval(script, 
            Collections.singletonList(key),
            Arrays.asList(String.valueOf(capacity), 
                         String.valueOf(rate),
                         String.valueOf(System.currentTimeMillis() / 1000)));
        return Long.valueOf(1).equals(result);
    }
}
```

#### 4. 漏桶算法

请求先进入队列，以固定速率处理，超出队列容量的请求被拒绝。

### 限流应用场景

| 场景 | 策略 |
|------|------|
| API 接口限流 | 按用户/IP 限流，防止恶意调用 |
| 秒杀活动 | 严格限流，保护后端服务 |
| 短信发送 | 按手机号限流，防止短信轰炸 |
| 登录接口 | 按用户名/IP 限流，防止暴力破解 |

## 接口防重放

### 什么是重放攻击

攻击者截获合法请求后，重复发送该请求，导致业务异常。

### 防重放方案

```
请求参数 + 时间戳 + 随机数 + 签名
                │
                ▼
        服务端验证签名
                │
                ▼
        检查时间戳（如 5 分钟内）
                │
                ▼
        检查随机数是否已使用（Redis 去重）
```

```java
public class ReplayAttackPrevention {
    
    /**
     * 检查请求是否重放
     */
    public boolean isReplay(String nonce, long timestamp) {
        // 1. 检查时间戳（5 分钟内有效）
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > 5 * 60 * 1000) {
            return true; // 超时，视为重放
        }
        
        // 2. 检查 nonce 是否已使用
        String key = "nonce:" + nonce;
        Boolean exists = jedis.exists(key);
        if (exists) {
            return true; // 已使用，是重放
        }
        
        // 3. 记录 nonce（5 分钟过期）
        jedis.setex(key, 5 * 60, "1");
        return false;
    }
}
```

## 总结

| 功能 | 实现方式 | 关键点 |
|------|---------|--------|
| 分布式锁 | SETNX + Lua / Redisson | 原子性、防死锁、可重入 |
| 限流 | 计数器/令牌桶/漏桶 | 算法选择、阈值设定 |
| 防重放 | 时间戳 + nonce + 签名 | 时间窗口、唯一性校验 |
