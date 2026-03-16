# MySQL 8 事务隔离级别实战指南

## 📋 目录

- [快速开始](#快速开始)
- [四种隔离级别详解](#四种隔离级别详解)
- [并发问题演示](#并发问题演示)
- [业务场景实战](#业务场景实战)
- [总结与选型建议](#总结与选型建议)

---

## 🚀 快速开始

### 1. 环境准备

```sql
-- 查看当前 MySQL 版本
SELECT VERSION();

-- 查看默认隔离级别
SELECT @@transaction_isolation;

-- 查看所有隔离级别配置
SHOW VARIABLES LIKE 'transaction_isolation';
```

### 2. 测试表准备

执行 `08-MySQL 事务隔离级别实战演示.sql` 文件中的建表语句。

---

## 📚 四种隔离级别详解

### 1️⃣ READ UNCOMMITTED (读未提交)

**特点**：最低的隔离级别，什么都不隔离

**核心问题**：脏读

#### 实验步骤

##### 第一步：会话 A - 读取初始数据

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 1000.00
```

##### 第二步：会话 B - 修改但不提交

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;
START TRANSACTION;
UPDATE test_account
SET balance = 800.00
WHERE id = 1;
-- 注意：不 COMMIT！
```

##### 第三步：会话 A - 读到脏数据

```sql
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 800.00 ❌（脏读！）
```

##### 第四步：会话 B - 回滚

```sql
ROLLBACK;
```

##### 第五步：会话 A - 数据又变了

```sql
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 1000.00（验证了刚才读到的是脏数据）
COMMIT;
```

#### 现象分析

```
时间线        会话 A                      会话 B
─────────────────────────────────────────────────────
T1           SELECT → 1000              
             (读到初始值)               
─────────────────────────────────────────────────────
T2                                      UPDATE → 800
                                          (未提交)
─────────────────────────────────────────────────────
T3           SELECT → 800   ❌         
             (脏读！读到未提交数据)      
─────────────────────────────────────────────────────
T4                                      ROLLBACK
─────────────────────────────────────────────────────
T5           SELECT → 1000             
             (数据恢复，证明之前是脏读)  
```

#### 核心结论

- ✅ **性能最高**：读操作不会阻塞写操作
- ❌ **脏读**：可能读到其他事务未提交的修改
- ❌ **不可重复读**：同一事务内多次读取结果不一致
- ❌ **幻读**：查询结果集数量可能变化

---

### 2️⃣ READ COMMITTED (读已提交)

**特点**：Oracle 默认隔离级别，解决脏读

**核心问题**：不可重复读、幻读

#### 实验 1：不可重复读

##### 第一步：会话 A - 第一次读取

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 1000.00
```

##### 第二步：会话 B - 修改并提交

```sql
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;
UPDATE test_account
SET balance = 700.00
WHERE id = 1;
COMMIT; -- 提交！
```

##### 第三步：会话 A - 第二次读取（不一样了！）

```sql
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 700.00 ❌（不可重复读！）
```

#### 实验 2：幻读演示

##### 第一步：会话 A - 查询所有账户

```sql
START TRANSACTION;
SELECT *
FROM test_account;
-- 结果：2 条记录
```

##### 第二步：会话 B - 插入新记录并提交

```sql
START TRANSACTION;
INSERT INTO test_account
VALUES (3, '王五', 1000.00);
COMMIT;
```

##### 第三步：会话 A - 再次查询（记录变多了！）

```sql
SELECT *
FROM test_account;
-- 结果：3 条记录 ❌（幻读！）
COMMIT;
```

#### MVCC 原理分析

在 RC 隔离级别下：

```
┌────────────────────────────────────────────────────┐
│ Read View 生成时机：每次 SELECT 都生成新的 Read View │
├────────────────────────────────────────────────────┤
│ 优点：能及时看到其他事务提交的修改                   │
│ 缺点：同一事务内多次读取结果不一致                   │
└────────────────────────────────────────────────────┘
```

**MVCC 可见性判断**：

```java
// 伪代码展示 RC 的 Read View 逻辑
for each SELECT
statement:
readView =

createNewReadView();  // 每次都新建
    
    if(trx_id<readView.min_trx_id){
        return true;  // 可见（已提交）
        }else if(trx_id >=readView.max_trx_id){
        return false; // 不可见（未开始）
        }else if(!readView.m_ids.

contains(trx_id)){
        return true;  // 可见（不在活跃列表中，已提交）
        }else{
        return false; // 不可见（在活跃列表中，未提交）
        }
```

#### 核心结论

- ✅ **解决脏读**：只能读到已提交的数据
- ❌ **不可重复读**：同一事务内多次读取结果可能不同
- ❌ **幻读**：查询结果集数量可能变化
- ⚠️ **适用场景**：对数据一致性要求不高的场景

---

### 3️⃣ REPEATABLE READ (可重复读)

**特点**：MySQL 默认隔离级别，解决不可重复读

**核心问题**：理论上存在幻读（但 InnoDB 基本解决）

#### 实验 1：可重复读验证

##### 第一步：会话 A - 第一次读取

```sql
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 1000.00
```

##### 第二步：会话 B - 修改并提交

```sql
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;
UPDATE test_account
SET balance = 600.00
WHERE id = 1;
COMMIT;
```

##### 第三步：会话 A - 第二次读取（还是一样！）

```sql
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 1000.00 ✅（可重复读！）
```

##### 第四步：会话 A - 提交后再读

```sql
COMMIT;

-- 重新开启事务
START TRANSACTION;
SELECT *
FROM test_account
WHERE id = 1;
-- 结果：balance = 600.00（现在能看到最新数据了）
```

#### 实验 2：转账场景对比

**RC 隔离级别的问题**：

```sql
-- 会话 A
START TRANSACTION;
SELECT SUM(balance) as total
FROM test_account;
-- 2000

-- 会话 B（此时提交）
START TRANSACTION;
UPDATE test_account
SET balance = balance - 200
WHERE id = 1;
UPDATE test_account
SET balance = balance + 200
WHERE id = 2;
COMMIT;

-- 会话 A（再次统计）
SELECT SUM(balance) as total
FROM test_account;
-- 结果：2000（但中间状态被看到了）
```

**RR 隔离级别的优势**：

```sql
-- 会话 A
START TRANSACTION;
SELECT SUM(balance) as total
FROM test_account;
-- 2000

-- 会话 B（提交）
START TRANSACTION;
UPDATE test_account
SET balance = balance - 300
WHERE id = 1;
UPDATE test_account
SET balance = balance + 300
WHERE id = 2;
COMMIT;

-- 会话 A（再次统计）
SELECT SUM(balance) as total
FROM test_account;
-- 结果：2000 ✅（整个事务内数据一致）
```

#### MVCC 原理分析

在 RR 隔离级别下：

```
┌────────────────────────────────────────────────────┐
│ Read View 生成时机：事务第一次 SELECT 时生成，后续复用  │
├────────────────────────────────────────────────────┤
│ 优点：保证事务内多次读取结果一致                    │
│ 缺点：长事务可能导致 Undo Log 过长                  │
└────────────────────────────────────────────────────┘
```

**MVCC 可见性判断**：

```java
// 伪代码展示 RR 的 Read View 逻辑
ReadView readView = null;

for
each transaction:
        if(readView ==null){
readView =

createReadView();  // 只创建一次！
    }

            // 使用同一个 readView 判断可见性
            if(

isVisible(readView, trx_id)){
        return currentVersion;
    }else{
            return historicalVersion;  // 读历史版本
    }
```

#### 幻读问题与 Next-Key Lock

InnoDB 通过以下机制解决幻读：

1. **快照读（普通 SELECT）**：MVCC 保证不可见
2. **当前读（SELECT ... FOR UPDATE）**：Next-Key Lock

```sql
-- 快照读：不会幻读
SELECT *
FROM test_account
WHERE id > 1;

-- 当前读：加 Next-Key Lock
SELECT *
FROM test_account
WHERE id > 1 FOR
UPDATE;
-- 锁定范围，阻止其他事务插入
```

**Next-Key Lock 示意图**：

```
假设有记录：id = 1, 5, 10

SELECT * FROM test_account WHERE id = 5 FOR UPDATE;

锁定区间：(1, 5]
├── 临键锁（Next-Key Lock）
│   ├── 间隙锁（Gap Lock）：(1, 5)
│   └── 行锁（Record Lock）：id=5

效果：
- 其他事务不能修改 id=5
- 其他事务不能在 (1,5) 区间插入
```

#### 核心结论

- ✅ **解决脏读**
- ✅ **解决不可重复读**
- ✅ **基本解决幻读**（MVCC + Next-Key Lock）
- ⚠️ **性能适中**：推荐使用
- 🎯 **MySQL 默认隔离级别**

---

### 4️⃣ SERIALIZABLE (串行化)

**特点**：最高的隔离级别，强制串行执行

**核心问题**：性能差

#### 实验演示

##### 第一步：会话 A - 查询

```sql
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
SELECT *
FROM test_account
WHERE id = 1;
-- 正常返回
```

##### 第二步：会话 B - 尝试更新（被阻塞！）

```sql
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;
UPDATE test_account
SET balance = 500.00
WHERE id = 1;
-- ⏳ 等待...（被阻塞）
```

##### 第三步：会话 A - 提交

```sql
COMMIT;
```

##### 第四步：会话 B - 继续执行

```sql
-- 现在可以执行了
COMMIT;
```

#### 核心结论

- ✅ **解决所有并发问题**
- ❌ **性能最差**：所有操作串行执行
- ❌ **大量锁竞争**：容易产生死锁
- ⚠️ **适用场景**：极少使用，除非对数据一致性要求极高

---

## 🔥 并发问题演示

### 1. 脏读（Dirty Read）

**定义**：读到其他事务未提交的修改

**演示**：READ UNCOMMITTED 隔离级别

```sql
-- 事务 A
START TRANSACTION;
SELECT *
FROM account
WHERE id = 1;
-- 1000

-- 事务 B
START TRANSACTION;
UPDATE account
SET balance = 800
WHERE id = 1;
-- 不提交

-- 事务 A
SELECT *
FROM account
WHERE id = 1;
-- 800 ❌（脏读）

-- 事务 B
ROLLBACK;

-- 事务 A
SELECT *
FROM account
WHERE id = 1; -- 1000（恢复原状）
```

### 2. 不可重复读（Non-Repeatable Read）

**定义**：同一事务内，多次读取同一数据结果不同

**演示**：READ COMMITTED 隔离级别

```sql
-- 事务 A
START TRANSACTION;
SELECT *
FROM account
WHERE id = 1;
-- 1000

-- 事务 B
START TRANSACTION;
UPDATE account
SET balance = 700
WHERE id = 1;
COMMIT;

-- 事务 A
SELECT *
FROM account
WHERE id = 1; -- 700 ❌（不可重复读）
```

### 3. 幻读（Phantom Read）

**定义**：同一事务内，多次查询结果集数量不同

**演示**：READ COMMITTED 隔离级别

```sql
-- 事务 A
START TRANSACTION;
SELECT COUNT(*)
FROM account;
-- 2 条

-- 事务 B
START TRANSACTION;
INSERT INTO account
VALUES (3, '王五', 1000);
COMMIT;

-- 事务 A
SELECT COUNT(*)
FROM account; -- 3 条 ❌（幻读）
```

---

## 💼 业务场景实战

### 场景 1：库存扣减（超卖问题）

#### 错误示范（应用层检查）

```sql
-- 会话 A
START TRANSACTION;
SELECT stock
FROM inventory
WHERE product_id = 1;
-- 10
-- 应用层判断：stock > 0，可以扣减
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1;
COMMIT;

-- 会话 B（并发）
START TRANSACTION;
SELECT stock
FROM inventory
WHERE product_id = 1;
-- 10
-- 应用层判断：stock > 0，可以扣减
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1;
COMMIT;

-- 结果：stock = 8（正确，因为 UPDATE 是原子操作）
```

#### 潜在问题（连续扣减）

```sql
-- 会话 A
START TRANSACTION;
SELECT stock
FROM inventory
WHERE product_id = 1;
-- 8
-- 循环扣减 8 次...
COMMIT;

-- 会话 B（基于缓存的 8）
START TRANSACTION;
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1;
COMMIT;

-- 结果：stock = -1 ❌（超卖！）
```

#### 正确做法（数据库层面控制）

```sql
-- 方案 1：乐观锁
UPDATE inventory
SET stock   = stock - 1,
    version = version + 1
WHERE product_id = 1
  AND stock > 0
  AND version = old_version;

-- 方案 2：条件扣减
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1
  AND stock - 1 >= 0;

-- 检查影响行数
-- 如果 affected_rows = 0，说明库存不足
```

### 场景 2：转账业务

#### RC vs RR 对比

```sql
-- RC 隔离级别
-- 事务 A
START TRANSACTION;
SELECT SUM(balance)
FROM account;
-- 2000

-- 事务 B（提交）
UPDATE account
SET balance = balance - 200
WHERE id = 1;
UPDATE account
SET balance = balance + 200
WHERE id = 2;
COMMIT;

-- 事务 A
SELECT SUM(balance)
FROM account;
-- 2000
-- 问题：虽然总额一致，但可能读到中间状态

-- RR 隔离级别
-- 事务 A
START TRANSACTION;
SELECT SUM(balance)
FROM account;
-- 2000

-- 事务 B（提交）
UPDATE account
SET balance = balance - 300
WHERE id = 1;
UPDATE account
SET balance = balance + 300
WHERE id = 2;
COMMIT;

-- 事务 A
SELECT SUM(balance)
FROM account;
-- 2000 ✅
-- 优势：整个事务内看到一致的数据视图
```

---

## 📊 总结与选型建议

### 隔离级别对比表

| 隔离级别             | 脏读 | 不可重复读 | 幻读 | 性能   | 适用场景            |
|------------------|----|-------|----|------|-----------------|
| READ UNCOMMITTED | ✓  | ✓     | ✓  | ⭐⭐⭐⭐ | 几乎不用            |
| READ COMMITTED   | ✗  | ✓     | ✓  | ⭐⭐⭐  | Oracle 默认，日志分析等 |
| REPEATABLE READ  | ✗  | ✗     | ~✓ | ⭐⭐   | **MySQL 默认，推荐** |
| SERIALIZABLE     | ✗  | ✗     | ✗  | ⭐    | 特殊场景            |

### 选型建议

#### 推荐使用：REPEATABLE READ（MySQL 默认）

**理由**：

1. ✅ 解决脏读、不可重复读
2. ✅ 基本解决幻读（MVCC + Next-Key Lock）
3. ✅ 性能适中，适合大多数业务场景
4. ✅ MySQL 深度优化

**适用场景**：

- 电商订单系统
- 金融转账系统
- 库存管理系统
- 大多数 OLTP 系统

#### 可选：READ COMMITTED

**理由**：

1. ✅ 更高的并发性能
2. ✅ 及时看到其他事务的修改
3. ❌ 需要处理不可重复读

**适用场景**：

- 日志记录系统
- 统计分析系统
- 对实时性要求高的场景

#### 避免使用：READ UNCOMMITTED 和 SERIALIZABLE

- **READ UNCOMMITTED**：数据一致性无法保证
- **SERIALIZABLE**：性能太差，除非必要

### 最佳实践

1. **默认使用 RR 隔离级别**
2. **合理设计事务粒度**：避免长事务
3. **使用合适的索引**：减少锁竞争
4. **乐观锁优先**：减少数据库锁开销
5. **监控慢查询**：及时发现性能问题

---

## 🔧 附录

### A. 常用命令

```sql
-- 查看当前隔离级别
SELECT @@transaction_isolation;

-- 设置会话隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 设置全局隔离级别
SET GLOBAL TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- 查看活跃事务
SELECT *
FROM information_schema.INNODB_TRX;

-- 查看锁等待
SELECT *
FROM performance_schema.data_lock_waits;

-- 终止事务
KILL <thread_id>;
```

### B. 查看 MVCC 相关信息

```sql
-- 查看 Undo Log 状态
SHOW ENGINE INNODB STATUS;

-- 查看事务信息
SELECT *
FROM information_schema.INNODB_TRX;

-- 查看锁信息
SELECT *
FROM information_schema.INNODB_LOCKS;
SELECT *
FROM information_schema.INNODB_LOCK_WAITS;
```

### C. 调试技巧

1. **开启两个 MySQL 客户端窗口**模拟并发
2. **使用 `START TRANSACTION`** 显式开启事务
3. **观察 `affected_rows`** 判断 SQL 执行情况
4. **使用 `SHOW PROCESSLIST`** 查看阻塞情况

---

## 📖 参考文档

- MySQL 8.0 官方文档：Transaction Isolation Levels
- 《高性能 MySQL》第 7 章：MySQL 高级特性
- 《MySQL 技术内幕：InnoDB 存储引擎》第 6 章：锁
