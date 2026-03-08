package cn.itzixiao.interview.mysql;

/**
 * MySQL 核心知识点详解 - 面试高频考点
 * 
 * <pre>
 * 涵盖内容：
 * 1. MySQL 字段类型
 * 2. MySQL 存储引擎
 * 3. MySQL 索引原理
 * 4. MySQL 日志系统
 * 5. MySQL事务特性
 * 6. MySQL 锁机制
 * 7. MySQL 性能优化
 * </pre>
 * 
 * @author itzixiao
 */
public class MySQLCorePrincipleDemo {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                    MySQL 核心知识点详解                                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");
        
        // 第一部分：MySQL 字段类型
        demonstrateFieldTypes();
        
        // 第二部分：存储引擎
        demonstrateStorageEngines();
        
        // 第三部分：索引原理
        demonstrateIndexPrinciple();
        
        // 第四部分：日志系统
        demonstrateLogSystem();
        
        // 第五部分：事务特性
        demonstrateTransaction();
        
        // 第六部分：锁机制
        demonstrateLockMechanism();
        
        // 第七部分：性能优化
        demonstratePerformanceOptimization();
        
        // 第八部分：高频面试题
        printInterviewQuestions();
    }
    
    // ==================== 第一部分：MySQL 字段类型 ====================
    
    private static void demonstrateFieldTypes() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第一部分：MySQL 字段类型                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【1.1 数值类型】\n");
        System.out.println("┌─────────────────┬──────────┬──────────────┬─────────────────────────┐");
        System.out.println("│ 类型            │ 字节     │ 有符号范围   │ 无符号范围              │");
        System.out.println("├─────────────────┼──────────┼──────────────┼─────────────────────────┤");
        System.out.println("│ TINYINT         │ 1        │ -128~127     │ 0~255                   │");
        System.out.println("│ SMALLINT        │ 2        │ -32768~32767 │ 0~65535                 │");
        System.out.println("│ MEDIUMINT       │ 3        │ 约±8 百万      │ 0~16 百万                │");
        System.out.println("│ INT/INTEGER     │ 4        │ ±21 亿         │ 0~42 亿                  │");
        System.out.println("│ BIGINT          │ 8        │ ±9×10^18     │ 0~1.8×10^19             │");
        System.out.println("├─────────────────┼──────────┼──────────────┼─────────────────────────┤");
        System.out.println("│ FLOAT           │ 4        │ 单精度浮点数 │                         │");
        System.out.println("│ DOUBLE          │ 8        │ 双精度浮点数 │                         │");
        System.out.println("│ DECIMAL(M,D)    │ 变长     │ 精确小数     │ 适合金额               │");
        System.out.println("└─────────────────┴──────────┴──────────────┴─────────────────────────┘\n");
        
        System.out.println("【1.2 字符串类型】\n");
        System.out.println("┌─────────────────┬────────────────────────────────────────────────────┐");
        System.out.println("│ 类型            │ 说明                                               │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│ CHAR(N)         │ 固定长度，N=0~255，不足补空格                       │");
        System.out.println("│ VARCHAR(N)      │ 可变长度，N=0~65535，节省空间                      │");
        System.out.println("│ TINYTEXT        │ 最大 255 字节                                        │");
        System.out.println("│ TEXT            │ 最大 65535 字节                                      │");
        System.out.println("│ MEDIUMTEXT      │ 最大 16MB                                          │");
        System.out.println("│ LONGTEXT        │ 最大 4GB                                           │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│ BINARY/VARBINARY│ 二进制字符串                                       │");
        System.out.println("│ BLOB            │ 二进制大对象（图片、视频）                          │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────┘\n");
        
        System.out.println("【1.3 日期时间类型】\n");
        System.out.println("┌─────────────────┬──────────────┬────────────────────────────────────┐");
        System.out.println("│ 类型            │ 格式         │ 范围                                 │");
        System.out.println("├─────────────────┼──────────────┼────────────────────────────────────┤");
        System.out.println("│ DATE            │ YYYY-MM-DD   │ 1000-01-01 ~ 9999-12-31            │");
        System.out.println("│ TIME            │ HH:MM:SS     │ -838:59:59 ~ 838:59:59             │");
        System.out.println("│ DATETIME        │ YYYY-MM-DD   │ 1000-01-01 ~ 9999-12-31            │");
        System.out.println("│                 │ HH:MM:SS     │                                    │");
        System.out.println("│ TIMESTAMP       │ YYYY-MM-DD   │ 1970-01-01 ~ 2038-01-19            │");
        System.out.println("│                 │ HH:MM:SS     │ (受时区影响)                        │");
        System.out.println("│ YEAR            │ YYYY         │ 1901 ~ 2155                        │");
        System.out.println("└─────────────────┴──────────────┴────────────────────────────────────┘\n");
        
        System.out.println("【选型建议】");
        System.out.println("1. 金额用 DECIMAL，不用 FLOAT/DOUBLE（避免精度丢失）");
        System.out.println("2. 定长字符串用 CHAR，变长用 VARCHAR");
        System.out.println("3. 时间存储推荐 DATETIME（与时区无关）");
        System.out.println("4. 大文本用 TEXT，不要直接存 VARCHAR");
        System.out.println("5. 状态标识用 TINYINT（0/1 表示 false/true）\n");
    }
    
    // ==================== 第二部分：存储引擎 ====================
    
    private static void demonstrateStorageEngines() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第二部分：MySQL 存储引擎                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【2.1 InnoDB 引擎（默认）】\n");
        System.out.println("特点：");
        System.out.println("✓ 支持事务（ACID）");
        System.out.println("✓ 支持行级锁（并发性能好）");
        System.out.println("✓ 支持外键约束");
        System.out.println("✓ 支持 MVCC（多版本并发控制）");
        System.out.println("✓ 聚簇索引（数据和索引在一起）");
        System.out.println("✓ 崩溃恢复能力强");
        System.out.println("\n适用场景：高并发、事务安全、频繁更新\n");
        
        System.out.println("【2.2 MyISAM 引擎】\n");
        System.out.println("特点：");
        System.out.println("✗ 不支持事务");
        System.out.println("✗ 只支持表级锁（并发差）");
        System.out.println("✗ 不支持外键");
        System.out.println("✓ 查询速度快（读多写少）");
        System.out.println("✓ 占用空间小");
        System.out.println("✓ 支持全文索引（5.6 前）");
        System.out.println("\n适用场景：读多写少、不需要事务\n");
        
        System.out.println("【2.3 Memory 引擎】\n");
        System.out.println("特点：");
        System.out.println("✓ 数据存储在内存中（速度极快）");
        System.out.println("✗ 重启后数据丢失");
        System.out.println("✗ 不支持大字段（TEXT/BLOB）");
        System.out.println("✗ 只支持表级锁");
        System.out.println("\n适用场景：临时表、缓存表\n");
        
        System.out.println("【2.4 Archive 引擎】\n");
        System.out.println("特点：");
        System.out.println("✓ 高压缩比（节省空间）");
        System.out.println("✓ 只支持 INSERT 和 SELECT");
        System.out.println("✗ 不支持 UPDATE 和 DELETE");
        System.out.println("\n适用场景：日志归档、历史数据存储\n");
        
        System.out.println("【2.5 引擎选择对比】\n");
        System.out.println("┌──────────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("│ 特性         │ InnoDB   │ MyISAM   │ Memory   │ Archive  │");
        System.out.println("├──────────────┼──────────┼──────────┼──────────┼──────────┤");
        System.out.println("│ 事务支持     │ ✓        │ ✗        │ ✗        │ ✗        │");
        System.out.println("│ 行级锁       │ ✓        │ ✗        │ ✗        │ ✗        │");
        System.out.println("│ 外键支持     │ ✓        │ ✗        │ ✗        │ ✗        │");
        System.out.println("│ MVCC         │ ✓        │ ✗        │ ✗        │ ✗        │");
        System.out.println("│ 崩溃恢复     │ 强       │ 弱       │ 无       │ 弱       │");
        System.out.println("│ 并发性能     │ 高       │ 低       │ 中       │ 低       │");
        System.out.println("│ 存储空间     │ 较大     │ 小       │ 内存     │ 最小     │");
        System.out.println("└──────────────┴──────────┴──────────┴──────────┴──────────┘\n");
        
        System.out.println("【查看表的存储引擎】");
        System.out.println("SHOW TABLE STATUS WHERE Name='table_name';");
        System.out.println("SHOW VARIABLES LIKE 'storage_engine'; -- 查看默认引擎\n");
    }
    
    // ==================== 第三部分：索引原理 ====================
    
    private static void demonstrateIndexPrinciple() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第三部分：MySQL 索引原理                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【3.1 索引数据结构】\n");
        
        System.out.println("B+Tree（InnoDB 使用）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│         根节点（索引项）                                                 │");
        System.out.println("│        /    |    \\                                                      │");
        System.out.println("│   内部节点  内部节点  内部节点（只存索引）                                │");
        System.out.println("│     / \\     / \\     / \\                                                 │");
        System.out.println("│  叶子 叶子 叶子 叶子 叶子 叶子（存数据 + 索引，双向链表连接）             │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘");
        System.out.println("特点：");
        System.out.println("- 非叶子节点只存索引，不存数据（容纳更多索引项）");
        System.out.println("- 所有数据都存在叶子节点（查询稳定 O(log N)）");
        System.out.println("- 叶子节点用链表连接（适合范围查询）\n");
        
        System.out.println("【3.2 聚簇索引 vs 二级索引】\n");
        
        System.out.println("聚簇索引（主键索引）：");
        System.out.println("- 叶子节点存储完整数据行");
        System.out.println("- 一张表只有一个聚簇索引");
        System.out.println("- InnoDB 的主键就是聚簇索引\n");
        
        System.out.println("二级索引（辅助索引）：");
        System.out.println("- 叶子节点存储主键值 + 索引列");
        System.out.println("- 需要回表查询（先查主键，再查聚簇索引）");
        System.out.println("- 可以有多个二级索引\n");
        
        System.out.println("【3.3 索引分类】\n");
        System.out.println("┌─────────────────┬────────────────────────────────────────────────────┐");
        System.out.println("│ 分类维度        │ 类型                                               │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│ 数据结构        │ B+Tree 索引、Hash 索引、Full-Text 全文索引            │");
        System.out.println("│ 物理存储        │ 聚簇索引、二级索引                                  │");
        System.out.println("│ 字段数量        │ 单列索引、联合索引（最左匹配原则）                  │");
        System.out.println("│ 唯一性          │ 唯一索引、普通索引                                  │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────┘\n");
        
        System.out.println("【3.4 创建索引的 SQL 示例】\n");
        System.out.println("-- 创建主键索引（自动创建聚簇索引）");
        System.out.println("ALTER TABLE users ADD PRIMARY KEY (id);");
        System.out.println("");
        System.out.println("-- 创建唯一索引");
        System.out.println("CREATE UNIQUE INDEX idx_email ON users(email);");
        System.out.println("");
        System.out.println("-- 创建普通索引");
        System.out.println("CREATE INDEX idx_name ON users(name);");
        System.out.println("");
        System.out.println("-- 创建联合索引（最左匹配原则）");
        System.out.println("CREATE INDEX idx_name_age ON users(name, age);\n");
        
        System.out.println("【3.5 索引失效场景】\n");
        System.out.println("1. 模糊查询以%开头：LIKE '%abc'");
        System.out.println("2. 函数操作：WHERE YEAR(create_time) = 2024");
        System.out.println("3. 类型转换：WHERE phone = 13800138000（phone 是字符串）");
        System.out.println("4. OR 连接条件：OR 两边有一边没索引");
        System.out.println("5. 违反最左匹配：联合索引 (a,b,c)，查询 b 或 c");
        System.out.println("6. IS NULL/IS NOT NULL（可能失效）");
        System.out.println("7. != 或 <>（可能全表扫描）\n");
    }
    
    // ==================== 第四部分：日志系统 ====================
    
    private static void demonstrateLogSystem() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第四部分：MySQL 日志系统                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【4.1 Redo Log（重做日志）】\n");
        System.out.println("作用：保证事务持久性，崩溃后恢复");
        System.out.println("特点：");
        System.out.println("- InnoDB 特有");
        System.out.println("- 循环写入（空间固定）");
        System.out.println("- 顺序写，性能高");
        System.out.println("- 记录的是物理日志（数据页修改后的值）");
        System.out.println("大小：由 innodb_log_file_size 决定\n");
        
        System.out.println("【4.2 Undo Log（回滚日志）】\n");
        System.out.println("作用：保证事务原子性，支持回滚");
        System.out.println("特点：");
        System.out.println("- 记录逻辑操作（如 INSERT 对应 DELETE）");
        System.out.println("- 支持 MVCC（多版本并发控制）");
        System.out.println("- 事务提交后不会立即删除（用于快照读）");
        System.out.println("- 可以清理（由 purge 线程负责）\n");
        
        System.out.println("【4.3 Binlog（归档日志）】\n");
        System.out.println("作用：记录所有 DDL 和 DML，用于主从复制和数据恢复");
        System.out.println("特点：");
        System.out.println("- Server 层实现，所有引擎都有");
        System.out.println("- 追加写入（不会循环）");
        System.out.println("- 记录的是 SQL 语句或行变更");
        System.out.println("格式：");
        System.out.println("- STATEMENT：记录原始 SQL（可能有风险）");
        System.out.println("- ROW：记录行变更（更可靠）");
        System.out.println("- MIXED：混合模式（默认）\n");
        
        System.out.println("【4.4 Redo Log vs Binlog】\n");
        System.out.println("┌──────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│ 对比项       │ Redo Log          │ Binlog              │");
        System.out.println("├──────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│ 所属层级     │ InnoDB 引擎层        │ Server 层            │");
        System.out.println("│ 写入方式     │ 循环写入（空间固定） │ 追加写入（文件递增） │");
        System.out.println("│ 记录内容     │ 物理日志（数据页）   │ 逻辑日志（SQL/行）  │");
        System.out.println("│ 主要作用     │ 崩溃恢复            │ 主从复制、数据恢复  │");
        System.out.println("│ 写入时机     │ 事务进行中          │ 事务提交后          │");
        System.out.println("└──────────────┴─────────────────────┴─────────────────────┘\n");
        
        System.out.println("【4.5 两阶段提交】\n");
        System.out.println("目的：保证 Redo Log 和 Binlog 的一致性");
        System.out.println("流程：");
        System.out.println("1. Prepare 阶段：写入 Redo Log，标记为 prepare 状态");
        System.out.println("2. 写入 Binlog");
        System.out.println("3. Commit 阶段：提交事务，标记 Redo Log 为 commit 状态\n");
        
        System.out.println("【4.6 Slow Query Log（慢查询日志）】\n");
        System.out.println("作用：记录执行时间超过阈值的 SQL");
        System.out.println("配置：");
        System.out.println("- slow_query_log：是否开启");
        System.out.println("- long_query_time：阈值（默认 10 秒）");
        System.out.println("- log_queries_not_using_indexes：记录未使用索引的查询\n");
    }
    
    // ==================== 第五部分：事务特性 ====================
    
    private static void demonstrateTransaction() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第五部分：MySQL事务特性                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【5.1 ACID 四大特性】\n");
        
        System.out.println("原子性（Atomicity）：");
        System.out.println("- 事务是不可分割的最小单位");
        System.out.println("- 要么全部成功，要么全部失败");
        System.out.println("- Undo Log 保证原子性\n");
        
        System.out.println("一致性（Consistency）：");
        System.out.println("- 事务前后数据保持一致");
        System.out.println("- 包括约束一致（主键、外键、唯一等）");
        System.out.println("- 业务逻辑一致（如转账总额不变）\n");
        
        System.out.println("隔离性（Isolation）：");
        System.out.println("- 多个事务互不干扰");
        System.out.println("- 通过锁和 MVCC 实现");
        System.out.println("- 不同隔离级别解决不同并发问题\n");
        
        System.out.println("持久性（Durability）：");
        System.out.println("- 事务一旦提交，永久生效");
        System.out.println("- 即使宕机也能恢复");
        System.out.println("- Redo Log 保证持久性\n");
        
        System.out.println("【5.2 并发一致性问题】\n");
        
        System.out.println("脏读（Dirty Read）：");
        System.out.println("- 读到其他事务未提交的数据");
        System.out.println("- 解决：READ COMMITTED 及以上级别\n");
        
        System.out.println("不可重复读（Non-Repeatable Read）：");
        System.out.println("- 同一事务内，多次读取结果不一致");
        System.out.println("- 原因：其他事务修改并提交");
        System.out.println("- 解决：REPEATABLE READ 及以上级别\n");
        
        System.out.println("幻读（Phantom Read）：");
        System.out.println("- 同一事务内，多次查询记录数不一致");
        System.out.println("- 原因：其他事务插入或删除");
        System.out.println("- 解决：SERIALIZABLE 级别或 Next-Key Lock\n");
        
        System.out.println("【5.3 四种隔离级别】\n");
        System.out.println("┌──────────────────┬──────────┬──────────┬──────────┬──────────┐");
        System.out.println("│ 隔离级别         │ 脏读     │ 不可重复读│ 幻读     │ 性能     │");
        System.out.println("├──────────────────┼──────────┼──────────┼──────────┼──────────┤");
        System.out.println("│ READ UNCOMMITTED │ 可能     │ 可能     │ 可能     │ 最高     │");
        System.out.println("│ READ COMMITTED   │ 避免     │ 可能     │ 可能     │ 高       │");
        System.out.println("│ REPEATABLE READ  │ 避免     │ 避免     │ 可能*    │ 中       │");
        System.out.println("│ SERIALIZABLE     │ 避免     │ 避免     │ 避免     │ 最低     │");
        System.out.println("└──────────────────┴──────────┴──────────┴──────────┴──────────┘");
        System.out.println("* MySQL 的 REPEATABLE READ 通过 MVCC+Next-Key Lock 基本避免幻读\n");
        
        System.out.println("【5.4 MVCC（多版本并发控制）】\n");
        System.out.println("原理：");
        System.out.println("- 每行数据有隐藏列（事务 ID、回滚指针、删除标志）");
        System.out.println("- 通过 Undo Log 构建历史版本链");
        System.out.println("- Read View（读视图）判断可见性\n");
        
        System.out.println("Read View 规则：");
        System.out.println("1. 未提交的事务，对当前事务不可见");
        System.out.println("2. 已提交的事务：");
        System.out.println("   - RC 级别：每次 SELECT 都生成新 Read View");
        System.out.println("   - RR 级别：第一次 SELECT 生成 Read View，后续复用\n");
    }
    
    // ==================== 第六部分：锁机制 ====================
    
    private static void demonstrateLockMechanism() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第六部分：MySQL 锁机制                                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【6.1 锁的粒度分类】\n");
        
        System.out.println("表级锁（Table-Level Lock）：");
        System.out.println("- 锁定整张表，开销小");
        System.out.println("- 容易冲突，并发度低");
        System.out.println("- MyISAM 使用\n");
        
        System.out.println("行级锁（Row-Level Lock）：");
        System.out.println("- 只锁定某一行，开销大");
        System.out.println("- 不易冲突，并发度高");
        System.out.println("- InnoDB 使用\n");
        
        System.out.println("页级锁（Page-Level Lock）：");
        System.out.println("- 锁定一页数据（介于表和行之间）");
        System.out.println("- Berkeley DB 使用\n");
        
        System.out.println("【6.2 InnoDB 行锁算法】\n");
        
        System.out.println("Record Lock（记录锁）：");
        System.out.println("- 锁定单条记录");
        System.out.println("- 基于索引实现（必须走索引）\n");
        
        System.out.println("Gap Lock（间隙锁）：");
        System.out.println("- 锁定记录之间的间隙");
        System.out.println("- 防止其他事务插入");
        System.out.println("- 解决幻读问题\n");
        
        System.out.println("Next-Key Lock（临键锁）：");
        System.out.println("- Record Lock + Gap Lock");
        System.out.println("- 锁定记录 + 间隙");
        System.out.println("- RR 级别下默认使用\n");
        
        System.out.println("【6.3 共享锁 vs 排他锁】\n");
        
        System.out.println("共享锁（S 锁，读锁）：");
        System.out.println("- 允许其他事务加 S 锁");
        System.out.println("- 不允许加 X 锁");
        System.out.println("- SELECT ... LOCK IN SHARE MODE\n");
        
        System.out.println("排他锁（X 锁，写锁）：");
        System.out.println("- 不允许其他事务加任何锁");
        System.out.println("- INSERT、UPDATE、DELETE 自动加 X 锁");
        System.out.println("- SELECT ... FOR UPDATE\n");
        
        System.out.println("【6.4 死锁产生与解决】\n");
        System.out.println("产生条件：");
        System.out.println("1. 两个事务互相持有对方需要的锁");
        System.out.println("2. 形成循环等待\n");
        
        System.out.println("解决方案：");
        System.out.println("1. 设置超时：innodb_lock_wait_timeout");
        System.out.println("2. 主动回滚：选择代价小的事务回滚");
        System.out.println("3. 预防：按固定顺序访问资源\n");
        
        System.out.println("查看锁信息：");
        System.out.println("SHOW ENGINE INNODB STATUS; -- 查看死锁信息");
        System.out.println("SELECT * FROM information_schema.INNODB_TRX; -- 查看运行中的事务");
        System.out.println("SELECT * FROM information_schema.INNODB_LOCKS; -- 查看锁信息\n");
    }
    
    // ==================== 第七部分：性能优化 ====================
    
    private static void demonstratePerformanceOptimization() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第七部分：MySQL 性能优化                                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("【7.1 EXPLAIN 分析 SQL】\n");
        System.out.println("EXPLAIN SELECT * FROM users WHERE id = 1;\n");
        
        System.out.println("关键字段说明：");
        System.out.println("┌─────────────────┬────────────────────────────────────────────────────┐");
        System.out.println("│ 字段            │ 说明                                               │");
        System.out.println("├─────────────────┼────────────────────────────────────────────────────┤");
        System.out.println("│ type            │ 访问类型（system > const > eq_ref > ref > range    │");
        System.out.println("│                 │ > index > ALL）ALL 最差（全表扫描）                │");
        System.out.println("│ possible_keys   │ 可能使用的索引                                     │");
        System.out.println("│ key             │ 实际使用的索引                                     │");
        System.out.println("│ rows            │ 扫描的行数（越少越好）                              │");
        System.out.println("│ Extra           │ 额外信息（Using index 好，Using filesort 差）       │");
        System.out.println("└─────────────────┴────────────────────────────────────────────────────┘\n");
        
        System.out.println("【7.2 索引优化策略】\n");
        System.out.println("1. 为 WHERE、ORDER BY、GROUP BY 创建索引");
        System.out.println("2. 使用覆盖索引（避免回表）");
        System.out.println("3. 联合索引遵循最左匹配原则");
        System.out.println("4. 避免索引失效（如函数操作、类型转换）");
        System.out.println("5. 前缀索引（针对长字符串）");
        System.out.println("6. 定期清理无用索引\n");
        
        System.out.println("【7.3 SQL 语句优化】\n");
        System.out.println("推荐：");
        System.out.println("✓ SELECT 指定字段，不用 SELECT *");
        System.out.println("✓ 用小表驱动大表（EXISTS vs IN）");
        System.out.println("✓ UNION ALL 代替 UNION（不去重）");
        System.out.println("✓ 批量操作代替单条操作");
        System.out.println("✓ LIMIT 分页优化（延迟关联）\n");
        
        System.out.println("避免：");
        System.out.println("✗ SELECT *（增加网络传输）");
        System.out.println("✗ LIKE '%xxx'（无法用索引）");
        System.out.println("✗ OR 连接（可能全表扫描）");
        System.out.println("✗ 函数操作（导致索引失效）");
        System.out.println("✗ 深度分页（LIMIT 100000,10）\n");
        
        System.out.println("【7.4 表结构优化】\n");
        System.out.println("1. 垂直拆分：大字段单独拆表");
        System.out.println("2. 水平分区：按时间/地区分表");
        System.out.println("3. 适当冗余：减少 JOIN（以空间换时间）");
        System.out.println("4. 选择合适引擎：InnoDB/MyISAM/Memory\n");
        
        System.out.println("【7.5 配置优化】\n");
        System.out.println("关键参数：");
        System.out.println("- innodb_buffer_pool_size：缓存池大小（物理内存 50%-70%）");
        System.out.println("- innodb_log_file_size：Redo Log 大小");
        System.out.println("- max_connections：最大连接数");
        System.out.println("- query_cache_size：查询缓存（8.0 已移除）\n");
    }
    
    // ==================== 第八部分：高频面试题 ====================
    
    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                 第八部分：高频面试题                                     ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("==================== 字段类型与存储引擎 ====================\n");
        
        System.out.println("【问题 1】VARCHAR(50) 的 50 表示什么？");
        System.out.println("答：");
        System.out.println("- 表示最多存储 50 个字符");
        System.out.println("- 实际占用空间 = 字符数 + 长度标识（1-2 字节）");
        System.out.println("- UTF8 编码下，一个汉字占 3 个字节\n");
        
        System.out.println("【问题 2】CHAR 和 VARCHAR 的区别？");
        System.out.println("答：");
        System.out.println("- CHAR：固定长度，不足补空格，适合定长数据");
        System.out.println("- VARCHAR：可变长度，节省空间，适合变长数据");
        System.out.println("- CHAR 查询效率高（无需计算长度）\n");
        
        System.out.println("【问题 3】InnoDB 和 MyISAM 的区别？");
        System.out.println("答：");
        System.out.println("1. InnoDB 支持事务，MyISAM 不支持");
        System.out.println("2. InnoDB 支持行级锁，MyISAM 只支持表级锁");
        System.out.println("3. InnoDB 支持外键，MyISAM 不支持");
        System.out.println("4. InnoDB 支持 MVCC，MyISAM 不支持");
        System.out.println("5. InnoDB 聚簇索引，MyISAM 非聚簇索引\n");
        
        System.out.println("==================== 索引相关 ====================\n");
        
        System.out.println("【问题 4】为什么 InnoDB 使用 B+Tree 作为索引？");
        System.out.println("答：");
        System.out.println("1. 非叶子节点只存索引，容纳更多索引项");
        System.out.println("2. 树高度低（通常 3-4 层），查询稳定 O(log N)");
        System.out.println("3. 叶子节点存数据 + 链表连接，适合范围查询");
        System.out.println("4. 相比二叉树，减少 IO 次数\n");
        
        System.out.println("【问题 5】聚簇索引和二级索引的区别？");
        System.out.println("答：");
        System.out.println("- 聚簇索引：叶子节点存完整数据，一张表只有一个");
        System.out.println("- 二级索引：叶子节点存主键值 + 索引列，需要回表");
        System.out.println("- 覆盖索引：二级索引包含所有查询字段，无需回表\n");
        
        System.out.println("【问题 6】什么是最左匹配原则？");
        System.out.println("答：");
        System.out.println("- 联合索引 (a,b,c)，查询时必须从最左开始匹配");
        System.out.println("- 例如：WHERE a=1 AND b=2 可以用索引");
        System.out.println("- WHERE b=2 或 WHERE c=3 无法使用索引\n");
        
        System.out.println("==================== 事务与日志 ====================\n");
        
        System.out.println("【问题 7】MySQL 如何保证事务的 ACID？");
        System.out.println("答：");
        System.out.println("- 原子性：Undo Log 保证，失败可回滚");
        System.out.println("- 一致性：通过原子性、隔离性、持久性共同保证");
        System.out.println("- 隔离性：锁 +MVCC 实现不同隔离级别");
        System.out.println("- 持久性：Redo Log 保证，提交后永久保存\n");
        
        System.out.println("【问题 8】Redo Log 和 Binlog 的区别？");
        System.out.println("答：");
        System.out.println("1. 所属层级：Redo Log 是 InnoDB 引擎层，Binlog 是 Server 层");
        System.out.println("2. 写入方式：Redo Log 循环写，Binlog 追加写");
        System.out.println("3. 记录内容：Redo Log 物理日志，Binlog 逻辑日志");
        System.out.println("4. 作用：Redo Log 崩溃恢复，Binlog 主从复制\n");
        
        System.out.println("【问题 9】什么是两阶段提交？为什么需要？");
        System.out.println("答：");
        System.out.println("两阶段：Prepare（写 Redo Log）→ Commit（写 Binlog 后提交）");
        System.out.println("目的：保证 Redo Log 和 Binlog 的一致性");
        System.out.println("避免：主从复制时数据不一致\n");
        
        System.out.println("==================== 锁与并发 ====================\n");
        
        System.out.println("【问题 10】MySQL 有哪些隔离级别？分别解决什么问题？");
        System.out.println("答：");
        System.out.println("1. READ UNCOMMITTED：未提交读，性能最高，但有脏读");
        System.out.println("2. READ COMMITTED：提交后读，避免脏读，但有不可重复读");
        System.out.println("3. REPEATABLE READ：可重复读（MySQL 默认），避免不可重复读");
        System.out.println("4. SERIALIZABLE：串行化，完全隔离，性能最低\n");
        
        System.out.println("【问题 11】什么是 MVCC？如何实现？");
        System.out.println("答：");
        System.out.println("MVCC：多版本并发控制，实现非阻塞读");
        System.out.println("实现原理：");
        System.out.println("1. 隐藏列（事务 ID、回滚指针、删除标志）");
        System.out.println("2. Undo Log 构建版本链");
        System.out.println("3. Read View 判断可见性\n");
        
        System.out.println("【问题 12】InnoDB 的行锁算法有哪些？");
        System.out.println("答：");
        System.out.println("1. Record Lock：锁定记录（基于索引）");
        System.out.println("2. Gap Lock：锁定间隙（防止插入）");
        System.out.println("3. Next-Key Lock：Record+Gap（RR 级别默认）\n");
        
        System.out.println("==================== 性能优化 ====================\n");
        
        System.out.println("【问题 13】SQL 优化有哪些常见手段？");
        System.out.println("答：");
        System.out.println("1. 使用覆盖索引，避免回表");
        System.out.println("2. SELECT 指定字段，不用 SELECT *");
        System.out.println("3. 避免索引失效（函数、类型转换、LIKE'%xxx'）");
        System.out.println("4. 小表驱动大表（EXISTS vs IN）");
        System.out.println("5. LIMIT 分页优化（延迟关联）\n");
        
        System.out.println("【问题 14】如何分析一条 SQL 的性能？");
        System.out.println("答：");
        System.out.println("1. 开启慢查询日志，定位慢 SQL");
        System.out.println("2. 使用 EXPLAIN 分析执行计划");
        System.out.println("3. 查看 type（是否全表扫描）");
        System.out.println("4. 查看 key（是否用到索引）");
        System.out.println("5. 查看 rows（扫描行数）");
        System.out.println("6. 查看 Extra（Using filesort 需优化）\n");
        
        System.out.println("【问题 15】深分页如何优化？");
        System.out.println("答：");
        System.out.println("问题：LIMIT 100000,10 需要扫描前 100000 条");
        System.out.println("优化方案：");
        System.out.println("1. 延迟关联：先查 ID，再 JOIN 原表");
        System.out.println("2. 游标分页：WHERE id > last_id LIMIT 10");
        System.out.println("3. 禁止跳页：限制最大页码\n");
        
        System.out.println("==========================================================================\n");
    }
}
