package cn.itzixiao.interview.concurrency;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * AQS (AbstractQueuedSynchronizer) 源码详解与原理剖析
 * <p>
 * =====================================================================================
 * 一、AQS 是什么？
 * =====================================================================================
 * AQS 全称 AbstractQueuedSynchronizer（抽象队列同步器），是 Java 并发包 (JUC) 的核心基石。
 * 它是一个用于构建锁和同步器的框架，采用「模板方法设计模式」。
 * <p>
 * 基于 AQS 实现的同步器：
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │  同步器              │  模式       │  state 含义              │  典型用途      │
 * ├─────────────────────┼─────────────┼──────────────────────────┼────────────────┤
 * │  ReentrantLock      │  独占       │  0=未锁，>0=重入次数     │  互斥锁        │
 * │  ReentrantReadWriteLock │ 独占+共享 │  高16位=读锁，低16位=写锁 │  读写锁      │
 * │  CountDownLatch     │  共享       │  剩余计数值              │  倒计时门闩    │
 * │  Semaphore          │  共享       │  剩余许可数              │  信号量        │
 * │  ThreadPoolExecutor.Worker │ 独占 │  0=空闲，1=运行中       │  线程池工作线程│
 * └─────────────────────────────────────────────────────────────────────────────────┘
 * <p>
 * =====================================================================================
 * 二、AQS 核心设计思想（三位一体）
 * =====================================================================================
 * <p>
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │  【核心组件 1】volatile int state - 同步状态变量                                 │
 * ├─────────────────────────────────────────────────────────────────────────────────┤
 * │  • 使用 volatile 修饰，保证多线程可见性                                          │
 * │  • 通过 CAS 操作修改，保证原子性                                                │
 * │  • 不同同步器赋予不同语义：                                                      │
 * │    - ReentrantLock：0 表示未锁定，>0 表示持有锁及重入次数                        │
 * │    - CountDownLatch：表示剩余需要等待的计数                                     │
 * │    - Semaphore：表示剩余可用许可数                                              │
 * ├─────────────────────────────────────────────────────────────────────────────────┤
 * │  【核心组件 2】CLH 队列变体 - 等待线程队列                                        │
 * ├─────────────────────────────────────────────────────────────────────────────────┤
 * │  • 基于 CLH (Craig, Landin, and Hagersten) 锁队列的改进版本                     │
 * │  • 双向链表结构，FIFO（先进先出）原则                                           │
 * │  • head：虚拟头节点（不存储线程），表示当前持有锁的节点                           │
 * │  • tail：队列尾节点，新节点通过 CAS 添加到尾部                                   │
 * │  • 每个节点包含：waitStatus（等待状态）、prev/next（前后指针）、thread（线程）   │
 * ├─────────────────────────────────────────────────────────────────────────────────┤
 * │  【核心组件 3】CAS 操作 - 原子性保证                                              │
 * ├─────────────────────────────────────────────────────────────────────────────────┤
 * │  • compareAndSetState()：原子修改 state 值                                      │
 * │  • compareAndSetHead()：原子修改头节点                                          │
 * │  • compareAndSetTail()：原子修改尾节点                                          │
 * │  • 底层依赖 Unsafe 类的 compareAndSwapInt 方法                                  │
 * └─────────────────────────────────────────────────────────────────────────────────┘
 * <p>
 * =====================================================================================
 * 三、设计模式 - 模板方法模式
 * =====================================================================================
 * AQS 定义了获取/释放锁的骨架流程，子类只需实现具体的「尝试获取」和「尝试释放」方法：
 * <p>
 * 子类需要重写的方法（根据模式选择）：
 * ┌───────────────────────────────────────────────────────────────────────────────────┐
 * │  方法名                    │  模式   │  说明                                        │
 * ├────────────────────────────┼─────────┼──────────────────────────────────────────────┤
 * │  tryAcquire(int arg)       │  独占   │  尝试获取独占锁，返回 true 表示成功          │
 * │  tryRelease(int arg)       │  独占   │  尝试释放独占锁，返回 true 表示完全释放      │
 * │  tryAcquireShared(int arg) │  共享   │  尝试获取共享锁，返回 >=0 表示成功           │
 * │  tryReleaseShared(int arg) │  共享   │  尝试释放共享锁，返回 true 表示可唤醒后继    │
 * │  isHeldExclusively()       │  独占   │  判断当前线程是否独占持有锁                  │
 * └───────────────────────────────────────────────────────────────────────────────────┘
 */
public class AQSDemo {

    /**
     * 主入口方法 - 演示 AQS 的各个方面
     * <p>
     * 执行流程：
     * 1. demonstrateAQSStructure() - 展示 AQS 核心数据结构
     * 2. demonstrateExclusiveLock() - 演示独占锁获取/释放流程
     * 3. demonstrateSharedLock() - 演示共享锁获取/释放流程
     * 4. demonstrateCondition() - 演示条件变量的实现原理
     * 5. demonstrateCustomAQS() - 演示如何基于 AQS 实现自定义锁
     */
    public static void main(String[] args) throws Exception {
        System.out.println("========== AQS (AbstractQueuedSynchronizer) 详解 ==========\n");

        // 1. AQS 核心结构 - 理解 state、队列、Node 的设计
        demonstrateAQSStructure();

        // 2. 独占锁获取与释放 - ReentrantLock 的实现原理
        demonstrateExclusiveLock();

        // 3. 共享锁获取与释放 - Semaphore、CountDownLatch 的实现原理
        demonstrateSharedLock();

        // 4. 条件队列 - Condition 的实现原理
        demonstrateCondition();

        // 5. 自定义 AQS 实现 - 实现一个简单的互斥锁
        demonstrateCustomAQS();
    }

    /**
     * =================================================================================
     * 1. AQS 核心结构详解
     * =================================================================================
     * <p>
     * AQS 的核心由三个部分组成：
     * 1. state 变量 - 使用 volatile 修饰，表示同步状态
     * 2. CLH 队列 - 双向链表，存储等待获取锁的线程
     * 3. Node 节点 - 队列中的元素，包含线程引用和等待状态
     * <p>
     * 关键理解点：
     * - head 节点是「虚节点」，不绑定线程，代表当前持有锁的位置
     * - tail 节点是队列尾部，新节点总是添加到尾部
     * - waitStatus 表示节点的等待状态，决定了线程的后续行为
     */
    private static void demonstrateAQSStructure() {
        System.out.println("【1. AQS 核心结构】\n");

        System.out.println("AQS 类结构：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  AbstractQueuedSynchronizer                                 │");
        System.out.println("│  ├── volatile int state          // 同步状态               │");
        System.out.println("│  ├── volatile Node head          // 队列头节点             │");
        System.out.println("│  ├── volatile Node tail          // 队列尾节点             │");
        System.out.println("│  ├── transient Thread exclusiveOwnerThread  // 独占线程    │");
        System.out.println("│  │                                                          │");
        System.out.println("│  ├── acquire(int arg)            // 获取独占锁             │");
        System.out.println("│  ├── release(int arg)            // 释放独占锁             │");
        System.out.println("│  ├── acquireShared(int arg)      // 获取共享锁             │");
        System.out.println("│  ├── releaseShared(int arg)      // 释放共享锁             │");
        System.out.println("│  │                                                          │");
        System.out.println("│  └── 子类实现的方法：                                        │");
        System.out.println("│      tryAcquire(int arg)         // 尝试获取独占锁         │");
        System.out.println("│      tryRelease(int arg)         // 尝试释放独占锁         │");
        System.out.println("│      tryAcquireShared(int arg)   // 尝试获取共享锁         │");
        System.out.println("│      tryReleaseShared(int arg)   // 尝试释放共享锁         │");
        System.out.println("│      isHeldExclusively()         // 是否独占模式           │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("Node 节点结构：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  static final class Node {                                  │");
        System.out.println("│      volatile int waitStatus     // 等待状态               │");
        System.out.println("│      // CANCELLED = 1            // 取消                   │");
        System.out.println("│      // SIGNAL = -1              // 后继节点需要唤醒       │");
        System.out.println("│      // CONDITION = -2           // 在条件队列中等待       │");
        System.out.println("│      // PROPAGATE = -3           // 共享模式传播           │");
        System.out.println("│                                                             │");
        System.out.println("│      volatile Node prev          // 前驱节点               │");
        System.out.println("│      volatile Node next          // 后继节点               │");
        System.out.println("│      volatile Thread thread      // 绑定的线程             │");
        System.out.println("│      Node nextWaiter             // 条件队列中的下一个节点 │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        /*
         * waitStatus 状态详解：
         * ─────────────────────────────────────────────────────────────
         * | 状态值    | 常量名      | 含义                              |
         * |-----------|-------------|-----------------------------------|
         * | 0         | 初始状态    | 节点刚初始化，还没有进入等待状态  |
         * | -1 (SIGNAL) | 待唤醒    | 后继节点需要被唤醒（当前节点释放锁后会唤醒后继）|
         * | 1 (CANCELLED) | 已取消  | 线程因超时或中断被取消，应从队列移除 |
         * | -2 (CONDITION) | 条件等待 | 节点在条件队列中，等待 signal 唤醒 |
         * | -3 (PROPAGATE) | 传播    | 共享模式下，状态需要向后传播      |
         * ─────────────────────────────────────────────────────────────
         */

        System.out.println("同步队列结构示意：");
        System.out.println("┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐");
        System.out.println("│   head   │←───→│  Node1   │←───→│  Node2   │←───→│  tail    │");
        System.out.println("│ (持有锁)  │     │(等待中)   │     │(等待中)   │     │ (等待中)  │");
        System.out.println("│waitStatus│     │waitStatus│     │waitStatus│     │waitStatus│");
        System.out.println("│  = -1    │     │  = -1    │     │  = 0     │     │  = 0     │");
        System.out.println("└──────────┘     └──────────┘     └──────────┘     └──────────┘");
        System.out.println("     ↑                                                ↑");
        System.out.println("  虚节点（不绑定线程）                          实际等待线程\n");

        System.out.println("【关键理解点】：");
        System.out.println("  1. head 节点是虚拟头节点，不存储实际线程，代表\"当前持有锁\"的位置");
        System.out.println("  2. 队列中只有 head 的后继节点才有资格尝试获取锁");
        System.out.println("  3. 每个节点的 waitStatus 表示该节点后继节点的状态，而不是自己的状态");
        System.out.println("  4. 当一个节点的 waitStatus = -1 (SIGNAL)，表示它释放锁时需要唤醒后继节点\n");
    }

    /**
     * =================================================================================
     * 2. 独占锁获取与释放流程详解
     * =================================================================================
     * <p>
     * 独占锁（Exclusive Lock）：同一时刻只有一个线程能持有锁
     * 典型应用：ReentrantLock
     * <p>
     * 核心流程图解：
     * <p>
     * 获取锁流程（acquire）：
     * ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
     * │ tryAcquire  │────→│  addWaiter  │────→│acquireQueued│────→│  阻塞等待   │
     * │ (尝试获取)  │ NO  │ (加入队列)  │     │ (自旋获取)  │     │ (park阻塞) │
     * └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
     * │ YES
     * ↓
     * [获取成功返回]
     * <p>
     * 释放锁流程（release）：
     * ┌─────────────┐     ┌──────────────────┐
     * │ tryRelease  │────→│unparkSuccessor   │
     * │ (尝试释放)  │     │(唤醒后继节点)    │
     * └─────────────┘     └──────────────────┘
     */
    private static void demonstrateExclusiveLock() {
        System.out.println("【2. 独占锁获取与释放流程】\n");

        System.out.println("acquire(int arg) 获取独占锁：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. tryAcquire(arg)                                         │");
        System.out.println("│     → 子类实现，尝试获取锁                                   │");
        System.out.println("│     → 成功：返回 true，获取锁成功                            │");
        System.out.println("│     → 失败：继续下一步                                       │");
        System.out.println("│                                                             │");
        System.out.println("│  2. addWaiter(Node.EXCLUSIVE)                               │");
        System.out.println("│     → 创建当前线程的 Node 节点                              │");
        System.out.println("│     → CAS 添加到队列尾部                                     │");
        System.out.println("│                                                             │");
        System.out.println("│  3. acquireQueued(node, arg)                                │");
        System.out.println("│     → 自旋 + 阻塞获取锁                                      │");
        System.out.println("│     → 找到前驱节点，如果前驱是 head，尝试获取锁              │");
        System.out.println("│     → 获取失败：将前驱 waitStatus 设为 -1（SIGNAL）          │");
        System.out.println("│     → 调用 LockSupport.park() 阻塞当前线程                   │");
        System.out.println("│                                                             │");
        System.out.println("│  4. 被唤醒后继续自旋，直到获取锁成功                         │");
        System.out.println("│     → 返回中断状态（是否被中断过）                           │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("release(int arg) 释放独占锁：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. tryRelease(arg)                                         │");
        System.out.println("│     → 子类实现，尝试释放锁                                   │");
        System.out.println("│     → 释放成功：返回 true                                    │");
        System.out.println("│                                                             │");
        System.out.println("│  2. unparkSuccessor(head)                                   │");
        System.out.println("│     → 找到 head 的后继节点                                   │");
        System.out.println("│     → 调用 LockSupport.unpark() 唤醒后继线程                 │");
        System.out.println("│                                                             │");
        System.out.println("│  3. 被唤醒的线程从 park 处返回，继续自旋获取锁               │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        /*
         * acquireQueued 核心源码解析：
         *
         * 这个方法是独占锁获取的核心，实现了「自旋 + 阻塞」的机制。
         *
         * 关键点：
         * 1. 只有自己的前驱是 head 时，才有资格尝试获取锁（FIFO 公平性）
         * 2. 获取失败后，将前驱节点的 waitStatus 设为 SIGNAL，表示"我需要被唤醒"
         * 3. 调用 park 阻塞自己，等待前驱节点释放锁时唤醒
         */
        System.out.println("关键代码逻辑：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  // 获取锁失败后的处理                                       │");
        System.out.println("│  final boolean acquireQueued(final Node node, int arg) {    │");
        System.out.println("│      boolean failed = true;                                 │");
        System.out.println("│      try {                                                  │");
        System.out.println("│          boolean interrupted = false;                       │");
        System.out.println("│          for (;;) {              // 自旋                    │");
        System.out.println("│              final Node p = node.predecessor();             │");
        System.out.println("│              if (p == head && tryAcquire(arg)) {            │");
        System.out.println("│                  setHead(node);   // 获取成功，设为头节点    │");
        System.out.println("│                  p.next = null;   // help GC                │");
        System.out.println("│                  failed = false;                            │");
        System.out.println("│                  return interrupted;                        │");
        System.out.println("│              }                                              │");
        System.out.println("│              if (shouldParkAfterFailedAcquire(p, node) &&   │");
        System.out.println("│                  parkAndCheckInterrupt())                   │");
        System.out.println("│                  interrupted = true;                        │");
        System.out.println("│          }                                                  │");
        System.out.println("│      } finally {                                            │");
        System.out.println("│          if (failed)                                        │");
        System.out.println("│              cancelAcquire(node);  // 取消获取               │");
        System.out.println("│      }                                                      │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("【核心流程解析】：");
        System.out.println("  acquireQueued 的执行逻辑：");
        System.out.println("  ├─ for(;;) 无限循环（自旋）");
        System.out.println("  ├─ 获取当前节点的前驱节点 p");
        System.out.println("  ├─ if (p == head && tryAcquire(arg))");
        System.out.println("  │     → 前驱是 head，说明轮到我了，尝试获取锁");
        System.out.println("  │     → 获取成功：把自己设为新的 head，清空 thread 引用");
        System.out.println("  │     → 返回中断状态");
        System.out.println("  ├─ shouldParkAfterFailedAcquire(p, node)");
        System.out.println("  │     → 检查前驱节点的 waitStatus");
        System.out.println("  │     → 如果是 SIGNAL(-1)，返回 true，可以阻塞");
        System.out.println("  │     → 如果 >0 (CANCELLED)，跳过取消的节点");
        System.out.println("  │     → 如果是 0，CAS 设为 SIGNAL，下次循环再检查");
        System.out.println("  └─ parkAndCheckInterrupt()");
        System.out.println("        → 调用 LockSupport.park() 阻塞当前线程");
        System.out.println("        → 返回是否被中断唤醒\n");
    }

    /**
     * =================================================================================
     * 3. 共享锁获取与释放流程详解
     * =================================================================================
     * <p>
     * 共享锁（Shared Lock）：同一时刻多个线程可以同时持有锁
     * 典型应用：Semaphore（信号量）、CountDownLatch（倒计时门闩）
     * <p>
     * 与独占锁的区别：
     * ┌──────────────────────────────────────────────────────────────────────────────┐
     * │  特性            │  独占锁                │  共享锁                      │
     * ├──────────────────┼────────────────────────┼──────────────────────────────┤
     * │  持有者数量      │  只能 1 个线程         │  可以多个线程同时持有        │
     * │  state 含义      │  0/1 或重入次数        │  剩余许可/计数               │
     * │  获取成功返回    │  boolean               │  int (>=0 成功, <0 失败)     │
     * │  唤醒后继        │  只唤醒 1 个           │  可能传播唤醒多个            │
     * └──────────────────────────────────────────────────────────────────────────────┘
     */
    private static void demonstrateSharedLock() {
        System.out.println("【3. 共享锁获取与释放流程】\n");

        System.out.println("共享锁特点：");
        System.out.println("- 多个线程可以同时持有锁");
        System.out.println("- 用于 Semaphore、CountDownLatch 等\n");

        System.out.println("acquireShared(int arg) 获取共享锁：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  1. tryAcquireShared(arg)                                   │");
        System.out.println("│     → 返回值 >= 0：获取成功                                  │");
        System.out.println("│     → 返回值 < 0：获取失败，需要等待                         │");
        System.out.println("│                                                             │");
        System.out.println("│  2. doAcquireShared(arg)                                    │");
        System.out.println("│     → 类似独占锁，加入队列等待                               │");
        System.out.println("│     → 被唤醒后，如果获取成功，会传播给后继节点               │");
        System.out.println("│       （setHeadAndPropagate）                                │");
        System.out.println("│     → 让其他等待的共享锁线程也尝试获取                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        /*
         * 共享锁的传播机制（Propagation）是核心：
         *
         * 当一个共享锁获取成功后，如果还有剩余资源，需要唤醒后继的共享锁节点。
         * 这就是「传播」的含义——唤醒操作会像多米诺骨牌一样向后传递。
         *
         * setHeadAndPropagate() 方法实现了这个机制：
         * 1. 将当前节点设为 head
         * 2. 检查是否还有剩余资源
         * 3. 如果有，调用 doReleaseShared() 唤醒后继
         */
        System.out.println("共享锁传播机制：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  场景：Semaphore 初始许可为 3，线程 A、B、C 都等待获取        │");
        System.out.println("│                                                             │");
        System.out.println("│  1. 线程 A 释放 1 个许可                                     │");
        System.out.println("│  2. 唤醒队列中的线程 B                                       │");
        System.out.println("│  3. 线程 B 获取许可成功，检查后继节点 C 也是共享模式           │");
        System.out.println("│  4. 调用 doReleaseShared() 继续唤醒 C                        │");
        System.out.println("│  5. 形成传播链，提高并发性能                                  │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("【共享锁 vs 独占锁对比】：");
        System.out.println("┌───────────────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  操作           │  独占锁                           │  共享锁                  │");
        System.out.println("├─────────────────┼───────────────────────────────────┼──────────────────────────┤");
        System.out.println("│  获取入口       │  acquire()                        │  acquireShared()        │");
        System.out.println("│  尝试获取       │  tryAcquire() → boolean           │  tryAcquireShared() → int│");
        System.out.println("│  入队等待       │  addWaiter(EXCLUSIVE)             │  addWaiter(SHARED)       │");
        System.out.println("│  自旋获取       │  acquireQueued()                  │  doAcquireShared()      │");
        System.out.println("│  释放入口       │  release()                        │  releaseShared()        │");
        System.out.println("│  尝试释放       │  tryRelease() → boolean           │  tryReleaseShared() → bool│");
        System.out.println("│  唤醒后继       │  unparkSuccessor()                │  doReleaseShared()      │");
        System.out.println("│  唤醒数量       │  只唤醒 1 个                      │  可能传播唤醒多个        │");
        System.out.println("└───────────────────────────────────────────────────────────────────────────────┘\n");
    }

    /**
     * =================================================================================
     * 4. 条件队列（Condition）详解
     * =================================================================================
     * <p>
     * Condition 是 AQS 提供的条件变量实现，用于替代传统的 Object.wait/notify。
     * 它允许线程在某些条件不满足时等待，条件满足时被唤醒。
     * <p>
     * 核心概念：
     * 1. 同步队列（Sync Queue）：等待获取锁的线程队列
     * 2. 条件队列（Condition Queue）：调用了 await 的线程队列
     * <p>
     * 一个 Lock 可以创建多个 Condition，每个 Condition 维护一个独立的条件队列。
     */
    private static void demonstrateCondition() {
        System.out.println("【4. 条件队列（Condition）】\n");

        System.out.println("Condition 与 Object.wait/notify 对比：");
        System.out.println("┌─────────────────┬─────────────────────┬─────────────────────┐");
        System.out.println("│   特性          │ Object wait/notify  │ Condition           │");
        System.out.println("├─────────────────┼─────────────────────┼─────────────────────┤");
        System.out.println("│  等待队列数量   │  1个（每个对象1个）  │  多个（每个Lock多个）│");
        System.out.println("│  唤醒方式       │  随机唤醒或全部唤醒  │  精确唤醒（signal）  │");
        System.out.println("│  使用方式       │  配合 synchronized   │  配合 Lock          │");
        System.out.println("│  中断响应       │  不响应中断          │  可响应中断          │");
        System.out.println("│  超时等待       │  不支持              │  支持                │");
        System.out.println("└─────────────────┴─────────────────────┴─────────────────────┘\n");

        /*
         * Condition 的核心实现原理：
         *
         * AQS 内部维护了两种队列：
         * 1. 同步队列：双向链表，存储等待获取锁的线程
         * 2. 条件队列：单向链表，存储等待条件的线程（通过 nextWaiter 连接）
         *
         * await() 操作：
         * - 将当前线程封装成 Node，加入条件队列
         * - 完全释放锁（保存锁状态，以便后续恢复）
         * - 阻塞等待，直到被 signal 或中断
         * - 被唤醒后，从条件队列转移到同步队列
         * - 在同步队列中竞争锁
         *
         * signal() 操作：
         * - 从条件队列头部取出一个节点
         * - 将该节点转移到同步队列尾部
         * - 唤醒该节点，让它去竞争锁
         */
        System.out.println("Condition 实现原理：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  AQS 维护两个队列：                                          │");
        System.out.println("│  1. 同步队列（Sync Queue）：获取锁失败的线程                  │");
        System.out.println("│  2. 条件队列（Condition Queue）：调用 await 的线程            │");
        System.out.println("│                                                             │");
        System.out.println("│  await() 流程：                                              │");
        System.out.println("│  1. 将当前线程加入条件队列（Node.CONDITION）                 │");
        System.out.println("│  2. 完全释放锁（saveState + release）                        │");
        System.out.println("│  3. 阻塞等待，直到被 signal 或中断                           │");
        System.out.println("│  4. 被唤醒后，从条件队列转移到同步队列                       │");
        System.out.println("│  5. 在同步队列中竞争锁                                       │");
        System.out.println("│  6. 获取锁后，检查中断状态                                   │");
        System.out.println("│                                                             │");
        System.out.println("│  signal() 流程：                                             │");
        System.out.println("│  1. 检查当前线程是否持有锁                                   │");
        System.out.println("│  2. 从条件队列头部取出一个节点                               │");
        System.out.println("│  3. 将节点转移到同步队列（enq）                              │");
        System.out.println("│  4. 如果前驱节点是取消状态，唤醒该节点                       │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("队列结构示意：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  同步队列（Sync Queue）        条件队列（Condition Queue）   │");
        System.out.println("│  ┌───┐    ┌───┐    ┌───┐      ┌───┐    ┌───┐    ┌───┐      │");
        System.out.println("│  │head│←──→│ T2│←──→│ T3│      │first│←──→│ T4│←──→│ T5│      │");
        System.out.println("│  └───┘    └───┘    └───┘      └───┘    └───┘    └───┘      │");
        System.out.println("│   ↑（持有锁）                                                │");
        System.out.println("│  T1 调用 await() → 进入条件队列，释放锁 → T2 获取锁          │");
        System.out.println("│  T2 调用 signal() → T1 从条件队列转移到同步队列              │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        System.out.println("【典型应用场景】：");
        System.out.println("  1. 生产者-消费者模型");
        System.out.println("     - 生产者：队列满时调用 notFull.await() 等待");
        System.out.println("     - 消费者：消费后调用 notEmpty.signal() 唤醒生产者");
        System.out.println("  2. 阻塞队列（ArrayBlockingQueue）");
        System.out.println("     - 使用两个 Condition：notEmpty 和 notFull");
        System.out.println("     - 分别控制\"队列非空\"和\"队列非满\"条件");
        System.out.println("  3. 线程池任务执行");
        System.out.println("     - 工作线程从队列获取任务时，队列为空则等待\n");
    }

    /**
     * =================================================================================
     * 5. 自定义 AQS 实现 - 手写一个不可重入独占锁
     * =================================================================================
     * <p>
     * 通过实现一个简单的 Mutex（互斥锁），深入理解 AQS 的使用方式。
     * <p>
     * 实现步骤：
     * 1. 创建内部类 Sync 继承 AbstractQueuedSynchronizer
     * 2. 重写 tryAcquire() - 尝试获取锁（CAS 修改 state 从 0 到 1）
     * 3. 重写 tryRelease() - 尝试释放锁（将 state 设为 0）
     * 4. 重写 isHeldExclusively() - 判断是否独占持有
     * 5. 外部类实现 Lock 接口，委托给 Sync 执行
     * <p>
     * 这是一个不可重入锁（Non-reentrant Lock）：
     * - 同一线程不能重复获取锁，否则会死锁
     * - ReentrantLock 是可重入的，因为 state 会累加计数
     */
    private static void demonstrateCustomAQS() throws Exception {
        System.out.println("【5. 自定义 AQS 实现 - 不可重入独占锁】\n");

        System.out.println("实现一个简单的 Mutex（互斥锁）：");
        System.out.println("┌─────────────────────────────────────────────────────────────┐");
        System.out.println("│  public class Mutex implements Lock {                       │");
        System.out.println("│      private final Sync sync = new Sync();                  │");
        System.out.println("│                                                             │");
        System.out.println("│      private static class Sync extends AbstractQueuedSynchronizer │");
        System.out.println("│      {                                                      │");
        System.out.println("│          // 是否被占用                                       │");
        System.out.println("│          protected boolean isHeldExclusively() {            │");
        System.out.println("│              return getState() == 1;                        │");
        System.out.println("│          }                                                  │");
        System.out.println("│                                                             │");
        System.out.println("│          // 尝试获取锁                                       │");
        System.out.println("│          public boolean tryAcquire(int acquires) {          │");
        System.out.println("│              if (compareAndSetState(0, 1)) {                │");
        System.out.println("│                  setExclusiveOwnerThread(Thread.currentThread());│");
        System.out.println("│                  return true;                               │");
        System.out.println("│              }                                              │");
        System.out.println("│              return false;                                  │");
        System.out.println("│          }                                                  │");
        System.out.println("│                                                             │");
        System.out.println("│          // 尝试释放锁                                       │");
        System.out.println("│          protected boolean tryRelease(int releases) {       │");
        System.out.println("│              if (getState() == 0)                           │");
        System.out.println("│                  throw new IllegalMonitorStateException();  │");
        System.out.println("│              setExclusiveOwnerThread(null);                 │");
        System.out.println("│              setState(0);                                   │");
        System.out.println("│              return true;                                   │");
        System.out.println("│          }                                                  │");
        System.out.println("│      }                                                      │");
        System.out.println("│      // ... Lock 接口方法实现                                │");
        System.out.println("│  }                                                          │");
        System.out.println("└─────────────────────────────────────────────────────────────┘\n");

        // 演示自定义锁
        Mutex mutex = new Mutex();
        System.out.println("测试自定义 Mutex 锁：");

        // 创建一个任务：尝试获取锁、持有1秒、释放锁
        Runnable task = () -> {
            System.out.println("  " + Thread.currentThread().getName() + " 尝试获取锁");
            mutex.lock();
            try {
                System.out.println("  " + Thread.currentThread().getName() + " 获取锁成功");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("  " + Thread.currentThread().getName() + " 释放锁");
                mutex.unlock();
            }
        };

        // 创建两个线程竞争锁
        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\n自定义锁测试完成！\n");

        System.out.println("【执行结果分析】：");
        System.out.println("  1. Thread-1 和 Thread-2 同时启动，竞争锁");
        System.out.println("  2. 假设 Thread-1 先获取到锁，Thread-2 进入等待队列");
        System.out.println("  3. Thread-1 持有锁 1 秒后释放，唤醒 Thread-2");
        System.out.println("  4. Thread-2 获取锁，持有 1 秒后释放");
        System.out.println("  5. 整个过程体现了 AQS 的队列管理和唤醒机制\n");
    }
}

/**
 * =================================================================================
 * 基于 AQS 的自定义互斥锁（Mutex）
 * =================================================================================
 * <p>
 * 这是一个简单的不可重入独占锁实现，展示如何使用 AQS 构建自定义同步器。
 * <p>
 * 核心原理：
 * 1. state = 0：锁未被占用
 * 2. state = 1：锁已被占用
 * 3. 获取锁：通过 CAS 将 state 从 0 改为 1
 * 4. 释放锁：将 state 设为 0
 * <p>
 * 与 ReentrantLock 的区别：
 * - ReentrantLock 是可重入的，同一线程可以多次获取锁，state 会累加
 * - 本实现是不可重入的，同一线程重复获取锁会阻塞（死锁）
 * <p>
 * 适用场景：
 * - 学习 AQS 原理
 * - 简单的互斥控制场景
 */
class Mutex implements Lock {

    /**
     * 同步器实例，继承 AQS 实现具体的锁逻辑
     */
    private final Sync sync = new Sync();

    /**
     * 内部同步器类 - AQS 的核心实现
     * <p>
     * 通过继承 AbstractQueuedSynchronizer 并重写以下方法：
     * - tryAcquire()：尝试获取独占锁
     * - tryRelease()：尝试释放独占锁
     * - isHeldExclusively()：判断当前线程是否独占持有锁
     */
    private static class Sync extends AbstractQueuedSynchronizer {

        /**
         * 判断当前线程是否独占持有锁
         *
         * @return true-当前线程持有锁，false-未持有
         * <p>
         * 使用场景：
         * - Condition 实现需要判断当前线程是否持有锁
         * - 用于实现锁的条件等待机制
         */
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        /**
         * 尝试获取独占锁
         *
         * @param acquires 获取参数（通常为 1）
         * @return true-获取成功，false-获取失败
         * <p>
         * 实现逻辑：
         * 1. 使用 compareAndSetState(0, 1) 原子操作尝试获取锁
         * 2. CAS 成功：设置独占线程为当前线程，返回 true
         * 3. CAS 失败：说明锁已被占用，返回 false
         * <p>
         * 注意：
         * - 这是不可重入锁，不会检查当前线程是否已持有锁
         * - 如果已持有锁的线程再次调用，会返回 false 导致阻塞
         */
        @Override
        public boolean tryAcquire(int acquires) {
            // CAS 操作：只有当 state=0 时，才能将其设为 1
            if (compareAndSetState(0, 1)) {
                // 设置当前线程为独占线程（用于 isHeldExclusively 判断）
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * 尝试释放独占锁
         *
         * @param releases 释放参数（通常为 1）
         * @return true-释放成功
         * @throws IllegalMonitorStateException 如果当前线程未持有锁
         *                                      <p>
         *                                      实现逻辑：
         *                                      1. 检查 state 是否为 0（未持有锁就释放是非法操作）
         *                                      2. 清空独占线程引用
         *                                      3. 将 state 设为 0
         *                                      <p>
         *                                      注意：
         *                                      - setState(0) 不需要 CAS，因为只有持有锁的线程才能释放
         */
        @Override
        protected boolean tryRelease(int releases) {
            // 如果 state 已经是 0，说明当前线程未持有锁
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            // 清空独占线程
            setExclusiveOwnerThread(null);
            // 释放锁：state 设为 0
            setState(0);
            return true;
        }

        /**
         * 创建条件变量
         *
         * @return 新的 Condition 实例
         */
        Condition newCondition() {
            return new ConditionObject();
        }
    }

    /**
     * 获取锁（阻塞式）
     * <p>
     * 调用 AQS 的 acquire() 方法：
     * 1. 先调用 tryAcquire() 尝试获取
     * 2. 失败则加入等待队列阻塞等待
     */
    @Override
    public void lock() {
        sync.acquire(1);
    }

    /**
     * 可中断地获取锁
     * <p>
     * 与 lock() 的区别：
     * - 等待过程中如果被中断，会抛出 InterruptedException
     * - lock() 会忽略中断，只是记录中断状态
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 尝试获取锁（非阻塞）
     *
     * @return true-获取成功，false-获取失败（锁被占用）
     * <p>
     * 不会阻塞，立即返回结果
     */
    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    /**
     * 超时尝试获取锁
     *
     * @param time 超时时间
     * @param unit 时间单位
     * @return true-获取成功，false-超时未获取
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    /**
     * 释放锁
     * <p>
     * 调用 AQS 的 release() 方法：
     * 1. 调用 tryRelease() 释放锁
     * 2. 唤醒等待队列中的后继线程
     */
    @Override
    public void unlock() {
        sync.release(1);
    }

    /**
     * 创建条件变量
     *
     * @return 新的 Condition 实例
     */
    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}
