package cn.itzixiao.interview.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * 网关鉴权自动配置类
 * <p>
 * 功能：
 * 1. 自动配置 JWT 验证服务
 * 2. 自动配置白名单属性
 * 3. 自动配置鉴权过滤器（order = -100，最高优先级）
 * 4. 可选配置 Token 黑名单服务（依赖 Redis，存在时自动装配）
 * <p>
 * 启用条件：
 * 1. 存在 Gateway 类（spring-cloud-starter-gateway）
 * 2. 响应式 Web 应用
 * 3. 配置项 gateway.auth.enabled=true（默认启用）
 *
 * @author itzixiao
 * @date 2026-03-20
 */
@Slf4j
@Configuration
@ConditionalOnClass({DispatcherHandler.class, GlobalFilter.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "gateway.auth", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(GatewayAutoConfiguration.class)
@EnableConfigurationProperties({WhiteListProperties.class, JwtProperties.class})
public class GatewayAuthAutoConfiguration {

    /**
     * 网关 JWT 验证服务
     *
     * @param jwtProperties JWT 配置属性
     * @return GatewayJwtService
     */
    @Bean
    public GatewayJwtService gatewayJwtService(JwtProperties jwtProperties) {
        log.info("【网关鉴权】初始化 JWT 验证服务, issuer: {}, expiration: {}s",
                jwtProperties.getIssuer(), jwtProperties.getExpiration());
        return new GatewayJwtService(jwtProperties.getSecretKey());
    }

    /**
     * Token 黑名单服务（可选）
     * <p>
     * 仅在 Spring 容器中存在 StringRedisTemplate 时才注册，
     * 若 Gateway 未引入 Redis 依赖，则黑名单功能自动禁用，不影响正常鉴权。
     *
     * @param redisTemplate Redis 操作模板
     * @return TokenBlacklistService
     */
    @Bean
    @ConditionalOnMissingBean(TokenBlacklistService.class)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnBean(StringRedisTemplate.class)
    public TokenBlacklistService tokenBlacklistService(StringRedisTemplate redisTemplate) {
        log.info("【网关鉴权】初始化 Token 黑名单服务（Redis 模式）");
        return new TokenBlacklistService(redisTemplate);
    }

    /**
     * 网关鉴权过滤器
     * <p>
     * order = -100，确保在所有业务过滤器之前执行。
     * TokenBlacklistService 为可选注入，未配置 Redis 时黑名单校验自动跳过。
     *
     * @param whiteListProperties  白名单配置属性
     * @param jwtService           JWT 验证服务
     * @param tokenBlacklistService Token 黑名单服务（可选，依赖 Redis）
     * @return AuthenticationFilter
     */
    @Bean
    public AuthenticationFilter authenticationFilter(
            WhiteListProperties whiteListProperties,
            GatewayJwtService jwtService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            TokenBlacklistService tokenBlacklistService) {
        log.info("【网关鉴权】初始化鉴权过滤器, 白名单路径数量: {}, 黑名单服务: {}",
                whiteListProperties.getPaths() != null ? whiteListProperties.getPaths().size() : 0,
                tokenBlacklistService != null ? "已启用" : "未启用（无 Redis）");
        return new AuthenticationFilter(whiteListProperties, jwtService, tokenBlacklistService);
    }
}
