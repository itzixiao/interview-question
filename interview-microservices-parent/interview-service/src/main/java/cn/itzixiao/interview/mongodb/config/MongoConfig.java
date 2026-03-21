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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
@ConditionalOnProperty(name = "mongodb.enabled", havingValue = "true", matchIfMissing = false)
public class MongoConfig extends AbstractMongoClientConfiguration {

    /**
     * MongoDB 用户名
     */
    @Value("${spring.data.mongodb.username:root}")
    private String username;

    /**
     * MongoDB 密码
     */
    @Value("${spring.data.mongodb.password:root}")
    private String password;

    /**
     * MongoDB 主机地址
     */
    @Value("${spring.data.mongodb.host:localhost}")
    private String host;

    /**
     * MongoDB 端口
     */
    @Value("${spring.data.mongodb.port:27017}")
    private int port;

    /**
     * MongoDB 认证数据库
     */
    @Value("${spring.data.mongodb.authentication-database:admin}")
    private String authDatabase;

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
     * <p>连接字符串支持多种配置选项：</p>
     * <ul>
     *   <li>maxPoolSize: 连接池最大连接数</li>
     *   <li>minPoolSize: 连接池最小连接数</li>
     *   <li>connectTimeoutMS: 连接超时时间</li>
     *   <li>socketTimeoutMS: Socket 超时时间</li>
     *   <li>serverSelectionTimeoutMS: 服务器选择超时时间</li>
     * </ul>
     * 
     * @return MongoClient 实例
     */
    @Override
    @Bean(destroyMethod = "close")
    public MongoClient mongoClient() {
        // 构建连接字符串，自动对用户名和密码进行URL编码
        // 这样可以正确处理包含特殊字符（如 : @ 等）的密码
        String encodedUsername = urlEncode(username);
        String encodedPassword = urlEncode(password);
        
        String connectionString = String.format(
            "mongodb://%s:%s@%s:%d/%s?authSource=%s",
            encodedUsername, encodedPassword, host, port, databaseName, authDatabase
        );
        
        return MongoClients.create(connectionString);
    }

    /**
     * 对字符串进行URL编码
     * 
     * <p>MongoDB连接字符串中，用户名和密码如果包含特殊字符（如 : @ / 等），
     * 必须进行URL编码才能正确解析</p>
     * 
     * @param value 需要编码的字符串
     * @return URL编码后的字符串
     */
    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 是标准编码，理论上不会抛出此异常
            throw new RuntimeException("Failed to encode MongoDB credential", e);
        }
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
