package cn.itzixiao.interview.rabbitmq.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnsCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 消息确认回调处理器
 * 处理 Publisher Confirm 和 Publisher Return
 */
@Slf4j
@Component
public class MessageConfirmHandler implements ConfirmCallback, ReturnsCallback {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 初始化回调
     */
    @PostConstruct
    public void init() {
        rabbitTemplate.setConfirmCallback(this);
        rabbitTemplate.setReturnsCallback(this);
    }

    /**
     * Publisher Confirm 回调
     * 当消息到达交换机时触发
     * 
     * @param correlationData 相关数据
     * @param ack true-成功到达交换机，false-未到达
     * @param cause 失败原因
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String correlationId = correlationData != null ? correlationData.getId() : "unknown";
        
        if (ack) {
            log.info("✅ 消息确认成功 [correlationId: {}]", correlationId);
        } else {
            log.error("❌ 消息确认失败 [correlationId: {}, 原因：{}]", correlationId, cause);
            // TODO: 可以在这里实现消息重试或记录到数据库
        }
    }

    /**
     * Publisher Return 回调
     * 当消息无法路由到任何队列时触发
     * 
     * @param returnedMessage 返回的消息
     */
    @Override
    public void returnedMessage(ReturnedMessage returnedMessage) {
        byte[] body = returnedMessage.getMessage().getBody();
        String messageBody = body != null ? new String(body) : "null";
        
        log.error("❌ 消息无法路由 [exchange: {}, routingKey: {}, replyCode: {}, replyText: {}, message: {}]",
                returnedMessage.getMessage().getMessageProperties().getReceivedExchange(),
                returnedMessage.getMessage().getMessageProperties().getReceivedRoutingKey(),
                returnedMessage.getReplyCode(),
                returnedMessage.getReplyText(),
                messageBody);
        
        // TODO: 可以在这里记录日志、发送告警或存储到数据库等待后续处理
    }
}
