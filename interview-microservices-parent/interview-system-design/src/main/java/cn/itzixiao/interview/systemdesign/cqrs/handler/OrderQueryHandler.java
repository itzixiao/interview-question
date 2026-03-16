package cn.itzixiao.interview.systemdesign.cqrs.handler;

import cn.itzixiao.interview.systemdesign.cqrs.query.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * 查询处理器 - 处理订单查询请求
 * <p>
 * 读模型职责：
 * 1. 高效查询（可以使用缓存、ES、MongoDB 等）
 * 2. 数据展示优化（扁平化、冗余字段）
 * 3. 复杂查询支持（多条件、分页、排序）
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@Component
public class OrderQueryHandler {

    /**
     * 根据订单 ID 查询详情
     */
    public OrderDetailDTO getOrderById(String orderId) {
        log.info("查询订单详情：orderId={}", orderId);

        // 模拟从读数据库（可能是 MongoDB、ES）查询
        return OrderDetailDTO.builder()
                .orderId(orderId)
                .userId(1001L)
                .username("张三")
                .status("PAID")
                .totalAmount(299.00)
                .items(new ArrayList<>())
                .address("北京市朝阳区 xxx 街道")
                .createTime("2026-03-15 10:30:00")
                .build();
    }

    /**
     * 根据用户 ID 查询订单列表
     */
    public java.util.List<OrderDetailDTO> getOrdersByUserId(Long userId, int page, int size) {
        log.info("查询用户订单列表：userId={}, page={}, size={}", userId, page, size);

        // 模拟查询列表
        java.util.List<OrderDetailDTO> orders = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            orders.add(OrderDetailDTO.builder()
                    .orderId("ORD" + (System.currentTimeMillis() + i))
                    .userId(userId)
                    .username("张三")
                    .status("PAID")
                    .totalAmount(199.00 + i * 100)
                    .items(new ArrayList<>())
                    .address("北京市朝阳区 xxx 街道")
                    .createTime("2026-03-15 10:30:00")
                    .build());
        }

        return orders;
    }
}
