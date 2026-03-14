package cn.itzixiao.interview.rabbitmq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单消息模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单 ID
     */
    private String orderId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 金额
     */
    private Double amount;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 创建时间
     */
    private Long createTime;
}
