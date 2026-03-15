package cn.itzixiao.interview.bigdata.datalake;

import lombok.extern.slf4j.Slf4j;

/**
 * Data Lake 数据湖架构演示
 * 
 * 数据湖概念：
 * - 集中式存储库
 * - 存储原始和处理后的数据
 * - 支持结构化、半结构化、非结构化数据
 * - 按需处理和分析
 * 
 * 主流方案：
 * 1. Delta Lake（Databricks）
 * 2. Hudi（Apache）
 * 3. Iceberg（Apache）
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
public class DataLakeDemo {
    
    /**
     * 数据湖架构图解
     */
    public void dataLakeArchitecture() {
        log.info("========== 数据湖架构 ==========");
        
        log.info("\n传统数据仓库 vs 数据湖：");
        log.info("┌─────────────────┐    ┌─────────────────┐");
        log.info("│   数据仓库      │    │    数据湖       │");
        log.info("├─────────────────┤    ├─────────────────┤");
        log.info("│ 结构化数据      │    │ 所有数据类型    │");
        log.info("│ Schema-on-Write │    │ Schema-on-Read  │");
        log.info("│ BI 报表         │    │ ML/AI/BI        │");
        log.info("│ 历史数据        │    │ 全量数据        │");
        log.info("└─────────────────┘    └─────────────────┘");
        
        log.info("\n数据湖分层架构：");
        log.info("┌─────────────────────────────────────┐");
        log.info("│          数据应用层                  │");
        log.info("│   (BI 报表、AI 模型、实时查询)         │");
        log.info("└─────────────────────────────────────┘");
        log.info("              ↓");
        log.info("┌─────────────────────────────────────┐");
        log.info("│          数据服务层                  │");
        log.info("│   (数据 API、查询引擎、计算引擎)     │");
        log.info("└─────────────────────────────────────┘");
        log.info("              ↓");
        log.info("┌─────────────────────────────────────┐");
        log.info("│          数据存储层                  │");
        log.info("│   (HDFS、S3、OSS、MinIO)            │");
        log.info("└─────────────────────────────────────┘");
        log.info("              ↓");
        log.info("┌─────────────────────────────────────┐");
        log.info("│          数据采集层                  │");
        log.info("│   (Kafka、Flume、Sqoop、CDC)        │");
        log.info("└─────────────────────────────────────┘");
        
        log.info("==============================\n");
    }
    
    /**
     * Lambda 架构 vs Kappa 架构
     */
    public void lambdaVsKappa() {
        log.info("========== Lambda vs Kappa 架构 ==========");
        
        log.info("\nLambda 架构：");
        log.info("┌─────────────────────────────────────┐");
        log.info("│          应用层                      │");
        log.info("│         ↙        ↘                  │");
        log.info("│    批处理层    速度层（流处理）      │");
        log.info("│         ↘        ↙                  │");
        log.info("│        服务层（合并结果）             │");
        log.info("└─────────────────────────────────────┘");
        log.info("特点：");
        log.info("  ✅ 兼顾准确性和实时性");
        log.info("  ❌ 维护两套代码");
        log.info("  ❌ 架构复杂");
        
        log.info("\nKappa 架构：");
        log.info("┌─────────────────────────────────────┐");
        log.info("│          应用层                      │");
        log.info("│              ↑                      │");
        log.info("│          流处理层                    │");
        log.info("│              ↑                      │");
        log.info("│        Kafka（日志存储）              │");
        log.info("└─────────────────────────────────────┘");
        log.info("特点：");
        log.info("  ✅ 只需维护一套代码");
        log.info("  ✅ 架构简化");
        log.info("  ❌ 对流处理要求高");
        
        log.info("\n数据湖仓一体（Lakehouse）：");
        log.info("  - 结合数据湖和数据仓库优势");
        log.info("  - 支持 ACID 事务");
        log.info("  - 支持 Schema 演化");
        log.info("  - 支持增量更新");
        log.info("  - 代表技术：Delta Lake、Hudi、Iceberg");
        
        log.info("==============================\n");
    }
    
    /**
     * 典型数据处理流程
     */
    public void dataProcessingPipeline() {
        log.info("========== 典型数据处理流程 ==========");
        
        log.info("\n实时数据处理链路：");
        log.info("1. 数据采集");
        log.info("   - 业务数据库 CDC → Debezium");
        log.info("   - 用户行为日志 → Flume/Filebeat");
        log.info("   - 服务器日志 → Filebeat");
        
        log.info("\n2. 消息队列缓冲");
        log.info("   - Kafka Topic: ods_user_behavior");
        log.info("   - Kafka Topic: ods_order_log");
        
        log.info("\n3. 实时计算（Flink）");
        log.info("   - 数据清洗（ETL）");
        log.info("   - 实时聚合");
        log.info("   - 实时风控");
        
        log.info("\n4. 数据存储");
        log.info("   - 明细数据 → HDFS/Iceberg");
        log.info("   - 聚合数据 → ClickHouse/Doris");
        log.info("   - 维度数据 → Redis/HBase");
        
        log.info("\n5. 数据服务");
        log.info("   - API 接口 → Spring Boot");
        log.info("   - BI 报表 → Superset/Metabase");
        log.info("   - 即席查询 → Presto/Trino");
        
        log.info("\n离线数据处理链路：");
        log.info("1. 数据同步 → Sqoop/DataX");
        log.info("2. 数据仓库分层（ODS→DWD→DWS→ADS）");
        log.info("3. Spark 批量计算");
        log.info("4. Hive SQL 分析");
        log.info("5. 导出到 MySQL/ClickHouse");
        
        log.info("==============================\n");
    }
    
    /**
     * 数据仓库分层设计
     */
    public void dataWarehouseLayers() {
        log.info("========== 数据仓库分层设计 ==========");
        
        log.info("\nODS 层（Operation Data Store）：");
        log.info("  - 原始数据层，保持数据原貌");
        log.info("  - 不做任何清洗和加工");
        log.info("  - 表命名：ods_{业务域}_{表名}");
        
        log.info("\nDWD 层（Data Warehouse Detail）：");
        log.info("  - 明细数据层，进行清洗和规范化");
        log.info("  - 维度退化，事实表关联");
        log.info("  - 表命名：dwd_{业务域}_{表名}");
        
        log.info("\nDWS 层（Data Warehouse Service）：");
        log.info("  - 服务数据层，轻度聚合");
        log.info("  - 按主题汇总");
        log.info("  - 表命名：dws_{业务域}_{聚合粒度}");
        
        log.info("\nADS 层（Application Data Store）：");
        log.info("  - 应用数据层，高度聚合");
        log.info("  - 面向具体应用场景");
        log.info("  - 表命名：ads_{应用场景}_{表名}");
        
        log.info("\nDIM 层（Dimension）：");
        log.info("  - 维度数据层");
        log.info("  - 存储维度信息");
        log.info("  - 表命名：dim_{维度名}");
        
        log.info("\n示例：电商交易域");
        log.info("  ODS: ods_trade_order_info");
        log.info("  DWD: dwd_trade_order_detail");
        log.info("  DWS: dws_trade_user_day_count");
        log.info("  ADS: ads_trade_gmv_stat");
        log.info("  DIM: dim_user_info, dim_product_info");
        
        log.info("==============================\n");
    }
}
