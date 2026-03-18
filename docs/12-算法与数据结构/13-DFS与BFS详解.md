# DFS与BFS详解

> **文档信息**
> - **难度等级**：⭐⭐⭐⭐（中高级）
> - **学习时长**：建议1-1.5天
> - **面试频率**：⭐⭐⭐⭐⭐（极高）
> - **配套代码**：`interview-algorithm/graph/DFSAndBFS.java`（673行，12种图遍历应用）
> - **关联文档**：[07-Dijkstra最短路径算法详解](./07-Dijkstra%E6%9C%80%E7%9F%AD%E8%B7%AF%E5%BE%84%E7%AE%97%E6%B3%95%E8%AF%A6%E8%A7%A3.md)、[03-算法面试题完整合集](./03-%E7%AE%97%E6%B3%95%E9%9D%A2%E8%AF%95%E9%A2%98%E5%AE%8C%E6%95%B4%E5%90%88%E9%9B%86.md)

## 一、算法概述

### 1.1 图遍历基础

图遍历是图算法的基础，DFS和BFS是最核心的两种遍历方式。

```
遍历方式对比：

DFS（深度优先）：
    A
   / \
  B   C
 / \   \
D   E   F

遍历顺序：A → B → D → E → C → F
特点：一条路走到底，回溯后再走其他路

BFS（广度优先）：
    A
   / \
  B   C
 / \   \
D   E   F

遍历顺序：A → B → C → D → E → F
特点：层层推进，先访问所有邻居
```

| 算法 | 全称 | 策略 | 数据结构 | 典型应用 |
|------|------|------|----------|----------|
| **DFS** | Depth First Search | 尽可能深地探索 | 栈（递归/迭代） | 连通分量、拓扑排序、全排列 |
| **BFS** | Breadth First Search | 先访问所有邻接节点 | 队列 | 最短路径、层次遍历、最小步数 |

### 1.2 时间复杂度

| 指标 | 复杂度 | 说明 |
|------|--------|------|
| **时间复杂度** | O(V + E) | 每个顶点和边访问一次 |
| **空间复杂度** | O(V) |  visited数组 + 栈/队列 |

**注意**：V是顶点数，E是边数。对于连通图，E ≥ V-1。

## 二、DFS深度优先搜索

### 2.1 递归实现

```java
public static List<Integer> dfsRecursive(GraphNode node) {
    List<Integer> result = new ArrayList<>();
    Set<GraphNode> visited = new HashSet<>();
    dfsHelper(node, visited, result);
    return result;
}

private static void dfsHelper(GraphNode node, Set<GraphNode> visited, 
                               List<Integer> result) {
    if (node == null || visited.contains(node)) return;
    
    visited.add(node);
    result.add(node.val);
    
    for (GraphNode neighbor : node.neighbors) {
        dfsHelper(neighbor, visited, result);
    }
}
```

### 2.2 迭代实现（使用栈）

```java
public static List<Integer> dfsIterative(GraphNode node) {
    List<Integer> result = new ArrayList<>();
    if (node == null) return result;
    
    Set<GraphNode> visited = new HashSet<>();
    Stack<GraphNode> stack = new Stack<>();
    stack.push(node);
    
    while (!stack.isEmpty()) {
        GraphNode current = stack.pop();
        
        if (visited.contains(current)) continue;
        
        visited.add(current);
        result.add(current.val);
        
        // 逆序压栈保证遍历顺序
        for (int i = current.neighbors.size() - 1; i >= 0; i--) {
            GraphNode neighbor = current.neighbors.get(i);
            if (!visited.contains(neighbor)) {
                stack.push(neighbor);
            }
        }
    }
    
    return result;
}
```

## 三、BFS广度优先搜索

### 3.1 标准BFS

```java
public static List<Integer> bfs(GraphNode node) {
    List<Integer> result = new ArrayList<>();
    if (node == null) return result;
    
    Set<GraphNode> visited = new HashSet<>();
    Queue<GraphNode> queue = new LinkedList<>();
    
    queue.offer(node);
    visited.add(node);
    
    while (!queue.isEmpty()) {
        GraphNode current = queue.poll();
        result.add(current.val);
        
        for (GraphNode neighbor : current.neighbors) {
            if (!visited.contains(neighbor)) {
                visited.add(neighbor);
                queue.offer(neighbor);
            }
        }
    }
    
    return result;
}
```

### 3.2 分层BFS

```java
public static List<List<Integer>> bfsLevelOrder(GraphNode node) {
    List<List<Integer>> result = new ArrayList<>();
    if (node == null) return result;
    
    Set<GraphNode> visited = new HashSet<>();
    Queue<GraphNode> queue = new LinkedList<>();
    queue.offer(node);
    visited.add(node);
    
    while (!queue.isEmpty()) {
        int levelSize = queue.size();
        List<Integer> level = new ArrayList<>();
        
        for (int i = 0; i < levelSize; i++) {
            GraphNode current = queue.poll();
            level.add(current.val);
            
            for (GraphNode neighbor : current.neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.offer(neighbor);
                }
            }
        }
        
        result.add(level);
    }
    
    return result;
}
```

## 四、经典应用

### 4.1 最短路径（BFS，无权图）

```java
public static List<Integer> shortestPath(List<Integer>[] adj, int start, int end, int n) {
    boolean[] visited = new boolean[n];
    int[] parent = new int[n];
    Arrays.fill(parent, -1);
    
    Queue<Integer> queue = new LinkedList<>();
    queue.offer(start);
    visited[start] = true;
    
    while (!queue.isEmpty()) {
        int v = queue.poll();
        
        if (v == end) {
            // 重建路径
            List<Integer> path = new ArrayList<>();
            for (int at = end; at != -1; at = parent[at]) {
                path.add(at);
            }
            Collections.reverse(path);
            return path;
        }
        
        for (int neighbor : adj[v]) {
            if (!visited[neighbor]) {
                visited[neighbor] = true;
                parent[neighbor] = v;
                queue.offer(neighbor);
            }
        }
    }
    
    return new ArrayList<>(); // 无路径
}
```

### 4.2 连通分量（DFS）

```java
public static List<List<Integer>> findConnectedComponents(List<Integer>[] adj, int n) {
    List<List<Integer>> components = new ArrayList<>();
    boolean[] visited = new boolean[n];
    
    for (int i = 0; i < n; i++) {
        if (!visited[i]) {
            List<Integer> component = new ArrayList<>();
            dfsComponent(adj, i, visited, component);
            components.add(component);
        }
    }
    
    return components;
}

private static void dfsComponent(List<Integer>[] adj, int v, 
                                  boolean[] visited, List<Integer> component) {
    visited[v] = true;
    component.add(v);
    
    for (int neighbor : adj[v]) {
        if (!visited[neighbor]) {
            dfsComponent(adj, neighbor, visited, component);
        }
    }
}
```

### 4.3 拓扑排序（DFS）

```java
public static List<Integer> topologicalSortDFS(List<Integer>[] adj, int n) {
    int[] state = new int[n]; // 0=未访问, 1=访问中, 2=已访问
    Stack<Integer> stack = new Stack<>();
    
    for (int i = 0; i < n; i++) {
        if (state[i] == 0) {
            if (!dfsTopo(adj, i, state, stack)) {
                return new ArrayList<>(); // 存在环
            }
        }
    }
    
    List<Integer> result = new ArrayList<>();
    while (!stack.isEmpty()) {
        result.add(stack.pop());
    }
    return result;
}

private static boolean dfsTopo(List<Integer>[] adj, int v, 
                                int[] state, Stack<Integer> stack) {
    state[v] = 1; // 标记为访问中
    
    for (int neighbor : adj[v]) {
        if (state[neighbor] == 1) return false; // 发现回边，存在环
        if (state[neighbor] == 0) {
            if (!dfsTopo(adj, neighbor, state, stack)) return false;
        }
    }
    
    state[v] = 2; // 标记为已访问
    stack.push(v);
    return true;
}
```

### 4.4 二分图判断（BFS）

```java
public static boolean isBipartite(List<Integer>[] adj, int n) {
    int[] color = new int[n]; // 0=未着色, 1=颜色1, -1=颜色2
    
    for (int i = 0; i < n; i++) {
        if (color[i] != 0) continue;
        
        Queue<Integer> queue = new LinkedList<>();
        queue.offer(i);
        color[i] = 1;
        
        while (!queue.isEmpty()) {
            int v = queue.poll();
            
            for (int neighbor : adj[v]) {
                if (color[neighbor] == 0) {
                    color[neighbor] = -color[v];
                    queue.offer(neighbor);
                } else if (color[neighbor] == color[v]) {
                    return false; // 相邻节点同色
                }
            }
        }
    }
    
    return true;
}
```

## 五、DFS vs BFS对比

| 特性 | DFS | BFS |
|------|-----|-----|
| 数据结构 | 栈 | 队列 |
| 空间复杂度 | O(h)，h为深度 | O(w)，w为宽度 |
| 最短路径 | 不保证 | 保证（无权图） |
| 适用场景 | 路径搜索、连通性 | 最短路径、层次遍历 |
| 实现难度 | 递归简单 | 迭代简单 |

## 六、面试高频题详解

### 题1：DFS和BFS的区别？⭐⭐⭐⭐⭐

| 特性 | DFS | BFS |
|------|-----|-----|
| **核心思想** | 一条路走到底，回溯 | 层层推进，先宽后深 |
| **数据结构** | 栈（递归/迭代） | 队列 |
| **空间复杂度** | O(h)，h为树深度 | O(w)，w为树宽度 |
| **最短路径** | 不保证 | 保证（无权图） |
| **适用场景** | 连通性、路径搜索、全排列 | 最短路径、层次遍历、最小步数 |
| **代码风格** | 递归简洁，迭代复杂 | 迭代简洁 |

### 题2：什么时候用DFS，什么时候用BFS？⭐⭐⭐⭐⭐

**选择DFS的场景**：
- ✅ 找所有解/路径（如全排列、子集）
- ✅ 连通性问题（如岛屿数量、连通分量）
- ✅ 路径存在性（如迷宫是否有通路）
- ✅ 拓扑排序
- ✅ 游戏树遍历（如象棋AI）

**选择BFS的场景**：
- ✅ 最短路径（无权图）
- ✅ 最小步数问题（如单词接龙）
- ✅ 层次遍历（如二叉树层序遍历）
- ✅ 最近公共祖先
- ✅ 社交网络中的最短关系链

**决策树**：
```
需要遍历图/树？
├── 需要最短路径？
│   ├── 是 → BFS
│   └── 否 → DFS
├── 需要所有解？
│   └── 是 → DFS（回溯）
└── 需要层次信息？
    └── 是 → BFS
```

### 题3：DFS递归和迭代哪个更好？⭐⭐⭐⭐

| 对比维度 | 递归DFS | 迭代DFS |
|----------|---------|---------|
| **代码复杂度** | 简单 | 较复杂 |
| **空间占用** | 系统栈，可能溢出 | 自定义栈，可控 |
| **适用规模** | 小规模（深度<10^4） | 大规模 |
| **调试难度** | 难（栈帧嵌套） | 相对容易 |

**建议**：
- 面试/算法题：递归（代码简洁）
- 生产环境：迭代（稳定性好）

### 题4：如何判断图中是否有环？⭐⭐⭐⭐⭐

**方法1：DFS检测回边**
```
三色标记法：
- 白色：未访问
- 灰色：正在访问（在当前递归栈中）
- 黑色：已访问完成

如果访问到灰色节点，说明存在环
```

**方法2：拓扑排序（Kahn算法）**
```
1. 计算所有节点的入度
2. 将入度为0的节点入队
3. 依次出队，删除出队节点的出边
4. 如果最终访问的节点数 < 总节点数，则有环
```

**适用场景**：
- DFS：有向图、无向图都适用
- Kahn算法：仅适用于有向图

### 题5：BFS为什么能找到最短路径？⭐⭐⭐⭐

**关键性质**：
```
BFS按层次遍历，先访问的节点距离起点更近

证明：
- 第k层节点距离起点为k
- 第k+1层节点只能通过第k层节点到达
- 因此第一次访问目标节点时，距离最短
```

**限制**：仅适用于无权图（所有边权重相同）

**有权图最短路径**：使用Dijkstra算法

## 七、知识图谱

```
图遍历算法体系
│
├── DFS深度优先搜索
│   ├── 递归实现
│   ├── 迭代实现（栈）
│   └── 应用场景
│       ├── 连通分量
│       ├── 路径搜索
│       ├── 拓扑排序
│       └── 全排列/组合
│
├── BFS广度优先搜索
│   ├── 标准BFS
│   ├── 分层BFS
│   └── 应用场景
│       ├── 最短路径（无权图）
│       ├── 层次遍历
│       ├── 最小步数
│       └── 最近公共祖先
│
├── 算法选择
│   ├── 最短路径 → BFS
│   ├── 所有路径 → DFS
│   ├── 连通性 → DFS/BFS
│   └── 拓扑排序 → DFS/Kahn
│
└── 面试重点
    ├── DFS vs BFS对比
    ├── 递归vs迭代选择
    ├── 环检测方法
    └── 最短路径证明
```

## 八、示例代码

完整示例代码位于：`interview-microservices-parent/interview-algorithm/src/main/java/cn/itzixiao/interview/algorithm/graph/DFSAndBFS.java`

运行方式：
```bash
mvn exec:java -pl interview-microservices-parent/interview-algorithm \
  -Dexec.mainClass="cn.itzixiao.interview.algorithm.graph.DFSAndBFS" -q
```
