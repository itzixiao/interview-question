# SPI 机制与 Java IO模型知识点详解

## 📚 文档列表

#### 1. [01-SPI机制详解.md](01-SPI机制详解.md)
- **内容：** SPI 工作原理、配置文件、ServiceLoader 使用
- **面试题：** 4+ 道
- **重要程度：** ⭐⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/spi/`

#### 2. [02-BIO同步阻塞IO.md](02-BIO同步阻塞IO.md)
- **内容：** BIO 工作原理、一对一模型、优缺点分析
- **面试题：** 4+ 道
- **重要程度：** ⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/io/BioServerDemo.java`

#### 3. [03-NIO同步非阻塞IO.md](./03-NIO同步非阻塞IO.md)
- **内容：** NIO 核心组件、Selector、Channel、Buffer、Reactor 模式
- **面试题：** 9+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/reactor/`

#### 4. [04-AIO 异步非阻塞 IO.md](04-AIO异步非阻塞IO.md)
- **内容：** AIO 工作原理、回调机制、CompletionHandler
- **面试题：** 5+ 道
- **重要程度：** ⭐⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/io/AioServerDemo.java`

---

## 📊 统计信息

- **文档数：** 4 个
- **面试题总数：** 22+ 道
- **代码示例：** 配套 Java 代码在 `interview-transaction-demo/` 目录（SPI 实现、BIO/NIO/AIO 示例、Reactor 模式）

---

## 🎯 学习建议

### 第一阶段：SPI 机制（1-2 天）
1. **核心概念**
   - Service Provider Interface 定义
   - META-INF/services 配置文件
   - ServiceLoader 加载机制

2. **应用场景**
   - JDBC 驱动加载
   - 日志框架 SLF4J
   - Spring Boot 自动装配

### 第二阶段：BIO（1 天）
1. **基础理解**
   - 同步阻塞 IO模型
   - 一对一处理模式
   - ServerSocket + Socket 编程

2. **局限性**
   - 线程资源消耗大
   - 不适合高并发场景

### 第三阶段：NIO（3-4 天）⭐重点
1. **核心组件**
   - Channel（通道）
   - Buffer（缓冲区）
   - Selector（选择器）

2. **Reactor 模式**
   - 单线程 Reactor
   - 多线程 Reactor
   - 主从 Reactor（Netty 采用）

3. **高级特性**
   - IO 多路复用原理
   - 零拷贝技术
   - 直接内存

### 第四阶段：AIO（2 天）
1. **异步机制**
   - CompletionHandler 回调
   - 操作系统支持（IOCP vs AIO）
   - 与 NIO 的区别

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Java基础](../01-Java基础/README.md)** - IO 流、反射机制
- ✅ **[Java并发编程](../02-Java并发编程/README.md)** - 线程池、多线程

### 后续进阶
- 📚 **[中间件](../09-中间件/README.md)** - Netty、RPC 框架
- 📚 **[分布式系统](../12-分布式系统/README.md)** - 高性能通信

### 知识点对应
| 技术 | 应用场景 |
|------|---------|
| **SPI** | JDBC 驱动、日志框架、Spring Boot 自动装配 |
| **BIO** | 少连接内部系统、快速原型 |
| **NIO** | 高并发服务器、Netty、RPC 框架 |
| **AIO** | Windows 高性能应用、文件传输 |

---

## 💡 高频面试题 Top 10

### SPI 相关
1. **什么是 SPI？工作原理是什么？**
2. **SPI 的优缺点？**
3. **JDBC 是如何使用 SPI 的？**
4. **Spring Boot 自动装配与 SPI 的关系？**

### IO模型相关
5. **BIO、NIO、AIO 三者之间的区别？**
6. **NIO 的核心组件有哪些？Selector 的作用？**
7. **什么是 IO 多路复用？Select、Poll、Epoll 的区别？**
8. **Reactor 模式的三种变体？**
9. **Netty 为什么性能高？采用的是哪种 IO模型？**
10. **如何选择合适的 IO模型？**

---

## 🛠️ 实战技巧

### SPI 配置示例
```properties
# META-INF/services/cn.itzixiao.interview.spi.PaymentService
cn.itzixiao.interview.spi.impl.AlipayService
cn.itzixiao.interview.spi.impl.WechatPayService
cn.itzixiao.interview.spi.impl.UnionPayService
```

### ServiceLoader 使用
```java
ServiceLoader<PaymentService> loader = ServiceLoader.load(PaymentService.class);
for (PaymentService service : loader) {
    service.pay(orderId, amount);
}
```

### NIO Selector 示例
```java
Selector selector = Selector.open();
channel.configureBlocking(false);
channel.register(selector, SelectionKey.OP_READ);

while (true) {
    if (selector.select() > 0) {
        for (SelectionKey key : selector.selectedKeys()) {
            if (key.isReadable()) {
                // 处理读事件
            }
        }
    }
}
```

---

## 📖 推荐学习顺序

```
SPI 机制基础
   ↓
BIO 同步阻塞 IO
   ↓
NIO 核心组件（Channel/Buffer/Selector）
   ↓
NIO Reactor 模式
   ↓
AIO 异步 IO
   ↓
综合对比与选型
```

---

## 📈 更新日志

### v1.0 - 2026-03-15
- ✅ 初始版本，拆分自原始文档
- ✅ 创建 4 个独立技术文档
- ✅ 整理 22+ 道高频面试题
- ✅ 完善学习建议和知识体系

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
