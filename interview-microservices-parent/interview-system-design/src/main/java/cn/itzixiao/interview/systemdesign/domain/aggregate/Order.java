package cn.itzixiao.interview.systemdesign.domain.aggregate;

import cn.itzixiao.interview.systemdesign.domain.event.OrderCreatedEvent;
import cn.itzixiao.interview.systemdesign.domain.event.OrderPaidEvent;
import cn.itzixiao.interview.systemdesign.domain.valueobject.Money;
import lombok.Getter;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合根 - 订单
 * <p>
 * 聚合根特点：
 * 1. 聚合的入口点，外部只能通过聚合根访问聚合内对象
 * 2. 维护聚合内的业务规则和一致性边界
 * 3. 负责发布领域事件
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
public class Order {

    /**
     * 订单 ID（聚合根标识）
     */
    private final String orderId;

    /**
     * 用户 ID
     */
    private final Long userId;

    /**
     * 订单金额（值对象）
     */
    private Money totalAmount;

    /**
     * 订单状态
     */
    private OrderStatus status;

    /**
     * 订单项列表
     */
    private final List<OrderItem> items;

    /**
     * 事件发布器
     */
    private transient ApplicationEventPublisher eventPublisher;

    /**
     * 私有构造函数，通过静态工厂方法创建
     */
    private Order(String orderId, Long userId) {
        this.orderId = orderId;
        this.userId = userId;
        this.status = OrderStatus.CREATED;
        this.items = new ArrayList<>();
        this.totalAmount = Money.builder()
                .currency("CNY")
                .amount(java.math.BigDecimal.ZERO)
                .build();
    }

    /**
     * 静态工厂方法 - 创建订单
     */
    public static Order createOrder(String orderId, Long userId,
                                    ApplicationEventPublisher publisher) {
        Order order = new Order(orderId, userId);
        order.eventPublisher = publisher;

        // 发布订单创建事件
        order.publishEvent(new OrderCreatedEvent(
                order, orderId, userId, 0.0, 0));

        return order;
    }

    /**
     * 添加订单项
     */
    public void addItem(OrderItem item) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 状态的订单才能添加商品");
        }

        this.items.add(item);
        recalculateTotal();
    }

    /**
     * 支付订单
     */
    public void pay(String paymentId, String paymentMethod) {
        if (this.status != OrderStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 状态的订单才能支付");
        }

        if (this.items.isEmpty()) {
            throw new IllegalStateException("订单为空，无法支付");
        }

        this.status = OrderStatus.PAID;

        // 发布订单支付事件
        publishEvent(new OrderPaidEvent(
                this, orderId, paymentId,
                totalAmount.getAmount().doubleValue(), paymentMethod));
    }

    /**
     * 取消订单
     */
    public void cancel() {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("已发货或已完成的订单不能取消");
        }

        this.status = OrderStatus.CANCELLED;
    }

    /**
     * 重新计算总金额
     */
    private void recalculateTotal() {
        java.math.BigDecimal total = items.stream()
                .map(item -> item.getPrice().getAmount()
                        .multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        this.totalAmount = Money.builder()
                .currency("CNY")
                .amount(total)
                .build();
    }

    /**
     * 发布领域事件
     */
    private void publishEvent(Object event) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(event);
        }
    }

    /**
     * 订单状态枚举
     */
    public enum OrderStatus {
        CREATED,      // 已创建
        PAID,         // 已支付
        SHIPPED,      // 已发货
        COMPLETED,    // 已完成
        CANCELLED     // 已取消
    }
}
