package cn.itzixiao.interview.observability.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义健康检查指示器 - 数据库连接池健康状态
 * <p>
 * 健康检查类型：
 * 1. Liveness Probe（存活探针）- 应用是否存活
 * 2. Readiness Probe（就绪探针）- 应用是否准备好处理请求
 * 3. Startup Probe（启动探针）- 应用是否已完成启动
 * <p>
 * 自愈机制：
 * 1. K8s 根据探针状态自动重启 Pod
 * 2. 负载均衡器自动摘除不健康实例
 * 3. 自动扩容增加新实例
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    /**
     * 最大允许活跃连接数比例
     */
    private static final double MAX_ACTIVE_CONNECTION_RATIO = 0.9;

    /**
     * 健康检查逻辑
     */
    @Override
    public Health health() {
        try {
            // 模拟数据库连接检查
            int activeConnections = checkActiveConnections();
            int maxConnections = 100;

            double usageRatio = (double) activeConnections / maxConnections;

            if (usageRatio >= MAX_ACTIVE_CONNECTION_RATIO) {
                log.warn("数据库连接池使用率过高：{}%", usageRatio * 100);
                return Health.down()
                        .withDetail("activeConnections", activeConnections)
                        .withDetail("maxConnections", maxConnections)
                        .withDetail("usageRatio", String.format("%.2f%%", usageRatio * 100))
                        .withDetail("status", "CRITICAL")
                        .build();
            }

            if (usageRatio >= 0.7) {
                log.warn("数据库连接池使用率偏高：{}%", usageRatio * 100);
                return Health.up()
                        .withDetail("activeConnections", activeConnections)
                        .withDetail("maxConnections", maxConnections)
                        .withDetail("usageRatio", String.format("%.2f%%", usageRatio * 100))
                        .withDetail("status", "WARNING")
                        .build();
            }

            return Health.up()
                    .withDetail("activeConnections", activeConnections)
                    .withDetail("maxConnections", maxConnections)
                    .withDetail("usageRatio", String.format("%.2f%%", usageRatio * 100))
                    .withDetail("status", "HEALTHY")
                    .build();

        } catch (Exception e) {
            log.error("数据库连接检查失败", e);
            return Health.down(e)
                    .withDetail("status", "ERROR")
                    .build();
        }
    }

    /**
     * 模拟检查活跃连接数
     */
    private int checkActiveConnections() {
        // 实际应用中应该从连接池获取真实数据
        // 例如：HikariCP.getHikariPoolMXBean().getActiveConnections()
        return (int) (Math.random() * 80);
    }
}
