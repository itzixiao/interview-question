package cn.itzixiao.interview.springboot.autoconfig;

import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;

/**
 * SpringFactoriesLoader 详解
 *
 * 作用：从 META-INF/spring.factories 文件加载配置
 *
 * spring.factories 文件格式：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  # 注释                                                      │
 * │  接口全限定名=实现类1,实现类2,实现类3                        │
 * │                                                              │
 * │  示例：                                                      │
 * │  org.springframework.boot.autoconfigure.EnableAutoConfiguration=\│
 * │    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\│
 * │    org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration│
 * └─────────────────────────────────────────────────────────────┘
 *
 * Spring Boot 2.7+ 新特性：
 * - 支持 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * - 每行一个自动配置类，更清晰
 */
public class SpringFactoriesLoaderDemo {

    /**
     * SpringFactoriesLoader 使用示例
     */
    public static void main(String[] args) {
        System.out.println("========== SpringFactoriesLoader 详解 ==========\n");

        System.out.println("【核心方法】");
        System.out.println("1. loadFactoryNames(Class<?> factoryClass, ClassLoader classLoader)");
        System.out.println("   - 加载指定接口的所有实现类名称");
        System.out.println("   - 返回 List<String>\n");

        System.out.println("2. loadFactories(Class<T> factoryClass, ClassLoader classLoader)");
        System.out.println("   - 加载并实例化所有实现类");
        System.out.println("   - 返回 List<T>\n");

        System.out.println("【加载位置】");
        System.out.println("  - 所有 classpath 下的 META-INF/spring.factories");
        System.out.println("  - 包括项目自身和引入的 jar 包\n");

        System.out.println("【Spring Boot 标准配置键】");
        System.out.println("  - EnableAutoConfiguration：自动配置类");
        System.out.println("  - ApplicationContextInitializer：上下文初始化器");
        System.out.println("  - ApplicationListener：应用监听器");
        System.out.println("  - SpringApplicationRunListener：运行监听器");
        System.out.println("  - EnvironmentPostProcessor：环境后置处理器");
        System.out.println("  - FailureAnalyzer：故障分析器\n");

        // 实际加载示例（需要真实环境）
        // List<String> autoConfigs = SpringFactoriesLoader.loadFactoryNames(
        //     org.springframework.boot.autoconfigure.EnableAutoConfiguration.class,
        //     SpringFactoriesLoaderDemo.class.getClassLoader()
        // );
        // System.out.println("加载到的自动配置类数量: " + autoConfigs.size());
    }

    /**
     * 完整的 spring.factories 示例
     */
    public void springFactoriesExample() {
        System.out.println("\n========== spring.factories 完整示例 ==========\n");

        System.out.println("# Auto Configure");
        System.out.println("org.springframework.boot.autoconfigure.EnableAutoConfiguration=\\");
        System.out.println("  com.example.autoconfigure.MyAutoConfiguration\\\n");

        System.out.println("# Application Context Initializers");
        System.out.println("org.springframework.context.ApplicationContextInitializer=\\");
        System.out.println("  com.example.initializer.MyContextInitializer\\\n");

        System.out.println("# Application Listeners");
        System.out.println("org.springframework.context.ApplicationListener=\\");
        System.out.println("  com.example.listener.MyApplicationListener\\\n");

        System.out.println("# Environment Post Processors");
        System.out.println("org.springframework.boot.env.EnvironmentPostProcessor=\\");
        System.out.println("  com.example.env.MyEnvironmentPostProcessor\\\n");

        System.out.println("# Failure Analyzers");
        System.out.println("org.springframework.boot.diagnostics.FailureAnalyzer=\\");
        System.out.println("  com.example.diagnostics.MyFailureAnalyzer\\\n");

        System.out.println("# Spring Boot 2.7+ 新方式");
        System.out.println("# 文件：META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        System.out.println("com.example.autoconfigure.MyAutoConfiguration");
        System.out.println("com.example.autoconfigure.OtherAutoConfiguration\n");
    }
}
