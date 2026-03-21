# LRU缓存淘汰策略详解

> **文档信息**
> - **难度等级**：⭐⭐⭐⭐（中高级）
> - **学习时长**：建议1-1.5天
> - **面试频率**：⭐⭐⭐⭐⭐（极高）
> - **配套代码**：`interview-algorithm/cache/LRUCache.java`（546行，7种缓存实现）
> - **关联文档**：[01-HashMap源码分析](./01-HashMap%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md)、[缓存策略详解](../10-缓存与NoSQL数据库/02-Redis缓存策略与三大问题.md)

## 一、算法概述

### 1.1 什么是LRU

LRU（Least Recently Used，最近最少使用）是一种**常用的缓存淘汰策略**。

**核心思想**：当缓存满时，淘汰最久未被访问的数据。

```
访问序列：A → B → C → A → D → B
缓存大小：3

步骤演示：
1. 访问A：[A]           （未满，直接放入）
2. 访问B：[B, A]        （未满，放入头部）
3. 访问C：[C, B, A]     （未满，放入头部）
4. 访问A：[A, C, B]     （已存在，移到头部）
5. 访问D：[D, A, C]     （已满，淘汰B，放入D）
6. 访问B：[B, D, A]     （B不在缓存，淘汰C，放入B）

最终缓存：[B, D, A]
被淘汰：C（最久未使用）
```

**设计原理**：
- **时间局部性原理**：最近被访问的数据很可能再次被访问
- **空间局部性原理**：相邻数据可能被一起访问

### 1.2 应用场景

| 场景 | 说明 | 典型应用 |
|------|------|----------|
| **数据库连接池** | 管理数据库连接 | HikariCP、Druid |
| **图片缓存** | 缓存用户浏览过的图片 | Glide、Picasso |
| **页面缓存** | Web页面缓存 | Spring Cache、Guava Cache |
| **CPU缓存** | 硬件级缓存管理 | CPU L1/L2/L3 Cache |
| **Redis缓存** | 内存数据库淘汰策略 | Redis LRU/LFU |
| **操作系统** | 页面置换算法 | Linux Page Cache |

## 二、LRU实现方式

### 2.1 哈希表 + 双向链表（标准实现）

**数据结构**：
- **哈希表**：存储key到节点的映射，实现O(1)查找
- **双向链表**：按访问时间排序，头部为最近访问，尾部为最久未访问

```java
class LRUCacheManual<K, V> {
    class DLinkedNode {
        K key;
        V value;
        DLinkedNode prev;
        DLinkedNode next;
    }
    
    private final int capacity;
    private final Map<K, DLinkedNode> cache;
    private final DLinkedNode head;  // 哨兵头节点
    private final DLinkedNode tail;  // 哨兵尾节点
    
    public LRUCacheManual(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        
        // 初始化哨兵节点
        head = new DLinkedNode();
        tail = new DLinkedNode();
        head.next = tail;
        tail.prev = head;
    }
    
    public V get(K key) {
        DLinkedNode node = cache.get(key);
        if (node == null) return null;
        
        moveToHead(node);  // 移动到头部（表示最近使用）
        return node.value;
    }
    
    public void put(K key, V value) {
        DLinkedNode node = cache.get(key);
        
        if (node == null) {
            // 创建新节点
            DLinkedNode newNode = new DLinkedNode();
            newNode.key = key;
            newNode.value = value;
            
            cache.put(key, newNode);
            addToHead(newNode);
            
            // 超出容量，淘汰尾部节点
            if (cache.size() > capacity) {
                DLinkedNode tailNode = removeTail();
                cache.remove(tailNode.key);
            }
        } else {
            node.value = value;
            moveToHead(node);
        }
    }
    
    // 双向链表操作
    private void addToHead(DLinkedNode node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    private void removeNode(DLinkedNode node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    private void moveToHead(DLinkedNode node) {
        removeNode(node);
        addToHead(node);
    }
    
    private DLinkedNode removeTail() {
        DLinkedNode node = tail.prev;
        removeNode(node);
        return node;
    }
}
```

### 2.2 LinkedHashMap实现（简洁版）

```java
class LRUCacheLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    
    public LRUCacheLinkedHashMap(int capacity) {
        // accessOrder = true，按访问顺序排序
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

## 三、LRU变体

### 3.1 带过期时间的LRU

```java
class ExpirableLRUCache<K, V> {
    class CacheEntry {
        V value;
        long expireTime;
        
        CacheEntry(V value, long ttlMillis) {
            this.value = value;
            this.expireTime = System.currentTimeMillis() + ttlMillis;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }
    
    private final LRUCacheManual<K, CacheEntry> cache;
    
    public V get(K key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) return null;
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return entry.value;
    }
    
    public void put(K key, V value, long ttlMillis) {
        cache.put(key, new CacheEntry(value, ttlMillis));
    }
}
```

### 3.2 LFU缓存（Least Frequently Used）

```java
class LFUCache<K, V> {
    class Node {
        K key;
        V value;
        int freq = 1;
    }
    
    private final int capacity;
    private final Map<K, Node> cache;
    private final Map<Integer, LinkedHashSet<K>> freqMap;
    private int minFreq;
    
    public V get(K key) {
        Node node = cache.get(key);
        if (node == null) return null;
        
        increaseFreq(node);
        return node.value;
    }
    
    public void put(K key, V value) {
        if (cache.containsKey(key)) {
            cache.get(key).value = value;
            increaseFreq(cache.get(key));
            return;
        }
        
        if (cache.size() >= capacity) {
            removeMinFreq();
        }
        
        Node newNode = new Node();
        newNode.key = key;
        newNode.value = value;
        cache.put(key, newNode);
        freqMap.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
        minFreq = 1;
    }
    
    private void increaseFreq(Node node) {
        int freq = node.freq;
        freqMap.get(freq).remove(node.key);
        if (freqMap.get(freq).isEmpty() && freq == minFreq) {
            minFreq++;
        }
        node.freq++;
        freqMap.computeIfAbsent(node.freq, k -> new LinkedHashSet<>()).add(node.key);
    }
}
```

## 四、LRU vs 其他淘汰策略

| 策略 | 淘汰依据 | 优点 | 缺点 |
|------|----------|------|------|
| LRU | 最近访问时间 | 符合时间局部性 | 对突发流量敏感 |
| LFU | 访问频率 | 保留热门数据 | 需要维护频率计数 |
| FIFO | 进入时间 | 实现简单 | 不考虑访问模式 |
| Random | 随机 | 实现最简单 | 效果最差 |

## 五、面试高频题

1. **LRU的时间复杂度？**
   - get操作：O(1)
   - put操作：O(1)

2. **为什么要用双向链表而不是单链表？**⭐⭐⭐⭐⭐
   
   **关键原因**：删除节点时需要O(1)找到前驱节点
   
   ```
   删除当前节点：
   
   双向链表：
   [prev] <-> [current] <-> [next]
   
   操作：
   prev.next = next
   next.prev = prev
   时间：O(1)
   
   单链表：
   [prev] -> [current] -> [next]
   
   操作：
   需要从头遍历找到prev
   时间：O(n)
   ```

3. **LRU和LFU的区别？**⭐⭐⭐⭐⭐
   
   | 特性 | LRU | LFU |
   |------|-----|-----|
   | **淘汰策略** | 最久未访问 | 访问频率最低 |
   | **数据结构** | HashMap + 双向链表 | HashMap + 多个频率链表 |
   | **适用场景** | 突发热点数据 | 长期热点数据 |
   | **缺点** | 可能淘汰长期热点 | 历史数据影响大 |
   
   **示例对比**：
   ```
   访问序列：A×100次, B×1次（最近访问）
   
   LRU淘汰：A（虽然访问100次，但最近没访问）
   LFU淘汰：B（只访问1次）
   
   结论：LRU关注时间，LFU关注频率
   ```

4. **如何实现线程安全的LRU？**⭐⭐⭐⭐
   
   | 方案 | 实现方式 | 优点 | 缺点 |
   |------|----------|------|------|
   | **方案1** | `Collections.synchronizedMap` | 简单 | 全局锁，并发低 |
   | **方案2** | `ConcurrentHashMap` + `ReentrantReadWriteLock` | 读写分离 | 实现复杂 |
   | **方案3** | `LinkedHashMap` + 同步块 | 简单 | 全局锁 |
   | **方案4** | Guava Cache / Caffeine | 成熟稳定 | 引入依赖 |
   
   **推荐**：生产环境使用Guava Cache或Caffeine

5. **Redis中的LRU实现？**⭐⭐⭐⭐
   
   **Redis近似LRU**：
   ```
   原因：精确LRU需要为每个key记录访问时间，内存开销大
   
   方案：
   1. 随机采样N个key（默认5个）
   2. 淘汰其中最久未访问的
   3. 重复直到释放足够内存
   
   效果：近似LRU，内存占用小，性能更好
   ```
   
   **Redis淘汰策略**：
   - `volatile-lru`：在过期key中使用LRU
   - `allkeys-lru`：在所有key中使用LRU
   - `volatile-lfu`：在过期key中使用LFU
   - `allkeys-lfu`：在所有key中使用LFU

## 六、知识图谱

```
LRU缓存知识体系
│
├── 基础概念
│   ├── 缓存淘汰策略
│   ├── 时间局部性原理
│   └── LRU vs LFU vs FIFO
│
├── 核心实现
│   ├── 哈希表 + 双向链表
│   │   ├── 查找：O(1)
│   │   ├── 插入：O(1)
│   │   └── 删除：O(1)
│   │
│   └── LinkedHashMap实现
│       └── accessOrder = true
│
├── 扩展变体
│   ├── 带过期时间的LRU
│   ├── LFU缓存
│   ├── 二级缓存
│   └── 线程安全版本
│
├── 实际应用
│   ├── 数据库连接池
│   ├── 图片/页面缓存
│   ├── Redis缓存
│   └── CPU缓存
│
└── 面试重点
    ├── 数据结构选择（为什么用双向链表）
    ├── 时间复杂度分析
    ├── LRU vs LFU对比
    └── 线程安全实现
```

## 七、示例代码

完整示例代码位于：`interview-microservices-parent/interview-algorithm/src/main/java/cn/itzixiao/interview/algorithm/cache/LRUCache.java`

运行方式：
```bash
mvn exec:java -pl interview-microservices-parent/interview-algorithm \
  -Dexec.mainClass="cn.itzixiao.interview.algorithm.cache.LRUCache" -q
```
