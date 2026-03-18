# Dijkstra最短路径算法详解

> **文档信息**
> - **难度等级**：⭐⭐⭐⭐（中高级）
> - **学习时长**：建议1-1.5天
> - **面试频率**：⭐⭐⭐⭐（高）
> - **配套代码**：`interview-algorithm/graph/DijkstraAlgorithm.java`（446行，6种实现变体）
> - **关联文档**：[13-DFS与BFS详解](./13-DFS%E4%B8%8EBFS%E8%AF%A6%E8%A7%A3.md)、[03-算法面试题完整合集](./03-%E7%AE%97%E6%B3%95%E9%9D%A2%E8%AF%95%E9%A2%98%E5%AE%8C%E6%95%B4%E5%90%88%E9%9B%86.md)

## 一、算法概述

### 1.1 什么是Dijkstra算法

Dijkstra算法由荷兰计算机科学家Edsger W. Dijkstra于1956年提出，用于在带权有向图中求解**单源最短路径问题**（Single Source Shortest Path）。

**核心思想**：贪心算法 + 动态规划
```
算法流程：
┌─────────────────────────────────────────────┐
│ 1. 初始化：dist[start] = 0, 其他dist = ∞      │
├─────────────────────────────────────────────┤
│ 2. 选择：从未访问顶点中选择dist最小的顶点u      │
├─────────────────────────────────────────────┤
│ 3. 更新：对u的所有邻接点v，尝试松弛操作        │
│          if dist[u] + w(u,v) < dist[v]       │
│             dist[v] = dist[u] + w(u,v)       │
├─────────────────────────────────────────────┤
│ 4. 标记：将u标记为已访问                      │
├─────────────────────────────────────────────┤
│ 5. 重复：直到所有顶点都被访问                  │
└─────────────────────────────────────────────┘
```

### 1.2 算法正确性证明（贪心选择性质）

**关键定理**：当顶点u被标记为已访问时，dist[u]已经是源点到u的最短距离。

**证明**（反证法）：
```
假设存在更短的路径：s → ... → x → ... → u，其中x是路径上第一个未访问顶点

由于边权非负：
dist[x] ≤ dist[x] + w(x...u) < dist[u]

但u是被选中的最小dist顶点，应该有dist[u] ≤ dist[x]

矛盾！因此假设不成立，dist[u]已是最短距离。
```

### 1.3 适用条件与限制

| 条件 | 要求 | 违反后果 |
|------|------|----------|
| **边权非负** | 所有边权重 ≥ 0 | 算法失效，可能得到错误结果 |
| **有向/无向图** | 均可 | - |
| **连通性** | 不要求全连通 | 不可达顶点dist保持∞ |

**负权边问题**：
```
反例：
    A --(-5)--> B --> C
    |            ↑
    └----(2)-----┘
    
Dijkstra从A出发：
- 先访问B，dist[B] = -5
- 再访问C，dist[C] = -5 + w(B,C)

但实际最短路径可能是A→C→B！
```

### 1.4 时间复杂度演进

| 实现方式 | 时间复杂度 | 适用场景 | 核心数据结构 |
|----------|------------|----------|--------------|
| **朴素实现** | O(V²) | 稠密图（E ≈ V²） | 数组 |
| **优先队列优化** | O((V+E)logV) | 稀疏图（E << V²） | 二叉堆 |
| **斐波那契堆优化** | O(VlogV + E) | 理论最优 | 斐波那契堆 |

**复杂度分析**：
```
朴素实现：
- 外层循环V次（每次选一个顶点）
- 内层找最小dist：O(V)
- 更新邻接点：O(V)
- 总：V × (V + V) = O(V²)

优先队列优化：
- 每个顶点入队一次，出队一次：O(VlogV)
- 每条边可能触发一次更新（入队）：O(ElogV)
- 总：O((V+E)logV)
```

## 二、基础实现

### 2.1 朴素Dijkstra（邻接矩阵）

```java
public static int[] dijkstraMatrix(int[][] graph, int start) {
    int n = graph.length;
    int[] dist = new int[n];
    boolean[] visited = new boolean[n];
    
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[start] = 0;
    
    for (int i = 0; i < n; i++) {
        // 找到未访问顶点中距离最小的
        int u = -1;
        int minDist = Integer.MAX_VALUE;
        for (int j = 0; j < n; j++) {
            if (!visited[j] && dist[j] < minDist) {
                minDist = dist[j];
                u = j;
            }
        }
        
        if (u == -1) break;
        visited[u] = true;
        
        // 更新u的所有邻接点
        for (int v = 0; v < n; v++) {
            if (!visited[v] && graph[u][v] != Integer.MAX_VALUE) {
                int newDist = dist[u] + graph[u][v];
                if (newDist < dist[v]) {
                    dist[v] = newDist;
                }
            }
        }
    }
    
    return dist;
}
```

### 2.2 堆优化Dijkstra（邻接表+优先队列）

```java
public static int[] dijkstraHeap(List<Edge>[] adj, int start, int n) {
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[start] = 0;
    
    PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.dist));
    pq.offer(new Node(start, 0));
    
    while (!pq.isEmpty()) {
        Node node = pq.poll();
        int u = node.vertex;
        int d = node.dist;
        
        if (d > dist[u]) continue; // 已处理过
        
        for (Edge edge : adj[u]) {
            int v = edge.to;
            int newDist = dist[u] + edge.weight;
            
            if (newDist < dist[v]) {
                dist[v] = newDist;
                pq.offer(new Node(v, dist[v]));
            }
        }
    }
    
    return dist;
}
```

## 三、高级应用

### 3.1 带路径记录的Dijkstra

```java
public static PathResult dijkstraWithPath(List<Edge>[] adj, int start, int n) {
    int[] dist = new int[n];
    int[] prev = new int[n];  // 前驱节点
    Arrays.fill(dist, Integer.MAX_VALUE);
    Arrays.fill(prev, -1);
    dist[start] = 0;
    
    PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a.dist));
    pq.offer(new Node(start, 0));
    
    while (!pq.isEmpty()) {
        Node node = pq.poll();
        int u = node.vertex;
        
        if (node.dist > dist[u]) continue;
        
        for (Edge edge : adj[u]) {
            int v = edge.to;
            int newDist = dist[u] + edge.weight;
            
            if (newDist < dist[v]) {
                dist[v] = newDist;
                prev[v] = u;  // 记录前驱
                pq.offer(new Node(v, dist[v]));
            }
        }
    }
    
    return new PathResult(dist, prev);
}

// 重建路径
public List<Integer> getPath(int start, int target) {
    List<Integer> path = new ArrayList<>();
    for (int at = target; at != -1; at = prev[at]) {
        path.add(at);
    }
    Collections.reverse(path);
    return path;
}
```

### 3.2 次短路径算法

```java
public static int[] secondShortestPath(List<Edge>[] adj, int start, int n) {
    int[] dist1 = new int[n];  // 最短距离
    int[] dist2 = new int[n];  // 次短距离
    Arrays.fill(dist1, Integer.MAX_VALUE);
    Arrays.fill(dist2, Integer.MAX_VALUE);
    dist1[start] = 0;
    
    // [顶点, 距离, 类型(1=最短,2=次短)]
    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
    pq.offer(new int[]{start, 0, 1});
    
    while (!pq.isEmpty()) {
        int[] curr = pq.poll();
        int u = curr[0];
        int d = curr[1];
        int type = curr[2];
        
        if ((type == 1 && d > dist1[u]) || (type == 2 && d > dist2[u])) {
            continue;
        }
        
        for (Edge edge : adj[u]) {
            int v = edge.to;
            int newDist = d + edge.weight;
            
            // 更新最短距离
            if (newDist < dist1[v]) {
                dist2[v] = dist1[v];
                dist1[v] = newDist;
                pq.offer(new int[]{v, dist1[v], 1});
                pq.offer(new int[]{v, dist2[v], 2});
            }
            // 更新次短距离（不能等于最短距离）
            else if (newDist > dist1[v] && newDist < dist2[v]) {
                dist2[v] = newDist;
                pq.offer(new int[]{v, dist2[v], 2});
            }
        }
    }
    
    return dist2;
}
```

## 四、Dijkstra vs 其他最短路径算法

| 算法 | 适用场景 | 时间复杂度 | 特点 |
|------|----------|------------|------|
| Dijkstra | 单源、非负权 | O((V+E)logV) | 贪心，不能处理负权 |
| Bellman-Ford | 单源、可有负权 | O(VE) | 可检测负环 |
| SPFA | 单源、稀疏图 | O(kE)平均 | Bellman-Ford优化 |
| Floyd | 多源 | O(V³) | 动态规划 |

## 五、面试高频题详解

### 题1：Dijkstra算法为什么不能处理负权边？⭐⭐⭐⭐⭐

**核心原因**：贪心策略的不可逆性

**反例演示**：
```
图结构：
    A ----(5)----> B
    |             |
   (4)           (-3)
    |             |
    v             v
    C ---------> D
         (1)

从A出发到D：

Dijkstra执行过程：
1. 选择A，dist[A]=0，更新dist[B]=5, dist[C]=4
2. 选择C（dist最小），标记C已访问
3. 选择B，标记B已访问，更新dist[D]=5+(-3)=2
4. 选择D，结束

结果：dist[D] = 2

但实际最短路径：A→C→D，dist[D] = 4+1 = 5？
等等，这个例子不对...

正确反例：
    A ----(5)----> B
    |             |
   (4)           (2)
    |             ↑
    v            (-3)
    C ---------> D
         (1)

Dijkstra：
1. A→B (5), A→C (4)
2. 选C，C→D (4+1=5)
3. 选B，B→D (5+2=7)，不更新
4. 选D，dist[D]=5

但实际：A→B→D→C？不行，D→C是-3...

正确理解：
当存在负权边时，已确定的最短路径可能被后续发现的更短路径更新，
但Dijkstra的贪心策略不允许重新访问已确定顶点。
```

**解决方案**：使用Bellman-Ford算法或SPFA算法处理负权边。

### 题2：Dijkstra和Prim算法的区别？⭐⭐⭐⭐

| 维度 | Dijkstra算法 | Prim算法 |
|------|-------------|----------|
| **问题类型** | 单源最短路径 | 最小生成树 |
| **更新对象** | dist[v] = 到起点的最短距离 | dist[v] = 到已选集合的最小边权 |
| **选择标准** | 选择dist最小的顶点 | 选择连接边权最小的顶点 |
| **最终结果** | 起点到各点的最短距离 | 连接所有顶点的最小边集 |
| **应用场景** | 导航、路由 | 网络设计、电路布线 |

**代码对比**：
```java
// Dijkstra：更新到起点的距离
if (dist[u] + weight < dist[v]) {
    dist[v] = dist[u] + weight;
}

// Prim：更新到已选集合的最小边权
if (weight < dist[v]) {
    dist[v] = weight;
}
```

### 题3：如何优化Dijkstra算法？⭐⭐⭐⭐⭐

| 优化策略 | 实现方式 | 效果 | 适用场景 |
|----------|----------|------|----------|
| **堆优化** | 优先队列代替线性查找 | O(V²) → O((V+E)logV) | 稀疏图 |
| **斐波那契堆** | 使用斐波那契堆 | O(VlogV + E) | 理论最优 |
| **提前终止** | 找到目标即返回 | 减少不必要计算 | 单目标查询 |
| **双向Dijkstra** | 从起点和终点同时搜索 | 搜索空间减半 | 大规模图 |
| **A*算法** | 加入启发式函数 | 更快找到目标 | 有坐标信息的图 |

### 题4：Dijkstra vs Bellman-Ford vs Floyd如何选择？⭐⭐⭐⭐

| 算法 | 时间复杂度 | 适用场景 | 特点 |
|------|------------|----------|------|
| **Dijkstra** | O((V+E)logV) | 单源、非负权 | 最快，不能处理负权 |
| **Bellman-Ford** | O(VE) | 单源、可有负权 | 可检测负环 |
| **SPFA** | O(kE)平均 | 单源、稀疏图 | Bellman-Ford优化 |
| **Floyd** | O(V³) | 多源 | 动态规划，代码短 |

**选择决策树**：
```
需要最短路径？
├── 单源？
│   ├── 有负权边？
│   │   ├── 是 → Bellman-Ford / SPFA
│   │   └── 否 → Dijkstra（优先队列优化）
│   └── 多源？
│       └── Floyd-Warshall
```

### 题5：实际应用场景？⭐⭐⭐⭐

| 领域 | 应用 | 具体实现 |
|------|------|----------|
| **地图导航** | GPS路径规划 | 结合A*算法，考虑实时交通 |
| **网络路由** | OSPF协议 | 链路状态路由算法 |
| **游戏开发** | NPC寻路 | 结合A*，考虑地形代价 |
| **社交网络** | 最短关系链 | 六度分隔理论验证 |
| **物流配送** | 最优配送路线 | 结合车辆容量约束 |

## 六、知识图谱

```
最短路径算法体系
│
├── 单源最短路径
│   ├── 非负权边
│   │   └── Dijkstra算法
│   │       ├── 朴素实现 O(V²)
│   │       ├── 堆优化 O((V+E)logV)
│   │       └── 斐波那契堆 O(VlogV+E)
│   │
│   └── 可有负权边
│       ├── Bellman-Ford O(VE)
│       └── SPFA（队列优化）
│
├── 多源最短路径
│   └── Floyd-Warshall O(V³)
│       └── 动态规划：dp[k][i][j]
│
├── 特殊场景
│   ├── 次短路径
│   ├── K短路径
│   └── 带限制的最短路径
│
└── 面试重点
    ├── 不能处理负权边的原因
    ├── 与Prim算法的区别
    ├── 时间复杂度优化
    └── 实际应用场景
```

## 七、示例代码

完整示例代码位于：`interview-microservices-parent/interview-algorithm/src/main/java/cn/itzixiao/interview/algorithm/graph/DijkstraAlgorithm.java`

运行方式：
```bash
mvn exec:java -pl interview-microservices-parent/interview-algorithm \
  -Dexec.mainClass="cn.itzixiao.interview.algorithm.graph.DijkstraAlgorithm" -q
```
