package cn.itzixiao.interview.algorithm.tree;

import java.util.*;

/**
 * 二叉树遍历 - 面试必考
 * 
 * @author itzixiao
 */
public class BinaryTreeTraversal {
    
    /**
     * 二叉树节点定义
     */
    static class TreeNode {
        int val;
        TreeNode left;
        TreeNode right;
        TreeNode(int x) { val = x; }
    }
    
    /**
     * 前序遍历 - 递归
     */
    public static void preorderRecursive(TreeNode root, List<Integer> result) {
        if (root == null) return;
        
        result.add(root.val);
        preorderRecursive(root.left, result);
        preorderRecursive(root.right, result);
    }
    
    /**
     * 前序遍历 - 迭代
     */
    public static List<Integer> preorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        if (root == null) return result;
        
        Stack<TreeNode> stack = new Stack<>();
        stack.push(root);
        
        while (!stack.isEmpty()) {
            TreeNode node = stack.pop();
            result.add(node.val);
            
            // 先右后左（因为栈是后进先出）
            if (node.right != null) stack.push(node.right);
            if (node.left != null) stack.push(node.left);
        }
        
        return result;
    }
    
    /**
     * 中序遍历 - 递归
     */
    public static void inorderRecursive(TreeNode root, List<Integer> result) {
        if (root == null) return;
        
        inorderRecursive(root.left, result);
        result.add(root.val);
        inorderRecursive(root.right, result);
    }
    
    /**
     * 中序遍历 - 迭代
     */
    public static List<Integer> inorderIterative(TreeNode root) {
        List<Integer> result = new ArrayList<>();
        Stack<TreeNode> stack = new Stack<>();
        TreeNode curr = root;
        
        while (curr != null || !stack.isEmpty()) {
            // 一直向左
            while (curr != null) {
                stack.push(curr);
                curr = curr.left;
            }
            
            curr = stack.pop();
            result.add(curr.val);
            curr = curr.right;
        }
        
        return result;
    }
    
    /**
     * 后序遍历 - 递归
     */
    public static void postorderRecursive(TreeNode root, List<Integer> result) {
        if (root == null) return;
        
        postorderRecursive(root.left, result);
        postorderRecursive(root.right, result);
        result.add(root.val);
    }
    
    /**
     * 层序遍历（BFS）
     */
    public static List<List<Integer>> levelOrder(TreeNode root) {
        List<List<Integer>> result = new ArrayList<>();
        if (root == null) return result;
        
        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);
        
        while (!queue.isEmpty()) {
            int size = queue.size();
            List<Integer> level = new ArrayList<>();
            
            for (int i = 0; i < size; i++) {
                TreeNode node = queue.poll();
                level.add(node.val);
                
                if (node.left != null) queue.offer(node.left);
                if (node.right != null) queue.offer(node.right);
            }
            
            result.add(level);
        }
        
        return result;
    }
    
    /**
     * 计算树的最大深度
     */
    public static int maxDepth(TreeNode root) {
        if (root == null) return 0;
        
        int leftDepth = maxDepth(root.left);
        int rightDepth = maxDepth(root.right);
        
        return Math.max(leftDepth, rightDepth) + 1;
    }
    
    /**
     * 验证二叉搜索树
     */
    public static boolean isValidBST(TreeNode root) {
        return isValidBST(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    
    private static boolean isValidBST(TreeNode node, long min, long max) {
        if (node == null) return true;
        
        if (node.val <= min || node.val >= max) {
            return false;
        }
        
        return isValidBST(node.left, min, node.val) && 
               isValidBST(node.right, node.val, max);
    }
    
    /**
     * 测试
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              二叉树遍历演示                             ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        // 构建树：
        //       1
        //      / \
        //     2   3
        //    / \   \
        //   4   5   6
        
        TreeNode root = new TreeNode(1);
        root.left = new TreeNode(2);
        root.right = new TreeNode(3);
        root.left.left = new TreeNode(4);
        root.left.right = new TreeNode(5);
        root.right.right = new TreeNode(6);
        
        List<Integer> result;
        
        // 前序遍历
        result = new ArrayList<>();
        preorderRecursive(root, result);
        System.out.println("前序遍历（递归）：" + result);
        System.out.println("前序遍历（迭代）：" + preorderIterative(root));
        
        // 中序遍历
        result = new ArrayList<>();
        inorderRecursive(root, result);
        System.out.println("\n中序遍历（递归）：" + result);
        System.out.println("中序遍历（迭代）：" + inorderIterative(root));
        
        // 后序遍历
        result = new ArrayList<>();
        postorderRecursive(root, result);
        System.out.println("\n后序遍历：" + result);
        
        // 层序遍历
        List<List<Integer>> levels = levelOrder(root);
        System.out.println("\n层序遍历：");
        for (int i = 0; i < levels.size(); i++) {
            System.out.println("第" + (i+1) + "层：" + levels.get(i));
        }
        
        System.out.println("\n树的最大深度：" + maxDepth(root));
        
        System.out.println("\n【面试高频题】");
        System.out.println("1. 三种遍历的递归和迭代实现");
        System.out.println("2. 层序遍历（BFS）");
        System.out.println("3. 树的最大深度");
        System.out.println("4. 验证二叉搜索树");
        System.out.println("5. 最近公共祖先");
        System.out.println("6. 路径总和");
    }
}
