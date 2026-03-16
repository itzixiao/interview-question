package cn.itzixiao.interview.redis.advanced;

/**
 * Redis 持久化与集群架构详解
 * <p>
 * RDB 持久化：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  原理：定时生成内存快照（二进制文件 dump.rdb）                │
 * │                                                             │
 * │  触发方式：                                                  │
 * │  1. 手动触发：SAVE（阻塞）、BGSAVE（后台）                   │
 * │  2. 自动触发：满足配置条件时自动执行                         │
 * │     save 900 1    # 900秒内1次修改                          │
 * │     save 300 10   # 300秒内10次修改                         │
 * │     save 60 10000 # 60秒内10000次修改                       │
 * ├─────────────────────────────────────────────────────────────┤
 * │  优点：                                                      │
 * │  - 文件紧凑，适合备份和灾难恢复                              │
 * │  - 恢复速度快                                                │
 * │  - 对性能影响小（子进程处理）                                │
 * │  缺点：                                                      │
 * │  - 可能丢失最后一次快照后的数据                              │
 * │  - 大数据集时 fork() 可能耗时                                │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * AOF 持久化：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  原理：记录所有写操作命令（Append Only File）                 │
 * │                                                             │
 * │  同步策略：                                                  │
 * │  appendfsync always   # 每次写入都同步（最安全，最慢）       │
 * │  appendfsync everysec # 每秒同步（推荐，最多丢1秒）          │
 * │  appendfsync no       # 由操作系统决定（最快，最不安全）     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  AOF 重写：                                                  │
 * │  - 解决 AOF 文件膨胀问题                                     │
 * │  - 创建当前数据集的最小命令集                                │
 * │  - 重写期间新命令写入缓冲区，重写完成后追加                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  优点：                                                      │
 * │  - 数据安全性高，最多丢1秒数据                               │
 * │  - AOF 文件可读，可手动修复                                  │
 * │  缺点：                                                      │
 * │  - 文件体积大                                                │
 * │  - 恢复速度慢                                                │
 * │  - 性能开销大                                                │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * 混合持久化（Redis 4.0+）：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  aof-use-rdb-preamble yes                                    │
 * │  - AOF 文件开头是 RDB 格式的全量数据                         │
 * │  - 后面是 AOF 格式的增量命令                                 │
 * │  - 结合两者优点：快速恢复 + 数据安全                         │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * 主从复制：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  复制方式：                                                  │
 * │  1. 全量复制：初次同步或无法部分复制时                       │
 * │  2. 部分复制：网络中断后恢复，只同步差异                     │
 * │  3. 无磁盘复制：直接通过网络发送 RDB（diskless）             │
 * ├─────────────────────────────────────────────────────────────┤
 * │  复制流程：                                                  │
 * │  1. 从节点发送 PSYNC 命令                                    │
 * │  2. 主节点执行 BGSAVE，生成 RDB                              │
 * │  3. 主节点发送 RDB 给从节点                                  │
 * │  4. 从节点加载 RDB                                           │
 * │  5. 主节点将新命令写入复制缓冲区，发送给从节点               │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * 哨兵模式（Sentinel）：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  功能：                                                      │
 * │  1. 监控：检查主从节点健康状态                               │
 * │  2. 通知：故障时通知管理员                                   │
 * │  3. 自动故障转移：主节点故障时自动选举新主节点               │
 * │  4. 配置提供：向客户端提供当前主节点地址                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  故障转移流程：                                              │
 * │  1. 主观下线：单个 Sentinel 认为主节点故障                   │
 * │  2. 客观下线：多数 Sentinel 同意主节点故障                   │
 * │  3. 选举 Leader Sentinel                                     │
 * │  4. 选择最优从节点提升为主节点                               │
 * │  5. 更新配置，通知客户端                                     │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * Cluster 集群：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  数据分片：                                                  │
 * │  - 16384 个哈希槽（slot）                                    │
 * │  - CRC16(key) % 16384 计算槽位                               │
 * │  - 每个节点负责一部分槽位                                    │
 * ├─────────────────────────────────────────────────────────────┤
 * │  节点通信：                                                  │
 * │  - Gossip 协议：节点间交换状态信息                           │
 * │  - 每秒随机 ping 几个节点                                    │
 * ├─────────────────────────────────────────────────────────────┤
 * │  请求路由：                                                  │
 * │  - MOVED 重定向：槽位已迁移                                  │
 * │  - ASK 重定向：槽位正在迁移                                  │
 * └─────────────────────────────────────────────────────────────┘
 */
public class RedisPersistenceDemo {

    /**
     * Redis 配置文件示例（redis.conf）
     */
    public static class RedisConfig {

        // ========== RDB 配置 ==========
        public static final String RDB_CONFIG =
                "# RDB 持久化配置\n" +
                        "save 900 1      # 900秒内至少1个key变化则保存\n" +
                        "save 300 10     # 300秒内至少10个key变化则保存\n" +
                        "save 60 10000   # 60秒内至少10000个key变化则保存\n" +
                        "\n" +
                        "stop-writes-on-bgsave-error yes  # 保存失败停止写入\n" +
                        "rdbcompression yes               # 压缩RDB文件\n" +
                        "rdbchecksum yes                  # 校验RDB文件\n" +
                        "dbfilename dump.rdb              # RDB文件名\n" +
                        "dir /var/lib/redis               # RDB文件保存路径\n";

        // ========== AOF 配置 ==========
        public static final String AOF_CONFIG =
                "# AOF 持久化配置\n" +
                        "appendonly yes                   # 开启AOF\n" +
                        "appendfilename \"appendonly.aof\" # AOF文件名\n" +
                        "\n" +
                        "# 同步策略\n" +
                        "appendfsync everysec             # 每秒同步\n" +
                        "# appendfsync always             # 每次写入同步\n" +
                        "# appendfsync no                 # 不同步\n" +
                        "\n" +
                        "no-appendfsync-on-rewrite no     # 重写时不停止同步\n" +
                        "auto-aof-rewrite-percentage 100  # AOF文件增长100%时重写\n" +
                        "auto-aof-rewrite-min-size 64mb   # AOF文件最小64MB才重写\n" +
                        "\n" +
                        "# 混合持久化（Redis 4.0+）\n" +
                        "aof-use-rdb-preamble yes         # 开启混合持久化\n";

        // ========== 主从复制配置 ==========
        public static final String REPLICATION_CONFIG =
                "# 主从复制配置（从节点）\n" +
                        "replicaof 192.168.1.100 6379     # 主节点地址\n" +
                        "masterauth password              # 主节点密码\n" +
                        "\n" +
                        "replica-serve-stale-data yes     # 同步期间是否响应读请求\n" +
                        "replica-read-only yes            # 从节点只读\n" +
                        "repl-diskless-sync no            # 是否使用无磁盘复制\n" +
                        "repl-backlog-size 1mb            # 复制缓冲区大小\n";

        // ========== 哨兵配置 ==========
        public static final String SENTINEL_CONFIG =
                "# Sentinel 配置\n" +
                        "port 26379\n" +
                        "\n" +
                        "# 监控主节点 mymaster，至少2个哨兵同意才故障转移\n" +
                        "sentinel monitor mymaster 192.168.1.100 6379 2\n" +
                        "\n" +
                        "sentinel down-after-milliseconds mymaster 5000   # 5秒无响应认为下线\n" +
                        "sentinel parallel-syncs mymaster 1               # 故障转移时同时同步的从节点数\n" +
                        "sentinel failover-timeout mymaster 60000         # 故障转移超时时间\n" +
                        "sentinel auth-pass mymaster password             # 主节点密码\n";

        // ========== Cluster 配置 ==========
        public static final String CLUSTER_CONFIG =
                "# Cluster 配置\n" +
                        "cluster-enabled yes              # 开启集群模式\n" +
                        "cluster-config-file nodes-6379.conf  # 集群配置文件\n" +
                        "cluster-node-timeout 5000        # 节点超时时间\n" +
                        "\n" +
                        "# 从节点配置\n" +
                        "cluster-slave-validity-factor 10 # 从节点延迟有效性因子\n" +
                        "cluster-migration-barrier 1      # 主节点保留的最少从节点数\n" +
                        "cluster-require-full-coverage yes # 需要所有槽位都被覆盖\n";
    }

    /**
     * 持久化策略选择建议
     */
    public static class PersistenceStrategy {

        /**
         * 方案1：RDB  only（不推荐生产环境）
         * 适用：数据可丢失，追求性能
         */
        public void rdbOnly() {
            System.out.println("RDB Only 配置：");
            System.out.println("save 900 1");
            System.out.println("save 300 10");
            System.out.println("save 60 10000");
            System.out.println("appendonly no");
        }

        /**
         * 方案2：AOF only
         * 适用：数据不能丢失，可接受性能损失
         */
        public void aofOnly() {
            System.out.println("AOF Only 配置：");
            System.out.println("appendonly yes");
            System.out.println("appendfsync everysec");
            System.out.println("# 禁用 RDB");
            System.out.println("save \"\"");
        }

        /**
         * 方案3：RDB + AOF（推荐）
         * 适用：生产环境，平衡性能与安全
         */
        public void rdbAndAof() {
            System.out.println("RDB + AOF 配置（推荐）：");
            System.out.println("# RDB");
            System.out.println("save 900 1");
            System.out.println("save 300 10");
            System.out.println("save 60 10000");
            System.out.println("# AOF");
            System.out.println("appendonly yes");
            System.out.println("appendfsync everysec");
            System.out.println("auto-aof-rewrite-percentage 100");
            System.out.println("auto-aof-rewrite-min-size 64mb");
        }

        /**
         * 方案4：混合持久化（Redis 4.0+ 推荐）
         * 适用：生产环境，最佳方案
         */
        public void hybridPersistence() {
            System.out.println("混合持久化配置（最佳）：");
            System.out.println("appendonly yes");
            System.out.println("appendfsync everysec");
            System.out.println("aof-use-rdb-preamble yes");  // 关键配置
            System.out.println("save 900 1");
            System.out.println("save 300 10");
        }
    }

    /**
     * 集群架构选择
     */
    public static class ClusterArchitecture {

        /**
         * 架构1：主从复制（读写分离）
         * 适用：读多写少，数据量不大
         */
        public void masterSlaveArchitecture() {
            System.out.println("主从复制架构：");
            System.out.println("    Master (写)");
            System.out.println("    /    |    \\");
            System.out.println("Slave1 Slave2 Slave3 (读)");
            System.out.println("\n特点：");
            System.out.println("- 手动故障转移");
            System.out.println("- 读写分离");
            System.out.println("- 数据全量复制");
        }

        /**
         * 架构2：哨兵模式（高可用）
         * 适用：需要自动故障转移
         */
        public void sentinelArchitecture() {
            System.out.println("哨兵模式架构：");
            System.out.println("       Sentinel1");
            System.out.println("      /    |    \\");
            System.out.println("Sentinel2  |  Sentinel3");
            System.out.println("      \\    |    /");
            System.out.println("       Master (写)");
            System.out.println("       /    |    \\");
            System.out.println("   Slave1 Slave2 Slave3 (读)");
            System.out.println("\n特点：");
            System.out.println("- 自动故障转移");
            System.out.println("- 监控和通知");
            System.out.println("- 最少3个哨兵节点");
        }

        /**
         * 架构3：Cluster 集群（数据分片）
         * 适用：大数据量，高并发
         */
        public void clusterArchitecture() {
            System.out.println("Cluster 集群架构：");
            System.out.println("  Master1 (0-5460)    Master2 (5461-10922)    Master3 (10923-16383)");
            System.out.println("     |                    |                       |");
            System.out.println("  Slave1               Slave2                  Slave3");
            System.out.println("\n特点：");
            System.out.println("- 数据分片（16384个槽）");
            System.out.println("- 自动故障转移");
            System.out.println("- 水平扩展");
            System.out.println("- 最少6个节点（3主3从）");
        }
    }

    /**
     * 运维命令
     */
    public static class RedisCommands {

        // 持久化相关
        public static final String[] PERSISTENCE_COMMANDS = {
                "SAVE",                          // 同步保存RDB
                "BGSAVE",                        // 后台异步保存RDB
                "LASTSAVE",                      // 返回上次RDB保存时间
                "BGREWRITEAOF",                  // 异步重写AOF
                "CONFIG GET appendonly",         // 查看AOF配置
                "CONFIG GET save",               // 查看RDB配置
        };

        // 复制相关
        public static final String[] REPLICATION_COMMANDS = {
                "INFO replication",              // 查看复制信息
                "ROLE",                          // 查看角色
                "SLAVEOF host port",             // 设置主节点
                "SLAVEOF NO ONE",                // 取消复制
                "PSYNC replicationid offset",    // 部分重同步
        };

        // 集群相关
        public static final String[] CLUSTER_COMMANDS = {
                "CLUSTER INFO",                  // 集群信息
                "CLUSTER NODES",                 // 集群节点
                "CLUSTER SLOTS",                 // 槽位分配
                "CLUSTER KEYSLOT key",           // 计算key的槽位
                "CLUSTER COUNTKEYSINSLOT slot",  // 槽位key数量
                "CLUSTER MEET ip port",          // 添加节点
                "CLUSTER REPLICATE node-id",     // 设为从节点
                "CLUSTER FAILOVER",              // 手动故障转移
                "CLUSTER ADDSLOTS slot...",      // 分配槽位
                "CLUSTER DELSLOTS slot...",      // 删除槽位
        };

        // 哨兵相关
        public static final String[] SENTINEL_COMMANDS = {
                "SENTINEL masters",              // 查看所有主节点
                "SENTINEL slaves mymaster",      // 查看从节点
                "SENTINEL get-master-addr-by-name mymaster", // 获取主节点地址
                "SENTINEL failover mymaster",    // 手动故障转移
                "SENTINEL monitor mymaster ip port quorum", // 添加监控
        };
    }
}
