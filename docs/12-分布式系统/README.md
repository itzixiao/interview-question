# 分布式系统知识点详解

## 📚 文档列表

#### 1. [01-分布式幂等 - 防重放 - 加密详解.md](./01-%E5%88%86%E5%B8%83%E5%BC%8F%E5%B9%82%E7%AD%89-%E9%98%B2%E9%87%8D%E6%94%BE-%E5%8A%A0%E5%AF%86%E8%AF%A6%E8%A7%A3.md)
- **内容：** 分布式幂等性设计、Token 防重放、RSA 加密应用
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 2. [02-系统设计与架构详解.md](./02-%E7%B3%BB%E7%BB%9F%E8%AE%BE%E8%AE%A1%E4%B8%8E%E6%9E%B6%E6%9E%84%E8%AF%A6%E8%A7%A3.md)
- **内容：** DDD 领域驱动设计、微服务拆分、数据库拆分、CQRS、事件溯源
- **子主题：**
  - DDD：实体、值对象、聚合根、领域事件
  - 微服务：限界上下文、拆分原则
  - 数据库：读写分离、分库分表
  - 架构模式：CQRS、Event Sourcing
- **代码示例：** 2000+ 行完整代码在 `interview-microservices-parent/interview-system-design/`
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-安全与认证授权详解.md](./03-%E5%AE%89%E5%85%A8%E4%B8%8E%E8%AE%A4%E8%AF%81%E6%8E%88%E6%9D%83%E8%AF%A6%E8%A7%A3.md)
- **内容：** OAuth2.0、Spring Security、SSO、RBAC、API 网关鉴权、常见攻击防护
- **子主题**：
  - OAuth2.0：四种授权模式、JWT Token 机制、刷新 Token 策略
  - Spring Security：认证授权实战
  - SSO：单点登录原理与实现
  - RBAC：权限模型设计与实现
  - API 网关：统一鉴权、限流防刷
  - 安全防护：XSS、CSRF、SQL 注入防护
- **代码示例：** 完整 Java 代码在 `interview-microservices-parent/interview-security/`（~1000 行代码）
- **面试题：** 28+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 4. [04-分布式事务深入详解.md](./04-%E5%88%86%E5%B8%83%E5%BC%8F%E4%BA%8B%E5%8A%A1%E6%B7%B1%E5%85%A5%E8%AF%A6%E8%A7%A3.md)
- **内容：** CAP/BASE 理论、Seata 框架、本地消息表、RocketMQ 事务消息、最大努力通知
- **子主题**：
  - CAP/BASE：CP vs AP 选择、最终一致性实践
  - Seata：AT/TCC/Saga三种模式原理与实战
  - 本地消息表：方案设计、代码实现
  - RocketMQ：事务消息原理、异常处理
  - 最大努力通知：重试策略、人工介入
- **代码示例：** `interview-microservices-parent/interview-transaction-demo/`
- **面试题：** 28+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 4 个
- **面试题总数：** 93+ 道
- **代码示例：** 
  - 分布式安全：配套 Java 代码在 `interview-service/security/` 目录（~1,000 行代码）
  - 系统架构：配套 Java 代码在 `interview-microservices-parent/interview-system-design/`（~2,000+ 行代码）
  - 认证授权：配套 Java 代码在 `interview-microservices-parent/interview-security/`（~1,000 行代码）
  - 分布式事务：配套 Java 代码在 `interview-microservices-parent/interview-transaction-demo/`（~500 行代码）

---

## 🎯 学习建议

### 幂等性设计（2 天）
1. **幂等性概念**
   - 同一操作多次执行结果一致
   - GET、PUT、DELETE 天然幂等

2. **实现方案**
   - 数据库唯一索引
   - Token 机制
   - 分布式锁
   - 状态机

### 系统设计与架构（5 天）
1. **DDD 领域驱动设计（2 天）**
   - 实体、值对象、聚合根
   - 领域事件
   - 限界上下文

2. **微服务拆分（1 天）**
   - 单一职责原则
   - 按业务领域拆分
   - 服务边界划分

3. **数据库拆分（1 天）**
   - 读写分离
   - 分库分表策略
   - 数据迁移方案

4. **架构模式（1 天）**
   - CQRS 命令查询分离
   - Event Sourcing 事件溯源

### 防重放攻击（1 天）
1. **Nonce + Timestamp**
   - 一次性 Token
   - 时间窗口校验

2. **签名机制**
   - 参数排序 + 密钥签名
   - 防止参数篡改

### 数据安全（2 天）
1. **加密算法**
   - RSA 非对称加密
   - AES 对称加密

2. **传输安全**
   - HTTPS 协议
   - 敏感数据脱敏

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Java并发编程](../02-Java并发编程/README.md)** - 分布式锁
- ✅ **[Redis](../08-Redis 缓存/README.md)** - 分布式锁实现

### 后续进阶
- 📚 **[Gateway](../06-SpringCloud 微服务/README.md)** - 统一鉴权、限流
- 📚 **[Sentinel](../09-中间件/README.md)** - 熔断降级

### 知识点对应
| 分布式系统 | 应用场景 |
|-----------|---------|
| 幂等性 | 支付回调、订单创建 |
| 防重放 | 接口安全、秒杀 |
| 分布式锁 | 库存扣减、抢券 |
| 加密传输 | 登录认证、支付 |
| DDD | 复杂业务系统建模 |
| CQRS | 高并发读写分离 |
| 事件溯源 | 金融审计、数据追溯 |
| OAuth2.0 | 第三方登录、开放平台 |
| JWT | 单点登录、移动认证 |
| RBAC | 企业权限管理 |
| 安全防护 | XSS、CSRF、SQL 注入防护 |

---

## 💡 高频面试题 Top 15

1. **什么是幂等性？为什么需要保证幂等？**
2. **如何设计一个幂等接口？**
3. **Token 机制如何防止重复提交？**
4. **分布式锁的实现方案有哪些？**
5. **Redis 和 Zookeeper 实现分布式锁的区别？**
6. **如何防止接口被恶意重放？**
7. **RSA 加密的原理？**
8. **对称加密和非对称加密的区别？**
9. **如何保证分布式事务的最终一致性？**
10. **CAP 理论和 BASE 理论？**
11. **分布式 ID 生成方案？**
12. **分布式 Session 如何实现？**
13. **分布式系统的限流策略？**
14. **如何设计一个高可用的分布式系统？**
15. **分布式链路追踪的原理？**

---

## 🛠️ 实战技巧

### Token 防重放实现
```java
// 1. 请求前获取 Token
String token = UUID.randomUUID().toString();
redis.setex("token:" + token, 300, "1");

// 2. 服务端校验并删除
public void checkToken(String token) {
    Boolean deleted = redis.delete("token:" + token);
    if (!deleted) {
        throw new BusinessException("重复请求或 Token 已过期");
    }
}
```

### 分布式锁实现幂等
```java
RLock lock = redisson.getLock("order:" + orderId);
if (lock.tryLock(0, 30, TimeUnit.SECONDS)) {
    try {
        // 检查是否已处理
        if (!isProcessed(orderId)) {
            processOrder(orderId);
        }
    } finally {
        lock.unlock();
    }
}
```

---

## 📖 推荐学习顺序

### 分布式安全方向
```
幂等性概念
   ↓
Token 机制
   ↓
分布式锁
   ↓
防重放攻击
   ↓
加密算法
   ↓
HTTPS 传输
   ↓
综合实战
```

### 系统架构设计方向
```
DDD 基础概念
   ↓
实体、值对象、聚合根
   ↓
领域事件
   ↓
微服务拆分
   ↓
数据库拆分
   ↓
CQRS 架构
   ↓
事件溯源
   ↓
综合实战
```

---

## 📈 更新日志

### v3.0 - 2026-03-15
- ✅ 新增《系统设计与架构详解》文档
- ✅ 新增 DDD 领域驱动设计完整示例（实体、值对象、聚合根、领域事件）
- ✅ 新增微服务拆分原则和限界上下文示例
- ✅ 新增数据库拆分策略（读写分离、分库分表）
- ✅ 新增 CQRS 架构模式示例（命令端、查询端）
- ✅ 新增事件溯源（Event Sourcing）完整实现
- ✅ 补充 20+ 道系统架构相关面试题
- ✅ 代码示例：2000+ 行完整代码

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 20+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础分布式系统文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
