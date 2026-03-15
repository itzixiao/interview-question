package cn.itzixiao.interview.security.oauth2;

import lombok.Data;

/**
 * JWT Token 封装
 * 
 * OAuth2.0 四种授权模式：
 * 1. 授权码模式（Authorization Code）- 最安全，适用于 Web 应用
 * 2. 简化模式（Implicit）- 不安全，已废弃
 * 3. 密码模式（Resource Owner Password Credentials）- 仅受信任应用使用
 * 4. 客户端模式（Client Credentials）- 机器对机器通信
 * 
 * JWT Token 结构：
 * - Header：算法和类型
 * - Payload：声明（用户信息、过期时间等）
 * - Signature：签名防篡改
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
public class JwtToken {
    
    /**
     * 访问令牌
     */
    private String accessToken;
    
    /**
     * 刷新令牌
     */
    private String refreshToken;
    
    /**
     * 令牌类型（Bearer）
     */
    private String tokenType;
    
    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
    
    /**
     * 刷新令牌过期时间
     */
    private Long refreshExpiresIn;
    
    /**
     * 权限范围
     */
    private String scope;
    
    public static JwtToken of(String accessToken, String refreshToken, long expiresIn) {
        JwtToken token = new JwtToken();
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setTokenType("Bearer");
        token.setExpiresIn(expiresIn);
        token.setRefreshExpiresIn(expiresIn * 7); // 刷新令牌 7 倍有效期
        return token;
    }
}
