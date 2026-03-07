# Java 面试题学习项目

[![JDK](https://img.shields.io/badge/JDK-1.8-blue.svg)](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green.svg)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2021.0.8-blue.svg)](https://spring.io/projects/spring-cloud)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 通过可运行的示例代码深入理解 Java 核心技术，涵盖 Java 基础、集合、并发、JVM、Spring、MySQL、Redis 等面试高频知识点。

## 项目简介

本项目是一个面向 Java 面试的学习项目，采用**代码驱动**的方式帮助理解各种底层原理。每个知识点都配有详细的示例代码和 Markdown 说明文档。

项目采用微服务架构，包含以下模块：
- **interview-common**: 公共模块，包含工具类和通用配置
- **interview-gateway**: 网关服务，基于 Spring Cloud Gateway
- **interview-service**: 核心业务服务，包含所有示例代码

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8 | Java 开发工具包 |
| Spring Boot | 2.7.18 | 应用框架 |
| Spring Cloud | 2021.0.8 | 微服务框架 |
| Spring Cloud Alibaba | 2021.0.5.0 | 阿里巴巴微服务组件 |
| MyBatis | 2.3.1 | ORM 框架 |
| MyBatis-Plus | 3.5.3.1 | MyBatis 增强工具 |
| Redis | - | 缓存数据库 |
| Redisson | 3.17.6 | Redis 客户端 |
| Nacos | - | 注册中心和配置中心 |
| Sentinel | - | 流量控制和熔断降级 |

## 项目结构

```
interview-question/
├── interview-common/          # 公共模块
│   ├── src/main/java/         # 公共代码
│   └── pom.xml               # 模块依赖
│
├── interview-gateway/         # 网关服务
│   ├── src/main/java/         # 网关代码
│   └── pom.xml               # 模块依赖
│
├── interview-service/         # 业务服务（示例代码）
│   └── src/main/java/cn/itzixiao/interview/
│       ├── concurrency/       # 并发编程示例
│       │   ├── ThreadPoolDemo.java
│       │   ├── CountDownLatchSequentialDemo.java
│       │   └── ...
│       ├── hashmap/           # HashMap 源码分析
│       │   ├── HashMapSourceAnalysis.java
│       │   └── ConcurrentHashMapDemo.java
│       ├── java/              # Java 基础
│       │   ├── basic/         # 基础数据类型、String
│       │   ├── CollectionDeepDiveDemo.java
│       │   ├── IODeepDiveDemo.java
│       │   ├── ReflectionDeepDiveDemo.java
│       │   ├── ClassLoaderDeepDiveDemo.java
│       │   ├── InterfaceAndAbstractDemo.java    # 接口与抽象类
│       │   └── DeepCopyAndShallowCopyDemo.java  # 深拷贝与浅拷贝
│       ├── designpattern/    # 23种设计模式
│       │   ├── creational/   # 创建型模式（5种）
│       │   ├── structural/   # 结构型模式（7种）
│       │   └── behavioral/   # 行为型模式（11种）
│       ├── jvm/               # JVM 原理
│       │   └── JVMUnderstandingDemo.java
│       ├── mybatis/           # MyBatis 示例
│       ├── mysql/             # MySQL 优化
│       ├── proxy/             # 动态代理
│       ├── redis/             # Redis 应用
│       ├── service/           # 业务服务
│       ├── spring/            # Spring 原理
│       │   ├── IOCDemo.java
│       │   ├── AOPDemo.java
│       │   ├── BeanLifecycleDemo.java
│       │   └── transaction/   # 事务传播行为
│       ├── springboot/        # Spring Boot 自动装配
│       └── springmvc/         # Spring MVC 机制
│
├── docs/                      # 说明文档（30+ 篇）
│   ├── Java基础-数据类型.md
│   ├── Java基础-==与equals.md
│   ├── Java基础-String对比.md
│   ├── Java基础-接口与抽象类.md
│   ├── Java基础-深拷贝与浅拷贝.md
│   ├── 设计模式详解.md
│   ├── Java集合框架.md
│   ├── HashMap源码分析.md
│   ├── ConcurrentHashMap详解.md
│   ├── 线程池详解.md
│   ├── CAS与原子类.md
│   ├── AQS详解.md
│   ├── JVM内存模型与垃圾回收.md
│   ├── Java-IO与NIO.md
│   ├── Java反射与类加载.md
│   ├── JDK动态代理与CGLIB.md
│   ├── Spring-IOC与AOP.md
│   ├── Spring-MVC运行机制.md
│   ├── Spring-事务传播行为.md
│   ├── Spring-Boot自动装配.md
│   ├── MySQL索引与MVCC.md
│   ├── Redis缓存与分布式锁.md
│   └── CountDownLatch线程顺序执行.md
│
├── pom.xml                    # 父 POM
├── mvnw                       # Maven Wrapper
└── README.md                  # 项目说明
```

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+（可选）
- Redis 5.0+（可选）
- Nacos 2.0+（可选）

### 编译运行

```bash
# 1. 克隆项目
git clone https://gitee.com/itzixiao/interview-question.git
cd interview-question

# 2. 编译项目
mvn clean install

# 3. 运行示例（以并发示例为例）
cd interview-service
mvn spring-boot:run

# 或者运行指定示例
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.concurrency.CountDownLatchSequentialDemo
```

### 运行单个示例

```bash
# Java 基础示例
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.java.basic.EqualsAndDoubleEqualsDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.java.basic.StringComparisonDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.java.InterfaceAndAbstractDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.java.DeepCopyAndShallowCopyDemo

# 设计模式示例
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.designpattern.DesignPatternDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.designpattern.creational.singleton.SingletonDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.designpattern.structural.proxy.ProxyDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.designpattern.behavioral.strategy.StrategyDemo

# 并发编程示例
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.concurrency.ThreadPoolDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.concurrency.CountDownLatchSequentialDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.concurrency.ProducerConsumerDemo

# HashMap 示例
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.hashmap.HashMapSourceAnalysis
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.hashmap.ConcurrentHashMapDemo

# Spring 示例
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.spring.IOCDemo
mvn spring-boot:run -Dspring-boot.run.mainClass=cn.itzixiao.interview.spring.transaction.TransactionPropagationDemo
```

## 知识点目录

### Java 基础

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| 数据类型 | `JavaDataTypesDemo.java` | [Java基础-数据类型.md](docs/Java基础-数据类型.md) |
| == 与 equals | `EqualsAndDoubleEqualsDemo.java` | [Java基础-==与equals.md](docs/Java基础-==与equals.md) |
| String 对比 | `StringComparisonDemo.java` | [Java基础-String对比.md](docs/Java基础-String对比.md) |
| 接口与抽象类 | `InterfaceAndAbstractDemo.java` | [Java基础-接口与抽象类.md](docs/Java基础-接口与抽象类.md) |
| 深拷贝与浅拷贝 | `DeepCopyAndShallowCopyDemo.java` | [Java基础-深拷贝与浅拷贝.md](docs/Java基础-深拷贝与浅拷贝.md) |

### 集合框架

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| 集合深入 | `CollectionDeepDiveDemo.java` | [Java集合框架.md](docs/Java集合框架.md) |
| HashMap 源码 | `HashMapSourceAnalysis.java` | [HashMap源码分析.md](docs/HashMap源码分析.md) |
| ConcurrentHashMap | `hashmap/ConcurrentHashMapDemo.java` | [ConcurrentHashMap详解.md](docs/ConcurrentHashMap详解.md) |

### 并发编程

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| 线程池 | `ThreadPoolDemo.java` | [线程池详解.md](docs/线程池详解.md) |
| CAS 与原子类 | `CASAndAtomicDemo.java` | [CAS与原子类.md](docs/CAS与原子类.md) |
| AQS | `AQSDemo.java` | [AQS详解.md](docs/AQS详解.md) |
| CountDownLatch | `CountDownLatchSequentialDemo.java` | [CountDownLatch线程顺序执行.md](docs/CountDownLatch线程顺序执行.md) |
| 生产者消费者 | `ProducerConsumerDemo.java` | [ReentrantLock生产者消费者.md](docs/ReentrantLock生产者消费者.md) |

### JVM

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| JVM 内存模型 | `JVMUnderstandingDemo.java` | [JVM内存模型与垃圾回收.md](docs/JVM内存模型与垃圾回收.md) |
| 垃圾回收 | `GarbageCollectorDemo.java` | [JVM内存模型与垃圾回收.md](docs/JVM内存模型与垃圾回收.md) |

### IO/NIO

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| IO 深入 | `IODeepDiveDemo.java` | [Java-IO与NIO.md](docs/Java-IO与NIO.md) |

### 反射与类加载

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| 反射深入 | `ReflectionDeepDiveDemo.java` | [Java反射与类加载.md](docs/Java反射与类加载.md) |
| 类加载机制 | `ClassLoaderDeepDiveDemo.java` | [Java反射与类加载.md](docs/Java反射与类加载.md) |

### Spring 框架

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| IOC 容器 | `IOCDemo.java` | [Spring-IOC与AOP.md](docs/Spring-IOC与AOP.md) |
| AOP 实现 | `AOPDemo.java` | [Spring-IOC与AOP.md](docs/Spring-IOC与AOP.md) |
| Bean 生命周期 | `BeanLifecycleDemo.java` | [Spring-IOC与AOP.md](docs/Spring-IOC与AOP.md) |
| 事务传播 | `TransactionPropagationDemo.java` | [Spring-事务传播行为.md](docs/Spring-事务传播行为.md) |
| Spring MVC | `SpringMvcMechanismDemo.java` | [Spring-MVC运行机制.md](docs/Spring-MVC运行机制.md) |
| 自动装配 | `AutoConfigurationPrincipleDemo.java` | [Spring-Boot自动装配.md](docs/Spring-Boot自动装配.md) |

### 数据库

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| MySQL 索引优化 | `IndexOptimizationDemo.java` | [MySQL索引与MVCC.md](docs/MySQL索引与MVCC.md) |
| MVCC 机制 | `MVCCDemo.java` | [MySQL索引与MVCC.md](docs/MySQL索引与MVCC.md) |
| MyBatis 动态 SQL | `MyBatisDynamicSqlDemo.java` | [MyBatis动态SQL与SQL注入防护.md](docs/MyBatis动态SQL与SQL注入防护.md) |
| SQL 注入防护 | `SqlInjectionPreventionDemo.java` | [MyBatis动态SQL与SQL注入防护.md](docs/MyBatis动态SQL与SQL注入防护.md) |
| MyBatis-Plus | `MyBatisPlusDemo.java` | [MyBatis-Plus快速入门.md](docs/MyBatis-Plus快速入门.md) |

### Redis

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| 缓存问题 | `RedisCacheProblemDemo.java` | [Redis缓存与分布式锁.md](docs/Redis缓存与分布式锁.md) |
| 分布式锁 | `DistributedLockDemo.java` | [Redis缓存与分布式锁.md](docs/Redis缓存与分布式锁.md) |
| Redisson 分布式锁 | `RedissonDistributedLockDemo.java` | [Redis高级应用-分布式锁与限流.md](docs/Redis高级应用-分布式锁与限流.md) |
| 限流与防重放 | `RedisRateLimiterDemo.java` | [Redis高级应用-分布式锁与限流.md](docs/Redis高级应用-分布式锁与限流.md) |
| 延时队列 | `RedisDelayQueueDemo.java` | [Redis高级应用-延时队列与幂等.md](docs/Redis高级应用-延时队列与幂等.md) |
| 幂等与加密 | `IdempotencyAndSecurityDemo.java` | [Redis高级应用-延时队列与幂等.md](docs/Redis高级应用-延时队列与幂等.md) |

### 动态代理

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| JDK/CGLIB 对比 | `ProxyComparisonDemo.java` | [JDK动态代理与CGLIB.md](docs/JDK动态代理与CGLIB.md) |

### 设计模式

| 主题 | 示例代码 | 说明文档 |
|------|----------|----------|
| 23种设计模式总览 | `DesignPatternDemo.java` | [设计模式详解.md](docs/设计模式详解.md) |
| 单例模式 | `SingletonDemo.java` | [设计模式详解.md](docs/设计模式详解.md#一创建型模式5种) |
| 工厂方法模式 | `FactoryMethodDemo.java` | [设计模式详解.md](docs/设计模式详解.md#一创建型模式5种) |
| 抽象工厂模式 | `AbstractFactoryDemo.java` | [设计模式详解.md](docs/设计模式详解.md#一创建型模式5种) |
| 建造者模式 | `BuilderDemo.java` | [设计模式详解.md](docs/设计模式详解.md#一创建型模式5种) |
| 原型模式 | `PrototypeDemo.java` | [设计模式详解.md](docs/设计模式详解.md#一创建型模式5种) |
| 适配器模式 | `AdapterDemo.java` | [设计模式详解.md](docs/设计模式详解.md#二结构型模式7种) |
| 桥接模式 | `BridgeDemo.java` | [设计模式详解.md](docs/设计模式详解.md#二结构型模式7种) |
| 组合模式 | `CompositeDemo.java` | [设计模式详解.md](docs/设计模式详解.md#二结构型模式7种) |
| 装饰器模式 | `DecoratorDemo.java` | [设计模式详解.md](docs/设计模式详解.md#二结构型模式7种) |
| 外观模式 | `FacadeDemo.java` | [设计模式详解.md](docs/设计模式详解.md#二结构型模式7种) |
| 享元模式 | `FlyweightDemo.java` | [设计模式详解.md](docs/设计模式详解.md#二结构型模式7种) |
| 代理模式 | `ProxyDemo.java` | [设计模式详解.md](docs/设计模式详解.md#二结构型模式7种) |
| 策略模式 | `StrategyDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 模板方法模式 | `TemplateMethodDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 观察者模式 | `ObserverDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 迭代器模式 | `IteratorDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 责任链模式 | `ChainOfResponsibilityDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 命令模式 | `CommandDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 备忘录模式 | `MementoDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 状态模式 | `StateDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 访问者模式 | `VisitorDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 中介者模式 | `MediatorDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |
| 解释器模式 | `InterpreterDemo.java` | [设计模式详解.md](docs/设计模式详解.md#三行为型模式11种) |

## 学习建议

1. **按顺序学习**：建议从 Java 基础开始，逐步深入到并发、JVM、框架等高级主题
2. **运行示例**：每个示例都可以独立运行，观察输出结果加深理解
3. **阅读文档**：配合 Markdown 文档理解原理，文档中包含详细的图解和说明
4. **修改实验**：在示例代码基础上修改参数，观察不同结果

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 许可证

本项目基于 MIT 许可证开源，详见 [LICENSE](LICENSE) 文件。

---

> 如果本项目对您有帮助，请给个 Star ⭐ 支持一下！
