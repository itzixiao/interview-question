# Spring Boot Actuator 完全指南

## 📖 目录

1. [Actuator 简介](#1-actuator-简介)
2. [快速开始](#2-快速开始)
3. [内置端点详解](#3-内置端点详解)
4. [自定义端点](#4-自定义端点)
5. [安全配置](#5-安全配置)
6. [实战案例](#6-实战案例)
7. [高频面试题](#7-高频面试题)

---

## 1. Actuator 简介

### 1.1 什么是 Actuator？

Spring Boot Actuator 是 Spring Boot 提供的**监控和管理**功能模块，它提供了：

- **健康检查** - 应用是否正常运行
- **性能指标** - CPU、内存、请求量等
- **应用信息** - 版本号、构建时间等
- **环境配置** - 环境变量、系统属性
- **线程快照** - 线程状态分析
- **HTTP 追踪** - 请求链路追踪

### 1.2 为什么需要 Actuator？

```mermaid
graph TB
    A[微服务应用] --> B[Actuator 端点]
    B --> C[/health 健康检查]
    B --> D[/metrics 性能指标]
    B --> E[/info 应用信息]
    B --> F[/env 环境配置]
    
    C --> G[K8s 存活探针]
    D --> H[Prometheus 监控]
    E --> I[运维管理平台]
    F --> J[配置中心]
```

**核心价值：**

- ✅ 统一监控接口标准
- ✅ 快速故障定位
- ✅ 性能瓶颈分析
- ✅ 自动化运维集成

---

## 2. 快速开始

### 2.1 添加依赖

```xml
<!-- interview-service/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 2.2 配置文件

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: "*"  # 暴露所有端点（生产环境谨慎使用）
  endpoint:
    health:
      show-details: always  # 始终显示健康详情
  info:
    env:
      enabled: true  # 启用环境信息
```

### 2.3 运行示例

```bash
cd interview-microservices-parent/interview-service/target/classes
java cn.itzixiao.interview.actuator.SpringBootActuatorDemo
```

访问端点：

```bash
# 查看所有端点列表
curl http://localhost:8080/actuator

# 健康检查
curl http://localhost:8080/actuator/health

# 应用信息
curl http://localhost:8080/actuator/info

# 性能指标
curl http://localhost:8080/actuator/metrics
```

---

## 3. 内置端点详解

### 3.1 常用端点列表

| 端点            | 路径                      | 说明             | 使用场景        |
|---------------|-------------------------|----------------|-------------|
| health        | /actuator/health        | 健康检查           | K8s 探针、负载均衡 |
| info          | /actuator/info          | 应用信息           | 版本管理、部署信息   |
| metrics       | /actuator/metrics       | 性能指标           | 监控告警、性能分析   |
| env           | /actuator/env           | 环境变量           | 配置排查、调试     |
| beans         | /actuator/beans         | Spring Bean 列表 | 依赖分析、调试     |
| threaddump    | /actuator/threaddump    | 线程快照           | 死锁检测、性能优化   |
| heapdump      | /actuator/heapdump      | 堆快照            | 内存泄漏分析      |
| logfile       | /actuator/logfile       | 日志文件           | 日志查看        |
| httpexchanges | /actuator/httpexchanges | HTTP 请求追踪      | 接口调用链分析     |

### 3.2 Health 端点详解

**基础响应：**

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "SELECT 1"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 100000000000,
        "threshold": 10485760
      }
    }
  }
}
```

**自定义健康检查器：**

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // 执行真实数据库查询
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up().withDetail("database", "MySQL").build();
        } catch (Exception e) {
            return Health.down(e).withDetail("error", e.getMessage()).build();
        }
    }
}
```

### 3.3 Metrics 端点详解

**JVM 内存指标：**

```bash
# 查看 JVM 内存使用情况
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 查看具体内存区域
curl http://localhost:8080/actuator/metrics/jvm.memory.used?tag=area:heap
```

**HTTP 请求指标：**

```bash
# 查看 HTTP 请求量
curl http://localhost:8080/actuator/metrics/http.server.requests

# 按状态码过滤
curl http://localhost:8080/actuator/metrics/http.server.requests?tag=status:200
```

**自定义业务指标：**

```java
@RestController
@RequestMapping("/api")
public class OrderController {
    
    private final MeterRegistry meterRegistry;
    
    public OrderController(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostMapping("/order")
    public String createOrder() {
        // 记录订单创建次数
        meterRegistry.counter("order.created").increment();
        
        // 记录订单创建耗时
        Timer timer = meterRegistry.timer("order.duration");
        return timer.record(() -> processOrder());
    }
}
```

---

## 4. 自定义端点

### 4.1 简单自定义端点

```java
@Component
@Endpoint(id = "demo")
public class DemoEndpoint {
    
    @ReadOperation
    public String getStatus() {
        return "Demo endpoint is running!";
    }
    
    @WriteOperation
    public String updateStatus(String message) {
        return "Updated with: " + message;
    }
}
```

**访问方式：**

```bash
# GET 请求
curl http://localhost:8080/actuator/demo

# POST 请求
curl -X POST http://localhost:8080/actuator/demo \
  -d "message=hello"
```

### 4.2 自定义 Info 贡献者

```java
@Component
public class CustomInfoContributor implements InfoContributor {
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", "interview-service")
               .withDetail("version", "1.0.0-SNAPSHOT")
               .withDetail("buildTime", "2026-03-09");
    }
}
```

**响应示例：**

```json
{
  "app": {
    "name": "interview-service",
    "version": "1.0.0-SNAPSHOT",
    "buildTime": "2026-03-09"
  }
}
```

---

## 5. 安全配置

### 5.1 生产环境配置

```yaml
# application-prod.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 只暴露必要端点
  server:
    port: 8081  # 独立管理端口
  security:
    enabled: true
```

### 5.2 添加 Spring Security

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
@Configuration
public class ManagementSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests()
            .anyRequest().hasRole("ADMIN")
            .and()
            .httpBasic();
        return http.build();
    }
}
```

---

## 6. 实战案例

### 6.1 K8s 健康检查集成

```yaml
# Kubernetes Deployment
apiVersion: apps/v1
kind: Deployment
metadata:
  name: interview-service
spec:
  template:
    spec:
      containers:
      - name: app
        image: interview-service:1.0.0
        livenessProbe:  # 存活探针
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:  # 就绪探针
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

### 6.2 Prometheus 监控集成

```xml
<!-- 添加 Prometheus 依赖 -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

**访问指标：**

```bash
curl http://localhost:8080/actuator/prometheus
```

### 6.3 自定义业务监控

```java
@Service
public class PaymentService {
    
    private final MeterRegistry meterRegistry;
    
    public PaymentService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    public void processPayment(BigDecimal amount) {
        // 记录支付金额
        meterRegistry.summary("payment.amount").record(amount.doubleValue());
        
        // 记录支付成功次数
        meterRegistry.counter("payment.success").increment();
        
        // 记录支付耗时
        Timer timer = meterRegistry.timer("payment.duration");
        timer.record(() -> executePayment(amount));
    }
    
    private void executePayment(BigDecimal amount) {
        // 执行支付逻辑
    }
}
```

---

## 7. 高频面试题

### Q1: Actuator 的核心作用是什么？

**答案：**
Actuator 提供应用的**可观测性**，包括：

1. **健康检查** - 判断应用是否正常运行
2. **性能监控** - CPU、内存、请求量等指标
3. **应用信息管理** - 版本号、构建时间
4. **配置管理** - 环境变量、系统属性
5. **故障诊断** - 线程快照、堆快照

**典型应用场景：**

- K8s 存活/就绪探针
- Prometheus 监控数据采集
- 运维管理平台集成

### Q2: 如何保证 Actuator 端点的安全性？

**答案：**

**方案 1：限制暴露的端点**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics  # 只暴露必要端点
```

**方案 2：使用独立管理端口**

```yaml
management:
  server:
    port: 8081  # 与业务端口分离
```

**方案 3：集成 Spring Security**

```java
@Configuration
public class ManagementSecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests()
            .anyRequest().hasRole("ADMIN");
        return http.build();
    }
}
```

**方案 4：网络隔离**

- 管理端口只监听内网
- 防火墙规则限制访问来源

### Q3: 自定义健康检查的实现方式？

**答案：**

实现 `HealthIndicator` 接口：

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        try {
            // 执行真实检查
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return Health.up().withDetail("database", "MySQL").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

**关键要点：**

- 使用 `Health.up()` 表示健康
- 使用 `Health.down()` 表示不健康
- 通过 `withDetail()` 添加详细信息
- 通过 `withException()` 添加异常信息

### Q4: 如何使用 Actuator 进行性能优化？

**答案：**

**步骤 1：启用详细指标**

```yaml
management:
  metrics:
    export:
      simple:
        enabled: true
```

**步骤 2：分析关键指标**

```bash
# CPU 使用率
curl /actuator/metrics/system.cpu.usage

# JVM 内存使用
curl /actuator/metrics/jvm.memory.used

# HTTP 请求延迟
curl /actuator/metrics/http.server.requests
```

**步骤 3：线程分析**

```bash
# 获取线程快照
curl /actuator/threaddump > thread_dump.txt

# 分析死锁、阻塞线程
```

**步骤 4：自定义业务指标**

```java
@GetMapping("/api/data")
public String getData() {
    Timer timer = meterRegistry.timer("data.query.duration");
    return timer.record(() -> queryData());
}
```

### Q5: Actuator 在生产环境的最佳实践？

**答案：**

**1. 最小化暴露原则**

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

**2. 网络隔离**

```yaml
management:
  server:
    address: 127.0.0.1  # 只允许本地访问
    port: 8081
```

**3. 认证授权**

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeRequests()
        .requestMatchers(EndpointRequest.to("health")).permitAll()
        .requestMatchers(EndpointRequest.toAnyEndpoint()).authenticated();
    return http.build();
}
```

**4. 监控告警**

- 集成 Prometheus + Grafana
- 设置健康检查告警规则
- 监控关键业务指标

**5. 日志审计**

- 记录所有管理端点访问
- 敏感操作（如 shutdown）需要审批

---

## 📊 总结

### 核心知识点

| 知识点         | 重要程度  | 考察频率 |
|-------------|-------|------|
| Actuator 作用 | ⭐⭐⭐⭐⭐ | 高    |
| 常用端点        | ⭐⭐⭐⭐⭐ | 高    |
| 安全配置        | ⭐⭐⭐⭐⭐ | 中    |
| 自定义端点       | ⭐⭐⭐⭐  | 中    |
| 健康检查        | ⭐⭐⭐⭐⭐ | 高    |
| 性能监控        | ⭐⭐⭐⭐  | 中    |

### 学习建议

1. **理解概念** - Actuator 的作用和价值
2. **动手实践** - 搭建完整示例并运行
3. **掌握配置** - 各种端点的配置方式
4. **安全加固** - 生产环境的安全措施
5. **监控集成** - Prometheus、Grafana 等工具

---

**维护者：** itzixiao  
**最后更新：** 2026-03-09  
**问题反馈：** 欢迎提 Issue 或 PR
