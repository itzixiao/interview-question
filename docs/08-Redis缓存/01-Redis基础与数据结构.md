# Redis基础与数据结构

## 一、Redis 简介


### 1.2 Redis 为什么这么快？

1. **纯内存操作**：单次操作微秒级别
2. **IO 多路复用**：采用 Reactor 模式（事件驱动），使用 epoll（Linux）/kqueue（BSD）/io_uring（Redis 7.0+）等高效 IO 模型，单线程监听多连接，避免阻塞。
3. **避免上下文切换**：无线程切换开销
4. **避免锁竞争**：无需加锁，执行效率高
5. **高效数据结构**：如 SDS、跳表、压缩列表等


### 2.2 List（列表）

**底层实现**：QuickList（双向链表，每个节点是一个 ziplist）

- Redis 3.2 之前：ziplist（小列表）或 linkedlist（大列表）
- Redis 3.2+：quicklist（结合双向链表和压缩列表的优点，默认实现）

**特点**：

### 2.5 ZSet（Sorted Set，有序集合）

**底层实现**：
- **小数据量**：ziplist（同时存储 score 和 member，按 score 有序排列）
- **大数据量**：skiplist（跳表）+ hashtable（skiplist 负责排序，hashtable 快速查询 member 对应的 score）

**跳表结构**：

**常用命令**：
```bash
ZADD rank 95 tom 90 jerry  # score=95 是 tom 的成绩，按 score 排序
ZRANGE rank 0 -1 WITHSCORES  # 获取排名及分数
```


#### HyperLogLog（基数统计）

**底层实现**：稀疏存储 / 稠密存储

**特点**：
- 稀疏模式：占用空间随数据量增长（节省内存）；
- 稠密模式：固定 12KB（16384 个桶 × 6 bits = 12KB）；
- 有误差（约 0.81%），适合大规模数据基数统计。


#### Stream（流）

**底层实现**：Rax（Radix Tree 基数树）+ ListPack（紧凑列表）
- Rax 存储消息 ID 到消息内容的映射；
- ListPack 存储同 ID 段的消息内容，节省内存。

**特点**：

**使用场景**：
- 签到统计、用户在线状态标记
- 布隆过滤器
- 状态标记


### 3.1 事务基础


**特点**：
- **不支持回滚**：命令执行失败（如类型错误）不会回滚已执行的命令；
- **弱原子性**：只有当事务入队阶段发生语法错误时，EXEC 才会全部拒绝执行；若入队成功但执行阶段部分命令失败，其余命令仍会执行；
- **无隔离级别**：事务中的命令在 EXEC 之前不会被执行，无脏读/幻读问题。


### 3.2 Lua 脚本

**优势**：

**示例 1：分布式锁释放**
```lua
-- 分布式锁释放脚本
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```
**调用方式**：
```bash
EVAL "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end" 1 lock:order 123456
-- 参数说明：1 表示 KEYS 数量，lock:order 是锁 key，123456 是锁的值
```


### 问题 2：String 类型的最大存储容量是多少？

**参考答案**：
- 最大存储：512 MB（字节）
- 实际建议：单 key 不超过 10KB（避免大 key 导致网络传输延迟、内存碎片、GC 耗时）


### 问题 3：ZSet 的跳表实现原理是什么？


**为什么不用红黑树？**
- 跳表实现更简单（红黑树需维护颜色平衡，代码复杂）；
- 范围查询效率更高（跳表可直接遍历相邻节点，红黑树需中序遍历）；
- 并发性能更好（跳表支持无锁更新，红黑树需加锁/自旋）；
- 插入/删除时，跳表只需修改相邻节点指针，红黑树需旋转调整平衡。


### 问题 5：Redis 6.0 为什么引入多线程？


**开启方式**：
```bash
io-threads 4          # IO 线程数（默认 1，建议设为 CPU 核心数的一半）
io-threads-do-reads yes  # 开启读多线程（默认 no，仅写多线程）
```
**注意**：IO 线程数并非越多越好，过多会导致线程切换开销，建议值：2/4/8（根据 CPU 核心数）。


### 5.2 批量操作

```bash
# 使用 MSET/MGET 代替多次 SET/GET
MSET k1 v1 k2 v2 k3 v3
MGET k1 k2 k3
```

**Pipeline 使用示例**（Python）：
```python
import redis
r = redis.Redis()
pipe = r.pipeline()
pipe.set('k1', 'v1')
pipe.get('k1')
pipe.incr('counter')
result = pipe.execute()  # 一次网络请求执行多个命令
```
**注意**：Pipeline 不保证原子性（与事务不同），但能将 N 次网络往返减少为 1 次，提升批量操作效率。


## 六、总结

| 数据类型 | 底层实现 | 核心特点 | 使用场景 |
|----------|----------|----------|----------|
| String | SDS | 二进制安全、O(1) 长度 | 缓存、计数器、分布式锁 |
| List | QuickList | 有序可重复、两端操作高效 | 消息队列、最新列表 |
| Hash | ziplist / hashtable | 字段级操作、适合对象 | 对象存储、购物车 |
| Set | intset / hashtable | 无序不重复、集合运算 | 标签、共同好友、去重 |
| ZSet | ziplist / skiplist+hash | 有序不重复、按分排序 | 排行榜、延时队列 |
| Bitmap | String | 按 bit 存储、极度省空间 | 签到统计、用户在线状态 |
| HyperLogLog | 稀疏/稠密存储 | 固定 12KB、有误差 | UV 统计、海量去重 |
| Stream | Rax + ListPack | 消息持久化、消费者组 | 消息队列、事件日志 |
