package cn.itzixiao.interview.kafka.controller;

import cn.itzixiao.interview.kafka.service.KafkaMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Kafka 示例接口 - 用于演示和测试
 */
@Slf4j
@RestController
@RequestMapping("/api/kafka")
public class KafkaController {

    @Autowired
    private KafkaMessageService kafkaMessageService;

    /**
     * 1. 同步发送消息
     * 
     * @param topic Topic 名称（默认：topic.order）
     * @param key 消息键
     * @param message 消息内容
     * @return 发送结果
     */
    @PostMapping("/send/sync")
    public String sendSync(
            @RequestParam(defaultValue = "topic.order") String topic,
            @RequestParam String key,
            @RequestParam String message) {
        try {
            SendResult<String, String> result = kafkaMessageService.sendSync(topic, key, message);
            return String.format("✓ 发送成功！Partition: %d, Offset: %d", 
                    result.getRecordMetadata().partition(), 
                    result.getRecordMetadata().offset());
        } catch (Exception e) {
            return "✗ 发送失败：" + e.getMessage();
        }
    }

    /**
     * 2. 异步发送消息
     * 
     * @param topic Topic 名称（默认：topic.order）
     * @param key 消息键
     * @param message 消息内容
     * @return 提交状态
     */
    @PostMapping("/send/async")
    public String sendAsync(
            @RequestParam(defaultValue = "topic.order") String topic,
            @RequestParam String key,
            @RequestParam String message) {
        kafkaMessageService.sendAsync(topic, key, message);
        return "✓ 消息已提交，请查看控制台回调日志";
    }

    /**
     * 3. 批量发送消息（模拟高吞吐场景）
     * 
     * @param topic Topic 名称（默认：topic.log）
     * @param count 消息数量（默认：1000）
     * @return 发送结果
     */
    @PostMapping("/send/batch")
    public String sendBatch(
            @RequestParam(defaultValue = "topic.log") String topic,
            @RequestParam(defaultValue = "1000") int count) {
        
        List<String> messages = Arrays.asList(new String[count]);
        for (int i = 0; i < count; i++) {
            messages.set(i, String.format("{\"logId\":\"%d\",\"content\":\"日志内容 %d\",\"timestamp\":%d}", 
                    i, i, System.currentTimeMillis()));
        }
        
        kafkaMessageService.sendBatch(topic, messages);
        return String.format("✓ 批量发送 %d 条消息已提交", count);
    }

    /**
     * 4. 发送事务消息（Exactly Once 语义）
     * 
     * @param orderId 订单 ID
     * @param userId 用户 ID
     * @return 处理结果
     */
    @PostMapping("/transaction/order")
    public String createOrderWithNotification(
            @RequestParam String orderId,
            @RequestParam String userId) {
        try {
            kafkaMessageService.createOrderWithNotification(orderId, userId);
            return String.format("✓ 订单事务处理完成！orderId=%s", orderId);
        } catch (Exception e) {
            return "✗ 事务处理失败：" + e.getMessage();
        }
    }

    /**
     * 5. 运行完整演示
     * 
     * @return 演示结果
     */
    @PostMapping("/demo")
    public String runDemo() {
        log.info("========== Kafka 示例演示开始 ==========");
        
        try {
            // 演示 1: 同步发送
            log.info("\n【演示 1】同步发送消息");
            kafkaMessageService.sendSync("topic.order", "order-001", "{\"orderId\":\"001\",\"amount\":100}");
            
            // 演示 2: 异步发送
            log.info("\n【演示 2】异步发送消息");
            kafkaMessageService.sendAsync("topic.order", "order-002", "{\"orderId\":\"002\",\"amount\":200}");
            
            // 演示 3: 批量发送
            log.info("\n【演示 3】批量发送（100 条日志）");
            List<String> logs = Arrays.asList(
                    "{\"level\":\"INFO\",\"msg\":\"日志 1\"}",
                    "{\"level\":\"WARN\",\"msg\":\"日志 2\"}",
                    "{\"level\":\"ERROR\",\"msg\":\"日志 3\"}"
            );
            kafkaMessageService.sendBatch("topic.log", logs);
            
            // 演示 4: 事务消息
            log.info("\n【演示 4】事务消息（订单 + 通知）");
            kafkaMessageService.createOrderWithNotification("TX-001", "user-001");
            
            log.info("\n========== 演示完成 ==========");
            
            return "✓ Kafka 演示执行成功！请查看控制台日志";
            
        } catch (Exception e) {
            log.error("演示失败", e);
            return "✗ 演示失败：" + e.getMessage();
        }
    }
}
