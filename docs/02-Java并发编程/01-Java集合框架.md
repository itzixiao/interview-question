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

详见 [HashMap 源码分析](../10-%E7%AE%97%E6%B3%95%E4%B8%8E%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84/01-HashMap%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md)

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

详见 [ConcurrentHashMap 详解](02-ConcurrentHashMap%E8%AF%A6%E8%A7%A3.md)

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

---

## 💡 高频面试题

**问题 1:ArrayList 和 LinkedList 的区别？**

答案：
| 特性 | ArrayList | LinkedList |
|------|-----------|------------|
| 底层结构 | 动态数组 | 双向链表 |
| 随机访问 | O(1) | O(n) |
| 插入删除 | O(n) | O(1) |
| 内存占用 | 少 | 多（节点指针） |
| 功能 | List 接口 | List + Deque 接口 |

**源码对比：**
```java
// ArrayList - 数组
transient Object[] elementData;
private static final int DEFAULT_CAPACITY = 10;

// LinkedList - 链表
transient Node<E> first, last;
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}
```

**使用场景：**
- ✅ ArrayList：查询多、增删少（90% 场景）
- ✅ LinkedList：频繁在首尾插入删除、需要队列操作

**性能测试（10 万次操作）：**
```java
// 随机访问
ArrayList.get(i);    // 0.1ms
LinkedList.get(i);   // 50ms

// 尾部插入
ArrayList.add();     // 1ms
LinkedList.add();    // 0.5ms

// 中间插入
ArrayList.add(i, e); // 50ms
LinkedList.add(i, e);// 0.5ms
```

**问题 2:HashMap 和 Hashtable 的区别？**

答案：
| 特性 | HashMap | Hashtable |
|------|---------|-----------|
| 线程安全 | ❌ 否 | ✅ 是（synchronized） |
| null 值 | ✅ 允许 | ❌ 不允许 |
| 性能 | 高 | 低 |
| 扩容 | 2 倍 | 2 倍 +1 |
| hash 计算 | 优化过 | 直接使用 |
| 继承 | AbstractMap | Dictionary（已过时） |

**HashMap 优势：**
```java
// 允许 null
map.put(null, value);
map.put(key, null);

// 性能更好（无锁）
for (int i = 0; i < 100000; i++) {
    map.put(i, i);  // 快
}
```

**线程安全替代方案：**
```java
// ❌ 不推荐 Hashtable
Map<String, String> map = new Hashtable<>();

// ✅ 推荐 ConcurrentHashMap
Map<String, String> map = new ConcurrentHashMap<>();

// ✅ 或者同步包装
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
```

**问题 3:HashSet如何保证元素不重复？**

答案：
HashSet 基于 HashMap 实现，通过 hashCode() 和 equals() 保证唯一性。

**源码分析：**
```java
public class HashSet<E> extends AbstractSet<E> {
    private transient HashMap<E,Object> map;
    private static final Object PRESENT = new Object();
    
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;  // put 返回 null 表示不存在
    }
}
```

**去重流程：**
```
1. 计算 hashCode() 确定桶位置
2. 如果桶为空 → 直接放入
3. 如果桶不为空 → 遍历链表/红黑树
4. 对每个元素：
   - 比较 hashCode 是否相同
   - 如果相同，调用 equals() 比较内容
   - 如果 equals() 返回 true → 已存在，返回 false
   - 否则 → 放入链表末尾，返回 true
```

**示例：**
```java
class Person {
    String name;
    int id;
    
    @Override
    public int hashCode() {
        return Objects.hash(name, id);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof Person) {
            Person p = (Person) o;
            return id == p.id && name.equals(p.name);
        }
        return false;
    }
}

Set<Person> set = new HashSet<>();
set.add(new Person("张三", 1));  // true
set.add(new Person("张三", 1));  // false，重复
```

**重写要求：**
- ✅ 必须同时重写 hashCode() 和 equals()
- ✅ 相等对象必须有相同 hashCode
- ✅ 只重写一个会导致去重失效

**问题 4:fail-fast机制是什么？如何解决？**

答案：
fail-fast 是集合的一种错误检测机制，在迭代过程中如果集合被修改，立即抛出 ConcurrentModificationException。

**原理：**
```java
// AbstractList 中维护 modCount
protected transient int modCount = 0;

// 添加/删除时修改 modCount
modCount++;

// 迭代器检查 expectedModCount
final void checkForComodification() {
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

**触发场景：**
```java
// ❌ 错误示范
List<String> list = new ArrayList<>();
list.add("A");
list.add("B");

for (String s : list) {
    list.remove(s);  // ConcurrentModificationException!
}
```

**解决方案：**

**方案 1：Iterator.remove()**
```java
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String s = it.next();
    if (condition) {
        it.remove();  // ✅ 安全
    }
}
```

**方案 2：removeIf（Java 8+）**
```java
list.removeIf(s -> condition);  // ✅ 最简洁
```

**方案 3：CopyOnWriteArrayList**
```java
CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>(list);
for (String s : cowList) {
    cowList.remove(s);  // ✅ 安全，写时复制
}
```

**方案 4：普通 for 循环（倒序）**
```java
for (int i = list.size() - 1; i >= 0; i--) {
    if (condition) {
        list.remove(i);  // ✅ 倒序避免索引错位
    }
}
```

**问题 5:ArrayBlockingQueue和 LinkedBlockingQueue的区别？**

答案：
| 特性 | ArrayBlockingQueue | LinkedBlockingQueue |
|------|-------------------|--------------------|
| 底层结构 | 数组 | 链表 |
| 有界/无界 | 有界（必须指定容量） | 可选（默认 Integer.MAX_VALUE） |
| 锁机制 | 一把锁（ReentrantLock） | 两把锁（读写分离） |
| 公平性 | 可选公平/非公平 | 只能非公平 |
| 内存占用 | 连续空间，较少 | 节点对象，较多 |
| 适用场景 | 数据量固定 | 数据量不确定 |

**源码对比：**
```java
// ArrayBlockingQueue - 一把锁
final ReentrantLock lock;
final Condition notEmpty;
final Condition notFull;

// LinkedBlockingQueue - 两把锁
private final ReentrantLock takeLock;
private final Condition notEmpty;
private final ReentrantLock putLock;
private final Condition notFull;
```

**性能对比：**
```java
// 高并发场景
LinkedBlockingQueue 吞吐量更高（读写锁分离）
ArrayBlockingQueue 简单高效（单锁）
```

**使用建议：**
- ✅ 容量固定 → ArrayBlockingQueue
- ✅ 容量不确定 → LinkedBlockingQueue（注意设置上限）
- ✅ 高并发读 → LinkedBlockingQueue

**问题 6:CopyOnWriteArrayList的原理和使用场景？**

答案：
CopyOnWriteArrayList（COW）是一种线程安全的 List，核心思想是**写时复制**。

**原理：**
```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        // 复制新数组
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        setArray(newElements);  // 引用指向新数组
        return true;
    } finally {
        lock.unlock();
    }
}
```

**特点：**
- ✅ 读操作无锁，性能极高
- ✅ 写操作加锁并复制数组
- ✅ 迭代器不会抛出 ConcurrentModificationException
- ❌ 内存占用大（复制数组）
- ❌ 数据一致性弱（可能读到旧数据）

**使用场景：**
- ✅ 读多写少（如监听器列表）
- ✅ 白名单、黑名单配置
- ❌ 写多读少（频繁复制，性能差）

**示例：**
```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
list.add("A");
list.add("B");

// 迭代安全
for (String s : list) {
    list.remove(s);  // ✅ 不会抛异常
}
```

**问题 7:LRU缓存如何用 LinkedHashMap 实现？**

答案：
LinkedHashMap 可以通过 accessOrder 参数实现 LRU（最近最少使用）缓存。

**实现方式：**
```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxCapacity;
    
    public LRUCache(int capacity) {
        super(16, 0.75f, true);  // accessOrder=true
        this.maxCapacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxCapacity;  // 超过容量删除最老
    }
}

// 使用
Map<Integer, Integer> cache = new LRUCache<>(4);
for (int i = 0; i < 10; i++) {
    cache.put(i, i);
}
// 缓存中只剩：6, 7, 8, 9（删除了 0-5）
```

**核心参数：**
```java
public LinkedHashMap(int initialCapacity,
                     float loadFactor,
                     boolean accessOrder) {  // true=访问顺序，false=插入顺序
    super(initialCapacity, loadFactor);
    this.accessOrder = accessOrder;
}
```

**LRU 原理：**
```
每次访问（get/put）→ 移动到链表尾部
删除时 → 删除链表头部（最久未使用）
```

**问题 8:Collection和 Collections的区别？**

答案：

| 类名 | 类型 | 作用 |
|------|------|------|
| Collection | 接口 | 集合的根接口（List/Set/Queue 的父接口） |
| Collections | 工具类 | 操作集合的静态方法 |

**Collection 接口：**
```java
public interface Collection<E> extends Iterable<E> {
    boolean add(E e);
    boolean remove(Object o);
    int size();
    // ... 其他集合操作方法
}
```

**Collections 工具类：**
```java
// 排序
Collections.sort(list);

// 同步包装
List<T> syncList = Collections.synchronizedList(new ArrayList<>());

// 不可修改
List<T> unmodifiableList = Collections.unmodifiableList(list);

// 空集合
List<T> emptyList = Collections.emptyList();

// 二分查找
int index = Collections.binarySearch(list, key);

// 最大值/最小值
Object max = Collections.max(list);
Object min = Collections.min(list);
```

**记忆技巧：**
- Collection → 集合（单数，接口）
- Collections → 集合工具（复数，工具类）

---

## 💡 高频面试题

**问题 1：ArrayList 和 LinkedList 的区别？**
