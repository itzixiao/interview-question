package cn.itzixiao.interview.algorithm.compression;

import java.util.*;

/**
 * 霍夫曼编码（Huffman Coding）详解与实现
 *
 * <p>霍夫曼编码由David A. Huffman于1952年提出，是一种经典的数据压缩算法。
 * 核心思想：根据字符出现频率构建最优前缀编码树，高频字符用短编码，低频字符用长编码。
 *
 * <p>前缀编码（Prefix Code）：任何字符的编码都不是其他字符编码的前缀，
 * 确保解码时不会产生歧义。
 *
 * <p>算法步骤：
 * 1. 统计字符频率
 * 2. 构建霍夫曼树：每次选择频率最小的两个节点合并
 * 3. 生成编码：左分支为0，右分支为1
 * 4. 编码数据：用生成的编码替换原始字符
 * 5. 解码数据：根据霍夫曼树遍历解码
 *
 * <p>时间复杂度：O(n log n)，n为字符种类数
 * <p>空间复杂度：O(n)
 * <p>压缩率：通常在20%-90%之间，取决于数据分布
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class HuffmanCoding {

    /**
     * 霍夫曼树节点
     */
    static class HuffmanNode implements Comparable<HuffmanNode> {
        char ch;           // 字符
        int freq;          // 频率
        HuffmanNode left;  // 左子节点
        HuffmanNode right; // 右子节点

        // 叶子节点构造函数
        HuffmanNode(char ch, int freq) {
            this.ch = ch;
            this.freq = freq;
        }

        // 内部节点构造函数
        HuffmanNode(int freq, HuffmanNode left, HuffmanNode right) {
            this.freq = freq;
            this.left = left;
            this.right = right;
        }

        // 是否为叶子节点
        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override
        public int compareTo(HuffmanNode other) {
            return this.freq - other.freq;
        }
    }

    /**
     * 编码结果类
     */
    static class HuffmanResult {
        String encodedData;                    // 编码后的二进制字符串
        Map<Character, String> encodingMap;    // 字符到编码的映射
        HuffmanNode root;                      // 霍夫曼树根节点
        int originalBits;                      // 原始比特数
        int compressedBits;                    // 压缩后比特数

        double getCompressionRatio() {
            return (1.0 - (double) compressedBits / originalBits) * 100;
        }
    }

    /**
     * 1. 统计字符频率
     *
     * @param text 输入文本
     * @return 字符频率映射
     */
    public static Map<Character, Integer> calculateFrequency(String text) {
        Map<Character, Integer> freqMap = new HashMap<>();
        for (char ch : text.toCharArray()) {
            freqMap.put(ch, freqMap.getOrDefault(ch, 0) + 1);
        }
        return freqMap;
    }

    /**
     * 2. 构建霍夫曼树
     *
     * <p>算法：
     * 1. 为每个字符创建叶子节点，加入优先队列
     * 2. 当队列中节点数 > 1：
     * a. 取出频率最小的两个节点
     * b. 创建新节点，频率为两者之和，作为它们的父节点
     * c. 将新节点加入队列
     * 3. 队列中最后一个节点即为根节点
     *
     * @param freqMap 字符频率映射
     * @return 霍夫曼树根节点
     */
    public static HuffmanNode buildHuffmanTree(Map<Character, Integer> freqMap) {
        if (freqMap.isEmpty()) return null;

        // 使用优先队列（最小堆）
        PriorityQueue<HuffmanNode> pq = new PriorityQueue<>();

        // 创建叶子节点
        for (Map.Entry<Character, Integer> entry : freqMap.entrySet()) {
            pq.offer(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        // 特殊情况：只有一个字符
        if (pq.size() == 1) {
            HuffmanNode single = pq.poll();
            return new HuffmanNode(single.freq, single, null);
        }

        // 构建霍夫曼树
        while (pq.size() > 1) {
            HuffmanNode left = pq.poll();
            HuffmanNode right = pq.poll();
            HuffmanNode parent = new HuffmanNode(left.freq + right.freq, left, right);
            pq.offer(parent);
        }

        return pq.poll();
    }

    /**
     * 3. 生成霍夫曼编码表
     *
     * <p>遍历霍夫曼树，左分支标记为0，右分支标记为1
     *
     * @param root 霍夫曼树根节点
     * @return 字符到编码的映射
     */
    public static Map<Character, String> buildEncodingMap(HuffmanNode root) {
        Map<Character, String> encodingMap = new HashMap<>();
        if (root == null) return encodingMap;

        // 特殊情况：只有一个节点
        if (root.isLeaf()) {
            encodingMap.put(root.ch, "0");
            return encodingMap;
        }

        buildEncodingMapHelper(root, "", encodingMap);
        return encodingMap;
    }

    private static void buildEncodingMapHelper(HuffmanNode node, String code,
                                               Map<Character, String> encodingMap) {
        if (node == null) return;

        if (node.isLeaf()) {
            encodingMap.put(node.ch, code);
            return;
        }

        buildEncodingMapHelper(node.left, code + "0", encodingMap);
        buildEncodingMapHelper(node.right, code + "1", encodingMap);
    }

    /**
     * 4. 编码文本
     *
     * @param text        原始文本
     * @param encodingMap 编码映射表
     * @return 编码后的二进制字符串
     */
    public static String encode(String text, Map<Character, String> encodingMap) {
        StringBuilder encoded = new StringBuilder();
        for (char ch : text.toCharArray()) {
            encoded.append(encodingMap.get(ch));
        }
        return encoded.toString();
    }

    /**
     * 5. 解码二进制字符串
     *
     * @param encodedData 编码后的二进制字符串
     * @param root        霍夫曼树根节点
     * @return 解码后的原始文本
     */
    public static String decode(String encodedData, HuffmanNode root) {
        if (root == null) return "";

        StringBuilder decoded = new StringBuilder();
        HuffmanNode current = root;

        for (char bit : encodedData.toCharArray()) {
            if (bit == '0') {
                current = current.left;
            } else {
                current = current.right;
            }

            if (current.isLeaf()) {
                decoded.append(current.ch);
                current = root;  // 回到根节点，开始解码下一个字符
            }
        }

        return decoded.toString();
    }

    /**
     * 6. 完整的霍夫曼压缩
     *
     * @param text 原始文本
     * @return 压缩结果
     */
    public static HuffmanResult compress(String text) {
        if (text == null || text.isEmpty()) {
            return new HuffmanResult();
        }

        HuffmanResult result = new HuffmanResult();

        // 统计频率
        Map<Character, Integer> freqMap = calculateFrequency(text);

        // 构建霍夫曼树
        result.root = buildHuffmanTree(freqMap);

        // 生成编码表
        result.encodingMap = buildEncodingMap(result.root);

        // 编码数据
        result.encodedData = encode(text, result.encodingMap);

        // 计算压缩率
        result.originalBits = text.length() * 8;  // 假设原始使用UTF-8（8位/字符）
        result.compressedBits = result.encodedData.length();

        return result;
    }

    /**
     * 7. 将二进制字符串转换为字节数组（实际存储）
     *
     * @param binaryString 二进制字符串
     * @return 字节数组
     */
    public static byte[] binaryStringToBytes(String binaryString) {
        // 补齐到8的倍数
        int padding = 8 - (binaryString.length() % 8);
        if (padding == 8) padding = 0;

        StringBuilder padded = new StringBuilder(binaryString);
        for (int i = 0; i < padding; i++) {
            padded.append('0');
        }

        byte[] bytes = new byte[padded.length() / 8];
        for (int i = 0; i < bytes.length; i++) {
            String byteStr = padded.substring(i * 8, (i + 1) * 8);
            bytes[i] = (byte) Integer.parseInt(byteStr, 2);
        }

        return bytes;
    }

    /**
     * 8. 将字节数组转换回二进制字符串
     *
     * @param bytes     字节数组
     * @param validBits 有效比特数
     * @return 二进制字符串
     */
    public static String bytesToBinaryString(byte[] bytes, int validBits) {
        StringBuilder binary = new StringBuilder();
        for (byte b : bytes) {
            String byteStr = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            binary.append(byteStr);
        }
        return binary.substring(0, validBits);
    }

    /**
     * 9. 打印霍夫曼树结构
     *
     * @param root 根节点
     */
    public static void printHuffmanTree(HuffmanNode root) {
        System.out.println("霍夫曼树结构:");
        printTreeHelper(root, "", true);
    }

    private static void printTreeHelper(HuffmanNode node, String prefix, boolean isLast) {
        if (node == null) return;

        System.out.print(prefix);
        System.out.print(isLast ? "└── " : "├── ");

        if (node.isLeaf()) {
            System.out.println("'" + node.ch + "' (freq=" + node.freq + ")");
        } else {
            System.out.println("* (freq=" + node.freq + ")");
        }

        String newPrefix = prefix + (isLast ? "    " : "│   ");
        if (node.left != null) {
            printTreeHelper(node.left, newPrefix, node.right == null);
        }
        if (node.right != null) {
            printTreeHelper(node.right, newPrefix, true);
        }
    }

    /**
     * 10. 打印编码表
     *
     * @param encodingMap 编码映射
     * @param freqMap     频率映射
     */
    public static void printEncodingTable(Map<Character, String> encodingMap,
                                          Map<Character, Integer> freqMap) {
        System.out.println("\n字符编码表:");
        System.out.println("字符 | 频率 | 编码 | 编码长度");
        System.out.println("-----|------|------|--------");

        // 按频率降序排序
        List<Map.Entry<Character, String>> entries = new ArrayList<>(encodingMap.entrySet());
        entries.sort((a, b) -> freqMap.get(b.getKey()) - freqMap.get(a.getKey()));

        int totalOriginalBits = 0;
        int totalCompressedBits = 0;

        for (Map.Entry<Character, String> entry : entries) {
            char ch = entry.getKey();
            String code = entry.getValue();
            int freq = freqMap.get(ch);
            totalOriginalBits += freq * 8;
            totalCompressedBits += freq * code.length();

            String displayChar = (ch == ' ') ? "空格" : (ch == '\n') ? "\\n" : String.valueOf(ch);
            System.out.printf(" %-4s| %4d | %-10s | %d%n",
                    displayChar, freq, code, code.length());
        }

        System.out.println("-----|------|------|--------");
        System.out.printf("总计 |      |      | %d → %d bits%n",
                totalOriginalBits, totalCompressedBits);
    }

    /**
     * 11. 自适应霍夫曼编码（动态更新）
     *
     * <p>在数据流中动态维护霍夫曼树，无需预先知道频率
     */
    static class AdaptiveHuffman {
        private Map<Character, Integer> freqMap;
        private HuffmanNode root;

        public AdaptiveHuffman() {
            freqMap = new HashMap<>();
        }

        public void update(char ch) {
            freqMap.put(ch, freqMap.getOrDefault(ch, 0) + 1);
            root = buildHuffmanTree(freqMap);
        }

        public String getCode(char ch) {
            if (root == null) return "";
            Map<Character, String> map = buildEncodingMap(root);
            return map.getOrDefault(ch, "");
        }
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 霍夫曼编码演示 ==========\n");

        // 1. 基本压缩演示
        System.out.println("1. 基本压缩演示:");
        String text = "this is an example of a huffman tree";
        System.out.println("   原始文本: \"" + text + "\"");
        System.out.println("   原始大小: " + (text.length() * 8) + " bits (UTF-8)");

        HuffmanResult result = compress(text);

        System.out.println("\n   编码后的二进制:");
        System.out.println("   " + result.encodedData.substring(0, Math.min(80, result.encodedData.length()))
                + (result.encodedData.length() > 80 ? "..." : ""));

        System.out.println("\n   压缩后大小: " + result.compressedBits + " bits");
        System.out.printf("   压缩率: %.2f%%%n", result.getCompressionRatio());

        // 2. 显示编码表
        System.out.println("\n2. 字符频率与编码:");
        Map<Character, Integer> freqMap = calculateFrequency(text);
        printEncodingTable(result.encodingMap, freqMap);

        // 3. 显示霍夫曼树
        System.out.println("\n3. 霍夫曼树结构:");
        printHuffmanTree(result.root);

        // 4. 解码验证
        System.out.println("\n4. 解码验证:");
        String decoded = decode(result.encodedData, result.root);
        System.out.println("   解码结果: \"" + decoded + "\"");
        System.out.println("   验证通过: " + text.equals(decoded));

        // 5. 不同文本类型的压缩效果
        System.out.println("\n5. 不同文本类型的压缩效果:");
        String[] testTexts = {
                "AAAAABBBBCCCDDE",  // 高度重复
                "abcdefgh",          // 均匀分布
                "The quick brown fox jumps over the lazy dog. " +
                        "The quick brown fox jumps over the lazy dog."
        };

        for (String test : testTexts) {
            HuffmanResult r = compress(test);
            System.out.printf("   \"%s...\" → 压缩率: %.2f%%%n",
                    test.substring(0, Math.min(30, test.length())),
                    r.getCompressionRatio());
        }

        // 6. 字节转换演示
        System.out.println("\n6. 字节数组转换演示:");
        byte[] bytes = binaryStringToBytes(result.encodedData);
        System.out.println("   二进制长度: " + result.encodedData.length() + " bits");
        System.out.println("   字节数组长度: " + bytes.length + " bytes");
        System.out.println("   实际压缩比: " + String.format("%.2f", (double) bytes.length / text.length() * 100) + "%");

        // 7. 自适应霍夫曼编码
        System.out.println("\n7. 自适应霍夫曼编码演示:");
        AdaptiveHuffman adaptive = new AdaptiveHuffman();
        String stream = "abracadabra";
        System.out.println("   数据流: " + stream);
        System.out.println("   动态编码过程:");
        for (char ch : stream.toCharArray()) {
            String code = adaptive.getCode(ch);
            System.out.printf("   字符 '%c' -> 编码: %s%n", ch,
                    code.isEmpty() ? "(新字符)" : code);
            adaptive.update(ch);
        }

        // 8. 边界情况测试
        System.out.println("\n8. 边界情况测试:");
        String[] edgeCases = {"", "A", "AA", "AB"};
        for (String edge : edgeCases) {
            HuffmanResult r = compress(edge);
            String dec = decode(r.encodedData, r.root);
            System.out.printf("   \"%s\" -> 编码: %s -> 解码: \"%s\" (验证: %s)%n",
                    edge, r.encodedData, dec, edge.equals(dec));
        }

        System.out.println("\n========== 演示结束 ==========");
    }
}
