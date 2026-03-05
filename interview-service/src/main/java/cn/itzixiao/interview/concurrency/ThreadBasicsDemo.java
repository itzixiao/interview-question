package cn.itzixiao.interview.concurrency;

import java.util.concurrent.*;

/**
 * 多线程基础与线程创建方式
 *
 * 线程状态：
 * ┌─────────┐   start()   ┌─────────┐   获取锁   ┌─────────┐
 * │  NEW    │ ─────────→ │ RUNNABLE│ ───────→ │ RUNNING │
 * │ (新建)  │            │ (就绪)  │          │ (运行)  │
 * └─────────┘            └─────────┘          └────┬────┘
 *      ↑                                           │
 *      │    ┌─────────┐   wait()    ┌─────────┐   │
 *      └────│ TERMINATED│ ←─────────│  WAITING │ ←─┘ 释放锁
 *           │ (终止)  │   notify()  │ (等待)  │
 *           └─────────┘             └────┬────┘
 *                ↑                       │ sleep()
 *                │    ┌─────────┐        │ (不释放锁)
 *                └────│ BLOCKED │ ←──────┘
 *                     │ (阻塞)  │
 *                     └─────────┘
 */
public class ThreadBasicsDemo {

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
    }

    /**
     * 1. 线程创建的三种方式
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
     * 2. 线程状态演示
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
     * 3. 线程基本方法
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
     * 4. 线程优先级和守护线程
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
