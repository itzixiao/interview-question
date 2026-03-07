package cn.itzixiao.interview.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * Gateway 限流配置 - KeyResolver 示例
 *
 * ┌─────────────────────────────────────────────────────────────────────────────┐
 * │                        Gateway 限流原理                                      │
 * │                                                                             │
 * │  基于 Redis + 令牌桶算法：                                                   │
 * │                                                                             │
 * │  ┌───────────────────────────────────────────────────────────────────┐     │
 * │  │                       令牌桶算法                                   │     │
 * │  │                                                                   │     │
 * │  │   ┌─────────────┐                                                │     │
 * │  │   │   令牌生成器 │  按 replenishRate 速率持续生成令牌             │     │
 * │  │   └──────┬──────┘                                                │     │
 * │  │          │                                                       │     │
 * │  │          ↓                                                       │     │
 * │  │   ┌─────────────────────────────────┐                           │     │
 * │  │   │         令牌桶                   │  容量 = burstCapacity    │     │
 * │  │   │  ┌───┬───┬───┬───┬───┬───┬───┐  │                           │     │
 * │  │   │  │ ● │ ● │ ● │ ● │ ○ │ ○ │ ○ │  │  ● = 可用令牌             │     │
 * │  │   │  └───┴───┴───┴───┴───┴───┴───┘  │  ○ = 空位                 │     │
 * │  │   └──────────────┬──────────────────┘                           │     │
 * │  │                  │                                               │     │
 * │  │                  ↓                                               │     │
 * │  │   请求到达 → 获取令牌 → 有令牌则通过，无令牌则拒绝（429）        │     │
 * │  │                                                                   │     │
 * │  └───────────────────────────────────────────────────────────────────┘     │
 * │                                                                             │
 * │  配置参数：                                                                  │
 * │  - replenishRate：每秒补充令牌数（平均QPS）                                  │
 * │  - burstCapacity：令牌桶容量（允许的突发流量）                               │
 * │  - requestedTokens：每个请求消耗的令牌数（默认1）                            │
 * │                                                                             │
 * │  KeyResolver：限流维度                                                       │
 * │  - 按 IP 限流：同一 IP 共享令牌桶                                            │
 * │  - 按用户限流：同一用户共享令牌桶                                            │
 * │  - 按接口限流：同一接口共享令牌桶                                            │
 * │                                                                             │
 * └─────────────────────────────────────────────────────────────────────────────┘
 *
 * 使用方式（application.yml）：
 *
 * spring:
 *   cloud:
 *     gateway:
 *       routes:
 *         - id: rate-limit-route
 *           uri: lb://some-service
 *           predicates:
 *             - Path=/api/limit/**
 *           filters:
 *             - name: RequestRateLimiter
 *               args:
 *                 redis-rate-limiter.replenishRate: 10   # 每秒10个请求
 *                 redis-rate-limiter.burstCapacity: 20   # 最大突发20个
 *                 key-resolver: "#{@ipKeyResolver}"      # 按IP限流
 *
 * 需要添加依赖：
 * <dependency>
 *     <groupId>org.springframework.boot</groupId>
 *     <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
 * </dependency>
 */
@Configuration
public class RateLimiterConfig {

    /**
     * 按 IP 限流
     * 同一 IP 地址的请求共享令牌桶
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }

    /**
     * 按用户 ID 限流
     * 同一用户的请求共享令牌桶
     * 需要先通过认证过滤器将用户信息放入请求头或属性中
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // 从请求头获取用户ID（由认证过滤器添加）
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }
            return Mono.just(userId);
        };
    }

    /**
     * 按接口路径限流
     * 同一接口的请求共享令牌桶
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            return Mono.just(path);
        };
    }

    /**
     * 组合限流（IP + 接口）
     * 每个 IP 对每个接口分别限流
     */
    @Bean
    public KeyResolver ipPathKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            String path = exchange.getRequest().getPath().value();
            return Mono.just(ip + ":" + path);
        };
    }
}
