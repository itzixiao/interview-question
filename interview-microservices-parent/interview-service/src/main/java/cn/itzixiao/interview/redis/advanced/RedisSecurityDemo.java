package cn.itzixiao.interview.redis.advanced;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 安全相关实现：分布式幂等、接口防重放、敏感数据加密
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  分布式幂等性                                                │
 * │  - 防止同一操作被重复执行                                    │
 * │  - 常用方案：Token机制、数据库唯一索引、Redis去重             │
 * ├─────────────────────────────────────────────────────────────┤
 * │  接口防重放                                                  │
 * │  - 防止请求被截获后重复发送                                  │
 * │  - 常用方案：时间戳+随机数+签名、Nonce机制                    │
 * ├─────────────────────────────────────────────────────────────┤
 * │  敏感数据加密                                                │
 * │  - 防止数据泄露                                              │
 * │  - 常用方案：对称加密(AES)、非对称加密(RSA)、哈希(BCrypt)     │
 * └─────────────────────────────────────────────────────────────┘
 */
public class RedisSecurityDemo {

    private StringRedisTemplate redisTemplate;

    // ==================== 1. 分布式幂等性实现 ====================

    /**
     * 方案1：Token 机制（推荐）
     *
     * 流程：
     * 1. 客户端调用接口前先申请 Token
     * 2. 服务端生成 Token 存入 Redis，返回给客户端
     * 3. 客户端携带 Token 执行业务操作
     * 4. 服务端使用 Redis 删除 Token（原子操作）
     * 5. 删除成功则执行业务，失败则认为是重复请求
     */
    public static class IdempotentTokenService {

        private StringRedisTemplate redisTemplate;
        private static final String TOKEN_PREFIX = "idempotent:token:";
        private static final long TOKEN_EXPIRE_MINUTES = 10;

        /**
         * 申请幂等 Token
         */
        public String generateToken(String userId) {
            String token = UUID.randomUUID().toString().replace("-", "");
            String key = TOKEN_PREFIX + token;

            // Token 存入 Redis，设置过期时间
            redisTemplate.opsForValue().set(key, userId, TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);

            return token;
        }

        /**
         * 验证并消费 Token（保证幂等性）
         *
         * 使用 Lua 脚本保证原子性：
         * - 查询 Token 是否存在
         * - 存在则删除并返回成功
         * - 不存在则返回失败
         */
        public boolean verifyAndConsumeToken(String token) {
            String key = TOKEN_PREFIX + token;

            // Lua 脚本：原子性查询并删除
            String luaScript =
                    "if redis.call('get', KEYS[1]) then " +
                            "   redis.call('del', KEYS[1]) " +
                            "   return 1 " +
                            "else " +
                            "   return 0 " +
                            "end";

            org.springframework.data.redis.core.script.DefaultRedisScript<Long> redisScript =
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>();
            redisScript.setScriptText(luaScript);
            redisScript.setResultType(Long.class);

            Long result = redisTemplate.execute(redisScript,
                    java.util.Collections.singletonList(key));

            return result != null && result == 1;
        }

        /**
         * 幂等性注解使用示例
         */
        public void createOrder(String userId, String token, OrderRequest request) {
            // 1. 验证 Token
            if (!verifyAndConsumeToken(token)) {
                throw new RuntimeException("重复请求或Token无效");
            }

            // 2. 执行业务（创建订单）
            System.out.println("【创建订单】用户: " + userId + ", 订单: " + request.getOrderNo());

            // Token 已删除，同一 Token 再次请求会失败，保证幂等性
        }
    }

    /**
     * 方案2：请求指纹去重
     *
     * 适用于：接口无 Token 参数，通过请求内容生成唯一标识
     */
    public static class RequestFingerprintService {

        private StringRedisTemplate redisTemplate;
        private static final String FINGERPRINT_PREFIX = "idempotent:fingerprint:";

        /**
         * 生成请求指纹
         */
        public String generateFingerprint(String userId, String api, String params) {
            // 组合关键信息生成指纹
            String content = userId + ":" + api + ":" + params;
            return DigestUtils.md5DigestAsHex(content.getBytes());
        }

        /**
         * 检查并记录请求（保证幂等）
         */
        public boolean checkAndRecord(String fingerprint, long expireSeconds) {
            String key = FINGERPRINT_PREFIX + fingerprint;

            // SETNX：只有 key 不存在时才设置成功
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", expireSeconds, TimeUnit.SECONDS);

            return Boolean.TRUE.equals(success);
        }

        /**
         * 使用示例：订单支付
         */
        public void payOrder(String userId, String orderNo, String amount) {
            // 生成请求指纹
            String fingerprint = generateFingerprint(userId, "/order/pay",
                    orderNo + ":" + amount);

            // 检查是否重复请求（根据业务设置过期时间，如支付5分钟内不能重复）
            if (!checkAndRecord(fingerprint, 300)) {
                throw new RuntimeException("请勿重复支付");
            }

            // 执行支付逻辑
            System.out.println("【支付订单】订单号: " + orderNo + ", 金额: " + amount);
        }
    }

    // ==================== 2. 接口防重放攻击 ====================

    /**
     * 防重放攻击实现
     *
     * 原理：
     * 1. 请求包含时间戳、随机数(Nonce)、签名
     * 2. 服务端验证时间戳（如5分钟内）
     * 3. 服务端检查 Nonce 是否已使用（Redis去重）
     * 4. 服务端验证签名
     */
    public static class AntiReplayService {

        private StringRedisTemplate redisTemplate;
        private static final String NONCE_PREFIX = "replay:nonce:";
        private static final long TIME_WINDOW_SECONDS = 300; // 5分钟时间窗口

        /**
         * 请求参数
         */
        public static class RequestParams {
            private String appId;           // 应用ID
            private long timestamp;         // 时间戳（毫秒）
            private String nonce;           // 随机数（唯一）
            private String sign;            // 签名
            private String data;            // 业务数据

            // Getters and Setters
            public String getAppId() { return appId; }
            public void setAppId(String appId) { this.appId = appId; }
            public long getTimestamp() { return timestamp; }
            public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
            public String getNonce() { return nonce; }
            public void setNonce(String nonce) { this.nonce = nonce; }
            public String getSign() { return sign; }
            public void setSign(String sign) { this.sign = sign; }
            public String getData() { return data; }
            public void setData(String data) { this.data = data; }
        }

        /**
         * 验证请求（防重放）
         */
        public boolean verifyRequest(RequestParams params, String appSecret) {
            long now = System.currentTimeMillis();

            // 1. 验证时间戳（防止过期请求）
            if (Math.abs(now - params.getTimestamp()) > TIME_WINDOW_SECONDS * 1000) {
                System.out.println("【防重放】请求已过期");
                return false;
            }

            // 2. 验证 Nonce（防止重复请求）
            String nonceKey = NONCE_PREFIX + params.getNonce();
            Boolean nonceExists = redisTemplate.hasKey(nonceKey);
            if (Boolean.TRUE.equals(nonceExists)) {
                System.out.println("【防重放】Nonce 已使用");
                return false;
            }

            // 3. 记录 Nonce（设置过期时间）
            redisTemplate.opsForValue().set(nonceKey, "1",
                    TIME_WINDOW_SECONDS, TimeUnit.SECONDS);

            // 4. 验证签名
            String expectedSign = generateSign(params, appSecret);
            if (!expectedSign.equals(params.getSign())) {
                System.out.println("【防重放】签名验证失败");
                return false;
            }

            return true;
        }

        /**
         * 生成签名
         */
        private String generateSign(RequestParams params, String appSecret) {
            // 按参数名排序后拼接
            String content = "appId=" + params.getAppId()
                    + "&data=" + params.getData()
                    + "&nonce=" + params.getNonce()
                    + "&timestamp=" + params.getTimestamp()
                    + "&key=" + appSecret;

            return DigestUtils.md5DigestAsHex(content.getBytes()).toUpperCase();
        }
    }

    // ==================== 3. 敏感数据加密 ====================

    /**
     * AES 加密工具类
     */
    public static class AESUtil {
        private static final String ALGORITHM = "AES";
        private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

        /**
         * 加密
         */
        public static String encrypt(String data, String key) throws Exception {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        }

        /**
         * 解密
         */
        public static String decrypt(String encryptedData, String key) throws Exception {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        }
    }

    /**
     * 敏感数据加密存储服务
     */
    public static class SensitiveDataService {

        private StringRedisTemplate redisTemplate;
        private static final String DATA_KEY_PREFIX = "sensitive:data:";
        private static final String AES_KEY = "YourSecretKey123"; // 实际应从配置中心获取

        /**
         * 存储敏感数据（加密）
         */
        public void storeSensitiveData(String dataId, String sensitiveData) throws Exception {
            // 1. 加密数据
            String encryptedData = AESUtil.encrypt(sensitiveData, AES_KEY);

            // 2. 存储到 Redis
            String key = DATA_KEY_PREFIX + dataId;
            redisTemplate.opsForValue().set(key, encryptedData, 30, TimeUnit.MINUTES);

            System.out.println("【加密存储】数据已加密存储");
        }

        /**
         * 获取敏感数据（解密）
         */
        public String getSensitiveData(String dataId) throws Exception {
            String key = DATA_KEY_PREFIX + dataId;
            String encryptedData = redisTemplate.opsForValue().get(key);

            if (encryptedData == null) {
                return null;
            }

            // 解密数据
            return AESUtil.decrypt(encryptedData, AES_KEY);
        }

        /**
         * 存储用户隐私信息示例
         */
        public void storeUserPrivacy(String userId, String idCard, String phone) throws Exception {
            // 加密身份证号
            String encryptedIdCard = AESUtil.encrypt(idCard, AES_KEY);
            // 加密手机号
            String encryptedPhone = AESUtil.encrypt(phone, AES_KEY);

            // 脱敏显示
            String maskedIdCard = maskIdCard(idCard);
            String maskedPhone = maskPhone(phone);

            // 存储到 Redis
            String key = "user:privacy:" + userId;
            redisTemplate.opsForHash().put(key, "idCard", encryptedIdCard);
            redisTemplate.opsForHash().put(key, "idCardMask", maskedIdCard);
            redisTemplate.opsForHash().put(key, "phone", encryptedPhone);
            redisTemplate.opsForHash().put(key, "phoneMask", maskedPhone);

            System.out.println("【隐私保护】用户 " + userId + " 信息已加密存储");
            System.out.println("  身份证号: " + maskedIdCard);
            System.out.println("  手机号: " + maskedPhone);
        }

        /**
         * 身份证号脱敏
         */
        private String maskIdCard(String idCard) {
            if (idCard == null || idCard.length() != 18) {
                return idCard;
            }
            return idCard.substring(0, 6) + "********" + idCard.substring(14);
        }

        /**
         * 手机号脱敏
         */
        private String maskPhone(String phone) {
            if (phone == null || phone.length() != 11) {
                return phone;
            }
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
    }

    /**
     * 订单请求（示例类）
     */
    public static class OrderRequest {
        private String orderNo;
        private String productId;
        private int quantity;

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    /**
     * 综合使用示例
     */
    public static void main(String[] args) {
        System.out.println("========== Redis 安全功能演示 ==========\n");

        System.out.println("1. 分布式幂等性：");
        System.out.println("   - Token 机制：先申请 Token，执行业务时消费 Token");
        System.out.println("   - 请求指纹：对请求内容哈希，Redis SETNX 去重\n");

        System.out.println("2. 接口防重放：");
        System.out.println("   - 时间戳校验：拒绝过期请求");
        System.out.println("   - Nonce 去重：Redis 记录已使用的随机数");
        System.out.println("   - 签名验证：防止参数篡改\n");

        System.out.println("3. 敏感数据加密：");
        System.out.println("   - AES 对称加密存储敏感信息");
        System.out.println("   - 数据脱敏展示");
        System.out.println("   - 加密数据存储在 Redis\n");
    }
}
