package cn.itzixiao.interview.algorithm.string;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * KMP字符串匹配算法详解与实现
 *
 * <p>KMP算法（Knuth-Morris-Pratt）由Donald Knuth、Vaughan Pratt和James Morris于1977年联合发表。
 * 核心思想：利用已匹配的信息，避免主串指针回溯，将时间复杂度从O(m×n)优化到O(m+n)。
 *
 * <p>核心概念：
 * 1. 前缀（Prefix）：字符串开头到某个位置的所有子串
 * 2. 后缀（Suffix）：字符串某个位置到结尾的所有子串
 * 3. 最长相等前后缀（LPS - Longest Prefix Suffix）：字符串的最长相等前缀和后缀
 * 4. next数组（部分匹配表）：记录模式串每个位置的最长相等前后缀长度
 *
 * <p>算法步骤：
 * 1. 预处理模式串，构建next数组
 * 2. 使用next数组指导匹配过程，失配时跳到合适位置继续匹配
 *
 * <p>时间复杂度：O(m + n)，其中m为模式串长度，n为主串长度
 * 空间复杂度：O(m)，用于存储next数组
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class KMPAlgorithm {

    /**
     * 1. 构建next数组（标准KMP）
     *
     * <p>next[i]表示模式串pattern[0..i]的最长相等前后缀长度
     * 示例：pattern = "ABABC"
     * - "A": 前后缀为空，next[0] = 0
     * - "AB": 无相等前后缀，next[1] = 0
     * - "ABA": 前缀"A"=后缀"A"，next[2] = 1
     * - "ABAB": 前缀"AB"=后缀"AB"，next[3] = 2
     * - "ABABC": 无相等前后缀，next[4] = 0
     * 结果：next = [0, 0, 1, 2, 0]
     *
     * @param pattern 模式串
     * @return next数组
     */
    public static int[] buildNext(String pattern) {
        int m = pattern.length();
        int[] next = new int[m];
        next[0] = 0;  // 第一个字符的最长相等前后缀为0

        int j = 0;  // j表示当前最长相等前后缀长度
        for (int i = 1; i < m; i++) {
            // 当pattern[i] != pattern[j]时，需要回退j
            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                j = next[j - 1];  // 回退到前一个位置的next值
            }

            // 如果相等，最长相等前后缀长度+1
            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
            }

            next[i] = j;
        }

        return next;
    }

    /**
     * 2. 构建next数组（优化版本，避免主串和模式串相同字符时的无效比较）
     *
     * <p>优化点：当pattern[i] == pattern[next[i-1]]时，直接继承next[i-1]
     * 避免在匹配时重复比较相同字符
     *
     * @param pattern 模式串
     * @return 优化后的next数组
     */
    public static int[] buildNextOptimized(String pattern) {
        int m = pattern.length();
        int[] next = new int[m];
        next[0] = -1;  // 优化版本，第一个位置设为-1

        int i = 0, j = -1;
        while (i < m - 1) {
            if (j == -1 || pattern.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                // 优化：如果pattern[i] == pattern[j]，则next[i] = next[j]
                if (pattern.charAt(i) == pattern.charAt(j)) {
                    next[i] = next[j];
                } else {
                    next[i] = j;
                }
            } else {
                j = next[j];
            }
        }

        return next;
    }

    /**
     * 3. KMP字符串匹配（查找单个匹配位置）
     *
     * <p>算法流程：
     * 1. 预处理模式串，构建next数组
     * 2. 双指针遍历主串和模式串
     * 3. 匹配成功则同时前进
     * 4. 匹配失败时，模式串指针根据next数组回退，主串指针不动
     * 5. 模式串指针到达末尾，匹配成功
     *
     * @param text    主串
     * @param pattern 模式串
     * @return 匹配的起始位置，未找到返回-1
     */
    public static int kmpSearch(String text, String pattern) {
        if (pattern.isEmpty()) return 0;
        if (text.length() < pattern.length()) return -1;

        int[] next = buildNext(pattern);
        int i = 0;  // 主串指针
        int j = 0;  // 模式串指针

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;

                // 完全匹配
                if (j == pattern.length()) {
                    return i - j;  // 返回匹配起始位置
                }
            } else {
                // 失配处理
                if (j > 0) {
                    j = next[j - 1];  // 根据next数组回退
                } else {
                    i++;  // j=0且失配，主串指针前进
                }
            }
        }

        return -1;  // 未找到
    }

    /**
     * 4. KMP查找所有匹配位置
     *
     * @param text    主串
     * @param pattern 模式串
     * @return 所有匹配起始位置的列表
     */
    public static List<Integer> kmpSearchAll(String text, String pattern) {
        List<Integer> results = new ArrayList<>();
        if (pattern.isEmpty()) return results;

        int[] next = buildNext(pattern);
        int i = 0;
        int j = 0;

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;

                if (j == pattern.length()) {
                    results.add(i - j);  // 记录匹配位置
                    j = next[j - 1];     // 继续寻找下一个匹配（允许重叠）
                }
            } else {
                if (j > 0) {
                    j = next[j - 1];
                } else {
                    i++;
                }
            }
        }

        return results;
    }

    /**
     * 5. KMP查找所有匹配位置（不重叠）
     *
     * @param text    主串
     * @param pattern 模式串
     * @return 所有不重叠匹配起始位置的列表
     */
    public static List<Integer> kmpSearchAllNonOverlapping(String text, String pattern) {
        List<Integer> results = new ArrayList<>();
        if (pattern.isEmpty()) return results;

        int[] next = buildNext(pattern);
        int i = 0;
        int j = 0;

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;

                if (j == pattern.length()) {
                    results.add(i - j);
                    j = 0;  // 重置模式串指针（不重叠）
                }
            } else {
                if (j > 0) {
                    j = next[j - 1];
                } else {
                    i++;
                }
            }
        }

        return results;
    }

    /**
     * 6. 统计模式串在主串中的出现次数（允许重叠）
     *
     * @param text    主串
     * @param pattern 模式串
     * @return 出现次数
     */
    public static int countOccurrences(String text, String pattern) {
        return kmpSearchAll(text, pattern).size();
    }

    /**
     * 7. 统计模式串在主串中的出现次数（不重叠）
     *
     * @param text    主串
     * @param pattern 模式串
     * @return 出现次数
     */
    public static int countOccurrencesNonOverlapping(String text, String pattern) {
        return kmpSearchAllNonOverlapping(text, pattern).size();
    }

    /**
     * 8. 替换所有匹配的子串
     *
     * @param text        主串
     * @param pattern     要替换的模式串
     * @param replacement 替换内容
     * @return 替换后的字符串
     */
    public static String replaceAll(String text, String pattern, String replacement) {
        if (pattern.isEmpty()) return text;

        List<Integer> positions = kmpSearchAllNonOverlapping(text, pattern);
        if (positions.isEmpty()) return text;

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        for (int pos : positions) {
            result.append(text, lastEnd, pos);
            result.append(replacement);
            lastEnd = pos + pattern.length();
        }
        result.append(text.substring(lastEnd));

        return result.toString();
    }

    /**
     * 9. 多模式串匹配（AC自动机简化版）
     *
     * <p>同时对多个模式串进行匹配
     *
     * @param text     主串
     * @param patterns 多个模式串
     * @return 每个模式串的匹配位置
     */
    public static java.util.Map<String, List<Integer>> multiPatternSearch(
            String text, String[] patterns) {
        java.util.Map<String, List<Integer>> results = new java.util.HashMap<>();

        for (String pattern : patterns) {
            results.put(pattern, kmpSearchAll(text, pattern));
        }

        return results;
    }

    /**
     * 10. 可视化next数组构建过程
     *
     * @param pattern 模式串
     */
    public static void visualizeNextArray(String pattern) {
        System.out.println("构建next数组过程:");
        System.out.println("模式串: " + pattern);

        int m = pattern.length();
        int[] next = new int[m];
        next[0] = 0;

        System.out.println("next[" + 0 + "] = 0 (第一个字符)");

        int j = 0;
        for (int i = 1; i < m; i++) {
            System.out.println("\n处理位置 " + i + " (字符 '" + pattern.charAt(i) + "'):");

            while (j > 0 && pattern.charAt(i) != pattern.charAt(j)) {
                System.out.println("  不匹配，j从" + j + "回退到next[" + (j - 1) + "]=" + next[j - 1]);
                j = next[j - 1];
            }

            if (pattern.charAt(i) == pattern.charAt(j)) {
                j++;
                System.out.println("  匹配，j增加到" + j);
            } else {
                System.out.println("  不匹配且j=0，保持j=0");
            }

            next[i] = j;
            System.out.println("  next[" + i + "] = " + j);
        }

        System.out.println("\n最终next数组: " + Arrays.toString(next));
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== KMP字符串匹配算法演示 ==========\n");

        // 1. next数组构建演示
        System.out.println("1. next数组构建演示:");
        String pattern1 = "ABABC";
        visualizeNextArray(pattern1);

        // 2. 标准KMP匹配
        System.out.println("\n2. 标准KMP匹配:");
        String text = "ABABDABACDABABCABAB";
        String pattern = "ABABC";
        System.out.println("   主串: " + text);
        System.out.println("   模式: " + pattern);

        int[] next = buildNext(pattern);
        System.out.println("   next数组: " + Arrays.toString(next));

        int pos = kmpSearch(text, pattern);
        System.out.println("   匹配位置: " + pos);

        // 3. 查找所有匹配（允许重叠）
        System.out.println("\n3. 查找所有匹配（允许重叠）:");
        String text2 = "AAAAA";
        String pattern2 = "AA";
        List<Integer> allMatches = kmpSearchAll(text2, pattern2);
        System.out.println("   主串: " + text2);
        System.out.println("   模式: " + pattern2);
        System.out.println("   所有匹配位置: " + allMatches);
        System.out.println("   出现次数: " + countOccurrences(text2, pattern2));

        // 4. 查找所有匹配（不重叠）
        System.out.println("\n4. 查找所有匹配（不重叠）:");
        List<Integer> nonOverlapMatches = kmpSearchAllNonOverlapping(text2, pattern2);
        System.out.println("   不重叠匹配位置: " + nonOverlapMatches);
        System.out.println("   不重叠出现次数: " + countOccurrencesNonOverlapping(text2, pattern2));

        // 5. 替换功能
        System.out.println("\n5. 字符串替换:");
        String text3 = "The quick brown fox jumps over the lazy dog";
        String pattern3 = "o";
        String replaced = replaceAll(text3, pattern3, "0");
        System.out.println("   原字符串: " + text3);
        System.out.println("   替换 '" + pattern3 + "' 为 '0': " + replaced);

        // 6. 多模式串匹配
        System.out.println("\n6. 多模式串匹配:");
        String text4 = "ABABCDABABCABABD";
        String[] patterns = {"ABAB", "ABC", "ABD"};
        System.out.println("   主串: " + text4);
        System.out.println("   模式串: " + Arrays.toString(patterns));

        java.util.Map<String, List<Integer>> multiResults = multiPatternSearch(text4, patterns);
        for (java.util.Map.Entry<String, List<Integer>> entry : multiResults.entrySet()) {
            System.out.println("   模式 '" + entry.getKey() + "' 匹配位置: " + entry.getValue());
        }

        // 7. 性能对比：KMP vs 暴力匹配
        System.out.println("\n7. 性能对比（长文本匹配）:");
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            longText.append("ABCDEFGHIJ");
        }
        longText.append("PATTERN");
        String longPattern = "PATTERN";

        String lt = longText.toString();

        // KMP
        long start1 = System.currentTimeMillis();
        int kmpResult = kmpSearch(lt, longPattern);
        long time1 = System.currentTimeMillis() - start1;

        // 暴力匹配
        long start2 = System.currentTimeMillis();
        int bruteResult = lt.indexOf(longPattern);
        long time2 = System.currentTimeMillis() - start2;

        System.out.println("   文本长度: " + lt.length());
        System.out.println("   KMP结果: " + kmpResult + ", 耗时: " + time1 + "ms");
        System.out.println("   indexOf结果: " + bruteResult + ", 耗时: " + time2 + "ms");

        // 8. 特殊情况测试
        System.out.println("\n8. 特殊情况测试:");
        System.out.println("   空模式串匹配: " + kmpSearch("hello", ""));
        System.out.println("   模式串比主串长: " + kmpSearch("hi", "hello"));
        System.out.println("   单字符匹配: " + kmpSearch("abc", "b"));
        System.out.println("   无匹配: " + kmpSearch("abcdef", "xyz"));

        System.out.println("\n========== 演示结束 ==========");
    }
}
