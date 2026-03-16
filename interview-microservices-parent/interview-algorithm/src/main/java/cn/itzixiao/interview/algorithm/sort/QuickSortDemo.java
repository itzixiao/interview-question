package cn.itzixiao.interview.algorithm.sort;

import java.util.Arrays;

/**
 * 快速排序详解 - 面试高频考点
 *
 * <pre>
 * 时间复杂度：
 *   - 最好：O(n log n)
 *   - 平均：O(n log n)
 *   - 最坏：O(n²)
 *
 * 空间复杂度：O(log n) - 递归栈
 *
 * 稳定性：不稳定
 * </pre>
 *
 * @author itzixiao
 */
public class QuickSortDemo {

    /**
     * 快速排序主方法
     */
    public static void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            // 找到分区点
            int pivotIndex = partition(arr, low, high);

            // 递归排序左右两部分
            quickSort(arr, low, pivotIndex - 1);
            quickSort(arr, pivotIndex + 1, high);
        }
    }

    /**
     * 分区操作
     *
     * @param arr  待排序数组
     * @param low  起始索引
     * @param high 结束索引
     * @return 分区点索引
     */
    private static int partition(int[] arr, int low, int high) {
        // 选择基准值（通常选第一个或最后一个）
        int pivot = arr[low];

        while (low < high) {
            // 从右向左找第一个小于 pivot 的数
            while (low < high && arr[high] >= pivot) {
                high--;
            }
            arr[low] = arr[high];

            // 从左向右找第一个大于 pivot 的数
            while (low < high && arr[low] <= pivot) {
                low++;
            }
            arr[high] = arr[low];
        }

        // 将 pivot 放到正确位置
        arr[low] = pivot;
        return low;
    }

    /**
     * 测试快速排序
     */
    public static void main(String[] args) {
        int[] arr = {64, 34, 25, 12, 22, 11, 90, 88, 76};

        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║              快速排序演示                               ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        System.out.println("原始数组：" + Arrays.toString(arr));

        quickSort(arr, 0, arr.length - 1);

        System.out.println("排序后：" + Arrays.toString(arr));

        System.out.println("\n【算法要点】");
        System.out.println("1. 分治思想：将大问题分解为小问题");
        System.out.println("2. 分区操作：选择一个基准，将数组分为两部分");
        System.out.println("3. 递归排序：对左右两部分继续排序");
        System.out.println("4. 原地排序：不需要额外空间");

        System.out.println("\n【面试考点】");
        System.out.println("1. 时间复杂度分析（最好/最坏/平均）");
        System.out.println("2. 空间复杂度（递归栈深度）");
        System.out.println("3. 稳定性（不稳定排序）");
        System.out.println("4. 优化方案（三数取中、小区间插入排序）");
    }
}
