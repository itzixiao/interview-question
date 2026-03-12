# interview-provider 模块集成指南

> Spring Boot Actuator + JWT 完整集成方案  
> **最后更新：** 2026-03-09 | **维护者：** itzixiao

---

## 📋 目录

1. [快速开始](#快速开始)
2. [Spring Boot Actuator 集成](#spring-boot-actuator-集成)
3. [JWT 功能集成](#jwt-功能集成)
4. [接口测试](#接口测试)
5. [生产环境最佳实践](#生产环境最佳实践)
6. [常见问题](#常见问题)

---

## 🚀 快速开始

### 1. 编译项目

```bash
cd E:\itzixiao\project\java\interview-question
mvn clean compile -pl interview-microservices-parent/interview-provider -am
```

### 2. 启动服务

```bash
# 方式 1：直接运行
cd interview-microservices-parent\interview-provider\target\classes
java cn.itzixiao.interview.provider.ProviderApplication

# 方式 2：Maven 运行
mvn spring-boot:run -pl interview-microservices-parent/interview-provider
```

**默认端口：** `8082`

---

## 🔧 Spring Boot Actuator 集成

### 1.1 pom.xml 依赖

```xml
<!-- Spring Boot Actuator - 监控和管理功能 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### 1.2 开发环境配置

**文件位置：** `src/main/resources/application-dev.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,env,beans,threaddump,httpexchanges,prometheus
  endpoint:
    health:
      show-details: always
      probes:
        enabled: true
  info:
    env:
      enabled: true
  metrics:
    export:
      simple:
        enabled: true
      prometheus:
        enabled: true
  server:
    tomcat:
      mbeanregistry:
        enabled: true

# 应用信息
info:
  app:
    name: interview-provider
    version: 1.0.0-SNAPSHOT
    description: 服务提供者 - OpenFeign 微服务调用演示
    environment: development
```

### 1.3 生产环境配置

**文件位置：** `src/main/resources/application-prod.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # 最小化原则
  endpoint:
    health:
      show-details: never  # 不显示详情
      probes:
        enabled: true
  info:
    env:
      enabled: false  # 禁用环境信息
  metrics:
    export:
      simple:
        enabled: false
      prometheus:
        enabled: true
  server:
    tomcat:
      mbeanregistry:
        enabled: true

info:
  app:
    name: interview-provider
    version: 1.0.0-SNAPSHOT
    description: 服务提供者 - OpenFeign 微服务调用演示
    environment: production
```

### 1.4 自定义健康检查器

**文件位置：** `src/main/java/cn/itzixiao/interview/provider/actuator/DatabaseHealthIndicator.java`

```java
package cn.itzixiao.interview.provider.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        boolean databaseIsUp = checkDatabaseConnection();
        
        if (databaseIsUp) {
            return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("status", "connected")
                    .withDetail("connectionPoolSize", 20)
                    .build();
        } else {
            return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("error", "Connection timeout")
                    .withException(new RuntimeException("数据库连接失败"))
                    .build();
        }
    }
    
    private boolean checkDatabaseConnection() {
        // TODO: 实际项目中应该执行真实数据库查询
        // try {
        //     jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        //     return true;
        // } catch (Exception e) {
        //     return false;
        // }
        return true; // 模拟正常
    }
}
```

### 1.5 自定义信息贡献者

**文件位置：** `src/main/java/cn/itzixiao/interview/provider/actuator/CustomInfoContributor.java`

```java
package cn.itzixiao.interview.provider.actuator;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class CustomInfoContributor implements InfoContributor {
    
    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("provider", "interview-provider")
               .withDetail("version", "1.0.0-SNAPSHOT")
               .withDetail("description", "服务提供者 - OpenFeign 微服务调用演示");
        
        // Java 版本信息
        builder.withDetail("java.version", System.getProperty("java.version"))
               .withDetail("java.vendor", System.getProperty("java.vendor"));
        
        // 操作系统信息
        builder.withDetail("os.name", System.getProperty("os.name"))
               .withDetail("os.arch", System.getProperty("os.arch"))
               .withDetail("os.version", System.getProperty("os.version"));
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        builder.withDetail("memory", new java.util.HashMap<String, Object>() {{
            put("maxMemory", runtime.maxMemory() / (1024 * 1024) + " MB");
            put("totalMemory", runtime.totalMemory() / (1024 * 1024) + " MB");
            put("freeMemory", runtime.freeMemory() / (1024 * 1024) + " MB");
        }});
    }
}
```

### 1.6 可用的 Actuator 端点

| 端点 | 路径 | 说明 | 开发环境 | 生产环境 |
|------|------|------|---------|---------|
| health | /actuator/health | 健康检查 | ✅ | ✅ |
| info | /actuator/info | 应用信息 | ✅ | ✅ |
| metrics | /actuator/metrics | 性能指标 | ✅ | ❌ |
| env | /actuator/env | 环境变量 | ✅ | ❌ |
| beans | /actuator/beans | Spring Bean 列表 | ✅ | ❌ |
| threaddump | /actuator/threaddump | 线程快照 | ✅ | ❌ |
| httpexchanges | /actuator/httpexchanges | HTTP 请求追踪 | ✅ | ❌ |
| prometheus | /actuator/prometheus | Prometheus 指标 | ✅ | ✅ |

### 1.7 端点访问示例

```bash
# 健康检查
curl http://localhost:8082/actuator/health

# 应用信息
curl http://localhost:8082/actuator/info

# JVM 内存使用
curl http://localhost:8082/actuator/metrics/jvm.memory.used

# Prometheus 指标
curl http://localhost:8082/actuator/prometheus
```

---

## 🔐 JWT 功能集成

### 2.1 pom.xml 依赖

```xml
<!-- JWT 依赖 -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

### 2.2 开发环境 JWT 配置

**文件位置：** `src/main/resources/application-dev.yml`

```yaml
jwt:
  secret-key: "itzixiao-interview-system-secret-key-2026-this-is-a-demo-for-learning"
  expiration: 86400  # token 过期时间（秒），默认 24 小时
  issuer: "interview-provider"
```

### 2.3 生产环境 JWT 配置

**文件位置：** `src/main/resources/application-prod.yml`

```yaml
jwt:
  secret-key: "${JWT_SECRET_KEY:interview-production-secure-secret-key-change-in-real-env}"
  expiration: 7200  # token 过期时间（秒），生产环境建议 2 小时
  issuer: "interview-provider"
```

**安全加固措施：**
- ✅ 从环境变量 `JWT_SECRET_KEY` 读取密钥
- ✅ 缩短 token 过期时间（2 小时）
- ✅ 提供默认值防止启动失败

### 2.4 JWT 配置类

**文件位置：** `src/main/java/cn/itzixiao/interview/provider/config/JwtConfig.java`

```java
package cn.itzixiao.interview.provider.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {
    
    @Value("${jwt.secret-key}")
    private String secretKey;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
```

### 2.5 JWT 工具服务

**文件位置：** `src/main/java/cn/itzixiao/interview/provider/service/JwtService.java`

```java
package cn.itzixiao.interview.provider.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    private final SecretKey secretKey;
    
    public JwtService(SecretKey secretKey) {
        this.secretKey = secretKey;
    }
    
    /**
     * 生成 JWT token
     */
    public String generateToken(String username, Map<String, Object> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);
        
        Map<String, Object> claims = new HashMap<>();
        if (roles != null) {
            claims.put("roles", roles);
        }
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * 生成简单 token（仅包含用户名）
     */
    public String generateSimpleToken(String username) {
        return generateToken(username, null);
    }
    
    /**
     * 解析 JWT token
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 验证 token 有效性
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 2.6 JWT 模拟接口

**文件位置：** `src/main/java/cn/itzixiao/interview/provider/controller/JwtController.java`

```java
package cn.itzixiao.interview.provider.controller;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.service.business.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class JwtController {

    private final JwtService jwtService;

    public JwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 模拟登录接口 - 获取 JWT token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> loginRequest) {
        String username = loginRequest.get("username");
        String password = loginRequest.get("password");

        // 模拟验证成功
        System.out.println("【登录请求】用户名：" + username + ", 密码：" + password);

        // 构建角色信息（自定义 claims）
        Map<String, Object> roles = new HashMap<>();
        roles.put("role", "ADMIN");
        roles.put("permissions", new String[]{"read", "write", "delete"});

        // 生成 token
        String token = jwtService.generateToken(username, roles);

        // 返回 token
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 86400);
        response.put("username", username);

        return Result.success(response);
    }

    /**
     * 验证 token 有效性
     */
    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyToken(@RequestParam String token) {
        boolean isValid = jwtService.validateToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);

        if (isValid) {
            try {
                io.jsonwebtoken.Claims claims = jwtService.parseToken(token);
                response.put("username", claims.getSubject());
                response.put("issuer", claims.getIssuer());
                response.put("issuedAt", claims.getIssuedAt());
                response.put("expiration", claims.getExpiration());
                response.put("roles", claims.get("roles"));
            } catch (Exception e) {
                response.put("error", e.getMessage());
            }
        } else {
            response.put("error", "Token 无效或已过期");
        }

        return Result.success(response);
    }

    /**
     * 从 token 中获取用户信息
     */
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Result.error("缺少 Authorization header 或格式不正确");
        }

        String token = authorizationHeader.substring(7);

        if (!jwtService.validateToken(token)) {
            return Result.error("Token 无效或已过期");
        }

        io.jsonwebtoken.Claims claims = jwtService.parseToken(token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", claims.getSubject());
        userInfo.put("issuer", claims.getIssuer());
        userInfo.put("roles", claims.get("roles"));
        userInfo.put("issuedAt", claims.getIssuedAt());
        userInfo.put("expiration", claims.getExpiration());

        return Result.success(userInfo);
    }
}
```

---

## 🧪 接口测试

### 3.1 Actuator 端点测试

```bash
# 1. 健康检查
curl http://localhost:8082/actuator/health

# 响应示例：
# {
#   "status": "UP",
#   "components": {
#     "db": {
#       "status": "UP",
#       "details": {
#         "database": "MySQL",
#         "connectionPoolSize": 20
#       }
#     },
#     "diskSpace": { ... }
#   }
# }

# 2. 查看应用信息
curl http://localhost:8082/actuator/info

# 3. 查看 Prometheus 指标
curl http://localhost:8082/actuator/prometheus
```

### 3.2 JWT 接口测试

#### 步骤 1：登录获取 token

```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6eyJyb2xlIjoiQURNSU4iLCJwZXJtaXNzaW9ucyI6WyJyZWFkIiwid3JpdGUiLCJkZWxldGUiXX0sInN1YiI6ImFkbWluIiwiaXNzIjoiaW50ZXJ2aWV3LXByb3ZpZGVyIiwiaWF0IjoxNjc4MzQ1Njc4LCJleHAiOjE2Nzg0MzIwNzh9.abc123...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "username": "admin"
  }
}
```

**保存返回的 token**，后续接口需要使用。

#### 步骤 2：验证 token 有效性

```bash
curl -X GET "http://localhost:8082/api/auth/verify?token=eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "valid": true,
    "username": "admin",
    "issuer": "interview-provider",
    "issuedAt": "2026-03-09T10:00:00",
    "expiration": "2026-03-10T10:00:00",
    "roles": {
      "role": "ADMIN",
      "permissions": ["read", "write", "delete"]
    }
  }
}
```

#### 步骤 3：使用 token 访问受保护接口

```bash
curl -X GET http://localhost:8082/api/auth/info \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

**响应示例：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "username": "admin",
    "issuer": "interview-provider",
    "roles": {
      "role": "ADMIN",
      "permissions": ["read", "write", "delete"]
    },
    "issuedAt": "2026-03-09T10:00:00",
    "expiration": "2026-03-10T10:00:00"
  }
}
```

---

## 🎯 生产环境最佳实践

### 4.1 Actuator 安全加固

#### ✅ 最小化端点暴露

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus  # 只暴露必要端点
```

#### ✅ 网络隔离

```yaml
management:
  server:
    address: 127.0.0.1  # 只允许本地访问
    port: 8083          # 独立管理端口
```

#### ✅ 安全认证

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

### 4.2 JWT 安全加固

#### ✅ 使用环境变量存储密钥

```bash
# Linux/Mac
export JWT_SECRET_KEY="your-super-secure-secret-key-here"

# Windows PowerShell
$env:JWT_SECRET_KEY="your-super-secure-secret-key-here"
```

```yaml
jwt:
  secret-key: ${JWT_SECRET_KEY}
```

#### ✅ 缩短 Token 过期时间

```yaml
jwt:
  expiration: 7200  # 生产环境建议 2 小时
```

#### ✅ 定期轮换密钥

```java
@Bean
public Map<String, SecretKey> jwtSecretKeys() {
    Map<String, SecretKey> keys = new HashMap<>();
    keys.put("key-2026-03", Keys.hmacShaKeyFor(currentKey.getBytes()));
    keys.put("key-2026-02", Keys.hmacShaKeyFor(oldKey.getBytes()));
    return keys;
}
```

---

## ❓ 常见问题

### Q1: JWT 密钥长度要求？

**答案：**

| 算法 | 最小密钥长度 | 推荐密钥长度 |
|------|-------------|-------------|
| HS256 | 256 位（32 字节） | 256 位 |
| HS384 | 384 位（48 字节） | 384 位 |
| HS512 | 512 位（64 字节） | 512 位 |

本示例使用的密钥长度为 64 字节，满足所有算法要求。

### Q2: 如何保证 Actuator 端点安全性？

**答案：**

1. **最小化暴露** - 只暴露 health、info、prometheus
2. **网络隔离** - 使用独立端口，限制内网访问
3. **安全认证** - 集成 Spring Security
4. **日志审计** - 记录所有管理端点访问

### Q3: Token 解析失败怎么办？

**答案：**

检查以下几点：
1. Token 是否完整（没有截断）
2. 密钥是否正确（开发/生产环境区分）
3. Token 是否过期（检查 expiration 字段）
4. 签名算法是否匹配（HS256）

### Q4: 如何在 K8s 中使用健康检查？

**答案：**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: interview-provider
spec:
  template:
    spec:
      containers:
      - name: app
        image: interview-provider:1.0.0
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 10
          periodSeconds: 5
```

---

## 📊 总结

✅ **已完成的功能：**

1. ✅ Spring Boot Actuator 完整集成
   - 健康检查（含自定义数据库检查）
   - 应用信息（含 JVM、OS、内存信息）
   - 性能指标（JVM、HTTP 请求）
   - Prometheus 监控集成

2. ✅ JWT 完整功能实现
   - Token 生成
   - Token 解析
   - Token 验证
   - 三个 RESTful API 接口

3. ✅ 生产环境安全加固
   - 端点最小化暴露
   - 环境变量注入密钥
   - 网络隔离方案
   - 安全认证配置

✅ **可访问的接口：**

| 类型 | 接口路径 | 说明 |
|------|---------|------|
| Actuator | `/actuator/health` | 健康检查 |
| Actuator | `/actuator/info` | 应用信息 |
| Actuator | `/actuator/prometheus` | Prometheus 指标 |
| JWT | `POST /api/auth/login` | 登录获取 token |
| JWT | `GET /api/auth/verify` | 验证 token |
| JWT | `GET /api/auth/info` | 获取用户信息 |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-09  
**问题反馈：** 欢迎提 Issue 或 PR
