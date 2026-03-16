package cn.itzixiao.interview.mysql;

/**
 * MySQL 事务与锁机制详解
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │                           MySQL 事务与锁机制体系                               │
 * ├──────────────────────────────────────────────────────────────────────────────┤
 * │                                                                              │
 * │    ┌─────────────┐        ┌─────────────┐        ┌─────────────┐             │
 * │    │   事务      │        │   锁机制    │        │   MVCC      │             │
 * │    │   ACID      │        │   行锁/表锁 │        │   多版本    │             │
 * │    └──────┬──────┘        └──────┬──────┘        └──────┬──────┘             │
 * │           │                      │                      │                    │
 * │    ┌──────▼──────┐        ┌──────▼──────┐        ┌──────▼──────┐             │
 * │    │ 隔离级别    │        │ Record Lock │        │ Read View   │             │
 * │    │ RU/RC/RR/S  │        │ Gap Lock    │        │ Undo Log    │             │
 * │    │             │        │ Next-Key    │        │ 版本链      │             │
 * │    └─────────────┘        └─────────────┘        └─────────────┘             │
 * │                                                                              │
 * └──────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author itzixiao
 */
public class TransactionAndLockDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     MySQL 事务与锁机制详解                                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");

        // 第一部分：事务ACID特性
        demonstrateACID();

        // 第二部分：事务隔离级别
        demonstrateIsolationLevels();

        // 第三部分：锁机制详解
        demonstrateLockMechanism();

        // 第四部分：死锁分析与解决
        demonstrateDeadlock();

        // 第五部分：事务最佳实践
        demonstrateBestPractices();

        // 第六部分：高频面试题
        printInterviewQuestions();
    }

    // ==================== 第一部分：事务ACID特性 ====================

    /**
     * 事务ACID特性详解
     */
    private static void demonstrateACID() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第一部分：事务ACID特性                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【ACID 四大特性】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  A - Atomicity（原子性）                                                │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  定义：事务是不可分割的最小操作单位，要么全部成功，要么全部失败回滚      │");
        System.out.println("│  实现：Undo Log（回滚日志）                                             │");
        System.out.println("│       - 记录事务修改前的数据状态                                        │");
        System.out.println("│       - 回滚时根据 Undo Log 恢复数据                                    │");
        System.out.println("│                                                                        │");
        System.out.println("│  示例：转账操作                                                         │");
        System.out.println("│    BEGIN;                                                              │");
        System.out.println("│    UPDATE account SET balance = balance - 100 WHERE id = 1; -- 扣款   │");
        System.out.println("│    UPDATE account SET balance = balance + 100 WHERE id = 2; -- 加款   │");
        System.out.println("│    COMMIT; -- 两个操作要么都成功，要么都回滚                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  C - Consistency（一致性）                                              │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  定义：事务执行前后，数据库从一个一致状态转换到另一个一致状态            │");
        System.out.println("│  实现：由原子性、隔离性、持久性共同保证                                  │");
        System.out.println("│       - 应用层也要保证业务逻辑正确                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│  示例：转账前后，总金额不变                                              │");
        System.out.println("│    转账前：A账户 1000，B账户 500，总金额 1500                           │");
        System.out.println("│    转账后：A账户 900， B账户 600，总金额 1500（一致）                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  I - Isolation（隔离性）                                                │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  定义：多个并发事务之间相互隔离，互不干扰                                │");
        System.out.println("│  实现：MVCC（多版本并发控制）+ 锁                                       │");
        System.out.println("│       - 读操作：MVCC 读历史版本，不加锁                                 │");
        System.out.println("│       - 写操作：加锁（行锁、间隙锁）                                    │");
        System.out.println("│                                                                        │");
        System.out.println("│  隔离级别：READ UNCOMMITTED → READ COMMITTED → REPEATABLE READ → SERIALIZABLE");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  D - Durability（持久性）                                               │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  定义：事务一旦提交，对数据库的修改是永久性的，即使系统故障也不丢失    │");
        System.out.println("│  实现：Redo Log（重做日志）                                             │");
        System.out.println("│       - WAL（Write-Ahead Logging）先写日志，后写磁盘                    │");
        System.out.println("│       - 顺序IO，性能更高                                                │");
        System.out.println("│       - 崩溃恢复时根据 Redo Log 重做已提交的事务                        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【Redo Log vs Undo Log】");
        System.out.println("┌─────────────────┬────────────────────────────┬────────────────────────────┐");
        System.out.println("│                 │ Redo Log                   │ Undo Log                   │");
        System.out.println("├─────────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 作用            │ 保证持久性（崩溃恢复）     │ 保证原子性（事务回滚）     │");
        System.out.println("├─────────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 记录内容        │ 物理日志（数据页修改）     │ 逻辑日志（操作的反向操作） │");
        System.out.println("├─────────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 写入时机        │ 事务执行过程中             │ 事务执行过程中             │");
        System.out.println("├─────────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 其他用途        │ -                          │ MVCC多版本读               │");
        System.out.println("└─────────────────┴────────────────────────────┴────────────────────────────┘\n");
    }

    // ==================== 第二部分：事务隔离级别 ====================

    /**
     * 事务隔离级别详解
     */
    private static void demonstrateIsolationLevels() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第二部分：事务隔离级别                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【并发事务问题】\n");

        System.out.println("1. 脏读（Dirty Read）");
        System.out.println("   - 事务A读取到事务B未提交的数据");
        System.out.println("   - 如果事务B回滚，事务A读到的就是无效数据");
        System.out.println("   ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("   │  事务A                        │  事务B                     │");
        System.out.println("   ├───────────────────────────────┼────────────────────────────┤");
        System.out.println("   │                               │  BEGIN;                    │");
        System.out.println("   │                               │  UPDATE balance SET 1000→500│");
        System.out.println("   │  SELECT balance; → 500（脏读）│                            │");
        System.out.println("   │                               │  ROLLBACK; → balance回到1000│");
        System.out.println("   └───────────────────────────────┴────────────────────────────┘\n");

        System.out.println("2. 不可重复读（Non-Repeatable Read）");
        System.out.println("   - 事务A内两次读取同一数据，结果不同");
        System.out.println("   - 因为事务B在中间修改并提交了数据");
        System.out.println("   ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("   │  事务A                        │  事务B                     │");
        System.out.println("   ├───────────────────────────────┼────────────────────────────┤");
        System.out.println("   │  BEGIN;                       │                            │");
        System.out.println("   │  SELECT balance; → 1000       │                            │");
        System.out.println("   │                               │  BEGIN;                    │");
        System.out.println("   │                               │  UPDATE balance SET 500;   │");
        System.out.println("   │                               │  COMMIT;                   │");
        System.out.println("   │  SELECT balance; → 500（变了）│                            │");
        System.out.println("   └───────────────────────────────┴────────────────────────────┘\n");

        System.out.println("3. 幻读（Phantom Read）");
        System.out.println("   - 事务A内两次查询，第二次多出或少了记录");
        System.out.println("   - 因为事务B在中间插入/删除了数据");
        System.out.println("   ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("   │  事务A                        │  事务B                     │");
        System.out.println("   ├───────────────────────────────┼────────────────────────────┤");
        System.out.println("   │  BEGIN;                       │                            │");
        System.out.println("   │  SELECT COUNT(*) WHERE age>20;│                            │");
        System.out.println("   │  → 3条                        │                            │");
        System.out.println("   │                               │  BEGIN;                    │");
        System.out.println("   │                               │  INSERT INTO user (age=25);│");
        System.out.println("   │                               │  COMMIT;                   │");
        System.out.println("   │  SELECT COUNT(*) WHERE age>20;│                            │");
        System.out.println("   │  → 4条（幻读）                │                            │");
        System.out.println("   └───────────────────────────────┴────────────────────────────┘\n");

        System.out.println("【四种隔离级别】\n");

        System.out.println("┌─────────────────────┬────────────┬──────────────┬────────────┬──────────────┐");
        System.out.println("│ 隔离级别            │ 脏读       │ 不可重复读   │ 幻读       │ 实现方式     │");
        System.out.println("├─────────────────────┼────────────┼──────────────┼────────────┼──────────────┤");
        System.out.println("│ READ UNCOMMITTED    │ 可能       │ 可能         │ 可能       │ 无隔离       │");
        System.out.println("│ (读未提交)          │            │              │            │              │");
        System.out.println("├─────────────────────┼────────────┼──────────────┼────────────┼──────────────┤");
        System.out.println("│ READ COMMITTED      │ 不可能     │ 可能         │ 可能       │ 每次SELECT   │");
        System.out.println("│ (读已提交)          │            │              │            │ 新建ReadView │");
        System.out.println("├─────────────────────┼────────────┼──────────────┼────────────┼──────────────┤");
        System.out.println("│ REPEATABLE READ     │ 不可能     │ 不可能       │ 不可能*    │ 事务开始时   │");
        System.out.println("│ (可重复读，默认)    │            │              │            │ 创建ReadView │");
        System.out.println("├─────────────────────┼────────────┼──────────────┼────────────┼──────────────┤");
        System.out.println("│ SERIALIZABLE        │ 不可能     │ 不可能       │ 不可能     │ 串行加锁     │");
        System.out.println("│ (串行化)            │            │              │            │              │");
        System.out.println("└─────────────────────┴────────────┴──────────────┴────────────┴──────────────┘");
        System.out.println("* InnoDB 在 RR 级别通过 MVCC + 间隙锁 解决幻读\n");

        System.out.println("【设置隔离级别】");
        System.out.println("-- 查看当前隔离级别");
        System.out.println("SELECT @@transaction_isolation;");
        System.out.println(" ");
        System.out.println("-- 设置会话隔离级别");
        System.out.println("SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;");
        System.out.println(" ");
        System.out.println("-- 设置全局隔离级别（需要重新连接生效）");
        System.out.println("SET GLOBAL TRANSACTION ISOLATION LEVEL REPEATABLE READ;\n");
    }

    // ==================== 第三部分：锁机制详解 ====================

    /**
     * MySQL锁机制详解
     */
    private static void demonstrateLockMechanism() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第三部分：锁机制详解                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【锁分类】\n");

        System.out.println("1. 按粒度分类：");
        System.out.println("┌─────────────────┬────────────────────────────────────────────────────────┐");
        System.out.println("│ 表级锁          │ - 锁定整张表，开销小，加锁快                           │");
        System.out.println("│                 │ - 粒度大，并发度低                                     │");
        System.out.println("│                 │ - MyISAM 和 MEMORY 引擎使用                            │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println("│ 行级锁          │ - 锁定单行记录，开销大，加锁慢                         │");
        System.out.println("│                 │ - 粒度小，并发度高                                     │");
        System.out.println("│                 │ - InnoDB 引擎使用，会发生死锁                          │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────────┘\n");

        System.out.println("2. 按模式分类：");
        System.out.println("┌─────────────────┬────────────────────────────────────────────────────────┐");
        System.out.println("│ 共享锁 (S锁)    │ - 读锁，多个事务可以同时持有                           │");
        System.out.println("│ Share Lock      │ - SELECT ... LOCK IN SHARE MODE                        │");
        System.out.println("│                 │ - 阻止其他事务获取排他锁                               │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println("│ 排他锁 (X锁)    │ - 写锁，只有一个事务可以持有                           │");
        System.out.println("│ Exclusive Lock  │ - SELECT ... FOR UPDATE / INSERT / UPDATE / DELETE     │");
        System.out.println("│                 │ - 阻止其他事务获取任何锁                               │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────────┘\n");

        System.out.println("3. InnoDB 行锁类型：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Record Lock（记录锁）                                                  │");
        System.out.println("│  ────────────────────────────────────────────────────────────────────  │");
        System.out.println("│  - 锁定索引中的一条记录                                                 │");
        System.out.println("│  - 如果没有索引，InnoDB 会创建隐藏聚簇索引                              │");
        System.out.println("│                                                                        │");
        System.out.println("│  示例：表中有 id=1,5,10 三条记录                                       │");
        System.out.println("│        SELECT * FROM t WHERE id = 5 FOR UPDATE;                        │");
        System.out.println("│        → 只锁定 id=5 这一行                                            │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Gap Lock（间隙锁）                                                     │");
        System.out.println("│  ────────────────────────────────────────────────────────────────────  │");
        System.out.println("│  - 锁定索引记录之间的间隙（不包含记录本身）                             │");
        System.out.println("│  - 防止其他事务在间隙内插入数据，解决幻读                               │");
        System.out.println("│  - 只在 RR 隔离级别下存在                                               │");
        System.out.println("│                                                                        │");
        System.out.println("│  示例：表中有 id=1,5,10 三条记录                                       │");
        System.out.println("│        SELECT * FROM t WHERE id = 3 FOR UPDATE;                        │");
        System.out.println("│        → 锁定 (1, 5) 这个间隙，阻止插入 id=2,3,4                       │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Next-Key Lock（临键锁）                                                │");
        System.out.println("│  ────────────────────────────────────────────────────────────────────  │");
        System.out.println("│  - Record Lock + Gap Lock 的组合                                       │");
        System.out.println("│  - 锁定记录本身 + 记录前面的间隙                                        │");
        System.out.println("│  - InnoDB 默认的行锁算法                                                │");
        System.out.println("│                                                                        │");
        System.out.println("│  示例：表中有 id=1,5,10 三条记录                                       │");
        System.out.println("│        SELECT * FROM t WHERE id >= 5 FOR UPDATE;                       │");
        System.out.println("│        → 锁定 (1,5], (5,10], (10,+∞)                                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【锁兼容矩阵】");
        System.out.println("┌─────────────────┬─────────────────┬─────────────────┐");
        System.out.println("│ 请求 \\ 持有     │ S锁（共享锁）   │ X锁（排他锁）   │");
        System.out.println("├─────────────────┼─────────────────┼─────────────────┤");
        System.out.println("│ S锁（共享锁）   │ 兼容 ✓          │ 冲突 ✗          │");
        System.out.println("├─────────────────┼─────────────────┼─────────────────┤");
        System.out.println("│ X锁（排他锁）   │ 冲突 ✗          │ 冲突 ✗          │");
        System.out.println("└─────────────────┴─────────────────┴─────────────────┘\n");

        System.out.println("【查看锁信息】");
        System.out.println("-- MySQL 8.0+");
        System.out.println("SELECT * FROM performance_schema.data_locks;");
        System.out.println("SELECT * FROM performance_schema.data_lock_waits;");
        System.out.println(" ");
        System.out.println("-- 查看当前锁等待");
        System.out.println("SHOW ENGINE INNODB STATUS;\n");
    }

    // ==================== 第四部分：死锁分析与解决 ====================

    /**
     * 死锁分析与解决
     */
    private static void demonstrateDeadlock() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第四部分：死锁分析与解决                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【什么是死锁？】");
        System.out.println("两个或多个事务相互等待对方持有的锁，导致所有事务都无法继续执行。\n");

        System.out.println("【死锁示例】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  时间    │  事务A                         │  事务B                     │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  T1      │  BEGIN;                        │  BEGIN;                    │");
        System.out.println("│  T2      │  UPDATE t SET x=1 WHERE id=1;  │                            │");
        System.out.println("│          │  (持有 id=1 的 X 锁)           │                            │");
        System.out.println("│  T3      │                                │  UPDATE t SET x=2 WHERE id=2;│");
        System.out.println("│          │                                │  (持有 id=2 的 X 锁)       │");
        System.out.println("│  T4      │  UPDATE t SET x=1 WHERE id=2;  │                            │");
        System.out.println("│          │  (等待 id=2 的锁 → 阻塞)       │                            │");
        System.out.println("│  T5      │                                │  UPDATE t SET x=2 WHERE id=1;│");
        System.out.println("│          │                                │  (等待 id=1 的锁 → 死锁!)  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【InnoDB 死锁检测】");
        System.out.println("- InnoDB 有死锁检测机制");
        System.out.println("- 检测到死锁后，选择回滚代价较小的事务（Undo Log 量较少）");
        System.out.println("- 被回滚的事务收到错误：Deadlock found when trying to get lock\n");

        System.out.println("【查看死锁信息】");
        System.out.println("SHOW ENGINE INNODB STATUS;  -- 查看最近一次死锁");
        System.out.println(" ");
        System.out.println("-- 开启死锁日志");
        System.out.println("SET GLOBAL innodb_print_all_deadlocks = ON;\n");

        System.out.println("【如何避免死锁】");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 固定加锁顺序                                                        │");
        System.out.println("│     - 所有事务按相同顺序访问资源（如按 ID 升序）                        │");
        System.out.println("│     - 最有效的方法                                                      │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. 缩短事务时间                                                        │");
        System.out.println("│     - 事务尽量小，持有锁时间短                                          │");
        System.out.println("│     - 避免在事务中做耗时操作（如RPC调用）                               │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. 降低隔离级别                                                        │");
        System.out.println("│     - RC 级别没有间隙锁，减少死锁概率                                   │");
        System.out.println("│     - 但可能引入幻读问题                                                │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  4. 合理使用索引                                                        │");
        System.out.println("│     - 没有索引会锁全表                                                  │");
        System.out.println("│     - 索引选择性低也会锁大量行                                          │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  5. 设置锁等待超时                                                      │");
        System.out.println("│     SET innodb_lock_wait_timeout = 5; -- 等待5秒后超时                  │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第五部分：事务最佳实践 ====================

    /**
     * 事务最佳实践
     */
    private static void demonstrateBestPractices() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第五部分：事务最佳实践                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【事务使用原则】\n");

        System.out.println("1. 事务尽量短小");
        System.out.println("   - 减少锁持有时间");
        System.out.println("   - 避免长事务占用 Undo Log 空间");
        System.out.println("   - 大批量操作分批提交\n");

        System.out.println("2. 避免在事务中做耗时操作");
        System.out.println("   ✗ BEGIN; → RPC调用 → UPDATE → COMMIT;");
        System.out.println("   ✓ RPC调用 → BEGIN; → UPDATE → COMMIT;\n");

        System.out.println("3. 合理选择隔离级别");
        System.out.println("   - 大多数场景：REPEATABLE READ（默认）");
        System.out.println("   - 允许不可重复读：READ COMMITTED（减少锁冲突）");
        System.out.println("   - 严格一致性要求：SERIALIZABLE（性能最差）\n");

        System.out.println("4. 避免不必要的事务");
        System.out.println("   - 纯读操作不需要显式事务");
        System.out.println("   - 单条SQL本身是原子的\n");

        System.out.println("5. 正确处理异常");
        System.out.println("   ┌────────────────────────────────────────────────────────────────────┐");
        System.out.println("   │  try {                                                             │");
        System.out.println("   │      connection.setAutoCommit(false);                              │");
        System.out.println("   │      // 业务操作                                                   │");
        System.out.println("   │      connection.commit();                                          │");
        System.out.println("   │  } catch (Exception e) {                                           │");
        System.out.println("   │      connection.rollback();  // 异常时必须回滚                     │");
        System.out.println("   │      throw e;                                                      │");
        System.out.println("   │  } finally {                                                       │");
        System.out.println("   │      connection.setAutoCommit(true);                               │");
        System.out.println("   │      connection.close();                                           │");
        System.out.println("   │  }                                                                 │");
        System.out.println("   └────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【Spring 事务注意事项】\n");

        System.out.println("1. @Transactional 失效场景");
        System.out.println("   - 非 public 方法");
        System.out.println("   - 同类内部调用（自调用）");
        System.out.println("   - 异常被 catch 吞掉");
        System.out.println("   - 抛出非 RuntimeException（默认只回滚 RuntimeException）\n");

        System.out.println("2. 事务传播行为");
        System.out.println("   - REQUIRED（默认）：有则加入，无则新建");
        System.out.println("   - REQUIRES_NEW：挂起当前，新建独立事务");
        System.out.println("   - NESTED：嵌套事务（Savepoint）");
        System.out.println("   - SUPPORTS：有则加入，无则非事务执行\n");
    }

    // ==================== 第六部分：高频面试题 ====================

    /**
     * 高频面试题
     */
    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第六部分：高频面试题                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("==========================================================================");
        System.out.println("【问题1】MySQL事务的ACID特性是什么？各由什么机制保证？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("A - 原子性：Undo Log 实现，记录修改前的数据，回滚时恢复");
        System.out.println("C - 一致性：由原子性、隔离性、持久性共同保证");
        System.out.println("I - 隔离性：MVCC + 锁机制实现");
        System.out.println("D - 持久性：Redo Log 实现，WAL机制先写日志再写磁盘\n");

        System.out.println("==========================================================================");
        System.out.println("【问题2】MySQL默认隔离级别是什么？为什么选择这个级别？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("默认隔离级别是 REPEATABLE READ（可重复读）。");
        System.out.println(" ");
        System.out.println("选择原因：");
        System.out.println("1. 解决脏读和不可重复读问题");
        System.out.println("2. InnoDB 通过 MVCC + 间隙锁还解决了幻读");
        System.out.println("3. 相比 SERIALIZABLE 有更好的并发性能");
        System.out.println("4. 相比 READ COMMITTED 有更强的一致性保证\n");

        System.out.println("==========================================================================");
        System.out.println("【问题3】什么是脏读、不可重复读、幻读？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("脏读：读取到其他事务未提交的数据");
        System.out.println("不可重复读：同一事务内两次读取同一数据，结果不同（被其他事务修改）");
        System.out.println("幻读：同一事务内两次查询，记录数量不同（被其他事务插入/删除）\n");

        System.out.println("==========================================================================");
        System.out.println("【问题4】InnoDB有哪些行锁类型？分别解决什么问题？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("1. Record Lock（记录锁）：锁定单条记录，解决并发修改问题");
        System.out.println("2. Gap Lock（间隙锁）：锁定索引间隙，解决幻读问题");
        System.out.println("3. Next-Key Lock（临键锁）：Record + Gap，InnoDB默认锁类型");
        System.out.println("4. Insert Intention Lock（插入意向锁）：提高插入并发性\n");

        System.out.println("==========================================================================");
        System.out.println("【问题5】MVCC是如何实现的？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("MVCC通过以下机制实现：");
        System.out.println("1. 隐藏字段：DB_TRX_ID（事务ID）、DB_ROLL_PTR（回滚指针）");
        System.out.println("2. Undo Log：保存历史版本，形成版本链");
        System.out.println("3. Read View：记录活跃事务信息，判断数据可见性");
        System.out.println(" ");
        System.out.println("不同隔离级别的区别：");
        System.out.println("- RC：每次SELECT创建新ReadView");
        System.out.println("- RR：事务开始时创建ReadView，后续复用\n");

        System.out.println("==========================================================================");
        System.out.println("【问题6】什么是死锁？如何避免？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("死锁：两个或多个事务相互等待对方持有的锁。");
        System.out.println(" ");
        System.out.println("避免方法：");
        System.out.println("1. 固定加锁顺序（最有效）");
        System.out.println("2. 缩短事务时间");
        System.out.println("3. 使用RC隔离级别（无间隙锁）");
        System.out.println("4. 合理设计索引");
        System.out.println("5. 设置锁等待超时\n");

        System.out.println("==========================================================================");
        System.out.println("【问题7】Redo Log和Undo Log的区别？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("┌─────────────┬────────────────────────────┬────────────────────────────┐");
        System.out.println("│             │ Redo Log                   │ Undo Log                   │");
        System.out.println("├─────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 保证特性    │ 持久性                     │ 原子性                     │");
        System.out.println("├─────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 内容        │ 物理日志（数据页修改）     │ 逻辑日志（反向操作）       │");
        System.out.println("├─────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 用途        │ 崩溃恢复                   │ 事务回滚 + MVCC            │");
        System.out.println("└─────────────┴────────────────────────────┴────────────────────────────┘\n");

        System.out.println("==========================================================================");
        System.out.println("【问题8】SELECT ... FOR UPDATE 和 LOCK IN SHARE MODE 的区别？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("FOR UPDATE：加排他锁（X锁），其他事务不能读写");
        System.out.println("LOCK IN SHARE MODE：加共享锁（S锁），其他事务可以读，不能写");
        System.out.println(" ");
        System.out.println("使用场景：");
        System.out.println("FOR UPDATE：读取后需要修改");
        System.out.println("LOCK IN SHARE MODE：只需要保证读取时数据不被修改\n");

        System.out.println("==========================================================================");
        System.out.println("【问题9】@Transactional失效的场景有哪些？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("1. 方法非public");
        System.out.println("2. 同类内部调用（自调用）- 没有经过代理");
        System.out.println("3. 异常被catch吞掉，没有抛出");
        System.out.println("4. 抛出非RuntimeException（默认只回滚RuntimeException）");
        System.out.println("5. 数据库引擎不支持事务（如MyISAM）");
        System.out.println("6. 多数据源情况下，数据源没有配置事务管理器\n");

        System.out.println("==========================================================================");
        System.out.println("【问题10】大事务有什么危害？如何优化？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("危害：");
        System.out.println("1. 长时间持有锁，阻塞其他事务");
        System.out.println("2. 占用大量Undo Log空间");
        System.out.println("3. 回滚时间长");
        System.out.println("4. 主从延迟增大");
        System.out.println(" ");
        System.out.println("优化：");
        System.out.println("1. 拆分大事务为小事务");
        System.out.println("2. 批量操作分批提交");
        System.out.println("3. 避免在事务中做RPC调用");
        System.out.println("4. 只读操作不加事务\n");

        System.out.println("==========================================================================\n");
    }
}
