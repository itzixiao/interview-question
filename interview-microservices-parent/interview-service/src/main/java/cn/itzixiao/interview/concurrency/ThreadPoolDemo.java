package cn.itzixiao.interview.concurrency;

import java.util.concurrent.*;

/**
 * 线程池详解与使用
 *
 * 线程池优势：
 * 1. 降低资源消耗：复用线程，减少创建销毁开销
 * 2. 提高响应速度：任务到达时无需等待创建线程
 * 3. 便于管理：统一分配、调优和监控
 *
 * 线程池状态：
 * ┌─────────┐  shutdown()  ┌─────────┐  stop()   ┌─────────┐
 * │ RUNNING │ ───────────→ │ SHUTDOWN│ ────────→ │  STOP   │
 * │ (运行)  │              │(关闭中)  │           │ (停止)  │
 * └─────────┘              └────┬────┘           └────┬────┘
 *                               │ 任务队列为空         │
 *                               │ 线程池为空           │
 *                               ↓                    ↓
 *                          ┌─────────┐         ┌─────────┐
 *                          │TIDYING  │ ──────→ │TERMINATED│
 *                          │(整理中)  │         │ (终止)   │
 *                          └─────────┘         └─────────┘
 */
public class ThreadPoolDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 线程池详解与使用 ==========\n");

        // 1. 线程池创建方式
        demonstrateThreadPoolCreation();

        // 2. 线程池参数详解
        demonstrateThreadPoolParameters();

        // 3. 线程池执行流程
        demonstrateExecutionFlow();

        // 4. 线程池监控
        demonstrateThreadPoolMonitoring();

        // 5. 并发工具类
        demonstrateConcurrentTools();

        // 6. ThreadLocal 详解
        demonstrateThreadLocal();
    }

    /**
     * 1. 线程池创建方式
     */
    private static void demonstrateThreadPoolCreation() {
        System.out.println("【1. 线程池创建方式】\n");

        System.out.println("Executors 工厂方法（不推荐生产环境使用）：");
        System.out.println("┌─────────────────────────┬────────────────────────────────────────┐");
        System.out.println("│  方法                   │  说明                                  │");
        System.out.println("├─────────────────────────┼────────────────────────────────────────┤");
        System.out.println("│  newFixedThreadPool(n)  │ 固定线程数，无界队列，可能OOM           │");
        System.out.println("│  newCachedThreadPool()  │ 无限线程数，可能资源耗尽                │");
        System.out.println("│  newSingleThreadExecutor│ 单线程，无界队列，可能OOM               │");
        System.out.println("│  newScheduledThreadPool │ 定时任务线程池                          │");
        System.out.println("└─────────────────────────┴────────────────────────────────────────┘\n");

        System.out.println("推荐方式：ThreadPoolExecutor 手动创建\n");

        // 创建自定义线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,                          // 核心线程数
                5,                          // 最大线程数
                60L,                        // 空闲线程存活时间
                TimeUnit.SECONDS,           // 时间单位
                new LinkedBlockingQueue<>(100), // 任务队列（有界队列）
                new CustomThreadFactory(),  // 线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );

        System.out.println("线程池创建成功：");
        System.out.println("  核心线程数: " + executor.getCorePoolSize());
        System.out.println("  最大线程数: " + executor.getMaximumPoolSize());
        System.out.println("  队列容量: " + executor.getQueue().remainingCapacity());
        System.out.println("  拒绝策略: CallerRunsPolicy\n");

        executor.shutdown();
    }

    /**
     * 2. 线程池参数详解
     */
    private static void demonstrateThreadPoolParameters() {
        System.out.println("【2. 线程池参数详解】\n");

        System.out.println("核心参数：");
        System.out.println("┌─────────────────────┬────────────────────────────────────────────┐");
        System.out.println("│  corePoolSize       │ 核心线程数，即使空闲也保留                 │");
        System.out.println("│  maximumPoolSize    │ 最大线程数，队列满后创建新线程             │");
        System.out.println("│  keepAliveTime      │ 非核心线程空闲存活时间                     │");
        System.out.println("│  workQueue          │ 任务等待队列                               │");
        System.out.println("│  threadFactory      │ 创建线程的工厂，可自定义线程名等           │");
        System.out.println("│  handler            │ 拒绝策略，队列满且线程数达最大时的处理     │");
        System.out.println("└─────────────────────┴────────────────────────────────────────────┘\n");

        System.out.println("任务队列类型：");
        System.out.println("1. ArrayBlockingQueue  - 有界数组队列");
        System.out.println("2. LinkedBlockingQueue - 链表队列（默认无界，可指定容量）");
        System.out.println("3. SynchronousQueue    - 同步队列，不存储元素，直接移交");
        System.out.println("4. PriorityBlockingQueue - 优先级队列\n");

        System.out.println("拒绝策略：");
        System.out.println("┌──────────────────────────┬─────────────────────────────────────┐");
        System.out.println("│  AbortPolicy(默认)       │ 抛出 RejectedExecutionException    │");
        System.out.println("│  CallerRunsPolicy        │ 由调用线程执行任务                  │");
        System.out.println("│  DiscardPolicy           │ 静默丢弃任务                        │");
        System.out.println("│  DiscardOldestPolicy     │ 丢弃队列最老的任务，重试提交        │");
        System.out.println("└──────────────────────────┴─────────────────────────────────────┘\n");
    }

    /**
     * 3. 线程池执行流程
     */
    private static void demonstrateExecutionFlow() throws InterruptedException {
        System.out.println("【3. 线程池执行流程】\n");

        System.out.println("任务提交后的处理流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 当前运行线程 < corePoolSize？                                │");
        System.out.println("│     是 → 创建新线程执行任务                                      │");
        System.out.println("│     否 → 继续下一步                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  2. 任务队列未满？                                               │");
        System.out.println("│     是 → 任务加入队列等待执行                                    │");
        System.out.println("│     否 → 继续下一步                                              │");
        System.out.println("│                                                                 │");
        System.out.println("│  3. 当前运行线程 < maximumPoolSize？                             │");
        System.out.println("│     是 → 创建新线程执行任务                                      │");
        System.out.println("│     否 → 执行拒绝策略                                            │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        // 演示执行流程
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 4, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new CustomThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );

        System.out.println("提交8个任务（核心2，最大4，队列2）：");
        System.out.println("预期：2个立即执行，2个进入队列，2个创建临时线程，2个被拒绝\n");

        for (int i = 0; i < 8; i++) {
            final int taskId = i;
            try {
                executor.execute(() -> {
                    System.out.println("  任务" + taskId + " 执行中，线程: " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("  任务" + taskId + " 完成");
                });
                System.out.println("任务" + i + " 提交成功");
            } catch (RejectedExecutionException e) {
                System.out.println("任务" + i + " 被拒绝: " + e.getMessage());
            }
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        System.out.println();
    }

    /**
     * 4. 线程池监控
     */
    private static void demonstrateThreadPoolMonitoring() throws InterruptedException {
        System.out.println("【4. 线程池监控】\n");

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2, 5, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10),
                new CustomThreadFactory()
        );

        // 提交一些任务
        for (int i = 0; i < 5; i++) {
            executor.execute(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }

        // 监控信息
        System.out.println("线程池监控指标：");
        System.out.println("  活跃线程数 (getActiveCount): " + executor.getActiveCount());
        System.out.println("  当前线程数 (getPoolSize): " + executor.getPoolSize());
        System.out.println("  核心线程数 (getCorePoolSize): " + executor.getCorePoolSize());
        System.out.println("  最大线程数 (getMaximumPoolSize): " + executor.getMaximumPoolSize());
        System.out.println("  队列大小 (getQueue.size): " + executor.getQueue().size());
        System.out.println("  完成任务数 (getCompletedTaskCount): " + executor.getCompletedTaskCount());
        System.out.println("  总任务数 (getTaskCount): " + executor.getTaskCount());
        System.out.println("  拒绝任务数 (getRejectedExecutionHandler): " + executor.getRejectedExecutionHandler().getClass().getSimpleName());

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n线程池关闭：");
        System.out.println("  shutdown()    - 优雅关闭，等待任务完成");
        System.out.println("  shutdownNow() - 立即关闭，返回未执行任务列表");
        System.out.println("  isShutdown()  - 是否已调用 shutdown");
        System.out.println("  isTerminated() - 所有任务是否已完成\n");
    }

    /**
     * 5. 常用并发工具类
     */
    private static void demonstrateConcurrentTools() throws Exception {
        System.out.println("【5. 常用并发工具类】\n");

        // CountDownLatch
        System.out.println("1. CountDownLatch - 倒计时门闩");
        System.out.println("   用途：等待多个线程完成后再继续执行\n");

        CountDownLatch latch = new CountDownLatch(3);
        for (int i = 0; i < 3; i++) {
            final int workerId = i;
            new Thread(() -> {
                System.out.println("   工作者" + workerId + " 开始工作");
                try {
                    Thread.sleep((workerId + 1) * 500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("   工作者" + workerId + " 完成工作");
                latch.countDown();
            }).start();
        }
        System.out.println("   主线程等待所有工作者完成...");
        latch.await();
        System.out.println("   所有工作者完成，主线程继续\n");

        // CyclicBarrier
        System.out.println("2. CyclicBarrier - 循环屏障");
        System.out.println("   用途：多个线程互相等待，到达屏障后一起继续\n");

        CyclicBarrier barrier = new CyclicBarrier(3);
        for (int i = 0; i < 3; i++) {
            final int runnerId = i;
            new Thread(() -> {
                System.out.println("   运动员" + runnerId + " 准备就绪");
                try {
                    barrier.await(); // 等待其他运动员
                    System.out.println("   运动员" + runnerId + " 起跑！");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Thread.sleep(1000);
        System.out.println();

        // Semaphore
        System.out.println("3. Semaphore - 信号量");
        System.out.println("   用途：控制同时访问某个资源的线程数量\n");

        Semaphore semaphore = new Semaphore(2); // 2个许可
        for (int i = 0; i < 5; i++) {
            final int carId = i;
            new Thread(() -> {
                try {
                    System.out.println("   车辆" + carId + " 尝试进入停车场");
                    semaphore.acquire();
                    System.out.println("   车辆" + carId + " 进入停车场，剩余车位: " + semaphore.availablePermits());
                    Thread.sleep(1000);
                    System.out.println("   车辆" + carId + " 离开停车场");
                    semaphore.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
        Thread.sleep(4000);
        System.out.println();

        // CompletableFuture
        System.out.println("4. CompletableFuture - 异步编程");
        System.out.println("   用途：链式组合异步操作，支持回调\n");

        CompletableFuture<String> future = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("   步骤1：获取数据");
                    return "原始数据";
                })
                .thenApply(data -> {
                    System.out.println("   步骤2：处理数据");
                    return data + "-已处理";
                })
                .thenApply(processed -> {
                    System.out.println("   步骤3：转换格式");
                    return "{" + processed + "}";
                })
                .exceptionally(ex -> {
                    System.out.println("   发生错误: " + ex.getMessage());
                    return "默认值";
                });

        String result = future.get();
        System.out.println("   最终结果: " + result + "\n");

        // 组合多个异步任务
        System.out.println("   组合多个异步任务：");
        CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "任务1结果";
        });

        CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "任务2结果";
        });

        CompletableFuture<String> combined = task1.thenCombine(task2, (r1, r2) -> r1 + " + " + r2);
        System.out.println("   组合结果：" + combined.get() + "\n");
    }
    
    /**
     * 6. ThreadLocal 详解
     */
    private static void demonstrateThreadLocal() {
        System.out.println("【6. ThreadLocal - 线程本地变量】\n");
    
        System.out.println("核心概念：");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  ThreadLocal 为每个线程提供独立的变量副本                       │");
        System.out.println("│                                                                 │");
        System.out.println("│  特点：                                                         │");
        System.out.println("│  1. 线程隔离：每个线程访问的都是自己线程内的变量               │");
        System.out.println("│  2. 资源复用：避免参数重复传递                                 │");
        System.out.println("│  3. 线程安全：无需同步，天然线程安全                           │");
        System.out.println("│                                                                 │");
        System.out.println("│  应用场景：                                                     │");
        System.out.println("│  - 数据库连接管理                                              │");
        System.out.println("│  - Session 管理                                                   │");
        System.out.println("│  - 用户上下文信息传递（如 userId、token）                      │");
        System.out.println("│  - SimpleDateFormat 线程安全包装                                │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");
    
        // 示例 1：基本使用
        System.out.println("示例 1：ThreadLocal 基本使用");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
    
        Thread thread1 = new Thread(() -> {
            threadLocal.set("线程 1 的数据");
            System.out.println("  线程 1 设置值：" + threadLocal.get());
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            System.out.println("  线程 1 再次获取：" + threadLocal.get());
            threadLocal.remove(); // 清理内存
        });
    
        Thread thread2 = new Thread(() -> {
            threadLocal.set("线程 2 的数据");
            System.out.println("  线程 2 设置值：" + threadLocal.get());
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            System.out.println("  线程 2 再次获取：" + threadLocal.get());
            threadLocal.remove(); // 清理内存
        });
    
        thread1.start();
        thread2.start();
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println();
    
        // 示例 2：ThreadLocal 内存泄漏问题
        System.out.println("示例 2：ThreadLocal 内存泄漏与解决方案");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("⚠️  内存泄漏原因：");
        System.out.println("   ThreadLocalMap 的 Key 是弱引用，Value 是强引用");
        System.out.println("   如果 ThreadLocal 对象被回收，但 Value 不会被回收");
        System.out.println("   导致内存泄漏\n");
    
        System.out.println("✅ 解决方案：使用完必须调用 remove() 方法");
        System.out.println("   threadLocal.set(value);\n");
        System.out.println("   try {");
        System.out.println("       // 业务逻辑");
        System.out.println("   } finally {");
        System.out.println("       threadLocal.remove();  // 必须清理！");
        System.out.println("   }\n");
    
        // 示例 3：实际应用 - 用户上下文传递
        System.out.println("示例 3：实际应用场景 - 用户上下文信息传递");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    
        UserContext userContext = new UserContext();
    
        // 模拟 Web 请求
        Thread requestThread = new Thread(() -> {
            try {
                // Controller 层：设置用户信息
                userContext.setUserId(1001L);
                userContext.setUsername("张三");
                System.out.println("  [Controller] 设置用户信息：userId=" + userContext.getUserId());
    
                // Service 层：无需传参，直接获取
                orderService();
    
                // Dao 层：也无需传参
                userDao();
    
            } finally {
                // 请求结束，清理上下文
                userContext.clear();
                System.out.println("  [Filter] 清理用户上下文\n");
            }
        });
    
        requestThread.start();
        try {
            requestThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        // 示例 4：InheritableThreadLocal - 父子线程传递
        System.out.println("示例 4：InheritableThreadLocal - 父子线程数据传递");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("InheritableThreadLocal 允许子线程继承父线程的值\n");
    
        InheritableThreadLocal<String> inheritableThreadLocal = new InheritableThreadLocal<>();
    
        Thread parentThread = new Thread(() -> {
            inheritableThreadLocal.set("父线程的数据");
            System.out.println("  父线程设置值：" + inheritableThreadLocal.get());
    
            // 创建子线程
            Thread childThread = new Thread(() -> {
                System.out.println("  子线程获取值：" + inheritableThreadLocal.get());
                System.out.println("  ✅ 子线程成功继承了父线程的值！\n");
            });
    
            childThread.start();
            try {
                childThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    
        parentThread.start();
        try {
            parentThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
        // 示例 5：ThreadLocal 高频面试题
        System.out.println("\n【ThreadLocal 高频面试题】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Q1: ThreadLocal 的原理是什么？");
        System.out.println("A1: 每个 Thread 内部维护了 ThreadLocalMap，Key 是 ThreadLocal 对象本身，");
        System.out.println("    Value 是线程本地变量。通过 get()/set() 方法操作当前线程的 Map 条目。\n");
    
        System.out.println("Q2: ThreadLocal 会导致内存泄漏吗？如何避免？");
        System.out.println("A2: 会。因为 ThreadLocalMap 的 Key 是弱引用，Value 是强引用。");
        System.out.println("    解决：使用完必须调用 remove() 方法，建议在 finally 块中清理。\n");
    
        System.out.println("Q3: InheritableThreadLocal 有什么坑？");
        System.out.println("A3: 使用线程池时，子线程会复用，导致继承的值不更新。");
        System.out.println("    解决：使用 Alibaba TransmittableThreadLocal 或手动管理。\n");
    
        System.out.println("Q4: ThreadLocal 适用场景？");
        System.out.println("A4: - 线程隔离的数据存储（如用户上下文、数据库连接）");
        System.out.println("    - 避免参数在多层调用中重复传递");
        System.out.println("    - 线程安全的 SimpleDateFormatter 封装\n");
    }
    
    // ==================== 辅助类 ====================
    
    static class CustomThreadFactory implements ThreadFactory {
        private int count = 0;
    
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "CustomPool-Thread-" + (++count));
            System.out.println("  创建线程：" + thread.getName());
            return thread;
        }
    }
    
    /**
     * 用户上下文工具类（实际项目中的典型应用）
     */
    static class UserContext {
        private static ThreadLocal<Long> userId = new ThreadLocal<>();
        private static ThreadLocal<String> username = new ThreadLocal<>();
    
        public static void setUserId(Long id) {
            userId.set(id);
        }
    
        public static Long getUserId() {
            return userId.get();
        }
    
        public static void setUsername(String name) {
            username.set(name);
        }
    
        public static String getUsername() {
            return username.get();
        }
    
        public static void clear() {
            userId.remove();
            username.remove();
        }
    }
    
    /**
     * 模拟 Service 层调用
     */
    private static void orderService() {
        System.out.println("  [Service] 处理订单，当前用户：userId=" + UserContext.getUserId() 
                          + ", username=" + UserContext.getUsername());
    }
    
    /**
     * 模拟 Dao 层调用
     */
    private static void userDao() {
        System.out.println("  [Dao] 查询用户数据，当前用户：userId=" + UserContext.getUserId());
    }
}
