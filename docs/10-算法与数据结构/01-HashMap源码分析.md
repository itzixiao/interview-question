# HashMap 源码分析

## 概述

HashMap 是 Java 中最常用的集合类之一，基于**哈希表**实现，提供快速的键值对存取操作。

## 核心数据结构

```
JDK 7：数组 + 链表
JDK 8+：数组 + 链表 + 红黑树
```

### 关键参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| DEFAULT_INITIAL_CAPACITY | 16 | 默认初始容量（必须是2的幂） |
| DEFAULT_LOAD_FACTOR | 0.75 | 默认负载因子 |
| TREEIFY_THRESHOLD | 8 | 链表转红黑树阈值 |
| UNTREEIFY_THRESHOLD | 6 | 红黑树转链表阈值 |
| MIN_TREEIFY_CAPACITY | 64 | 最小树化容量 |

## 核心方法解析

### 1. hash() 方法 - 扰动函数

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**作用**：将高16位与低16位异或，增加哈希值的随机性，减少冲突。

### 2. put() 方法流程

1. 计算 key 的 hash 值
2. 计算数组下标：`(n - 1) & hash`
3. 无冲突：直接插入新节点
4. 有冲突：
   - 链表长度 < 8：尾插法插入链表
   - 链表长度 ≥ 8 且数组长度 ≥ 64：转为红黑树
5. 检查是否需要扩容

### 3. 扩容机制（resize）

- **触发条件**：元素数量 > 容量 × 负载因子
- **扩容大小**：原容量的 2 倍
- **数据迁移**：
  - JDK 7：重新计算所有元素的哈希位置
  - JDK 8：根据 hash 高位判断，要么在原位置，要么在原位置 + 旧容量

### 4. 链表转红黑树

**条件**：
- 链表长度 ≥ 8
- 数组长度 ≥ 64（否则优先扩容）

**原因**：
- 链表查询复杂度 O(n)，红黑树 O(log n)
- 避免在较小容量时过早树化，浪费空间

## 线程安全问题

HashMap **不是线程安全的**，多线程环境下可能出现：
- 数据丢失（覆盖）
- 死循环（JDK 7 头插法导致的环形链表）

**解决方案**：
- 使用 `ConcurrentHashMap`
- 使用 `Collections.synchronizedMap()`

## 与 Hashtable 对比

| 特性 | HashMap | Hashtable |
|------|---------|-----------|
| 线程安全 | 否 | 是（synchronized）|
| 性能 | 高 | 低（全表锁）|
| null 键/值 | 允许 | 不允许 |
| 出现版本 | JDK 1.2 | JDK 1.0 |

## 最佳实践

1. **指定初始容量**：预估数据量，避免频繁扩容
2. **自定义对象作为 key**：必须重写 `hashCode()` 和 `equals()`
3. **遍历方式**：优先使用 `entrySet()` 而非 `keySet()`

---

## 八、高频面试题

**问题 1:HashMap的底层数据结构？**
答：
- JDK 7：数组 + 链表（头插法）
- JDK 8+：数组 + 链表 + 红黑树（尾插法）
- 链表长度 > 8 且数组长度 ≥ 64 时转红黑树

**问题 2:HashMap的 put 流程？**
答：
1. 计算 key 的 hash 值（扰动函数）
2. 计算数组下标：(n-1) & hash
3. 无冲突直接插入
4. 有冲突则遍历链表/红黑树
   - key 已存在：覆盖 value
   - key 不存在：插入尾部
5. 检查是否树化或扩容

**问题 3:HashMap为什么使用扰动函数？**
答：
```java
return (h = key.hashCode()) ^ (h >>> 16);
```
- 混合高 16 位和低 16 位
- 增加哈希值的随机性
- 减少哈希冲突，使分布更均匀

**问题 4:HashMap的扩容机制？**
答：
- **触发条件**：size > capacity × loadFactor
- **扩容大小**：原容量的 2 倍
- **数据迁移**：
  - 根据 hash 高位判断位置
  - 要么在原位置，要么在 原位置 + 旧容量
- 避免了 JDK 7 的重新计算

**问题 5:HashMap为什么线程不安全？**
答：
- 多线程同时 put 可能导致：
  1. 数据覆盖（后一个覆盖前一个）
  2. 死循环（JDK 7 头插法导致环形链表）
  3. size 计数不准确
- 解决方案：ConcurrentHashMap、Collections.synchronizedMap()

**问题 6：链表转红黑树的条件？**
答：
- 链表长度 ≥ 8
- 数组长度 ≥ 64
- 两个条件同时满足才树化
- 否则优先扩容而不是树化

**问题 7:HashMap与 HashTable 的区别？**
答：
| 特性 | HashMap | HashTable |
|------|---------|----------|
| 线程安全 | 否 | 是（全表锁）|
| 性能 | 高 | 低 |
| null 键值 | 允许 | 不允许 |
| 继承 | AbstractMap | Dictionary |
| 出现版本 | JDK 1.2 | JDK 1.0 |

**问题 8:HashMap的默认负载因子为什么是 0.75？**
答：
- **空间和时间权衡**
- 太大：空间利用率高，但冲突多，查询慢
- 太小：冲突少，但空间浪费
- 0.75 是经验值，综合表现最好

**问题 9:HashMap如何获取 key？**
答：
```java
Node<K,V> getNode(int hash, Object key) {
    Node<K,V>[] tab; Node<K,V> first, e;
    if ((tab = table) != null && 
        (first = tab[(n - 1) & hash]) != null) {
        // 检查首节点
        if (first.hash == hash && 
            ((k = first.key) == key || key.equals(k)))
            return first;
        // 遍历链表/红黑树
        if ((e = first.next) != null) {
            if (first instanceof TreeNode)
                return ((TreeNode<K,V>)first).getTreeNode(hash, key);
            do {
                if (e.hash == hash &&
                    ((k = e.key) == key || key.equals(k)))
                    return e;
            } while ((e = e.next) != null);
        }
    }
    return null;
}
```

**问题 10:HashMap的遍历方式？**
答：
1. **entrySet()**（推荐）：同时获取 key 和 value
2. **keySet()**：先获取 key，再 get value（效率低）
3. **forEach()**：Java 8 Lambda 方式
4. **Iterator**：迭代器方式
