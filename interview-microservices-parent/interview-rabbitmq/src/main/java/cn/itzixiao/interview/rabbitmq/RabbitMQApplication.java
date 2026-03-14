package cn.itzixiao.interview.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RabbitMQ 实战示例启动类
 * 
 * 功能包括：
 * 1. 基础消息队列（简单队列、工作队列）
 * 2. 交换机模式（Direct、Fanout、Topic）
 * 3. 高级特性（延迟队列、死信队列、优先级队列）
 * 4. 可靠性保障（消息确认、手动 ACK、持久化、幂等性）
 * 5. 高频面试题示例代码
 */
@SpringBootApplication
public class RabbitMQApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitMQApplication.class, args);
        System.out.println("===========================================");
        System.out.println("   RabbitMQ 实战示例启动成功！");
        System.out.println("   访问地址：http://localhost:8085");
        System.out.println("   API 文档：http://localhost:8085/swagger-ui.html");
        System.out.println("===========================================");
    }
}
