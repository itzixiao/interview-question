# Sentinel 限流熔断详解

## 一、Sentinel 是什么

Sentinel 是阿里巴巴开源的**流量防护组件**，以流量为切入点，从流量控制、熔断降级、系统负载保护等多个维度保障服务稳定性。

```
                    ┌──────────────────────────────┐
  海量请求          │         Sentinel             │       后端服务
  ─────────────>    │  ┌────────┐  ┌────────────┐  │  ──────────────>
                    │  │ 流量控制 │  │  熔断降级   │  │
                    │  └────────┘  └────────────┘  │
                    │  ┌────────┐  ┌────────────┐  │
                    │  │ 系统保护 │  │ 热点限流    │  │
                    │  └────────┘  └────────────┘  │
                    └──────────────────────────────┘
```

---

## 二、核心概念

### 2.1 资源（Resource）

资源是 Sentinel 的核心概念，可以是任何东西：一段代码、一个方法、一个 HTTP 接口。

```java
// 通过注解定义资源
@SentinelResource(value = "getQuestions", blockHandler = "getQuestionsBlockHandler")
public Result<String> getQuestions(String category) {
    return Result.success("面试题列表");
}
```

### 2.2 规则（Rule）

针对资源设置的保护规则，包括：

| 规则类型 | 说明               | 对应异常                   |
|------|------------------|------------------------|
| 流控规则 | QPS / 并发线程数限制    | `FlowException`        |
| 降级规则 | 慢调用 / 异常比例 / 异常数 | `DegradeException`     |
| 热点规则 | 针对热点参数限流         | `ParamFlowException`   |
| 系统规则 | CPU / 负载 / RT 保护 | `SystemBlockException` |
| 授权规则 | 黑白名单控制           | `AuthorityException`   |

---

## 三、流量控制

### 3.1 QPS 限流

限制每秒请求次数，超过阈值的请求直接拒绝。

```
时间轴:  |----1s----|----1s----|----1s----|
请求数:       8          12         5
阈值:         10         10         10
结果:       通过      拒绝2个      通过
```

### 3.2 并发线程数限流

限制同时处理该资源的线程数，防止线程资源被耗尽。

```
线程池: [T1][T2][T3][T4][T5]  阈值=5
新请求: T6 → 被拒绝
```

### 3.3 流控效果

| 效果          | 说明          | 适用场景   |
|-------------|-------------|--------|
| **快速失败**    | 超阈值立即拒绝     | 普通接口保护 |
| **Warm Up** | 冷启动，逐渐增加通过量 | 缓存预热场景 |
| **排队等待**    | 匀速排队，等待执行   | 削峰填谷   |

---

## 四、熔断降级

当下游服务不稳定时，自动熔断，防止故障扩散。

### 4.1 熔断状态机

```
         请求失败率 / 慢调用比例超阈值
              │
         ┌────▼─────┐
    ─────►  CLOSED   ├───────────────────────┐
         │  (关闭)   │  正常处理请求            │
         └──────────┘                        │
                                             │ 熔断触发
         ┌──────────┐                        │
    ◄────┤   OPEN   │◄───────────────────────┘
    请求  │   (开启)  │  直接拒绝，不访问下游
    失败  └────┬─────┘
              │ 等待时间窗口结束
              ▼
         ┌──────────┐
         │HALF-OPEN │  放一个探测请求
         │ (半开)    │──────────────────┐
         └──────────┘                  │
              ▲                        │ 成功 → CLOSED
              └────────────────────────┘ 失败 → OPEN
```

### 4.2 三种降级策略

#### 慢调用比例

```
统计窗口内：慢调用(RT > 阈值) 的比例 > 设定比例 → 触发熔断

示例：RT阈值=200ms，比例阈值=50%，窗口=10s
窗口内10次请求，6次响应>200ms → 60% > 50% → 触发熔断
```

#### 异常比例

```
统计窗口内：异常请求 / 总请求 > 比例阈值 → 触发熔断

示例：比例阈值=50%，10次请求中5次异常 → 触发熔断
```

#### 异常数

```
统计窗口内：异常数 > 阈值 → 触发熔断

示例：阈值=5，窗口内出现6次异常 → 触发熔断
```

---

## 五、热点参数限流

对方法的特定参数值进行精细化限流。

```java
// 对 getQuestion 的第 0 个参数（id）进行热点限流
@SentinelResource(value = "getQuestion", blockHandler = "blockHandler")
public Result getQuestion(@PathVariable Long id) { ... }
```

```
普通配置：每秒 QPS = 10（所有 id 共享）
热点配置：
  - id=1 → QPS = 100（热门题目）
  - id=2 → QPS = 100
  - 其他 id → QPS = 10
```

---

## 六、@SentinelResource 注解详解

```java
@SentinelResource(
    value = "resourceName",           // 资源名称（必填）
    blockHandler = "blockMethod",     // 限流/降级处理方法
    blockHandlerClass = XxxClass.class, // blockHandler 所在类（可跨类）
    fallback = "fallbackMethod",      // 业务异常处理方法
    fallbackClass = XxxClass.class,   // fallback 所在类
    exceptionsToIgnore = {RuntimeException.class} // 忽略的异常
)
```

### blockHandler vs fallback 区别

| 属性             | 触发时机                 | 参数要求                   |
|----------------|----------------------|------------------------|
| `blockHandler` | Sentinel 规则触发（限流/降级） | 原参数 + `BlockException` |
| `fallback`     | 业务代码抛出异常             | 原参数 + `Throwable`（可选）  |

```java
// 正确示例
public Result<String> getQuestionsBlockHandler(String category, BlockException ex) {
    return Result.error(429, "请求过于频繁");
}

public Result<String> getQuestionsFallback(String category, Throwable ex) {
    return Result.error(500, "服务异常: " + ex.getMessage());
}
```

---

## 七、本项目接口说明

### 7.1 InterviewController 限流接口

| 接口                             | 资源名            | 保护方式 |
|--------------------------------|----------------|------|
| `GET /interview/questions`     | `getQuestions` | 限流回调 |
| `GET /interview/question/{id}` | `getQuestion`  | 限流回调 |

### 7.2 SentinelTestController 测试接口

| 接口                              | 资源名             | 测试场景             |
|---------------------------------|-----------------|------------------|
| `GET /sentinel/hello`           | `sentinelHello` | QPS 限流测试         |
| `GET /sentinel/hot?userId=xxx`  | `sentinelHot`   | 热点参数限流测试         |
| `GET /sentinel/slow`            | `sentinelSlow`  | 慢调用降级测试（延迟500ms） |
| `GET /sentinel/error?fail=true` | `sentinelError` | 异常比例降级测试         |
| `GET /sentinel/info`            | -               | 查看配置信息           |

### 7.3 统一限流响应（SentinelConfig）

所有 HTTP 接口触发限流时，由 `SentinelConfig` 统一返回 JSON 响应：

```json
// FlowException（限流）
{"code": 429, "message": "请求过于频繁，已被限流", "path": "/interview/questions"}

// DegradeException（降级）
{"code": 503, "message": "服务暂时不可用，请稍后再试", "path": "/interview/questions"}

// SystemBlockException（系统保护）
{"code": 503, "message": "系统负载过高，请稍后再试", "path": "/interview/questions"}
```

---

## 八、控制台配置指南

### 8.1 启动控制台

```bash
java -jar sentinel-dashboard-1.8.6.jar
# 默认端口 8858
```

访问：http://localhost:8858（账号密码均为 sentinel）

### 8.2 应用接入配置

```yaml
# application-local.yml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8858  # 控制台地址
      eager: true                  # 立即注册，不等首次请求
```

### 8.3 在控制台配置规则示例

**流控规则**（给 `sentinelHello` 设置限流）：

```
资源名: sentinelHello
阈值类型: QPS
单机阈值: 2
流控效果: 快速失败
```

**降级规则**（给 `sentinelSlow` 设置慢调用降级）：

```
资源名: sentinelSlow
降级策略: 慢调用比例
最大RT: 300ms
比例阈值: 0.5
熔断时长: 10s
最小请求数: 5
```

---

## 九、Sentinel vs Hystrix

| 特性   | Sentinel        | Hystrix               |
|------|-----------------|-----------------------|
| 隔离策略 | 信号量（并发线程数）      | 线程池隔离                 |
| 熔断降级 | 基于响应时间、异常比例     | 基于异常比例                |
| 实时统计 | 滑动窗口（LeapArray） | 滑动窗口（RxJava）          |
| 控制台  | 完善（动态规则推送）      | 基础（Hystrix Dashboard） |
| 扩展性  | 强（SPI 扩展点）      | 有限                    |
| 维护状态 | 活跃维护            | 停止维护                  |

---

## 十、常见问题

### Q1：为什么规则在控制台配置后不生效？

**原因**：默认规则存储在内存中，应用重启后规则丢失。

**解决**：集成 Nacos 数据源持久化规则：

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        flow:
          nacos:
            server-addr: localhost:8848
            data-id: interview-service-flow-rules
            rule-type: flow
```

### Q2：blockHandler 方法签名错误会怎样？

如果方法签名不匹配，Sentinel 无法找到处理方法，会抛出 `BlockException` 到全局异常处理器。

**正确签名规则**：

- 与原方法相同的参数列表
- 在参数列表末尾追加 `BlockException`
- 返回值类型必须与原方法相同
- 访问修饰符不限（`public` 即可）
