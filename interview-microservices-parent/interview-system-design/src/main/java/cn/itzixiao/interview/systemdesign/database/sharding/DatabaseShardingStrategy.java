package cn.itzixiao.interview.systemdesign.database.sharding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 分库分表策略 - 按用户 ID 取模
 * <p>
 * 分库分表场景：
 * 1. 数据量大（单表超过 500 万）
 * 2. 写并发高（单库写入遇到瓶颈）
 * 3. 查询性能下降（索引效率降低）
 * <p>
 * 分片策略：
 * 1. 哈希分片 - 按 ID 取模
 * 2. 范围分片 - 按时间/区域
 * 3. 地理分片 - 按地域
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class DatabaseShardingStrategy {

    /**
     * 数据库数量
     */
    private static final int DATABASE_COUNT = 4;

    /**
     * 每个数据库的表数量
     */
    private static final int TABLE_COUNT_PER_DB = 4;

    /**
     * 计算应该路由到哪个数据库
     */
    public int getDatabaseIndex(Long userId) {
        return Math.abs(userId.hashCode()) % DATABASE_COUNT;
    }

    /**
     * 计算应该路由到哪个表
     */
    public int getTableIndex(Long userId) {
        return Math.abs(userId.hashCode()) % TABLE_COUNT_PER_DB;
    }

    /**
     * 获取实际的数据源名称
     */
    public String getDataSourceName(Long userId) {
        int dbIndex = getDatabaseIndex(userId);
        return "ds_" + dbIndex;
    }

    /**
     * 获取实际的表名
     */
    public String getTableName(Long userId) {
        int dbIndex = getDatabaseIndex(userId);
        int tableIndex = getTableIndex(userId);
        return "t_order_" + dbIndex + "_" + tableIndex;
    }

    /**
     * 演示分片路由
     */
    public void demonstrateSharding() {
        log.info("=== 分库分表路由演示 ===");

        Long[] userIds = {1001L, 1002L, 1003L, 2001L, 2002L, 3001L};

        for (Long userId : userIds) {
            String dataSource = getDataSourceName(userId);
            String tableName = getTableName(userId);

            log.info("userId={} -> 数据库：{}，表：{}",
                    userId, dataSource, tableName);
        }
    }
}
