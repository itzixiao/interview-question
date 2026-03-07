# AQS（AbstractQueuedSynchronizer）详解

## 概述

AQS 是 Java 并发包的核心框架，提供了**依赖先进先出（FIFO）等待队列**的阻塞锁和同步器的基础实现。

## 核心组件

### 1. state 变量

```java
private volatile int state;
```

- 表示同步状态
- 不同子类有不同含义：
  - ReentrantLock：0=未锁定，>0=重入次数
  - CountDownLatch：剩余计数
  - Semaphore：剩余许可数

### 2. FIFO 等待队列

- 基于双向链表实现
- 节点（Node）包含：
  - 等待状态（waitStatus）
  - 前驱节点（prev）
  - 后继节点（next）
  - 线程引用（thread）

### 3. Node 节点状态

| 状态 | 值 | 说明 |
|------|-----|------|
| CANCELLED | 1 | 节点已取消 |
| SIGNAL | -1 | 后继节点需要被唤醒 |
| CONDITION | -2 | 节点在条件队列中 |
| PROPAGATE | -3 | 共享模式下传播唤醒 |

## 核心方法

### 独占模式

```java
// 获取锁（阻塞）
public final void acquire(int arg)

// 释放锁
public final boolean release(int arg)

// 尝试获取（子类实现）
protected boolean tryAcquire(int arg)

// 尝试释放（子类实现）
protected boolean tryRelease(int arg)
```

### 共享模式

```java
// 获取共享锁
public final void acquireShared(int arg)

// 释放共享锁
public final boolean releaseShared(int arg)

// 尝试获取共享锁（子类实现）
protected int tryAcquireShared(int arg)

// 尝试释放共享锁（子类实现）
protected boolean tryReleaseShared(int arg)
```

## acquire 流程详解

```
acquire(int arg)
    │
    ├─ tryAcquire(arg) ──→ 成功 ──→ 结束
    │       失败
    │        ↓
    ├─ addWaiter(Node.EXCLUSIVE) ──→ 加入等待队列
    │        ↓
    ├─ acquireQueued(node, arg)
    │       │
    │       ├─ 自旋检查前驱节点
    │       │
    │       ├─ 前驱是头节点 ──→ tryAcquire ──→ 成功 ──→ 设为头节点
    │       │                           失败
    │       │                            ↓
    │       └─ 阻塞当前线程（LockSupport.park）
    │
    └─ 被唤醒后继续自旋
```

## AQS 实现类

| 类名 | 模式 | 说明 |
|------|------|------|
| ReentrantLock | 独占 | 可重入锁 |
| ReentrantReadWriteLock | 独占+共享 | 读写锁 |
| CountDownLatch | 共享 | 倒计时门闩 |
| CyclicBarrier | 共享 | 循环栅栏 |
| Semaphore | 共享 | 信号量 |
| ThreadPoolExecutor.Worker | 独占 | 线程池工作线程 |

## 自定义同步器示例

```java
public class Mutex extends AbstractQueuedSynchronizer {
    
    // 初始状态 0 表示未锁定
    public Mutex() {
        setState(0);
    }
    
    @Override
    protected boolean tryAcquire(int acquires) {
        // CAS 设置 state 从 0 到 1
        if (compareAndSetState(0, 1)) {
            setExclusiveOwnerThread(Thread.currentThread());
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean tryRelease(int releases) {
        if (getState() == 0) {
            throw new IllegalMonitorStateException();
        }
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
    }
}
```

## 条件队列（Condition）

### 与 Object.wait/notify 对比

| 特性 | Object | Condition |
|------|--------|-----------|
| 等待队列 | 1个 | 多个 |
| 唤醒 | notify 随机唤醒 | signal 精确唤醒 |
| 使用方式 | synchronized | Lock + Condition |

### 核心方法

```java
// 加入条件队列等待
void await() throws InterruptedException;

// 唤醒条件队列中的一个节点
void signal();

// 唤醒条件队列中的所有节点
void signalAll();
```

## 公平锁 vs 非公平锁

### 非公平锁（默认）

```java
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 直接 CAS 尝试获取，不检查队列
        if (compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // ...
}
```

### 公平锁

```java
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        // 先检查队列中是否有等待线程
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    // ...
}
```

## 最佳实践

1. **理解 state 含义**：不同同步器 state 含义不同
2. **线程安全**：AQS 是线程安全的
3. **公平性选择**：根据业务需求选择公平/非公平锁
4. **Condition 使用**：多条件等待时使用多个 Condition

---

## 十、高频面试题

### 【问题 1】AQS 的核心原理是什么？
答：
- AQS(AbstractQueuedSynchronizer) 是一个抽象队列同步器
- 核心思想：如果共享资源可以被访问，则将当前线程设置为执行状态；否则将线程加入等待队列
- 通过 state 变量表示同步状态，通过 FIFO 队列管理等待线程
- 基于模板方法模式，子类实现 tryAcquire/tryRelease 等方法

### 【问题 2】AQS 的两种同步模式？
答：
- **独占模式（Exclusive）**：只有一个线程能执行，如 ReentrantLock
- **共享模式（Shared）**：多个线程可同时执行，如 CountDownLatch、Semaphore

### 【问题 3】state 在不同同步器中的含义？
答：
- **ReentrantLock**：0=未锁定，>0=重入次数
- **CountDownLatch**：剩余需要等待的计数
- **Semaphore**：剩余可用许可数
- **ReentrantReadWriteLock**：高 16 位读锁，低 16 位写锁

### 【问题 4】AQS 为什么使用双向链表？
答：
- 方便节点删除（唤醒时）
- 可以从后向前传播唤醒信号
- 支持公平锁检查前驱节点

### 【问题 5】CLH 队列的作用？
答：
- 存储等待获取锁的线程
- 避免自旋消耗 CPU（阻塞挂起）
- 保证线程按序获取锁（公平锁）
- 减少上下文切换开销

### 【问题 6】AQS 的 acquire() 流程？
答：
1. 调用 tryAcquire() 尝试获取锁
2. 失败则创建 Node 节点加入队列尾部
3. 调用 park() 阻塞线程
4. 被唤醒后再次尝试获取
5. 成功则设为头节点

### 【问题 7】Condition 的作用和使用场景？
答：
- Condition 用于多线程协调，替代 Object 的 wait/notify
- 一个 Lock 可以绑定多个 Condition
- 适用场景：生产者消费者模式、多阶段任务
- await() 释放锁并等待，signal() 唤醒一个线程

### 【问题 8】公平锁和非公平锁的区别？
答：
- **公平锁**：按照请求顺序分配，新请求会排队
- **非公平锁**：插队机制，新请求可能优先获取
- 公平锁吞吐量低但公平性好
- 非公平锁吞吐量大但可能饥饿

### 【问题 9】AQS 有哪些实现类？
答：
- **ReentrantLock**：可重入独占锁
- **ReentrantReadWriteLock**：读写锁
- **CountDownLatch**：倒计时门闩
- **CyclicBarrier**：循环栅栏
- **Semaphore**：信号量
- **ThreadPoolExecutor.Worker**：线程池工作线程

### 【问题 10】为什么 AQS 使用自旋 + 阻塞？
答：
- **自旋**：短时等待，避免上下文切换
- **阻塞**：长时等待，避免浪费 CPU
- 结合两者优势：先自旋，失败再阻塞
- 现代操作系统对自旋优化较好
2. **正确处理中断**：响应中断或忽略中断
3. **避免长时间持有锁**：减少竞争
4. **优先使用现有实现**：ReentrantLock、Semaphore 等
