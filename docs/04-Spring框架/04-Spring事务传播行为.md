# Spring 事务传播行为详解

## 概述

事务传播行为（Propagation Behavior）定义了当一个事务方法被另一个事务方法调用时，事务应该如何传播。

## 七种传播行为

```
┌─────────────────────────────────────────────────────────────────┐
│                      事务传播行为对比表                          │
├───────────────┬─────────────────────────────────────────────────┤
│ 传播行为       │ 说明                                             │
├───────────────┼─────────────────────────────────────────────────┤
│ REQUIRED      │ 默认。当前有事务则加入，无则新建                   │
│ REQUIRES_NEW  │ 挂起当前事务，新建独立事务                         │
│ NESTED        │ 在当前事务中创建嵌套事务（savepoint）              │
│ SUPPORTS      │ 当前有事务则加入，无则以非事务执行                 │
│ NOT_SUPPORTED │ 挂起当前事务，以非事务执行                         │
│ MANDATORY     │ 当前必须有事务，否则抛出异常                       │
│ NEVER         │ 当前必须无事务，否则抛出异常                       │
└───────────────┴─────────────────────────────────────────────────┘
```

## 传播行为详解

### 1. REQUIRED（默认）

**定义**：如果当前存在事务，则加入该事务；如果当前没有事务，则创建一个新的事务。

**使用场景**：最常用的传播行为，适用于大多数业务场景。

```java
@Service
public class OrderService {
    
    @Transactional(propagation = Propagation.REQUIRED)
    public void createOrder(Order order) {
        // 如果外部有事务，加入外部事务
        // 如果外部无事务，新建事务
        orderDao.save(order);
    }
}
```

**执行流程**：
```
外部方法（有事务）
    ↓ 调用
内部方法（REQUIRED）
    ↓ 加入外部事务
外部方法回滚 → 内部方法也回滚
```

### 2. REQUIRES_NEW

**定义**：挂起当前事务，创建一个新的事务。新事务独立提交或回滚，不影响外部事务。

**使用场景**：
- 日志记录（无论业务是否成功，日志都要保存）
- 异步处理
- 需要独立提交的操作

```java
@Service
public class LogService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveLog(Log log) {
        // 独立事务，不受外部事务影响
        logDao.save(log);
    }
}
```

**执行流程**：
```
外部方法（事务A）
    ↓ 调用
    挂起事务A
    创建事务B（REQUIRES_NEW）
    ↓ 执行
    提交/回滚事务B
    恢复事务A
```

### 3. NESTED

**定义**：在当前事务中创建一个嵌套事务（使用 savepoint）。嵌套事务可以独立回滚，不影响外部事务。

**使用场景**：
- 部分操作可以失败，但整体继续
- 批量处理中的单条回滚

```java
@Service
public class BatchService {
    
    @Transactional(propagation = Propagation.NESTED)
    public void processItem(Item item) {
        // 创建 savepoint
        // 失败时回滚到 savepoint
        itemDao.save(item);
    }
}
```

**REQUIRES_NEW vs NESTED**：

| 特性 | REQUIRES_NEW | NESTED |
|------|--------------|--------|
| 事务 | 独立事务 | 嵌套事务（savepoint） |
| 回滚 | 完全独立 | 可回滚到 savepoint |
| 提交 | 独立提交 | 随外部事务一起提交 |
| 实现 | JTA 事务管理 | JDBC savepoint |

### 4. SUPPORTS

**定义**：如果当前存在事务，则加入该事务；如果当前没有事务，则以非事务方式执行。

**使用场景**：支持事务但不是必须的，如查询操作。

```java
@Service
public class UserService {
    
    @Transactional(propagation = Propagation.SUPPORTS)
    public User getUser(Long id) {
        // 有事务则加入，无事务则非事务执行
        return userDao.findById(id);
    }
}
```

### 5. NOT_SUPPORTED

**定义**：挂起当前事务，以非事务方式执行。

**使用场景**：
- 不需要事务的操作
- 避免长时间占用数据库连接

```java
@Service
public class ReportService {
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void generateReport() {
        // 以非事务方式执行，提高性能
        // 大数据量查询等
    }
}
```

### 6. MANDATORY

**定义**：如果当前存在事务，则加入该事务；如果当前没有事务，则抛出异常。

**使用场景**：必须在事务中执行的操作。

```java
@Service
public class AccountService {
    
    @Transactional(propagation = Propagation.MANDATORY)
    public void deductBalance(Long accountId, BigDecimal amount) {
        // 必须在事务中执行
        accountDao.deduct(accountId, amount);
    }
}
```

### 7. NEVER

**定义**：如果当前存在事务，则抛出异常；如果当前没有事务，则以非事务方式执行。

**使用场景**：
- 绝对不能在事务中执行的操作
- 防止事务嵌套

```java
@Service
public class RemoteService {
    
    @Transactional(propagation = Propagation.NEVER)
    public void callRemoteApi() {
        // 不能在事务中执行
        // 避免长时间占用连接
        httpClient.call();
    }
}
```

## 事务隔离级别

```
┌─────────────────────────────────────────────────────────────┐
│                    事务隔离级别                              │
├───────────────────┬─────────────────────────────────────────┤
│ 隔离级别           │ 说明                                     │
├───────────────────┼─────────────────────────────────────────┤
│ DEFAULT           │ 使用数据库默认隔离级别                    │
│ READ_UNCOMMITTED  │ 读未提交（可能出现脏读）                  │
│ READ_COMMITTED    │ 读已提交（Oracle默认）                    │
│ REPEATABLE_READ   │ 可重复读（MySQL默认）                     │
│ SERIALIZABLE      │ 串行化（最高隔离级别）                    │
└───────────────────┴─────────────────────────────────────────┘
```

### 隔离级别问题

| 问题 | 说明 | 隔离级别解决 |
|------|------|-------------|
| 脏读 | 读到未提交的数据 | READ_COMMITTED+ |
| 不可重复读 | 同一事务两次读取结果不同 | REPEATABLE_READ+ |
| 幻读 | 同一事务两次查询行数不同 | SERIALIZABLE |

## 事务失效场景

### 1. 非 public 方法

```java
@Service
public class UserService {
    
    @Transactional  // ❌ 不生效
    private void privateMethod() {
        // ...
    }
    
    @Transactional  // ✓ 生效
    public void publicMethod() {
        // ...
    }
}
```

### 2. 同类内部调用

```java
@Service
public class UserService {
    
    public void outerMethod() {
        // ❌ this 调用，不经过代理，事务不生效
        this.innerMethod();
    }
    
    @Transactional
    public void innerMethod() {
        // ...
    }
}
```

**解决方案**：

```java
@Service
public class UserService {
    
    @Autowired
    private ApplicationContext context;
    
    public void outerMethod() {
        // ✓ 获取代理对象调用
        UserService proxy = context.getBean(UserService.class);
        proxy.innerMethod();
    }
    
    @Transactional
    public void innerMethod() {
        // ...
    }
}
```

### 3. 异常被捕获

```java
@Service
public class UserService {
    
    @Transactional
    public void createUser(User user) {
        userDao.save(user);
        try {
            // 业务逻辑
            riskyOperation();
        } catch (Exception e) {
            // ❌ 异常被捕获，事务不会回滚
            log.error("Error", e);
        }
    }
}
```

**解决方案**：

```java
@Service
public class UserService {
    
    @Transactional
    public void createUser(User user) throws Exception {
        userDao.save(user);
        try {
            riskyOperation();
        } catch (Exception e) {
            log.error("Error", e);
            // ✓ 重新抛出异常
            throw e;
        }
    }
}
```

### 4. 异常类型不匹配

```java
@Service
public class UserService {
    
    // ❌ checked exception 默认不回滚
    @Transactional
    public void createUser(User user) throws IOException {
        userDao.save(user);
        throw new IOException("IO error");
    }
    
    // ✓ 指定 rollbackFor
    @Transactional(rollbackFor = Exception.class)
    public void createUser2(User user) throws IOException {
        userDao.save(user);
        throw new IOException("IO error");
    }
}
```

### 5. 数据库引擎不支持

```sql
-- MyISAM 引擎不支持事务
CREATE TABLE user (...) ENGINE=MyISAM;

-- ✓ InnoDB 引擎支持事务
CREATE TABLE user (...) ENGINE=InnoDB;
```

## 最佳实践

### 1. 事务方法设计

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderDao orderDao;
    
    @Autowired
    private InventoryService inventoryService;
    
    @Autowired
    private PaymentService paymentService;
    
    /**
     * 主业务方法：创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(CreateOrderRequest request) {
        // 1. 创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        orderDao.save(order);
        
        // 2. 扣减库存（同事务）
        inventoryService.deduct(request.getSkuId(), request.getQuantity());
        
        // 3. 支付（同事务）
        paymentService.pay(order.getId(), request.getAmount());
        
        return order;
    }
}
```

### 2. 日志记录（REQUIRES_NEW）

```java
@Service
public class LogService {
    
    @Autowired
    private LogDao logDao;
    
    /**
     * 记录操作日志 - 独立事务
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOperationLog(String operation, String detail) {
        OperationLog log = new OperationLog();
        log.setOperation(operation);
        log.setDetail(detail);
        log.setCreateTime(new Date());
        logDao.save(log);
    }
}
```

### 3. 批量处理（REQUIRES_NEW）

```java
@Service
public class BatchService {
    
    @Autowired
    private BatchService self;
    
    /**
     * 批量处理：单条失败不影响其他
     */
    public void batchProcess(List<Item> items) {
        for (Item item : items) {
            try {
                // 每条独立事务
                self.processSingle(item);
            } catch (Exception e) {
                log.error("处理失败: {}", item.getId(), e);
                // 继续处理下一条
            }
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingle(Item item) {
        // 处理单条
    }
}
```

### 4. 只读事务（查询优化）

```java
@Service
public class QueryService {
    
    /**
     * 只读查询
     */
    @Transactional(readOnly = true)
    public List<User> listUsers(QueryParam param) {
        // readOnly = true 时：
        // 1. Spring 不会刷新持久化上下文
        // 2. 某些数据库可以优化查询
        return userDao.findAll(param);
    }
}
```

### 5. 事务超时设置

```java
@Service
public class LongRunningService {
    
    /**
     * 设置事务超时时间（秒）
     */
    @Transactional(timeout = 30)
    public void longRunningOperation() {
        // 如果执行超过30秒，事务将回滚
    }
}
```

## 常见面试题

**问题 1:REQUIRED 和 REQUIRES_NEW 的区别？**

**A**: 
- **REQUIRED**：加入当前事务，同成功或同失败
- **REQUIRES_NEW**：挂起当前事务，创建独立事务，独立提交/回滚

**问题 2:NESTED 和 REQUIRES_NEW 的区别？**

**A**:
- **NESTED**：嵌套事务，使用 savepoint，随外部事务一起提交
- **REQUIRES_NEW**：独立事务，完全独立提交/回滚

**问题 3：事务失效的常见原因？**

**A**:
1. 非 public 方法
2. 同类内部调用（this 调用）
3. 异常被捕获未抛出
4. 异常类型不匹配（checked exception）
5. 数据库引擎不支持

**问题 4：如何解决同类内部调用事务失效？**

**A**:
1. 注入自身代理对象调用
2. 使用 `AopContext.currentProxy()`
3. 拆分到另一个 Service

**问题 5：Spring事务的实现原理？**

**A**:
Spring 事务基于 AOP 实现：
1. 使用 `@EnableTransactionManagement` 开启事务管理
2. Spring 创建代理对象（JDK 动态代理或 CGLIB）
3. 方法调用时，TransactionInterceptor 拦截
4. 根据传播行为决定加入现有事务或创建新事务
5. 方法执行完毕，根据异常决定提交或回滚
