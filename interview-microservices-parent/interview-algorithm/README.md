# 🧮 算法面试题库

本模块提供常见算法面试题的完整实现和详细注释，帮助理解算法原理和应对技术面试。

## 📁 模块结构

```
interview-algorithm/
├── src/main/java/cn/itzixiao/interview/
│   └── algorithm/                 # 核心算法类
│       ├── AlgorithmApplication.java    # 主入口
│       ├── dp/                        # 动态规划
│       │   └── DynamicProgrammingDemo.java
│       ├── linkedlist/                # 链表操作
│       │   └── LinkedListDemo.java
│       ├── search/                    # 查找算法
│       │   └── BinarySearchDemo.java
│       ├── sort/                      # 排序算法
│       │   └── QuickSortDemo.java
│       └── tree/                      # 树形算法
│           └── BinaryTreeTraversal.java
├── docs/                              # 算法文档
│   ├── 01-HashMap源码分析.md
│   ├── 02-树形数据结构详解.md
│   └── 03-算法面试题详解.md
└── pom.xml
```

## 🔧 已实现的算法

### 1. 排序算法

- ✅ **快速排序** - `QuickSortDemo`
    - 时间复杂度：O(n log n) 平均，O(n²) 最坏
    - 空间复杂度：O(log n)
    - 稳定性：不稳定
    - 面试考点：分区思想、递归实现、性能分析

### 2. 查找算法

- ✅ **二分查找** - `BinarySearchDemo`
    - 时间复杂度：O(log n)
    - 空间复杂度：O(1)（迭代版）/ O(log n)（递归版）
    - 前提条件：有序数组
    - 面试考点：边界处理、递归与迭代对比

### 3. 链表操作

- ✅ **链表高频操作** - `LinkedListDemo`
    - 链表反转（迭代 + 递归）
    - 合并两个有序链表
    - 检测环的存在
    - 找到环的入口
    - 删除倒数第 N 个节点
    - 找到中间节点
    - 面试考点：指针操作、虚拟头结点、快慢指针

### 4. 树形算法

- ✅ **二叉树遍历** - `BinaryTreeTraversal`
    - 前序遍历（递归 + 迭代）
    - 中序遍历（递归 + 迭代）
    - 后序遍历（递归 + 迭代）
    - 层序遍历（BFS）
    - 最大深度、最小深度计算
    - 验证 BST、最近公共祖先等

- ✅ **树形结构演示** - `TreeStructureDemo`
    - 树的创建与基本操作
    - 高度平衡检查
    - 对称性判断
    - 路径总和问题

### 5. 动态规划

- ✅ **经典 DP 问题** - `DynamicProgrammingDemo`
    - 斐波那契数列（记忆化搜索）
    - 爬楼梯问题
    - 最长递增子序列
    - 0-1 背包问题
    - 面试考点：状态定义、状态转移方程、初始值设定

## 🚀 运行示例

### 方式一：运行主入口类

```bash
cd interview-microservices-parent/interview-algorithm
mvn clean compile
java -cp target/classes cn.itzixiao.interview.algorithm.AlgorithmApplication
```

### 方式二：运行特定算法

```bash
# 快速排序示例
java -cp target/classes cn.itzixiao.interview.algorithm.sort.QuickSortDemo

# 二分查找示例
java -cp target/classes cn.itzixiao.interview.algorithm.search.BinarySearchDemo

# 链表操作示例
java -cp target/classes cn.itzixiao.interview.algorithm.linkedList.LinkedListDemo

# 二叉树遍历示例
java -cp target/classes cn.itzixiao.interview.algorithm.tree.BinaryTreeTraversal

# 动态规划示例
java -cp target/classes cn.itzixiao.interview.algorithm.dp.DynamicProgrammingDemo
```

**输出示例：**

```
========================================
    算法面试题库 - 示例代码演示
========================================

【快速排序】
原始数组：[64, 34, 25, 12, 22, 11, 90]
排序结果：[11, 12, 22, 25, 34, 64, 90]

【二分查找】
在数组 [1, 3, 5, 7, 9, 11] 中查找 7
查找成功，索引位置：3

【链表反转】
原链表：1 -> 2 -> 3 -> 4 -> 5 -> null
反转后：5 -> 4 -> 3 -> 2 -> 1 -> null
```

## 📖 配套文档

本模块配合以下文档使用：

1. **[HashMap源码分析](./docs/01-HashMap源码分析.md)**
    - HashMap 底层数据结构
    - put/get 操作原理
    - 扩容机制详解

2. **[树形数据结构详解](./docs/02-树形数据结构详解.md)**
    - 二叉树基础概念
    - 红黑树特性与应用
    - B 树与 B+ 树区别
    - 树在数据库中的应用

3. **[算法面试题详解](./docs/03-算法面试题详解.md)**
    - 排序算法时间与空间复杂度对比
    - 常见链表操作题解
    - 动态规划经典问题
    - 回溯法解题模板

## 💡 学习建议

1. **理解原理**：先理解算法思想，再看代码实现
2. **手写代码**：在理解基础上尝试自己实现，不要直接 copy
3. **复杂度分析**：掌握时间和空间复杂度计算方法
4. **刷题练习**：LeetCode 相关题目巩固提升（每个算法至少刷 5 道相关题）
5. **对比学习**：对比不同算法的优缺点和适用场景
6. **分类突破**：按算法分类逐个击破，不要东一榔头西一棒子
7. **总结模板**：形成自己的解题模板和思路框架

## 🎯 面试考点

### 排序算法

- 快速排序 vs 归并排序 vs 堆排序
- 稳定排序 vs 不稳定排序
- 时间复杂度最优选择
- 实际应用场景（大数据量用哪种？）
- 手写快排代码（高频考题）

### 查找算法

- 二分查找的变形应用
- 旋转数组中的查找
- 查找第一个/最后一个满足条件的元素
- 时间复杂度为什么是 O(log n)

### 链表操作

- 指针操作的细节（空指针、边界条件）
- 虚拟头结点的使用场景
- 快慢指针的应用
- 递归 vs 迭代实现对比
- 常考题型：反转、合并、环检测、删除倒数第 N 个

### 树形算法

- 递归 vs 迭代实现
- DFS vs BFS 遍历
- 树的深度/高度计算
- BST 的性质与验证
- 最近公共祖先问题
- 路径总和问题
- 树的构建（前序 + 中序序列）

### 动态规划

- 动态规划三要素：最优子结构、边界条件、状态转移方程
- 自顶向下 vs 自底向上
- 记忆化搜索优化
- 常见 DP 问题分类：线性 DP、区间 DP、背包 DP、数位 DP
- 如何识别可以用 DP 解决的问题

### 复杂度分析

- O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2^n) < O(n!)
- 最好、最坏、平均时间复杂度
- 空间复杂度计算（包括递归栈空间）
- 时间复杂度与空间复杂度的权衡

## 📊 编译构建

```bash
# 编译整个项目
mvn clean install -DskipTests

# 只编译算法模块
mvn clean compile -pl interview-microservices-parent/interview-algorithm -am

# 运行测试（如果有）
mvn test -pl interview-microservices-parent/interview-algorithm
```

---

**🔗 相关资源：**

- [LeetCode 算法题库](https://leetcode-cn.com/)
- [VisuAlgo 算法可视化](https://visualgo.net/zh)
- [labuladong 的算法小抄](https://labuladong.gitee.io/algo/)
