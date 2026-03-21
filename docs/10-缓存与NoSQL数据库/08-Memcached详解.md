# Memcached 详解

## 一、Memcached 概述

### 1.1 什么是 Memcached

Memcached 是一个高性能的分布式内存对象缓存系统，用于动态 Web 应用以减轻数据库负载。它通过在内存中缓存数据和对象来减少读取数据库的次数，从而提高动态、数据库驱动网站的速度。

**核心特点：**

1. **简单高效** - 协议简单，基于 libevent 的事件处理
2. **内存存储** - 所有数据存储在内存中，断电数据丢失
3. **分布式** - 不支持集群功能，需要客户端实现分布式
4. **LRU 算法** - 自动淘汰不常用的数据
5. **多线程** - 支持多线程处理并发请求

### 1.2 Memcached 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端应用层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │  App 1   │  │  App 2   │  │  App 3   │  │  App N   │       │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘       │
└───────┼─────────────┼─────────────┼─────────────┼───────────────┘
        │             │             │             │
        ▼             ▼             ▼             ▼
┌───────────────────────────────────────────────────────────────┐
│                    Memcached 客户端（一致性哈希）                │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  路由算法：Hash(key) % ServerCount 或 一致性哈希            │  │
│  └─────────────────────────────────────────────────────────┘  │
└───────────────────────────┬───────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Memcached 1  │   │  Memcached 2  │   │  Memcached N  │
│   (Slab 1-N)  │   │   (Slab 1-N)  │   │   (Slab 1-N)  │
│   LRU 链表     │   │   LRU 链表    │    │   LRU 链表    │
└───────────────┘   └───────────────┘   └───────────────┘
```

### 1.3 Memcached vs Redis 对比

| 对比项      | Memcached       | Redis                       |
|----------|-----------------|-----------------------------|
| 数据类型     | 仅支持 String      | String、List、Set、Hash、ZSet 等 |
| 持久化      | 不支持             | RDB、AOF、混合持久化               |
| 线程模型     | 多线程             | 单线程（6.0+ 支持 IO 多线程）         |
| 集群       | 不支持，需客户端实现      | 原生支持 Cluster                |
| 内存管理     | Slab Allocation | jemalloc（默认）                |
| 事务       | 不支持             | 支持（弱事务）                     |
| Lua 脚本   | 不支持             | 支持                          |
| 发布订阅     | 不支持             | 支持                          |
| 最大 Key   | 250 字节          | 512MB                       |
| 最大 Value | 1MB（可配置）        | 512MB                       |
| 适用场景     | 简单 KV 缓存        | 复杂数据结构、缓存、消息队列              |

---

## 二、Memcached 内存管理

### 2.1 Slab Allocation 机制

Memcached 使用 Slab Allocation 机制管理内存，将内存划分为多个 Slab Class，每个 Slab Class 包含多个大小相同的 Chunk。

```
┌─────────────────────────────────────────────────────────────────┐
│                      Memcached 内存结构                          │
├─────────────────────────────────────────────────────────────────┤
│  Slab Class 1 (Chunk: 80B)                                      │
│  ┌────────┬────────┬────────┬────────┬────────┬────────┐       │
│  │ Chunk  │ Chunk  │ Chunk  │ Chunk  │ Chunk  │ Chunk  │ ...   │
│  │  80B   │  80B   │  80B   │  80B   │  80B   │  80B   │       │
│  └────────┴────────┴────────┴────────┴────────┴────────┘       │
├─────────────────────────────────────────────────────────────────┤
│  Slab Class 2 (Chunk: 104B)                                     │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐      │
│  │  Chunk   │  Chunk   │  Chunk   │  Chunk   │  Chunk   │ ...  │
│  │  104B    │  104B    │  104B    │  104B    │  104B    │      │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘      │
├─────────────────────────────────────────────────────────────────┤
│  Slab Class 3 (Chunk: 136B)                                     │
│  ┌────────────┬────────────┬────────────┬────────────┐         │
│  │   Chunk    │   Chunk    │   Chunk    │   Chunk    │ ...     │
│  │   136B     │   136B     │   136B     │   136B     │         │
│  └────────────┴────────────┴────────────┴────────────┘         │
├─────────────────────────────────────────────────────────────────┤
│  ...                                                            │
│  Slab Class N (Chunk: 1MB)                                      │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                    Chunk 1MB                            │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

**Slab Allocation 核心参数：**

```bash
# 启动参数
memcached -m 64          # 最大内存 64MB
memcached -f 1.25        # Chunk 增长因子（默认 1.25）
memcached -n 48          # Chunk 最小分配空间（默认 48B + key + value + flags）
memcached -I 1m          # 最大 item 大小（默认 1MB）
```

### 2.2 LRU 淘汰策略

Memcached 使用 LRU（Least Recently Used）算法淘汰数据：

```
┌─────────────────────────────────────────────────────────────────┐
│                       LRU 链表结构                                │
│                                                                  │
│   HEAD ◄──► [Item1] ◄──► [Item2] ◄──► [Item3] ◄──► ... ◄──► TAIL│
│              最新使用                              最久未使用      │
│                                                                 │
│   访问 Item2 时：将 Item2 移动到 HEAD                              │
│   新增 Item 时：插入到 HEAD，如果空间不足则淘汰 TAIL                  │
└─────────────────────────────────────────────────────────────────┘
```

**淘汰策略配置：**

```bash
# -o 参数设置淘汰策略
memcached -o no_lru_maintainer    # 禁用后台 LRU 维护线程
memcached -o lru_maintainer       # 启用后台 LRU 维护线程（默认）
memcached -o hot_lru_pct=20       # HOT LRU 百分比
memcached -o warm_lru_pct=40      # WARM LRU 百分比
```

### 2.3 内存碎片问题

**问题：** 当存储的数据大小与 Chunk 大小不匹配时，会产生内存浪费。

```
示例：存储 90B 数据
┌─────────────────────────────────────────────────────────────────┐
│  Slab Class 2 (Chunk: 104B)                                     │
│  ┌────────────────────────────────────┬──────────────────────┐  │
│  │  数据: 90B                          │  浪费: 14B            │ │
│  └────────────────────────────────────┴──────────────────────┘  │
│  内存利用率: 90/104 = 86.5%                                       │
└─────────────────────────────────────────────────────────────────┘
```

**优化建议：**

1. 合理设置增长因子 `-f`，减少内存碎片
2. 根据业务数据大小分布调整 chunk 大小
3. 监控 `slab_classes` 统计信息，优化内存使用

---

## 三、Memcached 协议与命令

### 3.1 文本协议

Memcached 支持文本协议和二进制协议，文本协议更易调试。

**基本命令：**

```bash
# 存储命令
set key flags exptime bytes [noreply]
value

add key flags exptime bytes [noreply]    # 仅当 key 不存在时存储
value

replace key flags exptime bytes [noreply] # 仅当 key 存在时替换
value

append key flags exptime bytes [noreply]  # 追加到 value 末尾
value

prepend key flags exptime bytes [noreply] # 追加到 value 开头
value

cas key flags exptime bytes cas_unique [noreply]  # 检查并设置
value

# 读取命令
get key1 [key2 ...]           # 获取一个或多个 key
gets key1 [key2 ...]          # 获取并返回 cas 唯一标识

# 删除命令
delete key [noreply]

# 增减命令
incr key value [noreply]      # 自增
decr key value [noreply]      # 自减

# 统计命令
stats                         # 显示统计信息
stats items                   # 显示 item 统计
stats slabs                   # 显示 slab 统计
stats sizes                   # 显示大小统计

# 其他命令
flush_all [delay] [noreply]   # 清空所有数据
version                       # 显示版本
quit                          # 关闭连接
```

**示例操作：**

```bash
# 连接 Memcached
telnet localhost 11211

# 存储数据
set username 0 3600 5
admin
STORED

# 获取数据
get username
VALUE username 0 5
admin
END

# 自增计数器
set counter 0 0 1
1
STORED
incr counter 5
6

# 查看统计
stats
STAT pid 1234
STAT uptime 3600
STAT time 1640000000
STAT version 1.6.9
...
END
```

### 3.2 二进制协议

二进制协议更高效，支持更多特性：

```
┌─────────────────────────────────────────────────────────────────┐
│                    二进制协议请求格式                               │
├─────────────────────────────────────────────────────────────────┤
│  字节偏移  │  字段名         │  描述                                │
├─────────────────────────────────────────────────────────────────┤
│  0        │  Magic          │  0x80（请求）/ 0x81（响应）          │
│  1        │  Opcode         │  命令操作码                         │
│  2-3      │  Key length     │  Key 长度                          │
│  4        │  Extras length  │  额外数据长度                        │
│  5        │  Data type      │  数据类型（保留）                     │
│  6-7      │  Status/vbucket │  状态码（响应）/ vbucket（请求）       │
│  8-11     │  Total body     │  Body 总长度                        │
│  12-15    │  Opaque         │  请求标识                           │
│  16-23    │  CAS            │  CAS 值                            │
│  24+      │  Extras/Key/Val │  额外数据、Key、Value                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 四、Java 客户端实战

### 4.1 客户端选择

| 客户端                    | 特点              | 推荐场景   |
|------------------------|-----------------|--------|
| **Xmemcached**         | 高性能、支持二进制协议、连接池 | 生产环境首选 |
| **SpyMemcached**       | 异步 API、简单易用     | 简单场景   |
| **Elasticache Client** | AWS 优化版本        | AWS 环境 |

### 4.2 Xmemcached 示例代码

**Maven 依赖：**

```xml

<dependency>
    <groupId>com.googlecode.xmemcached</groupId>
    <artifactId>xmemcached</artifactId>
    <version>2.4.8</version>
</dependency>
```

**配置类：**

```java
package cn.itzixiao.interview.cache.memcached.config;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Memcached 配置类
 *
 * 配置说明：
 * - servers: Memcached 服务器地址列表，格式为 host1:port1,host2:port2
 * - poolSize: 连接池大小，建议根据并发量设置
 * - opTimeout: 操作超时时间，单位毫秒
 */
@Configuration
public class MemcachedConfig {

    @Value("${memcached.servers:localhost:11211}")
    private String servers;

    @Value("${memcached.poolSize:10}")
    private int poolSize;

    @Value("${memcached.opTimeout:3000}")
    private long opTimeout;

    /**
     * 创建 MemcachedClient Builder
     *
     * AddrUtil.getAddresses() 方法解析服务器地址列表
     * 支持格式：host1:port1 host2:port2 或 host1:port1,host2:port2
     */
    @Bean
    public MemcachedClientBuilder memcachedClientBuilder() {
        // 创建 Builder，传入服务器地址列表
        MemcachedClientBuilder builder = new XMemcachedClientBuilder(
                AddrUtil.getAddresses(servers)
        );

        // 设置连接池大小
        // 连接池可以提高并发性能，避免频繁创建/销毁连接
        builder.setConnectionPoolSize(poolSize);

        // 设置操作超时时间
        builder.setOpTimeout(opTimeout);

        // 设置失败模式：关闭自动故障转移
        // 在分布式环境中，故障转移可能导致数据不一致
        builder.setFailureMode(false);

        return builder;
    }

    /**
     * 创建 MemcachedClient 实例
     *
     * MemcachedClient 是线程安全的，可以全局共享
     */
    @Bean
    public MemcachedClient memcachedClient(MemcachedClientBuilder builder)
            throws IOException {
        return builder.build();
    }
}
```

**服务类：**

```java
package cn.itzixiao.interview.cache.memcached.service;

import lombok.extern.slf4j.Slf4j;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeoutException;

/**
 * Memcached 服务封装类
 *
 * 提供常用的缓存操作方法，包括：
 * - 基本的 CRUD 操作
 * - 原子操作（自增、自减）
 * - CAS（Compare And Set）操作
 */
@Slf4j
@Service
public class MemcachedService {

    private final MemcachedClient memcachedClient;

    // 默认过期时间：1小时（单位：秒）
    private static final int DEFAULT_EXPIRE = 3600;

    public MemcachedService(MemcachedClient memcachedClient) {
        this.memcachedClient = memcachedClient;
    }

    /**
     * 存储数据（set 操作）
     *
     * 无论 key 是否存在，都会覆盖原有数据
     *
     * @param key    缓存键，最大 250 字节
     * @param value  缓存值，最大 1MB（可配置）
     * @param expire 过期时间，单位秒，0 表示永不过期
     * @return 操作是否成功
     */
    public boolean set(String key, Object value, int expire) {
        try {
            // set 方法参数说明：
            // key: 缓存键
            // expire: 过期时间（秒），0 表示永不过期
            // value: 缓存值，需要实现 Serializable 接口
            return memcachedClient.set(key, expire, value);
        } catch (TimeoutException e) {
            // 操作超时，可能是网络问题或服务器负载过高
            log.error("Memcached set timeout, key: {}", key, e);
            return false;
        } catch (InterruptedException e) {
            // 线程被中断
            Thread.currentThread().interrupt();
            log.error("Memcached set interrupted, key: {}", key, e);
            return false;
        } catch (MemcachedException e) {
            // Memcached 服务端错误
            log.error("Memcached set error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 存储数据，使用默认过期时间
     */
    public boolean set(String key, Object value) {
        return set(key, value, DEFAULT_EXPIRE);
    }

    /**
     * 获取数据（get 操作）
     *
     * @param key 缓存键
     * @return 缓存值，不存在则返回 null
     */
    public <T> T get(String key) {
        try {
            // get 操作返回泛型，需要调用者进行类型转换
            return memcachedClient.get(key);
        } catch (TimeoutException e) {
            log.error("Memcached get timeout, key: {}", key, e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Memcached get interrupted, key: {}", key, e);
            return null;
        } catch (MemcachedException e) {
            log.error("Memcached get error, key: {}", key, e);
            return null;
        }
    }

    /**
     * 仅当 key 不存在时存储（add 操作）
     *
     * 常用于实现分布式锁、防止重复提交等场景
     *
     * @return true 表示存储成功，false 表示 key 已存在
     */
    public boolean add(String key, Object value, int expire) {
        try {
            return memcachedClient.add(key, expire, value);
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            log.error("Memcached add error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 仅当 key 存在时更新（replace 操作）
     *
     * @return true 表示更新成功，false 表示 key 不存在
     */
    public boolean replace(String key, Object value, int expire) {
        try {
            return memcachedClient.replace(key, expire, value);
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            log.error("Memcached replace error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 删除数据（delete 操作）
     *
     * @return true 表示删除成功，false 表示 key 不存在或删除失败
     */
    public boolean delete(String key) {
        try {
            return memcachedClient.delete(key);
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            log.error("Memcached delete error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 原子自增操作
     *
     * 如果 key 不存在，会创建并初始化为 delta 值
     * 注意：自增操作要求 value 是数值类型的字符串
     *
     * @param key   缓存键
     * @param delta 增量值
     * @return 自增后的值，失败返回 -1
     */
    public long incr(String key, long delta) {
        try {
            // incr 参数说明：
            // key: 缓存键
            // delta: 增量值（可以是负数，相当于 decr）
            // initValue: key 不存在时的初始值
            // expire: 过期时间
            return memcachedClient.incr(key, delta, delta, DEFAULT_EXPIRE);
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            log.error("Memcached incr error, key: {}", key, e);
            return -1;
        }
    }

    /**
     * 原子自减操作
     */
    public long decr(String key, long delta) {
        try {
            // decr 操作不会低于 0
            return memcachedClient.decr(key, delta);
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            log.error("Memcached decr error, key: {}", key, e);
            return -1;
        }
    }

    /**
     * CAS（Compare And Set）操作
     *
     * 实现乐观锁机制，仅当 cas 值匹配时才更新
     * 用于解决并发更新问题
     *
     * @param key    缓存键
     * @param value  新值
     * @param cas    原始 cas 值（通过 gets 命令获取）
     * @param expire 过期时间
     * @return 操作是否成功
     */
    public boolean cas(String key, Object value, long cas, int expire) {
        try {
            // cas 操作流程：
            // 1. 先通过 gets 获取 value 和 cas 值
            // 2. 修改 value
            // 3. 使用 cas 更新，如果 cas 值不匹配则说明数据已被其他线程修改
            return memcachedClient.cas(key, expire, value, cas);
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            log.error("Memcached cas error, key: {}", key, e);
            return false;
        }
    }

    /**
     * 获取数据及其 CAS 值
     *
     * 用于实现乐观锁
     */
    public <T> CASValue<T> gets(String key) {
        try {
            net.rubyeye.xmemcached.CASValue<T> casValue = memcachedClient.gets(key);
            if (casValue != null) {
                return new CASValue<>(casValue.getCas(), casValue.getValue());
            }
            return null;
        } catch (TimeoutException | InterruptedException | MemcachedException e) {
            log.error("Memcached gets error, key: {}", key, e);
            return null;
        }
    }

    /**
     * CAS 值封装类
     */
    public record CASValue<T>(long cas, T value) {
    }
}
```

**分布式锁实现：**

```java
package cn.itzixiao.interview.cache.memcached.lock;

import cn.itzixiao.interview.cache.memcached.service.MemcachedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Memcached 的分布式锁实现
 *
 * 实现原理：
 * 1. 使用 add 操作实现加锁（原子性保证）
 * 2. 使用 delete 操作释放锁
 * 3. 锁持有者标识防止误删
 *
 * 注意事项：
 * - Memcached 不支持 Redis 的过期续期机制
 * - 需要合理设置锁的过期时间
 * - 不支持可重入锁
 */
@Slf4j
@Component
public class MemcachedDistributedLock {

    private final MemcachedService memcachedService;

    // 锁的默认过期时间：30秒
    private static final int DEFAULT_LOCK_EXPIRE = 30;

    // 获取锁的重试间隔：100毫秒
    private static final long RETRY_INTERVAL_MS = 100;

    public MemcachedDistributedLock(MemcachedService memcachedService) {
        this.memcachedService = memcachedService;
    }

    /**
     * 尝试获取分布式锁
     *
     * 使用 add 操作实现，仅当 key 不存在时才能添加成功
     *
     * @param lockKey 锁的 key
     * @param expireSeconds 锁的过期时间（秒）
     * @return 锁持有者标识，用于释放锁；获取失败返回 null
     */
    public String tryLock(String lockKey, int expireSeconds) {
        // 生成唯一的锁持有者标识
        // 用于防止误删其他线程持有的锁
        String lockValue = UUID.randomUUID().toString();

        // add 操作是原子的，只有 key 不存在时才会成功
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
     */
    public String tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_LOCK_EXPIRE);
    }

    /**
     * 阻塞式获取锁
     *
     * 在指定时间内不断重试，直到获取锁或超时
     *
     * @param lockKey 锁的 key
     * @param expireSeconds 锁的过期时间（秒）
     * @param waitTime 最大等待时间
     * @param timeUnit 时间单位
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
     * 释放锁
     *
     * 需要验证锁持有者标识，防止误删其他线程的锁
     *
     * @param lockKey 锁的 key
     * @param lockValue 锁持有者标识（获取锁时返回的值）
     * @return 是否成功释放
     */
    public boolean unlock(String lockKey, String lockValue) {
        if (lockValue == null) {
            return false;
        }

        // 先获取当前锁的值，验证是否是自己持有的锁
        // 注意：这里的 get 和 delete 不是原子操作
        // 在高并发场景下可能出现问题，但 Memcached 不支持 Lua 脚本
        // 如果需要更严格的保证，建议使用 Redis
        String currentValue = memcachedService.get(lockKey);

        if (lockValue.equals(currentValue)) {
            // 是自己持有的锁，可以删除
            boolean deleted = memcachedService.delete(lockKey);
            if (deleted) {
                log.debug("Successfully released lock, key: {}", lockKey);
            }
            return deleted;
        }

        // 锁已被其他线程持有或已过期
        log.warn("Lock not owned by current thread, key: {}, expected: {}, actual: {}",
                lockKey, lockValue, currentValue);
        return false;
    }

    /**
     * 使用分布式锁执行任务
     *
     * @param lockKey 锁的 key
     * @param task 要执行的任务
     * @param <T> 返回值类型
     * @return 任务执行结果，获取锁失败返回 null
     */
    public <T> T executeWithLock(String lockKey, java.util.function.Supplier<T> task) {
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
}
```

---

## 五、分布式实现原理

### 5.1 客户端分片

Memcached 不支持服务端集群，需要客户端实现分布式：

```
┌─────────────────────────────────────────────────────────────────┐
│                    客户端分片架构                                │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │                     应用程序                              │   │
│  └─────────────────────────┬───────────────────────────────┘   │
│                            │                                    │
│  ┌─────────────────────────▼───────────────────────────────┐   │
│  │                  Memcached Client                        │   │
│  │  ┌─────────────────────────────────────────────────┐    │   │
│  │  │  一致性哈希环（Consistent Hashing Ring）          │    │   │
│  │  │                                                  │    │   │
│  │  │      Node1 ──── Node2 ──── Node3 ──── Node4     │    │   │
│  │  │        │          │          │          │        │    │   │
│  │  │      key1       key2       key3       key4      │    │   │
│  │  └─────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 一致性哈希算法

```java
package cn.itzixiao.interview.cache.memcached.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 一致性哈希算法实现
 *
 * 核心思想：
 * 1. 将服务器节点映射到哈希环上
 * 2. 将数据 key 也映射到哈希环上
 * 3. 数据存储在顺时针方向第一个节点上
 *
 * 虚拟节点：
 * - 解决数据倾斜问题
 * - 每个物理节点映射多个虚拟节点
 * - 虚拟节点均匀分布在哈希环上
 */
public class ConsistentHash<T> {

    // 哈希环，使用 TreeMap 实现，可以快速找到顺时针方向的下一个节点
    private final TreeMap<Long, T> ring = new TreeMap<>();

    // 虚拟节点数量，每个物理节点对应 virtualNodes 个虚拟节点
    private final int virtualNodes;

    // 物理节点集合
    private final Set<T> nodes = new HashSet<>();

    /**
     * 构造函数
     *
     * @param virtualNodes 每个物理节点的虚拟节点数量，建议 150-200
     */
    public ConsistentHash(int virtualNodes) {
        this.virtualNodes = virtualNodes;
    }

    /**
     * 添加节点
     *
     * 将物理节点和其虚拟节点添加到哈希环上
     */
    public void addNode(T node) {
        nodes.add(node);

        // 为每个物理节点创建 virtualNodes 个虚拟节点
        for (int i = 0; i < virtualNodes; i++) {
            // 虚拟节点的 key 格式：node##index
            String virtualKey = node.toString() + "##" + i;

            // 计算虚拟节点的哈希值
            long hash = hash(virtualKey);

            // 将虚拟节点放入哈希环
            ring.put(hash, node);
        }
    }

    /**
     * 移除节点
     *
     * 移除物理节点及其所有虚拟节点
     */
    public void removeNode(T node) {
        nodes.remove(node);

        // 移除所有虚拟节点
        for (int i = 0; i < virtualNodes; i++) {
            String virtualKey = node.toString() + "##" + i;
            long hash = hash(virtualKey);
            ring.remove(hash);
        }
    }

    /**
     * 根据 key 获取对应的节点
     *
     * 算法流程：
     * 1. 计算 key 的哈希值
     * 2. 在哈希环上顺时针查找第一个节点
     */
    public T getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }

        // 计算 key 的哈希值
        long hash = hash(key);

        // 在哈希环上顺时针查找第一个大于等于该哈希值的节点
        // tailMap 返回大于等于给定 key 的子 Map
        Map.Entry<Long, T> entry = ring.ceilingEntry(hash);

        if (entry == null) {
            // 如果没有找到，说明哈希值超过了环上最大的节点
            // 返回环上的第一个节点（环形结构）
            entry = ring.firstEntry();
        }

        return entry.getValue();
    }

    /**
     * 计算 MD5 哈希值
     *
     * 使用 MD5 算法计算哈希值，分布更均匀
     */
    private long hash(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(key.getBytes());

            // 取 MD5 的前 8 字节作为哈希值
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }

            // 确保返回正数
            return hash & 0x7FFFFFFFFFFFFFFFL;

        } catch (NoSuchAlgorithmException e) {
            // MD5 算法一定存在，不会抛出此异常
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 获取节点分布统计
     */
    public Map<T, Integer> getDistribution(List<String> keys) {
        Map<T, Integer> distribution = new HashMap<>();

        for (String key : keys) {
            T node = getNode(key);
            distribution.merge(node, 1, Integer::sum);
        }

        return distribution;
    }
}
```

---

## 六、Memcached 集群部署

### 6.1 部署架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    Memcached 集群架构                            │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                      应用服务器集群                        │   │
│  │   ┌─────────┐   ┌─────────┐   ┌─────────┐              │   │
│  │   │  App 1  │   │  App 2  │   │  App N  │              │   │
│  │   └────┬────┘   └────┬────┘   └────┬────┘              │   │
│  └────────┼─────────────┼─────────────┼────────────────────┘   │
│           │             │             │                         │
│           └─────────────┼─────────────┘                         │
│                         │                                       │
│  ┌──────────────────────▼──────────────────────────────────┐   │
│  │               一致性哈希客户端分片                        │   │
│  └──────────────────────┬──────────────────────────────────┘   │
│                         │                                       │
│     ┌───────────────────┼───────────────────┐                  │
│     ▼                   ▼                   ▼                  │
│  ┌──────────┐      ┌──────────┐      ┌──────────┐             │
│  │  MC-1    │      │  MC-2    │      │  MC-N    │             │
│  │ 11211    │      │ 11211    │      │ 11211    │             │
│  │ 4GB Mem  │      │ 4GB Mem  │      │ 4GB Mem  │             │
│  └──────────┘      └──────────┘      └──────────┘             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    监控层（可选）                          │   │
│  │   ┌─────────────┐   ┌─────────────┐   ┌─────────────┐   │   │
│  │   │ Prometheus  │   │  Grafana    │   │   Alert     │   │   │
│  │   └─────────────┘   └─────────────┘   └─────────────┘   │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 Docker 部署示例

```yaml
# docker-compose.yml
version: '3.8'

services:
  memcached-1:
    image: memcached:1.6.9
    container_name: memcached-1
    ports:
      - "11211:11211"
    command: memcached -m 512 -c 1024 -v
    networks:
      - cache-network

  memcached-2:
    image: memcached:1.6.9
    container_name: memcached-2
    ports:
      - "11212:11211"
    command: memcached -m 512 -c 1024 -v
    networks:
      - cache-network

  memcached-3:
    image: memcached:1.6.9
    container_name: memcached-3
    ports:
      - "11213:11211"
    command: memcached -m 512 -c 1024 -v
    networks:
      - cache-network

  # Memcached 监控（可选）
  memcached-exporter:
    image: prom/memcached-exporter:latest
    container_name: memcached-exporter
    ports:
      - "9150:9150"
    command:
      - '--memcached.address=memcached-1:11211'
    depends_on:
      - memcached-1
    networks:
      - cache-network

networks:
  cache-network:
    driver: bridge
```

### 6.3 启动参数详解

```bash
memcached [options]

常用参数：
  -p <num>      监听端口，默认 11211
  -U <num>      UDP 端口，默认 11211，0 表示禁用
  -l <ip_addr>  监听地址，默认所有网卡
  -d            以守护进程方式运行
  -u <username> 运行用户
  -m <num>      最大内存使用量（MB），默认 64
  -c <num>      最大并发连接数，默认 1024
  -P <file>     PID 文件路径
  -f <factor>   Chunk 大小增长因子，默认 1.25
  -n <bytes>    Chunk 最小分配空间，默认 48
  -I <size>     最大 item 大小，默认 1m
  -t <threads>  工作线程数，默认 4
  -v            详细输出（打印错误和警告）
  -vv           更详细的输出（打印命令和响应）
  -vvv          最详细的输出（打印内部状态变化）

高级参数：
  -o <options>  扩展选项，逗号分隔
    no_lru_maintainer  禁用后台 LRU 维护线程
    lru_maintainer     启用后台 LRU 维护线程
    hot_lru_pct=N      HOT LRU 百分比
    warm_lru_pct=N     WARM LRU 百分比
    maxconns_fast      快速关闭超出连接数限制的连接
    hash_algorithm=crc32  哈希算法选择
```

---

## 七、监控与调优

### 7.1 关键监控指标

```
┌─────────────────────────────────────────────────────────────────┐
│                    Memcached 监控指标                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  内存指标                                                        │
│  ├── bytes: 当前存储的字节数                                    │
│  ├── limit_maxbytes: 最大内存限制                               │
│  ├── bytes_read: 读取的总字节数                                 │
│  ├── bytes_written: 写入的总字节数                              │
│  └── 内存使用率 = bytes / limit_maxbytes                        │
│                                                                  │
│  连接指标                                                        │
│  ├── curr_connections: 当前连接数                               │
│  ├── total_connections: 累计连接数                              │
│  ├── maxconns: 最大连接数限制                                   │
│  └── 连接使用率 = curr_connections / maxconns                   │
│                                                                  │
│  命令指标                                                        │
│  ├── cmd_get: GET 命令次数                                      │
│  ├── cmd_set: SET 命令次数                                      │
│  ├── get_hits: 命中次数                                         │
│  ├── get_misses: 未命中次数                                     │
│  └── 命中率 = get_hits / (get_hits + get_misses)                │
│                                                                  │
│  淘汰指标                                                        │
│  ├── evictions: 因内存不足被淘汰的 item 数                      │
│  └── evictions > 0 表示内存不足，需要扩容或优化                  │
│                                                                  │
│  Slab 指标                                                       │
│  ├── stats slabs: 查看 slab 使用情况                            │
│  ├── stats items: 查看 item 统计                                │
│  └── 监控各 slab 的内存碎片率                                   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 性能调优建议

**1. 内存配置优化**

```bash
# 根据业务数据大小调整增长因子
# 数据大小相近：增大增长因子，减少 slab class 数量
# 数据大小差异大：减小增长因子，增加 slab class 数量

# 示例：存储 1KB-10KB 的数据
memcached -m 4096 -f 1.15 -n 80

# 示例：存储大小相近的 session 数据
memcached -m 4096 -f 1.5 -n 1024
```

**2. 连接池优化**

```java
// Xmemcached 连接池配置
MemcachedClientBuilder builder = new XMemcachedClientBuilder(
                AddrUtil.getAddresses("server1:11211,server2:11211")
        );

// 连接池大小建议：CPU 核心数 * 2 + 1
builder.

setConnectionPoolSize(Runtime.getRuntime().

availableProcessors() *2+1);

// 连接超时时间
        builder.

setConnectTimeout(3000);

// 操作超时时间
builder.

setOpTimeout(2000);

// 启用心跳检测
builder.

setEnableHeartBeat(true);
```

**3. 批量操作优化**

```java
/**
 * 批量获取优化
 *
 * 使用 getMulti 一次获取多个 key，减少网络往返
 */
public <T> Map<String, T> multiGet(Collection<String> keys) {
    try {
        // getMulti 内部使用多线程并行获取
        // 每个服务器节点一个线程
        return memcachedClient.get(keys);
    } catch (Exception e) {
        log.error("Multi-get error", e);
        return Collections.emptyMap();
    }
}
```

---

## 八、高频面试题

**问题 1：Memcached 和 Redis 有什么区别？如何选择？**

**答：**

| 对比维度 | Memcached       | Redis                           | 选择建议                   |
|------|-----------------|---------------------------------|------------------------|
| 数据结构 | 仅支持 String      | 丰富（List/Set/Hash/ZSet/Stream 等） | 需要复杂数据结构选 Redis        |
| 持久化  | 不支持             | RDB/AOF/混合持久化                   | 需要数据持久化选 Redis         |
| 分布式  | 客户端分片           | 原生 Cluster                      | 大规模集群选 Redis           |
| 线程模型 | 多线程             | 单线程（6.0+ IO 多线程）                | 简单 KV 高并发可考虑 Memcached |
| 内存管理 | Slab Allocation | jemalloc                        | Redis 内存利用率更高          |
| 功能特性 | 简单              | 事务/Lua/发布订阅/Stream              | 需要高级功能选 Redis          |

**选择建议：**

- **选 Memcached**：简单 KV 缓存、多核 CPU 利用率高、已有成熟运维体系
- **选 Redis**：复杂数据结构、持久化需求、消息队列、分布式锁、排行榜等

**问题 2：Memcached 的 Slab Allocation 机制是什么？有什么优缺点？**

**答：**

**机制原理：**

1. **内存预分配**：启动时将内存划分为多个 Slab Class
2. **固定 Chunk 大小**：每个 Slab Class 包含大小相同的 Chunk
3. **按大小分配**：存储数据时，选择能容纳数据的最小 Chunk

**优点：**

1. **避免内存碎片**：预先分配固定大小的内存块，减少碎片
2. **分配效率高**：O(1) 时间复杂度的内存分配
3. **内存复用**：Chunk 释放后可以被重新使用

**缺点：**

1. **空间浪费**：数据大小与 Chunk 不匹配时产生浪费
2. **无法调整**：运行时无法调整 Slab 配置
3. **内存浪费率**：最坏情况下可能浪费约 50% 的 Chunk 空间

**优化方法：**

- 根据业务数据分布调整增长因子 `-f`
- 监控 `stats slabs` 输出，分析各 Slab 使用情况
- 合理设置最小 Chunk 大小 `-n`

**问题 3：Memcached 如何实现分布式？一致性哈希的原理是什么？**

**答：**

**分布式实现：**
Memcached 不支持服务端集群，需要客户端实现分布式：

1. **客户端分片**：客户端决定数据存储在哪个节点
2. **路由算法**：通过哈希算法计算 key 应该存储的节点

**一致性哈希原理：**

```
普通哈希：hash(key) % N
问题：节点增减时，大量 key 需要迁移

一致性哈希：
1. 将哈希空间组织成环（0 ~ 2^32-1）
2. 将节点映射到环上
3. 将 key 映射到环上
4. key 存储在顺时针方向第一个节点

优点：节点增减只影响相邻节点的数据
```

**虚拟节点：**

- 每个物理节点映射多个虚拟节点
- 解决数据倾斜问题
- 虚拟节点均匀分布在哈希环上

**问题 4：Memcached 的 LRU 淘汰策略是如何工作的？**

**答：**

**LRU（Least Recently Used）最近最少使用：**

1. **数据结构**：每个 Slab Class 维护一个 LRU 链表
2. **访问时移动**：访问数据时，将其移动到链表头部
3. **淘汰尾部**：内存不足时，淘汰链表尾部的数据

**Memcached 的 LRU 优化：**

```
┌─────────────────────────────────────────────────────────────────┐
│                    LRU 分层结构                                  │
│                                                                  │
│  HOT LRU（新生数据）──► WARM LRU（活跃数据）──► COLD LRU（淘汰区）│
│       20%                  40%                   40%             │
│                                                                  │
│  数据流转：                                                      │
│  1. 新数据进入 HOT LRU                                          │
│  2. HOT 满后，最久未用的数据进入 WARM                           │
│  3. WARM 满后，最久未用的数据进入 COLD                          │
│  4. COLD 区数据被淘汰                                           │
└─────────────────────────────────────────────────────────────────┘
```

**配置参数：**

- `hot_lru_pct`：HOT LRU 百分比，默认 20%
- `warm_lru_pct`：WARM LRU 百分比，默认 40%

**问题 5：Memcached 如何实现分布式锁？有什么局限性？**

**答：**

**实现方式：**

```java
// 加锁：使用 add 操作（原子性）
boolean success = memcachedClient.add(lockKey, lockValue, expireSeconds);

// 释放锁：验证后删除
String value = memcachedClient.get(lockKey);
if(lockValue.

equals(value)){
        memcachedClient.

delete(lockKey);
}
```

**局限性：**

1. **无法续期**：不支持 Redis 的看门狗机制
2. **非原子释放**：get + delete 不是原子操作
3. **不可重入**：不支持同一线程重复获取锁
4. **无等待队列**：不支持公平锁

**建议：**

- 简单场景可以使用 Memcached 分布式锁
- 复杂场景（可重入、续期、公平锁）建议使用 Redis Redisson

**问题 6：Memcached 的多线程模型是怎样的？**

**答：**

**线程模型架构：**

```
┌─────────────────────────────────────────────────────────────────┐
│                    Memcached 线程模型                            │
│                                                                  │
│  ┌─────────────┐                                                │
│  │ 主线程       │  监听端口，接受新连接                          │
│  └──────┬──────┘                                                │
│         │                                                        │
│         ▼ 分发连接到工作线程                                     │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    工作线程池                             │   │
│  │   ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐    │   │
│  │   │ Worker 1│  │ Worker 2│  │ Worker 3│  │ Worker N│    │   │
│  │   │ libevent│  │ libevent│  │ libevent│  │ libevent│    │   │
│  │   └─────────┘  └─────────┘  └─────────┘  └─────────┘    │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  每个 Worker 线程：                                              │
│  - 独立的事件循环（libevent）                                   │
│  - 处理分配给自己的连接                                         │
│  - 独立的 LRU 锁，减少竞争                                      │
└─────────────────────────────────────────────────────────────────┘
```

**特点：**

1. **主线程**：负责监听和分发连接
2. **工作线程**：处理具体的读写请求
3. **libevent**：基于事件驱动的 IO 多路复用
4. **分段锁**：每个 Slab Class 有独立的锁

**线程数配置：**

```bash
# 设置工作线程数，建议等于 CPU 核心数
memcached -t 4
```

**问题 7：如何监控 Memcached 的运行状态？**

**答：**

**1. 命令行监控：**

```bash
# 连接 Memcached
telnet localhost 11211

# 查看基本统计
stats
STAT pid 1234
STAT uptime 3600
STAT time 1640000000
STAT version 1.6.9
STAT pointer_size 64
STAT rusage_user 0.123
STAT rusage_system 0.456
STAT curr_items 1000
STAT total_items 5000
STAT bytes 1048576
STAT curr_connections 10
STAT total_connections 100
STAT connection_structures 20
STAT cmd_get 10000
STAT cmd_set 5000
STAT get_hits 8000
STAT get_misses 2000
STAT evictions 100
STAT bytes_read 10485760
STAT bytes_written 20971520
STAT limit_maxbytes 67108864

# 查看 Slab 统计
stats slabs

# 查看 Item 统计
stats items

# 查看大小分布
stats sizes
```

**2. 关键指标：**

| 指标                             | 说明    | 告警阈值        |
|--------------------------------|-------|-------------|
| get_hits/(get_hits+get_misses) | 命中率   | < 80%       |
| evictions                      | 淘汰次数  | > 0 需关注     |
| bytes/limit_maxbytes           | 内存使用率 | > 85%       |
| curr_connections               | 当前连接数 | 接近 maxconns |

**3. Prometheus + Grafana：**

```yaml
# 使用 memcached_exporter
docker run -d -p 9150:9150 \
prom/memcached-exporter \
--memcached.address=memcached:11211
```

---

## 九、最佳实践

### 9.1 使用场景建议

| 场景         | 是否适合 Memcached | 说明                 |
|------------|----------------|--------------------|
| Session 缓存 | ✅ 适合           | 简单 KV 结构，读取频繁      |
| 页面缓存       | ✅ 适合           | 整页 HTML 缓存         |
| 数据库查询缓存    | ✅ 适合           | 简单结果缓存             |
| 分布式锁       | ⚠️ 可用          | 简单场景可用，复杂场景用 Redis |
| 排行榜        | ❌ 不适合          | 需要 ZSet 结构         |
| 消息队列       | ❌ 不适合          | 不支持 List 和发布订阅     |
| 计数器        | ⚠️ 可用          | 支持 incr，但功能有限      |

### 9.2 配置最佳实践

```bash
# 生产环境推荐配置
memcached \
  -m 4096 \           # 内存大小，根据实际需求设置
  -c 2048 \           # 最大连接数
  -t 4 \              # 工作线程数，等于 CPU 核心数
  -f 1.25 \           # 增长因子
  -n 48 \             # 最小 Chunk 大小
  -I 1m \             # 最大 item 大小
  -v \                # 输出日志
  -u memcached \      # 运行用户
  -P /var/run/memcached.pid \  # PID 文件
  -l 0.0.0.0 \        # 监听地址
  -p 11211            # 端口
```

---

**维护者：** itzixiao  
**最后更新：** 2026-03-21  
**问题反馈：** 欢迎提 Issue 或 PR
