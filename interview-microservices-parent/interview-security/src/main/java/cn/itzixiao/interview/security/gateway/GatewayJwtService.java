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
 * 作用：在网关层验证 JWT token 的有效性
 * <p>
 * 核心功能：
 * 1. 解析 token 获取用户信息
 * 2. 验证 token 是否有效（签名验证 + 过期检查）
 * 3. 提取用户信息传递给下游服务
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
            // 检查是否过期
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // 签名验证失败、token 过期等异常
            log.warn("JWT token 验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析 JWT token
     *
     * @param token JWT token 字符串
     * @return Claims - 包含所有声明信息
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)          // 设置签名密钥
                .build()
                .parseSignedClaims(token)       // 解析并验证签名
                .getPayload();                  // 获取 claims payload
    }

    /**
     * 从 token 中获取用户名
     *
     * @param token JWT token 字符串
     * @return 用户名（subject）
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从 token 中获取角色信息
     *
     * @param token JWT token 字符串
     * @return 角色信息对象
     */
    public Object getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("roles");
    }

    /**
     * 从 token 中获取用户 ID
     *
     * @param token JWT token 字符串
     * @return 用户 ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        return userId != null ? userId.toString() : claims.getSubject();
    }
}
