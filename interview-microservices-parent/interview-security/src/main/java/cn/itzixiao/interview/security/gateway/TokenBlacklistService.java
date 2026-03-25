package cn.itzixiao.interview.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务（基于 Redis）
 * <p>
 * 大厂规范：JWT 本身无法主动失效，通过 Redis 黑名单实现主动吊销。
 * <p>
 * 核心设计：
 * 1. Key：jwt:blacklist:{jti}，Value：username
 * 2. TTL = Token 剩余有效时间，到期自动清除，无需额外维护
 * 3. 仅存 jti（JWT ID）而非完整 Token，节省内存
 * <p>
 * 使用场景：
 * - 用户主动退出登录
 * - 管理员强制下线
 * - 修改密码后使旧 Token 失效
 *
 * @author itzixiao
 * @date 2026-03-25
 */
@Slf4j
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将 Token 加入黑名单
     *
     * @param jti        JWT ID（唯一标识）
     * @param username   用户名（用于日志追踪）
     * @param remainTtl  Token 剩余有效时间（秒），TTL 与 Token 保持一致，到期自动清除
     */
    public void addToBlacklist(String jti, String username, long remainTtl) {
        if (remainTtl <= 0) {
            // Token 已过期，无需加入黑名单
            log.debug("【黑名单】Token 已过期，无需加入黑名单, jti: {}", jti);
            return;
        }
        String key = BLACKLIST_PREFIX + jti;
        redisTemplate.opsForValue().set(key, username, remainTtl, TimeUnit.SECONDS);
        log.info("【黑名单】Token 已加入黑名单, jti: {}, username: {}, ttl: {}s", jti, username, remainTtl);
    }

    /**
     * 检查 Token 是否在黑名单中
     *
     * @param jti JWT ID
     * @return true - 在黑名单（已被吊销），false - 正常
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        Boolean exists = redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}
