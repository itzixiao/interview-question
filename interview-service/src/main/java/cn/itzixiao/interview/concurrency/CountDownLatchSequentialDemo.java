package cn.itzixiao.interview.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * CountDownLatch 实现线程顺序执行
 *
 * 原理：
 * CountDownLatch 是一个同步辅助类，它允许一个或多个线程等待，
 * 直到在其他线程中执行的一组操作完成。
 *
 * 核心方法：
 * - await(): 等待计数器归零
 * - countDown(): 计数器减1
 * - getCount(): 获取当前计数
 *
 * 线程顺序执行方案：
 * 使用两个 CountDownLatch，计数器都设为 1
 * - latchAB: 控制 A -> B 的顺序
 * - latchBC: 控制 B -> C 的顺序
 *
 * 执行流程：
 * Thread A: 执行 -> countDown(latchAB)
 * Thread B: await(latchAB) -> 执行 -> countDown(latchBC)
 * Thread C: await(latchBC) -> 执行
 */
public class CountDownLatchSequentialDemo {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("CountDownLatch 线程顺序执行示例");
        System.out.println("========================================\n");

        // 基础示例：ABC 顺序执行
        basicSequentialExample();

        System.out.println("\n==================================================\n");

        // 扩展示例：多阶段顺序执行
        multiStageExample();

        System.out.println("\n==================================================\n");

        // 实际应用场景：数据加载顺序
        dataLoadingScenario();

        System.out.println("\n========================================");
        System.out.println("所有示例执行完成");
        System.out.println("========================================");
    }

    /**
     * ============================================
     * 基础示例：ABC 顺序执行
     * ============================================
     *
     * 使用两个 CountDownLatch 实现 A -> B -> C 顺序
     */
    private static void basicSequentialExample() {
        System.out.println("【示例1】基础 ABC 顺序执行\n");

        // 创建两个 CountDownLatch，计数器都为 1
        CountDownLatch latchAB = new CountDownLatch(1);  // 控制 A -> B
        CountDownLatch latchBC = new CountDownLatch(1);  // 控制 B -> C

        // 线程 A
        Thread threadA = new Thread(() -> {
            System.out.println("[线程A] 开始执行...");
            try {
                // 模拟业务操作
                TimeUnit.MILLISECONDS.sleep(500);
                System.out.println("[线程A] 业务处理完成");

                // A 执行完毕，唤醒 B
                System.out.println("[线程A] 调用 latchAB.countDown() 唤醒线程B");
                latchAB.countDown();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-A");

        // 线程 B
        Thread threadB = new Thread(() -> {
            try {
                // B 等待 A 完成
                System.out.println("[线程B] 等待线程A完成 (latchAB.await())...");
                latchAB.await();

                System.out.println("[线程B] 被唤醒，开始执行...");
                // 模拟业务操作
                TimeUnit.MILLISECONDS.sleep(500);
                System.out.println("[线程B] 业务处理完成");

                // B 执行完毕，唤醒 C
                System.out.println("[线程B] 调用 latchBC.countDown() 唤醒线程C");
                latchBC.countDown();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-B");

        // 线程 C
        Thread threadC = new Thread(() -> {
            try {
                // C 等待 B 完成
                System.out.println("[线程C] 等待线程B完成 (latchBC.await())...");
                latchBC.await();

                System.out.println("[线程C] 被唤醒，开始执行...");
                // 模拟业务操作
                TimeUnit.MILLISECONDS.sleep(500);
                System.out.println("[线程C] 业务处理完成");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-C");

        // 启动线程（注意：启动顺序不影响执行顺序，因为 B 和 C 都在等待）
        System.out.println("启动线程 C -> B -> A（启动顺序不影响执行顺序）\n");
        threadC.start();
        threadB.start();
        threadA.start();

        // 等待所有线程完成
        try {
            threadA.join();
            threadB.join();
            threadC.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n[主线程] ABC 顺序执行完成！");
    }

    /**
     * ============================================
     * 扩展示例：多阶段顺序执行
     * ============================================
     *
     * 场景：数据加载分为多个阶段，每个阶段有多个并行任务
     * 阶段1：加载基础数据（并行加载配置、字典、权限）
     * 阶段2：加载业务数据（依赖阶段1完成）
     * 阶段3：数据校验和初始化（依赖阶段2完成）
     */
    private static void multiStageExample() {
        System.out.println("【示例2】多阶段顺序执行\n");

        // 阶段控制
        CountDownLatch stage1Complete = new CountDownLatch(3);  // 阶段1有3个任务
        CountDownLatch stage2Complete = new CountDownLatch(2);  // 阶段2有2个任务
        CountDownLatch stage3Complete = new CountDownLatch(1);  // 阶段3有1个任务

        // ========== 阶段1：并行加载基础数据 ==========
        System.out.println("【阶段1】并行加载基础数据（3个任务）\n");

        Thread loadConfig = new Thread(() -> {
            System.out.println("[阶段1-配置] 开始加载系统配置...");
            sleep(300);
            System.out.println("[阶段1-配置] 配置加载完成 ✓");
            stage1Complete.countDown();
        }, "LoadConfig");

        Thread loadDict = new Thread(() -> {
            System.out.println("[阶段1-字典] 开始加载数据字典...");
            sleep(400);
            System.out.println("[阶段1-字典] 字典加载完成 ✓");
            stage1Complete.countDown();
        }, "LoadDict");

        Thread loadPermission = new Thread(() -> {
            System.out.println("[阶段1-权限] 开始加载权限数据...");
            sleep(500);
            System.out.println("[阶段1-权限] 权限加载完成 ✓");
            stage1Complete.countDown();
        }, "LoadPermission");

        // ========== 阶段2：加载业务数据 ==========
        System.out.println("【阶段2】加载业务数据（等待阶段1完成）\n");

        Thread loadUserData = new Thread(() -> {
            try {
                System.out.println("[阶段2-用户] 等待阶段1完成...");
                stage1Complete.await();
                System.out.println("[阶段2-用户] 阶段1完成，开始加载用户数据...");
                sleep(400);
                System.out.println("[阶段2-用户] 用户数据加载完成 ✓");
                stage2Complete.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "LoadUserData");

        Thread loadOrderData = new Thread(() -> {
            try {
                System.out.println("[阶段2-订单] 等待阶段1完成...");
                stage1Complete.await();
                System.out.println("[阶段2-订单] 阶段1完成，开始加载订单数据...");
                sleep(500);
                System.out.println("[阶段2-订单] 订单数据加载完成 ✓");
                stage2Complete.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "LoadOrderData");

        // ========== 阶段3：数据校验 ==========
        System.out.println("【阶段3】数据校验和初始化（等待阶段2完成）\n");

        Thread validateData = new Thread(() -> {
            try {
                System.out.println("[阶段3-校验] 等待阶段2完成...");
                stage2Complete.await();
                System.out.println("[阶段3-校验] 阶段2完成，开始数据校验...");
                sleep(300);
                System.out.println("[阶段3-校验] 数据校验完成 ✓");
                stage3Complete.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ValidateData");

        // 启动所有线程
        loadConfig.start();
        loadDict.start();
        loadPermission.start();
        loadUserData.start();
        loadOrderData.start();
        validateData.start();

        // 等待所有阶段完成
        try {
            stage3Complete.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n[主线程] 多阶段加载全部完成！");
    }

    /**
     * ============================================
     * 实际应用场景：数据加载顺序
     * ============================================
     *
     * 场景：系统启动时需要按顺序加载数据
     * 1. 先加载数据库连接池
     * 2. 再加载缓存
     * 3. 最后加载业务数据
     */
    private static void dataLoadingScenario() {
        System.out.println("【示例3】系统启动数据加载顺序\n");

        CountDownLatch dbReady = new CountDownLatch(1);
        CountDownLatch cacheReady = new CountDownLatch(1);

        // 步骤1：初始化数据库连接池
        Thread initDatabase = new Thread(() -> {
            System.out.println("[系统启动] 步骤1: 初始化数据库连接池...");
            sleep(800);
            System.out.println("[系统启动] 数据库连接池初始化完成 ✓");
            dbReady.countDown();
        }, "InitDatabase");

        // 步骤2：初始化缓存（依赖数据库）
        Thread initCache = new Thread(() -> {
            try {
                System.out.println("[系统启动] 步骤2: 等待数据库就绪...");
                dbReady.await();
                System.out.println("[系统启动] 步骤2: 初始化缓存...");
                sleep(600);
                System.out.println("[系统启动] 缓存初始化完成 ✓");
                cacheReady.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "InitCache");

        // 步骤3：加载业务数据（依赖缓存）
        Thread loadBusinessData = new Thread(() -> {
            try {
                System.out.println("[系统启动] 步骤3: 等待缓存就绪...");
                cacheReady.await();
                System.out.println("[系统启动] 步骤3: 加载业务数据...");
                sleep(700);
                System.out.println("[系统启动] 业务数据加载完成 ✓");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "LoadBusinessData");

        // 按依赖关系启动
        loadBusinessData.start();
        initCache.start();
        initDatabase.start();

        // 等待系统启动完成
        try {
            loadBusinessData.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n[系统启动] 所有组件初始化完成，系统就绪！");
    }

    /**
     * ============================================
     * 工具方法
     * ============================================
     */
    private static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

/**
 * ============================================
 * 进阶示例：动态线程顺序控制
 * ============================================
 *
 * 使用数组实现任意数量的线程顺序执行
 */
class DynamicSequentialExecutor {

    private final CountDownLatch[] latches;

    /**
     * 创建顺序执行器
     * @param threadCount 线程数量
     */
    public DynamicSequentialExecutor(int threadCount) {
        // 创建 n-1 个 latch（最后一个线程不需要唤醒下一个）
        this.latches = new CountDownLatch[threadCount - 1];
        for (int i = 0; i < latches.length; i++) {
            latches[i] = new CountDownLatch(1);
        }
    }

    /**
     * 提交任务
     * @param index 线程索引（从0开始）
     * @param task 任务
     */
    public void submit(int index, Runnable task) {
        new Thread(() -> {
            try {
                // 第一个线程不需要等待
                // 其他线程等待前一个线程完成
                if (index > 0 && index - 1 < latches.length) {
                    System.out.println("[线程" + index + "] 等待线程" + (index - 1) + "...");
                    latches[index - 1].await();
                }

                // 执行任务
                System.out.println("[线程" + index + "] 开始执行...");
                task.run();
                System.out.println("[线程" + index + "] 执行完成");

                // 唤醒下一个线程（如果不是最后一个）
                if (index < latches.length) {
                    System.out.println("[线程" + index + "] 唤醒线程" + (index + 1));
                    latches[index].countDown();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Thread-" + index).start();
    }

    /**
     * 使用示例
     */
    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("动态线程顺序执行器");
        System.out.println("========================================\n");

        // 创建5个线程的顺序执行器
        DynamicSequentialExecutor executor = new DynamicSequentialExecutor(5);

        // 提交5个任务（倒序提交，验证顺序控制）
        for (int i = 4; i >= 0; i--) {
            final int index = i;
            executor.submit(i, () -> {
                System.out.println("  执行任务 " + index);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // 等待所有任务完成
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n所有线程按顺序执行完成！");
    }
}

/**
 * ============================================
 * 对比：CountDownLatch vs 其他同步工具
 * ============================================
 */
class SynchronizationComparison {

    /**
     * CountDownLatch 特点：
     * - 一次性，计数器归零后不能重置
     * - 一个或多个线程等待其他线程完成
     * - 适用于：任务分组、多阶段任务
     *
     * CyclicBarrier 特点：
     * - 可循环使用
     * - 多个线程互相等待，同时到达屏障
     * - 适用于：分阶段计算、并行迭代
     *
     * Semaphore 特点：
     * - 控制同时访问的线程数量
     * - 适用于：资源池、限流
     *
     * Phaser 特点：
     * - 更灵活的分阶段控制
     * - 支持动态注册/注销参与者
     * - 适用于：复杂多阶段任务
     */

    public static void main(String[] args) {
        System.out.println("同步工具对比：");
        System.out.println("┌─────────────────┬─────────────────────────────────────────┐");
        System.out.println("│ CountDownLatch  │ 一次性，等待其他线程完成                 │");
        System.out.println("│ CyclicBarrier   │ 可循环，线程互相等待                     │");
        System.out.println("│ Semaphore       │ 控制并发数量                             │");
        System.out.println("│ Phaser          │ 灵活分阶段控制                           │");
        System.out.println("└─────────────────┴─────────────────────────────────────────┘");
    }
}
