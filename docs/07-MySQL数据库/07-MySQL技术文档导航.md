# MySQL 技术文档导航

## 📚 文档概览

本项目包含完整的 MySQL 核心技术文档体系，涵盖从基础到高级的所有关键知识点。

---

## 📖 文档列表

### 1. MySQL 字段类型与存储引擎

- **文件路径：** [`docs/MySQL 字段类型与存储引擎.md`](./MySQL 字段类型与存储引擎.md)
- **内容概要：**
    - 数值类型（TINYINT ~ BIGINT、FLOAT/DOUBLE/DECIMAL）
    - 字符串类型（CHAR/VARCHAR、TEXT 系列、BLOB）
    - 日期时间类型（DATE、TIME、DATETIME、TIMESTAMP、YEAR）
    - 四大存储引擎详解（InnoDB、MyISAM、Memory、Archive）
- **面试题数量：** 5 道
- **适合阶段：** 基础入门

### 2. MySQL 索引原理详解

- **文件路径：** [`docs/MySQL 索引原理详解.md`](./MySQL 索引原理详解.md)
- **内容概要：**
    - B+Tree 数据结构图解
    - 聚簇索引 vs 二级索引
    - 索引分类与创建管理
    - 索引失效 7 大场景
    - 索引优化 6 大策略
- **面试题数量：** 6 道
- **适合阶段：** 进阶提升

### 3. MySQL 事务与锁机制详解

- **文件路径：** [`docs/MySQL 事务与锁机制详解.md`](./MySQL 事务与锁机制详解.md)
- **内容概要：**
    - ACID 四大特性
    - 并发一致性问题（脏读、不可重复读、幻读）
    - 四种隔离级别对比
    - MVCC 多版本并发控制
    - 锁机制（表锁、行锁、死锁）
    - InnoDB 行锁算法
- **面试题数量：** 8 道
- **适合阶段：** 高级深入

### 4. MySQL 日志与性能优化详解

- **文件路径：** [`docs/MySQL 日志与性能优化详解.md`](./MySQL 日志与性能优化详解.md)
- **内容概要：**
    - Redo Log vs Binlog vs Undo Log
    - 两阶段提交
    - Slow Query Log
    - EXPLAIN 分析 SQL
    - SQL 语句优化技巧
    - 表结构优化策略
- **面试题数量：** 8 道
- **适合阶段：** 实战调优

---

## 💻 示例代码

### MySQLCorePrincipleDemo.java

- **文件路径：** `interview-service/src/main/java/cn/itzixiao/interview/mysql/MySQLCorePrincipleDemo.java`
- **代码规模：** 617 行
- **功能模块：**
    1. 字段类型演示
    2. 存储引擎对比
    3. 索引原理讲解
    4. 日志系统介绍
    5. 事务特性展示
    6. 锁机制演示
    7. 性能优化示例
    8. 高频面试题汇总

**运行方式：**

```bash
# 编译项目
mvn clean compile -pl interview-service -am

# 运行示例
java cn.itzixiao.interview.mysql.MySQLCorePrincipleDemo
```

---

## 📊 知识体系图谱

```
MySQL 核心知识体系
├── 基础篇
│   ├── 字段类型
│   │   ├── 数值类型
│   │   ├── 字符串类型
│   │   └── 日期时间类型
│   └── 存储引擎
│       ├── InnoDB（默认）
│       ├── MyISAM
│       ├── Memory
│       └── Archive
│
├── 进阶篇
│   ├── 索引原理
│   │   ├── B+Tree 结构
│   │   ├── 聚簇索引 vs 二级索引
│   │   ├── 索引分类
│   │   └── 索引优化
│   │
│   ├── 事务特性
│   │   ├── ACID
│   │   ├── 并发问题
│   │   ├── 隔离级别
│   │   └── MVCC
│   │
│   └── 锁机制
│       ├── 锁粒度
│       ├── 行锁算法
│       ├── 共享锁 vs 排他锁
│       └── 死锁处理
│
└── 高级篇
    ├── 日志系统
    │   ├── Redo Log
    │   ├── Undo Log
    │   ├── Binlog
    │   └── 两阶段提交
    │
    └── 性能优化
        ├── EXPLAIN 分析
        ├── 索引优化
        ├── SQL 优化
        ├── 表结构优化
        └── 配置优化
```

---

## 🎯 学习路径推荐

### 初级工程师

1. ✅ 字段类型与存储引擎
2. ✅ 索引基础（创建、使用）
3. ✅ 简单 SQL 编写

### 中级工程师

1. ✅ 索引原理与优化
2. ✅ 事务基础（ACID、隔离级别）
3. ✅ SQL 性能分析（EXPLAIN）
4. ✅ 常见锁机制

### 高级工程师

1. ✅ MVCC 实现原理
2. ✅ InnoDB 行锁算法
3. ✅ 日志系统（Redo/Bin/Undo）
4. ✅ 性能调优实战
5. ✅ 死锁分析与预防

### 架构师

1. ✅ 主从复制原理
2. ✅ 读写分离方案
3. ✅ 分库分表策略
4. ✅ 高可用架构

---

## 🔥 高频面试题统计

| 文档        | 面试题数量    | 重要程度  |
|-----------|----------|-------|
| 字段类型与存储引擎 | 5 道      | ⭐⭐⭐   |
| 索引原理详解    | 6 道      | ⭐⭐⭐⭐⭐ |
| 事务与锁机制    | 8 道      | ⭐⭐⭐⭐⭐ |
| 日志与优化     | 8 道      | ⭐⭐⭐⭐⭐ |
| **总计**    | **27 道** | -     |

**最高频考点：**

1. InnoDB vs MyISAM
2. B+Tree 索引结构
3. 聚簇索引 vs 二级索引
4. 最左匹配原则
5. 覆盖索引
6. 索引失效场景
7. ACID 保证
8. 并发一致性问题
9. 隔离级别选择
10. MVCC 实现
11. 行锁算法
12. Redo Log vs Binlog
13. 两阶段提交
14. EXPLAIN 分析
15. SQL 优化技巧

---

## 📝 配套资源

### 官方文档

- [MySQL 8.0 Reference Manual](https://dev.mysql.com/doc/refman/8.0/en/)
- [InnoDB Storage Engine](https://dev.mysql.com/doc/refman/8.0/en/innodb-storage-engine.html)

### 实践工具

- **MySQL Workbench**：可视化数据库管理工具
- **Percona Toolkit**：高性能运维工具集
- **pt-query-digest**：慢查询分析工具
- **mysqltuner.pl**：性能调优建议脚本

### 推荐书籍

- 《高性能 MySQL》
- 《MySQL 技术内幕：InnoDB 存储引擎》
- 《深入理解 MySQL 主从复制》

---

## 🚀 快速开始

### 1. 查看字段类型

```bash
# 阅读文档
cat docs/MySQL 字段类型与存储引擎.md

# 运行示例
java cn.itzixiao.interview.mysql.MySQLCorePrincipleDemo
```

### 2. 学习索引原理

```bash
# 阅读文档
cat docs/MySQL 索引原理详解.md

# 实践索引创建
mysql> CREATE INDEX idx_name ON users(name);
mysql> EXPLAIN SELECT * FROM users WHERE name = 'Tom';
```

### 3. 理解事务与锁

```bash
# 阅读文档
cat docs/MySQL 事务与锁机制详解.md

# 模拟事务
mysql> BEGIN;
mysql> UPDATE account SET balance = balance - 100 WHERE id = 1;
mysql> UPDATE account SET balance = balance + 100 WHERE id = 2;
mysql> COMMIT;
```

### 4. 性能优化实战

```bash
# 开启慢查询日志
mysql> SET GLOBAL slow_query_log = 'ON';
mysql> SET GLOBAL long_query_time = 2;

# 分析慢 SQL
mysql> EXPLAIN SELECT * FROM users ORDER BY create_time;
```

---

## 📈 持续更新

本文档将持续更新，计划增加以下内容：

- [ ] SQL 实战案例精选
- [ ] 主从复制详解
- [ ] 分库分表方案
- [ ] 性能监控体系
- [ ] 故障排查手册

---

## 💡 使用建议

1. **循序渐进**：按学习路径逐步深入，不要跳跃
2. **理论结合实践**：边学边练，动手实验
3. **画图理解**：B+Tree、MVCC 等复杂概念多画图
4. **面试导向**：重点掌握 27 道高频面试题
5. **定期复习**：周期性回顾，加深记忆

---

**最后更新：** 2026-03-07  
**维护者：** itzixiao  
**联系方式：** 欢迎提 Issue 或 PR
