package cn.itzixiao.interview.systemdesign.eventsourcing.event;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

/**
 * 事件基类
 * 
 * 事件溯源（Event Sourcing）核心思想：
 * 1. 状态是事件的投影 - 当前状态由所有历史事件推导而来
 * 2. 事件不可变 - 已发生的事件不能修改，只能添加新事件
 * 3. 完整审计日志 - 所有变更都有记录
 * 
 * 适用场景：
 * 1. 需要完整审计 - 金融、医疗系统
 * 2. 数据追溯性强 - 需要回放历史状态
 * 3. 高并发写入 - 追加写性能更好
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
@ToString
public abstract class DomainEvent {
    
    /**
     * 事件 ID
     */
    private final String eventId;
    
    /**
     * 聚合根 ID
     */
    private final String aggregateId;
    
    /**
     * 事件发生时间
     */
    private final Instant timestamp;
    
    /**
     * 事件版本号
     */
    private final Long version;
    
    public DomainEvent(String aggregateId, Long version) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.version = version;
        this.timestamp = Instant.now();
    }
}
