package cn.itzixiao.interview.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka 配置类
 *
 * <p>包含生产者、消费者、管理员的完整配置</p>
 *
 * <h3>核心配置说明：</h3>
 * <ul>
 *     <li><b>生产者配置</b>: acks=all（最高可靠性）、retries=3（重试机制）</li>
 *     <li><b>消费者配置</b>: auto.offset.reset=earliest（从头消费）、enable.auto.commit=false（手动提交）</li>
 *     <li><b>高吞吐优化</b>: batch.size=16384、linger.ms=1、buffer.memory=33554432</li>
 *     <li><b>SASL 认证</b>: 支持环境变量配置用户名和密码（KAFKA_USERNAME、KAFKA_PASSWORD）</li>
 * </ul>
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * 生产者配置
     *
     * @return ProducerFactory
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // 基础配置
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // 可靠性配置
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");  // 所有 ISR 副本确认
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);   // 重试次数
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // 高吞吐优化配置
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);  // 批量大小：16KB
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);       // 等待更多消息加入批次
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 缓冲区大小：32MB

        // 压缩配置
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");  // lz4 压缩

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka 模板（用于发送消息）
     *
     * @return KafkaTemplate
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * 消费者配置
     *
     * @return ConsumerFactory
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // 基础配置
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // 消费者组配置
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-demo-group");

        // Offset 管理配置
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // 从头开始
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);      // 关闭自动提交

        // 会话超时配置
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        // 单次拉取数量
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * 并发监听容器工厂（支持批量消费）
     *
     * @return ConcurrentKafkaListenerContainerFactory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // 批量消费配置
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // 并发度配置
        factory.setConcurrency(3);

        return factory;
    }

    /**
     * Kafka 管理员（用于创建/删除 Topic）
     *
     * @return KafkaAdmin
     */
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * 示例 Topic：订单主题（3 个分区，2 个副本）
     *
     * @return NewTopic
     */
    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name("topic.order")
                .partitions(3)      // 3 个分区（提高并行度）
                .replicas(2)        // 2 个副本（保证高可用）
                .build();
    }

    /**
     * 日志收集 Topic（10 个分区，用于高吞吐场景）
     *
     * @return NewTopic
     */
    @Bean
    public NewTopic logTopic() {
        return TopicBuilder.name("topic.log")
                .partitions(10)     // 10 个分区（提高写入吞吐）
                .replicas(2)
                .build();
    }
}
