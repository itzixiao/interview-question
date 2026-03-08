package cn.itxiao.starter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义线程池
 * 
 * 功能特性：
 * 1. 支持核心线程数、最大线程数、队列容量配置
 * 2. 支持线程命名工厂（便于调试）
 * 3. 支持拒绝策略配置
 * 4. 提供线程池监控方法
 */
public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
    
    // 线程计数器（用于生成唯一线程名）
    private final AtomicLong threadCount = new AtomicLong(0);
    
    // 线程名前缀
    private final String namePrefix;
    
    /**
     * 构造方法
     * 
     * @param corePoolSize 核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime 空闲线程存活时间
     * @param unit 时间单位
     * @param workQueue 任务队列
     * @param namePrefix 线程名前缀
     * @param rejectedExecutionHandler 拒绝策略
     */
    public CustomThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            String namePrefix,
            RejectedExecutionHandler rejectedExecutionHandler) {
        
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
              new CustomThreadFactory(namePrefix), rejectedExecutionHandler);
        
        this.namePrefix = namePrefix;
    }
    
    /**
     * 自定义线程工厂
     * 
     * 作用：为每个创建的线程设置唯一的名称，便于调试和日志追踪
     */
    static class CustomThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicLong threadNumber = new AtomicLong(1);
        
        public CustomThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(namePrefix + "-Thread-" + threadNumber.getAndIncrement());
            thread.setDaemon(false); // 非守护线程
            return thread;
        }
    }
    
    /**
     * 获取线程池状态信息
     * 
     * @return 格式化的状态字符串
     */
    public String getPoolStatus() {
        return String.format(
            "线程池 [%s] 状态 -> 核心线程数：%d, 当前线程数：%d, 活跃线程数：%d, " +
            "队列大小：%d, 队列容量：%d, 已完成任务：%d",
            namePrefix,
            getCorePoolSize(),
            getPoolSize(),
            getActiveCount(),
            getQueue().size(),
            getQueue().remainingCapacity(),
            getCompletedTaskCount()
        );
    }
}
