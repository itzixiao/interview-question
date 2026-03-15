package cn.itzixiao.interview.bigdata.clickhouse;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * ClickHouse OLAP 分析示例
 * 
 * ClickHouse 特点：
 * - 列式存储
 * - 向量化执行
 * - 数据压缩率高
 * - SQL 支持
 * - 实时查询性能强
 * 
 * 应用场景：
 * - 用户行为分析
 * - 日志分析
 * - 实时监控
 * - BI 报表
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
public class ClickHouseOLAPDemo {
    
    /**
     * ClickHouse 表结构设计和创建
     */
    public void createTableExample() {
        log.info("========== ClickHouse 表结构设计 ==========");
        
        log.info("\n建表 SQL:");
        log.info("CREATE TABLE IF NOT EXISTS user_events (");
        log.info("    event_id UInt64,");
        log.info("    user_id UInt64,");
        log.info("    event_type String,");
        log.info("    event_time DateTime,");
        log.info("    device String,");
        log.info("    os String,");
        log.info("    country String,");
        log.info("    amount Decimal(18,2)");
        log.info(") ENGINE = MergeTree()");
        log.info("PARTITION BY toYYYYMM(event_time)");
        log.info("ORDER BY (user_id, event_time)");
        log.info("TTL event_time + INTERVAL 1 YEAR");
        log.info("SETTINGS index_granularity = 8192;");
        
        log.info("\n物化视图（预聚合）:");
        log.info("CREATE MATERIALIZED VIEW daily_stats_mv");
        log.info("ENGINE = SummingMergeTree()");
        log.info("AS SELECT toDate(event_time) AS date,");
        log.info("       event_type,");
        log.info("       count() AS event_count,");
        log.info("       sum(amount) AS total_amount");
        log.info("FROM user_events");
        log.info("GROUP BY date, event_type;");
        
        log.info("\n引擎说明：");
        log.info("  - MergeTree: 主引擎，支持分区、排序、索引");
        log.info("  - SummingMergeTree: 自动聚合");
        log.info("  - Distributed: 分布式查询");
        log.info("  - ReplicatedMergeTree: 副本复制");
        
        log.info("==============================\n");
    }
    
    /**
     * ClickHouse OLAP 查询示例
     */
    public void olapQueryExample() {
        log.info("========== ClickHouse OLAP 查询 ==========");
        
        log.info("\n查询 1 - 近 7 天事件统计:");
        log.info("SELECT event_type,");
        log.info("       count() AS event_count,");
        log.info("       sum(amount) AS total_amount,");
        log.info("       avg(amount) AS avg_amount");
        log.info("FROM user_events");
        log.info("WHERE event_time >= today() - INTERVAL 7 DAY");
        log.info("GROUP BY event_type");
        log.info("ORDER BY event_count DESC");
        log.info("LIMIT 10;");
        
        log.info("\n查询 2 - 漏斗分析:");
        log.info("SELECT event_type,");
        log.info("       count(DISTINCT user_id) AS unique_users");
        log.info("FROM user_events");
        log.info("WHERE event_time >= today() - INTERVAL 30 DAY");
        log.info("  AND event_type IN ('view', 'click', 'add_cart', 'purchase')");
        log.info("GROUP BY event_type;");
        
        log.info("\n查询 3 - 次日留存率:");
        log.info("WITH yesterday_users AS (");
        log.info("    SELECT DISTINCT user_id");
        log.info("    FROM user_events");
        log.info("    WHERE event_time >= today() - INTERVAL 1 DAY");
        log.info("      AND event_time < today()");
        log.info(")");
        log.info("SELECT count(DISTINCT e.user_id) / count(DISTINCT y.user_id) AS retention_rate");
        log.info("FROM yesterday_users y");
        log.info("LEFT JOIN user_events e");
        log.info("    ON y.user_id = e.user_id");
        log.info("    AND e.event_time >= today()");
        log.info("    AND e.event_time < today() + INTERVAL 1 DAY;");
        
        log.info("\n查询 4 - 本月消费 Top100 用户:");
        log.info("SELECT user_id,");
        log.info("       sum(amount) AS total_spending");
        log.info("FROM user_events");
        log.info("WHERE event_time >= toStartOfMonth(today())");
        log.info("GROUP BY user_id");
        log.info("ORDER BY total_spending DESC");
        log.info("LIMIT 100;");
        
        log.info("==============================\n");
    }
    
    /**
     * 生成示例数据
     */
    public List<Event> generateSampleEvents() {
        List<Event> events = new ArrayList<>();
        String[] eventTypes = {"view", "click", "add_cart", "purchase"};
        String[] devices = {"iOS", "Android", "Web"};
        String[] countries = {"CN", "US", "JP", "KR"};
        
        for (int i = 0; i < 100; i++) {
            Event event = new Event();
            event.eventId = (long) i;
            event.userId = (long) (i % 10);
            event.eventType = eventTypes[i % eventTypes.length];
            event.eventTime = new java.util.Date();
            event.device = devices[i % devices.length];
            event.country = countries[i % countries.length];
            event.amount = Math.random() * 1000;
            events.add(event);
        }
        
        return events;
    }
}

/**
 * 事件实体类
 */
class Event {
    public Long eventId;
    public Long userId;
    public String eventType;
    public java.util.Date eventTime;
    public String device;
    public String country;
    public Double amount;
}
