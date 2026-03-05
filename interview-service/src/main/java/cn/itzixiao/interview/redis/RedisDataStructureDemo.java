package cn.itzixiao.interview.redis;

import java.util.*;

/**
 * Redis 数据结构与使用场景
 *
 * Redis 数据类型：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  String - 字符串，最基础类型                                  │
 * │  Hash   - 哈希，适合存储对象                                  │
 * │  List   - 列表，双向链表，适合队列                             │
 * │  Set    - 集合，无序唯一，适合去重、交集并集                    │
 * │  ZSet   - 有序集合，带权重排序，适合排行榜                      │
 * │  Bitmap - 位图，节省空间，适合签到、在线状态                    │
 * │  HyperLogLog - 基数统计，适合UV统计                           │
 * │  Geo    - 地理位置，适合附近的人                               │
 * │  Stream - 流，消息队列，适合日志收集                           │
 * └─────────────────────────────────────────────────────────────┘
 */
public class RedisDataStructureDemo {

    public static void main(String[] args) {
        System.out.println("========== Redis 数据结构与使用场景 ==========\n");

        // 1. String 类型
        demonstrateString();

        // 2. Hash 类型
        demonstrateHash();

        // 3. List 类型
        demonstrateList();

        // 4. Set 类型
        demonstrateSet();

        // 5. ZSet 类型
        demonstrateZSet();

        // 6. 其他数据类型
        demonstrateOthers();

        // 7. 实际应用场景
        demonstrateRealWorldScenarios();
    }

    /**
     * 1. String 类型
     */
    private static void demonstrateString() {
        System.out.println("【1. String 类型】\n");

        System.out.println("基本操作：");
        System.out.println("SET key value [EX seconds] [PX milliseconds] [NX|XX]");
        System.out.println("GET key");
        System.out.println("INCR key / DECR key");
        System.out.println("MSET key1 value1 key2 value2");
        System.out.println("SETEX key seconds value\n");

        System.out.println("使用场景：");
        System.out.println("1. 缓存对象（JSON序列化）");
        System.out.println("   SET user:1001 '{\"id\":1001,\"name\":\"张三\"}' EX 3600");
        System.out.println();
        System.out.println("2. 计数器");
        System.out.println("   INCR view_count:article:123");
        System.out.println("   INCRBY view_count:article:123 10");
        System.out.println();
        System.out.println("3. 分布式锁");
        System.out.println("   SET lock:order:123 randomValue NX EX 30");
        System.out.println();
        System.out.println("4. 限流");
        System.out.println("   INCR rate_limit:ip:192.168.1.1");
        System.out.println("   EXPIRE rate_limit:ip:192.168.1.1 60\n");
    }

    /**
     * 2. Hash 类型
     */
    private static void demonstrateHash() {
        System.out.println("【2. Hash 类型】\n");

        System.out.println("基本操作：");
        System.out.println("HSET key field value");
        System.out.println("HGET key field");
        System.out.println("HGETALL key");
        System.out.println("HMSET key field1 value1 field2 value2");
        System.out.println("HINCRBY key field increment\n");

        System.out.println("存储对比：");
        System.out.println("方式1 - String（JSON）：");
        System.out.println("  SET user:1001 '{\"id\":1001,\"name\":\"张三\",\"age\":25}'");
        System.out.println("  - 优点：简单，整体操作");
        System.out.println("  - 缺点：修改一个字段需要整体更新\n");

        System.out.println("方式2 - Hash：");
        System.out.println("  HSET user:1001 id 1001");
        System.out.println("  HSET user:1001 name 张三");
        System.out.println("  HSET user:1001 age 25");
        System.out.println("  - 优点：可以单独修改字段，节省内存（ziplist编码）");
        System.out.println("  - 缺点：只能对一级字段操作，不能嵌套\n");

        System.out.println("使用场景：");
        System.out.println("1. 存储对象信息（用户信息、商品信息）");
        System.out.println("2. 购物车（HSET cart:user:1001 product:2001 2）");
        System.out.println("3. 配置信息存储\n");
    }

    /**
     * 3. List 类型
     */
    private static void demonstrateList() {
        System.out.println("【3. List 类型】\n");

        System.out.println("基本操作：");
        System.out.println("LPUSH key value [value ...]  - 左侧插入");
        System.out.println("RPUSH key value [value ...]  - 右侧插入");
        System.out.println("LPOP key                     - 左侧弹出");
        System.out.println("RPOP key                     - 右侧弹出");
        System.out.println("BLPOP key [key ...] timeout  - 阻塞左侧弹出");
        System.out.println("LRANGE key start stop        - 获取范围元素");
        System.out.println("LLEN key                     - 获取列表长度\n");

        System.out.println("数据结构：双向链表");
        System.out.println("┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐");
        System.out.println("│  Node1  │←──→│  Node2  │←──→│  Node3  │←──→│  Node4  │");
        System.out.println("│  (头)   │    │         │    │         │    │  (尾)   │");
        System.out.println("└─────────┘    └─────────┘    └─────────┘    └─────────┘");
        System.out.println("LPUSH/LPOP ←                          → RPUSH/RPOP\n");

        System.out.println("使用场景：");
        System.out.println("1. 消息队列");
        System.out.println("   生产者：LPUSH queue:message '{\"msg\":\"hello\"}'");
        System.out.println("   消费者：BRPOP queue:message 0");
        System.out.println();
        System.out.println("2. 最新N条数据（时间线）");
        System.out.println("   LPUSH timeline:user:1001 '微博内容'");
        System.out.println("   LTRIM timeline:user:1001 0 99  // 只保留100条");
        System.out.println();
        System.out.println("3. 栈（LPUSH + LPOP）");
        System.out.println("4. 队列（LPUSH + RPOP / RPUSH + LPOP）\n");
    }

    /**
     * 4. Set 类型
     */
    private static void demonstrateSet() {
        System.out.println("【4. Set 类型】\n");

        System.out.println("基本操作：");
        System.out.println("SADD key member [member ...]   - 添加成员");
        System.out.println("SREM key member [member ...]   - 移除成员");
        System.out.println("SMEMBERS key                   - 获取所有成员");
        System.out.println("SISMEMBER key member           - 判断是否存在");
        System.out.println("SCARD key                      - 获取成员数量");
        System.out.println("SINTER key [key ...]           - 交集");
        System.out.println("SUNION key [key ...]           - 并集");
        System.out.println("SDIFF key [key ...]            - 差集\n");

        System.out.println("使用场景：");
        System.out.println("1. 标签系统");
        System.out.println("   SADD tag:java article:1001 article:1002");
        System.out.println("   SADD tag:redis article:1002 article:1003");
        System.out.println("   SINTER tag:java tag:redis  // 同时有java和redis标签的文章");
        System.out.println();
        System.out.println("2. 共同好友");
        System.out.println("   SADD friends:user:1001 2001 2002 2003");
        System.out.println("   SADD friends:user:1002 2002 2003 2004");
        System.out.println("   SINTER friends:user:1001 friends:user:1002  // 共同好友");
        System.out.println();
        System.out.println("3. 抽奖（随机不重复）");
        System.out.println("   SADD lottery:activity:1 user:1001 user:1002 ...");
        System.out.println("   SRANDMEMBER lottery:activity:1 3  // 随机抽3个（不删除）");
        System.out.println("   SPOP lottery:activity:1 3         // 随机抽3个（删除）");
        System.out.println();
        System.out.println("4. 点赞/收藏");
        System.out.println("   SADD like:article:1001 user:1001");
        System.out.println("   SREM like:article:1001 user:1001  // 取消点赞");
        System.out.println("   SCARD like:article:1001           // 点赞数");
        System.out.println("   SISMEMBER like:article:1001 user:1001  // 是否点赞\n");
    }

    /**
     * 5. ZSet 类型
     */
    private static void demonstrateZSet() {
        System.out.println("【5. ZSet (Sorted Set) 类型】\n");

        System.out.println("基本操作：");
        System.out.println("ZADD key score member [score member ...]  - 添加");
        System.out.println("ZREM key member [member ...]              - 移除");
        System.out.println("ZRANGE key start stop [WITHSCORES]        - 按分数升序获取");
        System.out.println("ZREVRANGE key start stop [WITHSCORES]     - 按分数降序获取");
        System.out.println("ZRANGEBYSCORE key min max                 - 按分数范围获取");
        System.out.println("ZSCORE key member                         - 获取成员分数");
        System.out.println("ZCARD key                                 - 获取成员数量");
        System.out.println("ZINCRBY key increment member              - 增加分数\n");

        System.out.println("数据结构：跳表 (Skip List) + 哈希表");
        System.out.println("┌─────────────────────────────────────────────────────────┐");
        System.out.println("│  跳表层：                                                │");
        System.out.println("│       L4:  1 ──────────────────────────→ 9              │");
        System.out.println("│       L3:  1 ────────────→ 5 ──────────→ 9              │");
        System.out.println("│       L2:  1 ────→ 3 ────→ 5 ────→ 7 ───→ 9              │");
        System.out.println("│       L1:  1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9            │");
        System.out.println("│                                                         │");
        System.out.println("│  时间复杂度：查找 O(log N)，空间复杂度 O(N)               │");
        System.out.println("└─────────────────────────────────────────────────────────┘\n");

        System.out.println("使用场景：");
        System.out.println("1. 排行榜");
        System.out.println("   ZADD leaderboard:game1 1000 user:1001");
        System.out.println("   ZINCRBY leaderboard:game1 100 user:1001  // 加分");
        System.out.println("   ZREVRANGE leaderboard:game1 0 9 WITHSCORES  // Top10");
        System.out.println("   ZRANK leaderboard:game1 user:1001  // 用户排名");
        System.out.println();
        System.out.println("2. 延时队列");
        System.out.println("   ZADD delay_queue timestamp task_id");
        System.out.println("   ZRANGEBYSCORE delay_queue 0 current_timestamp LIMIT 0 1");
        System.out.println();
        System.out.println("3. 滑动窗口限流");
        System.out.println("   ZADD rate_limit:user:1001 current_timestamp timestamp");
        System.out.println("   ZREMRANGEBYSCORE rate_limit:user:1001 0 current_timestamp-60");
        System.out.println("   ZCARD rate_limit:user:1001  // 最近60秒请求数\n");
    }

    /**
     * 6. 其他数据类型
     */
    private static void demonstrateOthers() {
        System.out.println("【6. 其他数据类型】\n");

        System.out.println("Bitmap（位图）：");
        System.out.println("SETBIT key offset value    - 设置某一位");
        System.out.println("GETBIT key offset          - 获取某一位");
        System.out.println("BITCOUNT key               - 统计1的个数");
        System.out.println("BITOP operation destkey key [key ...]  - 位运算");
        System.out.println();
        System.out.println("使用场景：");
        System.out.println("1. 用户签到（365天只需46字节）");
        System.out.println("   SETBIT user:1001:sign 0 1  // 第1天签到");
        System.out.println("   SETBIT user:1001:sign 1 1  // 第2天签到");
        System.out.println("   BITCOUNT user:1001:sign    // 统计签到天数");
        System.out.println();
        System.out.println("2. 在线状态");
        System.out.println("   SETBIT online_users 1001 1  // user:1001 上线");
        System.out.println("   SETBIT online_users 1001 0  // user:1001 下线");
        System.out.println("   BITCOUNT online_users       // 在线人数\n");

        System.out.println("HyperLogLog（基数统计）：");
        System.out.println("PFADD key element [element ...]");
        System.out.println("PFCOUNT key [key ...]");
        System.out.println("PFMERGE destkey sourcekey [sourcekey ...]");
        System.out.println();
        System.out.println("特点：");
        System.out.println("- 不存储具体元素，只统计基数（去重后的数量）");
        System.out.println("- 标准误差 0.81%");
        System.out.println("- 占用固定 12KB 内存");
        System.out.println();
        System.out.println("使用场景：UV统计（页面访问用户数）");
        System.out.println("   PFADD uv:page:home user:1001 user:1002 ...");
        System.out.println("   PFCOUNT uv:page:home  // 估算UV\n");

        System.out.println("Geo（地理位置）：");
        System.out.println("GEOADD key longitude latitude member [longitude latitude member ...]");
        System.out.println("GEOPOS key member [member ...]");
        System.out.println("GEODIST key member1 member2 [M|KM|FT|MI]");
        System.out.println("GEORADIUS key longitude latitude radius M|KM|FT|MI [WITHCOORD] [WITHDIST] [WITHHASH] [COUNT count] [ASC|DESC]");
        System.out.println();
        System.out.println("使用场景：附近的人、距离计算");
        System.out.println("   GEOADD locations 116.40 39.90 'beijing'");
        System.out.println("   GEOADD locations 121.47 31.23 'shanghai'");
        System.out.println("   GEODIST locations beijing shanghai KM  // 距离");
        System.out.println("   GEORADIUS locations 116.40 39.90 100 KM WITHDIST  // 附近100km\n");
    }

    /**
     * 7. 实际应用场景
     */
    private static void demonstrateRealWorldScenarios() {
        System.out.println("【7. 实际应用场景综合】\n");

        System.out.println("场景1：秒杀系统");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 库存预扣（String DECR）                                   │");
        System.out.println("│     SET stock:product:1001 1000                               │");
        System.out.println("│     DECR stock:product:1001  // 原子扣减                      │");
        System.out.println("│                                                             │");
        System.out.println("│  2. 去重（Set）                                               │");
        System.out.println("│     SADD seckill:product:1001:user user:1001                  │");
        System.out.println("│     // 返回1表示成功，0表示已抢购过                           │");
        System.out.println("│                                                             │");
        System.out.println("│  3. 订单异步处理（List）                                       │");
        System.out.println("│     LPUSH seckill:orders '{\"user\":1001,\"product\":1001}'      │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("场景2：社交网络");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 关注/粉丝（Set）                                          │");
        System.out.println("│     SADD following:user:1001 2001 2002  // 关注列表           │");
        System.out.println("│     SADD followers:user:2001 1001       // 粉丝列表           │");
        System.out.println("│     SINTER following:user:1001 following:user:1002  // 共同关注│");
        System.out.println("│                                                             │");
        System.out.println("│  2. 时间线（List）                                            │");
        System.out.println("│     LPUSH timeline:user:1001 '{\"content\":\"...\"}'            │");
        System.out.println("│     LRANGE timeline:user:1001 0 20  // 获取最新20条           │");
        System.out.println("│                                                             │");
        System.out.println("│  3. 点赞（Set）                                               │");
        System.out.println("│     SADD like:post:1001 user:2001 user:2002                   │");
        System.out.println("│     SCARD like:post:1001  // 点赞数                           │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("场景3：实时统计");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 计数器（String）                                          │");
        System.out.println("│     INCR view_count:article:1001                              │");
        System.out.println("│                                                             │");
        System.out.println("│  2. 排行榜（ZSet）                                            │");
        System.out.println("│     ZINCRBY hot_articles 1 article:1001                       │");
        System.out.println("│     ZREVRANGE hot_articles 0 9  // Top10热门文章              │");
        System.out.println("│                                                             │");
        System.out.println("│  3. UV统计（HyperLogLog）                                     │");
        System.out.println("│     PFADD uv:daily:20240101 user:1001 user:1002 ...           │");
        System.out.println("│     PFCOUNT uv:daily:20240101                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Redis 使用建议：");
        System.out.println("1. Key 设计：业务名:表名:ID（如 user:profile:1001）");
        System.out.println("2. 避免大 Key：String < 10KB，集合元素 < 5000");
        System.out.println("3. 设置过期时间，避免数据无限增长");
        System.out.println("4. 热点 Key 分散：key_{0-N} 分散到多个 key");
        System.out.println("5. 使用 Pipeline 批量操作，减少网络往返");
        System.out.println("6. 慎用 O(N) 命令：KEYS *、FLUSHALL、HGETALL（大Hash）\n");
    }
}
