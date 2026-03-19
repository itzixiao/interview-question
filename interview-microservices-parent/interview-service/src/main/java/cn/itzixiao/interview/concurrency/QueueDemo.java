package cn.itzixiao.interview.concurrency;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * =====================================================================================
 * Queue 体系详解 —— PriorityQueue / ArrayDeque / Deque 实战演示
 * =====================================================================================
 * <p>
 * 对应文档：docs/02-Java并发编程/01-Java集合框架.md  Queue 实现章节
 * <p>
 * 演示内容：
 * 1. Queue 接口两套方法（抛异常 vs 返回特殊值）
 * 2. PriorityQueue —— 最小堆 / 最大堆 / 自定义优先级
 * 3. PriorityQueue 实现 Top K 问题
 * 4. ArrayDeque —— 循环数组，替代 Stack 和 LinkedList
 * 5. ArrayDeque 作为栈（DFS）和队列（BFS）
 * 6. Deque 双端队列操作
 * 7. 高频面试题
 */
public class QueueDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== Queue 体系详解 ==========\n");

        // 1. Queue 接口方法对比
        demonstrateQueueMethods();

        // 2. PriorityQueue 最小堆 / 最大堆
        demonstratePriorityQueue();

        // 3. Top K 问题
        demonstrateTopK();

        // 4. ArrayDeque 替代 Stack
        demonstrateArrayDequeAsStack();

        // 5. ArrayDeque 替代 LinkedList 作队列
        demonstrateArrayDequeAsQueue();

        // 6. ArrayDeque 滑动窗口最大值
        demonstrateSlidingWindowMax();

        // 7. 高频面试题
        showInterviewQuestions();
    }

    // =========================================================================
    // 1. Queue 接口两套方法对比
    // =========================================================================
    private static void demonstrateQueueMethods() {
        System.out.println("【一、Queue 接口方法对比】\n");

        Queue<String> queue = new LinkedList<>(Arrays.asList("A", "B", "C"));

        System.out.println("两套操作方法区别：");
        System.out.println("┌──────┬────────────┬─────────────────────────────────┐");
        System.out.println("│ 操作 │ 抛出异常   │ 返回特殊值（推荐）              │");
        System.out.println("├──────┼────────────┼─────────────────────────────────┤");
        System.out.println("│ 入队 │ add(e)     │ offer(e) → false 表示失败       │");
        System.out.println("│ 出队 │ remove()   │ poll()   → null  表示队空       │");
        System.out.println("│ 查首 │ element()  │ peek()   → null  表示队空       │");
        System.out.println("└──────┴────────────┴─────────────────────────────────┘\n");

        // offer / poll / peek
        System.out.println("队列内容：" + queue);
        System.out.println("peek()（不移除）：" + queue.peek());
        System.out.println("poll()（移除）：" + queue.poll());
        System.out.println("poll() 后：" + queue);

        // 空队列时的行为差异
        Queue<String> empty = new LinkedList<>();
        System.out.println("\n空队列时：");
        System.out.println("peek()：" + empty.peek() + "（返回 null，不抛异常）");
        System.out.println("poll()：" + empty.poll() + "（返回 null，不抛异常）");
        try {
            empty.element();    // 抛 NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("element()：抛出 " + e.getClass().getSimpleName());
        }
        try {
            empty.remove();     // 抛 NoSuchElementException
        } catch (NoSuchElementException e) {
            System.out.println("remove()：抛出 " + e.getClass().getSimpleName());
        }
        System.out.println();
    }

    // =========================================================================
    // 2. PriorityQueue 最小堆 / 最大堆
    // =========================================================================
    private static void demonstratePriorityQueue() {
        System.out.println("【二、PriorityQueue 最小堆 / 最大堆】\n");

        // --- 默认最小堆（自然升序） ---
        System.out.println("--- 默认最小堆（每次 poll 取最小值）---");
        PriorityQueue<Integer> minHeap = new PriorityQueue<>();
        int[] nums = {5, 2, 8, 1, 9, 3, 7, 4, 6};
        for (int n : nums) minHeap.offer(n);
        System.out.print("依次 poll：");
        while (!minHeap.isEmpty()) {
            System.out.print(minHeap.poll() + " ");
        }
        System.out.println("（从小到大）");

        // --- 最大堆（reverseOrder） ---
        System.out.println("\n--- 最大堆（每次 poll 取最大值）---");
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        for (int n : nums) maxHeap.offer(n);
        System.out.print("依次 poll：");
        while (!maxHeap.isEmpty()) {
            System.out.print(maxHeap.poll() + " ");
        }
        System.out.println("（从大到小）");

        // --- 自定义对象按优先级排序（任务调度） ---
        System.out.println("\n--- 自定义对象：任务调度（优先级数字越小越先执行）---");
        PriorityQueue<Task> taskQueue = new PriorityQueue<>(
                Comparator.comparingInt(Task::getPriority)
        );
        taskQueue.offer(new Task("数据备份",   3));
        taskQueue.offer(new Task("支付结算",   1));   // 最高优先级
        taskQueue.offer(new Task("报表生成",   2));
        taskQueue.offer(new Task("日志清理",   5));
        taskQueue.offer(new Task("缓存刷新",   4));

        System.out.println("任务调度顺序：");
        while (!taskQueue.isEmpty()) {
            Task t = taskQueue.poll();
            System.out.println("  执行：[P" + t.getPriority() + "] " + t.getName());
        }

        // peek 只看不取
        PriorityQueue<Integer> pq = new PriorityQueue<>(Arrays.asList(3, 1, 4, 1, 5));
        System.out.println("\npeek()（只看堆顶，不移除）：" + pq.peek());
        System.out.println("peek() 后大小不变：" + pq.size() + "\n");
    }

    // =========================================================================
    // 3. Top K 问题（经典面试题）
    // =========================================================================
    private static void demonstrateTopK() {
        System.out.println("【三、Top K 问题 —— PriorityQueue 最优解】\n");

        int[] arr = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7, 9, 3, 2, 3, 8, 4};
        int k = 5;

        System.out.println("数组：" + Arrays.toString(arr));
        System.out.println("求最大的 " + k + " 个数\n");

        System.out.println("方案对比：");
        System.out.println("┌────────────┬──────────────┬─────────┬─────────────────────┐");
        System.out.println("│ 方案       │ 时间复杂度   │ 空间    │ 适用场景            │");
        System.out.println("├────────────┼──────────────┼─────────┼─────────────────────┤");
        System.out.println("│ 全部排序   │ O(n log n)   │ O(n)    │ n 较小              │");
        System.out.println("│ 最小堆     │ O(n log k)   │ O(k)    │ n 很大、k 较小      │");
        System.out.println("│ 快速选择   │ O(n) 均摊    │ O(1)    │ 不要求有序输出      │");
        System.out.println("└────────────┴──────────────┴─────────┴─────────────────────┘\n");

        // --- 最小堆法（维护大小为 k 的最小堆） ---
        System.out.println("最小堆法（O(n log k)）：");
        System.out.println("核心思路：维护一个大小为 k 的最小堆，遍历数组：");
        System.out.println("  - 元素比堆顶大 → 替换堆顶（淘汰最小，保留较大）");
        System.out.println("  - 元素 ≤ 堆顶  → 跳过（堆里全是更大的）\n");

        PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);    // 最小堆
        for (int num : arr) {
            if (minHeap.size() < k) {
                minHeap.offer(num);
            } else if (num > minHeap.peek()) {
                // 当前元素大于堆顶（堆中最小值），替换
                minHeap.poll();
                minHeap.offer(num);
            }
        }
        // 堆中剩余 k 个即为最大的 k 个数
        int[] topK = new int[k];
        for (int i = k - 1; i >= 0; i--) {
            topK[i] = minHeap.poll();
        }
        System.out.println("最大的 " + k + " 个数（升序）：" + Arrays.toString(topK));

        // --- Top K 频次（词频统计） ---
        System.out.println("\n--- Top K 频次：出现频率最高的 3 个词 ---");
        String[] words = {"apple", "banana", "apple", "cherry", "banana",
                          "apple", "date", "cherry", "apple", "banana"};
        // 统计词频
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) freq.merge(w, 1, Integer::sum);
        System.out.println("词频：" + freq);

        // 按频次维护最小堆（大小为 3）
        PriorityQueue<Map.Entry<String, Integer>> freqHeap =
                new PriorityQueue<>(3, Comparator.comparingInt(Map.Entry::getValue));
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            if (freqHeap.size() < 3) {
                freqHeap.offer(e);
            } else if (e.getValue() > freqHeap.peek().getValue()) {
                freqHeap.poll();
                freqHeap.offer(e);
            }
        }
        System.out.print("Top 3 高频词：");
        List<String> topWords = new ArrayList<>();
        while (!freqHeap.isEmpty()) topWords.add(0, freqHeap.poll().getKey() );
        System.out.println(topWords + "\n");
    }

    // =========================================================================
    // 4. ArrayDeque 替代 Stack（DFS 迭代）
    // =========================================================================
    private static void demonstrateArrayDequeAsStack() {
        System.out.println("【四、ArrayDeque 替代 Stack（DFS 迭代版）】\n");

        System.out.println("Stack vs ArrayDeque 对比：");
        System.out.println("  Stack：继承 Vector，所有方法 synchronized，性能差");
        System.out.println("  ArrayDeque：基于循环数组，无锁，性能是 Stack 的 ~2 倍");
        System.out.println("  结论：官方推荐用 ArrayDeque 替代 Stack\n");

        // ArrayDeque 作为栈（LIFO）
        Deque<String> stack = new ArrayDeque<>();
        stack.push("第1帧：main()");
        stack.push("第2帧：methodA()");
        stack.push("第3帧：methodB()");
        System.out.println("调用栈（push 3帧）：" + stack);
        System.out.println("pop()：" + stack.pop() + "（后进先出）");
        System.out.println("pop()：" + stack.pop());
        System.out.println("当前栈：" + stack + "\n");

        // 经典面试题：括号匹配（用栈实现）
        System.out.println("--- 括号匹配（栈的经典应用）---");
        String[] testCases = {"([]{})", "([)]", "{[]}", "((("};
        for (String s : testCases) {
            System.out.println("  \"" + s + "\" → " + (isValidParentheses(s) ? "✅ 合法" : "❌ 非法"));
        }

        // DFS 迭代（用 ArrayDeque 替代递归栈）
        System.out.println("\n--- 二叉树 DFS 前序遍历（ArrayDeque 替代递归）---");
        // 构造简单二叉树：    1
        //                   / \
        //                  2   3
        //                 / \
        //                4   5
        TreeNode root = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), new TreeNode(5)),
                new TreeNode(3, null, null));

        System.out.print("前序遍历（根→左→右）：");
        Deque<TreeNode> dfsStack = new ArrayDeque<>();
        dfsStack.push(root);
        while (!dfsStack.isEmpty()) {
            TreeNode node = dfsStack.pop();
            System.out.print(node.val + " ");
            // 先压右子节点，再压左子节点（保证左先出）
            if (node.right != null) dfsStack.push(node.right);
            if (node.left  != null) dfsStack.push(node.left);
        }
        System.out.println("\n");
    }

    // =========================================================================
    // 5. ArrayDeque 替代 LinkedList 作队列（BFS）
    // =========================================================================
    private static void demonstrateArrayDequeAsQueue() {
        System.out.println("【五、ArrayDeque 作为队列（BFS 广度优先遍历）】\n");

        System.out.println("LinkedList vs ArrayDeque 作队列：");
        System.out.println("  LinkedList：每个节点都要创建 Node 对象，GC 压力大");
        System.out.println("  ArrayDeque：循环数组，内存连续，无节点对象开销，性能更好\n");

        // BFS 层序遍历二叉树
        //        1
        //       / \
        //      2   3
        //     / \   \
        //    4   5   6
        TreeNode root = new TreeNode(1,
                new TreeNode(2, new TreeNode(4), new TreeNode(5)),
                new TreeNode(3, null, new TreeNode(6)));

        System.out.println("二叉树层序遍历（BFS）：");
        Queue<TreeNode> bfsQueue = new ArrayDeque<>();  // 用 ArrayDeque 作 BFS 队列
        bfsQueue.offer(root);
        int level = 0;
        while (!bfsQueue.isEmpty()) {
            int size = bfsQueue.size();     // 当前层节点数
            System.out.print("第" + (++level) + "层：");
            for (int i = 0; i < size; i++) {
                TreeNode node = bfsQueue.poll();
                System.out.print(node.val + " ");
                if (node.left  != null) bfsQueue.offer(node.left);
                if (node.right != null) bfsQueue.offer(node.right);
            }
            System.out.println();
        }
        System.out.println();
    }

    // =========================================================================
    // 6. ArrayDeque 实现滑动窗口最大值（单调双端队列）
    // =========================================================================
    private static void demonstrateSlidingWindowMax() {
        System.out.println("【六、单调双端队列 —— 滑动窗口最大值】\n");

        int[] nums = {1, 3, -1, -3, 5, 3, 6, 7};
        int k = 3;
        System.out.println("数组：" + Arrays.toString(nums) + "，窗口大小 k=" + k);
        System.out.println("期望：每个窗口的最大值\n");

        // 单调递减双端队列：队首始终是当前窗口最大值的索引
        int[] result = slidingWindowMax(nums, k);
        System.out.println("滑动窗口最大值：" + Arrays.toString(result));

        System.out.println("\n单调队列核心思路：");
        System.out.println("  1. 新元素入队前，从队尾移除所有 ≤ 新元素的索引（保持递减）");
        System.out.println("  2. 队首索引超出窗口范围时，从队首移除");
        System.out.println("  3. 队首索引对应的值始终是当前窗口最大值\n");
    }

    // =========================================================================
    // 7. 高频面试题
    // =========================================================================
    private static void showInterviewQuestions() {
        System.out.println("【七、高频面试题】\n");

        System.out.println("Q1：PriorityQueue 底层是什么数据结构？时间复杂度？");
        System.out.println("A1：基于最小堆（完全二叉树用数组实现）");
        System.out.println("    offer/poll：O(log n)（堆化 siftUp/siftDown）");
        System.out.println("    peek：O(1)（直接取堆顶 queue[0]）");
        System.out.println("    非线程安全，并发场景用 PriorityBlockingQueue\n");

        System.out.println("Q2：ArrayDeque 和 LinkedList 作为队列/栈，哪个性能更好？");
        System.out.println("A2：ArrayDeque 更好。原因：");
        System.out.println("    - 基于循环数组，内存连续，CPU 缓存友好");
        System.out.println("    - 无 Node 对象创建，GC 压力小");
        System.out.println("    - 官方 Javadoc 明确推荐 ArrayDeque 替代 Stack 和 LinkedList\n");

        System.out.println("Q3：Queue 的 offer 和 add 有什么区别？");
        System.out.println("A3：offer(e)：入队失败返回 false（推荐使用）");
        System.out.println("    add(e)：入队失败抛出 IllegalStateException");
        System.out.println("    对于有界队列（如 ArrayBlockingQueue），优先使用 offer\n");

        System.out.println("Q4：Top K 问题为什么用最小堆而不是最大堆？");
        System.out.println("A4：求最大的 K 个数用最小堆（大小为 K）：");
        System.out.println("    - 堆顶是堆中最小值，遍历时若新元素 > 堆顶，替换");
        System.out.println("    - 淘汰小的，保留大的，最终堆中剩 K 个最大值");
        System.out.println("    - 时间 O(n log k)，空间 O(k)，比全排序 O(n log n) 更优\n");

        System.out.println("Q5：Deque 相比 Queue 多了哪些能力？");
        System.out.println("A5：Queue 只支持尾部入队 + 头部出队（单向）");
        System.out.println("    Deque 支持两端入队出队（双向），可当：");
        System.out.println("    - 队列（FIFO）：addLast + pollFirst");
        System.out.println("    - 栈（LIFO） ：addFirst + pollFirst (push/pop)");
        System.out.println("    - 单调队列    ：两端都可以 offer/poll，实现滑动窗口等算法\n");
    }

    // =========================================================================
    // 辅助方法和类
    // =========================================================================

    /** 括号匹配（栈的经典应用） */
    private static boolean isValidParentheses(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '[' || c == '{') {
                stack.push(c);
            } else {
                if (stack.isEmpty()) return false;
                char top = stack.pop();
                if (c == ')' && top != '(') return false;
                if (c == ']' && top != '[') return false;
                if (c == '}' && top != '{') return false;
            }
        }
        return stack.isEmpty();
    }

    /** 滑动窗口最大值（单调递减双端队列） */
    private static int[] slidingWindowMax(int[] nums, int k) {
        int n = nums.length;
        int[] result = new int[n - k + 1];
        // 双端队列存储索引，队首对应当前窗口最大值索引
        Deque<Integer> deque = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            // 移除超出窗口左边界的队首索引
            while (!deque.isEmpty() && deque.peekFirst() < i - k + 1) {
                deque.pollFirst();
            }
            // 从队尾移除所有比当前元素小的索引（保持单调递减）
            while (!deque.isEmpty() && nums[deque.peekLast()] < nums[i]) {
                deque.pollLast();
            }
            deque.offerLast(i);
            // 窗口形成后记录最大值
            if (i >= k - 1) {
                result[i - k + 1] = nums[deque.peekFirst()];
            }
        }
        return result;
    }

    /** 简单二叉树节点 */
    static class TreeNode {
        int val;
        TreeNode left, right;

        TreeNode(int val) { this.val = val; }
        TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val;
            this.left = left;
            this.right = right;
        }
    }

    /** 任务（用于 PriorityQueue 自定义优先级演示） */
    static class Task {
        private final String name;
        private final int priority;   // 数字越小优先级越高

        Task(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }

        public String getName() { return name; }
        public int getPriority() { return priority; }
    }
}
