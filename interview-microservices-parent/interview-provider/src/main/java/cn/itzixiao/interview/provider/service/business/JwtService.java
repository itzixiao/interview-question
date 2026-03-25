package cn.itzixiao.interview.provider.service.business;

import cn.itzixiao.interview.provider.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT 工具服务
 * <p>
 * 大厂规范：
 * 1. 生成 Token 时写入 jti（JWT ID），支持主动吊销（黑名单）
 * 2. 生成 Token 时写入 userId（数字 ID），直接透传给下游服务无需再次查表
 * 3. 依赖 JwtConfig（@ConfigurationProperties）统一管理配置
 * 4. Redis 可选：Redis 可用时，登录存储 token，登出写黑名单，验证时检查黑名单
 * 5. 所有 Redis 操作均有 try-catch 降级处理，Redis 不可用时不影响正常鉴权
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
@Service
public class JwtService {

    /** Redis key 前缀：存储在线 token（username -> token） */
    public static final String TOKEN_PREFIX = "jwt:token:";
    /** Redis key 前缀：黑名单（jti -> username） */
    public static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;
    /** 可选依赖：Redis 不可用时降级运行，登录/登出不操作 Redis */
    private final StringRedisTemplate redisTemplate;

    public JwtService(JwtConfig jwtConfig, SecretKey secretKey,
                      @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.jwtConfig = jwtConfig;
        this.secretKey = secretKey;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 生成 JWT token（标准版，包含 userId + jti）
     * <p>
     * Redis 可用时：同时将 token 存入 Redis（KEY: jwt:token:{username}，TTL = 过期时间）
     * 实现单设备登录：新 token 覆盖旧 token，旧 token 自动失效
     *
     * @param username 用户名（作为 subject）
     * @param userId   用户数字 ID（透传给下游，政策表关联等场景使用）
     * @param roles    角色和权限信息（自定义 claims）
     * @return 加密的 token 字符串
     */
    public String generateToken(String username, Long userId, Map<String, Object> roles) {
        Date now = new Date();
        long expirationSeconds = jwtConfig.getExpiration();
        Date expiryDate = new Date(now.getTime() + expirationSeconds * 1000);
        String jti = UUID.randomUUID().toString();

        Map<String, Object> claims = new HashMap<>();
        if (userId != null) {
            claims.put("userId", userId);
        }
        if (roles != null) {
            claims.put("roles", roles);
        }

        String token = Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .id(jti)
                .signWith(secretKey)
                .compact();

        // Redis 可用时：将 token 存入 Redis，实现单设备登录（新 token 覆盖旧 token）
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(
                        TOKEN_PREFIX + username, token, expirationSeconds, TimeUnit.SECONDS);
                log.info("【JWT】token 已存入 Redis, username: {}, jti: {}, ttl: {}s", username, jti, expirationSeconds);
            } catch (Exception e) {
                // Redis 不可用时降级：不影响 token 生成，仅跳过存储
                log.warn("【JWT】Redis 不可用，跳过 token 存储（降级）, username: {}, error: {}", username, e.getMessage());
            }
        }

        return token;
    }

    /**
     * 生成简单 token（向下兼容）
     */
    public String generateToken(String username, Map<String, Object> roles) {
        return generateToken(username, null, roles);
    }

    /**
     * 生成最简单 token（仅包含用户名）
     */
    public String generateSimpleToken(String username) {
        return generateToken(username, null, null);
    }

    /**
     * 解析 JWT token
     *
     * @param token 加密的 token 字符串
     * @return Claims - 包含所有声明信息
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 验证 token 是否有效
     * <p>
     * 检查顺序：
     * 1. 验证签名 + 过期时间（JJWT 内置）
     * 2. 检查 jti 是否在 Redis 黑名单中（主动吊销），Redis 不可用时降级跳过
     *
     * @param token token 字符串
     * @return true - 有效，false - 无效或已过期或已吊销
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            // 检查过期
            if (claims.getExpiration().before(new Date())) {
                return false;
            }
            // 检查 jti 黑名单（Redis 可用时）
            if (redisTemplate != null) {
                try {
                    String jti = claims.getId();
                    if (StringUtils.hasText(jti)) {
                        Boolean inBlacklist = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
                        if (Boolean.TRUE.equals(inBlacklist)) {
                            log.warn("【JWT】token 已在黑名单中，拒绝访问, jti: {}", jti);
                            return false;
                        }
                    }
                } catch (Exception redisEx) {
                    // Redis 不可用时降级：跳过黑名单校验，不影响正常鉴权
                    log.warn("【JWT】Redis 不可用，跳过黑名单校验（降级）: {}", redisEx.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取 JwtConfig（用于 Controller 层返回配置信息）
     */
    public JwtConfig getJwtConfig() {
        return jwtConfig;
    }

    /**
     * 踢下线（管理员主动将指定用户的 token 加入黑名单）
     * <p>
     * 大厂规范：
     * - 从 Redis 取出该用户当前在线 token
     * - 解析 jti 后写入黑名单（TTL = token 剩余有效期）
     * - 删除 jwt:token:{username} key，断开该用户的登录状态
     * - 用户再次请求时，Gateway 鉴权就会返回 401
     *
     * @param username 被踢下线的用户名
     * @return 操作结果描述
     */
    public String kickOut(String username) {
        if (redisTemplate == null) {
            log.warn("【踢下线】Redis 不可用，无法执行踢下线操作, username: {}", username);
            return "Redis 不可用，踢下线功能无法执行";
        }

        try {
            String tokenKey = TOKEN_PREFIX + username;
            String token = redisTemplate.opsForValue().get(tokenKey);

            if (!StringUtils.hasText(token)) {
                log.info("【踢下线】用户当前不在线, username: {}", username);
                return "用户当前不在线，无需踢下线";
            }

            // 解析 token 获取 jti 和剩余有效期，写入黑名单
            try {
                Claims claims = parseToken(token);
                String jti = claims.getId();
                long remainTtl = (claims.getExpiration().getTime() - System.currentTimeMillis()) / 1000;

                if (StringUtils.hasText(jti) && remainTtl > 0) {
                    redisTemplate.opsForValue().set(
                            BLACKLIST_PREFIX + jti, username, remainTtl, TimeUnit.SECONDS);
                    log.info("【踢下线】jti 已写入黑名单, username: {}, jti: {}, ttl: {}s",
                            username, jti, remainTtl);
                }
            } catch (Exception e) {
                // token 已过期时不需写入黑名单，直接删除 Redis key 即可
                log.debug("【踢下线】token 已过期，跳过黑名单写入, username: {}", username);
            }

            // 删除 Redis 中的在线 token，断开登录状态
            redisTemplate.delete(tokenKey);
            log.info("【踢下线】用户已撤销在线 token, username: {}", username);

            return "踢下线成功";
        } catch (Exception e) {
            log.error("【踢下线】Redis 操作异常, username: {}, error: {}", username, e.getMessage());
            return "踢下线失败，Redis 连接异常";
        }
    }
}
