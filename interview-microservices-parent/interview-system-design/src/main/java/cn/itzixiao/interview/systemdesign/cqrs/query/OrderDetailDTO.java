package cn.itzixiao.interview.systemdesign.cqrs.query;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 查询结果 - 订单详情 DTO（读模型）
 * 
 * 读模型特点：
 * 1. 扁平化结构 - 便于展示
 * 2. 包含冗余数据 - 减少关联查询
 * 3. 针对查询优化 - 可以使用 NoSQL、ES 等
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
@Builder
public class OrderDetailDTO {
    
    /**
     * 订单 ID
     */
    private String orderId;
    
    /**
     * 用户 ID
     */
    private Long userId;
    
    /**
     * 用户名（冗余字段，方便展示）
     */
    private String username;
    
    /**
     * 订单状态
     */
    private String status;
    
    /**
     * 订单总金额
     */
    private Double totalAmount;
    
    /**
     * 商品列表
     */
    private List<OrderItemDTO> items;
    
    /**
     * 收货地址
     */
    private String address;
    
    /**
     * 创建时间
     */
    private String createTime;
}
