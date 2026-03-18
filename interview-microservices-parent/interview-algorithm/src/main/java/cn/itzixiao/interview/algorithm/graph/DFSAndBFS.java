package cn.itzixiao.interview.algorithm.graph;

import java.util.*;

/**
 * DFS（深度优先搜索）与 BFS（广度优先搜索）详解与实现
 *
 * <p>图遍历是图算法的基础，DFS和BFS是最核心的两种遍历方式。
 *
 * <p>DFS（Depth First Search）深度优先搜索：
 * - 策略：尽可能深地探索图的分支
 * - 实现：递归或栈
 * - 特点：空间复杂度低，可能找到非最优解
 * - 应用：拓扑排序、连通分量、路径查找、回溯算法
 *
 * <p>BFS（Breadth First Search）广度优先搜索：
 * - 策略：先访问所有邻接节点，再访问邻接的邻接
 * - 实现：队列
 * - 特点：保证找到最短路径（无权图），空间复杂度高
 * - 应用：最短路径、层次遍历、社交网络分析
 *
 * <p>时间复杂度：O(V + E)，V为顶点数，E为边数
 * <p>空间复杂度：O(V)
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class DFSAndBFS {

    /**
     * 图节点
     */
    static class GraphNode {
        int val;
        List<GraphNode> neighbors;

        GraphNode(int val) {
            this.val = val;
            this.neighbors = new ArrayList<>();
        }
    }

    // ==================== DFS 实现 ====================

    /**
     * 1. 递归DFS遍历
     *
     * <p>算法步骤：
     * 1. 访问当前节点并标记
     * 2. 递归访问所有未访问的邻接节点
     *
     * @param node 起始节点
     * @return 遍历顺序
     */
    public static List<Integer> dfsRecursive(GraphNode node) {
        List<Integer> result = new ArrayList<>();
        Set<GraphNode> visited = new HashSet<>();
        dfsHelper(node, visited, result);
        return result;
    }

    private static void dfsHelper(GraphNode node, Set<GraphNode> visited, List<Integer> result) {
        if (node == null || visited.contains(node)) {
            return;
        }

        visited.add(node);
        result.add(node.val);

        for (GraphNode neighbor : node.neighbors) {
            dfsHelper(neighbor, visited, result);
        }
    }

    /**
     * 2. 迭代DFS遍历（使用栈）
     *
     * <p>避免递归栈溢出，适合大规模图
     *
     * @param node 起始节点
     * @return 遍历顺序
     */
    public static List<Integer> dfsIterative(GraphNode node) {
        List<Integer> result = new ArrayList<>();
        if (node == null) return result;

        Set<GraphNode> visited = new HashSet<>();
        Stack<GraphNode> stack = new Stack<>();
        stack.push(node);

        while (!stack.isEmpty()) {
            GraphNode current = stack.pop();

            if (visited.contains(current)) {
                continue;
            }

            visited.add(current);
            result.add(current.val);

            // 将邻接节点压栈（逆序保证遍历顺序一致）
            for (int i = current.neighbors.size() - 1; i >= 0; i--) {
                GraphNode neighbor = current.neighbors.get(i);
                if (!visited.contains(neighbor)) {
                    stack.push(neighbor);
                }
            }
        }

        return result;
    }

    // ==================== BFS 实现 ====================

    /**
     * 3. BFS遍历（使用队列）
     *
     * <p>算法步骤：
     * 1. 起始节点入队并标记
     * 2. 队列非空时，出队一个节点并访问
     * 3. 将所有未访问的邻接节点入队
     * 4. 重复步骤2-3直到队列为空
     *
     * @param node 起始节点
     * @return 遍历顺序
     */
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

    /**
     * 4. 分层BFS（记录每层节点）
     *
     * @param node 起始节点
     * @return 每层节点列表
     */
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

    // ==================== 邻接表表示的图遍历 ====================

    /**
     * 5. DFS遍历邻接表图
     *
     * @param adj   邻接表
     * @param start 起始顶点
     * @param n     顶点数量
     * @return 遍历顺序
     */
    public static List<Integer> dfsAdjacencyList(List<Integer>[] adj, int start, int n) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[n];
        dfsAdjHelper(adj, start, visited, result);
        return result;
    }

    private static void dfsAdjHelper(List<Integer>[] adj, int v,
                                     boolean[] visited, List<Integer> result) {
        visited[v] = true;
        result.add(v);

        for (int neighbor : adj[v]) {
            if (!visited[neighbor]) {
                dfsAdjHelper(adj, neighbor, visited, result);
            }
        }
    }

    /**
     * 6. BFS遍历邻接表图
     *
     * @param adj   邻接表
     * @param start 起始顶点
     * @param n     顶点数量
     * @return 遍历顺序
     */
    public static List<Integer> bfsAdjacencyList(List<Integer>[] adj, int start, int n) {
        List<Integer> result = new ArrayList<>();
        boolean[] visited = new boolean[n];
        Queue<Integer> queue = new LinkedList<>();

        queue.offer(start);
        visited[start] = true;

        while (!queue.isEmpty()) {
            int v = queue.poll();
            result.add(v);

            for (int neighbor : adj[v]) {
                if (!visited[neighbor]) {
                    visited[neighbor] = true;
                    queue.offer(neighbor);
                }
            }
        }

        return result;
    }

    // ==================== 经典应用 ====================

    /**
     * 7. 判断图中是否存在路径（DFS）
     *
     * @param adj   邻接表
     * @param start 起点
     * @param end   终点
     * @param n     顶点数
     * @return 是否存在路径
     */
    public static boolean hasPath(List<Integer>[] adj, int start, int end, int n) {
        boolean[] visited = new boolean[n];
        return hasPathDFS(adj, start, end, visited);
    }

    private static boolean hasPathDFS(List<Integer>[] adj, int current,
                                      int end, boolean[] visited) {
        if (current == end) return true;

        visited[current] = true;

        for (int neighbor : adj[current]) {
            if (!visited[neighbor]) {
                if (hasPathDFS(adj, neighbor, end, visited)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 8. 查找最短路径（BFS，无权图）
     *
     * @param adj   邻接表
     * @param start 起点
     * @param end   终点
     * @param n     顶点数
     * @return 最短路径，不存在返回空列表
     */
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

    /**
     * 9. 查找所有连通分量（DFS）
     *
     * @param adj 邻接表
     * @param n   顶点数
     * @return 所有连通分量
     */
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

    /**
     * 10. 拓扑排序（DFS）
     *
     * <p>适用于有向无环图（DAG）
     * 应用：任务调度、编译顺序、课程选修计划
     *
     * @param adj 邻接表（有向图）
     * @param n   顶点数
     * @return 拓扑排序结果，如果存在环返回空列表
     */
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
            if (state[neighbor] == 1) {
                return false; // 发现回边，存在环
            }
            if (state[neighbor] == 0) {
                if (!dfsTopo(adj, neighbor, state, stack)) {
                    return false;
                }
            }
        }

        state[v] = 2; // 标记为已访问
        stack.push(v);
        return true;
    }

    /**
     * 11. 拓扑排序（Kahn算法，BFS）
     *
     * @param adj 邻接表
     * @param n   顶点数
     * @return 拓扑排序结果
     */
    public static List<Integer> topologicalSortKahn(List<Integer>[] adj, int n) {
        int[] inDegree = new int[n];

        // 计算入度
        for (int i = 0; i < n; i++) {
            for (int neighbor : adj[i]) {
                inDegree[neighbor]++;
            }
        }

        // 入度为0的节点入队
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            if (inDegree[i] == 0) {
                queue.offer(i);
            }
        }

        List<Integer> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            int v = queue.poll();
            result.add(v);

            for (int neighbor : adj[v]) {
                inDegree[neighbor]--;
                if (inDegree[neighbor] == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        // 如果结果数量不等于顶点数，说明存在环
        return result.size() == n ? result : new ArrayList<>();
    }

    /**
     * 12. 判断二分图（BFS着色）
     *
     * <p>二分图：顶点可分为两组，组内无边，组间有边
     *
     * @param adj 邻接表
     * @param n   顶点数
     * @return 是否为二分图
     */
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
                        return false; // 相邻节点同色，不是二分图
                    }
                }
            }
        }

        return true;
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== DFS与BFS图遍历演示 ==========\n");

        // 1. 构建示例图（节点形式）
        System.out.println("1. 节点形式图遍历:");
        GraphNode node1 = new GraphNode(1);
        GraphNode node2 = new GraphNode(2);
        GraphNode node3 = new GraphNode(3);
        GraphNode node4 = new GraphNode(4);
        GraphNode node5 = new GraphNode(5);

        node1.neighbors.addAll(Arrays.asList(node2, node3));
        node2.neighbors.addAll(Arrays.asList(node1, node4, node5));
        node3.neighbors.addAll(Arrays.asList(node1, node5));
        node4.neighbors.add(node2);
        node5.neighbors.addAll(Arrays.asList(node2, node3));

        System.out.println("   图结构: 1-2-4, 1-3-5-2");
        System.out.println("   DFS递归: " + dfsRecursive(node1));
        System.out.println("   DFS迭代: " + dfsIterative(node1));
        System.out.println("   BFS: " + bfs(node1));
        System.out.println("   BFS分层: " + bfsLevelOrder(node1));

        // 2. 邻接表图遍历
        System.out.println("\n2. 邻接表形式图遍历:");
        int n = 6;
        @SuppressWarnings("unchecked")
        List<Integer>[] adj = new ArrayList[n];
        for (int i = 0; i < n; i++) adj[i] = new ArrayList<>();

        // 构建无向图: 0-1-2-3, 0-4-5
        adj[0].addAll(Arrays.asList(1, 4));
        adj[1].addAll(Arrays.asList(0, 2));
        adj[2].addAll(Arrays.asList(1, 3));
        adj[3].add(2);
        adj[4].addAll(Arrays.asList(0, 5));
        adj[5].add(4);

        System.out.println("   图结构: 0-1-2-3, 0-4-5");
        System.out.println("   从0开始DFS: " + dfsAdjacencyList(adj, 0, n));
        System.out.println("   从0开始BFS: " + bfsAdjacencyList(adj, 0, n));

        // 3. 路径查找
        System.out.println("\n3. 路径查找:");
        System.out.println("   0到3是否存在路径: " + hasPath(adj, 0, 3, n));
        System.out.println("   0到3最短路径: " + shortestPath(adj, 0, 3, n));
        System.out.println("   0到5最短路径: " + shortestPath(adj, 0, 5, n));

        // 4. 连通分量
        System.out.println("\n4. 连通分量查找:");
        @SuppressWarnings("unchecked")
        List<Integer>[] adj2 = new ArrayList[7];
        for (int i = 0; i < 7; i++) adj2[i] = new ArrayList<>();

        // 两个连通分量: {0,1,2} 和 {3,4} 和 {5,6}
        adj2[0].add(1);
        adj2[1].add(0);
        adj2[1].add(2);
        adj2[2].add(1);
        adj2[3].add(4);
        adj2[4].add(3);
        adj2[5].add(6);
        adj2[6].add(5);

        List<List<Integer>> components = findConnectedComponents(adj2, 7);
        System.out.println("   连通分量: " + components);

        // 5. 拓扑排序
        System.out.println("\n5. 拓扑排序:");
        @SuppressWarnings("unchecked")
        List<Integer>[] dag = new ArrayList[6];
        for (int i = 0; i < 6; i++) dag[i] = new ArrayList<>();

        // 有向边: 5->2, 5->0, 4->0, 4->1, 2->3, 3->1
        dag[5].addAll(Arrays.asList(2, 0));
        dag[4].addAll(Arrays.asList(0, 1));
        dag[2].add(3);
        dag[3].add(1);

        System.out.println("   有向边: 5→2, 5→0, 4→0, 4→1, 2→3, 3→1");
        System.out.println("   DFS拓扑排序: " + topologicalSortDFS(dag, 6));
        System.out.println("   Kahn拓扑排序: " + topologicalSortKahn(dag, 6));

        // 6. 二分图判断
        System.out.println("\n6. 二分图判断:");
        @SuppressWarnings("unchecked")
        List<Integer>[] bipartite = new ArrayList[4];
        for (int i = 0; i < 4; i++) bipartite[i] = new ArrayList<>();

        // 二分图: 0-1, 0-3, 1-2, 2-3
        bipartite[0].addAll(Arrays.asList(1, 3));
        bipartite[1].addAll(Arrays.asList(0, 2));
        bipartite[2].addAll(Arrays.asList(1, 3));
        bipartite[3].addAll(Arrays.asList(0, 2));

        System.out.println("   图结构: 0-1-2-3-0 (偶数环)");
        System.out.println("   是否为二分图: " + isBipartite(bipartite, 4));

        // 非二分图（奇数环）
        @SuppressWarnings("unchecked")
        List<Integer>[] notBipartite = new ArrayList[3];
        for (int i = 0; i < 3; i++) notBipartite[i] = new ArrayList<>();
        notBipartite[0].addAll(Arrays.asList(1, 2));
        notBipartite[1].addAll(Arrays.asList(0, 2));
        notBipartite[2].addAll(Arrays.asList(0, 1));

        System.out.println("   图结构: 0-1-2-0 (三角形)");
        System.out.println("   是否为二分图: " + isBipartite(notBipartite, 3));

        // 7. 实际应用：迷宫求解
        System.out.println("\n7. 实际应用 - 迷宫求解:");
        char[][] maze = {
                {'S', '.', '#', '.', '.'},
                {'.', '#', '.', '.', '#'},
                {'.', '.', '.', '#', '.'},
                {'#', '.', '#', '.', '.'},
                {'.', '.', '.', '.', 'E'}
        };

        System.out.println("   迷宫地图 (S=起点, E=终点, #=墙, .=通路):");
        for (char[] row : maze) {
            System.out.print("   ");
            for (char c : row) {
                System.out.print(c + " ");
            }
            System.out.println();
        }

        List<int[]> path = solveMazeBFS(maze);
        System.out.println("   BFS找到的最短路径步数: " + (path.size() - 1));

        System.out.println("\n========== 演示结束 ==========");
    }

    /**
     * 辅助方法：BFS求解迷宫
     */
    private static List<int[]> solveMazeBFS(char[][] maze) {
        int rows = maze.length;
        int cols = maze[0].length;

        int[] start = null, end = null;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (maze[i][j] == 'S') start = new int[]{i, j};
                if (maze[i][j] == 'E') end = new int[]{i, j};
            }
        }

        boolean[][] visited = new boolean[rows][cols];
        Map<String, int[]> parent = new HashMap<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        Queue<int[]> queue = new LinkedList<>();
        queue.offer(start);
        visited[start[0]][start[1]] = true;

        while (!queue.isEmpty()) {
            int[] curr = queue.poll();

            if (curr[0] == end[0] && curr[1] == end[1]) {
                // 重建路径
                List<int[]> path = new ArrayList<>();
                int[] at = end;
                while (at != null) {
                    path.add(at);
                    at = parent.get(at[0] + "," + at[1]);
                }
                Collections.reverse(path);
                return path;
            }

            for (int[] dir : dirs) {
                int nr = curr[0] + dir[0];
                int nc = curr[1] + dir[1];

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols
                        && !visited[nr][nc] && maze[nr][nc] != '#') {
                    visited[nr][nc] = true;
                    queue.offer(new int[]{nr, nc});
                    parent.put(nr + "," + nc, curr);
                }
            }
        }

        return new ArrayList<>();
    }
}
