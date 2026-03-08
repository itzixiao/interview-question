package cn.itzixiao.interview.mysql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 大表优化与批量处理详解
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │                           大表优化与批量处理体系                               │
 * ├──────────────────────────────────────────────────────────────────────────────┤
 * │                                                                              │
 * │    ┌─────────────┐        ┌─────────────┐        ┌─────────────┐             │
 * │    │   慢查询    │        │   大表      │        │   批量      │             │
 * │    │   优化      │        │   优化      │        │   处理      │             │
 * │    └──────┬──────┘        └──────┬──────┘        └──────┬──────┘             │
 * │           │                      │                      │                    │
 * │    ┌──────▼──────┐        ┌──────▼──────┐        ┌──────▼──────┐             │
 * │    │ EXPLAIN分析 │        │ 分库分表    │        │ 批量插入    │             │
 * │    │ 索引优化    │        │ 数据归档    │        │ 分批处理    │             │
 * │    │ SQL改写     │        │ 分区表      │        │ 游标遍历    │             │
 * │    └─────────────┘        └─────────────┘        └─────────────┘             │
 * │                                                                              │
 * └──────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author itzixiao
 */
public class BigTableOptimizationDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                   MySQL 大表优化与批量处理详解                            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");

        // 第一部分：慢查询分析与优化
        demonstrateSlowQueryOptimization();

        // 第二部分：EXPLAIN执行计划详解
        demonstrateExplainAnalysis();

        // 第三部分：大表优化策略
        demonstrateBigTableOptimization();

        // 第四部分：批量处理最佳实践
        demonstrateBatchProcessing();

        // 第五部分：高频面试题
        printInterviewQuestions();
    }

    // ==================== 第一部分：慢查询分析与优化 ====================

    /**
     * 慢查询分析与优化
     */
    private static void demonstrateSlowQueryOptimization() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第一部分：慢查询分析与优化                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【开启慢查询日志】\n");

        System.out.println("-- 查看是否开启");
        System.out.println("SHOW VARIABLES LIKE 'slow_query_log';");
        System.out.println("");
        System.out.println("-- 开启慢查询日志");
        System.out.println("SET GLOBAL slow_query_log = 'ON';");
        System.out.println("");
        System.out.println("-- 设置慢查询阈值（秒）");
        System.out.println("SET GLOBAL long_query_time = 2;");
        System.out.println("");
        System.out.println("-- 记录没有使用索引的查询");
        System.out.println("SET GLOBAL log_queries_not_using_indexes = 'ON';");
        System.out.println("");
        System.out.println("-- 查看慢查询日志位置");
        System.out.println("SHOW VARIABLES LIKE 'slow_query_log_file';\n");

        System.out.println("【慢查询分析工具】\n");

        System.out.println("1. mysqldumpslow（MySQL自带）");
        System.out.println("   # 查看最慢的10条SQL");
        System.out.println("   mysqldumpslow -s t -t 10 /var/lib/mysql/slow.log");
        System.out.println("");
        System.out.println("   # 参数说明：");
        System.out.println("   # -s: 排序方式（t=时间，c=次数，l=锁时间，r=返回行数）");
        System.out.println("   # -t: 显示条数\n");

        System.out.println("2. pt-query-digest（Percona Toolkit）");
        System.out.println("   pt-query-digest /var/lib/mysql/slow.log");
        System.out.println("   - 提供详细的统计分析");
        System.out.println("   - 按查询模式聚合");
        System.out.println("   - 生成可视化报告\n");

        System.out.println("【常见慢查询原因】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 索引问题                                                            │");
        System.out.println("│     - 缺少索引 → 全表扫描                                               │");
        System.out.println("│     - 索引失效 → 函数、类型转换、前导模糊等                             │");
        System.out.println("│     - 索引选择性低 → 优化器选择全表扫描                                 │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. SQL写法问题                                                         │");
        System.out.println("│     - SELECT * → 返回不必要的列                                         │");
        System.out.println("│     - 深分页 → LIMIT 1000000, 10                                        │");
        System.out.println("│     - 子查询 → 可能无法使用索引                                         │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. 数据量问题                                                          │");
        System.out.println("│     - 单表数据量过大                                                    │");
        System.out.println("│     - 热点数据分布不均                                                  │");
        System.out.println("├─────────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  4. 锁竞争                                                              │");
        System.out.println("│     - 长事务持有锁                                                      │");
        System.out.println("│     - 表锁导致阻塞                                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第二部分：EXPLAIN执行计划详解 ====================

    /**
     * EXPLAIN执行计划详解
     */
    private static void demonstrateExplainAnalysis() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第二部分：EXPLAIN执行计划详解                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【EXPLAIN使用方法】\n");

        System.out.println("EXPLAIN SELECT * FROM user WHERE id = 1;");
        System.out.println("EXPLAIN FORMAT=JSON SELECT * FROM user WHERE id = 1;  -- 详细JSON格式");
        System.out.println("EXPLAIN ANALYZE SELECT * FROM user WHERE id = 1;      -- MySQL 8.0+ 真实执行\n");

        System.out.println("【EXPLAIN关键字段详解】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  id - 查询序号                                                          │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  - id相同：从上到下顺序执行                                              │");
        System.out.println("│  - id不同：id越大优先级越高，越先执行                                    │");
        System.out.println("│  - id为NULL：表示结果集，不需要执行                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  select_type - 查询类型                                                 │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  SIMPLE       - 简单查询（不包含子查询或UNION）                         │");
        System.out.println("│  PRIMARY      - 最外层查询                                              │");
        System.out.println("│  SUBQUERY     - SELECT子句中的子查询                                    │");
        System.out.println("│  DERIVED      - FROM子句中的子查询（派生表）                            │");
        System.out.println("│  UNION        - UNION中第二个及之后的SELECT                             │");
        System.out.println("│  UNION RESULT - UNION的结果集                                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  type - 访问类型（重要！）                                               │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  性能从好到差排序：                                                      │");
        System.out.println("│                                                                        │");
        System.out.println("│  system      - 系统表，只有一行数据                                      │");
        System.out.println("│  const       - 主键或唯一索引等值查询，最多匹配一行                      │");
        System.out.println("│               示例：SELECT * FROM user WHERE id = 1                     │");
        System.out.println("│                                                                        │");
        System.out.println("│  eq_ref      - JOIN时使用主键或唯一索引                                  │");
        System.out.println("│               示例：SELECT * FROM t1 JOIN t2 ON t1.id = t2.user_id      │");
        System.out.println("│                                                                        │");
        System.out.println("│  ref         - 非唯一索引等值查询                                        │");
        System.out.println("│               示例：SELECT * FROM user WHERE name = 'Tom'               │");
        System.out.println("│                                                                        │");
        System.out.println("│  range       - 索引范围扫描（BETWEEN、IN、>、<）                        │");
        System.out.println("│               示例：SELECT * FROM user WHERE age BETWEEN 20 AND 30     │");
        System.out.println("│                                                                        │");
        System.out.println("│  index       - 全索引扫描（遍历整个索引树）                              │");
        System.out.println("│               示例：SELECT id FROM user                                 │");
        System.out.println("│                                                                        │");
        System.out.println("│  ALL         - 全表扫描（最差，要避免！）                                │");
        System.out.println("│               示例：SELECT * FROM user WHERE status = 1 (无索引)        │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  key_len - 索引使用长度                                                  │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  用于判断组合索引用了哪些列：                                            │");
        System.out.println("│  - INT: 4字节                                                           │");
        System.out.println("│  - BIGINT: 8字节                                                        │");
        System.out.println("│  - VARCHAR(n): 3*n + 2 (utf8mb4编码)                                    │");
        System.out.println("│  - 可为NULL的列：+1字节                                                  │");
        System.out.println("│                                                                        │");
        System.out.println("│  示例：索引 (a INT, b VARCHAR(10))                                      │");
        System.out.println("│  - 只用a: key_len = 4                                                   │");
        System.out.println("│  - 用a和b: key_len = 4 + 3*10 + 2 = 36                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Extra - 额外信息（重要！）                                              │");
        System.out.println("│  ──────────────────────────────────────────────────────────────────────│");
        System.out.println("│  Using index          - 覆盖索引，不需要回表 ✓                          │");
        System.out.println("│  Using where          - 存储引擎返回后在Server层过滤                    │");
        System.out.println("│  Using index condition- 索引下推（ICP），在存储引擎层过滤 ✓             │");
        System.out.println("│  Using temporary      - 使用临时表（GROUP BY/DISTINCT）警告！           │");
        System.out.println("│  Using filesort       - 使用文件排序（ORDER BY无索引）警告！            │");
        System.out.println("│  Select tables optimized away - 优化器直接从索引获取结果                │");
        System.out.println("└─────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【优化目标】");
        System.out.println("1. type 至少达到 range，最好是 ref 或 const");
        System.out.println("2. Extra 避免 Using filesort 和 Using temporary");
        System.out.println("3. Extra 出现 Using index 表示用了覆盖索引\n");
    }

    // ==================== 第三部分：大表优化策略 ====================

    /**
     * 大表优化策略
     */
    private static void demonstrateBigTableOptimization() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第三部分：大表优化策略                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【单表数据量预警】");
        System.out.println("- 500万行以内：通常无需特殊处理");
        System.out.println("- 500万-2000万：需要关注索引优化和SQL优化");
        System.out.println("- 2000万以上：考虑分库分表或归档\n");

        System.out.println("【优化策略一：分页查询优化】\n");

        System.out.println("问题SQL：");
        System.out.println("SELECT * FROM orders ORDER BY create_time DESC LIMIT 1000000, 10;");
        System.out.println("-- 需要扫描1000010行，丢弃前100万行\n");

        System.out.println("优化方案1：覆盖索引 + 延迟关联");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  SELECT o.*                                                           │");
        System.out.println("│  FROM orders o                                                        │");
        System.out.println("│  INNER JOIN (                                                         │");
        System.out.println("│      SELECT id FROM orders                                            │");
        System.out.println("│      ORDER BY create_time DESC                                        │");
        System.out.println("│      LIMIT 1000000, 10                                                │");
        System.out.println("│  ) tmp ON o.id = tmp.id;                                              │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 子查询只查id，使用覆盖索引，减少IO                                  │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("优化方案2：游标分页（记住上次位置）");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  -- 第一页                                                             │");
        System.out.println("│  SELECT * FROM orders ORDER BY id DESC LIMIT 10;                      │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 下一页（记住上页最后一条的id=100）                                 │");
        System.out.println("│  SELECT * FROM orders                                                 │");
        System.out.println("│  WHERE id < 100                                                       │");
        System.out.println("│  ORDER BY id DESC LIMIT 10;                                           │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 始终是索引范围扫描，不随页数增加变慢                               │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【优化策略二：数据归档】\n");

        System.out.println("冷热数据分离：");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  热数据表：orders（最近3个月数据）                                      │");
        System.out.println("│  冷数据表：orders_archive（3个月前数据）                                │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 定期归档（每天凌晨）                                               │");
        System.out.println("│  INSERT INTO orders_archive                                           │");
        System.out.println("│  SELECT * FROM orders                                                 │");
        System.out.println("│  WHERE create_time < DATE_SUB(NOW(), INTERVAL 3 MONTH);               │");
        System.out.println("│                                                                       │");
        System.out.println("│  DELETE FROM orders                                                   │");
        System.out.println("│  WHERE create_time < DATE_SUB(NOW(), INTERVAL 3 MONTH)                │");
        System.out.println("│  LIMIT 10000;  -- 分批删除，避免长事务                                 │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【优化策略三：分区表】\n");

        System.out.println("按时间范围分区：");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  CREATE TABLE orders (                                                │");
        System.out.println("│      id BIGINT PRIMARY KEY,                                           │");
        System.out.println("│      order_no VARCHAR(32),                                            │");
        System.out.println("│      create_time DATETIME                                             │");
        System.out.println("│  ) PARTITION BY RANGE (TO_DAYS(create_time)) (                        │");
        System.out.println("│      PARTITION p202401 VALUES LESS THAN (TO_DAYS('2024-02-01')),      │");
        System.out.println("│      PARTITION p202402 VALUES LESS THAN (TO_DAYS('2024-03-01')),      │");
        System.out.println("│      PARTITION p202403 VALUES LESS THAN (TO_DAYS('2024-04-01')),      │");
        System.out.println("│      PARTITION pmax VALUES LESS THAN MAXVALUE                         │");
        System.out.println("│  );                                                                   │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 查询时自动分区裁剪                                                 │");
        System.out.println("│  SELECT * FROM orders WHERE create_time >= '2024-02-01';              │");
        System.out.println("│  -- 只扫描 p202402 及之后的分区                                        │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【优化策略四：分库分表】\n");

        System.out.println("水平分表（按用户ID取模）：");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  orders_0  orders_1  orders_2  orders_3                               │");
        System.out.println("│                                                                       │");
        System.out.println("│  分片规则：user_id % 4                                                 │");
        System.out.println("│  - user_id=1 → orders_1                                               │");
        System.out.println("│  - user_id=8 → orders_0                                               │");
        System.out.println("│                                                                       │");
        System.out.println("│  中间件选型：                                                          │");
        System.out.println("│  - ShardingSphere（推荐）                                              │");
        System.out.println("│  - MyCat                                                              │");
        System.out.println("│  - TDDL                                                               │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第四部分：批量处理最佳实践 ====================

    /**
     * 批量处理最佳实践
     */
    private static void demonstrateBatchProcessing() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第四部分：批量处理最佳实践                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("【批量插入优化】\n");

        System.out.println("方式1：多值INSERT（推荐）");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  -- 低效：多次单条插入                                                 │");
        System.out.println("│  INSERT INTO user (name, age) VALUES ('Tom', 20);                     │");
        System.out.println("│  INSERT INTO user (name, age) VALUES ('Jerry', 22);                   │");
        System.out.println("│  INSERT INTO user (name, age) VALUES ('Bob', 25);                     │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 高效：批量插入                                                     │");
        System.out.println("│  INSERT INTO user (name, age) VALUES                                  │");
        System.out.println("│      ('Tom', 20), ('Jerry', 22), ('Bob', 25);                         │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 性能提升原因：                                                     │");
        System.out.println("│  1. 减少网络往返                                                       │");
        System.out.println("│  2. 减少SQL解析次数                                                    │");
        System.out.println("│  3. 减少事务提交次数                                                   │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("方式2：JDBC批量插入");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 1. 开启批量重写                                                    │");
        System.out.println("│  String url = \"jdbc:mysql://localhost:3306/test\"                     │");
        System.out.println("│              + \"?rewriteBatchedStatements=true\";                     │");
        System.out.println("│                                                                       │");
        System.out.println("│  // 2. 使用 addBatch + executeBatch                                   │");
        System.out.println("│  String sql = \"INSERT INTO user (name, age) VALUES (?, ?)\";          │");
        System.out.println("│  PreparedStatement ps = conn.prepareStatement(sql);                   │");
        System.out.println("│  for (User user : users) {                                            │");
        System.out.println("│      ps.setString(1, user.getName());                                 │");
        System.out.println("│      ps.setInt(2, user.getAge());                                     │");
        System.out.println("│      ps.addBatch();                                                   │");
        System.out.println("│      if (++count % 1000 == 0) {  // 每1000条提交一次                  │");
        System.out.println("│          ps.executeBatch();                                           │");
        System.out.println("│          ps.clearBatch();                                             │");
        System.out.println("│      }                                                                │");
        System.out.println("│  }                                                                    │");
        System.out.println("│  ps.executeBatch();  // 提交剩余的                                    │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【批量更新优化】\n");

        System.out.println("方式1：CASE WHEN 批量更新");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  UPDATE user SET status = CASE id                                     │");
        System.out.println("│      WHEN 1 THEN 0                                                    │");
        System.out.println("│      WHEN 2 THEN 1                                                    │");
        System.out.println("│      WHEN 3 THEN 0                                                    │");
        System.out.println("│  END                                                                  │");
        System.out.println("│  WHERE id IN (1, 2, 3);                                               │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("方式2：临时表JOIN更新");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  -- 创建临时表存放要更新的数据                                         │");
        System.out.println("│  CREATE TEMPORARY TABLE tmp_update (id BIGINT, status INT);           │");
        System.out.println("│  INSERT INTO tmp_update VALUES (1, 0), (2, 1), (3, 0);                │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 使用JOIN更新                                                      │");
        System.out.println("│  UPDATE user u                                                        │");
        System.out.println("│  INNER JOIN tmp_update t ON u.id = t.id                               │");
        System.out.println("│  SET u.status = t.status;                                             │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【批量删除优化】\n");

        System.out.println("原则：分批删除，避免长事务和锁表");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  -- 错误：一次性删除大量数据                                           │");
        System.out.println("│  DELETE FROM orders WHERE create_time < '2024-01-01';                 │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 正确：分批删除                                                     │");
        System.out.println("│  REPEAT                                                               │");
        System.out.println("│      DELETE FROM orders                                               │");
        System.out.println("│      WHERE create_time < '2024-01-01'                                 │");
        System.out.println("│      LIMIT 10000;                                                     │");
        System.out.println("│  UNTIL ROW_COUNT() = 0 END REPEAT;                                    │");
        System.out.println("│                                                                       │");
        System.out.println("│  -- 或使用Java循环                                                     │");
        System.out.println("│  int deletedRows;                                                     │");
        System.out.println("│  do {                                                                 │");
        System.out.println("│      deletedRows = jdbcTemplate.update(                               │");
        System.out.println("│          \"DELETE FROM orders WHERE create_time < ? LIMIT 10000\",     │");
        System.out.println("│          deadline);                                                   │");
        System.out.println("│      Thread.sleep(100);  // 适当间隔，避免影响业务                    │");
        System.out.println("│  } while (deletedRows > 0);                                           │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【MyBatis批量操作】\n");

        System.out.println("XML方式批量插入：");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  <insert id=\"batchInsert\">                                            │");
        System.out.println("│      INSERT INTO user (name, age, status) VALUES                      │");
        System.out.println("│      <foreach collection=\"list\" item=\"item\" separator=\",\">          │");
        System.out.println("│          (#{item.name}, #{item.age}, #{item.status})                  │");
        System.out.println("│      </foreach>                                                       │");
        System.out.println("│  </insert>                                                            │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("MyBatis-Plus批量操作：");
        System.out.println("┌────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 批量插入（默认每次1000条）                                          │");
        System.out.println("│  userService.saveBatch(userList);                                     │");
        System.out.println("│  userService.saveBatch(userList, 500);  // 自定义批次大小             │");
        System.out.println("│                                                                       │");
        System.out.println("│  // 批量更新                                                          │");
        System.out.println("│  userService.updateBatchById(userList);                               │");
        System.out.println("│                                                                       │");
        System.out.println("│  // 批量保存或更新                                                     │");
        System.out.println("│  userService.saveOrUpdateBatch(userList);                             │");
        System.out.println("└────────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第五部分：高频面试题 ====================

    /**
     * 高频面试题
     */
    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第五部分：高频面试题                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("==========================================================================");
        System.out.println("【问题1】EXPLAIN 执行计划中 type 字段有哪些值？含义是什么？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("性能从好到差排序：");
        System.out.println("system → const → eq_ref → ref → range → index → ALL");
        System.out.println("");
        System.out.println("- system: 系统表，只有一行");
        System.out.println("- const: 主键/唯一索引等值查询，最多一行");
        System.out.println("- eq_ref: JOIN时使用主键/唯一索引");
        System.out.println("- ref: 非唯一索引等值查询");
        System.out.println("- range: 索引范围扫描");
        System.out.println("- index: 全索引扫描");
        System.out.println("- ALL: 全表扫描（要避免）\n");

        System.out.println("==========================================================================");
        System.out.println("【问题2】如何优化深分页问题？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("深分页问题：LIMIT 1000000, 10 需要扫描100万行");
        System.out.println("");
        System.out.println("优化方案：");
        System.out.println("1. 延迟关联：先用覆盖索引查出ID，再JOIN查完整数据");
        System.out.println("2. 游标分页：记住上一页最后一条的ID，WHERE id < lastId");
        System.out.println("3. 避免跳页：只提供上一页/下一页功能");
        System.out.println("4. 搜索引擎：数据量特别大时考虑ES\n");

        System.out.println("==========================================================================");
        System.out.println("【问题3】索引失效的常见场景有哪些？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("1. 违反最左前缀原则");
        System.out.println("2. 在索引列上做运算或函数：WHERE YEAR(date) = 2024");
        System.out.println("3. 隐式类型转换：WHERE phone = 13800138000（phone是VARCHAR）");
        System.out.println("4. LIKE以%开头：WHERE name LIKE '%张'");
        System.out.println("5. OR条件两边有一边没索引");
        System.out.println("6. 使用 != 或 <>");
        System.out.println("7. IS NOT NULL（某些情况）\n");

        System.out.println("==========================================================================");
        System.out.println("【问题4】大表如何进行数据删除？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("原则：分批删除，避免长事务");
        System.out.println("");
        System.out.println("方法：");
        System.out.println("1. 使用 LIMIT 分批删除");
        System.out.println("   DELETE FROM table WHERE condition LIMIT 10000;");
        System.out.println("2. 循环执行直到删除完成");
        System.out.println("3. 每批之间适当间隔，避免影响业务");
        System.out.println("4. 在低峰期执行\n");

        System.out.println("==========================================================================");
        System.out.println("【问题5】批量插入如何优化？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("1. 使用多值INSERT语法：INSERT INTO t VALUES (...), (...), (...)");
        System.out.println("2. JDBC开启批量重写：rewriteBatchedStatements=true");
        System.out.println("3. 使用 addBatch() + executeBatch()");
        System.out.println("4. 控制每批数量（建议500-1000条）");
        System.out.println("5. 关闭自动提交，手动控制事务");
        System.out.println("6. 临时禁用索引，插入完成后重建\n");

        System.out.println("==========================================================================");
        System.out.println("【问题6】单表多大需要分库分表？有哪些方案？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("数据量参考：");
        System.out.println("- 500万以内：索引优化即可");
        System.out.println("- 500万-2000万：考虑分区表或归档");
        System.out.println("- 2000万以上：考虑分库分表");
        System.out.println("");
        System.out.println("分库分表方案：");
        System.out.println("1. 垂直拆分：按业务拆分到不同库/表");
        System.out.println("2. 水平拆分：按某字段（如用户ID）取模分表");
        System.out.println("");
        System.out.println("中间件：ShardingSphere、MyCat、TDDL\n");

        System.out.println("==========================================================================");
        System.out.println("【问题7】什么是覆盖索引？有什么好处？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("覆盖索引：查询的列全部包含在索引中，不需要回表。");
        System.out.println("");
        System.out.println("好处：");
        System.out.println("1. 减少IO：不需要读取数据行");
        System.out.println("2. 减少随机IO：索引是顺序存储的");
        System.out.println("3. EXPLAIN中 Extra 显示 Using index");
        System.out.println("");
        System.out.println("示例：");
        System.out.println("INDEX idx_name_age (name, age)");
        System.out.println("SELECT name, age FROM user WHERE name = 'Tom'; -- 覆盖索引\n");

        System.out.println("==========================================================================");
        System.out.println("【问题8】Using filesort 和 Using temporary 如何优化？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("Using filesort（文件排序）：");
        System.out.println("- 原因：ORDER BY 的列没有索引");
        System.out.println("- 优化：为排序列创建索引，注意排序方向要一致");
        System.out.println("");
        System.out.println("Using temporary（临时表）：");
        System.out.println("- 原因：GROUP BY、DISTINCT、UNION 需要暂存数据");
        System.out.println("- 优化：为分组列创建索引，减少临时表使用\n");

        System.out.println("==========================================================================");
        System.out.println("【问题9】如何定位线上慢SQL？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("1. 开启慢查询日志");
        System.out.println("   SET GLOBAL slow_query_log = ON;");
        System.out.println("   SET GLOBAL long_query_time = 2;");
        System.out.println("");
        System.out.println("2. 使用分析工具");
        System.out.println("   - mysqldumpslow（MySQL自带）");
        System.out.println("   - pt-query-digest（Percona）");
        System.out.println("");
        System.out.println("3. SHOW PROCESSLIST 查看当前执行SQL");
        System.out.println("4. Performance Schema 监控\n");

        System.out.println("==========================================================================");
        System.out.println("【问题10】MyBatis #{} 和 ${} 的区别？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("┌─────────────┬────────────────────────────┬────────────────────────────┐");
        System.out.println("│             │ #{}                        │ ${}                        │");
        System.out.println("├─────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 处理方式    │ 预编译，参数绑定           │ 直接字符串替换             │");
        System.out.println("├─────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 安全性      │ 安全，防SQL注入            │ 有SQL注入风险              │");
        System.out.println("├─────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 使用场景    │ 参数值                     │ 表名、列名、排序字段       │");
        System.out.println("├─────────────┼────────────────────────────┼────────────────────────────┤");
        System.out.println("│ 性能        │ 可缓存执行计划             │ 每次生成新SQL              │");
        System.out.println("└─────────────┴────────────────────────────┴────────────────────────────┘\n");

        System.out.println("==========================================================================\n");
    }
}
