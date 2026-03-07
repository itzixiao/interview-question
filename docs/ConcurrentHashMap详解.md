# ConcurrentHashMap 源码分析

## 一、概述

ConcurrentHashMap 是线程安全的 HashMap 实现，提供高并发的键值对存取操作。

### 核心设计

```
┌─────────────────────────────────────────────────────────────┐
│  数据结构：数组 + 链表 + 红黑树（与 HashMap 相同）              │
│  线程安全：CAS + synchronized（锁分段细化到桶级别）            │
│  锁粒度：  每个桶（Node 数组元素）独立加锁                      │
│  并发度：  默认 16（数组长度），可扩容                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、JDK 7 vs JDK 8 实现对比

### JDK 7：分段锁（Segment）

```
结构：Segment[] + HashEntry[]
锁粒度：Segment 级别（默认 16 段）
并发度：固定 16
```

**特点**：
- 每个 Segment 继承 ReentrantLock
- 写操作只锁对应 Segment
- 读操作不加锁（volatile）

**缺点**：
- 并发度固定，无法动态调整
- Segment 对象占用额外内存
- 锁粒度仍然较粗

### JDK 8：CAS + synchronized

```
结构：Node[] + 链表 + 红黑树
锁粒度：桶级别（单个 Node）
并发度：与桶数量相关
```

**特点**：
- 使用 CAS 进行无锁插入
- 冲突时使用 synchronized 锁定桶头节点
- 读操作完全无锁

**优点**：
- 锁粒度更细，并发度更高
- 内存占用更少
- synchronized 在 JDK6 后优化很好

---

## 三、核心数据结构

### 1. 类结构

```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
    implements ConcurrentMap<K,V>, Serializable {
    
    // 主干数组，2的幂次长度
    transient volatile Node<K,V>[] table;
    
    // 扩容时使用的新数组
    private transient volatile Node<K,V>[] nextTable;
    
    // 基础计数器
    private transient volatile long baseCount;
    
    // 控制标识符
    // -1: 正在初始化
    // -(1+n): n个线程正在扩容
    // 正数: 下次扩容阈值
    private transient volatile int sizeCtl;
    
    // 扩容进度
    private transient volatile int transferIndex;
    
    // 计数单元数组（高并发优化）
    private transient volatile CounterCell[] counterCells;
}
```

### 2. Node 节点类型

| 节点类型 | 说明 | hash 值 |
|----------|------|--------|
| Node | 普通节点，链表结构 | 正常 hash 值 |
| TreeNode | 红黑树节点 | 正常 hash 值 |
| TreeBin | 红黑树包装容器 | 正常 hash 值 |
| ForwardingNode | 转发节点（扩容时） | MOVED (-1) |
| ReservationNode | 保留节点（computeIfAbsent） | RESERVED (-3) |

### 3. 哈希桶数组结构

```
┌─────────┬─────────┬─────────┬─────────┬─────────┬─────────┐
│  table  │  table  │  table  │  table  │  table  │  table  │
│  [0]    │  [1]    │  [2]    │  [3]    │  [4]    │  [5]    │
├────┬────┼────┬────┼────┬────┼────┬────┼────┬────┼────┬────┤
│Node│next│Node│next│Tree│    │Node│next│FWD │    │Node│next│
│    │→Node│   │→null│Bin │    │   │→Node│→new│    │   │→null│
│    │→null│   │     │Root│    │   │→null│table│   │   │     │
└────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┴────┘
  链表      单节点   红黑树            转发节点    链表
```

---

## 四、核心方法源码分析

### 1. put() 方法

```java
public V put(K key, V value) {
    return putVal(key, value, false);
}

final V putVal(K key, V value, boolean onlyIfAbsent) {
    // 1. key 和 value 都不允许为 null
    if (key == null || value == null) throw new NullPointerException();
    
    // 2. 计算 hash
    int hash = spread(key.hashCode());
    
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        
        // 3. 数组为空，初始化
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();
            
        // 4. 目标桶为空，CAS 插入新节点
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;
        }
        
        // 5. 正在扩容，帮助扩容
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
            
        // 6. 桶不为空，synchronized 锁定桶头
        else {
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    // 链表
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key, value, null);
                                break;
                            }
                        }
                    }
                    // 红黑树
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key, value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            // 7. 检查是否需要树化
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    // 8. 更新计数，检查是否需要扩容
    addCount(1L, binCount);
    return null;
}
```

**put 流程图**：

```
┌─────────────────────────────────────────────────────────────┐
│  1. 计算 hash：spread(key.hashCode())                        │
│     → (h ^ (h >>> 16)) & HASH_BITS                          │
├─────────────────────────────────────────────────────────────┤
│  2. 检查 table 是否为空或长度为0                              │
│     → 是：初始化 table（initTable）                          │
├─────────────────────────────────────────────────────────────┤
│  3. 计算索引：i = (n - 1) & hash                             │
│     → 获取桶位置 tab[i]                                      │
├─────────────────────────────────────────────────────────────┤
│  4. 检查 tab[i] 是否为空                                      │
│     → 是：CAS 插入新节点（无锁）                             │
├─────────────────────────────────────────────────────────────┤
│  5. 检查 tab[i] 是否为 ForwardingNode（扩容中）               │
│     → 是：帮助扩容（helpTransfer），然后重试                 │
├─────────────────────────────────────────────────────────────┤
│  6. 桶不为空，synchronized 锁定桶头节点                       │
│     → 遍历链表/红黑树，找到或插入                             │
├─────────────────────────────────────────────────────────────┤
│  7. 检查是否需要树化（链表长度 >= 8）                         │
├─────────────────────────────────────────────────────────────┤
│  8. 更新计数（addCount）                                     │
└─────────────────────────────────────────────────────────────┘
```

### 2. get() 方法

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    
    // 1. 计算 hash
    int h = spread(key.hashCode());
    
    // 2. 定位桶
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        
        // 3. 检查头节点
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        
        // 4. hash < 0，可能是红黑树或 ForwardingNode
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
            
        // 5. 遍历链表
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```

**特点**：
- **完全无锁**：不需要加锁
- **volatile 保证可见性**：Node 的 val 和 next 都是 volatile

### 3. CAS 操作

```java
// 获取 tab[i]，使用 Unsafe.getObjectVolatile
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
    return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}

// CAS 设置 tab[i] = v，期望当前值为 c
static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                    Node<K,V> c, Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}

// 设置 tab[i] = v，仅在扩容时使用（已加锁保证安全）
static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
    U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
}
```

---

## 五、扩容机制（transfer）

### 1. 扩容触发条件

- 元素数量 > sizeCtl（容量 * 负载因子，默认 0.75）
- 单个桶链表长度 >= 8，但数组长度 < 64（优先扩容而非树化）

### 2. 多线程协同扩容

```
旧数组：16个桶            新数组：32个桶
┌──┬──┬──┬──┐            ┌──┬──┬──┬──┬──┬──┬──┬──┐
│0 │1 │2 │3 │            │0 │1 │2 │3 │4 │5 │6 │7 │
├──┼──┼──┼──┤            ├──┼──┼──┼──┼──┼──┼──┼──┤
│A │B │C │D │            │A │E │B │F │C │G │D │H │
└──┴──┴──┴──┘            └──┴──┴──┴──┴──┴──┴──┴──┘

扩容过程：
1. 创建新数组（2倍大小）
2. 多个线程从数组尾部开始，每个线程负责一个区间
3. 处理完的桶标记为 ForwardingNode（hash = -1）
4. 其他线程遇到 ForwardingNode 会帮助扩容或跳过
5. 所有桶处理完毕，替换旧数组引用
```

### 3. 链表拆分原理

数组扩容2倍后，元素的新索引 = 原索引 或 原索引 + 原数组长度

```java
// 根据 hash & n（原数组长度）的结果拆分
// 结果为 0：保持原位（lo 链表）
// 结果为 1：移到高位（hi 链表，索引 + n）

if ((e.hash & oldCap) == 0) {
    // 保持原位
    if (loTail == null)
        loHead = e;
    else
        loTail.next = e;
    loTail = e;
} else {
    // 移到高位
    if (hiTail == null)
        hiHead = e;
    else
        hiTail.next = e;
    hiTail = e;
}
```

### 4. 扩容期间的操作

| 操作 | 处理方式 |
|------|----------|
| get() | 正常访问，遇到 ForwardingNode 去新数组查找 |
| put() | 帮助扩容（如果正在扩容），然后在合适位置插入 |
| remove() | 帮助扩容（如果正在扩容），然后删除 |

---

## 六、计数机制（size）

### 1. 为什么不用 AtomicInteger？

- 所有 put/remove 都要修改计数器，成为热点
- AtomicInteger 是单个变量，高并发下 CAS 冲突严重

### 2. CounterCell 分散计数

借鉴 LongAdder 思想：

```
baseCount：基础计数器
└── 低并发时直接使用

CounterCell[] counterCells：计数单元数组
└── 高并发时，线程分散到不同 Cell 计数
└── 每个线程通过 ThreadLocalRandom 选择 Cell

最终 size = baseCount + sum(CounterCell[])
```

### 3. addCount() 流程

```java
private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    
    // 1. 尝试 CAS 更新 baseCount
    if ((as = counterCells) != null ||
        !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        
        CounterCell a; long v; int m;
        boolean uncontended = true;
        
        // 2. 失败说明有竞争，使用 CounterCell
        if (as == null || (m = as.length - 1) < 0 ||
            (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
            !(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
            
            // 3. 更新 CounterCell
            fullAddCount(x, uncontended);
            return;
        }
    }
    
    // 4. 检查是否需要扩容
    if (check >= 0) {
        Node<K,V>[] tab, nt; int n, sc;
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            // 扩容...
        }
    }
}
```

---

## 七、与其他 Map 对比

### 1. 对比表

| 特性 | HashMap | ConcurrentHashMap | Hashtable |
|------|---------|-------------------|-----------|
| 线程安全 | 否 | 是 | 是 |
| 锁机制 | 无 | CAS + synchronized | synchronized |
| 锁粒度 | - | 桶级别 | 全局锁 |
| 允许 null key | 是 | 否 | 否 |
| 允许 null value | 是 | 否 | 否 |
| 迭代器 | fail-fast | 弱一致性 | fail-fast |
| 性能 | 最高 | 高 | 低 |

### 2. 为什么不允许 null 键/值？

二义性问题：`get(key)` 返回 null 时，无法区分是「不存在」还是「值为null」

在多线程环境下，`containsKey()` 和 `get()` 之间可能被其他线程修改，为了避免歧义，直接禁止 null。

---

## 八、复合操作注意

ConcurrentHashMap 的单个操作是原子的，但**复合操作不是**：

```java
// 错误：非原子操作
if (!map.containsKey(k)) {
    map.put(k, v);  // 可能已被其他线程插入
}

// 正确：使用原子方法
map.putIfAbsent(k, v);
map.computeIfAbsent(k, key -> createValue(key));
```

### 原子方法

| 方法 | 说明 |
|------|------|
| putIfAbsent(key, value) | 不存在才放入 |
| computeIfAbsent(key, func) | 不存在则计算 |
| computeIfPresent(key, func) | 存在则计算 |
| compute(key, func) | 无论存在与否都计算 |
| merge(key, value, func) | 合并值 |
| replace(key, oldVal, newVal) | 条件替换 |
| remove(key, value) | 条件删除 |

---

## 九、高频面试题

### Q1: ConcurrentHashMap 如何保证线程安全？

**答案**：
- JDK 7: 分段锁（Segment），每个 Segment 继承 ReentrantLock
- JDK 8: CAS + synchronized，锁住链表/红黑树头节点
- 优势：锁粒度更细，并发度更高

### Q2: JDK7 和 JDK8 的 ConcurrentHashMap 有什么区别？

**答案**：

| JDK 7 | JDK 8 |
|--------|--------|
| 使用 Segment 分段锁 | 取消 Segment，直接在 Node 上加锁 |
| Segment 数组默认 16 | 锁粒度更细 |
| Segment 继承 ReentrantLock | 使用 CAS + synchronized |
| 并发度固定 | 支持动态扩容 |

### Q3: ConcurrentHashMap 的迭代器有什么特点？

**答案**：
弱一致性（Weakly Consistent）：
- 迭代期间修改不会抛 ConcurrentModificationException
- 可能反映迭代期间的修改，也可能不反映
- 不会导致死循环或数据不一致

### Q4: ConcurrentHashMap 如何保证扩容线程安全？

**答案**：
JDK 8 多线程协同扩容：
1. 每个线程负责一部分桶的迁移
2. 使用 ForwardingNode 标记正在迁移的桶
3. 其他线程遇到 ForwardingNode 会协助扩容
4. 扩容期间读写操作正常进行

### Q5: ConcurrentHashMap 的 size() 如何实现？

**答案**：
JDK 8 使用 LongAdder 思想：
- baseCount：基础计数
- counterCells：竞争时的分散计数
- 最终 size = baseCount + counterCells 总和

特点：高并发下性能好，但结果不是强一致的。

### Q6: 为什么 JDK8 用 synchronized 替代 ReentrantLock？

**答案**：
1. synchronized 在 JDK6 后优化很好（偏向锁、轻量级锁）
2. 锁粒度已细化到桶级别，冲突概率低
3. synchronized 不需要额外对象，内存开销小
4. JVM 对 synchronized 优化空间更大

---

## 十、最佳实践

1. **替代 Hashtable**：性能更好，功能更强
2. **复合操作**：使用 `putIfAbsent`、`computeIfAbsent` 等原子方法
3. **批量操作**：`forEach`、`search`、`reduce` 等方法支持批量并行操作
4. **不要在外部加锁**：ConcurrentHashMap 已经处理了同步

---

## 示例代码

完整示例代码请参考：[ConcurrentHashMapDemo.java](../interview-service/src/main/java/cn/itzixiao/interview/hashmap/ConcurrentHashMapDemo.java)
