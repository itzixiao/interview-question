# Thread Pool Starter 自定义 starter 实现详解

## 一、什么是 Spring Boot Starter

### 1.1 Starter 的作用

Spring Boot Starter 是一种**自动配置机制**，通过简单的依赖引入，即可自动完成以下工作：

```
┌─────────────────────────────────────────────────────────────┐
│  传统方式：                                                  │
│  1. 手动创建 Bean                                            │
│  2. 手动配置属性                                             │
│  3. 手动处理依赖关系                                         │
│                                                              │
│  使用 Starter 后：                                           │
│  1. 引入依赖（自动传递依赖）                                 │
│  2. 配置文件设置属性（自动绑定）                             │
│  3. 自动装配 Bean（开箱即用）                                │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 Starter 的核心原理

```
┌─────────────────────────────────────────────────────────────┐
│  Spring Boot Starter 自动装配流程                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  1. META-INF/spring.factories                         │   │
│  │     - 指定自动配置类                                  │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  2. @Configuration 配置类                             │   │
│  │     - 定义 Bean 创建逻辑                               │   │
│  │     - @ConditionalOnXxx 条件注解                       │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  3. @ConfigurationProperties 属性绑定                 │   │
│  │     - 读取 application.yml 配置                        │   │
│  │     - 自动注入到 JavaBean                              │   │
│  ├──────────────────────────────────────────────────────┤   │
│  │  4. spring.factories 加载                             │   │
│  │     - SpringBootServletInitializer                     │   │
│  │     - 扫描所有 Starter 的 spring.factories              │   │
│  │     - 按顺序执行自动配置                               │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、自定义线程池 Starter 实战

### 2.1 项目结构

```
interview-threadpool-starter/
├── src/main/java/cn/itzixiao/starter/
│   ├── ThreadPoolAutoConfiguration.java    # 自动配置类
│   ├── ThreadPoolProperties.java           # 属性配置类
│   └── CustomThreadPoolExecutor.java       # 自定义线程池
├── src/main/resources/
│   └── META-INF/
│       └── spring.factories                # 自动装配入口
└── pom.xml                                 # Maven 配置
```

### 2.2 核心代码实现

#### 1️⃣ **CustomThreadPoolExecutor.java** - 自定义线程池

```java
package cn.itxiao.starter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义线程池
 * 
 * 功能特性：
 * 1. 支持核心线程数、最大线程数、队列容量配置
 * 2. 支持线程命名工厂（便于调试）
 * 3. 支持拒绝策略配置
 * 4. 提供线程池监控方法
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    
    // 线程计数器（用于生成唯一线程名）
    private final AtomicLong threadCount = new AtomicLong(0);
    
    // 线程名前缀
    private final String namePrefix;
    
    /**
     * 构造方法
     * 
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime 空闲线程存活时间
     * @param unit 时间单位
     * @param workQueue 任务队列
     * @param namePrefix 线程名前缀
     * @param rejectedExecutionHandler 拒绝策略
     */
    public CustomThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            String namePrefix,
            RejectedExecutionHandler rejectedExecutionHandler) {
        
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
              new CustomThreadFactory(namePrefix), rejectedExecutionHandler);
        
        this.namePrefix = namePrefix;
    }
    
    /**
     * 自定义线程工厂
     */
    static class CustomThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicLong threadNumber = new AtomicLong(1);
        
        public CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(namePrefix + "-Thread-" + threadNumber.getAndIncrement());
            thread.setDaemon(false); // 非守护线程
            return thread;
        }
    }
    
    /**
     * 获取线程池状态信息
     */
    public String getPoolStatus() {
        return String.format(
            "线程池 [%s] 状态 -> 核心线程数：%d, 当前线程数：%d, 活跃线程数：%d, " +
            "队列大小：%d, 队列容量：%d, 已完成任务：%d",
            namePrefix,
            getCorePoolSize(),
            getPoolSize(),
            getActiveCount(),
            getQueue().size(),
            getQueue().remainingCapacity(),
            getCompletedTaskCount()
        );
    }
}
```

---

#### 2️⃣ **ThreadPoolProperties.java** - 属性配置类

```java
package cn.itxiao.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池配置属性
 * 
 * 作用：将 application.yml 中的配置绑定到 JavaBean
 * 
 * 配置前缀：custom.thread-pool
 * 
 * 支持的配置项：
 * - core-pool-size: 核心线程数（默认 5）
 * - max-pool-size: 最大线程数（默认 10）
 * - queue-capacity: 队列容量（默认 100）
 * - keep-alive-time: 空闲线程存活时间（秒，默认 60）
 * - name-prefix: 线程名前缀（默认 custom-pool）
 * - rejected-policy: 拒绝策略（默认 ABORT）
 */
@ConfigurationProperties(prefix = "custom.thread-pool")
public class ThreadPoolProperties {
    
    /**
     * 核心线程数
     * 说明：线程池中保持存活的最低线程数
     * 默认值：5
     */
    private int corePoolSize = 5;
    
    /**
     * 最大线程数
     * 说明：线程池允许创建的最大线程数
     * 默认值：10
     */
    private int maxPoolSize = 10;
    
    /**
     * 队列容量
     * 说明：用于缓存待执行任务的队列大小
     * 默认值：100
     */
    private int queueCapacity = 100;
    
    /**
     * 空闲线程存活时间（秒）
     * 说明：超过核心线程数的线程，空闲多久后被回收
     * 默认值：60
     */
    private long keepAliveTime = 60;
    
    /**
     * 线程名前缀
     * 说明：用于标识线程所属的线程池
     * 默认值：custom-pool
     */
    private String namePrefix = "custom-pool";
    
    /**
     * 拒绝策略
     * 说明：当队列满且达到最大线程数时的处理策略
     * 可选值：ABORT（抛异常）, CALLER_RUNS（调用者运行）, DISCARD（丢弃）, DISCARD_OLDEST（丢弃最老）
     * 默认值：ABORT
     */
    private String rejectedPolicy = "ABORT";
    
    // ========== Getter 和 Setter 方法 ==========
    
    public int getCorePoolSize() {
        return corePoolSize;
    }
    
    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }
    
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    public int getQueueCapacity() {
        return queueCapacity;
    }
    
    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
    
    public long getKeepAliveTime() {
        return keepAliveTime;
    }
    
    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }
    
    public String getNamePrefix() {
        return namePrefix;
    }
    
    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }
    
    public String getRejectedPolicy() {
        return rejectedPolicy;
    }
    
    public void setRejectedPolicy(String rejectedPolicy) {
        this.rejectedPolicy = rejectedPolicy;
    }
}
```

---

#### 3️⃣ **ThreadPoolAutoConfiguration.java** - 自动配置类

```java
package cn.itxiao.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池自动配置类
 * 
 * 核心注解说明：
 * 
 * 1. @Configuration
 *    - 标记为配置类，Spring 会扫描并加载其中的 Bean 定义
 * 
 * 2. @EnableConfigurationProperties(ThreadPoolProperties.class)
 *    - 启用配置属性绑定
 *    - 自动将 application.yml 中 custom.thread-pool.* 配置绑定到 ThreadPoolProperties
 * 
 * 3. @ConditionalOnClass(ThreadPoolExecutor.class)
 *    - 条件注解：当类路径中存在 ThreadPoolExecutor 时才生效
 *    - 确保 JDK 支持线程池
 * 
 * 4. @ConditionalOnProperty(prefix = "custom.thread-pool", name = "enabled", havingValue = "true", matchIfMissing = true)
 *    - 条件注解：当配置文件中 custom.thread-pool.enabled=true 或未配置时才生效
 *    - 可通过配置关闭自动装配
 * 
 * 5. @ConditionalOnMissingBean
 *    - 条件注解：当容器中没有自定义的 ThreadPoolExecutor Bean 时才创建
 *    - 允许用户覆盖默认配置
 */
@Configuration
@ConditionalOnClass(ThreadPoolExecutor.class)
@ConditionalOnProperty(
    prefix = "custom.thread-pool",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolAutoConfiguration {
    
    @Autowired
    private ThreadPoolProperties properties;
    
    /**
     * 创建线程池 Bean
     * 
     * @ConditionalOnMissingBean 确保用户可以自定义 ThreadPoolExecutor
     * 
     * @return 自定义线程池
     */
    @Bean
    @ConditionalOnMissingBean(ThreadPoolExecutor.class)
    public ThreadPoolExecutor threadPoolExecutor() {
        // 1. 创建阻塞队列
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(properties.getQueueCapacity());
        
        // 2. 创建拒绝策略
        RejectedExecutionHandler handler = createRejectedExecutionHandler(properties.getRejectedPolicy());
        
        // 3. 创建自定义线程池
        CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
            properties.getCorePoolSize(),
            properties.getMaxPoolSize(),
            properties.getKeepAliveTime(),
            java.util.concurrent.TimeUnit.SECONDS,
            workQueue,
            properties.getNamePrefix(),
            handler
        );
        
        System.out.println("✅ 自动装配：自定义线程池已创建");
        System.out.println("📊 配置信息：" + executor.getPoolStatus());
        
        return executor;
    }
    
    /**
     * 根据配置创建拒绝策略
     * 
     * @param policy 策略名称
     * @return 拒绝策略处理器
     */
    private RejectedExecutionHandler createRejectedExecutionHandler(String policy) {
        switch (policy.toUpperCase()) {
            case "CALLER_RUNS":
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case "DISCARD":
                return new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST":
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            case "ABORT":
            default:
                return new ThreadPoolExecutor.AbortPolicy();
        }
    }
}
```

---

#### 4️⃣ **spring.factories** - 自动装配入口文件

```properties
# 位置：src/main/resources/META-INF/spring.factories

# 自动配置类
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
cn.itxiao.starter.ThreadPoolAutoConfiguration
```

**作用说明：**
- Spring Boot 启动时会扫描所有 jar 包中的 `META-INF/spring.factories` 文件
- 读取 `EnableAutoConfiguration` 键对应的自动配置类
- 根据条件注解决定是否加载这些配置类

---

#### 5️⃣ **pom.xml** - Maven 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>cn.itxiao</groupId>
    <artifactId>interview-threadpool-starter</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>interview-threadpool-starter</name>
    <description>自定义线程池 Spring Boot Starter</description>
    
    <properties>
        <maven.compiler.source>8</maven.compiler.source>
        <maven.compiler.target>8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-boot.version>2.7.18</spring-boot.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot 自动装配依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        
        <!-- Spring Boot 配置处理器（生成元数据） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <version>${spring-boot.version}</version>
            <optional>true</optional>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 三、在 interview-provider 中使用 Starter

### 3.1 添加依赖

在 `interview-provider/pom.xml` 中添加：

```xml
<!-- 自定义线程池 Starter -->
<dependency>
    <groupId>cn.itxiao</groupId>
    <artifactId>interview-threadpool-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3.2 配置文件

在 `interview-provider/src/main/resources/application-dev.yml` 中添加：

```yaml
# 自定义线程池配置
custom:
  thread-pool:
    enabled: true                    # 是否启用（默认 true）
    core-pool-size: 10               # 核心线程数
    max-pool-size: 20                # 最大线程数
    queue-capacity: 200              # 队列容量
    keep-alive-time: 120             # 空闲线程存活时间（秒）
    name-prefix: "provider-pool"     # 线程名前缀
    rejected-policy: "CALLER_RUNS"   # 拒绝策略：ABORT/CALLER_RUNS/DISCARD/DISCARD_OLDEST
```

### 3.3 使用示例

```java
package cn.itzixiao.interview.provider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadPoolExecutor;

@RestController
public class TestController {
    
    /**
     * 自动注入自定义线程池
     * 
     * Spring Boot 会自动装配 ThreadPoolAutoConfiguration 中定义的 ThreadPoolExecutor Bean
     */
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    
    @GetMapping("/test/thread-pool")
    public String testThreadPool() {
        // 提交异步任务
        threadPoolExecutor.submit(() -> {
            System.out.println("✅ 任务执行中... 线程：" + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("✅ 任务完成！");
        });
        
        return "任务已提交，请查看控制台日志";
    }
    
    @GetMapping("/test/pool-status")
    public String getPoolStatus() {
        // 获取线程池状态
        return String.format(
            "核心线程数：%d, 当前线程数：%d, 活跃线程数：%d, 队列大小：%d",
            threadPoolExecutor.getCorePoolSize(),
            threadPoolExecutor.getPoolSize(),
            threadPoolExecutor.getActiveCount(),
            threadPoolExecutor.getQueue().size()
        );
    }
}
```

---

## 四、Starter 的工作原理图解

```
┌─────────────────────────────────────────────────────────────────┐
│  Spring Boot 应用启动流程                                        │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  1. 扫描 classpath 下的所有 spring.factories                      │
│     - interview-threadpool-starter.jar                          │
│       └── META-INF/spring.factories                             │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  2. 读取 EnableAutoConfiguration 配置                           │
│     org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
│     cn.itxiao.starter.ThreadPoolAutoConfiguration               │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  3. 检查条件注解                                                 │
│     ✓ @ConditionalOnClass(ThreadPoolExecutor.class)            │
│       → JDK 支持线程池 ✓                                         │
│     ✓ @ConditionalOnProperty(enabled=true)                     │
│       → 配置文件中启用了 ✓                                       │
│     ✓ @ConditionalOnMissingBean(ThreadPoolExecutor.class)      │
│       → 容器中没有自定义的 ThreadPoolExecutor ✓                  │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  4. 加载 ThreadPoolAutoConfiguration 配置类                      │
│     - 读取 ThreadPoolProperties（绑定 application.yml 配置）      │
│     - 调用 threadPoolExecutor() 方法创建 Bean                     │
└─────────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────────┐
│  5. ThreadPoolExecutor Bean 注册到 Spring 容器                   │
│     - 可以通过@Autowired 注入                                     │
│     - 可以在其他地方使用                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 五、高频面试题

### Q1：Spring Boot Starter 的自动装配原理是什么？

**答：**

Spring Boot Starter 的自动装配基于以下核心机制：

1. **spring.factories 文件**
   - 位于 `META-INF/spring.factories`
   - 指定自动配置类的全限定名

2. **@EnableAutoConfiguration 注解**
   - 扫描 classpath 下所有 jar 包的 spring.factories
   - 加载其中的自动配置类

3. **条件注解**
   - `@ConditionalOnClass`：类路径存在某类时才生效
   - `@ConditionalOnProperty`：配置满足条件时才生效
   - `@ConditionalOnMissingBean`：容器中没有某 Bean 时才创建

4. **配置属性绑定**
   - `@ConfigurationProperties` 将 yml 配置绑定到 JavaBean
   - 实现类型安全的配置管理

**执行流程：**
```
启动 → 扫描 spring.factories → 读取自动配置类 → 
检查条件注解 → 加载配置类 → 创建 Bean → 注册到容器
```

---

### Q2：如何自定义一个 Spring Boot Starter？需要哪些步骤？

**答：**

**步骤 1：创建 Maven 项目**
- groupId: cn.itxiao
- artifactId: interview-threadpool-starter
- packaging: jar

**步骤 2：添加依赖**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

**步骤 3：编写三个核心类**
1. XxxProperties：配置属性类（@ConfigurationProperties）
2. XxxAutoConfiguration：自动配置类（@Configuration）
3. 业务类：实际功能类（如 CustomThreadPoolExecutor）

**步骤 4：创建 spring.factories**
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
cn.itxiao.starter.XxxAutoConfiguration
```

**步骤 5：发布到 Maven 仓库**
- 本地安装：mvn clean install
- 或发布到 Nexus/Artifactory

**步骤 6：在业务项目中引入**
```xml
<dependency>
    <groupId>cn.itxiao</groupId>
    <artifactId>interview-threadpool-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

### Q3：@ConditionalOnMissingBean 的作用是什么？

**答：**

**作用：** 当 Spring 容器中**不存在**指定类型的 Bean 时，才创建当前 Bean。

**使用场景：**
1. **允许用户覆盖默认配置**
   - Starter 提供默认的 Bean 实现
   - 用户可以自定义同名/同类型的 Bean 来覆盖

2. **避免 Bean 冲突**
   - 多个 Starter 可能都定义了同类型的 Bean
   - 使用此注解确保只有一个被创建

**示例：**
```java
@Bean
@ConditionalOnMissingBean(ThreadPoolExecutor.class)
public ThreadPoolExecutor threadPoolExecutor() {
    return new CustomThreadPoolExecutor(...);
}

// 用户在配置类中自定义：
@Configuration
public class MyConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 这个 Bean 会覆盖 Starter 中的默认 Bean
        return new CustomThreadPoolExecutor(...);
    }
}
```

**优先级规则：**
- 用户自定义的 Bean 优先级 > Starter 自动配置的 Bean

---

### Q4：@ConfigurationProperties 是如何实现属性绑定的？

**答：**

**原理：**

1. **编译时代码生成**
   - spring-boot-configuration-processor 在编译时生成元数据
   - 生成 `META-INF/spring-configuration-metadata.json`
   - 包含所有可配置属性的名称、类型、描述

2. **运行时绑定**
   - Spring Boot 启动时读取元数据
   - 解析配置文件（yml/properties）
   - 通过反射将属性值注入到 JavaBean

3. **松散绑定（Relaxed Binding）**
   - 支持多种命名格式：
     - kebab-case: `core-pool-size`
     - camelCase: `corePoolSize`
     - snake_case: `core_pool_size`
   - 都能正确映射到 JavaBean 的 `corePoolSize` 字段

**示例：**
```yaml
# application.yml
custom:
  thread-pool:
    core-pool-size: 10      # kebab-case
    maxPoolSize: 20         # camelCase
    queue_capacity: 200     # snake_case
```

```java
@ConfigurationProperties(prefix = "custom.thread-pool")
public class ThreadPoolProperties {
    private int corePoolSize;      // 自动绑定 core-pool-size
    private int maxPoolSize;       // 自动绑定 maxPoolSize
    private int queueCapacity;     // 自动绑定 queue_capacity
}
```

---

### Q5：为什么要使用 Starter 而不是直接在项目中配置？

**答：**

**使用 Starter 的优势：**

| 对比项 | 直接配置 | Starter |
|--------|---------|---------|
| **代码复用** | ❌ 每个项目重复配置 | ✅ 一次开发，多处使用 |
| **维护成本** | ❌ 修改需更新所有项目 | ✅ 升级版本号即可 |
| **依赖管理** | ❌ 手动管理所有依赖 | ✅ 传递依赖自动引入 |
| **标准化** | ❌ 各项目配置不一致 | ✅ 统一配置规范 |
| **开箱即用** | ❌ 需手动创建 Bean | ✅ 引入依赖自动装配 |

**典型应用场景：**
1. **公司内部框架**
   - 统一的数据库连接池配置
   - 统一的 Redis、MQ 客户端配置

2. **第三方服务集成**
   - 阿里云 OSS、短信服务
   - 微信支付、支付宝支付

3. **技术中间件**
   - MyBatis-Plus、ShardingSphere
   - Sentinel、Seata

**最佳实践：**
- 通用性强、需要复用的功能 → 封装成 Starter
- 仅单个项目使用的功能 → 直接配置

---

## 六、总结

### 6.1 核心知识点

1. **Starter 的本质**
   - 自动配置 + 依赖管理
   - 约定优于配置

2. **三个核心组件**
   - Properties：配置属性绑定
   - AutoConfiguration：自动配置类
   - spring.factories：装配入口

3. **关键注解**
   - @Configuration：配置类
   - @EnableConfigurationProperties：启用配置属性
   - @ConditionalOnXxx：条件装配
   - @ConfigurationProperties：属性绑定

### 6.2 开发流程

```
1. 创建 Starter 项目
   ↓
2. 编写功能类（如 CustomThreadPoolExecutor）
   ↓
3. 编写配置属性类（ThreadPoolProperties）
   ↓
4. 编写自动配置类（ThreadPoolAutoConfiguration）
   ↓
5. 创建 spring.factories
   ↓
6. mvn clean install 安装到本地仓库
   ↓
7. 在业务项目中引入依赖
   ↓
8. 配置文件配置属性
   ↓
9. 直接使用（自动装配）
```

### 6.3 扩展练习

尝试实现以下功能：

1. **线程池监控**
   - 定时输出线程池状态
   - 超过阈值告警

2. **多数据源 Starter**
   - 支持配置多个 DataSource
   - 动态切换数据源

3. **分布式锁 Starter**
   - 基于 Redis 实现分布式锁
   - 支持看门狗自动续期
