package cn.itzixiao.interview.algorithm.sort;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

/**
 * 快速排序算法详解与实现
 *
 * <p>快速排序（Quick Sort）是由东尼·霍尔（C. A. R. Hoare）于1962年提出的排序算法。
 * 核心思想：分治法（Divide and Conquer）
 * 1. 选择基准值（Pivot）
 * 2. 分区（Partition）：将数组分为小于和大于基准值的两部分
 * 3. 递归排序子数组
 *
 * <p>时间复杂度分析：
 * - 最优情况：O(n log n) - 每次分区平衡
 * - 平均情况：O(n log n)
 * - 最坏情况：O(n²) - 每次分区极不平衡（如已排序数组）
 *
 * <p>空间复杂度：O(log n) - 递归栈空间
 *
 * <p>稳定性：不稳定（相等元素可能交换位置）
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class QuickSort {

    private static final Random random = new Random();

    /**
     * 1. 基础快速排序
     *
     * <p>算法步骤：
     * 1. 选择最右元素作为基准值
     * 2. 遍历数组，将小于基准值的放左边，大于的放右边
     * 3. 将基准值放到正确位置
     * 4. 递归排序左右两部分
     *
     * @param arr   待排序数组
     * @param left  左边界
     * @param right 右边界
     */
    public static void quickSort(int[] arr, int left, int right) {
        if (left < right) {
            // 获取分区点
            int pivotIndex = partition(arr, left, right);
            // 递归排序左半部分
            quickSort(arr, left, pivotIndex - 1);
            // 递归排序右半部分
            quickSort(arr, pivotIndex + 1, right);
        }
    }

    /**
     * 分区函数（Lomuto分区方案）
     *
     * <p>原理：
     * - 选择最右元素作为基准值
     * - i 指向小于基准值的区域边界
     * - j 遍历数组
     * - 当 arr[j] < pivot 时，i++ 并交换 arr[i] 和 arr[j]
     * - 最后将基准值放到正确位置
     *
     * @param arr   数组
     * @param left  左边界
     * @param right 右边界
     * @return 基准值的最终位置
     */
    private static int partition(int[] arr, int left, int right) {
        int pivot = arr[right]; // 选择最右元素作为基准值
        int i = left - 1;       // i指向小于pivot的最后一个元素

        for (int j = left; j < right; j++) {
            if (arr[j] <= pivot) {
                i++;
                swap(arr, i, j);
            }
        }

        // 将基准值放到正确位置
        swap(arr, i + 1, right);
        return i + 1;
    }

    /**
     * 2. 随机化快速排序
     *
     * <p>优化点：随机选择基准值，避免最坏情况
     * 适用场景：防止针对特定输入的攻击或退化
     *
     * @param arr   待排序数组
     * @param left  左边界
     * @param right 右边界
     */
    public static void randomizedQuickSort(int[] arr, int left, int right) {
        if (left < right) {
            int pivotIndex = randomizedPartition(arr, left, right);
            randomizedQuickSort(arr, left, pivotIndex - 1);
            randomizedQuickSort(arr, pivotIndex + 1, right);
        }
    }

    /**
     * 随机化分区
     */
    private static int randomizedPartition(int[] arr, int left, int right) {
        // 随机选择一个位置与最右元素交换
        int randomIndex = left + random.nextInt(right - left + 1);
        swap(arr, randomIndex, right);
        return partition(arr, left, right);
    }

    /**
     * 3. 三数取中快速排序
     *
     * <p>优化点：选择左、中、右三个元素的中位数作为基准值
     * 优点：对于已排序或逆序数组表现更好
     *
     * @param arr   待排序数组
     * @param left  左边界
     * @param right 右边界
     */
    public static void medianOfThreeQuickSort(int[] arr, int left, int right) {
        if (left < right) {
            int pivotIndex = medianOfThreePartition(arr, left, right);
            medianOfThreeQuickSort(arr, left, pivotIndex - 1);
            medianOfThreeQuickSort(arr, pivotIndex + 1, right);
        }
    }

    /**
     * 三数取中分区
     */
    private static int medianOfThreePartition(int[] arr, int left, int right) {
        int mid = left + (right - left) / 2;

        // 将中位数放到最右位置
        if (arr[left] > arr[mid]) swap(arr, left, mid);
        if (arr[left] > arr[right]) swap(arr, left, right);
        if (arr[mid] > arr[right]) swap(arr, mid, right);

        // 将中位数与倒数第二个元素交换
        swap(arr, mid, right - 1);
        return partition(arr, left, right);
    }

    /**
     * 4. 双路快速排序
     *
     * <p>优化点：处理大量重复元素的情况
     * 原理：将数组分为 <= pivot 和 >= pivot 两部分
     * 避免重复元素全部集中在一侧导致的退化
     *
     * @param arr   待排序数组
     * @param left  左边界
     * @param right 右边界
     */
    public static void dualPivotQuickSort(int[] arr, int left, int right) {
        if (left < right) {
            int[] pivots = dualPartition(arr, left, right);
            dualPivotQuickSort(arr, left, pivots[0] - 1);
            dualPivotQuickSort(arr, pivots[0] + 1, pivots[1] - 1);
            dualPivotQuickSort(arr, pivots[1] + 1, right);
        }
    }

    /**
     * 双路分区（Hoare分区方案）
     */
    private static int[] dualPartition(int[] arr, int left, int right) {
        int pivot = arr[left + (right - left) / 2];
        int i = left, j = right;

        while (true) {
            while (arr[i] < pivot) i++;
            while (arr[j] > pivot) j--;
            if (i >= j) break;
            swap(arr, i, j);
            i++;
            j--;
        }

        return new int[]{i, j};
    }

    /**
     * 5. 三路快速排序（荷兰国旗问题）
     *
     * <p>优化点：专门处理包含大量重复元素的情况
     * 将数组分为三部分：< pivot, = pivot, > pivot
     * 等于pivot的部分不需要再递归排序
     *
     * @param arr   待排序数组
     * @param left  左边界
     * @param right 右边界
     */
    public static void threeWayQuickSort(int[] arr, int left, int right) {
        if (left >= right) return;

        // 随机选择基准值
        int randomIndex = left + random.nextInt(right - left + 1);
        swap(arr, left, randomIndex);

        int pivot = arr[left];
        int lt = left;      // arr[left..lt] < pivot
        int gt = right;     // arr[gt..right] > pivot
        int i = left + 1;   // arr[lt+1..i-1] = pivot

        while (i <= gt) {
            if (arr[i] < pivot) {
                swap(arr, i, lt + 1);
                lt++;
                i++;
            } else if (arr[i] > pivot) {
                swap(arr, i, gt);
                gt--;
            } else {
                i++;
            }
        }
        swap(arr, left, lt);

        // 递归排序小于和大于的部分
        threeWayQuickSort(arr, left, lt - 1);
        threeWayQuickSort(arr, gt + 1, right);
    }

    /**
     * 6. 非递归快速排序（使用栈）
     *
     * <p>优化点：避免递归栈溢出，适合大规模数据
     * 使用显式栈代替递归调用
     *
     * @param arr 待排序数组
     */
    public static void iterativeQuickSort(int[] arr) {
        if (arr == null || arr.length <= 1) return;

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{0, arr.length - 1});

        while (!stack.isEmpty()) {
            int[] range = stack.pop();
            int left = range[0];
            int right = range[1];

            if (left < right) {
                int pivotIndex = partition(arr, left, right);

                // 先压入较大的子数组，后压入较小的（保证栈深度较小）
                if (pivotIndex - left > right - pivotIndex) {
                    stack.push(new int[]{left, pivotIndex - 1});
                    stack.push(new int[]{pivotIndex + 1, right});
                } else {
                    stack.push(new int[]{pivotIndex + 1, right});
                    stack.push(new int[]{left, pivotIndex - 1});
                }
            }
        }
    }

    /**
     * 7. 快速选择算法（求第K大/小元素）
     *
     * <p>应用：在O(n)平均时间复杂度内找到第K个元素
     * 原理：利用快速排序的分区思想，只递归包含目标的一侧
     *
     * @param arr   数组
     * @param left  左边界
     * @param right 右边界
     * @param k     要找的第k小（从1开始）
     * @return 第k小的元素
     */
    public static int quickSelect(int[] arr, int left, int right, int k) {
        if (left == right) return arr[left];

        int pivotIndex = randomizedPartition(arr, left, right);
        int rank = pivotIndex - left + 1;

        if (k == rank) {
            return arr[pivotIndex];
        } else if (k < rank) {
            return quickSelect(arr, left, pivotIndex - 1, k);
        } else {
            return quickSelect(arr, pivotIndex + 1, right, k - rank);
        }
    }

    /**
     * 交换数组元素
     */
    private static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 快速排序算法演示 ==========\n");

        // 1. 基础快速排序
        System.out.println("1. 基础快速排序:");
        int[] arr1 = {64, 34, 25, 12, 22, 11, 90};
        System.out.println("   原始: " + Arrays.toString(arr1));
        quickSort(arr1, 0, arr1.length - 1);
        System.out.println("   排序: " + Arrays.toString(arr1));

        // 2. 随机化快速排序
        System.out.println("\n2. 随机化快速排序:");
        int[] arr2 = {3, -1, 0, 5, -2};
        System.out.println("   原始: " + Arrays.toString(arr2));
        randomizedQuickSort(arr2, 0, arr2.length - 1);
        System.out.println("   排序: " + Arrays.toString(arr2));

        // 3. 三数取中快速排序
        System.out.println("\n3. 三数取中快速排序:");
        int[] arr3 = {1, 2, 3, 4, 5, 6, 7}; // 已排序数组
        System.out.println("   原始: " + Arrays.toString(arr3));
        medianOfThreeQuickSort(arr3, 0, arr3.length - 1);
        System.out.println("   排序: " + Arrays.toString(arr3));

        // 4. 三路快速排序（大量重复元素）
        System.out.println("\n4. 三路快速排序（处理重复元素）:");
        int[] arr4 = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5};
        System.out.println("   原始: " + Arrays.toString(arr4));
        threeWayQuickSort(arr4, 0, arr4.length - 1);
        System.out.println("   排序: " + Arrays.toString(arr4));

        // 5. 非递归快速排序
        System.out.println("\n5. 非递归快速排序:");
        int[] arr5 = {38, 27, 43, 3, 9, 82, 10};
        System.out.println("   原始: " + Arrays.toString(arr5));
        iterativeQuickSort(arr5);
        System.out.println("   排序: " + Arrays.toString(arr5));

        // 6. 快速选择（第K小元素）
        System.out.println("\n6. 快速选择算法:");
        int[] arr6 = {3, 2, 1, 5, 6, 4};
        System.out.println("   数组: " + Arrays.toString(arr6));
        int k = 2;
        int kthSmallest = quickSelect(arr6.clone(), 0, arr6.length - 1, k);
        System.out.printf("   第%d小的元素: %d%n", k, kthSmallest);

        // 7. 性能对比
        System.out.println("\n7. 性能测试（100000个随机数）:");
        int size = 100000;
        int[] testArr = new int[size];
        Random rand = new Random();
        for (int i = 0; i < size; i++) {
            testArr[i] = rand.nextInt(size);
        }

        // 基础快排
        int[] arrCopy1 = testArr.clone();
        long start1 = System.currentTimeMillis();
        quickSort(arrCopy1, 0, arrCopy1.length - 1);
        long time1 = System.currentTimeMillis() - start1;
        System.out.println("   基础快排: " + time1 + "ms");

        // 三路快排
        int[] arrCopy2 = testArr.clone();
        long start2 = System.currentTimeMillis();
        threeWayQuickSort(arrCopy2, 0, arrCopy2.length - 1);
        long time2 = System.currentTimeMillis() - start2;
        System.out.println("   三路快排: " + time2 + "ms");

        // 非递归快排
        int[] arrCopy3 = testArr.clone();
        long start3 = System.currentTimeMillis();
        iterativeQuickSort(arrCopy3);
        long time3 = System.currentTimeMillis() - start3;
        System.out.println("   非递归快排: " + time3 + "ms");

        System.out.println("\n========== 演示结束 ==========");
    }
}
