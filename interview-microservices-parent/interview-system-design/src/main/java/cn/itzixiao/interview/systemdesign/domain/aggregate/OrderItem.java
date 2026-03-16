package cn.itzixiao.interview.systemdesign.domain.aggregate;

import cn.itzixiao.interview.systemdesign.domain.valueobject.Money;
import lombok.Getter;

/**
 * 订单项（聚合内的实体）
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
public class OrderItem {

    /**
     * 商品 ID
     */
    private final String productId;

    /**
     * 商品名称
     */
    private final String productName;

    /**
     * 购买数量
     */
    private final Integer quantity;

    /**
     * 单价（值对象）
     */
    private final Money price;

    public OrderItem(String productId, String productName,
                     Integer quantity, Money price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
    }
}
