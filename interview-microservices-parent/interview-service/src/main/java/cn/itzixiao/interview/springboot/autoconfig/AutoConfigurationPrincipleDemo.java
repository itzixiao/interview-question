package cn.itzixiao.interview.springboot.autoconfig;

/**
 * Spring Boot 自动装配原理详解
 * <p>
 * 自动装配核心流程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 入口：@SpringBootApplication                             │
 * │     @SpringBootConfiguration                                │
 * │     @EnableAutoConfiguration  ← 自动装配入口                 │
 * │     @ComponentScan                                          │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. @EnableAutoConfiguration                                │
 * │     @Import(AutoConfigurationImportSelector.class)          │
 * │     └→ AutoConfigurationImportSelector                     │
 * │         └→ selectImports()                                  │
 * │             └→ getAutoConfigurationEntry()                  │
 * │                 └→ getCandidateConfigurations()             │
 * │                     └→ SpringFactoriesLoader                │
 * │                         └→ 加载 META-INF/spring.factories   │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. 过滤配置类                                               │
 * │     - @ConditionalOnClass：类路径存在某类时才生效           │
 * │     - @ConditionalOnMissingBean：容器不存在某Bean时才生效   │
 * │     - @ConditionalOnProperty：配置文件中某属性满足条件      │
 * │     - @ConditionalOnWebApplication：是Web应用时才生效       │
 * ├─────────────────────────────────────────────────────────────┤
 * │  4. 执行配置类                                               │
 * │     - 创建配置类中定义的 Bean                                │
 * │     - 注入配置属性（@EnableConfigurationProperties）        │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * spring.factories 文件格式：
 * org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
 * com.example.XXXAutoConfiguration,\
 * com.example.YYYAutoConfiguration
 */
public class AutoConfigurationPrincipleDemo {

    /**
     * 自动装配流程演示
     */
    public static void main(String[] args) {
        System.out.println("========== Spring Boot 自动装配原理 ==========\n");

        System.out.println("【步骤1】Spring Boot 启动");
        System.out.println("  - 运行 SpringApplication.run()\n");

        System.out.println("【步骤2】解析 @SpringBootApplication");
        System.out.println("  - @SpringBootConfiguration：标识配置类");
        System.out.println("  - @EnableAutoConfiguration：开启自动装配");
        System.out.println("  - @ComponentScan：组件扫描\n");

        System.out.println("【步骤3】@EnableAutoConfiguration 生效");
        System.out.println("  - 导入 AutoConfigurationImportSelector");
        System.out.println("  - 调用 selectImports() 方法\n");

        System.out.println("【步骤4】加载候选配置类");
        System.out.println("  - SpringFactoriesLoader.loadFactoryNames()");
        System.out.println("  - 从所有 jar 包的 META-INF/spring.factories 读取");
        System.out.println("  - 读取 key = EnableAutoConfiguration 的配置\n");

        System.out.println("【步骤5】过滤配置类");
        System.out.println("  - 去重（RemoveDuplicates）");
        System.out.println("  - 排除（@EnableAutoConfiguration.exclude）");
        System.out.println("  - 过滤（@ConditionalXxx 条件注解）\n");

        System.out.println("【步骤6】执行配置类");
        System.out.println("  - 创建配置类中定义的 Bean");
        System.out.println("  - 绑定配置属性\n");

        System.out.println("【关键类】");
        System.out.println("  - AutoConfigurationImportSelector：自动配置选择器");
        System.out.println("  - SpringFactoriesLoader：Spring工厂加载器");
        System.out.println("  - AutoConfigurationMetadata：自动配置元数据");
        System.out.println("  - ConditionEvaluator：条件评估器\n");
    }

    /**
     * 条件注解详解
     */
    public void conditionalAnnotations() {
        System.out.println("\n========== 条件注解详解 ==========\n");

        System.out.println("【@ConditionalOnClass】");
        System.out.println("  作用：类路径存在指定类时才生效");
        System.out.println("  示例：@ConditionalOnClass(name = \"com.mysql.jdbc.Driver\")");
        System.out.println("  用途：有 MySQL 驱动时才配置数据源\n");

        System.out.println("【@ConditionalOnMissingClass】");
        System.out.println("  作用：类路径不存在指定类时才生效\n");

        System.out.println("【@ConditionalOnBean】");
        System.out.println("  作用：容器中存在指定 Bean 时才生效\n");

        System.out.println("【@ConditionalOnMissingBean】");
        System.out.println("  作用：容器中不存在指定 Bean 时才生效");
        System.out.println("  示例：用户未自定义 DataSource 时才创建默认数据源\n");

        System.out.println("【@ConditionalOnProperty】");
        System.out.println("  作用：配置文件中指定属性满足条件时才生效");
        System.out.println("  示例：@ConditionalOnProperty(prefix=\"spring.datasource\", name=\"url\")\n");

        System.out.println("【@ConditionalOnWebApplication】");
        System.out.println("  作用：是 Web 应用时才生效\n");

        System.out.println("【@ConditionalOnExpression】");
        System.out.println("  作用：SpEL 表达式为 true 时才生效\n");
    }

    /**
     * 自动配置示例：DataSourceAutoConfiguration 简化版
     */
    public void dataSourceAutoConfigurationExample() {
        System.out.println("\n========== DataSourceAutoConfiguration 示例 ==========\n");

        System.out.println("@Configuration(proxyBeanMethods = false)");
        System.out.println("@ConditionalOnClass({ DataSource.class, EmbeddedDatabaseType.class })");
        System.out.println("@EnableConfigurationProperties(DataSourceProperties.class)");
        System.out.println("@Import({ DataSourcePoolMetadataProvidersConfiguration.class,");
        System.out.println("        DataSourceInitializationConfiguration.class })");
        System.out.println("public class DataSourceAutoConfiguration {");
        System.out.println(" ");
        System.out.println("    @Configuration(proxyBeanMethods = false)");
        System.out.println("    @Conditional(EmbeddedDatabaseCondition.class)");
        System.out.println("    @ConditionalOnMissingBean({ DataSource.class, XADataSource.class })");
        System.out.println("    protected static class EmbeddedDatabaseConfiguration {");
        System.out.println(" ");
        System.out.println("        @Bean");
        System.out.println("        public EmbeddedDatabaseFactoryBean dataSource(");
        System.out.println("                DataSourceProperties properties) {");
        System.out.println("            // 创建嵌入式数据源");
        System.out.println("        }");
        System.out.println("    }");
        System.out.println(" ");
        System.out.println("    @Configuration(proxyBeanMethods = false)");
        System.out.println("    @Conditional(PooledDataSourceCondition.class)");
        System.out.println("    @ConditionalOnMissingBean({ DataSource.class, XADataSource.class })");
        System.out.println("    @Import({ DataSourceConfiguration.Hikari.class,");
        System.out.println("            DataSourceConfiguration.Tomcat.class,");
        System.out.println("            DataSourceConfiguration.Dbcp2.class,");
        System.out.println("            DataSourceConfiguration.Generic.class })");
        System.out.println("    protected static class PooledDataSourceConfiguration {");
        System.out.println("        // 连接池数据源配置");
        System.out.println("    }");
        System.out.println("}");
    }
}
