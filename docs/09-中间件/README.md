# 中间件知识点详解

## 📚 文档列表

#### 1. [01-MyBatis-Plus快速入门.md](./01-MyBatis-Plus%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8.md)
- **内容：** 通用 mapper、Service、分页插件
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐

#### 2. [02-MyBatis动态 SQL 与 SQL 注入防护.md](./02-MyBatis%E5%8A%A8%E6%80%81SQL%E4%B8%8ESQL%E6%B3%A8%E5%85%A5%E9%98%B2%E6%8A%A4.md)
- **内容：** #{}vs${}、SQL 注入原理、防护方案
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-MyBatis核心原理与面试题.md](./03-MyBatis%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E4%B8%8E%E9%9D%A2%E8%AF%95%E9%A2%98.md)
- **内容：** SQL 映射、插件机制、缓存原理
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 4. [04-Nacos核心知识点详解.md](./04-Nacos核心知识点详解.md)
- **内容：** 服务发现、配置管理、服务注册与发现
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 5. [05-Sentinel 限流熔断详解.md](./05-Sentinel 限流熔断详解.md)
- **内容：** 限流规则、熔断降级、系统自适应保护
- **面试题：** 12+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 6. [06-RPC核心原理与实战指南.md](./06-RPC核心原理与实战指南.md)
- **内容：** RPC 架构、序列化协议、通信协议、负载均衡、容错机制
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 7. [07-RabbitMQ核心知识点详解.md](./07-RabbitMQ核心知识点详解.md)
- **内容：** 五种工作模式、高级特性（延迟队列、死信队列、优先级）、可靠性保障、高频面试题
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-rabbitmq/`

---

## 📊 统计信息

- **文档数：** 7 个
- **面试题总数：** 79+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/rpc/`、`interview-rabbitmq/` 等目录（~4,500 行代码）

---

## 🎯 学习建议

### MyBatis（2-3 天）
1. **核心概念**
   - SqlSessionFactory、SqlSession
   - Mapper 接口绑定
   - 动态 SQL 标签

2. **缓存机制**
   - 一级缓存（SqlSession 级别）
   - 二级缓存（Namespace 级别）
   - 缓存失效场景

3. **插件开发**
   - Interceptor 接口
   - 拦截 Executor、StatementHandler

### Nacos（2-3 天）
1. **服务发现**
   - 服务注册与注销
   - 心跳机制
   - 健康检查

2. **配置中心**
   - 配置动态刷新
   - 多环境隔离
   - 配置版本管理

### Sentinel（2 天）
1. **限流规则**
   - QPS 模式、线程模式
   - 热点参数限流

2. **熔断降级**
   - 异常比例、慢调用比例
   - 半开状态恢复

### RPC（2-3 天）
1. **核心原理**
   - RPC 架构
   - 动态代理机制
   - 序列化/反序列化

2. **通信协议**
   - HTTP vs TCP
   - 自定义协议设计
   - 网络通信模型

3. **服务治理**
   - 服务注册与发现
   - 负载均衡策略
   - 容错机制

### RabbitMQ（3-4 天）
1. **基础概念**
   - AMQP 协议
   - 核心组件（Producer、Consumer、Exchange、Queue）
   - 五种工作模式

2. **高级特性**
   - 延迟队列（TTL+DLX）
   - 死信队列
   - 优先级队列
   - 消息 TTL

3. **可靠性保障**
   - Publisher Confirm
   - 消费者手动 ACK
   - 消息持久化
   - 幂等性处理

4. **实战应用**
   - 订单超时取消
   - 异步任务处理
   - 日志收集
   - 数据同步

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Spring框架](../04-Spring框架/README.md)** - IOC、AOP
- ✅ **[MySQL](../07-MySQL 数据库/README.md)** - SQL 语法、索引优化

### 后续进阶
- 📚 **[SpringBoot](../05-SpringBoot 与自动装配/README.md)** - Starter 集成
- 📚 **[分布式系统](../12-分布式系统/README.md)** - 配置中心实战

### 知识点对应
| 中间件 | 应用场景 |
|--------|---------||
| MyBatis | ORM 映射、动态 SQL |
| Nacos | 微服务注册发现、配置管理 |
| Sentinel | 高并发限流、熔断降级 |
| MyBatis-Plus | 快速开发、通用 CRUD |
| **RPC** | **微服务远程调用、高性能通信** |
| **RabbitMQ** | **异步解耦、消息延迟处理、削峰填谷** |

---

## 💡 高频面试题 Top 15

1. **MyBatis 的一级缓存和二级缓存区别？**
2. **MyBatis 的插件原理是什么？**
3. **#{}和${}的区别？如何防止 SQL 注入？**
4. **Nacos 如何实现服务注册与发现？**
5. **Nacos 配置中心是如何实现动态刷新的？**
6. **Sentinel 的限流算法有哪些？**
7. **Sentinel 如何实现熔断降级？**
8. **MyBatis-Plus 相比 MyBatis 有什么优势？**
9. **MyBatis 的动态 SQL 有哪些常用标签？**
10. **Nacos 和 Eureka 的区别？**
11. **Sentinel 和 Hystrix 的区别？**
12. **MyBatis 中 ResultMap 的作用？**
13. **MyBatis 的分页插件原理？**
14. **Nacos 支持哪些配置格式？**
15. **Sentinel 的规则如何持久化？**
16. **什么是 RPC？它的核心优势是什么？**
17. **RPC 的完整工作流程是什么？**
18. **RPC 框架需要解决哪些核心问题？**
19. **JDK动态代理在 RPC 中是如何应用的？**
20. **RPC 中的序列化协议有哪些？如何选择？**
21. **RabbitMQ 是什么？为什么要用它？**
22. **RabbitMQ 有哪些组成部分？**
23. **说说 RabbitMQ 的五种工作模式？**
24. **如何保证消息不丢失？**
25. **如何保证消息的顺序性？**
26. **如何保证消息不重复消费？**
27. **延迟队列如何实现？**
28. **死信队列有什么用？**
29. **RabbitMQ 和 Kafka 的区别？**

---

## 🛠️ 实战技巧

### MyBatis 动态 SQL 示例
```xml
<select id="findUsers" resultType="User">
  SELECT * FROM users
  <where>
    <if test="name != null">
      AND name LIKE #{name}
    </if>
    <if test="age != null">
      AND age > #{age}
    </if>
  </where>
</select>
```

### Sentinel 限流配置
```java
@SentinelResource(value = "getUser", 
    blockHandler = "handleBlock",
    fallback = "handleFallback")
public User getUser(Long id) {
    return userService.getById(id);
}

public User handleBlock(Long id, BlockException ex) {
    return new User("默认用户");
}
```

### RPC 动态代理示例
```java
public class RpcProxy {
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(Class<T> interfaceClass, 
                                     String host, int port) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class<?>[]{interfaceClass},
            (proxy, method, args) -> {
                // 1. 封装 RPC 请求
                RpcRequest request = new RpcRequest();
                request.setMethodName(method.getName());
                request.setParameterTypes(method.getParameterTypes());
                request.setParameters(args);
                
                // 2. 发送请求到服务器
                Socket socket = new Socket(host, port);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                output.writeObject(request);
                
                // 3. 接收服务器响应
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                RpcResponse response = (RpcResponse) input.readObject();
                
                socket.close();
                return response.getResult();
            }
        );
    }
}
```

### RabbitMQ 延迟队列配置示例
```java
@Bean
public Queue orderDelayQueue() {
    return QueueBuilder.durable("queue.order.delay")
            .withArgument("x-dead-letter-exchange", "exchange.order.dlx")
            .withArgument("x-dead-letter-routing-key", "order.timeout")
            .withArgument("x-message-ttl", 30 * 60 * 1000) // 30 分钟
            .build();
}

// 发送延迟消息
public void sendOrderTimeoutMessage(Order order) {
    rabbitTemplate.convertAndSend("exchange.order.delay", 
        "order.delay", order,
        msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(30 * 60 * 1000));
            return msg;
        });
}
```

---

## 📖 推荐学习顺序

```
MyBatis 基础
   ↓
动态 SQL
   ↓
缓存机制
   ↓
插件开发
   ↓
Nacos 服务发现
   ↓
Nacos 配置中心
   ↓
Sentinel 限流
   ↓
RPC 原理
   ↓
RabbitMQ 基础
   ↓
延迟队列、死信队列
   ↓
可靠性保障
   ↓
综合实战
```

---

## 📈 更新日志

### v2.2 - 2026-03-14
- ✅ 新增《RabbitMQ核心知识点详解》文档（20道面试题）
- ✅ 更新文档统计信息为7个文档、79+面试题
- ✅ 补充 RabbitMQ五种工作模式、高级特性、可靠性保障等知识点
- ✅ 添加 RabbitMQ延迟队列配置示例代码
- ✅ 更新推荐学习顺序，增加 RabbitMQ 学习路径

### v2.1 - 2026-03-08
- ✅ 新增《RPC核心原理与实战指南》文档（8 道面试题）
- ✅ 更新文档统计信息
- ✅ 补充 RPC 原理、序列化协议、负载均衡等知识点
- ✅ 添加 RPC 动态代理示例代码

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 51+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础中间件文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-14  
**问题反馈：** 欢迎提 Issue 或 PR
