package cn.itzixiao.interview.security.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * 网关 JWT 验证服务
 * <p>
 * 职责：网关层只做验证，不做签发
 * <p>
 * 大厂规范：
 * 1. 一次 parseToken 获取全部 Claims，避免重复解析
 * 2. jti（JWT ID）用于黑名单标识，生成 Token 时必须写入
 * 3. 封装实验性小方法，方便 filter 一次调用得到全部信息
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
public class GatewayJwtService {

    private final SecretKey secretKey;

    public GatewayJwtService(String secretKey) {
        // 将字符串密钥转换为 SecretKey 对象
        this.secretKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 验证 token 是否有效
     *
     * @param token JWT token 字符串
     * @return true - 有效，false - 无效或已过期
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 JWT token，获取全部 Claims
     * <p>
     * 大厂规范：一次解析，多次使用，避免 filter 中重复解析 Token
     *
     * @param token JWT token 字符串
     * @return Claims
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Claims 提取用户名（subject）
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从 Claims 提取角色信息
     */
    public Object getRolesFromToken(String token) {
        return parseToken(token).get("roles");
    }

    /**
     * 从 Claims 提取用户 ID
     * <p>
     * 优先读取 userId 字段（数字 ID），没有则回退 subject（用户名）
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        return userId != null ? userId.toString() : claims.getSubject();
    }

    /**
     * 从 Claims 提取 JWT ID（jti）
     * <p>
     * jti 用于黑名单匹配，必须在签发时写入
     */
    public String getJtiFromToken(String token) {
        return parseToken(token).getId();
    }

    /**
     * 获取 Token 剩余有效秒数（用于黑名单 TTL）
     *
     * @param token JWT token 字符串
     * @return 剩余秒数，如已过期返回 0
     */
    public long getRemainTtlSeconds(String token) {
        try {
            Date expiration = parseToken(token).getExpiration();
            long remainMs = expiration.getTime() - System.currentTimeMillis();
            return remainMs > 0 ? remainMs / 1000 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
