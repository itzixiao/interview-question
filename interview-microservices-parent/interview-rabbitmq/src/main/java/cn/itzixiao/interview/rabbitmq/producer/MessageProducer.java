package cn.itzixiao.interview.rabbitmq.producer;

import cn.itzixiao.interview.rabbitmq.model.OrderMessage;
import cn.itzixiao.interview.rabbitmq.model.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 消息生产者
 * 演示各种发送方式和确认机制
 */
@Slf4j
@Component
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送简单消息（直连模式）
     */
    public void sendSimpleMessage(String routingKey, Object message) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend("exchange.direct", routingKey, message, correlationData);
        log.info("发送简单消息，correlationId: {}, routingKey: {}, message: {}",
                correlationId, routingKey, message);
    }

    /**
     * 发送订单消息
     */
    public void sendOrderMessage(OrderMessage orderMessage) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend("exchange.direct", "queue.dlx", orderMessage, correlationData);
        log.info("发送订单消息，correlationId: {}, orderId: {}",
                correlationId, orderMessage.getOrderId());
    }

    /**
     * 发送用户消息（扇形交换机 - 发布订阅模式）
     */
    public void sendFanoutMessage(UserMessage userMessage) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        // 扇形交换机不需要 routingKey
        rabbitTemplate.convertAndSend("exchange.fanout", "", userMessage, correlationData);
        log.info("发送扇形消息，correlationId: {}, username: {}",
                correlationId, userMessage.getUsername());
    }

    /**
     * 发送路由消息（Routing 模式）
     */
    public void sendRoutingMessage(String routingKey, UserMessage userMessage) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend("exchange.direct", routingKey, userMessage, correlationData);
        log.info("发送路由消息，correlationId: {}, routingKey: {}",
                correlationId, routingKey);
    }

    /**
     * 发送主题消息（Topic 模式）
     */
    public void sendTopicMessage(String routingKey, UserMessage userMessage) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend("exchange.topic", routingKey, userMessage, correlationData);
        log.info("发送主题消息，correlationId: {}, routingKey: {}",
                correlationId, routingKey);
    }

    /**
     * 发送延迟消息（通过 TTL 实现）
     */
    public void sendDelayMessage(Object message, int delaySeconds) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend("exchange.direct", "queue.delay", message,
                msg -> {
                    msg.getMessageProperties().setExpiration(String.valueOf(delaySeconds * 1000));
                    return msg;
                },
                correlationData);
        log.info("发送延迟消息，correlationId: {}, delay: {}秒", correlationId, delaySeconds);
    }

    /**
     * 发送优先级消息
     */
    public void sendPriorityMessage(Object message, int priority) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        // 确保优先级在 0-10 之间
        final int finalPriority = Math.max(0, Math.min(10, priority));

        rabbitTemplate.convertAndSend("exchange.direct", "queue.priority", message,
                msg -> {
                    msg.getMessageProperties().setPriority(finalPriority);
                    return msg;
                },
                correlationData);
        log.info("发送优先级消息，correlationId: {}, priority: {}", correlationId, finalPriority);
    }

    /**
     * 发送消息到死信队列
     */
    public void sendToDeadLetterQueue(Object message) {
        String correlationId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(correlationId);

        rabbitTemplate.convertAndSend("exchange.deadletter", "deadletter.routingkey",
                message, correlationData);
        log.info("发送到死信队列，correlationId: {}", correlationId);
    }
}
