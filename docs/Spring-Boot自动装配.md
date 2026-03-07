# Spring Boot 自动装配原理与自定义 Starter 开发详解

## 一、自动装配概述

Spring Boot 自动装配（Auto-Configuration）是 Spring Boot 的核心特性，根据 classpath 中的依赖自动配置 Spring 应用。

### 核心流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Spring Boot 自动装配流程                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   @SpringBootApplication                                                    │
│          ↓                                                                  │
│   @EnableAutoConfiguration                                                  │
│          ↓                                                                  │
│   @Import(AutoConfigurationImportSelector.class)                            │
│          ↓                                                                  │
│   selectImports() → getAutoConfigurationEntry()                             │
│          ↓                                                                  │
│   SpringFactoriesLoader 加载 META-INF/spring.factories                      │
│          ↓                                                                  │
│   获取所有自动配置类候选列表                                                 │
│          ↓                                                                  │
│   @Conditional 条件过滤（OnClass、OnBean、OnProperty等）                     │
│          ↓                                                                  │
│   实例化符合条件的配置类，创建 Bean 注入容器                                 │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、核心注解详解

### @SpringBootApplication

组合注解，包含三个核心注解：

```java
@SpringBootConfiguration    // 标识为配置类（等同于 @Configuration）
@EnableAutoConfiguration    // 启用自动装配
@ComponentScan             // 组件扫描（默认扫描当前包及子包）
public @interface SpringBootApplication {
}
```

### @EnableAutoConfiguration

```java
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
}
```

**关键实现**：`AutoConfigurationImportSelector.selectImports()` 方法负责加载所有自动配置类。

---

## 三、自动配置加载机制

### 1. SpringFactoriesLoader

从 `META-INF/spring.factories` 加载配置：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration,\
  org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,\
  org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

**Spring Boot 2.7+ 新方式**：
```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

每行一个配置类，更清晰：
```
org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
```

### 2. 条件注解

| 条件注解 | 说明 |
|----------|------|
| @ConditionalOnClass | 类路径存在指定类时生效 |
| @ConditionalOnMissingClass | 类路径不存在指定类时生效 |
| @ConditionalOnBean | 容器中存在指定 Bean 时生效 |
| @ConditionalOnMissingBean | 容器中不存在指定 Bean 时生效 |
| @ConditionalOnProperty | 配置属性满足条件时生效 |
| @ConditionalOnWebApplication | 是 Web 应用时生效 |
| @ConditionalOnResource | 指定资源存在时生效 |
| @ConditionalOnExpression | SpEL 表达式为 true 时生效 |

### 3. 配置属性绑定

```java
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {
    private String url;
    private String username;
    private String password;
    private String driverClassName;
    // getter/setter
}
```

---

## 四、自定义 Starter 开发详解

### 1. Starter 命名规范

| 类型 | 命名规范 | 示例 |
|------|---------|------|
| 官方 Starter | spring-boot-starter-{name} | spring-boot-starter-web |
| 第三方 Starter | {name}-spring-boot-starter | mybatis-spring-boot-starter |

### 2. 项目结构

```
sms-spring-boot-starter/
├── pom.xml
└── src/main/
    ├── java/com/example/sms/
    │   ├── SmsAutoConfiguration.java       # 自动配置类
    │   ├── SmsProperties.java              # 配置属性类
    │   ├── SmsClient.java                  # 核心客户端
    │   └── SmsTemplate.java                # 操作模板
    └── resources/META-INF/
        ├── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
        └── additional-spring-configuration-metadata.json  # IDE 提示
```

### 3. pom.xml 配置

```xml
<project>
    <groupId>com.example</groupId>
    <artifactId>sms-spring-boot-starter</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <!-- Spring Boot 自动配置 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        
        <!-- 配置属性处理器（生成 IDE 提示元数据）-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- 第三方 SDK -->
        <dependency>
            <groupId>com.aliyun</groupId>
            <artifactId>aliyun-java-sdk-dysmsapi</artifactId>
            <version>2.2.1</version>
        </dependency>
    </dependencies>
</project>
```

### 4. 配置属性类

```java
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    /**
     * 是否启用短信服务
     */
    private boolean enabled = true;

    /**
     * AccessKey，从服务商获取
     */
    private String accessKey;

    /**
     * SecretKey，从服务商获取
     */
    private String secretKey;

    /**
     * 签名名称
     */
    private String signName;

    /**
     * 连接池配置
     */
    private Pool pool = new Pool();

    public static class Pool {
        /**
         * 最大连接数
         */
        private int maxSize = 10;
        
        /**
         * 最小空闲连接
         */
        private int minIdle = 2;
        
        // getter/setter
    }

    // getter/setter
}
```

对应的 `application.yml`：
```yaml
sms:
  enabled: true
  access-key: your-access-key
  secret-key: your-secret-key
  sign-name: 我的签名
  pool:
    max-size: 20
    min-idle: 5
```

### 5. 自动配置类

```java
@AutoConfiguration
@ConditionalOnClass(SmsClient.class)
@EnableConfigurationProperties(SmsProperties.class)
@ConditionalOnProperty(
    prefix = "sms",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true  // 属性不存在时默认为 true
)
public class SmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean  // 用户没定义时才创建
    public SmsClient smsClient(SmsProperties properties) {
        return new SmsClient(
            properties.getAccessKey(),
            properties.getSecretKey()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SmsClient.class)
    public SmsTemplate smsTemplate(SmsClient client, SmsProperties properties) {
        return new SmsTemplate(client, properties.getSignName());
    }
}
```

### 6. 核心服务类

```java
public class SmsClient {
    private final String accessKey;
    private final String secretKey;

    public SmsClient(String accessKey, String secretKey) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public SendResult send(String phone, String templateCode, Map<String, String> params) {
        // 调用第三方 SDK 发送短信
        return new SendResult(true, "发送成功");
    }
}

public class SmsTemplate {
    private final SmsClient client;
    private final String signName;

    public SmsTemplate(SmsClient client, String signName) {
        this.client = client;
        this.signName = signName;
    }

    public void sendCode(String phone, String code) {
        client.send(phone, "SMS_CODE_TEMPLATE", Map.of("code", code));
    }
}
```

### 7. 配置自动配置导入文件

**Spring Boot 2.7+**：

创建文件 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`：
```
com.example.sms.SmsAutoConfiguration
```

**Spring Boot 2.6 及以前**：

创建文件 `META-INF/spring.factories`：
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.sms.SmsAutoConfiguration
```

### 8. 配置元数据（IDE 提示）

创建 `META-INF/additional-spring-configuration-metadata.json`：

```json
{
  "properties": [
    {
      "name": "sms.enabled",
      "type": "java.lang.Boolean",
      "description": "是否启用短信服务",
      "defaultValue": true
    },
    {
      "name": "sms.access-key",
      "type": "java.lang.String",
      "description": "短信服务 AccessKey，从服务商获取"
    }
  ],
  "hints": [
    {
      "name": "sms.provider",
      "values": [
        { "value": "aliyun", "description": "阿里云短信" },
        { "value": "tencent", "description": "腾讯云短信" }
      ]
    }
  ]
}
```

### 9. 使用自定义 Starter

```java
// 1. 引入依赖
// <dependency>
//     <groupId>com.example</groupId>
//     <artifactId>sms-spring-boot-starter</artifactId>
//     <version>1.0.0</version>
// </dependency>

// 2. 直接注入使用
@Service
public class UserService {
    
    @Autowired
    private SmsTemplate smsTemplate;
    
    public void sendVerifyCode(String phone) {
        String code = generateCode();
        smsTemplate.sendCode(phone, code);
    }
}
```

---

## 五、自动配置排除

### 方式一：注解排除

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
```

### 方式二：配置文件排除

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

## 六、调试自动配置

### 查看生效的自动配置

```bash
java -jar app.jar --debug
# 或
debug: true  # application.yml
```

### 条件评估报告

启动日志中查看 `CONDITIONS EVALUATION REPORT`：

```
============================
CONDITIONS EVALUATION REPORT
============================

Positive matches:（已匹配的配置）
-----------------
DataSourceAutoConfiguration matched:
  - @ConditionalOnClass found required classes 'javax.sql.DataSource'

Negative matches:（未匹配的配置）
-----------------
RedisAutoConfiguration:
  Did not match:
    - @ConditionalOnClass did not find required class 'org.springframework.data.redis.core.RedisOperations'

Exclusions:（已排除的配置）
-----------
- org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

---

## 七、高级特性

### 1. 配置类加载顺序控制

```java
@AutoConfiguration(
    before = DataSourceAutoConfiguration.class,
    after = JdbcTemplateAutoConfiguration.class
)
public class MyAutoConfiguration { }

// 或使用注解
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
```

### 2. 多环境配置

```java
@AutoConfiguration
public class SmsAutoConfiguration {

    @Bean
    @ConditionalOnProperty(name = "sms.provider", havingValue = "aliyun")
    public SmsClient aliyunSmsClient(SmsProperties props) {
        return new AliyunSmsClient(props);
    }

    @Bean
    @ConditionalOnProperty(name = "sms.provider", havingValue = "tencent")
    public SmsClient tencentSmsClient(SmsProperties props) {
        return new TencentSmsClient(props);
    }
}
```

### 3. proxyBeanMethods = false

```java
@Configuration(proxyBeanMethods = false)
public class MyAutoConfiguration {
    // 禁用 CGLIB 代理，提高启动性能
    // 但 @Bean 方法间相互调用不再保证单例
}
```

---

## 八、最佳实践

1. **配置类加 `proxyBeanMethods = false`**：减少代理开销，提高启动性能
2. **使用 `@ConditionalOnMissingBean`**：允许用户自定义覆盖默认配置
3. **合理的默认配置**：开箱即用，减少用户配置负担
4. **完善的配置属性**：提供 IDE 自动补全和文档说明
5. **配置元数据**：编写 `additional-spring-configuration-metadata.json`
6. **属性校验**：使用 `@Validated` + JSR-303 注解校验配置

---

## 九、高频面试题

### Q1：Spring Boot 自动装配的原理是什么？

**答案**：
1. `@SpringBootApplication` 包含 `@EnableAutoConfiguration`
2. `@EnableAutoConfiguration` 导入 `AutoConfigurationImportSelector`
3. `AutoConfigurationImportSelector` 通过 `SpringFactoriesLoader` 加载 `META-INF/spring.factories` 中的自动配置类
4. 根据 `@Conditional` 条件过滤，保留符合条件的配置类
5. 执行自动配置类，创建 Bean 并注入容器

---

### Q2：如何自定义一个 Spring Boot Starter？

**答案**：
1. 创建项目，命名为 `xxx-spring-boot-starter`
2. 编写配置属性类（`@ConfigurationProperties`）
3. 编写自动配置类（`@AutoConfiguration` + `@Conditional`）
4. 创建 `META-INF/spring/...AutoConfiguration.imports` 文件
5. 可选：添加配置元数据提供 IDE 提示

---

### Q3：@ConditionalOnClass 和 @ConditionalOnBean 的区别？

**答案**：

| 注解 | 判断对象 | 判断时机 | 用途 |
|------|---------|---------|------|
| @ConditionalOnClass | 类路径 | 配置类加载前 | 判断依赖是否引入 |
| @ConditionalOnBean | Spring 容器 | 配置类加载时 | 判断 Bean 是否存在 |

---

### Q4：@ConditionalOnMissingBean 的作用？

**答案**：
- 容器中不存在指定 Bean 时，才创建当前 Bean
- 允许用户自定义实现覆盖默认配置
- 体现了 Spring Boot「约定优于配置」的理念

---

### Q5：Spring Boot 2.7 和之前版本的自动配置有什么区别？

**答案**：
- **2.6 及以前**：使用 `META-INF/spring.factories`
- **2.7+**：自动配置使用 `META-INF/spring/...AutoConfiguration.imports`，每行一个配置类，更清晰

---

### Q6：proxyBeanMethods = false 的作用是什么？

**答案**：
- 禁用 CGLIB 代理，提高启动性能
- `@Bean` 方法之间相互调用时，不再保证单例（每次调用都创建新对象）
- 自动配置类建议设置为 false

---

### Q7：如何排除某个自动配置类？

**答案**：
```java
// 方式一：注解排除
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})

// 方式二：配置文件
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

---

### Q8：SpringFactoriesLoader 的工作原理？

**答案**：
1. 扫描 classpath 下所有 jar 包的 `META-INF/spring.factories`
2. 读取指定 key 对应的所有配置类全限定名
3. 使用反射实例化这些类
4. 这是 Java SPI 机制的扩展实现

---

### Q9：如何调试自动配置？

**答案**：
```bash
# 启动参数
java -jar app.jar --debug

# 或配置文件
debug: true

# 或 Actuator 端点
GET /actuator/conditions
```

查看启动日志中的 `CONDITIONS EVALUATION REPORT`。

---

### Q10：@EnableConfigurationProperties 的作用？

**答案**：
- 使 `@ConfigurationProperties` 注解的类生效
- 将配置属性类注册为 Bean
- 自动绑定配置文件中的属性到配置类
