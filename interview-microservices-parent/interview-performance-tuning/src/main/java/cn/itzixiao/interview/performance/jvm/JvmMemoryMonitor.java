package cn.itzixiao.interview.performance.jvm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM 内存监控与调优示例
 * 
 * GC 日志分析参数：
 * -Xloggc:/var/log/gc.log
 * -XX:+PrintGCDetails
 * -XX:+PrintGCDateStamps
 * -XX:+UseGCLogFileRotation
 * -XX:NumberOfGCLogFiles=5
 * -XX:GCLogFileSize=10M
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class JvmMemoryMonitor {
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    /**
     * 模拟内存泄漏场景 - 静态集合持续增长
     */
    private static final List<byte[]> MEMORY_LEAK = new ArrayList<>();
    
    /**
     * 定期打印 JVM 内存使用情况
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void printMemoryInfo() {
        log.info("========== JVM 内存信息 ==========");
        
        // 堆内存信息
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        log.info("堆内存 - 初始化：{} KB, 使用：{} KB, 最大：{} KB",
                heapUsage.getInit() / 1024,
                heapUsage.getUsed() / 1024,
                heapUsage.getMax() / 1024);
        
        // 非堆内存信息
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        log.info("非堆内存 - 初始化：{} KB, 使用：{} KB, 最大：{} KB",
                nonHeapUsage.getInit() / 1024,
                nonHeapUsage.getUsed() / 1024,
                nonHeapUsage.getMax() / 1024);
        
        // GC 次数和时间（通过 JMX 获取）
        log.info("==============================");
    }
    
    /**
     * 模拟内存泄漏 - 用于演示 MAT/JProfiler 工具排查
     * 
     * 触发方式：定时任务每 10 秒分配 1MB 内存
     */
    @Scheduled(fixedRate = 10000)
    public void simulateMemoryLeak() {
        // 每次分配 1MB 内存
        byte[] data = new byte[1024 * 1024];
        MEMORY_LEAK.add(data);
        
        if (MEMORY_LEAK.size() % 10 == 0) {
            log.warn("已分配 {} MB 内存，注意内存泄漏风险！", MEMORY_LEAK.size());
        }
    }
    
    /**
     * 获取当前堆内存使用率
     */
    public double getHeapUsagePercent() {
        MemoryUsage usage = memoryBean.getHeapMemoryUsage();
        return (double) usage.getUsed() / usage.getMax() * 100;
    }
}
