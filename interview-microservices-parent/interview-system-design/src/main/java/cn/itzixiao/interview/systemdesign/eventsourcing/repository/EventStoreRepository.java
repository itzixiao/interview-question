package cn.itzixiao.interview.systemdesign.eventsourcing.repository;

import cn.itzixiao.interview.systemdesign.eventsourcing.aggregate.Account;
import cn.itzixiao.interview.systemdesign.eventsourcing.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件存储仓库
 * <p>
 * 职责：
 * 1. 保存事件到事件存储（Event Store）
 * 2. 从事件存储加载历史事件
 * 3. 支持事件的追加写入
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Repository
public class EventStoreRepository {

    /**
     * 模拟事件存储（实际应该使用数据库）
     * Key: aggregateId, Value: 事件列表
     */
    private static final Map<String, List<DomainEvent>> EVENT_STORE =
            new ConcurrentHashMap<>();

    /**
     * 保存账户的未提交事件
     */
    public void save(Account account) {
        String aggregateId = account.getAccountId();
        List<DomainEvent> events = account.getUncommittedEvents();

        if (events.isEmpty()) {
            log.debug("没有需要保存的事件：aggregateId={}", aggregateId);
            return;
        }

        // 获取或创建事件列表
        List<DomainEvent> existingEvents = EVENT_STORE.computeIfAbsent(
                aggregateId, k -> new ArrayList<>());

        // 追加新事件
        existingEvents.addAll(events);

        log.info("保存事件成功：aggregateId={}, eventCount={}, currentVersion={}",
                aggregateId, events.size(), account.getVersion());
    }

    /**
     * 从事件存储加载所有历史事件
     */
    public List<DomainEvent> loadEvents(String aggregateId) {
        List<DomainEvent> events = EVENT_STORE.get(aggregateId);

        if (events == null || events.isEmpty()) {
            log.warn("未找到事件：aggregateId={}", aggregateId);
            return new ArrayList<>();
        }

        log.info("加载事件成功：aggregateId={}, totalEvents={}",
                aggregateId, events.size());

        return events;
    }

    /**
     * 通过事件回放重建聚合根
     */
    public Account reconstructAccount(String aggregateId) {
        List<DomainEvent> events = loadEvents(aggregateId);

        if (events.isEmpty()) {
            throw new IllegalStateException("无法重建账户：事件流为空");
        }

        // 使用事件回放重建状态
        Account account = Account.reconstructFromHistory(events);

        log.info("重建账户成功：accountId={}, balance={}, version={}",
                account.getAccountId(), account.getBalance(), account.getVersion());

        return account;
    }
}
