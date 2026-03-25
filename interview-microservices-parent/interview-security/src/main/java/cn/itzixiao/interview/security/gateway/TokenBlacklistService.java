package cn.itzixiao.interview.security.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Token 黑名单服务
 * <p>
 * 【编译时零 Redis 依赖】：本类不引用任何 Redis 类型（无 import、无局部变量类型声明、无方法签名引用）
 * JVM 内省 getDeclaredMethods 时完全不触发 Redis 类加载，彻底规避 NoClassDefFoundError。
 * <p>
 * 运行时通过 {@link RedisAccessor} 内部类持有 Redis 操作能力，该内部类在独立的类文件中加载，
 * 仅在 Redis jar 存在时才被实例化。
 * <p>
 * Redis key 规范（与 Provider 端 JwtService 保持一致）：
 * - jwt:blacklist:{jti}   Token 黑名单，TTL = Token 剩余有效期
 * - jwt:token:{username}  当前在线 Token，用于单设备登录 / 踢下线校验
 *
 * @author itzixiao
 * @date 2026-03-25
 */
@Slf4j
public class TokenBlacklistService {

    static final String BLACKLIST_PREFIX = "jwt:blacklist:";
    static final String TOKEN_PREFIX = "jwt:token:";

    private final ApplicationContext applicationContext;
    /** 懒加载的 Redis 访问器，成功获取后缓存，失败不缓存（下次请求重试） */
    private final AtomicReference<RedisAccessor> accessorRef = new AtomicReference<>();

    public TokenBlacklistService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * 懒加载获取 RedisAccessor
     * 每次失败不缓存，下次请求重试，直到 Redis 自动配置完成后 Bean 就绪
     */
    private RedisAccessor getAccessor() {
        RedisAccessor cached = accessorRef.get();
        if (cached != null) {
            return cached;
        }
        try {
            RedisAccessor accessor = RedisAccessor.create(applicationContext);
            if (accessorRef.compareAndSet(null, accessor)) {
                log.info("【黑名单】Redis 访问器已就绪，Token 黑名单 / 踢下线校验功能正式启用");
            }
            return accessorRef.get();
        } catch (Exception e) {
            log.debug("【黑名单】Redis 暂未就绪，本次降级放行（将在下次请求重试）: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 Token 加入黑名单（登出 / 踢下线时调用）
     */
    public void addToBlacklist(String jti, String username, long remainTtl) {
        if (remainTtl <= 0) {
            return;
        }
        RedisAccessor accessor = getAccessor();
        if (accessor == null) {
            log.warn("【黑名单】Redis 不可用，加入黑名单失败（降级）, jti: {}", jti);
            return;
        }
        try {
            accessor.set(BLACKLIST_PREFIX + jti, username, remainTtl, TimeUnit.SECONDS);
            log.info("【黑名单】Token 已加入黑名单, jti: {}, username: {}, ttl: {}s", jti, username, remainTtl);
        } catch (Exception e) {
            log.warn("【黑名单】操作异常，加入黑名单失败（降级）, jti: {}, error: {}", jti, e.getMessage());
        }
    }

    /**
     * 检查 Token jti 是否在黑名单中
     * Redis 不可用时降级返回 false（放行）
     */
    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        RedisAccessor accessor = getAccessor();
        if (accessor == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(accessor.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception e) {
            log.warn("【黑名单】Redis 不可用，跳过黑名单校验（降级放行）, jti: {}, error: {}", jti, e.getMessage());
            return false;
        }
    }

    /**
     * 校验在线 Token 一致性（单设备登录 / 踢下线核心校验）
     * <p>
     * 校验逻辑：
     * 1. Redis 中无 jwt:token:{username} → 已被踢下线 → 返回 false → 401
     * 2. 存在但与请求 token 不一致 → 已被新登录覆盖 → 返回 false → 401
     * 3. 一致 → 返回 true → 放行
     * <p>
     * 降级：Redis 不可用时返回 true（放行），避免 Redis 故障导致全量 401
     */
    public boolean isOnlineTokenValid(String username, String token) {
        if (username == null || username.isEmpty() || token == null || token.isEmpty()) {
            return false;
        }
        RedisAccessor accessor = getAccessor();
        if (accessor == null) {
            log.warn("【鉴权】Redis 不可用，跳过在线 token 校验（降级放行）, username: {}", username);
            return true;
        }
        try {
            String onlineToken = accessor.get(TOKEN_PREFIX + username);
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
}
