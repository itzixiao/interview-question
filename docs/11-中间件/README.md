# 中间件知识点详解

## 📚 文档列表

#### 1. [01-MyBatis-Plus快速入门.md](./01-MyBatis-Plus%E5%BF%AB%E9%80%9F%E5%85%A5%E9%97%A8.md)

- **内容：** 通用 mapper、Service、分页插件
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐

#### 2. [02-MyBatis动态SQL与SQL注入防护.md](./02-MyBatis%E5%8A%A8%E6%80%81SQL%E4%B8%8ESQL%E6%B3%A8%E5%85%A5%E9%98%B2%E6%8A%A4.md)

- **内容：** #{}vs${}、SQL 注入原理、防护方案
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-MyBatis核心原理与面试题.md](./03-MyBatis%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E4%B8%8E%E9%9D%A2%E8%AF%95%E9%A2%98.md)

- **内容：** SQL 映射、插件机制、缓存原理
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 4. [04-Spring-Data-JPA详解.md](./04-Spring-Data-JPA%E8%AF%A6%E8%A7%A3.md) ⭐ NEW

- **内容：** 实体映射、Repository 接口、方法名派生查询、@Query、Specification 动态查询、分页排序、N+1 问题、乐观锁/悲观锁、实体生命周期、审计功能
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-spring-ai/`（KnowledgeDocument 实战案例）

#### 5. [05-Nacos核心知识点详解.md](./05-Nacos%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)

- **内容：** 服务发现、配置管理、服务注册与发现
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 6. [06-Sentinel限流熔断详解.md](./06-Sentinel%E9%99%90%E6%B5%81%E7%86%94%E6%96%AD%E8%AF%A6%E8%A7%A3.md)

- **内容：** 限流规则、熔断降级、系统自适应保护
- **面试题：** 12+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 7. [07-RPC核心原理与实战指南.md](./07-RPC%E6%A0%B8%E5%BF%83%E5%8E%9F%E7%90%86%E4%B8%8E%E5%AE%9E%E6%88%98%E6%8C%87%E5%8D%97.md)

- **内容：** RPC 架构、序列化协议、通信协议、负载均衡、容错机制
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 8. [08-RabbitMQ核心知识点详解.md](./08-RabbitMQ%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)

- **内容：** 五种工作模式、高级特性（延迟队列、死信队列、优先级）、可靠性保障、高频面试题
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-rabbitmq/`

#### 9. [09-Kafka高吞吐消息队列详解.md](./09-Kafka%E9%AB%98%E5%90%9E%E5%90%90%E6%B6%88%E6%81%AF%E9%98%9F%E5%88%97%E8%AF%A6%E8%A7%A3.md)

- **内容：** 架构设计、高吞吐原理、Offset 管理、Exactly Once 语义、ISR 副本同步
- **面试题：** 6+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-kafka/`

#### 10. [10-Netty高性能框架详解.md](./10-Netty%E9%AB%98%E6%80%A7%E8%83%BD%E6%A1%86%E6%9E%B6%E8%AF%A6%E8%A7%A3.md)

- **内容：** NIO实战、Reactor模式、零拷贝、ByteBuf、编解码器
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-provider/netty/`

#### 11. [11-HBase详解.md](./11-HBase%E8%AF%A6%E8%A7%A3.md)

- **内容：** 数据模型、架构原理、核心操作、过滤器、性能优化、Row Key 设计
- **面试题：** 7+ 道
- **重要程度：** ⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-provider/hbase/`

#### 12. [12-大数据技术栈详解.md](./12-%E5%A4%A7%E6%95%B0%E6%8D%AE%E6%8A%80%E6%9C%AF%E6%A0%88%E8%AF%A6%E8%A7%A3.md)

- **内容：** Hadoop、Spark、Flink、Hive 等大数据技术
- **面试题：** 6+ 道
- **重要程度：** ⭐⭐⭐

#### 13. [13-Flowable工作流引擎详解.md](./13-Flowable%E5%B7%A5%E4%BD%9C%E6%B5%81%E5%BC%95%E6%93%8E%E8%AF%A6%E8%A7%A3.md)

- **内容：** BPMN 2.0、流程定义、监听器、动态审批人、会签、历史数据
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-workflow/`

#### 14. [14-XXL-JOB分布式任务调度详解.md](./14-XXL-JOB%E5%88%86%E5%B8%83%E5%BC%8F%E4%BB%BB%E5%8A%A1%E8%B0%83%E5%BA%A6%E8%AF%A6%E8%A7%A3.md)

- **内容：** 分布式调度、弹性扩容、故障转移、任务分片、监控告警
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-xxljob/`

#### 15. [15-Jsoup网络爬虫实战.md](./15-Jsoup%E7%BD%91%E7%BB%9C%E7%88%AC%E8%99%AB%E5%AE%9E%E6%88%98.md) ⭐ NEW

- **内容：** HTML 解析、CSS 选择器、数据提取、网站资源批量下载
- **面试题：** 3+ 道
- **重要程度：** ⭐⭐⭐⭐
- **配套代码：** `interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/downloader/service/HttpDownloaderService.java`

---

## 📊 统计信息

- **文档数：** 15 个
- **面试题总数：** 165+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/rpc/`、`interview-rabbitmq/`、`interview-kafka/`、
  `interview-provider/hbase/`、`interview-provider/elasticsearch/`、`interview-provider/netty/`、`interview-workflow/`、`interview-xxljob/` 等目录（~15,000 行代码）

---

## 🎯 学习建议

### MyBatis + JPA（3-4 天）

1. **MyBatis 核心概念**
    - SqlSessionFactory、SqlSession
    - Mapper 接口绑定
    - 动态 SQL 标签

2. **JPA / Hibernate**
    - 实体映射与生命周期
    - Repository 方法名派生查询
    - N+1 问题与解决方案
    - 乐观锁（@Version）与悲观锁（@Lock）

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

- ✅ **[Spring框架](../05-Spring框架/README.md)** - IOC、AOP
- ✅ **[MySQL](../08-MySQL数据库/README.md)** - SQL 语法、索引优化

### 后续进阶

- 📚 **[SpringBoot与自动装配](../06-SpringBoot与自动装配/README.md)** - Starter 集成
- 📚 **[分布式系统](../14-分布式系统/README.md)** - 配置中心实战
- 📚 **[SpringCloud微服务](../07-SpringCloud微服务/README.md)** - Spring AI 智能体

### 知识点对应

| 中间件 | 应用场景 |
|--------|------|
| MyBatis / MyBatis-Plus | ORM 映射、动态 SQL、快速 CRUD |
| Spring Data JPA | ORM 标准规范、Repository 快速开发、审计 |
| Nacos | 微服务注册发现、配置管理 |
| Sentinel | 高并发限流、熔断降级 |
| **RPC** | **微服务远程调用、高性能通信** |
| **RabbitMQ / Kafka** | **异步消息、事件驱动、削峰填谷** |
| **Flowable** | **工作流审批、流程编排、任务管理** |
| **XXL-JOB** | **分布式定时任务调度、分片、故障转移** |

---

## 💡 高频面试题 Top 25

1. **MyBatis 的一级缓存和二级缓存区别？**
2. **MyBatis 的插件原理是什么？**
3. **#{}和${}的区别？如何防止 SQL 注入？**
4. **JPA 和 Hibernate 是什么关系？**
5. **什么是 N+1 问题？如何解决？**
6. **@Version 乐观锁原理是什么？**
7. **Nacos 如何实现服务注册与发现？**
8. **Nacos 配置中心是如何实现动态刷新的？**
9. **Sentinel 的限流算法有哪些？**
10. **Sentinel 如何实现熔断降级？**
11. **什么是 RPC？它的核心优势是什么？**
12. **RabbitMQ 有哪些组成部分？**
13. **如何保证消息不丢失？**
14. **延迟队列如何实现？**
15. **RabbitMQ 和 Kafka 的区别？**
16. **RPC 的完整工作流程是什么？**
17. **JDK动态代理在 RPC 中是如何应用的？**
18. **RPC 中的序列化协议有哪些？如何选择？**
19. **什么是 Flowable？Flowable 的核心表有哪些？**
20. **ExecutionListener 和 TaskListener 的区别？**
21. **如何实现动态审批人？**
22. **如何实现会签（多人审批）？**
23. **open-in-view 是什么？为什么推荐关闭？**
24. **Hibernate 的脏检查是如何实现的？**
25. **save() 与 saveAndFlush() 的区别？**

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
                RpcRequest request = new RpcRequest();
                request.setMethodName(method.getName());
                request.setParameterTypes(method.getParameterTypes());
                request.setParameters(args);
                Socket socket = new Socket(host, port);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                output.writeObject(request);
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
MyBatis 基础 → 动态 SQL → 缓存机制
   ↓
Spring Data JPA → 实体映射 → N+1 优化 → 乐观锁
   ↓
Nacos 服务发现 → Nacos 配置中心
   ↓
Sentinel 限流 → 熔断降级
   ↓
RPC 原理 → 动态代理 → 序列化
   ↓
RabbitMQ 基础 → 延迟队列、死信队列 → 可靠性保障
   ↓
Kafka → 高吞吐 → 分区 → ISR
   ↓
Flowable 工作流 → XXL-JOB 调度
   ↓
综合实战
```

---

## 📈 更新日志

### v2.7 - 2026-03-24

- ✅ 删除「12-AI工程化与LLM应用开发」，AI 内容已全量并入 `docs/07-SpringCloud微服务/05-Spring-AI智能体详解.md`
- ✅ 将「JPA详解」提前到第 04 号（ORM 参考实现，与 MyBatis 组合学习）
- ✅ HBase 移到 11 号，大数据移到 12 号，目录结构更合理
- ✅ Spring AI 主文档新增 LLM 对比、Agent 类型、安全风险、RAG 评估 4 道新面试题（问题 12-14）
- ✅ 文档总数 15 → 14，面试题总量 165+ → 160+（AI 内容已并入 Spring AI 专题，避免重复）

### v2.6 - 2026-03-24

- ✅ 新增「15-Spring-Data-JPA详解」文档（10+ 道面试题）
- ✅ 涵盖实体映射、Repository 方法派生、@Query、Specification、N+1 优化、乐观锁/悲观锁、审计功能
- ✅ 配套本项目 KnowledgeDocument 实战案例

### v2.5 - 2026-03-21

- ✅ 新增《16-MongoDB详解》文档（10+ 道面试题）
- ✅ 包含文档模型、索引机制、聚合管道、副本集、分片集群
- ✅ 配套代码：Spring Data MongoDB 完整示例

### v2.4 - 2026-03-17

- ✅ 新增《14-Flowable工作流引擎详解》文档（12+ 道面试题）
- ✅ 包含完整的请假/报销审批流程图
- ✅ 涵盖监听器、动态审批人、会签等高级特性
- ✅ 配套代码：`interview-workflow/` 模块

### v2.3 - 2026-03-15

- ✅ 新增《10-Elasticsearch 进阶详解》文档（8+ 道面试题）
- ✅ 新增《12-大数据技术栈详解》文档（6+ 道面试题）
- ✅ 更新文档统计信息（11 个文档，106+ 面试题）

### v2.2 - 2026-03-14

- ✅ 新增《RabbitMQ核心知识点详解》文档（20道面试题）
- ✅ 补充 RabbitMQ五种工作模式、高级特性、可靠性保障等知识点

### v2.1 - 2026-03-08

- ✅ 新增《RPC核心原理与实战指南》文档（8 道面试题）
- ✅ 补充 RPC 原理、序列化协议、负载均衡等知识点

### v2.0 - 2026-03-08

- ✅ 新增跨模块关联章节
- ✅ 补充 51+ 道高频面试题
- ✅ 添加学习建议和实战技巧

### v1.0 - 早期版本

- ✅ 基础中间件文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-24  
**问题反馈：** 欢迎提 Issue 或 PR
