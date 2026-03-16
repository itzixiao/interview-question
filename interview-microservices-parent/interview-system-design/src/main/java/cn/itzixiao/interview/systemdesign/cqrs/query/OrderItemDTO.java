package cn.itzixiao.interview.systemdesign.cqrs.query;

import lombok.Builder;
import lombok.Data;

/**
 * 查询结果项 - 订单商品 DTO
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
@Builder
public class OrderItemDTO {

    /**
     * 商品 ID
     */
    private String productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品图片 URL（冗余）
     */
    private String productImage;

    /**
     * 单价
     */
    private Double price;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 小计金额
     */
    private Double subtotal;
}
