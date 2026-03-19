# Java 集合框架

## 集合框架结构

### 顶层接口体系

```
java.lang.Iterable
    └── java.util.Collection
              ├── List
              ├── Set
              └── Queue
java.util.Map（独立体系，不继承 Collection）
```

---

### List 体系

```
List（接口）
  ├── ArrayList          基于动态数组，随机访问 O(1)，非线程安全
  ├── LinkedList         基于双向链表，实现 List + Deque，非线程安全
  ├── Vector             基于动态数组，synchronized 全锁，已过时
  └── CopyOnWriteArrayList  写时复制，读无锁，线程安全，读多写少
```

---

### Set 体系

```
Set（接口）
  ├── HashSet            基于 HashMap，无序去重，O(1)，非线程安全
  ├── LinkedHashSet      基于 LinkedHashMap，保持插入顺序，非线程安全
  ├── TreeSet            基于红黑树，有序，O(log n)，非线程安全
  └── ConcurrentSkipListSet  基于跳表，有序，O(log n)，线程安全（CAS）
```

---

### Queue 体系

```
Queue（接口）
  │
  ├── PriorityQueue      基于最小堆，按优先级出队，O(log n)，非线程安全
  │
  ├── Deque（双端队列接口）
  │     ├── ArrayDeque       基于循环数组，替代 Stack/LinkedList，非线程安全
  │     ├── LinkedList       同时实现 List + Deque
  │     └── BlockingDeque（接口）
  │           └── LinkedBlockingDeque  双端阻塞队列，可选有界，线程安全
  │
  └── BlockingQueue（阻塞队列接口）
        ├── ArrayBlockingQueue      有界数组，一把锁，支持公平/非公平
        ├── LinkedBlockingQueue     可选有界链表，两把锁（读写分离），高吞吐
        ├── PriorityBlockingQueue   无界优先队列，基于最小堆，线程安全
        ├── SynchronousQueue        容量为 0，不存储元素，直接移交
        ├── DelayQueue              无界延迟队列，基于 PriorityQueue，到期才出队
        └── LinkedTransferQueue     无界链表，CAS 实现，综合性能最优
```

---

### Map 体系

```
Map（接口）
  ├── HashMap            数组+链表/红黑树，O(1)，非线程安全，允许 null
  ├── LinkedHashMap      继承 HashMap，双向链表维护插入/访问顺序
  ├── TreeMap            基于红黑树，按 Key 有序，O(log n)，非线程安全
  ├── Hashtable          全 synchronized，已过时，不允许 null
  └── ConcurrentHashMap  CAS + synchronized 分段，O(1)，线程安全，高并发首选
```

---

### 线程安全集合速查

| 类型  | 非线程安全（高性能）                       | 线程安全（并发场景）                            |
|-----|----------------------------------|---------------------------------------|
| List | ArrayList、LinkedList             | CopyOnWriteArrayList                  |
| Set | HashSet、LinkedHashSet、TreeSet    | ConcurrentSkipListSet                 |
| Map | HashMap、LinkedHashMap、TreeMap   | ConcurrentHashMap、ConcurrentSkipListMap |
| Queue | ArrayDeque、PriorityQueue        | ArrayBlockingQueue、LinkedBlockingQueue 等所有 BlockingQueue 实现 |

## List 实现

### ArrayList

**特点**：

- 基于动态数组（Object[]）实现
- 随机访问 O(1)
- 中间插入删除 O(n)（需移位）
- 非线程安全
- 支持 null 元素

**核心字段**：

```java
// 底层数组，transient 不直接参与序列化
transient Object[] elementData;
// 实际元素数量（非数组长度）
private int size;
// 默认初始容量
private static final int DEFAULT_CAPACITY = 10;
// 空数组共享实例（new ArrayList() 时指向此）
private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};
```

**扩容机制（完整源码）**：

```java
// 添加元素时触发扩容检查
private void ensureCapacityInternal(int minCapacity) {
    if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
        minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
    }
    ensureExplicitCapacity(minCapacity);
}

private void ensureExplicitCapacity(int minCapacity) {
    modCount++;
    if (minCapacity - elementData.length > 0)
        grow(minCapacity);
}

private void grow(int minCapacity) {
    int oldCapacity = elementData.length;
    // 新容量 = 旧容量 * 1.5（右移1位）
    int newCapacity = oldCapacity + (oldCapacity >> 1);
    if (newCapacity - minCapacity < 0)
        newCapacity = minCapacity;
    // 不超过 Integer.MAX_VALUE - 8
    if (newCapacity - MAX_ARRAY_SIZE > 0)
        newCapacity = hugeCapacity(minCapacity);
    elementData = Arrays.copyOf(elementData, newCapacity);
}
```

**扩容流程**：

```
初始 add() → 容量0 → 扩容至 DEFAULT_CAPACITY(10)
第11次 add() → 容量10 → 扩容至 15
第16次 add() → 容量15 → 扩容至 22
...（每次 *1.5）
```

**序列化机制**：

```java
// elementData 标记为 transient，自定义序列化只序列化有效元素
private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    int expectedModCount = modCount;
    s.defaultWriteObject();
    s.writeInt(size);  // 写入实际大小
    for (int i = 0; i < size; i++)
        s.writeObject(elementData[i]);  // 只序列化有效元素
    if (modCount != expectedModCount)
        throw new ConcurrentModificationException();
}
```

**适用场景**：查询多、插入删除少（90% 场景首选）

### LinkedList

**特点**：

- 基于双向链表实现
- 随机访问 O(n)
- 首尾插入删除 O(1)
- 实现了 List 和 Deque 双接口
- 无容量限制，不需要扩容

**节点结构**：

```java
private static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}

// 首尾指针
transient Node<E> first;
transient Node<E> last;
```

**核心操作源码**：

```java
// 头插
private void linkFirst(E e) {
    final Node<E> f = first;
    final Node<E> newNode = new Node<>(null, e, f);
    first = newNode;
    if (f == null) last = newNode;
    else f.prev = newNode;
    size++;
    modCount++;
}

// 尾插
void linkLast(E e) {
    final Node<E> l = last;
    final Node<E> newNode = new Node<>(l, e, null);
    last = newNode;
    if (l == null) first = newNode;
    else l.next = newNode;
    size++;
    modCount++;
}
```

**适用场景**：插入删除多、需要频繁在首尾操作、作为队列/栈使用

### Vector（已过时）

- 线程安全（所有方法加 synchronized）
- 扩容 2 倍（capacityIncrement 为 0 时）
- 性能低，不推荐使用；高并发场景用 `CopyOnWriteArrayList` 替代

### CopyOnWriteArrayList

**特点**：

- 线程安全
- **读操作完全无锁**（直接读快照数组）
- **写操作加 ReentrantLock + 复制新数组**
- 迭代器获取的是快照，不反映最新修改

**适用场景**：读多写少（监听器列表、配置白名单）

## ArrayList 线程安全实现方案

### 方案 1：CopyOnWriteArrayList（推荐）

```java
List<String> safeList = new CopyOnWriteArrayList<>();
```

**特点**：

- ✅ 读操作无锁，性能极高
- ✅ 写操作加锁并复制数组
- ✅ 迭代器不会抛出 ConcurrentModificationException
- ❌ 内存占用大（复制数组）
- ❌ 数据一致性弱（可能读到旧数据）

**适用场景**：读多写少（如监听器列表、白名单配置）

### 方案 2：Collections.synchronizedList

```java
List<String> list = new ArrayList<>();
List<String> syncList = Collections.synchronizedList(list);
```

**特点**：

- ✅ 所有操作加 synchronized 锁
- ✅ 包装已有 ArrayList
- ❌ 性能较低（每次操作都要加锁）
- ❌ 迭代时需手动同步

**使用注意**：

```java
// 迭代时必须手动同步
synchronized (syncList) {
    for (String s : syncList) {
        // 安全操作
    }
}
```

### 方案 3：Vector（已过时，不推荐）

```java
List<String> vector = new Vector<>();
```

**特点**：

- ✅ 线程安全（synchronized）
- ❌ 性能低
- ❌ 扩容 2 倍，浪费内存
- ❌ API 设计老旧

**不推荐原因**：所有方法都用 synchronized 修饰，并发度低

### 方案 4：ReentrantLock + ArrayList（自定义控制）

```java
public class SafeArrayList<T> {
    private final List<T> list = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    
    public void add(T element) {
        lock.lock();
        try {
            list.add(element);
        } finally {
            lock.unlock();
        }
    }
    
    public T get(int index) {
        lock.lock();
        try {
            return list.get(index);
        } finally {
            lock.unlock();
        }
    }
}
```

**特点**：

- ✅ 灵活控制锁的粒度
- ✅ 可实现读写分离
- ❌ 代码复杂

### 方案 5：ThreadLocal（线程隔离）

```java
private static final ThreadLocal<List<String>> threadLocalList = 
    ThreadLocal.withInitial(ArrayList::new);
```

**特点**：

- ✅ 每个线程独立副本，无需加锁
- ✅ 性能极高
- ❌ 线程间数据不共享
- ❌ 内存泄漏风险（需手动 remove）

**适用场景**：线程间不需要共享数据的场景

### 方案对比

| 方案                           | 性能    | 灵活性 | 适用场景   |
|------------------------------|-------|-----|--------|
| CopyOnWriteArrayList         | ⭐⭐⭐⭐⭐ | 中   | 读多写少   |
| Collections.synchronizedList | ⭐⭐⭐   | 高   | 通用场景   |
| Vector                       | ⭐⭐    | 低   | 不推荐    |
| ReentrantLock + ArrayList    | ⭐⭐⭐⭐  | 极高  | 需要精细控制 |
| ThreadLocal                  | ⭐⭐⭐⭐⭐ | 中   | 线程隔离   |

### 最佳实践

```java
// ✅ 推荐：读多写少
List<String> configList = new CopyOnWriteArrayList<>();

// ✅ 推荐：通用场景
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// ✅ 推荐：高并发读写分离
ReadWriteLock rwLock = new ReentrantReadWriteLock();

// ❌ 不推荐：Vector
List<String> vector = new Vector<>();  // 性能差
```

## SET 实现

### HashSet

**特点**：

- 基于 HashMap 实现（key 存元素，value 统一为 PRESENT 占位对象）
- 无序、去重
- O(1) 增删查（哈希冲突严重时退化）
- 允许 null 元素（至多一个）
- 非线程安全

**去重原理**：

```java
// 本质是 HashMap 的 put 操作
private static final Object PRESENT = new Object();

public boolean add(E e) {
    return map.put(e, PRESENT) == null;
}
```

- hashCode() 确定桶位置
- equals() 判断是否相等（两者必须同时重写）

### LinkedHashSet

- 基于 LinkedHashMap（HashSet 的子类）
- **保持插入顺序**（双向链表维护）
- 迭代顺序与插入顺序一致
- 适合需要去重又需要保持顺序的场景

### TreeSet

- 基于 **红黑树**（TreeMap 实现）
- **有序**（自然排序 Comparable 或自定义 Comparator）
- O(log n) 增删查
- 不允许 null 元素（null 无法比较）
- 适合需要排序的去重场景

```java
// 自定义排序
TreeSet<Person> set = new TreeSet<>(Comparator.comparingInt(Person::getAge));
```

### ConcurrentSkipListSet

- 基于 **跳表**（ConcurrentSkipListMap 实现）
- **线程安全**，无锁实现（CAS）
- 有序（自然排序或自定义 Comparator）
- O(log n) 增删查
- 高并发场景下替代 TreeSet 的首选

```java
// 线程安全的有序 Set
ConcurrentSkipListSet<Integer> set = new ConcurrentSkipListSet<>();
```

### Set 实现对比

| 特性     | HashSet  | LinkedHashSet | TreeSet    | ConcurrentSkipListSet |
|--------|----------|---------------|------------|-----------------------|
| 底层结构   | HashMap  | LinkedHashMap | 红黑树        | 跳表                    |
| 有序性    | 无序       | 插入顺序          | 自然/自定义排序   | 自然/自定义排序              |
| null支持 | 允许       | 允许            | 不允许        | 不允许                   |
| 线程安全   | 否        | 否             | 否          | 是                     |
| 时间复杂度  | O(1)     | O(1)          | O(log n)   | O(log n)              |
| 适用场景   | 通用去重     | 有序去重          | 排序去重       | 高并发有序去重               |

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

### Queue 接口方法体系

Queue 接口提供两套操作方法，区别在于**失败时的处理行为**：

| 操作   | 抛出异常      | 返回特殊值        |
|------|-----------|--------------|
| 入队   | add(e)    | offer(e)     |
| 出队   | remove()  | poll()       |
| 查看队首 | element() | peek()       |

```java
// Deque（双端队列）额外提供首尾操作
deque.addFirst(e) / deque.addLast(e)
deque.removeFirst() / deque.removeLast()
deque.peekFirst() / deque.peekLast()

// BlockingQueue 额外提供阻塞操作
queue.put(e)    // 阻塞直到有空间
queue.take()    // 阻塞直到有元素
queue.offer(e, timeout, unit)  // 超时等待
queue.poll(timeout, unit)      // 超时等待
```

---

### PriorityQueue

**特点**：

- 基于**最小堆**（完全二叉树数组实现）
- 默认自然排序（最小元素优先出队）
- **非线程安全**，不允许 null 元素
- O(log n) 入队/出队，O(1) 查看队首

**核心源码**：

```java
// 底层数组
transient Object[] queue;
// 默认容量
private static final int DEFAULT_INITIAL_CAPACITY = 11;

// 上浮（siftUp）：插入元素后维护堆性质
private void siftUpComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;  // 父节点索引
        Object e = queue[parent];
        if (key.compareTo((E) e) >= 0)
            break;
        queue[k] = e;
        k = parent;
    }
    queue[k] = key;
}

// 下沉（siftDown）：删除堆顶后维护堆性质
private void siftDownComparable(int k, E x) {
    Comparable<? super E> key = (Comparable<? super E>) x;
    int half = size >>> 1;
    while (k < half) {
        int child = (k << 1) + 1;        // 左子节点
        Object c = queue[child];
        int right = child + 1;
        if (right < size && ((Comparable<? super E>) c).compareTo((E) queue[right]) > 0)
            c = queue[child = right];     // 取较小子节点
        if (key.compareTo((E) c) <= 0)
            break;
        queue[k] = c;
        k = child;
    }
    queue[k] = key;
}
```

**使用示例**：

```java
// 默认最小堆（最小值先出）
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(5);
minHeap.offer(1);
minHeap.offer(3);
System.out.println(minHeap.poll()); // 输出 1

// 最大堆（最大值先出）
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
maxHeap.offer(5);
maxHeap.offer(1);
maxHeap.offer(3);
System.out.println(maxHeap.poll()); // 输出 5

// 自定义对象优先级（任务调度）
PriorityQueue<Task> taskQueue = new PriorityQueue<>(Comparator.comparingInt(Task::getPriority));
```

**适用场景**：

- ✅ Dijkstra 最短路径算法
- ✅ Top K 问题（如最大/最小 K 个数）
- ✅ 任务调度（按优先级执行）
- ✅ 合并 K 个有序链表

---

### ArrayDeque

**特点**：

- 基于**循环数组**实现的双端队列
- 非线程安全，不允许 null 元素
- 容量自动扩容（初始 16，扩容为 2 倍）
- **比 Stack 快**（推荐替代 Stack）
- **比 LinkedList 省内存**（无节点对象开销）

**核心字段**：

```java
transient Object[] elements;  // 循环数组
transient int head;           // 队头指针
transient int tail;           // 队尾指针
// head == tail 时为空，(tail+1)%len == head 时满
```

**循环数组原理**：

```
初始状态:  [_, _, _, _, _, _, _, _]  head=0, tail=0
addLast(1): [1, _, _, _, _, _, _, _]  head=0, tail=1
addLast(2): [1, 2, _, _, _, _, _, _]  head=0, tail=2
addFirst(0):[1, 2, _, _, _, _, _, 0]  head=7, tail=2  ← head 循环到末尾
```

**使用示例**：

```java
// 作为栈使用（替代 Stack）
Deque<String> stack = new ArrayDeque<>();
stack.push("A");   // 等价于 addFirst
stack.push("B");
System.out.println(stack.pop());   // 输出 B（LIFO）

// 作为队列使用（替代 LinkedList）
Deque<String> queue = new ArrayDeque<>();
queue.offer("A");  // 等价于 addLast
queue.offer("B");
System.out.println(queue.poll());  // 输出 A（FIFO）

// BFS 广度优先搜索
Deque<TreeNode> bfsQueue = new ArrayDeque<>();
bfsQueue.offer(root);
while (!bfsQueue.isEmpty()) {
    TreeNode node = bfsQueue.poll();
    if (node.left != null) bfsQueue.offer(node.left);
    if (node.right != null) bfsQueue.offer(node.right);
}
```

**适用场景**：

- ✅ 替代 Stack 实现栈操作（DFS 迭代版）
- ✅ 替代 LinkedList 实现队列（BFS）
- ✅ 滑动窗口最大值（单调双端队列）

---

### BlockingQueue 体系

BlockingQueue 是**线程安全的阻塞队列**，核心是**阻塞语义**：队满时 put 阻塞，队空时 take 阻塞。是线程池、生产者-消费者模式的基础。

#### ArrayBlockingQueue

**特点**：

- 基于**有界数组**（必须指定容量，创建后不可更改）
- **一把 ReentrantLock**（读写共用同一把锁）
- 支持**公平/非公平**锁（默认非公平）
- 元素 FIFO 顺序

**核心源码**：

```java
// 核心字段
final Object[] items;          // 循环数组
int takeIndex;                 // 出队指针
int putIndex;                  // 入队指针
int count;                     // 元素数量
final ReentrantLock lock;      // 唯一的锁
private final Condition notEmpty;  // 非空条件
private final Condition notFull;   // 非满条件

// put 操作（阻塞）
public void put(E e) throws InterruptedException {
    checkNotNull(e);
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == items.length)
            notFull.await();   // 队满时阻塞等待
        enqueue(e);
    } finally {
        lock.unlock();
    }
}

// take 操作（阻塞）
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        while (count == 0)
            notEmpty.await();  // 队空时阻塞等待
        return dequeue();
    } finally {
        lock.unlock();
    }
}
```

**适用场景**：容量固定、需要公平性保证

#### LinkedBlockingQueue

**特点**：

- 基于**链表**（可选有界，默认 Integer.MAX_VALUE 近似无界）
- **两把锁**（takeLock + putLock，读写分离，吞吐量更高）
- 不支持公平锁
- 内存占用略高（节点对象）

**核心源码**：

```java
// 两把锁实现读写分离
private final ReentrantLock takeLock = new ReentrantLock();
private final Condition notEmpty = takeLock.newCondition();

private final ReentrantLock putLock = new ReentrantLock();
private final Condition notFull = putLock.newCondition();

// 原子计数（两把锁需要原子操作维护 count）
private final AtomicInteger count = new AtomicInteger();

// 链表节点
static class Node<E> {
    E item;
    Node<E> next;
}
```

**生产者-消费者示例**：

```java
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

// 生产者线程
new Thread(() -> {
    try {
        queue.put("task-" + i);   // 满时阻塞
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

// 消费者线程
new Thread(() -> {
    try {
        String task = queue.take();  // 空时阻塞
        process(task);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();
```

**适用场景**：线程池工作队列、高并发生产者-消费者（默认容量需注意 OOM）

#### PriorityBlockingQueue

**特点**：

- **无界**优先级阻塞队列（基于最小堆）
- 线程安全（ReentrantLock）
- 只有队空时 take 才阻塞（无界不会阻塞 put）
- 不允许 null 元素

```java
// 按任务优先级处理
PriorityBlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>(
    11, Comparator.comparingInt(Task::getPriority)
);

taskQueue.put(new Task("LOW", 3));
taskQueue.put(new Task("HIGH", 1));
taskQueue.put(new Task("MED", 2));
System.out.println(taskQueue.take().getName()); // 输出 HIGH（优先级最高）
```

**适用场景**：需要按优先级消费的任务队列（如消息分级处理）

#### SynchronousQueue

**特点**：

- **不存储元素**，每次 put 必须等待对应的 take（直接移交）
- 容量为 0，是一个**握手机制**
- 支持公平/非公平模式（对应队列/栈数据结构）
- `Executors.newCachedThreadPool()` 使用此队列

```java
SynchronousQueue<String> sq = new SynchronousQueue<>();

// 线程1：put 阻塞，直到线程2 take
new Thread(() -> {
    try {
        sq.put("Hello");
        System.out.println("put 成功");
    } catch (InterruptedException e) { ... }
}).start();

// 线程2：take 接收，两线程直接握手
new Thread(() -> {
    try {
        String s = sq.take();
        System.out.println("take: " + s);
    } catch (InterruptedException e) { ... }
}).start();
```

**适用场景**：线程间直接传递数据、`newCachedThreadPool` 的工作队列

#### DelayQueue

**特点**：

- **无界延迟队列**，元素必须实现 `Delayed` 接口
- 只有**延迟时间到期**的元素才能被取出
- 内部基于 PriorityQueue（按到期时间排序）
- 线程安全（ReentrantLock）

```java
// 延迟任务
class DelayTask implements Delayed {
    private final String name;
    private final long expireTime;  // 绝对到期时间（ms）

    public DelayTask(String name, long delayMs) {
        this.name = name;
        this.expireTime = System.currentTimeMillis() + delayMs;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        // 返回剩余延迟时间
        return unit.convert(expireTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.expireTime, ((DelayTask) other).expireTime);
    }
}

// 使用
DelayQueue<DelayTask> delayQueue = new DelayQueue<>();
delayQueue.put(new DelayTask("Task-A", 3000));  // 3秒后可取
delayQueue.put(new DelayTask("Task-B", 1000));  // 1秒后可取

DelayTask task = delayQueue.take();  // 阻塞直到最近的任务到期
System.out.println("执行: " + task.getName()); // 先输出 Task-B
```

**适用场景**：

- ✅ 订单超时自动取消
- ✅ 缓存过期自动清理
- ✅ 定时任务调度（轻量级替代方案）

#### LinkedTransferQueue

**特点**：

- **无界**链表队列，JDK 7 新增
- 综合了 LinkedBlockingQueue 和 SynchronousQueue 的优点
- `transfer(e)`：若有消费者等待则直接移交，否则入队等待消费
- 性能优于 LinkedBlockingQueue（无锁 CAS 实现）

```java
LinkedTransferQueue<String> ltq = new LinkedTransferQueue<>();

// transfer：有消费者则直接交，无则阻塞
ltq.transfer("item");   // 类似 SynchronousQueue.put()

// tryTransfer：有消费者则交，无消费者立即返回 false
boolean success = ltq.tryTransfer("item");

// offer：正常入队（不阻塞）
ltq.offer("item");
```

**适用场景**：高性能消息传递场景

#### LinkedBlockingDeque

**特点**：

- **双端阻塞队列**（实现 BlockingDeque 接口）
- 可选有界（默认 Integer.MAX_VALUE）
- 支持 FIFO 和 LIFO 操作
- **一把 ReentrantLock** 控制首尾操作

```java
LinkedBlockingDeque<String> deque = new LinkedBlockingDeque<>(100);
deque.putFirst("A");   // 头部阻塞插入
deque.putLast("B");    // 尾部阻塞插入
deque.takeFirst();     // 头部阻塞取出
deque.takeLast();      // 尾部阻塞取出
```

**适用场景**：工作窃取（work-stealing）场景

---

### BlockingQueue 实现对比

| 实现                    | 有界/无界 | 底层结构 | 锁机制       | 是否存储 | 特殊能力     | 适用场景        |
|-----------------------|-------|------|-----------|------|----------|-------------|
| ArrayBlockingQueue    | 有界    | 数组   | 一把锁       | 是    | 支持公平锁    | 容量固定场景      |
| LinkedBlockingQueue   | 可选    | 链表   | 两把锁（读写分离） | 是    | 高吞吐      | 线程池工作队列     |
| PriorityBlockingQueue | 无界    | 最小堆  | 一把锁       | 是    | 按优先级出队   | 优先级任务调度     |
| SynchronousQueue      | 0     | -    | CAS       | 否    | 直接移交     | 线程间直接传递数据   |
| DelayQueue            | 无界    | 最小堆  | 一把锁       | 是    | 延迟到期才出队  | 定时任务/缓存过期   |
| LinkedTransferQueue   | 无界    | 链表   | CAS       | 是    | transfer | 高性能消息传递     |
| LinkedBlockingDeque   | 可选    | 链表   | 一把锁       | 是    | 双端操作     | 工作窃取        |

---

### Queue 操作方法速查

```java
// ===== 非阻塞操作 =====
queue.offer(e)    // 入队，失败返回 false（推荐）
queue.add(e)      // 入队，失败抛 IllegalStateException
queue.poll()      // 出队，空时返回 null（推荐）
queue.remove()    // 出队，空时抛 NoSuchElementException
queue.peek()      // 查看队首，空时返回 null（推荐）
queue.element()   // 查看队首，空时抛 NoSuchElementException

// ===== 阻塞操作（BlockingQueue）=====
queue.put(e)               // 入队，满时阻塞
queue.take()               // 出队，空时阻塞
queue.offer(e, 3, SECONDS) // 入队，超时返回 false
queue.poll(3, SECONDS)     // 出队，超时返回 null
```

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

### List / Map 核心操作

| 操作    | ArrayList | LinkedList | HashMap | TreeMap  |
|-------|-----------|------------|---------|----------|
| 随机访问  | O(1)      | O(n)       | -       | -        |
| 插入/删除 | O(n)      | O(1)       | O(1)    | O(log n) |
| 查找    | O(n)      | O(n)       | O(1)    | O(log n) |

### Queue 核心操作

| 操作    | ArrayDeque | PriorityQueue | ArrayBlockingQueue | LinkedBlockingQueue |
|-------|------------|---------------|--------------------|---------------------|
| 入队    | O(1) 均摊   | O(log n)      | O(1)               | O(1)                |
| 出队    | O(1)       | O(log n)      | O(1)               | O(1)                |
| 查看队首  | O(1)       | O(1)          | O(1)               | O(1)                |
| 线程安全  | 否          | 否             | 是                  | 是                   |
| 并发吞吐量 | -          | -             | 中（单锁）              | 高（双锁）               |

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

| 特性   | ArrayList | LinkedList      |
|------|-----------|-----------------|
| 底层结构 | 动态数组      | 双向链表            |
| 随机访问 | O(1)      | O(n)            |
| 插入删除 | O(n)      | O(1)            |
| 内存占用 | 少         | 多（节点指针）         |
| 功能   | List 接口   | List + Deque 接口 |

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

| 特性      | HashMap     | Hashtable         |
|---------|-------------|-------------------|
| 线程安全    | ❌ 否         | ✅ 是（synchronized） |
| null 值  | ✅ 允许        | ❌ 不允许             |
| 性能      | 高           | 低                 |
| 扩容      | 2 倍         | 2 倍 +1            |
| hash 计算 | 优化过         | 直接使用              |
| 继承      | AbstractMap | Dictionary（已过时）   |

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

**问题 5:ArrayBlockingQueue 和 LinkedBlockingQueue 的区别？**

答案：

| 特性    | ArrayBlockingQueue | LinkedBlockingQueue      |
|-------|--------------------|--------------------------|
| 底层结构  | 数组                 | 链表                       |
| 有界/无界 | 有界（必须指定容量）         | 可选（默认 Integer.MAX_VALUE） |
| 锁机制   | 一把锁（ReentrantLock） | 两把锁（读写分离）                |
| 公平性   | 可选公平/非公平           | 只能非公平                    |
| 内存占用  | 连续空间，较少            | 节点对象，较多                  |
| 适用场景  | 数据量固定              | 数据量不确定                   |

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

**问题 8:Collection 和 Collections 的区别？**

答案：

| 类名          | 类型  | 作用                          |
|-------------|-----|-----------------------------|
| Collection  | 接口  | 集合的根接口（List/Set/Queue 的父接口） |
| Collections | 工具类 | 操作集合的静态方法                   |

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

**问题 9：BlockingQueue 的 put/offer/add 有什么区别？**

答案：

| 方法                     | 队满时行为     | 返回值           | 推荐场景      |
|------------------------|-----------|---------------|-----------|
| add(e)                 | 抛出异常      | true/异常       | 不推荐       |
| offer(e)               | 返回 false  | true/false    | 非阻塞场景     |
| offer(e, timeout, unit) | 等待超时      | true/false    | 有超时要求的场景  |
| put(e)                 | **一直阻塞**  | void          | 生产者-消费者模式 |

```java
BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);

// add：队满抛 IllegalStateException
queue.add("A");
queue.add("B");
queue.add("C");  // 抛 IllegalStateException: Queue full

// offer：队满返回 false，不抛异常
boolean success = queue.offer("C");  // false

// offer 超时：等待最多 1 秒
boolean ok = queue.offer("C", 1, TimeUnit.SECONDS);

// put：队满一直阻塞，直到有空间
queue.put("C");  // 阻塞...直到消费者取走一个元素
```

**问题 10：DelayQueue 的实现原理？如何实现订单超时取消？**

答案：

DelayQueue 内部基于 **PriorityQueue** 存储元素（按到期时间排序），通过 `ReentrantLock + Condition` 实现线程安全和阻塞语义。

**核心原理：**

```java
// take() 核心逻辑
public E take() throws InterruptedException {
    final ReentrantLock lock = this.lock;
    lock.lockInterruptibly();
    try {
        for (;;) {
            E first = q.peek();  // 查看最早到期的元素
            if (first == null)
                available.await();  // 队空，阻塞
            else {
                long delay = first.getDelay(NANOSECONDS);
                if (delay <= 0)
                    return q.poll();  // 已到期，直接返回
                first = null;
                if (leader != null)
                    available.await();  // 其他线程已在等待，继续等
                else {
                    Thread thisThread = Thread.currentThread();
                    leader = thisThread;
                    try {
                        available.awaitNanos(delay);  // 等待到期
                    } finally {
                        if (leader == thisThread) leader = null;
                    }
                }
            }
        }
    } finally {
        if (leader == null && q.peek() != null)
            available.signal();
        lock.unlock();
    }
}
```

**订单超时取消实现：**

```java
@Component
public class OrderTimeoutHandler {
    private final DelayQueue<OrderDelayTask> delayQueue = new DelayQueue<>();

    @PostConstruct
    public void start() {
        // 启动消费线程
        new Thread(this::processTimeoutOrders, "order-timeout-thread").start();
    }

    // 下单时加入延迟队列（30分钟超时）
    public void addOrder(String orderId) {
        delayQueue.put(new OrderDelayTask(orderId, 30 * 60 * 1000L));
    }

    private void processTimeoutOrders() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                OrderDelayTask task = delayQueue.take(); // 阻塞直到有订单超时
                // 检查订单状态，若仍未支付则取消
                cancelOrderIfUnpaid(task.getOrderId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

**生产环境建议**：订单量大时用 Redis ZSet（zadd + zrangebyscore + 定时轮询）替代 DelayQueue，以支持分布式场景。

**问题 11：SynchronousQueue 的工作原理？为什么 newCachedThreadPool 选择它？**

答案：

SynchronousQueue **不存储元素**，每一个 put 操作都必须等待一个 take 操作，反之亦然。

**两种模式：**

```
非公平（默认，TransferStack）：LIFO，后来的请求优先匹配
公平（TransferQueue）：FIFO，先来的请求先匹配
```

**核心 transfer 方法（伪代码）：**

```java
// put/take 都调用 transfer
Object transfer(Object e, boolean timed, long nanos) {
    // e != null 为 put，e == null 为 take
    if (有等待的互补操作) {
        // 直接握手（put 遇到 take 或 take 遇到 put）
        return 直接传递数据;
    } else {
        // 入栈/队等待
        阻塞等待互补操作;
    }
}
```

**newCachedThreadPool 选择 SynchronousQueue 的原因：**

```java
// newCachedThreadPool 配置
new ThreadPoolExecutor(
    0,                      // 核心线程数为 0（无常驻线程）
    Integer.MAX_VALUE,      // 最大线程数无限
    60L, TimeUnit.SECONDS,  // 空闲60秒回收
    new SynchronousQueue<>()  // 任务不排队，直接创建新线程
);
```

- 任务提交时：SynchronousQueue 无法入队 → 直接创建新线程执行
- 无任务时：线程空闲 60 秒后自动销毁
- **注意**：任务量暴增时会创建大量线程，可能导致 OOM，生产慎用

**问题 12：PriorityQueue 如何实现 Top K 问题？**

答案：

Top K（最大的 K 个数）使用**最小堆**（大小为 K），遍历数组：元素比堆顶大则替换堆顶并重新堆化。

```java
public int[] topKFrequent(int[] nums, int k) {
    // 统计频次
    Map<Integer, Integer> freq = new HashMap<>();
    for (int n : nums) freq.merge(n, 1, Integer::sum);

    // 最小堆（按频次），堆大小固定为 k
    PriorityQueue<int[]> minHeap = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));

    for (Map.Entry<Integer, Integer> entry : freq.entrySet()) {
        minHeap.offer(new int[]{entry.getKey(), entry.getValue()});
        if (minHeap.size() > k)
            minHeap.poll();  // 移除频次最小的元素，保留 Top K
    }

    int[] result = new int[k];
    for (int i = k - 1; i >= 0; i--)
        result[i] = minHeap.poll()[0];
    return result;
}
```

**时间复杂度分析：**

| 方法     | 时间复杂度      | 空间复杂度 |
|--------|-----------|-------|
| 排序后取前K | O(n log n) | O(n)  |
| 最小堆    | O(n log k) | O(k)  |
| 快速选择   | O(n) 均摊   | O(1)  |

**堆的选择原则：**

- Top K 最大 → **最小堆**（淘汰小的，保留大的）
- Top K 最小 → **最大堆**（淘汰大的，保留小的）