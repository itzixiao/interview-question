# Gateway 鉴权与路由实战

## 一、架构概述

本文档详细讲解 Spring Cloud Gateway 的路由转发与 JWT 鉴权实现，采用模块化设计将鉴权逻辑抽取到公共模块
`interview-security`，实现代码复用和解耦。

### 1.1 模块职责划分

```
┌─────────────────────────────────────────────────────────────────┐
│                         客户端请求                                │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    interview-gateway (8080)                      │
│  ┌─────────────────┐    ┌─────────────────┐     ┌──────────────┐ │
│  │  路由转发         │───▶│  鉴权过滤器       │───▶│  负载均衡      │ │
│  │  StripPrefix    │    │  AuthenticationFilter │  lb://xxx    │ │
│  └─────────────────┘    └─────────────────┘     └──────────────┘ │
│                                │                                 │
│                                │ 依赖                            │
│                                ▼                                 │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │              interview-security (公共模块)                    ││
│  │  ├── GatewayJwtService        JWT 验证服务                    ││
│  │  ├── AuthenticationFilter     鉴权过滤器（响应式）              ││
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
│  ├── /api/auth/login    登录接口（生成 Token）                      │
│  ├── /api/auth/verify   验证接口                                  │
│  └── /api/users/*       用户接口（受保护）                          │
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

### 4.2 JWT 密钥配置

> **背景说明：** Gateway 验证 Token， Provider 签发 Token，两者必须使用**相同的私钥**才能正常工作。

**正确做法：密钥通过 Nacos `common.yml` 统一下发，各服务引用同一个配置项。**

```yaml
# 【Nacos】common.yml —— 所有服务共享，只在这一处定义
jwt:
  secret-key: "itzixiao-interview-system-secret-key-2026-this-is-a-demo-for-learning"
  issuer: "interview-provider"
  expiration: 86400  # 24小时
```

```yaml
# interview-gateway/src/main/resources/application-dev.yml
# Gateway 的 JwtProperties 绑定前缀为 security.jwt。
# 密钥不再硬编码，直接引用 common.yml 提供的变量。
# 本地调试无 Nacos 时占位符 ":" 后设置和 common.yml 相同的默认值即可。
security:
  jwt:
    secret-key: ${jwt.secret-key:itzixiao-interview-system-secret-key-2026-this-is-a-demo-for-learning}
    issuer: ${jwt.issuer:interview-provider}
    expiration: ${jwt.expiration:86400}
```

```yaml
# interview-provider/src/main/resources/application-dev.yml
# Provider 的 JwtConfig 绑定前缀为 jwt。
# 直接引用 common.yml 提供的同一批变量。
jwt:
  secret-key: ${jwt.secret-key:itzixiao-interview-system-secret-key-2026-this-is-a-demo-for-learning}
  expiration: ${jwt.expiration:86400}
  issuer: ${jwt.issuer:interview-provider}
```

**配置项命名差异说明：**

| 服务 | Spring 绑定前缀 | 对应类 | 说明 |
|------|------------|-------|------|
| Gateway | `security.jwt.*` | `JwtProperties` | 网关密钥仅用于验证 |
| Provider | `jwt.*` | `JwtConfig` | 业务服务密钥用于签发 |

两者 Spring 绑定前缀不同，但都引用 `${jwt.secret-key}` 这个来自 Nacos `common.yml` 的公共变量，所以实际密钥值始终保持一致。

**生产环境配置方式：**

```yaml
# 【生产】方式一：环境变量（最高优先级）
# 在容器或 K8s 的 Secret 中设置，配置文件不必动 任一个节点。
security:
  jwt:
    secret-key: ${JWT_SECRET_KEY:${jwt.secret-key:}}

# 【生产】方式二：纯 Nacos 配置中心管理
# Nacos common.yml 中统一设置，密钥轮换时只需在 Nacos 控制台修改一处，动态刷新。
```

---

## 五、请求流程详解

### 5.1 登录流程

```
┌──────────┐      POST /api/provider/api/auth/login      ┌──────────┐
│  客户端   │ ─────────────────────────────────────────▶  │ Gateway  │
└──────────┘                                             └──────────┘
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
│  客户端   │ ◀──────────────────────────────────────────────────┘
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

---

## 八、Gateway 与 Security 模块关系深度解析

### 8.1 模块依赖关系

`interview-gateway` 与 `interview-security` 是**依赖与被依赖**的关系，security 模块以公共库的形式被 gateway 引入：

```xml
<!-- interview-gateway/pom.xml -->
<dependency>
    <groupId>cn.itzixiao</groupId>
    <artifactId>interview-security</artifactId>
</dependency>
```

两个模块的职责完全分离：

```
┌──────────────────────────────────────────────────────────────────┐
│                     interview-security（公共库）                   │
│                                                                  │
│  gateway 包（响应式鉴权专用）           oauth2 包（通用 JWT 工具）     │
│  ├── GatewayAuthAutoConfiguration    ├── JwtTokenProvider        │
│  ├── AuthenticationFilter            └── JwtToken                │
│  ├── GatewayJwtService                                           │
│  ├── JwtProperties                  protection 包（防护工具）       │
│  └── WhiteListProperties            ├── RateLimiter              │
│                                     └── XssFilter                │
│                                                                  │
│                                     rbac 包（权限模型）             │
│                                     ├── User                     │
│                                     ├── Role                     │
│                                     └── Permission               │
└──────────────────────────────────────────────────────────────────┘
         ↑ 依赖                                  ↑ 可被任意服务引入
┌──────────────────┐                    ┌─────────────────────────┐
│interview-gateway │                    │ 其他业务服务（可选引入）    │
│（自动装配生效）     │                    │ interview-provider 等   │
└──────────────────┘                    └─────────────────────────┘
```

### 8.2 自动装配触发原理

`interview-security` 通过 Spring Boot SPI 机制实现自动装配，Gateway 引入依赖后无需任何额外配置即可启用鉴权过滤器：

```
# interview-security/src/main/resources/META-INF/spring/
# org.springframework.boot.autoconfigure.AutoConfiguration.imports
cn.itzixiao.interview.security.gateway.GatewayAuthAutoConfiguration
```

**装配条件链（四重条件全部满足才生效）：**

```java
@ConditionalOnClass({DispatcherHandler.class, GlobalFilter.class})
// 条件1：classpath 中存在 Gateway 核心类（引入了 spring-cloud-starter-gateway）

@ConditionalOnWebApplication(type = REACTIVE)
// 条件2：当前是响应式 Web 应用（Gateway 基于 WebFlux）

@ConditionalOnProperty(prefix = "gateway.auth", name = "enabled",
        havingValue = "true", matchIfMissing = true)
// 条件3：gateway.auth.enabled=true（默认为 true，不配置也生效）

@AutoConfigureBefore(GatewayAutoConfiguration.class)
// 条件4：在 GatewayAutoConfiguration 之前加载，确保过滤器优先注册
```

### 8.3 密钥共享机制

Gateway 和 Provider 使用**相同的 JWT 密钥**，这是 Token 能被 Gateway 验证的核心。正确方式是通过 **Nacos `common.yml`** 统一下发，两个服务**各自引用同一个配置变量**，而非各自写死同一个字符串：

```
┌──────────────────────────────────────────────────────────────────┐
│                   Nacos 配置中心 common.yml                        │
│                                                                   │
│   jwt:                                                             │
│     secret-key: "itzixiao-interview-system-secret-key-2026-..."   │
│     issuer: "interview-provider"                                   │
│     expiration: 86400                                              │
│                                                                   │
│         密钥只定义一次，所有服务引用这一处                         │
└──────────────────────────────────────────────────────────────────┘
             ↓ 下发到 Gateway                    ↓ 下发到 Provider
┌───────────────────────┐  ┌──────────────────────────────┐
│  interview-gateway       │  │      interview-provider           │
│                         │  │                                │
│  security.jwt.secret-key│  │  jwt.secret-key                 │
│  = ${jwt.secret-key}    │  │  = ${jwt.secret-key}           │
│                         │  │                                │
│  Spring 绑定前缀不同，    │  │  Spring 绑定前缀不同，           │
│  但引用的全局变量相同  │  │  但引用的全局变量相同           │
│                         │  │                                │
│  GatewayJwtService       │  │  JwtService                     │
│  validateToken() 验证    │  │  generateToken() 签发          │
└───────────────────────┘  └──────────────────────────────┘
```

**为什么不应该各自硬编码？**

| 问题 | 后果 |
|------|------|
| 密钥各自写死 | 轮换密钥时修改 N 个服务配置，漏改一处即全系统鉴权崩溃 |
| 密钥写入 Git 仓库 | 密钥泄露，安全风险极高 |
| 开发测试用同一密钥 | 一旦开发密钥泄露，生产鉴权直接被破 |

---

## 九、Token 存储与生命周期

### 9.1 Token 存储位置

本项目采用**无状态 JWT 方案**，Token 不在服务端存储，完全依赖签名验证：

```
┌──────────────────────────────────────────────────────────────────┐
│                        Token 生命周期                              │
│                                                                  │
│  1. 生成阶段（Provider）                                            │
│     JwtService.generateToken()                                    │
│     → 写入 username（subject）                                     │
│     → 写入 roles（自定义 claims）                                   │
│     → 写入 iat（签发时间）                                          │
│     → 写入 exp（过期时间 = 当前时间 + 86400秒）                      │
│     → 使用 SecretKey HMAC-SHA 签名                                │
│     → 返回 Base64Url 编码的三段式字符串                              │
│                                                                   │
│  2. 存储阶段（客户端负责）                                           │
│     ├── Web 端：localStorage / sessionStorage / Cookie            │
│     └── App 端：本地安全存储（KeyChain / SharedPreferences）        │
│     服务端：无存储，无 Session，完全无状态                            │
│                                                                 │
│  3. 使用阶段（每次请求携带）                                         │
│     请求头：Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...         │
│                                                                 │
│  4. 验证阶段（Gateway 拦截）                                        │
│     GatewayJwtService.validateToken()                           │
│     → 用相同 SecretKey 重新计算签名，与 Token 签名比对                │
│     → 检查 exp 是否早于当前时间                                     │
│     → 验证通过后解析 claims，注入请求头传递给下游                      │
│                                                                 │
│  5. 过期阶段                                                      │
│     → 客户端重新登录获取新 Token                                    │
│     → 旧 Token 自然失效（无需服务端操作）                             │
└──────────────────────────────────────────────────────────────────┘
```

### 9.2 JWT Token 结构

Provider 生成的 Token 包含以下 Payload 信息：

```json
// Header（算法声明）
{
  "alg": "HS256"
}

// Payload（实际存储内容）
{
  "sub": "admin",
  // 用户名（subject）
  "iss": "interview-provider",
  // 签发者（issuer）
  "iat": 1742860800,
  // 签发时间（issuedAt）
  "exp": 1742947200,
  // 过期时间（expiration）
  "roles": {
    "role": "ADMIN",
    "permissions": [
      "read",
      "write",
      "delete"
    ]
  }
}

// Signature（防篡改签名）
HMACSHA256(base64(header) + "." + base64(payload), secretKey)
```

### 9.3 Token 生成源码追踪

```java
// 1. 登录请求进入 JwtController.login()
@PostMapping("/api/auth/login")
public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
    // 验证用户名密码（生产环境查数据库）
    String username = loginRequest.get("username");

    // 构建角色信息（写入 Token Payload）
    Map<String, Object> roles = new HashMap<>();
    roles.put("role", "ADMIN");
    roles.put("permissions", new String[]{"read", "write", "delete"});

    // 2. 调用 JwtService.generateToken() 生成 Token
    String token = jwtService.generateToken(username, roles);

    // 3. 返回 Token 给客户端（客户端自行存储）
    response.put("token", token);
    response.put("tokenType", "Bearer");
    response.put("expiresIn", 86400);
    return Result.success(response);
}

// 2. JwtService.generateToken() 内部实现
public String generateToken(String username, Map<String, Object> roles) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expiration * 1000); // 86400秒后过期

    return Jwts.builder()
            .claims(claims)          // 写入 roles 等自定义 claims
            .subject(username)       // 写入 sub = username
            .issuer(issuer)          // 写入 iss = "interview-provider"
            .issuedAt(now)           // 写入 iat = 当前时间
            .expiration(expiryDate)  // 写入 exp = 当前时间 + 86400s
            .signWith(secretKey)     // HMAC-SHA256 签名
            .compact();              // 生成 Base64Url 三段式字符串
}
```

---

## 十、下游服务如何获取用户信息

### 10.1 网关透传机制

Gateway 验证 Token 通过后，会将用户信息**解析并注入到请求头**，下游服务无需重复验证 Token：

```java
// AuthenticationFilter.java - 验证通过后的处理
String username = jwtService.getUsernameFromToken(token);  // 从 Token 解析用户名
Object roles = jwtService.getRolesFromToken(token);     // 从 Token 解析角色
String userId = jwtService.getUserIdFromToken(token);    // 从 Token 解析用户ID

// 将用户信息写入请求头，转发给下游服务
ServerHttpRequest mutableReq = request.mutate()
        .header("X-User-Id", userId)                      // 用户ID
        .header("X-User-Name", username)                    // 用户名
        .header("X-User-Roles", roles != null ? roles.toString() : "")  // 角色
        .header("X-Auth-Source", "gateway")                   // 标记来源为网关
        .build();
```

**下游服务透传的请求头一览：**

| 请求头             | 值示例                               | 说明           |
|-----------------|-----------------------------------|--------------|
| `X-User-Id`     | `admin`                           | 用户唯一标识（或 ID） |
| `X-User-Name`   | `admin`                           | 用户名          |
| `X-User-Roles`  | `{role=ADMIN, permissions=[...]}` | 角色权限信息       |
| `X-Auth-Source` | `gateway`                         | 标记已经过网关鉴权    |

### 10.2 业务服务获取用户信息

下游服务（如 interview-provider）通过 `@RequestHeader` 直接读取网关注入的请求头：

```java
// 方式一：通过 @RequestHeader 获取（推荐）
@GetMapping("/api/users/{id}")
public Result<UserDTO> getUserInfo(
        @PathVariable Long id,
        @RequestHeader(value = "X-User-Id", required = false) String userId,
        @RequestHeader(value = "X-User-Name", required = false) String username,
        @RequestHeader(value = "X-User-Roles", required = false) String roles) {

    log.info("当前操作用户: id={}, name={}, roles={}", userId, username, roles);
    // 直接使用，无需再解析 Token
    return Result.success(userStorage.get(id));
}

// 方式二：通过 HttpServletRequest 获取
@GetMapping("/api/orders")
public Result<?> getOrders(HttpServletRequest request) {
    String userId = request.getHeader("X-User-Id");
    String username = request.getHeader("X-User-Name");
    String roles = request.getHeader("X-User-Roles");
    // 用于权限判断、数据隔离等业务逻辑
    return Result.success(orderService.getByUserId(userId));
}
```

### 10.3 服务间调用（Feign）鉴权

通过 Feign 进行服务间调用时，需要使用 `RequestInterceptor` 将 Token 或用户信息透传：

```java
/**
 * Feign 请求拦截器 - 透传用户信息
 * 将当前请求的用户信息（来自网关注入的请求头）透传给被调用的服务
 */
@Component
public class FeignUserContextInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        // 获取当前请求上下文
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return;

        HttpServletRequest request = attributes.getRequest();

        // 透传网关注入的用户信息头
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-User-Name");
        String roles = request.getHeader("X-User-Roles");

        if (StringUtils.hasText(userId)) {
            template.header("X-User-Id", userId);
            template.header("X-User-Name", username);
            template.header("X-User-Roles", roles);
            template.header("X-Auth-Source", "feign");  // 标记来源为 Feign 调用
        }
    }
}
```

### 10.4 完整鉴权数据流

```
客户端                  Gateway                  Provider               Service B
  │                       │                          │                      │
  │  POST /api/provider/  │                          │                      │
  │  api/auth/login       │                          │                      │
  │──────────────────────▶│                          │                      │
  │                       │  白名单放行                │                      │
  │                       │─────────────────────────▶│                      │
  │                       │                          │ JwtService           │
  │                       │                          │ .generateToken()     │
  │                       │                          │ 生成含 roles 的 JWT   │
  │◀──────────────────────│◀─────────────────────────│                      │
  │ {token: "eyJ..."}     │                          │                      │
  │                       │                          │                      │
  │  GET /api/provider/   │                          │                      │
  │  api/users/1          │                          │                      │
  │  Authorization:       │                          │                      │
  │  Bearer eyJ...        │                          │                      │
  │──────────────────────▶│                          │                      │
  │                       │ AuthenticationFilter     │                      │
  │                       │ 1. 不在白名单              │                      │
  │                       │ 2. 提取 Bearer Token      │                      │
  │                       │ 3. GatewayJwtService     │                      │
  │                       │    .validateToken()      │                      │
  │                       │ 4. 解析 username/roles    │                      │
  │                       │ 5. 注入 X-User-* 请求头    │                      │
  │                       │─────────────────────────▶│                      │
  │                       │                          │ @RequestHeader       │
  │                       │                          │ 获取 X-User-Id 等     │
  │                       │                          │                      │
  │                       │                          │ Feign 调用 Service B  │
  │                       │                          │ 透传 X-User-* 头      │
  │                       │                          │─────────────────────▶│
  │◀──────────────────────│◀─────────────────────────│◀─────────────────────│
```

---

## 十一、其他模块接入鉴权指南

### 11.1 新服务接入步骤

**步骤 1：在 Gateway 添加路由规则**

```yaml
# interview-gateway/src/main/resources/application-dev.yml
spring:
  cloud:
    gateway:
      routes:
        - id: interview-new-service
          uri: lb://interview-new-service
          predicates:
            - Path=/api/new/**
          filters:
            - StripPrefix=1
```

**步骤 2：配置白名单（按需放行）**

```yaml
# interview-gateway/src/main/resources/application.yml
gateway:
  white-list:
    paths:
      # 需要开放的接口才加入白名单，默认全部需要 Token
      - /api/new/public/**    # 公共接口无需鉴权
      # 不加则默认受 AuthenticationFilter 保护
```

**步骤 3：业务服务从请求头获取用户信息（无需引入 security 模块）**

```java

@RestController
public class NewServiceController {

    @GetMapping("/data")
    public Result<?> getData(
            // 直接读取 Gateway 注入的请求头
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        // userId/username/roles 已由网关验证并传入，业务直接使用
        log.info("请求用户: {}, 角色: {}", username, roles);
        return Result.success(dataService.getByUserId(userId));
    }
}
```

### 11.2 接入方式对比

| 方式                    | 适用场景       | 是否需要引入 security 模块 | 说明                       |
|-----------------------|------------|--------------------|--------------------------|
| **网关统一鉴权**            | 标准 HTTP 接口 | 否                  | Gateway 拦截，X-User-* 头传递  |
| **引入 security 模块自鉴权** | 直连场景/内部服务  | 是                  | 使用 JwtTokenProvider 独立验证 |
| **Feign 透传**          | 微服务间调用     | 否                  | 通过 RequestInterceptor 透传 |

### 11.3 Token 主动失效方案（扩展）

标准 JWT 无法主动失效，以下是生产中常用的补充方案：

```
方案一：Redis 黑名单（推荐）
  退出登录 → 将 Token 的 jti（JWT ID）写入 Redis 黑名单
  Gateway 验证时 → 额外检查 jti 是否在黑名单中
  黑名单 TTL = Token 剩余有效时间

方案二：版本号机制
  用户表维护 token_version 字段
  Token Payload 中写入 token_version
  Gateway 验证时调用 Provider 接口比对版本号
  修改密码/强制下线 → 自增 token_version，旧 Token 失效

方案三：短有效期 + Refresh Token
  Access Token：15分钟有效
  Refresh Token：7天有效，存 Redis
  Access Token 过期 → 用 Refresh Token 换取新 Access Token
  主动吊销 → 删除 Redis 中的 Refresh Token
```

---

## 十二、总结

### 12.1 关键配置项

| 配置项                           | 位置       | 说明                            |
|-------------------------------|----------|-------------------------------|
| `server.servlet.context-path` | 各服务      | 服务上下文路径                       |
| `spring.cloud.gateway.routes` | Gateway  | 路由规则                          |
| `StripPrefix`                 | Gateway  | 路径前缀剥离数量                      |
| `gateway.white-list.paths`    | Gateway  | 白名单路径                         |
| `security.jwt.secret-key`     | Gateway  | 网关 JWT 验证密钥                   |
| `jwt.secret-key`              | Provider | 业务服务 JWT 签发密钥（与 Gateway 必须一致） |
| `gateway.auth.enabled`        | Gateway  | 是否启用鉴权（默认 true）               |

### 12.2 最佳实践

1. **统一密钥管理**：使用 Nacos 配置中心 `common.yml` 统一下发 JWT 密钥，避免多处配置不一致
2. **白名单最小化**：只开放必要的接口，避免安全风险
3. **Token 有效期**：Access Token 建议 15 分钟~2 小时，配合 Refresh Token 使用
4. **日志记录**：记录鉴权失败的请求（路径 + IP），便于安全审计
5. **响应式编程**：Gateway 使用 WebFlux，鉴权过滤器严禁阻塞操作（IO、DB 查询等）
6. **Header 防伪造**：业务服务应检查 `X-Auth-Source: gateway`，防止绕过网关直接伪造 X-User-* 头
7. **敏感信息不入 Token**：密码、身份证等敏感字段不要写入 JWT Payload（Base64 可解码）
