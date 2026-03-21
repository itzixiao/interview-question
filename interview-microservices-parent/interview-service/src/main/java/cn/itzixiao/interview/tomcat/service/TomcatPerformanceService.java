package cn.itzixiao.interview.tomcat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tomcat 性能统计服务
 * 
 * <p>提供 Tomcat 运行时的性能统计功能，包括：</p>
 * <ul>
 *   <li>请求统计：请求数量、响应时间分布</li>
 *   <li>错误统计：错误数量、错误类型分布</li>
 *   <li>系统负载：CPU 使用率、内存使用情况</li>
 * </ul>
 * 
 * <p>使用示例：</p>
 * <pre>
 * {@code
 * // 记录请求开始
 * long startTime = performanceService.recordRequestStart("/api/users");
 * 
 * // 记录请求结束
 * performanceService.recordRequestEnd("/api/users", startTime, true);
 * }
 * </pre>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@Slf4j
@Service
public class TomcatPerformanceService {

    /**
     * 请求计数器：URI -> 请求次数
     */
    private final ConcurrentHashMap<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    
    /**
     * 响应时间累计：URI -> 总响应时间
     */
    private final ConcurrentHashMap<String, AtomicLong> responseTimes = new ConcurrentHashMap<>();
    
    /**
     * 错误计数器：URI -> 错误次数
     */
    private final ConcurrentHashMap<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    
    /**
     * 慢请求计数器：URI -> 慢请求数量
     */
    private final ConcurrentHashMap<String, AtomicLong> slowRequestCounts = new ConcurrentHashMap<>();
    
    /**
     * 慢请求阈值（毫秒）
     */
    private static final long SLOW_REQUEST_THRESHOLD = 1000;
    
    /**
     * JVM 启动时间
     */
    private final long startTime;

    public TomcatPerformanceService() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.startTime = runtimeMXBean.getStartTime();
    }

    /**
     * 记录请求开始
     * 
     * <p>在请求处理开始时调用，返回当前时间戳用于计算响应时间</p>
     * 
     * @param uri 请求 URI
     * @return 请求开始时间戳
     */
    public long recordRequestStart(String uri) {
        requestCounts.computeIfAbsent(uri, k -> new AtomicLong(0)).incrementAndGet();
        return System.currentTimeMillis();
    }

    /**
     * 记录请求结束
     * 
     * <p>在请求处理结束时调用，统计响应时间和错误情况</p>
     * 
     * @param uri       请求 URI
     * @param startTime 请求开始时间戳
     * @param success   是否成功
     */
    public void recordRequestEnd(String uri, long startTime, boolean success) {
        long duration = System.currentTimeMillis() - startTime;
        
        // 累计响应时间
        responseTimes.computeIfAbsent(uri, k -> new AtomicLong(0)).addAndGet(duration);
        
        // 记录错误
        if (!success) {
            errorCounts.computeIfAbsent(uri, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        // 记录慢请求
        if (duration > SLOW_REQUEST_THRESHOLD) {
            slowRequestCounts.computeIfAbsent(uri, k -> new AtomicLong(0)).incrementAndGet();
            log.warn("Slow request detected: uri={}, duration={}ms", uri, duration);
        }
    }

    /**
     * 获取请求统计
     * 
     * @return 请求统计数据
     */
    public Map<String, Object> getRequestStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        long totalRequests = 0;
        long totalErrors = 0;
        long totalSlowRequests = 0;
        
        for (Map.Entry<String, AtomicLong> entry : requestCounts.entrySet()) {
            String uri = entry.getKey();
            long count = entry.getValue().get();
            long time = responseTimes.getOrDefault(uri, new AtomicLong(0)).get();
            long errors = errorCounts.getOrDefault(uri, new AtomicLong(0)).get();
            long slowRequests = slowRequestCounts.getOrDefault(uri, new AtomicLong(0)).get();
            
            Map<String, Object> uriStats = new HashMap<>();
            uriStats.put("requestCount", count);
            uriStats.put("totalResponseTime", time);
            uriStats.put("avgResponseTime", count > 0 ? time / count : 0);
            uriStats.put("errorCount", errors);
            uriStats.put("slowRequestCount", slowRequests);
            uriStats.put("errorRate", count > 0 ? String.format("%.2f%%", (double) errors / count * 100) : "0%");
            
            statistics.put(uri, uriStats);
            
            totalRequests += count;
            totalErrors += errors;
            totalSlowRequests += slowRequests;
        }
        
        // 汇总统计
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalRequests", totalRequests);
        summary.put("totalErrors", totalErrors);
        summary.put("totalSlowRequests", totalSlowRequests);
        summary.put("errorRate", totalRequests > 0 ? 
            String.format("%.2f%%", (double) totalErrors / totalRequests * 100) : "0%");
        statistics.put("_summary", summary);
        
        return statistics;
    }

    /**
     * 获取系统负载信息
     * 
     * @return 系统负载数据
     */
    public Map<String, Object> getSystemLoad() {
        Map<String, Object> load = new HashMap<>();
        
        // 操作系统信息
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        load.put("osName", osMXBean.getName());
        load.put("osVersion", osMXBean.getVersion());
        load.put("osArch", osMXBean.getArch());
        load.put("availableProcessors", osMXBean.getAvailableProcessors());
        
        // 系统负载（如果支持）
        // 注意：getSystemLoadAverage() 返回 -1 表示不支持
        double systemLoadAverage = osMXBean.getSystemLoadAverage();
        load.put("systemLoadAverage", systemLoadAverage >= 0 ? 
            String.format("%.2f", systemLoadAverage) : "N/A");
        
        // JVM 运行时间
        long uptime = System.currentTimeMillis() - startTime;
        load.put("jvmUptimeMs", uptime);
        load.put("jvmUptimeFormatted", formatUptime(uptime));
        
        // 线程信息
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        load.put("threadCount", threadMXBean.getThreadCount());
        load.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        load.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        
        return load;
    }

    /**
     * 重置统计数据
     */
    public void resetStatistics() {
        requestCounts.clear();
        responseTimes.clear();
        errorCounts.clear();
        slowRequestCounts.clear();
        log.info("Performance statistics reset");
    }

    /**
     * 格式化运行时间
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        return String.format("%d days, %d hours, %d minutes, %d seconds", 
            days, hours, minutes, secs);
    }
}
