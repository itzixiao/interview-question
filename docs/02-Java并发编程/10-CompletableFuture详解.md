# CompletableFuture 详解

## 📖 目录

- [一、CompletableFuture 简介](#一 completablefuture-简介)
- [二、核心概念与原理](#二核心概念与原理)
- [三、创建异步任务](#三创建异步任务)
- [四、任务编排与组合](#四任务编排与组合)
- [五、异常处理](#五异常处理)
- [六、实战应用场景](#六实战应用场景)
- [七、性能优化与最佳实践](#七性能优化与最佳实践)
- [八、高频面试题](#八高频面试题)

---

## 一、CompletableFuture 简介

### 1.1 什么是 CompletableFuture？

`CompletableFuture` 是 Java 8 引入的异步编程工具类，位于 `java.util.concurrent` 包中。它实现了 `Future` 和
`CompletionStage` 接口，提供了强大的异步任务编排能力。

**核心优势：**

- ✅ **异步非阻塞**：无需等待任务完成即可继续执行
- ✅ **函数式编程**：支持 Lambda 表达式和方法引用
- ✅ **任务编排**：可以轻松实现多个任务的串行、并行、组合
- ✅ **异常处理**：提供完善的异常捕获和处理机制
- ✅ **线程池定制**：可以指定自定义线程池执行任务

### 1.2 为什么需要 CompletableFuture？

**传统 Future 的局限性：**

```java
// 1. 阻塞式获取结果
Future<Integer> future = executor.submit(() -> {
            Thread.sleep(1000);
            return 42;
        });
Integer result = future.get(); // 阻塞！无法做其他事情

// 2. 无法设置回调
// 无法在任务完成后自动触发后续操作

// 3. 任务编排困难
// 多个任务依赖关系难以表达
```

**CompletableFuture 的解决方案：**

```java
CompletableFuture<Integer> future = CompletableFuture
        .supplyAsync(() -> compute())  // 异步计算
        .thenApply(result -> result * 2)  // 转换结果
        .thenAccept(System.out::println)  // 消费结果
        .exceptionally(ex -> {  // 异常处理
            System.err.println("Error: " + ex.getMessage());
            return 0;
        });
```

---

## 二、核心概念与原理

### 2.1 CompletionStage 接口

`CompletableFuture` 实现了 `CompletionStage` 接口，这是异步编程的核心抽象。

**关键特性：**

- **Stage（阶段）**：每个异步操作都是一个 Stage
- **Trigger（触发）**：前一个 Stage 完成后触发下一个
- **Non-blocking（非阻塞）**：Stage 之间通过回调连接

### 2.2 内部结构

```java
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {
    //  volatile 保证可见性
    private volatile Object result;  // 存储结果或异常

    //  栈节点，用于链式调用
    private volatile StackNode head;

    //  执行器，默认为 ForkJoinPool.commonPool()
    private static final Executor defaultExecutor =
            ForkJoinPool.commonPool();
}
```

### 2.3 执行流程

```
supplyAsync → thenApply → thenAccept → thenRun
     ↓            ↓           ↓          ↓
  Stage 1 → Stage 2 → Stage 3 → Stage 4
     ↓            ↓           ↓          ↓
  异步执行   转换结果    消费结果    最终操作
```

---

## 三、创建异步任务

### 3.1 supplyAsync - 有返回值的异步任务

```java
/**
 * 示例：异步获取用户信息
 */
public class SupplyAsyncDemo {
    public static void main(String[] args) throws Exception {
        // 1. 基本用法
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("【supplyAsync】线程：" + Thread.currentThread().getName());
            // 模拟耗时操作
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return "用户信息";
        });

        // 2. 获取结果（阻塞）
        String result = future.get();
        System.out.println("结果：" + result);

        // 3. 非阻塞回调
        future.thenAccept(System.out::println);
    }
}
```

### 3.2 runAsync - 无返回值的异步任务

```java
/**
 * 示例：异步记录日志
 */
public class RunAsyncDemo {
    public static void main(String[] args) throws Exception {
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println("【runAsync】线程：" + Thread.currentThread().getName());
            System.out.println("正在记录日志...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            System.out.println("日志记录完成");
        });

        future.join(); // 等待完成
    }
}
```

### 3.3 使用自定义线程池 ⭐推荐

```java
/**
 * 示例：使用自定义线程池执行异步任务
 */
public class CustomExecutorDemo {
    // 自定义线程池
    private static final ExecutorService customExecutor =
            new ThreadPoolExecutor(
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

    public static void main(String[] args) throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println("【custom】线程：" + Thread.currentThread().getName());
            return "使用自定义线程池";
        }, customExecutor);

        System.out.println("结果：" + future.get());

        // 关闭线程池
        customExecutor.shutdown();
    }
}
```

---

## 四、任务编排与组合

### 4.1 thenApply - 转换结果

```java
/**
 * 示例：数据转换流水线
 */
public class ThenApplyDemo {
    public static void main(String[] args) throws Exception {
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
}
```

### 4.2 thenAccept - 消费结果

```java
/**
 * 示例：处理订单结果
 */
public class ThenAcceptDemo {
    public static void main(String[] args) throws Exception {
        CompletableFuture.supplyAsync(() -> {
            // 创建订单
            return new Order("ORDER-001", 99.99);
        }).thenAccept(order -> {
            // 处理订单
            System.out.printf("订单 %s 创建成功，金额：%.2f%n",
                    order.getId(), order.getAmount());
        });
    }

    static class Order {
        private String id;
        private Double amount;
        // 构造方法、getter、setter 省略
    }
}
```

### 4.3 thenCompose - 串联异步任务（扁平化）⭐重点

```java
/**
 * 示例：依赖前一个异步任务的结果
 */
public class ThenComposeDemo {
    public static void main(String[] args) throws Exception {
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
                    return "姓名：" + user.getName() + "，年龄：" + user.getAge();
                });

        System.out.println(future.get());
    }

    private static CompletableFuture<User> getUserDetail(String userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            return new User("张三", 25);
        });
    }

    static class User {
        private String name;
        private Integer age;
        // 构造方法、getter、setter 省略
    }
}
```

**thenCompose vs thenApply 的区别：**

- `thenApply`：`T → U`（普通函数转换）
- `thenCompose`：`T → CompletableFuture<U>`（异步任务串联）

### 4.4 thenCombine - 合并两个独立任务

```java
/**
 * 示例：并行查询后合并结果
 */
public class ThenCombineDemo {
    public static void main(String[] args) throws Exception {
        // 并行查询用户信息和订单信息
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return new User("张三", 25);
        });

        CompletableFuture<Order> orderFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            return new Order("ORDER-001", 99.99);
        });

        // 合并两个结果
        CompletableFuture<String> combined = userFuture.thenCombine(orderFuture, (user, order) -> {
            return String.format("用户：%s，订单：%s，金额：%.2f",
                    user.getName(), order.getId(), order.getAmount());
        });

        System.out.println(combined.get());
    }
}
```

### 4.5 allOf - 等待所有任务完成

```java
/**
 * 示例：批量处理任务，等待全部完成
 */
public class AllOfDemo {
    public static void main(String[] args) throws Exception {
        List<String> urls = Arrays.asList("url1", "url2", "url3");

        // 创建多个异步任务
        List<CompletableFuture<String>> futures = urls.stream()
                .map(url -> CompletableFuture.supplyAsync(() -> {
                    System.out.println("请求：" + url);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    return "响应：" + url;
                }))
                .collect(Collectors.toList());

        // 等待所有任务完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // 收集所有结果
        CompletableFuture<List<String>> allResults = allFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
        );

        System.out.println("所有结果：" + allResults.get());
    }
}
```

### 4.6 anyOf - 最快完成的任务

```java
/**
 * 示例：多路冗余请求，取最快响应
 */
public class AnyOfDemo {
    public static void main(String[] args) throws Exception {
        // 从多个 CDN 获取资源，取最快的
        CompletableFuture<Object> fastest = CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
                    return "CDN1 响应";
                }),
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                    }
                    return "CDN2 响应"; // 这个最快
                }),
                CompletableFuture.supplyAsync(() -> {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                    }
                    return "CDN3 响应";
                })
        );

        System.out.println("最快响应：" + fastest.get());
    }
}
```

---

## 五、异常处理

### 5.1 exceptionally - 简单异常处理

```java
/**
 * 示例：异常时返回默认值
 */
public class ExceptionallyDemo {
    public static void main(String[] args) throws Exception {
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
}
```

### 5.2 handle - 无论成功失败都处理

```java
/**
 * 示例：统一处理成功和失败
 */
public class HandleDemo {
    public static void main(String[] args) throws Exception {
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
}
```

### 5.3 whenComplete - 执行后置操作（不修改结果）

```java
/**
 * 示例：记录日志、清理资源
 */
public class WhenCompleteDemo {
    public static void main(String[] args) throws Exception {
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
    }
}
```

### 5.4 异常传播与链式处理

```java
/**
 * 示例：链式异常处理
 */
public class ExceptionChainDemo {
    public static void main(String[] args) throws Exception {
        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    throw new RuntimeException("步骤 1 失败");
                })
                .thenApply(result -> result.toUpperCase())  // 不会执行
                .exceptionally(ex -> {
                    System.err.println("捕获异常：" + ex.getMessage());
                    return "DEFAULT";
                })
                .thenApply(result -> {
                    System.out.println("恢复正常：" + result);
                    return result.toLowerCase();
                });

        System.out.println("最终结果：" + future.get());
    }
}
```

---

## 六、实战应用场景

### 6.1 电商订单查询（多表并联查询）

```java
/**
 * 实战案例：电商订单详情页查询
 * 场景：需要查询用户、订单、商品、物流等多个服务
 */
public class EcommerceOrderQueryDemo {

    public CompletableFuture<OrderDetailVO> getOrderDetail(String orderId) {
        // 1. 并行查询各个服务
        CompletableFuture<User> userFuture = queryUserService(orderId);
        CompletableFuture<Order> orderFuture = queryOrderService(orderId);
        CompletableFuture<Product> productFuture = queryProductService(orderId);
        CompletableFuture<Logistics> logisticsFuture = queryLogisticsService(orderId);

        // 2. 等待所有查询完成并组合
        return CompletableFuture.allOf(userFuture, orderFuture, productFuture, logisticsFuture)
                .thenApply(v -> {
                    // 3. 组装 VO 对象
                    OrderDetailVO vo = new OrderDetailVO();
                    vo.setUser(userFuture.join());
                    vo.setOrder(orderFuture.join());
                    vo.setProduct(productFuture.join());
                    vo.setLogistics(logisticsFuture.join());
                    return vo;
                });
    }

    // Mock 方法
    private CompletableFuture<User> queryUserService(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            return new User("张三", "138****1234");
        });
    }

    private CompletableFuture<Order> queryOrderService(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
            }
            return new Order(orderId, 299.99, "已支付");
        });
    }

    private CompletableFuture<Product> queryProductService(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(180);
            } catch (InterruptedException e) {
            }
            return new Product("iPhone 15", 1);
        });
    }

    private CompletableFuture<Logistics> queryLogisticsService(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            return new Logistics("顺丰速运", "运输中");
        });
    }

    // VO 和实体类
    static class OrderDetailVO {
        private User user;
        private Order order;
        private Product product;
        private Logistics logistics;
        // getter、setter 省略
    }

    static class User {
        private String name;
        private String phone;
        // 构造方法、getter、setter 省略
    }

    static class Order {
        private String id;
        private Double amount;
        private String status;
        // 构造方法、getter、setter 省略
    }

    static class Product {
        private String name;
        private Integer quantity;
        // 构造方法、getter、setter 省略
    }

    static class Logistics {
        private String company;
        private String status;
        // 构造方法、getter、setter 省略
    }
}
```

### 6.2 数据聚合与报表生成

```java
/**
 * 实战案例：销售数据聚合报表
 */
public class DataAggregationDemo {

    public CompletableFuture<SalesReport> generateReport(String date) {
        long startTime = System.currentTimeMillis();

        // 1. 并行获取各维度数据
        CompletableFuture<List<SaleItem>> salesFuture = getSalesData(date);
        CompletableFuture<List<UserStat>> userStatsFuture = getUserStats(date);
        CompletableFuture<List<ProductStat>> productStatsFuture = getProductStats(date);

        // 2. 组合并生成报表
        return CompletableFuture.allOf(salesFuture, userStatsFuture, productStatsFuture)
                .thenApply(v -> {
                    SalesReport report = new SalesReport();
                    report.setDate(date);
                    report.setSalesItems(salesFuture.join());
                    report.setUserStats(userStatsFuture.join());
                    report.setProductStats(productStatsFuture.join());

                    // 3. 计算汇总指标
                    calculateMetrics(report);

                    long costTime = System.currentTimeMillis() - startTime;
                    System.out.println("报表生成耗时：" + costTime + "ms");
                    return report;
                })
                .exceptionally(ex -> {
                    System.err.println("报表生成失败：" + ex.getMessage());
                    throw new RuntimeException("报表生成异常", ex);
                });
    }

    private void calculateMetrics(SalesReport report) {
        // 计算总销售额、转化率等指标
        double totalSales = report.getSalesItems().stream()
                .mapToDouble(SaleItem::getAmount)
                .sum();
        report.setTotalSales(totalSales);
    }

    // Mock 方法
    private CompletableFuture<List<SaleItem>> getSalesData(String date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
            return Arrays.asList(
                    new SaleItem("商品 A", 100.0),
                    new SaleItem("商品 B", 200.0)
            );
        });
    }

    private CompletableFuture<List<UserStat>> getUserStats(String date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
            }
            return Arrays.asList(new UserStat("新增用户", 50));
        });
    }

    private CompletableFuture<List<ProductStat>> getProductStats(String date) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(450);
            } catch (InterruptedException e) {
            }
            return Arrays.asList(new ProductStat("热销商品", "商品 A"));
        });
    }

    // 实体类省略...
}
```

### 6.3 超时控制

```java
/**
 * 实战案例：带超时的异步任务
 */
public class TimeoutControlDemo {

    public static void main(String[] args) throws Exception {
        CompletableFuture<String> task = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            return "任务完成";
        });

        // 设置 2 秒超时
        CompletableFuture<String> withTimeout = task.orTimeout(2, TimeUnit.SECONDS);

        withTimeout.exceptionally(ex -> {
            if (ex instanceof TimeoutException) {
                System.err.println("任务超时！");
                return "超时返回默认值";
            }
            throw new RuntimeException(ex);
        }).thenAccept(System.out::println);

        Thread.sleep(4000);
    }
}
```

### 6.4 并行获取章节详情（实战案例）

```java
/**
 * 实战案例：教材下载服务中并行获取章节详情
 * 场景：需要并发获取多个章节的详细信息，并支持超时控制
 */
public class ChapterDetailFetcher {

    private final ExecutorService chapterExecutor;

    public ChapterDetailFetcher(int threadPoolSize) {
        this.chapterExecutor = Executors.newFixedThreadPool(threadPoolSize);
    }

    /**
     * 并行获取所有章节详情
     */
    public void fetchAllChapterDetails(List<CatalogItem> catalogList) {
        if (catalogList == null || catalogList.isEmpty()) return;

        // 1. 收集所有节点（展平层级结构）
        List<CatalogItem> allNodes = new ArrayList<>();
        collectAllNodes(catalogList, allNodes);

        log.info("开始并行获取 {} 个节点的详情", allNodes.size());

        // 2. 并行获取所有节点详情
        List<CompletableFuture<Void>> futures = allNodes.stream()
                .map(item -> CompletableFuture.runAsync(
                        () -> fetchChapterDetail(item),
                        chapterExecutor
                ))
                .collect(Collectors.toList());

        // 3. 等待所有任务完成（60秒超时）
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS);
            log.info("并行获取节点详情完成");
        } catch (Exception e) {
            log.warn("获取章节详情超时或异常: {}", e.getMessage());
        }
    }

    /**
     * 递归收集所有节点（展平层级结构）
     */
    private void collectAllNodes(List<CatalogItem> items, List<CatalogItem> result) {
        if (items == null || items.isEmpty()) return;

        for (CatalogItem item : items) {
            if (item != null && StringUtils.isNotEmpty(item.getId())) {
                result.add(item);
                if (item.getChildren() != null && !item.getChildren().isEmpty()) {
                    collectAllNodes(item.getChildren(), result);
                }
            }
        }
    }

    /**
     * 获取单个章节详情
     */
    private void fetchChapterDetail(CatalogItem item) {
        try {
            if (item.getLevel() != null && item.getLevel() == 1) {
                fetchChapterDetailFromChapterApi(item);
            } else {
                fetchChapterDetailFromSectionApi(item);
            }
        } catch (Exception e) {
            log.warn("获取节点详情异常: {}", item.getTitle(), e);
        }
    }
}
```

**关键技术点：**

| 技术                             | 作用         | 说明            |
|--------------------------------|------------|---------------|
| `CompletableFuture.runAsync()` | 异步执行无返回值任务 | 使用自定义线程池      |
| `CompletableFuture.allOf()`    | 等待所有任务完成   | 将多个 Future 合并 |
| `get(timeout, unit)`           | 带超时的等待     | 防止永久阻塞        |
| 递归展平                           | 处理树形结构     | 将层级结构转为列表     |

**进度计算与实时反馈：**

```java
/**
 * 带进度追踪的批量下载
 */
public class ProgressTrackingDownload {

    public void executeDownload(String taskId, List<ResourceInfo> resources) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicLong downloadedSize = new AtomicLong(0);
        AtomicLong startTime = new AtomicLong(System.currentTimeMillis());

        // 创建异步任务列表
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < resources.size(); i++) {
            final ResourceInfo resource = resources.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    downloadResource(resource);
                    successCount.incrementAndGet();
                    if (resource.getFileSize() != null) {
                        downloadedSize.addAndGet(resource.getFileSize());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    resource.setErrorMsg(e.getMessage());
                }

                // 更新进度
                updateProgress(resources.size(), successCount, failCount,
                        downloadedSize, startTime);
            });

            futures.add(future);
        }

        // 等待所有下载完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();
    }

    private void updateProgress(int total, AtomicInteger success, AtomicInteger fail,
                                AtomicLong downloadedSize, AtomicLong startTime) {
        int completed = success.get() + fail.get();
        int progressPercent = (int) ((completed * 100.0) / total);

        // 计算下载速度
        long elapsed = System.currentTimeMillis() - startTime.get();
        double speed = elapsed > 0 ? (downloadedSize.get() / 1024.0) / (elapsed / 1000.0) : 0;

        log.info("进度: {}/{} ({}%), 速度: {:.2f} KB/s",
                completed, total, progressPercent, speed);
    }
}
```

---

## 七、性能优化与最佳实践

### 7.1 避免阻塞操作 ⭐重要

```java
/**
 * 反例：在异步链中使用阻塞方法
 */
public class BadPracticeDemo {

    // ❌ 错误示范
    public void badExample() throws Exception {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "data");

        // 阻塞获取结果，失去异步意义
        String result = future.get();

        CompletableFuture.supplyAsync(() -> result.toUpperCase());
    }

    // ✅ 正确示范
    public void goodExample() {
        CompletableFuture.supplyAsync(() -> "data")
                .thenApply(String::toUpperCase)
                .thenAccept(System.out::println);
    }
}
```

### 7.2 合理使用线程池

```java
/**
 * 最佳实践：根据任务类型选择线程池
 */
public class ThreadPoolBestPractice {

    // CPU 密集型任务
    private static final ExecutorService CPU_POOL = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),  // CPU 核心数
            Runtime.getRuntime().availableProcessors(),
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024)
    );

    // IO 密集型任务
    private static final ExecutorService IO_POOL = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,  // 2 倍 CPU 核心数
            Runtime.getRuntime().availableProcessors() * 4,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024)
    );

    public void cpuIntensiveTask() {
        CompletableFuture.supplyAsync(() -> {
            // 复杂计算
            return compute();
        }, CPU_POOL);
    }

    public void ioIntensiveTask() {
        CompletableFuture.supplyAsync(() -> {
            // 网络请求、文件读写
            return fetchData();
        }, IO_POOL);
    }
}
```

### 7.3 异常处理最佳实践

```java
/**
 * 最佳实践：分层异常处理
 */
public class ExceptionBestPractice {

    public CompletableFuture<Result> process() {
        return CompletableFuture.supplyAsync(this::step1)
                .thenCompose(this::step2)
                .thenApply(this::step3)
                .handle((result, ex) -> {
                    // 统一异常处理
                    if (ex != null) {
                        log.error("处理失败", ex);
                        // 记录日志、发送告警、返回降级数据
                        return Result.fail(ex.getMessage());
                    }
                    return Result.success(result);
                });
    }
}
```

### 7.4 监控与调试

```java
/**
 * 最佳实践：添加监控埋点
 */
public class MonitoringDemo {

    public CompletableFuture<String> monitoredTask() {
        long startTime = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            // 业务逻辑
            return "result";
        }).whenComplete((result, ex) -> {
            long costTime = System.currentTimeMillis() - startTime;

            // 监控指标
            if (ex != null) {
                System.err.println("任务失败，耗时：" + costTime + "ms");
            } else {
                System.out.println("任务成功，耗时：" + costTime + "ms");
            }
        });
    }
}
```

---

## 八、高频面试题

### 1. **CompletableFuture 和 Future 的区别？**

**答案：**

| 特性       | Future       | CompletableFuture             |
|----------|--------------|-------------------------------|
| **异步回调** | ❌ 不支持        | ✅ 支持                          |
| **任务编排** | ❌ 困难         | ✅ 强大（thenApply、thenCompose 等） |
| **异常处理** | ❌ 只能 get 时捕获 | ✅ 多种处理方式                      |
| **阻塞**   | ✅ get() 阻塞   | ✅ 也支持，但推荐非阻塞                  |
| **手动完成** | ❌ 不支持        | ✅ complete() 方法               |
| **组合能力** | ❌ 弱          | ✅ allOf、anyOf、thenCombine     |

**代码对比：**

```java
// Future - 阻塞式
Future<Integer> future = executor.submit(() -> 42);
Integer result = future.get(); // 阻塞！

// CompletableFuture - 非阻塞回调
CompletableFuture<Integer> cf = CompletableFuture.supplyAsync(() -> 42);
cf.thenAccept(System.out::println); // 非阻塞
```

---

### 2. **thenApply、thenAccept、thenRun 的区别？**

**答案：**

| 方法             | 输入参数 | 返回值  | 用途           |
|----------------|------|------|--------------|
| **thenApply**  | T    | R    | 转换结果         |
| **thenAccept** | T    | Void | 消费结果（无返回值）   |
| **thenRun**    | Void | Void | 纯动作（不需要前序结果） |

**示例：**

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "hello")
        .thenApply(s -> s.toUpperCase());  // HELLO

CompletableFuture<Void> f2 = CompletableFuture.supplyAsync(() -> "hello")
        .thenAccept(s -> System.out.println(s));  // 打印 hello

CompletableFuture<Void> f3 = CompletableFuture.supplyAsync(() -> "hello")
        .thenRun(() -> System.out.println("done"));  // 打印 done
```

---

### 3. **thenCompose 和 thenCombine 的区别？**

**答案：**

- **thenCompose**：串联两个**有依赖关系**的异步任务（类似 flatMap）
- **thenCombine**：合并两个**独立并行**的异步任务

**示例对比：**

```java
// thenCompose - 依赖关系
supplyAsync(() -> userId)  // 先获取 userId
    .thenCompose(id -> getUserDetail(id))  // 再查详情

// thenCombine - 并行关系
userFuture.thenCombine(orderFuture, (user, order) -> {
    // 两个独立任务完成后合并
    return user.getName() + " - " + order.getId();
});
```

---

### 4. **allOf 和 anyOf 的应用场景？**

**答案：**

**allOf - 等待所有任务完成**

- 场景：批量查询、聚合报表、多服务调用
- 特点：所有任务都完成后才触发后续

**anyOf - 取最快完成的任务**

- 场景：多路冗余请求、CDN 切换、备用方案
- 特点：哪个快用哪个

---

### 5. **如何优雅地处理异常？**

**答案：**

三种方式：

1. **exceptionally**：简单兜底
2. **handle**：统一处理成功和失败
3. **whenComplete**：后置操作（不修改结果）

**推荐实践：**

```java
CompletableFuture.supplyAsync(() -> doSomething())
    .thenApply(this::transform)
    .handle((result, ex) -> {
        if (ex != null) {
            log.error("失败", ex);
            return defaultValue;
        }
        return result;
    });
```

---

### 6. **CompletableFuture 的默认线程池是什么？如何自定义？**

**答案：**

**默认线程池：** `ForkJoinPool.commonPool()`

**自定义方式：**

```java
// 1. 传入 Executor
Executor executor = Executors.newFixedThreadPool(10);
CompletableFuture.supplyAsync(() -> task(), executor);

// 2. 自定义线程池参数
ThreadPoolExecutor customPool = new ThreadPoolExecutor(
    8, 16, 60L, TimeUnit.SECONDS, 
    new ArrayBlockingQueue<>(100)
);
CompletableFuture.supplyAsync(() -> task(), customPool);
```

---

### 7. **如何避免阻塞？join() 和 get() 的区别？**

**答案：**

**避免阻塞的方法：**

- 使用 `thenApply`、`thenAccept` 等回调方法
- 使用 `thenCompose` 串联任务
- 使用 `allOf` 等待多个任务

**join() vs get()：**

- `join()`：抛出 unchecked exception（RuntimeException）
- `get()`：抛出 checked exception（ExecutionException, InterruptedException）

**推荐：** 优先使用回调，避免显式调用 join()/get()

---

### 8. **CompletableFuture 会导致内存泄漏吗？如何避免？**

**答案：**

**可能原因：**

- 未处理的异常导致 Stage 无法完成
- 线程池未关闭
- 长时间持有大对象的引用

**避免方法：**

1. 始终处理异常（exceptionally/handle）
2. 及时关闭线程池
3. 使用 weak reference 持有大对象
4. 添加超时控制（orTimeout）

---

## 📚 参考资源

- **官方文档**：https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/CompletableFuture.html
- **源码位置**：`$JAVA_HOME/src.zip!/java.base/java/util/concurrent/CompletableFuture.java`
- **推荐阅读**：《Java并发编程实战》第 8 章

---

**维护者：** itzixiao  
**最后更新：** 2026-03-17  
**配套代码：**
`interview-microservices-parent/interview-service/src/main/java/cn/itzixiao/interview/service/concurrency/`
