# Spring事务传播机制详解 - 示例代码

## 📖 模块介绍

本模块用于演示 Spring事务的 7 种传播行为，通过完整的业务场景和可运行的代码，帮助你深入理解事务传播机制。

### 什么是事务传播行为？

当事务方法被另一个事务方法调用时，必须指定事务应该如何传播。例如，两个事务方法在同一个类中，或者一个类的方法调用另一个类的方法时，事务的行为。

Spring 定义了 7 种事务传播行为：

| 传播行为              | 说明                      | 使用场景        |
|-------------------|-------------------------|-------------|
| **REQUIRED**      | 默认。当前有事务则加入，无则新建        | 大多数业务场景     |
| **REQUIRES_NEW**  | 挂起当前事务，新建独立事务           | 日志记录、审计操作   |
| **NESTED**        | 在当前事务中创建嵌套事务（savepoint） | 批量处理、部分回滚   |
| **SUPPORTS**      | 当前有事务则加入，无则以非事务执行       | 查询操作        |
| **NOT_SUPPORTED** | 挂起当前事务，以非事务执行           | 文件 IO、批量导入  |
| **MANDATORY**     | 当前必须有事务，否则抛出异常          | 强制要求事务的核心业务 |
| **NEVER**         | 当前必须无事务，否则抛出异常          | 绝对不能有事务的操作  |

---

## 🏗️ 项目结构

```
interview-transaction-demo/
├── src/main/java/cn/itzixiao/interview/transaction/
│   ├── TransactionDemoApplication.java    # 启动类
│   ├── controller/
│   │   └── TransactionController.java     # 控制器（测试接口）
│   ├── service/
│   │   ├── OrderService.java              # 订单服务（核心演示）
│   │   ├── AccountService.java            # 账户服务
│   │   └── LogService.java                # 日志服务（REQUIRES_NEW）
│   ├── mapper/
│   │   ├── OrderMapper.java
│   │   ├── AccountMapper.java
│   │   ├── InventoryMapper.java
│   │   └── OperationLogMapper.java
│   └── entity/
│       ├── Order.java
│       ├── Account.java
│       ├── Inventory.java
│       └── OperationLog.java
├── src/main/resources/
│   ├── application.yml                    # 配置文件
│   └── schema.sql                         # 数据库表结构
└── pom.xml
```

---

## 🚀 快速开始

### 1. 环境准备

- JDK 1.8+
- MySQL 5.7+
- Maven 3.6+

### 2. 初始化数据库

执行 `schema.sql` 脚本创建数据库和表：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

### 3. 修改配置

编辑 `application.yml`，修改数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/interview
    username: root
    password: your_password
```

### 4. 启动应用

```bash
mvn spring-boot:run
```

启动成功后会显示：

```
===========================================
    事务传播机制演示服务启动成功！
    访问地址：http://localhost:8084
    API 文档：http://localhost:8084/api/transaction/*
===========================================
```

---

## 📝 API 接口说明

### 基础测试接口

#### 1. REQUIRED 传播行为（默认）

```bash
GET http://localhost:8084/api/transaction/required?userId=1&amount=100
```

**说明：**

- 当前有事务则加入，无则新建
- 所有操作在同一事务中，要么全部成功，要么全部失败
- 适用于大多数业务场景

**测试场景：**

- `amount=100`：正常情况，订单创建成功
- `amount=1500`：金额超过 1000，抛出异常，整个事务回滚

#### 2. REQUIRES_NEW 传播行为

```bash
GET http://localhost:8084/api/transaction/requires-new?userId=1&amount=50
```

**说明：**

- 挂起当前事务，创建新的独立事务
- 新事务的提交/回滚不影响外部事务
- 适用于日志记录、审计操作

**测试场景：**

- 即使订单事务回滚，日志也会独立保存

#### 3. NESTED 传播行为

```bash
GET http://localhost:8084/api/transaction/nested?userId=1
```

**说明：**

- 在当前事务中创建嵌套事务（使用 savepoint）
- 嵌套事务可以独立回滚，不影响外层
- 适用于批量处理中的单条回滚

**测试场景：**

- 批量创建 3 个订单，单个失败不影响其他

#### 4. SUPPORTS 传播行为

```bash
GET http://localhost:8084/api/transaction/supports?userId=1
```

**说明：**

- 当前有事务则加入，无则以非事务执行
- 适用于查询操作

#### 5. MANDATORY 传播行为

```bash
GET http://localhost:8084/api/transaction/mandatory?userId=1&amount=50
```

**说明：**

- 必须在事务中执行，否则抛出异常
- 适用于强制要求事务的核心业务

#### 6. NEVER 传播行为

```bash
GET http://localhost:8084/api/transaction/never?userId=1
```

**说明：**

- 必须在非事务环境中执行，否则抛出异常
- 适用于绝对不能有事务的操作

#### 7. NOT_SUPPORTED 传播行为

```bash
GET http://localhost:8084/api/transaction/not-supported?userId=1
```

**说明：**

- 挂起当前事务，以非事务执行
- 适用于不需要事务的操作（如文件 IO）

### 综合测试接口

#### 复杂业务场景演示

```bash
GET http://localhost:8084/api/transaction/complex?userId=1&amount=200
```

**说明：**

- 结合多种传播行为的完整业务流程
- 包含：SUPPORTS（查询）、REQUIRED（订单）、REQUIRES_NEW（日志）

#### 直接测试日志服务

```bash
GET http://localhost:8084/api/transaction/log/success?operation=TEST&detail=SUCCESS
```

#### 查询账户余额

```bash
GET http://localhost:8084/api/transaction/account?userId=1
```

---

## 💡 核心代码示例

### 1. REQUIRED 传播行为

```java

@Service
public class OrderService {

    @Transactional(rollbackFor = Exception.class)
    public Order createOrderWithRequired(Long userId, BigDecimal amount) {
        // 1. 创建订单
        Order order = new Order();
        order.setOrderNo(UUID.randomUUID().toString());
        orderMapper.insert(order);

        // 2. 扣减账户余额（REQUIRED - 加入同一事务）
        accountService.depositWithRequired(userId, amount.negate());

        // 3. 记录日志（REQUIRED - 加入同一事务）
        logService.logWithRequired("CREATE_ORDER", "订单创建成功");

        // 模拟异常测试回滚
        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            throw new RuntimeException("金额超过限制");
        }

        return order;
    }
}
```

**执行流程：**

```
外部方法（事务 A）
    ↓ 调用
内部方法（REQUIRED）
    ↓ 加入事务 A
外部方法回滚 → 内部方法也回滚
```

### 2. REQUIRES_NEW 传播行为

```java

@Service
public class LogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(String operation, String detail) {
        OperationLog log = new OperationLog();
        log.setOperation(operation);
        log.setDetail(detail);
        log.setStatus("SUCCESS");
        operationLogMapper.insert(log);
        // 独立事务，不受外部事务影响
    }
}
```

**执行流程：**

```
外部方法（事务 A）
    ↓ 调用
    挂起事务 A
    创建事务 B（REQUIRES_NEW）
    ↓ 执行
    提交/回滚事务 B
    恢复事务 A
```

### 3. NESTED 传播行为

```java

@Service
public class OrderService {

    @Transactional(rollbackFor = Exception.class)
    public void batchCreateOrders(Long userId) {
        for (int i = 1; i <= 3; i++) {
            try {
                createSingleOrderWithNested(userId, new BigDecimal("100"));
            } catch (Exception e) {
                // 单个失败不影响其他
            }
        }
    }

    @Transactional(propagation = Propagation.NESTED)
    public Order createSingleOrderWithNested(Long userId, BigDecimal amount) {
        // 创建 savepoint
        // 失败时回滚到 savepoint
    }
}
```

**REQUIRES_NEW vs NESTED 对比：**

| 特性 | REQUIRES_NEW | NESTED          |
|----|--------------|-----------------|
| 事务 | 独立事务         | 嵌套事务（savepoint） |
| 回滚 | 完全独立         | 可回滚到 savepoint  |
| 提交 | 独立提交         | 随外部事务一起提交       |
| 实现 | JTA 事务管理     | JDBC savepoint  |

---

## 🔍 调试技巧

### 1. 查看事务日志

在 `application.yml` 中开启事务调试日志：

```yaml
logging:
  level:
    org.springframework.transaction: DEBUG
    cn.itzixiao.interview.transaction: DEBUG
```

### 2. 查看当前事务名称

```java
log.info("当前事务名称：{}",TransactionSynchronizationManager.getCurrentTransactionName());
```

### 3. 验证数据是否回滚

```sql
-- 查看账户余额
SELECT *
FROM t_account
WHERE user_id = 1;

-- 查看订单
SELECT *
FROM t_order
WHERE user_id = 1
ORDER BY create_time DESC;

-- 查看操作日志
SELECT *
FROM t_operation_log
ORDER BY create_time DESC;
```

---

## 🎯 实战场景

### 场景 1：订单创建 + 日志记录

```java

@Transactional(rollbackFor = Exception.class)
public Order createOrder(OrderDTO dto) {
    // 1. 创建订单（REQUIRED）
    Order order = createOrder(dto);

    // 2. 扣减库存（REQUIRED）
    inventoryService.reduce(dto.getSkuId(), dto.getQuantity());

    // 3. 记录日志（REQUIRES_NEW - 独立提交）
    logService.logSuccess("ORDER_CREATED", "订单：" + order.getId());

    return order;
}
```

**好处：**

- 订单失败时，库存回滚
- 但日志独立保存，便于追踪

### 场景 2：批量数据处理

```java

@Transactional(rollbackFor = Exception.class)
public void batchProcess(List<Data> dataList) {
    for (Data data : dataList) {
        try {
            processSingle(data);  // NESTED
        } catch (Exception e) {
            log.error("处理失败：{}", data.getId(), e);
        }
    }
}

@Transactional(propagation = Propagation.NESTED)
public void processSingle(Data data) {
    // 单个处理逻辑
    // 失败只回滚到 savepoint
}
```

### 场景 3：查询优化

```java

@Transactional(readOnly = true)
public List<User> listUsers() {
    // readOnly=true 优化：
    // 1. Spring 不会刷新持久化上下文
    // 2. 某些数据库可以优化查询
    return userDao.findAll();
}
```

---

## ❓ 常见面试题

### Q1: REQUIRED 和 REQUIRES_NEW 的区别？

**A:**

- **REQUIRED**：加入当前事务，同成功或同失败
- **REQUIRES_NEW**：挂起当前事务，创建独立事务，独立提交/回滚

### Q2: NESTED 和 REQUIRES_NEW 的区别？

**A:**

- **NESTED**：嵌套事务，使用 savepoint，随外部事务一起提交
- **REQUIRES_NEW**：独立事务，完全独立提交/回滚

### Q3: 事务失效的常见场景？

**A:**

1. 非 public 方法
2. 同类内部调用（this 调用）
3. 异常被捕获未抛出
4. 异常类型不匹配（checked exception）
5. 数据库引擎不支持

### Q4: 如何解决同类内部调用事务失效？

**A:**

1. 注入自身代理对象调用
2. 使用 `AopContext.currentProxy()`
3. 拆分到另一个 Service

---

## 📊 测试用例

### 测试 1：REQUIRED 传播行为 - 正常流程

```bash
# 1. 查看初始余额
GET /api/transaction/account?userId=1

# 2. 创建订单（金额 100）
GET /api/transaction/required?userId=1&amount=100

# 3. 查看最终余额（应该减少 100）
GET /api/transaction/account?userId=1
```

### 测试 2:REQUIRED 传播行为 - 异常回滚

```bash
# 1. 创建订单（金额 1500，会抛出异常）
GET /api/transaction/required?userId=1&amount=1500

# 2. 查看余额（应该不变，因为回滚了）
GET /api/transaction/account?userId=1

# 3. 查看订单表（应该没有新订单）
SELECT * FROM t_order ORDER BY id DESC LIMIT 1;
```

### 测试 3:REQUIRES_NEW - 日志独立提交

```bash
# 1. 创建订单并记录日志
GET /api/transaction/requires-new?userId=1&amount=50

# 2. 查看日志表（应该有日志记录）
SELECT * FROM t_operation_log ORDER BY id DESC LIMIT 1;
```

---

## 🛠️ 最佳实践

### 1. 事务方法设计

```java

@Transactional(rollbackFor = Exception.class)
public Order createOrder(CreateOrderRequest request) {
    // 1. 创建订单
    Order order = new Order();
    orderMapper.save(order);

    // 2. 扣减库存（同事务）
    inventoryService.deduct(request.getSkuId(), request.getQuantity());

    // 3. 支付（同事务）
    paymentService.pay(order.getId(), request.getAmount());

    return order;
}
```

### 2. 日志记录（REQUIRES_NEW）

```java

@Transactional(propagation = Propagation.REQUIRES_NEW)
public void saveOperationLog(String operation, String detail) {
    OperationLog log = new OperationLog();
    log.setOperation(operation);
    log.setDetail(detail);
    logDao.save(log);
}
```

### 3. 避免大事务

```java
// ❌ 错误示范
@Transactional
public void createOrder() {
    validateParams();      // 参数校验（非事务）
    orderMapper.insert();  // 保存订单（事务）
    sendEmail();           // 发送邮件（非事务）
    updateStats();         // 更新统计（事务）
}

// ✓ 正确示范
public void createOrder() {
    validateParams();          // 非事务
    saveOrder();               // @Transactional
    sendEmail();               // 非事务
    updateStatistics();        // @Transactional
}
```

---

## 📚 参考资料

- [Spring 官方文档 - Transaction Management](https://docs.spring.io/spring-framework/docs/current/reference/html/data-access.html#transaction)
- [Spring事务传播行为详解](../../docs/04-Spring框架/04-Spring事务传播行为.md)
- [Spring事务完全指南](../../docs/04-Spring框架/06-Spring事务完全指南.md)

---

## 📈 更新日志

### v1.0 - 2026-03-13

- ✅ 初始版本发布
- ✅ 支持 7 种事务传播行为演示
- ✅ 完整的业务场景示例
- ✅ 详细的 API 接口文档
- ✅ 配套数据库脚本

---

**作者：** itzixiao  
**最后更新：** 2026-03-13  
**问题反馈：** 欢迎提 Issue 或 PR
