package cn.itzixiao.interview.rabbitmq.advanced;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 可靠性保障示例
 * 演示消息确认、重试、持久化等机制
 */
@Slf4j
@Component
public class ReliabilityDemo {

    /**
     * 演示手动 ACK 确认机制
     * 只有业务处理成功后才确认消息
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.reliability.manual-ack", durable = "true"))
    public void manualAckDemo(org.springframework.amqp.core.Message message,
                              com.rabbitmq.client.Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            String body = new String(message.getBody());
            log.info("📥 [手动 ACK] 收到消息：{}", body);

            // 模拟业务处理
            processBusiness(body);

            // 业务处理成功，手动确认
            channel.basicAck(deliveryTag, false);
            log.info("✅ [手动 ACK] 消息确认成功");

        } catch (Exception e) {
            log.error("❌ [手动 ACK] 处理失败", e);
            try {
                // 判断重试次数
                Integer xDeath = (Integer) message.getMessageProperties()
                        .getHeaders().get("x-death");

                if (xDeath != null && xDeath >= 3) {
                    // 重试超过 3 次，拒绝消息，进入死信队列
                    channel.basicNack(deliveryTag, false, false);
                    log.warn("⚠️ [手动 ACK] 重试次数过多，消息进入死信队列");
                } else {
                    // 否则重新入队
                    channel.basicNack(deliveryTag, false, true);
                    log.warn("⚠️ [手动 ACK] 消息重新入队，等待重试");
                }
            } catch (Exception ex) {
                log.error("❌ [手动 ACK] ACK 操作失败", ex);
            }
        }
    }

    /**
     * 演示消息持久化
     * 队列和消息都持久化，防止 RabbitMQ 重启丢失
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.reliability.persistent",
            durable = "true"))
    public void persistentMessageDemo(org.springframework.amqp.core.Message message,
                                      com.rabbitmq.client.Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            String body = new String(message.getBody());
            MessageDeliveryMode deliveryMode = message.getMessageProperties().getDeliveryMode();
            boolean isPersistent = deliveryMode == MessageDeliveryMode.PERSISTENT;

            log.info("💾 [持久化消息] 收到消息：{}, 持久化标志：{}", body, isPersistent);

            // 处理业务
            processBusiness(body);

            channel.basicAck(deliveryTag, false);
            log.info("✅ [持久化消息] 处理完成");

        } catch (Exception e) {
            log.error("❌ [持久化消息] 处理失败", e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("❌ [持久化消息] ACK 失败", ex);
            }
        }
    }

    /**
     * 演示幂等性处理
     * 通过消息 ID 去重，防止重复消费
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.reliability.idempotent",
            durable = "true"))
    public void idempotentConsumer(org.springframework.amqp.core.Message message,
                                   com.rabbitmq.client.Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();

        try {
            String body = new String(message.getBody());

            // TODO: 检查消息是否已处理过（可以使用 Redis 记录已处理的消息 ID）
            if (isMessageProcessed(messageId)) {
                log.info("⏭️ [幂等性] 消息已处理，跳过 - ID: {}", messageId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            log.info("🔄 [幂等性] 处理新消息 - ID: {}, 内容：{}", messageId, body);

            // 处理业务
            processBusiness(body);

            // TODO: 标记消息已处理
            markMessageAsProcessed(messageId);

            channel.basicAck(deliveryTag, false);
            log.info("✅ [幂等性] 消息处理完成并标记");

        } catch (Exception e) {
            log.error("❌ [幂等性] 处理失败 - ID: {}", messageId, e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("❌ [幂等性] ACK 失败", ex);
            }
        }
    }

    /**
     * 演示限流处理
     * 通过 prefetch 控制每次推送的消息数量
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.reliability.rate-limit",
            durable = "true"))
    public void rateLimitConsumer(org.springframework.amqp.core.Message message,
                                  com.rabbitmq.client.Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            String body = new String(message.getBody());
            log.info("🚦 [限流] 收到消息：{}", body);

            // 模拟耗时操作
            Thread.sleep(2000);

            processBusiness(body);

            channel.basicAck(deliveryTag, false);
            log.info("✅ [限流] 消息处理完成");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ [限流] 线程中断", e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("❌ [限流] ACK 失败", ex);
            }
        } catch (Exception e) {
            log.error("❌ [限流] 处理失败", e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("❌ [限流] ACK 失败", ex);
            }
        }
    }

    /**
     * 模拟业务处理
     */
    private void processBusiness(String data) {
        // 模拟业务逻辑
        log.debug("处理业务数据：{}", data);
    }

    /**
     * 检查消息是否已处理
     */
    private boolean isMessageProcessed(String messageId) {
        // TODO: 实际场景中应该查询 Redis 或数据库
        // 这里简单模拟，假设以"repeat"开头的消息已处理
        return messageId != null && messageId.startsWith("repeat");
    }

    /**
     * 标记消息已处理
     */
    private void markMessageAsProcessed(String messageId) {
        // TODO: 实际场景中应该写入 Redis 或数据库
        log.info("标记消息已处理：{}", messageId);
    }
}
