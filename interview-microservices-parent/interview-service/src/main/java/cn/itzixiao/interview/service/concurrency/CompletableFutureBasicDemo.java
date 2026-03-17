package cn.itzixiao.interview.service.concurrency;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * CompletableFuture 基础示例
 * 
 * @author itzixiao
 * @date 2026-03-17
 */
public class CompletableFutureBasicDemo {
    
    /**
     * supplyAsync 示例 - 有返回值的异步任务
     */
    public static void main(String[] args) throws Exception {
        System.out.println("========== supplyAsync 示例 ==========");
        supplyAsyncDemo();
        
        System.out.println("\n========== runAsync 示例 ==========");
        runAsyncDemo();
        
        System.out.println("\n========== 自定义线程池示例 ==========");
        customExecutorDemo();
    }
    
    /**
     * supplyAsync 示例
     */
    private static void supplyAsyncDemo() throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("【supplyAsync】线程：" + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "用户信息";
        });
        
        // 方式一：阻塞获取结果
        String result = future.get();
        System.out.println("【get】结果：" + result);
        
        // 方式二：非阻塞回调
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> "数据");
        future2.thenAccept(data -> System.out.println("【thenAccept】处理：" + data));
    }
    
    /**
     * runAsync 示例 - 无返回值的异步任务
     */
    private static void runAsyncDemo() throws Exception {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("【runAsync】线程：" + Thread.currentThread().getName());
            System.out.println("正在记录日志...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("日志记录完成");
        });
        
        future.join(); // 等待完成
        System.out.println("任务完成");
    }
    
    /**
     * 使用自定义线程池
     */
    private static void customExecutorDemo() throws Exception {
        // 自定义线程池
        ExecutorService customExecutor = new ThreadPoolExecutor(
            4,                              // 核心线程数
            8,                              // 最大线程数
            60L, TimeUnit.SECONDS,          // 空闲超时
            new LinkedBlockingQueue<>(100), // 工作队列
            new ThreadFactoryBuilder()
                .setNameFormat("async-task-%d")
                .setDaemon(true)
                .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("【custom】线程：" + Thread.currentThread().getName());
            return "使用自定义线程池";
        }, customExecutor);
        
        System.out.println("结果：" + future.get());
        
        // 关闭线程池
        customExecutor.shutdown();
        if (!customExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
            customExecutor.shutdownNow();
        }
    }
}
