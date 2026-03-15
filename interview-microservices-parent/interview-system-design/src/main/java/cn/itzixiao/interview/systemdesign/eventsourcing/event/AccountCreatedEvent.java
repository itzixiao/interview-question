package cn.itzixiao.interview.systemdesign.eventsourcing.event;

import lombok.Getter;
import lombok.ToString;

/**
 * 账户创建事件
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
@ToString
public class AccountCreatedEvent extends DomainEvent {
    
    /**
     * 用户名
     */
    private final String username;
    
    /**
     * 初始余额
     */
    private final Double initialBalance;
    
    public AccountCreatedEvent(String aggregateId, Long version, 
                               String username, Double initialBalance) {
        super(aggregateId, version);
        this.username = username;
        this.initialBalance = initialBalance;
    }
}
