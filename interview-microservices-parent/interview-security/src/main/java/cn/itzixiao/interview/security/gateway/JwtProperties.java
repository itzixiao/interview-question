package cn.itzixiao.interview.security.gateway;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性
 * <p>
 * 配置项：security.jwt.*
 * <p>
 * 注意：网关和业务服务需要使用相同的密钥才能验证 Token
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥
     * <p>
     * 要求：
     * 1. HS256 算法至少需要 256 位（32 字节）
     * 2. 生产环境应使用环境变量或配置中心
     * 3. 所有服务必须使用相同密钥
     */
    private String secretKey = "mySecretKeyForJWTGenerationMustBeLongEnoughAndSecure123456";

    /**
     * Access Token 过期时间（秒）
     * 默认 1 小时
     */
    private Long expiration = 3600L;

    /**
     * Refresh Token 过期时间（秒）
     * 默认 7 天
     */
    private Long refreshExpiration = 604800L;

    /**
     * Token 签发者
     */
    private String issuer = "interview-system";

    /**
     * Token 类型
     */
    private String tokenType = "Bearer";
}
