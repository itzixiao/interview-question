package cn.itzixiao.interview.gateway.config;

import cn.itzixiao.interview.security.gateway.TokenBlacklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Gateway Redis 鉴权配置
 * <p>
 * 本类在 interview-gateway 模块中，该模块有完整的 spring-data-redis 依赖，
 * 可安全引用 StringRedisTemplate。
 * <p>
 * 通过 @Primary 覆盖 interview-security 中的 TokenBlacklistService Bean，
 * 直接持有 StringRedisTemplate，不需要懒加载或反射，性能更好、逻辑更简单。
 *
 * @author itzixiao
 * @date 2026-03-25
 */
@Slf4j
@Configuration
public class GatewayRedisAuthConfig {

    /**
     * 覆盖 interview-security 的 TokenBlacklistService，直接注入 StringRedisTemplate
     * Gateway 模块有 spring-data-redis 依赖，此处类型引用完全安全
     */
    @Bean
    @Primary
    @ConditionalOnBean(StringRedisTemplate.class)
    public TokenBlacklistService tokenBlacklistService(
            StringRedisTemplate stringRedisTemplate,
            ApplicationContext context) {
        log.info("【网关鉴权】Gateway 本地 TokenBlacklistService 初始化（直接注入 StringRedisTemplate）");
        return new DirectRedisTokenBlacklistService(stringRedisTemplate);
    }

    /**
     * 直接使用 StringRedisTemplate 的 TokenBlacklistService 实现
     * 替代 interview-security 中基于懒加载反射的实现，更高效
     */
    static class DirectRedisTokenBlacklistService extends TokenBlacklistService {

        private static final String BLACKLIST_PREFIX = "jwt:blacklist:";
        private static final String TOKEN_PREFIX = "jwt:token:";

        private final StringRedisTemplate redisTemplate;

        DirectRedisTokenBlacklistService(StringRedisTemplate redisTemplate) {
            super(null);  // 父类 ApplicationContext 不使用
            this.redisTemplate = redisTemplate;
        }

        @Override
        public boolean isBlacklisted(String jti) {
            if (jti == null || jti.isEmpty()) return false;
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
            } catch (Exception e) {
                log.warn("【黑名单】Redis 操作异常，降级放行, jti: {}, error: {}", jti, e.getMessage());
                return false;
            }
        }

        @Override
        public boolean isOnlineTokenValid(String username, String token) {
            if (username == null || username.isEmpty() || token == null || token.isEmpty()) return false;
            try {
                String onlineToken = redisTemplate.opsForValue().get(TOKEN_PREFIX + username);
                if (onlineToken == null) {
                    log.warn("【鉴权】用户当前无在线 token（已被踢下线）, username: {}", username);
                    return false;
                }
                boolean valid = onlineToken.equals(token);
                if (!valid) {
                    log.warn("【鉴权】token 与在线 token 不一致（已被新登录覆盖）, username: {}", username);
                }
                return valid;
            } catch (Exception e) {
                log.warn("【鉴权】Redis 操作异常，降级放行, username: {}, error: {}", username, e.getMessage());
                return true;
            }
        }

        @Override
        public void addToBlacklist(String jti, String username, long remainTtl) {
            if (remainTtl <= 0) return;
            try {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, username, remainTtl, TimeUnit.SECONDS);
                log.info("【黑名单】Token 已加入黑名单, jti: {}, username: {}, ttl: {}s", jti, username, remainTtl);
            } catch (Exception e) {
                log.warn("【黑名单】加入黑名单失败（降级）, jti: {}, error: {}", jti, e.getMessage());
            }
        }
    }
}
