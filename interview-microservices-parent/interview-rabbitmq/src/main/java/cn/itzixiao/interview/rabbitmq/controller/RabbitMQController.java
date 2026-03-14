package cn.itzixiao.interview.rabbitmq.controller;

import cn.itzixiao.interview.rabbitmq.model.OrderMessage;
import cn.itzixiao.interview.rabbitmq.model.UserMessage;
import cn.itzixiao.interview.rabbitmq.producer.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 测试接口
 * 用于演示各种消息发送场景
 */
@Slf4j
@RestController
@RequestMapping("/api/rabbitmq")
public class RabbitMQController {

    @Autowired
    private MessageProducer messageProducer;

    /**
     * 发送简单消息
     */
    @PostMapping("/send/simple")
    public Map<String, Object> sendSimple(@RequestParam String routingKey,
                                          @RequestBody String message) {
        Map<String, Object> result = new HashMap<>();
        try {
            messageProducer.sendSimpleMessage(routingKey, message);
            result.put("success", true);
            result.put("message", "消息发送成功");
        } catch (Exception e) {
            log.error("发送简单消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送订单消息
     */
    @PostMapping("/send/order")
    public Map<String, Object> sendOrder(@RequestBody OrderMessage orderMessage) {
        Map<String, Object> result = new HashMap<>();
        try {
            orderMessage.setCreateTime(System.currentTimeMillis());
            messageProducer.sendOrderMessage(orderMessage);
            result.put("success", true);
            result.put("message", "订单消息发送成功");
        } catch (Exception e) {
            log.error("发送订单消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送用户消息（发布订阅模式）
     */
    @PostMapping("/send/fanout")
    public Map<String, Object> sendFanout(@RequestBody UserMessage userMessage) {
        Map<String, Object> result = new HashMap<>();
        try {
            userMessage.setTimestamp(System.currentTimeMillis());
            messageProducer.sendFanoutMessage(userMessage);
            result.put("success", true);
            result.put("message", "发布订阅消息发送成功，所有订阅者都会收到");
        } catch (Exception e) {
            log.error("发送发布订阅消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送路由消息
     */
    @PostMapping("/send/routing")
    public Map<String, Object> sendRouting(@RequestParam String routingKey,
                                           @RequestBody UserMessage userMessage) {
        Map<String, Object> result = new HashMap<>();
        try {
            userMessage.setTimestamp(System.currentTimeMillis());
            messageProducer.sendRoutingMessage(routingKey, userMessage);
            result.put("success", true);
            result.put("message", "路由消息发送成功");
        } catch (Exception e) {
            log.error("发送路由消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送主题消息
     */
    @PostMapping("/send/topic")
    public Map<String, Object> sendTopic(@RequestParam String routingKey,
                                         @RequestBody UserMessage userMessage) {
        Map<String, Object> result = new HashMap<>();
        try {
            userMessage.setTimestamp(System.currentTimeMillis());
            messageProducer.sendTopicMessage(routingKey, userMessage);
            result.put("success", true);
            result.put("message", "主题消息发送成功");
        } catch (Exception e) {
            log.error("发送主题消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送延迟消息
     */
    @PostMapping("/send/delay")
    public Map<String, Object> sendDelay(@RequestBody String message,
                                         @RequestParam(defaultValue = "10") int delaySeconds) {
        Map<String, Object> result = new HashMap<>();
        try {
            messageProducer.sendDelayMessage(message, delaySeconds);
            result.put("success", true);
            result.put("message", String.format("延迟消息发送成功，将在 %d 秒后处理", delaySeconds));
        } catch (Exception e) {
            log.error("发送延迟消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送优先级消息
     */
    @PostMapping("/send/priority")
    public Map<String, Object> sendPriority(@RequestBody String message,
                                            @RequestParam(defaultValue = "5") int priority) {
        Map<String, Object> result = new HashMap<>();
        try {
            messageProducer.sendPriorityMessage(message, priority);
            result.put("success", true);
            result.put("message", String.format("优先级消息发送成功，优先级：%d", priority));
        } catch (Exception e) {
            log.error("发送优先级消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 批量发送优先级消息（演示优先级效果）
     */
    @PostMapping("/send/priority/batch")
    public Map<String, Object> sendPriorityBatch() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 发送不同优先级的消息
            for (int i = 1; i <= 5; i++) {
                String message = "优先级测试消息-" + i;
                messageProducer.sendPriorityMessage(message, i);
                Thread.sleep(100); // 稍微延迟，便于观察
            }
            result.put("success", true);
            result.put("message", "批量发送 5 条不同优先级的消息，消费者会按优先级处理");
        } catch (Exception e) {
            log.error("批量发送优先级消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 发送消息到死信队列
     */
    @PostMapping("/send/deadletter")
    public Map<String, Object> sendDeadLetter(@RequestBody String message) {
        Map<String, Object> result = new HashMap<>();
        try {
            messageProducer.sendToDeadLetterQueue(message);
            result.put("success", true);
            result.put("message", "死信消息发送成功");
        } catch (Exception e) {
            log.error("发送死信消息失败", e);
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 模拟订单超时取消（使用延迟队列）
     */
    @PostMapping("/order/timeout/cancel")
    public Map<String, Object> orderTimeoutCancel(@RequestParam String orderId) {
        Map<String, Object> result = new HashMap<>();
        try {
            OrderMessage orderMessage = OrderMessage.builder()
                    .orderId(orderId)
                    .status("PENDING_PAYMENT")
                    .createTime(System.currentTimeMillis())
                    .build();
            
            // 发送延迟消息，30 秒后处理订单超时
            messageProducer.sendDelayMessage(orderMessage, 30);
            
            result.put("success", true);
            result.put("message", "订单超时任务已设置，30 秒后将自动取消未支付订单");
        } catch (Exception e) {
            log.error("设置订单超时任务失败", e);
            result.put("success", false);
            result.put("message", "设置失败：" + e.getMessage());
        }
        return result;
    }
}
