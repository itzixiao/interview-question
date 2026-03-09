package cn.itzixiao.interview.provider.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * 自定义数据库健康检查器
 * 
 * 作用：监控数据库连接状态
 * 访问端点：/actuator/health
 * 
 * 使用场景：
 * 1. 数据库连接池健康检查
 * 2. 第三方服务可用性检查
 * 3. 磁盘空间检查
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // 模拟数据库连接检查
        boolean databaseIsUp = checkDatabaseConnection();
        
        if (databaseIsUp) {
            // 健康状态
            return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("status", "connected")
                    .withDetail("connectionPoolSize", 20)
                    .build();
        } else {
            // 不健康状态
            return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("error", "Connection timeout")
                    .withException(new RuntimeException("数据库连接失败"))
                    .build();
        }
    }
    
    /**
     * 模拟数据库连接检查
     * 实际项目中应该执行真实的数据库查询
     */
    private boolean checkDatabaseConnection() {
        // TODO: 实际实现应该是：
        // try {
        //     jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        //     return true;
        // } catch (Exception e) {
        //     return false;
        // }
        return true; // 模拟正常
    }
}
