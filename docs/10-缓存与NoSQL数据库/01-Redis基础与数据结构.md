# Redis 基础与数据结构

## 一、Redis 简介

Redis（Remote Dictionary Server）是一个开源的、基于内存的键值存储数据库，常用于缓存、消息队列、会话管理等场景。

### 1.1 Redis 特点

- **纯内存操作**：数据存储在内存中，读写速度极快
- **单线程模型**：避免线程切换和锁竞争开销（命令执行阶段）
- **丰富的数据结构**：String、List、Set、Hash、ZSet 等
- **持久化支持**：RDB 快照、AOF 日志、混合持久化
- **高可用架构**：主从复制、哨兵模式、Cluster 集群
- **发布订阅**：支持消息队列场景

### 1.2 Redis 为什么这么快？

1. **纯内存操作**：单次操作微秒级别
2. **IO 多路复用**：使用 epoll/kqueue 等高效 IO 模型
3. **避免上下文切换**：无线程切换开销
4. **避免锁竞争**：无需加锁，执行效率高
5. **高效数据结构**：如 SDS、跳表、压缩列表等

**注意：** Redis 6.0 引入多线程 IO，但命令执行仍是单线程。

---

## 二、Redis 数据类型与底层实现

### 2.1 String（字符串）

**底层实现**：SDS（Simple Dynamic String，简单动态字符串）

```c
struct sdshdr {
    unsigned int len;     // 已使用长度
    unsigned int free;    // 空闲长度
    char buf[];           // 字节数组
};
```

**特点**：

- 支持自动扩容
- O(1) 获取字符串长度
- 二进制安全（可存储图片、序列化对象等）

**常用命令**：

```bash
SET key value
GET key
INCR key          # 原子自增
DECR key          # 原子自减
MSET k1 v1 k2 v2  # 批量设置
MGET k1 k2        # 批量获取
SETEX key 30 value  # 设置过期时间
```

**使用场景**：

- 缓存 Session、Token
- 计数器（点赞数、浏览量）
- 分布式锁
- 限流

---

### 2.2 List（列表）

**底层实现**：QuickList（双向链表 + ziplist）

- Redis 7.0 之前：ziplist 或 linkedlist
- Redis 7.0+：quicklist（结合了两者优点）

**特点**：

- 有序、可重复
- 支持两端插入和弹出
- 最大长度：2^32 - 1

**常用命令**：

```bash
LPUSH key value1 value2   # 左侧插入
RPUSH key value1 value2   # 右侧插入
LPOP key                  # 左侧弹出
RPOP key                  # 右侧弹出
BRPOP key timeout         # 阻塞式右侧弹出
LRANGE key start stop     # 获取范围元素
LLEN key                  # 获取列表长度
```

**使用场景**：

- 消息队列（LPUSH + BRPOP）
- 最新列表（微博关注列表）
- 栈和队列

---

### 2.3 Hash（哈希）

**底层实现**：ziplist（压缩列表）或 hashtable（哈希表）

**特点**：

- 适合存储对象
- field 无序
- 支持单个字段操作

**常用命令**：

```bash
HSET key field value       # 设置字段
HGET key field             # 获取字段
HMSET key f1 v1 f2 v2      # 批量设置
HMGET key f1 f2            # 批量获取
HGETALL key                # 获取所有字段
HDEL key field             # 删除字段
HKEYS key                  # 获取所有 field
HVALS key                  # 获取所有 value
```

**使用场景**：

- 对象存储（用户信息、商品信息）
- 购物车
- 配置项

---

### 2.4 Set（集合）

**底层实现**：intset（整数集合）或 hashtable

**特点**：

- 无序、不重复
- 支持交集、并集、差集运算

**常用命令**：

```bash
SADD key member1 member2   # 添加元素
SMEMBERS key               # 获取所有元素
SISMEMBER key member       # 判断是否包含
SCARD key                  # 获取元素数量
SINTER key1 key2           # 交集
SUNION key1 key2           # 并集
SDIFF key1 key2            # 差集
```

**使用场景**：

- 标签系统
- 共同好友/关注
- 去重统计
- 抽奖系统

---

### 2.5 ZSet（Sorted Set，有序集合）

**底层实现**：ziplist 或 skiplist（跳表）+ hashtable

**跳表结构**：

```
Level 4: 1 ──────────────────────────────────> NULL
Level 3: 1 ──────> 5 ────────────────────────> NULL
Level 2: 1 ──> 3 ──> 5 ──────> 8 ────────────> NULL
Level 1: 1 ──> 2 ──> 3 ──> 5 ──> 7 ──> 8 ────> NULL
```

**特点**：

- 有序、不重复
- 每个元素关联一个 score（分数）
- 支持范围查询

**常用命令**：

```bash
ZADD key score member1 score member2   # 添加元素
ZRANGE key start stop [WITHSCORES]     # 升序获取
ZREVRANGE key start stop               # 降序获取
ZREM key member                        # 删除元素
ZCARD key                              # 获取元素数量
ZRANK key member                       # 获取排名
ZCOUNT key min max                     # 统计分数范围内的元素
```

**使用场景**：

- 排行榜（成绩、销量）
- 延时队列（score = 执行时间戳）
- 带权重的队列

---

### 2.6 其他高级数据类型

#### Bitmap（位图）

**底层实现**：String

**特点**：

- 按 bit 存储，极度节省空间
- 支持位运算

**常用命令**：

```bash
SETBIT key offset value    # 设置位
GETBIT key offset          # 获取位
BITCOUNT key               # 统计 1 的数量
BITOP op destkey key [...] # 位运算
```

**使用场景**：

- 签到统计
- 布隆过滤器
- 状态标记

---

#### HyperLogLog（基数统计）

**底层实现**：稀疏存储 / 稠密存储

**特点**：

- 占用空间固定（12KB）
- 有误差（约 0.81%）
- 适合大规模数据统计

**常用命令**：

```bash
PFADD key element1 element2   # 添加元素
PFCOUNT key                   # 统计基数
PFMERGE destkey sourcekey [...]  # 合并多个 HLL
```

**使用场景**：

- UV 统计（网站访问量）
- 海量数据去重

---

#### Stream（流）

**底层实现**：Radix Tree（基数树）

**特点**：

- Redis 5.0+ 引入
- 支持消费者组
- 消息持久化

**常用命令**：

```bash
XADD key * field1 value1      # 添加消息
XREAD COUNT 2 STREAMS key id  # 读取消息
XGROUP CREATE key group $     # 创建消费者组
XREADGROUP GROUP group consumer ...  # 消费者组读取
```

**使用场景**：

- 消息队列
- 事件日志
- 数据流处理

---

## 三、Redis 事务与 Lua 脚本

### 3.1 事务基础

**命令**：

```bash
MULTI    # 开启事务
EXEC     # 执行事务
DISCARD  # 取消事务
WATCH    # 监视 key（乐观锁）
```

**特点**：

- **不支持回滚**：命令执行失败不会回滚已执行的命令
- **原子性**：事务中的命令要么全部执行，要么全部不执行
- **无隔离级别**：事务中的命令在 EXEC 之前不会被执行

**示例**：

```bash
MULTI
SET name redis
INCR counter
EXEC
```

---

### 3.2 Lua 脚本

**优势**：

- 保证原子性（脚本执行期间不会被中断）
- 减少网络开销
- 实现复杂逻辑

**示例 1：分布式锁释放**

```lua
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

**示例 2：限流**

```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local current = tonumber(redis.call('GET', key) or "0")

if current + 1 > limit then
    return 0
else
    redis.call('INCR', key)
    return 1
end
```

---

## 四、高频面试题

### 问题 1：Redis 有哪些基本数据结构？底层如何实现？

**参考答案**：

| 数据类型        | 底层实现                | 使用场景        |
|-------------|---------------------|-------------|
| String      | SDS                 | 缓存、计数器、分布式锁 |
| List        | QuickList           | 消息队列、最新列表   |
| Hash        | ziplist / hashtable | 对象存储、购物车    |
| Set         | intset / hashtable  | 标签、共同好友、去重  |
| ZSet        | ziplist / skiplist  | 排行榜、延时队列    |
| Bitmap      | String              | 签到、布隆过滤器    |
| HyperLogLog | 稀疏/稠密存储             | UV 统计       |
| Stream      | Radix Tree          | 消息队列        |

---

### 问题 2：String 类型的最大存储容量是多少？

**参考答案**：

- 最大存储：512MB
- 实际建议：不要超过 10KB，避免网络传输慢

---

### 问题 3：ZSet 的跳表实现原理是什么？

**参考答案**：

**跳表特点**：

1. 多层链表结构，高层节点跨度大
2. 查找时从高层开始，逐步下降
3. 平均时间复杂度：O(log n)

**查找过程**：

```
1. 从最高层开始查找
2. 如果当前节点 < 目标，向右移动
3. 如果当前节点 >= 目标，下降到下一层
4. 重复步骤 2-3，直到找到或到达最低层
```

**为什么不用红黑树？**

- 跳表实现更简单
- 范围查找效率更高
- 并发性能更好

---

### 问题 4：Redis 单线程为什么能支持高并发？

**参考答案**：

1. **内存操作速度快**：单次操作微秒级别
2. **IO 多路复用**：单线程监听多个连接，避免阻塞
3. **避免上下文切换**：无线程切换开销
4. **避免锁竞争**：无需加锁，执行效率高
5. **简单操作为主**：大部分操作 O(1) 或 O(log n)
6. **高效数据结构**：SDS、跳表、压缩列表等

---

### 问题 5：Redis 6.0 为什么引入多线程？

**参考答案**：

**背景**：

- Redis 瓶颈不在 CPU，而在网络 IO
- 单线程处理大量网络请求时 CPU 利用率不高

**多线程设计**：

- 仅 IO 线程多线程（读取请求、发送响应）
- **命令执行仍是单线程**
- 避免了加锁，保持简单性

**开启方式**：

```bash
io-threads 4
io-threads-do-reads yes
```

---

## 五、最佳实践

### 5.1 Key 命名规范

```bash
# 推荐
user:1001:info
order:20240101:detail

# 不推荐
user_1001
u1001
```

### 5.2 批量操作

```bash
# 使用 MSET/MGET 代替多次 SET/GET
MSET k1 v1 k2 v2 k3 v3
MGET k1 k2 k3

# 使用 Pipeline 减少网络往返
```

### 5.3 合理设置过期时间

```bash
# 避免永不过期导致内存膨胀
SETEX key 3600 value

# 添加随机值避免同时过期
SETEX key $((3600 + RANDOM % 300)) value
```

---

## 六、总结

| 知识点   | 核心内容                            |
|-------|---------------------------------|
| 数据类型  | String、List、Set、Hash、ZSet 及底层实现 |
| 高性能原因 | 内存操作、单线程、IO 多路复用、高效数据结构         |
| 事务    | MULTI/EXEC、不支持回滚、Lua 脚本保证原子性    |
| 应用场景  | 缓存、计数器、消息队列、排行榜、分布式锁            |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15
