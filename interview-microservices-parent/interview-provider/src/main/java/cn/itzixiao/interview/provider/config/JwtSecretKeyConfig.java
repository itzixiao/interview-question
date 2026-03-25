package cn.itzixiao.interview.provider.config;

import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT SecretKey 配置类
 * <p>
 * 职责分离：
 * - {@link JwtConfig} 只做属性绑定（@ConfigurationProperties）
 * - 本类只做 Bean 注册（@Configuration + @Bean）
 * <p>
 * 原因：@ConfigurationProperties 与 @Bean 方法不能放在同一个类里，
 * Spring 在处理 FactoryBean 类型时会产生 "Invalid value type for
 * attribute 'factoryBeanObjectType'" 冲突。
 *
 * @author itzixiao
 * @date 2026-03-25
 */
@Configuration
@EnableConfigurationProperties(JwtConfig.class)
public class JwtSecretKeyConfig {

    /**
     * 生成 SecretKey 对象并注册到 Spring 容器
     * <p>
     * HS256 算法要求密钥至少 32 字节，建议 64 字节以上。
     *
     * @param jwtConfig JWT 配置属性
     * @return SecretKey - 用于 JWT 签名和验证
     */
    @Bean
    public SecretKey jwtSecretKey(JwtConfig jwtConfig) {
        return Keys.hmacShaKeyFor(jwtConfig.getSecretKey().getBytes(StandardCharsets.UTF_8));
    }
}
