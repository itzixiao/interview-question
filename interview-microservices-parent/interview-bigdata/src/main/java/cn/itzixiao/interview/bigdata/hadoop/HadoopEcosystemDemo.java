package cn.itzixiao.interview.bigdata.hadoop;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.IOException;
import java.net.URI;

/**
 * Hadoop 生态系统概览演示
 * 
 * Hadoop 核心组件：
 * 1. HDFS - 分布式文件系统
 * 2. MapReduce - 分布式计算框架
 * 3. YARN - 资源调度系统
 * 
 * Hadoop 生态圈：
 * - Hive: 数据仓库工具
 * - HBase: NoSQL 数据库
 * - ZooKeeper: 分布式协调服务
 * - Kafka: 消息队列
 * - Flume: 日志采集
 * - Sqoop: 数据导入导出
 * - Oozie: 工作流调度
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
public class HadoopEcosystemDemo {
    
    /**
     * HDFS 基础操作示例
     * 
     * HDFS 特点：
     * - 高容错性
     * - 高吞吐量
     * - 适合大规模数据集
     * - 流式数据访问
     */
    public void hdfsBasicOperations() throws IOException {
        log.info("========== HDFS 基础操作 ==========");
        
        // 配置 Hadoop
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "hdfs://localhost:9000");
        
        // 获取文件系统实例
        FileSystem fs = FileSystem.get(URI.create("hdfs://localhost:9000"), conf);
        
        // 1. 创建目录
        Path dir = new Path("/user/hadoop/data");
        if (!fs.exists(dir)) {
            fs.mkdirs(dir);
            log.info("创建目录：{}", dir);
        }
        
        // 2. 上传文件
        Path localFile = new Path("local_data.txt");
        Path remoteFile = new Path("/user/hadoop/data/remote_data.txt");
        // fs.copyFromLocalFile(localFile, remoteFile);
        log.info("上传文件：{} -> {}", localFile, remoteFile);
        
        // 3. 读取文件
        if (fs.exists(remoteFile)) {
            FSDataInputStream in = fs.open(remoteFile);
            log.info("读取文件：{}", remoteFile);
            // IOUtils.copyBytes(in, System.out, 4096, false);
            in.close();
        }
        
        // 4. 列出目录内容
        FileStatus[] statuses = fs.listStatus(dir);
        for (FileStatus status : statuses) {
            log.info("文件：{}, 大小：{} bytes", status.getPath(), status.getLen());
        }
        
        // 5. 删除文件
        // fs.delete(remoteFile, false);
        log.info("删除文件：{}", remoteFile);
        
        fs.close();
        log.info("==============================\n");
    }
    
    /**
     * MapReduce 工作原理示意
     * 
     * MapReduce 流程：
     * 1. Input Split（输入分片）
     * 2. Map 阶段（映射）
     * 3. Shuffle 阶段（混洗）
     * 4. Reduce 阶段（归约）
     * 5. Output（输出）
     */
    public void mapreduceWorkflow() {
        log.info("========== MapReduce 工作流程 ==========");
        
        log.info("\n词频统计示例：");
        log.info("输入数据：");
        log.info("  'hello world'");
        log.info("  'hello hadoop'");
        log.info("  'mapreduce example'");
        
        log.info("\nMap 阶段：");
        log.info("  ('hello', 1)");
        log.info("  ('world', 1)");
        log.info("  ('hello', 1)");
        log.info("  ('hadoop', 1)");
        log.info("  ('mapreduce', 1)");
        log.info("  ('example', 1)");
        
        log.info("\nShuffle 阶段（按 key 分组）：");
        log.info("  'hello' -> [1, 1]");
        log.info("  'world' -> [1]");
        log.info("  'hadoop' -> [1]");
        log.info("  'mapreduce' -> [1]");
        log.info("  'example' -> [1]");
        
        log.info("\nReduce 阶段（求和）：");
        log.info("  ('hello', 2)");
        log.info("  ('world', 1)");
        log.info("  ('hadoop', 1)");
        log.info("  ('mapreduce', 1)");
        log.info("  ('example', 1)");
        
        log.info("\n输出结果：");
        log.info("  hello: 2");
        log.info("  world: 1");
        log.info("  hadoop: 1");
        log.info("  mapreduce: 1");
        log.info("  example: 1");
        
        log.info("==============================\n");
    }
    
    /**
     * Hadoop 生态圈组件介绍
     */
    public void hadoopEcosystemOverview() {
        log.info("========== Hadoop 生态圈 ==========");
        
        log.info("\n核心组件：");
        log.info("  1. HDFS - 分布式文件系统");
        log.info("     - NameNode: 管理文件系统命名空间");
        log.info("     - DataNode: 存储实际数据块");
        log.info("     - Secondary NameNode: 辅助检查点");
        
        log.info("\n  2. MapReduce - 分布式计算框架");
        log.info("     - Map: 数据处理和过滤");
        log.info("     - Reduce: 数据聚合");
        log.info("     - JobTracker/TaskTracker: 任务调度");
        
        log.info("\n  3. YARN - 资源管理系统");
        log.info("     - ResourceManager: 全局资源管理");
        log.info("     - NodeManager: 节点资源管理");
        log.info("     - ApplicationMaster: 应用管理");
        
        log.info("\n周边生态：");
        log.info("  - Hive: SQL 数据仓库（MapReduce/Tez 引擎）");
        log.info("  - HBase: NoSQL 列式数据库");
        log.info("  - Pig: 数据流语言和执行环境");
        log.info("  - Spark: 内存计算引擎");
        log.info("  - Flink: 流处理引擎");
        log.info("  - Kafka: 分布式消息队列");
        log.info("  - ZooKeeper: 分布式协调服务");
        log.info("  - Flume: 日志采集系统");
        log.info("  - Sqoop: 数据传输工具");
        log.info("  - Oozie: 工作流调度器");
        log.info("  - Hue: Web UI 界面");
        
        log.info("\n典型架构：");
        log.info("  数据采集 → Kafka → Flink/Spark → HDFS/Hive → ClickHouse/ES");
        
        log.info("==============================\n");
    }
}
