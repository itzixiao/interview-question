package cn.itzixiao.interview.performance.mysql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * MySQL 性能分析与调优工具类
 * <p>
 * Explain 执行计划关键指标：
 * - type: 连接类型（system > const > eq_ref > ref > range > index > ALL）
 * - key: 实际使用的索引
 * - rows: 扫描行数
 * - Extra: 额外信息（Using filesort, Using temporary 需要优化）
 * <p>
 * 慢查询日志配置：
 * slow_query_log = 1
 * slow_query_log_file = /var/log/mysql/slow.log
 * long_query_time = 2
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class SqlExplainAnalyzer {

    private final JdbcTemplate jdbcTemplate;

    public SqlExplainAnalyzer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 分析 SQL 执行计划
     *
     * @param sql 要分析的 SQL 语句
     * @return 执行计划详情
     */
    public void analyzeQuery(String sql) {
        log.info("========== SQL 执行计划分析 ==========");
        log.info("原始 SQL: {}", sql);

        // 使用 EXPLAIN 分析执行计划
        String explainSql = "EXPLAIN " + sql;

        try {
            // 这里简化处理，实际应该解析 EXPLAIN 结果集
            log.info("执行计划：{}", explainSql);

            // 优化建议检查点
            log.info("\n===== 优化建议检查 =====");
            log.info("1. 检查是否使用了索引（type != ALL）");
            log.info("2. 检查是否使用了覆盖索引（key != null）");
            log.info("3. 检查扫描行数（rows 越小越好）");
            log.info("4. 检查是否有 Using filesort（需要避免）");
            log.info("5. 检查是否有 Using temporary（需要避免）");
            log.info("======================\n");

        } catch (Exception e) {
            log.error("SQL 分析失败", e);
        }
    }

    /**
     * 模拟慢查询场景 - 未使用索引的全表扫描
     */
    public void simulateSlowQuery() {
        log.info("模拟慢查询 - 全表扫描");

        // 假设 orders 表没有 status 字段的索引
        String slowSql = "SELECT * FROM orders WHERE status = 'PENDING'";
        analyzeQuery(slowSql);

        // 优化方案：添加索引
        log.info("\n优化方案：CREATE INDEX idx_status ON orders(status);");
    }

    /**
     * 检查锁等待情况
     * <p>
     * SQL 查询：
     * SELECT * FROM information_schema.INNODB_TRX;
     * SELECT * FROM information_schema.INNODB_LOCKS;
     * SELECT * FROM performance_schema.data_locks;
     */
    public void checkLockWait() {
        log.info("========== 锁等待检查 ==========");
        log.info("查询当前事务：SELECT * FROM information_schema.INNODB_TRX");
        log.info("查询行锁：SELECT * FROM performance_schema.data_locks");
        log.info("查询锁等待：SELECT * FROM performance_schema.data_lock_waits");
        log.info("==============================");
    }
}
