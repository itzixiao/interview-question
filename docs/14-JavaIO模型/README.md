# Java IO模型知识点详解

## 📚 文档列表

#### 1. [01-BIO阻塞IO详解.md](./01-BIO阻塞IO详解.md)

- **内容：** BIO 工作原理、一对一模型、ServerSocket编程
- **面试题：** 4+ 道
- **重要程度：** ⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/io/BioServerDemo.java`

#### 2. [02-NIO非阻塞IO详解.md](./02-NIO非阻塞IO详解.md)

- **内容：** NIO 核心组件、Selector、Channel、Buffer、Reactor模式
- **面试题：** 9+ 道
- **重要程度：** ⭐⭐⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/reactor/`

#### 3. [03-AIO异步IO详解.md](./03-AIO异步IO详解.md)

- **内容：** AIO 工作原理、回调机制、CompletionHandler
- **面试题：** 5+ 道
- **重要程度：** ⭐⭐⭐⭐
- **配套代码：** `interview-transaction-demo/src/main/java/cn/itzixiao/interview/transaction/io/AioServerDemo.java`

---

## 📊 统计信息

- **文档数：** 3 个
- **面试题总数：** 18+ 道
- **代码示例：** 配套 Java 代码在 `interview-transaction-demo/` 目录（BIO/NIO/AIO 示例、Reactor 模式）

---

## 🎯 学习建议

### 第一阶段：BIO（1 天）

1. **基础理解**
    - 同步阻塞IO模型
    - 一对一处理模式
    - ServerSocket + Socket 编程

2. **局限性**
    - 线程资源消耗大
    - 不适合高并发场景

### 第二阶段：NIO（3-4 天）⭐重点

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

### 第三阶段：AIO（2 天）

1. **异步机制**
    - CompletionHandler 回调
    - 操作系统支持（IOCP vs AIO）
    - 与 NIO 的区别

---

## 🔗 跨模块关联

### 前置知识

- ✅ **[Java基础](../01-Java基础/README.md)** - IO流、反射机制
- ✅ **[传统IO流](../01-Java基础/09-Java-IO与NIO.md)** - 字节流、字符流、缓冲流
- ✅ **[Java并发编程](../02-Java并发编程/README.md)** - 线程池、多线程

### 后续进阶

- 📚 **[中间件](../09-中间件/README.md)** - Netty、RPC 框架
- 📚 **[分布式系统](../12-分布式系统/README.md)** - 高性能通信

### 知识点对应

| 技术      | 应用场景                |
|---------|---------------------|
| **BIO** | 少连接内部系统、快速原型        |
| **NIO** | 高并发服务器、Netty、RPC 框架 |
| **AIO** | Windows 高性能应用、文件传输  |

---

## 💡 高频面试题 Top 10

### IO模型对比

1. **BIO、NIO、AIO 三者之间的区别？**
2. **如何选择合适的 IO模型？**

### NIO 核心

3. **NIO 的核心组件有哪些？Selector 的作用？**
4. **什么是 IO 多路复用？Select、Poll、Epoll的区别？**
5. **Reactor 模式的三种变体？**

### 实战应用

6. **Netty 为什么性能高？采用的是哪种 IO模型？**
7. **零拷贝技术的原理？**
8. **直接内存 vs 堆内存的区别？**

### 操作系统支持

9. **Linux 下 Epoll 的实现原理？**
10. **Windows 的 IOCP 是什么？**

---

## 🛠️ 实战技巧

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

### Reactor 模式对比

| 模式      | 优点       | 缺点     | 适用场景       |
|---------|----------|--------|------------|
| **单线程** | 简单，无线程切换 | 性能低    | 低并发        |
| **多线程** | 性能提升     | 线程同步复杂 | 中等并发       |
| **主从**  | 高性能，职责分离 | 最复杂    | 高并发（Netty） |

---

## 📖 推荐学习顺序

```
BIO同步阻塞IO
   ↓
NIO核心组件（Channel/Buffer/Selector）
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

- ✅ 重建 IO模型独立文档
- ✅ 整理 18+ 道高频面试题
- ✅ 完善学习建议和知识体系
- ✅ 添加跨模块关联

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
