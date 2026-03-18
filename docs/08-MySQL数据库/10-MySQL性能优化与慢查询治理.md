# MySQL性能优化与慢查询治理

## 概述

SQL优化是高级Java工程师必备技能，涉及执行计划分析、索引优化策略、慢查询治理等实战能力。

---

## 一、执行计划分析（EXPLAIN）

### 1.1 EXPLAIN关键字段

| 字段                | 含义      | 优化建议                             |
|-------------------|---------|----------------------------------|
| **id**            | 查询序号    | id越大优先级越高                        |
| **select_type**   | 查询类型    | 关注SUBQUERY、DERIVED               |
| **table**         | 访问的表    | 确认是否预期表                          |
| **type**          | 访问类型    | 至少range，最好ref/const              |
| **possible_keys** | 可能使用的索引 | 确认是否有合适索引                        |
| **key**           | 实际使用的索引 | 关注是否使用预期索引                       |
| **rows**          | 扫描行数    | 越小越好                             |
| **Extra**         | 额外信息    | 避免Using filesort、Using temporary |

### 1.2 type字段详解

```
system > const > eq_ref > ref > range > index > ALL
(性能从好到差)
```

| type       | 说明          | 示例                            |
|------------|-------------|-------------------------------|
| **const**  | 主键或唯一索引等值查询 | `WHERE id = 1`                |
| **eq_ref** | 联表查询，主键关联   | `t1 JOIN t2 ON t1.id = t2.id` |
| **ref**    | 非唯一索引等值查询   | `WHERE name = 'Alice'`        |
| **range**  | 索引范围查询      | `WHERE id > 100`              |
| **index**  | 全索引扫描       | `SELECT count(*) FROM t`      |
| **ALL**    | 全表扫描        | 需要优化                          |

### 1.3 Extra字段解读

| Extra值                | 含义        | 优化方案                |
|-----------------------|-----------|---------------------|
| **Using index**       | 覆盖索引      | ✅ 好现象               |
| **Using where**       | 使用WHERE过滤 | 正常                  |
| **Using filesort**    | 文件排序      | 添加索引或优化排序           |
| **Using temporary**   | 使用临时表     | 优化GROUP BY/ORDER BY |
| **Using join buffer** | 使用连接缓存    | 关注大表关联              |

---

## 二、索引优化策略

### 2.1 索引设计原则

```
1. 最左前缀原则
   - 联合索引(a,b,c)，查询条件必须有a
   
2. 选择性高的列建索引
   - 区分度 = 不同值数量 / 总行数
   - 区分度 > 0.1 适合建索引
   
3. 避免冗余索引
   - (a) 和 (a,b) 保留后者即可
   
4. 控制索引数量
   - 单表索引不超过5个
   - 过多索引影响写入性能
```

### 2.2 索引优化案例

**案例1：联合索引优化ORDER BY**

```sql
-- 原SQL
SELECT * FROM orders 
WHERE user_id = 100 
ORDER BY create_time DESC 
LIMIT 10;

-- 优化前：Using filesort
-- 优化后索引：INDEX idx_user_time(user_id, create_time)
-- 优化后：Using index，无需排序
```

**案例2：覆盖索引优化**

```sql
-- 原SQL
SELECT user_id, order_no, amount FROM orders 
WHERE user_id = 100;

-- 优化前：回表查询
-- 优化后索引：INDEX idx_user_order(user_id, order_no, amount)
-- 优化后：覆盖索引，无需回表
```

---

## 三、慢查询治理

### 3.1 慢查询日志配置

```ini
[mysqld]
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow.log
long_query_time = 2    # 超过2秒记录
log_queries_not_using_indexes = 1  # 记录未使用索引的查询
```

### 3.2 慢查询分析工具

| 工具                  | 用途        | 命令                            |
|---------------------|-----------|-------------------------------|
| **mysqldumpslow**   | MySQL自带分析 | `mysqldumpslow -s t slow.log` |
| **pt-query-digest** | Percona工具 | `pt-query-digest slow.log`    |
| **MySQL Workbench** | 可视化分析     | 图形界面查看                        |

### 3.3 pt-query-digest输出解读

```
# 总体统计
# 300s user time, 10s system time, 15.45M rss
# 总共分析了 1000 个查询

# 查询排名
# Rank Query ID           Response time  Calls  R/Call  V/M   Item
# ==== ================== ============== ====== ======= ===== ==========
#    1 0x1234567890ABCDEF  5000.0000 50%    100   50.0000  0.00 SELECT orders
#    2 0xFEDCBA0987654321  2000.0000 20%    500    4.0000  0.01 SELECT users

# 详细分析
# Query 1: 0.50 QPS, 50.00x concurrency, ID 0x1234567890ABCDEF
# This item is included in the report because it matches --limit
# Scores: V/M = 0.00
# Time range: 2024-03-15 10:00:00 to 10:05:00
# Attribute    pct   total     min     max     avg     95%  stddev  median
# ============ === ======= ======= ======= ======= ======= ======= =======
# Count         10     100
# Exec time     50   5000s     40s     60s     50s     55s      5s     50s
# Lock time      5    100s      1s      2s      1s      1s      0s      1s
# Rows sent      1     100       1       1       1       1       0       1
# Rows examine  90  900000    9000    9000    9000    9000       0    9000
```

### 3.4 慢查询优化流程

```
1. 发现慢查询
   → 慢查询日志、监控告警、用户反馈
   
2. 分析执行计划
   → EXPLAIN查看type、key、rows、Extra
   
3. 定位问题
   → 是否走索引？扫描行数多少？是否文件排序？
   
4. 优化方案
   → 添加索引、改写SQL、优化表结构
   
5. 验证效果
   → 对比执行计划、压测验证
```

---

## 四、SQL改写优化

### 4.1 分页优化

```sql
-- 深分页问题（越往后越慢）
SELECT * FROM orders 
ORDER BY id DESC 
LIMIT 1000000, 10;

-- 优化方案1：游标分页（推荐）
SELECT * FROM orders 
WHERE id < last_seen_id 
ORDER BY id DESC 
LIMIT 10;

-- 优化方案2：延迟关联
SELECT o.* FROM orders o
JOIN (SELECT id FROM orders 
      ORDER BY id DESC 
      LIMIT 1000000, 10) tmp ON o.id = tmp.id;
```

### 4.2 大表JOIN优化

```sql
-- 问题：大表直接JOIN
SELECT * FROM big_table a 
JOIN big_table b ON a.user_id = b.user_id;

-- 优化：减少扫描数据量
SELECT * FROM big_table a 
JOIN (SELECT * FROM big_table 
      WHERE create_time > '2024-01-01') b 
ON a.user_id = b.user_id;
```

### 4.3 避免SELECT *

```sql
-- 不推荐
SELECT * FROM user WHERE id = 1;

-- 推荐：只查需要的列
SELECT id, name, email FROM user WHERE id = 1;
```

---

## 五、高频面试题

**问题 1：如何分析一条SQL的性能？**

**答：**

1. **使用EXPLAIN**查看执行计划
2. **关注type字段**：至少达到range级别
3. **关注key字段**：确认是否使用预期索引
4. **关注rows字段**：扫描行数是否过多
5. **关注Extra字段**：避免filesort和temporary
6. **使用SHOW PROFILE**分析执行耗时

---

**问题 2：什么情况下索引会失效？**

**答：**

| 场景       | 示例                               |
|----------|----------------------------------|
| 对索引列做运算  | `WHERE age + 1 = 18`             |
| 使用函数     | `WHERE YEAR(create_time) = 2024` |
| 隐式类型转换   | `WHERE user_id = '123'`（int类型）   |
| LIKE以%开头 | `WHERE name LIKE '%abc'`         |
| OR条件     | `WHERE a = 1 OR b = 2`（b无索引）     |
| 违背最左前缀   | `WHERE b = 2`（索引是a,b）            |

---

**问题 3：慢查询优化的思路是什么？**

**答：**

1. **开启慢查询日志**，收集慢SQL
2. **使用pt-query-digest分析**，找出最耗时的SQL
3. **EXPLAIN分析执行计划**，定位问题
4. **针对性优化**：
    - 无索引 → 添加索引
    - 索引失效 → 改写SQL
    - 数据量过大 → 分库分表
    - 锁竞争 → 优化事务
5. **验证优化效果**，持续监控

---

**问题 4：如何优化百万级数据的分页查询？**

**答：**

**方案对比：**

| 方案   | 优点   | 缺点     |
|------|------|--------|
| 游标分页 | 性能最好 | 无法跳页   |
| 延迟关联 | 可跳页  | 仍有一定开销 |
| 业务限制 | 简单有效 | 用户体验受限 |

**推荐方案（游标分页）：**

```sql
-- 首次查询
SELECT * FROM orders 
ORDER BY id DESC 
LIMIT 10;
-- 记录最后一条id = 1000

-- 下一页
SELECT * FROM orders 
WHERE id < 1000 
ORDER BY id DESC 
LIMIT 10;
```

---

## 六、相关代码示例

完整代码示例请参考：

- `interview-service/src/main/java/cn/itzixiao/interview/mysql/`
