package cn.itzixiao.interview.springboot.autoconfig;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 自定义应用上下文初始化器
 * <p>
 * 执行时机：在 Spring 应用上下文刷新之前，ConfigurableApplicationContext 创建之后
 * 用途：在上下文刷新前进行自定义设置，如添加 PropertySource、设置 Profiles 等
 */
public class MyContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        System.out.println("【ApplicationContextInitializer】上下文初始化...");

        // 可以在这里：
        // 1. 添加自定义的 PropertySource
        // 2. 激活特定的 Profile
        // 3. 注册自定义的 BeanDefinition
        // 4. 设置环境变量

        System.out.println("  - 应用名称: " + applicationContext.getApplicationName());
        System.out.println("  - 环境: " + applicationContext.getEnvironment());
    }
}
