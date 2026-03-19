# Spring 注解生命周期详解 - @PostConstruct 与@PreDestroy

## 📚 目录

1. [核心注解介绍](#一核心注解介绍)
2. [执行顺序详解](#二执行顺序详解)
3. [完整示例代码](#三完整示例代码)
4. [高频面试题](#四高频面试题)

---

## 一、核心注解介绍

### 1.1 @PostConstruct

**定义：** JSR-250 规范定义的注解，用于在依赖注入完成后执行初始化逻辑。

**特点：**

- ✅ **来源：** JDK 标准（`javax.annotation.PostConstruct`），不是 Spring 的注解
- ✅ **时机：** 在构造方法之后、依赖注入完成时立即执行
- ✅ **优先级：** 最高（第一个执行的初始化方法）
- ✅ **限制：**
    - 只能有一个方法标注此注解
    - 方法必须是非静态的
    - 不能有参数
    - 返回值会被忽略
    - 不能抛出受检异常（Checked Exception）

**典型应用场景：**

```java
@PostConstruct
public void init() {
    // 1. 资源初始化
    connectionPool = new ConnectionPool();
    
    // 2. 配置校验
    if (config == null) {
        throw new IllegalStateException("Config must not be null");
    }
    
    // 3. 缓存预热
    loadCache();
}
```

---

### 1.2 @PreDestroy

**定义：** JSR-250 规范定义的注解，用于在 Bean 销毁前执行清理逻辑。

**特点：**

- ✅ **来源：** JDK 标准（`javax.annotation.PreDestroy`），不是 Spring 的注解
- ✅ **时机：** 在容器关闭时、Bean 销毁前执行
- ✅ **优先级：** 最高（第一个执行的销毁方法）
- ✅ **限制：**
    - 只能有一个方法标注此注解
    - 方法必须是非静态的
    - 不能有参数
    - 返回值会被忽略
    - 不能抛出受检异常

**典型应用场景：**

```java
@PreDestroy
public void cleanup() {
    // 1. 释放数据库连接
    connectionPool.close();
    
    // 2. 保存状态到磁盘
    saveState();
    
    // 3. 关闭线程池
    executorService.shutdown();
}
```

**⚠️ 注意事项：**

- 只有容器**正常关闭**时才会调用（如 `context.close()`）
- 使用 `System.exit()` 或直接杀死进程**不会调用**
- 必须在容器启动时注册了关闭钩子（Shutdown Hook）

---

### 1.3 @Bean(initMethod / destroyMethod)

**定义：** Spring框架提供的注解，用于指定 Bean 的初始化和销毁方法。

**特点：**

- ✅ **来源：** Spring框架（`org.springframework.context.annotation.Bean`）
- ✅ **时机：**
    - `initMethod`：在初始化阶段最后执行
    - `destroyMethod`：在销毁阶段最后执行
- ✅ **优先级：** 最低
- ✅ **灵活性：** 可以自定义方法名

**使用示例：**

```java
@Configuration
class AppConfig {
    @Bean(initMethod = "start", destroyMethod = "stop")
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}

class HikariDataSource {
    public void start() {
        // 初始化逻辑
    }
    
    public void stop() {
        // 销毁逻辑
    }
}
```

---

## 二、执行顺序详解

### 2.1 初始化阶段（3 个方法的执行顺序）

```
┌──────────────────────────────────────────┐
│ 第 1 个：@PostConstruct                    │
│      （JSR-250，JDK 标准，优先级最高）     │
├──────────────────────────────────────────┤
│ 第 2 个：InitializingBean.afterPropertiesSet() │
│      （Spring 接口，优先级中等）            │
├──────────────────────────────────────────┤
│ 第 3 个：@Bean(initMethod) 或 XML init-method │
│      （自定义方法，优先级最低）             │
└──────────────────────────────────────────┘
```

**记忆口诀：** 先 JDK 后 Spring 最后自定义

**详细流程：**

```java
// 1. 构造方法创建 Bean 实例
UserService userService = new UserService();

// 2. 依赖注入（@Autowired、@Value）
userService.setServiceName("MyService");

// 3. 【第 1 个执行】@PostConstruct 方法
userService.postConstruct();

// 4. 【第 2 个执行】InitializingBean 接口方法
if (userService instanceof InitializingBean) {
    ((InitializingBean) userService).afterPropertiesSet();
}

// 5. 【第 3 个执行】@Bean(initMethod) 指定的方法
// 通过反射调用
Method initMethod = userService.getClass().getMethod("init");
initMethod.invoke(userService);
```

---

### 2.2 销毁阶段（3 个方法的执行顺序）

```
┌──────────────────────────────────────────┐
│ 第 1 个：@PreDestroy                       │
│      （JSR-250，JDK 标准，优先级最高）     │
├──────────────────────────────────────────┤
│ 第 2 个：DisposableBean.destroy()          │
│      （Spring 接口，优先级中等）            │
├──────────────────────────────────────────┤
│ 第 3 个：@Bean(destroyMethod) 或 XML destroy-method │
│      （自定义方法，优先级最低）             │
└──────────────────────────────────────────┘
```

**记忆口诀：** 先 JDK 后 Spring 最后自定义

**详细流程：**

```java
// 1. 【第 1 个执行】@PreDestroy 方法
userService.preDestroy();

// 2. 【第 2 个执行】DisposableBean 接口方法
if (userService instanceof DisposableBean) {
    ((DisposableBean) userService).destroy();
}

// 3. 【第 3 个执行】@Bean(destroyMethod) 指定的方法
// 通过反射调用
Method destroyMethod = userService.getClass().getMethod("cleanup");
destroyMethod.invoke(userService);
```

---

## 三、完整示例代码

### 3.1 主程序

```java
package cn.itzixiao.interview.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class AnnotationLifecycleDemo {
    public static void main(String[] args) {
        System.out.println("========== Spring 注解生命周期详解 ==========\n");

        System.out.println("【步骤 1：创建 Spring 容器】\n");
        ApplicationContext context = 
            new AnnotationConfigApplicationContext(AppConfig.class);

        System.out.println("\n【步骤 2：从容器获取 Bean】\n");
        UserService userService = context.getBean(UserService.class);
        
        System.out.println("\n【步骤 3：执行业务方法】\n");
        userService.process();

        System.out.println("\n【步骤 4：关闭容器】\n");
        ((AnnotationConfigApplicationContext) context).close();
    }
}
```

### 3.2 配置类

```java
@Configuration
class AppConfig {
    
    /**
     * 使用 @Bean 注解定义 Bean
     * initMethod = "init" - 指定初始化方法
     * destroyMethod = "cleanup" - 指定销毁方法
     */
    @Bean(name = "userService", initMethod = "init", destroyMethod = "cleanup")
    public UserService userService() {
        return new UserService();
    }
}
```

### 3.3 Bean 实现类

```java
@Component
class UserService {
    
    private String serviceName;
    
    // ========== 1. 构造方法（实例化）==========
    public UserService() {
        System.out.println("✓ [构造方法] UserService 实例化\n");
    }
    
    // ========== 2. Setter 注入（属性赋值）==========
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
        System.out.println("✓ [Setter 注入] serviceName = " + serviceName + "\n");
    }
    
    // ========== 3. @PostConstruct 初始化方法==========
    @PostConstruct
    public void postConstruct() {
        System.out.println("✓ [@PostConstruct] 注解初始化方法");
        System.out.println("  - 优先级最高，最先执行\n");
    }
    
    // ========== 4. @Bean 指定的初始化方法==========
    public void init() {
        System.out.println("✓ [@Bean(initMethod)] 自定义初始化方法 init()");
        System.out.println("  - 优先级最低，最后执行\n");
    }
    
    // ========== 5. 业务方法（使用阶段）==========
    public void process() {
        System.out.println("✓ [业务方法] process() - Bean 正在使用中\n");
    }
    
    // ========== 6. @PreDestroy 销毁方法==========
    @PreDestroy
    public void preDestroy() {
        System.out.println("✓ [@PreDestroy] 注解销毁方法");
        System.out.println("  - 优先级最高，最先执行\n");
    }
    
    // ========== 7. @Bean 指定的销毁方法==========
    public void cleanup() {
        System.out.println("✓ [@Bean(destroyMethod)] 自定义销毁方法 cleanup()\n");
    }
}
```

### 3.4 执行结果

```
========== Spring 注解生命周期详解 ==========

【步骤 1：创建 Spring 容器】

✓ [构造方法] UserService 实例化
✓ [Setter 注入] serviceName = userService
✓ [@PostConstruct] 注解初始化方法
  - 优先级最高，最先执行
✓ [@Bean(initMethod)] 自定义初始化方法 init()
  - 优先级最低，最后执行

【步骤 2：从容器获取 Bean】


【步骤 3：执行业务方法】

✓ [业务方法] process() - Bean 正在使用中

【步骤 4：关闭容器】

✓ [@PreDestroy] 注解销毁方法
  - 优先级最高，最先执行
✓ [@Bean(destroyMethod)] 自定义销毁方法 cleanup()
```

---

## 四、高频面试题

### ❓ 问题 1：@PostConstruct 和 InitializingBean 的区别？

**答：**

| 对比项      | @PostConstruct  | InitializingBean |
|----------|-----------------|------------------|
| **来源**   | JDK 标准（JSR-250） | Spring框架         |
| **优先级**  | 高（最先执行）         | 中（第二个执行）         |
| **耦合度**  | 低（普通 Java 类也能用） | 高（依赖 Spring API） |
| **数量限制** | 只能有一个方法         | 可以实现多次           |
| **推荐度**  | ⭐⭐⭐⭐⭐ 推荐        | ⭐⭐⭐ 不推荐          |

**最佳实践：** 优先使用 `@PostConstruct`，避免依赖 Spring API。

---

### ❓ 问题 2：为什么@PreDestroy 没有被调用？

**答：** 常见原因有：

1. **容器没有正常关闭**
   ```java
   // ❌ 错误：直接退出 JVM
   System.exit(0);
   
   // ✅ 正确：正常关闭容器
   context.close();
   ```

2. **Bean 不是由 Spring 管理的**
   ```java
   // ❌ 错误：手动创建的 Bean
   UserService service = new UserService();
   
   // ✅ 正确：从容器获取
   UserService service = context.getBean(UserService.class);
   ```

3. **没有注册关闭钩子**
   ```java
   // ✅ 正确：注册关闭钩子
   context.registerShutdownHook();
   ```

---

### ❓ 问题 3：@PostConstruct 可以抛出异常吗？

**答：**

**不可以！** `@PostConstruct` 方法不能抛出受检异常（Checked Exception）。

**原因：**

- 如果抛出异常，会导致 Bean 初始化失败
- Spring 容器会抛出 `BeanCreationException`
- 整个应用上下文无法启动

**示例：**

```java
// ❌ 错误：不能抛出受检异常
@PostConstruct
public void init() throws IOException {
    // ...
}

// ✅ 正确：抛出运行时异常
@PostConstruct
public void init() {
    if (config == null) {
        throw new IllegalStateException("Config must not be null");
    }
}
```

---

### ❓ 问题 4：多个初始化方法的执行顺序？

**答：** 严格执行以下顺序：

```
1. @PostConstruct（JDK 标准）
   ↓
2. InitializingBean.afterPropertiesSet()（Spring 接口）
   ↓
3. @Bean(initMethod) 或 XML init-method（自定义方法）
```

**记忆技巧：**

- 先国际标准（JDK）
- 再框架标准（Spring）
- 最后个人标准（自定义）

---

### ❓ 问题 5：@PostConstruct 和@Autowired 谁先执行？

**答：** **@Autowired 先执行**（依赖注入完成），然后才执行 `@PostConstruct`。

**完整顺序：**

```
1. 构造方法
   ↓
2. @Autowired 依赖注入
   ↓
3. @PostConstruct 初始化方法
```

**原因：** `@PostConstruct` 的设计初衷就是在依赖注入完成后执行初始化逻辑。

**示例：**

```java
@Component
public class UserService {
    
    @Autowired
    private UserRepository userRepository;  // 2. 先注入
    
    @PostConstruct
    public void init() {
        // 3. 后执行，此时 userRepository 已经不为 null
        userRepository.findAll();  // ✅ 安全
    }
}
```

---

## 💡 最佳实践建议

### ✅ 推荐做法

1. **优先使用@PostConstruct**
   ```java
   @Component
   public class MyService {
       @PostConstruct
       public void init() {
           // 初始化逻辑
       }
   }
   ```

2. **配合@Bean 的 initMethod/destroyMethod**
   ```java
   @Configuration
   public class AppConfig {
       @Bean(initMethod = "start", destroyMethod = "stop")
       public DataSource dataSource() {
           return new HikariDataSource();
       }
   }
   ```

3. **在@PreDestroy 中释放资源**
   ```java
   @PreDestroy
   public void cleanup() {
       connectionPool.close();
       executorService.shutdown();
   }
   ```

### ❌ 避免的做法

1. **不要混用多种初始化方式**
   ```java
   // ❌ 不推荐：同时使用三种方式
   @Component
   public class MyBean implements InitializingBean {
       @PostConstruct
       public void jsrInit() {}
       
       @Override
       public void afterPropertiesSet() {}
       
       public void customInit() {}
   }
   ```

2. **不要在@PostConstruct 中做耗时操作**
   ```java
   // ❌ 不推荐
   @PostConstruct
   public void init() {
       Thread.sleep(10000);  // 阻塞启动
   }
   ```

---

**作者：** itzixiao  
**版本：** 1.0  
**最后更新：** 2026-03-08
