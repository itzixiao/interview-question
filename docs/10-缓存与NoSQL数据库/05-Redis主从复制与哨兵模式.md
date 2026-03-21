# Redis主从复制与哨兵模式

## 一、主从复制

### 1.1 主从复制概述

**作用**：

- 数据冗余：从节点备份主节点数据
- 读写分离：主节点写，从节点读
- 高可用基础：故障转移的前提

**架构**：

```
         Master (主节点)
         /    |    \
        /     |     \
   Slave1  Slave2  Slave3
   (从 1)   (从 2)   (从 3)
```

---

### 1.2 主从复制流程

```
┌─────────────────────────────────────────────────────────────┐
│                     主从复制流程                            │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   从节点发送 PSYNC 命令                                     │
│          │                                                  │
│          ▼                                                  │
│   ┌──────────────────────────────────┐                     │
│   │  判断是否可以部分复制             │                     │
│   │  (检查 replication ID 和 offset) │                     │
│   └──────────────┬───────────────────┘                     │
│                  │                                          │
│        ┌─────────┴─────────┐                               │
│        ▼                   ▼                               │
│   全量复制            部分复制                              │
│   (首次同步)          (断线重连)                            │
│        │                   │                               │
│        ▼                   ▼                               │
│   主节点 BGSAVE        发送复制缓冲区                       │
│   生成 RDB             中的增量数据                         │
│        │                                                    │
│        ▼                                                    │
│   发送 RDB 给从节点                                         │
│        │                                                    │
│        ▼                                                    │
│   从节点加载 RDB                                             │
│        │                                                    │
│        ▼                                                    │
│   主节点发送复制缓冲区的新命令                              │
│        │                                                    │
│        ▼                                                    │
│   从节点执行新命令，完成同步                                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### 1.3 全量复制与部分复制

#### 全量复制（首次同步）

**触发条件**：

- 初次建立主从关系
- 从节点无法部分复制（replication ID 不匹配或 offset 超出范围）

**全量复制关键细节**：

1. 主节点执行 `BGSAVE` 时，会将期间的写命令写入**复制缓冲区**；
2. 从节点加载 RDB 前会先清空自身数据；
3. 从节点加载 RDB 完成后，主节点会发送复制缓冲区中的增量命令；
4. 全量复制的阻塞点：
    - 主节点：`BGSAVE` 的 fork 阶段短暂阻塞；
    - 从节点：加载 RDB 阶段阻塞（无法响应读请求，除非 `replica-serve-stale-data yes`）。

---

#### 部分复制（断线重连）

**部分复制触发条件**：

1. 从节点断线时间 < `repl-backlog-size` 能覆盖的时间；
2. 从节点的 `replication ID` 与主节点一致（主节点未故障重启）；
3. 从节点的 `offset` 在主节点的复制积压缓冲区范围内；
4. 主节点的 `repl-backlog-ttl` 未过期（无从节点连接时缓冲区保留时间）。

**判断依据**：

- Replication ID：标识主节点的身份
- Offset：记录已复制的命令位置

**流程**：

1. 从节点发送 `PSYNC <replication_id> <offset>`
2. 主节点检查 offset 是否在复制缓冲区中
3. 如果在，只发送增量命令
4. 如果不在，降级为全量复制

---

### 1.4 复制积压缓冲区（Replication Backlog）

**作用**：主节点专用，缓存最近的写命令，用于从节点断线重连时的部分复制

**配置（主节点）**：

```bash
# 主节点配置 - 复制积压缓冲区大小
repl-backlog-size 1mb           # 积压缓冲区大小（越大，允许从节点断线时间越长）
repl-backlog-ttl 3600           # 无从节点连接时，缓冲区保留时间（秒）
```

**注意**：

1. 该缓冲区属于**主节点**，从节点无此配置；
2. 复制缓冲区（client buffer）是主节点为每个从节点分配的临时缓冲区，无单独配置；
3. `repl-timeout` 是主从通信超时时间，非缓冲区超时。

---

### 1.5 主从复制配置

#### 主节点配置

```bash
# redis.conf
bind 0.0.0.0
port 6379
requirepass your_password

# 限制最大连接数
maxclients 10000
```

#### 从节点配置

```bash
# redis.conf（Redis 5.0+ 配置）
replicaof 192.168.1.100 6379   # 主节点地址（Redis 5.0- 为 slaveof）
masterauth your_password        # 主节点密码

# 从节点是否响应读请求（同步期间）
replica-serve-stale-data yes    # Redis 5.0- 为 slave-serve-stale-data

# 从节点是否只读
replica-read-only yes           # Redis 5.0- 为 slave-read-only
```

#### 主节点配置（复制相关）

```bash
# 主节点配置 - 复制缓冲区
repl-backlog-size 1mb           # 复制积压缓冲区大小
repl-timeout 60                 # 主从复制超时时间

# 无磁盘复制（全量复制时生效）
repl-diskless-sync no           # Redis 5.0- 为 slave-diskless-sync
repl-diskless-sync-delay 5      # 等待更多从节点加入同步
```

---

### 1.6 读写分离

**应用层实现**：

```java
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> masterTemplate() {
        // 主节点配置（写操作）
        RedisStandaloneConfiguration config = 
            new RedisStandaloneConfiguration("192.168.1.100", 6379);
        LettuceConnectionFactory factory = 
            new LettuceConnectionFactory(config);
        return new RedisTemplate<>(factory);
    }
    
    @Bean
    public RedisTemplate<String, Object> slaveTemplate() {
        // 从节点配置（读操作）
        RedisStandaloneConfiguration config = 
            new RedisStandaloneConfiguration("192.168.1.101", 6379);
        LettuceConnectionFactory factory = 
            new LettuceConnectionFactory(config);
        return new RedisTemplate<>(factory);
    }
}

@Service
public class UserService {
    
    @Autowired
    private RedisTemplate<String, Object> masterTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> slaveTemplate;
    
    public void saveUser(User user) {
        // 写操作 - 主节点
        masterTemplate.opsForValue().set("user:" + user.getId(), user);
    }
    
    public User getUser(Long id) {
        // 读操作 - 从节点
        return (User) slaveTemplate.opsForValue().get("user:" + id);
    }
}
```

---

## 二、哨兵模式（Sentinel）

### 2.1 Sentinel 概述

**功能**：

1. **监控**：监控主从节点健康状态
2. **自动故障转移**：主节点故障时自动选举新主
3. **通知**：通知客户端新的主节点地址
4. **配置中心**：存储当前集群配置

**架构**：

```
       Sentinel 1
          │
       Sentinel 2  ──── Master
          │              │
       Sentinel 3        ├─── Slave1
                         └─── Slave2
```

---

### 2.2 故障转移流程

```
┌─────────────────────────────────────────────────────────────┐
│                 Sentinel 故障转移流程                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   1. 主观下线（SDOWN）                                      │
│      单个 Sentinel 认为主节点故障                           │
│          │                                                  │
│          ▼                                                  │
│   2. 客观下线（ODOWN）                                      │
│      多数 Sentinel（≥quorum）确认主节点故障                │
│          │                                                  │
│          ▼                                                  │
│   3. 选举 Leader Sentinel                                   │
│      使用 Raft 算法选举                                      │
│          │                                                  │
│          ▼                                                  │
│   4. 选择新主节点                                           │
│      根据优先级、复制偏移量、runid                          │
│          │                                                  │
│          ▼                                                  │
│   5. 执行故障转移                                           │
│      SLAVEOF NO ONE                                         │
│          │                                                  │
│          ▼                                                  │
│   6. 更新配置                                               │
│      通知其他从节点和客户端                                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

### 2.3 Sentinel 配置

#### 基本配置

```bash
# sentinel.conf 完整示例
port 26379
daemonize yes
logfile "/var/log/redis/sentinel.log"
dir "/var/lib/redis"

# 监控主节点（mymaster 是监控名称）
sentinel monitor mymaster 192.168.1.100 6379 2

# 主节点密码
sentinel auth-pass mymaster your_password

# 主观下线时间（毫秒）
sentinel down-after-milliseconds mymaster 5000

# 故障转移超时时间（毫秒）
sentinel failover-timeout mymaster 180000

# 并行同步的从节点数量
sentinel parallel-syncs mymaster 1
```

**参数说明**：

- `2`（quorum）：确认主节点客观下线所需的最少 Sentinel 数量（不是选举 Leader 的票数）；
- `5000ms`：超过 5 秒无响应认为主观下线；
- `180000ms`：故障转移超时时间，具体包括：
    1. 同一主节点两次故障转移的最小间隔；
    2. 选举新主节点的超时时间；
    3. 从节点切换到新主节点的超时时间；
    4. 取消正在进行的故障转移的超时时间；
- `1`：每次只允许 1 个从节点并行同步。

---

### 2.4 Java 集成 Sentinel

```java
@Configuration
public class RedisSentinelConfig {
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Sentinel 配置
        RedisSentinelConfiguration sentinelConfig = 
            new RedisSentinelConfiguration();
        
        // 设置主节点名称
        sentinelConfig.master("mymaster");
        
        // 添加 Sentinel 节点
        sentinelConfig.sentinel("192.168.1.100", 26379);
        sentinelConfig.sentinel("192.168.1.101", 26379);
        sentinelConfig.sentinel("192.168.1.102", 26379);
        
        // 密码
        sentinelConfig.setPassword(RedisPassword.of("your_password"));
        
        // Lettuce 连接池配置（生产环境必须！）
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .clientOptions(ClientOptions.builder()
                .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS) // 断开连接时拒绝命令
                .autoReconnect(true) // 自动重连
                .build())
            .poolConfig(new GenericObjectPoolConfig<>() {{
                setMaxTotal(200); // 最大连接数
                setMaxIdle(50);   // 最大空闲连接
                setMinIdle(10);   // 最小空闲连接
                setMaxWait(Duration.ofSeconds(3)); // 最大等待时间
            }})
            .build();
        
        return new LettuceConnectionFactory(sentinelConfig, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        return new RedisTemplate<>(redisConnectionFactory());
    }
}
```

---

## 三、高频面试题

### 问题 1:Redis 主从复制的原理是什么？

**参考答案**：

**复制方式**：

1. **全量复制**：首次同步或无法部分复制时，发送完整 RDB
2. **部分复制**：断线重连，只同步增量数据

**全量复制流程**：

1. 从节点发送 PSYNC 命令
2. 主节点执行 BGSAVE 生成 RDB
3. 发送 RDB 给从节点
4. 从节点加载 RDB
5. 主节点发送复制缓冲区的增量命令

**部分复制关键**：

- Replication ID：标识主节点身份
- Offset：记录已复制位置
- 复制缓冲区：缓存增量命令

---

### 问题 2:Redis Sentinel 如何实现故障转移？

**参考答案**：

**故障转移流程**：

1. **主观下线**：单个 Sentinel 认为主节点故障
2. **客观下线**：多数 Sentinel（≥quorum）确认
3. **选举 Leader**：使用 Raft 算法选举 Leader Sentinel
4. **新主节点选择优先级（从高到低）**：
1. **从节点优先级**（`replica-priority`，默认 100，0 表示不参与选举）：值越小优先级越高；
2. **复制偏移量**：与原主节点偏移量越接近，优先级越高；
3. **运行 ID（runid）**：runid 越小（启动时间越早），优先级越高；
4. 配置示例（从节点）：
   ```bash
   replica-priority 90  # 提升该从节点的选举优先级
   ```
5. **执行切换**：向新主节点发送 `SLAVEOF NO ONE`
6. **更新配置**：通知其他从节点和客户端

**配置示例**：

```bash
sentinel monitor mymaster 192.168.1.100 6379 2
sentinel down-after-milliseconds mymaster 5000
```

---

### 问题 3：什么是全量复制和部分复制？

**参考答案**：

**全量复制**：

- 场景：首次同步、无法部分复制
- 过程：发送完整 RDB，耗时长
- 代价：阻塞主节点 fork，占用网络带宽

**部分复制**：

- 场景：断线重连
- 过程：只发送增量命令
- 关键：Replication ID + Offset + 复制缓冲区

---

### 问题 4：如何保证 Redis 主从数据一致性？

**参考答案**：

**Redis 主从是最终一致性**，不保证强一致。

**一致性保障措施**：

1. **复制缓冲区**：记录主节点的增量命令
   ```bash
   repl-backlog-size 1mb
   ```

2. **复制偏移量**：主从各自维护 offset，用于部分复制

3. **心跳检测**：从节点定期汇报 offset

> **主节点配置 - 写操作保护**：
> ```bash
> # 主节点配置！
> min-replicas-to-write 1    # 至少有 1 个从节点在线
> min-replicas-max-lag 10    # 从节点复制延迟不超过 10 秒
> ```
> 作用：当满足“从节点数量 < min-replicas-to-write”或“延迟 > min-replicas-max-lag”时，主节点拒绝写操作，避免数据丢失。

**注意**：如果需要强一致性，应使用分布式事务或其他方案。

---

### 问题 5：什么是 Redis 的无磁盘复制？有什么优缺点？

**参考答案**：

> **无磁盘复制（Diskless Replication）**：
> - 原理：主节点执行 `BGSAVE` 时，将 RDB 数据直接写入网络套接字发送给从节点，不落地到磁盘；
> - 触发时机：仅在**全量复制**时生效；
> - 配置（主节点）：
    >   ```bash
    > repl-diskless-sync yes # 开启无磁盘复制
    > repl-diskless-sync-delay 5 # 等待 5 秒，让更多从节点加入同步（减少多次全量复制）
    >   ```
> - 优点：减少磁盘 IO（适合 SSD 性能差或磁盘压力大的场景）；
> - 缺点：
    >

1. 网络中断需重新全量复制（无本地 RDB 兜底）；

> 2. 主节点 CPU/网络开销更大；
     >
3. 无法生成 RDB 备份文件。

---

### 问题 6：Sentinel 的 quorum 是什么？如何设置？

**参考答案**：

> **quorum 核心规则**：
> 1. quorum 是**确认主节点客观下线**所需的最少 Sentinel 数量，**不是**选举 Leader 的票数；
> 2. 选举 Leader Sentinel 需满足“票数 > Sentinel 总数的一半”（Raft 算法）；
> 3. quorum 建议值：
     >

- 3 个 Sentinel → quorum = 2（客观下线需 2 个确认，Leader 选举需 2 票）

> - 5 个 Sentinel → quorum = 3（客观下线需 3 个确认，Leader 选举需 3 票）
> 4. 示例配置：
     >    ```bash
     >    # 3 个 Sentinel 时，quorum=2（不是必须等于 N/2+1，而是建议值）
     >    sentinel monitor mymaster 192.168.1.100 6379 2
     >    ```

---

### 问题 7：主从复制会影响性能吗？如何优化？

**参考答案**：

**性能影响点**：

1. **全量复制**：fork() 阻塞、网络传输
2. **复制缓冲区**：内存占用
3. **命令复制**：网络延迟

**性能优化核心建议**：

1. **主节点优化**：
    - 调大 `repl-backlog-size`（如 10MB），减少全量复制；
    - 关闭主节点持久化，从节点开启（避免主节点 fork 阻塞）；
    - 配置 `repl-diskless-sync yes`（网络带宽 > 1Gbps 时）；
2. **从节点优化**：
    - 配置 `replica-lazy-flush yes`（Redis 6.2+）：加载 RDB 时延迟清空内存，提升加载速度；
    - 禁用从节点的 `rdbchecksum`（减少 CPU 开销）；
3. **网络优化**：
    - 主从节点部署在同一机房，降低网络延迟；
    - 避免跨公网复制（延迟高、易丢包）；
4. **业务优化**：
    - 拆分大 key（如 100MB 以上的 String），减少复制数据量；
    - 批量执行写命令，减少复制次数。

---

## 四、最佳实践

### 4.1 Sentinel 部署最佳实践

**Sentinel 部署 3 原则**：

1. **奇数个节点**：3/5 个（避免脑裂，满足 Raft 选举的“多数票”规则）；
2. **分散部署**：不同物理机/虚拟机/机房（避免单点故障）；
3. **独立部署**：不与 Redis主从节点部署在同一服务器（避免资源竞争）；

**推荐架构**：

```
# 1 主 2 从 + 3 Sentinel（最小高可用架构）
Master: 192.168.1.10:6379
Slave1: 192.168.1.11:6379
Slave2: 192.168.1.12:6379

Sentinel1: 192.168.1.13:26379
Sentinel2: 192.168.1.14:26379
Sentinel3: 192.168.1.15:26379
```

---

### 4.2 读写分离风险提示

> **读写分离风险**：
> 1. 从节点数据延迟（最终一致性），可能读取到旧数据；
> 2. 从节点故障时需切换到主节点读，增加主节点压力；
> 3. 解决方案：客户端实现读重试、延迟阈值过滤。

---

```
# 3 主 3 从 + 3 Sentinel
Master1: 192.168.1.10:6379  ←→  Slave1: 192.168.1.10:6380
Master2: 192.168.1.11:6379  ←→  Slave2: 192.168.1.11:6380
Master3: 192.168.1.12:6379  ←→  Slave3: 192.168.1.12:6380

Sentinel1: 192.168.1.10:26379
Sentinel2: 192.168.1.11:26379
Sentinel3: 192.168.1.12:26379
```

---

### 4.3 监控告警

```bash
# 1. 主从复制监控
redis-cli -h master_ip -p 6379 INFO replication
# 关键指标及阈值：
- master_repl_offset: 主节点偏移量
- slave0_offset: 从节点偏移量（延迟 = master_repl_offset - slave0_offset）
   告警阈值：延迟 > 10000 或 延迟时间 > 10 秒
- connected_slaves: 从节点数量  告警阈值：< 配置的数量
- repl_backlog_active: 复制积压缓冲区是否激活  告警阈值：0（未激活）

# 2. Sentinel 监控
redis-cli -h sentinel_ip -p 26379 INFO sentinel
# 关键指标及阈值：
- sentinel_masters: 监控的主节点数  告警阈值：< 预期数量
- sentinel_tilt: 是否进入倾斜模式  告警阈值：1（倾斜模式）
- sentinel_running_scripts: 运行中的脚本数  告警阈值：> 0（长时间）

# 3. 故障转移监控
redis-cli -h sentinel_ip -p 26379 SENTINEL GET-MASTER-ADDR-BY-NAME mymaster
# 告警场景：返回的主节点地址与预期不符（故障转移发生）
```

```bash
# 监控关键指标
INFO replication
INFO sentinel

# 告警指标
- 主从延迟 > 10 秒
- 从节点数量 < 2
- Sentinel 不可用
- 复制缓冲区溢出
```

---

## 五、总结

| 知识点      | 核心内容                                                     |
|----------|----------------------------------------------------------|
| 主从复制     | 全量复制（RDB+ 增量）、部分复制（replication ID+offset）、复制积压缓冲区（主节点专用） |
| 读写分离     | 主节点写、从节点读（最终一致性，存在延迟风险）                                  |
| Sentinel | 监控、自动故障转移、通知（需奇数节点、分散部署）                                 |
| 故障转移     | SDOWN→ODOWN→选举 Leader→选择新主→切换→通知客户端                      |
| 高可用架构    | 1 主 2 从 +3 Sentinel（最小高可用），Sentinel 独立部署                 |
| 性能优化     | 调大 backlog、关闭主节点持久化、无磁盘复制、避免大 key                        |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15
