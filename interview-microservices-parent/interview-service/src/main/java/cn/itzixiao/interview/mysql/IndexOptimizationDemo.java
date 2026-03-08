package cn.itzixiao.interview.mysql;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 索引优化详解
 *
 * 索引类型：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  按数据结构分类：                                             │
 * │  - B+Tree 索引：InnoDB 默认，支持范围查询、排序               │
 * │  - Hash 索引：Memory 引擎，精确匹配快，不支持范围查询          │
 * │  - Full-text 索引：全文检索                                   │
 * │  - R-Tree 索引：空间数据索引                                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  按物理存储分类：                                             │
 * │  - 聚簇索引：数据与索引在一起，InnoDB 主键索引                 │
 * │  - 非聚簇索引：索引与数据分离，叶子节点存主键值                │
 * ├─────────────────────────────────────────────────────────────┤
 * │  按逻辑功能分类：                                             │
 * │  - 主键索引、唯一索引、普通索引、前缀索引、组合索引            │
 * └─────────────────────────────────────────────────────────────┘
 */
public class IndexOptimizationDemo {

    public static void main(String[] args) {
        System.out.println("========== MySQL 索引优化详解 ==========\n");

        // 1. 索引基础概念
        demonstrateIndexBasics();

        // 2. B+Tree 索引结构
        demonstrateBPlusTreeStructure();

        // 3. 索引优化原则
        demonstrateIndexOptimizationRules();

        // 4. 索引失效场景
        demonstrateIndexFailure();

        // 5. 执行计划分析
        demonstrateExplain();

        // 6. 慢查询优化案例
        demonstrateSlowQueryOptimization();
    }

    /**
     * 1. 索引基础概念
     */
    private static void demonstrateIndexBasics() {
        System.out.println("【1. 索引基础概念】\n");

        System.out.println("为什么需要索引？");
        System.out.println("- 类比：书籍的目录 vs 全表扫描");
        System.out.println("- 没有索引：O(n) 逐行扫描");
        System.out.println("- 有索引：O(log n) 树查找\n");

        System.out.println("InnoDB 索引结构 - B+Tree：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│                     B+Tree 结构示意                          │");
        System.out.println("│                                                             │");
        System.out.println("│                         [10|30|50]                          │");
        System.out.println("│                        /    |    \\                         │");
        System.out.println("│              [5|8|9]  [20|25]  [40|45]  [60|70|80]          │");
        System.out.println("│              /  |  \\   /    \\   /    \\   /   |   \\        │");
        System.out.println("│            叶子节点（数据页）双向链表连接                      │");
        System.out.println("│            ┌─┐┌─┐┌─┐  ┌─┐┌─┐  ┌─┐┌─┐  ┌─┐┌─┐┌─┐            │");
        System.out.println("│            │5││8││9│→│20││25│→│40││45│→│60││70││80│→       │");
        System.out.println("│            └─┘└─┘└─┘  └─┘└─┘  └─┘└─┘  └─┘└─┘└─┘            │");
        System.out.println("│                                                             │");
        System.out.println("│  特点：                                                      │");
        System.out.println("│  1. 非叶子节点只存键值，不存数据（树更矮，IO更少）            │");
        System.out.println("│  2. 叶子节点包含所有数据，且按顺序链接（支持范围查询）         │");
        System.out.println("│  3. 所有查询都要到叶子节点（查询稳定）                        │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("聚簇索引 vs 非聚簇索引：");
        System.out.println("┌─────────────┬─────────────────────────┬─────────────────────────┐");
        System.out.println("│   特性      │     聚簇索引             │     非聚簇索引          │");
        System.out.println("├─────────────┼─────────────────────────┼─────────────────────────┤");
        System.out.println("│  数据存储   │ 叶子节点直接存数据行      │ 叶子节点存主键值        │");
        System.out.println("│  表数量     │ 一个表只能有一个          │ 一个表可以有多个        │");
        System.out.println("│  查询效率   │ 主键查询快                │ 需要回表查询（覆盖索引除外）│");
        System.out.println("│  插入效率   │ 需要按主键排序插入        │ 不影响数据物理顺序      │");
        System.out.println("│  更新代价   │ 大（数据移动）            │ 小（只更新索引）        │");
        System.out.println("└─────────────┴─────────────────────────┴─────────────────────────┘\n");
    }

    /**
     * 2. B+Tree 索引结构详解
     */
    private static void demonstrateBPlusTreeStructure() {
        System.out.println("【2. B+Tree 索引结构详解】\n");

        System.out.println("InnoDB 页结构（默认 16KB）：");
        System.out.println("┌─────────────────────────────────────────┐");
        System.out.println("│  File Header (38 byte)                  │");
        System.out.println("│  - 页号、上一页、下一页、页类型          │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  Page Header (56 byte)                  │");
        System.out.println("│  - 槽数量、记录数量、最后插入位置        │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  Infimum + Supremum (26 byte)           │");
        System.out.println("│  - 最小记录和最大记录                    │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  User Records（用户记录，按主键排序）     │");
        System.out.println("│  - 行格式：Compact/Dynamic/Compressed    │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  Free Space（空闲空间）                  │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  Page Directory（页目录，槽信息）         │");
        System.out.println("│  - 二分查找加速定位                      │");
        System.out.println("├─────────────────────────────────────────┤");
        System.out.println("│  File Trailer（8 byte）                 │");
        System.out.println("│  - 校验和，保证页完整性                  │");
        System.out.println("└─────────────────────────────────────────┘\n");

        System.out.println("主键索引查询过程（SELECT * FROM user WHERE id = 5）：");
        System.out.println("1. 从根页（常驻内存）开始二分查找");
        System.out.println("2. 定位到包含 id=5 的叶子页");
        System.out.println("3. 在叶子页中通过页目录二分查找");
        System.out.println("4. 找到记录，直接返回数据（聚簇索引）\n");

        System.out.println("二级索引查询过程（SELECT * FROM user WHERE name = 'Tom'）：");
        System.out.println("1. 在 name 索引树中找到 'Tom' 对应的叶子节点");
        System.out.println("2. 叶子节点存储的是主键值（如 id=5）");
        System.out.println("3. 用主键值去主键索引树查询（回表）");
        System.out.println("4. 返回完整数据\n");

        System.out.println("覆盖索引（避免回表）：");
        System.out.println("SELECT id, name FROM user WHERE name = 'Tom'");
        System.out.println("- id 和 name 都在二级索引中，无需回表");
        System.out.println("- Extra: Using index\n");
    }

    /**
     * 3. 索引优化原则
     */
    private static void demonstrateIndexOptimizationRules() {
        System.out.println("【3. 索引优化原则】\n");

        System.out.println("最左前缀法则：");
        System.out.println("组合索引 (a, b, c) 的有效查询：");
        System.out.println("✓ WHERE a = 1");
        System.out.println("✓ WHERE a = 1 AND b = 2");
        System.out.println("✓ WHERE a = 1 AND b = 2 AND c = 3");
        System.out.println("✓ WHERE a = 1 AND c = 3（c 不走索引，但 a 走）");
        System.out.println("✗ WHERE b = 2（缺少最左列 a）");
        System.out.println("✗ WHERE b = 2 AND c = 3（缺少最左列 a）\n");

        System.out.println("索引设计原则：");
        System.out.println("1. 为 WHERE、JOIN、ORDER BY、GROUP BY 的列创建索引");
        System.out.println("2. 区分度高的列放前面（选择性 = 不同值数量 / 总行数）");
        System.out.println("3. 避免冗余索引（如已有 (a,b)，则 (a) 是冗余的）");
        System.out.println("4. 控制索引数量（写操作会变慢，占用空间）");
        System.out.println("5. 小表不需要索引（全表扫描更快）");
        System.out.println("6. 使用覆盖索引减少回表\n");

        System.out.println("索引下推（Index Condition Pushdown, ICP）：");
        System.out.println("MySQL 5.6+ 优化，在存储引擎层过滤数据，减少回表");
        System.out.println("例：INDEX (name, age)，查询 WHERE name LIKE '张%' AND age = 20");
        System.out.println("- 无 ICP：先找到所有 '张%' 的主键，回表过滤 age");
        System.out.println("- 有 ICP：在索引层就过滤 age=20，减少回表次数\n");
    }

    /**
     * 4. 索引失效场景
     */
    private static void demonstrateIndexFailure() {
        System.out.println("【4. 索引失效场景】\n");

        System.out.println("1. 违反最左前缀法则");
        System.out.println("   索引 (a,b,c)，查询 WHERE b=1 AND c=2 → 失效\n");

        System.out.println("2. 在索引列上做运算或函数操作");
        System.out.println("   ✗ WHERE LEFT(name, 2) = '张'");
        System.out.println("   ✗ WHERE age + 1 = 20");
        System.out.println("   ✓ WHERE name LIKE '张%'");
        System.out.println("   ✓ WHERE age = 19\n");

        System.out.println("3. 隐式类型转换");
        System.out.println("   字段是 VARCHAR，查询时用 INT");
        System.out.println("   ✗ WHERE phone = 13800138000");
        System.out.println("   ✓ WHERE phone = '13800138000'\n");

        System.out.println("4. 使用 != 或 <>");
        System.out.println("   可能走索引，但效率低（需要扫描大部分数据）\n");

        System.out.println("5. 使用 OR 条件（部分情况）");
        System.out.println("   ✗ WHERE a = 1 OR b = 2（a 和 b 没有组合索引）");
        System.out.println("   ✓ 改用 UNION\n");

        System.out.println("6. LIKE 以 % 开头");
        System.out.println("   ✗ WHERE name LIKE '%张'");
        System.out.println("   ✗ WHERE name LIKE '%张%'");
        System.out.println("   ✓ WHERE name LIKE '张%'\n");

        System.out.println("7. 数据分布不均匀");
        System.out.println("   如果优化器认为全表扫描更快，会放弃索引\n");
    }

    /**
     * 5. 执行计划分析
     */
    private static void demonstrateExplain() {
        System.out.println("【5. EXPLAIN 执行计划分析】\n");

        System.out.println("EXPLAIN 关键字段：");
        System.out.println("┌─────────────┬────────────────────────────────────────────────┐");
        System.out.println("│  字段       │  说明                                          │");
        System.out.println("├─────────────┼────────────────────────────────────────────────┤");
        System.out.println("│  id         │  执行顺序，id 越大越先执行，相同从上到下        │");
        System.out.println("│  select_type│  查询类型：SIMPLE/PRIMARY/SUBQUERY/JOIN/DERIVED │");
        System.out.println("│  table      │  访问的表                                       │");
        System.out.println("│  type       │  访问类型：system>const>eq_ref>ref>range>index>ALL│");
        System.out.println("│  possible_keys│ 可能使用的索引                                 │");
        System.out.println("│  key        │  实际使用的索引                                 │");
        System.out.println("│  key_len    │  使用的索引长度（可判断用了哪些列）              │");
        System.out.println("│  ref        │  索引列与哪列或常量比较                         │");
        System.out.println("│  rows       │  预估扫描行数（越小越好）                        │");
        System.out.println("│  Extra      │  额外信息：Using index/Using where/Using filesort│");
        System.out.println("└─────────────┴────────────────────────────────────────────────┘\n");

        System.out.println("type 字段详解（性能从好到差）：");
        System.out.println("- system：系统表，只有一行数据");
        System.out.println("- const：主键或唯一索引，最多匹配一行");
        System.out.println("- eq_ref：JOIN 时主键或唯一索引匹配");
        System.out.println("- ref：非唯一索引匹配");
        System.out.println("- range：索引范围扫描（BETWEEN、IN、>、<）");
        System.out.println("- index：全索引扫描（遍历整个索引树）");
        System.out.println("- ALL：全表扫描（最差，要避免）\n");

        System.out.println("Extra 字段重要值：");
        System.out.println("- Using index：覆盖索引，不需要回表");
        System.out.println("- Using where：在 Server 层过滤数据");
        System.out.println("- Using index condition：索引下推");
        System.out.println("- Using temporary：使用临时表（GROUP BY、DISTINCT）");
        System.out.println("- Using filesort：需要额外排序（ORDER BY 无索引）\n");
    }

    /**
     * 6. 慢查询优化案例
     */
    private static void demonstrateSlowQueryOptimization() {
        System.out.println("【6. 慢查询优化案例】\n");

        System.out.println("案例1：分页查询优化");
        System.out.println("慢 SQL：");
        System.out.println("  SELECT * FROM orders ORDER BY create_time DESC LIMIT 1000000, 10;");
        System.out.println("问题：需要扫描 1000010 行，丢弃前 1000000 行\n");

        System.out.println("优化方案1：覆盖索引 + 子查询");
        System.out.println("  SELECT * FROM orders o");
        System.out.println("  JOIN (SELECT id FROM orders ORDER BY create_time DESC LIMIT 1000000, 10) tmp");
        System.out.println("  ON o.id = tmp.id;\n");

        System.out.println("优化方案2：记住上次位置");
        System.out.println("  SELECT * FROM orders");
        System.out.println("  WHERE create_time < '上次最后一条时间'");
        System.out.println("  ORDER BY create_time DESC LIMIT 10;\n");

        System.out.println("案例2：JOIN 优化");
        System.out.println("慢 SQL：");
        System.out.println("  SELECT * FROM user u LEFT JOIN orders o ON u.id = o.user_id");
        System.out.println("  WHERE u.status = 1 AND o.amount > 100;\n");

        System.out.println("优化：");
        System.out.println("1. 确保 user.id 和 orders.user_id 都有索引");
        System.out.println("2. 小表驱动大表（NLJ 算法）");
        System.out.println("3. 考虑是否需要 LEFT JOIN（数据过滤在 ON 还是 WHERE）\n");

        System.out.println("案例3：ORDER BY 优化");
        System.out.println("慢 SQL：");
        System.out.println("  SELECT * FROM user ORDER BY age, create_time LIMIT 100;\n");

        System.out.println("优化：");
        System.out.println("1. 创建组合索引 (age, create_time)");
        System.out.println("2. 避免 SELECT *，使用覆盖索引");
        System.out.println("3. 如果数据量大，考虑延迟关联\n");

        System.out.println("索引优化检查清单：");
        System.out.println("□ 是否使用了正确的索引？EXPLAIN 检查");
        System.out.println("□ 是否存在索引失效的情况？");
        System.out.println("□ 是否可以利用覆盖索引避免回表？");
        System.out.println("□ 是否可以利用索引下推减少回表？");
        System.out.println("□ 组合索引是否符合最左前缀？");
        System.out.println("□ 是否存在冗余或重复索引？");
        System.out.println("□ 写操作性能是否可以接受？\n");
    }
}
