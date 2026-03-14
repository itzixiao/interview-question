package cn.itzixiao.interview.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 包含各种队列、交换机、死信队列、延迟队列等配置
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 基础队列配置 ====================

    /**
     * 普通队列
     */
    @Bean
    public Queue simpleQueue() {
        return new Queue("queue.simple", true);
    }

    /**
     * 工作队列（用于演示工作模式）
     */
    @Bean
    public Queue workQueue() {
        return new Queue("queue.work", true);
    }

    /**
     * 发布订阅队列 1
     */
    @Bean
    public Queue fanoutQueue1() {
        return new Queue("queue.fanout.1", true);
    }

    /**
     * 发布订阅队列 2
     */
    @Bean
    public Queue fanoutQueue2() {
        return new Queue("queue.fanout.2", true);
    }

    /**
     * 路由队列 1
     */
    @Bean
    public Queue routingQueue1() {
        return new Queue("queue.routing.1", true);
    }

    /**
     * 路由队列 2
     */
    @Bean
    public Queue routingQueue2() {
        return new Queue("queue.routing.2", true);
    }

    /**
     * 主题队列 1
     */
    @Bean
    public Queue topicQueue1() {
        return new Queue("queue.topic.1", true);
    }

    /**
     * 主题队列 2
     */
    @Bean
    public Queue topicQueue2() {
        return new Queue("queue.topic.2", true);
    }

    // ==================== 交换机配置 ====================

    /**
     * 直连交换机
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("exchange.direct");
    }

    /**
     * 扇形交换机（发布订阅）
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("exchange.fanout");
    }

    /**
     * 主题交换机
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("exchange.topic");
    }

    // ==================== 绑定配置 ====================

    /**
     * 直连交换机绑定
     */
    @Bean
    public Binding directBinding1(@Qualifier("directExchange") DirectExchange exchange,
                                   @Qualifier("simpleQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("routing.key.1");
    }

    @Bean
    public Binding directBinding2(@Qualifier("directExchange") DirectExchange exchange,
                                   @Qualifier("workQueue") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("routing.key.2");
    }

    /**
     * 扇形交换机绑定
     */
    @Bean
    public Binding fanoutBinding1(@Qualifier("fanoutExchange") FanoutExchange exchange,
                                   @Qualifier("fanoutQueue1") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    @Bean
    public Binding fanoutBinding2(@Qualifier("fanoutExchange") FanoutExchange exchange,
                                   @Qualifier("fanoutQueue2") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    /**
     * 路由绑定
     */
    @Bean
    public Binding routingBinding1(@Qualifier("directExchange") DirectExchange exchange,
                                    @Qualifier("routingQueue1") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("routing.*.1");
    }

    @Bean
    public Binding routingBinding2(@Qualifier("directExchange") DirectExchange exchange,
                                    @Qualifier("routingQueue2") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("routing.*.2");
    }

    /**
     * 主题绑定
     */
    @Bean
    public Binding topicBinding1(@Qualifier("topicExchange") TopicExchange exchange,
                                  @Qualifier("topicQueue1") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("topic.#");
    }

    @Bean
    public Binding topicBinding2(@Qualifier("topicExchange") TopicExchange exchange,
                                  @Qualifier("topicQueue2") Queue queue) {
        return BindingBuilder.bind(queue).to(exchange).with("*.important");
    }

    // ==================== 死信队列配置 ====================

    /**
     * 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("queue.deadletter").build();
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("exchange.deadletter");
    }

    /**
     * 死信队列绑定
     */
    @Bean
    public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
                                      @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("deadletter.routingkey");
    }

    /**
     * 支持死信队列的普通队列
     */
    @Bean
    public Queue normalQueueWithDLX() {
        return QueueBuilder.durable("queue.normal.with.dlx")
                .withArgument("x-dead-letter-exchange", "exchange.deadletter")
                .withArgument("x-dead-letter-routing-key", "deadletter.routingkey")
                .build();
    }

    /**
     * 绑定到直连交换机
     */
    @Bean
    public Binding normalQueueWithDLXBinding(@Qualifier("normalQueueWithDLX") Queue queue,
                                              @Qualifier("directExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("queue.dlx");
    }

    // ==================== 延迟队列配置 ====================

    /**
     * 延迟队列（使用 TTL+ 死信队列实现）
     */
    @Bean
    public Queue delayQueue() {
        return QueueBuilder.durable("queue.delay")
                .withArgument("x-dead-letter-exchange", "exchange.deadletter")
                .withArgument("x-dead-letter-routing-key", "delay.routingkey")
                .withArgument("x-message-ttl", 60000) // 消息 TTL 60 秒
                .build();
    }

    /**
     * 延迟队列绑定
     */
    @Bean
    public Binding delayQueueBinding(@Qualifier("delayQueue") Queue delayQueue,
                                      @Qualifier("directExchange") DirectExchange exchange) {
        return BindingBuilder.bind(delayQueue).to(exchange).with("queue.delay");
    }

    /**
     * 延迟死信队列
     */
    @Bean
    public Queue delayDeadLetterQueue() {
        return QueueBuilder.durable("queue.delay.deadletter").build();
    }

    /**
     * 延迟死信队列绑定
     */
    @Bean
    public Binding delayDeadLetterBinding(@Qualifier("delayDeadLetterQueue") Queue queue,
                                           @Qualifier("deadLetterExchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with("delay.routingkey");
    }

    // ==================== 优先级队列配置 ====================

    /**
     * 优先级队列（最大优先级 10）
     */
    @Bean
    public Queue priorityQueue() {
        return QueueBuilder.durable("queue.priority")
                .withArgument("x-max-priority", 10)
                .build();
    }

    /**
     * 优先级队列绑定
     */
    @Bean
    public Binding priorityQueueBinding(@Qualifier("priorityQueue") Queue priorityQueue,
                                         @Qualifier("directExchange") DirectExchange exchange) {
        return BindingBuilder.bind(priorityQueue).to(exchange).with("queue.priority");
    }

    // ==================== RabbitTemplate 配置 ====================

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          @Qualifier("jsonMessageConverter") MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        // 开启 mandatory 标志，确保消息无法路由时触发 ReturnCallback
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }
}
