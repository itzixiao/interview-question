package cn.itzixiao.interview.kafka.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 消息服务 - 演示生产者和消费者的完整使用
 *
 * <p>包含：消息发送、批量发送、消息消费、Offset 提交等核心功能</p>
 */
@Slf4j
@Service
public class KafkaMessageService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 1. 发送普通消息（同步）
     *
     * @param topic   Topic 名称
     * @param key     消息键
     * @param message 消息内容
     * @return 发送结果
     */
    public SendResult<String, String> sendSync(String topic, String key, String message) {
        try {
            // 同步发送，阻塞等待结果
            SendResult<String, String> result = kafkaTemplate.send(topic, key, message).get(10, TimeUnit.SECONDS);

            RecordMetadata metadata = result.getRecordMetadata();
            log.info("【同步发送成功】topic={}, partition={}, offset={}, key={}",
                    metadata.topic(), metadata.partition(), metadata.offset(), key);

            return result;
        } catch (Exception e) {
            log.error("【同步发送失败】topic={}, key={}, error={}", topic, key, e.getMessage());
            throw new RuntimeException("发送失败", e);
        }
    }

    /**
     * 2. 发送消息（异步 + 回调）
     *
     * @param topic   Topic 名称
     * @param key     消息键
     * @param message 消息内容
     */
    public void sendAsync(String topic, String key, String message) {
        kafkaTemplate.send(topic, key, message).addCallback(
                result -> {
                    RecordMetadata metadata = result.getRecordMetadata();
                    log.info("【异步发送成功】topic={}, partition={}, offset={}, key={}",
                            metadata.topic(), metadata.partition(), metadata.offset(), key);
                },
                ex -> {
                    log.error("【异步发送失败】topic={}, key={}, error={}", topic, key, ex.getMessage());
                }
        );
    }

    /**
     * 3. 批量发送消息（高吞吐场景）
     *
     * @param topic    Topic 名称
     * @param messages 消息列表
     */
    public void sendBatch(String topic, List<String> messages) {
        log.info("【批量发送】开始发送 {} 条消息", messages.size());

        for (int i = 0; i < messages.size(); i++) {
            final int index = i;  // 用于 lambda
            String key = "batch-key-" + i;
            String message = messages.get(i);

            kafkaTemplate.send(topic, key, message).addCallback(
                    result -> {
                    },
                    ex -> {
                        log.error("【批量发送失败】index={}, key={}, error={}", index, key, ex.getMessage());
                    }
            );
        }

        log.info("【批量发送】{} 条消息已提交", messages.size());
    }

    /**
     * 4. 发送带事务的消息（Exactly Once 语义）
     *
     * @param topic   Topic 名称
     * @param key     消息键
     * @param message 消息内容
     */
    public void sendTransactional(String topic, String key, String message) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send(topic, key, message);
            log.info("【事务消息】发送成功：topic={}, key={}", topic, key);
            return true;
        });
    }

    /**
     * 5. 消费消息（单条）
     *
     * @param record 消费者记录
     */
    @KafkaListener(topics = "topic.order", groupId = "order-consumer-group")
    public void consumeOrder(ConsumerRecord<String, String> record) {
        log.info("【订单消费】收到消息：key={}, value={}, partition={}, offset={}",
                record.key(), record.value(), record.partition(), record.offset());

        // 业务处理逻辑
        processOrder(record.value());

        // Offset 由 Spring Kafka 自动提交（因为配置了 MANUAL_IMMEDIATE）
    }

    /**
     * 6. 批量消费消息（高吞吐优化）
     *
     * @param records 消息列表
     */
    @KafkaListener(topics = "topic.log", groupId = "log-consumer-group", containerFactory = "kafkaListenerContainerFactory")
    public void consumeLogBatch(List<ConsumerRecord<String, String>> records) {
        log.info("【日志消费】批量消费 {} 条消息", records.size());

        for (ConsumerRecord<String, String> record : records) {
            log.info("  → partition={}, offset={}, key={}, value={}",
                    record.partition(), record.offset(), record.key(), record.value());

            // 处理日志
            processLog(record.value());
        }

        // 批量提交 Offset（提高消费吞吐）
        log.info("【日志消费】批量处理完成，准备提交 Offset");
    }

    /**
     * 7. 分区消费（指定 Partition）
     *
     * @param record 消费者记录
     */
    @KafkaListener(topics = "topic.order",
            groupId = "partition-consumer-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeByPartition(ConsumerRecord<String, String> record) {
        int partition = record.partition();
        log.info("【分区消费】处理分区 {} 的消息：offset={}, value={}", partition, record.offset(), record.value());

        // 根据分区做特殊处理
        if (partition == 0) {
            log.info("  → 这是 VIP 订单，优先处理");
        }
    }

    /**
     * 处理订单业务逻辑
     */
    private void processOrder(String orderJson) {
        // 模拟业务处理
        try {
            Thread.sleep(100);
            log.info("  ✓ 订单处理完成：{}", orderJson.substring(0, Math.min(50, orderJson.length())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 处理日志业务逻辑
     */
    private void processLog(String logContent) {
        // 模拟日志处理
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 8. 演示 Exactly Once 语义实现
     * <p>
     * 场景：订单创建成功后，发送通知消息
     *
     * @param orderId 订单 ID
     * @param userId  用户 ID
     */
    public void createOrderWithNotification(String orderId, String userId) {
        kafkaTemplate.executeInTransaction(operations -> {
            try {
                // 步骤 1: 创建订单（模拟数据库操作）
                log.info("【事务处理】创建订单：orderId={}, userId={}", orderId, userId);

                // 步骤 2: 发送订单创建消息
                String orderMessage = String.format("{\"orderId\":\"%s\",\"userId\":\"%s\"}", orderId, userId);
                operations.send("topic.order", orderId, orderMessage);

                // 步骤 3: 发送通知消息
                String notifyMessage = String.format("订单 %s 创建成功，请处理", orderId);
                operations.send("topic.notify", userId, notifyMessage);

                log.info("【事务处理】所有操作完成，等待提交");

                return true;
            } catch (Exception e) {
                log.error("【事务处理】失败，触发回滚", e);
                throw e;
            }
        });
    }
}
