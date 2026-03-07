package cn.itzixiao.interview.openfeign.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单信息 DTO
 * <p>
 * 用于 OpenFeign 远程调用示例
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 订单状态 (0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消)
     */
    private Integer status;

    /**
     * 订单明细
     */
    private List<OrderItemDTO> items;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 订单明细 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 商品ID
         */
        private Long productId;

        /**
         * 商品名称
         */
        private String productName;

        /**
         * 单价
         */
        private BigDecimal price;

        /**
         * 数量
         */
        private Integer quantity;
    }
}
