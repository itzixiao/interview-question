package cn.itzixiao.interview.rabbitmq.interview;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.SimpleConnection;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 高频面试题示例代码
 * 包含常见面试题的实现和演示
 */
@Slf4j
@RestController
@RequestMapping("/api/interview")
public class InterviewQuestionDemo {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory connectionFactory;

    /**
     * 面试题 1: 如何保证消息的可靠性？
     * 答案要点：
     * 1. 生产者确认机制（Publisher Confirm）
     * 2. 消息持久化（队列、交换机、消息都持久化）
     * 3. 消费者手动 ACK
     * 4. 死信队列处理失败消息
     * 5. 消息重试机制
     */
    @PostMapping("/reliability/send")
    public Map<String, Object> sendReliableMessage(@RequestBody String message) {
        Map<String, Object> result = new HashMap<>();
        
        // 构建持久化消息
        Message reliableMessage = MessageBuilder
                .withBody(message.getBytes())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT) // 消息持久化
                .setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN)
                .build();
        
        try {
            // 发送到持久化队列
            rabbitTemplate.convertAndSend("exchange.direct", "queue.dlx", reliableMessage);
            
            result.put("success", true);
            result.put("message", "已发送可靠消息（持久化 + 确认机制）");
            result.put("tips", "查看日志确认 Publisher Confirm 回调");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 面试题 2: 如何实现延迟队列？
     * 答案要点：
     * 方案 1: TTL + 死信队列（本例使用）
     * 方案 2: RabbitMQ 延迟队列插件
     * 
     * 应用场景：
     * - 订单超时取消
     * - 定时任务
     * - 延时通知
     */
    @PostMapping("/delay-queue/send")
    public Map<String, Object> sendDelayQueueMessage(
            @RequestBody String message,
            @RequestParam(defaultValue = "60000") int ttl) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 方式 1: 使用消息 TTL
            Message delayMessage = MessageBuilder
                    .withBody(message.getBytes())
                    .setExpiration(String.valueOf(ttl)) // 设置 TTL
                    .build();
            
            rabbitTemplate.convertAndSend("exchange.direct", "queue.delay", delayMessage);
            
            result.put("success", true);
            result.put("message", String.format("延迟消息已发送，%d 毫秒后处理", ttl));
            result.put("solution", "使用 TTL+ 死信队列实现");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 面试题 3: 如何保证消息的顺序性？
     * 答案要点：
     * 1. 拆分 Queue，每个需要顺序的消息发送到同一个 Queue
     * 2. 单线程消费（性能差，不推荐）
     * 3. 使用队列内建优先级（部分场景适用）
     * 4. 业务层面通过版本号或时间戳判断
     */
    @PostMapping("/order/send")
    public Map<String, Object> sendOrderedMessages(
            @RequestParam String orderId,
            @RequestParam(defaultValue = "3") int count) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 发送一组有顺序的消息
            for (int i = 1; i <= count; i++) {
                String message = String.format("订单%s-操作%d", orderId, i);
                
                // 关键：使用相同的 routingKey，确保进入同一个队列
                rabbitTemplate.convertAndSend("exchange.direct", "queue.simple", message);
                
                log.info("发送顺序消息：{}", message);
            }
            
            result.put("success", true);
            result.put("message", String.format("已发送 %d 条顺序消息", count));
            result.put("solution", "使用相同 routingKey 确保进入同一队列");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 面试题 4: 如何处理重复消息（幂等性）？
     * 答案要点：
     * 1. 消息 ID 去重（Redis Set）
     * 2. 数据库唯一键
     * 3. 乐观锁
     * 4. 状态机
     */
    @PostMapping("/idempotent/send")
    public Map<String, Object> sendIdempotentMessage(
            @RequestBody String message,
            @RequestParam String messageId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 构建带消息 ID 的消息
            Message idempotentMessage = MessageBuilder
                    .withBody(message.getBytes())
                    .setMessageId(messageId) // 设置消息 ID
                    .build();
            
            rabbitTemplate.convertAndSend("exchange.direct", "queue.reliability.idempotent", 
                    idempotentMessage);
            
            result.put("success", true);
            result.put("message", "已发送幂等消息");
            result.put("solution", "消费者通过 messageId 去重");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 面试题 5: 如何实现优先级队列？
     * 答案要点：
     * 1. 队列设置 x-max-priority 参数
     * 2. 消息发送时设置 priority
     * 3. 优先级高的消息先被消费
     */
    @PostMapping("/priority/send-batch")
    public Map<String, Object> sendPriorityMessages() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 发送不同优先级的消息
            for (int priority = 1; priority <= 5; priority++) {
                String message = "优先级测试-" + priority;
                
                Message priorityMessage = MessageBuilder
                        .withBody(message.getBytes())
                        .setPriority(priority) // 设置优先级
                        .build();
                
                rabbitTemplate.convertAndSend("exchange.direct", "queue.priority", 
                        priorityMessage);
                
                Thread.sleep(100); // 间隔发送，便于观察
            }
            
            result.put("success", true);
            result.put("message", "已发送 5 条不同优先级的消息");
            result.put("solution", "消费者会优先处理高优先级消息");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 面试题 6: 发布订阅模式的几种交换机区别？
     * 答案要点：
     * 1. Direct Exchange: routingKey 完全匹配
     * 2. Fanout Exchange: 忽略 routingKey，广播到所有绑定队列
     * 3. Topic Exchange: routingKey 模糊匹配（* 和 #）
     */
    @PostMapping("/exchange/compare")
    public Map<String, Object> compareExchanges(@RequestBody String message) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. Direct Exchange - 精确匹配
            rabbitTemplate.convertAndSend("exchange.direct", "routing.key.1", 
                    "Direct: " + message);
            
            // 2. Fanout Exchange - 广播（不需要 routingKey）
            rabbitTemplate.convertAndSend("exchange.fanout", "", 
                    "Fanout: " + message);
            
            // 3. Topic Exchange - 模糊匹配
            rabbitTemplate.convertAndSend("exchange.topic", "topic.normal", 
                    "Topic: " + message);
            
            result.put("success", true);
            result.put("message", "已向三种交换机发送消息");
            result.put("comparison", "查看控制台日志，观察不同队列收到的消息");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 面试题 7: 如何处理大量积压的消息？
     * 答案要点：
     * 1. 临时扩容消费者
     * 2. 优化消费逻辑，提高处理速度
     * 3. 调整 prefetch 参数
     * 4. 丢弃非重要消息（设置 TTL）
     * 5. 转移到其他队列批量处理
     */
    @GetMapping("/backlog/solution")
    public Map<String, Object> getBacklogSolution() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("success", true);
        result.put("solutions", new String[]{
            "1. 增加消费者数量（扩容）",
            "2. 优化业务逻辑，减少单次处理时间",
            "3. 调整 prefetch 参数，提高吞吐量",
            "4. 对于有时效性的消息，设置 TTL 自动过期",
            "5. 将非紧急消息转移到其他队列，后续批量处理",
            "6. 检查是否有异常导致消费失败"
        });
        
        return result;
    }

    /**
     * 面试题 8: 如何保证高可用？
     * 答案要点：
     * 1. 集群模式（普通集群、镜像集群）
     * 2. 队列镜像（Mirror Queue）
     * 3. 负载均衡
     * 4. 故障转移
     */
    @GetMapping("/ha/solution")
    public Map<String, Object> getHASolution() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("success", true);
        result.put("solutions", new String[]{
            "1. 搭建 RabbitMQ 集群（至少 3 个节点）",
            "2. 使用镜像队列（Mirror Queue）同步数据",
            "3. 配置负载均衡（HAProxy/Nginx）",
            "4. 客户端配置多个连接地址",
            "5. 使用仲裁队列（Quorum Queues，RabbitMQ 3.8+）"
        });
        
        return result;
    }

    /**
     * 面试题 9: 死信队列的应用场景？
     * 答案要点：
     * 1. 消息处理失败后的容错
     * 2. 延迟队列实现
     * 3. 异常消息收集和分析
     * 4. 人工介入处理
     */
    @PostMapping("/dlx/send-failure")
    public Map<String, Object> sendToDeadLetter(@RequestBody String message) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 直接发送到死信队列
            rabbitTemplate.convertAndSend("exchange.deadletter", "deadletter.routingkey", 
                    message);
            
            result.put("success", true);
            result.put("message", "消息已发送到死信队列");
            result.put("scenarios", new String[]{
                "1. 正常队列消费失败的消息",
                "2. 延迟队列到期后的消息",
                "3. 超过最大长度限制的消息",
                "4. 需要人工介入的异常消息"
            });
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "发送失败：" + e.getMessage());
        }
        
        return result;
    }

    /**
     * 面试题 10: 消息队列的优缺点？
     * 答案要点：
     * 优点：解耦、异步、削峰
     * 缺点：系统复杂度增加、一致性挑战、依赖第三方
     */
    @GetMapping("/mq-pros-cons")
    public Map<String, Object> getMQProsCons() {
        Map<String, Object> result = new HashMap<>();
        
        Map<String, String[]> pros = new HashMap<>();
        pros.put("优点", new String[]{
            "1. 解耦：生产者和消费者互不依赖",
            "2. 异步：提高系统响应速度",
            "3. 削峰：平滑流量峰值，保护后端系统",
            "4. 缓冲：缓解瞬时压力"
        });
        
        Map<String, String[]> cons = new HashMap<>();
        cons.put("缺点", new String[]{
            "1. 系统复杂度增加",
            "2. 数据一致性挑战",
            "3. 依赖第三方中间件",
            "4. 运维成本增加"
        });
        
        result.put("success", true);
        result.putAll(pros);
        result.putAll(cons);
        
        return result;
    }
}
