package cn.itzixiao.interview.concurrency;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;

/**
 * AQS (AbstractQueuedSynchronizer) 源码详解
 *
 * AQS 是 JUC 包的核心框架，基于它实现了：
 * - ReentrantLock、ReentrantReadWriteLock
 * - CountDownLatch、CyclicBarrier、Semaphore
 * - ThreadPoolExecutor 中的 Worker
 *
 * AQS 核心思想：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. state 变量：表示同步状态（volatile int）                  │
 * │     - ReentrantLock：0=未锁定，>0=重入次数                   │
 * │     - CountDownLatch：剩余计数                               │
 * │     - Semaphore：剩余许可数                                  │
 * ├─────────────────────────────────────────────────────────────┤
 * │  2. FIFO 队列：等待获取同步状态的线程队列（CLH 变体）          │
 * │     - Node 节点组成的双向链表                                │
 * │     - head：持有锁的节点（虚节点）                           │
 * │     - tail：等待队列尾部                                     │
 * ├─────────────────────────────────────────────────────────────┤
 * │  3. CAS 操作：保证状态修改和队列操作的原子性                   │
 * │     - compareAndSetState()                                   │
 * │     - compareAndSetHead()                                    │
 * │     - compareAndSetTail()                                    │
 * └─────────────────────────────────────────────────────────────┘
 */
public class AQSDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== AQS (AbstractQueuedSynchronizer) 详解 ==========\n");

        // 1. AQS 核心结构
        demonstrateAQSStructure();

        // 2. 独占锁获取与释放
        demonstrateExclusiveLock();

        // 3. 共享锁获取与释放
        demonstrateSharedLock();

        // 4. 条件队列
        demonstrateCondition();

        // 5. 自定义 AQS 实现
        demonstrateCustomAQS();
    }

    /**
     * 1. AQS 核心结构
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

        System.out.println("同步队列结构示意：");
        System.out.println("┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐");
        System.out.println("│   head   │←───→│  Node1   │←───→│  Node2   │←───→│  tail    │");
        System.out.println("│ (持有锁)  │     │(等待中)   │     │(等待中)   │     │ (等待中)  │");
        System.out.println("│waitStatus│     │waitStatus│     │waitStatus│     │waitStatus│");
        System.out.println("│  = -1    │     │  = -1    │     │  = 0     │     │  = 0     │");
        System.out.println("└──────────┘     └──────────┘     └──────────┘     └──────────┘");
        System.out.println("     ↑                                                ↑");
        System.out.println("  虚节点（不绑定线程）                          实际等待线程\n");
    }

    /**
     * 2. 独占锁获取与释放
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
    }

    /**
     * 3. 共享锁获取与释放
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
    }

    /**
     * 4. 条件队列
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
    }

    /**
     * 5. 自定义 AQS 实现
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

        Thread t1 = new Thread(task, "Thread-1");
        Thread t2 = new Thread(task, "Thread-2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("\n自定义锁测试完成！\n");
    }
}

/**
 * 基于 AQS 的自定义互斥锁
 */
class Mutex implements Lock {
    private final Sync sync = new Sync();

    private static class Sync extends AbstractQueuedSynchronizer {
        @Override
        protected boolean isHeldExclusively() {
            return getState() == 1;
        }

        @Override
        public boolean tryAcquire(int acquires) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int releases) {
            if (getState() == 0) {
                throw new IllegalMonitorStateException();
            }
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        Condition newCondition() {
            return new ConditionObject();
        }
    }

    @Override
    public void lock() {
        sync.acquire(1);
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    @Override
    public boolean tryLock() {
        return sync.tryAcquire(1);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(time));
    }

    @Override
    public void unlock() {
        sync.release(1);
    }

    @Override
    public Condition newCondition() {
        return sync.newCondition();
    }
}
