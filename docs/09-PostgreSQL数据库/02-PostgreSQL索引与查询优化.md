# PostgreSQL 索引与查询优化详解

## 一、执行计划分析（EXPLAIN）

### 1.1 基本用法

```sql
-- 查看执行计划（不执行）
EXPLAIN SELECT * FROM users WHERE email = 'test@example.com';

-- 查看实际执行计划（实际执行，获取真实数据）
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';

-- 完整信息（包含缓冲区使用情况）
EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT)
SELECT * FROM orders WHERE user_id = 100 AND created_at > now() - interval '30 days';
```

### 1.2 关键节点类型

| 节点类型               | 说明                         | 性能提示              |
|--------------------|----------------------------|--------------------|
| `Seq Scan`         | 全表顺序扫描                    | 大表应避免              |
| `Index Scan`       | B-Tree 索引扫描（返回数据行）        | 索引命中，注意回表开销        |
| `Index Only Scan`  | 覆盖索引扫描（**无需回表**）          | 最优，充分利用             |
| `Bitmap Index Scan`| 位图索引扫描（多条件 AND/OR 合并）    | 中等选择率场景            |
| `Nested Loop`      | 嵌套循环 JOIN（小表驱动大表）         | 小数据集 JOIN 推荐       |
| `Hash Join`        | 哈希连接（大表等值 JOIN）           | 内存足够时高效            |
| `Merge Join`       | 归并连接（两侧已排序）               | 有序大数据集 JOIN        |
| `Sort`             | 排序操作                       | 避免大数据集无索引排序        |
| `Hash Aggregate`   | 哈希聚合（GROUP BY）             | 内存充足时推荐            |

### 1.3 解读 EXPLAIN 输出

```sql
EXPLAIN ANALYZE SELECT u.name, COUNT(o.id)
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
WHERE u.created_at > '2024-01-01'
GROUP BY u.name;
```

关注以下指标：
- **cost**：`startup_cost..total_cost`（估算代价，单位为页面 I/O 操作）
- **rows**：预计返回行数
- **actual time**：实际耗时（毫秒）
- **actual rows**：实际返回行数
- **loops**：循环次数

> 若 `rows` 与 `actual rows` 相差 10 倍以上，说明统计信息过时，需执行 `ANALYZE tablename`。

---

## 二、索引优化策略

### 2.1 复合索引最左匹配

```sql
-- 复合索引
CREATE INDEX idx_user_status_time ON orders(user_id, status, created_at);

-- ✅ 走索引（最左前缀）
SELECT * FROM orders WHERE user_id = 1;
SELECT * FROM orders WHERE user_id = 1 AND status = 'paid';
SELECT * FROM orders WHERE user_id = 1 AND status = 'paid' AND created_at > '2024-01-01';

-- ❌ 不走索引（缺少最左列）
SELECT * FROM orders WHERE status = 'paid';
SELECT * FROM orders WHERE created_at > '2024-01-01';
```

### 2.2 覆盖索引（Index Only Scan）

```sql
-- 建立包含查询所需所有列的索引
CREATE INDEX idx_cover ON users(status, name, email);

-- ✅ 触发 Index Only Scan（无需回表）
SELECT name, email FROM users WHERE status = 'active';
```

### 2.3 统计信息更新

```sql
-- 更新表的统计信息，优化器依赖此数据
ANALYZE users;
ANALYZE orders;

-- 调整统计精度（默认100，范围1-10000）
ALTER TABLE orders ALTER COLUMN user_id SET STATISTICS 500;
ANALYZE orders;
```

### 2.4 慢查询日志

```sql
-- postgresql.conf 配置
-- log_min_duration_statement = 1000  # 超过 1000ms 的查询记录日志

-- 通过 pg_stat_statements 查看
SELECT query, calls, total_exec_time, mean_exec_time, rows
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;

-- 启用 pg_stat_statements
-- 在 postgresql.conf 中添加：shared_preload_libraries = 'pg_stat_statements'
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

---

## 三、常见查询优化技巧

### 3.1 避免全表扫描

```sql
-- ❌ 函数导致索引失效
SELECT * FROM users WHERE LOWER(email) = 'test@example.com';

-- ✅ 使用表达式索引
CREATE INDEX idx_lower_email ON users (LOWER(email));
SELECT * FROM users WHERE LOWER(email) = 'test@example.com';
```

### 3.2 深分页优化

```sql
-- ❌ 深分页性能差，OFFSET 需扫描大量行
SELECT * FROM orders ORDER BY id LIMIT 10 OFFSET 1000000;

-- ✅ 游标分页（Keyset Pagination）
SELECT * FROM orders WHERE id > 1000000 ORDER BY id LIMIT 10;

-- ✅ 延迟关联
SELECT o.* FROM orders o
JOIN (SELECT id FROM orders ORDER BY id LIMIT 10 OFFSET 1000000) tmp
ON o.id = tmp.id;
```

### 3.3 使用 CTE 优化复杂查询

```sql
-- WITH 公共表表达式，提升可读性与复用性
WITH ranked_orders AS (
    SELECT
        user_id,
        amount,
        ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC) AS rn
    FROM orders
    WHERE status = 'paid'
),
top_users AS (
    SELECT user_id, SUM(amount) AS total
    FROM orders
    WHERE status = 'paid'
    GROUP BY user_id
    HAVING SUM(amount) > 10000
)
SELECT u.name, t.total
FROM users u
JOIN top_users t ON u.id = t.user_id
ORDER BY t.total DESC;
```

### 3.4 窗口函数替代子查询

```sql
-- ❌ 低效子查询
SELECT * FROM orders o1
WHERE o1.amount = (
    SELECT MAX(amount) FROM orders o2 WHERE o2.user_id = o1.user_id
);

-- ✅ 窗口函数
SELECT * FROM (
    SELECT *,
        MAX(amount) OVER (PARTITION BY user_id) AS max_amount
    FROM orders
) t
WHERE amount = max_amount;
```

### 3.5 批量操作优化

```sql
-- ✅ COPY 命令批量导入（比 INSERT 快数十倍）
COPY orders(user_id, amount, status, created_at)
FROM '/tmp/orders.csv'
WITH (FORMAT csv, HEADER true);

-- ✅ 批量 INSERT（减少事务开销）
INSERT INTO orders(user_id, amount, status)
VALUES
  (1, 100.00, 'paid'),
  (2, 200.00, 'paid'),
  (3, 150.00, 'pending');

-- ✅ UPSERT（INSERT ON CONFLICT）
INSERT INTO users(id, name, email)
VALUES (1, '张三', 'zhangsan@example.com')
ON CONFLICT (id) DO UPDATE
  SET name = EXCLUDED.name,
      email = EXCLUDED.email;
```

---

## 四、高频面试题

**问题 1：PostgreSQL 中如何分析 SQL 性能瓶颈？**

**答：**

1. **EXPLAIN ANALYZE**：查看实际执行计划，对比估算与实际行数
2. **pg_stat_statements**：找出系统中执行时间最长的 SQL
3. **慢查询日志**：配置 `log_min_duration_statement` 记录慢查询
4. **pg_stat_user_tables**：查看表的顺序扫描次数，判断是否缺少索引
5. **ANALYZE**：若 `rows` 估算严重偏差，需更新统计信息

```sql
-- 查找缺少索引的表（顺序扫描多）
SELECT relname, seq_scan, idx_scan,
       seq_scan::float / NULLIF(seq_scan + idx_scan, 0) AS seq_ratio
FROM pg_stat_user_tables
WHERE seq_scan > 1000
ORDER BY seq_scan DESC;
```

---

**问题 2：PostgreSQL 的 GIN 索引和 GiST 索引有什么区别？**

**答：**

| 对比项     | GIN（倒排索引）                     | GiST（广义搜索树）               |
|---------|--------------------------------|------------------------------|
| 适用场景  | JSONB、数组、全文检索（tsvector）        | 几何类型、范围类型、全文检索              |
| 查询速度  | 读取快                            | 读取稍慢                         |
| 构建速度  | 慢（写入开销大）                       | 快                             |
| 更新性能  | 有 Pending List 缓冲，批量写入         | 实时更新                         |
| 存储占用  | 较大                             | 较小                           |
| 支持操作符 | `@>`, `<@`, `?`, `?|`, `?&`   | `&&`, `<<`, `>>`, `@>`, `<@` |

**选择建议：**
- 需要 JSONB 查询、数组包含查询、全文检索 → 选 GIN
- 需要地理空间查询（PostGIS）、范围重叠查询 → 选 GiST

---

**问题 3：EXPLAIN ANALYZE 中 rows 与 actual rows 差距大怎么办？**

**答：**

差距大说明统计信息过期，查询优化器选择了错误的执行计划。

**解决步骤：**

```sql
-- 1. 手动更新统计信息
ANALYZE tablename;

-- 2. 对选择率低的列提高统计精度
ALTER TABLE orders ALTER COLUMN status SET STATISTICS 500;
ANALYZE orders;

-- 3. 如果使用了复杂表达式，可创建统计对象
CREATE STATISTICS stat_user_status ON user_id, status FROM orders;
ANALYZE orders;

-- 4. 调整 autovacuum 参数，更频繁地自动更新统计
ALTER TABLE orders SET (autovacuum_analyze_scale_factor = 0.01);
```
