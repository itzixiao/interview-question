package cn.itzixiao.interview.systemdesign.eventsourcing.event;

import lombok.Getter;
import lombok.ToString;

/**
 * 存款事件
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
@ToString
public class MoneyDepositedEvent extends DomainEvent {

    /**
     * 存款金额
     */
    private final Double amount;

    /**
     * 存款后余额
     */
    private final Double balanceAfterDeposit;

    public MoneyDepositedEvent(String aggregateId, Long version,
                               Double amount, Double balanceAfterDeposit) {
        super(aggregateId, version);
        this.amount = amount;
        this.balanceAfterDeposit = balanceAfterDeposit;
    }
}
