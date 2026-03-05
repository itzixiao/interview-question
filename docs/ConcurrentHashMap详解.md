# ConcurrentHashMap 详解

## 概述

ConcurrentHashMap 是线程安全的 HashMap 实现，提供高并发的键值对存取操作。

## JDK 7 vs JDK 8 实现对比

### JDK 7：分段锁（Segment）

```
结构：Segment[] + HashEntry[]
锁粒度：Segment 级别（默认 16 段）
并发度：固定 16
```

- 每个 Segment 继承 ReentrantLock
- 写操作只锁对应 Segment
- 读操作不加锁（volatile）

### JDK 8：CAS + synchronized

```
结构：Node[] + 链表 + 红黑树
锁粒度：桶级别（单个 Node）
并发度：与桶数量相关
```

- 使用 CAS 进行无锁插入
- 冲突时使用 synchronized 锁定桶头节点
- 读操作完全无锁

## 核心字段

| 字段 | 说明 |
|------|------|
| table | Node 数组，存储数据 |
| sizeCtl | 控制标识符（负数表示正在初始化或扩容）|
| baseCount | 元素计数基础值 |
| counterCells | 计数单元数组（分散热点）|

## 核心方法解析

### 1. put() 流程

1. 计算 hash 和索引
2. 数组为空：CAS 初始化
3. 目标桶为空：CAS 插入新节点
4. 目标桶正在扩容：协助扩容
5. 目标桶有数据：synchronized 锁定桶头，插入或更新

### 2. 扩容机制（transfer）

- **多线程协作**：多个线程可以同时进行扩容
- **ForwardingNode**：标记已迁移的桶
- **链表拆分**：根据 hash 高位拆分为 lo/hi 两个链表

### 3. 计数机制

```
总元素数 = baseCount + sum(counterCells)
```

- 使用 CounterCell 数组分散热点
- 减少多线程竞争

## 与 Hashtable/Collections.synchronizedMap 对比

| 实现方式 | 锁机制 | 读操作 | 并发度 |
|----------|--------|--------|--------|
| Hashtable | synchronized（全表锁）| 阻塞 | 低 |
| synchronizedMap | synchronized（全表锁）| 阻塞 | 低 |
| ConcurrentHashMap JDK7 | ReentrantLock（分段锁）| 无锁 | 中 |
| ConcurrentHashMap JDK8 | CAS + synchronized（桶锁）| 无锁 | 高 |

## 复合操作注意

ConcurrentHashMap 的单个操作是原子的，但**复合操作不是**：

```java
// 错误：非原子操作
if (!map.containsKey(k)) {
    map.put(k, v);  // 可能已被其他线程插入
}

// 正确：使用原子方法
map.putIfAbsent(k, v);
map.computeIfAbsent(k, k -> createValue());
```

## 最佳实践

1. **替代 Hashtable**：性能更好，功能更强
2. **复合操作**：使用 `putIfAbsent`、`computeIfAbsent` 等原子方法
3. **批量操作**：`forEach`、`search`、`reduce` 等方法支持批量并行操作
