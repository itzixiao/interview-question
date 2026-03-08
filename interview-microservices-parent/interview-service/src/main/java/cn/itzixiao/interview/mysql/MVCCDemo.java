package cn.itzixiao.interview.mysql;

/**
 * MySQL MVCC (多版本并发控制) 详解
 *
 * MVCC 核心思想：
 * - 读不加锁，读写不冲突，提高并发性能
 * - 每行记录保存多个版本，根据事务隔离级别返回合适版本
 * - 通过 Undo Log 实现版本链
 *
 * InnoDB 行记录隐藏字段：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  DB_TRX_ID (6 byte)  - 最后修改该记录的事务ID                 │
 * │  DB_ROLL_PTR (7 byte) - 回滚指针，指向 Undo Log              │
 * │  DB_ROW_ID (6 byte)   - 隐藏主键（如果没有显式主键）          │
 * └─────────────────────────────────────────────────────────────┘
 */
public class MVCCDemo {

    public static void main(String[] args) {
        System.out.println("========== MySQL MVCC (多版本并发控制) 详解 ==========\n");

        // 1. MVCC 基础概念
        demonstrateMVCCBasics();

        // 2. Undo Log 版本链
        demonstrateUndoLogChain();

        // 3. Read View 机制
        demonstrateReadView();

        // 4. 不同隔离级别下的 MVCC
        demonstrateIsolationLevels();

        // 5. MVCC 与锁的关系
        demonstrateMVCCAndLocks();

        // 6. 可见性判断算法
        demonstrateVisibilityAlgorithm();
    }

    /**
     * 1. MVCC 基础概念
     */
    private static void demonstrateMVCCBasics() {
        System.out.println("【1. MVCC 基础概念】\n");

        System.out.println("什么是 MVCC？");
        System.out.println("- Multi-Version Concurrency Control（多版本并发控制）");
        System.out.println("- 实现读-写并发不加锁，提高数据库并发性能");
        System.out.println("- 是 InnoDB 实现事务隔离级别的基础\n");

        System.out.println("MVCC 解决的问题：");
        System.out.println("- 读写冲突：读操作不需要等待写操作释放锁");
        System.out.println("- 写读冲突：写操作不需要等待读操作完成");
        System.out.println("- 幻读：通过版本链和 Read View 控制\n");

        System.out.println("InnoDB 行记录结构：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  用户定义的列（id, name, age...）                            │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  DB_TRX_ID (6 byte)  - 事务ID                               │");
        System.out.println("│  - 创建或最后修改该记录的事务ID                              │");
        System.out.println("│  - 每个事务开始时分配唯一的递增ID                            │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  DB_ROLL_PTR (7 byte) - 回滚指针                            │");
        System.out.println("│  - 指向 Undo Log 中的历史版本                               │");
        System.out.println("│  - 形成版本链，支持回滚和读历史版本                          │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  DB_ROW_ID (6 byte) - 隐藏主键                              │");
        System.out.println("│  - 如果没有定义主键，用这个生成聚簇索引                        │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. Undo Log 版本链
     */
    private static void demonstrateUndoLogChain() {
        System.out.println("【2. Undo Log 版本链】\n");

        System.out.println("Undo Log 类型：");
        System.out.println("- INSERT Undo：事务回滚时使用，提交后可立即删除");
        System.out.println("- UPDATE Undo：用于 MVCC 读历史版本，不能随便删除\n");

        System.out.println("版本链形成过程示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  初始状态：事务ID=100 插入记录                               │");
        System.out.println("│  ┌─────────────────────────┐                                │");
        System.out.println("│  │ id=1, name='Alice'      │ ← 当前记录（DB_TRX_ID=100）    │");
        System.out.println("│  │ DB_TRX_ID=100           │                                │");
        System.out.println("│  │ DB_ROLL_PTR=null        │                                │");
        System.out.println("│  └─────────────────────────┘                                │");
        System.out.println("│                                                             │");
        System.out.println("│  事务200更新：name='Bob'                                    │");
        System.out.println("│  ┌─────────────────────────┐                                │");
        System.out.println("│  │ id=1, name='Bob'        │ ← 当前记录（DB_TRX_ID=200）    │");
        System.out.println("│  │ DB_TRX_ID=200           │                                │");
        System.out.println("│  │ DB_ROLL_PTR ────────────────┐                            │");
        System.out.println("│  └─────────────────────────┘  │                            │");
        System.out.println("│                               ↓                            │");
        System.out.println("│                     ┌─────────────────────────┐            │");
        System.out.println("│                     │ id=1, name='Alice'      │            │");
        System.out.println("│                     │ DB_TRX_ID=100           │            │");
        System.out.println("│                     │ DB_ROLL_PTR=null        │            │");
        System.out.println("│                     │ (Undo Log)              │            │");
        System.out.println("│                     └─────────────────────────┘            │");
        System.out.println("│                                                             │");
        System.out.println("│  事务300更新：name='Charlie'                                │");
        System.out.println("│  ┌─────────────────────────┐                                │");
        System.out.println("│  │ id=1, name='Charlie'    │ ← 当前记录（DB_TRX_ID=300）    │");
        System.out.println("│  │ DB_TRX_ID=300           │                                │");
        System.out.println("│  │ DB_ROLL_PTR ────────────────┐                            │");
        System.out.println("│  └─────────────────────────┘  │                            │");
        System.out.println("│                               ↓                            │");
        System.out.println("│                     ┌─────────────────────────┐            │");
        System.out.println("│                     │ id=1, name='Bob'        │            │");
        System.out.println("│                     │ DB_TRX_ID=200           │            │");
        System.out.println("│                     │ DB_ROLL_PTR ────────────────┐        │");
        System.out.println("│                     └─────────────────────────┘  │        │");
        System.out.println("│                                                  ↓        │");
        System.out.println("│                                        ┌──────────────────┐│");
        System.out.println("│                                        │ id=1, name='Alice'│");
        System.out.println("│                                        │ DB_TRX_ID=100     │");
        System.out.println("│                                        │ DB_ROLL_PTR=null  │");
        System.out.println("│                                        └──────────────────┘│");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("版本链的作用：");
        System.out.println("1. 事务回滚：沿着版本链恢复数据");
        System.out.println("2. MVCC 读：根据 Read View 选择合适的版本");
        System.out.println("3. 清理：Purge 线程定期清理不再需要的 Undo Log\n");
    }

    /**
     * 3. Read View 机制
     */
    private static void demonstrateReadView() {
        System.out.println("【3. Read View (读视图) 机制】\n");

        System.out.println("Read View 是什么？");
        System.out.println("- 事务执行快照读时产生的读视图");
        System.out.println("- 记录了事务开始时系统中活跃事务的信息");
        System.out.println("- 用于判断数据的可见性\n");

        System.out.println("Read View 包含的字段：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  creator_trx_id (4 byte)                                    │");
        System.out.println("│  - 创建该 Read View 的事务ID                                │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  m_ids (列表)                                               │");
        System.out.println("│  - 生成 Read View 时，系统中活跃的事务ID列表                 │");
        System.out.println("│  - 这些事务的修改对当前事务不可见                            │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  min_trx_id (4 byte)                                        │");
        System.out.println("│  - m_ids 中的最小事务ID                                     │");
        System.out.println("│  - 小于这个ID的事务都已经提交                                │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  max_trx_id (4 byte)                                        │");
        System.out.println("│  - 生成 Read View 时，系统要分配的下一个事务ID               │");
        System.out.println("│  - 大于等于这个ID的事务都还没开始                            │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Read View 生成时机：");
        System.out.println("- RC (Read Committed)：每次 SELECT 都生成新的 Read View");
        System.out.println("- RR (Repeatable Read)：事务第一次 SELECT 时生成，后续复用\n");
    }

    /**
     * 4. 不同隔离级别下的 MVCC
     */
    private static void demonstrateIsolationLevels() {
        System.out.println("【4. 不同隔离级别下的 MVCC】\n");

        System.out.println("四种隔离级别实现方式：");
        System.out.println("┌─────────────────────┬────────────────────────────────────────┐");
        System.out.println("│  READ UNCOMMITTED   │ 直接读最新数据，不使用 MVCC            │");
        System.out.println("│  (读未提交)         │ 可能读到未提交的数据（脏读）            │");
        System.out.println("├─────────────────────┼────────────────────────────────────────┤");
        System.out.println("│  READ COMMITTED     │ 每次 SELECT 生成新 Read View           │");
        System.out.println("│  (读已提交)         │ 解决脏读，但不可重复读                  │");
        System.out.println("├─────────────────────┼────────────────────────────────────────┤");
        System.out.println("│  REPEATABLE READ    │ 事务开始时生成 Read View，一直复用      │");
        System.out.println("│  (可重复读)         │ 解决不可重复读，InnoDB 还解决幻读       │");
        System.out.println("├─────────────────────┼────────────────────────────────────────┤");
        System.out.println("│  SERIALIZABLE       │ 所有操作加锁，不使用 MVCC               │");
        System.out.println("│  (串行化)           │ 完全串行执行，性能最差                  │");
        System.out.println("└─────────────────────┴────────────────────────────────────────┘\n");

        System.out.println("RC vs RR 示例对比：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  时间线   │  事务A (SELECT)        │  事务B (UPDATE)          │");
        System.out.println("├───────────┼────────────────────────┼──────────────────────────┤");
        System.out.println("│  T1       │  BEGIN;                │                          │");
        System.out.println("│           │  SELECT → Alice        │                          │");
        System.out.println("├───────────┼────────────────────────┼──────────────────────────┤");
        System.out.println("│  T2       │                        │  BEGIN;                  │");
        System.out.println("│           │                        │  UPDATE → Bob;           │");
        System.out.println("│           │                        │  COMMIT;                 │");
        System.out.println("├───────────┼────────────────────────┼──────────────────────────┤");
        System.out.println("│  T3       │  SELECT → ?            │                          │");
        System.out.println("├───────────┼────────────────────────┼──────────────────────────┤");
        System.out.println("│  RC 结果  │  Bob (新 Read View)    │                          │");
        System.out.println("│  RR 结果  │  Alice (旧 Read View)  │                          │");
        System.out.println("└───────────┴────────────────────────┴──────────────────────────┘\n");

        System.out.println("InnoDB 如何解决幻读？");
        System.out.println("- 快照读（SELECT）：MVCC 保证不可见，不会幻读");
        System.out.println("- 当前读（SELECT ... FOR UPDATE）：间隙锁（Gap Lock）");
        System.out.println("  锁定范围，阻止其他事务插入\n");
    }

    /**
     * 5. MVCC 与锁的关系
     */
    private static void demonstrateMVCCAndLocks() {
        System.out.println("【5. MVCC 与锁的关系】\n");

        System.out.println("两种读操作：");
        System.out.println("1. 快照读（Snapshot Read）");
        System.out.println("   - 普通 SELECT 语句");
        System.out.println("   - 不加锁，使用 MVCC 读历史版本");
        System.out.println("   - 实现读写不阻塞\n");

        System.out.println("2. 当前读（Current Read）");
        System.out.println("   - SELECT ... LOCK IN SHARE MODE");
        System.out.println("   - SELECT ... FOR UPDATE");
        System.out.println("   - INSERT、UPDATE、DELETE");
        System.out.println("   - 读最新版本，需要加锁\n");

        System.out.println("锁类型：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  行锁（Record Lock）- 锁定单个记录                           │");
        System.out.println("│  - S 锁（共享锁）：SELECT ... LOCK IN SHARE MODE            │");
        System.out.println("│  - X 锁（排他锁）：SELECT ... FOR UPDATE / DML              │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  间隙锁（Gap Lock）- 锁定范围，防止幻读                       │");
        System.out.println("│  - 锁定索引记录之间的间隙                                     │");
        System.out.println("│  - 阻止其他事务在范围内插入                                   │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  临键锁（Next-Key Lock）- 行锁 + 间隙锁                      │");
        System.out.println("│  - InnoDB 默认的锁算法                                        │");
        System.out.println("│  - 锁定记录及其前面的间隙                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("示例：");
        System.out.println("表 user，id 为主键，有记录 id=1, 5, 10");
        System.out.println("SELECT * FROM user WHERE id = 5 FOR UPDATE;");
        System.out.println("- 加 Next-Key Lock：锁定 (1, 5] 区间");
        System.out.println("- 其他事务：不能修改 id=5，不能在 (1,5) 插入\n");
    }

    /**
     * 6. 可见性判断算法
     */
    private static void demonstrateVisibilityAlgorithm() {
        System.out.println("【6. 可见性判断算法】\n");

        System.out.println("判断数据版本是否可见的规则：");
        System.out.println("对于一条记录，DB_TRX_ID = trx_id\n");

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  规则1：trx_id == creator_trx_id                             │");
        System.out.println("│  → 数据是当前事务自己修改的，可见                            │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  规则2：trx_id < min_trx_id                                  │");
        System.out.println("│  → 数据在 Read View 生成前已提交，可见                       │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  规则3：trx_id >= max_trx_id                                 │");
        System.out.println("│  → 数据在 Read View 生成后创建，不可见                       │");
        System.out.println("│  → 沿着 Undo Log 找上一个版本                                │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  规则4：min_trx_id <= trx_id < max_trx_id                    │");
        System.out.println("│  → 检查 trx_id 是否在 m_ids 列表中                           │");
        System.out.println("│  → 在列表中：事务未提交，不可见，找上一个版本                  │");
        System.out.println("│  → 不在列表中：事务已提交，可见                              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("完整判断流程图：");
        System.out.println("┌─────────────┐");
        System.out.println("│ 开始判断     │");
        System.out.println("└──────┬──────┘");
        System.out.println("       ↓");
        System.out.println("┌─────────────────┐     是     ┌─────────────┐");
        System.out.println("│ trx_id ==       │──────────→│  可见       │");
        System.out.println("│ creator_trx_id? │            └─────────────┘");
        System.out.println("└────────┬────────┘");
        System.out.println("       ↓ 否");
        System.out.println("┌─────────────────┐     是     ┌─────────────┐");
        System.out.println("│ trx_id <        │──────────→│  可见       │");
        System.out.println("│ min_trx_id?     │            └─────────────┘");
        System.out.println("└────────┬────────┘");
        System.out.println("       ↓ 否");
        System.out.println("┌─────────────────┐     是     ┌─────────────────┐");
        System.out.println("│ trx_id >=       │──────────→│  不可见         │");
        System.out.println("│ max_trx_id?     │            │  找上一个版本   │");
        System.out.println("└────────┬────────┘            └─────────────────┘");
        System.out.println("       ↓ 否");
        System.out.println("┌─────────────────┐     是     ┌─────────────────┐");
        System.out.println("│ trx_id 在       │──────────→│  不可见         │");
        System.out.println("│ m_ids 中?       │            │  找上一个版本   │");
        System.out.println("└────────┬────────┘            └─────────────────┘");
        System.out.println("       ↓ 否");
        System.out.println("┌─────────────┐");
        System.out.println("│  可见       │");
        System.out.println("└─────────────┘\n");

        System.out.println("MVCC 总结：");
        System.out.println("1. 通过 Undo Log 实现多版本");
        System.out.println("2. 通过 Read View 控制版本可见性");
        System.out.println("3. 实现读-写不阻塞，提高并发性能");
        System.out.println("4. RC 每次读生成新视图，RR 事务内复用视图");
        System.out.println("5. 当前读需要加锁，保证数据一致性\n");
    }
}
