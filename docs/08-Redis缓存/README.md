# Redis 缓存与分布式锁知识点详解

## 📚 文档列表

#### 1. [01-Redis 缓存与分布式锁.md](./01-Redis%E7%BC%93%E5%AD%98%E4%B8%8E%E5%88%86%E5%B8%83%E5%BC%8F%E9%94%81.md)
- **内容：** Redis 基础、数据结构、持久化机制、分布式锁实现（RedLock）
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 初级 ~ 中级

#### 2. [02-Redis-Sentinel与 Cluster.md](./02-Redis-Sentinel%E4%B8%8ECluster.md)
- **内容：** 主从复制、哨兵机制、Cluster 分片、高可用方案
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 中级 ~ 高级

#### 3. [03-Redis 持久化详解.md](./03-Redis持久化详解.md)
- **内容：** RDB 快照、AOF 日志、混合持久化、性能对比
- **面试题：** 12+ 道
- **重要程度：** ⭐⭐⭐⭐
- **难度：** 中级

#### 4. [04-Redis 高频面试题汇总.md](./04-Redis%E9%AB%98%E9%A2%91%E9%9D%A2%E8%AF%95%E9%A2%98%E6%B1%87%E6%80%BB.md)
- **内容：** 缓存穿透/击穿/雪崩、热点 Key、大 Value、内存淘汰策略
- **面试题：** 25+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 中级 ~ 高级

---

## 📊 统计信息

- **文档数：** 4 个
- **面试题总数：** 72+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/redis/` 目录（10 个文件，~4,500 行代码）

---

## 🎯 学习建议

### 第一阶段：Redis 基础（2-3 天）
1. **五大基本数据结构**
   - String、List、Set、Hash、ZSet
   - 底层实现原理（SDS、链表、跳表、字典）

2. **持久化机制**
   - RDB 快照的触发时机和优缺点
   - AOF 日志的写入和重写机制
   - 混合持久化的最佳实践

3. **事务与 Lua 脚本**
   - MULTI/EXEC/WATCH 命令
   - Lua 脚本保证原子性

### 第二阶段：分布式锁（2-3 天）
1. **基础实现**
   - SETNX + EXPIRE
   - Redisson 框架使用

2. **进阶问题**
   - 锁超时与看门狗机制
   - 可重入锁实现
   - 红锁（RedLock）算法

3. **生产实践**
   - 锁的粒度控制
   - 死锁预防
   - 性能优化

### 第三阶段：高可用架构（3-4 天）
1. **主从复制**
   - 全量复制与增量复制
   - 复制缓冲区管理

2. **哨兵机制**
   - 监控与故障转移
   - 主观下线与客观下线
   - Leader 选举

3. **Cluster 分片**
   - 槽位分配（16384 个槽）
   - 数据分片策略
   - 集群扩容与缩容

### 第四阶段：高级应用与优化（3-4 天）
1. **缓存三大问题**
   - 缓存穿透：布隆过滤器、空值缓存
   - 缓存击穿：互斥锁、逻辑过期
   - 缓存雪崩：随机 TTL、多级缓存

2. **性能优化**
   - 热点 Key 发现与处理
   - 大 Value 拆分与优化
   - 内存淘汰策略选择

3. **最佳实践**
   - Key 命名规范
   - 批量操作（Pipeline、MSET）
   - 慢查询分析与监控

---

## 🔗 相关链接

- [项目总览](../../README.md)
- [代码索引](../../../interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/代码索引.md)
- [Redis 示例代码](../../../interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/redis/)

---

## 📖 推荐学习顺序

```
Redis 基础数据结构
   ↓
持久化机制（RDB/AOF）
   ↓
事务与 Lua 脚本
   ↓
分布式锁实现
   ↓
主从复制与哨兵
   ↓
Cluster 分片集群
   ↓
缓存三大问题
   ↓
性能优化与实战
```

---

## 💡 高频面试题 Top 15

### 数据结构与持久化
1. **Redis 有哪些基本数据结构？底层如何实现？**
2. **String 类型的最大存储容量是多少？**
3. **ZSet 的跳表实现原理是什么？**
4. **RDB 和 AOF 的区别？如何选择？**
5. **AOF 重写的过程是怎样的？**

### 分布式锁
6. **如何用 Redis 实现分布式锁？**
7. **如何解决锁超时导致的死锁问题？**
8. **Redisson 的看门狗机制是如何工作的？**
9. **什么是 RedLock 算法？适用场景？**
10. **Redis 锁如何保证可重入性？**

### 高可用架构
11. **Redis 主从复制的原理是什么？**
12. **哨兵机制是如何进行故障转移的？**
13. **Redis Cluster 有多少个槽位？如何分配？**
14. **客户端如何访问 Cluster 集群？**

### 缓存问题与优化
15. **什么是缓存穿透、击穿、雪崩？如何解决？**
16. **如何处理热点 Key 和大 Value 问题？**
17. **Redis 内存淘汰策略有哪些？**
18. **如何保证缓存与数据库的双写一致性？**

---

## 🛠️ 实战技巧

### 分布式锁基础实现
```java
// 使用 Jedis 实现分布式锁
public boolean tryLock(String key, String value, long timeout) {
    // SET key value NX EX timeout（原子操作）
    String result = jedis.set(key, value, "NX", "EX", timeout);
    return "OK".equals(result);
}

// 释放锁（Lua 脚本保证原子性）
public void unlock(String key, String value) {
    String script = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('del', KEYS[1]) " +
        "else return 0 end";
    jedis.eval(script, Collections.singletonList(key), 
               Collections.singletonList(value));
}
```

### Redisson 分布式锁
```java
RLock lock = redisson.getLock("myLock");

// 尝试加锁（自动续期）
if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
    try {
        // 业务逻辑
    } finally {
        lock.unlock();
    }
}
```

### 缓存穿透解决方案
```java
// 布隆过滤器
RBloomFilter<String> bloomFilter = redisson.getBloomFilter("urlFilter");
bloomFilter.tryInit(1000000L, 0.03);

// 检查是否存在
if (!bloomFilter.contains(url)) {
    throw new BusinessException("URL 不存在");
}
```

### 缓存击穿解决方案
```java
// 互斥锁方案
public String getData(String key) {
    String value = redis.get(key);
    if (value == null) {
        // 尝试获取分布式锁
        if (tryLock("lock:" + key)) {
            try {
                // 双重检查
                value = redis.get(key);
                if (value == null) {
                    value = db.query(key);
                    redis.setex(key, 300, value);
                }
            } finally {
                unlock("lock:" + key);
            }
        } else {
            // 等待后重试
            Thread.sleep(50);
            return getData(key);
        }
    }
    return value;
}
```

---

## 📈 更新日志

### v2.0 - 2026-03-08
- ✅ 新增 4 篇核心文档
- ✅ 补充 72+ 道高频面试题
- ✅ 添加分布式锁完整实现方案
- ✅ 补充缓存三大问题解决方案
- ✅ 添加实战技巧和代码示例

### v1.0 - 早期版本
- ✅ 基础 Redis 文档
- ✅ 简单数据结构介绍

---

## 🎯 下一步计划

- [ ] 增加 Redis 6.0 新特性详解
- [ ] 补充 Redis Stream 消息队列
- [ ] 添加 Redis Graph 图数据库
- [ ] 完善 Redis 监控体系
- [ ] 增加更多生产环境案例

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
