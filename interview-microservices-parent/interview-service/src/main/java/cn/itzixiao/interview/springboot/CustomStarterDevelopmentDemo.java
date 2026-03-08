package cn.itzixiao.interview.springboot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.lang.annotation.*;

/**
 * 自定义 Spring Boot Starter 开发详解 - 教学型示例
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                      自定义 Starter 开发完整流程                             │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                        Starter 核心组成                             │   │
 * │  │                                                                     │   │
 * │  │   1. autoconfigure 模块                                             │   │
 * │  │      ├── XxxAutoConfiguration.java    # 自动配置类                  │   │
 * │  │      ├── XxxProperties.java           # 配置属性类                  │   │
 * │  │      └── XxxService.java              # 核心服务类                  │   │
 * │  │                                                                     │   │
 * │  │   2. starter 模块（可选，用于聚合依赖）                             │   │
 * │  │      └── pom.xml                      # 依赖管理                    │   │
 * │  │                                                                     │   │
 * │  │   3. 配置文件                                                       │   │
 * │  │      ├── META-INF/spring.factories              # Spring Boot 2.6-  │   │
 * │  │      └── META-INF/spring/...AutoConfiguration.imports  # 2.7+       │   │
 * │  │                                                                     │   │
 * │  │   4. 配置元数据（可选，提供 IDE 提示）                              │   │
 * │  │      └── META-INF/spring-configuration-metadata.json                │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                        Starter 命名规范                             │   │
 * │  │                                                                     │   │
 * │  │   官方 Starter：  spring-boot-starter-{name}                        │   │
 * │  │   示例：spring-boot-starter-web, spring-boot-starter-data-redis     │   │
 * │  │                                                                     │   │
 * │  │   第三方 Starter：{name}-spring-boot-starter                        │   │
 * │  │   示例：mybatis-spring-boot-starter, druid-spring-boot-starter      │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * │                                                                             │
 * │  ┌─────────────────────────────────────────────────────────────────────┐   │
 * │  │                     自动配置加载机制                                │   │
 * │  │                                                                     │   │
 * │  │   @SpringBootApplication                                            │   │
 * │  │          ↓                                                          │   │
 * │  │   @EnableAutoConfiguration                                          │   │
 * │  │          ↓                                                          │   │
 * │  │   AutoConfigurationImportSelector                                   │   │
 * │  │          ↓                                                          │   │
 * │  │   SpringFactoriesLoader 加载 spring.factories                       │   │
 * │  │          ↓                                                          │   │
 * │  │   获取所有自动配置类                                                 │   │
 * │  │          ↓                                                          │   │
 * │  │   @Conditional 条件过滤                                              │   │
 * │  │          ↓                                                          │   │
 * │  │   实例化符合条件的配置类                                             │   │
 * │  │          ↓                                                          │   │
 * │  │   创建 Bean 注入容器                                                 │   │
 * │  └─────────────────────────────────────────────────────────────────────┘   │
 * └─────────────────────────────────────────────────────────────────────────────┘
 */
public class CustomStarterDevelopmentDemo {

    public static void main(String[] args) {
        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                                                                   █");
        System.out.println("█           自定义 Spring Boot Starter 开发详解                     █");
        System.out.println("█                                                                   █");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println();

        // 第一部分：Starter 核心概念
        demonstrateStarterConcept();

        // 第二部分：自动配置类开发
        demonstrateAutoConfiguration();

        // 第三部分：配置属性类开发
        demonstrateConfigurationProperties();

        // 第四部分：条件注解详解
        demonstrateConditionalAnnotations();

        // 第五部分：配置元数据
        demonstrateConfigurationMetadata();

        // 第六部分：完整开发流程
        demonstrateCompleteDevelopmentProcess();

        // 第七部分：高级特性
        demonstrateAdvancedFeatures();

        // 第八部分：高频面试题
        interviewQuestions();

        System.out.println("\n");
        System.out.println("███████████████████████████████████████████████████████████████████");
        System.out.println("█                        全部演示完成                               █");
        System.out.println("███████████████████████████████████████████████████████████████████");
    }

    // ==================== 第一部分：Starter 核心概念 ====================

    private static void demonstrateStarterConcept() {
        System.out.println("========== 1. Starter 核心概念 ==========\n");

        System.out.println("【什么是 Starter？】");
        System.out.println("  Starter 是一组预定义的依赖集合 + 自动配置");
        System.out.println("  开发者只需引入 Starter，无需手动配置，即可使用相关功能\n");

        System.out.println("【Starter 的作用】");
        System.out.println("  1. 简化依赖管理：一个 Starter 包含所需的所有依赖");
        System.out.println("  2. 自动配置：根据条件自动创建和配置 Bean");
        System.out.println("  3. 开箱即用：提供合理的默认配置");
        System.out.println("  4. 可扩展：允许用户自定义覆盖默认配置\n");

        System.out.println("【Starter 组成结构】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  xxx-spring-boot-starter（Starter 模块 - 可选）                 │");
        System.out.println("│  ├── pom.xml                    # 聚合依赖                      │");
        System.out.println("│  │   └── dependency: xxx-spring-boot-autoconfigure              │");
        System.out.println("│  │   └── dependency: 其他必要依赖                               │");
        System.out.println("│  │                                                              │");
        System.out.println("│  xxx-spring-boot-autoconfigure（自动配置模块 - 核心）           │");
        System.out.println("│  ├── src/main/java/                                             │");
        System.out.println("│  │   └── com.xxx.autoconfigure/                                 │");
        System.out.println("│  │       ├── XxxAutoConfiguration.java    # 自动配置类          │");
        System.out.println("│  │       ├── XxxProperties.java           # 配置属性            │");
        System.out.println("│  │       └── XxxService.java              # 核心服务            │");
        System.out.println("│  └── src/main/resources/                                        │");
        System.out.println("│      └── META-INF/                                              │");
        System.out.println("│          ├── spring.factories             # SB 2.6 及以前       │");
        System.out.println("│          ├── spring/...AutoConfiguration.imports  # SB 2.7+    │");
        System.out.println("│          └── spring-configuration-metadata.json  # IDE 提示    │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第二部分：自动配置类开发 ====================

    private static void demonstrateAutoConfiguration() {
        System.out.println("========== 2. 自动配置类开发 ==========\n");

        System.out.println("【自动配置类核心注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @AutoConfiguration (Spring Boot 2.7+)                          │");
        System.out.println("│  或                                                             │");
        System.out.println("│  @Configuration(proxyBeanMethods = false)                       │");
        System.out.println("│                                                                 │");
        System.out.println("│  proxyBeanMethods = false 的作用：                              │");
        System.out.println("│  - 禁用 CGLIB 代理                                              │");
        System.out.println("│  - 提高启动性能                                                 │");
        System.out.println("│  - 但 @Bean 方法间相互调用不再保证单例                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【完整自动配置类示例】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @AutoConfiguration                                             │");
        System.out.println("│  @ConditionalOnClass(SmsClient.class)         // 类存在才生效  │");
        System.out.println("│  @EnableConfigurationProperties(SmsProperties.class) // 绑定属性│");
        System.out.println("│  @ConditionalOnProperty(                                        │");
        System.out.println("│      prefix = \"sms\",                                            │");
        System.out.println("│      name = \"enabled\",                                          │");
        System.out.println("│      havingValue = \"true\",                                      │");
        System.out.println("│      matchIfMissing = true    // 属性不存在时默认为 true        │");
        System.out.println("│  )                                                              │");
        System.out.println("│  public class SmsAutoConfiguration {                            │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Bean                                                      │");
        System.out.println("│      @ConditionalOnMissingBean   // 用户没定义时才创建          │");
        System.out.println("│      public SmsClient smsClient(SmsProperties props) {          │");
        System.out.println("│          return new SmsClient(                                  │");
        System.out.println("│              props.getAccessKey(),                              │");
        System.out.println("│              props.getSecretKey(),                              │");
        System.out.println("│              props.getSignName()                                │");
        System.out.println("│          );                                                     │");
        System.out.println("│      }                                                          │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Bean                                                      │");
        System.out.println("│      @ConditionalOnMissingBean                                  │");
        System.out.println("│      @ConditionalOnBean(SmsClient.class)  // SmsClient 存在时   │");
        System.out.println("│      public SmsTemplate smsTemplate(SmsClient client) {         │");
        System.out.println("│          return new SmsTemplate(client);                        │");
        System.out.println("│      }                                                          │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【@AutoConfiguration 顺序控制】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @AutoConfiguration(                                            │");
        System.out.println("│      before = DataSourceAutoConfiguration.class,  // 在之前加载│");
        System.out.println("│      after = JdbcTemplateAutoConfiguration.class  // 在之后加载│");
        System.out.println("│  )                                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  或使用注解：                                                   │");
        System.out.println("│  @AutoConfigureBefore(DataSourceAutoConfiguration.class)        │");
        System.out.println("│  @AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)       │");
        System.out.println("│  @AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)                │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第三部分：配置属性类开发 ====================

    private static void demonstrateConfigurationProperties() {
        System.out.println("========== 3. 配置属性类开发 ==========\n");

        System.out.println("【@ConfigurationProperties 基础用法】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @ConfigurationProperties(prefix = \"sms\")                       │");
        System.out.println("│  public class SmsProperties {                                   │");
        System.out.println("│                                                                 │");
        System.out.println("│      /**                                                        │");
        System.out.println("│       * AccessKey，用于API认证                                  │");
        System.out.println("│       */                                                        │");
        System.out.println("│      private String accessKey;                                  │");
        System.out.println("│                                                                 │");
        System.out.println("│      /**                                                        │");
        System.out.println("│       * SecretKey，用于签名                                     │");
        System.out.println("│       */                                                        │");
        System.out.println("│      private String secretKey;                                  │");
        System.out.println("│                                                                 │");
        System.out.println("│      /**                                                        │");
        System.out.println("│       * 签名名称                                                │");
        System.out.println("│       */                                                        │");
        System.out.println("│      private String signName = \"默认签名\";  // 默认值          │");
        System.out.println("│                                                                 │");
        System.out.println("│      /**                                                        │");
        System.out.println("│       * 是否启用                                                │");
        System.out.println("│       */                                                        │");
        System.out.println("│      private boolean enabled = true;                            │");
        System.out.println("│                                                                 │");
        System.out.println("│      // getter/setter                                           │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【对应的配置文件 application.yml】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  sms:                                                           │");
        System.out.println("│    access-key: your-access-key      # 驼峰转中划线             │");
        System.out.println("│    secret-key: your-secret-key                                  │");
        System.out.println("│    sign-name: 我的签名                                          │");
        System.out.println("│    enabled: true                                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【嵌套属性】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @ConfigurationProperties(prefix = \"sms\")                       │");
        System.out.println("│  public class SmsProperties {                                   │");
        System.out.println("│                                                                 │");
        System.out.println("│      private Pool pool = new Pool();  // 嵌套配置              │");
        System.out.println("│                                                                 │");
        System.out.println("│      public static class Pool {                                 │");
        System.out.println("│          private int maxSize = 10;                              │");
        System.out.println("│          private int minIdle = 2;                               │");
        System.out.println("│          private Duration maxWait = Duration.ofSeconds(30);     │");
        System.out.println("│          // getter/setter                                       │");
        System.out.println("│      }                                                          │");
        System.out.println("│  }                                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  # 对应配置：                                                   │");
        System.out.println("│  sms:                                                           │");
        System.out.println("│    pool:                                                        │");
        System.out.println("│      max-size: 20                                               │");
        System.out.println("│      min-idle: 5                                                │");
        System.out.println("│      max-wait: 60s                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【属性校验】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @ConfigurationProperties(prefix = \"sms\")                       │");
        System.out.println("│  @Validated                                                     │");
        System.out.println("│  public class SmsProperties {                                   │");
        System.out.println("│                                                                 │");
        System.out.println("│      @NotBlank(message = \"accessKey不能为空\")                   │");
        System.out.println("│      private String accessKey;                                  │");
        System.out.println("│                                                                 │");
        System.out.println("│      @NotBlank(message = \"secretKey不能为空\")                   │");
        System.out.println("│      private String secretKey;                                  │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Min(value = 1, message = \"超时时间最小为1秒\")             │");
        System.out.println("│      @Max(value = 300, message = \"超时时间最大为300秒\")         │");
        System.out.println("│      private int timeout = 30;                                  │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第四部分：条件注解详解 ====================

    private static void demonstrateConditionalAnnotations() {
        System.out.println("========== 4. 条件注解详解 ==========\n");

        System.out.println("【常用条件注解一览】");
        System.out.println("┌───────────────────────────────┬─────────────────────────────────┐");
        System.out.println("│  注解                         │  说明                           │");
        System.out.println("├───────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnClass          │  类路径存在指定类时生效         │");
        System.out.println("│  @ConditionalOnMissingClass   │  类路径不存在指定类时生效       │");
        System.out.println("├───────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnBean           │  容器中存在指定 Bean 时生效     │");
        System.out.println("│  @ConditionalOnMissingBean    │  容器中不存在指定 Bean 时生效   │");
        System.out.println("│  @ConditionalOnSingleCandidate│  容器中只有一个候选 Bean 时生效 │");
        System.out.println("├───────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnProperty       │  配置属性满足条件时生效         │");
        System.out.println("│  @ConditionalOnResource       │  指定资源存在时生效             │");
        System.out.println("├───────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnWebApplication │  是 Web 应用时生效              │");
        System.out.println("│  @ConditionalOnNotWebApplication│ 不是 Web 应用时生效           │");
        System.out.println("├───────────────────────────────┼─────────────────────────────────┤");
        System.out.println("│  @ConditionalOnExpression     │  SpEL 表达式为 true 时生效      │");
        System.out.println("│  @ConditionalOnJava           │  指定 Java 版本时生效           │");
        System.out.println("│  @ConditionalOnJndi           │  JNDI 环境可用时生效            │");
        System.out.println("└───────────────────────────────┴─────────────────────────────────┘\n");

        System.out.println("【@ConditionalOnProperty 详解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 属性存在且值为 true 时生效                                  │");
        System.out.println("│  @ConditionalOnProperty(                                        │");
        System.out.println("│      prefix = \"sms\",                                            │");
        System.out.println("│      name = \"enabled\",                                          │");
        System.out.println("│      havingValue = \"true\",                                      │");
        System.out.println("│      matchIfMissing = true   // 属性不存在时，默认匹配          │");
        System.out.println("│  )                                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  // 属性存在即生效（不关心值）                                  │");
        System.out.println("│  @ConditionalOnProperty(name = \"sms.access-key\")                │");
        System.out.println("│                                                                 │");
        System.out.println("│  // 多个属性同时满足                                            │");
        System.out.println("│  @ConditionalOnProperty(                                        │");
        System.out.println("│      prefix = \"sms\",                                            │");
        System.out.println("│      name = {\"access-key\", \"secret-key\"}                        │");
        System.out.println("│  )                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【自定义条件注解】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 1. 定义条件注解                                             │");
        System.out.println("│  @Target({ElementType.TYPE, ElementType.METHOD})                │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                            │");
        System.out.println("│  @Conditional(OnLinuxCondition.class)                           │");
        System.out.println("│  public @interface ConditionalOnLinux { }                       │");
        System.out.println("│                                                                 │");
        System.out.println("│  // 2. 实现 Condition 接口                                      │");
        System.out.println("│  public class OnLinuxCondition implements Condition {           │");
        System.out.println("│      @Override                                                  │");
        System.out.println("│      public boolean matches(ConditionContext context,           │");
        System.out.println("│                             AnnotatedTypeMetadata metadata) {   │");
        System.out.println("│          String os = System.getProperty(\"os.name\");             │");
        System.out.println("│          return os != null && os.toLowerCase().contains(\"linux\");│");
        System.out.println("│      }                                                          │");
        System.out.println("│  }                                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  // 3. 使用                                                     │");
        System.out.println("│  @Bean                                                          │");
        System.out.println("│  @ConditionalOnLinux                                            │");
        System.out.println("│  public LinuxService linuxService() { ... }                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第五部分：配置元数据 ====================

    private static void demonstrateConfigurationMetadata() {
        System.out.println("========== 5. 配置元数据（IDE 提示）==========\n");

        System.out.println("【spring-configuration-metadata.json】");
        System.out.println("  提供 IDE 自动补全和属性说明提示\n");

        System.out.println("【自动生成方式】");
        System.out.println("  添加依赖：");
        System.out.println("  <dependency>");
        System.out.println("      <groupId>org.springframework.boot</groupId>");
        System.out.println("      <artifactId>spring-boot-configuration-processor</artifactId>");
        System.out.println("      <optional>true</optional>");
        System.out.println("  </dependency>");
        System.out.println("  编译后自动生成 META-INF/spring-configuration-metadata.json\n");

        System.out.println("【手动补充元数据】");
        System.out.println("  创建 META-INF/additional-spring-configuration-metadata.json：");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  {                                                              │");
        System.out.println("│    \"properties\": [                                              │");
        System.out.println("│      {                                                          │");
        System.out.println("│        \"name\": \"sms.enabled\",                                   │");
        System.out.println("│        \"type\": \"java.lang.Boolean\",                             │");
        System.out.println("│        \"description\": \"是否启用短信服务\",                       │");
        System.out.println("│        \"defaultValue\": true                                     │");
        System.out.println("│      },                                                         │");
        System.out.println("│      {                                                          │");
        System.out.println("│        \"name\": \"sms.access-key\",                                │");
        System.out.println("│        \"type\": \"java.lang.String\",                              │");
        System.out.println("│        \"description\": \"短信服务AccessKey，从服务商获取\"         │");
        System.out.println("│      }                                                          │");
        System.out.println("│    ],                                                           │");
        System.out.println("│    \"hints\": [                                                   │");
        System.out.println("│      {                                                          │");
        System.out.println("│        \"name\": \"sms.provider\",                                  │");
        System.out.println("│        \"values\": [                                              │");
        System.out.println("│          {\"value\": \"aliyun\", \"description\": \"阿里云短信\"},    │");
        System.out.println("│          {\"value\": \"tencent\", \"description\": \"腾讯云短信\"}    │");
        System.out.println("│        ]                                                        │");
        System.out.println("│      }                                                          │");
        System.out.println("│    ]                                                            │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第六部分：完整开发流程 ====================

    private static void demonstrateCompleteDevelopmentProcess() {
        System.out.println("========== 6. 完整开发流程 ==========\n");

        System.out.println("【步骤1：创建项目结构】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  sms-spring-boot-starter/                                       │");
        System.out.println("│  ├── pom.xml                                                    │");
        System.out.println("│  └── src/main/                                                  │");
        System.out.println("│      ├── java/com/example/sms/                                  │");
        System.out.println("│      │   ├── SmsAutoConfiguration.java                          │");
        System.out.println("│      │   ├── SmsProperties.java                                 │");
        System.out.println("│      │   ├── SmsClient.java                                     │");
        System.out.println("│      │   └── SmsTemplate.java                                   │");
        System.out.println("│      └── resources/META-INF/                                    │");
        System.out.println("│          ├── spring/org.springframework.boot.autoconfigure.    │");
        System.out.println("│          │        AutoConfiguration.imports                     │");
        System.out.println("│          └── additional-spring-configuration-metadata.json      │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【步骤2：pom.xml 配置】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  <project>                                                      │");
        System.out.println("│    <groupId>com.example</groupId>                               │");
        System.out.println("│    <artifactId>sms-spring-boot-starter</artifactId>             │");
        System.out.println("│    <version>1.0.0</version>                                     │");
        System.out.println("│                                                                 │");
        System.out.println("│    <dependencies>                                               │");
        System.out.println("│      <!-- Spring Boot 自动配置 -->                              │");
        System.out.println("│      <dependency>                                               │");
        System.out.println("│        <groupId>org.springframework.boot</groupId>              │");
        System.out.println("│        <artifactId>spring-boot-autoconfigure</artifactId>       │");
        System.out.println("│      </dependency>                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│      <!-- 配置属性处理器（可选，生成元数据）-->                 │");
        System.out.println("│      <dependency>                                               │");
        System.out.println("│        <groupId>org.springframework.boot</groupId>              │");
        System.out.println("│        <artifactId>spring-boot-configuration-processor</artifactId>│");
        System.out.println("│        <optional>true</optional>                                │");
        System.out.println("│      </dependency>                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│      <!-- 第三方 SDK -->                                        │");
        System.out.println("│      <dependency>                                               │");
        System.out.println("│        <groupId>com.aliyun</groupId>                            │");
        System.out.println("│        <artifactId>aliyun-java-sdk-core</artifactId>            │");
        System.out.println("│        <version>4.6.0</version>                                 │");
        System.out.println("│      </dependency>                                              │");
        System.out.println("│    </dependencies>                                              │");
        System.out.println("│  </project>                                                     │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【步骤3：配置 AutoConfiguration.imports】");
        System.out.println("  文件：META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  com.example.sms.SmsAutoConfiguration                           │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【步骤4：使用 Starter】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 1. 引入依赖                                                 │");
        System.out.println("│  <dependency>                                                   │");
        System.out.println("│    <groupId>com.example</groupId>                               │");
        System.out.println("│    <artifactId>sms-spring-boot-starter</artifactId>             │");
        System.out.println("│    <version>1.0.0</version>                                     │");
        System.out.println("│  </dependency>                                                  │");
        System.out.println("│                                                                 │");
        System.out.println("│  // 2. 配置属性                                                 │");
        System.out.println("│  sms:                                                           │");
        System.out.println("│    access-key: xxx                                              │");
        System.out.println("│    secret-key: yyy                                              │");
        System.out.println("│    sign-name: 我的签名                                          │");
        System.out.println("│                                                                 │");
        System.out.println("│  // 3. 直接注入使用                                             │");
        System.out.println("│  @Service                                                       │");
        System.out.println("│  public class UserService {                                     │");
        System.out.println("│      @Autowired                                                 │");
        System.out.println("│      private SmsTemplate smsTemplate;                           │");
        System.out.println("│                                                                 │");
        System.out.println("│      public void sendCode(String phone, String code) {          │");
        System.out.println("│          smsTemplate.send(phone, \"SMS_CODE\", Map.of(\"code\", code));│");
        System.out.println("│      }                                                          │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第七部分：高级特性 ====================

    private static void demonstrateAdvancedFeatures() {
        System.out.println("========== 7. 高级特性 ==========\n");

        System.out.println("【多环境配置】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Configuration                                                 │");
        System.out.println("│  public class SmsAutoConfiguration {                            │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Bean                                                      │");
        System.out.println("│      @ConditionalOnProperty(name=\"sms.provider\", havingValue=\"aliyun\")│");
        System.out.println("│      public SmsClient aliyunSmsClient(SmsProperties props) {    │");
        System.out.println("│          return new AliyunSmsClient(props);                     │");
        System.out.println("│      }                                                          │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Bean                                                      │");
        System.out.println("│      @ConditionalOnProperty(name=\"sms.provider\", havingValue=\"tencent\")│");
        System.out.println("│      public SmsClient tencentSmsClient(SmsProperties props) {   │");
        System.out.println("│          return new TencentSmsClient(props);                    │");
        System.out.println("│      }                                                          │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【配置类拆分】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @AutoConfiguration                                             │");
        System.out.println("│  @EnableConfigurationProperties(SmsProperties.class)            │");
        System.out.println("│  @Import({                                                      │");
        System.out.println("│      SmsAutoConfiguration.AliyunConfiguration.class,            │");
        System.out.println("│      SmsAutoConfiguration.TencentConfiguration.class            │");
        System.out.println("│  })                                                             │");
        System.out.println("│  public class SmsAutoConfiguration {                            │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Configuration(proxyBeanMethods = false)                   │");
        System.out.println("│      @ConditionalOnClass(name = \"com.aliyuncs.DefaultAcsClient\")│");
        System.out.println("│      static class AliyunConfiguration { ... }                   │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Configuration(proxyBeanMethods = false)                   │");
        System.out.println("│      @ConditionalOnClass(name = \"com.tencentcloudapi.sms.v20210111.SmsClient\")│");
        System.out.println("│      static class TencentConfiguration { ... }                  │");
        System.out.println("│  }                                                              │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("【FailureAnalyzer 友好错误提示】");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  public class SmsConfigFailureAnalyzer                          │");
        System.out.println("│          extends AbstractFailureAnalyzer<SmsConfigException> {  │");
        System.out.println("│                                                                 │");
        System.out.println("│      @Override                                                  │");
        System.out.println("│      protected FailureAnalysis analyze(Throwable rootFailure,   │");
        System.out.println("│                                        SmsConfigException cause) {│");
        System.out.println("│          return new FailureAnalysis(                            │");
        System.out.println("│              \"SMS配置错误: \" + cause.getMessage(),               │");
        System.out.println("│              \"请检查application.yml中的sms配置:\\n\" +             │");
        System.out.println("│              \"1. 确保sms.access-key已配置\\n\" +                  │");
        System.out.println("│              \"2. 确保sms.secret-key已配置\"                       │");
        System.out.println("│          );                                                     │");
        System.out.println("│      }                                                          │");
        System.out.println("│  }                                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  // 在 spring.factories 中注册                                  │");
        System.out.println("│  org.springframework.boot.diagnostics.FailureAnalyzer=\\        │");
        System.out.println("│  com.example.sms.SmsConfigFailureAnalyzer                       │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第八部分：高频面试题 ====================

    private static void interviewQuestions() {
        System.out.println("==================== 高频面试题 ====================\n");

        // Q1
        System.out.println("【面试题1】Spring Boot 自动装配的原理是什么？\n");
        System.out.println("  核心流程：");
        System.out.println("  1. @SpringBootApplication 包含 @EnableAutoConfiguration");
        System.out.println("  2. @EnableAutoConfiguration 导入 AutoConfigurationImportSelector");
        System.out.println("  3. AutoConfigurationImportSelector 通过 SpringFactoriesLoader");
        System.out.println("     加载 META-INF/spring.factories 中的自动配置类");
        System.out.println("  4. 根据 @Conditional 条件过滤，保留符合条件的配置类");
        System.out.println("  5. 执行自动配置类，创建 Bean 并注入容器\n");

        // Q2
        System.out.println("【面试题2】如何自定义一个 Spring Boot Starter？\n");
        System.out.println("  步骤：");
        System.out.println("  1. 创建项目，命名为 xxx-spring-boot-starter");
        System.out.println("  2. 编写配置属性类（@ConfigurationProperties）");
        System.out.println("  3. 编写自动配置类（@AutoConfiguration + @Conditional）");
        System.out.println("  4. 创建 META-INF/spring/...AutoConfiguration.imports 文件");
        System.out.println("  5. 可选：添加配置元数据提供 IDE 提示");
        System.out.println("  6. 打包发布，其他项目引入依赖即可使用\n");

        // Q3
        System.out.println("【面试题3】@ConditionalOnClass 和 @ConditionalOnBean 的区别？\n");
        System.out.println("  @ConditionalOnClass：");
        System.out.println("  - 判断类路径（classpath）中是否存在某个类");
        System.out.println("  - 在配置类加载前判断");
        System.out.println("  - 用于判断某个依赖是否被引入");
        System.out.println("  ");
        System.out.println("  @ConditionalOnBean：");
        System.out.println("  - 判断 Spring 容器中是否存在某个 Bean");
        System.out.println("  - 在配置类加载时判断");
        System.out.println("  - 用于判断某个 Bean 是否已被创建\n");

        // Q4
        System.out.println("【面试题4】@ConditionalOnMissingBean 的作用？\n");
        System.out.println("  作用：");
        System.out.println("  - 容器中不存在指定 Bean 时，才创建当前 Bean");
        System.out.println("  - 允许用户自定义实现覆盖默认配置");
        System.out.println("  ");
        System.out.println("  示例场景：");
        System.out.println("  - 自动配置类创建默认的 DataSource Bean");
        System.out.println("  - 如果用户自定义了 DataSource Bean，则使用用户的");
        System.out.println("  - 否则使用自动配置创建的默认 Bean\n");

        // Q5
        System.out.println("【面试题5】Spring Boot 2.7 和之前版本的自动配置有什么区别？\n");
        System.out.println("  Spring Boot 2.7 之前：");
        System.out.println("  - 使用 META-INF/spring.factories");
        System.out.println("  - 所有类型的配置都放在同一个文件");
        System.out.println("  ");
        System.out.println("  Spring Boot 2.7+：");
        System.out.println("  - 自动配置使用 META-INF/spring/...AutoConfiguration.imports");
        System.out.println("  - 每行一个配置类，更清晰");
        System.out.println("  - spring.factories 仍可用，但已弃用\n");

        // Q6
        System.out.println("【面试题6】proxyBeanMethods = false 的作用是什么？\n");
        System.out.println("  作用：");
        System.out.println("  - 禁用 CGLIB 代理");
        System.out.println("  - 提高启动性能，减少内存占用");
        System.out.println("  ");
        System.out.println("  影响：");
        System.out.println("  - @Bean 方法之间相互调用时，不再保证单例");
        System.out.println("  - 每次调用都会创建新对象");
        System.out.println("  ");
        System.out.println("  建议：");
        System.out.println("  - 自动配置类建议设置为 false（大多数场景不需要方法间调用）");
        System.out.println("  - 如果需要方法间调用保证单例，设置为 true 或省略（默认 true）\n");

        // Q7
        System.out.println("【面试题7】如何排除某个自动配置类？\n");
        System.out.println("  方式一：注解排除");
        System.out.println("  @SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})");
        System.out.println("  ");
        System.out.println("  方式二：配置文件排除");
        System.out.println("  spring:");
        System.out.println("    autoconfigure:");
        System.out.println("      exclude:");
        System.out.println("        - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration");
        System.out.println("  ");
        System.out.println("  方式三：使用 excludeName（类名字符串）");
        System.out.println("  @SpringBootApplication(excludeName = \"...DataSourceAutoConfiguration\")\n");

        // Q8
        System.out.println("【面试题8】@EnableConfigurationProperties 的作用？\n");
        System.out.println("  作用：");
        System.out.println("  1. 使 @ConfigurationProperties 注解的类生效");
        System.out.println("  2. 将配置属性类注册为 Bean");
        System.out.println("  3. 自动绑定配置文件中的属性到配置类");
        System.out.println("  ");
        System.out.println("  使用方式：");
        System.out.println("  @AutoConfiguration");
        System.out.println("  @EnableConfigurationProperties(MyProperties.class)");
        System.out.println("  public class MyAutoConfiguration { ... }\n");

        // Q9
        System.out.println("【面试题9】SpringFactoriesLoader 的工作原理？\n");
        System.out.println("  原理：");
        System.out.println("  1. 扫描 classpath 下所有 jar 包的 META-INF/spring.factories");
        System.out.println("  2. 读取指定 key 对应的所有配置类全限定名");
        System.out.println("  3. 使用反射实例化这些类");
        System.out.println("  ");
        System.out.println("  spring.factories 格式：");
        System.out.println("  接口全限定名=实现类全限定名1,实现类全限定名2");
        System.out.println("  ");
        System.out.println("  这是 Java SPI 机制的扩展实现\n");

        // Q10
        System.out.println("【面试题10】如何调试自动配置？查看哪些配置生效了？\n");
        System.out.println("  方式一：启动参数");
        System.out.println("  java -jar app.jar --debug");
        System.out.println("  ");
        System.out.println("  方式二：配置文件");
        System.out.println("  debug: true");
        System.out.println("  ");
        System.out.println("  方式三：Actuator 端点");
        System.out.println("  GET /actuator/conditions");
        System.out.println("  ");
        System.out.println("  查看启动日志中的 CONDITIONS EVALUATION REPORT：");
        System.out.println("  - Positive matches：匹配的配置");
        System.out.println("  - Negative matches：不匹配的配置");
        System.out.println("  - Exclusions：被排除的配置\n");
    }

    // ==================== 示例代码：完整的 Starter 实现 ====================

    /**
     * 示例：配置属性类
     */
    @ConfigurationProperties(prefix = "example.sms")
    public static class ExampleSmsProperties {

        /**
         * 是否启用短信服务
         */
        private boolean enabled = true;

        /**
         * AccessKey
         */
        private String accessKey;

        /**
         * SecretKey
         */
        private String secretKey;

        /**
         * 签名名称
         */
        private String signName = "默认签名";

        /**
         * 连接池配置
         */
        private Pool pool = new Pool();

        public static class Pool {
            private int maxSize = 10;
            private int minIdle = 2;

            public int getMaxSize() { return maxSize; }
            public void setMaxSize(int maxSize) { this.maxSize = maxSize; }
            public int getMinIdle() { return minIdle; }
            public void setMinIdle(int minIdle) { this.minIdle = minIdle; }
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getSignName() { return signName; }
        public void setSignName(String signName) { this.signName = signName; }
        public Pool getPool() { return pool; }
        public void setPool(Pool pool) { this.pool = pool; }
    }

    /**
     * 示例：核心服务类
     */
    public static class ExampleSmsClient {

        private final String accessKey;
        private final String secretKey;
        private final String signName;

        public ExampleSmsClient(String accessKey, String secretKey, String signName) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
            this.signName = signName;
        }

        public void send(String phone, String templateCode, String params) {
            System.out.println("发送短信: phone=" + phone + ", template=" + templateCode);
        }
    }

    /**
     * 示例：自动配置类
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ExampleSmsClient.class)
    @EnableConfigurationProperties(ExampleSmsProperties.class)
    @ConditionalOnProperty(prefix = "example.sms", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class ExampleSmsAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public ExampleSmsClient exampleSmsClient(ExampleSmsProperties properties) {
            return new ExampleSmsClient(
                    properties.getAccessKey(),
                    properties.getSecretKey(),
                    properties.getSignName()
            );
        }
    }

    /**
     * 示例：自定义条件注解
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Conditional(OnWindowsCondition.class)
    public @interface ConditionalOnWindows {
    }

    /**
     * 示例：自定义条件实现
     */
    public static class OnWindowsCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String os = System.getProperty("os.name");
            return os != null && os.toLowerCase().contains("windows");
        }
    }
}
