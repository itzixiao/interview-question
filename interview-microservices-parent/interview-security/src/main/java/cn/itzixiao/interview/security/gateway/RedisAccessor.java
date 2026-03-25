package cn.itzixiao.interview.security.gateway;

import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * Redis 访问适配器
 * <p>
 * 该类封装所有 StringRedisTemplate 操作，与 TokenBlacklistService 隔离在不同的类文件中。
 * 当 Redis jar 不在 classpath 时，本类不会被加载（ClassLoader 懒加载机制）。
 * TokenBlacklistService 主类字节码中不出现任何 Redis 类型引用，
 * JVM 内省 getDeclaredMethods 时不触发 Redis 类加载，彻底规避 NoClassDefFoundError。
 *
 * @author itzixiao
 * @date 2026-03-25
 */
class RedisAccessor {

    private final StringRedisTemplate redisTemplate;

    private RedisAccessor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 工厂方法：从 ApplicationContext 获取 StringRedisTemplate 并创建访问器
     * Redis jar 不存在或 Bean 未就绪时抛出异常，由调用方处理
     */
    static RedisAccessor create(ApplicationContext context) {
        StringRedisTemplate template = context.getBean("stringRedisTemplate", StringRedisTemplate.class);
        return new RedisAccessor(template);
    }

    Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    void delete(String key) {
        redisTemplate.delete(key);
    }
}
