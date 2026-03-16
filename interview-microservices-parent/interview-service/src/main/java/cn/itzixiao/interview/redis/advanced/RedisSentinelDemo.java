package cn.itzixiao.interview.redis.advanced;

/**
 * Redis Sentinel（哨兵）详解
 * <p>
 * Sentinel 是 Redis 的高可用解决方案，用于监控 Redis 主从架构，
 * 自动故障转移，并提供配置发现功能。
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │                        Redis Sentinel 架构                          │
 * │                                                                     │
 * │    ┌──────────┐    ┌──────────┐    ┌──────────┐                    │
 * │    │ Sentinel │    │ Sentinel │    │ Sentinel │                    │
 * │    │   S1     │    │   S2     │    │   S3     │                    │
 * │    └────┬─────┘    └────┬─────┘    └────┬─────┘                    │
 * │         │               │               │                          │
 * │         └───────────────┼───────────────┘                          │
 * │                         │                                          │
 * │         ┌───────────────┼───────────────┐                          │
 * │         ▼               ▼               ▼                          │
 * │    ┌──────────┐    ┌──────────┐    ┌──────────┐                    │
 * │    │  Master  │───>│  Slave1  │    │  Slave2  │                    │
 * │    │   M      │    │          │    │          │                    │
 * │    └──────────┘    └──────────┘    └──────────┘                    │
 * └─────────────────────────────────────────────────────────────────────┘
 */
public class RedisSentinelDemo {

    public static void main(String[] args) {
        System.out.println("========== Redis Sentinel（哨兵）详解 ==========\n");

        // 1. 什么是 Sentinel？有什么用？
        demonstrateSentinelBasics();

        // 2. Sentinel 如何检测节点是否下线？
        demonstrateDownDetection();

        // 3. 主观下线与客观下线的区别
        demonstrateSubjectiveObjectiveDown();

        // 4. Sentinel 是如何实现故障转移的？
        demonstrateFailover();

        // 5. 为什么建议部署多个 Sentinel 节点？
        demonstrateSentinelCluster();

        // 6. Sentinel 如何选择出新的 master？
        demonstrateMasterSelection();

        // 7. 如何从 Sentinel 集群中选择出 Leader？
        demonstrateSentinelLeaderElection();

        // 8. Sentinel 可以防止脑裂吗？
        demonstrateSplitBrain();

        // 9. Sentinel 客户端连接示例
        demonstrateClientConnection();
    }

    /**
     * 1. 什么是 Sentinel？有什么用？
     */
    private static void demonstrateSentinelBasics() {
        System.out.println("【1. 什么是 Sentinel？有什么用？】\n");

        System.out.println("Sentinel 是什么？");
        System.out.println("────────────────────────────────────────");
        System.out.println("- Redis 官方提供的高可用（HA）解决方案");
        System.out.println("- 用于监控 Redis 主从复制架构");
        System.out.println("- 在 Master 故障时自动进行故障转移\n");

        System.out.println("Sentinel 的三大核心功能：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 监控（Monitoring）                                               │");
        System.out.println("│     - 持续检查 Master 和 Slave 是否正常运行                          │");
        System.out.println("│     - 通过 PING 命令检测节点状态                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 通知（Notification）                                            │");
        System.out.println("│     - 当监控的实例出现问题时，通知管理员或其他应用程序               │");
        System.out.println("│     - 通过 Pub/Sub 发布事件                                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. 自动故障转移（Automatic Failover）                              │");
        System.out.println("│     - 当 Master 故障时，自动从 Slave 中选举新的 Master              │");
        System.out.println("│     - 通知客户端新的 Master 地址                                    │");
        System.out.println("│     - 让其他 Slave 复制新的 Master                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("Sentinel 的配置示例（sentinel.conf）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 监控名为 mymaster 的主节点                                        │");
        System.out.println("│  # 语法：sentinel monitor <master-name> <ip> <port> <quorum>        │");
        System.out.println("│  sentinel monitor mymaster 127.0.0.1 6379 2                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 判断主观下线的超时时间（毫秒）                                    │");
        System.out.println("│  sentinel down-after-milliseconds mymaster 30000                    │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 故障转移超时时间（毫秒）                                          │");
        System.out.println("│  sentinel failover-timeout mymaster 180000                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 同时可以向新 Master 复制的 Slave 数量                             │");
        System.out.println("│  sentinel parallel-syncs mymaster 1                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. Sentinel 如何检测节点是否下线？
     */
    private static void demonstrateDownDetection() {
        System.out.println("【2. Sentinel 如何检测节点是否下线？】\n");

        System.out.println("检测机制：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n1. 心跳检测");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Sentinel 每 1 秒向所有节点发送 PING 命令                            │");
        System.out.println("│                                                                     │");
        System.out.println("│  Sentinel ───────PING──────> Master/Slave                          │");
        System.out.println("│  Sentinel <──────PONG─────── Master/Slave                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  有效回复：+PONG、-LOADING、-MASTERDOWN                             │");
        System.out.println("│  无效回复：超时无响应、其他错误回复                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n2. down-after-milliseconds 配置");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 默认 30 秒                                                        │");
        System.out.println("│  sentinel down-after-milliseconds mymaster 30000                    │");
        System.out.println("│                                                                     │");
        System.out.println("│  如果在这个时间内没有收到有效回复，则认为该节点「主观下线」           │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n3. Sentinel 之间的通信");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Sentinel 通过 Pub/Sub 机制相互发现和通信：                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  频道：__sentinel__:hello                                           │");
        System.out.println("│                                                                     │");
        System.out.println("│  每个 Sentinel 每隔 2 秒发布自己的信息：                             │");
        System.out.println("│  - Sentinel 的 IP、端口、runid                                      │");
        System.out.println("│  - 监控的 Master 信息                                               │");
        System.out.println("│  - Master 的配置纪元（configuration epoch）                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  通过订阅该频道，Sentinel 可以：                                     │");
        System.out.println("│  - 发现其他 Sentinel 节点                                           │");
        System.out.println("│  - 交换 Master/Slave 的状态信息                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("检测流程图：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  Sentinel 启动                                                      │");
        System.out.println("│      │                                                              │");
        System.out.println("│      ▼                                                              │");
        System.out.println("│  连接 Master，获取 Slave 列表                                       │");
        System.out.println("│      │                                                              │");
        System.out.println("│      ▼                                                              │");
        System.out.println("│  连接所有 Slave                                                     │");
        System.out.println("│      │                                                              │");
        System.out.println("│      ▼                                                              │");
        System.out.println("│  订阅 __sentinel__:hello 频道                                       │");
        System.out.println("│      │                                                              │");
        System.out.println("│      ▼                                                              │");
        System.out.println("│  ┌─────────────────────────────────┐                               │");
        System.out.println("│  │ 循环检测（每秒发送 PING）        │                               │");
        System.out.println("│  │   ├─ 检测 Master 状态           │                               │");
        System.out.println("│  │   ├─ 检测 Slave 状态            │                               │");
        System.out.println("│  │   └─ 检测其他 Sentinel 状态     │                               │");
        System.out.println("│  └─────────────────────────────────┘                               │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 3. 主观下线与客观下线的区别
     */
    private static void demonstrateSubjectiveObjectiveDown() {
        System.out.println("【3. 主观下线与客观下线的区别】\n");

        System.out.println("概念对比：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    主观下线（Subjectively Down，SDOWN）             │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  定义：                                                              │");
        System.out.println("│  - 单个 Sentinel 认为某个节点下线                                    │");
        System.out.println("│                                                                     │");
        System.out.println("│  判断条件：                                                          │");
        System.out.println("│  - Sentinel 在 down-after-milliseconds 时间内未收到有效 PING 回复   │");
        System.out.println("│                                                                     │");
        System.out.println("│  特点：                                                              │");
        System.out.println("│  - 是单个 Sentinel 的「主观」判断                                    │");
        System.out.println("│  - 可能是误判（网络问题、Sentinel 自身问题）                         │");
        System.out.println("│  - 不会触发故障转移                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                    客观下线（Objectively Down，ODOWN）              │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  定义：                                                              │");
        System.out.println("│  - 足够数量（≥quorum）的 Sentinel 认为某个 Master 下线               │");
        System.out.println("│                                                                     │");
        System.out.println("│  判断条件：                                                          │");
        System.out.println("│  - 一个 Sentinel 认为某 Master 主观下线后，询问其他 Sentinel         │");
        System.out.println("│  - 收到 quorum 个 Sentinel 的确认（都认为该 Master 下线）            │");
        System.out.println("│                                                                     │");
        System.out.println("│  特点：                                                              │");
        System.out.println("│  - 是「客观」共识，减少误判可能                                      │");
        System.out.println("│  - 会触发故障转移流程                                                │");
        System.out.println("│  - 仅对 Master 节点有效                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n状态转换流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  Sentinel S1 发现 Master 无响应                                     │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  S1 标记 Master 为 SDOWN（主观下线）                                │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  S1 向其他 Sentinel 发送 SENTINEL is-master-down-by-addr 命令       │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  统计认为下线的 Sentinel 数量                                       │");
        System.out.println("│          │                                                          │");
        System.out.println("│     ┌────┴────┐                                                    │");
        System.out.println("│     ▼         ▼                                                    │");
        System.out.println("│  < quorum   ≥ quorum                                               │");
        System.out.println("│     │         │                                                    │");
        System.out.println("│     ▼         ▼                                                    │");
        System.out.println("│  保持 SDOWN  标记为 ODOWN（客观下线）                               │");
        System.out.println("│               │                                                    │");
        System.out.println("│               ▼                                                    │");
        System.out.println("│          触发故障转移                                              │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nquorum 参数说明：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  sentinel monitor mymaster 127.0.0.1 6379 2                        │");
        System.out.println("│                                                       │            │");
        System.out.println("│                                                   quorum=2         │");
        System.out.println("│                                                                     │");
        System.out.println("│  含义：需要 2 个 Sentinel 同意才能判定为客观下线                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  建议：quorum = Sentinel 数量 / 2 + 1（多数派）                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 4. Sentinel 是如何实现故障转移的？
     */
    private static void demonstrateFailover() {
        System.out.println("【4. Sentinel 是如何实现故障转移的？】\n");

        System.out.println("故障转移完整流程：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                     Redis Sentinel 故障转移流程                     │");
        System.out.println("\n┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 1: 发现问题                                                   │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  - Sentinel 检测到 Master 下线                                      │");
        System.out.println("│  - 多数 Sentinel 确认 → 标记为 ODOWN                                │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 2: 选举 Leader Sentinel                                       │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  - 多个 Sentinel 竞争成为 Leader                                    │");
        System.out.println("│  - 使用 Raft 算法选举 Leader                                        │");
        System.out.println("│  - Leader 负责执行故障转移                                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 3: 选择新 Master                                              │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  - 从 Slave 列表中选择一个作为新 Master                             │");
        System.out.println("│  - 根据优先级、复制偏移量等规则选举                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 4: 提升新 Master                                              │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  - 向选中的 Slave 发送 SLAVEOF NO ONE                               │");
        System.out.println("│  - 等待该 Slave 变为 Master                                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 5: 重新配置其他 Slave                                         │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  - 向其他 Slave 发送 SLAVEOF <new-master-ip> <port>                 │");
        System.out.println("│  - 让它们复制新 Master                                              │");
        System.out.println("│                                                                     │");
        System.out.println("│  Step 6: 更新配置                                                   │");
        System.out.println("│  ─────────────────────────────                                      │");
        System.out.println("│  - 更新 Sentinel 的配置信息                                         │");
        System.out.println("│  - 通过 Pub/Sub 通知客户端新 Master 地址                            │");
        System.out.println("│  - 继续监控新 Master                                                │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n故障转移时序图：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  S1       S2       S3       Old-Master   Slave1    Slave2          │");
        System.out.println("│   │        │        │           │           │         │             │");
        System.out.println("│   │  PING  │        │           X           │         │             │");
        System.out.println("│   │────────┼────────┼──────────>│           │         │             │");
        System.out.println("│   │        │        │      超时无响应        │         │             │");
        System.out.println("│   │<───────┼────────┼───────────┼───────────┼─────────┤             │");
        System.out.println("│   │ 标记SDOWN        │           │           │         │             │");
        System.out.println("│   │        │        │           │           │         │             │");
        System.out.println("│   │ 询问是否下线     │           │           │         │             │");
        System.out.println("│   │───────>|───────>|           │           │         │             │");
        System.out.println("│   │<───────|<───────|           │           │         │             │");
        System.out.println("│   │  确认下线        │           │           │         │             │");
        System.out.println("│   │        │        │           │           │         │             │");
        System.out.println("│   │ 标记ODOWN，选举Leader         │         │             │");
        System.out.println("│   │<───────┼───────>|           │           │         │             │");
        System.out.println("│   │  S1成为Leader    │           │           │         │             │");
        System.out.println("│   │        │        │           │           │         │             │");
        System.out.println("│   │              SLAVEOF NO ONE  │         │             │");
        System.out.println("│   │─────────────────────────────────────────>│         │             │");
        System.out.println("│   │              Slave1 成为 Master         │         │             │");
        System.out.println("│   │        │        │           │           │         │             │");
        System.out.println("│   │              SLAVEOF Slave1 │         │             │");
        System.out.println("│   │─────────────────────────────────────────────────────>│           │");
        System.out.println("│   │              Slave2 复制新 Master       │         │             │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 5. 为什么建议部署多个 Sentinel 节点？
     */
    private static void demonstrateSentinelCluster() {
        System.out.println("【5. 为什么建议部署多个 Sentinel 节点（哨兵集群）？】\n");

        System.out.println("部署多个 Sentinel 的原因：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n1. 避免单点故障");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  如果只有一个 Sentinel：                                             │");
        System.out.println("│  - Sentinel 自身故障 → 失去监控和故障转移能力                        │");
        System.out.println("│  - 无法进行客观下线判断                                              │");
        System.out.println("│  - 无法进行 Leader 选举                                             │");
        System.out.println("│                                                                     │");
        System.out.println("│  多个 Sentinel 可以：                                               │");
        System.out.println("│  - 互为备份，任一 Sentinel 故障不影响整体功能                        │");
        System.out.println("│  - 实现客观下线判断，减少误判                                        │");
        System.out.println("│  - 实现 Leader 选举，保证只有一个执行者                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n2. 减少误判");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  单 Sentinel 判断下线：                                              │");
        System.out.println("│  - 可能是 Sentinel 与 Master 之间网络问题                           │");
        System.out.println("│  - 可能是 Sentinel 自身繁忙                                         │");
        System.out.println("│  - Master 实际正常，但被误判为下线                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  多 Sentinel 判断：                                                 │");
        System.out.println("│  - 需要 quorum 个 Sentinel 同时认为下线                             │");
        System.out.println("│  - 多数节点网络同时故障的概率很低                                    │");
        System.out.println("│  - 大大降低了误判的可能性                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n3. 保证 Leader 选举的可靠性");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  故障转移需要先选举 Leader Sentinel：                               │");
        System.out.println("│  - 使用类 Raft 算法                                                 │");
        System.out.println("│  - 需要多数派投票同意                                               │");
        System.out.println("│  - 单 Sentinel 无法进行选举                                         │");
        System.out.println("│  - 2 个 Sentinel 可能出现平票                                       │");
        System.out.println("│  - 建议至少 3 个 Sentinel 节点                                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n部署建议：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  推荐：3 个 Sentinel（奇数个）                                       │");
        System.out.println("│  ┌──────────┐  ┌──────────┐  ┌──────────┐                         │");
        System.out.println("│  │ Sentinel │  │ Sentinel │  │ Sentinel │                         │");
        System.out.println("│  │    1     │  │    2     │  │    3     │                         │");
        System.out.println("│  └────┬─────┘  └────┬─────┘  └────┬─────┘                         │");
        System.out.println("│       │             │             │                                │");
        System.out.println("│  部署在不同机器/机房，避免同时故障                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  quorum 设置：                                                      │");
        System.out.println("│  - 3 个 Sentinel → quorum = 2                                       │");
        System.out.println("│  - 5 个 Sentinel → quorum = 3                                       │");
        System.out.println("│  - 公式：quorum = N/2 + 1（多数派）                                 │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 6. Sentinel 如何选择出新的 master？
     */
    private static void demonstrateMasterSelection() {
        System.out.println("【6. Sentinel 如何选择出新的 master（选举机制）？】\n");

        System.out.println("新 Master 选举规则：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n筛选条件（排除不合格的 Slave）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 排除主观下线的 Slave                                            │");
        System.out.println("│  2. 排除最近 5 秒没有回复 INFO 命令的 Slave                          │");
        System.out.println("│  3. 排除与旧 Master 断开连接时间过长的 Slave                        │");
        System.out.println("│     （down-after-milliseconds * 10）                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n选举优先级（按顺序判断）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  1. Slave 优先级（slave-priority）                                  │");
        System.out.println("│     ─────────────────────────────                                   │");
        System.out.println("│     - Redis 配置项：slave-priority（默认 100）                      │");
        System.out.println("│     - 数值越小，优先级越高                                          │");
        System.out.println("│     - slave-priority = 0 表示永不成为 Master                        │");
        System.out.println("│     - 选择优先级最高的 Slave                                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 复制偏移量（replication offset）                                │");
        System.out.println("│     ─────────────────────────────                                   │");
        System.out.println("│     - 如果优先级相同，选择复制偏移量最大的                           │");
        System.out.println("│     - 复制偏移量大 = 数据更新 = 数据更完整                          │");
        System.out.println("│     - 优先选择数据最完整的 Slave                                    │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. Run ID（运行 ID）                                               │");
        System.out.println("│     ─────────────────────────────                                   │");
        System.out.println("│     - 如果优先级和偏移量都相同，选择 runid 最小的                   │");
        System.out.println("│     - runid 是字典序比较，相当于随机选择                            │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n选举流程图：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  获取所有 Slave 列表                                                │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  过滤不合格的 Slave                                                 │");
        System.out.println("│  （排除下线、断开时间过长等）                                        │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  按 slave-priority 排序（升序）                                     │");
        System.out.println("│          │                                                          │");
        System.out.println("│          ▼                                                          │");
        System.out.println("│  选择优先级最小的 Slave                                             │");
        System.out.println("│          │                                                          │");
        System.out.println("│     ┌────┴────┐                                                    │");
        System.out.println("│     ▼         ▼                                                    │");
        System.out.println("│  唯一一个    多个相同优先级                                         │");
        System.out.println("│     │              │                                               │");
        System.out.println("│     │              ▼                                               │");
        System.out.println("│     │      按 replication offset 排序（降序）                       │");
        System.out.println("│     │              │                                               │");
        System.out.println("│     │              ▼                                               │");
        System.out.println("│     │      选择偏移量最大的                                        │");
        System.out.println("│     │              │                                               │");
        System.out.println("│     │         ┌────┴────┐                                         │");
        System.out.println("│     │         ▼         ▼                                         │");
        System.out.println("│     │      唯一一个   多个相同                                      │");
        System.out.println("│     │         │          │                                        │");
        System.out.println("│     │         │          ▼                                        │");
        System.out.println("│     │         │   选择 runid 最小的                                │");
        System.out.println("│     │         │          │                                        │");
        System.out.println("│     └─────────┴──────────┘                                        │");
        System.out.println("│               │                                                    │");
        System.out.println("│               ▼                                                    │");
        System.out.println("│         确定新 Master                                              │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n配置建议：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # 在 redis.conf 中配置                                             │");
        System.out.println("│  slave-priority 100    # 默认值                                     │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 如果某台机器配置较低，不想成为 Master：                          │");
        System.out.println("│  slave-priority 50     # 较低优先级                                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  # 如果某台机器永不成为 Master：                                    │");
        System.out.println("│  slave-priority 0                                                    │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 7. 如何从 Sentinel 集群中选择出 Leader？
     */
    private static void demonstrateSentinelLeaderElection() {
        System.out.println("【7. 如何从 Sentinel 集群中选择出 Leader？】\n");

        System.out.println("Sentinel Leader 选举机制：");
        System.out.println("────────────────────────────────────────");

        System.out.println("\n为什么需要选举 Leader Sentinel？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  - 故障转移只需要一个 Sentinel 来执行                               │");
        System.out.println("│  - 多个 Sentinel 同时执行会导致混乱                                 │");
        System.out.println("│  - 需要选举一个 Leader 来负责故障转移                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n选举算法：类 Raft 共识算法");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  核心概念：                                                          │");
        System.out.println("│  - 配置纪元（configuration epoch）：选举的代次，单调递增             │");
        System.out.println("│  - 投票：每个 Sentinel 在每个纪元只能投一票                         │");
        System.out.println("│  - 多数派：获得超过半数投票才能成为 Leader                          │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n选举流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  1. 触发选举                                                        │");
        System.out.println("│     ────────────                                                    │");
        System.out.println("│     - Sentinel 确认 Master 客观下线（ODOWN）                        │");
        System.out.println("│     - 当前没有进行中的故障转移                                      │");
        System.out.println("│     - 尝试成为 Leader                                               │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 增加配置纪元                                                    │");
        System.out.println("│     ────────────                                                    │");
        System.out.println("│     - 局部配置纪元 + 1                                              │");
        System.out.println("│     - 用于区分不同的选举轮次                                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. 请求投票                                                        │");
        System.out.println("│     ────────────                                                    │");
        System.out.println("│     - 向其他 Sentinel 发送请求：                                    │");
        System.out.println("│       SENTINEL is-master-down-by-addr <ip> <port> <epoch> <runid>   │");
        System.out.println("│     - runid 非空表示请求投票                                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  4. 投票规则                                                        │");
        System.out.println("│     ────────────                                                    │");
        System.out.println("│     - 每个 Sentinel 在每个纪元只能投一票                            │");
        System.out.println("│     - 先到先得（FIFO）                                              │");
        System.out.println("│     - 投票后记录投票信息，纪元内不能改                              │");
        System.out.println("│     - 不会投票给配置纪元更小的 Sentinel                             │");
        System.out.println("│                                                                     │");
        System.out.println("│  5. 统计投票                                                        │");
        System.out.println("│     ────────────                                                    │");
        System.out.println("│     - 收到超过半数的投票 → 成为 Leader                              │");
        System.out.println("│     - 未达到多数 → 等待或重试                                       │");
        System.out.println("│                                                                     │");
        System.out.println("│  6. 成为 Leader 后                                                  │");
        System.out.println("│     ────────────                                                    │");
        System.out.println("│     - 更新配置纪元                                                  │");
        System.out.println("│     - 开始执行故障转移                                              │");
        System.out.println("│     - 通知其他 Sentinel 更新配置                                    │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n选举示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  假设有 3 个 Sentinel：S1、S2、S3                                   │");
        System.out.println("│                                                                     │");
        System.out.println("│  T1: S1 发现 Master 客观下线，配置纪元设为 1，请求投票              │");
        System.out.println("│      S1 ──请求投票──> S2 ──同意──> S1                               │");
        System.out.println("│      S1 ──请求投票──> S3 ──同意──> S1                               │");
        System.out.println("│      S1 获得 2 票（自己+S2+S3），超过半数，成为 Leader              │");
        System.out.println("│                                                                     │");
        System.out.println("│  T2: S2 也发现客观下线，但 S1 已先获得投票                          │");
        System.out.println("│      S2 的请求被拒绝（S2、S3 已投票给 S1）                          │");
        System.out.println("│                                                                     │");
        System.out.println("│  结果：S1 成为 Leader Sentinel，执行故障转移                        │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n为什么需要多数派？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  - 保证同一时间只有一个 Leader                                      │");
        System.out.println("│  - 避免「脑裂」（多个 Leader 同时操作）                             │");
        System.out.println("│  - 多数派算法是分布式共识的基础                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 8. Sentinel 可以防止脑裂吗？
     */
    private static void demonstrateSplitBrain() {
        System.out.println("【8. Sentinel 可以防止脑裂吗？】\n");

        System.out.println("什么是脑裂？");
        System.out.println("────────────────────────────────────────");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  脑裂（Split Brain）：在分布式系统中，出现两个或多个「主节点」       │");
        System.out.println("│  导致数据不一致、写入冲突等问题                                      │");
        System.out.println("│                                                                     │");
        System.out.println("│  场景示例：                                                          │");
        System.out.println("│  ┌──────────────────────┐    ┌──────────────────────┐             │");
        System.out.println("│  │      网络 partition   │    │                      │             │");
        System.out.println("│  │  ┌────────────────┐  │    │  ┌────────────────┐  │             │");
        System.out.println("│  │  │ Old Master     │  │    │  │ New Master     │  │             │");
        System.out.println("│  │  │ (仍认为自己是主) │  │    │  │ (Sentinel选举) │  │             │");
        System.out.println("│  │  └────────────────┘  │    │  └────────────────┘  │             │");
        System.out.println("│  │        客户端A       │    │        客户端B       │             │");
        System.out.println("│  └──────────────────────┘    └──────────────────────┘             │");
        System.out.println("│                                                                     │");
        System.out.println("│  结果：客户端 A 写入 Old Master，客户端 B 写入 New Master          │");
        System.out.println("│        两个 Master 数据不一致！                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nSentinel 能完全防止脑裂吗？");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  答案：不能完全防止，但可以减轻影响                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  原因：                                                              │");
        System.out.println("│  - Sentinel 依赖网络通信判断节点状态                                │");
        System.out.println("│  - 网络分区时，可能误判 Master 下线                                  │");
        System.out.println("│  - Old Master 可能仍然存活并接受写入                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nSentinel 的防护措施：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 客观下线机制                                                     │");
        System.out.println("│     ────────────────                                                │");
        System.out.println("│     - 需要 quorum 个 Sentinel 确认才能下线                          │");
        System.out.println("│     - 减少网络抖动导致的误判                                        │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 配置纪元（Configuration Epoch）                                 │");
        System.out.println("│     ─────────────────────────────                                   │");
        System.out.println("│     - 每次 Failover 都会增加纪元                                    │");
        System.out.println("│     - 新 Master 的纪元更大，会被优先认可                            │");
        System.out.println("│     - Old Master 恢复后会降级为 Slave                               │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. min-replicas-to-write 配置                                      │");
        System.out.println("│     ─────────────────────────────                                   │");
        System.out.println("│     # Master 至少需要连接 N 个 Slave 才能接受写入                   │");
        System.out.println("│     min-replicas-to-write 1                                         │");
        System.out.println("│     min-replicas-max-lag 10                                         │");
        System.out.println("│                                                                     │");
        System.out.println("│     - 如果 Slave 不足或延迟过高，Master 拒绝写入                    │");
        System.out.println("│     - 网络分区时，少数派的 Master 会停止写入                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n完整的脑裂防护方案：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 合理配置 quorum                                                  │");
        System.out.println("│     - quorum = Sentinel 数量 / 2 + 1                                │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. 配置 min-replicas-to-write                                      │");
        System.out.println("│     min-replicas-to-write 1                                         │");
        System.out.println("│     min-replicas-max-lag 10                                         │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. 合理设置 down-after-milliseconds                                │");
        System.out.println("│     - 不要太短（如 5 秒），避免网络抖动误判                          │");
        System.out.println("│     - 建议 30 秒或更长                                              │");
        System.out.println("│                                                                     │");
        System.out.println("│  4. 监控和告警                                                       │");
        System.out.println("│     - 监控 Sentinel 状态                                            │");
        System.out.println("│     - 故障转移时发送告警                                            │");
        System.out.println("│                                                                     │");
        System.out.println("│  5. 应用层处理                                                       │");
        System.out.println("│     - 写入时验证 Master 身份                                        │");
        System.out.println("│     - 使用版本号或时间戳检测冲突                                    │");
        System.out.println("│     - 业务幂等设计                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 9. Sentinel 客户端连接示例
     */
    private static void demonstrateClientConnection() {
        System.out.println("【9. Sentinel 客户端连接示例】\n");

        System.out.println("Jedis 连接 Sentinel：");
        System.out.println("────────────────────────────────────────");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 配置 Sentinel 节点                                              │");
        System.out.println("│  Set<String> sentinels = new HashSet<>();                           │");
        System.out.println("│  sentinels.add(\"192.168.1.1:26379\");                               │");
        System.out.println("│  sentinels.add(\"192.168.1.2:26379\");                               │");
        System.out.println("│  sentinels.add(\"192.168.1.3:26379\");                               │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 创建连接池                                                      │");
        System.out.println("│  JedisSentinelPool pool = new JedisSentinelPool(                   │");
        System.out.println("│      \"mymaster\",    // master 名称                                 │");
        System.out.println("│      sentinels,      // Sentinel 集合                               │");
        System.out.println("│      poolConfig,     // 连接池配置                                  │");
        System.out.println("│      timeout,        // 超时时间                                    │");
        System.out.println("│      password        // 密码                                        │");
        System.out.println("│  );                                                                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 获取连接（自动从 Sentinel 获取 Master 地址）                    │");
        System.out.println("│  try (Jedis jedis = pool.getResource()) {                           │");
        System.out.println("│      jedis.set(\"key\", \"value\");                                     │");
        System.out.println("│  }                                                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nRedisson 连接 Sentinel：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Config config = new Config();                                      │");
        System.out.println("│  config.useSentinelServers()                                        │");
        System.out.println("│      .setMasterName(\"mymaster\")                                     │");
        System.out.println("│      .addSentinelAddress(                                           │");
        System.out.println("│          \"redis://192.168.1.1:26379\",                               │");
        System.out.println("│          \"redis://192.168.1.2:26379\",                               │");
        System.out.println("│          \"redis://192.168.1.3:26379\"                                │");
        System.out.println("│      )                                                              │");
        System.out.println("│      .setPassword(\"yourpassword\");                                  │");
        System.out.println("│                                                                     │");
        System.out.println("│  RedissonClient redisson = Redisson.create(config);                 │");
        System.out.println("│                                                                     │");
        System.out.println("│  // 正常使用                                                        │");
        System.out.println("│  RBucket<String> bucket = redisson.getBucket(\"key\");                │");
        System.out.println("│  bucket.set(\"value\");                                               │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\nSpring Boot 配置 Sentinel：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  # application.yml                                                  │");
        System.out.println("│  spring:                                                            │");
        System.out.println("│    redis:                                                           │");
        System.out.println("│      password: yourpassword                                         │");
        System.out.println("│      sentinel:                                                      │");
        System.out.println("│        master: mymaster                                             │");
        System.out.println("│        nodes:                                                       │");
        System.out.println("│          - 192.168.1.1:26379                                        │");
        System.out.println("│          - 192.168.1.2:26379                                        │");
        System.out.println("│          - 192.168.1.3:26379                                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘");

        System.out.println("\n客户端获取 Master 地址的过程：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                                                                     │");
        System.out.println("│  客户端                                                             │");
        System.out.println("│     │                                                               │");
        System.out.println("│     │ SENTINEL get-master-addr-by-name mymaster                    │");
        System.out.println("│     ▼                                                               │");
        System.out.println("│  Sentinel 集群                                                      │");
        System.out.println("│     │                                                               │");
        System.out.println("│     │ 返回 Master IP:Port                                          │");
        System.out.println("│     ▼                                                               │");
        System.out.println("│  客户端连接 Master                                                  │");
        System.out.println("│     │                                                               │");
        System.out.println("│     │ 如果连接失败，重新向 Sentinel 查询                            │");
        System.out.println("│     ▼                                                               │");
        System.out.println("│  故障转移后自动感知新 Master                                        │");
        System.out.println("│                                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("========== Redis Sentinel 详解完成 ==========\n");
    }
}
