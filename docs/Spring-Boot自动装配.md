# Spring Boot 自动装配原理

## 概述

Spring Boot 自动装配（Auto-Configuration）是 Spring Boot 的核心特性，根据 classpath 中的依赖自动配置 Spring 应用。

## 核心流程

```
@SpringBootApplication
    └─ @EnableAutoConfiguration
        └─ @Import(AutoConfigurationImportSelector.class)
            └─ selectImports()
                └─ getAutoConfigurationEntry()
                    └─ SpringFactoriesLoader
                        └─ META-INF/spring.factories
                            └─ 候选配置类
                                └─ 条件过滤
                                    └─ 创建 Bean
```

## 核心注解

### @SpringBootApplication

组合注解，包含：
- `@SpringBootConfiguration`：标识配置类
- `@EnableAutoConfiguration`：开启自动装配
- `@ComponentScan`：组件扫描

### @EnableAutoConfiguration

```java
@AutoConfigurationPackage
@Import(AutoConfigurationImportSelector.class)
public @interface EnableAutoConfiguration {
}
```

## 自动配置加载机制

### 1. SpringFactoriesLoader

从 `META-INF/spring.factories` 加载配置：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.XXXAutoConfiguration,\
  com.example.YYYAutoConfiguration
```

### 2. 条件过滤

| 条件注解 | 说明 |
|----------|------|
| @ConditionalOnClass | 类路径存在指定类 |
| @ConditionalOnMissingClass | 类路径不存在指定类 |
| @ConditionalOnBean | 容器中存在指定 Bean |
| @ConditionalOnMissingBean | 容器中不存在指定 Bean |
| @ConditionalOnProperty | 配置属性满足条件 |
| @ConditionalOnWebApplication | 是 Web 应用 |

### 3. 配置属性绑定

```java
@ConfigurationProperties(prefix = "spring.datasource")
public class DataSourceProperties {
    private String url;
    private String username;
    private String password;
    // ...
}
```

## 自定义 Starter

### 项目结构

```
my-spring-boot-starter/
├── pom.xml
└── src/main/
    ├── java/com/example/autoconfigure/
    │   ├── MyAutoConfiguration.java
    │   ├── MyProperties.java
    │   └── MyService.java
    └── resources/META-INF/
        ├── spring.factories (Spring Boot 2.7-)
        └── spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports (2.7+)
```

### 关键代码

**自动配置类**：
```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
@ConditionalOnProperty(prefix = "my.service", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties properties) {
        return new MyService(properties);
    }
}
```

**配置属性**：
```java
@ConfigurationProperties(prefix = "my.service")
public class MyProperties {
    private boolean enabled = true;
    private String name = "default";
    // getters and setters
}
```

**spring.factories**：
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.example.autoconfigure.MyAutoConfiguration
```

## 自动配置排除

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

## 调试自动配置

### 查看生效的自动配置

```bash
java -jar app.jar --debug
# 或
java -jar app.jar -Ddebug
```

### 条件评估报告

启动日志中查看 `CONDITIONS EVALUATION REPORT`：

```
Positive matches:（匹配的配置）
Negative matches:（不匹配的配置）
Exclusions:（排除的配置）
```

## 最佳实践

1. **配置类加 `@Configuration(proxyBeanMethods = false)`**：减少代理开销
2. **使用 `@ConditionalOnMissingBean`**：允许用户自定义覆盖
3. **合理的默认配置**：开箱即用
4. **完善的配置属性**：提供 IDE 自动补全
5. **配置元数据**：编写 `additional-spring-configuration-metadata.json`
