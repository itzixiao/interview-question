# Spring Cloud 微服务知识点详解

## 📚 文档列表

### 1. [01-微服务架构基础.md](./01-微服务架构基础.md)

- **内容：** 微服务拆分原则、CAP/BASE 理论、中间件选型
- **面试题：** 5+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 中级

### 2. [02-Spring-Cloud-Gateway详解.md](./02-Spring-Cloud-Gateway详解.md)

- **内容：** 路由配置、过滤器、断言工厂、限流熔断
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 中级 ~ 高级

### 3. [03-Gateway鉴权与路由实战.md](./03-Gateway鉴权与路由实战.md)

- **内容：** JWT 鉴权实现、路由配置、StripPrefix 原理、模块化设计
- **面试题：** 实战案例 + 源码分析
- **重要程度：** ⭐⭐⭐⭐⭐
- **难度：** 中级 ~ 高级

### 4. [04-Spring-Cloud-OpenFeign详解.md](./04-Spring-Cloud-OpenFeign详解.md)

- **内容：** 声明式 HTTP 客户端、负载均衡、超时重试、日志优化
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐
- **难度：** 中级

---

## 📊 统计信息

- **文档数：** 4 个
- **面试题总数：** 30+ 道
- **代码示例：** 配套 Java 代码在 `interview-microservices-parent/` 模块中

---

## 📖 推荐学习顺序

```
微服务架构基础（CAP/BASE 理论）
          ↓
Spring Cloud Gateway（网关核心）
          ↓
Gateway 鉴权与路由实战（实战应用）
          ↓
OpenFeign（服务调用）
          ↓
Sentinel（限流熔断）
          ↓
Sleuth + Zipkin（链路追踪）
```

---

## 🎯 学习建议

### 微服务架构基础（重点）

1. **CAP 定理**：理解一致性、可用性、分区容错性的权衡
2. **BASE 理论**：基本可用、软状态、最终一致性
3. **拆分原则**：单一职责、业务领域、数据自治
4. **中间件选型**：根据 CAP 需求选择合适的中间件

### Spring Cloud Gateway

1. **核心概念**：Route、Predicate、Filter
2. **路由配置**：Path 匹配、StripPrefix、负载均衡
3. **过滤器开发**：GlobalFilter、GatewayFilter
4. **高级特性**：限流、熔断、鉴权

### OpenFeign

1. **基础使用**：@FeignClient 注解、接口定义
2. **负载均衡**：LoadBalancer 集成
3. **性能优化**：连接池、超时配置、日志级别
4. **最佳实践**：参数传递、异常处理、降级策略

---

## 🔗 相关链接

- [项目总览](../../README.md)
- [Gateway 示例代码](../../../interview-gateway/)
- [Provider 示例代码](../../../interview-microservices-parent/interview-provider/)
- [Service 示例代码](../../../interview-microservices-parent/interview-service/)

---

## 📈 更新日志

### v3.0 - 2026-03-20

- ✅ 新增 01-微服务架构基础.md（CAP/BASE 理论、拆分原则）
- ✅ 新增 03-Gateway鉴权与路由实战.md（JWT 鉴权、路由配置）
- ✅ 重新整理文档结构，按学习顺序编号

### v2.0 - 2026-03-08

- ✅ 新增 Spring Cloud Gateway 详解文档
- ✅ 新增 OpenFeign 详解文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-20  
**问题反馈：** 欢迎提 Issue 或 PR
