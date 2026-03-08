package cn.itzixiao.interview.provider.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池Starter 使用示例
 * 
 * 演示如何在业务项目中使用自定义的线程池Starter
 */
@RestController
public class ThreadPoolTestController {
    
    /**
     * 自动注入自定义线程池
     * 
     * Spring Boot 会自动装配 ThreadPoolAutoConfiguration 中定义的 ThreadPoolExecutor Bean
     * 
     * 注入成功的前提：
     * 1. 引入了 interview-threadpool-starter 依赖
     * 2. 配置文件中 custom.thread-pool.enabled=true（或未配置，默认 true）
     * 3. 容器中没有其他自定义的 ThreadPoolExecutor Bean
     */
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    
    /**
     * 测试线程池 - 提交异步任务
     * 
     * @return 提示信息
     */
    @GetMapping("/test/thread-pool")
    public String testThreadPool() {
        System.out.println("\n========== 提交异步任务 ==========");
        
        // 提交 5 个异步任务
        for (int i = 0; i < 5; i++) {
            final int taskId = i + 1;
            threadPoolExecutor.submit(() -> {
                System.out.println("✅ 任务 " + taskId + " 执行中... 线程：" + Thread.currentThread().getName());
                try {
                    Thread.sleep(500); // 模拟耗时操作
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("✅ 任务 " + taskId + " 完成！");
            });
        }
        
        return "已提交 5 个异步任务，请查看控制台日志";
    }
    
    /**
     * 获取线程池状态
     * 
     * @return 线程池状态信息
     */
    @GetMapping("/test/pool-status")
    public String getPoolStatus() {
        // 获取线程池状态
        int coreSize = threadPoolExecutor.getCorePoolSize();
        int poolSize = threadPoolExecutor.getPoolSize();
        int activeCount = threadPoolExecutor.getActiveCount();
        int queueSize = threadPoolExecutor.getQueue().size();
        long completedTaskCount = threadPoolExecutor.getCompletedTaskCount();
        
        return String.format(
            "线程池状态 -> " +
            "核心线程数：%d, " +
            "当前线程数：%d, " +
            "活跃线程数：%d, " +
            "队列大小：%d, " +
            "已完成任务数：%d",
            coreSize, poolSize, activeCount, queueSize, completedTaskCount
        );
    }
    
    /**
     * 压力测试 - 提交大量任务
     * 
     * @return 提示信息
     */
    @GetMapping("/test/stress-test")
    public String stressTest() {
        System.out.println("\n========== 压力测试开始 ==========");
        
        // 提交 20 个任务，观察队列和拒绝策略
        for (int i = 0; i < 20; i++) {
            final int taskId = i + 1;
            threadPoolExecutor.submit(() -> {
                System.out.println("🔥 压力任务 " + taskId + " 执行中... 线程：" + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("🔥 压力任务 " + taskId + " 完成！");
            });
        }
        
        return "已提交 20 个压力测试任务，请查看控制台日志和线程池状态";
    }
}
