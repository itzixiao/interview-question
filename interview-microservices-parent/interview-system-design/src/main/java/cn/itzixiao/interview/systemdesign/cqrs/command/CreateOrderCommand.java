package cn.itzixiao.interview.systemdesign.cqrs.command;

import lombok.Builder;
import lombok.Data;

/**
 * 命令 - 创建订单命令
 * <p>
 * CQRS（Command Query Responsibility Segregation）- 命令查询职责分离
 * <p>
 * 核心思想：
 * 1. 写模型（Command）- 负责业务逻辑和数据验证
 * 2. 读模型（Query）- 负责高效查询和展示
 * 3. 读写分离 - 可以独立优化和扩展
 * <p>
 * 适用场景：
 * 1. 高并发系统 - 读写比例悬殊
 * 2. 复杂业务逻辑 - 写操作需要复杂验证
 * 3. 性能要求高 - 需要独立的读写优化
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
@Builder
public class CreateOrderCommand {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 商品 ID 列表
     */
    private String productIds;

    /**
     * 收货地址
     */
    private String address;

    /**
     * 备注
     */
    private String remark;
}
