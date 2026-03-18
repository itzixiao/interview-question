# RabbitMQ 核心知识点详解

> **说明：** 本文档配合代码示例模块 `interview-microservices-parent/interview-rabbitmq` 学习，包含完整的实战代码和高频面试题。

## 📚 目录

1. [RabbitMQ 基础概念](#1-rabbitmq-基础概念)
2. [消息队列的五种工作模式](#2-消息队列的五种工作模式)
3. [高级特性](#3-高级特性)
4. [可靠性保障机制](#4-可靠性保障机制)
5. [高频面试题与答案](#5-高频面试题与答案)
6. [实战应用场景](#6-实战应用场景)
7. [性能优化与最佳实践](#7-性能优化与最佳实践)

---

## 1. RabbitMQ 基础概念

### 1.1 什么是 RabbitMQ？

RabbitMQ 是一个开源的消息代理软件（Message Broker），基于 AMQP（Advanced Message Queuing Protocol）协议实现。它的主要作用是*
*接收、存储和转发消息**。

### 1.2 核心架构组件

```
Producer → Exchange → Queue → Consumer
```

| 组件                   | 说明          | 类比     |
|----------------------|-------------|--------|
| **Producer（生产者）**    | 发送消息的应用程序   | 寄件人    |
| **Consumer（消费者）**    | 接收消息的应用程序   | 收件人    |
| **Queue（队列）**        | 存储消息的缓冲区    | 邮箱     |
| **Exchange（交换机）**    | 接收消息并路由到队列  | 邮局分拣中心 |
| **Binding（绑定）**      | 连接交换机和队列的规则 | 邮递路线   |
| **Routing Key（路由键）** | 消息的路由标识     | 收件地址   |

### 1.3 为什么使用消息队列？

#### 优点

1. **解耦**：生产者和消费者互不依赖
2. **异步**：提高系统响应速度
3. **削峰**：平滑流量峰值，保护后端系统
4. **缓冲**：缓解瞬时压力

#### 缺点

1. 系统复杂度增加
2. 数据一致性挑战
3. 依赖第三方中间件
4. 运维成本增加

---

## 2. 消息队列的五种工作模式

### 2.1 简单模式（Simple Queue）

**特点：**

- 一对一模式
- 一个生产者，一个消费者
- 最简单的使用场景

**适用场景：**

- 简单的任务分发
- 单线程处理任务

**代码示例：**

```java
// 生产者
rabbitTemplate.convertAndSend("queue.simple", "这是一条简单消息");

// 消费者
@RabbitListener(queues = "queue.simple")
public void handleSimple(String message) {
    log.info("收到消息：{}", message);
}
```

### 2.2 工作队列模式（Work Queue）

**特点：**

- 多个消费者监听同一个队列
- 消息会被轮询分发给消费者
- 负载均衡

**适用场景：**

- 批量任务处理
- 后台任务并发处理

**代码示例：**

```java
// 多个消费者监听同一个队列
@RabbitListener(queues = "queue.work")
public void handleWork1(String message) {
    // 消费者 1
    log.info("消费者 1 处理：{}", message);
}

@RabbitListener(queues = "queue.work")
public void handleWork2(String message) {
    // 消费者 2
    log.info("消费者 2 处理：{}", message);
}
```

### 2.3 发布订阅模式（Fanout Exchange）

**特点：**

- 广播模式
- 忽略 routingKey
- 所有绑定的队列都会收到消息

**适用场景：**

- 群发消息
- 系统公告
- 事件通知

**代码示例：**

```java
// 生产者 - 发送到扇形交换机
rabbitTemplate.convertAndSend("exchange.fanout", "", "广播消息");

// 配置类
@Bean
public FanoutExchange fanoutExchange() {
    return new FanoutExchange("exchange.fanout");
}
```

### 2.4 路由模式（Direct Exchange）

**特点：**

- routingKey 完全匹配
- 精确路由
- 一个队列可以绑定多个 routingKey

**适用场景：**

- 按类型分发
- 多租户隔离

**代码示例：**

```java
// 生产者 - 发送不同 routingKey 的消息
rabbitTemplate.convertAndSend("exchange.direct", "routing.key.1", "消息 1");
rabbitTemplate.convertAndSend("exchange.direct", "routing.key.2", "消息 2");

// 配置类
@Bean
public DirectExchange directExchange() {
    return new DirectExchange("exchange.direct");
}

@Bean
public Binding binding1(DirectExchange exchange, Queue queue1) {
    return BindingBuilder.bind(queue1).to(exchange).with("routing.key.1");
}
```

### 2.5 主题模式（Topic Exchange）

**特点：**

- routingKey 模糊匹配
- 支持通配符：`*` (匹配一个单词), `#` (匹配零个或多个单词)
- 最灵活的路由方式

**适用场景：**

- 多级分类
- 标签过滤

**代码示例：**

```java
// routingKey 示例
"topic.normal"      // 匹配 *.normal
"topic.important"   // 匹配 *.important
"news.china.beijing" // 匹配 news.#

// 配置类
@Bean
public TopicExchange topicExchange() {
    return new TopicExchange("exchange.topic");
}

@Bean
public Binding topicBinding(TopicExchange exchange, Queue queue) {
    return BindingBuilder.bind(queue).to(exchange).with("topic.*");
}
```

### 2.6 五种模式对比

| 模式   | Exchange 类型 | Routing Key | 特点   | 应用场景 |
|------|-------------|-------------|------|------|
| 简单   | 无           | 无           | 一对一  | 简单任务 |
| 工作队列 | 无           | 无           | 负载均衡 | 批量任务 |
| 发布订阅 | Fanout      | 忽略          | 广播   | 群发通知 |
| 路由   | Direct      | 完全匹配        | 精确路由 | 分类处理 |
| 主题   | Topic       | 模糊匹配        | 灵活路由 | 多级分类 |

---

## 3. 高级特性

### 3.1 延迟队列（Delay Queue）

**实现方案：**

#### 方案一：TTL + 死信队列（推荐）

**原理：**

1. 设置消息 TTL（Time To Live）
2. 消息到期后自动进入死信队列
3. 消费者监听死信队列

**配置示例：**

```java
@Bean
public Queue delayQueue() {
    return QueueBuilder.durable("queue.delay")
            .withArgument("x-dead-letter-exchange", "exchange.deadletter")
            .withArgument("x-dead-letter-routing-key", "delay.routingkey")
            .withArgument("x-message-ttl", 60000) // 60 秒 TTL
            .build();
}
```

**应用场景：**

- 订单超时取消（30 分钟未支付）
- 定时任务调度
- 延时通知

**代码示例：**

```java
// 发送延迟消息
public void sendDelayMessage(Order order, int delaySeconds) {
    rabbitTemplate.convertAndSend("exchange.direct", "queue.delay", order,
        msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(delaySeconds * 1000));
            return msg;
        });
}

// 消费者处理到期消息
@RabbitListener(queues = "queue.delay.deadletter")
public void handleDelayMessage(Order order) {
    // 处理延迟任务，如取消订单
    orderService.cancelOrder(order.getId());
}
```

#### 方案二：RabbitMQ 延迟队列插件

**步骤：**

1. 启用 `rabbitmq_delayed_message_exchange` 插件
2. 使用 `x-delayed-type` 参数

### 3.2 死信队列（Dead Letter Queue）

**什么是死信？**

当消息在一个队列中变成死信时，它会被重新发布到另一个交换机（死信交换机）。

**死信产生的原因：**

1. 消息被拒绝（basic.reject/basicNack）并且 requeue=false
2. 消息 TTL 过期
3. 队列达到最大长度

**配置示例：**

```java
// 死信队列
@Bean
public Queue deadLetterQueue() {
    return QueueBuilder.durable("queue.deadletter").build();
}

// 死信交换机
@Bean
public DirectExchange deadLetterExchange() {
    return new DirectExchange("exchange.deadletter");
}

// 绑定
@Bean
public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
    return BindingBuilder.bind(deadLetterQueue)
            .to(deadLetterExchange)
            .with("deadletter.routingkey");
}

// 普通队列绑定死信
@Bean
public Queue normalQueueWithDLX() {
    return QueueBuilder.durable("queue.normal.with.dlx")
            .withArgument("x-dead-letter-exchange", "exchange.deadletter")
            .withArgument("x-dead-letter-routing-key", "deadletter.routingkey")
            .build();
}
```

**应用场景：**

- 失败消息容错处理
- 异常消息收集和分析
- 人工介入处理

### 3.3 优先级队列（Priority Queue）

**配置示例：**

```java
@Bean
public Queue priorityQueue() {
    return QueueBuilder.durable("queue.priority")
            .withArgument("x-max-priority", 10) // 最大优先级 10
            .build();
}
```

**发送优先级消息：**

```java
public void sendPriorityMessage(Message message, int priority) {
    // 确保优先级在 0-10 之间
    final int finalPriority = Math.max(0, Math.min(10, priority));
    
    rabbitTemplate.convertAndSend("exchange.direct", "queue.priority", message,
        msg -> {
            msg.getMessageProperties().setPriority(finalPriority);
            return msg;
        });
}
```

**应用场景：**

- VIP 用户优先处理
- 紧急任务插队
- 重要订单优先处理

### 3.4 消息 TTL（Time To Live）

**两种设置方式：**

#### 方式一：队列级别

```java
@Bean
public Queue ttlQueue() {
    return QueueBuilder.durable("queue.ttl")
            .withArgument("x-message-ttl", 60000) // 60 秒
            .build();
}
```

#### 方式二：消息级别

```java
Message message = MessageBuilder
    .withBody("消息内容".getBytes())
    .setExpiration("60000") // 60 秒
    .build();
rabbitTemplate.send("exchange", "routingKey", message);
```

---

## 4. 可靠性保障机制

### 4.1 如何保证消息不丢失？

#### 完整解决方案

**1. 生产者确认机制（Publisher Confirm）**

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
```

```java
@Configuration
public class MessageConfirmHandler implements ConfirmCallback, ReturnsCallback {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }
    
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.info("✅ 消息确认成功");
        } else {
            log.error("❌ 消息确认失败：{}", cause);
            // 记录日志或重试
        }
    }
    
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.error("❌ 消息无法路由：{}", returnedMessage.getReplyText());
        // 记录到数据库或告警
    }
}
```

**2. 消息持久化**

```java
// 队列持久化
@Bean
public Queue queue() {
    return QueueBuilder.durable("queue.name").build(); // durable=true
}

// 消息持久化
Message message = MessageBuilder
    .withBody(body.getBytes())
    .setDeliveryMode(MessageDeliveryMode.PERSISTENT) // 持久化
    .build();
```

**3. 消费者手动 ACK**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual # 手动确认
```

```java
@RabbitListener(queues = "queue.name")
public void handleMessage(Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    
    try {
        // 处理业务
        processBusiness(message);
        
        // 手动确认
        channel.basicAck(deliveryTag, false);
        
    } catch (Exception e) {
        // 判断重试次数
        Integer xDeath = (Integer) message.getMessageProperties()
                .getHeaders().get("x-death");
        
        if (xDeath != null && xDeath >= 3) {
            // 重试超过 3 次，拒绝消息，进入死信队列
            channel.basicNack(deliveryTag, false, false);
        } else {
            // 否则重新入队
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
```

### 4.2 如何保证消息的顺序性？

**问题场景：**
同一个订单的多个操作（创建→支付→发货）需要按顺序处理。

**解决方案：**

**方案一：使用相同的 routingKey**

```java
// 发送订单的多个操作
for (int i = 1; i <= 3; i++) {
    OrderOperation op = new OrderOperation(orderId, "操作" + i);
    // 关键：使用相同的 routingKey，确保进入同一个队列
    rabbitTemplate.convertAndSend("exchange", "order." + orderId, op);
}
```

**方案二：单线程消费（不推荐）**

```java
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConcurrentConsumers(1); // 单消费者
    factory.setMaxConcurrentConsumers(1);
    return factory;
}
```

**方案三：业务层面保证**

```java
@RabbitListener(queues = "order.queue")
public void handleOrder(OrderOperation operation) {
    // 检查版本号或时间戳
    if (operation.getVersion() > lastProcessedVersion) {
        process(operation);
        lastProcessedVersion = operation.getVersion();
    } else {
        log.warn("跳过旧版本操作");
    }
}
```

### 4.3 如何保证消息不重复消费（幂等性）？

**重复消费的原因：**

1. 网络问题导致 ACK 丢失
2. 消费者重启
3. 消息重投

**解决方案：**

**方案一：消息 ID 去重（推荐）**

```java
@RabbitListener(queues = "queue.idempotent")
public void handleMessage(Message message, Channel channel) throws IOException {
    String messageId = message.getMessageProperties().getMessageId();
    
    // 检查是否已处理
    if (redisTemplate.hasKey("msg:" + messageId)) {
        log.info("消息已处理，跳过：{}", messageId);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        return;
    }
    
    // 处理业务
    processBusiness(message);
    
    // 标记已处理
    redisTemplate.opsForValue().set("msg:" + messageId, "1", 24, TimeUnit.HOURS);
    
    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
}
```

**方案二：数据库唯一键**

```java
@Transactional
public void processOrder(Order order) {
    // 利用数据库唯一索引
    try {
        orderMapper.insert(order); // order_id 是唯一键
    } catch (DuplicateKeyException e) {
        log.warn("订单已存在，跳过：{}", order.getId());
    }
}
```

**方案三：乐观锁**

```java
@Update("UPDATE orders SET status = #{status}, version = version + 1 
         WHERE id = #{id} AND version = #{version}")
int updateOrder(@Param("id") Long id, 
                @Param("status") String status, 
                @Param("version") Integer version);
```

### 4.4 如何处理大量积压的消息？

**应急方案：**

1. **临时扩容消费者**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 10  # 增加到 10 个消费者
        max-concurrency: 50
```

2. **调整 prefetch 参数**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10  # 每次推送 10 条消息
```

3. **优化消费逻辑**

```java
// 批量处理
@RabbitListener(queues = "queue.backlog")
public void handleBatch(List<Message> messages) {
    // 批量插入数据库
    batchInsert(messages);
}
```

4. **非重要消息设置 TTL 自动过期**

```java
@Bean
public Queue ttlQueue() {
    return QueueBuilder.durable("queue.ttl")
            .withArgument("x-message-ttl", 300000) // 5 分钟后过期
            .build();
}
```

### 4.5 如何保证高可用？

**方案一：集群模式**

```bash
# 至少 3 个节点
node1: rabbitmq@host1
node2: rabbitmq@host2
node3: rabbitmq@host3
```

**方案二：镜像队列（Mirror Queue）**

```bash
# 设置队列镜像策略
rabbitmqctl set_policy ha-all "^ha\." '{"ha-mode":"all"}'
```

**方案三：负载均衡**

```nginx
upstream rabbitmq {
    server node1:5672;
    server node2:5672;
    server node3:5672;
}
```

**方案四：客户端配置多个连接地址**

```yaml
spring:
  rabbitmq:
    addresses: host1:5672,host2:5672,host3:5672
```

---

## 5. 高频面试题与答案

### 5.1 基础题（⭐⭐）

#### 面试题 1：RabbitMQ 是什么？为什么要用它？

**考察点：** MQ 基础概念、应用场景理解

**答案要点：**

1. **定义**：基于 AMQP 协议的消息代理中间件
2. **核心作用**：接收、存储和转发消息
3. **使用理由**：解耦、异步、削峰、缓冲

**详细解析：**

RabbitMQ 是一个开源的消息代理软件（Message Broker），基于 AMQP（Advanced Message Queuing Protocol）协议实现。

**为什么要使用 RabbitMQ？**

| 优势     | 说明          | 实际场景            |
|--------|-------------|-----------------|
| **解耦** | 生产者和消费者互不依赖 | 订单系统→库存/物流/短信系统 |
| **异步** | 提高系统响应速度    | 注册后异步发送邮件       |
| **削峰** | 平滑流量峰值      | 秒杀活动缓冲瞬时流量      |
| **缓冲** | 保护后端系统      | 数据库压力过大时缓冲      |

**典型应用场景：**

- 电商订单流程处理
- 用户注册后的通知发送
- 日志收集和数据处理
- 定时任务和延迟任务

---

#### 面试题 2：RabbitMQ 有哪些组成部分？

**考察点：** 架构组件理解

**答案要点：**

1. Producer（生产者）
2. Consumer（消费者）
3. Queue（队列）
4. Exchange（交换机）
5. Binding（绑定）
6. Routing Key（路由键）

**详细解析：**

```
Producer → Exchange → Queue → Consumer
```

| 组件              | 说明          | 生活类比   |
|-----------------|-------------|--------|
| **Producer**    | 发送消息的应用程序   | 寄件人    |
| **Consumer**    | 接收消息的应用程序   | 收件人    |
| **Queue**       | 存储消息的缓冲区    | 邮箱     |
| **Exchange**    | 接收消息并路由到队列  | 邮局分拣中心 |
| **Binding**     | 连接交换机和队列的规则 | 邮递路线   |
| **Routing Key** | 消息的路由标识     | 收件地址   |

**工作流程：**

1. Producer 发送消息到 Exchange
2. Exchange 根据 Routing Key 和 Binding 规则路由到 Queue
3. Consumer 从 Queue 拉取消息并处理

---

#### 面试题 3：说说 RabbitMQ 的五种工作模式？

**考察点：** 消息模式理解与应用

**答案要点：**

1. **简单模式**：一对一
2. **工作队列模式**：负载均衡
3. **发布订阅模式**（Fanout）：广播
4. **路由模式**（Direct）：精确匹配
5. **主题模式**（Topic）：模糊匹配

**详细解析：**

| 模式       | Exchange 类型 | Routing Key | 特点        | 应用场景      |
|----------|-------------|-------------|-----------|-----------|
| **简单模式** | 无           | 无           | 一对一，最简单   | 简单任务分发    |
| **工作队列** | 无           | 无           | 多消费者负载均衡  | 批量后台任务    |
| **发布订阅** | Fanout      | 忽略          | 广播到所有绑定队列 | 群发通知、公告   |
| **路由模式** | Direct      | 完全匹配        | 精确路由      | 按类型分发     |
| **主题模式** | Topic       | 模糊匹配        | 支持*和#通配符  | 多级分类、标签过滤 |

**代码对比：**

```java
// 1. 简单模式
rabbitTemplate.convertAndSend("queue.simple", "消息");

// 2. 工作队列模式（多个消费者监听同一队列）
@RabbitListener(queues = "queue.work")
public void handleWork(String message) { }

// 3. 发布订阅模式（Fanout）
rabbitTemplate.convertAndSend("exchange.fanout", "", "广播消息");

// 4. 路由模式（Direct）
rabbitTemplate.convertAndSend("exchange.direct", "routing.key.1", "消息 1");

// 5. 主题模式（Topic）
rabbitTemplate.convertAndSend("exchange.topic", "topic.*", "消息");
```

---

### 5.2 进阶题（⭐⭐⭐）

#### 面试题 4：如何保证消息不丢失？

**考察点：** 可靠性保障机制

**答案要点：**

1. **生产者端**：Publisher Confirm + 消息持久化
2. **RabbitMQ 端**：队列/交换机/绑定持久化
3. **消费者端**：手动 ACK 确认

**详细解析：**

**一、生产者端保障**

1. **开启 Publisher Confirm**

```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
```

```java
@Configuration
public class MessageConfirmHandler implements ConfirmCallback, ReturnsCallback {
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            log.info("✅ 消息确认成功");
        } else {
            log.error("❌ 消息确认失败：{}", cause);
        }
    }
    
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        log.error("❌ 消息无法路由：{}", returnedMessage.getReplyText());
    }
}
```

2. **消息持久化**

```java
Message message = MessageBuilder
    .withBody(body.getBytes())
    .setDeliveryMode(MessageDeliveryMode.PERSISTENT) // 持久化
    .build();
```

**二、RabbitMQ 端保障**

```java
@Bean
public Queue queue() {
    return QueueBuilder.durable("queue.name").build(); // durable=true
}
```

**三、消费者端保障**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual # 手动确认
```

```java
@RabbitListener(queues = "queue.name")
public void handleMessage(Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    
    try {
        processBusiness(message);
        channel.basicAck(deliveryTag, false); // 手动确认
    } catch (Exception e) {
        // 失败消息进入死信队列
        channel.basicNack(deliveryTag, false, false);
    }
}
```

---

#### 面试题 5：如何保证消息的顺序性？

**考察点：** 消息顺序控制方案

**答案要点：**

1. **最优方案**：相同 routingKey 确保进入同一队列
2. **备选方案**：业务层面版本号控制
3. **不推荐**：单线程消费（影响性能）

**详细解析：**

**问题场景：**
同一个订单的多个操作（创建→支付→发货）需要按顺序处理。

**方案一：使用相同的 routingKey（推荐）**

```java
// 发送订单的多个操作
for (int i = 1; i <= 3; i++) {
    OrderOperation op = new OrderOperation(orderId, "操作" + i);
    // 关键：使用相同的 routingKey，确保进入同一个队列
    rabbitTemplate.convertAndSend("exchange", "order." + orderId, op);
}
```

**原理：**

- 相同 routingKey 的消息会进入同一个队列
- 队列内部是 FIFO（先进先出）
- 保证消息按序被消费

**方案二：业务层面控制**

```java
@RabbitListener(queues = "order.queue")
public void handleOrder(OrderOperation operation) {
    // 检查版本号或时间戳
    if (operation.getVersion() > lastProcessedVersion) {
        process(operation);
        lastProcessedVersion = operation.getVersion();
    } else {
        log.warn("跳过旧版本操作");
    }
}
```

---

#### 面试题 6：如何保证消息不重复消费（幂等性）？

**考察点：** 幂等性设计与实现

**答案要点：**

1. **消息 ID 去重**（Redis）
2. **数据库唯一键**
3. **乐观锁机制**
4. **状态机控制**

**详细解析：**

**重复消费的原因：**

1. 网络问题导致 ACK 丢失
2. 消费者重启
3. 消息重投

**方案一：消息 ID 去重（推荐）**

```java
@RabbitListener(queues = "queue.idempotent")
public void handleMessage(Message message, Channel channel) throws IOException {
    String messageId = message.getMessageProperties().getMessageId();
    
    // 检查是否已处理
    if (redisTemplate.hasKey("msg:" + messageId)) {
        log.info("消息已处理，跳过：{}", messageId);
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        return;
    }
    
    // 处理业务
    processBusiness(message);
    
    // 标记已处理
    redisTemplate.opsForValue().set("msg:" + messageId, "1", 24, TimeUnit.HOURS);
    
    channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
}
```

**方案二：数据库唯一键**

```java
@Transactional
public void processOrder(Order order) {
    try {
        orderMapper.insert(order); // order_id 是唯一键
    } catch (DuplicateKeyException e) {
        log.warn("订单已存在，跳过：{}", order.getId());
    }
}
```

**方案三：乐观锁**

```java
@Update("UPDATE orders SET status = #{status}, version = version + 1 
         WHERE id = #{id} AND version = #{version}")
int updateOrder(@Param("id") Long id, 
                @Param("status") String status, 
                @Param("version") Integer version);
```

---

#### 面试题 7：延迟队列如何实现？

**考察点：** 延迟队列实现方案

**答案要点：**

1. **TTL + 死信队列**（推荐，无需插件）
2. **RabbitMQ 延迟队列插件**

**详细解析：**

**方案一：TTL + 死信队列（推荐）**

**原理：**

1. 设置消息 TTL（Time To Live）
2. 消息到期后自动进入死信队列
3. 消费者监听死信队列

**配置示例：**

```java
@Bean
public Queue delayQueue() {
    return QueueBuilder.durable("queue.delay")
            .withArgument("x-dead-letter-exchange", "exchange.deadletter")
            .withArgument("x-dead-letter-routing-key", "delay.routingkey")
            .withArgument("x-message-ttl", 60000) // 60 秒 TTL
            .build();
}
```

**应用场景：**

- 订单超时取消（30 分钟未支付）
- 定时任务调度
- 延时通知

**方案二：RabbitMQ 延迟队列插件**

**步骤：**

1. 启用 `rabbitmq_delayed_message_exchange` 插件
2. 使用 `x-delayed-type` 参数

```java
// 使用插件发送延迟消息
rabbitTemplate.convertAndSend("exchange.delay", "routingKey", message,
    msg -> {
        msg.getMessageProperties().setHeader("x-delay", 60000); // 60 秒延迟
        return msg;
    });
```

**两种方案对比：**

| 方案      | 优点        | 缺点      |
|---------|-----------|---------|
| TTL+DLX | 无需插件，原生支持 | 配置稍复杂   |
| 延迟插件    | 配置简单      | 需额外安装插件 |

---

#### 面试题 8：死信队列有什么用？

**考察点：** 死信队列应用场景

**答案要点：**

1. **失败容错**：正常队列消费失败的消息
2. **延迟队列**：TTL 到期后的消息
3. **异常收集**：便于分析和问题定位
4. **人工处理**：需要人工介入的异常消息

**详细解析：**

**什么是死信？**

当消息在一个队列中变成死信时，它会被重新发布到另一个交换机（死信交换机）。

**死信产生的原因：**

1. 消息被拒绝（basic.reject/basicNack）并且 requeue=false
2. 消息 TTL 过期
3. 队列达到最大长度

**配置示例：**

```java
// 死信队列
@Bean
public Queue deadLetterQueue() {
    return QueueBuilder.durable("queue.deadletter").build();
}

// 普通队列绑定死信
@Bean
public Queue normalQueueWithDLX() {
    return QueueBuilder.durable("queue.normal.with.dlx")
            .withArgument("x-dead-letter-exchange", "exchange.deadletter")
            .withArgument("x-dead-letter-routing-key", "deadletter.routingkey")
            .build();
}
```

---

### 5.3 高阶题（⭐⭐⭐⭐）

#### 面试题 9：消息积压了怎么办？

**考察点：** 应急处理能力与优化思路

**答案要点：**

1. **应急扩容**：增加消费者数量
2. **优化逻辑**：减少单次处理时间
3. **调整参数**：提高 prefetch 值
4. **批量处理**：提升吞吐量
5. **限流降级**：非重要消息设置 TTL

**详细解析：**

**应急处理方案：**

**1. 临时扩容消费者**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 10  # 增加到 10 个消费者
        max-concurrency: 50
```

**2. 调整 prefetch 参数**

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10  # 每次推送 10 条消息
```

**3. 批量处理**

```java
@RabbitListener(queues = "queue.backlog")
public void handleBatch(List<Message> messages) {
    // 批量插入数据库
    batchInsert(messages);
}
```

**4. 非重要消息设置 TTL 自动过期**

```java
@Bean
public Queue ttlQueue() {
    return QueueBuilder.durable("queue.ttl")
            .withArgument("x-message-ttl", 300000) // 5 分钟后过期
            .build();
}
```

**长期优化措施：**

1. 监控系统，提前预警
2. 弹性伸缩，自动扩容
3. 限流降级，保护系统

---

#### 面试题 10：如何保证 RabbitMQ 的高可用？

**考察点：** 高可用架构设计

**答案要点：**

1. **集群部署**：至少 3 个节点
2. **镜像队列**：数据多副本
3. **负载均衡**：HAProxy/Nginx
4. **多机房部署**：异地容灾

**详细解析：**

**方案一：集群模式**

```bash
# 至少 3 个节点
node1: rabbitmq@host1
node2: rabbitmq@host2
node3: rabbitmq@host3
```

**方案二：镜像队列（Mirror Queue）**

```bash
# 设置队列镜像策略
rabbitmqctl set_policy ha-all "^ha\." '{"ha-mode":"all"}'
```

**方案三：负载均衡**

```nginx
upstream rabbitmq {
    server node1:5672;
    server node2:5672;
    server node3:5672;
}
```

**方案四：客户端配置多个连接地址**

```yaml
spring:
  rabbitmq:
    addresses: host1:5672,host2:5672,host3:5672
```

---

#### 面试题 11：RabbitMQ 和 Kafka 的区别？

**考察点：** 技术选型能力

**答案要点：**

1. **协议不同**：AMQP vs 自定义 TCP
2. **吞吐量不同**：万级 vs 十万级
3. **延迟不同**：微秒级 vs 毫秒级
4. **适用场景不同**

**详细解析：**

| 对比项       | RabbitMQ  | Kafka      |
|-----------|-----------|------------|
| **协议**    | AMQP      | 自定义 TCP 协议 |
| **消息可靠性** | 高（ACK 机制） | 高（副本机制）    |
| **吞吐量**   | 万级        | 十万级        |
| **延迟**    | 微秒级       | 毫秒级        |
| **消息堆积**  | 较弱        | 强（持久化）     |
| **适用场景**  | 复杂路由、低延迟  | 日志收集、大数据   |

**选型建议：**

- **选择 RabbitMQ**：需要复杂路由、低延迟、高可靠性
- **选择 Kafka**：大数据日志收集、高吞吐场景

---

### 5.4 实战题（⭐⭐⭐⭐⭐）

#### 面试题 12：设计一个订单超时取消系统

**考察点：** 延迟队列实战应用

**需求：** 订单 30 分钟未支付自动取消

**答案要点：**

1. **使用延迟队列**
2. **TTL + 死信队列实现**
3. **幂等性保证**

**实现步骤：**

**1. 创建延迟队列**

```java
@Bean
public Queue orderDelayQueue() {
    return QueueBuilder.durable("queue.order.delay")
            .withArgument("x-dead-letter-exchange", "exchange.order.dlx")
            .withArgument("x-dead-letter-routing-key", "order.timeout")
            .withArgument("x-message-ttl", 30 * 60 * 1000) // 30 分钟
            .build();
}
```

**2. 下单时发送延迟消息**

```java
@Transactional
public void createOrder(Order order) {
    // 1. 创建订单
    orderMapper.insert(order);
    
    // 2. 发送延迟消息
    rabbitTemplate.convertAndSend("exchange.order.delay", 
        "order.delay", order,
        msg -> {
            msg.getMessageProperties().setExpiration(String.valueOf(30 * 60 * 1000));
            return msg;
        });
}
```

**3. 监听死信队列处理超时**

```java
@RabbitListener(queues = "queue.order.timeout")
public void handleTimeout(Order order) {
    // 检查订单状态（保证幂等性）
    Order dbOrder = orderMapper.selectById(order.getId());
    if ("UNPAID".equals(dbOrder.getStatus())) {
        // 取消订单
        orderService.cancelOrder(order.getId());
    }
}
```

---

#### 面试题 13：设计一个积分变更系统（保证不重复、不丢失）

**考察点：** 可靠性 + 幂等性综合设计

**关键点：**

1. 消息不丢失
2. 积分变更幂等
3. 事务一致性

**实现方案：**

**1. 发送可靠消息**

```java
@Transactional
public void awardPoints(Long userId, Integer points) {
    // 创建积分记录
    PointsRecord record = new PointsRecord(userId, points);
    recordMapper.insert(record);
    
    // 发送消息
    PointsMessage msg = new PointsMessage(record.getId(), userId, points);
    rabbitTemplate.convertAndSend("exchange.points", "points.change", msg,
        message -> {
            message.getMessageProperties().setDeliveryMode(
                MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setMessageId(
                UUID.randomUUID().toString());
            return message;
        });
}
```

**2. 幂等消费**

```java
@RabbitListener(queues = "queue.points")
public void handlePointsChange(PointsMessage msg, Channel channel) 
        throws IOException {
    long tag = msg.getMessageProperties().getDeliveryTag();
    String messageId = msg.getMessageProperties().getMessageId();
    
    try {
        // 检查是否已处理
        if (redisTemplate.hasKey("points:" + messageId)) {
            channel.basicAck(tag, false);
            return;
        }
        
        // 更新积分（带版本号）
        int rows = userMapper.updatePoints(msg.getUserId(), msg.getPoints(), 
                                           msg.getVersion());
        
        if (rows > 0) {
            // 标记已处理
            redisTemplate.set("points:" + messageId, "1", 24, TimeUnit.HOURS);
            channel.basicAck(tag, false);
        } else {
            throw new RuntimeException("更新失败");
        }
        
    } catch (Exception e) {
        channel.basicNack(tag, false, true);
    }
}
```

---

## 6. 实战应用场景

### 6.1 电商系统

**场景 1：订单流程**

```
下单 → 发送消息 → 
  ├─ 扣减库存
  ├─ 生成物流单
  └─ 发送短信通知
```

**场景 2：秒杀系统**

```
用户请求 → 消息队列 → 异步处理
  ├─ 验证资格
  ├─ 扣减库存
  └─ 生成订单
```

### 6.2 日志收集

```
应用服务器 → Filebeat → RabbitMQ → Logstash → Elasticsearch → Kibana
```

### 6.3 数据同步

```
MySQL Binlog → Canal → RabbitMQ → 
  ├─ Elasticsearch
  ├─ Redis
  └─ HBase
```

### 6.4 邮件/短信发送

```java
// 异步发送邮件
@Async
public void sendEmail(String to, String subject, String content) {
    rabbitTemplate.convertAndSend("exchange.email", "email.send", 
        new EmailMessage(to, subject, content));
}

// 批量消费
@RabbitListener(queues = "queue.email")
public void handleEmails(List<EmailMessage> emails) {
    emailService.sendBatch(emails);
}
```

---

## 7. 性能优化与最佳实践

### 7.1 性能优化

#### 1. 批量发送

```java
// 批量发送消息
List<Message> messages = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    messages.add(createMessage(i));
}
rabbitTemplate.convertAndSend("exchange", "routingKey", messages);
```

#### 2. 连接池配置

```yaml
spring:
  rabbitmq:
    cache:
      connection:
        size: 10  # 连接池大小
      channel:
        size: 25  # 通道池大小
```

#### 3. 并发消费

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 5     # 最小消费者数
        max-concurrency: 20 # 最大消费者数
        prefetch: 10       # 预取数量
```

### 7.2 最佳实践

#### 1. 队列命名规范

```
queue.{业务}.{功能}.{环境}
例如：queue.order.create.prod
```

#### 2. 消息格式

```json
{
  "messageId": "uuid",
  "timestamp": 1234567890,
  "version": "1.0",
  "data": {}
}
```

#### 3. 错误处理

```java
try {
    process(message);
    channel.basicAck(tag, false);
} catch (BusinessException e) {
    // 业务异常，不重试
    channel.basicNack(tag, false, false);
} catch (Exception e) {
    // 系统异常，重试
    channel.basicNack(tag, false, true);
}
```

#### 4. 监控指标

- 队列长度
- 消息生产/消费速率
- 消费者数量
- 消息积压情况
- 死信队列消息数

### 7.3 常见问题排查

#### 问题 1：消息发送失败

```
原因：
1. RabbitMQ 服务不可用
2. 网络问题
3. 认证失败

解决：
1. 检查服务状态
2. 检查网络连接
3. 检查用户名密码
```

#### 问题 2：消息未被消费

```
原因：
1. 消费者未启动
2. 队列未正确绑定
3. routingKey 不匹配

解决：
1. 检查消费者日志
2. 检查绑定关系
3. 检查 routingKey
```

#### 问题 3：消息重复消费

```
原因：
1. 网络抖动导致 ACK 丢失
2. 消费者重启

解决：
1. 实现幂等性
2. 记录已处理消息 ID
```

---

## 📚 参考资料

- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP 参考指南](https://docs.spring.io/spring-amqp/reference/)
- [代码示例](../../interview-microservices-parent/interview-rabbitmq/)

---

## 🎯 学习建议

1. **先理解基础概念**（1 天）
    - 了解 RabbitMQ 架构
    - 掌握五种工作模式

2. **动手实践**（2-3 天）
    - 运行示例代码
    - 测试各种模式
    - 观察消息流转

3. **深入高级特性**（2 天）
    - 延迟队列
    - 死信队列
    - 优先级队列

4. **掌握可靠性保障**（2 天）
    - 消息确认机制
    - 幂等性处理
    - 高可用方案

5. **刷面试题**（1-2 天）
    - 理解每道题的答案
    - 结合实际场景思考

---

**维护者：** itzixiao  
**最后更新：** 2026-03-14  
**配套代码：** `interview-microservices-parent/interview-rabbitmq`  
**问题反馈：** 欢迎提 Issue 或 PR
