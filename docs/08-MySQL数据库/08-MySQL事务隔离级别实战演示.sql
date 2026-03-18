-- ================================================================
-- MySQL 8 事务隔离级别实战演示
-- ================================================================
-- 用途：通过实际操作体验四种事务隔离级别的特性
-- 环境：MySQL 8.0+
-- ================================================================

-- ================================================================
-- 准备工作：创建测试表和数据
-- ================================================================
DROP TABLE IF EXISTS test_account;
CREATE TABLE test_account
(
    id      INT PRIMARY KEY,
    name    VARCHAR(50),
    balance DECIMAL(10, 2)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

-- 插入初始数据
INSERT INTO test_account
VALUES (1, '张三', 1000.00);
INSERT INTO test_account
VALUES (2, '李四', 1000.00);

-- 查看当前隔离级别
SELECT @@transaction_isolation;

-- ================================================================
-- 1. READ UNCOMMITTED (读未提交)
-- 问题：脏读、不可重复读、幻读
-- ================================================================

-- ============ 会话 A ============
-- 设置隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

-- 开启事务
START TRANSACTION;

-- 第一次查询：读取张三余额
SELECT *
FROM test_account
WHERE id = 1;
-- 预期结果：balance = 1000.00

-- ============ 会话 B ============
-- 设置隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

-- 开启事务
START TRANSACTION;

-- 更新张三余额（但不提交）
UPDATE test_account
SET balance = 800.00
WHERE id = 1;
-- 注意：此时不 COMMIT

-- ============ 会话 A ============
-- 第二次查询：再次读取张三余额
SELECT *
FROM test_account
WHERE id = 1;
-- 【现象】读到 800.00（脏读！读到未提交的数据）

-- ============ 会话 B ============
-- 回滚事务
ROLLBACK;

-- ============ 会话 A ============
-- 第三次查询：又变回 1000.00（验证了脏读）
SELECT *
FROM test_account
WHERE id = 1;

-- 提交事务
COMMIT;


-- ================================================================
-- 2. READ COMMITTED (读已提交) - Oracle 默认隔离级别
-- 问题：不可重复读、幻读
-- 解决：脏读
-- ================================================================

-- ============ 会话 A ============
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

-- 第一次查询
SELECT *
FROM test_account
WHERE id = 1;
-- 预期结果：balance = 1000.00

-- ============ 会话 B ============
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

-- 修改并提交
UPDATE test_account
SET balance = 700.00
WHERE id = 1;
COMMIT;

-- ============ 会话 A ============
-- 第二次查询
SELECT *
FROM test_account
WHERE id = 1;
-- 【现象】读到 700.00（不可重复读！同一个事务内两次读不一样）

-- 继续演示幻读问题
-- ============ 会话 A ============
-- 查询所有账户
SELECT *
FROM test_account;
-- 假设有 2 条记录

-- ============ 会话 B ============
START TRANSACTION;
INSERT INTO test_account
VALUES (3, '王五', 1000.00);
COMMIT;

-- ============ 会话 A ============
-- 再次查询所有账户
SELECT *
FROM test_account;
-- 【现象】读到 3 条记录（幻读！同一个查询条件，结果集不一致）

COMMIT;


-- ================================================================
-- 3. REPEATABLE READ (可重复读) - MySQL 默认隔离级别
-- 问题：可能产生幻读（但 InnoDB 通过 MVCC+Next-Key Lock 基本解决）
-- 解决：脏读、不可重复读
-- ================================================================

-- ============ 会话 A ============
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- 第一次查询
SELECT *
FROM test_account
WHERE id = 1;
-- 预期结果：balance = 1000.00

-- ============ 会话 B ============
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- 修改并提交
UPDATE test_account
SET balance = 600.00
WHERE id = 1;
COMMIT;

-- ============ 会话 A ============
-- 第二次查询
SELECT *
FROM test_account
WHERE id = 1;
-- 【现象】仍然读到 1000.00（可重复读！解决了不可重复读）

-- 提交后能看到最新数据
COMMIT;

-- 验证可重复读
START TRANSACTION;
SELECT *
FROM test_account
WHERE id = 1; -- 600.00
COMMIT;


-- ================================================================
-- 4. SERIALIZABLE (串行化)
-- 解决：所有并发问题，但性能最差
-- ================================================================

-- ============ 会话 A ============
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;

-- 查询
SELECT *
FROM test_account
WHERE id = 1;
-- 正常返回

-- ============ 会话 B ============
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;
START TRANSACTION;

-- 尝试更新（会被阻塞，直到会话 A 提交或回滚）
UPDATE test_account
SET balance = 500.00
WHERE id = 1;
-- 【现象】等待锁释放...

-- ============ 会话 A ============
-- 提交事务
COMMIT;

-- ============ 会话 B ============
-- 现在可以执行了
-- 然后提交
COMMIT;


-- ================================================================
-- 综合对比实验：转账场景
-- ================================================================

-- 重置数据
UPDATE test_account
SET balance = 1000.00
WHERE id IN (1, 2);
DELETE
FROM test_account
WHERE id = 3;

-- ================================================================
-- 实验 1: RC 隔离级别下的不可重复读
-- ================================================================
-- ============ 会话 A ============
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

-- 统计总余额
SELECT SUM(balance) as total
FROM test_account;
-- 结果：2000.00

-- ============ 会话 B ============
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

-- 转账：张三转给李四 200 元
UPDATE test_account
SET balance = balance - 200
WHERE id = 1;
UPDATE test_account
SET balance = balance + 200
WHERE id = 2;
COMMIT;

-- ============ 会话 A ============
-- 再次统计总余额
SELECT SUM(balance) as total
FROM test_account;
-- 【现象】还是 2000.00（因为 RC 每次读都生成新 Read View）

-- 但如果再次查询
SELECT SUM(balance) as total
FROM test_account;
-- 结果：2000.00（数据一致，但中间状态不一致）

COMMIT;


-- ================================================================
-- 实验 2: RR 隔离级别下的可重复读
-- ================================================================
-- ============ 会话 A ============
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- 第一次统计
SELECT SUM(balance) as total
FROM test_account;
-- 结果：2000.00

-- ============ 会话 B ============
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- 转账
UPDATE test_account
SET balance = balance - 300
WHERE id = 1;
UPDATE test_account
SET balance = balance + 300
WHERE id = 2;
COMMIT;

-- ============ 会话 A ============
-- 再次统计
SELECT SUM(balance) as total
FROM test_account;
-- 【现象】仍然是 2000.00（RR 保证整个事务内看到的数据一致性）

COMMIT;


-- ================================================================
-- 实验 3: 幻读演示（RR 隔离级别）
-- ================================================================
-- ============ 会话 A ============
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- 查询 id > 1 的所有记录
SELECT *
FROM test_account
WHERE id > 1;
-- 假设只有 id=2 的记录

-- 使用 FOR UPDATE 加锁（当前读）
SELECT *
FROM test_account
WHERE id > 1 FOR
UPDATE;

-- ============ 会话 B ============
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
START TRANSACTION;

-- 尝试插入 id=3 的记录
INSERT INTO test_account
VALUES (3, '赵六', 1000.00);
-- 【现象】可能被阻塞（Next-Key Lock 防止幻读）

-- 或者尝试插入 id=4
INSERT INTO test_account
VALUES (4, '钱七', 1000.00);
-- 【现象】也可能被阻塞

COMMIT;

-- ============ 会话 A ============
COMMIT;


-- ================================================================
-- 实验 4: 实际业务场景模拟 - 库存扣减
-- ================================================================

-- 创建库存表
DROP TABLE IF EXISTS inventory;
CREATE TABLE inventory
(
    product_id   INT PRIMARY KEY,
    product_name VARCHAR(100),
    stock        INT
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

INSERT INTO inventory
VALUES (1, 'iPhone 15', 10);

-- ============ 场景 1: RC 隔离级别 - 超卖风险 ============
-- ============ 会话 A ============
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

-- 查询库存
SELECT stock
FROM inventory
WHERE product_id = 1;
-- 结果：10

-- ============ 会话 B ============
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
START TRANSACTION;

SELECT stock
FROM inventory
WHERE product_id = 1;
-- 结果：10

-- ============ 会话 A ============
-- 扣减库存
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1;
COMMIT;

-- ============ 会话 B ============
-- 也扣减库存（基于读到的 10）
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1;
COMMIT;

-- ============ 验证 ============
SELECT *
FROM inventory;
-- 结果：stock = 8（正确，没有超卖，因为 UPDATE 是原子操作）


-- ============ 场景 2: 检查库存逻辑 - RC 的问题 ============
-- ============ 会话 A ============
START TRANSACTION;
SELECT stock
FROM inventory
WHERE product_id = 1;
-- 8
-- 业务逻辑：如果库存 >= 1，则扣减

-- ============ 会话 B ============
START TRANSACTION;
SELECT stock
FROM inventory
WHERE product_id = 1;
-- 8

-- ============ 会话 A ============
-- 连续扣减 8 次
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1; -- 7 次...
COMMIT;

-- ============ 会话 B ============
-- 基于缓存的库存值 8，继续扣减
UPDATE inventory
SET stock = stock - 1
WHERE product_id = 1;
-- 结果：stock = -1（超卖了！）

COMMIT;


-- ================================================================
-- 清理环境
-- ================================================================
DROP TABLE IF EXISTS test_account;
DROP TABLE IF EXISTS inventory;

-- ================================================================
-- 总结表格
-- ================================================================
/*
┌─────────────────────┬──────────┬──────────┬──────────┬──────────┐
│ 隔离级别            │ 脏读     │ 不可重复读│ 幻读     │ 性能     │
├─────────────────────┼──────────┼──────────┼──────────┼──────────┤
│ READ UNCOMMITTED    │ ✓        │ ✓        │ ✓        │ 最高     │
│ READ COMMITTED      │ ✗        │ ✓        │ ✓        │ 高       │
│ REPEATABLE READ     │ ✗        │ ✗        │ 基本解决 │ 中       │
│ SERIALIZABLE        │ ✗        │ ✗        │ ✗        │ 最低     │
└─────────────────────┴──────────┴──────────┴──────────┴──────────┘

说明：
✓ = 存在问题
✗ = 已解决

MySQL 8 推荐使用：REPEATABLE READ（默认）+ MVCC + Next-Key Lock
*/
