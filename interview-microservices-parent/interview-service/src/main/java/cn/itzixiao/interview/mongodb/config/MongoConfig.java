package cn.itzixiao.interview.mongodb.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 配置类
 * 
 * <p>配置说明：</p>
 * <ul>
 *   <li>uri: MongoDB 连接字符串，支持副本集和分片集群</li>
 *   <li>database: 默认数据库</li>
 * </ul>
 * 
 * <p>连接字符串格式：</p>
 * <pre>
 * # 单节点
 * mongodb://localhost:27017/mydb
 * 
 * # 副本集
 * mongodb://host1:27017,host2:27017,host3:27017/mydb?replicaSet=rs0
 * 
 * # 分片集群
 * mongodb://mongos1:27017,mongos2:27017/mydb
 * 
 * # 带认证
 * mongodb://user:password@localhost:27017/mydb?authSource=admin
 * </pre>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Configuration
@EnableMongoRepositories(basePackages = "cn.itzixiao.interview.mongodb.repository")
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true", matchIfMissing = false)
public class MongoConfig extends AbstractMongoClientConfiguration {

    /**
     * MongoDB 连接字符串
     * 支持单节点、副本集、分片集群等多种连接方式
     */
    @Value("${spring.data.mongodb.uri:mongodb://localhost:27017/interview_db}")
    private String mongoUri;

    /**
     * 数据库名称
     */
    @Value("${spring.data.mongodb.database:interview_db}")
    private String databaseName;

    /**
     * 获取数据库名称
     */
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    /**
     * 创建 MongoDB 客户端
     * 
     * <p>MongoClient 是线程安全的，可以全局共享</p>
     * <p>内部使用连接池管理连接</p>
     * 
     * @return MongoClient 实例
     */
    @Override
    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        // 创建 MongoDB 客户端
        // 连接字符串支持多种配置选项：
        // - maxPoolSize: 连接池最大连接数
        // - minPoolSize: 连接池最小连接数
        // - connectTimeoutMS: 连接超时时间
        // - socketTimeoutMS: Socket 超时时间
        // - serverSelectionTimeoutMS: 服务器选择超时时间
        return MongoClients.create(mongoUri);
    }

    /**
     * 创建 MongoTemplate
     * 
     * <p>MongoTemplate 是 Spring Data MongoDB 的核心类</p>
     * <p>提供了丰富的数据库操作方法：</p>
     * <ul>
     *   <li>save/find/remove：基本 CRUD 操作</li>
     *   <li>aggregate：聚合查询</li>
     *   <li>updateFirst/updateMulti：更新操作</li>
     *   <li>indexOps：索引操作</li>
     * </ul>
     * 
     * @return MongoTemplate 实例
     */
    @Bean
    public MongoTemplate mongoTemplate() {
        return new MongoTemplate(mongoClient(), getDatabaseName());
    }
}
