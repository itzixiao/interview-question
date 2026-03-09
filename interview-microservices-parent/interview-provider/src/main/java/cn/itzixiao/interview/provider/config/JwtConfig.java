package cn.itzixiao.interview.provider.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 配置类
 * 
 * 作用：加载 JWT 密钥并生成 SecretKey 对象
 * 
 * 核心知识点：
 * 1. JWT 密钥长度要求：HS256 算法至少需要 256 位（32 字节）
 * 2. 密钥安全性：生产环境应使用环境变量或配置中心
 * 3. 密钥轮换：定期更换密钥，旧 token 继续有效直到过期
 */
@Configuration
public class JwtConfig {
    
    /**
     * 从配置文件读取 JWT 密钥
     * 开发环境：application-dev.yml
     * 生产环境：通过环境变量 JWT_SECRET_KEY 注入
     */
    @Value("${jwt.secret-key}")
    private String secretKey;
    
    /**
     * JWT 签发者
     */
    @Value("${jwt.issuer}")
    private String issuer;
    
    /**
     * JWT 过期时间（秒）
     */
    @Value("${jwt.expiration}")
    private Long expiration;
    
    /**
     * 生成 SecretKey 对象
     * 
     * @return SecretKey - 用于 JWT 签名和验证
     */
    @Bean
    public SecretKey jwtSecretKey() {
        // 将字符串密钥转换为 SecretKey 对象
        // 底层原理：使用 HMAC-SHA 算法进行签名
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }
}
