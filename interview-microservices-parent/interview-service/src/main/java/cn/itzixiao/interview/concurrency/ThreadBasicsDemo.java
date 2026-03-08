package cn.itzixiao.interview.concurrency;

import java.util.concurrent.*;

/**
 * 多线程基础与线程创建方式详解
 * 
 * <p>本类全面讲解 Java 多线程编程的核心知识点，包含以下内容：</p>
 * 
 * <h2>主要内容：</h2>
 * <ol>
 *   <li><strong>线程创建的三种方式</strong>
 *     <ul>
 *       <li>继承 Thread 类（简单但有单继承限制）</li>
 *       <li>实现 Runnable 接口（推荐，灵活无返回值）</li>
 *       <li>实现 Callable 接口（有返回值，可获取执行结果）</li>
 *     </ul>
 *   </li>
 *   <li><strong>线程的 6 种状态</strong>
 *     <ul>
 *       <li>NEW - 新建状态，未调用 start()</li>
 *       <li>RUNNABLE - 就绪/运行状态，等待 CPU 调度</li>
 *       <li>BLOCKED - 阻塞状态，等待获取监视器锁</li>
 *       <li>WAITING - 无限期等待状态，等待其他线程唤醒</li>
 *       <li>TIMED_WAITING - 超时等待状态，指定时间内自动唤醒</li>
 *       <li>TERMINATED - 终止状态，线程执行完毕</li>
 *     </ul>
 *   </li>
 *   <li><strong>线程基本方法对比</strong>
 *     <ul>
 *       <li>sleep() - 暂停执行，不释放锁</li>
 *       <li>yield() - 让出 CPU，进入就绪队列</li>
 *       <li>join() - 等待线程完成</li>
 *       <li>interrupt() - 中断线程</li>
 *     </ul>
 *   </li>
 *   <li><strong>线程优先级和守护线程</strong>
 *     <ul>
 *       <li>优先级范围：1(MIN) - 5(NORMAL) - 10(MAX)</li>
 *       <li>守护线程：后台服务线程，JVM 退出时自动终止</li>
 *     </ul>
 *   </li>
 *   <li><strong>sleep() vs wait() 深度对比</strong>
 *     <ul>
 *       <li>核心区别：是否释放锁、所属类、使用场景</li>
 *       <li>经典应用：生产者 - 消费者模式</li>
 *     </ul>
 *   </li>
 *   <li><strong>死锁详解</strong>
 *     <ul>
 *       <li>死锁定义和 4 个必要条件</li>
 *       <li>经典死锁场景演示（ABBA 模式）</li>
 *       <li>预防和避免死锁的策略</li>
 *       <li>生产环境的最佳实践</li>
 *     </ul>
 *   </li>
 * </ol>
 * 
 * <h2>高频面试题覆盖：</h2>
 * <ul>
 *   <li>线程创建的三种方式及区别？</li>
 *   <li>Java 线程的生命周期和状态转换？</li>
 *   <li>sleep() 和 wait() 的本质区别？</li>
 *   <li>什么是死锁？如何预防和检测？</li>
 *   <li>守护线程的作用和应用场景？</li>
 *   <li>如何正确中断一个线程？</li>
 * </ul>
 * 
 * <h2>使用示例：</h2>
 * <pre>{@code
 * public static void main(String[] args) throws Exception {
 *     ThreadBasicsDemo.main(new String[]{});
 * }
 * }</pre>
 * 
 * @author itzixiao
 * @version 1.0
 * @since 2026-03-08
 */
public class ThreadBasicsDemo {

    /**
     * 主方法：执行所有演示示例
     * 
     * <p>按顺序执行以下 6 个部分的多线程演示：</p>
     * <ol>
     *   <li>线程创建的三种方式</li>
     *   <li>线程状态演示</li>
     *   <li>线程基本方法</li>
     *   <li>线程优先级和守护线程</li>
     *   <li>sleep() vs wait() 深度对比</li>
     *   <li>死锁详解与演示</li>
     * </ol>
     * 
     * @param args 命令行参数（未使用）
     * @throws Exception 演示过程中可能抛出的异常
     */
    public static void main(String[] args) throws Exception {
        System.out.println("========== 多线程基础与线程创建 ==========\n");

        // 1. 线程创建的三种方式
        demonstrateThreadCreation();

        // 2. 线程状态演示
        demonstrateThreadStates();

        // 3. 线程基本方法
        demonstrateThreadMethods();

        // 4. 线程优先级和守护线程
        demonstratePriorityAndDaemon();

        // 5. sleep() vs wait() 深度对比
        compareSleepAndWait();

        // 6. 死锁详解与演示
        demonstrateDeadlock();
    }

    /**
     * 1. 演示线程创建的三种方式
     * 
     * <p>详细说明：</p>
     * <ul>
     *   <li><strong>方式 1：继承 Thread 类</strong>
     *     <ul>
     *       <li>优点：代码简单直观</li>
     *       <li>缺点：受 Java 单继承限制，无法继承其他类</li>
     *     </ul>
     *   </li>
     *   <li><strong>方式 2：实现 Runnable 接口（推荐）</strong>
     *     <ul>
     *       <li>优点：避免单继承限制，可同时实现多个接口</li>
     *       <li>缺点：无法直接返回执行结果</li>
     *     </ul>
     *   </li>
     *   <li><strong>方式 3：实现 Callable 接口</strong>
     *     <ul>
     *       <li>优点：支持返回值，可获取异步执行结果</li>
     *       <li>缺点：需要配合 FutureTask 使用</li>
     *     </ul>
     *   </li>
     * </ul>
     * 
     * @throws InterruptedException 线程中断异常
     */
    private static void demonstrateThreadCreation() throws InterruptedException {
        System.out.println("【1. 线程创建的三种方式】\n");

        // 方式1：继承 Thread 类
        System.out.println("方式1：继承 Thread 类");
        Thread thread1 = new MyThread();
        thread1.start();
        thread1.join();
        System.out.println();

        // 方式2：实现 Runnable 接口（推荐）
        System.out.println("方式2：实现 Runnable 接口");
        Thread thread2 = new Thread(new MyRunnable(), "RunnableThread");
        thread2.start();
        thread2.join();
        System.out.println();

        // 方式3：实现 Callable 接口（有返回值）
        System.out.println("方式3：实现 Callable 接口（有返回值）");
        FutureTask<String> futureTask = new FutureTask<>(new MyCallable());
        Thread thread3 = new Thread(futureTask, "CallableThread");
        thread3.start();

        try {
            String result = futureTask.get(); // 阻塞等待结果
            System.out.println("Callable 返回结果: " + result);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println();

        // 对比总结
        System.out.println("三种方式对比：");
        System.out.println("┌─────────────┬─────────────┬─────────────┬─────────────┐");
        System.out.println("│   方式      │   继承      │   返回值    │   灵活性    │");
        System.out.println("├─────────────┼─────────────┼─────────────┼─────────────┤");
        System.out.println("│  Thread     │ 单继承限制   │ 无          │ 低          │");
        System.out.println("│  Runnable   │ 可实现多接口 │ 无          │ 高（推荐）   │");
        System.out.println("│  Callable   │ 可实现多接口 │ 有          │ 高          │");
        System.out.println("└─────────────┴─────────────┴─────────────┴─────────────┘\n");
    }

    /**
     * 2. 演示线程状态变化
     * 
     * <p>通过实际案例展示线程从 NEW → RUNNABLE → TIMED_WAITING → TERMINATED</p>
     * 的状态转换过程。</p>
     * 
     * <p>关键测试点：</p>
     * <ul>
     *   <li>使用 getState() 方法查看线程状态</li>
     *   <li>wait(timeout) 使线程进入 TIMED_WAITING</li>
     *   <li>超时后自动唤醒继续执行</li>
     * </ul>
     * 
     * @throws InterruptedException 线程中断异常
     */
    private static void demonstrateThreadStates() throws InterruptedException {
        System.out.println("【2. 线程状态演示】\n");

        System.out.println("Java 线程的 6 种状态：");
        System.out.println("1. NEW - 新建，未调用 start()");
        System.out.println("2. RUNNABLE - 就绪/运行，在 JVM 中执行");
        System.out.println("3. BLOCKED - 阻塞，等待监视器锁");
        System.out.println("4. WAITING - 等待，无限期等待（wait/join）");
        System.out.println("5. TIMED_WAITING - 超时等待（sleep/wait(timeout)）");
        System.out.println("6. TERMINATED - 终止，执行完毕\n");

        // 演示状态变化
        Object lock = new Object();

        Thread waitingThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    System.out.println("等待线程：获取锁，准备 wait()");
                    lock.wait(1000); // 进入 TIMED_WAITING
                    System.out.println("等待线程：被唤醒，继续执行");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "WaitingThread");

        System.out.println("新建状态: " + waitingThread.getState()); // NEW

        waitingThread.start();
        Thread.sleep(100); // 确保线程启动

        System.out.println("启动后状态: " + waitingThread.getState()); // RUNNABLE 或 TIMED_WAITING

        Thread.sleep(1500);
        System.out.println("执行完毕后状态: " + waitingThread.getState()); // TERMINATED
        System.out.println();
    }

    /**
     * 3. 演示线程基本方法的使用
     * 
     * <p>重点讲解以下方法的区别和使用场景：</p>
     * <ul>
     *   <li><strong>sleep(millis)</strong> - 暂停指定时间，不释放锁</li>
     *   <li><strong>yield()</strong> - 提示调度器让出 CPU（不保证立即生效）</li>
     *   <li><strong>join()</strong> - 等待线程执行完毕</li>
     *   <li><strong>interrupt()</strong> - 中断线程，抛出 InterruptedException</li>
     * </ul>
     * 
     * <p>注意事项：</p>
     * <ul>
     *   <li>sleep() 和 join() 都必须捕获 InterruptedException</li>
     *   <li>被中断后应重新设置中断标志：Thread.currentThread().interrupt()</li>
     * </ul>
     * 
     * @throws InterruptedException 线程中断异常
     */
    private static void demonstrateThreadMethods() throws InterruptedException {
        System.out.println("【3. 线程基本方法】\n");

        // sleep vs yield vs join
        System.out.println("sleep() vs yield() vs join()：");
        System.out.println("┌─────────────┬─────────────────────────────────────────┐");
        System.out.println("│   方法      │   说明                                  │");
        System.out.println("├─────────────┼─────────────────────────────────────────┤");
        System.out.println("│  sleep()    │ 暂停执行指定时间，不释放锁，进入 TIMED_WAITING │");
        System.out.println("│  yield()    │ 提示调度器当前线程愿意让出CPU，进入 RUNNABLE   │");
        System.out.println("│  join()     │ 等待该线程执行完毕，当前线程进入 WAITING       │");
        System.out.println("└─────────────┴─────────────────────────────────────────┘\n");

        // sleep 演示
        System.out.println("sleep() 演示：");
        Thread sleepThread = new Thread(() -> {
            System.out.println("  线程开始，时间: " + System.currentTimeMillis() % 10000);
            try {
                Thread.sleep(1000); // 休眠1秒
            } catch (InterruptedException e) {
                System.out.println("  线程被中断");
            }
            System.out.println("  线程结束，时间: " + System.currentTimeMillis() % 10000);
        });
        sleepThread.start();
        sleepThread.join();
        System.out.println();

        // join 演示
        System.out.println("join() 演示：");
        Thread worker = new Thread(() -> {
            System.out.println("  工作线程开始执行任务...");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("  工作线程任务完成");
        }, "Worker");

        System.out.println("主线程：启动工作线程");
        worker.start();
        System.out.println("主线程：等待工作线程完成（join）");
        worker.join();
        System.out.println("主线程：继续执行\n");

        // interrupt 演示
        System.out.println("interrupt() 演示：");
        Thread interruptibleThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("  工作中...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    System.out.println("  收到中断信号，清理资源后退出");
                    Thread.currentThread().interrupt(); // 重新设置中断标志
                    break;
                }
            }
        });

        interruptibleThread.start();
        Thread.sleep(1200);
        System.out.println("主线程：发送中断信号");
        interruptibleThread.interrupt();
        interruptibleThread.join();
        System.out.println();
    }

    /**
     * 4. 演示线程优先级和守护线程
     * 
     * <p>包含两个重要概念：</p>
     * <ol>
     *   <li><strong>线程优先级</strong>
     *     <ul>
     *       <li>范围：1 (MIN_PRIORITY) 到 10 (MAX_PRIORITY)，默认 5 (NORM_PRIORITY)</li>
     *       <li>注意：优先级只是提示，实际调度取决于操作系统</li>
     *       <li>高优先级线程获得 CPU 的概率更高，但不保证一定先执行</li>
     *     </ul>
     *   </li>
     *   <li><strong>守护线程（Daemon Thread）</strong>
     *     <ul>
     *       <li>后台服务线程，如 GC 垃圾回收线程</li>
     *       <li>当所有非守护线程结束时，JVM 自动退出，不等待守护线程</li>
     *       <li>必须在 start() 之前调用 setDaemon(true)</li>
     *     </ul>
     *   </li>
     * </ol>
     * 
     * @throws InterruptedException 线程中断异常
     */
    private static void demonstratePriorityAndDaemon() throws InterruptedException {
        System.out.println("【4. 线程优先级和守护线程】\n");

        // 线程优先级
        System.out.println("线程优先级（1-10，默认5）：");
        System.out.println("注意：优先级只是提示，实际调度取决于操作系统\n");

        Thread highPriority = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                System.out.println("  高优先级线程执行: " + i);
            }
        }, "HighPriority");
        highPriority.setPriority(Thread.MAX_PRIORITY); // 10

        Thread lowPriority = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                System.out.println("  低优先级线程执行: " + i);
            }
        }, "LowPriority");
        lowPriority.setPriority(Thread.MIN_PRIORITY); // 1

        lowPriority.start();
        highPriority.start();
        highPriority.join();
        lowPriority.join();
        System.out.println();

        // 守护线程
        System.out.println("守护线程（Daemon Thread）：");
        System.out.println("- 后台服务线程，为其他线程提供服务");
        System.out.println("- 所有非守护线程结束后，JVM 自动退出，不等待守护线程\n");

        Thread daemon = new Thread(() -> {
            while (true) {
                System.out.println("  守护线程：后台监控中...");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "DaemonThread");
        daemon.setDaemon(true);
        daemon.start();

        Thread.sleep(1000);
        System.out.println("主线程即将结束，守护线程也会随之终止\n");
    }

    /**
     * 5. sleep() vs wait() 深度对比
     * 
     * <p>全面对比这两个容易混淆的方法，包含以下维度：</p>
     * 
     * <h3>核心区别：</h3>
     * <table border="1">
     *   <tr><th>对比项</th><th>sleep()</th><th>wait()</th></tr>
     *   <tr><td>所属类</td><td>Thread（静态方法）</td><td>Object（实例方法）</td></tr>
     *   <tr><td>锁释放</td><td>❌ 不释放锁</td><td>✅ 释放锁</td></tr>
     *   <tr><td>使用范围</td><td>任何地方</td><td>synchronized 同步块内</td></tr>
     *   <tr><td>唤醒方式</td><td>超时自动唤醒</td><td>notify()/notifyAll()</td></tr>
     * </table>
     * 
     * <h3>示例代码：</h3>
     * <ol>
     *   <li>sleep() 不释放锁演示 - 其他线程无法访问同步代码</li>
     *   <li>wait() 释放锁演示 - 其他线程可以获取锁并执行</li>
     *   <li>生产者 - 消费者模式 - wait()/notify() 经典应用</li>
     * </ol>
     * 
     * <h3>高频面试题：</h3>
     * <ul>
     *   <li>sleep() 和 wait() 的主要区别？</li>
     *   <li>为什么 wait() 必须在 synchronized 块中调用？</li>
     *   <li>如何正确停止一个正在 sleep 的线程？</li>
     * </ul>
     * 
     * @throws InterruptedException 线程中断异常
     */
    private static void compareSleepAndWait() throws InterruptedException {
        System.out.println("【5. sleep() vs wait() 深度对比】\n");

        System.out.println("核心区别对比表：");
        System.out.println("┌───────────────────┬─────────────────────────────┬─────────────────────────────┐");
        System.out.println("│      对比项       │      Thread.sleep()         │       Object.wait()         │");
        System.out.println("├───────────────────┼─────────────────────────────┼─────────────────────────────┤");
        System.out.println("│   所属类          │   Thread 类（静态方法）      │   Object 类（实例方法）      │");
        System.out.println("│   是否释放锁      │   ❌ 不释放锁                │   ✅ 释放锁                   │");
        System.out.println("│   使用范围        │   任何地方                  │   synchronized 同步块内       │");
        System.out.println("│   唤醒方式        │   超时自动唤醒              │   notify()/notifyAll()       │");
        System.out.println("│   异常处理        │   必须捕获 InterruptedException │ 必须捕获 InterruptedException │");
        System.out.println("│   依赖监视器      │   不需要                    │   需要（必须先获取锁）       │");
        System.out.println("└───────────────────┴─────────────────────────────┴─────────────────────────────┘\n");

        // 示例 1：sleep() 不释放锁
        System.out.println("示例 1：sleep() 不释放锁（其他线程无法访问同步代码）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Object sleepLock = new Object();
        
        Thread sleepHolder = new Thread(() -> {
            synchronized (sleepLock) {
                System.out.println("  线程 A: 获取锁，开始 sleep(2000ms)");
                try {
                    Thread.sleep(2000); // 休眠 2 秒，但不释放锁
                    System.out.println("  线程 A: sleep 结束，继续执行");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "SleepHolder");

        Thread sleepWaiter = new Thread(() -> {
            synchronized (sleepLock) {
                System.out.println("  线程 B: 获取锁，执行任务");
            }
        }, "SleepWaiter");

        sleepHolder.start();
        Thread.sleep(100); // 确保 A 先启动
        sleepWaiter.start();

        sleepHolder.join();
        sleepWaiter.join();
        System.out.println("✅ 结论：线程 A sleep 时持有锁，线程 B 必须等待\n");

        // 示例 2：wait() 释放锁
        System.out.println("示例 2：wait() 释放锁（其他线程可以访问同步代码）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        final Object waitLock = new Object();
        final boolean[] notified = {false};

        Thread waiter = new Thread(() -> {
            synchronized (waitLock) {
                System.out.println("  线程 C: 获取锁，准备 wait()");
                try {
                    waitLock.wait(2000); // 等待 2 秒或直到被通知
                    System.out.println("  线程 C: 被唤醒，继续执行");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Waiter");

        Thread notifier = new Thread(() -> {
            synchronized (waitLock) {
                System.out.println("  线程 D: 获取锁，执行任务");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("  线程 D: 任务完成，调用 notify()");
                waitLock.notify(); // 唤醒等待的线程
            }
        }, "Notifier");

        waiter.start();
        Thread.sleep(100); // 确保 C 先进入等待
        notifier.start();

        waiter.join();
        notifier.join();
        System.out.println("✅ 结论：线程 C wait() 时释放锁，线程 D 可以获取并执行\n");

        // 示例 3：经典生产者 - 消费者模式
        System.out.println("示例 3：wait()/notify() 经典应用 - 生产者消费者模式");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Buffer buffer = new Buffer();

        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                buffer.put(i);
            }
        }, "Producer");

        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 3; i++) {
                buffer.get();
            }
        }, "Consumer");

        consumer.start();
        Thread.sleep(100); // 确保消费者先启动等待
        producer.start();

        producer.join();
        consumer.join();
        System.out.println();

        // 示例 4：高频面试题
        System.out.println("【sleep() vs wait() 高频面试题】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Q1: sleep() 和 wait() 的主要区别是什么？");
        System.out.println("A1: ");
        System.out.println("   1. 锁释放：sleep 不释放锁，wait 释放锁");
        System.out.println("   2. 所属类：sleep 是 Thread 的静态方法，wait 是 Object 的实例方法");
        System.out.println("   3. 使用场景：sleep 用于暂停执行，wait 用于线程间通信");
        System.out.println("   4. 依赖：wait 必须在 synchronized 块中使用，sleep 无此要求\n");

        System.out.println("Q2: 为什么 wait() 必须在 synchronized 块中调用？");
        System.out.println("A2: 因为 wait() 会释放当前持有的对象锁，如果不在 synchronized 块中，");
        System.out.println("   线程就没有持有任何锁，也就无法释放，会导致 IllegalMonitorStateException。\n");

        System.out.println("Q3: sleep() 和 yield() 的区别？");
        System.out.println("A3: ");
        System.out.println("   - sleep(): 暂停指定时间，进入 TIMED_WAITING，不释放锁");
        System.out.println("   - yield(): 让出 CPU，进入 RUNNABLE，给其他同优先级线程机会，不释放锁\n");

        System.out.println("Q4: 如何正确停止一个正在 sleep 的线程？");
        System.out.println("A4: 使用 interrupt() 方法中断，sleep 会抛出 InterruptedException，");
        System.out.println("   在 catch 块中重新设置中断标志：Thread.currentThread().interrupt();\n");
    }

    /**
     * 6. 死锁（Deadlock）详解与演示
     * 
     * <p>深入讲解死锁的概念、产生条件、预防策略和实际案例。</p>
     * 
     * <h3>死锁的 4 个必要条件：</h3>
     * <ol>
     *   <li><strong>互斥条件</strong> - 资源一次只能被一个线程占用</li>
     *   <li><strong>请求与保持条件</strong> - 线程已持有资源，又申请新资源</li>
     *   <li><strong>不剥夺条件</strong> - 已获得的资源不能被强制剥夺</li>
     *   <li><strong>循环等待条件</strong> - 线程间形成头尾相连的循环等待链</li>
     * </ol>
     * 
     * <h3>演示案例：</h3>
     * <ol>
     *   <li><strong>经典死锁场景（ABBA 模式）</strong> - 两个线程互相等待对方的锁</li>
     *   <li><strong>正确的锁顺序</strong> - 按相同顺序获取锁，破坏循环等待</li>
     *   <li><strong>数据库事务死锁模拟</strong> - 转账操作的死锁场景</li>
     * </ol>
     * 
     * <h3>预防策略：</h3>
     * <ul>
     *   <li>破坏请求与保持：一次性申请所有资源</li>
     *   <li>破坏不剥夺：允许抢占资源（可能导致饥饿）</li>
     *   <li>破坏循环等待：对资源编号，按顺序申请（最常用）</li>
     * </ul>
     * 
     * <h3>生产环境最佳实践：</h3>
     * <ul>
     *   <li>使用定时锁：tryLock(timeout, TimeUnit)</li>
     *   <li>减少锁粒度：使用更细粒度的锁对象</li>
     *   <li>使用并发工具类：ConcurrentHashMap、ReentrantLock 等</li>
     *   <li>死锁检测：jps + jstack、VisualVM、JConsole</li>
     * </ul>
     * 
     * <h3>高频面试题：</h3>
     * <ul>
     *   <li>什么是死锁？产生的 4 个条件？</li>
     *   <li>如何预防和避免死锁？</li>
     *   <li>如何检测和诊断死锁？</li>
     *   <li>tryLock() 能否完全避免死锁？</li>
     * </ul>
     * 
     * @throws InterruptedException 线程中断异常
     */
    private static void demonstrateDeadlock() throws InterruptedException {
        System.out.println("【6. 死锁（Deadlock）详解】\n");

        System.out.println("死锁定义：");
        System.out.println("┌─────────────────────────────────────────────────────────────────┐");
        System.out.println("│  两个或多个线程互相等待对方释放锁资源，导致所有线程都无法      │");
        System.out.println("│  继续执行的僵局状态。                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────────┘\n");

        System.out.println("死锁产生的 4 个必要条件：");
        System.out.println("1. 互斥条件         - 资源一次只能被一个线程占用");
        System.out.println("2. 请求与保持条件   - 线程已持有资源，又申请新资源");
        System.out.println("3. 不剥夺条件       - 已获得的资源不能被强制剥夺");
        System.out.println("4. 循环等待条件     - 线程间形成头尾相连的循环等待链\n");

        // 示例 1：经典死锁场景
        System.out.println("示例 1：经典死锁场景（ABBA 模式）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Object lock1 = new Object();
        Object lock2 = new Object();

        Thread threadA = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("  线程 A: 获取 lock1");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                System.out.println("  线程 A: 等待 lock2...");
                synchronized (lock2) {
                    System.out.println("  线程 A: 获取 lock2，执行任务");
                }
            }
        }, "Thread-A");

        Thread threadB = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("  线程 B: 获取 lock2");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                System.out.println("  线程 B: 等待 lock1...");
                synchronized (lock1) {
                    System.out.println("  线程 B: 获取 lock1，执行任务");
                }
            }
        }, "Thread-B");

        System.out.println("启动两个线程（将产生死锁）：");
        threadA.start();
        threadB.start();
        Thread.sleep(500); // 等待死锁形成

        System.out.println("\n⚠️  检测到死锁！两个线程都在等待对方释放锁");
        System.out.println("💡 解决方案：按相同顺序获取锁，破坏循环等待条件\n");

        // 示例 2：解决死锁的正确方式
        System.out.println("示例 2：正确的锁顺序（避免死锁）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        final Object resource1 = new Object();
        final Object resource2 = new Object();

        Thread safeThreadA = new Thread(() -> {
            synchronized (resource1) {
                System.out.println("  安全线程 A: 获取 resource1");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (resource2) {
                    System.out.println("  安全线程 A: 获取 resource2，执行任务");
                }
            }
        }, "SafeThread-A");

        Thread safeThreadB = new Thread(() -> {
            synchronized (resource1) { // 也先获取 resource1，破坏循环等待
                System.out.println("  安全线程 B: 获取 resource1");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (resource2) {
                    System.out.println("  安全线程 B: 获取 resource2，执行任务");
                }
            }
        }, "SafeThread-B");

        System.out.println("启动两个线程（按相同顺序获取锁）：");
        safeThreadA.start();
        safeThreadB.start();
        safeThreadA.join();
        safeThreadB.join();
        System.out.println("✅ 两个线程都成功执行完成，无死锁\n");

        // 示例 3：数据库死锁模拟
        System.out.println("示例 3：数据库事务死锁模拟");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        Database db = new Database();

        Thread transaction1 = new Thread(() -> {
            System.out.println("  事务 1: 开始转账操作");
            db.transferMoney("账户 A", "账户 B", 100);
        }, "Transaction-1");

        Thread transaction2 = new Thread(() -> {
            System.out.println("  事务 2: 开始反向转账操作");
            db.transferMoney("账户 B", "账户 A", 50);
        }, "Transaction-2");

        transaction1.start();
        transaction2.start();
        Thread.sleep(1000);
        System.out.println("⚠️  可能发生死锁，取决于数据库锁的获取顺序\n");

        // 示例 4：死锁检测与预防
        System.out.println("【死锁预防策略】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("1. 破坏请求与保持条件：");
        System.out.println("   - 一次性申请所有资源，否则不分配任何资源");
        System.out.println("   - 缺点：资源利用率低\n");

        System.out.println("2. 破坏不剥夺条件：");
        System.out.println("   - 允许抢占资源，如果一个线程请求不到所有资源，");
        System.out.println("     则释放已持有的资源");
        System.out.println("   - 缺点：可能导致饥饿\n");

        System.out.println("3. 破坏循环等待条件（最常用）：");
        System.out.println("   - 对所有资源编号，按编号顺序申请");
        System.out.println("   - 例如：总是先获取 lock1，再获取 lock2");
        System.out.println("   - 优点：简单有效\n");

        // 示例 5：高频面试题
        System.out.println("【死锁高频面试题】");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("Q1: 什么是死锁？产生的条件是什么？");
        System.out.println("A1: 死锁是多个线程互相等待对方释放锁导致的僵局。");
        System.out.println("   4 个必要条件：互斥、请求与保持、不剥夺、循环等待。\n");

        System.out.println("Q2: 如何预防和避免死锁？");
        System.out.println("A2: ");
        System.out.println("   - 破坏循环等待：按固定顺序获取锁（最常用）");
        System.out.println("   - 破坏请求与保持：一次性申请所有资源");
        System.out.println("   - 破坏不剥夺：允许抢占资源\n");

        System.out.println("Q3: 如何检测和诊断死锁？");
        System.out.println("A3: ");
        System.out.println("   - jps + jstack 命令：查看线程堆栈");
        System.out.println("   - VisualVM/JConsole：图形化工具监控");
        System.out.println("   - 日志分析：查找长时间阻塞的线程\n");

        System.out.println("Q4: 生产环境如何避免死锁？");
        System.out.println("A4: ");
        System.out.println("   - 使用定时锁：tryLock(timeout, TimeUnit)");
        System.out.println("   - 银行家算法：资源分配前预检查安全性");
        System.out.println("   - 减少锁粒度：使用更细粒度的锁对象");
        System.out.println("   - 使用并发工具类：如 ConcurrentHashMap 等\n");
    }

    /**
     * 数据库类（模拟数据库事务死锁）
     * 
     * <p>模拟银行账户转账场景，演示不当的锁获取顺序可能导致的死锁问题。</p>
     * 
     * <h3>死锁场景：</h3>
     * <pre>{@code
     * 事务 1: transferMoney("账户 A", "账户 B", 100)
     *   - 锁定 accountA
     *   - 等待 accountB
     * 
     * 事务 2: transferMoney("账户 B", "账户 A", 50)
     *   - 锁定 accountB
     *   - 等待 accountA
     * 
     * 结果：两个事务互相等待，形成死锁
     * }</pre>
     * 
     * <h3>解决方案：</h3>
     * <ul>
     *   <li>固定锁顺序：总是先锁定编号小的账户，再锁定编号大的账户</li>
     *   <li>使用定时锁：tryLock(timeout)，超时后回滚并重试</li>
     *   <li>银行家算法：预先检查资源分配的安全性</li>
     * </ul>
     */
    static class Database {
        private final Object accountA = new Object();
        private final Object accountB = new Object();

        /**
         * 转账操作（可能产生死锁的版本）
         * 
         * <p>按照 from → to 的顺序获取锁，当两个事务反向转账时会形成死锁。</p>
         * 
         * @param from   转出账户名称
         * @param to     转入账户名称
         * @param amount 转账金额
         */
        public void transferMoney(String from, String to, int amount) {
            Object fromLock = getLock(from);
            Object toLock = getLock(to);

            // 注意：这里可能产生死锁，因为两个事务获取锁的顺序不同
            synchronized (fromLock) {
                System.out.println("  [" + Thread.currentThread().getName() + 
                                 "] 锁定 " + from);
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (toLock) {
                    System.out.println("  [" + Thread.currentThread().getName() + 
                                     "] 锁定 " + to + ", 转账 " + amount);
                }
            }
        }

        /**
         * 根据账户名称获取对应的锁对象
         * 
         * @param account 账户名称（"账户 A" 或 "账户 B"）
         * @return 对应的锁对象
         */
        private Object getLock(String account) {
            return "账户 A".equals(account) ? accountA : accountB;
        }
    }

    /**
     * 缓冲区类（用于生产者 - 消费者示例）
     * 
     * <p>实现一个简单的线程安全缓冲区，支持单个数据的生产与消费。</p>
     * 
     * <h3>核心特性：</h3>
     * <ul>
     *   <li>使用 wait()/notifyAll() 实现线程间通信</li>
     *   <li>缓冲区为空时，消费者等待；缓冲区非空时，生产者等待</li>
     *   <li>使用 while 循环检查条件，防止虚假唤醒</li>
     *   <li>synchronized 保证方法同步，同一时间只有一个线程访问</li>
     * </ul>
     * 
     * <h3>工作流程：</h3>
     * <pre>{@code
     * 生产者：put() → 如果 !empty 则 wait() → 放入数据 → notifyAll()
     * 消费者：get()  → 如果 empty 则 wait()  → 取出数据 → notifyAll()
     * }</pre>
     * 
     * <p><strong>注意：</strong>使用 notifyAll() 而不是 notify()，确保所有等待的线程都有机会被唤醒。</p>
     */
    static class Buffer {
        private int data;
        private boolean empty = true;

        /**
         * 向缓冲区放入数据（生产者方法）
         * 
         * <p>如果缓冲区非空（!empty），则等待消费者消费；否则放入数据并通知所有等待的线程。</p>
         * 
         * @param value 要放入的数据值
         */
        public synchronized void put(int value) {
            while (!empty) { // 缓冲区非空，等待
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.data = value;
            this.empty = false;
            System.out.println("  [Producer] 生产数据：" + value);
            notifyAll(); // 通知消费者
        }

        /**
         * 从缓冲区取出数据（消费者方法）
         * 
         * <p>如果缓冲区为空（empty），则等待生产者生产；否则取出数据并通知所有等待的线程。</p>
         * 
         * @return 取出的数据值
         */
        public synchronized int get() {
            while (empty) { // 缓冲区为空，等待
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            this.empty = true;
            System.out.println("  [Consumer] 消费数据：" + data);
            notifyAll(); // 通知生产者
            return data;
        }
    }

    // ==================== 线程创建类定义 ====================

    /**
     * 方式1：继承 Thread
     */
    static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("Thread 类线程运行: " + Thread.currentThread().getName());
        }
    }

    /**
     * 方式2：实现 Runnable
     */
    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("Runnable 接口线程运行: " + Thread.currentThread().getName());
        }
    }

    /**
     * 方式3：实现 Callable
     */
    static class MyCallable implements Callable<String> {
        @Override
        public String call() throws Exception {
            Thread.sleep(500);
            return "Callable 执行结果 from " + Thread.currentThread().getName();
        }
    }
}
