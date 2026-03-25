package cn.itzixiao.interview.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * 网关鉴权自动配置类
 * <p>
 * 功能：
 * 1. 自动配置 JWT 验证服务
 * 2. 自动配置白名单属性
 * 3. 自动配置鉴权过滤器（order = -100，最高优先级）
 * 4. 配置 Token 黑名单服务（注入 ApplicationContext，懒加载 StringRedisTemplate）
 * <p>
 * 设计原则：
 * - 方法签名中不直接引用 StringRedisTemplate，避免 Redis jar 不存在时 NoClassDefFoundError
 * - TokenBlacklistService 持有 ApplicationContext，首次 Redis 操作时懒加载 StringRedisTemplate
 * - 避免自动配置顺序问题：本类先于 Redis 自动配置执行，直接注入 StringRedisTemplate 会报 "No bean"
 * - 所有 Redis 操作均有 try-catch 降级，运行时故障不影响正常请求
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "gateway.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(GatewayAutoConfiguration.class)
@EnableConfigurationProperties({WhiteListProperties.class, JwtProperties.class})
public class GatewayAuthAutoConfiguration {

    /**
     * 网关 JWT 验证服务
     */
    @Bean
    public GatewayJwtService gatewayJwtService(JwtProperties jwtProperties) {
        log.info("【网关鉴权】初始化 JWT 验证服务, issuer: {}, expiration: {}s",
                jwtProperties.getIssuer(), jwtProperties.getExpiration());
        return new GatewayJwtService(jwtProperties.getSecretKey());
    }

    /**
     * Token 黑名单服务
     * <p>
     * 注入 ApplicationContext 而非 StringRedisTemplate，由 TokenBlacklistService 内部懒加载。
     * 原因：本配置类通过 @AutoConfigureBefore 先于 Redis 自动配置执行，
     * 此时 StringRedisTemplate Bean 尚未注册，直接注入会报 "No bean named 'stringRedisTemplate'"。
     * 懒加载方案：首次实际 Redis 操作时才从容器获取 StringRedisTemplate，此时 Redis 已完成配置。
     */
    @Bean
    @ConditionalOnMissingBean(TokenBlacklistService.class)
    public TokenBlacklistService tokenBlacklistService(ApplicationContext context) {
        log.info("【网关鉴权】初始化 Token 黑名单服务（懒加载 Redis，首次请求时建立连接）");
        return new TokenBlacklistService(context);
    }

    /**
     * 网关鉴权过滤器
     * <p>
     * order = -100，确保在所有业务过滤器之前执行。
     */
    @Bean
    public AuthenticationFilter authenticationFilter(
            WhiteListProperties whiteListProperties,
            GatewayJwtService jwtService,
            @Autowired(required = false) TokenBlacklistService tokenBlacklistService) {
        log.info("【网关鉴权】初始化鉴权过滤器, 白名单路径数量: {}, 黑名单服务: 已启用（Redis 懒加载）",
                whiteListProperties.getPaths() != null ? whiteListProperties.getPaths().size() : 0);
        return new AuthenticationFilter(whiteListProperties, jwtService, tokenBlacklistService);
    }
}
