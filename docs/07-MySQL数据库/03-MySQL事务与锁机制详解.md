# MySQL 事务与锁机制详解

## 一、事务 ACID 特性

### 1.1 原子性（Atomicity）

**定义：** 事务是不可分割的最小单位，要么全部成功，要么全部失败

**实现机制：** Undo Log（回滚日志）

**示例：**

```sql
BEGIN;
  UPDATE account SET balance = balance - 100 WHERE id = 1;
  UPDATE account SET balance = balance + 100 WHERE id = 2;
-- 如果任意一条失败，整个事务回滚
COMMIT;
```

### 1.2 一致性（Consistency）

**定义：** 事务前后数据保持一致

**包括：**

- **约束一致**：主键、外键、唯一等约束不被破坏
- **业务一致**：如转账总额不变

**示例：**

```sql
-- 转账前：A(500) + B(500) = 1000
BEGIN;
  UPDATE account SET balance = balance - 100 WHERE id = A;
  UPDATE account SET balance = balance + 100 WHERE id = B;
COMMIT;
-- 转账后：A(400) + B(600) = 1000
```

### 1.3 隔离性（Isolation）

**定义：** 多个事务互不干扰

**实现机制：** 锁 + MVCC

**不同隔离级别解决不同并发问题**

### 1.4 持久性（Durability）

**定义：** 事务一旦提交，永久生效，即使宕机也能恢复

**实现机制：** Redo Log（重做日志）

---

## 二、并发一致性问题

### 2.1 脏读（Dirty Read）

**定义：** 读到其他事务**未提交**的数据

**示例：**

```
时间    事务 A                事务 B
T1     BEGIN
T2             UPDATE users SET age = 18 WHERE id = 1;
T3     SELECT age FROM users WHERE id = 1;  -- 读到 18（脏读）
T4             ROLLBACK;  -- B 回滚，age 实际还是 20
-- A 读到的是错误数据
```

**解决：** READ COMMITTED 及以上级别

### 2.2 不可重复读（Non-Repeatable Read）

**定义：** 同一事务内，多次读取结果不一致

**原因：** 其他事务**修改并提交**

**示例：**

```
时间    事务 A                事务 B
T1     BEGIN                 BEGIN
T2     SELECT age=20
T3                             UPDATE users SET age=18;
T4                             COMMIT;
T5     SELECT age=18  -- 两次读取不一致
```

**解决：** REPEATABLE READ 及以上级别

### 2.3 幻读（Phantom Read）

**定义：** 同一事务内，多次查询**记录数**不一致

**原因：** 其他事务**插入或删除**

**示例：**

```
时间    事务 A                事务 B
T1     BEGIN                 BEGIN
T2     SELECT COUNT(*)=10
T3                             INSERT INTO users VALUES(...);
T4                             COMMIT;
T5     SELECT COUNT(*)=11  -- 出现"幻觉"
```

**解决：** SERIALIZABLE 级别或 Next-Key Lock

---

## 三、四种隔离级别

### 3.1 隔离级别对比

| 隔离级别             | 脏读   | 不可重复读 | 幻读     | 性能 |
|------------------|------|-------|--------|----|
| READ UNCOMMITTED | ✓ 可能 | ✓ 可能  | ✓ 可能   | 最高 |
| READ COMMITTED   | ✗ 避免 | ✓ 可能  | ✓ 可能   | 高  |
| REPEATABLE READ  | ✗ 避免 | ✗ 避免  | △ 基本避免 | 中  |
| SERIALIZABLE     | ✗ 避免 | ✗ 避免  | ✗ 避免   | 最低 |

> 注：MySQL 的 REPEATABLE READ 通过 MVCC+Next-Key Lock 基本避免幻读

### 3.2 查看和设置隔离级别

```sql
-- 查看当前会话隔离级别
SELECT @@transaction_isolation;

-- 查看全局隔离级别
SELECT @@global.transaction_isolation;

-- 设置会话隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;

-- 设置全局隔离级别
SET GLOBAL TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

### 3.3 各数据库默认隔离级别

| 数据库        | 默认隔离级别          |
|------------|-----------------|
| MySQL      | REPEATABLE READ |
| Oracle     | READ COMMITTED  |
| SQL Server | READ COMMITTED  |
| PostgreSQL | READ COMMITTED  |

---

## 四、MVCC（多版本并发控制）

### 4.1 MVCC 核心概念

**定义：** Multi-Version Concurrency Control，实现非阻塞读

**优势：**

- 读写不冲突（读不加锁）
- 提升并发性能

### 4.2 实现原理

**三个隐藏列：**

1. **DB_TRX_ID**：最近修改事务 ID
2. **DB_ROLL_PTR**：回滚指针（指向 Undo Log）
3. **DB_ROW_ID**：行 ID（没有主键时使用）

**版本链：** 通过 Undo Log 串联历史版本

### 4.3 Read View（读视图）

**作用：** 判断数据版本对当前事务是否可见

**规则：**

1. 未提交的事务，不可见
2. 已提交的事务：
    - **RC 级别**：每次 SELECT 都生成新 Read View
    - **RR 级别**：第一次 SELECT 生成 Read View，后续复用

### 4.4 可见性判断

```
假设当前事务 ID=100，Read View 中活跃事务列表=[98, 99]

版本 1: trx_id=95  → 已提交，可见
版本 2: trx_id=98  → 未提交，不可见，找上一个版本
版本 3: trx_id=90  → 已提交，可见
```

---

## 五、锁机制

### 5.1 锁的粒度分类

#### 表级锁（Table-Level Lock）

- 锁定整张表，开销小
- 容易冲突，并发度低
- MyISAM 使用

#### 行级锁（Row-Level Lock）

- 只锁定某一行，开销大
- 不易冲突，并发度高
- InnoDB 使用

#### 页级锁（Page-Level Lock）

- 锁定一页数据（介于表和行之间）
- Berkeley DB 使用

### 5.2 InnoDB 行锁算法

#### Record Lock（记录锁）

- 锁定单条记录
- 基于索引实现（必须走索引）

```sql
-- 自动加锁（更新操作）
UPDATE users SET age = 18 WHERE id = 1;

-- 手动加锁
SELECT * FROM users WHERE id = 1 FOR UPDATE;
```

#### Gap Lock（间隙锁）

- 锁定记录之间的间隙
- 防止其他事务插入
- 解决幻读问题

```sql
-- 锁定 (10, 20) 之间的间隙
SELECT * FROM users WHERE id > 10 AND id < 20 FOR UPDATE;
```

#### Next-Key Lock（临键锁）

- Record Lock + Gap Lock
- 锁定记录 + 间隙
- RR 级别下默认使用

### 5.3 共享锁 vs 排他锁

#### 共享锁（S 锁，读锁）

- 允许其他事务加 S 锁
- 不允许加 X 锁
- `SELECT ... LOCK IN SHARE MODE`

```sql
-- 加共享锁
SELECT * FROM users WHERE id = 1 LOCK IN SHARE MODE;

-- 其他事务可以读
SELECT * FROM users WHERE id = 1;  -- ✅

-- 其他事务不能写
UPDATE users SET age = 18 WHERE id = 1;  -- ❌ 阻塞
```

#### 排他锁（X 锁，写锁）

- 不允许其他事务加任何锁
- INSERT、UPDATE、DELETE 自动加 X 锁
- `SELECT ... FOR UPDATE`

```sql
-- 加排他锁
SELECT * FROM users WHERE id = 1 FOR UPDATE;

-- 其他事务不能读也不能写
SELECT * FROM users WHERE id = 1 FOR UPDATE;  -- ❌ 阻塞
UPDATE users SET age = 18 WHERE id = 1;  -- ❌ 阻塞
```

### 5.4 锁兼容性矩阵

|     | S 锁 | X 锁 |
|-----|-----|-----|
| S 锁 | ✓   | ✗   |
| X 锁 | ✗   | ✗   |

---

## 六、死锁

### 6.1 死锁产生条件

1. 两个事务互相持有对方需要的锁
2. 形成循环等待

**示例：**

```
时间    事务 A                事务 B
T1     UPDATE users WHERE id=1;  -- 持有 id=1 的 X 锁
T2                             UPDATE users WHERE id=2;  -- 持有 id=2 的 X 锁
T3     UPDATE users WHERE id=2;  -- 等待 id=2 的锁
T4                             UPDATE users WHERE id=1;  -- 等待 id=1 的锁
-- 死锁！
```

### 6.2 死锁解决方案

**1. 超时**

```sql
-- 设置锁等待超时（秒）
SET innodb_lock_wait_timeout = 50;
```

**2. 主动回滚**

- 选择代价小的事务回滚

**3. 预防**

- 按固定顺序访问资源

### 6.3 查看死锁信息

```sql
-- 查看死锁信息
SHOW ENGINE INNODB STATUS;

-- 查看运行中的事务
SELECT * FROM information_schema.INNODB_TRX;

-- 查看锁信息
SELECT * FROM information_schema.INNODB_LOCKS;

-- 查看等待锁的事务
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
```

---

## 七、高频面试题

**问题 1:MySQL 如何保证事务的 ACID？**

**答：**

- **原子性**：Undo Log 保证，失败可回滚
- **一致性**：通过原子性、隔离性、持久性共同保证
- **隔离性**：锁 + MVCC 实现不同隔离级别
- **持久性**：Redo Log 保证，提交后永久保存

**问题 2：脏读、不可重复读、幻读的区别？**

**答：**

- **脏读**：读到**未提交**的数据
- **不可重复读**：同一事务内，多次读取**值不一致**（被修改）
- **幻读**：同一事务内，多次查询**记录数不一致**（被插入/删除）

**问题 3:MySQL 的默认隔离级别是什么？为什么？**

**答：**

**默认级别：** REPEATABLE READ

**原因：**

1. 避免了脏读和不可重复读
2. 通过 MVCC+Next-Key Lock 基本避免幻读
3. 性能较好（相比 SERIALIZABLE）

**问题 4：什么是 MVCC？如何实现？**

**答：**

**MVCC：** 多版本并发控制，实现非阻塞读

**实现原理：**

1. **隐藏列**：事务 ID、回滚指针、删除标志
2. **Undo Log**：构建历史版本链
3. **Read View**：判断版本可见性

**优势：** 读写不冲突，提升并发性能

**问题 5:InnoDB 的行锁算法有哪些？**

**答：**

1. **Record Lock**：锁定记录（基于索引）
2. **Gap Lock**：锁定间隙（防止插入）
3. **Next-Key Lock**：Record + Gap（RR 级别默认）

**问题 6：共享锁和排他锁的区别？**

**答：**

| 对比项  | 共享锁（S）             | 排他锁（X）           |
|------|--------------------|------------------|
| 别名   | 读锁                 | 写锁               |
| 兼容性  | 可与 S 锁共存           | 不与任何锁共存          |
| 获取方式 | LOCK IN SHARE MODE | FOR UPDATE / DML |
| 权限   | 可读不可写              | 可读可写             |

**问题 7：什么是死锁？如何解决？**

**答：**

**死锁：** 两个事务互相持有对方需要的锁，形成循环等待

**解决方案：**

1. **超时**：`innodb_lock_wait_timeout`
2. **主动回滚**：选择代价小的事务
3. **预防**：按固定顺序访问资源

**问题 8:RC 和 RR 级别的区别？**

**答：**

| 对比项       | RC（读已提交）          | RR（可重复读）         |
|-----------|-------------------|------------------|
| 脏读        | 避免                | 避免               |
| 不可重复读     | 可能                | 避免               |
| 幻读        | 可能                | 基本避免             |
| Read View | 每次 SELECT 生成      | 第一次 SELECT 生成后复用 |
| 默认级别      | Oracle/SQL Server | MySQL            |

---

## 八、最佳实践

### 8.1 事务使用建议

1. **保持事务短小**：快速提交，避免长事务
2. **避免大事务**：拆分为多个小事务
3. **选择合适的隔离级别**：不要盲目提高
4. **注意隐式提交**：DDL 语句会自动提交

### 8.2 锁使用建议

1. **尽量使用行级锁**：减少锁冲突
2. **避免死锁**：按固定顺序访问资源
3. **及时释放锁**：事务结束立即释放
4. **监控锁等待**：定期检查锁状态
