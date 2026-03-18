# Redis Cluster集群详解

## 一、Redis Cluster 概述

### 1.1 为什么需要 Cluster？

**单机 Redis 的局限性**：

- 内存容量有限（最大 64GB）
- QPS 受限（单线程，约 10 万/秒）
- 数据量增长快于单机容量
- 高并发场景性能瓶颈

**Cluster 解决方案**：

- 数据分片：分散到多个节点
- 水平扩展：增加节点提升容量和性能
- 高可用：主从复制 + 自动故障转移

---

### 1.2 Cluster 架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Redis Cluster 架构                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌──────────────┐      ┌──────────────┐                  │
│   │  Master 1    │      │  Master 2    │                  │
│   │  :7000       │      │  :7001       │                  │
│   │  [0-5460]    │      │  [5461-10922]│                  │
│   │      │       │      │      │       │                  │
│   │      ├──┐    │      │    ┌─┤       │                  │
│   │      │  ↓   │      │    ↓ │       │                  │
│   │  Slave 1     │      │  Slave 2     │                  │
│   │  :7003       │      │  :7004       │                  │
│   └──────────────┘      └──────────────┘                  │
│                                                             │
│   ┌──────────────┐                                          │
│   │  Master 3    │                                          │
│   │  :7002       │                                          │
│   │  [10923-16383]│                                         │
│   │      │       │                                          │
│   │      ├──┐    │                                          │
│   │      │  ↓   │                                          │
│   │  Slave 3     │                                          │
│   │  :7005       │                                          │
│   └──────────────┘                                          │
│                                                             │
│   特点：                                                    │
│   - 3 个主节点（分片），每个负责一部分槽位                   │
│   - 3 个从节点（备份），提供高可用                           │
│   - 客户端可连接任意节点                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、数据分片机制

### 2.1 槽位分配

**槽位总数**：16384 个（0-16383）

**分片公式（完整）**：

```
slot = CRC16(key, "XMODEM") & 0x3FFF  # 等价于 % 16384
```

- Redis 使用的是 **CRC16/XMODEM** 算法（非标准 CRC16）；
- `& 0x3FFF` 是位运算优化（0x3FFF = 16383），效率高于取模；
- 示例验证：
  ```bash
  127.0.0.1:7000> CLUSTER KEYSLOT user:1001
  (integer) 5320  # 实际是 CRC16/XMODEM("user:1001") & 0x3FFF
  ```

**哈希标签（Hash Tag）- 解决多 key 操作问题**：

- 语法：`key{tag}`，Redis 仅对 `{}` 内的 `tag` 计算哈希值；
- 示例：
  ```bash
  # 以下 key 会分配到同一槽位（因为 {1001} 相同）
  127.0.0.1:7000> CLUSTER KEYSLOT user:{1001}:info
  (integer) 5320
  127.0.0.1:7000> CLUSTER KEYSLOT user:{1001}:orders
  (integer) 5320
  
  # 无标签的 key 按完整字符串计算
  127.0.0.1:7000> CLUSTER KEYSLOT user:1001:info
  (integer) 12345
  ```
- 注意：`{}` 内为空（如 `user{}:1001`）或无 `{}`，则按完整 key 计算。

# 查看槽位归属

127.0.0.1:7000> CLUSTER SLOTS

1)
    1) (integer) 0
    2) (integer) 5460
    3)
        1) "127.0.0.1"
        2) (integer) 7000
        3) "node_id_1"
    4)
        1) "127.0.0.1"
        2) (integer) 7003
        3) "node_id_4"

```

---

### 2.2 为什么是 16384 个槽？

**16384 槽位的核心原因（Redis 官方解释）**：
1. **Gossip 协议开销**（最核心）：
   - 集群节点间通过 Gossip 消息交换槽位状态，每个槽位用 **2bit** 表示状态（0=未分配，1=正常，2=PFail，3=Fail）；
   - 16384 槽位 = 16384 × 2bit = 4096 字节 ≈ 4KB（消息头大小）；
   - 若用 65536 槽位 = 16KB，网络开销增加 4 倍，违背 Redis "轻量级"设计原则；
2. **实际使用场景**：
   - 生产环境中 Redis 集群节点数极少超过 100 个，16384 槽位完全足够；
   - 每个主节点分配约 100-2000 个槽位，便于均衡分片；
3. **历史兼容**：
   - 早期 Redis 集群设计时，考虑到与 Sentinel 模式的兼容，16384 是折中值；
   - 位运算优化是附加优势，非核心原因。

---

### 2.3 哈希槽分布

**查看集群信息**：
```bash
# 查看槽位分布
CLUSTER SLOTS

# 查看节点信息
CLUSTER NODES

# 查看键的槽位
CLUSTER KEYSLOT key_name
```

**示例输出**：

```bash
127.0.0.1:7000> CLUSTER NODES
a1b2c3d4e5f6g7h8i9j0 127.0.0.1:7000@17000 myself,master - 0 0 1 connected 0-5460
k1l2m3n4o5p6q7r8s9t0 127.0.0.1:7001@17001 master - 0 1647252000000 2 connected 5461-10922
u1v2w3x4y5z6a7b8c9d0 127.0.0.1:7002@17002 master - 0 1647252000000 3 connected 10923-16383
e1f2g3h4i5j6k7l8m9n0 127.0.0.1:7003@17003 slave a1b2c3d4e5f6g7h8i9j0 - 0 1647252000000 1 disconnected
o1p2q3r4s5t6u7v8w9x0 127.0.0.1:7004@17004 slave k1l2m3n4o5p6q7r8s9t0 - 0 1647252000000 2 disconnected
y1z2a3b4c5d6e7f8g9h0 127.0.0.1:7005@17005 slave u1v2w3x4y5z6a7b8c9d0 - 0 1647252000000 3 disconnected
```

---

## 三、客户端请求路由

### 3.1 请求路由流程

```
┌─────────────────────────────────────────────────────────────┐
│                  Cluster 请求路由流程                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   客户端发送命令                                            │
│          │                                                  │
│          ▼                                                  │
│   计算 key 的槽位（CRC16 % 16384）                          │
│          │                                                  │
│          ▼                                                  │
│   检查本地槽位映射表                                        │
│          │                                                  │
│          ▼                                                  │
│   ┌─────────────────┐                                       │
│   │ 槽位在本地？    │                                       │
│   └───────┬─────────┘                                       │
│           │                                                 │
│     ┌─────┴─────┐                                           │
│     │           │                                           │
│    是          否                                           │
│     │           │                                           │
│     ▼           ▼                                           │
│ 执行命令    返回重定向响应                                  │
│             (MOVED/ASK)                                     │
│                 │                                           │
│                 ▼                                           │
│         更新本地槽位映射                                    │
│                 │                                           │
│                 ▼                                           │
│         连接正确节点执行                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### 3.2 重定向机制

#### MOVED 重定向（永久迁移）

**场景**：槽位已永久迁移到其他节点

**响应格式**：

```bash
-MOVED <slot> <ip>:<port>
```

**处理流程**：

```java
// 伪代码
public <T> T executeCommand(String key, Command command) {
    int slot = calculateSlot(key);
    Node node = getMasterNodeForSlot(slot);
    
    try {
        return node.execute(command);
    } catch (MovedException e) {
        // 更新槽位映射
        updateSlotMapping(e.getSlot(), e.getNewNode());
        
        // 重新执行
        return e.getNewNode().execute(command);
    }
}
```

---

#### ASK 重定向（临时迁移）

**ASK 重定向完整逻辑**：

1. **触发条件**：
    - 槽位处于"迁移中"状态（源节点仍持有槽位，但部分 key 已迁移到目标节点）；
    - 客户端请求的 key 恰好是已迁移到目标节点的那部分；
2. **处理流程**：
   ```java
   public <T> T executeCommand(String key, Command command) {
       int slot = calculateSlot(key);
       Node node = getMasterNodeForSlot(slot);
       
       try {
           return node.execute(command);
       } catch (AskException e) {
           Node targetNode = e.getTargetNode();
           // 发送 ASKING 命令：告诉目标节点"允许执行当前槽位的命令"（临时授权）
           targetNode.sendCommand(ASKING);
           // 执行命令（仅本次请求有效，不更新本地槽位映射）
           return targetNode.execute(command);
       }
   }
   ```
3. **关键区别**：
    - `ASKING` 命令仅对**下一条命令**生效，目标节点不会持久化该授权；
    - ASK 重定向是"临时解决方案"，迁移完成后会转为 MOVED 重定向。

---

### 3.3 MOVED vs ASK 的区别

| 类型        | 说明     | 处理方式              | 是否更新映射 |
|-----------|--------|-------------------|--------|
| **MOVED** | 槽位永久迁移 | 更新槽位映射，重新发送       | 是      |
| **ASK**   | 槽位正在迁移 | 发送 ASKING，仅当前请求有效 | 否      |

---

## 四、集群运维

### 4.1 创建集群

**使用 redis-cli 工具**：

```bash
# Redis 6.0+ 带密码创建集群（生产环境必须）
redis-cli --cluster create \
  127.0.0.1:7000 127.0.0.1:7001 127.0.0.1:7002 \
  127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
  --cluster-replicas 1 \
  --cluster-yes \
  -a your_password  # Redis 6.0+ 密码参数（5.0- 需在节点配置中设置 requirepass）

# 注意：
# 1. Redis 5.0- 创建集群需先在 redis.conf 中配置 requirepass 和 masterauth；
# 2. --cluster-replicas 1 表示"每个主节点对应 1 个从节点"；
# 3. 节点配置必须开启 cluster-enabled yes。
```

**Redis Cluster 节点核心配置（redis-7000.conf）**：

```bash
# 开启集群模式（必须）
cluster-enabled yes
# 集群配置文件（自动生成，无需手动修改）
cluster-config-file nodes-7000.conf
# 集群节点超时时间（毫秒）
cluster-node-timeout 15000
# 端口
port 7000
# 绑定地址
bind 0.0.0.0
# 后台运行
daemonize yes
# 日志文件
logfile "redis-7000.log"
# 数据目录
dir /data/redis/7000
# 密码（Redis 6.0+）
requirepass your_password
masterauth your_password
# 持久化（推荐混合持久化）
appendonly yes
aof-use-rdb-preamble yes
# 内存限制
maxmemory 4gb
maxmemory-policy volatile-lru
```

---

### 4.2 扩容集群

#### 添加新主节点

```bash
# 正确的缩容主节点步骤：
# 1. 查看待删除节点的 ID
redis-cli -h 127.0.0.1 -p 7000 CLUSTER NODES | grep master

# 2. 迁移该节点的所有槽位（批量迁移，无需手动输入槽位数）
redis-cli --cluster reshard 127.0.0.1:7000 \
  --cluster-from <待删除主节点 ID> \
  --cluster-to <目标主节点 ID> \
  --cluster-slots 0  # 0 表示迁移该节点的所有槽位
  --cluster-yes      # 自动确认

# 3. 验证槽位已全部迁移（待删除节点的 slots 列为空）
redis-cli -h 127.0.0.1 -p 7000 CLUSTER NODES | grep <待删除节点 ID>

# 4. 删除节点
redis-cli --cluster del-node 127.0.0.1:7000 <待删除节点 ID>
```

---

#### 添加从节点

```bash
# 启动新从节点（7007）
redis-server redis-7007.conf

# 指定为主节点 7006 的从节点
redis-cli --cluster add-node 127.0.0.1:7007 127.0.0.1:7006 \
  --cluster-slave
```

---

### 4.3 缩容集群

#### 删除从节点

```bash
# 1. 查看节点信息
CLUSTER NODES

# 2. 删除节点
redis-cli --cluster del-node 127.0.0.1:7000 <node_id_to_remove>
```

---

#### 删除主节点

```bash
# 1. 迁移该节点的槽位到其他主节点
redis-cli --cluster reshard 127.0.0.1:7000 \
  --cluster-from <要删除的 node_id> \
  --cluster-to <目标 node_id> \
  --cluster-slots <槽位数>

# 2. 确保槽位全部迁移完成后，删除节点
redis-cli --cluster del-node 127.0.0.1:7000 <node_id>
```

---

### 4.4 故障恢复

**Redis Cluster 自动故障转移流程（无需 Sentinel）**：

1. **故障检测**：
    - 节点间通过 Gossip 消息检测健康状态，超过 `cluster-node-timeout` 未响应则标记为 PFail（疑似下线）；
    - 多数主节点确认 PFail 后，标记为 Fail（客观下线）；
2. **选举新主节点**：
    - 故障主节点的从节点发起选举，需获得半数以上主节点的投票；
    - 优先级最高的从节点（`replica-priority` 最小）当选新主节点；
3. **槽位迁移**：
    - 新主节点接管原主节点的所有槽位；
    - 集群节点更新槽位映射，客户端通过 MOVED 重定向到新主节点；
4. **恢复完成**：
    - 原主节点重启后，自动成为新主节点的从节点。

> **手动故障转移完整命令**：
> ```bash

# 在从节点执行（关键！必须在从节点执行）

127.0.0.1:7003> CLUSTER FAILOVER [选项]

```
> **选项说明**：
> 1. **无选项**（默认）：
>    - 安全故障转移：先与主节点确认，同步增量数据后再切换（推荐生产环境使用）；
> 2. **FORCE**：
>    - 强制故障转移：不与主节点确认，直接接管（主节点宕机时使用）；
> 3. **TAKEOVER**：
>    - 强制接管：跳过所有一致性检查（如不等待客观下线），仅用于测试/紧急恢复（生产环境禁用）。

---

#### 检查集群健康

```bash
# 检查集群状态
redis-cli --cluster check 127.0.0.1:7000

# 修复集群
redis-cli --cluster fix 127.0.0.1:7000

# 清理从节点
redis-cli --cluster call 127.0.0.1:7000 cluster saveconfig
```

---

## 五、Java 集成 Cluster

### 5.1 Jedis 连接 Cluster

```java
@Configuration
public class RedisClusterConfig {
    
    @Bean
    public JedisCluster jedisCluster() {
        Set<HostAndPort> nodes = new HashSet<>();
        nodes.add(new HostAndPort("192.168.1.10", 7000));
        nodes.add(new HostAndPort("192.168.1.10", 7001));
        nodes.add(new HostAndPort("192.168.1.10", 7002));
        nodes.add(new HostAndPort("192.168.1.10", 7003));
        nodes.add(new HostAndPort("192.168.1.10", 7004));
        nodes.add(new HostAndPort("192.168.1.10", 7005));
        
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(10);
        poolConfig.setMaxWaitMillis(3000); // 连接超时时间
        poolConfig.setTestOnBorrow(true);  // 借连接时测试
        
        // 完整构造函数（生产环境必须指定超时参数）
        return new JedisCluster(
            nodes,
            5000,        // 连接超时
            5000,        // 读取超时
            3,           // 重试次数
            "your_password", // 密码（Redis 6.0+）
            poolConfig   // 连接池配置
        );
    }
}

// 销毁 Bean 时关闭连接池（防止内存泄漏）
@PreDestroy
public void destroy() {
    if (jedisCluster != null) {
        jedisCluster.close();
    }
}

@Service
public class UserService {
    
    @Autowired
    private JedisCluster jedisCluster;
    
    public void saveUser(User user) {
        String key = "user:" + user.getId();
        String value = JSON.toJSONString(user);
        jedisCluster.set(key, value);
    }
    
    public User getUser(Long id) {
        String key = "user:" + id;
        String value = jedisCluster.get(key);
        return JSON.parseObject(value, User.class);
    }
}
```

---

### 5.2 Spring Data Redis 连接 Cluster

```java
@Configuration
public class RedisClusterConfig {
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisClusterConfiguration clusterConfig = 
            new RedisClusterConfiguration();
        
        // 添加集群节点
        clusterConfig.clusterNode("192.168.1.10", 7000);
        clusterConfig.clusterNode("192.168.1.10", 7001);
        clusterConfig.clusterNode("192.168.1.10", 7002);
        clusterConfig.clusterNode("192.168.1.10", 7003);
        clusterConfig.clusterNode("192.168.1.10", 7004);
        clusterConfig.clusterNode("192.168.1.10", 7005);
        
        // 密码
        clusterConfig.setPassword(RedisPassword.of("your_password"));
        // 集群超时配置
        clusterConfig.setMaxRedirects(3); // 最大重定向次数
        clusterConfig.setTimeout(Duration.ofMillis(5000)); // 连接超时
        
        // Lettuce 连接池配置（生产环境必须！）
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(200);
        poolConfig.setMaxIdle(50);
        poolConfig.setMinIdle(10);
        poolConfig.setMaxWait(Duration.ofMillis(3000));
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofMillis(5000)) // 命令超时
            .build();
        
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }
}
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return new RedisTemplate<>(redisConnectionFactory());
    }
}
```

---

## 六、高频面试题

### 问题 1:Redis Cluster 如何分片？为什么是 16384 个槽？

**参考答案**：

**分片机制**：

```
slot = CRC16(key) % 16384
```

**为什么是 16384？**

1. **节点数量权衡**：支持约 1000 个节点
2. **Gossip 消息大小**：16384 槽约 2KB，65536 槽约 8KB
3. **2 的幂次方**：16384 = 2^14，计算效率高

---

### 问题 2:Redis Cluster 如何处理请求路由？

**参考答案**：

**路由流程**：

1. 客户端计算 key 的槽位
2. 连接对应节点执行命令
3. 如果槽位迁移，收到重定向响应

**重定向类型**：

- **MOVED**：槽位永久迁移，更新本地映射
- **ASK**：槽位正在迁移，仅当前请求有效

---

### 问题 3:MOVED 和 ASK 的区别是什么？

**参考答案**：

| 类型    | 说明     | 处理方式              | 是否更新映射 |
|-------|--------|-------------------|--------|
| MOVED | 槽位永久迁移 | 更新槽位映射，重新发送       | 是      |
| ASK   | 槽位正在迁移 | 发送 ASKING，仅当前请求有效 | 否      |

---

### 问题 4:Redis Cluster 如何进行扩缩容？

**参考答案**：

**扩容**：

1. 添加新节点到集群
2. 使用 `reshard` 命令迁移槽位
3. 数据自动迁移（不影响服务）

**缩容**：

1. 迁移待删除节点的槽位
2. 确保槽位全部迁移完成
3. 删除节点

**命令**：

```bash
# 添加节点
redis-cli --cluster add-node <new_node> <existing_node>

# 迁移槽位
redis-cli --cluster reshard <node>

# 删除节点
redis-cli --cluster del-node <node> <node_id>
```

---

### 问题 5:Redis Cluster 的局限性有哪些？

**参考答案**：

**Redis Cluster 核心局限性（完整）**：

1. **多 key 操作限制**：
    - 不支持跨槽位的 MSET/MGET、SUNION、ZUNIONSTORE 等操作；
    - 解决方案：使用**哈希标签（Hash Tag）**，如 `user:{1001}:info`、`user:{1001}:orders`（`{}` 内的内容参与哈希计算，确保同一用户的
      key 在同一槽位）；
2. **事务/Lua 脚本限制**：
    - 事务/Lua 脚本只能操作单个槽位的 key；
    - 解决方案：拆分事务/脚本，或使用哈希标签；
3. **数据一致性**：
    - 主从复制是最终一致性，故障转移时可能丢失少量数据；
    - 无分布式锁原生支持，需依赖 Redlock 等方案；
4. **运维复杂度**：
    - 扩缩容需手动迁移槽位（或依赖工具）；
    - 不支持自动分片均衡，需人工干预；
5. **功能缺失**：
    - 不支持 Redis Cluster 模式下的 `SELECT` 命令（仅支持 db 0）；
    - 不支持 `BITOP` 跨槽位操作。

**哈希标签示例**：

```bash
# 这些 key 会在同一槽位
user:{1001}:info
user:{1001}:orders
user:{1001}:cart

# 因为 {} 内的内容相同
```

---

## 七、最佳实践

### 7.1 集群规模建议

```
# 推荐配置
3 主 3 从：适合中等规模（数据量 < 100GB）
6 主 6 从：适合大规模（数据量 < 500GB）
9 主 9 从：适合超大规模（数据量 > 500GB）

# 注意
- 主节点不宜过多（< 1000）
- 每个主节点至少配 1 个从节点
- Sentinel 数量 >= 3
```

---

### 7.2 监控指标

```bash
# 1. 集群健康检查（核心命令）
redis-cli --cluster check 127.0.0.1:7000 -a your_password

# 2. 集群状态详情
redis-cli -h 127.0.0.1 -p 7000 -a your_password CLUSTER INFO
# 关键指标：
# cluster_state: ok（集群状态，down 表示异常）
# cluster_slots_assigned: 16384（已分配槽位数，需等于 16384）
# cluster_slots_ok: 16384（健康槽位数）
# cluster_slots_pfail: 0（疑似下线槽位数）
# cluster_slots_fail: 0（下线槽位数）

# 3. 节点状态
redis-cli -h 127.0.0.1 -p 7000 -a your_password CLUSTER NODES
# 关键字段：
# connected：节点是否在线
# master/slave：节点角色
# connected 0-5460：主节点持有的槽位

# 4. 槽位迁移进度
redis-cli -h 127.0.0.1 -p 7000 -a your_password CLUSTER MIGRATE INFO
```

---

### 7.3 性能优化

**Redis Cluster 性能优化核心建议**：

1. **节点部署**：
    - 主从节点分散部署在不同物理机/可用区，避免单点故障；
    - 每个主节点内存不超过 8GB（减少 fork 耗时和故障恢复时间）；
2. **网络优化**：
    - 集群节点部署在同一机房，使用千兆/万兆网卡；
    - 禁用透明大页（THP）：`echo never > /sys/kernel/mm/transparent_hugepage/enabled`；
3. **客户端优化**：
    - 使用 Lettuce（非阻塞）替代 Jedis（阻塞），提升高并发性能；
    - 客户端连接池大小设置为 `CPU 核心数 × 2 + 1`，避免连接数过多；
4. **业务优化**：
    - 避免热点槽位（单个槽位 QPS 过高，导致单节点瓶颈）；
    - 批量操作使用 Pipeline，减少网络往返；
    - 禁用不必要的持久化（如纯缓存场景）。

---

## 八、总结

| 知识点     | 核心内容                                          |
|---------|-----------------------------------------------|
| 数据分片    | 16384 个槽位，CRC16/XMODEM 算法 & 0x3FFF 位运算        |
| 请求路由    | MOVED/ASK 重定向机制，客户端本地槽位缓存优化                   |
| 扩缩容     | reshard 命令迁移槽位（--cluster-slots 0 批量迁移）        |
| 高可用     | 自动故障转移（Gossip 检测→选举→槽位接管）                     |
| Java 集成 | Jedis/Spring Data Redis（必须配置连接池和超时）           |
| 局限性     | 多 key 操作限制（Hash Tag 解决）、事务/Lua 限制、无 SELECT 命令 |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15
