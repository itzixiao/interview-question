package cn.itzixiao.interview.concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * =====================================================================================
 * BlockingQueue 体系详解 —— 阻塞队列各实现类实战演示
 * =====================================================================================
 * <p>
 * 对应文档：docs/02-Java并发编程/01-Java集合框架.md  BlockingQueue 体系章节
 * <p>
 * 演示内容：
 * 1. BlockingQueue 操作方法四类对比（抛异常/返回特殊值/阻塞/超时）
 * 2. ArrayBlockingQueue —— 有界数组、单锁、公平模式
 * 3. LinkedBlockingQueue —— 双锁读写分离、高吞吐量
 * 4. PriorityBlockingQueue —— 无界优先级队列
 * 5. SynchronousQueue —— 零容量直接移交、newCachedThreadPool
 * 6. DelayQueue —— 订单超时取消完整实现
 * 7. LinkedTransferQueue —— transfer 直接移交
 * 8. 各实现类横向对比
 * 9. 高频面试题
 */
public class BlockingQueueDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== BlockingQueue 体系详解 ==========\n");

        // 1. 操作方法四类对比
        demonstrateQueueOperations();

        // 2. ArrayBlockingQueue
        demonstrateArrayBlockingQueue();

        // 3. LinkedBlockingQueue
        demonstrateLinkedBlockingQueue();

        // 4. PriorityBlockingQueue
        demonstratePriorityBlockingQueue();

        // 5. SynchronousQueue
        demonstrateSynchronousQueue();

        // 6. DelayQueue：订单超时取消
        demonstrateDelayQueue();

        // 7. LinkedTransferQueue
        demonstrateLinkedTransferQueue();

        // 8. 高频面试题
        showInterviewQuestions();
    }

    // =========================================================================
    // 1. BlockingQueue 操作方法四类对比
    // =========================================================================
    private static void demonstrateQueueOperations() throws InterruptedException {
        System.out.println("【一、BlockingQueue 四类操作方法对比】\n");

        System.out.println("┌──────────┬───────────┬───────────────┬────────┬────────────────────────┐");
        System.out.println("│ 操作     │ 抛出异常  │ 返回特殊值    │ 阻塞   │ 超时                   │");
        System.out.println("├──────────┼───────────┼───────────────┼────────┼────────────────────────┤");
        System.out.println("│ 入队     │ add(e)    │ offer(e)      │ put(e) │ offer(e,timeout,unit)  │");
        System.out.println("│ 出队     │ remove()  │ poll()        │ take() │ poll(timeout,unit)     │");
        System.out.println("│ 查看队首 │ element() │ peek()        │   -    │        -               │");
        System.out.println("└──────────┴───────────┴───────────────┴────────┴────────────────────────┘\n");

        // 演示 offer 超时
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(2);
        queue.put("A");
        queue.put("B");
        System.out.println("队列已满（容量 2），尝试 offer(C, 500ms)...");
        boolean success = queue.offer("C", 500, TimeUnit.MILLISECONDS);
        System.out.println("offer 超时返回：" + success + "（false 表示超时未入队）\n");
    }

    // =========================================================================
    // 2. ArrayBlockingQueue —— 有界数组、单锁
    // =========================================================================
    private static void demonstrateArrayBlockingQueue() throws InterruptedException {
        System.out.println("【二、ArrayBlockingQueue —— 有界数组、单锁】\n");

        // 容量为 3，非公平锁（默认）
        BlockingQueue<Integer> abq = new ArrayBlockingQueue<>(3);

        System.out.println("特点：");
        System.out.println("  - 必须指定容量，创建后不可更改");
        System.out.println("  - 一把 ReentrantLock（put/take 共用），读写互斥");
        System.out.println("  - 支持公平锁：new ArrayBlockingQueue<>(capacity, true)");
        System.out.println("  - 内存连续，GC 压力小\n");

        // 生产者：put 满时阻塞
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 6; i++) {
                try {
                    abq.put(i);
                    System.out.printf("  [生产者] put(%d) 成功，队列：%s%n", i, abq);
                    Thread.sleep(100);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "ABQ-Producer");

        // 消费者：take 空时阻塞
        Thread consumer = new Thread(() -> {
            for (int i = 1; i <= 6; i++) {
                try {
                    Thread.sleep(300);  // 消费慢于生产，触发 put 阻塞
                    int val = abq.take();
                    System.out.printf("  [消费者] take() = %d，剩余：%s%n", val, abq);
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }, "ABQ-Consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
        System.out.println();
    }

    // =========================================================================
    // 3. LinkedBlockingQueue —— 双锁读写分离
    // =========================================================================
    private static void demonstrateLinkedBlockingQueue() throws InterruptedException {
        System.out.println("【三、LinkedBlockingQueue —— 双锁读写分离，高吞吐】\n");

        // 设置有界容量（生产必须设置，否则默认 Integer.MAX_VALUE 可能 OOM）
        LinkedBlockingQueue<String> lbq = new LinkedBlockingQueue<>(10);

        System.out.println("特点：");
        System.out.println("  - 两把锁：putLock（入队）+ takeLock（出队），读写可并行");
        System.out.println("  - 原子计数 AtomicInteger count，两把锁之间共享");
        System.out.println("  - 吞吐量高于 ArrayBlockingQueue（读写锁分离）");
        System.out.println("  - ⚠️ 默认容量 Integer.MAX_VALUE，生产环境务必指定上限\n");

        // 多生产者多消费者演示
        int producerCount = 3;
        int consumerCount = 2;
        AtomicInteger produced = new AtomicInteger(0);
        AtomicInteger consumed = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(producerCount + consumerCount);

        // 启动生产者
        for (int p = 0; p < producerCount; p++) {
            final int pid = p;
            new Thread(() -> {
                try {
                    for (int i = 0; i < 5; i++) {
                        String item = "P" + pid + "-item" + i;
                        lbq.put(item);
                        produced.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { latch.countDown(); }
            }, "LBQ-Producer-" + p).start();
        }

        // 启动消费者
        for (int c = 0; c < consumerCount; c++) {
            final int cid = c;
            new Thread(() -> {
                try {
                    // 每个消费者消费 7-8 个
                    for (int i = 0; i < (cid == 0 ? 8 : 7); i++) {
                        lbq.poll(2, TimeUnit.SECONDS);
                        consumed.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                finally { latch.countDown(); }
            }, "LBQ-Consumer-" + c).start();
        }

        latch.await(5, TimeUnit.SECONDS);
        System.out.printf("  %d 生产者 × 5 = 生产 %d 条，%d 消费者消费 %d 条%n",
                producerCount, produced.get(), consumerCount, consumed.get());
        System.out.println("  LinkedBlockingQueue 双锁并发验证通过\n");
    }

    // =========================================================================
    // 4. PriorityBlockingQueue —— 无界优先级队列
    // =========================================================================
    private static void demonstratePriorityBlockingQueue() throws InterruptedException {
        System.out.println("【四、PriorityBlockingQueue —— 无界优先级阻塞队列】\n");

        // 按任务优先级排序（数字小优先）
        PriorityBlockingQueue<PriorityTask> pbq = new PriorityBlockingQueue<>(
                11, (a, b) -> a.priority - b.priority
        );

        System.out.println("特点：");
        System.out.println("  - 无界（只在队空时 take 阻塞，put 不会阻塞）");
        System.out.println("  - 内部基于最小堆，线程安全（ReentrantLock）");
        System.out.println("  - 不允许 null 元素\n");

        // 乱序提交任务
        pbq.put(new PriorityTask("日志压缩",   5));
        pbq.put(new PriorityTask("支付回调",   1));   // 最高优先级
        pbq.put(new PriorityTask("积分结算",   3));
        pbq.put(new PriorityTask("余额变更",   2));
        pbq.put(new PriorityTask("报表统计",   4));

        System.out.println("按优先级出队：");
        while (!pbq.isEmpty()) {
            PriorityTask t = pbq.take();
            System.out.printf("  [P%d] 执行任务：%s%n", t.priority, t.name);
        }
        System.out.println();
    }

    // =========================================================================
    // 5. SynchronousQueue —— 零容量直接移交
    // =========================================================================
    private static void demonstrateSynchronousQueue() throws InterruptedException {
        System.out.println("【五、SynchronousQueue —— 零容量、线程间直接移交】\n");

        System.out.println("特点：");
        System.out.println("  - 容量为 0，不存储任何元素");
        System.out.println("  - put 必须等 take，take 必须等 put（配对移交）");
        System.out.println("  - 非公平（默认）TransferStack：LIFO");
        System.out.println("  - 公平 TransferQueue：FIFO");
        System.out.println("  - newCachedThreadPool() 内部使用此队列\n");

        SynchronousQueue<String> sq = new SynchronousQueue<>();

        // 消费者先启动等待（模拟线程池工作线程等待任务）
        Thread consumer = new Thread(() -> {
            try {
                System.out.println("  [消费者] 等待接收...");
                String data = sq.take();    // 阻塞直到有生产者 put
                System.out.println("  [消费者] 接收到：" + data);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "SQ-Consumer");
        consumer.start();

        Thread.sleep(200);  // 确保消费者先进入等待

        Thread producer = new Thread(() -> {
            try {
                System.out.println("  [生产者] 准备发送数据...");
                sq.put("重要消息");       // 立即与等待的消费者握手
                System.out.println("  [生产者] 数据已移交给消费者");
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }, "SQ-Producer");
        producer.start();

        producer.join();
        consumer.join();

        // newCachedThreadPool 分析
        System.out.println("\nnewCachedThreadPool 使用 SynchronousQueue 的原因：");
        System.out.println("  corePoolSize=0，maxPoolSize=Integer.MAX_VALUE");
        System.out.println("  任务提交时：SynchronousQueue 无法入队 → 直接创建新线程");
        System.out.println("  无任务时：线程空闲 60s 后自动销毁");
        System.out.println("  ⚠️ 生产慎用：任务暴增时创建大量线程可能导致 OOM\n");
    }

    // =========================================================================
    // 6. DelayQueue —— 订单超时取消
    // =========================================================================
    private static void demonstrateDelayQueue() throws InterruptedException {
        System.out.println("【六、DelayQueue —— 订单超时取消实战】\n");

        System.out.println("特点：");
        System.out.println("  - 无界延迟队列，元素需实现 Delayed 接口");
        System.out.println("  - 只有延迟时间到期的元素才能被 take/poll");
        System.out.println("  - 内部基于 PriorityQueue（按到期时间排序）");
        System.out.println("  - ⚠️ 大规模场景建议用 Redis ZSet 实现（支持分布式）\n");

        DelayQueue<OrderDelayTask> delayQueue = new DelayQueue<>();

        // 模拟下单，设置不同的超时时间
        System.out.println("模拟下单（超时时间不同）：");
        delayQueue.put(new OrderDelayTask("ORDER-001", 2000));  // 2秒超时
        delayQueue.put(new OrderDelayTask("ORDER-002", 1000));  // 1秒超时（最先到期）
        delayQueue.put(new OrderDelayTask("ORDER-003", 3000));  // 3秒超时
        System.out.println("  ORDER-001 下单，超时 2s");
        System.out.println("  ORDER-002 下单，超时 1s");
        System.out.println("  ORDER-003 下单，超时 3s");

        // 消费线程：模拟超时取消处理器
        Thread cancelHandler = new Thread(() -> {
            System.out.println("\n[超时取消处理器] 启动，等待订单超时...");
            int count = 0;
            while (count < 3) {
                try {
                    OrderDelayTask task = delayQueue.take();   // 阻塞直到有订单到期
                    System.out.println("[超时取消处理器] 订单 " + task.getOrderId()
                            + " 已超时，执行取消操作（" + System.currentTimeMillis() + "ms）");
                    count++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("[超时取消处理器] 所有超时订单处理完毕\n");
        }, "OrderCancelHandler");

        long startTime = System.currentTimeMillis();
        cancelHandler.start();
        cancelHandler.join(5000);
        System.out.println("总耗时：" + (System.currentTimeMillis() - startTime) + "ms（约 3 秒，按到期时间依次处理）\n");
    }

    // =========================================================================
    // 7. LinkedTransferQueue —— transfer 直接移交
    // =========================================================================
    private static void demonstrateLinkedTransferQueue() throws InterruptedException {
        System.out.println("【七、LinkedTransferQueue —— transfer 直接移交】\n");

        System.out.println("特点（相比 LinkedBlockingQueue 的增强）：");
        System.out.println("  - transfer(e)：若有消费者等待则直接移交，否则入队并阻塞");
        System.out.println("  - tryTransfer(e)：只在有消费者等待时移交，否则立即返回 false");
        System.out.println("  - 无锁 CAS 实现，吞吐量优于 LinkedBlockingQueue");
        System.out.println("  - 无界队列\n");

        LinkedTransferQueue<String> ltq = new LinkedTransferQueue<>();

        // tryTransfer：无消费者时立即返回 false
        boolean transferred = ltq.tryTransfer("先来的数据（无消费者等待）");
        System.out.println("tryTransfer（无消费者）返回：" + transferred + "（false，数据未传）");

        // 先启动消费者等待，再 transfer
        Thread consumer = new Thread(() -> {
            try {
                System.out.println("  [消费者] 等待接收...");
                String data = ltq.take();
                System.out.println("  [消费者] 接收：" + data);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        consumer.start();
        Thread.sleep(100);

        // 现在有消费者等待，transfer 直接握手
        boolean ok = ltq.tryTransfer("直接移交的数据");
        System.out.println("tryTransfer（有消费者）返回：" + ok + "（true，直接移交）");
        consumer.join();
        System.out.println();
    }

    // =========================================================================
    // 8. 高频面试题
    // =========================================================================
    private static void showInterviewQuestions() {
        System.out.println("【八、高频面试题】\n");

        System.out.println("Q1：ArrayBlockingQueue 和 LinkedBlockingQueue 的核心区别？");
        System.out.println("┌───────────────┬─────────────────────┬────────────────────────┐");
        System.out.println("│ 特性          │ ArrayBlockingQueue  │ LinkedBlockingQueue    │");
        System.out.println("├───────────────┼─────────────────────┼────────────────────────┤");
        System.out.println("│ 底层          │ 数组                │ 链表                   │");
        System.out.println("│ 容量          │ 有界（必须指定）    │ 可选（默认近似无界）   │");
        System.out.println("│ 锁机制        │ 一把锁              │ 两把锁（读写分离）     │");
        System.out.println("│ 公平锁        │ 支持                │ 不支持                 │");
        System.out.println("│ 内存          │ 连续，占用少        │ 节点对象，占用多       │");
        System.out.println("│ 并发吞吐      │ 中                  │ 高                     │");
        System.out.println("└───────────────┴─────────────────────┴────────────────────────┘\n");

        System.out.println("Q2：SynchronousQueue 为什么 newCachedThreadPool 要选它？");
        System.out.println("A2：corePoolSize=0，任务无法排队（SynchronousQueue 容量为0）");
        System.out.println("    → 直接创建新线程执行，达到按需创建线程的目的");
        System.out.println("    → 空闲 60s 后线程自动回收，节约资源");
        System.out.println("    ⚠️ 风险：任务突增时无限创建线程，可能 OOM\n");

        System.out.println("Q3：DelayQueue 的应用场景？生产级如何实现订单超时？");
        System.out.println("A3：单机场景：DelayQueue（简单，无需引入中间件）");
        System.out.println("    分布式场景：Redis ZSet（zadd expireTime orderId）");
        System.out.println("              + 定时任务轮询 zrangebyscore(0, now) 取到期订单");
        System.out.println("    大规模场景：RabbitMQ 死信队列 / RocketMQ 延时消息\n");

        System.out.println("Q4：put/offer/add 在队满时的行为对比？");
        System.out.println("A4：add(e)                → 抛 IllegalStateException");
        System.out.println("    offer(e)              → 返回 false");
        System.out.println("    offer(e,timeout,unit) → 等待超时，返回 false");
        System.out.println("    put(e)                → 一直阻塞，直到有空间");
        System.out.println("    生产者-消费者模式推荐用 put，有超时要求用 offer(timeout)\n");

        System.out.println("Q5：BlockingQueue 是如何实现阻塞的？");
        System.out.println("A5：以 ArrayBlockingQueue 为例：");
        System.out.println("    - 一把 ReentrantLock + 两个 Condition（notEmpty、notFull）");
        System.out.println("    - put 时队满：notFull.await()（挂起当前线程，释放锁）");
        System.out.println("    - take 时队空：notEmpty.await()（挂起当前线程，释放锁）");
        System.out.println("    - 成功入队后：notEmpty.signal()（唤醒等待消费的线程）");
        System.out.println("    - 成功出队后：notFull.signal()（唤醒等待生产的线程）\n");
    }

    // =========================================================================
    // 辅助类
    // =========================================================================

    /**
     * 延迟任务（订单超时取消）
     * 实现 Delayed 接口，DelayQueue 依此排序和判断是否到期
     */
    static class OrderDelayTask implements Delayed {
        private final String orderId;
        private final long expireTimeMs;   // 绝对到期时间（毫秒）

        OrderDelayTask(String orderId, long delayMs) {
            this.orderId = orderId;
            this.expireTimeMs = System.currentTimeMillis() + delayMs;
        }

        /** 返回剩余延迟时间；≤0 表示已到期，DelayQueue.take() 可返回此元素 */
        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expireTimeMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        /** DelayQueue 内部 PriorityQueue 依此排序，到期时间越早越先出队 */
        @Override
        public int compareTo(Delayed other) {
            return Long.compare(this.expireTimeMs, ((OrderDelayTask) other).expireTimeMs);
        }

        public String getOrderId() { return orderId; }
    }

    /** 优先级任务（用于 PriorityBlockingQueue 演示） */
    static class PriorityTask {
        final String name;
        final int priority;

        PriorityTask(String name, int priority) {
            this.name = name;
            this.priority = priority;
        }
    }
}
