# Redis Sentinel 与 Redis Cluster 详解

## 一、Redis Sentinel（哨兵）

### 1. 什么是 Sentinel？有什么用？

**Sentinel 是什么？**
- Redis 官方提供的高可用（HA）解决方案
- 用于监控 Redis 主从复制架构
- 在 Master 故障时自动进行故障转移

**Sentinel 的三大核心功能：**

| 功能 | 说明 |
|------|------|
| 监控（Monitoring） | 持续检查 Master 和 Slave 是否正常运行 |
| 通知（Notification） | 当监控的实例出现问题时，通知管理员或其他应用程序 |
| 自动故障转移（Automatic Failover） | 当 Master 故障时，自动从 Slave 中选举新的 Master |

### 2. Sentinel 如何检测节点是否下线？

**心跳检测机制：**
- Sentinel 每 1 秒向所有节点发送 PING 命令
- 有效回复：+PONG、-LOADING、-MASTERDOWN
- 无效回复：超时无响应、其他错误回复

**配置参数：**
```
# 判断主观下线的超时时间（毫秒）
sentinel down-after-milliseconds mymaster 30000
```

**Sentinel 之间的通信：**
- 通过 Pub/Sub 机制相互发现和通信
- 频道：`__sentinel__:hello`
- 每个 Sentinel 每隔 2 秒发布自己的信息

### 3. 主观下线与客观下线的区别

| 特性 | 主观下线（SDOWN） | 客观下线（ODOWN） |
|------|------------------|------------------|
| 定义 | 单个 Sentinel 认为某个节点下线 | 足够数量的 Sentinel 认为某 Master 下线 |
| 判断条件 | 超时未收到有效 PING 回复 | ≥quorum 个 Sentinel 确认 |
| 触发条件 | 单节点判断 | 多节点共识 |
| 是否触发故障转移 | 否 | 是 |

**状态转换流程：**
```
Sentinel 发现 Master 无响应
        ↓
标记为 SDOWN（主观下线）
        ↓
向其他 Sentinel 发送确认请求
        ↓
    ┌───┴───┐
< quorum   ≥ quorum
    ↓         ↓
保持 SDOWN  标记为 ODOWN（客观下线）
              ↓
         触发故障转移
```

### 4. Sentinel 是如何实现故障转移的？

**故障转移完整流程：**

1. **发现问题** - 检测到 Master 下线，多数 Sentinel 确认 → 标记为 ODOWN
2. **选举 Leader Sentinel** - 多个 Sentinel 竞争成为 Leader，使用 Raft 算法选举
3. **选择新 Master** - 从 Slave 列表中选择一个作为新 Master
4. **提升新 Master** - 向选中的 Slave 发送 `SLAVEOF NO ONE`
5. **重新配置其他 Slave** - 让其他 Slave 复制新 Master
6. **更新配置** - 通过 Pub/Sub 通知客户端新 Master 地址

### 5. 为什么建议部署多个 Sentinel 节点？

| 原因 | 说明 |
|------|------|
| 避免单点故障 | 单 Sentinel 故障将失去监控和故障转移能力 |
| 减少误判 | 需要 quorum 个 Sentinel 同时认为下线才能触发故障转移 |
| 保证选举可靠性 | Leader 选举需要多数派投票同意 |

**部署建议：**
- 推荐 3 个 Sentinel（奇数个）
- 部署在不同机器/机房
- quorum = N/2 + 1（多数派）

### 6. Sentinel 如何选择出新的 master？

**筛选条件（排除不合格的 Slave）：**
1. 排除主观下线的 Slave
2. 排除最近 5 秒没有回复 INFO 命令的 Slave
3. 排除与旧 Master 断开连接时间过长的 Slave

**选举优先级：**

| 优先级 | 条件 | 说明 |
|--------|------|------|
| 1 | slave-priority | 数值越小优先级越高，0 表示永不成为 Master |
| 2 | replication offset | 复制偏移量大 = 数据更完整 |
| 3 | runid | 字典序最小的（相当于随机选择） |

### 7. 如何从 Sentinel 集群中选择出 Leader？

**选举算法：类 Raft 共识算法**

核心概念：
- **配置纪元（configuration epoch）**：选举的代次，单调递增
- **投票**：每个 Sentinel 在每个纪元只能投一票
- **多数派**：获得超过半数投票才能成为 Leader

**选举流程：**
1. 触发选举 - Sentinel 确认 Master 客观下线
2. 增加配置纪元 - 局部配置纪元 + 1
3. 请求投票 - 向其他 Sentinel 发送请求
4. 投票规则 - 每个 Sentinel 在每个纪元只能投一票，先到先得
5. 统计投票 - 收到超过半数投票 → 成为 Leader

### 8. Sentinel 可以防止脑裂吗？

**答案：不能完全防止，但可以减轻影响**

**什么是脑裂？**
网络分区时，出现两个或多个「主节点」，导致数据不一致。

**Sentinel 的防护措施：**

| 措施 | 说明 |
|------|------|
| 客观下线机制 | 需要 quorum 个 Sentinel 确认 |
| 配置纪元 | 新 Master 的纪元更大，Old Master 恢复后会降级 |
| min-replicas-to-write | Master 至少需要连接 N 个 Slave 才能接受写入 |

**配置建议：**
```
min-replicas-to-write 1
min-replicas-max-lag 10
```

### 9. Sentinel 客户端连接示例

**Jedis 连接 Sentinel：**
```java
Set<String> sentinels = new HashSet<>();
sentinels.add("192.168.1.1:26379");
sentinels.add("192.168.1.2:26379");
sentinels.add("192.168.1.3:26379");

JedisSentinelPool pool = new JedisSentinelPool(
    "mymaster", sentinels, poolConfig, timeout, password
);

try (Jedis jedis = pool.getResource()) {
    jedis.set("key", "value");
}
```

**Redisson 连接 Sentinel：**
```java
Config config = new Config();
config.useSentinelServers()
    .setMasterName("mymaster")
    .addSentinelAddress(
        "redis://192.168.1.1:26379",
        "redis://192.168.1.2:26379",
        "redis://192.168.1.3:26379"
    );

RedissonClient redisson = Redisson.create(config);
```

---

## 二、Redis Cluster

### 1. 为什么需要 Redis Cluster？

**单机 Redis 的限制：**
- **内存限制**：单机内存有限，数据量超过容量无法存储
- **性能瓶颈**：单机 QPS 上限约 10 万/秒
- **单点故障**：主从复制模式下，Master 故障需要手动切换

**Redis Cluster 解决的问题：**

| 问题 | 解决方案 |
|------|---------|
| 数据分片 | 将数据分散到多个节点 |
| 高可用 | 内置主从复制，自动故障转移 |
| 水平扩展 | 支持动态添加/删除节点 |

**对比：**

| 特性 | 主从复制 | Sentinel | Cluster |
|------|---------|----------|---------|
| 数据分片 | 否 | 否 | 是 |
| 高可用 | 否 | 是 | 是 |
| 自动故障转移 | 否 | 是 | 是 |
| 水平扩展 | 否 | 否 | 是 |
| 额外组件 | 否 | Sentinel | 否 |

### 2. Redis Cluster 是如何分片的？

**哈希槽（Hash Slot）分片机制：**
- Redis Cluster 将所有数据划分为 **16384 个哈希槽**
- 每个键值对根据 key 计算出一个槽位号：`slot = CRC16(key) % 16384`
- 每个节点负责一部分槽位

**分片示例（3 个主节点）：**
```
Node A: 槽位 0 - 5460      (5461 个槽)
Node B: 槽位 5461 - 10922  (5462 个槽)
Node C: 槽位 10923 - 16383 (5461 个槽)
```

**Hash Tags（哈希标签）：**
对于包含 `{}` 的 key，只计算 `{}` 内的内容：
```
user:{1000}:name → 只计算 "1000" 的哈希值
user:{1000}:age  → 只计算 "1000" 的哈希值
# 这两个 key 会分配到同一个槽
```

### 3. 为什么 Redis Cluster 的哈希槽是 16384 个？

| 原因 | 说明 |
|------|------|
| 节点数量权衡 | 支持最多约 1000 个节点，每个节点约 16 个槽位 |
| Gossip 消息大小 | 16384 槽位约 2KB，65536 槽位约 8KB |
| CRC16 特性 | 16384 = 65536 / 4，取模后分布均匀 |
| 2 的幂次方 | 16384 = 2^14，计算效率高 |

### 4. 如何确定给定 key 的应该分布到哪个哈希槽中？

**槽位计算规则：**
```
slot = CRC16(key) % 16384
```

**CLUSTER KEYSLOT 命令：**
```
> CLUSTER KEYSLOT user:1000
(integer) 12539

> CLUSTER KEYSLOT {user}:1000:name
(integer) 5474
```

**客户端路由流程：**
1. 计算 key 的槽位
2. 查找槽位对应的节点（本地缓存或通过 CLUSTER SLOTS 命令获取）
3. 连接正确的节点执行命令
4. 收到 MOVED 重定向时更新本地槽位映射

### 5. Redis Cluster 支持重新分配哈希槽吗？

**支持的！** Redis Cluster 支持在线重新分配哈希槽（Resharding）。

**Resharding 场景：**
- 扩容：添加新节点，迁移部分槽位到新节点
- 缩容：删除节点，将槽位迁移到其他节点
- 负载均衡：调整各节点的槽位数量

**迁移命令：**
```bash
# 添加新节点
redis-cli --cluster add-node new_host:port existing_host:port

# 重新分配槽位
redis-cli --cluster reshard host:port \
  --cluster-from <source-node-id> \
  --cluster-to <target-node-id> \
  --cluster-slots <number-of-slots> \
  --cluster-yes

# 删除节点
redis-cli --cluster del-node host:port <node-id>
```

### 6. Redis Cluster 扩容缩容期间可以提供服务吗？

**答案：可以！** Redis Cluster 支持在线扩容缩容。

**迁移期间的处理：**

| 请求类型 | 迁移前节点 | 迁移后节点 |
|---------|-----------|-----------|
| 读取未迁移 key | 正常响应 | - |
| 读取已迁移 key | ASK 重定向 | 正常响应（需 ASKING） |
| 写入未迁移 key | 正常处理 | - |
| 写入已迁移 key | MOVED 重定向 | 正常响应 |

**MOVED vs ASK 重定向：**
- **MOVED**：槽位永久迁移，客户端应更新缓存
- **ASK**：槽位临时迁移（迁移中），只对当前请求有效

### 7. Redis Cluster 中的节点是怎么进行通信的？

**Gossip 协议：**
- 去中心化的分布式协议
- 每个节点定期随机选择几个节点交换信息
- 信息像八卦一样传播到整个集群

**Gossip 消息类型：**

| 类型 | 说明 |
|------|------|
| PING | 检测节点是否存活，包含发送者的集群状态信息 |
| PONG | 响应 PING 消息 |
| MEET | 邀请新节点加入集群 |
| FAIL | 广播某节点已下线 |

**Gossip 优缺点：**

| 优点 | 缺点 |
|------|------|
| 去中心化，无单点故障 | 信息传播有延迟（秒级） |
| 容错性强 | 消息冗余，有一定网络开销 |
| 扩展性好 | 最终一致性，不保证实时一致 |

### 8. Cluster 客户端连接示例

**Jedis 连接 Cluster：**
```java
Set<HostAndPort> nodes = new HashSet<>();
nodes.add(new HostAndPort("192.168.1.1", 6379));
nodes.add(new HostAndPort("192.168.1.2", 6379));
nodes.add(new HostAndPort("192.168.1.3", 6379));

JedisCluster jedisCluster = new JedisCluster(nodes, 2000, 2000, 5, poolConfig);

jedisCluster.set("user:1000", "value");
String value = jedisCluster.get("user:1000");
```

**Redisson 连接 Cluster：**
```java
Config config = new Config();
config.useClusterServers()
    .addNodeAddress(
        "redis://192.168.1.1:6379",
        "redis://192.168.1.2:6379",
        "redis://192.168.1.3:6379"
    )
    .setScanInterval(2000);

RedissonClient redisson = Redisson.create(config);

// 使用分布式锁
RLock lock = redisson.getLock("myLock");
lock.lock();
try {
    // 执行业务
} finally {
    lock.unlock();
}
```

---

## 三、Redis 与 Redisson 分布式锁

### 1. Redis 原生分布式锁

**基本实现：**
```java
// 加锁（SET NX EX 原子命令）
public boolean tryLock(String lockKey, String requestId, int expireTime) {
    String result = jedis.set(lockKey, requestId, "NX", "EX", expireTime);
    return "OK".equals(result);
}

// 解锁（Lua 脚本保证原子性）
public boolean unlock(String lockKey, String requestId) {
    String script = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "    return redis.call('del', KEYS[1]) " +
        "else " +
        "    return 0 " +
        "end";
    Object result = jedis.eval(script, 
        Collections.singletonList(lockKey), 
        Collections.singletonList(requestId));
    return Long.valueOf(1).equals(result);
}
```

**Redis 原生锁的问题：**
- 不可重入
- 无自动续期
- 无法实现阻塞等待
- 主从切换可能丢锁

### 2. Redisson 分布式锁

**特点：**
- 可重入
- 看门狗自动续期
- 阻塞等待
- 支持公平锁、读写锁等

**使用示例：**
```java
// 可重入锁
RLock lock = redisson.getLock("myLock");
lock.lock();  // 看门狗自动续期
try {
    // 执行业务
} finally {
    lock.unlock();
}

// 尝试获取锁
boolean acquired = lock.tryLock(10, 30, TimeUnit.SECONDS);
if (acquired) {
    try {
        // 执行业务
    } finally {
        lock.unlock();
    }
}
```

**Redisson 锁类型：**

| 类型 | 说明 |
|------|------|
| 可重入锁 | 同一线程可多次获取锁 |
| 公平锁 | 按请求顺序获取锁 |
| 读写锁 | 读读共享，读写互斥，写写互斥 |
| 联锁 | 同时获取多个锁 |
| 红锁 | 在多个 Redis 节点上加锁 |
| 信号量 | 控制同时访问的线程数量 |
| 闭锁 | 等待多个线程完成 |

### 3. 对比

| 功能 | Redis 原生 | Redisson |
|------|-----------|----------|
| 可重入 | 不支持 | 支持 |
| 自动续期 | 不支持 | 看门狗机制 |
| 阻塞等待 | 需自己实现 | 支持 |
| 公平锁 | 不支持 | 支持 |
| 读写锁 | 不支持 | 支持 |
| API 简洁度 | 较复杂 | 简洁 |

### 4. 最佳实践

1. **锁的粒度要小** - 使用细粒度锁，减少竞争
2. **锁内业务要短** - 只包裹必要的临界区代码
3. **必须 finally 释放锁** - 确保异常时锁能释放
4. **使用看门狗** - 不手动设置过期时间
5. **业务层做好幂等** - 分布式锁只是辅助手段
6. **监控锁的使用情况** - 锁获取失败次数、持有时间等
