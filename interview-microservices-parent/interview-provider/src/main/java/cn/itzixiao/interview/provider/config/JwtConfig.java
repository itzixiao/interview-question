package cn.itzixiao.interview.provider.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置属性类
 * <p>
 * 大厂规范：使用 @ConfigurationProperties 统一管理 JWT 相关配置，
 * 与 Gateway 共用 Nacos common.yml 中的 jwt.* 配置项。
 * <p>
 * 配置优先级：环境变量 JWT_SECRET_KEY > Nacos common.yml > application-dev.yml 默认值
 * <p>
 * 注意：此类仅做属性绑定，不包含 @Bean 方法。
 * SecretKey Bean 定义在 {@link JwtSecretKeyConfig} 中。
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 签名密钥
     * 开发环境：application-dev.yml 中直接写固定值
     * 生产环境：通过环境变量 JWT_SECRET_KEY 注入
     */
    private String secretKey;

    /**
     * JWT 签发者（必须与 Gateway 侧一致）
     */
    private String issuer = "interview-provider";

    /**
     * Access Token 过期时间（秒），默认 2 小时
     */
    private Long expiration = 7200L;

    /**
     * Refresh Token 过期时间（秒），默认 7 天
     */
    private Long refreshExpiration = 604800L;

    /**
     * Token 类型前缀
     */
    private String tokenType = "Bearer";
}
