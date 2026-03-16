package cn.itzixiao.interview.redis.advanced;

/**
 * Redis Cluster 详解
 * <p>
 * Redis Cluster 是 Redis 的分布式解决方案，提供数据分片、高可用和自动故障转移功能。
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                          Redis Cluster 架构                                 │
 * │                                                                             │
 * │    ┌──────────────────────────────────────────────────────────────────┐    │
 * │    │                        哈希槽分配                                  │    │
 * │    │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐         │    │
 * │    │  │ Node A   │  │ Node B   │  │ Node C   │  │ Node D   │         │    │
 * │    │  │ 0-5460   │  │ 5461-10922│ │10923-16383│  │ (备用)   │         │    │
 * │    │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘         │    │
 * │    │       │             │             │             │               │    │
 * │    │  ┌────┴───┐    ┌────┴───┐    ┌────┴───┐    ┌────┴───┐         │    │
 * │    │  │Slave A1│    │Slave B1│    │Slave C1│    │Slave D1│         │    │
 * │    │  └────────┘    └────────┘    └────────┘    └────────┘         │    │
 * │    └──────────────────────────────────────────────────────────────────┘    │
 * │                                                                             │
 * │    Gossip 协议：节点之间通过 Gossip 协议进行通信和状态同步                  │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
public class RedisClusterDemo {

    // 模拟哈希槽范围
    private static final int HASH_SLOTS = 16384;

    public static void main(String[] args) {
        System.out.println("========== Redis Cluster 详解 ==========\n");

        // 1. 为什么需要 Redis Cluster？
        demonstrateWhyNeedCluster();

        // 2. Redis Cluster 是如何分片的？
        demonstrateSharding();

        // 3. 为什么 Redis Cluster 的哈希槽是 16384 个？
        demonstrateWhy16384();

        // 4. 如何确定给定 key 的应该分布到哪个哈希槽中？
        demonstrateKeyToSlot();

        // 5. Redis Cluster 支持重新分配哈希槽吗？
        demonstrateSlotResharding();

        // 6. Redis Cluster 扩容缩容期间可以提供服务吗？
        demonstrateReshardingService();

        // 7. Redis Cluster 中的节点是怎么进行通信的？
        demonstrateNodeCommunication();

        // 8. Cluster 客户端连接示例
        demonstrateClientConnection();
    }

    /**
     * 1. 为什么需要 Redis Cluster？解决了什么问题？有什么优势？
     */
    private static void demonstrateWhyNeedCluster() {
        System.out.println("【1. 为什么需要 Redis Cluster？】\n");

        System.out.println("单机 Redis 的限制：");
        System.out.println("────────────────────────────────────────");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 内存限制                                                         │");
        System.out.println("│     - 单机 Redis 内存有限（如 64GB）                                 │");
        System.out.println("│     - 数据量超过单机容量时，无法存储                                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 性能瓶颈                                                         │");
        System.out.println("│     - 单机 QPS 上限约 10 万/秒                                       │");
        System.out.println("│     - 高并发场景下单机性能不足                                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. 单点故障                                                         │");
        System.out.println("│     - 主从复制模式下，Master 故障需要手动切换                        │");
        System.out.println("│     - Sentinel 解决了自动故障转移，但没有解决数据分片问题            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nRedis Cluster 解决的问题：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ┌────────────────────────────────────────────────────────────┐    │");
        System.out.println("│  │  数据分片                                                   │    │");
        System.out.println("│  │  - 将数据分散到多个节点                                    │    │");
        System.out.println("│  │  - 突破单机内存限制                                        │    │");
        System.out.println("│  │  - 支持水平扩展                                            │    │");
        System.out.println("│  └────────────────────────────────────────────────────────────┘    │");
        System.out.println("│  ┌────────────────────────────────────────────────────────────┐    │");
        System.out.println("│  │  高可用                                                     │    │");
        System.out.println("│  │  - 内置主从复制                                            │    │");
        System.out.println("│  │  - 自动故障转移（类似 Sentinel）                           │    │");
        System.out.println("│  │  - 无需额外部署 Sentinel                                   │    │");
        System.out.println("│  └────────────────────────────────────────────────────────────┘    │");
        System.out.println("│  ┌────────────────────────────────────────────────────────────┐    │");
        System.out.println("│  │  高性能                                                     │    │");
        System.out.println("│  │  - 读写分离                                                │    │");
        System.out.println("│  │  - 分担单机压力                                            │    │");
        System.out.println("│  │  - 线性扩展 QPS                                            │    │");
        System.out.println("│  └────────────────────────────────────────────────────────────┘    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nRedis Cluster vs Sentinel vs 主从复制：");
        System.out.println("┌────────────────┬─────────────┬─────────────┬─────────────┐");
        System.out.println("│      特性       │  主从复制   │   Sentinel  │   Cluster   │");
        System.out.println("├────────────────┼─────────────┼─────────────┼─────────────┤");
        System.out.println("│  数据分片       │     否      │     否      │     是      │");
        System.out.println("│  高可用         │     否      │     是      │     是      │");
        System.out.println("│  自动故障转移   │     否      │     是      │     是      │");
        System.out.println("│  水平扩展       │     否      │     否      │     是      │");
        System.out.println("│  额外组件       │     否      │   Sentinel  │     否      │");
        System.out.println("│  复杂度         │     低      │     中      │     高      │");
        System.out.println("│  数据一致性     │   弱一致    │   弱一致    │   弱一致    │");
        System.out.println("└────────────────┴─────────────┴─────────────┴─────────────┘");

        System.out.println("\nCluster 的优势：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 去中心化：不需要独立的 Sentinel 节点                             │");
        System.out.println("│  2. 水平扩展：支持动态添加/删除节点                                  │");
        System.out.println("│  3. 高可用：每个分片都有主从副本                                     │");
        System.out.println("│  4. 自动分片：通过哈希槽自动分布数据                                 │");
        System.out.println("│  5. 客户端路由：客户端可以直接连接正确的节点                         │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. Redis Cluster 是如何分片的？
     */
    private static void demonstrateSharding() {
        System.out.println("【2. Redis Cluster 是如何分片的？】\n");

        System.out.println("哈希槽（Hash Slot）分片机制：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n基本概念：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Redis Cluster 将所有数据划分为 16384 个哈希槽                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  每个键值对根据 key 计算出一个槽位号：                               │");
        System.out.println("│  slot = CRC16(key) % 16384                                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  每个节点负责一部分槽位                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n分片示例（3 个主节点）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  总槽数：16384                                                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  ┌──────────────────────────────────────────────────────────────┐  │");
        System.out.println("│  │  Node A: 槽位 0 - 5460      (5461 个槽)                       │  │");
        System.out.println("│  │  Node B: 槽位 5461 - 10922  (5462 个槽)                       │  │");
        System.out.println("│  │  Node C: 槽位 10923 - 16383 (5461 个槽)                       │  │");
        System.out.println("│  └──────────────────────────────────────────────────────────────┘  │");
        System.out.println("│                                                                     │");
        System.out.println("│  每个 Master 可以有一个或多个 Slave 作为备份                        │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n分片算法详解：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  CRC16 算法：                                                       │");
        System.out.println("│  - 循环冗余校验，输出 16 位整数（0 ~ 65535）                        │");
        System.out.println("│  - 分布均匀，冲突概率低                                              │");
        System.out.println("│  - 计算速度快                                                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  计算步骤：                                                         │");
        System.out.println("│  1. 计算 CRC16(key) 得到 16 位整数                                  │");
        System.out.println("│  2. 对 16384 取模得到槽位号                                         │");
        System.out.println("│  3. 根据槽位号找到对应的节点                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n支持哈希标签（Hash Tags）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  问题：如何让相关 key 分配到同一个槽？                              │");
        System.out.println("│                                                                     │");
        System.out.println("│  解决：使用 {} 包含相同的标签                                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  示例：                                                             │");
        System.out.println("│  user:1000:profile  → 只计算 1000 的哈希值                          │");
        System.out.println("│  user:1000:settings → 只计算 1000 的哈希值                          │");
        System.out.println("│  user:1000:orders   → 只计算 1000 的哈希值                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  这三个 key 会分配到同一个槽，可以用于事务                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n数据分布图示：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  Key: \"user:1000\"                                                   │");
        System.out.println("│       │                                                             │");
        System.out.println("│       │ CRC16(\"user:1000\") = 12345                                  │");
        System.out.println("│       │ 12345 % 16384 = 12345                                        │");
        System.out.println("│       │ 槽位 12345 属于 Node C (10923-16383)                         │");
        System.out.println("│       ▼                                                             │");
        System.out.println("│  存储到 Node C                                                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  Key: \"product:500\"                                                 │");
        System.out.println("│       │                                                             │");
        System.out.println("│       │ CRC16(\"product:500\") = 3000                                 │");
        System.out.println("│       │ 3000 % 16384 = 3000                                          │");
        System.out.println("│       │ 槽位 3000 属于 Node A (0-5460)                               │");
        System.out.println("│       ▼                                                             │");
        System.out.println("│  存储到 Node A                                                      │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 3. 为什么 Redis Cluster 的哈希槽是 16384 个？
     */
    private static void demonstrateWhy16384() {
        System.out.println("【3. 为什么 Redis Cluster 的哈希槽是 16384 个？】\n");

        System.out.println("原因分析：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n1. 槽位数量与节点数量的权衡");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  槽位太少（如 1024）：                                               │");
        System.out.println("│  - 每个节点分配的槽数少                                              │");
        System.out.println("│  - 节点间负载均衡困难                                                │");
        System.out.println("│  - 数据迁移粒度太大                                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  槽位太多（如 65536）：                                              │");
        System.out.println("│  - 节点需要维护更多槽位信息                                          │");
        System.out.println("│  - Gossip 消息更大                                                   │");
        System.out.println("│  - 内存开销增加                                                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  16384 是一个折中：                                                  │");
        System.out.println("│  - 支持最多约 1000 个节点                                            │");
        System.out.println("│  - 每个节点约 16 个槽位（1000 节点时）                               │");
        System.out.println("│  - 数据迁移粒度适中                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n2. Gossip 协议消息大小");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Redis Cluster 使用 Gossip 协议进行节点间通信                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  每个槽位需要 2 字节（16 位）来表示：                                │");
        System.out.println("│  - 16384 个槽位 = 16384 / 8 = 2048 字节 = 2KB                       │");
        System.out.println("│  - 65536 个槽位 = 65536 / 8 = 8192 字节 = 8KB                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  Gossip 消息包含每个节点的槽位信息                                   │");
        System.out.println("│  - 16384 槽位：节点信息约 2KB                                       │");
        System.out.println("│  - 65536 槽位：节点信息约 8KB                                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  选择 16384 可以控制消息大小，减少网络带宽消耗                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n3. CRC16 的特性");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  CRC16 输出范围：0 ~ 65535（16 位）                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  - 16384 = 65536 / 4                                                │");
        System.out.println("│  - CRC16 的分布足够均匀                                              │");
        System.out.println("│  - 取模 16384 后仍然保持良好的均匀性                                │");
        System.out.println("│  - 取模 65536 会有更多的哈希冲突（因为 CRC16 只有 65536 种可能）     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n4. Redis 作者的说明");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Redis 作者 antirez 的解释：                                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  \"16384 是一个合理的数字，因为：\"                                   │");
        System.out.println("│  - 它足够大，可以支持合理数量的节点                                  │");
        System.out.println("│  - 它足够小，可以保持较小的消息大小                                  │");
        System.out.println("│  - 它是 2 的幂次方（2^14），计算效率高                               │");
        System.out.println("│                                                                     │");
        System.out.println("│  实际上，Redis Cluster 建议节点数不超过 1000                         │");
        System.out.println("│  16384 个槽位已经足够使用                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n对比其他分片方案：");
        System.out.println("┌────────────────┬──────────────┬──────────────────────────────────┐");
        System.out.println("│     方案       │   分片数量    │              说明                │");
        System.out.println("├────────────────┼──────────────┼──────────────────────────────────┤");
        System.out.println("│  Redis Cluster │    16384     │  哈希槽，支持动态迁移             │");
        System.out.println("│  Memcached     │     可变     │  一致性哈希，虚拟节点             │");
        System.out.println("│  Couchbase     │    1024      │  vBuckets                        │");
        System.out.println("│  Cassandra     │     可变     │  Token Range                     │");
        System.out.println("└────────────────┴──────────────┴──────────────────────────────────┘\n");
    }

    /**
     * 4. 如何确定给定 key 的应该分布到哪个哈希槽中？
     */
    private static void demonstrateKeyToSlot() {
        System.out.println("【4. 如何确定给定 key 的应该分布到哪个哈希槽中？】\n");

        System.out.println("槽位计算规则：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n基本规则：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  slot = CRC16(key) % 16384                                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  CRC16：                                                            │");
        System.out.println("│  - XMODEM 标准的 CRC16                                              │");
        System.out.println("│  - 多项式：0x1021                                                   │");
        System.out.println("│  - 输出范围：0 ~ 65535                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nHash Tags（哈希标签）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  对于包含 {} 的 key，只计算 {} 内的内容                              │");
        System.out.println("│                                                                     │");
        System.out.println("│  规则：                                                             │");
        System.out.println("│  - 找到第一个 { 和其后最近的 }                                      │");
        System.out.println("│  - 只对 {} 内的内容计算哈希                                          │");
        System.out.println("│  - 如果没有 {} 或只有 { 没有 }，对整个 key 计算哈希                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  示例：                                                             │");
        System.out.println("│  key              →  计算哈希的部分                                 │");
        System.out.println("│  ────────────────────────────────────────                           │");
        System.out.println("│  user:{1000}:name →  \"1000\"                                        │");
        System.out.println("│  user:{1000}:age  →  \"1000\"                                        │");
        System.out.println("│  {product}:500    →  \"product\"                                     │");
        System.out.println("│  foo{bar}{zap}    →  \"bar\"                                         │");
        System.out.println("│  foo{bar}{zap}    →  只计算第一个 {}                                │");
        System.out.println("│  foo{{bar}}       →  \"{bar\"                                        │");
        System.out.println("│  foo{}{bar}       →  \"\"（空字符串）                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n计算演示（模拟）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");

        // 模拟计算几个 key 的槽位
        String[] keys = {"user:1000", "product:500", "order:12345", "{user}:1000:name", "{user}:1000:email"};
        for (String key : keys) {
            int slot = calculateSlot(key);
            System.out.printf("  %-20s → 槽位 %d\n", key, slot);
        }
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nCLUSTER KEYSLOT 命令：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 在 Redis 中查询 key 的槽位                                       │");
        System.out.println("│  > CLUSTER KEYSLOT user:1000                                        │");
        System.out.println("│  (integer) 12539                                                    │");
        System.out.println("│                                                                     │");
        System.out.println("│  > CLUSTER KEYSLOT {user}:1000:name                                 │");
        System.out.println("│  (integer) 5474                                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  > CLUSTER KEYSLOT {user}:1000:email                                │");
        System.out.println("│  (integer) 5474   # 相同的槽位                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n客户端如何找到节点：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  1. 计算 key 的槽位                                                 │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  2. 查找槽位对应的节点                                              │");
        System.out.println("│     - 本地缓存槽位映射表                                            │");
        System.out.println("│     - 或通过 CLUSTER SLOTS 命令获取                                 │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  3. 连接正确的节点执行命令                                          │");
        System.out.println("│          │                                                          │");
        System.out.println("│     ┌────┴────┐                                                    │");
        System.out.println("│     ▼         ▼                                                    │");
        System.out.println("│  在正确节点   不在此节点                                            │");
        System.out.println("│  执行成功     收到 MOVED 重定向                                     │");
        System.out.println("│                   │                                                 │");
        System.out.println("│                   ▼                                                 │");
        System.out.println("│              更新本地槽位映射                                       │");
        System.out.println("│              重新发送命令                                           │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 5. Redis Cluster 支持重新分配哈希槽吗？
     */
    private static void demonstrateSlotResharding() {
        System.out.println("【5. Redis Cluster 支持重新分配哈希槽吗？】\n");

        System.out.println("支持的！Redis Cluster 支持在线重新分配哈希槽（Resharding）：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\nResharding 场景：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 扩容：添加新节点，需要迁移部分槽位到新节点                       │");
        System.out.println("│  2. 缩容：删除节点，需要将槽位迁移到其他节点                         │");
        System.out.println("│  3. 负载均衡：调整各节点的槽位数量                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nResharding 原理：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  槽位迁移过程（以槽位 1 为例）：                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  初始状态：                                                         │");
        System.out.println("│  Node A 拥有槽位 1，包含 key1, key2, key3                           │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 1: 标记槽位 1 正在迁移                                        │");
        System.out.println("│  Node A: CLUSTER SETSLOT 1 IMPORTING <target-node-id>               │");
        System.out.println("│  Node B: CLUSTER SETSLOT 1 MIGRATING <source-node-id>               │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 2: 批量迁移 key                                               │");
        System.out.println("│  Node A → Node B:                                                   │");
        System.out.println("│    MIGRATE 127.0.0.1 6381 \"\" 0 5000 KEYS key1 key2 key3             │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 3: 更新槽位归属                                               │");
        System.out.println("│  所有节点: CLUSTER SETSLOT 1 NODE <target-node-id>                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  最终状态：                                                         │");
        System.out.println("│  Node B 拥有槽位 1，包含 key1, key2, key3                           │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n迁移期间的处理：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  客户端请求槽位 1 的 key：                                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  情况1：key 还未迁移                                                │");
        System.out.println("│  Node A 处理请求 → 返回结果                                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  情况2：key 已迁移                                                  │");
        System.out.println("│  Node A 返回 ASK 重定向 → 客户端向 Node B 发送 ASKING 命令          │");
        System.out.println("│  → 客户端向 Node B 发送请求 → Node B 处理请求                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  情况3：新写入                                                       │");
        System.out.println("│  Node A 返回 MOVED 重定向 → 客户端向 Node B 发送请求                │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nMOVED vs ASK 重定向：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  MOVED：槽位永久迁移                                                │");
        System.out.println("│  - 客户端应该更新槽位映射缓存                                       │");
        System.out.println("│  - 后续请求直接发送到新节点                                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  ASK：槽位临时迁移（迁移中）                                        │");
        System.out.println("│  - 只对当前请求有效                                                 │");
        System.out.println("│  - 客户端不应该更新缓存                                             │");
        System.out.println("│  - 需要先发送 ASKING 命令                                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n使用 redis-cli 进行 Resharding：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 添加新节点                                                       │");
        System.out.println("│  redis-cli --cluster add-node new_host:port existing_host:port      │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 重新分配槽位（交互式）                                           │");
        System.out.println("│  redis-cli --cluster reshard host:port                              │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 重新分配槽位（非交互式）                                         │");
        System.out.println("│  redis-cli --cluster reshard host:port \\                           │");
        System.out.println("│    --cluster-from <source-node-id> \\                               │");
        System.out.println("│    --cluster-to <target-node-id> \\                                 │");
        System.out.println("│    --cluster-slots <number-of-slots> \\                             │");
        System.out.println("│    --cluster-yes                                                    │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 删除节点（先迁移槽位）                                           │");
        System.out.println("│  redis-cli --cluster del-node host:port <node-id>                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 6. Redis Cluster 扩容缩容期间可以提供服务吗？
     */
    private static void demonstrateReshardingService() {
        System.out.println("【6. Redis Cluster 扩容缩容期间可以提供服务吗？】\n");

        System.out.println("答案：可以！Redis Cluster 支持在线扩容缩容。");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n扩容流程（添加新节点）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  初始状态：3 个 Master，各负责约 5461 个槽                          │");
        System.out.println("│  ┌─────────┐  ┌─────────┐  ┌─────────┐                            │");
        System.out.println("│  │ Node A  │  │ Node B  │  │ Node C  │                            │");
        System.out.println("│  │ 0-5460  │  │5461-10922│ │10923-16383│                           │");
        System.out.println("│  └─────────┘  └─────────┘  └─────────┘                            │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 1: 添加新节点 Node D                                          │");
        System.out.println("│  redis-cli --cluster add-node new_host:port existing_host:port      │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 2: 迁移部分槽位到 Node D                                      │");
        System.out.println("│  - Node A → Node D: 约 1365 个槽                                    │");
        System.out.println("│  - Node B → Node D: 约 1365 个槽                                    │");
        System.out.println("│  - Node C → Node D: 约 1365 个槽                                    │");
        System.out.println("│                                                                     │");
        System.out.println("│  最终状态：4 个 Master，各负责约 4096 个槽                          │");
        System.out.println("│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐               │");
        System.out.println("│  │ Node A  │  │ Node B  │  │ Node C  │  │ Node D  │               │");
        System.out.println("│  │0-4095   │  │4096-8191│  │8192-12287│ │12288-16383│              │");
        System.out.println("│  └─────────┘  └─────────┘  └─────────┘  └─────────┘               │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n缩容流程（删除节点）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Step 1: 将 Node D 的槽位迁移到其他节点                             │");
        System.out.println("│  Step 2: 确认 Node D 没有槽位                                       │");
        System.out.println("│  Step 3: 删除 Node D                                                │");
        System.out.println("│  redis-cli --cluster del-node host:port <node-id>                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n迁移期间的服务可用性：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  迁移过程中的处理：                                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  ┌───────────────────────────────────────────────────────────┐     │");
        System.out.println("│  │  请求类型      │  迁移前节点    │  迁移后节点              │     │");
        System.out.println("│  ├───────────────────────────────────────────────────────────┤     │");
        System.out.println("│  │  读取未迁移key │  正常响应      │  -                       │     │");
        System.out.println("│  │  读取已迁移key │  ASK重定向     │  正常响应（需ASKING）    │     │");
        System.out.println("│  │  写入未迁移key │  正常处理      │  -                       │     │");
        System.out.println("│  │  写入已迁移key │  MOVED重定向   │  正常响应                │     │");
        System.out.println("│  │  新写入        │  MOVED重定向   │  正常响应                │     │");
        System.out.println("│  └───────────────────────────────────────────────────────────┘     │");
        System.out.println("│                                                                     │");
        System.out.println("│  结论：                                                             │");
        System.out.println("│  - 迁移过程中，集群正常提供服务                                     │");
        System.out.println("│  - 客户端可能收到 ASK/MOVED 重定向                                  │");
        System.out.println("│  - 智能客户端会自动处理重定向                                       │");
        System.out.println("│  - 迁移完成后，客户端更新本地槽位映射                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n注意事项：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 迁移时间：取决于数据量和网络状况                                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 性能影响：                                                       │");
        System.out.println("│     - 迁移过程会占用网络带宽                                         │");
        System.out.println("│     - 建议在低峰期执行                                               │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. 客户端要求：                                                     │");
        System.out.println("│     - 客户端必须支持 ASK/MOVED 重定向处理                            │");
        System.out.println("│     - 建议使用 smart 客户端（如 Jedis, Lettuce, Redisson）           │");
        System.out.println("│                                                                     │");
        System.out.println("│  4. 错误处理：                                                       │");
        System.out.println("│     - 客户端应正确处理连接超时                                       │");
        System.out.println("│     - 建议实现重试机制                                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 7. Redis Cluster 中的节点是怎么进行通信的？
     */
    private static void demonstrateNodeCommunication() {
        System.out.println("【7. Redis Cluster 中的节点是怎么进行通信的？】\n");

        System.out.println("Gossip 协议：");
        System.out.println("────────────────────────────────────────");
        System.out.println("Redis Cluster 使用 Gossip 协议进行节点间通信。");

        System.out.println("\n什么是 Gossip 协议？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Gossip 协议（八卦协议、流言协议）：                                 │");
        System.out.println("│  - 一种去中心化的分布式协议                                          │");
        System.out.println("│  - 每个节点定期随机选择几个节点交换信息                              │");
        System.out.println("│  - 信息像八卦一样传播到整个集群                                      │");
        System.out.println("│  - 最终所有节点都能获得完整信息                                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  特点：                                                             │");
        System.out.println("│  - 去中心化：没有中央协调者                                          │");
        System.out.println("│  - 容错性强：部分节点故障不影响整体                                  │");
        System.out.println("│  - 扩展性好：节点数增加不影响协议复杂度                              │");
        System.out.println("│  - 最终一致：信息传播有延迟，但最终一致                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nGossip 消息类型：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. PING                                                            │");
        System.out.println("│     - 检测节点是否存活                                              │");
        System.out.println("│     - 包含发送者的集群状态信息                                       │");
        System.out.println("│     - 定期发送（每秒几次）                                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. PONG                                                            │");
        System.out.println("│     - 响应 PING 消息                                                │");
        System.out.println("│     - 包含发送者的集群状态信息                                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. MEET                                                            │");
        System.out.println("│     - 邀请新节点加入集群                                            │");
        System.out.println("│     - 收到 MEET 的节点会开始与其他节点交换信息                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  4. FAIL                                                            │");
        System.out.println("│     - 广播某节点已下线                                              │");
        System.out.println("│     - 收到 FAIL 消息的节点标记该节点为下线                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nGossip 消息结构：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  消息头（clusterMsg）：                                             │");
        System.out.println("│  ├── type: 消息类型（PING/PONG/MEET/FAIL）                         │");
        System.out.println("│  ├── sender: 发送者节点 ID                                          │");
        System.out.println("│  ├── myslots: 发送者负责的槽位（16384 位位图）                      │");
        System.out.println("│  ├── slaveof: 主节点 ID（如果是 Slave）                             │");
        System.out.println("│  ├── port: 端口                                                     │");
        System.out.println("│  ├── state: 集群状态                                                │");
        System.out.println("│  └── gossip: 其他节点的状态信息                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  Gossip 数据（clusterMsgDataGossip）：                              │");
        System.out.println("│  ├── nodename: 节点 ID                                              │");
        System.out.println("│  ├── ping_sent: 最后一次发送 PING 的时间                            │");
        System.out.println("│  ├── pong_received: 最后一次收到 PONG 的时间                        │");
        System.out.println("│  ├── ip: 节点 IP                                                    │");
        System.out.println("│  ├── port: 节点端口                                                 │");
        System.out.println("│  └── flags: 节点状态标志（MASTER/SLAVE/FAIL 等）                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nGossip 工作流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  定时任务（每秒执行）：                                             │");
        System.out.println("│  ┌────────────────────────────────────────────────────────────┐    │");
        System.out.println("│  │  1. 从节点列表中随机选择几个节点（通常 3-5 个）             │    │");
        System.out.println("│  │  2. 构造 PING 消息，包含自己的信息和部分其他节点信息       │    │");
        System.out.println("│  │  3. 发送给选中的节点                                       │    │");
        System.out.println("│  │  4. 等待 PONG 响应                                         │    │");
        System.out.println("│  │  5. 更新本地集群状态                                       │    │");
        System.out.println("│  └────────────────────────────────────────────────────────────┘    │");
        System.out.println("│                                                                     │");
        System.out.println("│  信息传播示例：                                                     │");
        System.out.println("│  Node A 知道 Node X 下线                                           │");
        System.out.println("│       │                                                             │");
        System.out.println("│       │ Gossip PING (包含 Node X 下线信息)                         │");
        System.out.println("│       ▼                                                             │");
        System.out.println("│  Node B、Node C 收到信息                                           │");
        System.out.println("│       │                                                             │");
        System.out.println("│       │ 继续传播给其他节点                                         │");
        System.out.println("│       ▼                                                             │");
        System.out.println("│  整个集群最终都知道 Node X 下线                                     │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n故障检测机制：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  类似 Sentinel 的主观/客观下线：                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  1. 主观下线（PFAIL）：                                             │");
        System.out.println("│     - 单个节点认为某节点下线                                        │");
        System.out.println("│     - PING 超时（cluster-node-timeout，默认 15 秒）                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 客观下线（FAIL）：                                              │");
        System.out.println("│     - 多数主节点认为某 Master 下线                                  │");
        System.out.println("│     - 标记为 FAIL 并广播                                            │");
        System.out.println("│     - 触发故障转移                                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  区别：Sentinel 使用 quorum 配置，Cluster 使用多数派原则            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nGossip 优缺点：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  优点：                                                             │");
        System.out.println("│  - 去中心化，无单点故障                                             │");
        System.out.println("│  - 容错性强，部分节点故障不影响整体                                 │");
        System.out.println("│  - 扩展性好，节点数增加影响小                                       │");
        System.out.println("│  - 实现简单                                                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  缺点：                                                             │");
        System.out.println("│  - 信息传播有延迟（秒级）                                           │");
        System.out.println("│  - 消息冗余，有一定网络开销                                         │");
        System.out.println("│  - 最终一致性，不保证实时一致                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 8. Cluster 客户端连接示例
     */
    private static void demonstrateClientConnection() {
        System.out.println("【8. Cluster 客户端连接示例】\n");

        System.out.println("Jedis 连接 Cluster：");
        System.out.println("────────────────────────────────────────");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 配置集群节点                                                     │");
        System.out.println("│  Set<HostAndPort> nodes = new HashSet<>();                          │");
        System.out.println("│  nodes.add(new HostAndPort(\"192.168.1.1\", 6379));                   │");
        System.out.println("│  nodes.add(new HostAndPort(\"192.168.1.2\", 6379));                   │");
        System.out.println("│  nodes.add(new HostAndPort(\"192.168.1.3\", 6379));                   │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 创建集群连接池                                                  │");
        System.out.println("│  JedisCluster jedisCluster = new JedisCluster(                     │");
        System.out.println("│      nodes,                                                         │");
        System.out.println("│      2000,        // 连接超时                                       │");
        System.out.println("│      2000,        // 读取超时                                       │");
        System.out.println("│      5,           // 最大重试次数                                   │");
        System.out.println("│      poolConfig   // 连接池配置                                     │");
        System.out.println("│  );                                                                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 使用（自动处理重定向）                                          │");
        System.out.println("│  jedisCluster.set(\"user:1000\", \"value\");                            │");
        System.out.println("│  String value = jedisCluster.get(\"user:1000\");                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nRedisson 连接 Cluster：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Config config = new Config();                                      │");
        System.out.println("│  config.useClusterServers()                                         │");
        System.out.println("│      .addNodeAddress(                                               │");
        System.out.println("│          \"redis://192.168.1.1:6379\",                                │");
        System.out.println("│          \"redis://192.168.1.2:6379\",                                │");
        System.out.println("│          \"redis://192.168.1.3:6379\"                                 │");
        System.out.println("│      )                                                              │");
        System.out.println("│      .setPassword(\"yourpassword\")                                   │");
        System.out.println("│      .setScanInterval(2000);  // 集群状态扫描间隔                   │");
        System.out.println("│                                                                     │");
        System.out.println("│  RedissonClient redisson = Redisson.create(config);                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 使用 Redisson 分布式锁                                          │");
        System.out.println("│  RLock lock = redisson.getLock(\"myLock\");                           │");
        System.out.println("│  lock.lock();                                                       │");
        System.out.println("│  try {                                                              │");
        System.out.println("│      // 执行业务                                                    │");
        System.out.println("│  } finally {                                                        │");
        System.out.println("│      lock.unlock();                                                 │");
        System.out.println("│  }                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nSpring Boot 配置 Cluster：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # application.yml                                                  │");
        System.out.println("│  spring:                                                            │");
        System.out.println("│    redis:                                                           │");
        System.out.println("│      password: yourpassword                                         │");
        System.out.println("│      cluster:                                                       │");
        System.out.println("│        nodes:                                                       │");
        System.out.println("│          - 192.168.1.1:6379                                         │");
        System.out.println("│          - 192.168.1.2:6379                                         │");
        System.out.println("│          - 192.168.1.3:6379                                         │");
        System.out.println("│          - 192.168.1.4:6379                                         │");
        System.out.println("│          - 192.168.1.5:6379                                         │");
        System.out.println("│          - 192.168.1.6:6379                                         │");
        System.out.println("│        max-redirects: 3  # 最大重定向次数                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nSmart 客户端原理：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  1. 初始化时获取槽位映射                                            │");
        System.out.println("│     - 连接任一节点发送 CLUSTER SLOTS 命令                           │");
        System.out.println("│     - 获取每个槽位范围对应的节点信息                                │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 本地缓存槽位映射                                                │");
        System.out.println("│     - 存储槽位 → 节点的映射关系                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. 请求时直接定位节点                                              │");
        System.out.println("│     - 计算 key 的槽位                                               │");
        System.out.println("│     - 从缓存中找到对应节点                                          │");
        System.out.println("│     - 直接连接该节点                                                │");
        System.out.println("│                                                                     │");
        System.out.println("│  4. 处理重定向                                                      │");
        System.out.println("│     - MOVED：更新本地缓存，重新发送请求                             │");
        System.out.println("│     - ASK：临时重定向，不更新缓存                                   │");
        System.out.println("│                                                                     │");
        System.out.println("│  5. 定期刷新槽位映射                                                │");
        System.out.println("│     - 定期更新集群状态                                              │");
        System.out.println("│     - 或在收到重定向时更新                                          │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("========== Redis Cluster 详解完成 ==========\n");
    }

    // ==================== 辅助方法 ====================

    /**
     * 模拟计算 key 的槽位（简化版）
     */
    private static int calculateSlot(String key) {
        // 处理 hash tags
        int start = key.indexOf('{');
        int end = key.indexOf('}', start + 1);
        String hashKey = key;

        if (start != -1 && end != -1 && end > start + 1) {
            hashKey = key.substring(start + 1, end);
        }

        // 简化的 CRC16 模拟（实际 Redis 使用标准 CRC16）
        int crc = hashKey.hashCode() & 0xFFFF;
        return crc % HASH_SLOTS;
    }
}
