## 📖 目录结构

### 完整模块导航

| 模块 | 文档数 | 面试题 | README 导航 |
|------|--------|--------|-------------|
| 🔹 **01-Java基础** | 9 篇 | 45+ 道 | [详细文档](./docs/01-Java基础/README.md) |
| 🔹 **02-Java并发编程** | 8 篇 | 50+ 道 | [详细文档](./docs/02-Java并发编程/README.md) |
| 🔹 **03-JVM** | 1 篇 | 10+ 道 | [详细文档](./docs/03-JVM/README.md) |
| 🔹 **04-Spring框架** | 5 篇 | 35+ 道 | [详细文档](./docs/04-Spring框架/README.md) |
| 🔹 **05-SpringBoot与自动装配** | 1 篇 | 8+ 道 | [详细文档](./docs/05-SpringBoot与自动装配/README.md) |
| 🔹 **06-SpringCloud微服务** | 2 篇 | 25+ 道 | [详细文档](./docs/06-SpringCloud微服务/README.md) |
| 🔹 **07-MySQL数据库** | 7 篇 | 40+ 道 | [详细文档](./docs/07-MySQL数据库/README.md) |
| 🔹 **08-Redis缓存** | 4 篇 | 30+ 道 | [详细文档](./docs/08-Redis缓存/README.md) |
| 🔹 **09-中间件** | 5 篇 | 35+ 道 | [详细文档](./docs/09-中间件/README.md) |
| 🔹 **10-算法与数据结构** | 3 篇 | 35+ 道 | [详细文档](./docs/10-算法与数据结构/README.md) |
| 🔹 **11-设计模式** | 1 篇 | 20+ 道 | [详细文档](./docs/11-设计模式/README.md) |
| 🔹 **12-分布式系统** | 1 篇 | 15+ 道 | [详细文档](./docs/12-分布式系统/README.md) |
| 🔹 **13-DevOps** | 1 篇 | 20+ 道 | [详细文档](./docs/13-DevOps/README.md) |

**总计：** 48 篇文档 · 368+ 道面试题 · 配套 ~30,000 行示例代码

---

### 详细文件结构

```
docs/
├── 01-Java基础/ (9 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-等于与 equals.md
│   ├── 02-String 详解.md
│   ├── 03-数据类型详解.md
│   ├── 04-接口与抽象类.md
│   ├── 05-深拷贝与浅拷贝.md
│   ├── 06-Java 反射详解.md
│   ├── 07-Java 泛型详解.md
│   ├── 08-Java 集合框架详解.md
│   └── 09-Java IO 与 NIO.md
│
├── 02-Java并发编程/ (8 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-Java 集合框架.md
│   ├── 02-ConcurrentHashMap 详解.md
│   ├── 03-AQS详解.md
│   ├── 04-CAS与原子类.md
│   ├── 05-ReentrantLock 生产者消费者.md
│   ├── 06-CountDownLatch 线程顺序执行.md
│   ├── 07-线程池详解.md
│   └── 08-高并发线程安全详解.md
│
├── 03-JVM/ (1 篇文档)
│   ├── README.md ← 模块导航
│   └── 01-JVM 内存模型与垃圾回收.md
│
├── 04-Spring框架/ (5 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-Spring IOC 与 AOP.md
│   ├── 02-Spring 事务传播行为.md
│   ├── 03-Spring MVC 运行机制.md
│   ├── 04-Spring Bean生命周期详解.md
│   └── 05-JDK动态代理与CGLIB.md
│
├── 05-SpringBoot与自动装配/ (1 篇文档)
│   ├── README.md ← 模块导航
│   └── 01-Spring-Boot 自动装配.md
│
├── 06-SpringCloud微服务/ (2 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-Spring-Cloud-Gateway详解.md
│   └── 02-Spring-Cloud-OpenFeign详解.md
│
├── 07-MySQL数据库/ (7 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-MySQL 字段类型与存储引擎.md
│   ├── 02-MySQL 索引原理详解.md
│   ├── 03-MVCC 详解.md
│   ├── 04-SQL 注入与防护.md
│   ├── 05-MyBatis-Plus快速入门.md
│   ├── 06-MyBatis动态 SQL 与 SQL 注入防护.md
│   └── 07-MyBatis 核心原理与面试题.md
│
├── 08-Redis缓存/ (4 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-Redis缓存与分布式锁.md
│   ├── 02-Redis-Sentinel与 Cluster.md
│   ├── 03-Redis 高级应用 - 延时队列与幂等.md
│   └── 04-Redis 高级应用 - 分布式锁与限流.md
│
├── 09-中间件/ (5 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-MyBatis-Plus快速入门.md
│   ├── 02-MyBatis动态 SQL 与 SQL 注入防护.md
│   ├── 03-MyBatis 核心原理与面试题.md
│   ├── 04-Nacos 核心知识点详解.md
│   └── 05-Sentinel限流熔断详解.md
│
├── 10-算法与数据结构/ (3 篇文档)
│   ├── README.md ← 模块导航
│   ├── 01-HashMap源码分析.md
│   ├── 02-树形数据结构详解.md
│   └── 03-算法面试题详解.md
│
├── 11-设计模式/ (1 篇文档)
│   ├── README.md ← 模块导航
│   └── 01-设计模式详解.md
│
├── 12-分布式系统/ (1 篇文档)
│   ├── README.md ← 模块导航
│   └── 01-分布式幂等 - 防重放 - 加密详解.md
│
└── 13-DevOps/ (1 篇文档)
    ├── README.md ← 模块导航
    └── 01-DevOps核心知识点详解.md
```

---

## 🔥 高频面试题分类（链接直达）

### Java基础（45+ 道）
- [`==` 和 `equals` 的区别？](./docs/01-Java基础/01-等于与 equals.md)
- [String、StringBuilder、StringBuffer 区别？](./docs/01-Java基础/02-String 详解.md)
- [接口和抽象类的区别？](./docs/01-Java基础/04-接口与抽象类.md)
- [什么是反射？应用场景？](./docs/01-Java基础/06-Java 反射详解.md)
- [Java 泛型详解？](./docs/01-Java基础/07-Java 泛型详解.md)
- [Java 集合框架体系？](./docs/01-Java基础/08-Java 集合框架详解.md)
- [IO 和 NIO 的区别？](./docs/01-Java基础/09-Java-IO 与 NIO.md)

### Java并发编程（50+ 道）
- [HashMap 和 ConcurrentHashMap 区别？](./docs/02-Java并发编程/02-ConcurrentHashMap 详解.md)
- [AQS 的核心原理？](./docs/02-Java并发编程/03-AQS详解.md)
- [CAS 原理和 ABA 问题？](./docs/02-Java并发编程/04-CAS与原子类.md)
- [synchronized 和 ReentrantLock 区别？](./docs/02-Java并发编程/05-ReentrantLock 生产者消费者.md)
- [线程池的 7 大参数？](./docs/02-Java并发编程/07-线程池详解.md)
- [CountDownLatch 如何使用？](./docs/02-Java并发编程/06-CountDownLatch 线程顺序执行.md)

### Spring（35+ 道）
- [Spring IOC 和 AOP 原理？](./docs/04-Spring框架/01-Spring-IOC 与 AOP.md)
- [Spring Bean 的生命周期？](./docs/04-Spring框架/04-Spring-Bean生命周期详解.md)
- [Spring 事务传播行为？](./docs/04-Spring框架/02-Spring 事务传播行为.md)
- [Spring Boot 自动装配原理？](./docs/05-SpringBoot与自动装配/01-Spring-Boot 自动装配.md)
- [Spring Cloud Gateway 原理？](./docs/06-SpringCloud微服务/01-Spring-Cloud-Gateway详解.md)
- [OpenFeign 工作原理？](./docs/06-SpringCloud微服务/02-Spring-Cloud-OpenFeign详解.md)

### MySQL（40+ 道）
- [InnoDB 和 MyISAM 区别？](./docs/07-MySQL数据库/01-MySQL 字段类型与存储引擎.md)
- [为什么使用 B+Tree 索引？](./docs/07-MySQL数据库/02-MySQL 索引原理详解.md)
- [MVCC 是什么？](./docs/07-MySQL数据库/03-MVCC 详解.md)
- [如何防止 SQL 注入？](./docs/07-MySQL数据库/04-SQL 注入与防护.md)
- [MyBatis 核心原理？](./docs/07-MySQL数据库/07-MyBatis 核心原理与面试题.md)

### Redis（30+ 道）
- [Redis 支持的数据类型？](./docs/08-Redis缓存/01-Redis缓存与分布式锁.md)
- [Redis 哨兵和集群机制？](./docs/08-Redis缓存/02-Redis-Sentinel与 Cluster.md)
- [如何实现分布式锁？](./docs/08-Redis缓存/01-Redis缓存与分布式锁.md)
- [Redis 延时队列实现？](./docs/08-Redis缓存/03-Redis 高级应用 - 延时队列与幂等.md)

### 中间件（35+ 道）
- [Nacos 的核心功能？](./docs/09-中间件/04-Nacos 核心知识点详解.md)
- [MyBatis 的#{}和${}区别？](./docs/09-中间件/02-MyBatis动态 SQL 与 SQL 注入防护.md)
- [Sentinel限流熔断原理？](./docs/09-中间件/05-Sentinel限流熔断详解.md)
- [MyBatis-Plus 优势？](./docs/09-中间件/01-MyBatis-Plus快速入门.md)

### 算法与数据结构（35+ 道）
- [HashMap源码解析](./docs/10-算法与数据结构/01-HashMap源码分析.md)
- [红黑树的特性？](./docs/10-算法与数据结构/02-树形数据结构详解.md)
- [B 树和 B+树的区别？](./docs/10-算法与数据结构/02-树形数据结构详解.md)
- [常见排序算法时间复杂度？](./docs/10-算法与数据结构/03-算法面试题详解.md)

### 设计模式（20+ 道）
- [单例模式实现方式？](./docs/11-设计模式/01-设计模式详解.md)
- [工厂模式和抽象工厂区别？](./docs/11-设计模式/01-设计模式详解.md)
- [Spring 中用到了哪些设计模式？](./docs/11-设计模式/01-设计模式详解.md)

### 分布式系统（15+ 道）
- [如何保证接口幂等性？](./docs/12-分布式系统/01-分布式幂等 - 防重放 - 加密详解.md)
- [如何防止接口重放攻击？](./docs/12-分布式系统/01-分布式幂等 - 防重放 - 加密详解.md)
- [分布式锁实现方案？](./docs/12-分布式系统/01-分布式幂等 - 防重放 - 加密详解.md)

### DevOps（20+ 道）
- [Docker 的优势？](./docs/13-DevOps/01-DevOps核心知识点详解.md)
- [Nginx 负载均衡策略？](./docs/13-DevOps/01-DevOps核心知识点详解.md)
- [CI/CD流程？](./docs/13-DevOps/01-DevOps核心知识点详解.md)

---

## 📊 知识点覆盖统计

| 分类 | 文档数 | 面试题数 | 重要程度 |
|------|--------|----------|----------|
| Java基础 | 9 | 45+ | ⭐⭐⭐⭐⭐ |
| Java并发编程 | 8 | 50+ | ⭐⭐⭐⭐⭐ |
| JVM | 1 | 10+ | ⭐⭐⭐⭐ |
| Spring框架 | 5 | 35+ | ⭐⭐⭐⭐⭐ |
| SpringBoot与自动装配 | 1 | 8+ | ⭐⭐⭐⭐ |
| SpringCloud微服务 | 2 | 25+ | ⭐⭐⭐⭐ |
| MySQL数据库 | 7 | 40+ | ⭐⭐⭐⭐⭐ |
| Redis缓存 | 4 | 30+ | ⭐⭐⭐⭐⭐ |
| 中间件 | 5 | 35+ | ⭐⭐⭐⭐ |
| 算法与数据结构 | 3 | 35+ | ⭐⭐⭐⭐ |
| 设计模式 | 1 | 20+ | ⭐⭐⭐ |
| 分布式系统 | 1 | 15+ | ⭐⭐⭐⭐ |
| DevOps | 1 | 20+ | ⭐⭐⭐ |
| **总计** | **48** | **368+** | - |

---

## 🎯 学习路线建议

### 第一阶段：Java基础（1-2周）
1. ✅ Java基础语法和数据类型
2. ✅ 面向对象（接口、抽象类）
3. ✅ 泛型和反射
4. ✅ 类加载机制

### 第二阶段：Java并发编程（2-3周）
1. ✅ 集合框架（HashMap、ConcurrentHashMap）
2. ✅ 线程池和并发工具类
3. ✅ AQS 和锁机制
4. ✅ CAS 和原子类

### 第三阶段：JVM（1周）
1. ✅ 内存模型
2. ✅ 垃圾回收机制

### 第四阶段：Spring框架（2-3周）
1. ✅ IOC 和 AOP 原理
2. ✅ Bean生命周期
3. ✅ 事务管理
4. ✅ Spring MVC

### 第五阶段：数据库（2周）
1. ✅ MySQL 索引和事务
2. ✅ SQL 优化
3. ✅ Redis缓存

### 第六阶段：微服务（2周）
1. ✅ Spring Boot 自动装配
2. ✅ Spring Cloud Gateway
3. ✅ OpenFeign
4. ✅ Sentinel限流熔断

### 第七阶段：算法与数据结构（持续）
1. ✅ HashMap源码
2. ✅ 树形数据结构
3. ✅ 常见算法题

### 第八阶段：设计模式（1周）
1. ✅ 创建型模式（单例、工厂、建造者）
2. ✅ 结构型模式（代理、装饰器）
3. ✅ 行为型模式（策略、观察者）

### 第九阶段：分布式系统（1-2周）
1. ✅ 幂等性设计
2. ✅ 分布式锁
3. ✅ 防重放攻击

### 第十阶段：DevOps（1 周）
1. ✅ Docker容器化
2. ✅ Nginx配置
3. ✅ CI/CD流程

---

## 💡 使用建议

1. **配合示例代码学习**：每个文档都有对应的示例代码在 `interview-service` 模块中
2. **重点突破**：优先掌握标⭐⭐⭐⭐⭐的知识点
3. **理解原理**：不要死记硬背，理解背后的设计思想
4. **实战练习**：运行示例代码，加深理解
5. **定期复习**：周期性回顾，形成知识体系
6. **利用导航**：点击各模块 README.md 中的链接可快速跳转文档

---

## 🔗 快速导航

### 模块级导航（推荐）
- [📘 01-Java基础](./docs/01-Java基础/README.md) - 9 篇文档，45+ 道面试题
- [📘 02-Java并发编程](./docs/02-Java并发编程/README.md) - 8 篇文档，50+ 道面试题
- [📘 03-JVM](./docs/03-JVM/README.md) - 1 篇文档，10+ 道面试题
- [📘 04-Spring框架](./docs/04-Spring框架/README.md) - 5 篇文档，35+ 道面试题
- [📘 05-SpringBoot与自动装配](./docs/05-SpringBoot与自动装配/README.md) - 1 篇文档，8+ 道面试题
- [📘 06-SpringCloud微服务](./docs/06-SpringCloud微服务/README.md) - 2 篇文档，25+ 道面试题
- [📘 07-MySQL数据库](./docs/07-MySQL数据库/README.md) - 7 篇文档，40+ 道面试题
- [📘 08-Redis缓存](./docs/08-Redis缓存/README.md) - 4 篇文档，30+ 道面试题
- [📘 09-中间件](./docs/09-中间件/README.md) - 5 篇文档，35+ 道面试题
- [📘 10-算法与数据结构](./docs/10-算法与数据结构/README.md) - 3 篇文档，35+ 道面试题
- [📘 11-设计模式](./docs/11-设计模式/README.md) - 1 篇文档，20+ 道面试题
- [📘 12-分布式系统](./docs/12-分布式系统/README.md) - 1 篇文档，15+ 道面试题
- [📘 13-DevOps](./docs/13-DevOps/README.md) - 1 篇文档，20+ 道面试题
