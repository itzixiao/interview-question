package cn.itzixiao.interview.systemdesign.eventsourcing.aggregate;

import cn.itzixiao.interview.systemdesign.eventsourcing.event.AccountCreatedEvent;
import cn.itzixiao.interview.systemdesign.eventsourcing.event.DomainEvent;
import cn.itzixiao.interview.systemdesign.eventsourcing.event.MoneyDepositedEvent;
import cn.itzixiao.interview.systemdesign.eventsourcing.event.MoneyWithdrawnEvent;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * 账户聚合根（事件溯源模式）
 * <p>
 * 特点：
 * 1. 不保存当前状态，只保存事件流
 * 2. 通过事件回放（replay）重建状态
 * 3. 所有变更都记录为事件
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
public class Account {

    /**
     * 账户 ID（聚合根标识）
     */
    private final String accountId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 当前余额
     */
    private Double balance;

    /**
     * 版本号（用于乐观锁）
     */
    private Long version;

    /**
     * 未提交的事件列表
     */
    private final List<DomainEvent> uncommittedEvents;

    private Account(String accountId) {
        this.accountId = accountId;
        this.uncommittedEvents = new ArrayList<>();
        this.version = 0L;
        this.balance = 0.0;
    }

    /**
     * 静态工厂方法 - 创建账户
     */
    public static Account createAccount(String accountId, String username,
                                        Double initialBalance) {
        Account account = new Account(accountId);

        // 创建账户创建事件
        AccountCreatedEvent event = new AccountCreatedEvent(
                accountId, 1L, username, initialBalance);

        account.applyEvent(event);
        account.uncommittedEvents.add(event);

        return account;
    }

    /**
     * 存款
     */
    public void deposit(Double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("存款金额必须大于 0");
        }

        Long nextVersion = version + 1;
        Double newBalance = this.balance + amount;

        MoneyDepositedEvent event = new MoneyDepositedEvent(
                accountId, nextVersion, amount, newBalance);

        applyEvent(event);
        uncommittedEvents.add(event);
    }

    /**
     * 取款
     */
    public void withdraw(Double amount, String reason) {
        if (amount <= 0) {
            throw new IllegalArgumentException("取款金额必须大于 0");
        }

        if (amount > this.balance) {
            throw new IllegalStateException("余额不足");
        }

        Long nextVersion = version + 1;
        Double newBalance = this.balance - amount;

        MoneyWithdrawnEvent event = new MoneyWithdrawnEvent(
                accountId, nextVersion, amount, reason, newBalance);

        applyEvent(event);
        uncommittedEvents.add(event);
    }

    /**
     * 应用事件（更新状态）
     */
    private void applyEvent(DomainEvent event) {
        if (event instanceof AccountCreatedEvent) {
            AccountCreatedEvent e = (AccountCreatedEvent) event;
            this.username = e.getUsername();
            this.balance = e.getInitialBalance();
            this.version = e.getVersion();
        } else if (event instanceof MoneyDepositedEvent) {
            MoneyDepositedEvent e = (MoneyDepositedEvent) event;
            this.balance = e.getBalanceAfterDeposit();
            this.version = e.getVersion();
        } else if (event instanceof MoneyWithdrawnEvent) {
            MoneyWithdrawnEvent e = (MoneyWithdrawnEvent) event;
            this.balance = e.getBalanceAfterWithdraw();
            this.version = e.getVersion();
        }
    }

    /**
     * 从事件流重建状态（事件回放）
     */
    public static Account reconstructFromHistory(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            throw new IllegalArgumentException("事件流不能为空");
        }

        Account account = new Account(events.get(0).getAggregateId());

        // 按顺序应用所有事件，重建状态
        for (DomainEvent event : events) {
            account.applyEvent(event);
        }

        return account;
    }

    /**
     * 获取未提交的事件
     */
    public List<DomainEvent> getUncommittedEvents() {
        List<DomainEvent> events = new ArrayList<>(uncommittedEvents);
        uncommittedEvents.clear();
        return events;
    }
}
