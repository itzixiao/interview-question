# 🎯 面试题库微服务项目

> **一站式 Java 面试准备平台** - 48 篇文档 · 368+ 道面试题 · 配套 ~30,000 行示例代码

---

## 📁 项目架构

### 完整层级结构

```
interview-question/                              # 根目录
│
├── interview-common/                            # 公共模块
│   ├── exception/                               # 业务异常类
│   ├── handler/                                 # 全局异常处理器
│   └── result/                                  # 统一返回结果
│
├── interview-gateway/                           # API 网关服务
│   ├── config/                                  # 网关配置
│   ├── filter/                                  # 自定义过滤器
│   └── handler/                                 # 异常处理器
│
├── interview-microservices/                     # 微服务模块集合（父模块）
│   ├── interview-algorithm/                     # 算法面试题库
│   ├── interview-provider/                      # 服务提供者
│   └── interview-service/                       # 业务服务
│
├── interview-starters/                          # Starter 模块集合（父模块）
│   └── interview-threadpool-starter/            # 线程池Starter
│
├── docs/                                        # 面试知识点文档（15 个分类）
│   ├── 01-Java基础/
│   ├── 02-Java并发编程/
│   ├── 03-JVM/
│   ├── 04-Spring框架/
│   ├── 05-SpringBoot与自动装配/
│   ├── 06-SpringCloud微服务/
│   ├── 07-MySQL数据库/
│   ├── 08-Redis缓存/
│   ├── 09-中间件/
│   ├── 10-算法与数据结构/
│   ├── 11-设计模式/
│   ├── 12-分布式系统/
│   ├── 13-DevOps/
│   ├── 14-SPI与 IO模型/
│   └── 15-工程效能/
```

### 模块职责说明

| 模块 | 职责 | 说明 |
|------|------|------|
| **interview-common** | 公共模块 | 提供统一的异常处理、返回结果等公共组件 |
| **interview-gateway** | API 网关 | 基于 Spring Cloud Gateway，负责路由、过滤、限流等 |
| **interview-microservices** | 微服务集合（父模块） | 包含所有业务微服务模块 |
| **interview-starters** | Starter 集合（父模块） | 包含自定义的 Spring Boot Starter |
| **interview-algorithm** | 算法题库 | 包含常见算法面试题及实现 |
| **interview-provider** | 服务提供者 | 提供 RESTful API 服务，演示 OpenFeign 调用 |
| **interview-service** | 业务服务 | 包含所有业务逻辑和技术点示例代码 |
| **interview-threadpool-starter** | 线程池Starter | 自定义线程池自动配置 Starter |

### 技术栈

- **Java 8** + **Spring Boot 2.7.18** + **Spring Cloud 2021.0.8**
- **Spring Cloud Alibaba 2021.0.5.0** (Nacos, Sentinel, Gateway, OpenFeign)
- **MySQL 8.0** + **Redis**
- **Maven** 多模块项目管理

### 快速开始

```bash
# 编译整个项目
mvn clean compile -DskipTests

# 安装到本地仓库
mvn clean install -DskipTests

# 启动服务
cd interview-microservices/interview-provider
mvn spring-boot:run
```

---

## 📖 文档导航

### 完整模块导航

| 模块 | 文档数 | 面试题 | README 导航 |
|------|--------|--------|-------------|
| 🔹 **01-Java基础** | 9 篇 | 45+ 道 | [详细文档](./docs/01-Java基础/README.md) |
| 🔹 **02-Java并发编程** | 9 篇 | 98+ 道 | [详细文档](./docs/02-Java并发编程/README.md) |
| 🔹 **03-JVM** | 1 篇 | 10+ 道 | [详细文档](./docs/03-JVM/README.md) |
| 🔹 **04-Spring框架** | 8 篇 | 62+ 道 | [详细文档](./docs/04-Spring框架/README.md) |
| 🔹 **05-SpringBoot与自动装配** | 4 篇 | 20+ 道 | [详细文档](./docs/05-SpringBoot与自动装配/README.md) |
| 🔹 **06-SpringCloud微服务** | 2 篇 | 25+ 道 | [详细文档](./docs/06-SpringCloud微服务/README.md) |
| 🔹 **07-MySQL数据库** | 9 篇 | 50+ 道 | [详细文档](./docs/07-MySQL数据库/README.md) |
| 🔹 **08-Redis缓存** | 4 篇 | 30+ 道 | [详细文档](./docs/08-Redis缓存/README.md) |
| 🔹 **09-中间件** | 11 篇 | 106+ 道 | [详细文档](./docs/09-中间件/README.md) |
| 🔹 **10-算法与数据结构** | 4 篇 | 45+ 道 | [详细文档](./docs/10-算法与数据结构/README.md) |
| 🔹 **11-设计模式** | 1 篇 | 20+ 道 | [详细文档](./docs/11-设计模式/README.md) |
| 🔹 **12-分布式系统** | 1 篇 | 15+ 道 | [详细文档](./docs/12-分布式系统/README.md) |
| 🔹 **13-DevOps** | 2 篇 | 25+ 道 | [详细文档](./docs/13-DevOps/README.md) |
| 🔹 **14-SPI与 IO模型** | 4 篇 | 22+ 道 | [详细文档](./docs/14-SPI与 IO模型/README.md) |
| 🔹 **15-工程效能** | 6 篇 | 26+ 道 | [详细文档](./docs/15-工程效能/README.md) |

**总计：** 61 篇文档 · 579+ 道面试题 · 配套 ~40,000 行示例代码

---

## 🔥 高频面试题分类（链接直达）

### Java基础（45+ 道）
- [`==` 和 equals 的区别？](./docs/01-Java基础/01-==与equals.md)
- [String、StringBuilder、StringBuffer 区别？](./docs/01-Java基础/02-String详解.md)
- [接口和抽象类的区别？](./docs/01-Java基础/04-接口与抽象类.md)
- [什么是反射？应用场景？](./docs/01-Java基础/07-反射机制详解.md)
- [Java 泛型详解？](./docs/01-Java基础/06-泛型深入详解.md)
- [Java 集合框架体系？](./docs/01-Java基础/08-类加载机制详解.md)
- [IO 和 NIO 的区别？](./docs/01-Java基础/09-Java-IO与NIO.md)

### Java并发编程（50+ 道）
- [HashMap 和 ConcurrentHashMap 区别？](./docs/02-Java并发编程/01-HashMap源码分析.md)
- [AQS 的核心原理？](./docs/02-Java并发编程/03-AQS详解.md)
- [CAS 原理和 ABA 问题？](./docs/02-Java并发编程/04-CAS与原子类.md)
- [synchronized 和 ReentrantLock 区别？](./docs/02-Java并发编程/05-ReentrantLock生产者消费者.md)
- [线程池的 7 大参数？](./docs/02-Java并发编程/07-线程池详解.md)
- [CountDownLatch 如何使用？](./docs/02-Java并发编程/06-CountDownLatch线程顺序执行.md)

### Spring（35+ 道）
- [Spring IOC 和 AOP 原理？](./docs/04-Spring框架/01-Spring-IOC与AOP.md)
- [Spring Bean 的生命周期？](./docs/04-Spring框架/04-Spring事务传播行为.md)
- [Spring事务传播行为？](./docs/04-Spring框架/02-Spring事务传播行为.md)
- [Spring Boot 自动装配原理？](./docs/05-SpringBoot与自动装配/01-Spring-Boot自动装配.md)
- [Spring Cloud Gateway 原理？](./docs/06-SpringCloud微服务/01-Spring-Cloud-Gateway详解.md)
- [OpenFeign 工作原理？](./docs/06-SpringCloud微服务/02-Spring-Cloud-OpenFeign详解.md)

### MySQL（40+ 道）
- [InnoDB 和 MyISAM 区别？](./docs/07-MySQL数据库/01-MySQL字段类型与存储引擎.md)
- [为什么使用 B+Tree 索引？](./docs/07-MySQL数据库/02-MySQL索引原理详解.md)
- [MySQL索引与MVCC](docs/07-MySQL数据库/05-MySQL索引与MVCC.md)
- [如何防止 SQL注入？](./docs/07-MySQL数据库/04-MySQL日志与性能优化详解.md)
- [ShardingSphere 分库分表实战](./docs/07-MySQL数据库/09-ShardingSphere 整合实战指南.md)
- [MyBatis核心原理？](./docs/09-中间件/03-MyBatis核心原理与面试题.md)

### Redis（30+ 道）
- [Redis 支持的数据类型？](./docs/08-Redis缓存/01-Redis缓存与分布式锁.md)
- [Redis 哨兵和集群机制？](./docs/08-Redis缓存/02-Redis-Sentinel与Cluster.md)
- [如何实现分布式锁？](./docs/08-Redis缓存/01-Redis缓存与分布式锁.md)
- [Redis 延时队列实现？](./docs/08-Redis缓存/03-Redis持久化详解.md)

### 中间件（35+ 道）
- [Nacos 的核心功能？](./docs/09-中间件/04-Nacos核心知识点详解.md)
- [MyBatis 的#{}和${}区别？](./docs/09-中间件/02-MyBatis动态SQL与SQL注入防护.md)
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
- [分布式幂等-防重放-加密详解.md](docs/12-%E5%88%86%E5%B8%83%E5%BC%8F%E7%B3%BB%E7%BB%9F/01-%E5%88%86%E5%B8%83%E5%BC%8F%E5%B9%82%E7%AD%89-%E9%98%B2%E9%87%8D%E6%94%BE-%E5%8A%A0%E5%AF%86%E8%AF%A6%E8%A7%A3.md)

### DevOps（25+ 道）
- [Docker 的优势？](./docs/13-DevOps/01-DevOps%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)
- [Nginx 负载均衡策略？](./docs/13-DevOps/01-DevOps%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)
- [CI/CD流程？](./docs/13-DevOps/01-DevOps%E6%A0%B8%E5%BF%83%E7%9F%A5%E8%AF%86%E7%82%B9%E8%AF%A6%E8%A7%A3.md)
- [容器化与云原生详解](./docs/13-DevOps/02-容器化与云原生详解.md)

### SPI与IO模型（22+ 道）
- [什么是 SPI？工作原理是什么？](docs/14-SPI与IO模型/01-SPI机制详解.md)
- [BIO、NIO、AIO 的区别？](./docs/14-SPI与IO模型/README.md#九 - 三种 io-模型对比)
- [NIO 的核心组件？](docs/14-SPI与IO模型/03-NIO同步非阻塞IO.md)
- [Reactor 模式的三种变体？](docs/14-SPI与IO模型/03-NIO同步非阻塞IO.md#73-reactor-模式)
- [Netty 为什么性能高？](./docs/14-SPI与IO模型/README.md#十二 - 主流框架)

### 工程效能（26+ 道） ⭐新增
- [代码生成器的核心原理？](./docs/15-工程效能/02-代码生成器设计.md)
- [动态表单引擎的设计思路？](./docs/15-工程效能/03-动态表单引擎.md)
- [Activiti 的核心组件？](docs/15-工程效能/04-流程引擎Activiti.md)
- [Drools 的工作原理？](docs/15-工程效能/05-规则引擎Drools.md)
- [POI 生成 Excel 的优化技巧？](./docs/15-工程效能/06-报表引擎.md)

---

## 📊 知识点覆盖统计

| 分类 | 文档数 | 面试题数 | 重要程度 |
|------|--------|----------|----------|
| Java基础 | 9 | 45+ | ⭐⭐⭐⭐⭐ |
| Java并发编程 | 9 | 98+ | ⭐⭐⭐⭐⭐ |
| JVM | 1 | 10+ | ⭐⭐⭐⭐ |
| Spring框架 | 8 | 62+ | ⭐⭐⭐⭐⭐ |
| SpringBoot与自动装配 | 4 | 20+ | ⭐⭐⭐⭐ |
| SpringCloud微服务 | 2 | 25+ | ⭐⭐⭐⭐ |
| MySQL数据库 | 9 | 50+ | ⭐⭐⭐⭐⭐ |
| Redis缓存 | 4 | 30+ | ⭐⭐⭐⭐⭐ |
| 中间件 | 11 | 106+ | ⭐⭐⭐⭐ |
| 算法与数据结构 | 4 | 45+ | ⭐⭐⭐⭐ |
| 设计模式 | 1 | 20+ | ⭐⭐⭐ |
| 分布式系统 | 1 | 15+ | ⭐⭐⭐⭐ |
| DevOps | 2 | 25+ | ⭐⭐⭐ |
| **SPI与IO模型** | **4** | **22+** | **⭐⭐⭐⭐⭐** |
| **工程效能** | **6** | **26+** | **⭐⭐⭐⭐** |
| **总计** | **61** | **579+** | - |

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
- [📘 02-Java并发编程](./docs/02-Java并发编程/README.md) - 9 篇文档，98+ 道面试题
- [📘 03-JVM](./docs/03-JVM/README.md) - 1 篇文档，10+ 道面试题
- [📘 04-Spring框架](./docs/04-Spring框架/README.md) - 8 篇文档，62+ 道面试题
- [📘 05-SpringBoot与自动装配](./docs/05-SpringBoot与自动装配/README.md) - 4 篇文档，20+ 道面试题
- [📘 06-SpringCloud微服务](./docs/06-SpringCloud微服务/README.md) - 2 篇文档，25+ 道面试题
- [📘 07-MySQL数据库](./docs/07-MySQL数据库/README.md) - 9 篇文档，50+ 道面试题
- [📘 08-Redis缓存](./docs/08-Redis缓存/README.md) - 4 篇文档，30+ 道面试题
- [📘 09-中间件](./docs/09-中间件/README.md) - 11 篇文档，106+ 道面试题
- [📘 10-算法与数据结构](./docs/10-算法与数据结构/README.md) - 4 篇文档，45+ 道面试题
- [📘 11-设计模式](./docs/11-设计模式/README.md) - 1 篇文档，20+ 道面试题
- [📘 12-分布式系统](./docs/12-分布式系统/README.md) - 1 篇文档，15+ 道面试题
- [📘 13-DevOps](./docs/13-DevOps/README.md) - 2 篇文档，25+ 道面试题
- [📘 14-SPI与IO模型](./docs/14-SPI与IO模型/README.md) - 4 篇文档，22+ 道面试题
- [📘 15-工程效能](./docs/15-工程效能/README.md) - 6 篇文档，26+ 道面试题 ⭐新增
