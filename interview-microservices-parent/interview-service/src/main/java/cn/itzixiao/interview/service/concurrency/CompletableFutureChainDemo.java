package cn.itzixiao.interview.service.concurrency;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * CompletableFuture 任务编排示例
 * 
 * @author itzixiao
 * @date 2026-03-17
 */
public class CompletableFutureChainDemo {
    
    public static void main(String[] args) throws Exception {
        System.out.println("========== thenApply 链式转换 ==========");
        thenApplyDemo();
        
        System.out.println("\n========== thenCompose 串联异步任务 ==========");
        thenComposeDemo();
        
        System.out.println("\n========== thenCombine 合并独立任务 ==========");
        thenCombineDemo();
        
        System.out.println("\n========== allOf 等待所有任务完成 ==========");
        allOfDemo();
        
        System.out.println("\n========== anyOf 取最快响应 ==========");
        anyOfDemo();
    }
    
    /**
     * thenApply - 转换结果
     */
    private static void thenApplyDemo() throws ExecutionException, InterruptedException {
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("步骤 1：获取原始数据");
                return "hello";
            })
            .thenApply(data -> {
                System.out.println("步骤 2：转换为大写");
                return data.toUpperCase();
            })
            .thenApply(str -> {
                System.out.println("步骤 3：添加后缀");
                return str + " WORLD!";
            });
        
        System.out.println("最终结果：" + future.get());
    }
    
    /**
     * thenCompose - 串联异步任务（扁平化）
     */
    private static void thenComposeDemo() throws Exception {
        // 场景：先获取用户 ID，再根据用户 ID 获取用户详情
        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> {
                System.out.println("步骤 1：获取用户 ID");
                return "USER-123";
            })
            .thenCompose(userId -> {
                System.out.println("步骤 2：根据 ID 获取用户详情");
                // 返回另一个 CompletableFuture
                return getUserDetail(userId);
            })
            .thenApply(user -> {
                System.out.println("步骤 3：格式化用户信息");
                return "姓名：" + user.name + "，年龄：" + user.age;
            });
        
        System.out.println(future.get());
    }
    
    private static CompletableFuture<User> getUserDetail(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new User("张三", 25);
        });
    }
    
    /**
     * thenCombine - 合并两个独立任务
     */
    private static void thenCombineDemo() throws Exception {
        // 并行查询用户信息和订单信息
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new User("张三", 25);
        });
        
        CompletableFuture<Order> orderFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new Order("ORDER-001", 99.99);
        });
        
        // 合并两个结果
        CompletableFuture<String> combined = userFuture.thenCombine(orderFuture, (user, order) -> {
            return String.format("用户：%s，订单：%s，金额：%.2f", 
                user.name, order.id, order.amount);
        });
        
        System.out.println(combined.get());
    }
    
    /**
     * allOf - 等待所有任务完成
     */
    private static void allOfDemo() throws Exception {
        String[] urls = {"url1", "url2", "url3"};
        
        // 创建多个异步任务
        CompletableFuture<String>[] futures = Arrays.stream(urls)
            .map(url -> CompletableFuture.supplyAsync(() -> {
                System.out.println("请求：" + url);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "响应：" + url;
            }))
            .toArray(CompletableFuture[]::new);
        
        // 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures);
        
        // 收集所有结果
        CompletableFuture<String> allResults = allFutures.thenApply(v -> 
            Arrays.stream(futures)
                .map(future -> future.join())
                .collect(java.util.stream.Collectors.joining(", "))
        );
        
        System.out.println("所有结果：" + allResults.get());
    }
    
    /**
     * anyOf - 最快完成的任务
     */
    private static void anyOfDemo() throws Exception {
        // 从多个 CDN 获取资源，取最快的
        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
            CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "CDN1 响应";
            }),
            CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "CDN2 响应"; // 这个最快
            }),
            CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "CDN3 响应";
            })
        );
        
        System.out.println("最快响应：" + fastest.get());
    }
    
    static class User {
        String name;
        Integer age;
        
        public User(String name, Integer age) {
            this.name = name;
            this.age = age;
        }
    }
    
    static class Order {
        String id;
        Double amount;
        
        public Order(String id, Double amount) {
            this.id = id;
            this.amount = amount;
        }
    }
}
