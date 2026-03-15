package cn.itzixiao.interview.observability.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 系统指标监控 - JVM 和系统级别指标
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class SystemMetricsMonitor {
    
    /**
     * 活跃用户数（模拟）
     */
    private final AtomicInteger activeUsers = new AtomicInteger(0);
    
    /**
     * 当前连接数（模拟）
     */
    private final AtomicInteger currentConnections = new AtomicInteger(0);
    
    private final MeterRegistry meterRegistry;
    
    public SystemMetricsMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
    
    @PostConstruct
    public void init() {
        // 注册活跃用户数 Gauge
        Gauge.builder("system.active.users", activeUsers, AtomicInteger::get)
                .description("Number of active users")
                .tag("service", "user-service")
                .register(meterRegistry);
        
        // 注册当前连接数 Gauge
        Gauge.builder("system.connections.current", currentConnections, AtomicInteger::get)
                .description("Current number of connections")
                .tag("service", "api-gateway")
                .register(meterRegistry);
        
        // 模拟用户和连接变化
        simulateMetricsChange();
    }
    
    /**
     * 增加活跃用户数
     */
    public void incrementActiveUsers() {
        int newValue = activeUsers.incrementAndGet();
        log.info("活跃用户数：{}", newValue);
    }
    
    /**
     * 减少活跃用户数
     */
    public void decrementActiveUsers() {
        int newValue = activeUsers.decrementAndGet();
        log.info("活跃用户数：{}", newValue);
    }
    
    /**
     * 增加连接数
     */
    public void incrementConnections() {
        int newValue = currentConnections.incrementAndGet();
        log.info("当前连接数：{}", newValue);
    }
    
    /**
     * 减少连接数
     */
    public void decrementConnections() {
        int newValue = currentConnections.decrementAndGet();
        log.info("当前连接数：{}", newValue);
    }
    
    /**
     * 模拟指标变化（用于演示）
     */
    private void simulateMetricsChange() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    
                    // 随机增减用户
                    if (Math.random() > 0.5) {
                        incrementActiveUsers();
                    } else {
                        decrementActiveUsers();
                    }
                    
                    // 随机增减连接
                    if (Math.random() > 0.5) {
                        incrementConnections();
                    } else {
                        decrementConnections();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "Metrics-Simulator").start();
    }
}
