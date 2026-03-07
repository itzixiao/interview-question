# Spring Cloud Gateway 详解

## 一、Gateway 概述

### 什么是 API 网关？

API 网关是微服务架构的入口，统一接收所有客户端请求。

**主要功能**：
- 路由转发：根据规则将请求转发到不同的服务
- 认证鉴权：统一的身份验证和权限控制
- 限流熔断：保护后端服务，防止过载
- 日志监控：统一的访问日志和监控
- 协议转换：HTTP/WebSocket 等协议转换

### 为什么选择 Spring Cloud Gateway？

| 特性 | Zuul 1.x | Gateway |
|------|----------|---------|
| IO 模型 | 同步阻塞 | 异步非阻塞 |
| 底层框架 | Servlet | WebFlux/Netty |
| 性能 | 较低 | 高（约1.6倍） |
| WebSocket | 不支持 | 支持 |
| 编程模型 | 命令式 | 响应式 |
| Spring 集成 | 兼容 | 原生支持 |

---

## 二、核心组件

### 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                     Spring Cloud Gateway                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│   Client → Gateway Handler Mapping → Gateway Web Handler         │
│                    ↓                        ↓                    │
│              匹配 Route                执行 Filter Chain          │
│                                            ↓                     │
│                                      Downstream Service          │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Route（路由）

路由是 Gateway 的基本构建单元：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service           # 唯一标识
          uri: lb://user-service     # 目标地址（lb:// 表示负载均衡）
          predicates:                # 断言（匹配条件）
            - Path=/api/user/**
          filters:                   # 过滤器
            - StripPrefix=1
```

### Predicate（断言）

断言用于匹配 HTTP 请求，决定请求是否路由到该路由。

### Filter（过滤器）

两种类型：
- **GatewayFilter**：作用于特定路由
- **GlobalFilter**：作用于所有路由

---

## 三、内置断言（Predicate）

| 断言 | 说明 | 示例 |
|------|------|------|
| Path | 路径匹配 | `Path=/api/**` |
| Method | HTTP 方法 | `Method=GET,POST` |
| Header | 请求头匹配 | `Header=X-Token, \d+` |
| Query | 查询参数 | `Query=name, zhang.*` |
| Cookie | Cookie 匹配 | `Cookie=sessionId, .*` |
| Host | 主机名匹配 | `Host=**.example.com` |
| RemoteAddr | IP 匹配 | `RemoteAddr=192.168.1.0/24` |
| Before/After | 时间匹配 | `After=2024-01-01T00:00:00+08:00` |
| Weight | 权重（灰度发布）| `Weight=group1, 8` |

**配置示例**：

```yaml
predicates:
  - Path=/api/user/**
  - Method=GET,POST
  - Header=Authorization, Bearer.*
  - Query=version, v2
```

---

## 四、内置过滤器（Filter）

### 常用过滤器

| 过滤器 | 说明 |
|--------|------|
| StripPrefix | 去掉路径前缀 |
| AddRequestHeader | 添加请求头 |
| AddResponseHeader | 添加响应头 |
| RewritePath | 重写路径 |
| Retry | 失败重试 |
| CircuitBreaker | 熔断器 |
| RequestRateLimiter | 限流 |

### 配置示例

```yaml
filters:
  # 去掉前缀：/api/user/list → /list
  - StripPrefix=1
  
  # 添加请求头
  - AddRequestHeader=X-Request-Source, gateway
  
  # 路径重写
  - RewritePath=/api/(?<segment>.*), /$\{segment}
  
  # 重试配置
  - name: Retry
    args:
      retries: 3
      statuses: BAD_GATEWAY,SERVICE_UNAVAILABLE
      
  # 限流配置
  - name: RequestRateLimiter
    args:
      redis-rate-limiter.replenishRate: 10
      redis-rate-limiter.burstCapacity: 20
      key-resolver: "#{@ipKeyResolver}"
      
  # 熔断配置
  - name: CircuitBreaker
    args:
      name: myCircuitBreaker
      fallbackUri: forward:/fallback
```

---

## 五、自定义过滤器

### GlobalFilter（全局过滤器）

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Pre 阶段：请求前处理
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        
        if (!StringUtils.hasText(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        
        // 传递用户信息到下游
        ServerHttpRequest request = exchange.getRequest().mutate()
            .header("X-User-Id", "user123")
            .build();
        
        return chain.filter(exchange.mutate().request(request).build())
            .then(Mono.fromRunnable(() -> {
                // Post 阶段：响应后处理
            }));
    }

    @Override
    public int getOrder() {
        return 0;  // 数值越小，优先级越高
    }
}
```

### GatewayFilterFactory（路由过滤器工厂）

```java
@Component
public class IpBlacklistGatewayFilterFactory 
        extends AbstractGatewayFilterFactory<Config> {

    public IpBlacklistGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("blacklist");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = getClientIp(exchange.getRequest());
            
            if (config.getBlacklist().contains(clientIp)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            
            return chain.filter(exchange);
        };
    }
    
    public static class Config {
        private Set<String> blacklist;
        // getter/setter
    }
}
```

使用：
```yaml
filters:
  - IpBlacklist=192.168.1.100,10.0.0.1
```

---

## 六、限流

### 基于 Redis 的令牌桶限流

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: rate-limit-route
          uri: lb://some-service
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10   # 每秒补充10个令牌
                redis-rate-limiter.burstCapacity: 20   # 令牌桶容量
                key-resolver: "#{@ipKeyResolver}"      # 限流维度
```

### KeyResolver 配置

```java
@Configuration
public class RateLimiterConfig {

    // 按 IP 限流
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }
    
    // 按用户限流
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }
}
```

---

## 七、熔断

### 集成 Resilience4j

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: circuit-breaker-route
          uri: lb://some-service
          filters:
            - name: CircuitBreaker
              args:
                name: myCircuitBreaker
                fallbackUri: forward:/fallback

resilience4j:
  circuitbreaker:
    instances:
      myCircuitBreaker:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10000
  timelimiter:
    instances:
      myCircuitBreaker:
        timeoutDuration: 3s
```

### 熔断状态

```
CLOSED（正常） → 失败率超过阈值 → OPEN（熔断）
                                    ↓ 等待超时
                            HALF_OPEN（半开）
                                    ↓ 探测成功
                            CLOSED（恢复正常）
```

---

## 八、动态路由

### 基于服务发现

```yaml
spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true
          lower-case-service-id: true
```

启用后，自动为注册中心的每个服务创建路由。

### 基于 Nacos 动态配置

```java
@Component
public class NacosRouteListener {

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;
    
    @Autowired
    private ApplicationEventPublisher publisher;

    public void updateRoute(RouteDefinition route) {
        // 保存路由
        routeDefinitionWriter.save(Mono.just(route)).subscribe();
        // 发布刷新事件
        publisher.publishEvent(new RefreshRoutesEvent(this));
    }
}
```

---

## 九、跨域配置

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOriginPatterns: "*"
            allowedMethods: "*"
            allowedHeaders: "*"
            allowCredentials: true
            maxAge: 3600
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Credentials Access-Control-Allow-Origin
```

---

## 十、高频面试题

### Q1：Spring Cloud Gateway 的核心组件有哪些？

**答案**：
- **Route（路由）**：由 ID、URI、断言、过滤器组成
- **Predicate（断言）**：匹配 HTTP 请求的条件
- **Filter（过滤器）**：处理请求和响应
  - GatewayFilter：路由级
  - GlobalFilter：全局

---

### Q2：Gateway 和 Zuul 的区别？

**答案**：
- Gateway 基于 WebFlux，异步非阻塞，性能更高
- Zuul 1.x 基于 Servlet，同步阻塞
- Gateway 支持 WebSocket，Zuul 不支持
- Gateway 是 Spring Cloud 官方推荐

---

### Q3：过滤器的执行顺序？

**答案**：
- 实现 `Ordered` 接口，`getOrder()` 返回优先级
- 数值越小，优先级越高
- Pre 阶段：按 order 从小到大执行
- Post 阶段：按 order 从大到小执行（逆序）

---

### Q4：GlobalFilter 和 GatewayFilter 的区别？

**答案**：

| 类型 | 作用范围 | 配置方式 |
|------|---------|---------|
| GlobalFilter | 所有路由 | 实现接口，@Component |
| GatewayFilter | 特定路由 | YAML filters 配置 |

---

### Q5：Gateway 如何实现限流？

**答案**：
使用 `RequestRateLimiter` 过滤器 + Redis：
- 基于令牌桶算法
- `replenishRate`：每秒补充令牌数
- `burstCapacity`：令牌桶容量
- `KeyResolver`：限流维度（IP/用户/接口）

---

### Q6：Gateway 如何实现熔断降级？

**答案**：
集成 Resilience4j 或 Sentinel：
- 配置 `CircuitBreaker` 过滤器
- 设置 `fallbackUri` 降级路径
- 熔断状态：CLOSED → OPEN → HALF_OPEN

---

### Q7：如何实现动态路由？

**答案**：
1. **服务发现**：`discovery.locator.enabled=true`
2. **配置中心**：监听 Nacos 配置变化，调用 `RouteDefinitionWriter` 更新
3. **Actuator**：`POST /actuator/gateway/routes/{id}`

---

### Q8：Gateway 为什么基于 WebFlux？

**答案**：
- **异步非阻塞**：少量线程处理大量请求
- **高吞吐量**：适合网关这种 IO 密集型场景
- **背压支持**：响应式流控制
- **函数式编程**：更简洁的代码

---

### Q9：如何传递用户信息到下游服务？

**答案**：
在 GlobalFilter 中修改请求头：

```java
ServerHttpRequest request = exchange.getRequest().mutate()
    .header("X-User-Id", userId)
    .build();
return chain.filter(exchange.mutate().request(request).build());
```

下游服务通过请求头获取用户信息。

---

### Q10：如何处理跨域（CORS）？

**答案**：
配置 `globalcors`：

```yaml
spring.cloud.gateway.globalcors.cors-configurations:
  '[/**]':
    allowedOriginPatterns: "*"
    allowedMethods: "*"
    allowedHeaders: "*"
    allowCredentials: true
```

注意：Gateway 处理跨域后，下游服务不要重复处理。

---

## 十一、最佳实践

1. **统一认证**：在 Gateway 层实现，下游服务信任网关传递的用户信息
2. **限流策略**：根据业务场景选择合适的限流维度
3. **熔断配置**：合理设置阈值和等待时间
4. **日志记录**：记录请求入口和出口，便于问题排查
5. **跨域统一处理**：在 Gateway 层处理，避免重复配置
6. **动态路由**：结合配置中心实现路由的动态更新
7. **灰度发布**：使用 Weight 断言实现流量分配
