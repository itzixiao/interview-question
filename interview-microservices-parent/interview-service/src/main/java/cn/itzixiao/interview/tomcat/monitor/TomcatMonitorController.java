package cn.itzixiao.interview.tomcat.monitor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Tomcat 性能监控端点
 * 
 * <p>提供 Tomcat 运行状态的关键指标，包括：</p>
 * <ul>
 *   <li>线程状态：线程数、峰值线程数、守护线程数</li>
 *   <li>内存状态：堆内存、非堆内存使用情况</li>
 *   <li>运行时状态：CPU 核心数、内存使用情况</li>
 *   <li>类加载状态：已加载类数量</li>
 * </ul>
 * 
 * <p>使用场景：</p>
 * <ul>
 *   <li>生产环境监控</li>
 *   <li>性能问题排查</li>
 *   <li>容量规划参考</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2026-03-21
 */
@RestController
@RequestMapping("/monitor/tomcat")
public class TomcatMonitorController {

    /**
     * 获取线程状态
     * 
     * <p>线程监控指标说明：</p>
     * <ul>
     *   <li>threadCount: 当前活动线程数</li>
     *   <li>peakThreadCount: 峰值线程数（JVM 启动以来的最大值）</li>
     *   <li>daemonThreadCount: 守护线程数</li>
     *   <li>totalStartedThreadCount: 累计启动的线程总数</li>
     * </ul>
     * 
     * <p>调优建议：</p>
     * <ul>
     *   <li>如果 peakThreadCount 接近 Tomcat maxThreads，需要考虑增加线程数</li>
     *   <li>如果 totalStartedThreadCount 增长过快，可能存在线程泄漏</li>
     * </ul>
     * 
     * @return 线程状态信息
     */
    @GetMapping("/threads")
    public Map<String, Object> getThreadStatus() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        Map<String, Object> status = new HashMap<>();
        status.put("threadCount", threadMXBean.getThreadCount());
        status.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        status.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());
        status.put("totalStartedThreadCount", threadMXBean.getTotalStartedThreadCount());
        
        // 计算线程使用率
        Runtime runtime = Runtime.getRuntime();
        int availableProcessors = runtime.availableProcessors();
        status.put("availableProcessors", availableProcessors);
        status.put("threadsPerCpu", threadMXBean.getThreadCount() / (double) availableProcessors);
        
        return status;
    }

    /**
     * 获取内存状态
     * 
     * <p>内存监控指标说明：</p>
     * <ul>
     *   <li>heap: 堆内存使用情况（对象存储区域）</li>
     *   <li>nonHeap: 非堆内存使用情况（方法区、代码缓存等）</li>
     * </ul>
     * 
     * <p>内存指标含义：</p>
     * <ul>
     *   <li>used: 已使用内存</li>
     *   <li>committed: 已分配内存（JVM 从操作系统申请的）</li>
     *   <li>max: 最大可用内存（-Xmx 配置）</li>
     * </ul>
     * 
     * <p>调优建议：</p>
     * <ul>
     *   <li>堆内存使用率超过 80% 需要关注</li>
     *   <li>如果 used 接近 max，可能存在内存泄漏</li>
     * </ul>
     * 
     * @return 内存状态信息
     */
    @GetMapping("/memory")
    public Map<String, Object> getMemoryStatus() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        
        Map<String, Object> status = new HashMap<>();
        
        // 堆内存
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        Map<String, Object> heap = new HashMap<>();
        heap.put("used", heapUsage.getUsed());
        heap.put("committed", heapUsage.getCommitted());
        heap.put("max", heapUsage.getMax());
        heap.put("init", heapUsage.getInit());
        
        // 计算使用率
        long heapUsed = heapUsage.getUsed();
        long heapMax = heapUsage.getMax();
        if (heapMax > 0) {
            heap.put("usagePercent", String.format("%.2f%%", (double) heapUsed / heapMax * 100));
        }
        status.put("heap", heap);
        
        // 非堆内存
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("used", nonHeapUsage.getUsed());
        nonHeap.put("committed", nonHeapUsage.getCommitted());
        nonHeap.put("init", nonHeapUsage.getInit());
        status.put("nonHeap", nonHeap);
        
        // 对象待 finalize 数量
        status.put("objectPendingFinalizationCount", memoryMXBean.getObjectPendingFinalizationCount());
        
        return status;
    }

    /**
     * 获取运行时状态
     * 
     * <p>运行时监控指标说明：</p>
     * <ul>
     *   <li>availableProcessors: 可用 CPU 核心数</li>
     *   <li>freeMemory: 空闲内存</li>
     *   <li>totalMemory: 已分配内存</li>
     *   <li>maxMemory: 最大可用内存</li>
     * </ul>
     * 
     * @return 运行时状态信息
     */
    @GetMapping("/runtime")
    public Map<String, Object> getRuntimeStatus() {
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> status = new HashMap<>();
        status.put("availableProcessors", runtime.availableProcessors());
        status.put("freeMemory", runtime.freeMemory());
        status.put("totalMemory", runtime.totalMemory());
        status.put("maxMemory", runtime.maxMemory());
        
        // 计算已使用内存
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        status.put("usedMemory", usedMemory);
        
        // 内存使用率
        long maxMemory = runtime.maxMemory();
        if (maxMemory > 0) {
            status.put("memoryUsagePercent", String.format("%.2f%%", (double) usedMemory / maxMemory * 100));
        }
        
        return status;
    }

    /**
     * 获取类加载状态
     * 
     * <p>类加载监控指标说明：</p>
     * <ul>
     *   <li>loadedClassCount: 当前已加载类数量</li>
     *   <li>totalLoadedClassCount: 累计加载类总数</li>
     *   <li>unloadedClassCount: 已卸载类数量</li>
     * </ul>
     * 
     * <p>调优建议：</p>
     * <ul>
     *   <li>如果 loadedClassCount 持续增长，可能存在类泄漏</li>
     *   <li>频繁的类加载/卸载会影响性能</li>
     * </ul>
     * 
     * @return 类加载状态信息
     */
    @GetMapping("/classloading")
    public Map<String, Object> getClassLoadingStatus() {
        java.lang.management.ClassLoadingMXBean classLoadingMXBean = 
            ManagementFactory.getClassLoadingMXBean();
        
        Map<String, Object> status = new HashMap<>();
        status.put("loadedClassCount", classLoadingMXBean.getLoadedClassCount());
        status.put("totalLoadedClassCount", classLoadingMXBean.getTotalLoadedClassCount());
        status.put("unloadedClassCount", classLoadingMXBean.getUnloadedClassCount());
        status.put("isVerbose", classLoadingMXBean.isVerbose());
        
        return status;
    }

    /**
     * 获取完整状态
     * 
     * <p>一次性获取所有监控指标</p>
     * 
     * @return 完整状态信息
     */
    @GetMapping("/status")
    public Map<String, Object> getFullStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("threads", getThreadStatus());
        status.put("memory", getMemoryStatus());
        status.put("runtime", getRuntimeStatus());
        status.put("classloading", getClassLoadingStatus());
        status.put("timestamp", System.currentTimeMillis());
        return status;
    }

    /**
     * 健康检查端点
     * 
     * <p>用于负载均衡器健康检查</p>
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return health;
    }
}
