# 🎯 面试题库微服务项目

> **一站式 Java 面试准备平台** - 48 篇文档 · 368+ 道面试题 · 配套 ~30,000 行示例代码

---

## 📁 项目架构

### 完整层级结构

```
interview-question/                              # 根目录
├── interview-common/                            # 公共模块
│   └── src/main/java/
│       ├── exception/                           # 业务异常类
│       ├── handler/                             # 全局异常处理器
│       └── result/                              # 统一返回结果
│
├── interview-gateway/                           # API 网关服务
│   └── src/main/java/
│       ├── config/                              # 网关配置
│       ├── filter/                              # 自定义过滤器
│       └── handler/                             # 异常处理器
│
├── interview-microservices-parent/              # 微服务模块集合（父模块）
│   ├── interview-algorithm/                     # 算法面试题库
│   ├── interview-bigdata/                       # 大数据技术栈
│   ├── interview-containerization/              # 容器化技术
│   ├── interview-kafka/                         # Kafka消息队列
│   ├── interview-lowcode/                       # 低代码平台
│   ├── interview-observability/                 # 可观测性
│   ├── interview-performance-tuning/            # 性能调优
│   ├── interview-provider/                      # 服务提供者
│   ├── interview-rabbitmq/                      # RabbitMQ消息队列
│   ├── interview-reactive/                      # 响应式编程
│   ├── interview-security/                      # 安全认证
│   ├── interview-service/                       # 业务服务
│   ├── interview-service-mesh/                  # 服务网格
│   ├── interview-system-design/                 # 系统设计
│   ├── interview-transaction-demo/              # 分布式事务演示
│   ├── interview-workflow/                      # 工作流引擎
│   └── interview-xxljob/                        # XXL-JOB分布式任务调度
│
├── interview-starters-parent/                   # Starter 模块集合（父模块）
│   └── interview-threadpool-starter/            # 线程池Starter
│
├── docs/                  
│   ├── 01-Java基础/         
│   ├── 02-Java并发编程/       
│   ├── 03-JVM/            
│   ├── 04-JavaIO模型/       
│   ├── 05-Spring框架/       
│   ├── 06-SpringBoot与自动装配/
│   ├── 07-SpringCloud微服务/ 
│   ├── 08-MySQL数据库/       
│   ├── 09-PostgreSQL数据库/  
│   ├── 10-缓存与NoSQL数据库/        
│   ├── 11-中间件/            
│   ├── 12-算法与数据结构/        
│   ├── 13-设计模式/           
│   ├── 14-分布式系统/          
│   ├── 15-DevOps/         
│   ├── 16-工程效能/           
│   ├── 17-SPI机制/          
│   └── 18-前端开发/          
├── pom.xml                                      # 根POM文件
└── README.md                                    # 项目说明文档
```

### 模块职责说明

| 模块                                 | 职责              | 说明                                  |
|------------------------------------|-----------------|-------------------------------------|
| **interview-common**               | 公共模块            | 提供统一的异常处理、返回结果等公共组件                 |
| **interview-gateway**              | API 网关          | 基于 Spring Cloud Gateway，负责路由、过滤、限流等 |
| **interview-microservices-parent** | 微服务集合（父模块）      | 包含所有业务微服务模块                         |
| **interview-starters-parent**      | Starter 集合（父模块） | 包含自定义的 Spring Boot Starter          |
| **interview-algorithm**            | 算法题库            | 包含常见算法面试题及实现                        |
| **interview-bigdata**              | 大数据技术栈          | Hadoop、Spark、Flink等大数据组件示例          |
| **interview-containerization**     | 容器化技术           | Docker、Kubernetes容器化部署示例            |
| **interview-kafka**                | Kafka消息队列       | Kafka生产者消费者示例及配置                    |
| **interview-lowcode**              | 低代码平台           | 动态表单、代码生成器等低代码实现                    |
| **interview-observability**        | 可观测性            | 日志、监控、链路追踪等可观测性实现                   |
| **interview-performance-tuning**   | 性能调优            | JVM调优、SQL优化等性能优化示例                  |
| **interview-provider**             | 服务提供者           | 提供 RESTful API 服务，演示 OpenFeign 调用   |
| **interview-rabbitmq**             | RabbitMQ消息队列    | RabbitMQ消息发送与消费示例                   |
| **interview-reactive**             | 响应式编程           | WebFlux、Reactor响应式编程示例              |
| **interview-security**             | 安全认证            | JWT、OAuth2、Spring Security安全实现      |
| **interview-service**              | 业务服务            | 包含所有业务逻辑和技术点示例代码                    |
| **interview-service-mesh**         | 服务网格            | Istio等服务网格技术示例                      |
| **interview-system-design**        | 系统设计            | 高并发、高可用系统设计方案                       |
| **interview-transaction-demo**     | 分布式事务演示         | Seata分布式事务实现示例                      |
| **interview-workflow**             | 工作流引擎           | Flowable工作流引擎集成与使用                  |
| **interview-xxljob**               | 分布式任务调度         | XXL-JOB任务调度平台集成与使用                  |
| **interview-threadpool-starter**   | 线程池Starter      | 自定义线程池自动配置 Starter                  |

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

| 模块                        | 文档数  | 面试题    | README 导航                                   |
|---------------------------|------|--------|---------------------------------------------|
| 🔹 **01-Java基础**          | 9 篇  | 45+ 道  | [详细文档](./docs/01-Java基础/README.md)          |
| 🔹 **02-Java并发编程**        | 9 篇  | 98+ 道  | [详细文档](./docs/02-Java并发编程/README.md)        |
| 🔹 **03-JVM**             | 4 篇  | 10+ 道  | [详细文档](./docs/03-JVM/README.md)             |
| 🔹 **04-JavaIO模型**        | 4 篇  | 22+ 道  | [详细文档](./docs/04-JavaIO模型/README.md)        |
| 🔹 **05-Spring框架**        | 9 篇  | 62+ 道  | [详细文档](./docs/05-Spring框架/README.md)        |
| 🔹 **06-SpringBoot与自动装配** | 4 篇  | 20+ 道  | [详细文档](./docs/06-SpringBoot与自动装配/README.md) |
| 🔹 **07-SpringCloud微服务**  | 2 篇  | 25+ 道  | [详细文档](./docs/07-SpringCloud微服务/README.md)  |
| 🔹 **08-MySQL数据库**        | 11 篇 | 50+ 道  | [详细文档](./docs/08-MySQL数据库/README.md)        |
| 🔹 **09-PostgreSQL数据库**   | 3 篇  | 11+ 道  | [详细文档](./docs/09-PostgreSQL数据库/README.md)   |
| 🔹 **10-缓存与NoSQL数据库**     | 10 篇 | 145+ 道 | [详细文档](./docs/10-缓存与NoSQL数据库/README.md)     |
| 🔹 **11-中间件**             | 14 篇 | 116+ 道 | [详细文档](./docs/11-中间件/README.md)             |
| 🔹 **12-算法与数据结构**         | 4 篇  | 45+ 道  | [详细文档](./docs/12-算法与数据结构/README.md)         |
| 🔹 **13-设计模式**            | 1 篇  | 20+ 道  | [详细文档](./docs/13-设计模式/README.md)            |
| 🔹 **14-分布式系统**           | 7 篇  | 15+ 道  | [详细文档](./docs/14-分布式系统/README.md)           |
| 🔹 **15-DevOps**          | 5 篇  | 25+ 道  | [详细文档](./docs/15-DevOps/README.md)          |
| 🔹 **16-工程效能**            | 6 篇  | 26+ 道  | [详细文档](./docs/16-工程效能/README.md)            |
| 🔹 **17-SPI机制**           | 1 篇  | 4+ 道   | [详细文档](./docs/17-SPI机制/README.md)           |
| 🔹 **18-前端开发**            | 8 篇  | 49+ 道  | [详细文档](./docs/18-前端开发/README.md)            |

**总计：** 74 篇文档 · 653+ 道面试题 · 配套 ~42,000 行示例代码

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

- [Spring IOC 和 AOP 原理？](./docs/05-Spring框架/01-Spring-IOC与AOP.md)
- [Spring Bean 的生命周期？](./docs/05-Spring框架/02-Spring-Bean生命周期详解.md)
- [Spring事务传播行为？](./docs/05-Spring框架/04-Spring事务详解.md)
- [Spring Boot 自动装配原理？](./docs/06-SpringBoot与自动装配/01-Spring-Boot自动装配.md)
- [Spring Cloud Gateway 原理？](./docs/07-SpringCloud微服务/01-Spring-Cloud-Gateway详解.md)
- [OpenFeign 工作原理？](./docs/07-SpringCloud微服务/02-Spring-Cloud-OpenFeign详解.md)

### MySQL（40+ 道）

- [InnoDB 和 MyISAM 区别？](./docs/08-MySQL数据库/01-MySQL字段类型与存储引擎.md)
- [为什么使用 B+Tree 索引？](./docs/08-MySQL数据库/02-MySQL索引原理详解.md)
- [MySQL索引与MVCC](docs/08-MySQL数据库/05-MySQL索引与MVCC.md)
- [如何防止 SQL注入？](./docs/08-MySQL数据库/04-MySQL日志与性能优化详解.md)
- [ShardingSphere 分库分表实战](./docs/08-MySQL数据库/09-ShardingSphere整合实战指南.md)
- [PostgreSQL vs MySQL 核心区别？](./docs/09-PostgreSQL数据库/01-PostgreSQL基础与核心特性.md)
- [PostgreSQL JSONB 索引与查询优化](./docs/09-PostgreSQL数据库/02-PostgreSQL索引与查询优化.md)
- [MyBatis核心原理？](./docs/11-中间件/03-MyBatis核心原理与面试题.md)

### 缓存中间件（130+ 道）

- [Redis 支持的数据类型？](./docs/10-缓存与NoSQL数据库/01-Redis基础与数据结构.md)
- [Redis 哨兵和集群机制？](./docs/10-缓存与NoSQL数据库/05-Redis主从复制与哨兵模式.md)
- [如何实现分布式锁？](./docs/10-缓存与NoSQL数据库/03-Redis分布式锁详解.md)
- [Redis 持久化机制？](./docs/10-缓存与NoSQL数据库/04-Redis持久化机制详解.md)
- [Memcached vs Redis 区别？](./docs/10-缓存与NoSQL数据库/08-Memcached详解.md)
- [缓存中间件如何选型？](./docs/10-缓存与NoSQL数据库/09-缓存中间件对比详解.md)
- [MongoDB 文档模型与索引？](./docs/10-缓存与NoSQL数据库/10-MongoDB详解.md)

### 中间件（35+ 道）

- [Nacos 的核心功能？](./docs/11-中间件/04-Nacos核心知识点详解.md)
- [MyBatis 的#{}和${}区别？](./docs/11-中间件/02-MyBatis动态SQL与SQL注入防护.md)
- [Sentinel限流熔断原理？](./docs/11-中间件/05-Sentinel限流熔断详解.md)
- [MyBatis-Plus 优势？](./docs/11-中间件/01-MyBatis-Plus快速入门.md)

### 算法与数据结构（35+ 道）

- [HashMap源码解析](./docs/12-算法与数据结构/01-HashMap源码分析.md)
- [红黑树的特性？](./docs/12-算法与数据结构/02-树形数据结构详解.md)
- [B 树和 B+树的区别？](./docs/12-算法与数据结构/02-树形数据结构详解.md)
- [常见排序算法时间复杂度？](./docs/12-算法与数据结构/03-算法面试题完整合集.md)

### 设计模式（20+ 道）

- [单例模式实现方式？](./docs/13-设计模式/01-设计模式详解.md)
- [工厂模式和抽象工厂区别？](./docs/13-设计模式/01-设计模式详解.md)
- [Spring 中用到了哪些设计模式？](./docs/13-设计模式/01-设计模式详解.md)

### 分布式系统（15+ 道）

- [分布式幂等详解](./docs/14-分布式系统/01-分布式幂等详解.md)
- [接口防重放详解](./docs/14-分布式系统/02-接口防重放详解.md)
- [分布式事务深入详解](./docs/14-分布式系统/06-分布式事务深入详解.md)

### DevOps（25+ 道）

- [Docker 的优势？](./docs/15-DevOps/01-DevOps核心知识点详解.md)
- [Nginx 负载均衡策略？](./docs/15-DevOps/01-DevOps核心知识点详解.md)
- [CI/CD流程？](./docs/15-DevOps/01-DevOps核心知识点详解.md)
- [容器化与云原生详解](./docs/15-DevOps/02-容器化与云原生详解.md)

### JavaIO模型（22+ 道）

- [BIO、NIO、AIO 三者之间的区别？](docs/04-JavaIO模型/README.md#1-bionioaio-三者之间的区别)
- [如何选择合适的 IO模型？](docs/04-JavaIO模型/README.md#2-如何选择合适的-io模型)
- [NIO 的核心组件有哪些？Selector的作用？](docs/04-JavaIO模型/README.md#3-nio-的核心组件有哪些)
- [什么是 IO 多路复用？Select、Poll、Epoll 的区别？](docs/04-JavaIO模型/README.md#4-什么是-io-多路复用selectpollepoll的区别)
- [Reactor模式的三种变体？](docs/04-JavaIO模型/README.md#5-reactor模式的三种变体)
- [Netty为什么性能高？采用的是哪种 IO模型？](docs/04-JavaIO模型/README.md#6-netty-为什么性能高)
- [零拷贝技术的原理？](docs/04-JavaIO模型/README.md#7-零拷贝技术的原理)
- [直接内存 vs 堆内存的区别？](docs/04-JavaIO模型/README.md#8-直接内存-vs-堆内存的区别)
- [Linux 下 Epoll 的实现原理？](docs/04-JavaIO模型/README.md#9-linux-下-epoll-的实现原理)
- [Windows 的 IOCP 是什么？](docs/04-JavaIO模型/README.md#10-windows-的-iocp-是什么)

### SPI机制（4+ 道） ⭐新增

- [什么是 SPI？工作原理是什么？](docs/17-SPI机制/01-SPI机制详解.md)
- [SPI 的优缺点？](docs/17-SPI机制/01-SPI机制详解.md)
- [JDBC 是如何使用 SPI 的？](docs/17-SPI机制/01-SPI机制详解.md)
- [Spring Boot自动装配与 SPI 的关系？](docs/17-SPI机制/01-SPI机制详解.md)

### 前端开发（49+ 道） ⭐新增

**Vue2/Vue3**

- [Vue2 的响应式原理是什么？](./docs/18-前端开发/01-Vue2核心原理详解.md)
- [Vue2 的 Diff 算法原理是什么？](./docs/18-前端开发/01-Vue2核心原理详解.md)
- [Vue 组件的通信方式有哪些？](./docs/18-前端开发/01-Vue2核心原理详解.md)
- [Vue3 的响应式原理与 Vue2 有什么区别？](./docs/18-前端开发/02-Vue3核心原理详解.md)
- [Composition API 与 Options API 的区别？](./docs/18-前端开发/02-Vue3核心原理详解.md)
- [ref 和 reactive 的区别？](./docs/18-前端开发/02-Vue3核心原理详解.md)
- [Pinia 与 Vuex 的区别？](./docs/18-前端开发/02-Vue3核心原理详解.md)

**前端工程化**

- [Vite 为什么比 Webpack 快？](./docs/18-前端开发/03-前端工程化详解.md)
- [Monorepo 的优势和挑战是什么？](./docs/18-前端开发/03-前端工程化详解.md)

**TypeScript**

- [TypeScript 与 Java 泛型的主要区别？](./docs/18-前端开发/04-TypeScript高级类型与实战.md)
- [如何提取 Promise 的返回值类型？](./docs/18-前端开发/04-TypeScript高级类型与实战.md)

**React**

- [React Hooks 为什么不能放在条件语句中？](./docs/18-前端开发/05-React核心原理.md)
- [React 的 Fiber 架构解决了什么问题？](./docs/18-前端开发/05-React核心原理.md)
- [Redux 和 Context API 应该如何选择？](./docs/18-前端开发/05-React核心原理.md)

**前后端联调**

- [如何设计一个可扩展的 API 层？](./docs/18-前端开发/06-前后端联调最佳实践.md)
- [如何防止重复提交？](./docs/18-前端开发/06-前后端联调最佳实践.md)

**性能优化**

- [如何优化首屏加载时间？](./docs/18-前端开发/07-前端性能优化全链路.md)
- [虚拟列表的实现原理是什么？](./docs/18-前端开发/07-前端性能优化全链路.md)

**微前端**

- [微前端解决了什么问题？带来了什么挑战？](./docs/18-前端开发/08-微前端架构实战.md)
- [qiankun 的 JS 沙箱是如何实现的？](./docs/18-前端开发/08-微前端架构实战.md)
- [Module Federation 与 qiankun 有什么区别？](./docs/18-前端开发/08-微前端架构实战.md)

### 工程效能（26+ 道） ⭐新增

- [代码生成器的核心原理？](./docs/16-工程效能/02-代码生成器设计.md)
- [动态表单引擎的设计思路？](./docs/16-工程效能/03-动态表单引擎.md)
- [Activiti 的核心组件？](docs/16-工程效能/04-流程引擎Activiti.md)
- [Drools 的工作原理？](docs/16-工程效能/05-规则引擎Drools.md)
- [POI 生成 Excel 的优化技巧？](./docs/16-工程效能/06-报表引擎.md)

---

## 📊 知识点覆盖统计

| 分类                | 文档数    | 面试题数     | 重要程度      |
|-------------------|--------|----------|-----------|
| Java基础            | 9      | 45+      | ⭐⭐⭐⭐⭐     |
| Java并发编程          | 9      | 98+      | ⭐⭐⭐⭐⭐     |
| JVM               | 4      | 10+      | ⭐⭐⭐⭐      |
| JavaIO模型          | 4      | 22+      | ⭐⭐⭐⭐⭐     |
| Spring框架          | 9      | 62+      | ⭐⭐⭐⭐⭐     |
| SpringBoot与自动装配   | 4      | 20+      | ⭐⭐⭐⭐      |
| SpringCloud微服务    | 2      | 25+      | ⭐⭐⭐⭐      |
| MySQL数据库          | 11     | 50+      | ⭐⭐⭐⭐⭐     |
| **PostgreSQL数据库** | **3**  | **11+**  | **⭐⭐⭐⭐⭐** |
| Redis缓存           | 7      | 30+      | ⭐⭐⭐⭐⭐     |
| 中间件               | 14     | 116+     | ⭐⭐⭐⭐      |
| 算法与数据结构           | 4      | 45+      | ⭐⭐⭐⭐      |
| 设计模式              | 1      | 20+      | ⭐⭐⭐       |
| 分布式系统             | 7      | 15+      | ⭐⭐⭐⭐      |
| DevOps            | 5      | 25+      | ⭐⭐⭐       |
| 工程效能              | 6      | 26+      | ⭐⭐⭐⭐      |
| SPI机制             | 1      | 4+       | ⭐⭐⭐⭐      |
| 前端开发              | 8      | 49+      | ⭐⭐⭐⭐⭐     |
| **总计**            | **74** | **653+** | -         |

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
- [📘 03-JVM](./docs/03-JVM/README.md) - 4 篇文档，10+ 道面试题
- [📘 04-JavaIO模型](./docs/04-JavaIO模型/README.md) - 4 篇文档，22+ 道面试题
- [📘 05-Spring框架](./docs/05-Spring框架/README.md) - 9 篇文档，62+ 道面试题
- [📘 06-SpringBoot与自动装配](./docs/06-SpringBoot与自动装配/README.md) - 4 篇文档，20+ 道面试题
- [📘 07-SpringCloud微服务](./docs/07-SpringCloud微服务/README.md) - 2 篇文档，25+ 道面试题
- [📘 08-MySQL数据库](./docs/08-MySQL数据库/README.md) - 11 篇文档，50+ 道面试题
- [📘 09-PostgreSQL数据库](./docs/09-PostgreSQL数据库/README.md) - 3 篇文档，11+ 道面试题 ⭐新增
- [📘 10-缓存与NoSQL数据库](./docs/10-缓存与NoSQL数据库/README.md) - 10 篇文档，145+ 道面试题
- [📘 11-中间件](./docs/11-中间件/README.md) - 14 篇文档，116+ 道面试题
- [📘 12-算法与数据结构](./docs/12-算法与数据结构/README.md) - 4 篇文档，45+ 道面试题
- [📘 13-设计模式](./docs/13-设计模式/README.md) - 1 篇文档，20+ 道面试题
- [📘 14-分布式系统](./docs/14-分布式系统/README.md) - 7 篇文档，15+ 道面试题
- [📘 15-DevOps](./docs/15-DevOps/README.md) - 5 篇文档，25+ 道面试题
- [📘 16-工程效能](./docs/16-工程效能/README.md) - 6 篇文档，26+ 道面试题
- [📘 17-SPI机制](./docs/17-SPI机制/README.md) - 1 篇文档，4+ 道面试题
- [📘 18-前端开发](./docs/18-前端开发/README.md) - 8 篇文档，49+ 道面试题 ⭐新增
