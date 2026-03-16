package cn.itxiao.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池配置属性
 * <p>
 * 作用：将 application.yml 中的配置绑定到 JavaBean
 * <p>
 * 配置前缀：custom.thread-pool
 * <p>
 * 支持的配置项：
 * - core-pool-size: 核心线程数（默认 5）
 * - max-pool-size: 最大线程数（默认 10）
 * - queue-capacity: 队列容量（默认 100）
 * - keep-alive-time: 空闲线程存活时间（秒，默认 60）
 * - name-prefix: 线程名前缀（默认 custom-pool）
 * - rejected-policy: 拒绝策略（默认 ABORT）
 */
@ConfigurationProperties(prefix = "custom.thread-pool")
public class ThreadPoolProperties {

    /**
     * 核心线程数
     * 说明：线程池中保持存活的最低线程数
     * 默认值：5
     */
    private int corePoolSize = 5;

    /**
     * 最大线程数
     * 说明：线程池允许创建的最大线程数
     * 默认值：10
     */
    private int maxPoolSize = 10;

    /**
     * 队列容量
     * 说明：用于缓存待执行任务的队列大小
     * 默认值：100
     */
    private int queueCapacity = 100;

    /**
     * 空闲线程存活时间（秒）
     * 说明：超过核心线程数的线程，空闲多久后被回收
     * 默认值：60
     */
    private long keepAliveTime = 60;

    /**
     * 线程名前缀
     * 说明：用于标识线程所属的线程池
     * 默认值：custom-pool
     */
    private String namePrefix = "custom-pool";

    /**
     * 拒绝策略
     * 说明：当队列满且达到最大线程数时的处理策略
     * 可选值：ABORT（抛异常）, CALLER_RUNS（调用者运行）, DISCARD（丢弃）, DISCARD_OLDEST（丢弃最老）
     * 默认值：ABORT
     */
    private String rejectedPolicy = "ABORT";

    // ========== Getter 和 Setter 方法 ==========

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String getRejectedPolicy() {
        return rejectedPolicy;
    }

    public void setRejectedPolicy(String rejectedPolicy) {
        this.rejectedPolicy = rejectedPolicy;
    }
}
