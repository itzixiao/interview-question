package cn.itzixiao.interview.algorithm.linkedlist;

/**
 * 链表操作 - 面试高频考点
 * 
 * @author itzixiao
 */
public class LinkedListDemo {
    
    /**
     * 链表节点定义
     */
    static class ListNode {
        int val;
        ListNode next;
        ListNode(int x) { val = x; }
    }
    
    /**
     * 反转链表 - 迭代法
     */
    public static ListNode reverseList(ListNode head) {
        ListNode prev = null;
        ListNode curr = head;
        
        while (curr != null) {
            ListNode nextTemp = curr.next;  // 保存下一个节点
            curr.next = prev;               // 指向前一个节点
            prev = curr;                    // 移动 prev
            curr = nextTemp;                // 移动 curr
        }
        
        return prev;
    }
    
    /**
     * 反转链表 - 递归法
     */
    public static ListNode reverseListRecursive(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        
        ListNode newHead = reverseListRecursive(head.next);
        head.next.next = head;
        head.next = null;
        
        return newHead;
    }
    
    /**
     * 合并两个有序链表
     */
    public static ListNode mergeTwoLists(ListNode l1, ListNode l2) {
        ListNode dummy = new ListNode(0);
        ListNode curr = dummy;
        
        while (l1 != null && l2 != null) {
            if (l1.val <= l2.val) {
                curr.next = l1;
                l1 = l1.next;
            } else {
                curr.next = l2;
                l2 = l2.next;
            }
            curr = curr.next;
        }
        
        curr.next = (l1 != null) ? l1 : l2;
        
        return dummy.next;
    }
    
    /**
     * 检测链表是否有环 - 快慢指针
     */
    public static boolean hasCycle(ListNode head) {
        if (head == null || head.next == null) {
            return false;
        }
        
        ListNode slow = head;
        ListNode fast = head.next;
        
        while (slow != fast) {
            if (fast == null || fast.next == null) {
                return false;
            }
            slow = slow.next;
            fast = fast.next.next;
        }
        
        return true;
    }
    
    /**
     * 找到链表的中间节点
     */
    public static ListNode middleNode(ListNode head) {
        ListNode slow = head;
        ListNode fast = head;
        
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        
        return slow;
    }
    
    /**
     * 删除链表的倒数第 N 个节点
     */
    public static ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0);
        dummy.next = head;
        
        ListNode first = dummy;
        ListNode second = dummy;
        
        // first 先走 n+1 步
        for (int i = 0; i <= n; i++) {
            first = first.next;
        }
        
        // 同时移动，直到 first 到达末尾
        while (first != null) {
            first = first.next;
            second = second.next;
        }
        
        // 删除倒数第 n 个节点
        second.next = second.next.next;
        
        return dummy.next;
    }
    
    /**
     * 打印链表
     */
    public static void printList(ListNode head) {
        ListNode curr = head;
        while (curr != null) {
            System.out.print(curr.val + " -> ");
            curr = curr.next;
        }
        System.out.println("null");
    }
    
    /**
     * 测试链表操作
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              链表操作演示                               ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        // 创建链表：1 -> 2 -> 3 -> 4 -> 5
        ListNode head = new ListNode(1);
        head.next = new ListNode(2);
        head.next.next = new ListNode(3);
        head.next.next.next = new ListNode(4);
        head.next.next.next.next = new ListNode(5);
        
        System.out.print("原始链表：");
        printList(head);
        
        // 反转链表
        ListNode reversed = reverseList(head);
        System.out.print("反转后：");
        printList(reversed);
        
        // 合并两个有序链表
        ListNode l1 = new ListNode(1);
        l1.next = new ListNode(3);
        l1.next.next = new ListNode(5);
        
        ListNode l2 = new ListNode(2);
        l2.next = new ListNode(4);
        l2.next.next = new ListNode(6);
        
        System.out.print("\n链表 1: ");
        printList(l1);
        System.out.print("链表 2: ");
        printList(l2);
        
        ListNode merged = mergeTwoLists(l1, l2);
        System.out.print("合并后：");
        printList(merged);
        
        System.out.println("\n【面试高频题】");
        System.out.println("1. 反转链表（迭代/递归）");
        System.out.println("2. 合并两个有序链表");
        System.out.println("3. 检测链表是否有环");
        System.out.println("4. 找到链表的中间节点");
        System.out.println("5. 删除倒数第 N 个节点");
        System.out.println("6. 判断两个链表是否相交");
        System.out.println("7. 环形链表入口点");
    }
}
