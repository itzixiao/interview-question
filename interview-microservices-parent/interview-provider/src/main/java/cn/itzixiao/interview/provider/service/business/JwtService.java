package cn.itzixiao.interview.provider.service.business;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具服务
 * <p>
 * 作用：生成和解析 JWT token
 * <p>
 * 核心方法：
 * 1. generateToken() - 生成 token
 * 2. parseToken() - 解析 token
 * 3. validateToken() - 验证 token 是否有效
 */
@Service
public class JwtService {

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private final SecretKey secretKey;

    public JwtService(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * 生成 JWT token
     *
     * @param username 用户名（作为 subject）
     * @param roles    角色列表（自定义 claims）
     * @return 加密的 token 字符串
     */
    public String generateToken(String username, Map<String, Object> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration * 1000);

        // 构建 JWT claims
        Map<String, Object> claims = new HashMap<>();
        if (roles != null) {
            claims.put("roles", roles);
        }

        // 使用 Jwts 构建器生成 token
        return Jwts.builder()
                .claims(claims)                          // 自定义 claims
                .subject(username)                       // 主题（用户名）
                .issuer(issuer)                          // 签发者
                .issuedAt(now)                           // 签发时间
                .expiration(expiryDate)                  // 过期时间
                .signWith(secretKey)                     // 签名算法
                .compact();
    }

    /**
     * 生成简单 token（仅包含用户名）
     *
     * @param username 用户名
     * @return token 字符串
     */
    public String generateSimpleToken(String username) {
        return generateToken(username, null);
    }

    /**
     * 解析 JWT token
     *
     * @param token 加密的 token 字符串
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
     * @param token token 字符串
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
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
            // 检查是否过期
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // 签名验证失败、token 过期等异常
            return false;
        }
    }
}
