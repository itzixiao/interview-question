# Redis 高级应用 - 延时队列与幂等

## 延时队列

### 什么是延时队列

延时队列是一种特殊的消息队列，消息不会立即被消费，而是在指定时间后才可被消费。

### 应用场景

| 场景 | 说明 |
|------|------|
| 订单超时取消 | 用户下单后 30 分钟未支付，自动取消订单 |
| 延时通知 | 活动结束后 1 小时发送评价提醒 |
| 定时任务 | 定时执行某些业务逻辑 |
| 重试机制 | 失败任务延时后重试 |

### Redis 实现延时队列

#### 方案一：Sorted Set（推荐）

使用 Redis 的 Sorted Set，以时间戳作为 score，消息内容作为 member。

```
Key: delay:queue
┌─────────────────────────────────────────┐
│  Member          │  Score (时间戳)       │
├─────────────────────────────────────────┤
│  order:1001      │  1704067200          │
│  order:1002      │  1704067260          │
│  order:1003      │  1704067320          │
└─────────────────────────────────────────┘
```

```java
public class RedisDelayQueue {
    
    private static final String DELAY_QUEUE_KEY = "delay:queue";
    
    /**
     * 添加延时任务
     */
    public void addTask(String taskId, long delaySeconds) {
        long executeTime = System.currentTimeMillis() / 1000 + delaySeconds;
        jedis.zadd(DELAY_QUEUE_KEY, executeTime, taskId);
    }
    
    /**
     * 获取到期任务（轮询）
     */
    public Set<String> getExpiredTasks() {
        long now = System.currentTimeMillis() / 1000;
        // 获取 score <= now 的所有成员
        return jedis.zrangeByScore(DELAY_QUEUE_KEY, 0, now);
    }
    
    /**
     * 移除已处理任务
     */
    public void removeTask(String taskId) {
        jedis.zrem(DELAY_QUEUE_KEY, taskId);
    }
}
```

#### 方案二：Redisson 延时队列

```java
// 创建延时队列
RDelayedQueue<String> delayedQueue = redisson.getDelayedQueue("myDelayQueue");

// 添加延时任务（10秒后执行）
delayedQueue.offer("task:1001", 10, TimeUnit.SECONDS);

// 消费队列
RQueue<String> destinationQueue = redisson.getQueue("myDelayQueue");
String task = destinationQueue.poll();
```

### 延时队列对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| Sorted Set | 简单、可控 | 需要轮询 |
| Redisson | 功能完善、自动消费 | 引入额外依赖 |
| RabbitMQ DLX | 专业消息队列 | 架构复杂 |

## 分布式幂等

### 什么是幂等

幂等性是指一个操作执行一次和执行多次的效果相同，不会产生副作用。

### 为什么需要幂等

```
场景1: 网络超时重试
客户端 ──请求──> 服务端处理成功 ──响应丢失──> 客户端重试
                                                    │
                                                    ▼
                                              服务端再次处理
                                              （重复操作！）

场景2: 消息队列消费
消息队列 ──消息──> 消费者处理成功 ──ACK丢失──> 消息重新投递
                                                    │
                                                    ▼
                                              消费者再次处理
                                              （重复消费！）
```

### 幂等解决方案

#### 1. 数据库唯一索引

```sql
-- 订单表添加唯一索引
CREATE UNIQUE INDEX uk_order_idempotent ON order_idempotent (order_no, operation_type);
```

```java
@Service
public class OrderService {
    
    @Transactional
    public void createOrder(String orderNo) {
        try {
            // 插入幂等表（唯一索引保证幂等）
            idempotentMapper.insert(new IdempotentRecord(orderNo, "CREATE"));
            
            // 执行业务逻辑
            orderMapper.insert(new Order(orderNo));
        } catch (DuplicateKeyException e) {
            // 已处理过，直接返回
            log.info("订单已存在，跳过处理: {}", orderNo);
        }
    }
}
```

#### 2. Redis 去重（Token 机制）

```
客户端                    服务端                    Redis
   │                        │                        │
   │  1. 申请 Token         │                        │
   │───────────────────────>│                        │
   │                        │  生成 UUID             │
   │                        │  存入 Redis (5分钟)    │
   │<───────────────────────│                        │
   │                        │                        │
   │  2. 携带 Token 请求    │                        │
   │───────────────────────>│                        │
   │                        │  检查 Token            │
   │                        │───────────────────────>│
   │                        │  存在则删除并执行      │
   │                        │  不存在则拒绝          │
   │<───────────────────────│                        │
```

```java
public class IdempotentService {
    
    /**
     * 生成幂等 Token
     */
    public String generateToken(String userId) {
        String token = UUID.randomUUID().toString();
        String key = "idempotent:token:" + token;
        jedis.setex(key, 5 * 60, userId); // 5分钟有效
        return token;
    }
    
    /**
     * 校验 Token（保证原子性）
     */
    public boolean validateToken(String token) {
        String key = "idempotent:token:" + token;
        // Lua 脚本保证原子性
        String script = 
            "if redis.call('get', KEYS[1]) then " +
            "    redis.call('del', KEYS[1]);" +
            "    return 1;" +
            "else " +
            "    return 0;" +
            "end";
        
        Object result = jedis.eval(script, 
            Collections.singletonList(key), 
            Collections.emptyList());
        return Long.valueOf(1).equals(result);
    }
}

// Controller 使用
@RestController
public class OrderController {
    
    @PostMapping("/order")
    public Result createOrder(@RequestHeader("Idempotent-Token") String token,
                              @RequestBody OrderRequest request) {
        // 校验幂等 Token
        if (!idempotentService.validateToken(token)) {
            return Result.error("重复请求或 Token 无效");
        }
        
        // 执行业务逻辑
        orderService.createOrder(request);
        return Result.success();
    }
}
```

#### 3. 状态机幂等

根据业务状态判断是否可以执行操作。

```java
public enum OrderStatus {
    CREATED(1, "已创建"),
    PAID(2, "已支付"),
    SHIPPED(3, "已发货"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消");
    
    // 状态流转图
    private static final Map<OrderStatus, List<OrderStatus>> transitions = new HashMap<>();
    static {
        transitions.put(CREATED, Arrays.asList(PAID, CANCELLED));
        transitions.put(PAID, Arrays.asList(SHIPPED));
        transitions.put(SHIPPED, Arrays.asList(COMPLETED));
    }
    
    public boolean canTransitionTo(OrderStatus newStatus) {
        return transitions.getOrDefault(this, Collections.emptyList())
                         .contains(newStatus);
    }
}

@Service
public class OrderService {
    
    @Transactional
    public void payOrder(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        
        // 状态机校验
        if (!order.getStatus().canTransitionTo(OrderStatus.PAID)) {
            if (order.getStatus() == OrderStatus.PAID) {
                log.info("订单已支付，幂等返回: {}", orderNo);
                return; // 已支付，幂等返回
            }
            throw new BusinessException("订单状态不允许支付: " + order.getStatus());
        }
        
        // 执行支付逻辑
        orderMapper.updateStatus(orderNo, OrderStatus.PAID);
    }
}
```

### 幂等方案对比

| 方案 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| 唯一索引 | 数据库操作 | 简单可靠 | 只能防重复插入 |
| Token 机制 | 接口调用 | 通用性强 | 需要额外交互 |
| 状态机 | 业务流程 | 业务语义清晰 | 需要状态设计 |
| Redis 去重 | 短时窗口 | 性能好 | 有有效期限制 |

## 敏感数据加密

### 加密场景

| 数据类型 | 加密方式 | 说明 |
|---------|---------|------|
| 密码 | 单向哈希（BCrypt） | 不可解密 |
| 手机号 | 对称加密（AES） | 可解密查询 |
| 身份证号 | 脱敏 + 加密 | 部分展示 |
| 银行卡号 | 令牌化（Tokenization） | 替换为令牌 |

### Redis 加密存储示例

```java
public class SecureDataService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private AESUtil aesUtil;
    
    /**
     * 存储敏感数据
     */
    public void storeSensitiveData(String key, String data) {
        // AES 加密后存储
        String encrypted = aesUtil.encrypt(data);
        redisTemplate.opsForValue().set(key, encrypted, 30, TimeUnit.MINUTES);
    }
    
    /**
     * 获取敏感数据
     */
    public String getSensitiveData(String key) {
        String encrypted = redisTemplate.opsForValue().get(key);
        return encrypted != null ? aesUtil.decrypt(encrypted) : null;
    }
}
```

## 总结

| 功能 | 核心方案 | 关键点 |
|------|---------|--------|
| 延时队列 | Sorted Set / Redisson | 时间精度、消费机制 |
| 幂等控制 | Token / 唯一索引 / 状态机 | 选择合适的方案 |
| 数据安全 | 加密 + 脱敏 + 访问控制 | 分层保护 |
