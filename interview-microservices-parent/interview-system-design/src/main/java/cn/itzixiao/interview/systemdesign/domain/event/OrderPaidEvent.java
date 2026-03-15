package cn.itzixiao.interview.systemdesign.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * 领域事件 - 订单支付成功事件
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
public class OrderPaidEvent extends ApplicationEvent {
    
    /**
     * 订单 ID
     */
    private final String orderId;
    
    /**
     * 支付流水号
     */
    private final String paymentId;
    
    /**
     * 支付金额
     */
    private final Double paymentAmount;
    
    /**
     * 支付方式（ALIPAY/WECHAT/CARD）
     */
    private final String paymentMethod;
    
    /**
     * 事件发生时间
     */
    private final Instant occurredOn;
    
    public OrderPaidEvent(Object source, String orderId, String paymentId, 
                          Double paymentAmount, String paymentMethod) {
        super(source);
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.paymentAmount = paymentAmount;
        this.paymentMethod = paymentMethod;
        this.occurredOn = Instant.now();
    }
}
