package cn.itzixiao.interview.redis.advanced;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;
import java.util.concurrent.*;

/**
 * Redis 延时队列实现详解
 *
 * 延时队列应用场景：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  1. 订单超时取消：下单后30分钟未支付自动取消                  │
 * │  2. 延时通知：预约提醒、活动开始提醒                          │
 * │  3. 重试机制：失败任务延时重试                                │
 * │  4. 定时任务：分布式定时任务调度                              │
 * │  5. 延迟处理：优惠券过期提醒、会员到期提醒                    │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 实现方式对比：
 * ┌─────────────┬─────────────┬─────────────┬─────────────┐
 * │   方式       │   精度      │   可靠性    │   复杂度    │
 * ├─────────────┼─────────────┼─────────────┼─────────────┤
 * │  ZSet       │   毫秒      │   高        │   低        │
 * │  过期监听   │   秒级      │   低        │   低        │
 * │  Redisson   │   毫秒      │   高        │   低        │
 * │  Stream     │   毫秒      │   高        │   中        │
 * └─────────────┴─────────────┴─────────────┴─────────────┘
 */
public class RedisDelayQueueDemo {

    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 延时任务消息
     */
    public static class DelayMessage {
        private String id;
        private String type;
        private String content;
        private long executeTime;
        private int retryCount;

        public DelayMessage() {}

        public DelayMessage(String id, String type, String content, long delayMs) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.executeTime = System.currentTimeMillis() + delayMs;
            this.retryCount = 0;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public long getExecuteTime() { return executeTime; }
        public void setExecuteTime(long executeTime) { this.executeTime = executeTime; }
        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    }

    /**
     * 1. ZSet 实现延时队列（最常用）
     *
     * 原理：
     * - 使用 ZADD 添加任务，score 为执行时间戳
     * - 使用 ZRANGEBYSCORE 获取到期任务
     * - 使用 ZREM 删除已处理任务
     */
    public void zsetDelayQueueDemo() throws JsonProcessingException {
        String queueKey = "delay:queue:order";

        // 添加延时任务
        DelayMessage message1 = new DelayMessage("order:001", "ORDER_TIMEOUT",
                "{\"orderId\":\"001\",\"userId\":\"1001\"}", 30000); // 30秒后执行

        DelayMessage message2 = new DelayMessage("order:002", "ORDER_TIMEOUT",
                "{\"orderId\":\"002\",\"userId\":\"1002\"}", 60000); // 60秒后执行

        // 添加到 ZSet，score 为执行时间戳
        redisTemplate.opsForZSet().add(queueKey,
                objectMapper.writeValueAsString(message1),
                message1.getExecuteTime());

        redisTemplate.opsForZSet().add(queueKey,
                objectMapper.writeValueAsString(message2),
                message2.getExecuteTime());

        System.out.println("【延时队列】添加2个延时任务");
    }

    /**
     * 消费延时任务（轮询方式）
     */
    public void consumeDelayMessage(String queueKey) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();

                // 获取已到期的任务（score <= now）
                Set<String> messages = redisTemplate.opsForZSet()
                        .rangeByScore(queueKey, 0, now, 0, 10);

                if (messages == null || messages.isEmpty()) {
                    return;
                }

                for (String messageJson : messages) {
                    DelayMessage message = objectMapper.readValue(messageJson, DelayMessage.class);

                    // 删除任务（使用 ZREM 保证原子性，避免重复消费）
                    Long removed = redisTemplate.opsForZSet()
                            .remove(queueKey, messageJson);

                    if (removed != null && removed > 0) {
                        // 成功获取任务，异步处理
                        executorService.submit(() -> processDelayMessage(message));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    /**
     * 2. Redisson 延时队列（推荐生产使用）
     */
    public void redissonDelayQueueDemo() {
        org.redisson.api.RedissonClient redisson = org.redisson.Redisson.create();

        // 目标队列（普通队列）
        org.redisson.api.RQueue<String> destinationQueue =
                redisson.getQueue("order:timeout:queue");

        // 延时队列
        org.redisson.api.RDelayedQueue<String> delayedQueue =
                redisson.getDelayedQueue(destinationQueue);

        // 添加延时任务
        delayedQueue.offer("order:001", 30, TimeUnit.SECONDS);
        delayedQueue.offer("order:002", 60, TimeUnit.SECONDS);
        delayedQueue.offer("order:003", 5, TimeUnit.MINUTES);

        System.out.println("【Redisson延时队列】添加3个延时任务");

        // 消费任务
        new Thread(() -> {
            while (true) {
                String orderId = destinationQueue.poll();
                if (orderId != null) {
                    System.out.println("【处理超时订单】" + orderId);
                }
            }
        }).start();
    }

    /**
     * 3. 订单超时取消完整实现
     */
    public class OrderDelayService {
        private static final String ORDER_DELAY_QUEUE = "delay:order:cancel";
        private static final long ORDER_TIMEOUT_MS = 30 * 60 * 1000; // 30分钟

        /**
         * 创建订单时添加延时取消任务
         */
        public void createOrder(String orderId) throws JsonProcessingException {
            // 1. 创建订单（数据库操作）
            System.out.println("【创建订单】" + orderId);

            // 2. 添加延时取消任务
            DelayMessage cancelTask = new DelayMessage(
                    orderId,
                    "ORDER_CANCEL",
                    orderId,
                    ORDER_TIMEOUT_MS
            );

            redisTemplate.opsForZSet().add(ORDER_DELAY_QUEUE,
                    objectMapper.writeValueAsString(cancelTask),
                    cancelTask.getExecuteTime());

            System.out.println("【延时任务】订单 " + orderId + " 将在30分钟后检查超时");
        }

        /**
         * 支付成功，删除延时取消任务
         */
        public void paySuccess(String orderId) throws JsonProcessingException {
            // 1. 更新订单状态
            System.out.println("【支付成功】" + orderId);

            // 2. 删除延时取消任务
            DelayMessage cancelTask = new DelayMessage(
                    orderId, "ORDER_CANCEL", orderId, 0);

            redisTemplate.opsForZSet().remove(ORDER_DELAY_QUEUE,
                    objectMapper.writeValueAsString(cancelTask));

            System.out.println("【取消延时任务】订单 " + orderId + " 已支付");
        }

        /**
         * 定时检查超时订单
         */
        public void checkTimeoutOrders() {
            long now = System.currentTimeMillis();

            Set<String> tasks = redisTemplate.opsForZSet()
                    .rangeByScore(ORDER_DELAY_QUEUE, 0, now, 0, 100);

            if (tasks == null || tasks.isEmpty()) return;

            for (String taskJson : tasks) {
                try {
                    DelayMessage task = objectMapper.readValue(taskJson, DelayMessage.class);

                    // 原子性删除
                    Long removed = redisTemplate.opsForZSet()
                            .remove(ORDER_DELAY_QUEUE, taskJson);

                    if (removed != null && removed > 0) {
                        // 执行取消操作
                        cancelOrder(task.getContent());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void cancelOrder(String orderId) {
            System.out.println("【取消订单】订单 " + orderId + " 超时未支付，自动取消");
            // 1. 检查订单状态
            // 2. 如果未支付，取消订单
            // 3. 恢复库存
            // 4. 发送通知
        }
    }

    /**
     * 4. 延时任务处理器（带重试机制）
     */
    public abstract class DelayMessageHandler {
        private static final int MAX_RETRY = 3;
        private static final long RETRY_DELAY_MS = 5000;

        public void handle(DelayMessage message) {
            try {
                process(message);
                onSuccess(message);
            } catch (Exception e) {
                onError(message, e);
            }
        }

        protected abstract void process(DelayMessage message);

        protected void onSuccess(DelayMessage message) {
            System.out.println("【任务成功】" + message.getId());
        }

        protected void onError(DelayMessage message, Exception e) {
            System.err.println("【任务失败】" + message.getId() + ", 错误: " + e.getMessage());

            if (message.getRetryCount() < MAX_RETRY) {
                // 重试
                message.setRetryCount(message.getRetryCount() + 1);
                message.setExecuteTime(System.currentTimeMillis() + RETRY_DELAY_MS);

                try {
                    redisTemplate.opsForZSet().add("delay:queue:retry",
                            objectMapper.writeValueAsString(message),
                            message.getExecuteTime());
                    System.out.println("【重试任务】" + message.getId() + " 将在5秒后重试");
                } catch (JsonProcessingException ex) {
                    ex.printStackTrace();
                }
            } else {
                // 进入死信队列
                System.err.println("【死信】" + message.getId() + " 超过最大重试次数");
            }
        }
    }

    private void processDelayMessage(DelayMessage message) {
        System.out.println("【处理延时任务】" + message.getId() + ", 类型: " + message.getType());

        switch (message.getType()) {
            case "ORDER_TIMEOUT":
                System.out.println("处理订单超时: " + message.getContent());
                break;
            case "COUPON_EXPIRE":
                System.out.println("处理优惠券过期: " + message.getContent());
                break;
            case "REMINDER":
                System.out.println("发送提醒: " + message.getContent());
                break;
            default:
                System.out.println("未知类型任务: " + message.getType());
        }
    }
}
