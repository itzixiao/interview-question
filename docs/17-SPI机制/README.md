# SPI机制知识点详解

## 📚 文档列表

#### 1. [01-SPI机制详解.md](./01-SPI机制详解.md)

- **内容：** SPI 工作原理、配置文件、ServiceLoader 使用、SPI vs Spring IOC
- **面试题：** 4+ 道
- **重要程度：** ⭐⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/spi/`

---

## 📊 统计信息

- **文档数：** 1 个
- **面试题总数：** 4+ 道
- **代码示例：** 配套 Java 代码在 `interview-transaction-demo/` 目录（SPI 实现示例）

---

## 🎯 学习建议

### 第一阶段：SPI 基础（1-2 天）

1. **核心概念**
    - Service Provider Interface 定义
    - META-INF/services 配置文件格式
    - ServiceLoader 加载机制

2. **工作流程**
   ```java
   // Step1: 加载服务
   ServiceLoader<PaymentService> loader = ServiceLoader.load(PaymentService.class);
   
   // Step2: 遍历使用
   for (PaymentService service : loader) {
       service.pay(orderId, amount);
   }
   ```

3. **应用场景**
    - JDBC 驱动加载
    - 日志框架 SLF4J
    - Dubbo SPI 扩展
    - Spring Boot自动装配

---

## 🔗 跨模块关联

### 前置知识

- ✅ **[Java基础](../01-Java基础/README.md)** - 反射机制、接口定义
- ✅ **[类加载机制](../01-Java基础/08-类加载机制详解.md)** - ClassLoader 原理

### 后续进阶

- 📚 **[SpringBoot自动装配](../05-SpringBoot与自动装配/README.md)** - spring.factories
- 📚 **[中间件](../09-中间件/README.md)** - Dubbo SPI、RPC 框架

### 知识点对应

| SPI 技术          | 应用场景    |
|-----------------|---------|
| **JDBC**        | 数据库驱动加载 |
| **SLF4J**       | 日志门面模式  |
| **Dubbo**       | 扩展点加载   |
| **Spring Boot** | 自动配置    |

---

## 💡 高频面试题 Top 4

1. **什么是 SPI？工作原理是什么？**
2. **SPI 的优缺点？**
3. **JDBC 是如何使用 SPI 的？**
4. **Spring Boot自动装配与 SPI 的关系？**

---

## 🛠️ 实战技巧

### SPI 配置示例

```properties
# META-INF/services/cn.itzixiao.interview.transaction.spi.PaymentService
cn.itzixiao.interview.transaction.spi.impl.AlipayService
cn.itzixiao.interview.transaction.spi.impl.WechatPayService
cn.itzixiao.interview.transaction.spi.impl.UnionPayService
```

### ServiceLoader 使用

```java
ServiceLoader<PaymentService> loader = ServiceLoader.load(PaymentService.class);
for (PaymentService service : loader) {
    System.out.println("支付方式：" + service.getName());
    String result = service.pay("ORDER001", 100.00);
    System.out.println(result);
}
```

### SPI vs Spring IOC

| 特性   | SPI                  | Spring IOC |
|------|----------------------|------------|
| 实现方式 | 配置文件 + ServiceLoader | 注解 + 容器管理  |
| 加载时机 | 延迟加载                 | 启动时加载      |
| 依赖注入 | 无                    | 支持         |
| 应用场景 | JDBC、日志框架            | Spring 应用  |

---

## 📖 推荐学习顺序

```
SPI机制基础
   ↓
配置文件格式
   ↓
ServiceLoader 使用
   ↓
实际应用场景
   ↓
对比 Spring IOC
```

---

## 📈 更新日志

### v1.0 - 2026-03-15

- ✅ 初始版本，独立 SPI机制文档
- ✅ 整理 4+ 道高频面试题
- ✅ 完善学习建议和知识体系
- ✅ 添加跨模块关联

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
