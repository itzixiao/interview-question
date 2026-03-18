# Spring事务完全指南 - 失效场景 + 传播行为 + 隔离级别

## 一、Spring事务基础

### 1.1 什么是事务？

事务是数据库操作的最小工作单元，必须满足 **ACID** 特性：

- **原子性（Atomicity）**：事务是一个不可分割的工作单位，要么全部成功，要么全部失败
- **一致性（Consistency）**：事务执行前后，数据库从一个一致性状态变到另一个一致性状态
- **隔离性（Isolation）**：多个事务并发执行时，彼此互不干扰
- **持久性（Durability）**：事务一旦提交，对数据的改变是永久的

### 1.2 Spring事务管理方式

#### 声明式事务（推荐）

```java
@Transactional(
    rollbackFor = Exception.class,  // 指定回滚异常类型
    timeout = 30,                    // 超时时间（秒）
    readOnly = false,                // 是否只读
    isolation = Isolation.DEFAULT    // 隔离级别
)
public void createOrder() {
    // 业务逻辑
}
```

**优点：**

- ✅ 非侵入式，业务代码与事务管理分离
- ✅ 配置简单，易于维护
- ✅ 基于 AOP 原理，自动代理

**缺点：**

- ❌ 只能用于 public 方法
- ❌ 自调用会失效
- ❌ 灵活性相对较低

#### 编程式事务

```java
@Autowired
private TransactionTemplate transactionTemplate;

public void createUser(String name) {
    transactionTemplate.execute(status -> {
        try {
            jdbcTemplate.update("INSERT INTO user (name) VALUES (?)", name);
            return true;
        } catch (Exception e) {
            status.setRollbackOnly();  // 手动标记回滚
            return false;
        }
    });
}
```

**优点：**

- ✅ 灵活，可以精确控制事务边界
- ✅ 不受方法访问权限限制
- ✅ 可以在代码中动态决定事务行为

**缺点：**

- ❌ 侵入式，业务代码与事务管理耦合
- ❌ 代码冗长，维护成本高

---

## 二、Spring事务失效的 8 大场景

### 场景 1：非 public 方法

```java
// ❌ 错误示范
@Transactional
private void privateMethod() {
    // 事务不会生效
}

// ✓ 正确做法
@Transactional
public void publicMethod() {
    // 事务生效
}
```

**原因分析：**
Spring AOP 基于动态代理实现，只能代理 public 方法。非 public 方法无法被代理，因此@Transactional 注解不会生效。

**解决方案：**
将方法改为 public 访问级别。

### 场景 2：自调用问题（最常见）

```java
@Service
public class UserService {
    
    public void outerMethod() {
        // ❌ this 调用不走代理，事务失效
        this.innerMethod();
    }
    
    @Transactional
    public void innerMethod() {
        // 业务逻辑
    }
}
```

**原因分析：**
在同一个类中，通过 `this` 调用内部方法时，不会经过 Spring 的代理对象，直接调用了目标对象的方法，导致@Transactional 不生效。

**解决方案：**

**方案 1：注入自身代理对象**

```java
@Service
public class UserService {
    
    @Autowired
    @Lazy
    private UserService self;  // 注入自身代理
    
    public void outerMethod() {
        // ✓ 通过代理对象调用，事务生效
        self.innerMethod();
    }
    
    @Transactional
    public void innerMethod() {
        // 业务逻辑
    }
}
```

**方案 2：使用 AopContext**

```java
@Service
public class UserService {
    
    public void outerMethod() {
        // ✓ 通过 AopContext 获取当前代理对象
        ((UserService) AopContext.currentProxy()).innerMethod();
    }
    
    @Transactional
    public void innerMethod() {
        // 业务逻辑
    }
}
// 需要配置：@EnableAspectJAutoProxy(exposeProxy = true)
```

**方案 3：拆分到另一个 Service**

```java
@Service
public class UserService {
    @Autowired
    private InnerService innerService;
    
    public void outerMethod() {
        innerService.innerMethod();  // ✓ 跨类调用，事务生效
    }
}

@Service
public class InnerService {
    @Transactional
    public void innerMethod() {
        // 业务逻辑
    }
}
```

### 场景 3：异常被捕获未抛出

```java
@Transactional
public void businessMethod() {
    try {
        // 业务逻辑
        throw new RuntimeException("业务异常");
    } catch (Exception e) {
        // ❌ 异常被吞掉，事务不会回滚
        log.error("业务失败", e);
    }
}
```

**原因分析：**
Spring事务的回滚机制依赖于异常抛出。如果异常被捕获且不再抛出，Spring 会认为方法执行成功，从而提交事务。

**解决方案：**

```java
@Transactional
public void businessMethod() {
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("业务失败", e);
        throw e;  // ✓ 继续抛出异常
    }
}
```

或者手动标记回滚：

```java
@Transactional
public void businessMethod() {
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("业务失败", e);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
```

### 场景 4：checked Exception 默认不回滚

```java
@Transactional
public void businessMethod() throws IOException {
    // ❌ 默认情况下，checked Exception 不会触发回滚
    throw new IOException("IO 异常");
}
```

**原因分析：**
Spring 默认只回滚 RuntimeException 和 Error，不检查 checked Exception。这是遵循 EJB 规范的设计。

**解决方案：**

```java
@Transactional(rollbackFor = Exception.class)  // ✓ 指定回滚所有异常
public void businessMethod() throws IOException {
    throw new IOException("IO 异常");
}
```

### 场景 5：数据库引擎不支持事务

```sql
-- ❌ MySQL MyISAM 引擎不支持事务
CREATE TABLE user_myisam (
    id INT PRIMARY KEY,
    name VARCHAR(50)
) ENGINE=MyISAM;

-- ✓ 使用 InnoDB 引擎
CREATE TABLE user_innodb (
    id INT PRIMARY KEY,
    name VARCHAR(50)
) ENGINE=InnoDB;
```

**解决方案：**
确保使用支持事务的数据库引擎（如 MySQL InnoDB）。

### 场景 6：异步方法

```java
@Transactional
public void asyncMethod() {
    new Thread(() -> {
        // ❌ 新线程不在 Spring事务管理中
        jdbcTemplate.update("INSERT INTO user (name) VALUES (?)", "AsyncUser");
    }).start();
}
```

**原因分析：**
异步线程不在 Spring 容器管理范围内，无法享受事务管理。

**解决方案：**
使用 Spring 的@Async 注解，并配置事务管理器：

```java
@Async
@Transactional
public void asyncMethod() {
    // ✓ 在独立的线程池中执行，有独立的事务
}
```

### 场景 7：传播行为配置错误

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void businessMethod() {
    // ❌ 以非事务方式执行，即使外部有事务也会被挂起
}
```

**解决方案：**
理解各种传播行为的含义，选择合适的传播行为。

### 场景 8：多数据源未指定事务管理器

```java
// ❌ 未指定使用哪个数据源的事务管理器
@Transactional
public void businessMethod() {
    // 使用默认事务管理器
}

// ✓ 明确指定事务管理器
@Transactional(transactionManager = "orderTransactionManager")
public void orderBusinessMethod() {
    // 使用订单数据源的事务
}
```

---

## 三、Spring事务传播行为详解

### 3.1 REQUIRED（默认）

**规则：** 当前有事务则加入，无则新建

**适用场景：** 大多数业务场景，保证操作的原子性

```java
@Transactional(propagation = Propagation.REQUIRED)
public void createOrder() {
    // 1. 创建订单
    orderMapper.insert(order);
    
    // 2. 扣减库存（加入同一事务）
    inventoryMapper.reduce();
    
    // 一个失败，全部回滚
}
```

### 3.2 REQUIRES_NEW

**规则：** 挂起当前事务，新建独立事务

**适用场景：** 日志记录、审计操作等需要独立提交的场景

```java
@Transactional(propagation = Propagation.REQUIRED)
public void createOrder() {
    orderMapper.insert(order);
    
    try {
        logService.logSuccess("订单创建成功");  // REQUIRES_NEW
    } catch (Exception e) {
        // 日志失败不影响订单
    }
}

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logSuccess(String msg) {
    // ✓ 独立事务，即使订单回滚，日志也会保留
    logMapper.insert(msg);
}
```

### 3.3 NESTED

**规则：** 在当前事务中创建嵌套事务（savepoint）

**适用场景：** 部分回滚需求

```java
@Transactional(propagation = Propagation.REQUIRED)
public void batchProcess() {
    for (Item item : items) {
        try {
            processSingleItem(item);  // NESTED
        } catch (Exception e) {
            // 单条失败不影响其他
        }
    }
}

@Transactional(propagation = Propagation.NESTED)
public void processSingleItem(Item item) {
    // 失败只回滚到 savepoint
    itemMapper.insert(item);
}
```

**注意事项：**

- 仅 DataSourceTransactionManager 支持
- 基于 JDBC savepoint 实现
- 外层回滚，内层也会回滚

### 3.4 SUPPORTS

**规则：** 当前有事务则加入，无则以非事务执行

**适用场景：** 查询操作

```java
@Transactional(propagation = Propagation.SUPPORTS)
public User getUserById(Long id) {
    // 有事务就加入，没有也无所谓
    return userMapper.selectById(id);
}
```

### 3.5 NOT_SUPPORTED

**规则：** 挂起当前事务，以非事务执行

**适用场景：** 不需要事务的操作（如文件 IO、批量导入）

```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void exportExcel() {
    // ✓ 以非事务方式执行，提高性能
    // 文件导出操作
}
```

### 3.6 MANDATORY

**规则：** 当前必须有事务，否则抛出异常

**适用场景：** 强制要求事务的核心业务

```java
@Transactional(propagation = Propagation.MANDATORY)
public void criticalBusiness() {
    // 必须在事务中执行
}
```

### 3.7 NEVER

**规则：** 当前必须无事务，否则抛出异常

**适用场景：** 绝对不能有事务的操作

```java
@Transactional(propagation = Propagation.NEVER)
public void noTransactionMethod() {
    // 必须在非事务环境下执行
}
```

### 3.8 传播行为对比表

| 传播行为              | 外部有事务 | 外部无事务 | 典型场景  |
|-------------------|-------|-------|-------|
| **REQUIRED**      | 加入    | 新建    | 默认选择  |
| **REQUIRES_NEW**  | 挂起，新建 | 新建    | 日志、审计 |
| **NESTED**        | 嵌套事务  | 新建    | 部分回滚  |
| **SUPPORTS**      | 加入    | 非事务   | 查询操作  |
| **NOT_SUPPORTED** | 挂起    | 非事务   | 文件 IO |
| **MANDATORY**     | 加入    | 抛异常   | 核心业务  |
| **NEVER**         | 抛异常   | 非事务   | 特殊操作  |

---

## 四、事务隔离级别

### 4.1 SQL 标准隔离级别

从低到高：

1. **READ_UNCOMMITTED（读未提交）**
    - 可能脏读、不可重复读、幻读
    - 性能最好
    - 适用：统计分析等对一致性要求不高的场景

2. **READ_COMMITTED（读已提交）**
    - 避免脏读
    - 可能不可重复读、幻读
    - Oracle 默认隔离级别
    - 适用：大多数业务场景

3. **REPEATABLE_READ（可重复读）**
    - 避免脏读、不可重复读
    - 可能幻读（InnoDB 通过 Next-Key Lock 基本解决）
    - MySQL 默认隔离级别
    - 适用：推荐选择

4. **SERIALIZABLE（串行化）**
    - 避免所有并发问题
    - 强制事务串行执行
    - 性能最差
    - 适用：金融等对一致性要求极高的场景

### 4.2 并发问题详解

#### 脏读（Dirty Read）

```
事务 A                          事务 B
----------------------------------------
读取 (余额=1000)                
                              修改 (余额=800)
读取 (余额=800)  ← 脏读        
                              回滚
实际余额还是 1000，A 读到的是脏数据
```

#### 不可重复读（Non-repeatable Read）

```
事务 A                          事务 B
----------------------------------------
读取 (余额=1000)                
                              修改 (余额=800)
                              提交
再次读取 (余额=800)  ← 不一致
```

#### 幻读（Phantom Read）

```
事务 A                          事务 B
----------------------------------------
查询 (SELECT * FROM user WHERE age>10) 
返回 3 条记录                    
                              插入 (age=15)
                              提交
再次查询 (SELECT * FROM user WHERE age>10)
返回 4 条记录  ← 出现"幻影"
```

### 4.3 MySQL InnoDB 的特殊优化

MySQL InnoDB 在 REPEATABLE_READ 隔离级别下，通过以下技术基本解决了幻读问题：

1. **MVCC（多版本并发控制）**
    - 读操作不加锁，读取历史版本
    - 提高并发性能

2. **Next-Key Lock**
    - 记录锁 + 间隙锁
    - 防止其他事务在间隙中插入

```sql
-- 查看当前隔离级别
SELECT @@transaction_isolation;

-- 设置会话隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;
```

### 4.4 隔离级别选择建议

```java
// 互联网应用：使用默认 REPEATABLE_READ
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void business() {}

// 金融系统：考虑 SERIALIZABLE
@Transactional(isolation = Isolation.SERIALIZABLE)
public void transfer() {}

// 数据分析：可使用 READ_UNCOMMITTED
@Transactional(isolation = Isolation.READ_UNCOMMITTED, readOnly = true)
public void analyze() {}
```

---

## 五、事务最佳实践

### 5.1 声明式事务配置

```java
@Transactional(
    rollbackFor = Exception.class,      // ✓ 指定回滚所有异常
    timeout = 30,                        // ✓ 设置超时时间
    readOnly = false,                    // ✓ 明确是否只读
    isolation = Isolation.DEFAULT,       // ✓ 使用数据库默认隔离级别
    propagation = Propagation.REQUIRED   // ✓ 默认传播行为
)
public void businessMethod() {
    // 业务逻辑
}
```

### 5.2 避免大事务

```java
// ❌ 错误示范：大事务包含不必要的操作
@Transactional
public void createOrder() {
    validateParams();      // 参数校验（非事务）
    orderMapper.insert();  // 保存订单（事务）
    sendEmail();           // 发送邮件（非事务）
    updateStats();         // 更新统计（事务）
    // 整个方法都在事务中，持有锁时间过长
}

// ✓ 正确示范：拆分事务
public void createOrder() {
    validateParams();          // 非事务
    saveOrder();               // @Transactional
    sendEmail();               // 非事务
    updateStatistics();        // @Transactional
}

@Transactional
public void saveOrder() {
    orderMapper.insert();
}

@Transactional
public void updateStatistics() {
    statsMapper.update();
}
```

### 5.3 只读事务优化

```java
@Transactional(readOnly = true)
public List<User> listUsers() {
    // ✓ readOnly=true 时：
    // 1. Spring 不会刷新持久化上下文
    // 2. 某些数据库可以优化查询（如 MySQL 不加锁）
    // 3. 提高并发性能
    return userMapper.selectAll();
}
```

### 5.4 日志记录使用 REQUIRES_NEW

```java
@Service
public class OrderService {
    
    @Autowired
    private LogService logService;
    
    @Transactional
    public void createOrder() {
        try {
            // 业务逻辑
            orderMapper.insert(order);
            
            // ✓ 记录成功日志（独立事务）
            logService.logSuccess("订单创建成功");
            
        } catch (Exception e) {
            // ✓ 记录失败日志（独立事务）
            logService.logError("订单创建失败：" + e.getMessage());
            throw e;
        }
    }
}

@Service
public class LogService {
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(String msg) {
        logMapper.insert(msg, "SUCCESS");
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logError(String msg) {
        logMapper.insert(msg, "ERROR");
    }
}
```

### 5.5 批量处理分批提交

```java
@Transactional
public void batchInsert(List<Data> dataList) {
    int batchSize = 100;
    
    for (int i = 0; i < dataList.size(); i += batchSize) {
        List<Data> batch = dataList.subList(
            i, Math.min(i + batchSize, dataList.size()));
        
        for (Data data : batch) {
            dataMapper.insert(data);
        }
        
        // ✓ 每批提交一次，减少锁持有时间
        if ((i / batchSize) % 10 == 0) {
            // 可以考虑手动刷新
        }
    }
}
```

### 5.6 事务超时设置

```java
// 查询操作：短超时
@Transactional(timeout = 5)
public User getUserById(Long id) {}

// 简单业务：中超时
@Transactional(timeout = 30)
public void createOrder() {}

// 复杂业务：长超时
@Transactional(timeout = 60)
public void complexProcess() {}

// 批处理：根据数据量设置
@Transactional(timeout = 300)
public void batchProcess() {}
```

---

## 六、高频面试题

**问题 1：Spring事务失效的常见场景有哪些？**

**答：**

1. **非 public 方法** - Spring AOP 只能代理 public 方法
2. **自调用问题** - this 调用不走代理
3. **异常被捕获未抛出** - 事务回滚依赖异常
4. **异常类型不匹配** - 默认只回滚 RuntimeException
5. **数据库引擎不支持** - 如 MySQL MyISAM
6. **异步方法** - 新线程不在事务管理中
7. **传播行为配置错误** - 如 NOT_SUPPORTED
8. **多数据源未指定事务管理器**

**问题 2：REQUIRED 和 REQUIRES_NEW 有什么区别？**

**答：**

| 对比项   | REQUIRED | REQUIRES_NEW |
|-------|----------|--------------|
| 事务关系  | 加入当前事务   | 创建新事务        |
| 回滚影响  | 同成功或同失败  | 内层回滚不影响外层    |
| 锁持有时间 | 整个事务期间   | 仅内层事务期间      |
| 性能    | 较好       | 较差           |
| 使用场景  | 大多数业务    | 日志、审计        |

**问题 3：NESTED 和 REQUIRES_NEW 有什么区别？**

**答：**

1. **事务关系不同**
    - NESTED: 嵌套事务，是外层事务的一部分（savepoint）
    - REQUIRES_NEW: 全新事务，完全独立于外层

2. **回滚影响不同**
    - NESTED: 外层回滚，内层也回滚
    - REQUIRES_NEW: 外层回滚，内层已提交的不受影响

3. **实现机制不同**
    - NESTED: 基于 JDBC savepoint 实现
    - REQUIRES_NEW: 挂起当前事务，创建新事务

4. **支持程度不同**
    - NESTED: 仅 DataSourceTransactionManager 支持
    - REQUIRES_NEW: 所有事务管理器都支持

**问题 4：如何正确地在事务中记录日志？**

**答：**

```java
@Service
public class BusinessService {
    
    @Autowired
    private LogService logService;
    
    @Transactional
    public void business() {
        try {
            // 业务逻辑
            logService.logSuccess("成功");  // ✓ REQUIRES_NEW
        } catch (Exception e) {
            logService.logError("失败");   // ✓ REQUIRES_NEW
            throw e;
        }
    }
}

@Service
public class LogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(String msg) {
        // 独立事务，不受业务回滚影响
    }
}
```

**关键点：**

1. 日志服务使用 REQUIRES_NEW 传播行为
2. 日志操作在 try-catch 块之外调用
3. 确保日志服务是独立的 Bean

**问题 5：事务隔离级别有哪些？MySQL 默认是什么？**

**答：**

SQL 标准定义的隔离级别（从低到高）：

1. **READ_UNCOMMITTED** - 读未提交（可能脏读）
2. **READ_COMMITTED** - 读已提交（避免脏读，Oracle 默认）
3. **REPEATABLE_READ** - 可重复读（MySQL 默认）
4. **SERIALIZABLE** - 串行化（避免幻读）

**MySQL InnoDB 的特殊优化：**

- 在 REPEATABLE_READ 隔离级别下，通过 MVCC+Next-Key Lock 基本解决了幻读问题
- 因此 MySQL 默认使用 REPEATABLE_READ 而非 READ_COMMITTED

**问题 6：什么是大事务？有什么危害？如何优化？**

**答：**

**大事务定义：**

- 执行时间长
- 操作数据量大
- 持有锁时间久

**危害：**

1. 锁竞争加剧，并发性能下降
2. 可能导致死锁
3. 数据库连接占用时间长
4. 回滚时间长，影响系统可用性

**优化方案：**

1. **事务拆分** - 将大事务拆分为小事务
2. **异步处理** - 非核心操作异步执行
3. **批量操作分批提交** - 减少锁持有时间
4. **只读事务优化** - 使用 readOnly=true

**问题 7：@Transactional 的原理是什么？**

**答：**

**核心原理：** AOP（面向切面编程）+ 动态代理

**执行流程：**

```
客户端调用
  ↓
代理对象（JDK/CGLIB）
  ↓
TransactionInterceptor.invoke()
  ↓
PlatformTransactionManager.getTransaction()
  ↓
执行目标方法
  ↓
成功：commit()
异常：rollback()
```

**关键源码：**

```java
// TransactionInterceptor.java
@Override
public Object invoke(MethodInvocation invocation) throws Throwable {
    // 1. 获取事务属性
    TransactionAttribute txAttr = determineTransactionAttribute();
    
    // 2. 获取事务管理器
    PlatformTransactionManager tm = getTransactionManager(txAttr);
    
    // 3. 创建事务
    TransactionStatus status = tm.getTransaction(txAttr);
    
    try {
        // 4. 执行目标方法
        Object result = invocation.proceed();
        
        // 5. 提交事务
        tm.commit(status);
        return result;
    } catch (Throwable ex) {
        // 6. 回滚事务
        completeTransactionAfterThrowing(status, ex);
        throw ex;
    }
}
```

**问题 8：如何手动回滚事务？**

**答：**

**方法 1: 抛出异常（推荐）**

```java
@Transactional
public void method() {
    if (error) {
        throw new RuntimeException("业务失败"); // ✓ 自动回滚
    }
}
```

**方法 2: 手动设置 rollbackOnly**

```java
@Transactional
public void method() {
    try {
        // 业务逻辑
    } catch (Exception e) {
        TransactionAspectSupport.currentTransactionStatus()
            .setRollbackOnly(); // ✓ 手动回滚
        throw e;
    }
}
```

**方法 3: 编程式事务**

```java
transactionTemplate.execute(status -> {
    try {
        // 业务逻辑
        return result;
    } catch (Exception e) {
        status.setRollbackOnly(); // ✓ 标记回滚
        throw e;
    }
});
```

---

**作者：** itzixiao  
**版本：** 1.0  
**最后更新：** 2026-03-08
