package cn.itzixiao.interview.algorithm.linkedlist;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * 合并 K 个升序链表
 * 
 * LeetCode 第 23 题：https://leetcode.cn/problems/merge-k-sorted-lists/
 * 
 * 题目描述：
 * 给你一个链表数组，每个链表都已经按升序排列。
 * 请你将所有链表合并到一个升序链表中，返回合并后的链表。
 * 
 * 示例：
 * 输入：lists = [[1,4,5],[1,3,4],[2,6]]
 * 输出：[1,1,2,3,4,4,5,6]
 * 
 * 解题思路：
 * 1. 顺序合并：依次将每个链表与结果链表合并
 * 2. 分治合并：使用归并排序的思想，两两合并
 * 3. 最小堆：使用优先队列维护 k 个链表的最小值
 * 
 * @author itzixiao
 * @date 2026-03-16
 */
@Slf4j
class MergeKSortedLists {

    /**
     * 链表节点定义
     */
    public static class ListNode {
        int val;
        ListNode next;

        ListNode() {
        }

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

    /**
     * 方法 3：最小堆/优先队列（推荐，最优解）
     * 
     * 核心思路：
     * 1. 创建一个小顶堆，将每个链表的头节点加入堆中
     * 2. 每次从堆中取出最小节点，加入结果链表
     * 3. 如果该节点有下一个节点，将下一个节点加入堆中
     * 4. 重复步骤 2-3，直到堆为空
     * 
     * 时间复杂度：O(n*logk)
     *   - n: 所有节点的总数
     *   - k: 链表数量
     *   - 每次从堆中取最小值 O(logk)，共 n 次操作
     * 
     * 空间复杂度：O(k)
     *   - 优先队列最多存储 k 个节点
     * 
     * 优势：
     *   - 不需要一次性遍历所有节点
     *   - 适合处理海量数据
     *   - 代码简洁，性能优秀
     * 
     * @param lists 升序链表数组
     * @return 合并后的升序链表
     */
    public ListNode mergeKListsHeap(ListNode[] lists) {
        // 边界条件检查
        if (lists == null || lists.length == 0) {
            return null;
        }
        
        // 创建小顶堆，自定义比较器（按节点值排序）
        PriorityQueue<ListNode> minHeap = new PriorityQueue<>(
            Comparator.comparingInt(node -> node.val)
        );
        
        // 将每个链表的头节点加入堆（排除空链表）
        for (ListNode list : lists) {
            if (list != null) {
                minHeap.offer(list);
            }
        }
        
        // 创建虚拟头节点
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        // 当堆不为空时，持续取出最小节点
        while (!minHeap.isEmpty()) {
            // 取出堆顶元素（当前最小值）
            ListNode node = minHeap.poll();
            
            // 连接到结果链表
            current.next = node;
            current = current.next;
            
            // 如果该节点有下一个节点，加入堆中
            if (node.next != null) {
                minHeap.offer(node.next);
            }
        }
        
        return dummy.next;
    }
    
    /**
     * 方法 1：顺序合并（适合小规模数据）
     * 
     * 核心思路：
     * 1. 初始化结果为第一个链表
     * 2. 依次将后续链表合并到结果中
     * 
     * 时间复杂度：O(k*n)
     *   - k: 链表数量
     *   - n: 平均链表长度
     *   - 最坏情况下，每次合并都需要遍历所有节点
     * 
     * 空间复杂度：O(1)
     *   - 只需要常数级别的额外空间
     * 
     * @param lists 升序链表数组
     * @return 合并后的升序链表
     */
    public ListNode mergeKLists(ListNode[] lists) {
        // 边界条件检查：处理空数组和 null 的情况
        if (lists == null || lists.length == 0) {
            return null;
        }
        
        // 初始化结果为第一个链表
        ListNode result = lists[0];
        
        // 依次将后续链表合并到结果中
        for (int i = 1; i < lists.length; i++) {
            result = mergeTwoLists(result, lists[i]);
        }
        return result;
    }

    /**
     * 合并两个升序链表（迭代法，避免递归栈溢出）
     * 
     * 核心思路：
     * 1. 使用虚拟头节点简化边界处理
     * 2. 双指针分别遍历两个链表
     * 3. 比较节点值，将较小的节点连接到结果链表
     * 4. 连接剩余节点
     * 
     * 时间复杂度：O(m + n)
     *   - m: list1 的长度
     *   - n: list2 的长度
     * 
     * 空间复杂度：O(1)
     *   - 只使用了几个指针变量
     * 
     * @param list1 第一个升序链表
     * @param list2 第二个升序链表
     * @return 合并后的升序链表
     */
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // 处理空指针情况：如果一个链表为空，直接返回另一个
        if (list1 == null) {
            return list2;
        }
        if (list2 == null) {
            return list1;
        }
        
        // 创建虚拟头节点，简化边界处理
        // dummy.next 将指向合并后链表的头节点
        ListNode dummy = new ListNode(0);
        ListNode current = dummy;
        
        // 双指针遍历两个链表
        while (list1 != null && list2 != null) {
            // 将较小的节点连接到结果链表
            if (list1.val <= list2.val) {
                current.next = list1;
                list1 = list1.next;  // list1 指针后移
            } else {
                current.next = list2;
                list2 = list2.next;  // list2 指针后移
            }
            current = current.next;  // 结果链表指针后移
        }
        
        // 连接剩余节点
        // 只有一个链表会有剩余，直接连接即可
        current.next = (list1 != null) ? list1 : list2;
        
        // 返回真正的头节点（dummy.next）
        return dummy.next;
    }
    
    /**
     * 方法 2：分治合并（推荐，性能更优）
     * 
     * 核心思路：
     * 1. 使用归并排序的分治思想
     * 2. 将 k 个链表不断二分，直到每组只有一个链表
     * 3. 然后两两合并，最终得到结果
     * 
     * 时间复杂度：O(n*logk)
     *   - n: 所有节点的总数
     *   - k: 链表数量
     *   - 分治的层数为 logk，每层需要 O(n) 的时间
     * 
     * 空间复杂度：O(logk)
     *   - 递归调用栈的深度为 logk
     * 
     * 优势：
     *   - 相比顺序合并，减少了重复遍历
     *   - 适合大规模数据
     * 
     * @param lists 升序链表数组
     * @return 合并后的升序链表
     */
    public ListNode mergeKListsDivideAndConquer(ListNode[] lists) {
        // 边界条件检查
        if (lists == null || lists.length == 0) {
            return null;
        }
        // 调用分治合并方法
        return merge(lists, 0, lists.length - 1);
    }
    
    /**
     * 分治合并：将链表数组从 left 到 right 进行合并
     * 
     * @param lists 链表数组
     * @param left 左边界
     * @param right 右边界
     * @return 合并后的链表
     */
    private ListNode merge(ListNode[] lists, int left, int right) {
        // 递归终止条件：只有一个链表时，直接返回
        if (left == right) {
            return lists[left];
        }
        // 无效区间，返回 null
        if (left > right) {
            return null;
        }
        
        // 计算中间位置（防止溢出的写法）
        int mid = left + (right - left) / 2;
        
        // 递归合并左半部分
        ListNode leftList = merge(lists, left, mid);
        
        // 递归合并右半部分
        ListNode rightList = merge(lists, mid + 1, right);
        
        // 合并左右两部分
        return mergeTwoLists(leftList, rightList);
    }

    public static void main(String[] args) {
        MergeKSortedLists solution = new MergeKSortedLists();
        
        System.out.println("========== 测试用例 1：正常合并 ==========");
        ListNode[] lists1 = new ListNode[]{
            new ListNode(1, new ListNode(4, new ListNode(5))),
            new ListNode(1, new ListNode(3, new ListNode(4))),
            new ListNode(2, new ListNode(6))
        };
        ListNode result1 = solution.mergeKLists(lists1);
        printList(result1); // 期望输出：1->1->2->3->4->4->5->6
        
        System.out.println("\n========== 测试用例 2：空数组 ==========");
        ListNode[] lists2 = new ListNode[]{};
        ListNode result2 = solution.mergeKLists(lists2);
        System.out.println("空数组结果：" + result2); // 期望输出：null
        
        System.out.println("\n========== 测试用例 3：单个链表 ==========");
        ListNode[] lists3 = new ListNode[]{new ListNode(1, new ListNode(2, new ListNode(3)))};
        ListNode result3 = solution.mergeKLists(lists3);
        printList(result3); // 期望输出：1->2->3
        
        System.out.println("\n========== 测试用例 4：包含空链表 ==========");
        ListNode[] lists4 = new ListNode[]{
            new ListNode(1, new ListNode(2)),
            null,
            new ListNode(3, new ListNode(4))
        };
        ListNode result4 = solution.mergeKLists(lists4);
        printList(result4); // 期望输出：1->2->3->4
        
        System.out.println("\n========== 测试用例 5：分治合并 ==========");
        ListNode result5 = solution.mergeKListsDivideAndConquer(lists1);
        printList(result5); // 期望输出：1->1->2->3->4->4->5->6
        
        System.out.println("\n========== 测试用例 6：最小堆合并 ==========");
        ListNode result6 = solution.mergeKListsHeap(lists1);
        printList(result6); // 期望输出：1->1->2->3->4->4->5->6
        
        System.out.println("\n========== 测试用例 7：最小堆 - 空数组 ==========");
        ListNode result7 = solution.mergeKListsHeap(lists2);
        System.out.println("空数组结果：" + result7); // 期望输出：null
    }
    
    /**
     * 辅助方法：打印链表
     * 
     * @param head 链表头节点
     */
    private static void printList(ListNode head) {
        StringBuilder sb = new StringBuilder();
        ListNode current = head;
        while (current != null) {
            sb.append(current.val);
            if (current.next != null) {
                sb.append("->");
            }
            current = current.next;
        }
        System.out.println(sb.toString());
    }
}