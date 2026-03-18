package cn.itzixiao.interview.algorithm.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

/**
 * 哈希算法详解与实现
 *
 * <p>哈希算法（Hash Algorithm）是一种将任意长度的数据映射为固定长度数据的算法。
 * 核心特性：
 * 1. 确定性：相同输入始终产生相同输出
 * 2. 高效性：计算速度快
 * 3. 雪崩效应：微小输入变化导致输出巨大差异
 * 4. 抗碰撞性：难以找到两个不同输入产生相同输出
 *
 * <p>常见应用场景：
 * - 数据完整性校验
 * - 密码存储
 * - 数据分片（如分布式缓存）
 * - 数字签名
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class HashAlgorithm {

    /**
     * 1. 简单取模哈希（用于理解哈希基础原理）
     *
     * <p>原理：hash(key) = key % tableSize
     * 适用场景：简单的哈希表实现
     *
     * @param key       要哈希的键
     * @param tableSize 哈希表大小
     * @return 哈希值（索引位置）
     */
    public static int simpleModHash(int key, int tableSize) {
        // 处理负数情况
        return Math.abs(key % tableSize);
    }

    /**
     * 2. 字符串哈希 - DJB2算法
     *
     * <p>原理：hash = hash * 33 + c（对每个字符）
     * 特点：计算快速，分布均匀
     *
     * @param str 输入字符串
     * @return 哈希值
     */
    public static long djb2Hash(String str) {
        long hash = 5381;
        for (int i = 0; i < str.length(); i++) {
            // hash * 33 + c 的位运算优化版本
            hash = ((hash << 5) + hash) + str.charAt(i);
        }
        return hash;
    }

    /**
     * 3. 字符串哈希 - BKDR算法
     *
     * <p>原理：hash = hash * seed + c（种子通常取31、131、1313等）
     * 特点：经典字符串哈希，Java String的hashCode()基于此
     *
     * @param str 输入字符串
     * @return 哈希值
     */
    public static long bkdrHash(String str) {
        long hash = 0;
        long seed = 131; // 31, 131, 1313, 13131, 131313...
        for (int i = 0; i < str.length(); i++) {
            hash = hash * seed + str.charAt(i);
        }
        return hash & 0x7FFFFFFFFFFFFFFFL; // 确保为正数
    }

    /**
     * 4. Java标准hashCode实现原理演示
     *
     * <p>s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
     * 选择31的原因：
     * 1. 是质数，减少哈希冲突
     * 2. 31 * i = (i << 5) - i，可被优化为位运算
     *
     * @param str 输入字符串
     * @return 哈希值
     */
    public static int javaStringHashCode(String str) {
        int hash = 0;
        for (int i = 0; i < str.length(); i++) {
            hash = 31 * hash + str.charAt(i);
        }
        return hash;
    }

    /**
     * 5. CRC32循环冗余校验
     *
     * <p>原理：基于多项式除法的校验算法
     * 特点：计算快速，用于数据完整性校验
     *
     * @param data 输入数据
     * @return CRC32值
     */
    public static long crc32Hash(String data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data.getBytes(StandardCharsets.UTF_8));
        return crc32.getValue();
    }

    /**
     * 6. MD5哈希（Message Digest Algorithm 5）
     *
     * <p>原理：将任意长度数据压缩为128位（16字节）摘要
     * 注意：MD5已不再安全，仅用于教学或非安全场景
     *
     * @param input 输入字符串
     * @return MD5哈希值（32位十六进制字符串）
     */
    public static String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * 7. SHA-256哈希（Secure Hash Algorithm）
     *
     * <p>原理：将任意长度数据压缩为256位（32字节）摘要
     * 特点：目前安全的哈希算法，广泛用于区块链、数字签名
     *
     * @param input 输入字符串
     * @return SHA-256哈希值（64位十六进制字符串）
     */
    public static String sha256Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * 8. 双重哈希（解决哈希冲突）
     *
     * <p>原理：使用两个不同的哈希函数计算位置
     * hash1(key) + i * hash2(key)
     *
     * @param key       键值
     * @param tableSize 表大小
     * @param probe     探测次数
     * @return 最终位置
     */
    public static int doubleHash(String key, int tableSize, int probe) {
        int hash1 = Math.abs(key.hashCode() % tableSize);
        int hash2 = 1 + Math.abs(key.hashCode() % (tableSize - 1));
        return (hash1 + probe * hash2) % tableSize;
    }

    /**
     * 9. 布隆过滤器哈希（多哈希函数）
     *
     * <p>原理：使用多个哈希函数减少误判率
     * 特点：空间效率高，存在误判可能（可能误报，不会漏报）
     *
     * @param key     输入键
     * @param bitSize 位数组大小
     * @param hashNum 哈希函数数量
     * @return 哈希位置数组
     */
    public static int[] bloomFilterHashes(String key, int bitSize, int hashNum) {
        int[] positions = new int[hashNum];
        long hash1 = djb2Hash(key);
        long hash2 = bkdrHash(key);

        for (int i = 0; i < hashNum; i++) {
            // 使用两个基础哈希生成多个哈希值
            positions[i] = (int) Math.abs((hash1 + i * hash2) % bitSize);
        }
        return positions;
    }

    /**
     * 10. 密码学安全哈希（加盐哈希）
     *
     * <p>原理：原始数据 + 随机盐值 → 哈希
     * 目的：防止彩虹表攻击，相同密码产生不同哈希值
     *
     * @param password 原始密码
     * @param salt     随机盐值
     * @return 加盐后的哈希值
     */
    public static String saltedHash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String salted = password + salt;
            byte[] digest = md.digest(salted.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) {
        System.out.println("========== 哈希算法演示 ==========\n");

        // 1. 简单取模哈希
        System.out.println("1. 简单取模哈希:");
        int[] keys = {15, 28, 33, 42, 55};
        int tableSize = 10;
        for (int key : keys) {
            System.out.printf("   key=%d → hash=%d%n", key, simpleModHash(key, tableSize));
        }

        // 2. DJB2哈希
        System.out.println("\n2. DJB2字符串哈希:");
        String[] strings = {"hello", "world", "hash", "algorithm"};
        for (String s : strings) {
            System.out.printf("   \"%s\" → hash=%d%n", s, djb2Hash(s));
        }

        // 3. BKDR哈希
        System.out.println("\n3. BKDR字符串哈希:");
        for (String s : strings) {
            System.out.printf("   \"%s\" → hash=%d%n", s, bkdrHash(s));
        }

        // 4. Java hashCode对比
        System.out.println("\n4. Java String hashCode对比:");
        for (String s : strings) {
            System.out.printf("   \"%s\" → custom=%d, java=%d%n",
                    s, javaStringHashCode(s), s.hashCode());
        }

        // 5. CRC32
        System.out.println("\n5. CRC32校验:");
        String testData = "Hello, World!";
        System.out.printf("   \"%s\" → CRC32=%d%n", testData, crc32Hash(testData));

        // 6. MD5
        System.out.println("\n6. MD5哈希 (32位):");
        System.out.printf("   \"%s\" → MD5=%s%n", testData, md5Hash(testData));

        // 7. SHA-256
        System.out.println("\n7. SHA-256哈希 (64位):");
        System.out.printf("   \"%s\" → SHA256=%s%n", testData, sha256Hash(testData));

        // 8. 雪崩效应演示
        System.out.println("\n8. 雪崩效应演示 (微小变化导致输出巨大差异):");
        String original = "Hello";
        String modified = "hello"; // 仅首字母大小写变化
        System.out.printf("   \"%s\" → MD5=%s%n", original, md5Hash(original));
        System.out.printf("   \"%s\" → MD5=%s%n", modified, md5Hash(modified));

        // 9. 双重哈希
        System.out.println("\n9. 双重哈希 (解决冲突):");
        String key = "test";
        for (int i = 0; i < 3; i++) {
            System.out.printf("   probe=%d → position=%d%n", i, doubleHash(key, 100, i));
        }

        // 10. 布隆过滤器哈希
        System.out.println("\n10. 布隆过滤器多哈希:");
        int[] bloomPositions = bloomFilterHashes("example", 1000, 3);
        System.out.print("   位置: ");
        for (int pos : bloomPositions) {
            System.out.print(pos + " ");
        }
        System.out.println();

        // 11. 加盐哈希
        System.out.println("\n11. 加盐哈希 (密码存储):");
        String password = "myPassword123";
        String salt = "randomSalt456";
        System.out.printf("   密码: %s%n", password);
        System.out.printf("   盐值: %s%n", salt);
        System.out.printf("   结果: %s%n", saltedHash(password, salt));

        System.out.println("\n========== 演示结束 ==========");
    }
}
