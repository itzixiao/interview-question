package cn.itzixiao.interview.algorithm.crypto;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA加密算法详解与实现
 *
 * <p>RSA算法由Ron Rivest、Adi Shamir和Leonard Adleman于1977年提出，
 * 是第一个既能用于数据加密也能用于数字签名的非对称加密算法。
 *
 * <p>核心原理：基于大整数分解的数学难题
 * - 选择两个大质数 p 和 q
 * - 计算 n = p × q（模数）
 * - 计算欧拉函数 φ(n) = (p-1) × (q-1)
 * - 选择公钥指数 e，满足 1 < e < φ(n) 且 gcd(e, φ(n)) = 1
 * - 计算私钥指数 d，满足 d × e ≡ 1 (mod φ(n))
 *
 * <p>密钥对：
 * - 公钥：(e, n) - 用于加密和验证签名
 * - 私钥：(d, n) - 用于解密和生成签名
 *
 * <p>加密过程：c = m^e mod n
 * <p>解密过程：m = c^d mod n
 *
 * <p>安全性基础：从n分解出p和q在计算上不可行（大整数分解难题）
 *
 * <p>特点：
 * - 非对称加密：公钥加密，私钥解密
 * - 速度慢：比对称加密慢100-1000倍
 * - 适合加密小数据或加密对称密钥
 *
 * @author itzixiao
 * @since 2024-01-01
 */
public class RSAAlgorithm {

    /**
     * 1. 使用Java标准库生成RSA密钥对
     *
     * @param keySize 密钥长度（推荐2048或4096位）
     * @return KeyPair对象
     */
    public static KeyPair generateKeyPair(int keySize) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(keySize);
        return keyGen.generateKeyPair();
    }

    /**
     * 2. RSA加密（使用公钥）
     *
     * @param plainText 明文
     * @param publicKey 公钥
     * @return Base64编码的密文
     */
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 3. RSA解密（使用私钥）
     *
     * @param cipherText Base64编码的密文
     * @param privateKey 私钥
     * @return 明文
     */
    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 4. RSA数字签名（使用私钥签名）
     *
     * @param message    消息
     * @param privateKey 私钥
     * @return Base64编码的签名
     */
    public static String sign(String message, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    /**
     * 5. RSA签名验证（使用公钥验证）
     *
     * @param message   原始消息
     * @param signature Base64编码的签名
     * @param publicKey 公钥
     * @return 验证结果
     */
    public static boolean verify(String message, String signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(message.getBytes(StandardCharsets.UTF_8));
        return sig.verify(Base64.getDecoder().decode(signature));
    }

    // ==================== 手动实现RSA（教学用） ====================

    /**
     * RSA密钥参数类（手动实现用）
     */
    static class RSAKey {
        BigInteger n;  // 模数
        BigInteger e;  // 公钥指数
        BigInteger d;  // 私钥指数

        RSAKey(BigInteger n, BigInteger e, BigInteger d) {
            this.n = n;
            this.e = e;
            this.d = d;
        }
    }

    /**
     * 6. 手动实现RSA密钥生成（教学演示）
     *
     * <p>步骤：
     * 1. 选择两个大质数 p 和 q
     * 2. 计算 n = p × q
     * 3. 计算 φ(n) = (p-1) × (q-1)
     * 4. 选择 e（通常取65537）
     * 5. 计算 d = e^(-1) mod φ(n)
     *
     * @param bitLength 密钥位数
     * @return RSAKey对象
     */
    public static RSAKey generateRSAKeysManual(int bitLength) {
        // 1. 生成两个大质数（实际应用中需要更大）
        int primeBits = bitLength / 2;
        BigInteger p = BigInteger.probablePrime(primeBits, new java.security.SecureRandom());
        BigInteger q = BigInteger.probablePrime(primeBits, new java.security.SecureRandom());

        // 2. 计算 n = p × q
        BigInteger n = p.multiply(q);

        // 3. 计算欧拉函数 φ(n) = (p-1) × (q-1)
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

        // 4. 选择公钥指数 e（通常取65537，即0x10001）
        BigInteger e = new BigInteger("65537");

        // 确保 e 和 φ(n) 互质
        while (!e.gcd(phi).equals(BigInteger.ONE)) {
            e = e.add(new BigInteger("2"));
        }

        // 5. 计算私钥指数 d = e^(-1) mod φ(n)
        BigInteger d = e.modInverse(phi);

        return new RSAKey(n, e, d);
    }

    /**
     * 7. 手动实现RSA加密（教学演示）
     *
     * <p>加密公式：c = m^e mod n
     *
     * @param message 明文（数字形式）
     * @param key     RSA密钥
     * @return 密文
     */
    public static BigInteger encryptManual(BigInteger message, RSAKey key) {
        // 确保消息小于模数
        if (message.compareTo(key.n) >= 0) {
            throw new IllegalArgumentException("Message too large for key size");
        }
        return message.modPow(key.e, key.n);
    }

    /**
     * 8. 手动实现RSA解密（教学演示）
     *
     * <p>解密公式：m = c^d mod n
     *
     * @param cipher 密文
     * @param key    RSA密钥
     * @return 明文
     */
    public static BigInteger decryptManual(BigInteger cipher, RSAKey key) {
        return cipher.modPow(key.d, key.n);
    }

    /**
     * 9. 使用中国剩余定理(CRT)加速解密
     *
     * <p>原理：利用 p 和 q 分别计算，再通过CRT合并
     * 速度比直接计算快约4倍
     *
     * @param cipher 密文
     * @param p      质数p
     * @param q      质数q
     * @param d      私钥指数
     * @param n      模数
     * @return 明文
     */
    public static BigInteger decryptWithCRT(BigInteger cipher,
                                            BigInteger p, BigInteger q,
                                            BigInteger d, BigInteger n) {
        // 计算 dp = d mod (p-1), dq = d mod (q-1)
        BigInteger dp = d.mod(p.subtract(BigInteger.ONE));
        BigInteger dq = d.mod(q.subtract(BigInteger.ONE));

        // 计算 qInv = q^(-1) mod p
        BigInteger qInv = q.modInverse(p);

        // 计算 m1 = c^dp mod p, m2 = c^dq mod q
        BigInteger m1 = cipher.modPow(dp, p);
        BigInteger m2 = cipher.modPow(dq, q);

        // CRT合并
        BigInteger h = qInv.multiply(m1.subtract(m2)).mod(p);
        return m2.add(h.multiply(q));
    }

    /**
     * 10. 密钥序列化与反序列化
     */
    public static class KeySerializer {

        /**
         * 公钥转Base64字符串
         */
        public static String publicKeyToBase64(PublicKey publicKey) {
            return Base64.getEncoder().encodeToString(publicKey.getEncoded());
        }

        /**
         * Base64字符串转公钥
         */
        public static PublicKey base64ToPublicKey(String base64Key) throws Exception {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(spec);
        }

        /**
         * 私钥转Base64字符串
         */
        public static String privateKeyToBase64(PrivateKey privateKey) {
            return Base64.getEncoder().encodeToString(privateKey.getEncoded());
        }

        /**
         * Base64字符串转私钥
         */
        public static PrivateKey base64ToPrivateKey(String base64Key) throws Exception {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(spec);
        }
    }

    /**
     * 11. RSA-OAEP加密（更安全的填充方案）
     *
     * <p>OAEP（Optimal Asymmetric Encryption Padding）是RSA推荐的安全填充方案
     */
    public static String encryptOAEP(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * 12. RSA-OAEP解密
     */
    public static String decryptOAEP(String cipherText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // ==================== 演示与测试 ====================

    public static void main(String[] args) throws Exception {
        System.out.println("========== RSA加密算法演示 ==========\n");

        // 1. 生成密钥对
        System.out.println("1. 生成RSA密钥对 (2048位):");
        KeyPair keyPair = generateKeyPair(2048);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        System.out.println("   公钥算法: " + publicKey.getAlgorithm());
        System.out.println("   公钥格式: " + publicKey.getFormat());
        System.out.println("   私钥算法: " + privateKey.getAlgorithm());
        System.out.println("   私钥格式: " + privateKey.getFormat());

        // 2. 加密解密演示
        System.out.println("\n2. RSA加密解密演示:");
        String originalMessage = "Hello, RSA Encryption!";
        System.out.println("   原始消息: " + originalMessage);

        String encrypted = encrypt(originalMessage, publicKey);
        System.out.println("   加密后 (Base64): " + encrypted.substring(0, 50) + "...");

        String decrypted = decrypt(encrypted, privateKey);
        System.out.println("   解密后: " + decrypted);
        System.out.println("   验证: " + originalMessage.equals(decrypted));

        // 3. 数字签名演示
        System.out.println("\n3. RSA数字签名演示:");
        String document = "这是一份重要合同，需要数字签名确认。";
        System.out.println("   原始文档: " + document);

        String signature = sign(document, privateKey);
        System.out.println("   数字签名 (Base64): " + signature.substring(0, 50) + "...");

        boolean isValid = verify(document, signature, publicKey);
        System.out.println("   签名验证结果: " + isValid);

        // 验证篡改后的签名
        String tamperedDocument = "这是一份被篡改的合同。";
        boolean isTamperedValid = verify(tamperedDocument, signature, publicKey);
        System.out.println("   篡改后验证结果: " + isTamperedValid);

        // 4. 手动实现RSA演示（小数字）
        System.out.println("\n4. 手动实现RSA演示（小数字教学用）:");
        // 使用小质数便于理解
        BigInteger p = new BigInteger("61");
        BigInteger q = new BigInteger("53");
        BigInteger n = p.multiply(q);  // 3233
        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));  // 3120
        BigInteger e = new BigInteger("17");  // 公钥指数
        BigInteger d = e.modInverse(phi);     // 私钥指数 = 2753

        System.out.println("   p = " + p);
        System.out.println("   q = " + q);
        System.out.println("   n = p × q = " + n);
        System.out.println("   φ(n) = (p-1) × (q-1) = " + phi);
        System.out.println("   e = " + e);
        System.out.println("   d = e^(-1) mod φ(n) = " + d);

        BigInteger message = new BigInteger("123");
        System.out.println("   明文 m = " + message);

        BigInteger cipher = message.modPow(e, n);
        System.out.println("   密文 c = m^e mod n = " + cipher);

        BigInteger decryptedMessage = cipher.modPow(d, n);
        System.out.println("   解密 m = c^d mod n = " + decryptedMessage);

        // 5. 密钥序列化演示
        System.out.println("\n5. 密钥序列化演示:");
        String pubKeyBase64 = KeySerializer.publicKeyToBase64(publicKey);
        String priKeyBase64 = KeySerializer.privateKeyToBase64(privateKey);
        System.out.println("   公钥 (Base64, 前100字符): " + pubKeyBase64.substring(0, 100) + "...");
        System.out.println("   私钥 (Base64, 前100字符): " + priKeyBase64.substring(0, 100) + "...");

        // 反序列化并验证
        PublicKey restoredPubKey = KeySerializer.base64ToPublicKey(pubKeyBase64);
        PrivateKey restoredPriKey = KeySerializer.base64ToPrivateKey(priKeyBase64);
        String testEncrypt = encrypt("Test", restoredPubKey);
        String testDecrypt = decrypt(testEncrypt, restoredPriKey);
        System.out.println("   反序列化验证: " + "Test".equals(testDecrypt));

        // 6. OAEP加密演示
        System.out.println("\n6. RSA-OAEP加密演示（更安全）:");
        String sensitiveData = "敏感信息：密码123456";
        String oaepEncrypted = encryptOAEP(sensitiveData, publicKey);
        String oaepDecrypted = decryptOAEP(oaepEncrypted, privateKey);
        System.out.println("   原始数据: " + sensitiveData);
        System.out.println("   OAEP加密后: " + oaepEncrypted.substring(0, 50) + "...");
        System.out.println("   OAEP解密后: " + oaepDecrypted);

        // 7. 性能测试
        System.out.println("\n7. 性能测试（加密100次）:");
        String testMessage = "Performance Test Message";
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            String enc = encrypt(testMessage, publicKey);
            decrypt(enc, privateKey);
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("   100次加密解密耗时: " + duration + "ms");
        System.out.println("   平均每次: " + (duration / 100.0) + "ms");

        // 8. 密钥长度对比
        System.out.println("\n8. 不同密钥长度加密结果大小对比:");
        for (int keyLen : new int[]{1024, 2048, 4096}) {
            KeyPair kp = generateKeyPair(keyLen);
            String enc = encrypt("Test", kp.getPublic());
            System.out.println("   " + keyLen + "位密钥，密文长度: " + enc.length() + "字符");
        }

        System.out.println("\n========== 演示结束 ==========");
    }
}
