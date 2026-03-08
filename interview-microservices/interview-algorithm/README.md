# 🧮 算法面试题库

本模块提供常见算法面试题的完整实现和详细注释，帮助理解算法原理和应对技术面试。

## 📁 模块结构

```
interview-algorithm/
├── src/main/java/cn/itzixiao/interview/
│   ├── algorithm/           # 核心算法类
│   │   └── AlgorithmApplication.java    # 主入口（快速排序示例）
│   └── tree/                # 树形算法（从 interview-service 复制）
│       └── TreeStructureDemo.java
├── docs/                    # 算法文档
│   ├── 01-HashMap源码分析.md
│   ├── 02-树形数据结构详解.md
│   └── 03-算法面试题详解.md
└── pom.xml
```

## 🔧 已实现的算法

### 1. 排序算法
- ✅ **快速排序** - `AlgorithmApplication.quickSort()`
  - 时间复杂度：O(n log n) 平均，O(n²) 最坏
  - 空间复杂度：O(log n)
  - 稳定性：不稳定

### 2. 树形算法
- ✅ **二叉树遍历** - `TreeStructureDemo`
  - 前序遍历（递归 + 迭代）
  - 中序遍历（递归 + 迭代）
  - 后序遍历（递归 + 迭代）
  - 层序遍历（BFS）
  - 最大深度、最小深度计算
  - 验证 BST、最近公共祖先等

## 🚀 运行示例

### 快速排序演示
```bash
cd interview-microservices/interview-algorithm
mvn clean compile
java -cp target/classes cn.itzixiao.interview.algorithm.AlgorithmApplication
```

**输出：**
```
========================================
    算法面试题库 - 示例代码演示
========================================

原始数组：[64, 34, 25, 12, 22, 11, 90]
快速排序后：[11, 12, 22, 25, 34, 64, 90]
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
2. **手写代码**：在理解基础上尝试自己实现
3. **复杂度分析**：掌握时间和空间复杂度计算方法
4. **刷题练习**：LeetCode 相关题目巩固提升
5. **对比学习**：对比不同算法的优缺点和适用场景

## 🎯 面试考点

### 排序算法
- 快速排序 vs 归并排序 vs 堆排序
- 稳定排序 vs 不稳定排序
- 时间复杂度最优选择
- 实际应用场景

### 树形算法
- 递归 vs 迭代实现
- DFS vs BFS 遍历
- 树的深度/高度计算
- BST 的性质与验证

### 复杂度分析
- O(1) < O(log n) < O(n) < O(n log n) < O(n²) < O(2^n)
- 最好、最坏、平均时间复杂度
- 空间复杂度计算

## 📊 编译构建

```bash
# 编译整个项目
mvn clean install -DskipTests

# 只编译算法模块
mvn clean compile -pl interview-microservices/interview-algorithm -am
```

---

**🔗 相关资源：**
- [LeetCode 算法题库](https://leetcode-cn.com/)
- [VisuAlgo 算法可视化](https://visualgo.net/zh)
- [labuladong 的算法小抄](https://labuladong.gitee.io/algo/)
