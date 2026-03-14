package cn.itzixiao.interview.rabbitmq.consumer;

import cn.itzixiao.interview.rabbitmq.model.OrderMessage;
import cn.itzixiao.interview.rabbitmq.model.UserMessage;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 高级消息消费者
 * 演示发布订阅、路由、主题等模式
 */
@Slf4j
@Component
public class AdvancedConsumer {

    /**
     * 消费扇形交换机消息（发布订阅模式）
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.fanout.1", durable = "true"))
    public void consumeFanoutQueue1(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());
        
        try {
            log.info("📢 [发布订阅 - 队列 1] 收到消息：{}", body);
            
            // TODO: 处理业务逻辑
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [发布订阅 - 队列 1] 消息确认成功");
            
        } catch (Exception e) {
            log.error("❌ [发布订阅 - 队列 1] 处理失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 消费扇形交换机消息（发布订阅模式 - 队列 2）
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.fanout.2", durable = "true"))
    public void consumeFanoutQueue2(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());
        
        try {
            log.info("📢 [发布订阅 - 队列 2] 收到消息：{}", body);
            
            // TODO: 处理业务逻辑
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [发布订阅 - 队列 2] 消息确认成功");
            
        } catch (Exception e) {
            log.error("❌ [发布订阅 - 队列 2] 处理失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 消费路由队列消息（Routing 模式）
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.routing.1", durable = "true"))
    public void consumeRoutingQueue1(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody());
        
        try {
            log.info("🎯 [路由模式 - 队列 1] 收到消息 - routingKey: {}, 内容：{}", routingKey, body);
            
            // TODO: 处理业务逻辑
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [路由模式 - 队列 1] 消息确认成功");
            
        } catch (Exception e) {
            log.error("❌ [路由模式 - 队列 1] 处理失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 消费路由队列消息（Routing 模式 - 队列 2）
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.routing.2", durable = "true"))
    public void consumeRoutingQueue2(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody());
        
        try {
            log.info("🎯 [路由模式 - 队列 2] 收到消息 - routingKey: {}, 内容：{}", routingKey, body);
            
            // TODO: 处理业务逻辑
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [路由模式 - 队列 2] 消息确认成功");
            
        } catch (Exception e) {
            log.error("❌ [路由模式 - 队列 2] 处理失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 消费主题队列消息（Topic 模式 - 队列 1）
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.topic.1", durable = "true"))
    public void consumeTopicQueue1(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody());
        
        try {
            log.info("🏷️ [主题模式 - 队列 1] 收到消息 - routingKey: {}, 内容：{}", routingKey, body);
            
            // TODO: 处理业务逻辑
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [主题模式 - 队列 1] 消息确认成功");
            
        } catch (Exception e) {
            log.error("❌ [主题模式 - 队列 1] 处理失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 消费主题队列消息（Topic 模式 - 队列 2）
     */
    @RabbitListener(queuesToDeclare = @Queue(value = "queue.topic.2", durable = "true"))
    public void consumeTopicQueue2(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String body = new String(message.getBody());
        
        try {
            log.info("🏷️ [主题模式 - 队列 2] 收到消息 - routingKey: {}, 内容：{}", routingKey, body);
            
            // TODO: 处理业务逻辑
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [主题模式 - 队列 2] 消息确认成功");
            
        } catch (Exception e) {
            log.error("❌ [主题模式 - 队列 2] 处理失败", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }

    /**
     * 消费订单消息（带 DLX 的队列）
     */
    @RabbitListener(queues = "queue.normal.with.dlx")
    public void consumeOrderMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());
        
        try {
            log.info("🛒 [订单队列] 收到订单消息：{}", body);
            
            // TODO: 处理订单业务逻辑
            // 这里可以调用 OrderService 处理订单
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [订单队列] 订单处理成功");
            
        } catch (Exception e) {
            log.error("❌ [订单队列] 订单处理失败", e);
            
            // 失败后拒绝消息，进入死信队列
            channel.basicNack(deliveryTag, false, false);
            log.warn("⚠️ [订单队列] 订单消息已拒绝，将进入死信队列");
        }
    }

    /**
     * 消费死信队列消息
     */
    @RabbitListener(queues = "queue.deadletter")
    public void consumeDeadLetterMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String originalQueue = (String) message.getMessageProperties().getHeaders().get("x-first-death-queue");
        String originalReason = (String) message.getMessageProperties().getHeaders().get("x-first-death-reason");
        String body = new String(message.getBody());
        
        log.warn("💀 [死信队列] 收到死信 - 原队列：{}, 死亡原因：{}, 内容：{}", 
                originalQueue, originalReason, body);
        
        // TODO: 记录死信消息到数据库，人工介入处理
        
        // 死信队列的消息一般不再重试，直接确认
        channel.basicAck(deliveryTag, false);
        log.info("✅ [死信队列] 死信已确认");
    }

    /**
     * 消费延迟队列的死信消息
     */
    @RabbitListener(queues = "queue.delay.deadletter")
    public void consumeDelayDeadLetterMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody());
        
        log.info("⏰ [延迟队列] 延迟消息到期：{}", body);
        
        // TODO: 处理延迟任务，如订单超时取消、定时任务等
        
        channel.basicAck(deliveryTag, false);
        log.info("✅ [延迟队列] 延迟任务处理完成");
    }

    /**
     * 消费优先级队列消息
     */
    @RabbitListener(queues = "queue.priority")
    public void consumePriorityMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        Integer priority = message.getMessageProperties().getPriority();
        String body = new String(message.getBody());
        
        try {
            log.info("⭐ [优先级队列] 收到消息 - 优先级：{}, 内容：{}", priority, body);
            
            // TODO: 根据优先级处理不同的业务逻辑
            
            channel.basicAck(deliveryTag, false);
            log.info("✅ [优先级队列] 消息处理完成 - 优先级：{}", priority);
            
        } catch (Exception e) {
            log.error("❌ [优先级队列] 处理失败 - 优先级：{}", priority, e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
