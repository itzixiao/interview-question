package cn.itzixiao.interview.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.DispatcherHandler;

/**
 * 网关鉴权自动配置类
 * <p>
 * 功能：
 * 1. 自动配置 JWT 验证服务
 * 2. 自动配置白名单属性
 * 3. 自动配置鉴权过滤器
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
        log.info("【网关鉴权】初始化 JWT 验证服务, issuer: {}", jwtProperties.getIssuer());
        return new GatewayJwtService(jwtProperties.getSecretKey());
    }

    /**
     * 网关鉴权过滤器
     *
     * @param whiteListProperties 白名单配置属性
     * @param jwtService          JWT 验证服务
     * @return AuthenticationFilter
     */
    @Bean
    public AuthenticationFilter authenticationFilter(
            WhiteListProperties whiteListProperties,
            GatewayJwtService jwtService) {
        log.info("【网关鉴权】初始化鉴权过滤器, 白名单路径数量: {}", 
                whiteListProperties.getPaths() != null ? whiteListProperties.getPaths().size() : 0);
        return new AuthenticationFilter(whiteListProperties, jwtService);
    }
}
