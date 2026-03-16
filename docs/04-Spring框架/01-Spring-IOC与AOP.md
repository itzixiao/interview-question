# Spring IOC 与 AOP 详解

## IOC（控制反转）

### 概念

**控制反转**（Inversion of Control）是一种设计原则，将对象的创建、依赖注入、生命周期管理交给 Spring 容器。

### 核心概念对比

| 传统方式          | IOC 方式      |
|---------------|-------------|
| 对象自己创建依赖（new） | 容器创建对象，注入依赖 |
| 主动查找依赖        | 被动接收依赖      |
| 代码耦合度高        | 松耦合，便于测试    |

### 依赖注入（DI）方式

#### 1. 构造器注入（推荐）

```java
@Service
public class UserService {
    private final UserDao userDao;
    
    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

**优点**：

- 依赖明确，不可变
- 便于单元测试
- 循环依赖可检测

#### 2. Setter 注入

```java
@Service
public class UserService {
    private UserDao userDao;
    
    @Autowired
    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

#### 3. 字段注入

```java
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
}
```

**缺点**：

- 依赖不明确
- 无法使用 final
- 不便单元测试

### Bean 作用域

| 作用域           | 说明                     |
|---------------|------------------------|
| singleton（默认） | 每个 Spring 容器一个实例       |
| prototype     | 每次请求创建新实例              |
| request       | 每个 HTTP 请求一个实例         |
| session       | 每个 HTTP Session 一个实例   |
| application   | 每个 ServletContext 一个实例 |

## AOP（面向切面编程）

### 核心概念

| 概念             | 说明              |
|----------------|-----------------|
| Aspect（切面）     | 横切关注点的模块化       |
| JoinPoint（连接点） | 程序执行过程中的点（方法调用） |
| Pointcut（切点）   | 匹配连接点的表达式       |
| Advice（通知）     | 切点处执行的逻辑        |
| Target（目标对象）   | 被代理的原始对象        |
| Proxy（代理）      | AOP 框架创建的对象     |

### 通知类型

| 通知   | 注解              | 执行时机          |
|------|-----------------|---------------|
| 前置通知 | @Before         | 方法执行前         |
| 后置通知 | @After          | 方法执行后（无论是否异常） |
| 返回通知 | @AfterReturning | 方法成功返回后       |
| 异常通知 | @AfterThrowing  | 方法抛出异常后       |
| 环绕通知 | @Around         | 包围方法执行        |

### 切点表达式

```java
@Pointcut("execution(* com.example.service.*.*(..))")  // 执行方法
@Pointcut("within(com.example.service.*)")             // 包内
@Pointcut("@annotation(com.example.annotation.Log)")   // 注解标记
@Pointcut("args(java.lang.String)")                    // 参数类型
```

### 执行顺序

```
@Around 前置
@Before
目标方法
@AfterReturning / @AfterThrowing
@After
@Around 后置
```

## 代理机制

### JDK 动态代理

- 基于接口
- 目标类必须实现接口
- Spring 默认选择

### CGLIB

- 基于继承
- 生成目标类的子类
- 无法代理 final 方法
- 设置 `proxyTargetClass=true` 强制使用

## 循环依赖解决

### 构造器循环依赖

```java
// A 依赖 B，B 依赖 A - 无法解决，启动报错
@Component
public class A {
    public A(B b) {}  // 错误
}
```

### Setter/字段循环依赖

Spring 使用**三级缓存**解决：

1. **singletonObjects**：成品 Bean
2. **earlySingletonObjects**：半成品 Bean（已实例化，未注入）
3. **singletonFactories**：Bean 工厂

```
A 创建 → 放入三级缓存 → 发现依赖 B
B 创建 → 放入三级缓存 → 发现依赖 A
从三级缓存获取 A 的引用 → B 完成 → A 完成
```

## 最佳实践

1. **优先构造器注入**：依赖明确，不可变
2. **AOP 切点粒度**：不要太细，避免性能问题
3. **环绕通知注意**：必须调用 `proceed()`，否则目标方法不执行
4. **避免循环依赖**：重构代码，使用中间层

---

## 💡 高频面试题

**问题 1：什么是 IOC？有什么好处？**

答案：
**IOC（Inversion of Control，控制反转）** 是一种设计原则，将对象的创建、依赖注入和生命周期管理从应用程序代码中剥离，交给
Spring 容器统一管理。

**核心理解：**

- **控制**：指对象创建和依赖管理的控制权
- **反转**：从"自己主动创建"反转为"被动接收注入"

**好处：**
| 方面 | 说明 |
|------|------|
| 松耦合 | 对象之间不直接依赖，便于维护和扩展 |
| 可测试 | 可以轻松注入 Mock 对象进行单元测试 |
| 集中管理 | Bean 的创建和配置集中在容器中 |
| 动态替换 | 运行时可以灵活替换实现类 |

**代码对比：**

```java
// 传统方式 - 高耦合
public class UserService {
    private UserDao userDao = new UserDaoImpl(); // 硬编码
}

// IOC 方式 - 松耦合
@Service
public class UserService {
    @Autowired
    private UserDao userDao; // Spring 注入
}
```

**问题 2：DI 有哪些方式？推荐使用哪种？**

答案：
Spring 提供三种依赖注入方式：

| 注入方式      | 注解         | 优点           | 缺点            |
|-----------|------------|--------------|---------------|
| 构造器注入     | @Autowired | 依赖明确、不可变、易测试 | 参数过多时臃肿       |
| Setter 注入 | @Autowired | 灵活、可选依赖      | 依赖不明确         |
| 字段注入      | @Autowired | 简洁           | 无法 final、不便测试 |

**推荐优先级：构造器注入 > Setter 注入 > 字段注入**

**构造器注入（Spring 官方推荐）：**

```java
@Service
public class UserService {
    private final UserDao userDao;
    
    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao; // 依赖明确，可保证非空
    }
}
```

**为什么推荐构造器注入？**

1. **依赖显式化**：构造函数参数清晰展示依赖关系
2. **不可变性**：可以使用 final 修饰字段
3. **便于测试**：可以直接传入 Mock 对象
4. **避免循环依赖**：启动时就能检测出循环依赖
5. **强制依赖检查**：确保必要依赖不为 null

**字段注入的问题：**

```java
@Service
public class UserService {
    @Autowired
    private UserDao userDao; // ❌ 依赖隐藏，无法 final
}
```

**问题 3：AOP 的实现原理？JDK 和 CGLIB 的区别？**

答案：
Spring AOP 基于**动态代理**模式实现，在运行时创建代理对象，拦截目标方法的执行。

**两种代理方式对比：**

| 特性        | JDK 动态代理                | CGLIB     |
|-----------|-------------------------|-----------|
| 实现方式      | 基于接口                    | 基于继承      |
| 要求        | 目标类必须实现接口               | 无特殊要求     |
| 底层        | java.lang.reflect.Proxy | ASM 字节码生成 |
| 性能        | 较低（反射调用）                | 较高（直接调用）  |
| final 方法  | 无法代理                    | 无法代理      |
| Spring 选择 | 默认（有接口时）                | 无接口时自动切换  |

**JDK 动态代理示例：**

```java
// 目标类实现接口
public interface UserService {
    void addUser();
}

@Service
public class UserServiceImpl implements UserService {
    public void addUser() {
        System.out.println("添加用户");
    }
}

// Spring 创建代理：Proxy.newProxyInstance()
// 代理对象实现了 UserService 接口
```

**CGLIB 代理示例：**

```java
@Service
public class UserService {
    public void addUser() {
        System.out.println("添加用户");
    }
}

// Spring 创建子类：Enhancer.create(UserService.class)
// 代理对象是 UserService$$EnhancerByCGLIB$$...
```

**强制使用 CGLIB：**

```yaml
spring:
  aop:
    proxy-target-class: true # 强制使用 CGLIB
```

**问题 4：Spring Bean 的作用域有哪些？**

答案：
Spring Bean 有 5 种作用域：

| 作用域           | 说明                     | 应用场景               |
|---------------|------------------------|--------------------|
| singleton（默认） | 每个容器一个实例               | 无状态服务（Service、Dao） |
| prototype     | 每次请求新实例                | 有状态的 Bean          |
| request       | 每个 HTTP 请求一个实例         | Web 应用中的请求级数据      |
| session       | 每个 Session 一个实例        | 用户会话数据             |
| application   | 每个 ServletContext 一个实例 | 全局共享数据             |

**singleton vs prototype：**

```java
// singleton - 单例
@Component
@Scope("singleton") // 默认，可省略
public class UserService {
    // 所有请求共享同一个实例
}

// prototype - 多例
@Component
@Scope("prototype")
public class ShoppingCart {
    // 每次注入或获取都创建新实例
}
```

**Web 作用域：**

```java
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
public class UserRequestContext {
    // 每个 HTTP 请求独享
}
```

**问题 5：Spring 如何解决循环依赖？**

答案：
Spring 通过**三级缓存**机制解决 Setter/字段循环依赖，但无法解决构造器循环依赖。

**三级缓存结构：**
| 缓存级别 | 名称 | 存储内容 |
|----------|------|----------|
| 一级 | singletonObjects | 成品 Bean（已完成初始化） |
| 二级 | earlySingletonObjects | 半成品 Bean（已实例化，未注入） |
| 三级 | singletonFactories | Bean 工厂（用于创建早期引用） |

**解决流程（A 依赖 B，B 依赖 A）：**

```
1. 创建 A → 实例化 → 放入三级缓存
2. A 发现需要 B → 暂停 A，去创建 B
3. 创建 B → 实例化 → 放入三级缓存
4. B 发现需要 A → 从三级缓存获取 A 的工厂 → 
   创建 A 的早期引用 → 放入二级缓存 → 注入给 B
5. B 完成初始化 → 放入一级缓存
6. A 获取已初始化的 B → 完成初始化 → 放入一级缓存
```

**构造器循环依赖无解：**

```java
@Component
public class A {
    public A(B b) {} // ❌ 启动报错：BeanCurrentlyInCreationException
}

@Component
public class B {
    public B(A a) {} // 实例化时就需要对方，形成死锁
}
```

**解决方案：**

1. 使用 @Lazy 延迟加载
2. 重构代码，引入中间层
3. 改用 Setter/字段注入

**问题 6：@Autowired 和@Resource 的区别？**

答案：

| 特性   | @Autowired        | @Resource         |
|------|-------------------|-------------------|
| 来源   | Spring 注解         | JDK 标准注解（JSR-250） |
| 注入规则 | byType（按类型）       | byName（按名称）       |
| 配合使用 | @Qualifier 指定名称   | name 属性指定名称       |
| 回退策略 | byType→byName     | byName→byType     |
| 必需性  | required=false 可选 | 默认必需              |

**@Autowired（按类型注入）：**

```java
@Service
public class UserService {
    @Autowired
    private UserDao userDao; // 按类型查找
    
    // 多个实现类时用@Qualifier 指定
    @Autowired
    @Qualifier("userDaoImpl")
    private UserDao userDao;
}
```

**@Resource（按名称注入）：**

```java
@Service
public class UserService {
    @Resource(name = "userDaoImpl") // 直接指定名称
    private UserDao userDao;
    
    // 不指定名称时，默认用字段名作为 beanName
    @Resource
    private UserDao userDao; // 等价于@Resource(name="userDao")
}
```

**使用建议：**

- 优先使用 @Autowired（Spring 生态更统一）
- 需要按名称注入时用 @Qualifier
- 想脱离 Spring 容器时用 @Resource

**问题 7：AOP 的通知类型有哪些？**

答案：

| 通知类型 | 注解              | 执行时机  | 特点                   |
|------|-----------------|-------|----------------------|
| 前置通知 | @Before         | 方法执行前 | 无法阻止方法执行             |
| 后置通知 | @After          | 方法执行后 | 无论是否异常都执行            |
| 返回通知 | @AfterReturning | 成功返回后 | 可以修改返回值              |
| 异常通知 | @AfterThrowing  | 抛出异常后 | 可以指定异常类型             |
| 环绕通知 | @Around         | 包围方法  | 功能最强，需手动调用 proceed() |

**完整示例：**

```java
@Aspect
@Component
public class LogAspect {
    
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void logPointcut() {}
    
    @Before("logPointcut()")
    public void before(JoinPoint jp) {
        System.out.println("前置：" + jp.getSignature().getName());
    }
    
    @After("logPointcut()")
    public void after(JoinPoint jp) {
        System.out.println("后置：无论成败都会执行");
    }
    
    @AfterReturning(pointcut = "logPointcut()", returning = "result")
    public void afterReturning(Object result) {
        System.out.println("返回结果：" + result);
    }
    
    @AfterThrowing(pointcut = "logPointcut()", throwing = "ex")
    public void afterThrowing(Exception ex) {
        System.out.println("异常：" + ex.getMessage());
    }
    
    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("环绕前置");
        try {
            Object result = pjp.proceed(); // 必须调用！
            System.out.println("环绕后置");
            return result;
        } catch (Throwable t) {
            System.out.println("环绕异常处理");
            throw t;
        }
    }
}
```

**执行顺序：**

```
@Around 前置
@Before
目标方法执行
@AfterReturning（成功）或 @AfterThrowing（失败）
@After
@Around 后置
```

**问题 8：Spring 中用到了哪些设计模式？**

答案：

| 设计模式  | 应用场景                         | 说明             |
|-------|------------------------------|----------------|
| 工厂模式  | BeanFactory                  | 创建 Bean 实例     |
| 单例模式  | DefaultSingletonBeanRegistry | 保证 Bean 单例     |
| 代理模式  | AOP、事务                       | JDK/CGLIB 动态代理 |
| 模板方法  | JdbcTemplate                 | 定义算法骨架         |
| 观察者模式 | ApplicationEvent             | 事件驱动模型         |
| 适配器模式 | HandlerAdapter               | 统一处理器调用        |
| 策略模式  | Resource                     | 不同资源读取策略       |
| 装饰器模式 | BeanWrapper                  | 增强 Bean 功能     |

**工厂模式示例：**

```java
public interface BeanFactory {
    Object getBean(String name); // 工厂方法
}
```

**模板方法模式示例：**

```java
public abstract class JdbcTemplate {
    // 模板方法 - 定义流程
    public final void execute() {
        openConnection();
        doExecute(); // 子类实现
        closeConnection();
    }
    protected abstract void doExecute();
}
```

**观察者模式示例：**

```java
// 发布事件
applicationContext.publishEvent(new OrderCreatedEvent(order));

// 监听事件
@EventListener
public void handleOrderCreated(OrderCreatedEvent event) {
    // 处理业务
}
```
