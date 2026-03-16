package cn.itzixiao.interview.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 基础消息消费者
 * 演示简单队列和工作队列模式
 */
@Slf4j
@Component
public class SimpleConsumer {

    /**
     * 消费简单队列消息
     * 手动确认模式
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.simple", durable = "true"))
    public void consumeSimpleMessage(Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());

        try {
            log.info("📩 [简单队列] 收到消息 - ID: {}, 内容：{}", messageId, body);

            // TODO: 处理业务逻辑

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("✅ [简单队列] 消息确认成功 - ID: {}", messageId);

        } catch (Exception e) {
            log.error("❌ [简单队列] 处理消息失败 - ID: {}, 错误：{}", messageId, e.getMessage(), e);

            // 判断是否重试
            Integer xDeath = (Integer) message.getMessageProperties().getHeaders().get("x-death");
            if (xDeath != null && xDeath > 2) {
                // 重试次数超过 2 次，拒绝消息并进入死信队列
                channel.basicNack(deliveryTag, false, false);
                log.warn("⚠️ [简单队列] 消息重试次数过多，已拒绝 - ID: {}", messageId);
            } else {
                // 否则重新入队
                channel.basicNack(deliveryTag, false, true);
                log.warn("⚠️ [简单队列] 消息处理失败，重新入队 - ID: {}", messageId);
            }
        }
    }

    /**
     * 消费工作队列消息
     * 多个消费者会负载均衡
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.work", durable = "true"))
    public void consumeWorkQueueMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());

        try {
            log.info("💼 [工作队列] 收到消息 - Thread: {}, 内容：{}",
                    Thread.currentThread().getName(), body);

            // 模拟耗时操作
            Thread.sleep(1000);

            // 手动确认
            channel.basicAck(deliveryTag, false);
            log.info("✅ [工作队列] 消息处理完成 - Thread: {}", Thread.currentThread().getName());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ [工作队列] 线程中断", e);
            channel.basicNack(deliveryTag, false, false);
        } catch (Exception e) {
            log.error("❌ [工作队列] 处理失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
