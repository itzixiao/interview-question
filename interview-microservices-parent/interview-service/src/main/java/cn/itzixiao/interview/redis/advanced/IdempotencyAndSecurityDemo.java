package cn.itzixiao.interview.redis.advanced;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/**
 * 分布式幂等、接口防重放、敏感数据加密
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │                         分布式幂等                                      │
 * │  定义：同一操作多次执行，结果与执行一次相同                               │
 * │  场景：网络超时重试、消息队列消费、定时任务                               │
 * │  实现：Token 机制、数据库唯一索引、Redis 去重                             │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                         接口防重放                                      │
 * │  定义：防止请求被截获后重复发送                                           │
 * │  实现：时间戳 + 随机数 + 签名、一次性 Token                               │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │                         敏感数据加密                                    │
 * │  场景：密码、身份证号、银行卡号、手机号                                   │
 * │  方案：对称加密（AES）、非对称加密（RSA）、哈希（SHA-256）                │
 * └─────────────────────────────────────────────────────────────────────────┘
 */
public class IdempotencyAndSecurityDemo {

    private static JedisPool jedisPool;

    public static void main(String[] args) throws Exception {
        System.out.println("========== 分布式幂等、接口防重放、敏感数据加密 ==========\n");

        initJedis();

        // 1. 分布式幂等
        demonstrateIdempotency();

        // 2. 接口防重放
        demonstrateReplayProtection();

        // 3. 敏感数据加密
        demonstrateDataEncryption();

        jedisPool.close();
    }

    private static void initJedis() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(100);
        jedisPool = new JedisPool(config, "localhost", 6379);
        System.out.println("Jedis 连接池初始化完成\n");
    }

    /**
     * 1. 分布式幂等
     */
    private static void demonstrateIdempotency() {
        System.out.println("【1. 分布式幂等】\n");

        System.out.println("幂等性定义：");
        System.out.println("- 数学：f(f(x)) = f(x)");
        System.out.println("- 程序：同一操作多次执行，结果与执行一次相同");
        System.out.println("- HTTP：GET/PUT/DELETE 幂等，POST 不幂等\n");

        System.out.println("常见场景：");
        System.out.println("1. 网络超时重试");
        System.out.println("2. 消息队列消费");
        System.out.println("3. 定时任务执行");
        System.out.println("4. 表单重复提交\n");

        System.out.println("实现方案对比：");
        System.out.println("┌─────────────────┬─────────────────────────────────────────────┐");
        System.out.println("│  Token 机制     │  客户端申请 Token，服务端校验并删除          │");
        System.out.println("│  （推荐）       │  使用 Redis SETNX 或 Lua 脚本保证原子性      │");
        System.out.println("├─────────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  数据库唯一索引 │  业务字段建唯一索引，重复插入抛异常           │");
        System.out.println("│                 │  适合订单号、流水号等                         │");
        System.out.println("├─────────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  Redis 去重     │  SETNX key value EX seconds                  │");
        System.out.println("│                 │  key = 业务标识 + 唯一ID                       │");
        System.out.println("├─────────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  状态机幂等     │  业务状态流转，同一状态多次操作结果相同       │");
        System.out.println("│                 │  如：已支付订单再次支付，直接返回成功         │");
        System.out.println("└─────────────────┴─────────────────────────────────────────────┘\n");

        // Token 机制实现
        System.out.println("Token 机制实现（防止表单重复提交）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 用户进入提交页面                                          │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  2. 服务端生成 Token，存入 Redis（过期时间10分钟）            │");
        System.out.println("│     String token = UUID.randomUUID().toString();            │");
        System.out.println("│     redis.setex(\"idempotent:token:\" + token, 600, \"1\");   │");
        System.out.println("│     返回给页面                                                │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  3. 用户提交表单，携带 Token                                  │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  4. 服务端校验 Token（使用 Lua 保证原子性）                   │");
        System.out.println("│     Lua脚本：                                                │");
        System.out.println("│     if redis.call('get', KEYS[1]) then                      │");
        System.out.println("│         redis.call('del', KEYS[1])                          │");
        System.out.println("│         return 1  -- 删除成功，第一次提交                    │");
        System.out.println("│     end                                                     │");
        System.out.println("│     return 0  -- Token不存在，重复提交                       │");
        System.out.println("│       ↓                                                     │");
        System.out.println("│  5. 校验成功执行业务，失败返回重复提交提示                    │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        try (Jedis jedis = jedisPool.getResource()) {
            // 生成 Token
            String token = UUID.randomUUID().toString();
            String tokenKey = "idempotent:token:" + token;
            jedis.setex(tokenKey, 600, "1");
            System.out.println("生成 Token: " + token);

            // 第一次提交
            Long result1 = jedis.del(tokenKey);
            System.out.println("第一次提交，删除 Token: " + (result1 > 0 ? "成功（继续处理）" : "失败"));

            // 第二次提交（重复）
            Long result2 = jedis.del(tokenKey);
            System.out.println("第二次提交，删除 Token: " + (result2 > 0 ? "成功" : "失败（重复提交）"));
        }
        System.out.println();
    }

    /**
     * 2. 接口防重放
     */
    private static void demonstrateReplayProtection() {
        System.out.println("【2. 接口防重放】\n");

        System.out.println("重放攻击定义：");
        System.out.println("- 攻击者截获合法请求后，重复发送该请求");
        System.out.println("- 可能导致重复转账、重复下单等\n");

        System.out.println("防重放方案：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  方案1：时间戳 + 随机数 + 签名                                │");
        System.out.println("│                                                             │");
        System.out.println("│  请求参数：                                                  │");
        System.out.println("│  - timestamp: 当前时间戳（服务端校验时间差 < 5分钟）         │");
        System.out.println("│  - nonce: 随机字符串（唯一，服务端缓存5分钟）                │");
        System.out.println("│  - sign: 签名（参数 + 密钥 + timestamp + nonce 的哈希）      │");
        System.out.println("│                                                             │");
        System.out.println("│  服务端校验：                                                │");
        System.out.println("│  1. 校验时间戳（防过期重放）                                 │");
        System.out.println("│  2. 校验 nonce 是否已使用（Redis SETNX）                     │");
        System.out.println("│  3. 校验签名是否正确                                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  方案2：一次性 Token（与幂等 Token 类似）                     │");
        System.out.println("│                                                             │");
        System.out.println("│  - 每次请求前申请 Token                                      │");
        System.out.println("│  - Token 只能使用一次                                        │");
        System.out.println("│  - 使用后立即删除                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        // 演示时间戳 + nonce 方案
        System.out.println("演示：时间戳 + nonce + 签名\n");

        try (Jedis jedis = jedisPool.getResource()) {
            // 客户端生成请求
            long timestamp = System.currentTimeMillis() / 1000;
            String nonce = UUID.randomUUID().toString().substring(0, 8);
            String secretKey = "mySecretKey123";
            String data = "amount=100&to=123456";

            // 生成签名
            String signInput = data + timestamp + nonce + secretKey;
            String sign = sha256(signInput);

            System.out.println("请求参数:");
            System.out.println("  data: " + data);
            System.out.println("  timestamp: " + timestamp);
            System.out.println("  nonce: " + nonce);
            System.out.println("  sign: " + sign);

            // 服务端校验
            System.out.println("\n服务端校验:");

            // 1. 校验时间戳
            long serverTime = System.currentTimeMillis() / 1000;
            if (Math.abs(serverTime - timestamp) > 300) {
                System.out.println("  时间戳校验: 失败（请求已过期）");
                return;
            }
            System.out.println("  时间戳校验: 通过");

            // 2. 校验 nonce
            String nonceKey = "nonce:" + nonce;
            Long setResult = jedis.setnx(nonceKey, "1");
            jedis.expire(nonceKey, 300);
            if (setResult == 0) {
                System.out.println("  nonce 校验: 失败（重复请求）");
                return;
            }
            System.out.println("  nonce 校验: 通过");

            // 3. 校验签名
            String serverSign = sha256(signInput);
            if (!serverSign.equals(sign)) {
                System.out.println("  签名校验: 失败");
                return;
            }
            System.out.println("  签名校验: 通过");

            System.out.println("  所有校验通过，处理业务...");
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }

    /**
     * 3. 敏感数据加密
     */
    private static void demonstrateDataEncryption() throws Exception {
        System.out.println("【3. 敏感数据加密】\n");

        System.out.println("敏感数据类型：");
        System.out.println("- 密码：不可逆加密（哈希）");
        System.out.println("- 身份证号、银行卡号：可逆加密（对称加密）");
        System.out.println("- 手机号：部分脱敏 + 加密存储\n");

        System.out.println("加密方案对比：");
        System.out.println("┌─────────────────┬─────────────────────────────────────────────┐");
        System.out.println("│  哈希（SHA-256）│  密码存储，不可逆                           │");
        System.out.println("│  + 盐值         │  防止彩虹表攻击                             │");
        System.out.println("├─────────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  对称加密（AES）│  身份证号、银行卡号加密                     │");
        System.out.println("│                 │  相同明文加密后结果相同（可搜索）           │");
        System.out.println("├─────────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  非对称加密     │  密钥交换、数字签名                         │");
        System.out.println("│  （RSA）        │  性能较差，不适合大数据量                   │");
        System.out.println("├─────────────────┼─────────────────────────────────────────────┤");
        System.out.println("│  格式保留加密   │  加密后格式不变（如手机号加密后还是11位）   │");
        System.out.println("│  （FPE）        │  需要特殊算法或数据库支持                   │");
        System.out.println("└─────────────────┴─────────────────────────────────────────────┘\n");

        // 密码哈希
        System.out.println("1. 密码哈希（SHA-256 + 盐值）");
        String password = "userPassword123";
        String salt = UUID.randomUUID().toString().substring(0, 8);
        String hashedPassword = sha256(password + salt);
        System.out.println("  原始密码: " + password);
        System.out.println("  盐值: " + salt);
        System.out.println("  哈希结果: " + hashedPassword);
        System.out.println("  存储: " + salt + ":" + hashedPassword);
        System.out.println("  （校验时取出盐值，重新计算哈希对比）\n");

        // AES 加密
        System.out.println("2. AES 对称加密");
        String secretKey = "1234567890123456"; // 16字节密钥
        String idCard = "110101199001011234";

        String encrypted = aesEncrypt(idCard, secretKey);
        String decrypted = aesDecrypt(encrypted, secretKey);

        System.out.println("  原始身份证号: " + idCard);
        System.out.println("  AES 加密后: " + encrypted);
        System.out.println("  AES 解密后: " + decrypted);
        System.out.println();

        // 手机号脱敏
        System.out.println("3. 手机号脱敏");
        String phone = "13800138000";
        String maskedPhone = phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        System.out.println("  原始手机号: " + phone);
        System.out.println("  脱敏显示: " + maskedPhone);
        System.out.println("  （数据库存储加密后的完整号码）\n");

        // 数据库加密存储建议
        System.out.println("数据库加密存储建议：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 敏感字段单独加密存储，不要和明文放一起                    │");
        System.out.println("│  2. 密钥分级管理，定期轮换                                    │");
        System.out.println("│  3. 使用 KMS（密钥管理服务）托管密钥                          │");
        System.out.println("│  4. 数据库连接使用 SSL/TLS 加密                               │");
        System.out.println("│  5. 日志中不要打印敏感信息                                    │");
        System.out.println("│  6. 接口返回数据按需脱敏                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 工具方法 ====================

    private static String sha256(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private static String aesEncrypt(String plainText, String secretKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private static String aesDecrypt(String encryptedText, String secretKey) throws Exception {
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        byte[] decoded = Base64.getDecoder().decode(encryptedText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}

/**
 * 安全最佳实践总结：
 * <p>
 * 1. 接口安全
 * - HTTPS 传输
 * - 请求签名验证
 * - 防重放攻击
 * - 限流保护
 * <p>
 * 2. 数据安全
 * - 敏感数据加密存储
 * - 密码哈希加盐
 * - 密钥安全托管
 * - 最小权限原则
 * <p>
 * 3. 业务安全
 * - 幂等设计
 * - 事务一致性
 * - 操作审计日志
 * - 异常监控告警
 */
