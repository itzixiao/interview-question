# PostgreSQL 事务、锁与高可用详解

## 一、事务与隔离级别

### 1.1 ACID 支持

PostgreSQL 完整支持 ACID，且额外支持 **DDL 事务**（MySQL 不支持）：

```sql
-- ✅ PostgreSQL 支持 DDL 事务回滚
BEGIN;
CREATE TABLE test_table (id INT);
ALTER TABLE test_table ADD COLUMN name TEXT;
ROLLBACK;  -- test_table 不会被创建，整个 DDL 被回滚
```

### 1.2 隔离级别

PostgreSQL 支持 SQL 标准的四种隔离级别，但实现比 MySQL 更严格：

| 隔离级别                         | 脏读    | 不可重复读 | 幻读    | 实现方式                     |
|------------------------------|-------|-------|-------|--------------------------|
| `READ UNCOMMITTED`           | 可能    | 可能    | 可能    | PG 内部等同 READ COMMITTED    |
| `READ COMMITTED`（默认）         | 防止    | 可能    | 可能    | 每个语句获取快照                 |
| `REPEATABLE READ`            | 防止    | 防止    | **防止** | 事务开始时获取快照（比 SQL 标准更严格）   |
| `SERIALIZABLE`               | 防止    | 防止    | 防止    | SSI（串行化快照隔离）              |

> **注意：** PostgreSQL 的 `REPEATABLE READ` 已能防止幻读，比 MySQL 的可重复读更强。

```sql
-- 设置隔离级别
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ;
-- 或
SET TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 查看当前隔离级别
SHOW transaction_isolation;
```

### 1.3 可串行化快照隔离（SSI）

PostgreSQL 的 `SERIALIZABLE` 使用 SSI 技术，无需加锁，通过检测冲突实现完全串行化：

```sql
-- Session 1
BEGIN ISOLATION LEVEL SERIALIZABLE;
SELECT SUM(amount) FROM accounts WHERE user_id = 1;  -- 读取

-- Session 2
BEGIN ISOLATION LEVEL SERIALIZABLE;
INSERT INTO accounts(user_id, amount) VALUES (1, 100);  -- 插入
COMMIT;

-- Session 1
INSERT INTO accounts(user_id, amount) VALUES (2, 200);
COMMIT;  -- 可能报错：could not serialize access due to read/write dependencies
```

---

## 二、锁机制

### 2.1 表级锁

PostgreSQL 有 8 种表级锁，从轻到重：

| 锁模式                      | 获取操作                            | 冲突锁                    |
|--------------------------|----------------------------------|------------------------|
| `ACCESS SHARE`           | SELECT                           | ACCESS EXCLUSIVE 冲突    |
| `ROW SHARE`              | SELECT FOR UPDATE/SHARE          | EXCLUSIVE, ACCESS EXCLUSIVE 冲突 |
| `ROW EXCLUSIVE`          | INSERT/UPDATE/DELETE             | SHARE, SHARE ROW EXCLUSIVE, EXCLUSIVE, ACCESS EXCLUSIVE 冲突 |
| `SHARE UPDATE EXCLUSIVE` | VACUUM, ANALYZE, CREATE INDEX CONCURRENTLY | 多种冲突 |
| `SHARE`                  | CREATE INDEX                     | ROW EXCLUSIVE+ 冲突      |
| `SHARE ROW EXCLUSIVE`    | CREATE TRIGGER                   | ROW EXCLUSIVE+ 冲突      |
| `EXCLUSIVE`              | 较少直接使用                          | ROW SHARE+ 冲突          |
| `ACCESS EXCLUSIVE`       | DROP/TRUNCATE/ALTER TABLE        | 所有锁冲突（排他最强）            |

### 2.2 行级锁

```sql
-- 悲观锁：SELECT FOR UPDATE（排他行锁）
BEGIN;
SELECT * FROM orders WHERE id = 1 FOR UPDATE;
UPDATE orders SET status = 'processing' WHERE id = 1;
COMMIT;

-- 共享行锁：SELECT FOR SHARE
SELECT * FROM users WHERE id = 1 FOR SHARE;

-- 跳过锁定行（NOWAIT 和 SKIP LOCKED）
-- NOWAIT：如果行被锁定，立即报错
SELECT * FROM orders WHERE status = 'pending' FOR UPDATE NOWAIT;

-- SKIP LOCKED：跳过被锁定的行（队列消费场景）
SELECT * FROM task_queue
WHERE status = 'pending'
ORDER BY created_at
LIMIT 10
FOR UPDATE SKIP LOCKED;
```

### 2.3 Advisory Lock（咨询锁）

PostgreSQL 特有的应用级分布式锁：

```sql
-- 获取会话级锁（事务结束不自动释放）
SELECT pg_advisory_lock(12345);
-- 业务逻辑...
SELECT pg_advisory_unlock(12345);

-- 获取事务级锁（事务结束自动释放）
BEGIN;
SELECT pg_advisory_xact_lock(12345);
-- 业务逻辑...
COMMIT;  -- 自动释放

-- 非阻塞尝试获取
SELECT pg_try_advisory_lock(12345);  -- 返回 true/false
```

### 2.4 死锁检测

PostgreSQL 自动检测死锁（默认 1 秒），检测到后随机终止一个事务：

```sql
-- 查看当前锁等待情况
SELECT
    blocked_locks.pid AS blocked_pid,
    blocked_activity.query AS blocked_query,
    blocking_locks.pid AS blocking_pid,
    blocking_activity.query AS blocking_query
FROM pg_catalog.pg_locks blocked_locks
JOIN pg_catalog.pg_stat_activity blocked_activity ON blocked_activity.pid = blocked_locks.pid
JOIN pg_catalog.pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
    AND blocking_locks.relation = blocked_locks.relation
    AND blocking_locks.pid != blocked_locks.pid
JOIN pg_catalog.pg_stat_activity blocking_activity ON blocking_activity.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted;
```

---

## 三、高可用架构

### 3.1 流复制（Streaming Replication）

PostgreSQL 原生流复制基于 WAL（Write-Ahead Log）日志传输：

```
主节点(Primary)  ──── WAL 流 ───► 备节点(Standby)
    │                                    │
  读写                               只读（Hot Standby）
```

**配置示例（postgresql.conf - 主节点）：**

```ini
wal_level = replica          # WAL 级别
max_wal_senders = 5          # 最大 WAL 发送进程数
wal_keep_size = 1GB          # 保留 WAL 大小
archive_mode = on            # 开启归档
```

**pg_hba.conf（主节点允许复制连接）：**

```
host  replication  replica  standby_ip/32  scram-sha-256
```

**recovery.conf（备节点）：**

```ini
primary_conninfo = 'host=primary_ip port=5432 user=replica password=xxx'
hot_standby = on  # 允许备节点接受只读查询
```

### 3.2 物理复制 vs 逻辑复制

| 对比项     | 物理复制（流复制）              | 逻辑复制                       |
|---------|------------------------|----------------------------|
| 复制粒度  | 整个数据库集群（字节级）           | 表级别（行级）                    |
| 跨版本    | 不支持                    | 支持（可跨大版本）                  |
| 双向复制  | 不支持                    | 支持（多主）                     |
| 选择性复制 | 不支持                    | 支持（指定表/操作）                 |
| 适用场景  | 高可用主备、读写分离             | 数据迁移、跨版本升级、异构数据同步          |

```sql
-- 逻辑复制：在主节点创建发布
CREATE PUBLICATION my_pub FOR TABLE users, orders;

-- 在订阅节点创建订阅
CREATE SUBSCRIPTION my_sub
CONNECTION 'host=primary_ip port=5432 user=replica dbname=mydb'
PUBLICATION my_pub;
```

### 3.3 Patroni 高可用方案

生产中常用 **Patroni + etcd** 实现自动故障转移：

```
┌─────────────────────────────────────────┐
│               etcd 集群                  │
│   (存储集群状态、领导者选举、配置信息)          │
└─────────────────────────────────────────┘
          ▲               ▲              ▲
          │               │              │
    ┌─────┴──┐       ┌────┴───┐    ┌────┴───┐
    │Patroni │       │Patroni │    │Patroni │
    │Primary │──WAL─►│Standby1│    │Standby2│
    │  PG    │       │  PG    │    │  PG    │
    └────────┘       └────────┘    └────────┘
         ▲
         │ VIP 漂移（HAProxy/keepalived）
         │
    ┌────┴────┐
    │ 应用层   │
    └─────────┘
```

---

## 四、连接池与性能调优

### 4.1 PgBouncer 连接池

PostgreSQL 每个连接对应一个后端进程，高并发时需要连接池：

```ini
# pgbouncer.ini
[databases]
mydb = host=127.0.0.1 port=5432 dbname=mydb

[pgbouncer]
pool_mode = transaction          # 事务级连接池（推荐）
max_client_conn = 10000          # 最大客户端连接
default_pool_size = 100          # 每个数据库的连接池大小
min_pool_size = 10
reserve_pool_size = 5
```

**Pool Mode 说明：**

| 模式            | 说明                         | 适用场景            |
|---------------|----------------------------|--------------------|
| `session`     | 会话结束才归还连接                  | 使用会话级特性（set local 等） |
| `transaction` | 事务结束归还连接（推荐）               | 大多数 Web 应用       |
| `statement`   | 语句结束归还（不支持事务）              | 简单查询              |

### 4.2 关键参数调优

```ini
# postgresql.conf 重要参数

# 内存配置
shared_buffers = 4GB               # 推荐系统内存的 25%
effective_cache_size = 12GB        # 系统总可用内存的 75%
work_mem = 64MB                    # 排序/哈希操作内存（每个操作单独分配）
maintenance_work_mem = 1GB         # VACUUM/CREATE INDEX 使用

# WAL 配置
wal_buffers = 64MB
checkpoint_completion_target = 0.9
max_wal_size = 4GB

# 查询优化器
random_page_cost = 1.1             # SSD 磁盘推荐 1.1（默认 4.0）
effective_io_concurrency = 200     # SSD 推荐 200
```

---

## 五、高频面试题

**问题 1：PostgreSQL 的 SKIP LOCKED 有什么作用？典型使用场景？**

**答：**

`SKIP LOCKED` 用于跳过已被其他事务锁定的行，非常适合**任务队列/消费者场景**。

```sql
-- 任务队列消费（多个 Worker 并发消费，互不干扰）
SELECT * FROM task_queue
WHERE status = 'pending'
ORDER BY priority DESC, created_at ASC
LIMIT 5
FOR UPDATE SKIP LOCKED;
```

**优势：** 多个 Worker 可以同时从队列取任务，不会互相等待或取到相同任务，实现高效并发消费。

---

**问题 2：PostgreSQL 的 WAL 是什么？有什么作用？**

**答：**

**WAL（Write-Ahead Log，预写日志）** 是 PostgreSQL 的核心机制：

- **崩溃恢复**：数据写入磁盘前先写 WAL，崩溃后从 WAL 重放恢复
- **流复制**：将 WAL 实时传输到备节点，实现主从同步
- **逻辑复制**：基于 WAL 的逻辑解码，实现表级别数据同步
- **PITR（时间点恢复）**：结合 base backup + WAL 归档，可恢复到任意时间点

WAL 类似于 MySQL 的 Redo Log + Binlog 的综合体。

---

**问题 3：如何在 PostgreSQL 中实现分布式锁？**

**答：**

使用 **Advisory Lock（咨询锁）**，相比 Redis 分布式锁的优势在于不需要额外组件：

```sql
-- 方案一：会话级 Advisory Lock
public boolean tryLock(long lockKey) {
    // SQL: SELECT pg_try_advisory_lock(lockKey)
    // 返回 true 表示获取成功
}

public void unlock(long lockKey) {
    // SQL: SELECT pg_advisory_unlock(lockKey)
}

-- 方案二：事务级 Advisory Lock（推荐，自动释放）
BEGIN;
SELECT pg_advisory_xact_lock(hash_key);
-- 执行业务逻辑
COMMIT;  -- 自动释放锁
```

**注意事项：**
- 会话级锁需要手动释放，连接池归还连接前必须释放
- 事务级锁更安全，推荐在事务内使用
- 锁 Key 为 64 位整数，使用业务 Key 的 hashCode
