# Spring Boot 与自动装配知识点详解

## 📚 文档列表

#### 1. [01-Spring-Boot 自动装配.md](./01-Spring-Boot%E8%87%AA%E5%8A%A8%E8%A3%85%E9%85%8D.md)

- **内容:** @SpringBootApplication、@EnableAutoConfiguration、spring.factories 机制
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 2. [02-自定义线程池 Starter 详解.md](./02-%E8%87%AA%E5%AE%9A%E4%B9%89%E7%BA%BF%E7%A8%8B%E6%B1%A0Starter%E8%AF%A6%E8%A7%A3.md)

- **内容:** 自定义 Starter、自动配置、@ConfigurationProperties
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 3. [03-Spring-Boot-Actuator 完全指南.md](./03-Spring-Boot-Actuator%E5%AE%8C%E5%85%A8%E6%8C%87%E5%8D%97.md) ⭐新增

- **内容:** Actuator 监控端点、健康检查、性能指标、自定义端点、安全配置
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **特色**:
    - ✅ 完整的 Actuator 知识体系
    - ✅ 可运行的示例代码（含自定义端点）
    - ✅ 生产环境安全配置方案
    - ✅ K8s、Prometheus 集成实战
    - ✅ 高频面试题与参考答案

#### 4. [04-SpringBoot整合Elasticsearch完全指南.md](./04-SpringBoot%E6%95%B4%E5%90%88Elasticsearch%E5%AE%8C%E5%85%A8%E6%8C%87%E5%8D%97.md) ⭐新增

- **内容**: Spring Boot整合 ES、倒排索引、全文检索、批量索引优化、数据同步策略
- **面试题**: 7+ 道
- **重要程度**: ⭐⭐⭐⭐⭐
- **特色**:
    - ✅ 完整的 ES 整合实战案例（从 0 到 1）
    - ✅ 可运行的示例代码（配置 + 实体 +Repository+Service+Controller）
    - ✅ 性能优化方案（批量索引、深度分页、字段映射）
    - ✅ MySQL 与 ES 数据一致性解决方案
    - ✅ 生产环境最佳实践（ILM、监控告警）

---

## 📊 统计信息

- **文档数：** 4 个核心文档
- **面试题总数：** 40+ 道
- **代码示例：** 配套 Java 代码在 `interview-starters-parent/interview-threadpool-starter/`、
  `interview-service/actuator/`、`interview-microservices-parent/interview-provider/`（ES 整合）目录

---

## 🎯 学习建议

### Spring Boot 自动装配（2 天）

1. **核心注解**
    - @SpringBootApplication
    - @EnableAutoConfiguration
    - @Conditional 系列注解

2. **自动配置原理**
    - spring.factories 加载机制
    - 按需加载原则
    - 覆盖默认配置

### 自定义 Starter（1-2 天）

1. **Starter 结构**
    - META-INF/spring.factories
    - 自动配置类
    - 属性绑定类

2. **实战练习**
    - 自定义线程池 Starter
    - Redis Starter
    - MyBatis Starter

### Actuator 监控（1 天） ⭐新增

1. **内置端点**
    - /actuator/health 健康检查
    - /actuator/info 应用信息
    - /actuator/metrics 性能指标
    - /actuator/env 环境变量

2. **自定义扩展**
    - 自定义 HealthIndicator
    - 自定义 InfoContributor
    - 自定义 Endpoint

3. **安全配置**
    - 端点暴露控制
    - 独立管理端口
    - Spring Security 集成

4. **监控集成**
    - K8s 探针
    - Prometheus + Grafana
    - 自定义业务指标

---

## 🔗 跨模块关联

### 前置知识

- ✅ **[Spring框架](../04-Spring框架/README.md)** - IOC、AOP、Bean 生命周期
- ✅ **[Java并发编程](../02-Java并发编程/README.md)** - 线程池基础

### 后续进阶

- 📚 **[微服务架构](../06-SpringCloud 微服务/README.md)** - 服务监控、链路追踪
- 📚 **[DevOps](../13-DevOps/README.md)** - CI/CD、容器化部署

### 知识点对应

| Spring Boot 技术 | 应用场景            |
|----------------|-----------------|
| 自动装配           | Starter 开发、框架集成 |
| 自定义 Starter    | 中间件封装、通用组件      |
| Actuator       | 微服务监控、K8s 集成    |
| 外部化配置          | 多环境配置、配置中心      |
| Profile        | 环境隔离、灰度发布       |

---

## 💡 高频面试题 Top 10

1. **Spring Boot 自动装配原理？⭐重点**
    - @EnableAutoConfiguration 的作用
    - spring.factories 的加载机制
    - 如何按需加载 Bean

2. **如何自定义一个 Starter？**
    - Starter 的目录结构
    - spring.factories 的配置格式
    - 条件注解的使用

3. **Actuator 的核心作用是什么？**
    - 健康检查的实现
    - 性能指标的采集
    - 自定义端点的开发

4. **如何保证 Actuator 端点的安全性？**
    - 端点暴露控制
    - 网络隔离方案
    - Spring Security 集成

5. **Spring Boot 配置文件优先级？**
    - application.properties vs application.yml
    - 多环境配置加载顺序
    - 外部化配置的优先级

6. **@SpringBootApplication 包含哪些注解？**
    - @SpringBootConfiguration
    - @EnableAutoConfiguration
    - @ComponentScan

7. **如何实现按环境加载配置？**
    - Profile 激活方式
    - 多环境配置文件命名
    - 命令行参数指定环境

8. **Spring Boot 启动流程？**
    - SpringApplication.run() 执行过程
    - 上下文初始化
    - Runner 回调机制

9. **如何优化 Spring Boot 启动速度？**
    - 延迟加载策略
    - 排除不必要的自动配置
    - 使用 Spring Boot Devtools

10. **Actuator 在生产环境的最佳实践？**
    - 最小化暴露原则
    - 监控告警配置
    - 日志审计

---

## 🛠️ 实战技巧

### 自定义 Starter 步骤

```java
// 1. 创建自动配置类
@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolAutoConfiguration {
    
    @Bean
    public ThreadPoolExecutor customThreadPool(ThreadPoolProperties properties) {
        return new CustomThreadPoolExecutor(
            properties.getCorePoolSize(),
            properties.getMaxPoolSize()
        );
    }
}

// 2. 创建配置属性类
@ConfigurationProperties(prefix = "custom.threadpool")
public class ThreadPoolProperties {
    private int corePoolSize = 10;
    private int maxPoolSize = 20;
    // getters and setters
}

// 3. 配置 spring.factories
# META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
cn.itzixiao.starter.ThreadPoolAutoConfiguration
```

### Actuator 快速集成

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

```bash
# 访问健康检查
curl http://localhost:8080/actuator/health

# 访问 Prometheus 指标
curl http://localhost:8080/actuator/prometheus
```

---

## 📖 推荐学习顺序

```
Spring Boot 基础
   ↓
自动装配原理
   ↓
自定义 Starter
   ├─ 理论：spring.factories 机制
   ├─ 实践：线程池 Starter
   └─ 理解：条件注解
   ↓
Actuator 监控 ⭐新增
   ├─ 内置端点使用
   ├─ 自定义端点开发
   ├─ 安全配置方案
   └─ 监控工具集成
   ↓
微服务监控进阶
```

---

## 📈 更新日志

### v2.1 - 2026-03-09 ⭐新增

- ✅ 新增 [03-Spring-Boot-Actuator 完全指南.md](03-Spring-Boot-Actuator完全指南.md)
- ✅ 提供完整的 Actuator 示例代码（可运行）
- ✅ 自定义 HealthIndicator、InfoContributor、Endpoint
- ✅ 生产环境安全配置方案
- ✅ K8s、Prometheus 集成实战案例
- ✅ 10+ 道高频面试题与参考答案

### v2.0 - 早期版本

- ✅ Spring Boot 自动装配核心知识点
- ✅ 自定义 Starter 实现

---

**维护者：** itzixiao  
**最后更新：** 2026-03-09 ⭐新增 Spring Boot Actuator 完全指南  
**问题反馈：** 欢迎提 Issue 或 PR
