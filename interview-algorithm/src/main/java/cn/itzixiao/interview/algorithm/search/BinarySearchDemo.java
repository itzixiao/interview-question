package cn.itzixiao.interview.algorithm.search;

/**
 * 二分查找详解 - 面试必考
 * 
 * <pre>
 * 时间复杂度：O(log n)
 * 空间复杂度：O(1)
 * 
 * 前提条件：
 * 1. 数组必须是有序的
 * 2. 支持随机访问（数组）
 * </pre>
 * 
 * @author itzixiao
 */
public class BinarySearchDemo {
    
    /**
     * 二分查找 - 迭代实现
     * @param arr 有序数组
     * @param target 目标值
     * @return 目标值索引，不存在返回 -1
     */
    public static int binarySearch(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;
        
        while (left <= right) {
            // 防止溢出的 mid 计算方式
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return -1;
    }
    
    /**
     * 二分查找 - 递归实现
     */
    public static int binarySearchRecursive(int[] arr, int target, int left, int right) {
        if (left > right) {
            return -1;
        }
        
        int mid = left + (right - left) / 2;
        
        if (arr[mid] == target) {
            return mid;
        } else if (arr[mid] < target) {
            return binarySearchRecursive(arr, target, mid + 1, right);
        } else {
            return binarySearchRecursive(arr, target, left, mid - 1);
        }
    }
    
    /**
     * 查找第一个等于目标值的位置
     */
    public static int findFirst(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;
        int result = -1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                result = mid;
                right = mid - 1;  // 继续在左边找
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return result;
    }
    
    /**
     * 查找最后一个等于目标值的位置
     */
    public static int findLast(int[] arr, int target) {
        int left = 0;
        int right = arr.length - 1;
        int result = -1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (arr[mid] == target) {
                result = mid;
                left = mid + 1;  // 继续在右边找
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        return result;
    }
    
    /**
     * 测试二分查找
     */
    public static void main(String[] args) {
        int[] arr = {1, 3, 5, 7, 9, 11, 13, 15, 17, 19};
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              二分查找演示                               ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");
        
        System.out.println("有序数组：" + java.util.Arrays.toString(arr));
        
        // 普通查找
        int target = 11;
        int index = binarySearch(arr, target);
        System.out.println("\n查找 " + target + "，索引：" + index);
        
        // 查找重复元素
        int[] arr2 = {1, 3, 5, 7, 7, 7, 9, 11};
        System.out.println("\n含重复元素的数组：" + java.util.Arrays.toString(arr2));
        System.out.println("第一个 7 的位置：" + findFirst(arr2, 7));
        System.out.println("最后一个 7 的位置：" + findLast(arr2, 7));
        
        System.out.println("\n【算法要点】");
        System.out.println("1. 每次将搜索范围缩小一半");
        System.out.println("2. mid 计算防止溢出：left + (right-left)/2");
        System.out.println("3. 循环条件：left <= right");
        System.out.println("4. 边界更新：mid±1（因为 mid 已经检查过）");
        
        System.out.println("\n【面试变体】");
        System.out.println("1. 查找第一个等于目标值的位置");
        System.out.println("2. 查找最后一个等于目标值的位置");
        System.out.println("3. 查找第一个大于等于目标值的位置");
        System.out.println("4. 查找最后一个小于等于目标值的位置");
        System.out.println("5. 旋转有序数组的二分查找");
        System.out.println("6. 寻找峰值元素");
    }
}
