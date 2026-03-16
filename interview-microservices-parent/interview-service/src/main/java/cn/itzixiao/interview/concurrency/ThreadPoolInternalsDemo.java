package cn.itzixiao.interview.concurrency;

/**
 * 线程池核心源码详解（基于 ThreadPoolExecutor）
 * <p>
 * 线程池核心设计：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 线程复用：工作线程循环从任务队列取任务执行                │
 * │  2. 线程管理：核心线程数、最大线程数、空闲回收                │
 * │  3. 任务队列：缓冲待执行任务                                  │
 * │  4. 拒绝策略：队列满且线程数达最大时的处理                    │
 * └─────────────────────────────────────────────────────────────┘
 * <p>
 * 线程池状态（ctl 变量高3位）：
 * ┌───────────┬─────────────────────────────────────────────────┐
 * │  RUNNING  │  接受新任务，处理队列任务    │  111 << 29       │
 * │  SHUTDOWN │  不接受新任务，处理队列任务  │  000 << 29       │
 * │  STOP     │  不接受新任务，不处理队列    │  001 << 29       │
 * │  TIDYING  │  所有任务完成，准备terminated│  010 << 29       │
 * │  TERMINATED│  terminated() 执行完毕      │  011 << 29       │
 * └───────────┴─────────────────────────────────────────────────┘
 */
public class ThreadPoolInternalsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 线程池核心源码详解 ==========\n");

        // 1. 核心结构与状态
        demonstrateCoreStructure();

        // 2. 任务提交流程
        demonstrateExecuteFlow();

        // 3. Worker 内部类
        demonstrateWorker();

        // 4. 线程回收
        demonstrateWorkerExit();

        // 5. 关闭流程
        demonstrateShutdown();

        // 6. 线程池监控与调优
        demonstrateMonitoring();
    }

    /**
     * 1. 核心结构与状态
     */
    private static void demonstrateCoreStructure() {
        System.out.println("【1. 线程池核心结构】\n");

        System.out.println("ThreadPoolExecutor 核心字段：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 主控制变量：高3位=状态，低29位=工作线程数                  │");
        System.out.println("│  private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));│");
        System.out.println("│                                                             │");
        System.out.println("│  private final BlockingQueue<Runnable> workQueue;          │");
        System.out.println("│  // 任务等待队列                                            │");
        System.out.println("│                                                             │");
        System.out.println("│  private final HashSet<Worker> workers = new HashSet<>();  │");
        System.out.println("│  // 工作线程集合（非线程安全，需加 mainLock）                 │");
        System.out.println("│                                                             │");
        System.out.println("│  private final ReentrantLock mainLock = new ReentrantLock();│");
        System.out.println("│  // 操作 workers 时的锁                                     │");
        System.out.println("│                                                             │");
        System.out.println("│  private final Condition termination = mainLock.newCondition();│");
        System.out.println("│  // 等待终止的条件                                          │");
        System.out.println("│                                                             │");
        System.out.println("│  private volatile int corePoolSize;    // 核心线程数        │");
        System.out.println("│  private volatile int maximumPoolSize; // 最大线程数        │");
        System.out.println("│  private volatile long keepAliveTime;  // 空闲线程存活时间   │");
        System.out.println("│  private volatile ThreadFactory threadFactory;              │");
        System.out.println("│  private volatile RejectedExecutionHandler handler;         │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("ctl 变量设计（一个变量保存两个信息）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  32位整数：                                                 │");
        System.out.println("│  ┌────────┬───────────────────────────────────────────────┐ │");
        System.out.println("│  │  高3位  │              低29位（工作线程数量）            │ │");
        System.out.println("│  ├────────┼───────────────────────────────────────────────┤ │");
        System.out.println("│  │  111   │              000...000（0个线程）              │ │");
        System.out.println("│  │ RUNNING│                                                 │ │");
        System.out.println("│  ├────────┼───────────────────────────────────────────────┤ │");
        System.out.println("│  │  000   │              000...101（5个线程）              │ │");
        System.out.println("│  │SHUTDOWN│                                                 │ │");
        System.out.println("│  └────────┴───────────────────────────────────────────────┘ │");
        System.out.println("│                                                             │");
        System.out.println("│  位运算操作：                                                │");
        System.out.println("│  - CAPACITY = (1 << 29) - 1 = 000111...111（29个1）        │");
        System.out.println("│  - runStateOf(ctl) = ctl & ~CAPACITY  // 取高3位状态       │");
        System.out.println("│  - workerCountOf(ctl) = ctl & CAPACITY  // 取低29位计数    │");
        System.out.println("│  - ctlOf(rs, wc) = rs | wc  // 组合状态和计数              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 2. 任务提交流程
     */
    private static void demonstrateExecuteFlow() {
        System.out.println("【2. execute() 任务提交流程】\n");

        System.out.println("execute(Runnable command) 执行流程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. if (command == null) throw new NullPointerException();  │");
        System.out.println("│     // 参数校验                                             │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. int c = ctl.get();                                      │");
        System.out.println("│     if (workerCountOf(c) < corePoolSize) {                  │");
        System.out.println("│         if (addWorker(command, true))  // 核心线程          │");
        System.out.println("│             return;                                         │");
        System.out.println("│         c = ctl.get();                                      │");
        System.out.println("│     }                                                       │");
        System.out.println("│     // 当前线程数 < 核心线程数：创建核心线程执行任务         │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. if (isRunning(c) && workQueue.offer(command)) {         │");
        System.out.println("│         // 线程池运行中，任务加入队列成功                    │");
        System.out.println("│         int recheck = ctl.get();                            │");
        System.out.println("│         if (!isRunning(recheck) && remove(command))         │");
        System.out.println("│             reject(command);  // 状态变化，拒绝任务          │");
        System.out.println("│         else if (workerCountOf(recheck) == 0)               │");
        System.out.println("│             addWorker(null, false);  // 至少保证一个线程     │");
        System.out.println("│         return;                                             │");
        System.out.println("│     }                                                       │");
        System.out.println("│     // 加入队列（核心线程已满）                              │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  4. else if (!addWorker(command, false))  // 非核心线程     │");
        System.out.println("│     reject(command);                                        │");
        System.out.println("│     // 队列已满，创建非核心线程，失败则拒绝                  │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("addWorker(Runnable firstTask, boolean core) 创建线程：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 自旋 CAS 增加 workerCount                                │");
        System.out.println("│     retry:                                                  │");
        System.out.println("│     for (;;) {                                              │");
        System.out.println("│         int c = ctl.get();                                  │");
        System.out.println("│         int rs = runStateOf(c);                             │");
        System.out.println("│         // 检查状态是否可以创建线程...                       │");
        System.out.println("│         int wc = workerCountOf(c);                          │");
        System.out.println("│         if (wc >= CAPACITY ||                               │");
        System.out.println("│             wc >= (core ? corePoolSize : maximumPoolSize))  │");
        System.out.println("│             return false;  // 超过限制                      │");
        System.out.println("│         if (compareAndIncrementWorkerCount(c))              │");
        System.out.println("│             break retry;  // CAS成功，退出循环              │");
        System.out.println("│     }                                                       │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. 创建 Worker，加入 workers 集合，启动线程                │");
        System.out.println("│     Worker w = new Worker(firstTask);                       │");
        System.out.println("│     final Thread t = w.thread;                              │");
        System.out.println("│     mainLock.lock();                                        │");
        System.out.println("│     try {                                                   │");
        System.out.println("│         workers.add(w);                                     │");
        System.out.println("│     } finally {                                             │");
        System.out.println("│         mainLock.unlock();                                  │");
        System.out.println("│     }                                                       │");
        System.out.println("│     t.start();  // 启动工作线程                             │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 3. Worker 内部类
     */
    private static void demonstrateWorker() {
        System.out.println("【3. Worker 内部类 - 工作线程封装】\n");

        System.out.println("Worker 类结构：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  private final class Worker extends AbstractQueuedSynchronizer│");
        System.out.println("│          implements Runnable {                              │");
        System.out.println("│                                                             │");
        System.out.println("│      final Thread thread;      // 工作线程                  │");
        System.out.println("│      Runnable firstTask;       // 初始任务（可能为null）    │");
        System.out.println("│      volatile long completedTasks;  // 完成任务计数         │");
        System.out.println("│                                                             │");
        System.out.println("│      Worker(Runnable firstTask) {                           │");
        System.out.println("│          setState(-1);  // 禁止中断直到 runWorker           │");
        System.out.println("│          this.firstTask = firstTask;                        │");
        System.out.println("│          this.thread = getThreadFactory().newThread(this);  │");
        System.out.println("│      }                                                      │");
        System.out.println("│                                                             │");
        System.out.println("│      public void run() {                                    │");
        System.out.println("│          runWorker(this);                                   │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("为什么 Worker 继承 AQS 而不是直接使用 ReentrantLock？");
        System.out.println("1. 实现不可重入的互斥锁（Worker 只需要独占锁，不需要重入）");
        System.out.println("2. 控制中断状态（setState(-1) 禁止中断，runWorker 后允许）");
        System.out.println("3. 更轻量，不需要 ReentrantLock 的复杂功能\n");

        System.out.println("runWorker(Worker w) - 线程执行主循环：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  final void runWorker(Worker w) {                           │");
        System.out.println("│      Runnable task = w.firstTask;                           │");
        System.out.println("│      w.firstTask = null;                                    │");
        System.out.println("│      w.unlock();  // 允许中断（state从-1变为0）             │");
        System.out.println("│                                                             │");
        System.out.println("│      while (task != null || (task = getTask()) != null) {   │");
        System.out.println("│          // 循环获取任务执行                                 │");
        System.out.println("│          w.lock();  // 获取独占锁，防止被其他线程中断        │");
        System.out.println("│                                                             │");
        System.out.println("│          // 检查线程池状态...                                │");
        System.out.println("│                                                             │");
        System.out.println("│          try {                                              │");
        System.out.println("│              beforeExecute(thread, task);  // 前置钩子       │");
        System.out.println("│              try {                                          │");
        System.out.println("│                  task.run();     // 执行任务！               │");
        System.out.println("│              } finally {                                    │");
        System.out.println("│                  afterExecute(task, thrown);  // 后置钩子    │");
        System.out.println("│              }                                              │");
        System.out.println("│          } finally {                                        │");
        System.out.println("│              task = null;                                   │");
        System.out.println("│              w.completedTasks++;                            │");
        System.out.println("│              w.unlock();                                    │");
        System.out.println("│          }                                                  │");
        System.out.println("│      }                                                      │");
        System.out.println("│                                                             │");
        System.out.println("│      processWorkerExit(w, completedAbruptly);  // 线程退出  │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("getTask() - 从队列获取任务（带超时控制）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  private Runnable getTask() {                               │");
        System.out.println("│      boolean timedOut = false;                              │");
        System.out.println("│                                                             │");
        System.out.println("│      for (;;) {                                             │");
        System.out.println("│          int c = ctl.get();                                 │");
        System.out.println("│          int rs = runStateOf(c);                            │");
        System.out.println("│                                                             │");
        System.out.println("│          // 检查是否需要回收线程...                          │");
        System.out.println("│                                                             │");
        System.out.println("│          // 是否需要超时控制                                 │");
        System.out.println("│          boolean timed = allowCoreThreadTimeOut ||          │");
        System.out.println("│                    wc > corePoolSize;                       │");
        System.out.println("│                                                             │");
        System.out.println("│          try {                                              │");
        System.out.println("│              Runnable r = timed ?                           │");
        System.out.println("│                  workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :│");
        System.out.println("│                  workQueue.take();                          │");
        System.out.println("│              if (r != null)                                 │");
        System.out.println("│                  return r;                                  │");
        System.out.println("│              timedOut = true;  // 超时未获取到任务          │");
        System.out.println("│          } catch (InterruptedException retry) {             │");
        System.out.println("│              timedOut = false;                              │");
        System.out.println("│          }                                                  │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 4. 线程回收
     */
    private static void demonstrateWorkerExit() {
        System.out.println("【4. 线程回收机制】\n");

        System.out.println("线程回收触发条件：");
        System.out.println("1. 当前线程数 > 核心线程数，且从队列获取任务超时");
        System.out.println("2. 允许核心线程超时（allowCoreThreadTimeOut=true），核心线程获取任务超时");
        System.out.println("3. 线程池关闭，任务队列为空\n");

        System.out.println("processWorkerExit(Worker w, boolean completedAbruptly) - 线程退出处理：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  private void processWorkerExit(Worker w, boolean completedAbruptly) {│");
        System.out.println("│      // completedAbruptly=true 表示用户任务抛出异常          │");
        System.out.println("│      if (completedAbruptly)                                 │");
        System.out.println("│          decrementWorkerCount();  // 减少计数               │");
        System.out.println("│                                                             │");
        System.out.println("│      final ReentrantLock mainLock = this.mainLock;          │");
        System.out.println("│      mainLock.lock();                                       │");
        System.out.println("│      try {                                                  │");
        System.out.println("│          completedTaskCount += w.completedTasks;            │");
        System.out.println("│          workers.remove(w);     // 从集合移除               │");
        System.out.println("│      } finally {                                            │");
        System.out.println("│          mainLock.unlock();                                 │");
        System.out.println("│      }                                                      │");
        System.out.println("│                                                             │");
        System.out.println("│      // 尝试终止线程池                                       │");
        System.out.println("│      tryTerminate();                                        │");
        System.out.println("│                                                             │");
        System.out.println("│      // 如果线程池还在运行，且线程数不足，添加新线程          │");
        System.out.println("│      int c = ctl.get();                                     │");
        System.out.println("│      if (runStateLessThan(c, STOP)) {                       │");
        System.out.println("│          if (!completedAbruptly) {                          │");
        System.out.println("│              int min = allowCoreThreadTimeOut ? 0 : corePoolSize;│");
        System.out.println("│              if (min == 0 && !workQueue.isEmpty())          │");
        System.out.println("│                  min = 1;                                   │");
        System.out.println("│              if (workerCountOf(c) >= min)                   │");
        System.out.println("│                  return;  // 不需要补充线程                  │");
        System.out.println("│          }                                                  │");
        System.out.println("│          addWorker(null, false);  // 补充一个线程            │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 5. 关闭流程
     */
    private static void demonstrateShutdown() {
        System.out.println("【5. 线程池关闭流程】\n");

        System.out.println("shutdown() - 优雅关闭：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  public void shutdown() {                                   │");
        System.out.println("│      final ReentrantLock mainLock = this.mainLock;          │");
        System.out.println("│      mainLock.lock();                                       │");
        System.out.println("│      try {                                                  │");
        System.out.println("│          checkShutdownAccess();                             │");
        System.out.println("│          advanceRunState(SHUTDOWN);  // 状态改为 SHUTDOWN   │");
        System.out.println("│          interruptIdleWorkers();  // 中断空闲线程            │");
        System.out.println("│          onShutdown();  // 钩子方法                          │");
        System.out.println("│      } finally {                                            │");
        System.out.println("│          mainLock.unlock();                                 │");
        System.out.println("│      }                                                      │");
        System.out.println("│      tryTerminate();                                        │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("shutdownNow() - 立即关闭：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  public List<Runnable> shutdownNow() {                      │");
        System.out.println("│      // 状态改为 STOP                                       │");
        System.out.println("│      // 中断所有线程（包括正在运行的）                       │");
        System.out.println("│      // 返回队列中未执行的任务列表                           │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("tryTerminate() - 尝试终止：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  final void tryTerminate() {                                │");
        System.out.println("│      for (;;) {                                             │");
        System.out.println("│          int c = ctl.get();                                 │");
        System.out.println("│          // 如果还在运行，或正在清理，不终止                 │");
        System.out.println("│          if (isRunning(c) ||                                │");
        System.out.println("│              runStateAtLeast(c, TIDYING) ||                 │");
        System.out.println("│              (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty()))│");
        System.out.println("│              return;                                        │");
        System.out.println("│                                                             │");
        System.out.println("│          if (workerCountOf(c) != 0) {                       │");
        System.out.println("│              interruptIdleWorkers();  // 中断一个空闲线程    │");
        System.out.println("│              return;                                        │");
        System.out.println("│          }                                                  │");
        System.out.println("│                                                             │");
        System.out.println("│          // 状态改为 TIDYING，执行 terminated() 钩子         │");
        System.out.println("│          // 最后改为 TERMINATED，唤醒等待终止的线程          │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * 6. 线程池监控与调优
     */
    private static void demonstrateMonitoring() {
        System.out.println("【6. 线程池监控与调优】\n");

        System.out.println("线程池监控指标：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  getPoolSize()          // 当前线程数                       │");
        System.out.println("│  getActiveCount()       // 活跃线程数（正在执行任务）       │");
        System.out.println("│  getQueue().size()      // 队列中等待的任务数               │");
        System.out.println("│  getCompletedTaskCount()// 已完成任务总数                   │");
        System.out.println("│  getTaskCount()         // 提交任务总数                     │");
        System.out.println("│  getLargestPoolSize()   // 历史最大线程数                   │");
        System.out.println("│  getRejectedExecutionHandler() // 拒绝策略                  │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("线程池调优建议：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. 核心线程数设置                                           │");
        System.out.println("│     - CPU 密集型：NCPU + 1                                   │");
        System.out.println("│     - IO 密集型：2 * NCPU 或更大                             │");
        System.out.println("│     - 混合任务：根据实际压测调整                             │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  2. 队列选择                                                 │");
        System.out.println("│     - 有界队列：防止 OOM，推荐 ArrayBlockingQueue            │");
        System.out.println("│     - 无界队列：小心内存溢出，LinkedBlockingQueue            │");
        System.out.println("│     - 同步移交：SynchronousQueue，直接交给线程执行           │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  3. 拒绝策略                                                 │");
        System.out.println("│     - CallerRunsPolicy：由调用线程执行，提供降级保护         │");
        System.out.println("│     - DiscardPolicy：静默丢弃，用于非关键任务                │");
        System.out.println("│     - 自定义：记录日志、发送告警                             │");
        System.out.println("├─────────────────────────────────────────────────────────────┤");
        System.out.println("│  4. 优雅关闭                                                 │");
        System.out.println("│     - shutdown() + awaitTermination()                        │");
        System.out.println("│     - 配合 shutdownHook 在 JVM 关闭时执行                    │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("线程池使用最佳实践：");
        System.out.println("1. 使用 ThreadPoolExecutor 手动创建，不要用 Executors");
        System.out.println("2. 自定义线程工厂，设置有意义的线程名");
        System.out.println("3. 使用有界队列，设置合理的队列大小");
        System.out.println("4. 配置拒绝策略，不要静默丢弃任务");
        System.out.println("5. 配置 ThreadPoolExecutor 的钩子方法（before/afterExecute）");
        System.out.println("6. 监控线程池运行状态，动态调整参数");
        System.out.println("7. 线程池不用时要 shutdown，避免内存泄漏\n");
    }
}
