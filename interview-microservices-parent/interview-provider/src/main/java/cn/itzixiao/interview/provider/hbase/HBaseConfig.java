package cn.itzixiao.interview.provider.hbase;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

/**
 * HBase 配置类
 *
 * <p>用于配置和创建 HBase 连接</p>
 *
 * <h3>使用说明：</h3>
 * <ul>
 *     <li>需要在 application.yml 中配置 HBase 的 ZooKeeper 地址</li>
 *     <li>生产环境建议使用 Profile 区分不同环境</li>
 *     <li>Connection 是线程安全的，应该作为单例使用</li>
 * </ul>
 */
@Configuration
@Profile("!dev") // 测试环境不加载
public class HBaseConfig {

    @Value("${hbase.zookeeper.quorum:localhost}")
    private String zkQuorum;

    @Value("${hbase.zookeeper.property.clientPort:2181}")
    private String zkPort;

    @Value("${hbase.rootdir:hdfs://localhost:9000/hbase}")
    private String rootDir;

    @Value("${zookeeper.znode.parent:/hbase}")
    private String znodeParent;

    /**
     * 创建 HBase 配置对象
     *
     * @return HBaseConfiguration
     */
    @Bean
    public org.apache.hadoop.conf.Configuration hbaseConfiguration() {
        org.apache.hadoop.conf.Configuration config = HBaseConfiguration.create();

        // ZooKeeper 配置
        config.set("hbase.zookeeper.quorum", zkQuorum);
        config.set("hbase.zookeeper.property.clientPort", zkPort);

        // HDFS 配置
        config.set("hbase.rootdir", rootDir);
        config.set("zookeeper.znode.parent", znodeParent);

        // 其他配置
        config.set("hbase.cluster.distributed", "true");
        config.set("hbase.client.retries.number", "3");
        config.set("hbase.client.pause", "1000");
        config.set("hbase.rpc.timeout", "60000");
        config.set("hbase.client.operation.timeout", "60000");

        return config;
    }

    /**
     * 创建 HBase 连接
     *
     * @param config HBase 配置
     * @return Connection
     * @throws IOException IO 异常
     */
    @Bean
    public Connection hbaseConnection(org.apache.hadoop.conf.Configuration config) throws IOException {
        return ConnectionFactory.createConnection(config);
    }
}
