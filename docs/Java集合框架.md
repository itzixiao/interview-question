# Java 集合框架

## 集合框架结构

```
                            Collection
                    /           |           \
                List          Set          Queue
               /    \       /      \       /      \
         ArrayList  LinkedList  HashSet  TreeSet  PriorityQueue
              |                    |
        CopyOnWriteArrayList   LinkedHashSet
                                 |
                           ConcurrentSkipListSet

                               Map
                    /           |           \
              HashMap    LinkedHashMap    TreeMap
                 |
          ConcurrentHashMap
```

## List 实现

### ArrayList

**特点**：
- 基于动态数组实现
- 随机访问 O(1)
- 插入删除 O(n)
- 非线程安全

**扩容机制**：
```java
// 默认初始容量 10
// 扩容：原容量的 1.5 倍
grow() {
    int newCapacity = oldCapacity + (oldCapacity >> 1);
}
```

**适用场景**：查询多、插入删除少

### LinkedList

**特点**：
- 基于双向链表实现
- 随机访问 O(n)
- 插入删除 O(1)
- 实现了 Deque 接口

**节点结构**：
```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}
```

**适用场景**：插入删除多、需要频繁在首尾操作

### Vector（已过时）

- 线程安全（synchronized）
- 扩容 2 倍
- 性能低，不推荐使用

### CopyOnWriteArrayList

**特点**：
- 线程安全
- 读操作无锁
- 写操作复制新数组

**适用场景**：读多写少

## Set 实现

### HashSet

**特点**：
- 基于 HashMap 实现
- 无序、去重
- O(1) 操作

**去重原理**：
- hashCode() 确定桶位置
- equals() 判断是否相等

### LinkedHashSet

- 基于 LinkedHashMap
- 保持插入顺序

### TreeSet

- 基于红黑树
- 有序（自然排序或 Comparator）
- O(log n) 操作

## Map 实现

### HashMap

详见 [HashMap 源码分析](./HashMap源码分析.md)

### LinkedHashMap

- 继承 HashMap
- 双向链表维护插入顺序
- 可实现 LRU 缓存

```java
// LRU 实现
new LinkedHashMap<K, V>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
};
```

### TreeMap

- 基于红黑树
- 按键排序
- O(log n) 操作

### ConcurrentHashMap

详见 [ConcurrentHashMap 详解](./ConcurrentHashMap详解.md)

## Queue 实现

### PriorityQueue

- 基于小顶堆
- 按优先级出队
- O(log n) 插入，O(1) 查看队首

### ArrayDeque

- 基于循环数组
- 双端队列
- 比 Stack 快，比 LinkedList 省内存

### BlockingQueue

| 实现 | 特点 |
|------|------|
| ArrayBlockingQueue | 有界数组，一把锁 |
| LinkedBlockingQueue | 可选有界，两把锁（读写分离）|
| PriorityBlockingQueue | 无界优先队列 |
| SynchronousQueue | 不存储元素，直接传递 |
| DelayQueue | 延时队列 |

## 迭代器与 fail-fast

### fail-fast 机制

```java
// 迭代过程中修改集合会抛出 ConcurrentModificationException
for (String s : list) {
    list.remove(s);  // 抛出异常
}
```

**原理**：
- modCount 记录修改次数
- 迭代时检查 modCount 是否变化

### 正确删除方式

```java
// 方式1：Iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (condition) {
        it.remove();
    }
}

// 方式2：removeIf（Java 8+）
list.removeIf(s -> condition);

// 方式3：CopyOnWriteArrayList
CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(list);
for (String s : cowList) {
    cowList.remove(s);  // 安全
}
```

## Collections 工具类

### 创建集合

```java
// 空集合（不可修改）
List<String> emptyList = Collections.emptyList();
Set<String> emptySet = Collections.emptySet();
Map<String, String> emptyMap = Collections.emptyMap();

// 单元素集合（不可修改）
List<String> singletonList = Collections.singletonList("item");
Set<String> singletonSet = Collections.singleton("item");

// 不可修改包装
List<String> unmodifiableList = Collections.unmodifiableList(list);
```

### 同步包装

```java
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
```

### 排序与查找

```java
Collections.sort(list);                    // 排序
Collections.reverse(list);                 // 反转
Collections.shuffle(list);                 // 随机打乱
Collections.binarySearch(list, key);       // 二分查找
Collections.max(list);                     // 最大值
Collections.min(list);                     // 最小值
```

## 性能对比

| 操作 | ArrayList | LinkedList | HashMap | TreeMap |
|------|-----------|------------|---------|---------|
| 随机访问 | O(1) | O(n) | - | - |
| 插入/删除 | O(n) | O(1) | O(1) | O(log n) |
| 查找 | O(n) | O(n) | O(1) | O(log n) |

## 最佳实践

1. **预估容量**：避免频繁扩容
2. **选择合适实现**：根据操作特点选择
3. **注意线程安全**：并发环境使用线程安全集合
4. **优先使用接口**：List/Map/Set 作为类型
5. **遍历优化**：entrySet() 优于 keySet()
