package cn.itzixiao.interview.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 服务提供者启动类
 * <p>
 * 用于演示 OpenFeign 微服务调用的服务端
 * <p>
 * ShardingSphere 5.4.1 与 Spring Boot 3.2.x 存在已知兼容性问题（factoryBeanObjectType 冲突），
 * 已通过在 application-dev.yml 中设置 spring.shardingsphere.enabled=false 禁用 ShardingDataSourceConfig，
 * DataSource 由 MyBatis-Plus 自动配置提供。
 */
@SpringBootApplication(
        scanBasePackages = {"cn.itzixiao.interview"},
        excludeName = {
                "org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration",
                "org.apache.shardingsphere.spring.boot.ShardingSphereJdbcAutoConfiguration"
        }
)
@EnableDiscoveryClient
public class ProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
