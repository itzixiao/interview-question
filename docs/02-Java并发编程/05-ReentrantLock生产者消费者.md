# ReentrantLock 实现生产者消费者模式

## 一、概述

生产者消费者模式是一种经典的并发协作模式：
- **生产者**：负责生产数据，放入缓冲区
- **消费者**：从缓冲区取出数据进行消费
- **缓冲区**：存储数据的容器，通常是有界队列

### 核心问题

1. 缓冲区满时，生产者需要等待
2. 缓冲区空时，消费者需要等待
3. 需要正确处理线程间的通信

---

## 二、为什么用 ReentrantLock？

### synchronized vs ReentrantLock

| 特性 | synchronized | ReentrantLock |
|------|--------------|---------------|
| 锁获取方式 | JVM 自动管理 | 手动 lock/unlock |
| 条件变量 | 单个 wait/notify | 多个 Condition |
| 公平性 | 非公平 | 可选公平/非公平 |
| 可中断 | 不支持 | 支持 lockInterruptibly |
| 超时获取 | 不支持 | 支持 tryLock(timeout) |

### ReentrantLock 的优势

1. **多个 Condition**：生产者和消费者可以使用不同的条件队列，避免"惊群效应"
2. **灵活控制**：可以精细控制锁的获取和释放
3. **公平锁**：可以避免线程饥饿

---

## 三、核心实现

### BoundedBuffer 有界缓冲区

```java
class BoundedBuffer<T> {
    private final Queue<T> queue;
    private final int capacity;
    private final ReentrantLock lock;
    private final Condition notFull;   // 缓冲区未满条件
    private final Condition notEmpty;  // 缓冲区非空条件

    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.queue = new LinkedList<>();
        this.lock = new ReentrantLock(true); // 公平锁
        this.notFull = lock.newCondition();   // 生产者等待
        this.notEmpty = lock.newCondition();  // 消费者等待
    }
}
```

### put() 方法（生产者）

```java
public void put(T item) throws InterruptedException {
    lock.lock();
    try {
        // while 而不是 if，防止虚假唤醒
        while (queue.size() == capacity) {
            notFull.await(); // 缓冲区已满，等待
        }
        queue.offer(item);
        notEmpty.signal(); // 唤醒一个消费者
    } finally {
        lock.unlock();
    }
}
```

### take() 方法（消费者）

```java
public T take() throws InterruptedException {
    lock.lock();
    try {
        while (queue.isEmpty()) {
            notEmpty.await(); // 缓冲区为空，等待
        }
        T item = queue.poll();
        notFull.signal(); // 唤醒一个生产者
        return item;
    } finally {
        lock.unlock();
    }
}
```

---

## 四、ReentrantLock vs synchronized 对比

### synchronized 实现方式

```java
synchronized (lock) {
    while (queue.isFull()) {
        lock.wait();  // 只有一个等待队列
    }
    queue.add(item);
    lock.notifyAll();  // 唤醒所有线程（效率低）
}
```

### ReentrantLock 实现方式

```java
lock.lock();
try {
    while (queue.isFull()) {
        notFull.await();  // 生产者独立等待队列
    }
    queue.add(item);
    notEmpty.signal();  // 精准唤醒消费者
} finally {
    lock.unlock();
}
```

### ReentrantLock 的优势

| 优势 | 说明 |
|------|------|
| 多条件变量 | 生产者和消费者分别等待，避免相互干扰 |
| 精准唤醒 | signal() 只唤醒特定类型的线程 |
| 避免惊群 | 不会唤醒所有线程再让大部分重新等待 |
| 公平性 | 可选择公平锁，避免线程饥饿 |

---

## 五、流程图

### 生产者流程

```
┌─────────────────────────────────────────────────────────────┐
│  lock.lock()                                                │
│      ↓                                                      │
│  缓冲区是否已满？                                             │
│      ├── 是 → notFull.await() 等待                          │
│      └── 否 → 继续                                          │
│      ↓                                                      │
│  放入元素                                                    │
│      ↓                                                      │
│  notEmpty.signal() 唤醒消费者                                │
│      ↓                                                      │
│  lock.unlock()                                              │
└─────────────────────────────────────────────────────────────┘
```

### 消费者流程

```
┌─────────────────────────────────────────────────────────────┐
│  lock.lock()                                                │
│      ↓                                                      │
│  缓冲区是否为空？                                             │
│      ├── 是 → notEmpty.await() 等待                         │
│      └── 否 → 继续                                          │
│      ↓                                                      │
│  取出元素                                                    │
│      ↓                                                      │
│  notFull.signal() 唤醒生产者                                 │
│      ↓                                                      │
│  lock.unlock()                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 六、高频面试题

### Q1: 生产者消费者模式的核心是什么？

**答案**：
- **互斥**：缓冲区是共享资源，需要互斥访问
- **同步**：生产者和消费者需要协调工作
- **通信**：缓冲区状态变化时需要通知对方

### Q2: 为什么用 while 而不是 if 判断条件？

**答案**：
防止虚假唤醒（Spurious Wakeup）：
- 线程可能在没有收到 signal/notify 的情况下被唤醒
- 使用 while 可以在被唤醒后重新检查条件
- 如果用 if，虚假唤醒会导致逻辑错误

### Q3: Condition 的 signal 和 signalAll 有什么区别？

**答案**：
- `signal()`：唤醒一个等待线程（效率高）
- `signalAll()`：唤醒所有等待线程（更安全但效率低）
- ReentrantLock 可以用多个 Condition，signal 更精准

### Q4: ReentrantLock 的公平锁和非公平锁有什么区别？

**答案**：

| 非公平锁（默认） | 公平锁 |
|------------------|--------|
| 获取锁时不排队 | 按照请求顺序获取锁 |
| 性能高 | 不会饥饿 |
| 可能导致线程饥饿 | 性能略低 |

创建方式：`new ReentrantLock(true)`

### Q5: 生产者消费者模式有哪些实际应用？

**答案**：
1. **线程池**：任务队列 + 工作线程
2. **消息队列**：Kafka、RabbitMQ
3. **数据库连接池**：连接的生产和复用
4. **日志系统**：异步日志写入

### Q6: 如何避免死锁？

**答案**：
1. 确保锁的获取和释放成对出现（try-finally）
2. 避免嵌套锁
3. 使用 tryLock 设置超时
4. 使用更高级的并发工具（BlockingQueue）

### Q7: LinkedBlockingQueue 和 ArrayBlockingQueue 有什么区别？

**答案**：

| 特性 | LinkedBlockingQueue | ArrayBlockingQueue |
|------|---------------------|-------------------|
| 底层结构 | 链表 | 数组 |
| 容量 | 可无界也可有界 | 必须有界 |
| 锁机制 | 两把锁（读写分离） | 一把锁（读写互斥） |
| 吞吐量 | 更高 | 较低 |
| 内存占用 | 较大 | 较小 |

### Q8: 为什么推荐使用 BlockingQueue？

**答案**：
1. **代码简洁**：无需手动处理锁和条件变量
2. **功能完整**：提供阻塞、超时、非阻塞等多种操作
3. **稳定可靠**：JDK 标准库，经过充分测试
4. **性能优化**：内部实现经过高度优化

---

## 七、LinkedBlockingQueue 实现（推荐）

LinkedBlockingQueue 是 Java 并发包提供的阻塞队列实现，是生产者消费者模式的最佳实践。

### 核心特点

- 基于链表的阻塞队列，可选有界或无界
- 内部使用两把锁（putLock、takeLock），读写分离
- 提供现成的 `put()` 和 `take()` 阻塞方法
- 无需手动处理锁和条件变量

### 使用示例

```java
// 创建有界阻塞队列
BlockingQueue<String> queue = new LinkedBlockingQueue<>(5);

// 生产者
queue.put(item);  // 队列满时阻塞

// 消费者
String item = queue.take();  // 队列空时阻塞

// 带超时的操作
queue.offer(item, 1, TimeUnit.SECONDS);  // 超时插入
String item = queue.poll(1, TimeUnit.SECONDS);  // 超时获取
```

### 三种实现方式对比

| 特性 | synchronized | ReentrantLock | LinkedBlockingQueue |
|------|--------------|---------------|---------------------|
| 代码复杂度 | 中 | 高 | 低 |
| 灵活性 | 低 | 高 | 中 |
| 性能 | 中 | 高 | 高 |
| 推荐度 | ★★ | ★★★ | ★★★★★ |
| 使用场景 | 简单场景 | 需要精细控制 | 大多数场景 |

### LinkedBlockingQueue 源码要点

1. **两把锁分离**：putLock 和 takeLock，生产和消费可以并行
2. **两个条件**：notEmpty 和 notFull，精准唤醒
3. **原子计数**：AtomicInteger count，保证计数原子性
4. **内存一致性**：使用 full GC 屏障保证可见性

---

## 示例代码

完整示例代码请参考：[ProducerConsumerDemo.java](../../interview-service/src/main/java/cn/itzixiao/interview/concurrency/ProducerConsumerDemo.java)