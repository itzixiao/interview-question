# Spring IOC 与 AOP 详解

## IOC（控制反转）

### 概念

**控制反转**（Inversion of Control）是一种设计原则，将对象的创建、依赖注入、生命周期管理交给 Spring 容器。

### 核心概念对比

| 传统方式 | IOC 方式 |
|----------|----------|
| 对象自己创建依赖（new） | 容器创建对象，注入依赖 |
| 主动查找依赖 | 被动接收依赖 |
| 代码耦合度高 | 松耦合，便于测试 |

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

| 作用域 | 说明 |
|--------|------|
| singleton（默认）| 每个 Spring 容器一个实例 |
| prototype | 每次请求创建新实例 |
| request | 每个 HTTP 请求一个实例 |
| session | 每个 HTTP Session 一个实例 |
| application | 每个 ServletContext 一个实例 |

## AOP（面向切面编程）

### 核心概念

| 概念 | 说明 |
|------|------|
| Aspect（切面） | 横切关注点的模块化 |
| JoinPoint（连接点） | 程序执行过程中的点（方法调用）|
| Pointcut（切点） | 匹配连接点的表达式 |
| Advice（通知） | 切点处执行的逻辑 |
| Target（目标对象） | 被代理的原始对象 |
| Proxy（代理） | AOP 框架创建的对象 |

### 通知类型

| 通知 | 注解 | 执行时机 |
|------|------|----------|
| 前置通知 | @Before | 方法执行前 |
| 后置通知 | @After | 方法执行后（无论是否异常）|
| 返回通知 | @AfterReturning | 方法成功返回后 |
| 异常通知 | @AfterThrowing | 方法抛出异常后 |
| 环绕通知 | @Around | 包围方法执行 |

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
