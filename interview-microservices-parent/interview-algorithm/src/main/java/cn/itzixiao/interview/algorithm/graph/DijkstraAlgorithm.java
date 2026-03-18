package cn.itzixiao.interview.algorithm.graph;

import java.util.*;

/**
 * 最短路径算法 - Dijkstra算法详解与实现
 *
 * <p>Dijkstra算法由荷兰计算机科学家Edsger W. Dijkstra于1956年提出，
 * 用于在带权有向图中求解单源最短路径问题。
 *
 * <p>核心思想：贪心算法
 * 1. 维护一个距离数组dist[]，记录源点到各点的最短距离
 * 2. 维护一个访问集合，记录已确定最短路径的顶点
 * 3. 每次选择距离最小的未访问顶点，更新其邻接点的距离
 * 4. 重复直到所有顶点都被访问
 *
 * <p>适用条件：
 * - 图必须是有向图或无向图
 * - 边的权重必须为非负数
 *
 * <p>时间复杂度：
 * - 朴素实现：O(V²)
 * - 优先队列优化：O((V+E)logV)
 * - 斐波那契堆优化：O(VlogV + E)
 *
 * <p>空间复杂度：O(V)
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class DijkstraAlgorithm {

    /**
     * 图的边结构
     */
    static class Edge {
        int to;      // 目标顶点
        int weight;  // 边权重

        Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    /**
     * 优先队列节点（用于堆优化版本）
     */
    static class Node implements Comparable<Node> {
        int vertex;   // 顶点
        int dist;     // 当前距离

        Node(int vertex, int dist) {
            this.vertex = vertex;
            this.dist = dist;
        }

        @Override
        public int compareTo(Node other) {
            return this.dist - other.dist;
        }
    }

    /**
     * 1. 朴素Dijkstra算法（邻接矩阵实现）
     *
     * <p>适用场景：稠密图（边数接近V²）
     * 时间复杂度：O(V²)
     * 空间复杂度：O(V²)（邻接矩阵）
     *
     * @param graph 邻接矩阵，graph[i][j]表示i到j的权重，无穷大表示无连接
     * @param start 起始顶点
     * @return 源点到各顶点的最短距离数组
     */
    public static int[] dijkstraMatrix(int[][] graph, int start) {
        int n = graph.length;
        int[] dist = new int[n];        // 距离数组
        boolean[] visited = new boolean[n]; // 访问标记

        // 初始化距离为无穷大
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        // 遍历n次，每次确定一个顶点的最短路径
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

            // 如果没有可达的未访问顶点，结束
            if (u == -1) break;

            visited[u] = true;

            // 更新u的所有邻接点的距离
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

    /**
     * 2. 堆优化Dijkstra算法（邻接表+优先队列）
     *
     * <p>适用场景：稀疏图（边数远小于V²）
     * 时间复杂度：O((V+E)logV)
     * 空间复杂度：O(V+E)
     *
     * @param adj   邻接表
     * @param start 起始顶点
     * @param n     顶点数量
     * @return 源点到各顶点的最短距离数组
     */
    public static int[] dijkstraHeap(List<Edge>[] adj, int start, int n) {
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        // 优先队列，按距离排序
        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.offer(new Node(start, 0));

        while (!pq.isEmpty()) {
            Node node = pq.poll();
            int u = node.vertex;
            int d = node.dist;

            // 如果当前距离大于已记录距离，跳过
            if (d > dist[u]) continue;

            // 遍历所有邻接边
            for (Edge edge : adj[u]) {
                int v = edge.to;
                int weight = edge.weight;
                int newDist = dist[u] + weight;

                if (newDist < dist[v]) {
                    dist[v] = newDist;
                    pq.offer(new Node(v, dist[v]));
                }
            }
        }

        return dist;
    }

    /**
     * 3. 带路径记录的Dijkstra算法
     *
     * <p>不仅返回最短距离，还返回最短路径
     *
     * @param adj   邻接表
     * @param start 起始顶点
     * @param n     顶点数量
     * @return PathResult包含距离数组和前驱节点数组
     */
    public static PathResult dijkstraWithPath(List<Edge>[] adj, int start, int n) {
        int[] dist = new int[n];
        int[] prev = new int[n];  // 前驱节点，用于重建路径
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(prev, -1);
        dist[start] = 0;

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.offer(new Node(start, 0));

        while (!pq.isEmpty()) {
            Node node = pq.poll();
            int u = node.vertex;
            int d = node.dist;

            if (d > dist[u]) continue;

            for (Edge edge : adj[u]) {
                int v = edge.to;
                int weight = edge.weight;
                int newDist = dist[u] + weight;

                if (newDist < dist[v]) {
                    dist[v] = newDist;
                    prev[v] = u;  // 记录前驱节点
                    pq.offer(new Node(v, dist[v]));
                }
            }
        }

        return new PathResult(dist, prev);
    }

    /**
     * 路径结果类
     */
    static class PathResult {
        int[] dist;  // 最短距离
        int[] prev;  // 前驱节点

        PathResult(int[] dist, int[] prev) {
            this.dist = dist;
            this.prev = prev;
        }

        /**
         * 获取从start到target的最短路径
         */
        List<Integer> getPath(int start, int target) {
            List<Integer> path = new ArrayList<>();
            if (dist[target] == Integer.MAX_VALUE) {
                return path; // 不可达
            }

            for (int at = target; at != -1; at = prev[at]) {
                path.add(at);
            }
            Collections.reverse(path);
            return path;
        }
    }

    /**
     * 4. 多源最短路径（多起点Dijkstra）
     *
     * <p>应用场景：多个源点同时出发，求到各点的最短距离
     * 技巧：添加虚拟源点，连接到所有真实源点（权重为0）
     *
     * @param adj     邻接表
     * @param sources 多个源点
     * @param n       顶点数量
     * @return 每个源点到各点的最短距离
     */
    public static int[][] multiSourceDijkstra(List<Edge>[] adj, int[] sources, int n) {
        int[][] allDist = new int[sources.length][n];

        for (int i = 0; i < sources.length; i++) {
            allDist[i] = dijkstraHeap(adj, sources[i], n);
        }

        return allDist;
    }

    /**
     * 5. 带约束的最短路径（限制边数）
     *
     * <p>应用场景：最多经过K条边的最短路径
     * 使用动态规划思想：dp[k][v] = min(dp[k-1][u] + weight(u,v))
     *
     * @param adj      邻接表
     * @param start    起始顶点
     * @param n        顶点数量
     * @param maxEdges 最多经过的边数
     * @return 最短距离数组
     */
    public static int[] dijkstraWithEdgeLimit(List<Edge>[] adj, int start, int n, int maxEdges) {
        // dp[i]表示最多使用maxEdges条边时到i的最短距离
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[start] = 0;

        // 使用BFS思想，记录层数
        Queue<int[]> queue = new LinkedList<>(); // [vertex, edges_used]
        queue.offer(new int[]{start, 0});

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();
            int u = curr[0];
            int edges = curr[1];

            if (edges >= maxEdges) continue;

            for (Edge edge : adj[u]) {
                int v = edge.to;
                int weight = edge.weight;
                int newDist = dist[u] + weight;

                if (newDist < dist[v]) {
                    dist[v] = newDist;
                    queue.offer(new int[]{v, edges + 1});
                }
            }
        }

        return dist;
    }

    /**
     * 6. 次短路径算法
     *
     * <p>应用场景：求第二短的最短路径（用于备选方案）
     * 维护两个距离数组：最短距离和次短距离
     *
     * @param adj   邻接表
     * @param start 起始顶点
     * @param n     顶点数量
     * @return 次短距离数组
     */
    public static int[] secondShortestPath(List<Edge>[] adj, int start, int n) {
        int[] dist1 = new int[n];  // 最短距离
        int[] dist2 = new int[n];  // 次短距离
        Arrays.fill(dist1, Integer.MAX_VALUE);
        Arrays.fill(dist2, Integer.MAX_VALUE);
        dist1[start] = 0;

        // 优先队列：[顶点, 距离, 类型(1=最短,2=次短)]
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{start, 0, 1});

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int u = curr[0];
            int d = curr[1];
            int type = curr[2];

            // 如果当前距离大于已记录距离，跳过
            if ((type == 1 && d > dist1[u]) || (type == 2 && d > dist2[u])) {
                continue;
            }

            for (Edge edge : adj[u]) {
                int v = edge.to;
                int newDist = d + edge.weight;

                // 更新最短距离
                if (newDist < dist1[v]) {
                    dist2[v] = dist1[v];  // 原最短变为次短
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

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== Dijkstra最短路径算法演示 ==========\n");

        // 构建示例图（邻接矩阵）
        int INF = Integer.MAX_VALUE;
        int[][] graph = {
                {0, 4, INF, INF, INF, INF, INF, 8, INF},
                {4, 0, 8, INF, INF, INF, INF, 11, INF},
                {INF, 8, 0, 7, INF, 4, INF, INF, 2},
                {INF, INF, 7, 0, 9, 14, INF, INF, INF},
                {INF, INF, INF, 9, 0, 10, INF, INF, INF},
                {INF, INF, 4, 14, 10, 0, 2, INF, INF},
                {INF, INF, INF, INF, INF, 2, 0, 1, 6},
                {8, 11, INF, INF, INF, INF, 1, 0, 7},
                {INF, INF, 2, INF, INF, INF, 6, 7, 0}
        };

        // 1. 朴素Dijkstra
        System.out.println("1. 朴素Dijkstra算法（邻接矩阵）:");
        int[] dist1 = dijkstraMatrix(graph, 0);
        System.out.println("   从顶点0到各顶点的最短距离:");
        for (int i = 0; i < dist1.length; i++) {
            System.out.printf("   到顶点%d: %d%n", i, dist1[i] == INF ? -1 : dist1[i]);
        }

        // 构建邻接表
        int n = 9;
        @SuppressWarnings("unchecked")
        List<Edge>[] adj = new ArrayList[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();

        // 添加边（无向图）
        int[][] edges = {
                {0, 1, 4}, {0, 7, 8}, {1, 2, 8}, {1, 7, 11},
                {2, 3, 7}, {2, 5, 4}, {2, 8, 2}, {3, 4, 9},
                {3, 5, 14}, {4, 5, 10}, {5, 6, 2}, {6, 7, 1},
                {6, 8, 6}, {7, 8, 7}
        };
        for (int[] e : edges) {
            adj[e[0]].add(new Edge(e[1], e[2]));
            adj[e[1]].add(new Edge(e[0], e[2]));
        }

        // 2. 堆优化Dijkstra
        System.out.println("\n2. 堆优化Dijkstra算法（邻接表）:");
        int[] dist2 = dijkstraHeap(adj, 0, n);
        System.out.println("   从顶点0到各顶点的最短距离:");
        for (int i = 0; i < dist2.length; i++) {
            System.out.printf("   到顶点%d: %d%n", i, dist2[i] == INF ? -1 : dist2[i]);
        }

        // 3. 带路径记录的Dijkstra
        System.out.println("\n3. 带路径记录的Dijkstra算法:");
        PathResult result = dijkstraWithPath(adj, 0, n);
        System.out.println("   从顶点0到顶点4的最短路径:");
        List<Integer> path = result.getPath(0, 4);
        System.out.println("   路径: " + path);
        System.out.println("   距离: " + result.dist[4]);

        System.out.println("   从顶点0到顶点8的最短路径:");
        path = result.getPath(0, 8);
        System.out.println("   路径: " + path);
        System.out.println("   距离: " + result.dist[8]);

        // 4. 多源最短路径
        System.out.println("\n4. 多源最短路径:");
        int[] sources = {0, 3};
        int[][] multiDist = multiSourceDijkstra(adj, sources, n);
        for (int i = 0; i < sources.length; i++) {
            System.out.printf("   从顶点%d出发:%n", sources[i]);
            for (int j = 0; j < n; j++) {
                System.out.printf("     到顶点%d: %d%n", j, multiDist[i][j]);
            }
        }

        // 5. 带边数限制的最短路径
        System.out.println("\n5. 最多经过3条边的最短路径（从顶点0）:");
        int[] limitedDist = dijkstraWithEdgeLimit(adj, 0, n, 3);
        for (int i = 0; i < n; i++) {
            System.out.printf("   到顶点%d: %d%n", i, limitedDist[i] == INF ? -1 : limitedDist[i]);
        }

        // 6. 次短路径
        System.out.println("\n6. 次短路径（从顶点0）:");
        int[] secondDist = secondShortestPath(adj, 0, n);
        for (int i = 0; i < n; i++) {
            System.out.printf("   到顶点%d的次短距离: %d%n", i, secondDist[i] == INF ? -1 : secondDist[i]);
        }

        System.out.println("\n========== 演示结束 ==========");
    }
}
