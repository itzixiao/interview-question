# HashMap 扩容与树化调试指南

## 📌 核心结论

### 1. 扩容时机：**先扩容，再插入**

```java
// HashMap.putVal() 方法的核心逻辑
final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
    Node<K,V>[] tab; Node<K,V> p; int n, i;
    
    // 1. 初始化数组
    if ((tab = table) == null || (n = tab.length) == 0)
        n = (tab = resize()).length;
    
    // 2. 如果桶为空，直接创建新节点
    if ((p = tab[i = (n - 1) & hash]) == null)
        tab[i] = newNode(hash, key, value, null);
    else {
        // 3. 处理冲突（链表或树）
        Node<K,V> e; K k;
        if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
            e = p;
        else if (p instanceof TreeNode)
            e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
        else {
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, null);
                    // ⚠️ 关键：插入后检查是否需要树化
                    if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
                        treeifyBin(tab, hash);
                    break;
                }
                if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
        }
        if (e != null) { // existing mapping found
            V oldValue = e.value;
            if (!onlyIfAbsent || oldValue == null)
                e.value = value;
            afterNodeAccess(e);
            return null;
        }
    }
    
    ++modCount;
    // ⚠️ 关键：在插入完成后，检查是否需要扩容
    if (++size > threshold)
        resize();  // 先扩容
    afterNodeInsertion(evict);
    return null;
}
```

**实际执行流程：**
```
第 1-12 个元素：正常插入到容量为 16 的数组中
准备插入第 13 个元素时：
  → size(12) >= threshold(12) 条件满足
  → 调用 resize() 扩容到 32
  → 然后才将第 13 个元素插入到新数组
```

### 2. 树化时机：**先插入，再转树**

```java
// 树化的完整流程
if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st
    treeifyBin(tab, hash);
```

**实际执行流程：**
```
插入第 8 个元素到链表尾部
  → binCount = 8 (达到阈值)
  → 检查 capacity 是否 >= 64
  → 如果是：调用 treeifyBin() 转树
  → 如果否：调用 resize() 扩容
```

---

## 🔍 调试方法论

### 方法 1：运行示例代码（推荐快速上手）

```bash
cd E:\itzixiao\project\java\interview-question
cd interview-microservices-parent\interview-service\target\classes
java cn.itzixiao.interview.hashmap.HashMapSourceAnalysis
```

**输出关键信息：**
- 每次插入前后的 size、capacity、threshold
- 扩容发生的确切时机（第 13 个元素）
- 树化发生的确切时机（容量 64 + 链表长度 8）

### 方法 2：IDEA Debug 断点调试（深入理解）

#### 步骤 1：找到源码位置
在 IDEA 中打开 JDK 源码，定位到 `HashMap.java`

#### 步骤 2：设置关键断点

**断点 1：扩容入口**
```java
// HashMap.java line ~667
if (++size > threshold)
    resize();  // 👈 在这里设置断点
```

**断点 2：resize() 方法**
```java
// HashMap.java line ~716
final Node<K,V>[] resize() {  // 👈 在这里设置断点
    Node<K,V>[] oldTab = table;
    int oldCap = (oldTab == null) ? 0 : oldTab.length;
    int oldThr = threshold;
    // ...
}
```

**断点 3：树化入口**
```java
// HashMap.java line ~659
if (binCount >= TREEIFY_THRESHOLD - 1)
    treeifyBin(tab, hash);  // 👈 在这里设置断点
```

**断点 4：treeifyBin() 方法**
```java
// HashMap.java line ~1876
final void treeifyBin(Node<K,V>[] tab, int hash) {  // 👈 在这里设置断点
    int n, index; Node<K,V> e;
    // ...
}
```

#### 步骤 3：观察关键变量

在 Debug 过程中，重点关注以下变量：

**扩容时观察：**
- `size`: 当前元素个数
- `threshold`: 扩容阈值
- `table.length`: 数组容量
- `oldTab` vs `newTab`: 扩容前后的数组对比

**树化时观察：**
- `binCount`: 链表长度
- `TREEIFY_THRESHOLD`: 树化阈值（值为 8）
- `MIN_TREEIFY_CAPACITY`: 最小树化容量（值为 64）
- `tab.length`: 当前数组容量

---

## 📊 实验数据对比

### 实验 1：扩容时机验证

| 插入元素 | 插入前 size | 插入前 threshold | 插入后 size | 插入后 capacity | 是否扩容 |
|---------|------------|-----------------|------------|----------------|---------|
| 0-11    | 0-11       | 12              | 1-12       | 16             | ❌ 否    |
| **12**  | **12**     | **12**          | **13**     | **32**         | ✅ **是** |
| 13-14   | 13-14      | 24              | 14-15      | 32             | ❌ 否    |

**结论：** 当 `size >= threshold` 时，在下一次插入前会先触发扩容。

### 实验 2：树化时机验证

#### 场景 A：小容量数组（capacity < 64）

| 插入元素 | 链表长度 | 当前 capacity | 是否树化 | 行为           |
|---------|---------|---------------|---------|----------------|
| 1-7     | 1-7     | 16            | ❌ 否    | 正常插入链表   |
| 8       | 8       | 16            | ❌ 否    | 不树化，等待扩容 |
| 9-10    | 9-10    | 16→32→64      | ❌ 否    | 优先扩容       |

**结论：** 即使链表长度达到 8，如果容量不足 64，会优先扩容而非树化。

#### 场景 B：大容量数组（capacity >= 64）

| 插入元素 | 链表长度 | 当前 capacity | 是否树化 | 行为               |
|---------|---------|---------------|---------|--------------------|
| 1-7     | 1-7     | 64            | ❌ 否    | 正常插入链表       |
| **8**   | **8**   | **64**        | ✅ **是**| **调用 treeifyBin()** |
| 9-10    | 树结构   | 64            | ❌ 否    | 树的平衡插入逻辑   |

**结论：** 只有同时满足 `链表长度>=8` 且 `容量>=64` 才会树化。

---

## 🎯 核心问题解答

### Q1: 为什么扩容是在插入之前？

**答案：** 
扩容涉及到数组的重新分配和元素的重新哈希，必须在插入新元素之前完成，否则：
1. 如果先插入再扩容，新元素可能插入到旧数组的错误位置
2. 扩容后所有元素的索引需要重新计算，必须先创建新数组

**源码证明：**
```java
// putVal() 方法的最后部分
++modCount;
if (++size > threshold)  // 先检查
    resize();            // 再扩容
afterNodeInsertion(evict);
```

### Q2: 为什么树化是在插入之后？

**答案：**
树化是对已有链表的优化，必须等元素插入后才能判断是否需要转换：
1. 需要先知道链表的实际长度
2. 树化是针对已存在的数据结构进行转换
3. 新插入的节点可能就是触发树化的最后一个节点

**源码证明：**
```java
for (int binCount = 0; ; ++binCount) {
    if ((e = p.next) == null) {
        p.next = newNode(hash, key, value, null); // 先插入
        if (binCount >= TREEIFY_THRESHOLD - 1)    // 后检查
            treeifyBin(tab, hash);                 // 再树化
        break;
    }
    // ...
}
```

### Q3: 为什么容量<64 时不树化？

**答案：**
这是空间与时间的权衡：
1. **扩容更经济**：在小数组上，扩容成本低，且能有效分散冲突
2. **避免浪费**：红黑树节点占用更多内存（包含左右子节点引用），小数组上过早树化浪费空间
3. **性能考虑**：容量较小时，链表长度通常较短，遍历效率并不比红黑树差太多

---

## 💡 学习建议

### 第一遍：运行示例代码
1. 直接运行 `HashMapSourceAnalysis.java`
2. 观察输出日志，理解扩容和树化的时机
3. 关注关键节点的提示信息

### 第二遍：Debug 跟读源码
1. 在 IDEA 中设置上述 4 个断点
2. 使用 Debug 模式运行示例代码
3. 逐个断点暂停，观察变量变化
4. 手动 Step Over/Step Into 关键方法

### 第三遍：画图理解
1. 画出扩容前后的数组结构对比
2. 画出链表转红黑树的节点变化
3. 标注出每个阶段的关键变量值

### 第四遍：面试题巩固
回答以下问题：
1. HashMap 什么时候扩容？
2. HashMap 什么时候树化？
3. 为什么容量必须是 2 的幂？
4. 负载因子 0.75 的设计原理？

---

## 📚 扩展阅读

### 相关面试题
1. HashMap 的底层数据结构是什么？
2. HashMap 如何解决哈希冲突？
3. HashMap 为什么线程不安全？
4. ConcurrentHashMap 如何保证线程安全？
5. HashMap 与 HashTable 的区别？

### 进阶研究
1. JDK7 与 JDK8 的 HashMap 实现差异
2. ConcurrentHashMap 的分段锁机制
3. 红黑树的自平衡算法
4. 位运算在哈希计算中的应用

---

## ✅ 总结

| 操作   | 时机         | 触发条件                  | 执行顺序       |
|--------|--------------|---------------------------|----------------|
| 扩容   | 插入新元素前 | `size >= threshold`       | **先扩容，再插入** |
| 树化   | 插入链表节点后 | `binCount >= 8 && capacity >= 64` | **先插入，再转树** |

**记忆口诀：**
- 扩容看大小（size vs threshold），插前先扩
- 树化看长短（链表长度 + 数组容量），插后再转
