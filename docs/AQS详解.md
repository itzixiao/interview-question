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
2. **正确处理中断**：响应中断或忽略中断
3. **避免长时间持有锁**：减少竞争
4. **优先使用现有实现**：ReentrantLock、Semaphore 等
