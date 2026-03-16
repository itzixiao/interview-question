package cn.itzixiao.interview.springboot.autoconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义环境后置处理器
 * <p>
 * 执行时机：在应用环境准备好之后，在 ApplicationContext 创建之前
 * 用途：动态修改环境配置，如从远程配置中心加载配置
 */
public class MyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        System.out.println("【EnvironmentPostProcessor】环境后置处理...");

        // 可以在这里：
        // 1. 从配置中心加载配置
        // 2. 解密加密的环境变量
        // 3. 添加自定义的 PropertySource
        // 4. 根据环境动态调整配置

        Map<String, Object> customProperties = new HashMap<>();
        customProperties.put("custom.property", "fromEnvironmentPostProcessor");
        customProperties.put("custom.timestamp", System.currentTimeMillis());

        MapPropertySource propertySource = new MapPropertySource("customProperties", customProperties);
        environment.getPropertySources().addFirst(propertySource);

        System.out.println("  - 已添加自定义 PropertySource");
        System.out.println("  - 激活的 Profiles: " + String.join(",", environment.getActiveProfiles()));
    }
}
