package cn.itxiao.starter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池自动配置类
 * 
 * 核心注解说明：
 * 
 * 1. @Configuration
 *    - 标记为配置类，Spring 会扫描并加载其中的 Bean 定义
 * 
 * 2. @EnableConfigurationProperties(ThreadPoolProperties.class)
 *    - 启用配置属性绑定
 *    - 自动将 application.yml 中 custom.thread-pool.* 配置绑定到 ThreadPoolProperties
 * 
 * 3. @ConditionalOnClass(ThreadPoolExecutor.class)
 *    - 条件注解：当类路径中存在 ThreadPoolExecutor 时才生效
 *    - 确保 JDK 支持线程池
 * 
 * 4. @ConditionalOnProperty(prefix = "custom.thread-pool", name = "enabled", havingValue = "true", matchIfMissing = true)
 *    - 条件注解：当配置文件中 custom.thread-pool.enabled=true 或未配置时才生效
 *    - 可通过配置关闭自动装配
 * 
 * 5. @ConditionalOnMissingBean
 *    - 条件注解：当容器中没有自定义的 ThreadPoolExecutor Bean 时才创建
 *    - 允许用户覆盖默认配置
 */
@Configuration
@ConditionalOnClass(ThreadPoolExecutor.class)
@ConditionalOnProperty(
    prefix = "custom.thread-pool",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolAutoConfiguration {
    
    @Autowired
    private ThreadPoolProperties properties;
    
    /**
     * 创建线程池 Bean
     * 
     * @ConditionalOnMissingBean 确保用户可以自定义 ThreadPoolExecutor
     * 
     * @return 自定义线程池
     */
    @Bean
    @ConditionalOnMissingBean(ThreadPoolExecutor.class)
    public ThreadPoolExecutor threadPoolExecutor() {
        // 1. 创建阻塞队列
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(properties.getQueueCapacity());
        
        // 2. 创建拒绝策略
        RejectedExecutionHandler handler = createRejectedExecutionHandler(properties.getRejectedPolicy());
        
        // 3. 创建自定义线程池
        CustomThreadPoolExecutor executor = new CustomThreadPoolExecutor(
            properties.getCorePoolSize(),
            properties.getMaxPoolSize(),
            properties.getKeepAliveTime(),
            java.util.concurrent.TimeUnit.SECONDS,
            workQueue,
            properties.getNamePrefix(),
            handler
        );
        
        System.out.println("✅ 自动装配：自定义线程池已创建");
        System.out.println("📊 配置信息：" + executor.getPoolStatus());
        
        return executor;
    }
    
    /**
     * 根据配置创建拒绝策略
     * 
     * @param policy 策略名称
     * @return 拒绝策略处理器
     */
    private RejectedExecutionHandler createRejectedExecutionHandler(String policy) {
        switch (policy.toUpperCase()) {
            case "CALLER_RUNS":
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case "DISCARD":
                return new ThreadPoolExecutor.DiscardPolicy();
            case "DISCARD_OLDEST":
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            case "ABORT":
            default:
                return new ThreadPoolExecutor.AbortPolicy();
        }
    }
}
