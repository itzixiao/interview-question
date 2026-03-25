# Gateway 鉴权与路由实战

## 一、架构概述

本文档详细讲解 Spring Cloud Gateway 的路由转发与 JWT 鉴权实现，采用模块化设计将鉴权逻辑抽取到公共模块
`interview-security`，实现代码复用和解耦。

### 1.1 模块职责划分

```
┌──────────────────────────────────────────────────────────────────┐
│                          客户端请求                                │
└──────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                    interview-gateway (8080)                      │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────────┐│
│  │                GatewayRedisAuthConfig                        ││
│  │  @Primary TokenBlacklistService（直接注入 StringRedisTemplate）││
│  │  覆盖 security 模块的懒加载实现，Gateway 内直接读写 Redis          ││
│  └──────────────────────────────────────────────────────────────┘│
│                            │                                     │
│                            ▼                                     │
│  ┌───────────────┐   ┌───────────────────────┐    ┌───────────┐  │
│  │  路由转发      │──▶│  AuthenticationFilter  │──▶│  负载均衡   │  │
│  │  StripPrefix  │   │  order = -100          │   │ lb://xxx  │  │
│  └───────────────┘   └──────────┬────────────┘    └───────────┘  │
│                                 │                                │
│                         Redis 双重校验                            │
│                    ┌────────────┴────────────┐                   │
│                    │  1. jti 黑名单校验        │                    │
│                    │  2. 在线 token 一致性     │                    │
│                    │     jwt:token:{username} │                   │
│                    └────────────────────────-┘                    │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              interview-security (公共模块)                 │   │
│  │  ├── GatewayAuthAutoConfiguration   自动配置（SPI）         │   │
│  │  ├── AuthenticationFilter           鉴权过滤器（响应式）     │   │
│  │  ├── GatewayJwtService              JWT 验证服务           │   │
│  │  ├── TokenBlacklistService          黑名单+踢下线校验        │   │
│  │  ├── RedisAccessor                  Redis 访问器（类隔离）   │   │
│  │  ├── WhiteListProperties            白名单配置              │   │
│  │  └── JwtProperties                  JWT 配置               │   │
│  └───────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘
                                 │
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                    interview-provider (8082)                     │
│  context-path: /provider                                         │
│  ├── /api/auth/login    登录接口（生成 Token + 存入 Redis）          │
│  ├── /api/auth/logout   登出接口（写黑名单 + 删除 Redis key）        │
│  ├── /api/auth/kick     踢下线接口（管理员主动失效指定用户 token）     │
│  └── /api/users/*       用户接口（受保护，需携带有效 Token）          │
└──────────────────────────────────────────────────────────────────┘
                                 │ 读写 Redis
                                 ▼
┌──────────────────────────────────────────────────────────────────┐
│                           Redis                                  │
│  jwt:token:{username}     当前在线 Token（单设备登录）               │
│  jwt:blacklist:{jti}      已吊销的 Token（logout/踢下线写入）        │
└──────────────────────────────────────────────────────────────────┘
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
│   ├── GatewayJwtService.java              # JWT 验证服务（Gateway 专用）
│   ├── TokenBlacklistService.java          # Token 黑名单 + 踢下线校验服务
│   ├── RedisAccessor.java                  # Redis 访问器（隔离类，避免类加载异常）
│   ├── WhiteListProperties.java            # 白名单配置
│   └── JwtProperties.java                  # JWT 配置
└── src/main/resources/META-INF/spring/
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports

interview-gateway/
└── src/main/java/cn/itzixiao/interview/gateway/config/
    └── GatewayRedisAuthConfig.java         # Gateway 本地 Redis 直连配置（覆盖懒加载实现）

interview-provider/
└── src/main/java/cn/itzixiao/interview/provider/service/business/
    └── JwtService.java                     # Token 签发 + Redis 存储 + 踢下线逻辑
```

### 3.2 自动配置类

```java
/**
 * 网关鉴权自动配置类
 *
 * 条件加载：
 * 1. @ConditionalOnWebApplication(REACTIVE)：需要是响应式 Web 应用（Gateway 基于 WebFlux）
 * 2. @ConditionalOnProperty：配置 gateway.auth.enabled=true（默认启用）
 * 3. @AutoConfigureBefore：在 GatewayAutoConfiguration 之前加载
 *
 * 设计要点：
 * - TokenBlacklistService 注入 ApplicationContext 而非 StringRedisTemplate
 *   原因：本类先于 Redis 自动配置执行，直接注入会报 "No bean named 'stringRedisTemplate'"
 * - 懒加载方案：首次 Redis 操作时才从容器获取 StringRedisTemplate
 * - Gateway 模块内 GatewayRedisAuthConfig 通过 @Primary 覆盖本类提供的 Bean
 *   实现直连 StringRedisTemplate，性能更好
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "gateway.auth", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(GatewayAutoConfiguration.class)
@EnableConfigurationProperties({WhiteListProperties.class, JwtProperties.class})
public class GatewayAuthAutoConfiguration {

    /**
     * 网关 JWT 验证服务
     */
    @Bean
    public GatewayJwtService gatewayJwtService(JwtProperties jwtProperties) {
        log.info("【网关鉴权】初始化 JWT 验证服务, issuer: {}, expiration: {}s",
                jwtProperties.getIssuer(), jwtProperties.getExpiration());
        return new GatewayJwtService(jwtProperties.getSecretKey());
    }

    /**
     * Token 黑名单服务（懒加载 Redis）
     * 由 GatewayRedisAuthConfig 的 @Primary Bean 覆盖，优先级更高
     */
    @Bean
    @ConditionalOnMissingBean(TokenBlacklistService.class)
    public TokenBlacklistService tokenBlacklistService(ApplicationContext context) {
        log.info("【网关鉴权】初始化 Token 黑名单服务（懒加载 Redis，首次请求时建立连接）");
        return new TokenBlacklistService(context);
    }

    /**
     * 网关鉴权过滤器，order = -100，确保在所有业务过滤器之前执行
     */
    @Bean
    public AuthenticationFilter authenticationFilter(
            WhiteListProperties whiteListProperties,
            GatewayJwtService jwtService,
            @Autowired(required = false) TokenBlacklistService tokenBlacklistService) {
        return new AuthenticationFilter(whiteListProperties, jwtService, tokenBlacklistService);
    }
}
```

### 3.3 鉴权过滤器（响应式实现）

```java
/**
 * Gateway 全局鉴权过滤器
 *
 * 实现原理：
 * 1. 白名单路径直接放行
 * 2. 提取 Token 并一次解析 Claims（避免重复解析带来的性能开销）
 * 3. 检查 Token 过期
 * 4. Redis 可用时两重校验：jti 黑名单 + 在线 token 一致性
 * 5. 校验通过后掌蔽 Authorization 头，并注入 X-User-* 头给下游
 */
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final WhiteListProperties whiteListProperties;
    private final GatewayJwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;  // 可为 null（Redis 不可用时降级）
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 白名单路径，直接放行
        if (isWhitePath(path)) {
            return chain.filter(exchange);
        }

        // 2. 检查 Authorization header
        String authorizationHeader = request.getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authorizationHeader)) {
            return buildUnauthorizedResponse(exchange, "缺少认证令牌");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            return buildUnauthorizedResponse(exchange, "认证令牌格式错误，需要 Bearer 前缀");
        }

        // 3. 提取 token 并一次解析 Claims
        String token = authorizationHeader.substring(7);
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (Exception e) {
            return buildUnauthorizedResponse(exchange, "认证令牌无效或已过期");
        }

        // 4. 检查 Token 是否过期
        if (claims.getExpiration().before(new Date())) {
            return buildUnauthorizedResponse(exchange, "认证令牌已过期");
        }

        // 5. Redis 可用时：黑名单 + 在线 token 一致性双重校验
        if (tokenBlacklistService != null) {
            // 5.1 jti 黑名单校验（logout 主动登出 / kickOut 踢下线时写入）
            String jti = claims.getId();
            if (jti != null && tokenBlacklistService.isBlacklisted(jti)) {
                return buildUnauthorizedResponse(exchange, "认证令牌已被呀销，请重新登录");
            }
            // 5.2 在线 token 一致性校验（踢下线 / 单设备登录失效的核心防线）
            //    jwt:token:{username} 不存在（被踢下线删除）或与当前 token 不一致 -> 401
            String usernameForCheck = claims.getSubject();
            if (!tokenBlacklistService.isOnlineTokenValid(usernameForCheck, token)) {
                return buildUnauthorizedResponse(exchange, "认证令牌已失效，请重新登录");
            }
        }

        // 6. 解析用户信息并注入请求头，掌蔽 Authorization 头（防止下游二次鉴权）
        String username = claims.getSubject();
        Object roles = claims.get("roles");
        Object userIdObj = claims.get("userId");
        String userId = userIdObj != null ? userIdObj.toString() : username;

        ServerHttpRequest mutableReq = request.mutate()
                .header("X-User-Id", userId)
                .header("X-User-Name", username)
                .header("X-User-Roles", roles != null ? roles.toString() : "")
                .header("X-Auth-Source", "gateway")
                .headers(headers -> headers.remove("Authorization"))  // 掌蔽 Authorization 头
                .build();

        return chain.filter(exchange.mutate().request(mutableReq).build());
    }

    @Override
    public int getOrder() {
        return -100;  // 大厂规范：在路由转发和所有业务过滤器之前执行鉴权
    }
}
```

### 3.4 JWT 服务类（GatewayJwtService）

```java
/**
 * 网关 JWT 验证服务（interview-security 模块，Gateway 专用）
 *
 * 职责：网关层只做验证，不做签发
 *
 * 大厂规范：
 * 1. 一次 parseToken 获取全部 Claims，避免重复解析
 * 2. jti（JWT ID）用于黑名单标识，生成 Token 时必须写入
 * 3. 封装实用小方法，方便 filter 一次调用得到全部信息
 */
@Slf4j
public class GatewayJwtService {

    private final SecretKey secretKey;

    public GatewayJwtService(String secretKey) {
        // 将字符串密鑰转换为 SecretKey 对象
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析 JWT token，获取全部 Claims（一次解析，多次使用）
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证 token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /** 提取用户名（subject） */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /** 提取角色信息 */
    public Object getRolesFromToken(String token) {
        return parseToken(token).get("roles");
    }

    /**
     * 提取用户 ID（优先读取 userId 字段，没有则回退 subject）
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        return userId != null ? userId.toString() : claims.getSubject();
    }

    /**
     * 提取 JWT ID（jti），用于黑名单匹配
     */
    public String getJtiFromToken(String token) {
        return parseToken(token).getId();
    }

    /**
     * 获取 Token 剩余有效秒数（用于黑名单 TTL 设置）
     */
    public long getRemainTtlSeconds(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            long remainMs = expiration.getTime() - System.currentTimeMillis();
            return remainMs > 0 ? remainMs / 1000 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
```

### 3.5 Provider JwtService（Token 签发 + Redis 存储 + 踢下线）

```java
/**
 * Provider JWT 服务
 *
 * 职责：
 * 1. 签发 JWT Token（包含 jti、userId、roles）
 * 2. 登录时将 token 存入 Redis（jwt:token:{username}），实现单设备登录
 * 3. 踢下线操作：取在线 token -> 解析 jti 写黑名单 -> 删除 Redis key
 * 4. validateToken 支持黑名单校验降级
 *
 * Redis key 规则：
 *   jwt:token:{username}    -> 当前在线 token
 *   jwt:blacklist:{jti}    -> 已吐销的 jti（黑名单）
 */
@Slf4j
@Service
public class JwtService {

    public static final String TOKEN_PREFIX = "jwt:token:";
    public static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;
    /** 可选依赖：Redis 不可用时降级运行，登录/登出不操作 Redis */
    private final StringRedisTemplate redisTemplate;

    public JwtService(JwtConfig jwtConfig, SecretKey secretKey,
                      @Autowired(required = false) StringRedisTemplate redisTemplate) { ... }

    /**
     * 生成 Token：写入 jti + userId + roles，登录后将 token 存入 Redis
     */
    public String generateToken(String username, Long userId, Map<String, Object> roles) {
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .claims(claims)    // roles / userId
                .subject(username)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .id(jti)           // jti：踢下线时写入黑名单的唯一标识
                .signWith(secretKey)
                .compact();

        // Redis 可用时：将 token 存入 Redis，实现单设备登录（新 token 覆盖旧 token）
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(
                    TOKEN_PREFIX + username, token, expirationSeconds, TimeUnit.SECONDS);
        }
        return token;
    }

    /**
     * 踢下线：取在线 token -> 解析 jti 写黑名单 -> 删除 Redis key
     */
    public String kickOut(String username) {
        String token = redisTemplate.opsForValue().get(TOKEN_PREFIX + username);
        if (!StringUtils.hasText(token)) return "用户当前不在线，无需踢下线";

        // 解析 token 获取 jti 和剩余有效期并写入黑名单
        Claims claims = parseToken(token);
        String jti = claims.getId();
        long remainTtl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;
        if (StringUtils.hasText(jti) && remainTtl > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, username, remainTtl, TimeUnit.SECONDS);
        }

        // 删除在线 token key，断开登录状态
        redisTemplate.delete(TOKEN_PREFIX + username);
        return "踢下线成功";
    }

    /**
     * 验证 token：签名 + 过期检查 + jti 黑名单检查（Redis 可用时）
     */
    public boolean validateToken(String token) { ... }
}
```

### 3.6 RedisAccessor（类隔离，避免 NoClassDefFoundError）

```java
/**
 * Redis 访问适配器
 *
 * 【设计目的】将所有 StringRedisTemplate 操作封装在该类中，
 * 与 TokenBlacklistService 隔离在不同类文件中。
 *
 * 【JVM 类加载机制】
 * - TokenBlacklistService 主类字节码中不出现任何 Redis 类型引用
 * - JVM 内省 getDeclaredMethods 时不触发 Redis 类加载
 * - 彻底规避后端编排时不展开 NoClassDefFoundError：StringRedisTemplate
 *
 * 【若卌时运行时未有 Redis jar】ClassLoader 懒加载机制保证本类不被加载，
 * 不会安全地提前抛出 NoClassDefFoundError。
 */
class RedisAccessor {

    private final StringRedisTemplate redisTemplate;

    private RedisAccessor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 工厂方法：从 ApplicationContext 获取 StringRedisTemplate 并创建访问器
     * Redis jar 不存在或 Bean 未就绪时抛出异常，由调用方处理
     */
    static RedisAccessor create(ApplicationContext context) {
        StringRedisTemplate template = context.getBean("stringRedisTemplate", StringRedisTemplate.class);
        return new RedisAccessor(template);
    }

    Boolean hasKey(String key)    { return redisTemplate.hasKey(key); }
    String  get(String key)       { return redisTemplate.opsForValue().get(key); }
    void    set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }
    void    delete(String key)    { redisTemplate.delete(key); }
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

**装配条件链（三重条件全部满足才生效）：**

```java
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
// 条件1：当前是响应式 Web 应用（Gateway 基于 WebFlux），Servlet 应用不触发

@ConditionalOnProperty(prefix = "gateway.auth", name = "enabled",
        havingValue = "true", matchIfMissing = true)
// 条件2：gateway.auth.enabled=true（默认为 true，不配置也生效，可通过配置关闭）

@AutoConfigureBefore(GatewayAutoConfiguration.class)
// 条件3：在 GatewayAutoConfiguration 之前加载，确保鉴权过滤器先于路由过滤器注册
```

**Bean 覆盖机制：**

```java
// interview-security 中（兜底实现）：
@Bean
@ConditionalOnMissingBean(TokenBlacklistService.class)  // 容器中不存在时才创建
public TokenBlacklistService tokenBlacklistService(ApplicationContext context) {
    return new TokenBlacklistService(context);  // 懒加载 Redis
}

// interview-gateway 中（优先使用）：
@Bean
@Primary                                               // 同类型多 Bean 时优先注入
@ConditionalOnBean(StringRedisTemplate.class)          // Redis Bean 就绪后才创建
public TokenBlacklistService tokenBlacklistService(StringRedisTemplate redisTemplate, ...) {
    return new DirectRedisTokenBlacklistService(redisTemplate);  // 直接持有 RedisTemplate
}
```

> **为什么不用 `@ConditionalOnClass`？** 当前版本已确认 Gateway 始终有 Redis 依赖，classpath 条件没有意义。同时 `@ConditionalOnClass` 在某些 IDE 运行时的类加载顺序下会提前触发 Redis 类加载，反而引发问题。

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
┌──────────────────────────┐  ┌──────────────────────────────┐
│  interview-gateway      │  │      interview-provider       │
│                         │  │                                │
│  security.jwt.secret-key│  │  jwt.secret-key                │
│  = ${jwt.secret-key}    │  │  = ${jwt.secret-key}           │
│                         │  │                                │
│  Spring 绑定前缀不同，    │  │  Spring 绑定前缀不同，           │
│  但引用的全局变量相同      │  │  但引用的全局变量相同             │
│                         │  │                                │
│  GatewayJwtService       │  │  JwtService                   │
│  validateToken() 验证    │  │  generateToken() 签发          │
└───────────────────────┘  └─────────────────────────────────┘
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

本项目采用 **JWT + Redis 引状态方案**，Token 同时存在于客户端和服务端 Redis，支持主动吐销和踢下线：

```
┌──────────────────────────────────────────────────────────────────┐
│                        Token 生命周期                              │
│                                                                  │
│  1. 生成阶段（Provider）                                            │
│     JwtService.generateToken()                                    │
│     → 写入 jti（UUID，用于黑名单标识）                               │
│     → 写入 userId（数字 ID，透传给下游）                             │
│     → 写入 username（subject）                                     │
│     → 写入 roles（自定义 claims）                                   │
│     → 写入 iat（签发时间）、exp（过期时间）                           │
│     → 使用 SecretKey HMAC-SHA 签名                                │
│     → 同时将 token 存入 Redis（jwt:token:{username}）               │
│       实现单设备登录，新 token 覆盖旧 token                          │
│                                                                   │
│  2. 存储阶段（客户端 + Redis 双存）                                  │
│     ├── Web 端：localStorage / sessionStorage / Cookie            │
│     ├── App 端：本地安全存储（KeyChain / SharedPreferences）        │
│     └── Redis：jwt:token:{username} = token，TTL = 过期时间        │
│                                                                 │
│  3. 使用阶段（每次请求携带）                                         │
│     请求头：Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...         │
│                                                                 │
│  4. 验证阶段（Gateway 拦截）                                        │
│     AuthenticationFilter：                                        │
│     → 签名验证 + 过期检查（JJWT 内置）                              │
│     → jti 黑名单检查（Redis jwt:blacklist:{jti}）                  │
│     → 在线 token 一致性检查（Redis jwt:token:{username}）           │
│     → 通过后解析 claims，注入 X-User-* 头给下游                     │
│                                                                 │
│  5. 主动吊销阶段（logout / 踢下线）                                  │
│     → 解析 jti 写入 Redis 黑名单（TTL = 剩余有效期）                 │
│     → 删除 jwt:token:{username}，断开登录状态                       │
│     → 下次请求 Gateway 返回 401                                    │
│                                                                 │
│  6. 自然过期阶段                                                   │
│     → 客户端重新登录获取新 Token                                    │
│     → 旧 Token 自然失效（JJWT 过期报错）                            │
│     → Redis key 自动 TTL 到期清理                                  │
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
  "jti": "550e8400-e29b-41d4-a716-446655440000",
  // JWT ID，唯一标识（用于黑名单匹配）
  "sub": "admin",
  // 用户名（subject）
  "iss": "interview-provider",
  // 签发者（issuer）
  "iat": 1742860800,
  // 签发时间（issuedAt）
  "exp": 1742947200,
  // 过期时间（expiration）
  "userId": 1001,
  // 用户数字 ID（透传给下游，避免下游查表）
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
public Result<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
    String username = loginRequest.getUsername();
    Long userId = user.getId();

    // 构建角色信息（写入 Token Payload）
    Map<String, Object> roles = new HashMap<>();
    roles.put("role", "ADMIN");
    roles.put("permissions", new String[]{"read", "write", "delete"});

    // 2. 调用 JwtService.generateToken() 生成 Token（同时存入 Redis）
    String token = jwtService.generateToken(username, userId, roles);

    // 3. 返回 Token 给客户端（客户端自行存储）
    response.put("token", token);
    response.put("tokenType", "Bearer");
    response.put("expiresIn", jwtService.getJwtConfig().getExpiration());
    return Result.success(response);
}

// 2. JwtService.generateToken() 内部实现（含 jti + userId + Redis 存储）
public String generateToken(String username, Long userId, Map<String, Object> roles) {
    Date now = new Date();
    long expirationSeconds = jwtConfig.getExpiration();
    Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);
    String jti = UUID.randomUUID().toString();  // 生成唯一 jti

    Map<String, Object> claims = new HashMap<>();
    if (userId != null) claims.put("userId", userId);
    if (roles != null)  claims.put("roles", roles);

    String token = Jwts.builder()
            .claims(claims)
            .subject(username)       // 写入 sub = username
            .issuer(jwtConfig.getIssuer())
            .issuedAt(now)           // 写入 iat = 当前时间
            .expiration(expiryDate)  // 写入 exp = 当前时间 + 配置秒数
            .id(jti)                 // 写入 jti = UUID（用于黑名单）
            .signWith(secretKey)     // HMAC-SHA256 签名
            .compact();

    // Redis 可用时：存入在线 token，实现单设备登录（新 token 覆盖旧 token）
    if (redisTemplate != null) {
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + username, token, expirationSeconds, TimeUnit.SECONDS);
    }
    return token;
}
```

---

## 十、下游服务如何获取用户信息

### 10.1 网关透传机制

Gateway 验证 Token 通过后，会将用户信息**解析并注入到请求头**，下游服务无需重复验证 Token：

```java
// AuthenticationFilter.java - 验证通过后的处理（一次解析 Claims，多次使用）
String username = claims.getSubject();       // 从 Claims 获取用户名
Object roles = claims.get("roles");          // 从 Claims 获取角色
Object userIdObj = claims.get("userId");     // 从 Claims 获取用户 ID
String userId = userIdObj != null ? userIdObj.toString() : username;

// 将用户信息写入请求头，转发给下游服务
ServerHttpRequest mutableReq = request.mutate()
        .header("X-User-Id", userId)                                        // 用户ID
        .header("X-User-Name", username)                                    // 用户名
        .header("X-User-Roles", roles != null ? roles.toString() : "")     // 角色
        .header("X-Auth-Source", "gateway")                                 // 标记来源为网关
        .headers(headers -> headers.remove("Authorization"))  // 屏蔽 Authorization 头，下游服务不再鉴权
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
  │                       │                          │ 生成含 jti+userId 的 JWT│
  │                       │                          │ 存入 Redis           │
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
  │                       │ 3. 解析 Claims           │                      │
  │                       │ 4. 黑名单校验（Redis）      │                      │
  │                       │ 5. 在线 token 一致性（Redis）│                      │
  │                       │ 6. 屏蔽 Authorization 头   │                      │
  │                       │ 7. 注入 X-User-* 请求头    │                      │
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

### 11.3 Token 主动失效方案（已实现）

本项目已实现方案一（Redis 黑名单）和在线 token 一致性校验：

```
方案一：Redis 黑名单（已实现 - logout 主动登出）
  Provider logout 接口：
    1. 解析当前请求的 token 获取 jti和剩余有效期
    2. 将 jti 写入 Redis 黑名单（KEY: jwt:blacklist:{jti}，TTL = 剩余有效期）
    3. 删除 jwt:token:{username} key
  Gateway 验证时：
    → 读取 jti -> 检查 jwt:blacklist:{jti} -> 存在则 401

方案二：在线 Token 一致性校验（已实现 - 踢下线、单设备登录）
  Provider kickOut 接口 / 新登录覆盖：
    1. 将当前用户的在线 token 写入黑名单（kickOut 时）
    2. 删除 jwt:token:{username}（kickOut） 或 覆盖写入（新登录）
  Gateway 验证时：
    → 读取 jwt:token:{username} -> 不存在或不一致 -> 401

方案三：版本号机制（未实现——备选方案）
  用户表维护 token_version 字段
  Token Payload 中写入 token_version
  Gateway 验证时调用 Provider 接口比对版本号
  修改密码/强制下线 → 自增 token_version，旧 Token 失效

方案四：短有效期 + Refresh Token（未实现——适合下一期迭代）
  Access Token：15分钟有效
  Refresh Token：7天有效，存 Redis
  Access Token 过期 → 用 Refresh Token 换取新 Access Token
  主动吐销 → 删除 Redis 中的 Refresh Token
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
| `security.jwt.secret-key`     | Gateway  | 网关 JWT 验证密鑰                   |
| `jwt.secret-key`              | Provider | 业务服务 JWT 签发密鑰（与 Gateway 必须一致） |
| `gateway.auth.enabled`        | Gateway  | 是否启用鉴权（默认 true）               |
| `spring.data.redis.host`      | Gateway  | Redis 地址（黑名单和在线 token 存储）   |
| `spring.data.redis.port`      | Gateway  | Redis 端口（默认 6379）              |
| `spring.data.redis.database`  | Gateway  | Redis 数据库编号（与 Provider 保持一致）  |

### 12.2 最佳实践

1. **统一密钥管理**：使用 Nacos 配置中心 `common.yml` 统一下发 JWT 密钥，避免多处配置不一致
2. **白名单最小化**：只开放必要的接口，避免安全风险
3. **Token 有效期**：Access Token 建议 15 分钟~2 小时，配合 Refresh Token 使用
4. **日志记录**：记录鉴权失败的请求（路径 + IP），便于安全审计
5. **响应式编程**：Gateway 使用 WebFlux，鉴权过滤器严禁阻塞操作（IO、DB 查询等）
6. **Header 防伪造**：业务服务应检查 `X-Auth-Source: gateway`，防止绕过网关直接伪造 X-User-* 头
7. **敏感信息不入 Token**：密码、身份证等敏感字段不要写入 JWT Payload（Base64 可解码）
