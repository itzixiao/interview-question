# KMP字符串匹配算法详解

> **文档信息**
> - **难度等级**：⭐⭐⭐⭐⭐（高级）
> - **学习时长**：建议1.5-2天
> - **面试频率**：⭐⭐⭐⭐（高）
> - **配套代码**：`interview-algorithm/string/KMPAlgorithm.java`（419行，10种字符串匹配功能）
> - **关联文档**：[03-算法面试题完整合集](./03-%E7%AE%97%E6%B3%95%E9%9D%A2%E8%AF%95%E9%A2%98%E5%AE%8C%E6%95%B4%E5%90%88%E9%9B%86.md)

## 一、算法概述

### 1.1 什么是KMP算法

KMP算法（Knuth-Morris-Pratt）由Donald Knuth、Vaughan Pratt和James Morris于1977年联合发表，是**字符串匹配算法的经典之作**。

**核心思想**：利用已匹配的信息，避免主串指针回溯，将时间复杂度从**O(m×n)**优化到**O(m+n)**。

```
算法对比：
┌─────────────────────────────────────────────────────┐
│ 暴力匹配：                                            │
│ 主串：  ABABDABACDABABCABAB                           │
│ 模式：    ABABC                                       │
│ 失配时：主串指针回溯，模式串指针归零                      │
│ 时间：O(m × n)                                       │
├─────────────────────────────────────────────────────┤
│ KMP算法：                                             │
│ 主串：  ABABDABACDABABCABAB                           │
│ 模式：    ABABC                                       │
│ 失配时：主串指针不回溯，模式串根据next数组跳转            │
│ 时间：O(m + n)                                       │
└─────────────────────────────────────────────────────┘
```

### 1.2 核心概念详解

| 概念 | 定义 | 示例（模式串"ABABC"） |
|------|------|----------------------|
| **前缀（Prefix）** | 字符串开头到某个位置的所有子串（不含本身） | "A"的前缀：空；"AB"的前缀："A" |
| **后缀（Suffix）** | 字符串某个位置到结尾的所有子串（不含本身） | "AB"的后缀："B"；"ABA"的后缀："A", "BA" |
| **最长相等前后缀（LPS）** | 字符串的最长相等前缀和后缀长度 | "ABAB"的LPS=2（"AB"） |
| **next数组** | 记录模式串每个位置的LPS长度 | next = [0, 0, 1, 2, 0] |

**LPS计算示例**：
```
模式串：A B A B C
位置：  0 1 2 3 4

位置0 "A":
  前缀：空
  后缀：空
  LPS = 0

位置1 "AB":
  前缀："A"
  后缀："B"
  无相等前后缀，LPS = 0

位置2 "ABA":
  前缀："A", "AB"
  后缀："A", "BA"
  相等："A"，LPS = 1

位置3 "ABAB":
  前缀："A", "AB", "ABA"
  后缀："B", "AB", "BAB"
  相等："AB"，LPS = 2

位置4 "ABABC":
  前缀："A", "AB", "ABA", "ABAB"
  后缀："C", "BC", "ABC", "BABC"
  无相等前后缀，LPS = 0

next数组：[0, 0, 1, 2, 0]
```

## 二、next数组构建

### 2.1 构建原理

```
模式串: ABABC

"A":      前后缀为空      → next[0] = 0
"AB":     无相等前后缀    → next[1] = 0
"ABA":    前缀"A"=后缀"A" → next[2] = 1
"ABAB":   前缀"AB"=后缀"AB" → next[3] = 2
"ABABC":  无相等前后缀    → next[4] = 0

结果: next = [0, 0, 1, 2, 0]
```

### 2.2 代码实现

```java
public static int[] buildNext(String pattern) {
    int m = pattern.length();
    int[] next = new int[m];
    next[0] = 0;
    
    int j = 0;  // 当前最长相等前后缀长度
    for (int i = 1; i < m; i++) {
        // 失配时回退
        while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
            j = next[j - 1];
        }
        
        // 匹配成功
        if (pattern.charAt(i) == pattern.charAt(j)) {
            j++;
        }
        
        next[i] = j;
    }
    
    return next;
}
```

## 三、KMP匹配过程

### 3.1 标准KMP匹配

```java
public static int kmpSearch(String text, String pattern) {
    if (pattern.isEmpty()) return 0;
    
    int[] next = buildNext(pattern);
    int i = 0;  // 主串指针
    int j = 0;  // 模式串指针
    
    while (i < text.length()) {
        if (text.charAt(i) == pattern.charAt(j)) {
            i++;
            j++;
            
            if (j == pattern.length()) {
                return i - j;  // 匹配成功
            }
        } else {
            if (j > 0) {
                j = next[j - 1];  // 根据next数组回退
            } else {
                i++;  // j=0且失配，主串前进
            }
        }
    }
    
    return -1;  // 未找到
}
```

### 3.2 匹配过程图解

```
主串:  ABABDABACDABABCABAB
模式:  ABABC
next:  [0,0,1,2,0]

步骤1: ABABD...
       ABABC  (D≠C，j=4回退到next[3]=2)
       
步骤2: ABABD...
         ABC   (D≠A，j=2回退到next[1]=0)
         
步骤3: ABABDABACDABABC...
              ABABC  (匹配成功！)
```

## 四、高级应用

### 4.1 查找所有匹配（允许重叠）

```java
public static List<Integer> kmpSearchAll(String text, String pattern) {
    List<Integer> results = new ArrayList<>();
    int[] next = buildNext(pattern);
    int i = 0, j = 0;
    
    while (i < text.length()) {
        if (text.charAt(i) == pattern.charAt(j)) {
            i++;
            j++;
            
            if (j == pattern.length()) {
                results.add(i - j);
                j = next[j - 1];  // 继续寻找下一个（允许重叠）
            }
        } else {
            if (j > 0) j = next[j - 1];
            else i++;
        }
    }
    
    return results;
}
```

**示例**：
```
主串: "AAAAA"
模式: "AA"
结果: [0, 1, 2, 3] （允许重叠）
```

### 4.2 查找所有匹配（不重叠）

```java
public static List<Integer> kmpSearchAllNonOverlapping(String text, String pattern) {
    List<Integer> results = new ArrayList<>();
    int[] next = buildNext(pattern);
    int i = 0, j = 0;
    
    while (i < text.length()) {
        if (text.charAt(i) == pattern.charAt(j)) {
            i++;
            j++;
            
            if (j == pattern.length()) {
                results.add(i - j);
                j = 0;  // 重置，不重叠
            }
        } else {
            if (j > 0) j = next[j - 1];
            else i++;
        }
    }
    
    return results;
}
```

### 4.3 字符串替换

```java
public static String replaceAll(String text, String pattern, String replacement) {
    List<Integer> positions = kmpSearchAllNonOverlapping(text, pattern);
    if (positions.isEmpty()) return text;
    
    StringBuilder result = new StringBuilder();
    int lastEnd = 0;
    
    for (int pos : positions) {
        result.append(text, lastEnd, pos);
        result.append(replacement);
        lastEnd = pos + pattern.length();
    }
    result.append(text.substring(lastEnd));
    
    return result.toString();
}
```

## 五、KMP vs 其他字符串匹配算法

| 算法 | 预处理时间 | 匹配时间 | 空间 | 特点 |
|------|------------|----------|------|------|
| 暴力匹配 | O(1) | O(m×n) | O(1) | 简单，小数据可用 |
| KMP | O(m) | O(m+n) | O(m) | 线性时间，经典 |
| BM | O(m+σ) | O(m×n)最坏 | O(σ) | 实际很快，从右向左 |
| Sunday | O(m+σ) | O(m×n)最坏 | O(σ) | 简单高效 |

## 六、面试高频题详解

### 题1：KMP算法的时间复杂度？⭐⭐⭐⭐⭐

**详细分析**：
```
预处理阶段（构建next数组）：
- 双指针i和j遍历模式串
- i从1到m-1，j最多增加m次
- 虽然while循环，但j的总回退次数不超过m
- 时间：O(m)

匹配阶段：
- 主串指针i只增不减，遍历n个字符
- 模式串指针j根据next数组跳转
- j的总移动次数不超过2n
- 时间：O(n)

总时间：O(m + n)
空间：O(m)（next数组）
```

### 题2：next数组的作用是什么？⭐⭐⭐⭐⭐

**核心作用**：
1. **记录信息**：模式串每个位置的最长相等前后缀长度
2. **指导回退**：失配时告诉模式串应该回退到哪里

**为什么能避免主串回溯？**
```
主串：... A B A B D ...
模式：    A B A B C
              ↑
            失配位置j=4（字符C）

已匹配："ABAB"
next[3] = 2（"ABAB"的最长相等前后缀）

含义：
- 已匹配的"ABAB"中，前缀"AB" = 后缀"AB"
- 主串对应位置的"AB"已经匹配过
- 可以直接从模式串的"AB"后继续匹配

回退后：
主串：... A B A B D ...
模式：      A B A B C
            ↑
          j=2，从"AB"后继续
```

### 题3：为什么KMP比暴力匹配快？⭐⭐⭐⭐⭐

| 对比维度 | 暴力匹配 | KMP算法 |
|----------|----------|---------|
| **主串指针** | 失配时回溯 | 永不回溯 |
| **时间复杂度** | O(m×n) | O(m+n) |
| **最坏情况** | 每次都比较m次 | 线性扫描 |
| **空间复杂度** | O(1) | O(m) |

**最坏情况示例**：
```
主串：  A A A A A A A A A B
模式：  A A A B

暴力匹配：
第1轮：A A A A 失配，i回到1
第2轮：A A A A 失配，i回到2
... 共需要 (n-m+1) × m 次比较

KMP匹配：
第1轮：A A A A 失配，i不动，j=next[2]=2
第2轮：A A A A 失配，i不动，j=next[2]=2
... 总共O(n)次比较
```

### 题4：如何理解最长相等前后缀？⭐⭐⭐⭐

**定义**：字符串的最长相等前缀和后缀（不含本身）的长度。

**示例分析**：
```
字符串："ABABAB"

所有前缀："A", "AB", "ABA", "ABAB", "ABABA"
所有后缀："B", "AB", "BAB", "ABAB", "BABAB"

相等的前后缀对：
- "AB" = "AB"（长度2）
- "ABAB" = "ABAB"（长度4）

最长相等前后缀："ABAB"，长度=4
```

**与next数组的关系**：
```
next[i] = 子串pattern[0..i]的最长相等前后缀长度
```

### 题5：KMP vs BM vs Sunday算法如何选择？⭐⭐⭐⭐

| 算法 | 预处理时间 | 匹配时间 | 特点 | 适用场景 |
|------|------------|----------|------|----------|
| **KMP** | O(m) | O(m+n) | 稳定线性，理论最优 | 通用场景 |
| **BM** | O(m+σ) | O(mn)最坏，实际很快 | 从右向左，跳跃大 | 大字符集 |
| **Sunday** | O(m+σ) | O(mn)最坏，实际很快 | 简单高效 | 实际应用 |

**实际选择**：
- 面试/考试：KMP（必会）
- 工程实践：Sunday（实现简单，实际快）
- 特定场景：BM（大字符集，如DNA序列）

## 七、知识图谱

```
字符串匹配算法体系
│
├── 单模式匹配
│   ├── 暴力匹配 O(mn)
│   │
│   ├── KMP算法 O(m+n)
│   │   ├── next数组构建 O(m)
│   │   ├── 匹配过程 O(n)
│   │   └── 优化版本（nextval数组）
│   │
│   ├── BM算法
│   │   ├── 坏字符规则
│   │   └── 好后缀规则
│   │
│   └── Sunday算法
│       └── 偏移表
│
├── 多模式匹配
│   └── AC自动机
│
├── KMP扩展应用
│   ├── 查找所有匹配（重叠/非重叠）
│   ├── 字符串替换
│   ├── 统计出现次数
│   └── 多模式匹配
│
└── 面试重点
    ├── next数组构建过程
    ├── 时间复杂度证明
    ├── 与暴力匹配的对比
    └── 实际应用场景
```

## 八、示例代码

完整示例代码位于：`interview-microservices-parent/interview-algorithm/src/main/java/cn/itzixiao/interview/algorithm/string/KMPAlgorithm.java`

运行方式：
```bash
mvn exec:java -pl interview-microservices-parent/interview-algorithm \
  -Dexec.mainClass="cn.itzixiao.interview.algorithm.string.KMPAlgorithm" -q
```
