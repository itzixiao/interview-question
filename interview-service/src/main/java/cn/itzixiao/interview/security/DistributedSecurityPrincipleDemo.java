package cn.itzixiao.interview.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 分布式幂等、接口防重放、敏感数据加密 - 核心原理详解
 *
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────────────┐
 * │                          分布式安全体系架构图                                  │
 * ├──────────────────────────────────────────────────────────────────────────────┤
 * │                                                                              │
 * │    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                     │
 * │    │   分布式    │    │   接口      │    │   敏感数据  │                     │
 * │    │   幂等      │    │   防重放    │    │   加密      │                     │
 * │    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                     │
 * │           │                  │                  │                            │
 * │    ┌──────▼──────┐    ┌──────▼──────┐    ┌──────▼──────┐                     │
 * │    │ Token机制   │    │ 时间戳校验  │    │ AES对称加密 │                     │
 * │    │ 指纹去重    │    │ Nonce去重   │    │ RSA非对称   │                     │
 * │    │ 状态机幂等  │    │ 签名验证    │    │ 数据脱敏    │                     │
 * │    └──────┬──────┘    └──────┬──────┘    └──────┬──────┘                     │
 * │           │                  │                  │                            │
 * │           └──────────────────┼──────────────────┘                            │
 * │                              │                                               │
 * │                       ┌──────▼──────┐                                        │
 * │                       │   Redis     │                                        │
 * │                       │  分布式存储 │                                        │
 * │                       └─────────────┘                                        │
 * └──────────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @author itzixiao
 */
public class DistributedSecurityPrincipleDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║       分布式幂等、接口防重放、敏感数据加密 - 核心原理详解                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝\n");

        // 第一部分：分布式幂等
        demonstrateIdempotency();

        // 第二部分：接口防重放
        demonstrateAntiReplay();

        // 第三部分：敏感数据加密
        demonstrateDataEncryption();

        // 第四部分：高频面试题
        printInterviewQuestions();
    }

    // ==================== 第一部分：分布式幂等 ====================

    /**
     * 分布式幂等详解
     *
     * <pre>
     * 【什么是幂等性？】
     * - 数学定义：f(f(x)) = f(x)，多次执行结果与一次执行相同
     * - 编程定义：同一操作多次执行，结果与执行一次相同
     * - HTTP幂等：GET/PUT/DELETE 幂等，POST 非幂等
     *
     * 【为什么需要幂等？】
     * 1. 网络超时重试 - 客户端超时重发请求
     * 2. 消息队列重试 - 消费失败后重新投递
     * 3. 前端重复提交 - 用户连续点击按钮
     * 4. 定时任务重复 - 分布式任务多节点执行
     *
     * 【幂等实现方案对比】
     * ┌─────────────────┬───────────────────┬────────────────────────────────┐
     * │ 方案            │ 适用场景          │ 优缺点                          │
     * ├─────────────────┼───────────────────┼────────────────────────────────┤
     * │ Token机制       │ 表单提交、下单    │ 需要两次请求，但最通用           │
     * │ 请求指纹去重    │ 无Token场景       │ 一次请求，但需要合理选择指纹字段 │
     * │ 数据库唯一索引  │ 插入操作          │ 依赖数据库，并发高时有锁竞争     │
     * │ 状态机幂等      │ 状态流转          │ 最优雅，但需要业务支持           │
     * │ 乐观锁          │ 更新操作          │ 需要版本字段，重试逻辑复杂       │
     * └─────────────────┴───────────────────┴────────────────────────────────┘
     * </pre>
     */
    private static void demonstrateIdempotency() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第一部分：分布式幂等                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        // 1. Token 机制
        demonstrateTokenIdempotency();

        // 2. 请求指纹去重
        demonstrateFingerprintIdempotency();

        // 3. 状态机幂等
        demonstrateStateMachineIdempotency();

        // 4. AOP 注解实现
        demonstrateAopIdempotency();
    }

    /**
     * 方案1：Token 机制（推荐）
     *
     * <pre>
     * 【执行流程】
     *
     *  客户端                        服务端                         Redis
     *    │                            │                              │
     *    │  1.请求获取Token           │                              │
     *    │ ─────────────────────────> │                              │
     *    │                            │  2.生成Token存入Redis        │
     *    │                            │ ─────────────────────────────>
     *    │                            │  SET idempotent:token:xxx 1  │
     *    │                            │  EX 600                      │
     *    │  3.返回Token               │                              │
     *    │ <───────────────────────── │                              │
     *    │                            │                              │
     *    │  4.携带Token提交业务       │                              │
     *    │ ─────────────────────────> │                              │
     *    │                            │  5.Lua脚本原子删除Token      │
     *    │                            │ ─────────────────────────────>
     *    │                            │  if GET key then DEL key     │
     *    │                            │                              │
     *    │                            │  6.删除成功?                 │
     *    │                            │ <─────────────────────────────
     *    │                            │                              │
     *    │  7.执行/拒绝业务          │                              │
     *    │ <───────────────────────── │                              │
     *
     * 【为什么用Lua脚本？】
     * - 保证查询和删除的原子性
     * - 避免并发情况下两个请求同时通过校验
     * </pre>
     */
    private static void demonstrateTokenIdempotency() {
        System.out.println("【方案1：Token 机制】\n");

        System.out.println("Lua 脚本（保证原子性）：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  -- 原子性校验并删除 Token                                          │");
        System.out.println("│  local key = KEYS[1]                                               │");
        System.out.println("│  local token = redis.call('GET', key)                              │");
        System.out.println("│  if token then                                                     │");
        System.out.println("│      redis.call('DEL', key)                                        │");
        System.out.println("│      return 1  -- 第一次请求，删除成功                              │");
        System.out.println("│  end                                                               │");
        System.out.println("│  return 0      -- 重复请求，Token不存在或已被消费                   │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 模拟实现
        System.out.println("模拟执行：");
        String token = UUID.randomUUID().toString().replace("-", "");
        System.out.println("  1. 生成 Token: " + token);
        System.out.println("  2. 存入 Redis: SET idempotent:token:" + token.substring(0, 8) + "... 1 EX 600");
        System.out.println("  3. 第一次提交: Token 存在，删除成功，执行业务 ✓");
        System.out.println("  4. 第二次提交: Token 不存在，拒绝请求 ✗\n");
    }

    /**
     * 方案2：请求指纹去重
     *
     * <pre>
     * 【适用场景】
     * - 接口无法传递 Token（如开放API、Webhook回调）
     * - 消息队列消费去重
     *
     * 【指纹生成规则】
     * fingerprint = MD5(userId + api + 关键业务参数)
     *
     * 例如支付场景：
     * fingerprint = MD5(userId + "/pay" + orderNo + amount)
     *
     * 【注意事项】
     * - 参数选择要能唯一标识一次业务操作
     * - 过期时间要根据业务场景设置（如支付5分钟内不可重复）
     * </pre>
     */
    private static void demonstrateFingerprintIdempotency() {
        System.out.println("【方案2：请求指纹去重】\n");

        System.out.println("指纹生成示例：");
        String userId = "user_10086";
        String api = "/api/order/pay";
        String orderNo = "ORD202403070001";
        String amount = "99.99";

        String content = userId + ":" + api + ":" + orderNo + ":" + amount;
        String fingerprint = DigestUtils.md5DigestAsHex(content.getBytes());

        System.out.println("  userId:    " + userId);
        System.out.println("  api:       " + api);
        System.out.println("  orderNo:   " + orderNo);
        System.out.println("  amount:    " + amount);
        System.out.println("  指纹内容:  " + content);
        System.out.println("  MD5指纹:   " + fingerprint);
        System.out.println("\n  Redis命令: SETNX idempotent:fingerprint:" + fingerprint.substring(0, 8) + "... 1 EX 300\n");
    }

    /**
     * 方案3：状态机幂等
     *
     * <pre>
     * 【原理】
     * 利用业务状态机的单向流转特性，同一状态的重复操作直接返回成功
     *
     * 【订单状态机示例】
     *
     *   待支付 ──────> 已支付 ──────> 已发货 ──────> 已完成
     *     │              │              │              │
     *     └──> 已取消 <──┴──────────────┘              │
     *                                                  │
     *     └──────────────────> 已退款 <────────────────┘
     *
     * 【实现要点】
     * 1. 数据库更新时带上状态条件：
     *    UPDATE orders SET status = '已支付'
     *    WHERE order_no = ? AND status = '待支付'
     *
     * 2. 影响行数为0时，查询当前状态：
     *    - 如果已是目标状态，返回成功（幂等）
     *    - 如果是其他状态，返回错误
     * </pre>
     */
    private static void demonstrateStateMachineIdempotency() {
        System.out.println("【方案3：状态机幂等】\n");

        System.out.println("订单支付幂等伪代码：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  public Result payOrder(String orderNo) {                          │");
        System.out.println("│      // 1. 带状态条件更新                                           │");
        System.out.println("│      int rows = orderMapper.updateStatus(                          │");
        System.out.println("│          orderNo, \"待支付\", \"已支付\");                            │");
        System.out.println("│                                                                    │");
        System.out.println("│      if (rows > 0) {                                               │");
        System.out.println("│          return Result.success(\"支付成功\");                        │");
        System.out.println("│      }                                                             │");
        System.out.println("│                                                                    │");
        System.out.println("│      // 2. 更新失败，检查当前状态                                   │");
        System.out.println("│      Order order = orderMapper.selectByOrderNo(orderNo);           │");
        System.out.println("│      if (\"已支付\".equals(order.getStatus())) {                     │");
        System.out.println("│          // 幂等：已是目标状态，直接返回成功                        │");
        System.out.println("│          return Result.success(\"订单已支付\");                      │");
        System.out.println("│      }                                                             │");
        System.out.println("│                                                                    │");
        System.out.println("│      // 3. 状态冲突                                                 │");
        System.out.println("│      return Result.error(\"订单状态异常: \" + order.getStatus());    │");
        System.out.println("│  }                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 方案4：AOP 注解实现幂等（企业级推荐）
     */
    private static void demonstrateAopIdempotency() {
        System.out.println("【方案4：AOP 注解实现（企业级）】\n");

        System.out.println("1. 自定义注解：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Target(ElementType.METHOD)                                       │");
        System.out.println("│  @Retention(RetentionPolicy.RUNTIME)                               │");
        System.out.println("│  public @interface Idempotent {                                    │");
        System.out.println("│      String key() default \"\";        // SpEL表达式，支持取参数值  │");
        System.out.println("│      int expireSeconds() default 300; // 幂等有效期                │");
        System.out.println("│      String message() default \"请勿重复提交\";                     │");
        System.out.println("│  }                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("2. 使用示例：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Idempotent(key = \"#orderRequest.orderNo\", expireSeconds = 60)   │");
        System.out.println("│  public Result createOrder(OrderRequest orderRequest) {            │");
        System.out.println("│      // 业务逻辑                                                    │");
        System.out.println("│  }                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第二部分：接口防重放 ====================

    /**
     * 接口防重放详解
     *
     * <pre>
     * 【什么是重放攻击？】
     * 攻击者截获合法请求后，原封不动地重复发送，达到非法目的
     *
     * 【重放攻击危害】
     * 1. 重复转账 - 截获转账请求重发
     * 2. 重复下单 - 截获下单请求重发
     * 3. 绕过验证 - 截获已验证的请求重发
     *
     * 【防重放三要素】
     * ┌─────────────────────────────────────────────────────────────────────┐
     * │  1. 时间戳（Timestamp）                                              │
     * │     - 拒绝过期请求（如超过5分钟）                                     │
     * │     - 缩小攻击时间窗口                                               │
     * │                                                                     │
     * │  2. 随机数（Nonce）                                                  │
     * │     - 每次请求唯一，服务端缓存已用Nonce                               │
     * │     - 拒绝重复Nonce的请求                                            │
     * │                                                                     │
     * │  3. 签名（Sign）                                                     │
     * │     - 对请求参数+时间戳+随机数+密钥 计算签名                          │
     * │     - 防止参数被篡改                                                 │
     * └─────────────────────────────────────────────────────────────────────┘
     * </pre>
     */
    private static void demonstrateAntiReplay() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第二部分：接口防重放                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        // 1. 防重放原理
        demonstrateAntiReplayPrinciple();

        // 2. 签名算法
        demonstrateSignatureAlgorithm();

        // 3. 完整校验流程
        demonstrateCompleteVerification();
    }

    /**
     * 防重放原理图解
     */
    private static void demonstrateAntiReplayPrinciple() {
        System.out.println("【防重放校验流程】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│                         请求参数结构                                 │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  {                                                                 │");
        System.out.println("│      \"appId\": \"app_10001\",           // 应用ID                    │");
        System.out.println("│      \"timestamp\": 1709827200000,      // 时间戳（毫秒）            │");
        System.out.println("│      \"nonce\": \"a1b2c3d4e5f6\",        // 随机字符串（每次不同）    │");
        System.out.println("│      \"sign\": \"D4B5E6F7...\",          // 签名                      │");
        System.out.println("│      \"data\": {                        // 业务数据                  │");
        System.out.println("│          \"orderNo\": \"ORD001\",                                     │");
        System.out.println("│          \"amount\": 99.99                                          │");
        System.out.println("│      }                                                             │");
        System.out.println("│  }                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        System.out.println("服务端校验步骤：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Step 1: 校验时间戳                                                 │");
        System.out.println("│    - 计算 |服务器时间 - 请求时间戳| < 5分钟                         │");
        System.out.println("│    - 超时则拒绝（防止过期请求重放）                                  │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Step 2: 校验 Nonce                                                │");
        System.out.println("│    - Redis: SETNX nonce:{value} 1 EX 300                           │");
        System.out.println("│    - 设置成功 = 首次请求；设置失败 = 重复请求                       │");
        System.out.println("├─────────────────────────────────────────────────────────────────────┤");
        System.out.println("│  Step 3: 校验签名                                                   │");
        System.out.println("│    - 服务端用相同算法计算签名，与请求签名对比                        │");
        System.out.println("│    - 不一致则拒绝（参数被篡改）                                      │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 签名算法详解
     */
    private static void demonstrateSignatureAlgorithm() {
        System.out.println("【签名算法】\n");

        System.out.println("常用签名方式：");
        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. MD5签名（简单场景）                                              │");
        System.out.println("│     sign = MD5(参数按key排序拼接 + timestamp + nonce + secretKey)   │");
        System.out.println("│                                                                     │");
        System.out.println("│  2. HMAC-SHA256（推荐）                                              │");
        System.out.println("│     sign = HMAC_SHA256(secretKey, 参数按key排序拼接)                │");
        System.out.println("│                                                                     │");
        System.out.println("│  3. RSA签名（高安全场景）                                            │");
        System.out.println("│     sign = RSA_Sign(私钥, 参数按key排序拼接)                        │");
        System.out.println("│     服务端用公钥验签                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");

        // 签名计算示例
        System.out.println("MD5签名计算示例：");

        String appId = "app_10001";
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString().substring(0, 12);
        String data = "amount=99.99&orderNo=ORD001";
        String secretKey = "mySecretKey123456";

        // 按key排序拼接参数
        String signContent = "appId=" + appId
                + "&data=" + data
                + "&nonce=" + nonce
                + "&timestamp=" + timestamp
                + "&key=" + secretKey;

        String sign = DigestUtils.md5DigestAsHex(signContent.getBytes()).toUpperCase();

        System.out.println("  待签名内容: " + signContent.substring(0, 60) + "...");
        System.out.println("  签名结果:   " + sign + "\n");
    }

    /**
     * 完整校验流程
     */
    private static void demonstrateCompleteVerification() {
        System.out.println("【完整防重放校验代码】\n");

        System.out.println("┌─────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  public boolean verifyRequest(RequestDTO request, String secret) { │");
        System.out.println("│      long serverTime = System.currentTimeMillis();                 │");
        System.out.println("│                                                                    │");
        System.out.println("│      // 1. 校验时间戳（5分钟内有效）                                │");
        System.out.println("│      if (Math.abs(serverTime - request.getTimestamp()) > 300000) { │");
        System.out.println("│          throw new BusinessException(\"请求已过期\");                │");
        System.out.println("│      }                                                             │");
        System.out.println("│                                                                    │");
        System.out.println("│      // 2. 校验 Nonce（Redis SETNX）                               │");
        System.out.println("│      String nonceKey = \"replay:nonce:\" + request.getNonce();      │");
        System.out.println("│      Boolean success = redis.opsForValue()                         │");
        System.out.println("│          .setIfAbsent(nonceKey, \"1\", 5, TimeUnit.MINUTES);        │");
        System.out.println("│      if (!Boolean.TRUE.equals(success)) {                          │");
        System.out.println("│          throw new BusinessException(\"请求已被处理\");              │");
        System.out.println("│      }                                                             │");
        System.out.println("│                                                                    │");
        System.out.println("│      // 3. 校验签名                                                 │");
        System.out.println("│      String expectedSign = generateSign(request, secret);          │");
        System.out.println("│      if (!expectedSign.equals(request.getSign())) {                │");
        System.out.println("│          throw new BusinessException(\"签名验证失败\");              │");
        System.out.println("│      }                                                             │");
        System.out.println("│                                                                    │");
        System.out.println("│      return true;                                                  │");
        System.out.println("│  }                                                                 │");
        System.out.println("└─────────────────────────────────────────────────────────────────────┘\n");
    }

    // ==================== 第三部分：敏感数据加密 ====================

    /**
     * 敏感数据加密详解
     *
     * <pre>
     * 【敏感数据分类】
     * 1. 密码类 - 只存哈希，永不解密
     * 2. 证件类 - 可逆加密，需要查询
     * 3. 展示类 - 脱敏显示
     *
     * 【加密算法选型】
     * ┌───────────────┬────────────────────────────────────────────────────┐
     * │ 算法          │ 特点与适用场景                                      │
     * ├───────────────┼────────────────────────────────────────────────────┤
     * │ MD5/SHA-256   │ 哈希算法，不可逆，适合密码存储                      │
     * │ + 盐值        │ 加盐防彩虹表                                         │
     * ├───────────────┼────────────────────────────────────────────────────┤
     * │ BCrypt/Argon2 │ 专用密码哈希，自带盐值，更安全                       │
     * ├───────────────┼────────────────────────────────────────────────────┤
     * │ AES-256-GCM   │ 对称加密，高性能，适合大量数据                       │
     * │               │ 身份证、银行卡、手机号加密存储                       │
     * ├───────────────┼────────────────────────────────────────────────────┤
     * │ RSA-2048      │ 非对称加密，适合密钥交换、数字签名                   │
     * │               │ 性能差，不适合大数据量                               │
     * └───────────────┴────────────────────────────────────────────────────┘
     * </pre>
     */
    private static void demonstrateDataEncryption() throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第三部分：敏感数据加密                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        // 1. 密码哈希
        demonstratePasswordHash();

        // 2. AES对称加密
        demonstrateAesEncryption();

        // 3. RSA非对称加密
        demonstrateRsaEncryption();

        // 4. 数据脱敏
        demonstrateDataMasking();
    }

    /**
     * 密码哈希
     */
    private static void demonstratePasswordHash() throws Exception {
        System.out.println("【1. 密码哈希（SHA-256 + 盐值）】\n");

        String password = "MyP@ssw0rd123";
        String salt = UUID.randomUUID().toString().substring(0, 16);

        // SHA-256 哈希
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest((password + salt).getBytes(StandardCharsets.UTF_8));
        String hashedPassword = Base64.getEncoder().encodeToString(hash);

        System.out.println("  原始密码:    " + password);
        System.out.println("  盐值:        " + salt);
        System.out.println("  哈希结果:    " + hashedPassword);
        System.out.println("  数据库存储:  salt:hash = " + salt + ":" + hashedPassword.substring(0, 20) + "...\n");

        System.out.println("  密码验证流程：");
        System.out.println("  1. 从数据库取出 salt 和 hash");
        System.out.println("  2. 用户输入密码 + salt 重新计算 hash");
        System.out.println("  3. 对比两个 hash 是否一致\n");

        System.out.println("  【最佳实践】推荐使用 BCrypt / Argon2：");
        System.out.println("  - 自动生成盐值并嵌入结果");
        System.out.println("  - 有计算成本参数，防暴力破解");
        System.out.println("  - Spring Security 内置支持\n");
    }

    /**
     * AES 对称加密（GCM 模式）
     */
    private static void demonstrateAesEncryption() throws Exception {
        System.out.println("【2. AES 对称加密（AES-256-GCM）】\n");

        // 生成 256 位密钥
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        String keyBase64 = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        // 原始数据
        String idCard = "110101199001011234";
        String bankCard = "6222021234567890123";

        // AES-GCM 加密
        String encryptedIdCard = aesGcmEncrypt(idCard, secretKey);
        String encryptedBankCard = aesGcmEncrypt(bankCard, secretKey);

        // AES-GCM 解密
        String decryptedIdCard = aesGcmDecrypt(encryptedIdCard, secretKey);

        System.out.println("  密钥(Base64): " + keyBase64.substring(0, 32) + "...");
        System.out.println("  ───────────────────────────────────────────");
        System.out.println("  身份证号:");
        System.out.println("    原文:   " + idCard);
        System.out.println("    加密:   " + encryptedIdCard.substring(0, 40) + "...");
        System.out.println("    解密:   " + decryptedIdCard);
        System.out.println("  ───────────────────────────────────────────");
        System.out.println("  银行卡号:");
        System.out.println("    原文:   " + bankCard);
        System.out.println("    加密:   " + encryptedBankCard.substring(0, 40) + "...\n");

        System.out.println("  【AES模式选择】");
        System.out.println("  - ECB: 简单但不安全（相同明文相同密文）");
        System.out.println("  - CBC: 需要IV，常用于文件加密");
        System.out.println("  - GCM: 推荐，自带认证标签，防篡改\n");
    }

    /**
     * AES-GCM 加密
     */
    private static String aesGcmEncrypt(String plainText, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

        // 生成12字节的随机IV
        byte[] iv = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // IV + 密文 一起存储
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(result);
    }

    /**
     * AES-GCM 解密
     */
    private static String aesGcmDecrypt(String encryptedText, SecretKey secretKey) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedText);

        // 分离 IV 和密文
        byte[] iv = new byte[12];
        byte[] encrypted = new byte[decoded.length - 12];
        System.arraycopy(decoded, 0, iv, 0, 12);
        System.arraycopy(decoded, 12, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * RSA 非对称加密
     */
    private static void demonstrateRsaEncryption() throws Exception {
        System.out.println("【3. RSA 非对称加密】\n");

        // 生成 RSA 密钥对
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        // 原始数据（AES密钥等短数据）
        String aesKey = "ThisIsAES256Key!";

        // RSA 加密（公钥加密）
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        byte[] encrypted = rsaCipher.doFinal(aesKey.getBytes(StandardCharsets.UTF_8));
        String encryptedBase64 = Base64.getEncoder().encodeToString(encrypted);

        // RSA 解密（私钥解密）
        rsaCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        byte[] decrypted = rsaCipher.doFinal(encrypted);
        String decryptedText = new String(decrypted, StandardCharsets.UTF_8);

        System.out.println("  公钥(Base64): " + publicKeyBase64.substring(0, 50) + "...");
        System.out.println("  私钥(Base64): " + privateKeyBase64.substring(0, 50) + "...");
        System.out.println("  ───────────────────────────────────────────");
        System.out.println("  原文(AES密钥): " + aesKey);
        System.out.println("  RSA加密后:     " + encryptedBase64.substring(0, 50) + "...");
        System.out.println("  RSA解密后:     " + decryptedText + "\n");

        System.out.println("  【RSA典型应用场景】");
        System.out.println("  1. 密钥交换: RSA加密AES密钥，实现混合加密");
        System.out.println("  2. 数字签名: 私钥签名，公钥验签");
        System.out.println("  3. HTTPS:    证书中的公钥用于建立安全连接\n");

        System.out.println("  【混合加密流程】（推荐）");
        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │  1. 客户端生成随机 AES 密钥                                  │");
        System.out.println("  │  2. 用 AES 密钥加密业务数据（高效）                          │");
        System.out.println("  │  3. 用服务端 RSA 公钥加密 AES 密钥（安全）                   │");
        System.out.println("  │  4. 发送: RSA(AES_KEY) + AES(DATA)                          │");
        System.out.println("  │  5. 服务端用 RSA 私钥解密得到 AES 密钥                       │");
        System.out.println("  │  6. 用 AES 密钥解密业务数据                                  │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 数据脱敏
     */
    private static void demonstrateDataMasking() {
        System.out.println("【4. 数据脱敏】\n");

        // 常见脱敏规则
        String phone = "13800138000";
        String idCard = "110101199001011234";
        String bankCard = "6222021234567890123";
        String email = "zhangsan@example.com";
        String name = "张三丰";

        System.out.println("  原始数据          脱敏规则                脱敏结果");
        System.out.println("  ──────────────────────────────────────────────────────────────");
        System.out.println("  " + phone + "    手机号: 前3后4          " + maskPhone(phone));
        System.out.println("  " + idCard + "  身份证: 前6后4          " + maskIdCard(idCard));
        System.out.println("  " + bankCard + "  银行卡: 前4后4          " + maskBankCard(bankCard));
        System.out.println("  " + email + "  邮箱: @前保留首尾        " + maskEmail(email));
        System.out.println("  " + name + "            姓名: 保留姓            " + maskName(name) + "\n");

        System.out.println("  【脱敏注解实现】（推荐）");
        System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
        System.out.println("  │  public class UserVO {                                       │");
        System.out.println("  │      @Sensitive(type = SensitiveType.PHONE)                 │");
        System.out.println("  │      private String phone;                                  │");
        System.out.println("  │                                                             │");
        System.out.println("  │      @Sensitive(type = SensitiveType.ID_CARD)               │");
        System.out.println("  │      private String idCard;                                 │");
        System.out.println("  │  }                                                          │");
        System.out.println("  │                                                             │");
        System.out.println("  │  // Jackson 序列化时自动脱敏                                 │");
        System.out.println("  │  // 实现 JsonSerializer<String>                              │");
        System.out.println("  └─────────────────────────────────────────────────────────────┘\n");
    }

    // 脱敏工具方法
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    private static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) return bankCard;
        return bankCard.substring(0, 4) + "****" + bankCard.substring(bankCard.length() - 4);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return email;
        return email.charAt(0) + "***" + email.substring(atIndex - 1);
    }

    private static String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "**";
    }

    // ==================== 第四部分：高频面试题 ====================

    private static void printInterviewQuestions() {
        System.out.println("╔════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                      第四部分：高频面试题                                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════╝\n");

        System.out.println("==========================================================================");
        System.out.println("【问题1】什么是接口幂等性？哪些场景需要保证幂等？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("幂等性是指同一操作多次执行，结果与执行一次相同。");
        System.out.println("");
        System.out.println("需要保证幂等的场景：");
        System.out.println("1. 网络超时重试 - 客户端超时后重发请求");
        System.out.println("2. 消息队列重试 - 消费失败后重新投递");
        System.out.println("3. 表单重复提交 - 用户快速多次点击");
        System.out.println("4. 定时任务执行 - 分布式任务多节点触发");
        System.out.println("5. 第三方回调 - 支付回调可能重复调用\n");

        System.out.println("==========================================================================");
        System.out.println("【问题2】如何实现分布式幂等？对比各方案优缺点？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("┌─────────────┬──────────────────────────────────────────────────────────┐");
        System.out.println("│ Token机制   │ 先申请Token，提交时原子删除。通用性强，需要两次请求       │");
        System.out.println("├─────────────┼──────────────────────────────────────────────────────────┤");
        System.out.println("│ 指纹去重    │ 对请求参数MD5，Redis SETNX。一次请求，需合理选指纹字段   │");
        System.out.println("├─────────────┼──────────────────────────────────────────────────────────┤");
        System.out.println("│ 数据库唯一  │ 业务字段唯一索引，重复插入抛异常。依赖数据库，有锁竞争   │");
        System.out.println("│ 索引        │                                                          │");
        System.out.println("├─────────────┼──────────────────────────────────────────────────────────┤");
        System.out.println("│ 状态机幂等  │ 带状态条件更新，已是目标状态直接返回。最优雅，需业务支持 │");
        System.out.println("├─────────────┼──────────────────────────────────────────────────────────┤");
        System.out.println("│ 乐观锁      │ 版本号控制，CAS更新。需要版本字段，重试逻辑复杂          │");
        System.out.println("└─────────────┴──────────────────────────────────────────────────────────┘\n");

        System.out.println("==========================================================================");
        System.out.println("【问题3】Token幂等方案为什么要用Lua脚本？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("为了保证'查询Token是否存在'和'删除Token'两个操作的原子性。");
        System.out.println("");
        System.out.println("如果分开执行：");
        System.out.println("  线程A: GET token → 存在");
        System.out.println("  线程B: GET token → 存在（还未删除）");
        System.out.println("  线程A: DEL token → 成功");
        System.out.println("  线程B: DEL token → 成功（但实际是重复请求）");
        System.out.println("");
        System.out.println("Lua脚本在Redis中原子执行，不会被其他命令打断。\n");

        System.out.println("==========================================================================");
        System.out.println("【问题4】什么是重放攻击？如何防止？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("重放攻击是攻击者截获合法请求后，原封不动地重复发送。");
        System.out.println("");
        System.out.println("防止方案（三要素）：");
        System.out.println("1. 时间戳 - 拒绝超时请求（如5分钟外），缩小攻击窗口");
        System.out.println("2. Nonce - 随机数，服务端缓存已用，拒绝重复");
        System.out.println("3. 签名 - 参数+时间戳+Nonce+密钥 计算签名，防篡改");
        System.out.println("");
        System.out.println("三者缺一不可：");
        System.out.println("- 无时间戳: Nonce永久存储，内存爆炸");
        System.out.println("- 无Nonce: 时间窗口内可重放");
        System.out.println("- 无签名: 攻击者可伪造时间戳和Nonce\n");

        System.out.println("==========================================================================");
        System.out.println("【问题5】幂等和防重放的区别？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("┌─────────────┬─────────────────────────────────────────────────────────┐");
        System.out.println("│             │ 幂等                          │ 防重放                 │");
        System.out.println("├─────────────┼───────────────────────────────┼────────────────────────┤");
        System.out.println("│ 目的        │ 防止业务重复执行              │ 防止请求被恶意重发     │");
        System.out.println("├─────────────┼───────────────────────────────┼────────────────────────┤");
        System.out.println("│ 触发场景    │ 网络重试、消息重投            │ 中间人攻击、抓包重放   │");
        System.out.println("├─────────────┼───────────────────────────────┼────────────────────────┤");
        System.out.println("│ 关注点      │ 业务数据一致性                │ 接口安全性             │");
        System.out.println("├─────────────┼───────────────────────────────┼────────────────────────┤");
        System.out.println("│ 实现手段    │ Token去重、状态机             │ 时间戳+Nonce+签名      │");
        System.out.println("└─────────────┴───────────────────────────────┴────────────────────────┘\n");

        System.out.println("==========================================================================");
        System.out.println("【问题6】AES和RSA的区别？实际项目中如何选择？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("┌─────────────┬───────────────────────────────┬───────────────────────┐");
        System.out.println("│             │ AES（对称加密）               │ RSA（非对称加密）     │");
        System.out.println("├─────────────┼───────────────────────────────┼───────────────────────┤");
        System.out.println("│ 密钥        │ 加解密同一密钥                │ 公钥加密，私钥解密    │");
        System.out.println("├─────────────┼───────────────────────────────┼───────────────────────┤");
        System.out.println("│ 性能        │ 快，适合大数据量              │ 慢，适合小数据        │");
        System.out.println("├─────────────┼───────────────────────────────┼───────────────────────┤");
        System.out.println("│ 密钥管理    │ 密钥需安全传输                │ 公钥可公开            │");
        System.out.println("├─────────────┼───────────────────────────────┼───────────────────────┤");
        System.out.println("│ 典型场景    │ 数据库字段加密、文件加密      │ 密钥交换、数字签名    │");
        System.out.println("└─────────────┴───────────────────────────────┴───────────────────────┘");
        System.out.println("");
        System.out.println("实际项目推荐「混合加密」：");
        System.out.println("1. 用RSA加密AES密钥（安全传输）");
        System.out.println("2. 用AES加密业务数据（高效处理）\n");

        System.out.println("==========================================================================");
        System.out.println("【问题7】为什么密码要加盐？彩虹表攻击是什么？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("彩虹表是预先计算好的「明文→哈希值」对照表。");
        System.out.println("攻击者获取数据库后，直接查表就能反推原始密码。");
        System.out.println("");
        System.out.println("加盐原理：");
        System.out.println("  原始: hash = SHA256(password)          → 可查表破解");
        System.out.println("  加盐: hash = SHA256(password + salt)   → 彩虹表失效");
        System.out.println("");
        System.out.println("最佳实践：");
        System.out.println("1. 每个用户使用不同的随机盐值");
        System.out.println("2. 盐值和哈希一起存储: salt:hash");
        System.out.println("3. 推荐 BCrypt/Argon2，自带盐和计算成本\n");

        System.out.println("==========================================================================");
        System.out.println("【问题8】AES的ECB和GCM模式有什么区别？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("ECB（电子密码本模式）：");
        System.out.println("- 相同明文块加密出相同密文块");
        System.out.println("- 可通过密文模式推测明文结构");
        System.out.println("- 不安全，不推荐使用");
        System.out.println("");
        System.out.println("GCM（Galois/Counter模式）：");
        System.out.println("- 每次加密使用随机IV，相同明文密文不同");
        System.out.println("- 自带认证标签（Authentication Tag），可检测篡改");
        System.out.println("- 推荐使用，HTTPS/TLS默认采用\n");

        System.out.println("==========================================================================");
        System.out.println("【问题9】如何实现接口级别的数据脱敏？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("推荐使用自定义注解 + Jackson序列化器：");
        System.out.println("");
        System.out.println("1. 定义脱敏注解 @Sensitive");
        System.out.println("2. 定义脱敏类型枚举：PHONE/ID_CARD/BANK_CARD/EMAIL");
        System.out.println("3. 实现 JsonSerializer<String>，根据类型脱敏");
        System.out.println("4. VO字段加注解，返回时自动脱敏");
        System.out.println("");
        System.out.println("优点：");
        System.out.println("- 统一管理脱敏规则");
        System.out.println("- 字段级别控制");
        System.out.println("- 与业务代码解耦\n");

        System.out.println("==========================================================================");
        System.out.println("【问题10】分布式环境下如何保证密钥安全？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("1. 密钥管理服务（KMS）");
        System.out.println("   - 使用阿里云KMS、AWS KMS等托管服务");
        System.out.println("   - 密钥不落地，由云服务管理");
        System.out.println("");
        System.out.println("2. 配置中心加密");
        System.out.println("   - Nacos/Apollo支持配置加密");
        System.out.println("   - 密钥从配置中心动态获取");
        System.out.println("");
        System.out.println("3. 环境变量注入");
        System.out.println("   - K8s Secret、Docker Secret");
        System.out.println("   - 启动时注入，不写入代码和配置文件");
        System.out.println("");
        System.out.println("4. 密钥轮换");
        System.out.println("   - 定期更换密钥");
        System.out.println("   - 支持新旧密钥并存过渡期\n");

        System.out.println("==========================================================================");
        System.out.println("【问题11】如何设计一个通用的幂等框架？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("核心设计：");
        System.out.println("1. 注解驱动: @Idempotent(key=\"SpEL\", expire=300)");
        System.out.println("2. AOP拦截: 方法执行前后处理幂等逻辑");
        System.out.println("3. Key生成: SpEL表达式支持从参数取值");
        System.out.println("4. 存储抽象: 支持Redis、数据库等多种存储");
        System.out.println("");
        System.out.println("执行流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  @Around(\"@annotation(idempotent)\")                        │");
        System.out.println("│  public Object around(ProceedingJoinPoint pjp) {           │");
        System.out.println("│      String key = parseKey(idempotent.key(), pjp);         │");
        System.out.println("│      if (!tryLock(key, idempotent.expire())) {             │");
        System.out.println("│          throw new RepeatRequestException();               │");
        System.out.println("│      }                                                     │");
        System.out.println("│      try {                                                 │");
        System.out.println("│          return pjp.proceed();                             │");
        System.out.println("│      } finally {                                           │");
        System.out.println("│          // 根据策略决定是否释放锁                          │");
        System.out.println("│      }                                                     │");
        System.out.println("│  }                                                         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("==========================================================================");
        System.out.println("【问题12】生产环境遇到幂等失效怎么排查？");
        System.out.println("==========================================================================");
        System.out.println("答：");
        System.out.println("排查思路：");
        System.out.println("1. 确认Key生成逻辑");
        System.out.println("   - 是否包含了唯一标识？");
        System.out.println("   - 参数是否可能为空导致Key相同？");
        System.out.println("");
        System.out.println("2. 检查Redis连接");
        System.out.println("   - Redis是否可用？");
        System.out.println("   - 是否存在网络分区？");
        System.out.println("");
        System.out.println("3. 检查过期时间");
        System.out.println("   - 过期时间是否设置过短？");
        System.out.println("   - 业务执行时间是否超过过期时间？");
        System.out.println("");
        System.out.println("4. 检查Lua脚本执行");
        System.out.println("   - 是否被其他命令打断？");
        System.out.println("   - 集群模式下Key是否在同一slot？");
        System.out.println("");
        System.out.println("5. 日志追踪");
        System.out.println("   - 记录幂等Key、请求参数、执行结果");
        System.out.println("   - 便于定位重复请求来源\n");

        System.out.println("==========================================================================\n");
    }
}

// ==================== 辅助类定义 ====================

/**
 * 幂等注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Idempotent {
    /**
     * 幂等Key，支持SpEL表达式
     */
    String key() default "";

    /**
     * 过期时间（秒）
     */
    int expireSeconds() default 300;

    /**
     * 重复请求提示信息
     */
    String message() default "请勿重复提交";
}

/**
 * 幂等切面实现
 */
@Aspect
@Component
class IdempotentAspect {

    private StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENT_PREFIX = "idempotent:";

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint pjp, Idempotent idempotent) throws Throwable {
        // 1. 解析幂等Key
        String key = parseKey(idempotent.key(), pjp);
        String redisKey = IDEMPOTENT_PREFIX + key;

        // 2. 尝试获取幂等锁
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "1", idempotent.expireSeconds(), TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException(idempotent.message());
        }

        // 3. 执行业务
        return pjp.proceed();
    }

    /**
     * 解析SpEL表达式获取Key
     */
    private String parseKey(String keyExpression, ProceedingJoinPoint pjp) {
        if (keyExpression == null || keyExpression.isEmpty()) {
            // 默认使用方法签名 + 参数哈希
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            String methodName = signature.getMethod().getName();
            String argsHash = DigestUtils.md5DigestAsHex(
                    java.util.Arrays.toString(pjp.getArgs()).getBytes());
            return methodName + ":" + argsHash;
        }

        // SpEL解析（简化实现）
        // 实际项目中使用 SpelExpressionParser 解析
        return keyExpression;
    }
}

/**
 * 防重放注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface AntiReplay {
    /**
     * 时间窗口（秒）
     */
    int timeWindow() default 300;
}

/**
 * 防重放切面实现
 */
@Aspect
@Component
class AntiReplayAspect {

    private StringRedisTemplate redisTemplate;

    private static final String NONCE_PREFIX = "replay:nonce:";

    @Around("@annotation(antiReplay)")
    public Object around(ProceedingJoinPoint pjp, AntiReplay antiReplay) throws Throwable {
        HttpServletRequest request = getRequest();

        // 1. 获取请求参数
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String sign = request.getHeader("X-Sign");

        // 2. 校验时间戳
        long requestTime = Long.parseLong(timestamp);
        if (Math.abs(System.currentTimeMillis() - requestTime) > antiReplay.timeWindow() * 1000L) {
            throw new RuntimeException("请求已过期");
        }

        // 3. 校验Nonce
        String nonceKey = NONCE_PREFIX + nonce;
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(nonceKey, "1", antiReplay.timeWindow(), TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(success)) {
            throw new RuntimeException("请求已被处理");
        }

        // 4. 校验签名（省略具体实现）

        return pjp.proceed();
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}

/**
 * 敏感数据类型
 */
enum SensitiveType {
    PHONE,
    ID_CARD,
    BANK_CARD,
    EMAIL,
    NAME
}

/**
 * 敏感数据脱敏注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@interface Sensitive {
    SensitiveType type();
}
