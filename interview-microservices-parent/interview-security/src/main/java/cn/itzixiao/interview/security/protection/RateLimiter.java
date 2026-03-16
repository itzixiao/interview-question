package cn.itzixiao.interview.security.protection;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 限流防刷工具类 - 防止接口被恶意调用
 * <p>
 * 攻击原理：
 * - 攻击者短时间内发起大量请求
 * - 耗尽服务器资源，导致服务不可用（DDoS）
 * - 恶意刷接口（如：刷优惠券、刷短信）
 * <p>
 * 防护方案：
 * 1. 固定窗口计数
 * 2. 滑动窗口计数
 * 3. 漏桶算法
 * 4. 令牌桶算法（推荐）
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class RateLimiter {

    /**
     * 用户访问频率缓存（Guava Cache）
     */
    private final Cache<String, AccessCounter> userAccessCache;

    public RateLimiter() {
        this.userAccessCache = CacheBuilder.newBuilder()
                .maximumSize(10000)  // 最多缓存 10000 个用户
                .expireAfterWrite(1, TimeUnit.MINUTES)  // 1 分钟后过期
                .build();
    }

    /**
     * 限流检查（固定窗口算法）
     *
     * @param userId        用户 ID
     * @param limit         限制次数
     * @param windowSeconds 时间窗口（秒）
     * @return true-允许访问，false-触发限流
     */
    public boolean tryAcquire(String userId, int limit, long windowSeconds) {
        try {
            AccessCounter counter = userAccessCache.get(userId, AccessCounter::new);

            synchronized (counter) {
                long now = System.currentTimeMillis();
                long windowStart = now - windowSeconds * 1000;

                // 重置窗口
                if (counter.windowStart < windowStart) {
                    counter.windowStart = now;
                    counter.count = 0;
                }

                // 增加计数
                counter.count++;

                // 检查是否超过限制
                if (counter.count > limit) {
                    log.warn("Rate limit exceeded for user: {}, count: {}, limit: {}",
                            userId, counter.count, limit);
                    return false;
                }

                return true;
            }
        } catch (Exception e) {
            log.error("Rate limiter error", e);
            return true; // 出错时放行
        }
    }

    /**
     * 内部类：访问计数器
     */
    private static class AccessCounter {
        long windowStart = System.currentTimeMillis();
        long count = 0;
    }
}
