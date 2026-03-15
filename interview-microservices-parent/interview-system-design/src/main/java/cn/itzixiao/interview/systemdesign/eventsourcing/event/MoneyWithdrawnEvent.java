package cn.itzixiao.interview.systemdesign.eventsourcing.event;

import lombok.Getter;
import lombok.ToString;

/**
 * 取款事件
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
@ToString
public class MoneyWithdrawnEvent extends DomainEvent {
    
    /**
     * 取款金额
     */
    private final Double amount;
    
    /**
     * 取款原因
     */
    private final String reason;
    
    /**
     * 取款后余额
     */
    private final Double balanceAfterWithdraw;
    
    public MoneyWithdrawnEvent(String aggregateId, Long version, 
                               Double amount, String reason, 
                               Double balanceAfterWithdraw) {
        super(aggregateId, version);
        this.amount = amount;
        this.reason = reason;
        this.balanceAfterWithdraw = balanceAfterWithdraw;
    }
}
