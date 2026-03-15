package cn.itzixiao.interview.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Kafka 示例应用启动类
 */
@SpringBootApplication
public class KafkaApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("   Kafka 示例应用启动成功！");
        System.out.println("   访问地址：http://localhost:8086/api/kafka/demo");
        System.out.println("========================================\n");
    }
}
