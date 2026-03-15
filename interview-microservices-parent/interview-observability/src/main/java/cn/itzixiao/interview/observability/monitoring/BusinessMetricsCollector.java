package cn.itzixiao.interview.observability.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Prometheus 指标采集器 - 自定义业务指标
 * 
 * Prometheus 监控体系：
 * 1. 指标采集（Exporter）- 通过 Micrometer 采集应用指标
 * 2. PromQL 查询语言 - 强大的时序数据查询
 * 3. 告警规则配置 - AlertManager 告警管理
 * 
 * 指标类型：
 * - Counter：只增不减的计数器（如请求总数）
 * - Gauge：可增可减的度量值（如活跃连接数）
 * - Timer：耗时统计（如接口响应时间）
 * - Summary：分位数统计
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class BusinessMetricsCollector {
    
    /**
     * 订单创建计数器
     */
    private final Counter orderCreatedCounter;
    
    /**
     * 订单支付成功计数器
     */
    private final Counter orderPaidCounter;
    
    /**
     * 订单处理耗时计时器
     */
    private final Timer orderProcessTimer;
    
    /**
     * 活跃用户数（Gauge 将在配置中定义）
     */
    
    public BusinessMetricsCollector(MeterRegistry meterRegistry) {
        // 注册订单创建计数器
        this.orderCreatedCounter = Counter.builder("order.created.total")
                .description("Total number of orders created")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // 注册订单支付成功计数器
        this.orderPaidCounter = Counter.builder("order.paid.total")
                .description("Total number of orders paid successfully")
                .tag("service", "order-service")
                .register(meterRegistry);
        
        // 注册订单处理耗时计时器
        this.orderProcessTimer = Timer.builder("order.process.time")
                .description("Time taken to process order")
                .tag("service", "order-service")
                .register(meterRegistry);
    }
    
    /**
     * 记录订单创建
     */
    public void recordOrderCreated(String orderType) {
        orderCreatedCounter.increment();
        log.info("订单创建：type={}", orderType);
    }
    
    /**
     * 记录订单支付成功
     */
    public void recordOrderPaid(String paymentMethod) {
        orderPaidCounter.increment();
        log.info("订单支付成功：method={}", paymentMethod);
    }
    
    /**
     * 记录订单处理耗时（使用 Timer 包装业务逻辑）
     */
    public void processOrder(String orderId) {
        orderProcessTimer.record(() -> {
            // 模拟订单处理逻辑
            try {
                log.info("开始处理订单：orderId={}", orderId);
                Thread.sleep(100); // 模拟业务处理
                log.info("订单处理完成：orderId={}", orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * 手动记录耗时（高级用法）
     */
    public void measureExecutionTime(Runnable task) {
        Timer.Sample sample = Timer.start();
        try {
            task.run();
        } finally {
            sample.stop(orderProcessTimer);
        }
    }
}
