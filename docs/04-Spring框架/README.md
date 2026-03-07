# Spring框架知识点详解

## 📚 文档列表

#### 1. [01-Spring IOC与 AOP.md](./01-Spring-IOC 与 AOP.md)
- **内容：** IOC 容器、依赖注入、AOP 原理
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 2. [02-Spring Bean生命周期详解.md](./02-Spring-Bean生命周期详解.md)
- **内容：** Bean 创建、初始化、销毁全过程
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-Spring MVC 运行机制.md](./03-Spring-MVC 运行机制.md)
- **内容：** DispatcherServlet、处理器映射、视图解析
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 4. [04-Spring 事务传播行为.md](./04-Spring 事务传播行为.md)
- **内容：** 7 种事务传播行为、应用场景
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 5. [05-JDK 动态代理与 CGLIB.md](./05-JDK 动态代理与 CGLIB.md)
- **内容：** 两种代理方式对比、Spring AOP 选择策略
- **面试题：** 6+ 道
- **重要程度：** ⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 5 个
- **面试题总数：** 42+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/spring/` 目录（5 个文件，~1,800 行代码）

---

## 🎯 学习建议

### 第一阶段：核心概念（3-4 天）
1. **IOC 容器**
   - BeanFactory vs ApplicationContext
   - 依赖注入方式（构造器、Setter、接口）
   - Bean 的作用域（singleton、prototype 等）

2. **AOP 原理**
   - 面向切面编程思想
   - 动态代理机制（JDK vs CGLIB）
   - 通知类型（Before、After、Around）

### 第二阶段：Bean 生命周期（2-3 天）
1. **完整生命周期**
   - 实例化 → 属性赋值 → 初始化 → 销毁
   - Aware 接口回调
   - BeanPostProcessor 后置处理器

2. **循环依赖解决**
   - 三级缓存机制
   - 提前暴露引用

### 第三阶段：事务管理（2-3 天）
1. **声明式事务**
   - @Transactional 注解
   - 7 种传播行为
   - 5 种隔离级别

2. **事务失效场景**
   - 非 public 方法
   - 自调用问题
   - 异常被吞

### 第四阶段：MVC 与实战（2-3 天）
1. **Spring MVC 流程**
   - DispatcherServlet 核心作用
   - HandlerMapping、HandlerAdapter
   - 视图解析器

2. **常见注解**
   - @Controller、@Service、@Repository
   - @RequestMapping、@GetMapping
   - @RequestBody、@ResponseBody

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Java基础](../01-Java基础/README.md)** - 反射机制、类加载
- ✅ **[Java并发编程](../02-Java并发编程/README.md)** - 线程安全、volatile

### 后续进阶
- 📚 **[SpringBoot](../05-SpringBoot 与自动装配/README.md)** - 自动配置、starter
- 📚 **[SpringCloud](../06-SpringCloud 微服务/README.md)** - 微服务架构
- 📚 **[MyBatis](../09-中间件/README.md)** - ORM 框架集成

### 知识点对应
| Spring | 应用场景 |
|--------|---------|
| IOC/DI | Bean 管理、解耦 |
| AOP | 日志、事务、权限控制 |
| 事务传播 | 多数据源、复杂业务 |
| Bean 生命周期 | 自定义扩展点 |
| 动态代理 | MyBatis Mapper、Feign Client |

---

## 💡 高频面试题 Top 15

1. **什么是 IOC？有什么好处？**
2. **DI 有哪些方式？推荐使用哪种？**
3. **AOP 的实现原理？JDK 和 CGLIB 的区别？**
4. **Spring Bean 的生命周期是怎样的？**
5. **BeanPostProcessor 有什么作用？**
6. **Spring 如何解决循环依赖？**
7. **Spring 事务的传播行为有哪些？**
8. **@Transactional 注解失效的场景有哪些？**
9. **Spring MVC 的工作流程？**
10. **@Autowired 和@Resource 的区别？**
11. **Spring 中用到了哪些设计模式？**
12. **Spring 单例 Bean 是线程安全的吗？**
13. **如何自定义一个 Spring Starter？**
14. **Spring 事件机制是如何工作的？**
15. **Spring Boot 相比 Spring 有什么优势？**

---

## 🛠️ 实战技巧

### 自定义 BeanPostProcessor
```java
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("Before: " + beanName);
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("After: " + beanName);
        return bean;
    }
}
```

### 使用@Async 实现异步调用
```java
@Service
public class AsyncService {
    
    @Async
    public void doAsyncTask() {
        // 异步执行的任务
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor taskExecutor() {
        return new ThreadPoolExecutor(...);
    }
}
```

---

## 📖 推荐学习顺序

```
IOC 与 DI
   ↓
AOP 原理
   ↓
Bean 生命周期
   ↓
循环依赖解决
   ↓
事务管理
   ↓
Spring MVC
   ↓
综合实战
```

---

## 📈 更新日志

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 42+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础 Spring 知识点文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
