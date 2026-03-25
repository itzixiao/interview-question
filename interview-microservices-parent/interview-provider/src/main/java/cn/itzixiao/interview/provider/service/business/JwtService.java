package cn.itzixiao.interview.provider.service.business;

import cn.itzixiao.interview.provider.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具服务
 * <p>
 * 大厂规范：
 * 1. 生成 Token 时写入 jti（JWT ID），支持主动吹销（黑名单）
 * 2. 生成 Token 时写入 userId（数字 ID），直接透传给下游服务无需再次查表
 * 3. 依赖 JwtConfig（@ConfigurationProperties）统一管理配置
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;

    public JwtService(JwtConfig jwtConfig, SecretKey secretKey) {
        this.jwtConfig = jwtConfig;
        this.secretKey = secretKey;
    }

    /**
     * 生成 JWT token（标准版，包含 userId + jti）
     *
     * @param username 用户名（作为 subject）
     * @param userId   用户数字 ID（透传给下游，政策表关联等场景使用）
     * @param roles    角色和权限信息（自定义 claims）
     * @return 加密的 token 字符串
     */
    public String generateToken(String username, Long userId, Map<String, Object> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration() * 1000);

        Map<String, Object> claims = new HashMap<>();
        // 写入 userId：下游服务通过 X-User-Id 头获取，无需再次查表
        if (userId != null) {
            claims.put("userId", userId);
        }
        if (roles != null) {
            claims.put("roles", roles);
        }

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiryDate)
                // jti：JWT 唯一标识，用于黑名单匹配（主动吹销必需）
                .id(UUID.randomUUID().toString())
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成简单 token（安共向下兼容）
     *
     * @param username 用户名
     * @return token 字符串
     */
    public String generateToken(String username, Map<String, Object> roles) {
        return generateToken(username, null, roles);
    }

    /**
     * 生成最简单 token（仅包含用户名）
     *
     * @param username 用户名
     * @return token 字符串
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
     *
     * @param token token 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 验证 token 是否有效
     *
     * @param token token 字符串
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
     * 获取 JwtConfig（用于 Controller 层返回配置信息）
     *
     * @return JwtConfig
     */
    public JwtConfig getJwtConfig() {
        return jwtConfig;
    }
}
