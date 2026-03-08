# Spring Bean 生命周期详解

## 一、Bean 生命周期概览

Spring Bean 的生命周期是指从 Bean 创建到销毁的完整过程，分为四个主要阶段：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Spring Bean 生命周期                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│   │   实例化    │ →  │  属性赋值   │ →  │   初始化    │ →  │    销毁     │ │
│   │Instantiation│    │ Population  │    │Initialization│   │ Destruction │ │
│   └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘ │
│         │                  │                  │                  │          │
│         ↓                  ↓                  ↓                  ↓          │
│    构造方法            依赖注入           Aware回调          @PreDestroy   │
│                       @Autowired       @PostConstruct      DisposableBean │
│                       @Value           InitializingBean     destroy-method│
│                                        init-method                         │
│                                        BeanPostProcessor                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 二、生命周期详细阶段

### 阶段1：实例化（Instantiation）

**调用构造方法创建 Bean 实例**。

```java
public class UserService {
    public UserService() {
        System.out.println("1. 构造方法执行 - Bean 实例化");
        // 此时依赖还未注入，属性为 null
    }
}
```

**扩展点**：`InstantiationAwareBeanPostProcessor`

```java
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {
    // 实例化前调用，可返回代理对象替代正常实例化
    Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName);
    
    // 实例化后调用
    boolean postProcessAfterInstantiation(Object bean, String beanName);
}
```

---

### 阶段2：属性赋值（Population）

**将依赖注入到 Bean 中**。

```java
@Service
public class UserService {
    @Autowired
    private UserDao userDao;  // 字段注入
    
    @Value("${app.name}")
    private String appName;   // 值注入
    
    @Autowired
    public void setOrderService(OrderService orderService) {
        System.out.println("2. Setter 注入执行");
    }
}
```

**三种注入方式**：
| 方式 | 示例 | 特点 |
|------|------|------|
| 构造器注入 | `@Autowired public UserService(UserDao dao)` | 推荐，依赖明确，不可变 |
| Setter注入 | `@Autowired public void setDao(UserDao dao)` | 可选依赖 |
| 字段注入 | `@Autowired private UserDao dao` | 简单但不推荐 |

---

### 阶段3：初始化（Initialization）

初始化阶段最复杂，包含多个步骤，**执行顺序**如下：

#### 3.1 Aware 接口回调

```java
public class UserService implements 
        BeanNameAware,
        BeanFactoryAware,
        ApplicationContextAware {
    
    @Override
    public void setBeanName(String name) {
        System.out.println("3. BeanNameAware - Bean名称: " + name);
    }
    
    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        System.out.println("4. BeanFactoryAware - 获取BeanFactory");
    }
    
    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        System.out.println("5. ApplicationContextAware - 获取ApplicationContext");
    }
}
```

**常用 Aware 接口**：
| 接口 | 作用 |
|------|------|
| BeanNameAware | 获取 Bean 在容器中的名称 |
| BeanFactoryAware | 获取 BeanFactory 引用 |
| ApplicationContextAware | 获取 ApplicationContext 引用 |
| EnvironmentAware | 获取 Environment 环境配置 |
| ResourceLoaderAware | 获取资源加载器 |
| MessageSourceAware | 获取国际化资源 |

#### 3.2 BeanPostProcessor 前置处理

```java
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("6. BeanPostProcessor.postProcessBeforeInitialization");
        // @PostConstruct 在此处被处理
        return bean;
    }
}
```

#### 3.3 @PostConstruct

```java
@Service
public class UserService {
    
    @PostConstruct
    public void postConstruct() {
        System.out.println("7. @PostConstruct - 初始化方法");
        // 此时依赖已注入完成，可以进行初始化逻辑
    }
}
```

#### 3.4 InitializingBean.afterPropertiesSet()

```java
@Service
public class UserService implements InitializingBean {
    
    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("8. InitializingBean.afterPropertiesSet()");
    }
}
```

#### 3.5 自定义 init-method

```java
// 方式1：@Bean 指定
@Bean(initMethod = "customInit")
public UserService userService() {
    return new UserService();
}

// 方式2：XML 配置
// <bean id="userService" class="..." init-method="customInit"/>

public class UserService {
    public void customInit() {
        System.out.println("9. 自定义 init-method");
    }
}
```

#### 3.6 BeanPostProcessor 后置处理

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) {
    System.out.println("10. BeanPostProcessor.postProcessAfterInitialization");
    // AOP 代理在此创建！
    // 返回的可能是代理对象，而非原始 Bean
    return bean;
}
```

**重要**：AOP 代理就是在这一步创建的！

---

### 阶段4：销毁（Destruction）

容器关闭时，按以下顺序销毁 Bean：

#### 4.1 @PreDestroy

```java
@Service
public class UserService {
    
    @PreDestroy
    public void preDestroy() {
        System.out.println("11. @PreDestroy - 销毁前回调");
    }
}
```

#### 4.2 DisposableBean.destroy()

```java
@Service
public class UserService implements DisposableBean {
    
    @Override
    public void destroy() throws Exception {
        System.out.println("12. DisposableBean.destroy()");
    }
}
```

#### 4.3 自定义 destroy-method

```java
@Bean(destroyMethod = "customDestroy")
public UserService userService() {
    return new UserService();
}

public class UserService {
    public void customDestroy() {
        System.out.println("13. 自定义 destroy-method");
    }
}
```

**注意**：只有 Singleton 作用域的 Bean 会调用销毁方法，Prototype 作用域不会！

---

## 三、完整生命周期执行顺序

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Bean 生命周期完整流程                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【实例化阶段】                                                              │
│  1. InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()   │
│  2. 构造方法                                                                 │
│  3. InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()    │
│                                                                             │
│  【属性赋值阶段】                                                            │
│  4. InstantiationAwareBeanPostProcessor.postProcessProperties()            │
│  5. @Autowired、@Value 注入                                                  │
│                                                                             │
│  【初始化阶段】                                                              │
│  6. BeanNameAware.setBeanName()                                             │
│  7. BeanFactoryAware.setBeanFactory()                                       │
│  8. ApplicationContextAware.setApplicationContext()                         │
│  9. BeanPostProcessor.postProcessBeforeInitialization()                     │
│  10. @PostConstruct                                                          │
│  11. InitializingBean.afterPropertiesSet()                                  │
│  12. 自定义 init-method                                                      │
│  13. BeanPostProcessor.postProcessAfterInitialization() 【AOP代理创建】      │
│                                                                             │
│  【使用阶段】                                                                │
│  14. Bean 就绪，可以使用                                                      │
│                                                                             │
│  【销毁阶段】                                                                │
│  15. @PreDestroy                                                             │
│  16. DisposableBean.destroy()                                               │
│  17. 自定义 destroy-method                                                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 四、BeanPostProcessor 详解

### 核心接口

```java
public interface BeanPostProcessor {
    // 初始化前调用
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }
    
    // 初始化后调用（AOP 代理在此创建）
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

### 重要实现类

| 实现类 | 作用 |
|--------|------|
| `AutowiredAnnotationBeanPostProcessor` | 处理 @Autowired、@Value 注入 |
| `CommonAnnotationBeanPostProcessor` | 处理 @PostConstruct、@PreDestroy、@Resource |
| `AbstractAutoProxyCreator` | 创建 AOP 代理 |
| `AsyncAnnotationBeanPostProcessor` | 处理 @Async 异步方法 |
| `ScheduledAnnotationBeanPostProcessor` | 处理 @Scheduled 定时任务 |

### 自定义 BeanPostProcessor

```java
@Component
public class LoggingBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("[Before] 初始化 Bean: " + beanName);
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("[After] 初始化完成: " + beanName);
        // 可以返回代理对象
        return bean;
    }
}
```

---

## 五、BeanFactoryPostProcessor 详解

**区别**：
- `BeanPostProcessor`：处理 Bean 实例
- `BeanFactoryPostProcessor`：处理 BeanDefinition（Bean 定义信息）

```java
@Component
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // 修改 BeanDefinition
        BeanDefinition bd = beanFactory.getBeanDefinition("userService");
        bd.setScope("prototype");  // 修改作用域
        bd.getPropertyValues().add("name", "modified");  // 修改属性
    }
}
```

---

## 六、循环依赖与三级缓存

### 循环依赖场景

```java
@Service
public class ServiceA {
    @Autowired
    private ServiceB serviceB;
}

@Service
public class ServiceB {
    @Autowired
    private ServiceA serviceA;
}
```

### 三级缓存解决

```java
// Spring 内部三级缓存
Map<String, Object> singletonObjects;        // 一级：成品 Bean
Map<String, Object> earlySingletonObjects;   // 二级：半成品 Bean（已实例化，未注入）
Map<String, ObjectFactory<?>> singletonFactories;  // 三级：Bean 工厂
```

**解决流程**：
```
1. 创建 A → 实例化 A → 放入三级缓存（工厂）
2. A 需要注入 B → 创建 B
3. 创建 B → 实例化 B → 放入三级缓存
4. B 需要注入 A → 从三级缓存获取 A（提前暴露的引用）
5. B 注入 A 成功 → B 完成初始化 → 放入一级缓存
6. A 注入 B 成功 → A 完成初始化 → 放入一级缓存
```

**注意**：构造器注入的循环依赖无法解决！

---

## 七、源码解析

### getBean() 核心流程

```java
// AbstractBeanFactory.doGetBean()
protected <T> T doGetBean(String name, ...) {
    // 1. 从缓存获取
    Object sharedInstance = getSingleton(beanName);
    if (sharedInstance != null) {
        return sharedInstance;
    }
    
    // 2. 创建 Bean
    if (mbd.isSingleton()) {
        sharedInstance = getSingleton(beanName, () -> {
            return createBean(beanName, mbd, args);
        });
    }
    return sharedInstance;
}
```

### createBean() 核心流程

```java
// AbstractAutowireCapableBeanFactory.doCreateBean()
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, Object[] args) {
    // 1. 实例化
    BeanWrapper instanceWrapper = createBeanInstance(beanName, mbd, args);
    Object bean = instanceWrapper.getWrappedInstance();
    
    // 2. 提前暴露（解决循环依赖）
    addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    
    // 3. 属性赋值
    populateBean(beanName, mbd, instanceWrapper);
    
    // 4. 初始化
    Object exposedObject = initializeBean(beanName, bean, mbd);
    
    return exposedObject;
}
```

### initializeBean() 核心流程

```java
protected Object initializeBean(String beanName, Object bean, RootBeanDefinition mbd) {
    // 1. Aware 接口回调
    invokeAwareMethods(beanName, bean);
    
    // 2. BeanPostProcessor 前置处理（@PostConstruct 在此处理）
    Object wrappedBean = applyBeanPostProcessorsBeforeInitialization(bean, beanName);
    
    // 3. 初始化方法（afterPropertiesSet、init-method）
    invokeInitMethods(beanName, wrappedBean, mbd);
    
    // 4. BeanPostProcessor 后置处理（AOP 代理在此创建）
    wrappedBean = applyBeanPostProcessorsAfterInitialization(wrappedBean, beanName);
    
    return wrappedBean;
}
```

---

## 八、高频面试题

**问题 1：Spring Bean 的生命周期有哪些阶段？**

**答案**：

四个主要阶段：
1. **实例化**：调用构造方法创建 Bean 实例
2. **属性赋值**：注入依赖（@Autowired、@Value）
3. **初始化**：Aware 回调 → @PostConstruct → afterPropertiesSet → init-method → AOP 代理
4. **销毁**：@PreDestroy → destroy() → destroy-method

---

**问题 2：@PostConstruct、InitializingBean、init-method 的执行顺序？**

**答案**：

```
@PostConstruct → InitializingBean.afterPropertiesSet() → 自定义 init-method
```

销毁顺序类似：
```
@PreDestroy → DisposableBean.destroy() → 自定义 destroy-method
```

---

**问题 3：BeanPostProcessor 和 BeanFactoryPostProcessor 的区别？**

**答案**：

| 特性 | BeanPostProcessor | BeanFactoryPostProcessor |
|------|-------------------|--------------------------|
| 处理对象 | Bean 实例 | BeanDefinition（Bean 定义信息） |
| 执行时机 | Bean 初始化前后 | 所有 BeanDefinition 加载完成后 |
| 作用 | 修改或代理 Bean | 修改 Bean 定义 |
| 典型应用 | AOP 代理、@Autowired 处理 | 占位符替换、修改作用域 |

---

**问题 4：Spring 如何解决循环依赖？**

**答案**：

通过**三级缓存**解决 Setter/字段注入的循环依赖：

1. **singletonObjects**：完整的 Bean
2. **earlySingletonObjects**：半成品 Bean（已实例化，未注入）
3. **singletonFactories**：Bean 工厂（提前暴露引用）

**流程**：A 创建时放入三级缓存 → A 需要 B → B 创建时从三级缓存获取 A 的引用 → B 完成 → A 完成

**注意**：构造器注入的循环依赖无法解决！

---

**问题 5：AOP 代理是在 Bean生命周期的哪个阶段创建的？**

**答案**：

在 **BeanPostProcessor.postProcessAfterInitialization()** 阶段创建。

具体实现类是 `AbstractAutoProxyCreator`，它在初始化完成后判断是否需要创建代理，如果需要则返回代理对象替代原始 Bean。

---

**问题 6：为什么构造器注入的循环依赖无法解决？**

**答案**：

因为三级缓存的提前暴露机制需要 Bean 先实例化。

构造器注入时，Bean 还没有实例化，无法放入三级缓存，所以无法提前暴露引用。

**解决方案**：
1. 改用 Setter/字段注入
2. 使用 `@Lazy` 延迟加载
3. 重构代码，消除循环依赖

---

**问题 7：Prototype 作用域的 Bean 会调用销毁方法吗？**

**答案**：

**不会！**

Spring 不管理 Prototype Bean 的完整生命周期，只负责创建，不负责销毁。

原因：Prototype Bean 每次请求都创建新实例，Spring 不跟踪这些实例，所以无法在容器关闭时调用销毁方法。

如果需要清理资源，需要手动处理。

---

**问题 8：@Autowired 是在生命周期的哪个阶段处理的？**

**答案**：

在**属性赋值阶段**，由 `AutowiredAnnotationBeanPostProcessor` 处理。

具体是在 `InstantiationAwareBeanPostProcessor.postProcessProperties()` 中处理。

---

**问题 9：如何在 Bean 初始化完成后执行自定义逻辑？**

**答案**：

三种方式（按执行顺序）：

1. **@PostConstruct**（推荐）
   ```java
   @PostConstruct
   public void init() { }
   ```

2. **实现 InitializingBean**
   ```java
   @Override
   public void afterPropertiesSet() { }
   ```

3. **指定 init-method**
   ```java
   @Bean(initMethod = "init")
   ```

推荐使用 `@PostConstruct`，与 Spring 解耦。

---

**问题 10：Spring 容器启动时，Bean 的加载顺序是怎样的？**

**答案**：

1. **加载 BeanDefinition**：扫描配置，解析 Bean 定义
2. **执行 BeanFactoryPostProcessor**：修改 BeanDefinition
3. **注册 BeanPostProcessor**：准备 Bean 处理器
4. **实例化所有非延迟单例 Bean**：按依赖顺序创建
5. **发布 ContextRefreshedEvent**：容器启动完成

可以通过 `@DependsOn` 控制 Bean 创建顺序，但不推荐过度依赖。

---

## 九、最佳实践

### 1. 初始化方法选择

```java
// 推荐：@PostConstruct，与 Spring 解耦
@PostConstruct
public void init() { }

// 不推荐：InitializingBean，与 Spring 耦合
// 不推荐：init-method，需要额外配置
```

### 2. 避免在构造方法中使用依赖

```java
// 错误：构造方法中依赖还没注入
public UserService() {
    userDao.query();  // NullPointerException!
}

// 正确：在 @PostConstruct 中使用
@PostConstruct
public void init() {
    userDao.query();  // OK
}
```

### 3. 优先使用构造器注入

```java
// 推荐：依赖明确，不可变，便于测试
@Service
public class UserService {
    private final UserDao userDao;
    
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }
}
```

### 4. 避免循环依赖

循环依赖是设计问题，应该重构代码：
- 引入中间层
- 使用事件机制解耦
- 延迟加载（@Lazy）

### 5. 正确使用 BeanPostProcessor

```java
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 注意：必须返回 bean 或代理对象
        // 返回 null 会导致 Bean 丢失！
        return bean;
    }
}
```
