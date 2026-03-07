# Spring Cloud 微服务知识点详解

## 📚 文档列表

#### 1. [01-Spring-Cloud-Gateway详解.md](./01-Spring-Cloud-Gateway%E8%AF%A6%E8%A7%A3.md)
- **内容：** 路由配置、过滤器、断言工厂、限流熔断、鉴权实现
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 中级 ~ 高级

#### 2. [02-Spring-Cloud-OpenFeign详解.md](./02-Spring-Cloud-OpenFeign%E8%AF%A6%E8%A7%A3.md)
- **内容：** 声明式 HTTP 客户端、负载均衡、超时重试、日志优化
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐
- **难度：** 中级

---

## 📊 统计信息

- **文档数：** 2 个
- **面试题总数：** 25+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/` 模块中

---

## 🎯 学习建议

### Spring Cloud Gateway（重点）
1. **核心概念**
   - Route（路由）、Predicate（断言）、Filter（过滤器）
   - Gateway 的工作原理和请求处理流程

2. **路由配置方式**
   - 基于 URI 的路由
   - 基于 Path 的路由
   - 基于方法的动态路由

3. **过滤器开发**
   - GlobalFilter 全局过滤器
   - GatewayFilter 局部过滤器
   - 过滤器链的执行顺序

4. **高级特性**
   - 限流（Redis + Lua）
   - 熔断降级（Resilience4j/Sentinel）
   - 统一鉴权与日志记录

### Spring Cloud OpenFeign
1. **基础使用**
   - @FeignClient 注解
   - 接口定义与调用

2. **负载均衡**
   - Ribbon 集成
   - LoadBalancer 替换

3. **性能优化**
   - 连接池配置（HttpClient/OkHttp）
   - 超时时间设置
   - 日志级别调整

4. **最佳实践**
   - 参数传递规范
   - 异常处理机制
   - 降级策略设计

---

## 🔗 相关链接

- [项目总览](../../README.md)
- [代码索引](../../interview-service/src/main/java/cn/itzixiao/interview/代码索引.md)
- [Gateway 示例代码](../../interview-gateway/)
- [Service 示例代码](../../interview-service/)

---

## 📖 推荐学习顺序

```
Spring Cloud 基础
   ↓
服务注册与发现 (Nacos/Eureka)
   ↓
Spring Cloud Gateway（网关）
   ↓
OpenFeign（服务调用）
   ↓
Sentinel（限流熔断）
   ↓
Sleuth + Zipkin（链路追踪）
```

---

## 💡 高频面试题 Top 10

### Gateway 相关
1. **Spring Cloud Gateway 的工作原理是什么？**
2. **Gateway 有哪些常用的 Predicate？**
3. **如何实现自定义 GlobalFilter？**
4. **Gateway 如何实现限流？有哪些方案？**
5. **Gateway 和 Zuul 的区别是什么？**

### OpenFeign 相关
6. **OpenFeign 的工作原理是什么？**
7. **OpenFeign 如何实现负载均衡？**
8. **OpenFeign 的超时如何配置？**
9. **OpenFeign 如何进行日志优化？**
10. **OpenFeign 的性能优化有哪些手段？**

---

## 🛠️ 实战技巧

### Gateway 鉴权白名单配置
```yaml
custom:
  white-list:
    paths:
      - /api/user/login
      - /api/doc/**
```

### OpenFeign 连接池配置
```yaml
feign:
  httpclient:
    enabled: true
    max-connections: 200
    max-connections-per-route: 50
```

### Gateway 限流配置
```java
@Bean
public KeyResolver ipKeyResolver() {
    return exchange -> Mono.just(
        exchange.getRequest().getRemoteAddress().getHostName()
    );
}
```

---

## 📈 更新日志

### v2.0 - 2026-03-08
- ✅ 新增 Spring Cloud Gateway 详解文档
- ✅ 新增 OpenFeign 详解文档
- ✅ 补充 25+ 道高频面试题
- ✅ 添加实战技巧和配置示例

### v1.0 - 早期版本
- ✅ 基础 Spring Cloud 文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
