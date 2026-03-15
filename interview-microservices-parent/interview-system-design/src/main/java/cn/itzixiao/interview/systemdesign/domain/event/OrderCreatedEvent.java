package cn.itzixiao.interview.systemdesign.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * 领域事件 - 订单创建事件
 * 
 * 领域事件特点：
 * 1. 描述过去发生的事情（使用过去时态）
 * 2. 不可变
 * 3. 包含事件发生时的完整上下文信息
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
public class OrderCreatedEvent extends ApplicationEvent {
    
    /**
     * 订单 ID
     */
    private final String orderId;
    
    /**
     * 用户 ID
     */
    private final Long userId;
    
    /**
     * 订单总金额
     */
    private final Double totalAmount;
    
    /**
     * 商品数量
     */
    private final Integer itemCount;
    
    /**
     * 事件发生时间
     */
    private final Instant occurredOn;
    
    public OrderCreatedEvent(Object source, String orderId, Long userId, 
                             Double totalAmount, Integer itemCount) {
        super(source);
        this.orderId = orderId;
        this.userId = userId;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
        this.occurredOn = Instant.now();
    }
}
