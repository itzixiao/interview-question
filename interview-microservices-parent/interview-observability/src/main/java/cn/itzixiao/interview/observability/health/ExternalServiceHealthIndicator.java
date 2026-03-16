package cn.itzixiao.interview.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义健康检查指示器 - 外部服务依赖健康状态
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class ExternalServiceHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // 模拟检查外部服务（如 Redis、MQ、第三方 API）
            boolean redisAvailable = checkRedis();
            boolean mqAvailable = checkMessageQueue();
            boolean thirdPartyApiAvailable = checkThirdPartyAPI();

            if (!redisAvailable || !mqAvailable || !thirdPartyApiAvailable) {
                StringBuilder details = new StringBuilder();

                if (!redisAvailable) {
                    log.warn("Redis 服务不可用");
                    details.append("Redis: DOWN; ");
                }

                if (!mqAvailable) {
                    log.warn("消息队列服务不可用");
                    details.append("MQ: DOWN; ");
                }

                if (!thirdPartyApiAvailable) {
                    log.warn("第三方 API 不可用");
                    details.append("ThirdPartyAPI: DOWN; ");
                }

                return Health.down()
                        .withDetail("externalServices", details.toString())
                        .build();
            }

            return Health.up()
                    .withDetail("redis", "UP")
                    .withDetail("messageQueue", "UP")
                    .withDetail("thirdPartyAPI", "UP")
                    .build();

        } catch (Exception e) {
            log.error("外部服务健康检查失败", e);
            return Health.down(e).build();
        }
    }

    /**
     * 模拟检查 Redis
     */
    private boolean checkRedis() {
        // 实际应该执行 redisTemplate.executePing()
        return Math.random() > 0.1; // 90% 概率可用
    }

    /**
     * 模拟检查消息队列
     */
    private boolean checkMessageQueue() {
        // 实际应该检查 MQ 连接
        return Math.random() > 0.1;
    }

    /**
     * 模拟检查第三方 API
     */
    private boolean checkThirdPartyAPI() {
        // 实际应该调用第三方健康检查接口
        return Math.random() > 0.1;
    }
}
