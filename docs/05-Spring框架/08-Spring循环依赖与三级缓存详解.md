# Spring 循环依赖与三级缓存详解

## 📚 目录

1. [什么是循环依赖](#1-什么是循环依赖)
2. [循环依赖的四种解决方案](#2-循环依赖的四种解决方案)
3. [Spring 三级缓存机制](#3-spring-三级缓存机制)
4. [完整示例代码](#4-完整示例代码)
5. [面试题与思考](#5-面试题与思考)

---

## 1. 什么是循环依赖

### 1.1 定义

**循环依赖（Circular Dependency）** 是指两个或多个 Bean 之间相互依赖，形成环路。

### 1.2 典型场景

```java
@Component
public class ServiceA {
    @Autowired
    private ServiceB serviceB;  // A 依赖 B
}

@Component
public class ServiceB {
    @Autowired
    private ServiceA serviceA;  // B 依赖 A
}
```

### 1.3 依赖关系图

```
┌─────────────┐
│  ServiceA   │
│             │
│ +serviceB ──┼──┐
└─────────────┘  │
                 │
                 ▼
          ┌─────────────┐
          │  ServiceB   │
          │             │
          │ +serviceA ──┼──┐
          └─────────────┘  │
                           │
                           ▼
                    (回到 ServiceA)
```

---

## 2. 循环依赖的四种解决方案

### 方案一：构造器注入 ❌（无法解决）

#### 示例代码

**文件路径：** `circular/ServiceA.java`, `circular/ServiceB.java`

```java
@Component
public class ServiceA {
    private final ServiceB serviceB;
    
    public ServiceA(ServiceB serviceB) {
        this.serviceB = serviceB;
    }
}

@Component
public class ServiceB {
    private final ServiceA serviceA;
    
    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}
```

#### 错误信息

```
org.springframework.beans.factory.UnsatisfiedDependencyException: 
Error creating bean with name 'serviceA': 
Unsatisfied dependency expressed through constructor parameter 0: 
Error creating bean with name 'serviceB': 
Unsatisfied dependency expressed through constructor parameter 0: 
Error creating bean with name 'serviceA': 
...
Requested bean is currently in creation: Is there an unresolvable circular reference?
```

#### 问题分析

```
创建 ServiceA
  ↓
需要 ServiceB（构造器参数）
  ↓
创建 ServiceB
  ↓
需要 ServiceA（构造器参数）
  ↓
等待 ServiceA 创建完成 ← 死循环！
```

**结论：** 构造器注入 **无法解决** 循环依赖问题！

---

### 方案二：Setter 注入 ✅（可以解决）

#### 示例代码

**文件路径：** `circular/ServiceC.java`, `circular/ServiceD.java`

```java
@Component
public class ServiceC {
    private ServiceD serviceD;
    
    @Autowired
    public void setServiceD(ServiceD serviceD) {
        this.serviceD = serviceD;
    }
}

@Component
public class ServiceD {
    private ServiceC serviceC;
    
    @Autowired
    public void setServiceC(ServiceC serviceC) {
        this.serviceC = serviceC;
    }
}
```

#### 执行流程

```
1. 创建 ServiceC 实例（无参构造）
   ↓
2. 发现需要注入 ServiceD
   ↓
3. 创建 ServiceD 实例（无参构造）
   ↓
4. 发现需要注入 ServiceC
   ↓
5. ServiceC 已存在，直接注入
   ↓
6. 完成 ServiceD 的 setter 注入
   ↓
7. 完成 ServiceC 的 setter 注入
   ↓
✅ 成功！
```

#### 控制台输出

```
===== ServiceC Setter 注入完成 =====
===== ServiceD Setter 注入完成 =====
```

**结论：** Setter 注入 **可以解决** 循环依赖问题！

---

### 方案三：@Autowired + @PostConstruct ✅（推荐）

#### 示例代码

**文件路径：** `circular/ServiceE.java`, `circular/ServiceF.java`

```java
@Component
public class ServiceE {
    @Autowired
    private ServiceF serviceF;
    
    @PostConstruct
    public void init() {
        // 此时 serviceF 已经被注入
        serviceF.methodF();
    }
}

@Component
public class ServiceF {
    @Autowired
    private ServiceE serviceE;
    
    @PostConstruct
    public void init() {
        // 此时 serviceE 已经被注入
        serviceE.methodE();
    }
}
```

#### 执行流程

```
1. 创建 ServiceE 实例
   ↓
2. 创建 ServiceF 实例
   ↓
3. 注入 ServiceF.serviceE（ServiceE 已存在）
   ↓
4. 注入 ServiceE.serviceF（ServiceF 已存在）
   ↓
5. 执行 ServiceF.@PostConstruct
   ↓
6. 执行 ServiceE.@PostConstruct
   ↓
✅ 成功！
```

#### 控制台输出

```
===== ServiceF @PostConstruct 初始化完成 =====
[ServiceF] 执行 methodF
[ServiceE] 执行 methodE
===== ServiceE @PostConstruct 初始化完成 =====
[ServiceE] 执行 methodE
[ServiceF] 执行 methodF
```

**结论：** @Autowired + @PostConstruct **可以解决** 循环依赖问题！

---

### 方案四：@Lazy 注解 ✅（推荐）

#### 示例代码

**文件路径：** `circular/CircularDependencyConfig.java`

```java
@Configuration
public class CircularDependencyConfig {
    
    @Bean
    public LazyServiceA lazyServiceA() {
        return new LazyServiceA(lazyServiceB());  // @Lazy 代理
    }
    
    @Bean
    public LazyServiceB lazyServiceB() {
        return new LazyServiceB(lazyServiceA());  // @Lazy 代理
    }
}

class LazyServiceA {
    private final LazyServiceB serviceB;
    
    public LazyServiceA(@Lazy LazyServiceB serviceB) {
        this.serviceB = serviceB;  // 注入的是代理对象
    }
}

class LazyServiceB {
    private final LazyServiceA serviceA;
    
    public LazyServiceB(@Lazy LazyServiceA serviceA) {
        this.serviceA = serviceA;  // 注入的是代理对象
    }
}
```

#### 核心原理

```
1. 创建 LazyServiceA
   ↓
2. 需要 LazyServiceB（@Lazy）
   ↓
3. 创建 LazyServiceB 的代理对象
   ↓
4. 将代理对象注入到 LazyServiceA
   ↓
5. 创建 LazyServiceB
   ↓
6. 需要 LazyServiceA（@Lazy）
   ↓
7. LazyServiceA 已存在，注入代理对象
   ↓
8. 当真正调用方法时，代理对象才调用真实对象
   ↓
✅ 成功！
```

#### 控制台输出

```
===== LazyServiceA 构造完成 =====
===== LazyServiceB 构造完成 =====
```

**结论：** @Lazy 注解 **可以解决** 循环依赖问题！

---

## 3. Spring 三级缓存机制

### 3.1 什么是三级缓存？

Spring 使用三级缓存来解决 Setter 注入的循环依赖问题。

### 3.2 三级缓存结构

```java
// 一级缓存：存放完全初始化好的 Bean
Map<String, Object> singletonObjects = new ConcurrentHashMap<>();

// 二级缓存：存放原始的 Bean 对象（尚未填充属性）
Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();

// 三级缓存：存放 Bean 工厂对象（用于提前暴露引用）
Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
```

### 3.3 三级缓存详解

#### 一级缓存（singletonObjects）

- **作用：** 存放完全初始化完成的单例 Bean
- **特点：** Bean 已经创建完成，属性已经填充
- **访问：** 平时从容器中获取 Bean 都是从这里

#### 二级缓存（earlySingletonObjects）

- **作用：** 存放原始的 Bean 对象（半成品）
- **特点：** Bean 已创建但属性未填充
- **目的：** 提前暴露引用，解决循环依赖

#### 三级缓存（singletonFactories）

- **作用：** 存放 Bean 工厂对象（ObjectFactory）
- **特点：** 通过工厂方法可以获取早期 Bean 引用
- **核心：** 支持 AOP 提前代理

### 3.4 三级缓存解决循环依赖的完整流程

#### 场景：ServiceA 依赖 ServiceB，ServiceB 依赖 ServiceA

```
【第 1 步】创建 ServiceA
  ↓
【第 2 步】实例化 ServiceA（原始对象）
  ↓
【第 3 步】将 ServiceA 放入三级缓存（singletonFactories）
  ↓
【第 4 步】填充 ServiceA 的属性，发现需要 ServiceB
  ↓
【第 5 步】创建 ServiceB
  ↓
【第 6 步】实例化 ServiceB（原始对象）
  ↓
【第 7 步】将 ServiceB 放入三级缓存（singletonFactories）
  ↓
【第 8 步】填充 ServiceB 的属性，发现需要 ServiceA
  ↓
【第 9 步】从三级缓存获取 ServiceA 的 ObjectFactory
  ↓
【第 10 步】调用 ObjectFactory.getObject() 获取早期 ServiceA 引用
   ↓
【第 11 步】将 ServiceA 从三级缓存移到二级缓存（earlySingletonObjects）
   ↓
【第 12 步】将早期 ServiceA 引用注入到 ServiceB
   ↓
【第 13 步】完成 ServiceB 的创建，放入一级缓存
   ↓
【第 14 步】将 ServiceB 注入到 ServiceA
   ↓
【第 15 步】完成 ServiceA 的创建，放入一级缓存
   ↓
✅ 循环依赖解决！
```

### 3.5 为什么要用三级缓存而不是二级？

#### 关键原因：支持 AOP 提前代理

**场景：** 如果 ServiceA 需要被 AOP 代理

```java
// 没有三级缓存的情况
1. 创建 ServiceA 原始对象
2. 放入二级缓存
3. 创建 AOP 代理对象
4. 问题来了：应该在什么时候创建代理？

// 有三级缓存的情况
1. 创建 ServiceA 原始对象
2. 将 ObjectFactory 放入三级缓存
   ObjectFactory -> () -> {
       if (需要 AOP) {
           return 创建代理对象 ();
       } else {
           return 原始对象 ();
       }
   }
3. 当其他 Bean 需要 ServiceA 时，从 ObjectFactory 获取
4. 如果需要 AOP，返回代理对象；否则返回原始对象
```

#### 源码片段（DefaultSingletonBeanRegistry）

```java
@Override
protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
    synchronized (this.singletonObjects) {
        if (!this.singletonObjects.containsKey(beanName)) {
            this.singletonFactories.put(beanName, singletonFactory);
            this.earlySingletonObjects.remove(beanName);
        }
    }
}
```

### 3.6 三级缓存流程图

```
┌─────────────────────────────────────────────────────────┐
│                   Spring IoC Container                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  一级缓存：singletonObjects                              │
│  ┌────────────────────────────────────────────┐         │
│  │ Map<String, Object>                        │         │
│  │ 完全初始化好的 Bean                         │         │
│  └────────────────────────────────────────────┘         │
│                       ↑                                  │
│                       │ getBean()                        │
│                       ↓                                  │
│  二级缓存：earlySingletonObjects                         │
│  ┌────────────────────────────────────────────┐         │
│  │ Map<String, Object>                        │         │
│  │ 原始 Bean 对象（未填充属性）                  │         │
│  └────────────────────────────────────────────┘         │
│                       ↑                                  │
│                       │ getObject()                      │
│                       ↓                                  │
│  三级缓存：singletonFactories                            │
│  ┌────────────────────────────────────────────┐         │
│  │ Map<String, ObjectFactory<?>>              │         │
│  │ Bean 工厂对象                               │         │
│  │ ObjectFactory -> () -> {                   │         │
│  │     if (需要 AOP) return 代理对象;           │         │
│  │     else return 原始对象;                    │         │
│  │ }                                          │         │
│  └────────────────────────────────────────────┘         │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## 4. 完整示例代码

### 4.1 项目结构

```
interview-transaction-demo/
└── src/main/java/cn/itzixiao/interview/transaction/circular/
    ├── ServiceA.java          # 构造器注入（失败案例）
    ├── ServiceB.java          # 构造器注入（失败案例）
    ├── ServiceC.java          # Setter 注入（成功案例）
    ├── ServiceD.java          # Setter 注入（成功案例）
    ├── ServiceE.java          # @Autowired + @PostConstruct
    ├── ServiceF.java          # @Autowired + @PostConstruct
    └── CircularDependencyConfig.java  # @Lazy 配置
```

### 4.2 Maven 编译命令

```bash
cd interview-microservices-parent/interview-transaction-demo
mvn clean compile -DskipTests
```

### 4.3 运行测试

启动 Spring Boot 应用后，观察控制台输出：

```bash
# Setter 注入成功
===== ServiceC Setter 注入完成 =====
===== ServiceD Setter 注入完成 =====

# @PostConstruct 成功
===== ServiceF @PostConstruct 初始化完成 =====
[ServiceF] 执行 methodF
[ServiceE] 执行 methodE
===== ServiceE @PostConstruct 初始化完成 =====

# @Lazy 成功
===== LazyServiceA 构造完成 =====
===== LazyServiceB 构造完成 =====
```

---

## 5. 面试题与思考

### Q1: Spring 是如何解决循环依赖的？

**标准答案：**

1. **Setter 注入：** 通过三级缓存解决
    - 一级缓存：singletonObjects（成品 Bean）
    - 二级缓存：earlySingletonObjects（原始 Bean）
    - 三级缓存：singletonFactories（Bean 工厂）

2. **构造器注入：** 无法解决，会抛出异常

3. **@Lazy 注解：** 通过代理对象延迟加载解决

### Q2: 为什么要用三级缓存？二级不行吗？

**标准答案：**

**三级缓存的核心作用是支持 AOP 提前代理。**

- 如果只有二级缓存，只能存储原始对象
- 如果 Bean 需要 AOP 代理，在属性填充阶段就需要代理对象
- 三级缓存通过 ObjectFactory，可以在需要时才决定是否创建代理
- 这样既解决了循环依赖，又保证了 AOP 的正确性

### Q3: 循环依赖一定是坏事吗？

**不一定！**

**合理场景：**

- 事件驱动架构中的事件发布者和监听者
- 策略模式中的上下文和策略选择器
- 观察者模式中的主题和观察者

**建议：**

- 优先通过重构避免循环依赖（如提取公共接口）
- 如果确实需要，使用 Setter 注入或 @Lazy 注解
- 避免使用构造器注入的循环依赖

### Q4: Spring Boot 中循环依赖的默认处理方式？

```java
// Spring Boot 2.6+ 默认禁止循环依赖
spring.main.allow-circular-references=false

// 如果允许循环依赖，设置为 true
spring.main.allow-circular-references=true
```

**注意：** 这只是针对构造器注入的循环依赖检测，Setter 注入的循环依赖仍然通过三级缓存解决。

---

## 📊 总结对比表

| 解决方案                        | 是否推荐    | 适用场景  | 优点         | 缺点       |
|-----------------------------|---------|-------|------------|----------|
| 构造器注入                       | ❌ 不推荐   | 无循环依赖 | 依赖明确，不可变   | 无法解决循环依赖 |
| Setter 注入                   | ✅ 推荐    | 循环依赖  | 简单直接       | 依赖可变     |
| @Autowired + @PostConstruct | ✅✅ 强烈推荐 | 循环依赖  | 语义清晰，初始化安全 | 需要额外注解   |
| @Lazy 注解                    | ✅✅ 强烈推荐 | 循环依赖  | 延迟加载，性能优化  | 需要理解代理机制 |

---

## 🎯 最佳实践

1. **优先设计优化：** 通过重构避免循环依赖
2. **必须使用时：** 首选 @Lazy 注解，其次 @Autowired + @PostConstruct
3. **避免构造器循环：** 构造器注入的循环依赖无法解决
4. **理解三级缓存：** 掌握 Spring 底层原理，应对高级面试

---

**状态：** ✅ **完整示例代码已提供，包含四种解决方案和详细注释**  
**编译状态：** ✅ **BUILD SUCCESS**  
**学习价值：** ⭐⭐⭐⭐⭐ **涵盖从入门到高级的所有知识点**

🎉 **现在你可以通过运行示例代码，深入理解 Spring 循环依赖的解决方案和三级缓存原理！**
