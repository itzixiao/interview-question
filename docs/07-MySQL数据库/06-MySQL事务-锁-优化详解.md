# MySQL 事务、锁机制与性能优化详解

## 概述

本文档涵盖 MySQL 核心知识点：
- 事务 ACID 特性与隔离级别
- InnoDB 锁机制（行锁、间隙锁、临键锁）
- EXPLAIN 执行计划分析
- 慢查询优化与大表处理
- 批量处理最佳实践

---

## 第一部分：事务 ACID 特性

### 1.1 ACID 四大特性

| 特性 | 含义 | 实现机制 |
|------|------|----------|
| **A - 原子性** | 事务不可分割，全成功或全失败 | Undo Log（回滚日志） |
| **C - 一致性** | 数据从一个一致状态到另一个一致状态 | 由 A、I、D 共同保证 |
| **I - 隔离性** | 并发事务相互隔离 | MVCC + 锁机制 |
| **D - 持久性** | 提交后数据永久保存 | Redo Log（重做日志） |

### 1.2 Redo Log vs Undo Log

| 特性 | Redo Log | Undo Log |
|------|----------|----------|
| 作用 | 保证持久性（崩溃恢复） | 保证原子性（事务回滚） |
| 内容 | 物理日志（数据页修改） | 逻辑日志（反向操作） |
| 其他用途 | - | MVCC 多版本读 |

---

## 第二部分：事务隔离级别

### 2.1 并发事务问题

| 问题 | 描述 |
|------|------|
| **脏读** | 读取到其他事务未提交的数据 |
| **不可重复读** | 同一事务内两次读取同一数据，结果不同 |
| **幻读** | 同一事务内两次查询，记录数量不同 |

### 2.2 四种隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 实现方式 |
|----------|------|------------|------|----------|
| READ UNCOMMITTED | 可能 | 可能 | 可能 | 无隔离 |
| READ COMMITTED | 不可能 | 可能 | 可能 | 每次SELECT新建ReadView |
| **REPEATABLE READ** | 不可能 | 不可能 | 不可能* | 事务开始时创建ReadView |
| SERIALIZABLE | 不可能 | 不可能 | 不可能 | 串行加锁 |

> * InnoDB 在 RR 级别通过 MVCC + 间隙锁解决幻读

### 2.3 设置隔离级别

```sql
-- 查看当前隔离级别
SELECT @@transaction_isolation;

-- 设置会话隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

---

## 第三部分：InnoDB 锁机制

### 3.1 锁分类

#### 按粒度分类

| 类型 | 特点 |
|------|------|
| 表级锁 | 开销小，并发度低，MyISAM 使用 |
| 行级锁 | 开销大，并发度高，InnoDB 使用 |

#### 按模式分类

| 类型 | 说明 | SQL |
|------|------|-----|
| 共享锁 (S锁) | 读锁，多事务可同时持有 | `SELECT ... LOCK IN SHARE MODE` |
| 排他锁 (X锁) | 写锁，独占 | `SELECT ... FOR UPDATE` |

### 3.2 InnoDB 行锁类型

#### Record Lock（记录锁）
- 锁定单条索引记录
- 示例：`SELECT * FROM t WHERE id = 5 FOR UPDATE;`

#### Gap Lock（间隙锁）
- 锁定索引记录之间的间隙
- 解决幻读问题
- 只在 RR 隔离级别存在

#### Next-Key Lock（临键锁）
- Record Lock + Gap Lock
- InnoDB 默认锁类型
- 锁定记录本身 + 前面的间隙

### 3.3 死锁

**原因**：两个事务相互等待对方持有的锁

**避免方法**：
1. 固定加锁顺序（最有效）
2. 缩短事务时间
3. 使用 RC 隔离级别
4. 合理设计索引
5. 设置锁等待超时

---

## 第四部分：EXPLAIN 执行计划

### 4.1 使用方法

```sql
EXPLAIN SELECT * FROM user WHERE id = 1;
EXPLAIN FORMAT=JSON SELECT * FROM user WHERE id = 1;
EXPLAIN ANALYZE SELECT * FROM user WHERE id = 1;  -- MySQL 8.0+
```

### 4.2 关键字段详解

#### type（访问类型）- 性能从好到差

| 类型 | 含义 | 示例 |
|------|------|------|
| system | 系统表，只有一行 | - |
| const | 主键/唯一索引等值查询 | `WHERE id = 1` |
| eq_ref | JOIN 时使用主键/唯一索引 | - |
| ref | 非唯一索引等值查询 | `WHERE name = 'Tom'` |
| range | 索引范围扫描 | `WHERE age BETWEEN 20 AND 30` |
| index | 全索引扫描 | - |
| ALL | 全表扫描（要避免！） | - |

#### Extra（额外信息）

| 值 | 含义 |
|----|------|
| Using index | 覆盖索引，不需要回表 ✓ |
| Using where | Server层过滤 |
| Using index condition | 索引下推 ✓ |
| Using temporary | 使用临时表 ⚠️ |
| Using filesort | 文件排序 ⚠️ |

### 4.3 优化目标

1. type 至少达到 range，最好是 ref 或 const
2. 避免 Using filesort 和 Using temporary
3. 争取 Using index（覆盖索引）

---

## 第五部分：慢查询优化

### 5.1 开启慢查询日志

```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;  -- 秒
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

### 5.2 索引失效场景

1. 违反最左前缀原则
2. 索引列做运算或函数：`WHERE YEAR(date) = 2024`
3. 隐式类型转换：`WHERE phone = 13800138000`（phone是VARCHAR）
4. LIKE 以 % 开头：`WHERE name LIKE '%张'`
5. OR 条件两边有一边没索引
6. 使用 != 或 <>

### 5.3 深分页优化

**问题**：`LIMIT 1000000, 10` 需要扫描100万行

**优化方案**：

```sql
-- 方案1：延迟关联
SELECT o.* FROM orders o
INNER JOIN (
    SELECT id FROM orders ORDER BY create_time DESC LIMIT 1000000, 10
) tmp ON o.id = tmp.id;

-- 方案2：游标分页
SELECT * FROM orders
WHERE id < 上次最后一条ID
ORDER BY id DESC LIMIT 10;
```

---

## 第六部分：大表优化

### 6.1 优化策略

| 策略 | 适用场景 |
|------|----------|
| 数据归档 | 冷热数据分离 |
| 分区表 | 按时间范围查询 |
| 分库分表 | 单表超过2000万行 |

### 6.2 分区表示例

```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(32),
    create_time DATETIME
) PARTITION BY RANGE (TO_DAYS(create_time)) (
    PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

---

## 第七部分：批量处理

### 7.1 批量插入

```sql
-- 多值INSERT（推荐）
INSERT INTO user (name, age) VALUES
    ('Tom', 20), ('Jerry', 22), ('Bob', 25);
```

### 7.2 JDBC批量插入

```java
// 开启批量重写
String url = "jdbc:mysql://...?rewriteBatchedStatements=true";

PreparedStatement ps = conn.prepareStatement(sql);
for (User user : users) {
    ps.setString(1, user.getName());
    ps.addBatch();
    if (count % 1000 == 0) {
        ps.executeBatch();
        ps.clearBatch();
    }
}
ps.executeBatch();
```

### 7.3 批量删除

```sql
-- 分批删除，避免长事务
DELETE FROM orders WHERE create_time < '2024-01-01' LIMIT 10000;
-- 循环执行直到删除完成
```

---

## 高频面试题

**问题 1:MySQL事务的ACID特性是什么？各由什么机制保证？

**答**：
- A - 原子性：Undo Log 实现
- C - 一致性：由 A、I、D 共同保证
- I - 隔离性：MVCC + 锁机制
- D - 持久性：Redo Log 实现

---

**问题 2:MySQL默认隔离级别是什么？为什么？

**答**：
REPEATABLE READ（可重复读）。

原因：
1. 解决脏读和不可重复读
2. InnoDB 通过 MVCC + 间隙锁还解决了幻读
3. 相比 SERIALIZABLE 有更好的并发性能

---

**问题 3:InnoDB 有哪些行锁类型？

**答**：
1. Record Lock（记录锁）：锁定单条记录
2. Gap Lock（间隙锁）：锁定索引间隙，解决幻读
3. Next-Key Lock（临键锁）：Record + Gap，默认锁类型

---

**问题 4:什么是死锁？如何避免？

**答**：
死锁：两个或多个事务相互等待对方持有的锁。

避免方法：
1. 固定加锁顺序（最有效）
2. 缩短事务时间
3. 使用 RC 隔离级别（无间隙锁）
4. 合理设计索引
5. 设置锁等待超时

---

**问题 5:EXPLAIN 中 type 字段有哪些值？

**答**：
性能从好到差：system → const → eq_ref → ref → range → index → ALL

- const：主键/唯一索引等值查询
- ref：非唯一索引等值查询
- range：索引范围扫描
- ALL：全表扫描（要避免）

---

**问题 6:如何优化深分页问题？

**答**：
1. 延迟关联：先用覆盖索引查ID，再JOIN查完整数据
2. 游标分页：记住上一页最后一条的ID
3. 避免跳页：只提供上一页/下一页
4. ES 搜索：数据量特别大时

---

**问题 7:索引失效的常见场景有哪些？

**答**：
1. 违反最左前缀原则
2. 索引列做运算或函数
3. 隐式类型转换
4. LIKE 以 % 开头
5. OR 条件两边有一边没索引

---

**问题 8:什么是覆盖索引？

**答**：
覆盖索引：查询的列全部包含在索引中，不需要回表。

好处：
- 减少 IO
- 减少随机 IO
- EXPLAIN 中 Extra 显示 Using index

---

**问题 9:大表如何删除数据？

**答**：
原则：分批删除，避免长事务。

```sql
DELETE FROM table WHERE condition LIMIT 10000;
-- 循环执行直到删除完成
-- 每批之间适当间隔
```

---

**问题 10:批量插入如何优化？

**答**：
1. 使用多值 INSERT 语法
2. JDBC 开启 `rewriteBatchedStatements=true`
3. 使用 `addBatch()` + `executeBatch()`
4. 控制每批数量（500-1000条）
5. 关闭自动提交，手动控制事务

---

## 相关代码示例

- [TransactionAndLockDemo.java](../../interview-service/src/main/java/cn/itzixiao/interview/mysql/TransactionAndLockDemo.java) - 事务与锁机制
- [BigTableOptimizationDemo.java](../../interview-service/src/main/java/cn/itzixiao/interview/mysql/BigTableOptimizationDemo.java) - 大表优化与批量处理
- [IndexOptimizationDemo.java](../../interview-service/src/main/java/cn/itzixiao/interview/mysql/IndexOptimizationDemo.java) - 索引优化
- [MVCCDemo.java](../../interview-service/src/main/java/cn/itzixiao/interview/mysql/MVCCDemo.java) - MVCC原理
