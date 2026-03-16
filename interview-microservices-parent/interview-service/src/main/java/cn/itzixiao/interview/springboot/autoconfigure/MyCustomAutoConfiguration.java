package cn.itzixiao.interview.springboot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义自动配置类示例
 * <p>
 * 这个类展示了如何编写一个 Spring Boot 自动配置类
 * 当满足条件时，会自动创建 MyCustomService Bean
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MyCustomService.class)
@EnableConfigurationProperties(MyCustomProperties.class)
@ConditionalOnProperty(
        prefix = "my.custom",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MyCustomAutoConfiguration {

    private final MyCustomProperties properties;

    public MyCustomAutoConfiguration(MyCustomProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    public MyCustomService myCustomService() {
        System.out.println("[MyCustomAutoConfiguration] 创建 MyCustomService");
        System.out.println("  配置名称: " + properties.getName());
        System.out.println("  配置值: " + properties.getValue());
        return new MyCustomService(properties.getName(), properties.getValue());
    }
}
