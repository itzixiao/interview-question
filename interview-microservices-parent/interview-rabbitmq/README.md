# RabbitMQ 实战示例与高频面试题详解

本模块包含 RabbitMQ 的完整实战示例和面试常见题目代码，涵盖从基础到高级的所有知识点。

## 📋 目录结构

```
interview-rabbitmq/
├── http/                      # HTTP Client 测试文件
│   ├── RabbitMQ 基础测试.http   # 完整的 API 测试脚本
│   └── http-client.env.json   # 环境变量配置
├── src/main/java/cn/itzixiao/interview/rabbitmq/
│   ├── config/              # 配置类
│   │   └── RabbitMQConfig.java      # RabbitMQ 配置（队列、交换机、绑定等）
│   ├── model/               # 消息模型
│   │   ├── OrderMessage.java        # 订单消息
│   │   └── UserMessage.java         # 用户消息
│   ├── producer/            # 生产者
│   │   ├── MessageProducer.java     # 消息生产者
│   │   └── MessageConfirmHandler.java # 消息确认回调
│   ├── consumer/            # 消费者
│   │   ├── SimpleConsumer.java      # 基础消费者
│   │   └── AdvancedConsumer.java    # 高级消费者
│   ├── advanced/            # 高级特性
│   │   └── ReliabilityDemo.java     # 可靠性保障示例
│   ├── interview/           # 面试题
│   │   └── InterviewQuestionDemo.java # 面试题示例代码
│   ├── controller/          # 测试接口
│   │   └── RabbitMQController.java  # REST API
│   └── RabbitMQApplication.java     # 启动类
└── resources/
    └── application.yml              # 配置文件
```

## 🚀 快速开始

### 1. 启动 RabbitMQ

使用 Docker 启动（推荐）：

```bash
docker run -d \
  --name rabbitmq \
  -p 5672:5672 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=admin123 \
  -v $(pwd)/data:/var/lib/rabbitmq/mnesia \
  -v $(pwd)/conf:/etc/rabbitmq \
  -v $(pwd)/log:/var/log/rabbitmq \
  rabbitmq:management
```

访问管理界面：http://localhost:15672 (用户名：admin, 密码：admin123)

### 2. 修改配置

编辑 `application.yml` 中的 RabbitMQ 连接信息：

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin123
```

### 3. 启动应用

```bash
cd interview-microservices-parent/interview-rabbitmq
mvn spring-boot:run
```

### 4. 使用 HTTP Client 测试（推荐）

**方式一：IDEA 直接运行**

打开 `http/RabbitMQ 基础测试.http` 文件，点击接口旁边的 ▶️ 运行按钮。

**方式二：VS Code + REST Client 插件**

安装 REST Client 插件后，打开 `.http` 文件，点击 `Send Request` 链接。

**方式三：curl 命令**

参考下文各功能模块的 curl 示例。

## 📚 功能模块

### 1. 基础消息队列

#### 简单队列（Simple Queue）
- 一对一模式，一个生产者，一个消费者
- 演示最基本的消息发送和接收

**测试接口：**
```bash
POST http://localhost:8080/api/rabbitmq/send/simple?routingKey=routing.key.1
Content-Type: application/json

"这是一条简单消息"
```

#### 工作队列（Work Queue）
- 多个消费者负载均衡
- 演示任务分发模式

### 2. 交换机模式

#### Direct Exchange（直连交换机）
- routingKey 完全匹配
- 示例：`routing.key.1` → `queue.simple`

**测试接口：**
```bash
POST http://localhost:8080/api/rabbitmq/send/routing?routingKey=routing.key.1
Content-Type: application/json

{
  "userId": 1,
  "username": "张三",
  "email": "zhangsan@example.com",
  "action": "REGISTER"
}
```

#### Fanout Exchange（扇形交换机）
- 广播模式，忽略 routingKey
- 所有绑定的队列都会收到消息

**测试接口：**
```bash
POST http://localhost:8080/api/rabbitmq/send/fanout
Content-Type: application/json

{
  "userId": 1,
  "username": "李四",
  "email": "lisi@example.com",
  "action": "UPDATE"
}
```

#### Topic Exchange（主题交换机）
- routingKey 模糊匹配
- 支持通配符：`*` (匹配一个单词), `#` (匹配零个或多个单词)

**测试接口：**
```bash
POST http://localhost:8080/api/rabbitmq/send/topic?routingKey=topic.normal
Content-Type: application/json

{
  "userId": 1,
  "username": "王五",
  "action": "DELETE"
}
```

### 3. 高级特性

#### 延迟队列（Delay Queue）
- 使用 TTL + 死信队列实现
- 应用场景：订单超时取消、定时任务

**测试接口：**
```bash
POST http://localhost:8080/api/rabbitmq/send/delay?delaySeconds=30
Content-Type: application/json

"这是延迟消息，30 秒后处理"
```

**订单超时取消示例：**
```bash
POST http://localhost:8080/api/rabbitmq/order/timeout/cancel?orderId=ORDER_001
```

#### 死信队列（Dead Letter Queue）
- 处理失败的消息
- 消息被拒绝、超时、队列满等情况

**测试接口：**
```bash
POST http://localhost:8080/api/rabbitmq/send/deadletter
Content-Type: application/json

"这是一条死信消息"
```

#### 优先级队列（Priority Queue）
- 高优先级消息优先消费
- 优先级范围：0-10

**测试接口：**
```bash
POST http://localhost:8080/api/rabbitmq/send/priority?priority=8
Content-Type: application/json

"高优先级消息"
```

**批量发送（观察优先级效果）：**
```bash
POST http://localhost:8080/api/rabbitmq/send/priority/batch
```

### 4. 可靠性保障

#### 消息确认机制
- Publisher Confirm：消息到达交换机的确认
- Publisher Return：消息无法路由时的回调
- Consumer ACK：消费者手动确认

#### 手动 ACK 示例
```java
channel.basicAck(deliveryTag, false);  // 确认消息
channel.basicNack(deliveryTag, false, true);  // 拒绝并重新入队
```

#### 幂等性处理
通过消息 ID 去重，防止重复消费

**测试接口：**
```bash
POST http://localhost:8080/api/interview/idempotent/send?messageId=MSG_001
Content-Type: application/json

"需要保证幂等的消息"
```

### 5. 高频面试题

提供 10 道高频面试题及代码示例：

1. ✅ **如何保证消息的可靠性？**
   - 生产者确认 + 消息持久化 + 消费者手动 ACK + 死信队列

2. ✅ **如何实现延迟队列？**
   - TTL + 死信队列 / 延迟队列插件

3. ✅ **如何保证消息的顺序性？**
   - 相同 routingKey 发送到同一队列

4. ✅ **如何处理重复消息（幂等性）？**
   - 消息 ID 去重 + Redis / 数据库唯一键

5. ✅ **如何实现优先级队列？**
   - 队列设置 x-max-priority + 消息设置 priority

6. ✅ **发布订阅模式的几种交换机区别？**
   - Direct / Fanout / Topic 对比

7. ✅ **如何处理大量积压的消息？**
   - 扩容消费者 + 优化逻辑 + 调整 prefetch

8. ✅ **如何保证高可用？**
   - 集群 + 镜像队列 + 负载均衡

9. ✅ **死信队列的应用场景？**
   - 失败容错 + 延迟队列 + 异常收集

10. ✅ **消息队列的优缺点？**
    - 解耦、异步、削峰 / 复杂度增加、一致性挑战

**面试题测试接口：**
```bash
# 1. 可靠消息
POST http://localhost:8080/api/interview/reliability/send
Content-Type: application/json

"可靠消息内容"

# 2. 延迟队列
POST http://localhost:8080/api/interview/delay-queue/send?ttl=60000
Content-Type: application/json

"延迟消息"

# 3. 顺序消息
POST http://localhost:8080/api/interview/order/send?orderId=ORDER_001&count=3

# 5. 优先级批量
POST http://localhost:8080/api/interview/priority/send-batch

# 6. 交换机对比
POST http://localhost:8080/api/interview/exchange/compare
Content-Type: application/json

"测试消息"

# 7. 积压解决方案
GET http://localhost:8080/api/interview/backlog/solution

# 8. 高可用方案
GET http://localhost:8080/api/interview/ha/solution

# 9. 死信队列
POST http://localhost:8080/api/interview/dlx/send-failure
Content-Type: application/json

"死信消息"

# 10. 优缺点
GET http://localhost:8080/api/interview/mq-pros-cons
```

## 🔧 核心配置说明

### application.yml 关键配置

```yaml
spring:
  rabbitmq:
    # 连接信息
    host: localhost
    port: 5672
    username: admin
    password: admin123
    virtual-host: /
    
    # 生产者确认
    publisher-confirm-type: correlated
    publisher-returns: true
    
    listener:
      simple:
        # 消费者手动 ACK
        acknowledge-mode: manual
        # 消费者数量
        concurrency: 3
        max-concurrency: 10
        # 预取数量（限流）
        prefetch: 5
        # 重试配置
        retry:
          enabled: true
          initial-interval: 1000ms
          max-attempts: 3
```

## 📝 知识点总结

### 消息生产流程
1. Producer → Exchange（根据 routingKey）
2. Exchange → Queue（根据 Binding）
3. Queue → Consumer（推送或拉取）

### 五种消息模式
1. **简单模式**：一对一
2. **工作队列模式**：多对多（负载均衡）
3. **发布订阅模式**：Fanout Exchange，广播
4. **路由模式**：Direct Exchange，精确匹配
5. **主题模式**：Topic Exchange，模糊匹配

### 可靠性保障机制
- **生产者确认**：确保消息到达交换机
- **消息持久化**：队列、交换机、消息都持久化
- **消费者手动 ACK**：确保消息被成功处理
- **死信队列**：失败消息的容错处理
- **重试机制**：自动重试失败的消息

### 高级特性
- **延迟队列**：TTL + DLX 实现
- **优先级队列**：x-max-priority 参数
- **死信队列**：消息失败后的归宿
- **幂等性**：消息 ID 去重

## 🎯 使用建议

1. **学习顺序**：
   - 先理解基础队列 → 交换机模式 → 高级特性 → 可靠性保障
   
2. **实践建议**：
   - 每个接口都实际调用，观察日志
   - 尝试修改配置参数，观察效果
   - 模拟异常情况（如关闭 RabbitMQ）

3. **面试准备**：
   - 重点掌握可靠性保障机制
   - 理解各种交换机的区别
   - 掌握延迟队列、死信队列的实现
   - 能够手写代码解决实际问题

## 📖 参考资料

- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP 参考指南](https://docs.spring.io/spring-amqp/reference/)
- [RabbitMQ 面试题汇总](https://github.com/Snailclimb/JavaGuide)

## 💡 常见问题

### Q: 消息丢失怎么办？
A: 开启持久化（队列、消息都持久化）+ 生产者确认 + 消费者手动 ACK

### Q: 如何保证消息不重复？
A: 业务层面实现幂等性（消息 ID 去重、数据库唯一键）

### Q: 如何保证消息顺序？
A: 使用相同的 routingKey，确保进入同一队列

### Q: 延迟队列怎么实现？
A: TTL + 死信队列 或 使用延迟队列插件

## 📧 联系方式

如有问题，欢迎交流！
