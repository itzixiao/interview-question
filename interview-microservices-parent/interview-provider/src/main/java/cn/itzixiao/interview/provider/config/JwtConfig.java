package cn.itzixiao.interview.provider.config;

import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 配置类
 * <p>
 * 大厂规范：使用 @ConfigurationProperties 统一管理 JWT 相关配置，
 * 与 Gateway 共用 Nacos common.yml 中的 jwt.* 配置项。
 * <p>
 * 配置优先级：环境变量 JWT_SECRET_KEY > Nacos common.yml > application-dev.yml 默认值
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 签名密钥
     * 开发环境：application-dev.yml 中的 ${jwt.secret-key:...} 回退默认值
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

    /**
     * 生成 SecretKey 对象（注册到 Spring 容器）
     * <p>
     * HS256 算法要求密钥至少 32 字节，建议 64 字节以上。
     *
     * @return SecretKey - 用于 JWT 签名和验证
     */
    @Bean
    public SecretKey jwtSecretKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
