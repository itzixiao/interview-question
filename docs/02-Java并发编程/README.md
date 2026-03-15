# Java并发编程知识点详解

## 📚 文档列表

#### 1. [01-Java集合框架.md](./01-Java%E9%9B%86%E5%90%88%E6%A1%86%E6%9E%B6.md)
- **内容：** List、Set、Map 等集合类详解
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 2. [02-ConcurrentHashMap 详解.md](./02-ConcurrentHashMap%E8%AF%A6%E8%A7%A3.md)
- **内容：** ConcurrentHashMap 源码分析、线程安全机制
- **面试题：** 12+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-AQS 详解.md](./03-AQS%E8%AF%A6%E8%A7%A3.md)
- **内容：** AbstractQueuedSynchronizer 原理、CLH 队列
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 4. [04-CAS 与原子类.md](./04-CAS%E4%B8%8E%E5%8E%9F%E5%AD%90%E7%B1%BB.md)
- **内容：** CAS 原理、AtomicInteger 等原子类
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 5. [05-ReentrantLock 生产者消费者.md](./05-ReentrantLock%E7%94%9F%E4%BA%A7%E8%80%85%E6%B6%88%E8%B4%B9%E8%80%85.md)
- **内容：** ReentrantLock 实现、Condition 应用
- **面试题：** 8+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 6. [06-CountDownLatch 线程顺序执行.md](./06-CountDownLatch%E7%BA%BF%E7%A8%8B%E9%A1%BA%E5%BA%8F%E6%89%A7%E8%A1%8C.md)
- **内容：** CountDownLatch、CyclicBarrier 等工具类
- **面试题：** 6+ 道
- **重要程度：** ⭐⭐⭐⭐

#### 7. [07-线程池详解.md](./07-%E7%BA%BF%E7%A8%8B%E6%B1%A0%E8%AF%A6%E8%A7%A3.md)
- **内容：** ThreadPoolExecutor、线程池参数配置
- **面试题：** 12+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 8. [08-高并发线程安全详解.md](./08-%E9%AB%98%E5%B9%B6%E5%8F%91%E7%BA%BF%E7%A8%8B%E5%AE%89%E5%85%A8%E8%AF%A6%E8%A7%A3.md)
- **内容：** 线程安全问题、解决方案
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 9. [09-多线程基础详解.md](./09-%E5%A4%9A%E7%BA%BF%E7%A8%8B%E5%9F%BA%E7%A1%80%E8%AF%A6%E8%A7%A3.md)
- **内容：** 线程创建、生命周期、线程通信基础
- **面试题：** 10+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 9 个
- **面试题总数：** 98+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/concurrency/` 目录（11 个文件，~6,000 行代码）

---

## 🎯 学习建议

### 第一阶段：基础概念（2-3 天）
1. **线程基础**
   - 线程的创建方式（Thread、Runnable、Callable）
   - 线程状态转换
   - sleep() vs wait()

2. **集合框架**
   - ArrayList vs LinkedList
   - HashMap vs TreeMap
   - HashSet vs TreeSet

### 第二阶段：锁与同步（3-4 天）
1. **synchronized 关键字**
   - 对象锁与类锁
   - 锁升级过程（偏向锁→轻量级锁→重量级锁）

2. **ReentrantLock**
   - AQS 原理
   - Condition 条件队列
   - 公平锁与非公平锁

3. **CAS 与原子类**
   - CAS 原理与 ABA 问题
   - AtomicInteger、AtomicReference
   - LongAdder 高性能原子类

### 第三阶段：并发工具（2-3 天）
1. **JUC 工具类**
   - CountDownLatch（倒计时门闩）
   - CyclicBarrier（循环栅栏）
   - Semaphore（信号量）

2. **ConcurrentHashMap**
   - JDK7 vs JDK8 实现差异
   - Segment 分段锁到 CAS+synchronized

### 第四阶段：线程池与实战（3-4 天）
1. **ThreadPoolExecutor**
   - 核心参数配置
   - 拒绝策略选择
   - 线程池监控

2. **生产者消费者模式**
   - BlockingQueue 应用
   - 等待通知机制

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Java基础](../01-Java基础/README.md)** - 反射、类加载、Object 方法

### 后续进阶
- 📚 **[JVM](../03-JVM/README.md)** - 内存模型、GC 对并发的影响
- 📚 **[Spring框架](../04-Spring框架/README.md)** - 事务传播、AOP 线程安全
- 📚 **[Redis](../08-Redis 缓存/README.md)** - 分布式锁、Lua 脚本原子性
- 📚 **[MySQL](../07-MySQL 数据库/README.md)** - 事务隔离级别、MVCC

### 知识点对应
| 并发编程 | 应用场景 |
|----------|---------|
| ConcurrentHashMap | 高并发缓存场景 |
| ThreadPoolExecutor | Web 服务器、异步任务 |
| ReentrantLock | 复杂同步控制 |
| CAS | 无锁编程、乐观锁 |
| CountDownLatch | 批量任务等待完成 |

---

## 💡 高频面试题 Top 15

1. **ArrayList 和 LinkedList 的区别？适用场景？**
2. **HashMap 的底层实现？扩容机制？**
3. **ConcurrentHashMap 如何保证线程安全？**
4. **synchronized 和 ReentrantLock 的区别？**
5. **什么是 AQS？原理是什么？**
6. **CAS 的 ABA 问题如何解决？**
7. **volatile 关键字的作用？能保证原子性吗？**
8. **线程池的核心参数有哪些？如何配置？**
9. **sleep() 和 wait() 的区别？**
10. **notify() 和 notifyAll() 的区别？**
11. **ThreadLocal 的原理？内存泄漏问题？**
12. **CountDownLatch 和 CyclicBarrier 的区别？**
13. **BlockingQueue 有哪些实现类？**
14. **什么是死锁？如何预防和排查？**
15. **如何优雅地关闭线程池？**

---

## 🛠️ 实战技巧

### 自定义线程池
```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    8,                          // 核心线程数
    16,                         // 最大线程数
    60L, TimeUnit.SECONDS,      // 空闲超时
    new ArrayBlockingQueue<>(100), // 工作队列
    new ThreadFactoryBuilder().setNameFormat("task-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
);
```

### 使用 CountDownLatch 等待多个任务完成
```java
CountDownLatch latch = new CountDownLatch(10);
for (int i = 0; i < 10; i++) {
    executor.submit(() -> {
        try {
            // 执行任务
        } finally {
            latch.countDown();
        }
    });
}
latch.await(); // 等待所有任务完成
```

---

## 📖 推荐学习顺序

```
Java 集合框架
   ↓
synchronized 锁机制
   ↓
ReentrantLock 与 AQS
   ↓
CAS 与原子类
   ↓
ConcurrentHashMap
   ↓
并发工具类
   ↓
线程池详解
   ↓
高并发实战
```

---

## 📈 更新日志

### v2.1 - 2026-03-15
- ✅ 新增 09-多线程基础详解文档
- ✅ 补充 10+ 道高频面试题
- ✅ 更新统计信息（9 个文档，98+ 面试题）

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 88+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础并发编程文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-15  
**问题反馈：** 欢迎提 Issue 或 PR
