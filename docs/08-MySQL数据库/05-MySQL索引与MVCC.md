# MySQL 索引与 MVCC

## 索引

### 索引类型

#### 按数据结构

| 类型        | 特点          | 适用场景  |
|-----------|-------------|-------|
| B+Tree    | 支持范围查询、排序   | 大多数场景 |
| Hash      | 精确匹配快，不支持范围 | 等值查询  |
| Full-text | 全文检索        | 文本搜索  |
| R-Tree    | 空间索引        | 地理位置  |

#### 按功能

| 类型   | 说明          |
|------|-------------|
| 主键索引 | 唯一标识记录，聚簇索引 |
| 唯一索引 | 保证列值唯一      |
| 普通索引 | 加速查询        |
| 组合索引 | 多列联合索引      |
| 覆盖索引 | 查询字段都在索引中   |

### B+Tree 结构

```
                    [10, 20, 30]
                   /     |      \
            [1,5,9]  [11,15,19]  [21,25,29,35]
             / | \    /  |  \     /  |  \  \
           叶子节点（存储完整数据）
           叶子节点之间双向链表连接
```

**特点**：

- 非叶子节点只存键值，不存数据
- 叶子节点存完整数据
- 叶子节点有序且链表连接，便于范围查询

### 聚簇索引 vs 非聚簇索引

| 特性   | 聚簇索引      | 非聚簇索引    |
|------|-----------|----------|
| 数据存储 | 叶子节点存完整数据 | 叶子节点存主键值 |
| 数量   | 只能有一个     | 可以有多个    |
| 查询效率 | 高（直接取数据）  | 可能需要回表   |
| 插入效率 | 需要维护顺序    | 较快       |

### 索引优化原则

#### 最左前缀原则

```sql
-- 索引：(a, b, c)
WHERE a = 1              -- 使用索引
WHERE a = 1 AND b = 2    -- 使用索引
WHERE a = 1 AND b = 2 AND c = 3  -- 使用索引
WHERE b = 2              -- 不使用索引
WHERE a = 1 AND c = 3    -- 部分使用（只用 a）
```

#### 索引失效场景

1. **使用函数**：`WHERE YEAR(create_time) = 2023`
2. **类型转换**：`WHERE id = '123'`（id 是 int）
3. **前导模糊**：`WHERE name LIKE '%abc'`
4. **OR 条件**：`WHERE a = 1 OR b = 2`（b 无索引）
5. **NOT/<>**：`WHERE status != 1`
6. **IS NOT NULL**：`WHERE name IS NOT NULL`
7. **索引列计算**：`WHERE id + 1 = 100`

### EXPLAIN 分析

```sql
EXPLAIN SELECT * FROM user WHERE id = 1;
```

| 字段    | 说明                                                        |
|-------|-----------------------------------------------------------|
| type  | 访问类型（system > const > eq_ref > ref > range > index > all） |
| key   | 使用的索引                                                     |
| rows  | 扫描行数                                                      |
| Extra | 额外信息（Using index、Using where、Using filesort）              |

## MVCC（多版本并发控制）

### 核心概念

MVCC 实现**读不加锁**，提高并发性能。

### 隐藏字段

每行记录包含三个隐藏字段：

| 字段          | 大小  | 说明               |
|-------------|-----|------------------|
| DB_TRX_ID   | 6字节 | 最后修改该记录的事务ID     |
| DB_ROLL_PTR | 7字节 | 回滚指针，指向 Undo Log |
| DB_ROW_ID   | 6字节 | 隐藏主键（无主键时）       |

### Undo Log 版本链

```
当前记录 ← Undo Log 1 ← Undo Log 2 ← Undo Log 3
  (最新)    (上个版本)    (更早版本)    (最早版本)
```

### Read View（读视图）

事务执行 SELECT 时创建的一致性视图，包含：

| 字段             | 说明                  |
|----------------|---------------------|
| creator_trx_id | 创建该 Read View 的事务ID |
| m_ids          | 活跃事务ID列表            |
| min_trx_id     | 最小活跃事务ID            |
| max_trx_id     | 最大活跃事务ID + 1        |

### 可见性判断规则

对于某条记录的 DB_TRX_ID：

1. **DB_TRX_ID == creator_trx_id**：自己修改的，可见
2. **DB_TRX_ID < min_trx_id**：已提交，可见
3. **DB_TRX_ID >= max_trx_id**：将来事务修改的，不可见
4. **min_trx_id <= DB_TRX_ID < max_trx_id**：
    - 在 m_ids 中：未提交，不可见
    - 不在 m_ids 中：已提交，可见

### 隔离级别实现

| 隔离级别             | Read View 创建时机            |
|------------------|---------------------------|
| READ UNCOMMITTED | 直接读取最新数据                  |
| READ COMMITTED   | 每次 SELECT 创建新 Read View   |
| REPEATABLE READ  | 事务第一次 SELECT 创建 Read View |
| SERIALIZABLE     | 加锁访问                      |

### 当前读 vs 快照读

#### 快照读（Snapshot Read）

```sql
SELECT * FROM user WHERE id = 1;  -- 普通 SELECT，使用 MVCC
```

- 不加锁
- 读取历史版本

#### 当前读（Current Read）

```sql
SELECT * FROM user WHERE id = 1 FOR UPDATE;  -- 加锁读
SELECT * FROM user WHERE id = 1 LOCK IN SHARE MODE;
INSERT / UPDATE / DELETE
```

- 加锁（Record Lock、Gap Lock、Next-Key Lock）
- 读取最新版本

## 最佳实践

1. **索引设计**：
    - 区分度高的列放前面
    - 避免冗余索引
    - 定期分析慢查询

2. **事务使用**：
    - 尽量使用 REPEATABLE READ
    - 事务尽量短小
    - 避免长事务

3. **避免幻读**：
    - 使用 SERIALIZABLE
    - 或使用 SELECT ... FOR UPDATE（Next-Key Lock）

---

## 高频面试题

**问题 1:B+Tree 为什么适合做索引？**

答案：

1. **非叶子节点只存键值**：可以容纳更多索引项，树更矮胖
2. **查询性能稳定**：所有查询都要走到叶子节点，稳定 O(log N)
3. **适合范围查询**：叶子节点用链表连接，直接遍历即可
4. **减少 IO 次数**：树高度通常 3-4 层，IO 次数少

**问题 2：聚簇索引和非聚簇索引的区别？**

答案：

| 对比项  | 聚簇索引      | 非聚簇索引          |
|------|-----------|----------------|
| 存储内容 | 叶子节点存完整数据 | 叶子节点存主键值 + 索引列 |
| 数量限制 | 只能有一个     | 可以有多个          |
| 查询效率 | 高（直接获取数据） | 可能需要回表查询       |
| 插入效率 | 需要维护顺序，较慢 | 较快             |

**问题 3：什么是 MVCC？如何实现？**

答案：
**MVCC（Multi-Version Concurrency Control）** 是多版本并发控制技术，实现读不加锁。

**实现原理：**

1. **隐藏字段**：每行记录有 DB_TRX_ID、DB_ROLL_PTR、DB_ROW_ID
2. **Undo Log 版本链**：串联历史版本
3. **Read View**：判断数据版本对当前事务是否可见

**优势：** 读写不冲突，提升并发性能

**问题 4:Read View 如何判断可见性？**

答案：
对于某条记录的 DB_TRX_ID：

1. **DB_TRX_ID == creator_trx_id**：自己修改的，可见
2. **DB_TRX_ID < min_trx_id**：已提交，可见
3. **DB_TRX_ID >= max_trx_id**：将来事务修改的，不可见
4. **min_trx_id <= DB_TRX_ID < max_trx_id**：
    - 在 m_ids 中：未提交，不可见
    - 不在 m_ids 中：已提交，可见

**问题 5：当前读和快照读的区别？**

答案：

| 类型  | SQL 示例                                     | 是否加锁 | 读取版本       |
|-----|--------------------------------------------|------|------------|
| 快照读 | SELECT * FROM t WHERE id = 1               | 否    | 历史版本（MVCC） |
| 当前读 | SELECT ... FOR UPDATE / LOCK IN SHARE MODE | 是    | 最新版本       |

**问题 6：如何避免幻读？**

答案：

1. **SERIALIZABLE 隔离级别**：强制串行执行
2. **Next-Key Lock**：RR 级别下，InnoDB 默认使用
3. **SELECT ... FOR UPDATE**：加锁访问
4. **MVCC + Next-Key Lock**：MySQL 的 RR 级别基本避免幻读
