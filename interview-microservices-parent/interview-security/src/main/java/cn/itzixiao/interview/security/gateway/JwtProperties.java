package cn.itzixiao.interview.security.gateway;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gateway JWT 配置属性
 * <p>
 * 配置项：jwt.*（与 interview-provider 使用相同前缀，由 Nacos common.yml 统一下发）
 * <p>
 * 大厂规范：
 * 1. 密钥由配置中心统一管理，禁止硬编码
 * 2. 生产环境通过环境变量 JWT_SECRET_KEY 覆盖
 * 3. Gateway 只做验证，Provider 负责签发，共享同一密钥
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * JWT 签名密钥
     * 来源优先级：环境变量 JWT_SECRET_KEY > Nacos common.yml > 此处默认值
     */
    private String secretKey = "itzixiao-interview-system-secret-key-2026-must-be-32-bytes";

    /**
     * Access Token 过期时间（秒），默认 2 小时
     */
    private Long expiration = 7200L;

    /**
     * Refresh Token 过期时间（秒），默认 7 天
     */
    private Long refreshExpiration = 604800L;

    /**
     * Token 签发者
     */
    private String issuer = "interview-provider";

    /**
     * Token 类型
     */
    private String tokenType = "Bearer";
}
