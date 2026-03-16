# Java 异常详解

## 📚 目录

1. [异常体系结构](#异常体系结构)
2. [异常处理机制](#异常处理机制)
3. [throws vs throw](#throws-vs-throw)
4. [自定义异常](#自定义异常)
5. [try-with-resources](#try-with-resources)
6. [异常最佳实践](#异常最佳实践)
7. [高频面试题](#高频面试题)

---

## 异常体系结构

### Throwable 层次结构

```
Throwable
├── Error（错误）
│   ├── OutOfMemoryError          // 内存溢出
│   ├── StackOverflowError        // 栈溢出
│   └── VirtualMachineError       // JVM 错误
│
└── Exception（异常）
    ├── RuntimeException（非受检异常）
    │   ├── NullPointerException
    │   ├── ArrayIndexOutOfBoundsException
    │   ├── ClassCastException
    │   ├── IllegalArgumentException
    │   └── IllegalStateException
    │
    └── Checked Exception（受检异常）
        ├── IOException
        ├── SQLException
        ├── ClassNotFoundException
        └── InterruptedException
```

### Error vs Exception

| 特性     | Error             | Exception       |
|--------|-------------------|-----------------|
| **性质** | JVM 级别的严重错误       | 程序可以处理的异常       |
| **原因** | 系统资源耗尽、虚拟机错误      | 程序逻辑或外部资源问题     |
| **处理** | 不应尝试捕获            | 应该捕获并处理         |
| **示例** | OOM、StackOverflow | NPE、IOException |

### RuntimeException vs Checked Exception

| 特性       | RuntimeException | Checked Exception        |
|----------|------------------|--------------------------|
| **检查时机** | 运行时              | 编译时                      |
| **强制处理** | ❌ 不需要            | ✅ 必须处理                   |
| **常见类型** | NPE、数组越界         | IOException、SQLException |
| **设计意图** | 程序逻辑错误           | 可恢复的外部条件                 |

---

## 异常处理机制

### try-catch-finally 语法

```java
try {
    // 可能抛出异常的代码
    int result = divide(10, 0);
} catch (ArithmeticException e) {
    // 捕获特定异常
    System.out.println("算术错误：" + e.getMessage());
} catch (Exception e) {
    // 捕获其他异常
    System.out.println("其他错误：" + e.getMessage());
} finally {
    // 总是执行，用于清理资源
    System.out.println("释放资源");
}
```

### 执行流程

1. **正常情况**：try → finally → 返回
2. **异常情况**：try → catch → finally → 返回
3. **finally 不执行的情况**：
    - `System.exit()` 调用
    - 线程被杀死
    - JVM 崩溃

### 多个 catch 块的顺序

```java
try {
    // ...
} catch (FileNotFoundException e) {  // 子类在前
    e.printStackTrace();
} catch (IOException e) {            // 父类在后
    e.printStackTrace();
} catch (Exception e) {              // 最顶层
    e.printStackTrace();
}
```

⚠️ **注意**：catch 块必须从具体到抽象排序

---

## throws vs throw

### throws 关键字

在方法签名中**声明**可能抛出的异常：

```java
// 声明可能抛出 IOException
public void readFile() throws IOException {
    if (!fileExists()) {
        throw new FileNotFoundException("文件不存在");
    }
}
```

**特点**：

- 用于方法签名
- 可以声明多个异常：`throws AException, BException`
- 将异常处理责任交给调用者

### throw 关键字

在方法内部**实际抛出**一个异常对象：

```java
public void validateAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("年龄无效：" + age);
    }
}
```

**特点**：

- 用于方法体内部
- 只能抛出一个异常对象
- 后面不能有可执行代码

### 对比表

| 特性       | throw  | throws |
|----------|--------|--------|
| **位置**   | 方法体内   | 方法签名   |
| **作用**   | 抛出异常对象 | 声明异常类型 |
| **数量**   | 一次一个   | 可声明多个  |
| **后续代码** | 不可执行   | 无影响    |

---

## 自定义异常

### 为什么要自定义异常？

1. **业务语义清晰**：一眼看出是什么业务错误
2. **统一错误码管理**：便于前端展示和日志追踪
3. **分层处理**：不同层级处理不同类型的异常

### 自定义异常规范

```java
// 业务异常基类
public class BusinessException extends RuntimeException {
    private final String errorCode;
    
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}

// 用户相关异常
public class UserException extends BusinessException {
    public UserNotFoundException(String userId) {
        super("USER_NOT_FOUND", "用户不存在：" + userId);
    }
    
    public UserAlreadyExistsException(String username) {
        super("USER_EXISTS", "用户已存在：" + username);
    }
}
```

### 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public UserDTO getUserById(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return convertToDTO(user);
    }
    
    public void register(UserRegisterRequest request) {
        // 检查用户名是否存在
        User existing = userMapper.findByUsername(request.getUsername());
        if (existing != null) {
            throw new UserAlreadyExistsException(request.getUsername());
        }
        
        // 注册逻辑...
    }
}

// Controller 层统一异常处理
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        log.error("业务异常：{}", e.getErrorCode(), e);
        return Result.error(e.getErrorCode(), e.getMessage());
    }
    
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("SYS_ERROR", "系统繁忙，请稍后再试");
    }
}
```

---

## try-with-resources

### JDK7+ 自动资源管理

适用于实现了 `AutoCloseable` 接口的资源：

```java
// 传统写法（需要手动关闭）
InputStream is = null;
try {
    is = new FileInputStream("file.txt");
    // 使用资源
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (is != null) {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// JDK7+ 写法（自动关闭）
try (InputStream is = new FileInputStream("file.txt")) {
    // 使用资源
} catch (IOException e) {
    e.printStackTrace();
}
// 自动调用 close()，无需 finally
```

### 多资源管理

```java
try (
    InputStream is = new FileInputStream("input.txt");
    OutputStream os = new FileOutputStream("output.txt");
    BufferedReader reader = new BufferedReader(new InputStreamReader(is))
) {
    // 使用多个资源
    String line = reader.readLine();
    os.write(line.getBytes());
} catch (IOException e) {
    e.printStackTrace();
}
// 所有资源按逆序自动关闭
```

### 实现 AutoCloseable

```java
public class MyResource implements AutoCloseable {
    
    public void doWork() {
        System.out.println("使用资源...");
    }
    
    @Override
    public void close() throws Exception {
        System.out.println("资源已关闭");
    }
}

// 使用
try (MyResource resource = new MyResource()) {
    resource.doWork();
}
```

---

## 异常最佳实践

### ✅ 推荐做法

#### 1. 针对具体异常编程

```java
// ✅ 精确捕获
try {
    readFile();
} catch (FileNotFoundException e) {
    log.error("文件未找到", e);
} catch (IOException e) {
    log.error("IO 错误", e);
}

// ❌ 过于宽泛
try {
    readFile();
} catch (Exception e) {
    // 无法区分具体错误
}
```

#### 2. 保持异常链完整

```java
// ✅ 保留原始异常
public void businessMethod() {
    try {
        dao.operation();
    } catch (SQLException e) {
        throw new BusinessException("DB_ERROR", "数据库操作失败", e);
    }
}

// 获取完整信息
try {
    service.businessMethod();
} catch (BusinessException e) {
    log.error("业务异常：{}", e.getErrorCode());
    log.error("根本原因：", e.getCause());  // 查看底层原因
}
```

#### 3. 使用 try-with-resources

```java
// ✅ 自动关闭
try (Connection conn = dataSource.getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    // 使用资源
} catch (SQLException e) {
    log.error("数据库错误", e);
}
```

#### 4. 记录完整的堆栈信息

```java
// ✅ 记录异常
log.error("处理订单失败，orderId: {}", orderId, e);

// ❌ 只记录消息
log.error("处理订单失败：" + e.getMessage());  // 丢失堆栈信息
```

### ❌ 常见错误

#### 1. 吞掉异常

```java
// ❌ 错误做法
try {
    riskyOperation();
} catch (Exception e) {
    // 什么都不做，异常消失
}

// ✅ 正确做法
try {
    riskyOperation();
} catch (Exception e) {
    log.error("风险操作失败", e);
    throw e;  // 或抛出业务异常
}
```

#### 2. 用异常控制流程

```java
// ❌ 性能差
for (int i = 0; ; i++) {
    try {
        array[i] = i;
    } catch (ArrayIndexOutOfBoundsException e) {
        break;  // 靠异常退出循环
    }
}

// ✅ 先检查边界
for (int i = 0; i < array.length; i++) {
    array[i] = i;
}
```

#### 3. finally 中 return

```java
// ❌ 会覆盖 try 的返回值
public int getValue() {
    try {
        return 1;
    } finally {
        return 2;  // 实际返回 2
    }
}

// ✅ 避免在 finally 中返回
public int getValue() {
    try {
        return 1;
    } finally {
        cleanup();  // 只做清理工作
    }
}
```

#### 4. 捕获无法处理的异常

```java
// ❌ 捕获了却处理不了
try {
    transaction.begin();
    // 业务逻辑
    transaction.commit();
} catch (Exception e) {
    log.error("失败", e);
    // 事务状态未知，但未回滚
}

// ✅ 妥善处理
try {
    transaction.begin();
    // 业务逻辑
    transaction.commit();
} catch (Exception e) {
    log.error("失败，回滚事务", e);
    transaction.rollback();
    throw new BusinessException("TX_FAILED", "交易失败", e);
}
```

---

## 高频面试题

### 问题 1：Error 和 Exception 的区别？

**答案：**

| 维度       | Error                                  | Exception                           |
|----------|----------------------------------------|-------------------------------------|
| **定义**   | JVM 无法处理的严重错误                          | 程序可以处理的异常                           |
| **原因**   | 系统资源问题、虚拟机错误                           | 程序逻辑、外部环境问题                         |
| **处理策略** | 不应捕获，应让程序终止                            | 应该捕获并恢复                             |
| **常见类型** | OutOfMemoryError<br>StackOverflowError | IOException<br>NullPointerException |

**示例：**

```java
// Error - 不应捕获
try {
    // 无限递归
    recursiveMethod();
} catch (StackOverflowError e) {
    // ❌ 即使捕获也无法恢复
}

// Exception - 应该捕获
try {
    readFile();
} catch (FileNotFoundException e) {
    // ✅ 可以降级处理
    useDefaultData();
}
```

---

### 问题 2：RuntimeException 和 Checked Exception 的区别？

**答案：**

| 维度       | RuntimeException | Checked Exception     |
|----------|------------------|-----------------------|
| **检查时机** | 运行时              | 编译时                   |
| **强制处理** | 否                | 是（throws 或 try-catch） |
| **代表场景** | 程序逻辑错误           | 外部资源访问                |
| **恢复性**  | 通常不可恢复           | 通常可恢复                 |

**常见类型：**

- **RuntimeException**：NPE、数组越界、类型转换异常
- **Checked Exception**：IOException、SQLException、ClassNotFoundException

**设计哲学：**

- RuntimeException：程序员应该避免的错误
- Checked Exception：外部环境导致的可预期问题

---

### 问题 3：final、finally、finalize 的区别？

**答案：**

| 关键字          | 作用               | 使用场景                  |
|--------------|------------------|-----------------------|
| **final**    | 修饰符：不可变          | 常量、方法不可重写、类不可继承       |
| **finally**  | 异常处理：总执行         | try-catch 后清理资源       |
| **finalize** | Object 方法：GC 前回调 | 已废弃（JDK9+），改用 Cleaner |

**示例：**

```java
// final
final int MAX_VALUE = 100;  // 常量
final class ImmutableClass {}  // 不可继承

// finally
try {
    // 业务逻辑
} finally {
    // 总是执行，清理资源
}

// finalize（已废弃）
@Override
protected void finalize() throws Throwable {
    // GC 前清理，不推荐使用
}
```

---

### 问题 4：try-with-resources 的优势？

**答案：**

**优势：**

1. **自动关闭资源**：无需手动调用 close()
2. **避免资源泄漏**：即使异常也会关闭
3. **代码简洁**：减少大量样板代码
4. **抑制异常**：多个异常时保留主异常

**对比：**

```java
// 传统写法（冗长）
InputStream is = null;
try {
    is = new FileInputStream("file.txt");
    // 使用资源
} finally {
    if (is != null) {
        try {
            is.close();
        } catch (IOException e) {
            log.error("关闭失败", e);
        }
    }
}

// try-with-resources（简洁）
try (InputStream is = new FileInputStream("file.txt")) {
    // 使用资源
} catch (IOException e) {
    log.error("IO 错误", e);
}
```

**适用条件：**

- 实现了 `AutoCloseable` 或 `Closeable` 接口
- JDK7+ 支持

---

### 问题 5：如何设计自定义异常体系？

**答案：**

**设计原则：**

1. **继承 RuntimeException**：非受检异常，避免污染方法签名
2. **包含错误码**：便于前端展示和日志追踪
3. **保持异常链**：传递原始异常原因
4. **分层设计**：不同业务域有不同异常

**示例架构：**

```
BaseException（抽象基类）
├── BusinessException（业务异常）
│   ├── OrderException
│   ├── PaymentException
│   └── UserException
├── TechnicalException（技术异常）
│   ├── DatabaseException
│   └── RpcException
└── ValidationException（校验异常）
```

**代码实现：**

```java
// 基类
public abstract class BaseException extends RuntimeException {
    protected final String errorCode;
    
    public BaseException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

// 业务异常
public class BusinessException extends BaseException {
    public BusinessException(String code, String msg) {
        super(code, msg);
    }
}

// 订单异常
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(String orderId) {
        super("ORDER_NOT_FOUND", "订单不存在：" + orderId);
    }
}
```

---

### 问题 6：异常处理有哪些性能考虑？

**答案：**

**性能影响：**

1. **创建异常对象开销大**：填充堆栈跟踪
2. **频繁抛异常影响性能**：比条件判断慢几个数量级
3. **避免用异常控制流程**：应用于真正的异常情况

**性能对比：**

```java
// ❌ 性能差（10000 次异常耗时约 500ms）
long start = System.currentTimeMillis();
for (int i = 0; i < 10000; i++) {
    try {
        throw new Exception("test");
    } catch (Exception e) {}
}

// ✅ 性能好（10000 次条件判断耗时约 1ms）
long start = System.currentTimeMillis();
for (int i = 0; i < 10000; i++) {
    if (condition) {
        // 处理逻辑
    }
}
```

**优化建议：**

- 只在真正异常情况下抛异常
- 正常业务流程用条件判断
- 高并发场景避免频繁抛异常

---

## 🔗 跨模块关联

### 前置知识

- ✅ **Java基础语法** - 方法、类、接口
- ✅ **面向对象** - 继承、多态

### 后续应用

- 📚 **[MySQL](../07-MySQL 数据库/README.md)** - SQLException 处理
- 📚 **[Spring框架](../04-Spring框架/README.md)** - 事务异常传播
- 📚 **[MyBatis](../09-中间件/README.md)** - 持久层异常封装
- 📚 **[微服务](../06-SpringCloud 微服务/README.md)** - 全局异常处理

---

## 📖 学习建议

### 第一阶段：理解异常体系（1 天）

1. 掌握 Throwable、Error、Exception 的关系
2. 区分 RuntimeException 和 Checked Exception
3. 熟悉常见异常类型

### 第二阶段：异常处理语法（1 天）

1. try-catch-finally 执行流程
2. throws 和 throw 的使用
3. 多异常捕获和异常链

### 第三阶段：实战应用（1-2 天）

1. 自定义异常设计
2. try-with-resources
3. 全局异常处理器
4. 异常处理最佳实践

---

## 📈 更新日志

### v1.0 - 2026-03-15

- ✅ 新增 Java 异常详解文档
- ✅ 补充 6 道高频面试题
- ✅ 添加异常处理最佳实践
- ✅ 配套示例代码在 `interview-service/java/basic/JavaExceptionDemo.java`

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
