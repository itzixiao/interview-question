# Spring Boot Starter 开发实战 - 从零实现自定义线程池

## 📖 本文内容

本文档是 `interview-threadpool-starter` 项目的配套教程，带你从零开始开发一个完整的 Spring Boot Starter。

---

## 🎯 学习目标

学完本教程后，你将能够：

1. ✅ 理解 Spring Boot Starter 的工作原理
2. ✅ 掌握自动配置的核心注解使用
3. ✅ 独立开发自定义的 Spring Boot Starter
4. ✅ 在业务项目中集成和使用自定义 Starter

---

## 🚀 快速开始

### 步骤 1：安装 Starter 到本地 Maven 仓库

```bash
cd interview-threadpool-starter
mvn clean install
```

**预期输出：**
```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Installed: cn.itxiao:interview-threadpool-starter:1.0.0
[INFO] ------------------------------------------------------------------------
```

---

### 步骤 2：在业务项目中引入依赖

编辑 `interview-provider/pom.xml`，确认已添加：

```xml
<!-- 自定义线程池Starter -->
<dependency>
    <groupId>cn.itxiao</groupId>
    <artifactId>interview-threadpool-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

### 步骤 3：配置线程池参数

编辑 `interview-provider/src/main/resources/application-dev.yml`：

```yaml
# 自定义线程池配置
custom:
  thread-pool:
    enabled: true                    # 是否启用
    core-pool-size: 10               # 核心线程数
    max-pool-size: 20                # 最大线程数
    queue-capacity: 200              # 队列容量
    keep-alive-time: 120             # 空闲线程存活时间（秒）
    name-prefix: "provider-pool"     # 线程名前缀
    rejected-policy: "CALLER_RUNS"   # 拒绝策略
```

---

### 步骤 4：启动应用并测试

```bash
cd interview-provider
mvn spring-boot:run
```

**访问测试接口：**

1. **提交异步任务**
   ```bash
   curl http://localhost:8082/test/thread-pool
   ```
   
   **预期控制台输出：**
   ```
   ✅ 自动装配：自定义线程池已创建
   📊 配置信息：线程池 [provider-pool] 状态 -> 核心线程数：10, 当前线程数：0, ...
   
   ========== 提交异步任务 ==========
   ✅ 任务 1 执行中... 线程：provider-pool-Thread-1
   ✅ 任务 2 执行中... 线程：provider-pool-Thread-2
   ...
   ```

2. **查看线程池状态**
   ```bash
   curl http://localhost:8082/test/pool-status
   ```
   
   **预期响应：**
   ```
   线程池状态 -> 核心线程数：10, 当前线程数：5, 活跃线程数：5, 队列大小：0, 已完成任务数：5
   ```

3. **压力测试**
   ```bash
   curl http://localhost:8082/test/stress-test
   ```

---

## 📦 项目结构

```
interview-threadpool-starter/
├── src/main/java/cn/itxiao/starter/
│   ├── CustomThreadPoolExecutor.java       # 自定义线程池实现
│   ├── ThreadPoolProperties.java           # 配置属性绑定类
│   └── ThreadPoolAutoConfiguration.java    # 自动配置类
├── src/main/resources/
│   └── META-INF/
│       └── spring.factories                # 自动装配入口文件
└── pom.xml                                 # Maven 配置

interview-provider/                         # 使用示例项目
├── src/main/java/cn/itzixiao/interview/provider/
│   └── controller/
│       └── ThreadPoolTestController.java   # 使用示例 Controller
└── src/main/resources/
    └── application-dev.yml                 # 配置文件
```

---

## 🔧 核心组件详解

### 1️⃣ CustomThreadPoolExecutor.java

**作用：** 自定义线程池实现类

**核心功能：**
- 继承 `ThreadPoolExecutor`
- 提供自定义线程命名工厂
- 支持线程池状态监控

**关键代码：**
```java
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    
    static class CustomThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(namePrefix + "-Thread-" + threadNumber.getAndIncrement());
            return thread;
        }
    }
}
```

---

### 2️⃣ ThreadPoolProperties.java

**作用：** 配置属性绑定类

**核心注解：**
```java
@ConfigurationProperties(prefix = "custom.thread-pool")
```

**绑定的配置项：**
| 配置项 | Java 字段 | 默认值 | 说明 |
|--------|---------|--------|------|
| core-pool-size | corePoolSize | 5 | 核心线程数 |
| max-pool-size | maxPoolSize | 10 | 最大线程数 |
| queue-capacity | queueCapacity | 100 | 队列容量 |
| keep-alive-time | keepAliveTime | 60 | 空闲存活时间（秒） |
| name-prefix | namePrefix | custom-pool | 线程名前缀 |
| rejected-policy | rejectedPolicy | ABORT | 拒绝策略 |

---

### 3️⃣ ThreadPoolAutoConfiguration.java

**作用：** 自动配置类（Starter 的核心）

**核心注解：**

```java
@Configuration                                    // 标记为配置类
@ConditionalOnClass(ThreadPoolExecutor.class)    // 类路径存在 ThreadPoolExecutor 才生效
@ConditionalOnProperty(                          // 配置文件中启用了才生效
    prefix = "custom.thread-pool",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(ThreadPoolProperties.class)  // 启用配置属性绑定
public class ThreadPoolAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(ThreadPoolExecutor.class)  // 容器中没有该 Bean 才创建
    public ThreadPoolExecutor threadPoolExecutor() {
        // 创建线程池 Bean
    }
}
```

---

### 4️⃣ spring.factories

**位置：** `src/main/resources/META-INF/spring.factories`

**内容：**
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
cn.itxiao.starter.ThreadPoolAutoConfiguration
```

**作用：** Spring Boot 启动时扫描并加载自动配置类

---

## 💡 工作原理图解

```
┌─────────────────────────────────────────────────────────────┐
│  1. Spring Boot 应用启动                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  2. 扫描 classpath 下所有 jar 包的 META-INF/spring.factories   │
│     - interview-threadpool-starter.jar                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  3. 读取 EnableAutoConfiguration 配置                        │
│     org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
│     cn.itxiao.starter.ThreadPoolAutoConfiguration           │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  4. 检查条件注解                                             │
│     ✓ @ConditionalOnClass → JDK 支持线程池                   │
│     ✓ @ConditionalOnProperty → 配置启用                      │
│     ✓ @ConditionalOnMissingBean → 无自定义 Bean              │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  5. 加载 ThreadPoolAutoConfiguration                         │
│     - 读取 ThreadPoolProperties（绑定 yml 配置）                │
│     - 调用 threadPoolExecutor() 方法                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│  6. ThreadPoolExecutor Bean 注册到 Spring 容器                │
│     - 可以通过@Autowired 注入                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎓 高频面试题

### Q1：Spring Boot Starter 的自动装配原理是什么？

**参考答案：**

Spring Boot Starter 的自动装配基于以下机制：

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

**执行流程：**
```
启动 → 扫描 spring.factories → 读取自动配置类 → 
检查条件注解 → 加载配置类 → 创建 Bean → 注册到容器
```

---

### Q2：如何自定义一个 Spring Boot Starter？

**参考答案：**

**三步走：**

**Step 1：创建 Maven 项目**
```xml
<groupId>cn.itxiao</groupId>
<artifactId>interview-threadpool-starter</artifactId>
<version>1.0.0</version>
```

**Step 2：编写三个核心类**

1. **XxxProperties**：配置属性类
   ```java
   @ConfigurationProperties(prefix = "custom.thread-pool")
   public class ThreadPoolProperties { ... }
   ```

2. **XxxAutoConfiguration**：自动配置类
   ```java
   @Configuration
   @ConditionalOnClass(...)
   @EnableConfigurationProperties(ThreadPoolProperties.class)
   public class ThreadPoolAutoConfiguration {
       @Bean
       @ConditionalOnMissingBean
       public ThreadPoolExecutor threadPoolExecutor() { ... }
   }
   ```

3. **业务类**：实际功能类
   ```java
   public class CustomThreadPoolExecutor extends ThreadPoolExecutor { ... }
   ```

**Step 3：创建 spring.factories**
```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
cn.itxiao.starter.ThreadPoolAutoConfiguration
```

**Step 4：发布到 Maven 仓库**
```bash
mvn clean install
```

---

### Q3：@ConditionalOnMissingBean 的作用是什么？

**参考答案：**

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
// Starter 中的默认配置
@Bean
@ConditionalOnMissingBean(ThreadPoolExecutor.class)
public ThreadPoolExecutor threadPoolExecutor() {
    return new CustomThreadPoolExecutor(...);
}

// 用户自定义配置（会覆盖默认配置）
@Configuration
public class MyConfig {
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new CustomThreadPoolExecutor(...);
    }
}
```

**优先级规则：**
- 用户自定义的 Bean 优先级 > Starter 自动配置的 Bean

---

## 🛠️ 扩展练习

尝试实现以下功能：

### 练习 1：添加线程池监控

**需求：**
- 定时输出线程池状态（每 30 秒）
- 当活跃线程数超过阈值时告警

**提示：**
```java
@Bean
public ScheduledExecutorService monitorScheduler(
        ThreadPoolExecutor executor,
        ThreadPoolProperties properties) {
    
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(() -> {
        System.out.println("📊 监控：" + executor.getPoolStatus());
        
        if (executor.getActiveCount() > properties.getMaxPoolSize() * 0.8) {
            System.err.println("⚠️ 警告：线程池负载过高！");
        }
    }, 0, 30, TimeUnit.SECONDS);
    
    return scheduler;
}
```

---

### 练习 2：支持多数据源 Starter

**需求：**
- 支持配置多个数据源
- 通过注解切换数据源

**提示：**
```yaml
custom:
  datasource:
    primary: mysql
    sources:
      mysql:
        url: jdbc:mysql://localhost:3306/db1
        username: root
        password: 123456
      oracle:
        url: jdbc:oracle:thin:@localhost:1521:orcl
        username: system
        password: oracle
```

---

### 练习 3：分布式锁 Starter

**需求：**
- 基于 Redis 实现分布式锁
- 支持看门狗自动续期

**提示：**
```java
@DistributedLock(key = "order:#orderId", expire = 30)
public void createOrder(String orderId) {
    // 业务逻辑
}
```

---

## 📚 参考资料

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Boot AutoConfigure 源码](https://github.com/spring-projects/spring-boot/tree/main/spring-boot-project/spring-boot-autoconfigure)
- [自定义 Starter 最佳实践](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)

---

**作者：** itxiao  
**更新时间：** 2026-03-08  
**欢迎 Star & Fork！**
