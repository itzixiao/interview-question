package cn.itzixiao.interview.provider.controller;

import cn.itzixiao.interview.common.result.Result;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 订单服务接口（服务提供者）
 * <p>
 * 提供订单相关的 REST API，供 Feign 客户端调用
 *
 * @author itzixiao
 * @since 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    /**
     * 模拟数据库存储
     */
    private final Map<Long, OrderDTO> orderStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 初始化一些测试数据
     */
    public OrderController() {
        // 初始化测试数据
        OrderDTO order1 = OrderDTO.builder()
                .id(idGenerator.getAndIncrement())
                .orderNo(generateOrderNo())
                .userId(1L)
                .totalAmount(new BigDecimal("199.00"))
                .status(1)
                .items(Arrays.asList(
                        OrderDTO.OrderItemDTO.builder()
                                .productId(1L)
                                .productName("Java 编程思想")
                                .price(new BigDecimal("99.00"))
                                .quantity(2)
                                .build()
                ))
                .createTime(LocalDateTime.now())
                .build();
        orderStorage.put(order1.getId(), order1);

        log.info("OrderController 初始化完成，已创建 {} 条测试数据", orderStorage.size());
    }

    /**
     * 根据ID查询订单
     */
    @GetMapping("/{id}")
    public Result<OrderDTO> getById(@PathVariable("id") Long id) {
        log.info("收到查询订单请求: id={}", id);
        OrderDTO order = orderStorage.get(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 根据订单号查询
     */
    @GetMapping("/orderNo/{orderNo}")
    public Result<OrderDTO> getByOrderNo(@PathVariable("orderNo") String orderNo) {
        log.info("收到查询订单请求: orderNo={}", orderNo);
        OrderDTO order = orderStorage.values().stream()
                .filter(o -> orderNo.equals(o.getOrderNo()))
                .findFirst()
                .orElse(null);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 查询用户订单列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<OrderDTO>> listByUserId(@PathVariable("userId") Long userId) {
        log.info("收到查询用户订单请求: userId={}", userId);
        List<OrderDTO> orders = orderStorage.values().stream()
                .filter(o -> userId.equals(o.getUserId()))
                .collect(Collectors.toList());
        return Result.success(orders);
    }

    /**
     * 创建订单
     */
    @PostMapping
    public Result<OrderDTO> create(@RequestBody OrderDTO order) {
        log.info("收到创建订单请求: {}", order);
        order.setId(idGenerator.getAndIncrement());
        order.setOrderNo(generateOrderNo());
        order.setCreateTime(LocalDateTime.now());
        if (order.getStatus() == null) {
            order.setStatus(0);  // 待支付
        }
        orderStorage.put(order.getId(), order);
        return Result.success(order);
    }

    /**
     * 取消订单
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable("id") Long id) {
        log.info("收到取消订单请求: id={}", id);
        OrderDTO order = orderStorage.get(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        if (order.getStatus() != 0) {
            return Result.error(400, "只有待支付订单可以取消");
        }
        order.setStatus(4);  // 已取消
        return Result.success(null);
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        return "ORD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", idGenerator.get());
    }

    /**
     * 订单 DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDTO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String orderNo;
        private Long userId;
        private BigDecimal totalAmount;
        private Integer status;
        private List<OrderItemDTO> items;
        private LocalDateTime createTime;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderItemDTO implements Serializable {
            private static final long serialVersionUID = 1L;
            private Long productId;
            private String productName;
            private BigDecimal price;
            private Integer quantity;
        }
    }
}
