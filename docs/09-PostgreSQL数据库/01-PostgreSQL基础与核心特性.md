# PostgreSQL 基础与核心特性详解

## 一、PostgreSQL 简介

PostgreSQL（简称 PG）是目前功能最强大的开源关系型数据库，完全遵循 SQL 标准，支持丰富的数据类型、强大的扩展性和高度的 ACID 合规性。

**核心优势：**

- ✅ 完全开源，无商业授权费用
- ✅ 严格遵循 SQL 标准（SQL:2016）
- ✅ 支持 JSONB、数组、范围类型等丰富数据类型
- ✅ 支持窗口函数、CTE、递归查询
- ✅ 强大的并发控制（MVCC）
- ✅ 原生支持表继承、分区表
- ✅ 高度可扩展（自定义类型、函数、索引）

---

## 二、PostgreSQL vs MySQL 核心对比

| 特性            | PostgreSQL                      | MySQL                        |
|---------------|---------------------------------|------------------------------|
| **SQL标准合规**  | 高（严格遵循）                        | 中（部分支持）                      |
| **数据类型**     | 丰富（JSONB、数组、hstore、几何类型）       | 基础类型为主                        |
| **事务**        | 完整 ACID，支持 DDL 事务               | 完整 ACID，DDL 不可回滚              |
| **MVCC实现**    | 多版本存储在数据文件中                    | Undo Log 链表                   |
| **索引类型**     | B-Tree、Hash、GiST、GIN、BRIN、SP-GiST | B+Tree、Hash、全文索引              |
| **窗口函数**     | 完整支持                            | 8.0+ 支持                       |
| **CTE递归**    | 完整支持                            | 8.0+ 支持                       |
| **存储过程**     | PL/pgSQL（强大）                    | 支持（较弱）                        |
| **分区表**      | 原生声明式分区                         | 原生分区（5.1+）                    |
| **并发性能**     | 读写高并发均衡                         | 读多写少场景更优                      |
| **复制方式**     | 流复制（物理/逻辑）                      | Binlog（Statement/Row/Mixed）    |
| **扩展插件**     | PostGIS、TimescaleDB、pg_stat 等   | 有限                            |
| **适用场景**     | 复杂查询、GIS、金融、OLAP                 | Web应用、简单CRUD、高速写入             |

---

## 三、数据类型

### 3.1 数值类型

| 类型          | 大小     | 说明               |
|-------------|--------|------------------|
| `SMALLINT`  | 2 字节   | -32768 ~ 32767   |
| `INTEGER`   | 4 字节   | -21亿 ~ 21亿       |
| `BIGINT`    | 8 字节   | ±9.2×10^18       |
| `DECIMAL`   | 可变     | 精确小数，等同 NUMERIC  |
| `NUMERIC`   | 可变     | 精确小数，**推荐金额存储**  |
| `REAL`      | 4 字节   | 单精度浮点数           |
| `DOUBLE PRECISION` | 8 字节 | 双精度浮点数       |
| `SERIAL`    | 4 字节   | 自增整数（等效 AUTO_INCREMENT） |
| `BIGSERIAL` | 8 字节   | 自增大整数            |

### 3.2 字符串类型

| 类型            | 说明                        |
|---------------|---------------------------|
| `CHAR(n)`     | 定长字符串，不足补空格               |
| `VARCHAR(n)`  | 变长字符串，最大长度限制              |
| `TEXT`        | 无限长度文本（PostgreSQL 推荐使用）   |

> **PostgreSQL 特点：** TEXT 与 VARCHAR 性能相同，推荐直接使用 `TEXT`。

### 3.3 日期时间类型

| 类型                       | 说明                           |
|--------------------------|------------------------------|
| `DATE`                   | 日期（YYYY-MM-DD）               |
| `TIME`                   | 时间（HH:MM:SS）                 |
| `TIMESTAMP`              | 日期+时间（无时区）                   |
| `TIMESTAMPTZ`            | 日期+时间（**带时区**，推荐使用）          |
| `INTERVAL`               | 时间间隔（如 `INTERVAL '1 day'`）   |

### 3.4 PostgreSQL 特有类型

| 类型        | 说明                           | 示例                                    |
|-----------|------------------------------|---------------------------------------|
| `BOOLEAN` | 布尔类型（true/false/null）        | `TRUE`, `FALSE`, `'yes'`, `'no'`      |
| `UUID`    | 通用唯一标识符                      | `gen_random_uuid()`                   |
| `JSONB`   | 二进制JSON（**支持索引**，推荐）         | `'{"key": "value"}'::jsonb`           |
| `JSON`    | 文本JSON（原样存储）                 | `'{"key": "value"}'::json`            |
| `ARRAY`   | 数组类型                         | `ARRAY[1,2,3]` 或 `'{1,2,3}'::int[]` |
| `HSTORE`  | 键值对存储                        | `'a=>1, b=>2'::hstore`                |
| `INET`    | IP地址                         | `'192.168.1.1'::inet`                 |
| `CIDR`    | IP网段                         | `'192.168.1.0/24'::cidr`              |
| `TSVECTOR`| 全文检索向量                       | `to_tsvector('english', 'Hello')`     |

```sql
-- JSONB 操作示例
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    profile JSONB
);

INSERT INTO users(name, profile) VALUES
('张三', '{"age": 28, "skills": ["Java", "PostgreSQL"], "city": "北京"}');

-- 查询 JSONB 字段
SELECT name, profile->>'age' AS age FROM users;
SELECT name FROM users WHERE profile @> '{"city": "北京"}';

-- JSONB 索引
CREATE INDEX idx_users_profile ON users USING GIN (profile);
```

---

## 四、索引类型详解

### 4.1 B-Tree 索引（默认）

适用：等值查询、范围查询、排序

```sql
CREATE INDEX idx_users_name ON users(name);
CREATE INDEX idx_orders_time ON orders(created_at DESC);
```

### 4.2 Hash 索引

适用：仅等值查询，不支持范围

```sql
CREATE INDEX idx_users_email_hash ON users USING HASH (email);
```

### 4.3 GIN 索引（Generalized Inverted Index）

适用：JSONB、数组、全文检索

```sql
-- JSONB GIN 索引
CREATE INDEX idx_profile_gin ON users USING GIN (profile);

-- 全文检索 GIN 索引
CREATE INDEX idx_content_fts ON articles USING GIN (to_tsvector('chinese', content));
```

### 4.4 GiST 索引（Generalized Search Tree）

适用：几何类型、范围类型、全文检索

```sql
-- 地理位置索引（PostGIS）
CREATE INDEX idx_location_gist ON places USING GIST (location);
```

### 4.5 BRIN 索引（Block Range INdex）

适用：超大表的有序列（如时间戳），**极小的存储开销**

```sql
-- 适合时序数据
CREATE INDEX idx_logs_time_brin ON logs USING BRIN (created_at);
```

### 4.6 部分索引（Partial Index）

只对满足条件的行建索引，**节省空间，提升性能**

```sql
-- 只索引状态为 active 的用户
CREATE INDEX idx_active_users ON users(email) WHERE status = 'active';
```

### 4.7 表达式索引（Expression Index）

对函数或表达式结果建索引

```sql
-- 对小写邮箱建索引，支持大小写不敏感查询
CREATE INDEX idx_lower_email ON users (lower(email));
SELECT * FROM users WHERE lower(email) = 'test@example.com';
```

---

## 五、高频面试题

**问题 1：PostgreSQL 和 MySQL 的主要区别是什么？**

**答：**

1. **SQL标准合规性**：PostgreSQL 严格遵循 SQL 标准，MySQL 有较多扩展方言
2. **DDL事务**：PostgreSQL 支持 DDL 语句的事务回滚，MySQL 不支持
3. **数据类型**：PostgreSQL 支持 JSONB、数组、hstore 等丰富类型；MySQL 主要支持基础类型
4. **索引类型**：PostgreSQL 有 B-Tree、Hash、GIN、GiST、BRIN、SP-GiST 六种索引；MySQL 主要用 B+Tree
5. **MVCC实现**：PostgreSQL 将旧版本存储在数据文件中，MySQL 通过 Undo Log 链表管理
6. **存储过程**：PostgreSQL 的 PL/pgSQL 功能更完整，支持更复杂的业务逻辑
7. **适用场景**：PostgreSQL 适合复杂查询、GIS、分析场景；MySQL 适合 Web 应用高速读写

---

**问题 2：PostgreSQL 的 JSONB 和 JSON 有什么区别？**

**答：**

| 特性      | JSON             | JSONB             |
|---------|------------------|-------------------|
| 存储方式  | 原文本存储            | 解析后二进制存储          |
| 读取速度  | 快（原样返回）          | 稍慢（需反序列化）         |
| 写入速度  | 快                | 稍慢（需解析）           |
| 索引支持  | 不支持              | **支持 GIN 索引**      |
| 键顺序    | 保留原始顺序           | 不保留，自动去重          |
| 运算符    | 有限               | 丰富（`@>`, `?`, `?|` 等） |
| 推荐场景  | 仅存储，不查询          | **需要查询/索引时使用**    |

**结论：实际开发推荐使用 JSONB。**

---

**问题 3：PostgreSQL 的 MVCC 与 MySQL 有何不同？**

**答：**

| 对比项        | PostgreSQL                       | MySQL (InnoDB)              |
|------------|----------------------------------|-----------------------------|
| 旧版本存放位置 | 数据文件本身（堆表中）                    | Undo Log 独立文件               |
| 版本清理方式  | **VACUUM** 定期清理死元组               | purge 线程自动清理                |
| 可见性判断   | 通过事务快照（snapshot）                | 通过 Read View + Undo Log 链   |
| 写放大问题   | 更新时原行标记删除，新行写入（写放大较高）          | 原地更新，旧版本放 Undo Log          |
| 好处        | 读不阻塞写，写不阻塞读                    | 读不阻塞写，写不阻塞读                 |

**PostgreSQL MVCC 关键点：**
- 每行有 `xmin`（插入事务ID）和 `xmax`（删除/更新事务ID）隐藏列
- 事务可见性通过快照（snapshot）中的 `xmin`/`xmax` 判断
- 旧版本行数据不会立即删除，需要 **VACUUM** 回收

---

**问题 4：什么是 VACUUM？为什么 PostgreSQL 需要它？**

**答：**

由于 MVCC 机制，PostgreSQL 更新/删除行时不直接删除物理数据，而是将旧行标记为"死元组"（dead tuple）。`VACUUM` 用于回收这些空间。

**VACUUM 类型：**

| 类型               | 说明                              |
|------------------|-----------------------------------|
| `VACUUM`         | 清理死元组，不锁表，不归还 OS 空间            |
| `VACUUM FULL`    | 重建表文件，归还 OS 空间，**需要排他锁**       |
| `AUTOVACUUM`     | 后台自动触发，推荐开启                     |
| `ANALYZE`        | 更新统计信息，配合查询优化器使用               |
| `VACUUM ANALYZE` | 同时执行清理和统计更新                     |

```sql
-- 手动 VACUUM
VACUUM orders;
VACUUM ANALYZE orders;
VACUUM FULL orders;  -- 谨慎使用，会锁表

-- 查看死元组数量
SELECT relname, n_dead_tup, last_vacuum, last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

---

**问题 5：PostgreSQL 如何实现分区表？**

**答：**

PostgreSQL 10+ 支持声明式分区（Declarative Partitioning），支持范围分区、列表分区、哈希分区。

```sql
-- 范围分区（按年）
CREATE TABLE orders (
    id BIGSERIAL,
    user_id INT,
    amount NUMERIC(10,2),
    created_at TIMESTAMPTZ NOT NULL
) PARTITION BY RANGE (created_at);

CREATE TABLE orders_2024 PARTITION OF orders
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');

CREATE TABLE orders_2025 PARTITION OF orders
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');

-- 哈希分区（按用户ID分片）
CREATE TABLE user_logs (
    id BIGSERIAL,
    user_id INT NOT NULL,
    action TEXT,
    created_at TIMESTAMPTZ
) PARTITION BY HASH (user_id);

CREATE TABLE user_logs_0 PARTITION OF user_logs
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE user_logs_1 PARTITION OF user_logs
    FOR VALUES WITH (MODULUS 4, REMAINDER 1);
```
