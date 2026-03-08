# MySQL 索引原理详解

## 一、索引数据结构

### 1.1 B+Tree（InnoDB 使用）

```
         根节点（索引项）
        /    |    \
   内部节点  内部节点  内部节点（只存索引）
     / \     / \     / \
  叶子 叶子 叶子 叶子 叶子 叶子（存数据 + 索引，双向链表连接）
```

**B+Tree 特点：**
1. **非叶子节点只存索引**：不存数据，容纳更多索引项
2. **所有数据在叶子节点**：查询稳定 O(log N)
3. **叶子节点链表连接**：适合范围查询和排序
4. **树高度低**：通常 3-4 层，减少 IO 次数

### 1.2 B+Tree vs B-Tree

| 特性 | B-Tree | B+Tree |
|------|--------|--------|
| 数据存储 | 所有节点都存 | 只在叶子节点 |
| 查询稳定性 | 不稳定 | 稳定 O(log N) |
| 范围查询 | 需中序遍历 | 直接链表遍历 |
| 磁盘 IO | 较多 | 较少 |

---

## 二、聚簇索引 vs 二级索引

### 2.1 聚簇索引（主键索引）

**特点：**
- 叶子节点存储**完整数据行**
- 一张表只有**一个**聚簇索引
- InnoDB 的**主键**就是聚簇索引

**示例：**
```sql
CREATE TABLE users (
    id INT PRIMARY KEY,  -- 聚簇索引
    name VARCHAR(50),
    email VARCHAR(100)
);
```

### 2.2 二级索引（辅助索引）

**特点：**
- 叶子节点存储**主键值 + 索引列**
- 可以有**多个**二级索引
- 需要**回表查询**（先查主键，再查聚簇索引）

**示例：**
```sql
CREATE INDEX idx_email ON users(email);
-- 叶子节点存储：email + id（主键）
```

### 2.3 覆盖索引

**定义：** 二级索引包含所有查询字段，无需回表

**示例：**
```sql
-- 创建联合索引
CREATE INDEX idx_name_age ON users(name, age);

-- 覆盖索引查询（不需要回表）
SELECT id, name, age FROM users WHERE name = 'Tom' AND age > 18;
```

---

## 三、索引分类

### 3.1 按数据结构分

| 类型 | 说明 | 适用场景 |
|------|------|----------|
| B+Tree 索引 | 最常用 | 等值、范围查询 |
| Hash 索引 | 等值查询 O(1) | Memory 引擎 |
| Full-Text 全文索引 | 文本搜索 | MyISAM/InnoDB |

### 3.2 按物理存储分

| 类型 | 说明 | 数量限制 |
|------|------|----------|
| 聚簇索引 | 数据与索引在一起 | 1 个 |
| 二级索引 | 独立的索引结构 | 多个 |

### 3.3 按字段数量分

| 类型 | 说明 | 注意事项 |
|------|------|----------|
| 单列索引 | 一个字段 | 简单高效 |
| 联合索引 | 多个字段 | **最左匹配原则** |

### 3.4 按唯一性分

| 类型 | 说明 | 约束 |
|------|------|------|
| 唯一索引 | 不允许重复 | UNIQUE |
| 普通索引 | 允许重复 | - |

---

## 四、索引创建与管理

### 4.1 创建索引

```sql
-- 创建主键索引（自动创建聚簇索引）
ALTER TABLE users ADD PRIMARY KEY (id);

-- 创建唯一索引
CREATE UNIQUE INDEX idx_email ON users(email);

-- 创建普通索引
CREATE INDEX idx_name ON users(name);

-- 创建联合索引（最左匹配原则）
CREATE INDEX idx_name_age ON users(name, age);

-- 前缀索引（针对长字符串）
CREATE INDEX idx_description ON users(description(50));
```

### 4.2 删除索引

```sql
-- 删除索引
DROP INDEX idx_name ON users;

-- 删除主键
ALTER TABLE users DROP PRIMARY KEY;
```

### 4.3 查看索引

```sql
-- 查看表的索引
SHOW INDEX FROM users;

-- 查看索引使用情况
EXPLAIN SELECT * FROM users WHERE name = 'Tom';
```

---

## 五、索引失效场景

### 5.1 常见失效情况

**1. 模糊查询以%开头**
```sql
-- ❌ 索引失效
SELECT * FROM users WHERE name LIKE '%Tom';

-- ✅ 索引有效
SELECT * FROM users WHERE name LIKE 'Tom%';
```

**2. 函数操作**
```sql
-- ❌ 索引失效
SELECT * FROM users WHERE YEAR(create_time) = 2024;

-- ✅ 索引有效
SELECT * FROM users WHERE create_time >= '2024-01-01';
```

**3. 类型转换**
```sql
-- ❌ 索引失效（phone 是字符串）
SELECT * FROM users WHERE phone = 13800138000;

-- ✅ 索引有效
SELECT * FROM users WHERE phone = '13800138000';
```

**4. OR 连接条件**
```sql
-- ❌ 索引失效（name 无索引）
SELECT * FROM users WHERE email = 'test@example.com' OR name = 'Tom';

-- ✅ 索引有效
SELECT * FROM users WHERE email = 'test@example.com' OR email = 'test2@example.com';
```

**5. 违反最左匹配原则**
```sql
-- 联合索引 idx_name_age (name, age)

-- ❌ 索引失效
SELECT * FROM users WHERE age = 18;

-- ✅ 索引有效
SELECT * FROM users WHERE name = 'Tom' AND age = 18;
```

**6. IS NULL/IS NOT NULL**
```sql
-- ❌ 可能失效
SELECT * FROM users WHERE name IS NULL;

-- ✅ 建议
ALTER TABLE users MODIFY name VARCHAR(50) NOT NULL DEFAULT '';
```

**7. != 或 <>**
```sql
-- ❌ 可能全表扫描
SELECT * FROM users WHERE status != 1;

-- ✅ 建议
SELECT * FROM users WHERE status = 0 OR status = 2;
```

---

## 六、索引优化策略

### 6.1 创建索引的原则

1. **为 WHERE、ORDER BY、GROUP BY 创建索引**
2. **使用覆盖索引**（避免回表）
3. **联合索引遵循最左匹配原则**
4. **避免索引失效**（如函数操作、类型转换）
5. **前缀索引**（针对长字符串）
6. **定期清理无用索引**

### 6.2 索引设计技巧

**1. 选择区分度高的字段**
```sql
-- ✅ 好索引（区分度高）
CREATE INDEX idx_email ON users(email);

-- ❌ 差索引（区分度低）
CREATE INDEX idx_gender ON users(gender);  -- 只有男/女
```

**2. 利用覆盖索引**
```sql
-- 创建联合索引
CREATE INDEX idx_name_age ON users(name, age);

-- 覆盖索引查询
SELECT id, name, age FROM users WHERE name = 'Tom';
```

**3. 扩展已有索引**
```sql
-- 已有索引
CREATE INDEX idx_name ON users(name);

-- 扩展为联合索引（删除原索引）
CREATE INDEX idx_name_age ON users(name, age);
```

---

## 七、高频面试题

**问题 1：为什么 InnoDB 使用 B+Tree 作为索引？**

**答：**

1. **非叶子节点只存索引**：可以容纳更多索引项，树更矮胖
2. **查询性能稳定**：所有查询都要走到叶子节点，稳定 O(log N)
3. **适合范围查询**：叶子节点用链表连接，直接遍历即可
4. **减少 IO 次数**：树高度通常 3-4 层，IO 次数少

**对比：**
- 二叉树：树太高，IO 次数多
- B-Tree：范围查询需中序遍历
- Hash：只支持等值查询，不支持范围查询

**问题 2：聚簇索引和二级索引的区别？**

**答：**

| 对比项 | 聚簇索引 | 二级索引 |
|--------|----------|----------|
| 存储内容 | 完整数据行 | 主键值 + 索引列 |
| 数量限制 | 1 个 | 多个 |
| 查询方式 | 直接获取数据 | 需要回表 |
| 适用场景 | 主键查询 | 条件查询 |

**回表：** 通过二级索引找到主键后，再到聚簇索引查询完整数据

**问题 3：什么是最左匹配原则？**

**答：**

- 联合索引 `(a, b, c)`，查询时必须从**最左开始**匹配
- 例如：`WHERE a=1 AND b=2` 可以用索引
- `WHERE b=2` 或 `WHERE c=3` **无法使用索引**

**示例：**
```sql
-- 联合索引 idx_name_age_city (name, age, city)

-- ✅ 可以使用索引
WHERE name = 'Tom' AND age = 18 AND city = 'Beijing';
WHERE name = 'Tom' AND age = 18;
WHERE name = 'Tom';

-- ❌ 无法使用索引
WHERE age = 18 AND city = 'Beijing';
WHERE city = 'Beijing';
```

**问题 4：什么是覆盖索引？有什么优势？**

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

**问题 5：索引失效的常见场景有哪些？**

**答：**

1. **模糊查询以%开头**：`LIKE '%abc'`
2. **函数操作**：`YEAR(create_time)`
3. **类型转换**：字符串字段用数字查询
4. **OR 连接**：有一边没索引
5. **违反最左匹配**：跳过左边字段
6. **IS NULL/IS NOT NULL**：可能失效
7. **!= 或 <>**：可能全表扫描

**问题 6：如何优化慢查询？**

**答：**

1. **开启慢查询日志**：定位慢 SQL
2. **使用 EXPLAIN 分析**：查看执行计划
3. **添加合适索引**：覆盖查询条件
4. **避免索引失效**：优化 SQL 写法
5. **使用覆盖索引**：避免回表
6. **优化表结构**：垂直拆分、水平分区

---

## 八、最佳实践

### 8.1 索引设计原则

1. **不要过度索引**：影响写性能
2. **及时清理无用索引**：定期分析
3. **优先扩展现有索引**：而不是新建
4. **考虑写入频率**：频繁更新的表少建索引

### 8.2 索引维护

```sql
-- 查看索引使用情况
SHOW INDEX FROM users;

-- 分析表
ANALYZE TABLE users;

-- 检查未使用的索引
SELECT * FROM sys.schema_unused_indexes;
```
