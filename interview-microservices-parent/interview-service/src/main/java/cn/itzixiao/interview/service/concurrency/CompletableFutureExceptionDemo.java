package cn.itzixiao.interview.service.concurrency;

import java.util.concurrent.*;

/**
 * CompletableFuture 异常处理示例
 * 
 * @author itzixiao
 * @date 2026-03-17
 */
public class CompletableFutureExceptionDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("========== exceptionally 简单异常处理 ==========");
        exceptionallyDemo();
        
        System.out.println("\n========== handle 统一处理成功和失败 ==========");
        handleDemo();
        
        System.out.println("\n========== whenComplete 后置操作 ==========");
        whenCompleteDemo();
        
        System.out.println("\n========== 超时控制 ==========");
        timeoutControlDemo();
    }
    
    /**
     * exceptionally - 简单异常处理
     */
    private static void exceptionallyDemo() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                if (true) throw new RuntimeException("计算失败");
                return "成功";
            })
            .exceptionally(ex -> {
                System.err.println("发生异常：" + ex.getMessage());
                return "默认值";
            });
        
        System.out.println("结果：" + future.get());
    }
    
    /**
     * handle - 无论成功失败都处理
     */
    private static void handleDemo() throws Exception {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                if (true) throw new RuntimeException("测试异常");
                return "成功";
            })
            .handle((result, ex) -> {
                if (ex != null) {
                    System.err.println("异常：" + ex.getMessage());
                    return "异常恢复后的值";
                } else {
                    System.out.println("成功：" + result);
                    return result;
                }
            });
        
        System.out.println("最终结果：" + future.get());
    }
    
    /**
     * whenComplete - 执行后置操作（不修改结果）
     */
    private static void whenCompleteDemo() throws Exception {
        CompletableFuture.supplyAsync(() -> {
            System.out.println("执行业务逻辑");
            return "业务结果";
        }).whenComplete((result, ex) -> {
            // 无论成功失败都会执行
            System.out.println("【后置操作】开始清理资源...");
            if (ex != null) {
                System.err.println("发生异常：" + ex.getMessage());
            } else {
                System.out.println("执行成功，结果：" + result);
            }
        });
        
        Thread.sleep(1000);
    }
    
    /**
     * 超时控制（Java 8 兼容版本）
     */
    private static void timeoutControlDemo() throws Exception {
        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "任务完成";
        });
        
        // Java 8 实现超时：使用 completeOnTimeout（需要 Java 9+）或手动实现
        // 这里演示手动实现超时的方式
        CompletableFuture<String> withTimeout = new CompletableFuture<>();
        
        // 设置定时任务
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> {
            if (!task.isDone()) {
                withTimeout.completeExceptionally(new TimeoutException("任务超时！"));
            }
        }, 2, TimeUnit.SECONDS);
        
        // 当原任务完成时，也完成 withTimeout
        task.whenComplete((result, ex) -> {
            if (!withTimeout.isDone()) {
                if (ex != null) {
                    withTimeout.completeExceptionally(ex);
                } else {
                    withTimeout.complete(result);
                }
            }
        });
        
        withTimeout.exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
                System.err.println("任务超时！");
                return "超时返回默认值";
            }
            throw new RuntimeException(ex);
        }).thenAccept(System.out::println);
        
        Thread.sleep(4000);
        scheduler.shutdown();
    }
}
