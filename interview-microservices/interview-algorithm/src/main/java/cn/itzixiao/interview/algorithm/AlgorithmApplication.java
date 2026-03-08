package cn.itzixiao.interview.algorithm;

/**
 * 算法面试题库入口类
 * 
 * 本模块包含常见算法面试题及实现：
 * 1. 排序算法：快速排序、归并排序、堆排序等
 * 2. 查找算法：二分查找、DFS、BFS 等
 * 3. 链表操作：反转、合并、环检测等
 * 4. 树形结构：遍历、BST、AVL 树等
 * 5. 动态规划：背包问题、最长子序列等
 * 6. 回溯法：全排列、N 皇后等
 * 7. 贪心算法：区间问题等
 * 8. 滑动窗口：子串问题等
 */
public class AlgorithmApplication {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("    算法面试题库 - 示例代码演示");
        System.out.println("========================================\n");
        
        // 示例：快速排序
        int[] arr = {64, 34, 25, 12, 22, 11, 90};
        System.out.println("原始数组：" + java.util.Arrays.toString(arr));
        quickSort(arr, 0, arr.length - 1);
        System.out.println("快速排序后：" + java.util.Arrays.toString(arr));
    }
    
    /**
     * 快速排序
     * @param arr 待排序数组
     * @param low 起始索引
     * @param high 结束索引
     */
    public static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }
    
    /**
     * 分区操作
     * @param arr 数组
     * @param low 起始索引
     * @param high 结束索引
     * @return 分区点索引
     */
    private static int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = (low - 1);
        
        for (int j = low; j < high; j++) {
            if (arr[j] <= pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        
        return i + 1;
    }
}
