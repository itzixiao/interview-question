package cn.itzixiao.interview.algorithm.dp;

import java.util.Arrays;

/**
 * 动态规划经典问题 - 面试高频考点
 * 
 * @author itzixiao
 */
public class DynamicProgrammingDemo {
    
    /**
     * 斐波那契数列
     * F(0) = 0, F(1) = 1, F(n) = F(n-1) + F(n-2)
     */
    public static int fibonacci(int n) {
        if (n <= 1) return n;
        
        int prev2 = 0;  // F(n-2)
        int prev1 = 1;  // F(n-1)
        int curr = 0;   // F(n)
        
        for (int i = 2; i <= n; i++) {
            curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        
        return curr;
    }
    
    /**
     * 爬楼梯问题
     * 每次可以爬 1 或 2 个台阶，问有多少种方法爬到楼顶
     */
    public static int climbStairs(int n) {
        if (n <= 2) return n;
        
        int prev2 = 1;  // 1 阶
        int prev1 = 2;  // 2 阶
        int curr = 0;   // n 阶
        
        for (int i = 3; i <= n; i++) {
            curr = prev1 + prev2;
            prev2 = prev1;
            prev1 = curr;
        }
        
        return curr;
    }
    
    /**
     * 最长上升子序列（LIS）
     * 给定一个无序的整数数组，找到其中最长上升子序列的长度
     */
    public static int lengthOfLIS(int[] nums) {
        if (nums.length == 0) return 0;
        
        int[] dp = new int[nums.length];
        Arrays.fill(dp, 1);
        
        int maxLen = 1;
        for (int i = 1; i < nums.length; i++) {
            for (int j = 0; j < i; j++) {
                if (nums[i] > nums[j]) {
                    dp[i] = Math.max(dp[i], dp[j] + 1);
                }
            }
            maxLen = Math.max(maxLen, dp[i]);
        }
        
        return maxLen;
    }
    
    /**
     * 0-1 背包问题
     * 有 N 个物品，每个物品重量为 w[i]，价值为 v[i]
     * 背包容量为 W，求能装入的最大价值
     */
    public static int knapsack(int[] weights, int[] values, int capacity) {
        int n = weights.length;
        int[][] dp = new int[n + 1][capacity + 1];
        
        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= capacity; w++) {
                // 不选第 i 个物品
                dp[i][w] = dp[i - 1][w];
                
                // 选第 i 个物品（如果装得下）
                if (w >= weights[i - 1]) {
                    dp[i][w] = Math.max(dp[i][w], 
                                       dp[i - 1][w - weights[i - 1]] + values[i - 1]);
                }
            }
        }
        
        return dp[n][capacity];
    }
    
    /**
     * 测试
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              动态规划演示                               ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        // 斐波那契数列
        System.out.println("斐波那契数列：");
        for (int i = 0; i <= 10; i++) {
            System.out.print("F(" + i + ")=" + fibonacci(i) + " ");
        }
        
        // 爬楼梯
        System.out.println("\n\n爬楼梯问题：");
        for (int i = 1; i <= 10; i++) {
            System.out.println(i + " 层楼梯：" + climbStairs(i) + " 种方法");
        }
        
        // 最长上升子序列
        System.out.println("\n最长上升子序列：");
        int[] nums = {10, 9, 2, 5, 3, 7, 101, 18};
        System.out.println("数组：" + java.util.Arrays.toString(nums));
        System.out.println("LIS 长度：" + lengthOfLIS(nums));
        
        // 0-1 背包
        System.out.println("\n0-1 背包问题：");
        int[] weights = {2, 3, 4, 5};
        int[] values = {3, 4, 5, 6};
        int capacity = 8;
        System.out.println("物品重量：" + java.util.Arrays.toString(weights));
        System.out.println("物品价值：" + java.util.Arrays.toString(values));
        System.out.println("背包容量：" + capacity);
        System.out.println("最大价值：" + knapsack(weights, values, capacity));
        
        System.out.println("\n【面试高频题】");
        System.out.println("1. 斐波那契数列（多种解法）");
        System.out.println("2. 爬楼梯问题");
        System.out.println("3. 最长上升子序列");
        System.out.println("4. 0-1 背包问题");
        System.out.println("5. 完全背包问题");
        System.out.println("6. 最小路径和");
        System.out.println("7. 不同路径");
        System.out.println("8. 编辑距离");
    }
}
