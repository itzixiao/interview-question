# 算法与数据结构知识点详解

## 📚 文档列表

#### 1. [01-HashMap源码分析.md](./01-HashMap%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md)
- **内容：** HashMap 底层结构、扩容机制、哈希冲突解决
- **面试题：** 12+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 2. [02-树形数据结构详解.md](./02-%E6%A0%91%E5%BD%A2%E6%95%B0%E6%8D%AE%E7%BB%93%E6%9E%84%E8%AF%A6%E8%A7%A3.md)
- **内容：** 二叉树、AVL 树、红黑树、B 树、B+ 树对比
- **面试题：** 15+ 道
- **重要程度：** ⭐⭐⭐⭐⭐

#### 3. [03-算法面试题详解.md](./03-%E7%AE%97%E6%B3%95%E9%9D%A2%E8%AF%95%E9%A2%98%E8%AF%A6%E8%A7%A3.md)
- **内容：** 排序、查找、链表、动态规划等常见算法
- **面试题：** 20+ 道
- **重要程度：** ⭐⭐⭐⭐

---

## 📊 统计信息

- **文档数：** 3 个
- **面试题总数：** 47+ 道
- **代码示例：** 配套 Java 代码在 `interview-service/hashmap/`、`interview-service/tree/`、`interview-algorithm/` 目录（~3,000 行代码）

---

## 🎯 学习建议

### HashMap（2 天）
1. **底层结构**
   - JDK7 vs JDK8 差异
   - 数组 + 链表 + 红黑树

2. **核心机制**
   - hash 计算与扰动函数
   - 扩容因子与阈值
   - 树化条件

### 树形结构（3 天）
1. **二叉搜索树**
   - 插入、删除、查找

2. **平衡树**
   - AVL 树旋转操作
   - 红黑树性质与插入

3. **多路查找树**
   - B 树（数据库索引）
   - B+ 树（MySQL 索引）

### 常见算法（3-4 天）
1. **排序算法**
   - 快速排序、归并排序、堆排序

2. **查找算法**
   - 二分查找、DFS、BFS

3. **动态规划**
   - 背包问题、最长子序列

---

## 🔗 跨模块关联

### 前置知识
- ✅ **[Java基础](../01-Java基础/README.md)** - 集合框架、泛型
- ✅ **[Java并发编程](../02-Java并发编程/README.md)** - ConcurrentHashMap

### 后续进阶
- 📚 **[MySQL](../07-MySQL 数据库/README.md)** - B+ 树索引实现
- 📚 **[Redis](../08-Redis 缓存/README.md)** - ZSet 跳表实现

### 知识点对应
| 算法与数据结构 | 应用场景 |
|---------------|---------|
| HashMap | 缓存、Map 映射 |
| 红黑树 | TreeMap、HashMap 桶 |
| B+ 树 | MySQL 索引 |
| 快速排序 | Collections.sort |
| 动态规划 | 最优解问题 |

---

## 💡 高频面试题 Top 15

1. **HashMap 的底层数据结构？**
2. **HashMap 如何解决哈希冲突？**
3. **HashMap 的扩容机制？**
4. **HashMap 为什么线程不安全？**
5. **ConcurrentHashMap 如何保证线程安全？**
6. **红黑树的特性？与 AVL 树的区别？**
7. **B 树和 B+ 树的区别？**
8. **为什么 MySQL 选择 B+ 树作为索引？**
9. **快速排序的时间复杂度？**
10. **如何判断链表是否有环？**
11. **反转二叉树的实现？**
12. **动态规划的解题步骤？**
13. **Top K 问题的解决方案？**
14. **LRU 缓存的实现？**
15. **布隆过滤器的原理？**

---

## 🛠️ 实战技巧

### HashMap 遍历方式
```java
// 推荐：entrySet 遍历
for (Map.Entry<K, V> entry : map.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}

// 或使用 forEach
map.forEach((k, v) -> System.out.println(k + "=" + v));
```

### LRU 缓存实现
```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    
    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);
        this.capacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > capacity;
    }
}
```

---

## 📖 推荐学习顺序

```
HashMap 源码
   ↓
哈希冲突解决
   ↓
二叉树基础
   ↓
红黑树详解
   ↓
B+ 树原理
   ↓
排序算法
   ↓
动态规划
   ↓
刷题实战
```

---

## 📈 更新日志

### v2.0 - 2026-03-08
- ✅ 新增跨模块关联章节
- ✅ 补充 47+ 道高频面试题
- ✅ 添加学习建议和实战技巧
- ✅ 完善推荐学习顺序

### v1.0 - 早期版本
- ✅ 基础算法文档

---

**维护者：** itzixiao  
**最后更新：** 2026-03-08  
**问题反馈：** 欢迎提 Issue 或 PR
