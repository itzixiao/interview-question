# Gateway 鉴权与路由实战

## 一、架构概述

本文档详细讲解 Spring Cloud Gateway 的路由转发与 JWT 鉴权实现，采用模块化设计将鉴权逻辑抽取到公共模块
`interview-security`，实现代码复用和解耦。

### 1.1 模块职责划分

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端请求                               │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    interview-gateway (8080)                      │
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │  路由转发        │───▶│  鉴权过滤器      │───▶│  负载均衡    │ │
│  │  StripPrefix    │    │  AuthenticationFilter │  lb://xxx   │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
│                                │                                 │
│                                │ 依赖                            │
│                                ▼                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              interview-security (公共模块)                   ││
│  │  ├── GatewayJwtService        JWT 验证服务                   ││
│  │  ├── AuthenticationFilter     鉴权过滤器（响应式）            ││
│  │  ├── WhiteListProperties      白名单配置                      ││
│  │  ├── JwtProperties            JWT 配置                       ││
│  │  └── GatewayAuthAutoConfiguration 自动配置                    ││
│  └─────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    interview-provider (8082)                     │
│  context-path: /provider                                         │
│  ├── /api/auth/login    登录接口（生成 Token）                    │
│  ├── /api/auth/verify   验证接口                                  │
│  └── /api/users/*       用户接口（受保护）                        │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 核心设计原则

| 原则        | 说明                                  |
|-----------|-------------------------------------|
| **单一职责**  | Gateway 负责路由和鉴权，Provider 负责业务逻辑     |
| **模块化**   | 鉴权逻辑抽取到 interview-security，可被多个网关复用 |
| **配置外置**  | 白名单、JWT 密钥等通过配置文件管理                 |
| **响应式编程** | Gateway 使用 WebFlux，鉴权过滤器需实现响应式接口    |

---

## 二、路由配置详解

### 2.1 context-path 配置

每个微服务配置独立的 `context-path`，便于 Gateway 区分和路由：

```yaml
# interview-provider/src/main/resources/application-dev.yml
server:
  port: 8082
  servlet:
    context-path: /provider
```

**各服务 context-path 一览：**

| 服务                  | 端口   | context-path |
|---------------------|------|--------------|
| interview-provider  | 8082 | `/provider`  |
| interview-service   | 8081 | `/service`   |
| interview-algorithm | 8083 | `/algorithm` |
| interview-kafka     | 8086 | `/kafka`     |
| interview-rabbitmq  | 8085 | `/rabbitmq`  |

### 2.2 Gateway 路由规则

```yaml
# interview-gateway/src/main/resources/application-dev.yml
spring:
  cloud:
    gateway:
      routes:
        # 服务提供者
        - id: interview-provider
          uri: lb://interview-provider    # 负载均衡，从 Nacos 获取服务实例
          predicates:
            - Path=/api/provider/**       # 匹配 /api/provider/** 的请求
          filters:
            - StripPrefix=1               # 去掉第一段 /api
          # 转发结果：/api/provider/xxx → /provider/xxx
```

### 2.3 StripPrefix 工作原理

```
原始请求：    /api/provider/api/auth/login
StripPrefix=1 去掉 /api
转发路径：    /provider/api/auth/login

到达 Provider 服务：
  context-path: /provider
  Controller:   /api/auth/login
  完整路径：    /provider/api/auth/login ✓
```

### 2.4 StripPrefix 源码分析

```java
// org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory
public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();
        
        // 按 "/" 分割路径
        String[] parts = path.split("/");
        
        // 构建 StripPrefix 数
        int partsToStrip = config.getParts();
        StringBuilder newPath = new StringBuilder("/");
        
        // 跳过前 N 段，重新拼接路径
        for (int i = partsToStrip + 1; i < parts.length; i++) {
            newPath.append(parts[i]);
            if (i < parts.length - 1) {
                newPath.append("/");
            }
        }
        
        // 创建新请求
        ServerHttpRequest newRequest = request.mutate()
            .path(newPath.toString())
            .build();
        
        return chain.filter(exchange.mutate().request(newRequest).build());
    };
}
```

---

## 三、JWT 鉴权实现

### 3.1 鉴权模块设计

将鉴权逻辑抽取到 `interview-security` 公共模块，实现自动配置：

```
interview-security/
├── src/main/java/cn/itzixiao/interview/security/gateway/
│   ├── GatewayAuthAutoConfiguration.java   # 自动配置类
│   ├── AuthenticationFilter.java           # 鉴权过滤器
│   ├── GatewayJwtService.java              # JWT 服务
│   ├── WhiteListProperties.java            # 白名单配置
│   └── JwtProperties.java                  # JWT 配置
└── src/main/resources/META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 3.2 自动配置类

```java
/**
 * Gateway 鉴权自动配置
 * 
 * 条件加载：
 * 1. @ConditionalOnClass: 需要 DispatcherHandler 和 GlobalFilter 类存在
 * 2. @ConditionalOnWebApplication: 需要是响应式 Web 应用
 * 3. @ConditionalOnProperty: 配置 gateway.auth.enabled=true（默认启用）
 */
@Slf4j
@Configuration
@ConditionalOnClass({DispatcherHandler.class, GlobalFilter.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "gateway.auth", name = "enabled", 
                       havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(GatewayAutoConfiguration.class)
@EnableConfigurationProperties({WhiteListProperties.class, JwtProperties.class})
public class GatewayAuthAutoConfiguration {

    /**
     * 创建 JWT 验证服务
     */
    @Bean
    public GatewayJwtService gatewayJwtService(JwtProperties jwtProperties) {
        log.info("初始化 Gateway JWT 服务");
        return new GatewayJwtService(jwtProperties.getSecretKey());
    }

    /**
     * 创建鉴权过滤器
     */
    @Bean
    public AuthenticationFilter authenticationFilter(
            WhiteListProperties whiteListProperties,
            GatewayJwtService jwtService) {
        log.info("初始化 Gateway 鉴权过滤器");
        return new AuthenticationFilter(whiteListProperties, jwtService);
    }
}
```

### 3.3 鉴权过滤器（响应式实现）

```java
/**
 * Gateway 全局鉴权过滤器
 * 
 * 实现原理：
 * 1. 实现 GlobalFilter 接口，自动注册为全局过滤器
 * 2. 使用 Ordered 接口控制执行顺序
 * 3. 返回 Mono<Void> 实现响应式编程
 */
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final WhiteListProperties whiteListProperties;
    private final GatewayJwtService jwtService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 检查白名单
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取 Authorization Header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少认证令牌");
        }

        // 3. 提取并验证 Token
        String token = authHeader.substring(7);
        try {
            if (!jwtService.validateToken(token)) {
                return unauthorized(exchange, "认证令牌无效或已过期");
            }

            // 4. 解析用户信息并添加到请求头
            Claims claims = jwtService.parseToken(token);
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Roles", String.valueOf(claims.get("roles")))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            return unauthorized(exchange, "认证令牌无效或已过期");
        }
    }

    /**
     * 检查路径是否在白名单中
     */
    private boolean isWhiteListed(String path) {
        for (String pattern : whiteListProperties.getPaths()) {
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                if (path.startsWith(prefix)) {
                    return true;
                }
            } else if (path.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回 401 未授权响应
     */
    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
            "{\"code\":401,\"data\":null,\"message\":\"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes());
        
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;  // 优先级：在路由转发之前执行
    }
}
```

### 3.4 JWT 服务类

```java
/**
 * Gateway JWT 验证服务
 * 
 * 职责：
 * 1. 验证 Token 签名和有效期
 * 2. 解析 Token 获取 Claims
 */
public class GatewayJwtService {

    private final String secretKey;

    public GatewayJwtService(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * 验证 Token 有效性
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 Token 获取 Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secretKey.getBytes()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```

---

## 四、配置详解

### 4.1 白名单配置

```yaml
# interview-gateway/src/main/resources/application.yml
gateway:
  white-list:
    paths:
      - /login                           # 登录页面
      - /register                        # 注册页面
      - /api/provider/api/auth/login     # JWT 登录接口
      - /api/provider/api/auth/verify    # JWT 验证接口
      - /api/service/**                  # 面试题服务所有接口
```

### 4.2 JWT 配置

```yaml
# interview-gateway/src/main/resources/application-dev.yml
security:
  jwt:
    secret-key: "itzixiao-interview-system-secret-key-2026"
    issuer: "interview-provider"
    expiration: 86400  # 24小时
```

**重要：** Gateway 和 Provider 必须使用相同的 `secret-key`，否则 Token 验证会失败。

---

## 五、请求流程详解

### 5.1 登录流程

```
┌──────────┐      POST /api/provider/api/auth/login      ┌──────────┐
│  客户端   │ ─────────────────────────────────────────▶  │ Gateway  │
└──────────┘                                              └──────────┘
                                                                │
                                                    白名单匹配，放行
                                                                │
                                                                ▼
                                                          ┌──────────┐
                                                          │ Provider │
                                                          └──────────┘
                                                                │
                                                    验证用户名密码
                                                    生成 JWT Token
                                                                │
                                                                ▼
┌──────────┐   {"token": "eyJhbG...", "tokenType": "Bearer"}    │
│  客户端   │ ◀───────────────────────────────────────────────── │
└──────────┘
```

### 5.2 访问受保护资源流程

```
┌──────────┐  GET /api/provider/api/users/1              ┌──────────┐
│  客户端   │  Authorization: Bearer eyJhbG...            │ Gateway  │
└──────────┘                                             └──────────┘
                                                                │
                                                    ┌───────────┴───────────┐
                                                    │ AuthenticationFilter  │
                                                    └───────────┬───────────┘
                                                                │
                                              1. 检查白名单（不在白名单）
                                              2. 提取 Token
                                              3. 验证签名和有效期
                                              4. 解析用户信息
                                                                │
                                                    验证通过，添加请求头
                                                                │
                                                                ▼
                                                          ┌──────────┐
                                                          │ Provider │
                                                          └──────────┘
```

### 5.3 完整请求路径示例

```
Gateway 接收请求：
  http://localhost:8080/api/provider/api/auth/login

路由匹配：
  Path=/api/provider/** ✓

StripPrefix=1 处理：
  /api/provider/api/auth/login
  → /provider/api/auth/login

负载均衡转发：
  lb://interview-provider
  → 选择一个 Provider 实例

Provider 接收请求：
  完整路径：/provider/api/auth/login
  context-path 匹配：/provider ✓
  Controller 匹配：/api/auth/login ✓
  方法匹配：POST /login ✓
```

---

## 六、HTTP 测试用例

```http
### 测试 1: 直接访问受保护接口（无 Token）
### 预期结果: 401 未授权
GET http://localhost:8080/api/provider/api/users/1
Accept: application/json

### 测试 2: 登录获取 Token（白名单接口）
### 预期结果: 200 成功，返回 token
POST http://localhost:8080/api/provider/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}

### 测试 3: 携带 Token 访问受保护接口
### 预期结果: 200 成功
GET http://localhost:8080/api/provider/api/users/1
Accept: application/json
Authorization: Bearer <TOKEN>

### 测试 4: 直接访问 Provider（绕过网关）
POST http://localhost:8082/provider/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

---

## 七、常见问题与解决方案

### 7.1 404 Not Found

**原因：** StripPrefix 数值与实际路径不匹配

**排查步骤：**

```bash
# 1. 检查原始请求路径
原始请求：/api/provider/api/auth/login

# 2. 检查 StripPrefix 值
StripPrefix=1 → 去掉 /api → /provider/api/auth/login

# 3. 检查 Provider 完整路径
context-path: /provider
Controller:   /api/auth/login
完整路径：    /provider/api/auth/login ✓
```

### 7.2 Token 验证失败

**可能原因：**

1. **密钥不一致**：Gateway 和 Provider 使用不同的 `secret-key`
2. **Token 过期**：检查 `exp` 字段
3. **格式错误**：确保 Header 格式为 `Bearer <token>`

### 7.3 白名单不生效

**检查清单：**

1. 配置文件格式是否正确（YAML 缩进）
2. 路径是否包含 context-path
3. `WhiteListProperties` 是否被正确注入

```yaml
# 正确的白名单配置
gateway:
  white-list:
    paths:
      - /api/provider/api/auth/login   # 完整的 Gateway 请求路径
```

---

## 八、总结

### 8.1 关键配置项

| 配置项                           | 位置                 | 说明           |
|-------------------------------|--------------------|--------------|
| `server.servlet.context-path` | 各服务                | 服务上下文路径      |
| `spring.cloud.gateway.routes` | Gateway            | 路由规则         |
| `StripPrefix`                 | Gateway            | 路径前缀剥离数量     |
| `gateway.white-list.paths`    | Gateway            | 白名单路径        |
| `security.jwt.secret-key`     | Gateway + Provider | JWT 密钥（必须一致） |

### 8.2 最佳实践

1. **统一密钥管理**：使用 Nacos 配置中心或环境变量管理 JWT 密钥
2. **白名单最小化**：只开放必要的接口，避免安全风险
3. **Token 有效期**：根据业务需求设置合理的过期时间
4. **日志记录**：记录鉴权失败的请求，便于安全审计
5. **响应式编程**：Gateway 使用 WebFlux，避免阻塞操作
