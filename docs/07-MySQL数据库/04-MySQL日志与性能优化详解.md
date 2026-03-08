# MySQL 日志系统与性能优化详解

## 一、MySQL 日志系统

### 1.1 Redo Log（重做日志）

**作用：** 保证事务持久性，崩溃后恢复

**特点：**
- InnoDB **特有**
- **循环写入**（空间固定）
- **顺序写**，性能高
- 记录的是**物理日志**（数据页修改后的值）

**大小配置：**
```sql
-- 查看 Redo Log 大小
SHOW VARIABLES LIKE 'innodb_log_file_size';

-- 查看 Redo Log 文件组大小
SHOW VARIABLES LIKE 'innodb_log_files_in_group';
```

**写入流程：**
```
1. 修改 Buffer Pool 中的数据页
2. 写入 Redo Log（内存 → 磁盘）
3. 异步刷盘（Checkpoint）
```

### 1.2 Undo Log（回滚日志）

**作用：** 保证事务原子性，支持回滚

**特点：**
- 记录**逻辑操作**（如 INSERT 对应 DELETE）
- 支持 **MVCC**（多版本并发控制）
- 事务提交后不会立即删除（用于快照读）
- 可以清理（由 purge 线程负责）

**示例：**
```sql
BEGIN;
  INSERT INTO users VALUES(1, 'Tom');
  -- Undo Log 记录：DELETE FROM users WHERE id=1
  
  UPDATE users SET age = 18 WHERE id = 1;
  -- Undo Log 记录：UPDATE users SET age = old_age WHERE id=1
  
ROLLBACK;  -- 通过 Undo Log 恢复
```

### 1.3 Binlog（归档日志）

**作用：** 记录所有 DDL 和 DML，用于主从复制和数据恢复

**特点：**
- **Server 层**实现，所有引擎都有
- **追加写入**（不会循环）
- 记录的是 SQL 语句或行变更

**三种格式：**

| 格式 | 说明 | 优缺点 |
|------|------|--------|
| STATEMENT | 记录原始 SQL | 占用小，但可能有风险 |
| ROW | 记录行变更 | 可靠，但占用大 |
| MIXED | 混合模式（默认） | 智能选择 |

**查看 Binlog：**
```sql
-- 开启 Binlog（my.cnf）
[mysqld]
log-bin=mysql-bin
binlog-format=MIXED

-- 查看 Binlog 文件
SHOW BINARY LOGS;

-- 查看当前正在写入的 Binlog
SHOW MASTER STATUS;

-- 查看 Binlog 内容
mysqlbinlog mysql-bin.000001;
```

### 1.4 Redo Log vs Binlog

| 对比项 | Redo Log | Binlog |
|--------|----------|--------|
| 所属层级 | InnoDB 引擎层 | Server 层 |
| 写入方式 | 循环写入（空间固定） | 追加写入（文件递增） |
| 记录内容 | 物理日志（数据页） | 逻辑日志（SQL/行） |
| 主要作用 | 崩溃恢复 | 主从复制、数据恢复 |
| 写入时机 | 事务进行中 | 事务提交后 |
| 空间大小 | 固定（可配置） | 不固定（持续增加） |

### 1.5 两阶段提交

**目的：** 保证 Redo Log 和 Binlog 的一致性

**流程：**
```
1. Prepare 阶段：写入 Redo Log，标记为 prepare 状态
2. 写入 Binlog
3. Commit 阶段：提交事务，标记 Redo Log 为 commit 状态
```

**为什么需要两阶段？**

避免主从复制时数据不一致：
```
场景：没有两阶段提交

1. 写入 Redo Log
2. 写入 Binlog
3. 宕机！

结果：
- 如果 Redo Log 已刷盘，Binlog 丢失 → 主库有数据，从库无数据
- 如果 Binlog 已刷盘，Redo Log 丢失 → 主库无数据，从库有数据
```

### 1.6 Slow Query Log（慢查询日志）

**作用：** 记录执行时间超过阈值的 SQL

**配置：**
```sql
-- my.cnf 配置
[mysqld]
slow_query_log = 1              # 开启慢查询
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2             # 阈值（秒，默认 10 秒）
log_queries_not_using_indexes = 1  # 记录未使用索引的查询

-- 查看配置
SHOW VARIABLES LIKE '%slow%';
SHOW VARIABLES LIKE 'long_query_time';
```

---

## 二、性能优化

### 2.1 EXPLAIN 分析 SQL

**语法：**
```sql
EXPLAIN SELECT * FROM users WHERE id = 1;
```

**关键字段说明：**

| 字段 | 说明 | 好坏标准 |
|------|------|----------|
| type | 访问类型 | system > const > eq_ref > ref > range > index > ALL |
| possible_keys | 可能使用的索引 | - |
| key | 实际使用的索引 | 有值好 |
| rows | 扫描的行数 | 越少越好 |
| Extra | 额外信息 | Using index 好，Using filesort 差 |

**type 类型详解：**

| type | 说明 | 示例 |
|------|------|------|
| system | 系统表，只有一行 | - |
| const | 主键或唯一索引 | WHERE id = 1 |
| eq_ref | 主键或唯一索引连接 | JOIN ON t1.id = t2.id |
| ref | 普通索引 | WHERE name = 'Tom' |
| range | 索引范围查询 | WHERE id > 10 |
| index | 全索引扫描 | SELECT COUNT(*) |
| ALL | 全表扫描（最差） | 无索引 |

**Extra 常见值：**

| 值 | 说明 | 好坏 |
|----|------|------|
| Using index | 覆盖索引 | ✅ 好 |
| Using where | 使用 WHERE 过滤 | 一般 |
| Using temporary | 使用临时表 | ❌ 差 |
| Using filesort | 文件排序 | ❌ 差 |
| Using join buffer | 使用连接缓存 | ❌ 差 |

### 2.2 索引优化策略

**1. 为 WHERE、ORDER BY、GROUP BY 创建索引**
```sql
-- 为查询条件加索引
CREATE INDEX idx_email ON users(email);

-- 为排序字段加索引
CREATE INDEX idx_create_time ON users(create_time);
```

**2. 使用覆盖索引（避免回表）**
```sql
-- 创建联合索引
CREATE INDEX idx_name_age ON users(name, age);

-- 覆盖索引查询
SELECT id, name, age FROM users WHERE name = 'Tom';
```

**3. 联合索引遵循最左匹配原则**
```sql
-- 联合索引 idx_name_age_city (name, age, city)

-- ✅ 可以使用索引
WHERE name = 'Tom' AND age = 18;

-- ❌ 无法使用索引
WHERE age = 18 AND city = 'Beijing';
```

**4. 避免索引失效**
```sql
-- ❌ 函数操作导致索引失效
SELECT * FROM users WHERE YEAR(create_time) = 2024;

-- ✅ 改为范围查询
SELECT * FROM users WHERE create_time >= '2024-01-01';
```

**5. 前缀索引（针对长字符串）**
```sql
-- 全文索引
CREATE INDEX idx_description ON users(description(50));
```

**6. 定期清理无用索引**
```sql
-- 查看未使用的索引
SELECT * FROM sys.schema_unused_indexes;

-- 删除无用索引
DROP INDEX idx_old ON users;
```

### 2.3 SQL 语句优化

**推荐做法：**

✅ **SELECT 指定字段，不用 SELECT \***
```sql
-- ❌ 增加网络传输
SELECT * FROM users;

-- ✅ 只查需要的字段
SELECT id, name, email FROM users;
```

✅ **用小表驱动大表（EXISTS vs IN）**
```sql
-- 小表：order（1000 条）
-- 大表：user（100 万条）

-- ✅ EXISTS（先扫描小表）
SELECT * FROM user u WHERE EXISTS (
    SELECT 1 FROM order o WHERE o.user_id = u.id
);

-- ❌ IN（先扫描大表）
SELECT * FROM user u WHERE u.id IN (
    SELECT user_id FROM order
);
```

✅ **UNION ALL 代替 UNION（不去重）**
```sql
-- ❌ 去重，性能差
SELECT id FROM users WHERE id < 100
UNION
SELECT id FROM users WHERE id > 900;

-- ✅ 不去重，性能好
SELECT id FROM users WHERE id < 100
UNION ALL
SELECT id FROM users WHERE id > 900;
```

✅ **批量操作代替单条操作**
```sql
-- ❌ 单条插入（1000 次 IO）
INSERT INTO users VALUES(1, 'Tom');
INSERT INTO users VALUES(2, 'Jerry');
...

-- ✅ 批量插入（1 次 IO）
INSERT INTO users VALUES
(1, 'Tom'),
(2, 'Jerry'),
...;
```

✅ **LIMIT 分页优化（延迟关联）**
```sql
-- ❌ 深度分页（扫描 100010 条）
SELECT * FROM users LIMIT 100000, 10;

-- ✅ 延迟关联（先查 ID）
SELECT u.* FROM users u
INNER JOIN (
    SELECT id FROM users LIMIT 100000, 10
) tmp ON u.id = tmp.id;

-- ✅ 游标分页（WHERE id > last_id）
SELECT * FROM users WHERE id > 100000 LIMIT 10;
```

**避免的做法：**

❌ **LIKE '%xxx'（无法用索引）**
```sql
-- ❌ 索引失效
SELECT * FROM users WHERE name LIKE '%Tom';

-- ✅ 使用前缀
SELECT * FROM users WHERE name LIKE 'Tom%';
```

❌ **OR 连接（可能全表扫描）**
```sql
-- ❌ 有一边没索引
SELECT * FROM users WHERE email = 'test@example.com' OR name = 'Tom';

-- ✅ 使用 UNION ALL
SELECT * FROM users WHERE email = 'test@example.com'
UNION ALL
SELECT * FROM users WHERE name = 'Tom';
```

❌ **函数操作（导致索引失效）**
```sql
-- ❌ 索引失效
SELECT * FROM users WHERE LEFT(phone, 3) = '138';

-- ✅ 使用范围查询
SELECT * FROM users WHERE phone >= '138' AND phone < '139';
```

### 2.4 表结构优化

**1. 垂直拆分：大字段单独拆表**
```sql
-- 原始表
CREATE TABLE articles (
    id INT PRIMARY KEY,
    title VARCHAR(200),
    content TEXT,  -- 大字段
    created_at DATETIME
);

-- 垂直拆分
CREATE TABLE articles (
    id INT PRIMARY KEY,
    title VARCHAR(200),
    created_at DATETIME
);

CREATE TABLE articles_detail (
    article_id INT PRIMARY KEY,
    content TEXT,
    FOREIGN KEY (article_id) REFERENCES articles(id)
);
```

**2. 水平分区：按时间/地区分表**
```sql
-- 按月分表
CREATE TABLE orders_202401 (...);
CREATE TABLE orders_202402 (...);
CREATE TABLE orders_202403 (...);
```

**3. 适当冗余：减少 JOIN（以空间换时间）**
```sql
-- 原始设计（需要 JOIN）
SELECT o.*, u.name 
FROM orders o 
JOIN users u ON o.user_id = u.id;

-- 冗余设计（在订单表冗余用户名）
CREATE TABLE orders (
    id INT,
    user_id INT,
    user_name VARCHAR(50),  -- 冗余字段
    ...
);
```

### 2.5 配置优化

**关键参数：**

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| innodb_buffer_pool_size | 缓存池大小 | 物理内存 50%-70% |
| innodb_log_file_size | Redo Log 大小 | 512M - 1G |
| max_connections | 最大连接数 | 1000 - 2000 |
| query_cache_size | 查询缓存 | 8.0 已移除 |

**查看配置：**
```sql
-- 查看缓冲池大小
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';

-- 查看 Redo Log 大小
SHOW VARIABLES LIKE 'innodb_log_file_size';

-- 查看最大连接数
SHOW VARIABLES LIKE 'max_connections';
```

---

## 三、高频面试题

**问题 1:Redo Log 和 Binlog 的区别？**

**答：**

| 对比项 | Redo Log | Binlog |
|--------|----------|--------|
| 所属层级 | InnoDB 引擎层 | Server 层 |
| 写入方式 | 循环写入（空间固定） | 追加写入（文件递增） |
| 记录内容 | 物理日志（数据页） | 逻辑日志（SQL/行） |
| 主要作用 | 崩溃恢复 | 主从复制、数据恢复 |
| 写入时机 | 事务进行中 | 事务提交后 |

**问题 2：什么是两阶段提交？为什么需要？**

**答：**

**两阶段：**
1. **Prepare 阶段**：写入 Redo Log，标记为 prepare
2. **Commit 阶段**：写 Binlog 后提交

**目的：** 保证 Redo Log 和 Binlog 的一致性

**避免：** 主从复制时数据不一致

**问题 3：如何分析一条 SQL 的性能？**

**答：**

1. **开启慢查询日志**：定位慢 SQL
2. **使用 EXPLAIN 分析**：查看执行计划
3. **查看 type**：是否全表扫描（ALL 最差）
4. **查看 key**：是否用到索引
5. **查看 rows**：扫描行数（越少越好）
6. **查看 Extra**：Using filesort 需优化

**问题 4:SQL 优化有哪些常见手段？**

**答：**

1. **使用覆盖索引**：避免回表
2. **SELECT 指定字段**：不用 SELECT *
3. **避免索引失效**：函数、类型转换、LIKE'%xxx'
4. **小表驱动大表**：EXISTS vs IN
5. **LIMIT 分页优化**：延迟关联
6. **批量操作**：代替单条操作
7. **垂直拆分**：大字段单独拆表
8. **水平分区**：按时间/地区分表

**问题 5：深分页如何优化？**

**答：**

**问题：** `LIMIT 100000,10` 需要扫描前 100000 条

**优化方案：**

1. **延迟关联**：先查 ID，再 JOIN 原表
```sql
SELECT u.* FROM users u
INNER JOIN (
    SELECT id FROM users LIMIT 100000, 10
) tmp ON u.id = tmp.id;
```

2. **游标分页**：`WHERE id > last_id LIMIT 10`
```sql
SELECT * FROM users WHERE id > 100000 LIMIT 10;
```

3. **禁止跳页**：限制最大页码

**问题 6：覆盖索引是什么？有什么优势？**

**答：**

**覆盖索引：** 二级索引包含所有查询字段，无需回表

**优势：**
1. **避免回表**：减少 IO 次数
2. **提升性能**：直接返回数据

**示例：**
```sql
-- 创建联合索引
CREATE INDEX idx_name_age ON users(name, age);

-- 覆盖索引查询
SELECT id, name, age FROM users WHERE name = 'Tom';
```

**问题 7：索引失效的常见场景有哪些？**

**答：**

1. **模糊查询以%开头**：`LIKE '%abc'`
2. **函数操作**：`YEAR(create_time)`
3. **类型转换**：字符串字段用数字查询
4. **OR 连接**：有一边没索引
5. **违反最左匹配**：跳过左边字段
6. **IS NULL/IS NOT NULL**：可能失效
7. **!= 或 <>**：可能全表扫描

**问题 8:MySQL 性能优化有哪些方面？**

**答：**

1. **索引优化**：创建合适索引，避免失效
2. **SQL 优化**：避免 SELECT *，批量操作
3. **表结构优化**：垂直拆分、水平分区
4. **配置优化**：buffer_pool_size 等
5. **架构优化**：读写分离、分库分表

---

## 四、最佳实践

### 4.1 日志管理

1. **定期清理 Binlog**：避免磁盘占满
2. **监控 Redo Log 大小**：合理配置
3. **分析慢查询日志**：定期优化

### 4.2 性能监控

1. **开启慢查询日志**：定位问题
2. **定期检查执行计划**：EXPLAIN
3. **监控锁等待**：避免死锁
4. **分析未使用索引**：及时清理
