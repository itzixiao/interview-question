# CAS 与原子类详解

## 概述

CAS（Compare And Swap）是一种乐观锁实现，是并发编程的基础机制。

## CAS 原理

### 操作包含三个值

1. **内存位置 V**：变量的内存地址
2. **预期值 A**：旧值
3. **新值 B**：要更新的值

### 操作流程

```
if V == A:
    V = B（交换成功）
else:
    交换失败，重试
```

### CPU 指令支持

- x86：`cmpxchg` 指令
- ARM：`LDREX` / `STREX` 指令

## Unsafe 类

Java 通过 `sun.misc.Unsafe` 类提供 CAS 操作：

```java
// 比较并交换 int
public final native boolean compareAndSwapInt(
    Object o, long offset, int expected, int x);

// 比较并交换 long
public final native boolean compareAndSwapLong(
    Object o, long offset, long expected, long x);

// 比较并交换 Object
public final native boolean compareAndSwapObject(
    Object o, long offset, Object expected, Object x);
```

## 原子类体系

### 基本类型

| 类名 | 说明 |
|------|------|
| AtomicInteger | int 原子操作 |
| AtomicLong | long 原子操作 |
| AtomicBoolean | boolean 原子操作 |

### 引用类型

| 类名 | 说明 |
|------|------|
| AtomicReference | 引用原子操作 |
| AtomicStampedReference | 带版本号的引用（解决ABA）|
| AtomicMarkableReference | 带标记的引用 |

### 数组类型

| 类名 | 说明 |
|------|------|
| AtomicIntegerArray | int 数组原子操作 |
| AtomicLongArray | long 数组原子操作 |
| AtomicReferenceArray | 引用数组原子操作 |

### 字段更新器

| 类名 | 说明 |
|------|------|
| AtomicIntegerFieldUpdater | 更新对象的 int 字段 |
| AtomicLongFieldUpdater | 更新对象的 long 字段 |
| AtomicReferenceFieldUpdater | 更新对象的引用字段 |

### 累加器（JDK 8+）

| 类名 | 说明 |
|------|------|
| LongAdder | long 累加器（高并发优化）|
| DoubleAdder | double 累加器 |
| LongAccumulator | 自定义累加规则 |
| DoubleAccumulator | 自定义累加规则 |

## ABA 问题

### 问题描述

```
线程1：读取 A
线程2：A → B → A
线程1：CAS 成功（但值已被修改过）
```

### 解决方案

使用 `AtomicStampedReference`，增加版本号：

```java
AtomicStampedReference<String> ref = 
    new AtomicStampedReference<>("A", 0);

// 需要同时比较值和版本号
ref.compareAndSet("A", "B", 0, 1);
```

## LongAdder 优化原理

### 问题

高并发下，多个线程同时 CAS 更新同一个变量，导致大量自旋重试。

### 解决方案

```
base + Cell[0] + Cell[1] + ... + Cell[n]
```

- 将累加分散到多个 Cell
- 不同线程操作不同 Cell
- 最后求和时汇总所有 Cell

### 伪共享问题

Cell 使用 `@sun.misc.Contended` 注解进行缓存行填充，避免伪共享。

## CAS 的优缺点

### 优点

- 无锁，性能高
- 无死锁风险
- 原子性保证

### 缺点

- ABA 问题
- 自旋消耗 CPU
- 只能保证单个变量原子性

## 使用场景

1. **计数器**：AtomicInteger、LongAdder
2. **序列号生成**：AtomicLong
3. **状态标志**：AtomicBoolean
4. **无锁队列**：ConcurrentLinkedQueue（基于 CAS）
