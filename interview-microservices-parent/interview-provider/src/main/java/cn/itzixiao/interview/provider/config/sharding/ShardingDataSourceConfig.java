package cn.itzixiao.interview.provider.config.sharding;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * ShardingSphere 5.x 手动编程式数据源配置
 * <p>
 * 通过 Java API 直接构建 ShardingSphereDataSource，完全绕过 Spring Boot 自动配置，
 * 解决 ShardingSphere 5.4.1 与 Spring Boot 3.2.x 的 factoryBeanObjectType 兼容性问题。
 * <p>
 * 仅在 spring.shardingsphere.enabled=true 时激活（默认 true），
 * 关闭时回退到普通 DataSource（由 DataSourceAutoConfiguration 提供）。
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.shardingsphere", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ShardingDataSourceConfig {

    @Value("${spring.shardingsphere.datasource.ds0.jdbc-url}")
    private String jdbcUrl;

    @Value("${spring.shardingsphere.datasource.ds0.username}")
    private String username;

    @Value("${spring.shardingsphere.datasource.ds0.password:}")
    private String password;

    @Value("${spring.shardingsphere.datasource.ds0.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.shardingsphere.datasource.ds0.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.shardingsphere.props.sql-show:true}")
    private boolean sqlShow;

    /**
     * 构建 ShardingSphere DataSource（编程式 API，不走 Spring Boot 自动配置）
     */
    @Bean
    @Primary
    public DataSource shardingDataSource() throws SQLException {
        // 1. 构建底层 HikariCP 数据源
        HikariDataSource ds0 = new HikariDataSource();
        ds0.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds0.setJdbcUrl(jdbcUrl);
        ds0.setUsername(username);
        ds0.setPassword(password);
        ds0.setMinimumIdle(minimumIdle);
        ds0.setMaximumPoolSize(maximumPoolSize);
        ds0.setConnectionTimeout(30000);
        ds0.setIdleTimeout(600000);
        ds0.setMaxLifetime(1800000);

        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("ds0", ds0);

        // 2. 配置分片规则
        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        // 分片表规则：device_operation_log 按月分片
        String actualDataNodes = buildActualDataNodes();
        ShardingTableRuleConfiguration tableRule =
                new ShardingTableRuleConfiguration("device_operation_log", actualDataNodes);

        // 表分片策略：使用自定义算法
        tableRule.setTableShardingStrategy(
                new StandardShardingStrategyConfiguration("operation_time", "device-log-month-algorithm")
        );
        shardingRuleConfig.getTables().add(tableRule);

        // 3. 注册分片算法（CLASS_BASED 类型，引用自定义算法类）
        Properties algorithmProps = new Properties();
        algorithmProps.setProperty("strategy", "STANDARD");
        algorithmProps.setProperty("algorithmClassName",
                "cn.itzixiao.interview.provider.config.sharding.DeviceOperationLogMonthShardingAlgorithm");
        shardingRuleConfig.getShardingAlgorithms().put(
                "device-log-month-algorithm",
                new AlgorithmConfiguration("CLASS_BASED", algorithmProps)
        );

        // 4. 全局属性
        Properties props = new Properties();
        props.setProperty("sql-show", String.valueOf(sqlShow));

        // 5. 使用 ShardingSphereDataSourceFactory 构建（编程式，不触发 Spring 自动配置）
        return ShardingSphereDataSourceFactory.createDataSource(
                dataSourceMap,
                Collections.singleton(shardingRuleConfig),
                props
        );
    }

    /**
     * 注册事务管理器（排除 DataSourceTransactionManagerAutoConfiguration 后需手动提供）
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(DataSource shardingDataSource) {
        return new DataSourceTransactionManager(shardingDataSource);
    }

    /**
     * 构建实际数据节点字符串：ds0.device_operation_log_202601,...,ds0.device_operation_log_202612
     */
    private String buildActualDataNodes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 12; i++) {
            if (i > 1) sb.append(",");
            sb.append(String.format("ds0.device_operation_log_2026%02d", i));
        }
        return sb.toString();
    }
}
