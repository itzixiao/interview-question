package cn.itzixiao.interview.openfeign.client;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.openfeign.dto.OrderDTO;
import cn.itzixiao.interview.openfeign.fallback.OrderClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务 Feign 客户端
 * <p>
 * 演示使用 fallback（与 UserClient 的 fallbackFactory 区分）
 *
 * @author itzixiao
 * @since 1.0
 */
@FeignClient(
        name = "interview-provider",
        path = "/api/orders",
        fallback = OrderClientFallback.class  // 使用 fallback（无法获取异常信息）
)
public interface OrderClient {

    /**
     * 根据ID查询订单
     *
     * @param id 订单ID
     * @return 订单信息
     */
    @GetMapping("/{id}")
    Result<OrderDTO> getById(@PathVariable("id") Long id);

    /**
     * 根据订单号查询
     *
     * @param orderNo 订单号
     * @return 订单信息
     */
    @GetMapping("/orderNo/{orderNo}")
    Result<OrderDTO> getByOrderNo(@PathVariable("orderNo") String orderNo);

    /**
     * 查询用户订单列表
     *
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    Result<List<OrderDTO>> listByUserId(@PathVariable("userId") Long userId);

    /**
     * 创建订单
     *
     * @param order 订单信息
     * @return 创建结果
     */
    @PostMapping
    Result<OrderDTO> create(@RequestBody OrderDTO order);

    /**
     * 取消订单
     *
     * @param id 订单ID
     * @return 取消结果
     */
    @PostMapping("/{id}/cancel")
    Result<Void> cancel(@PathVariable("id") Long id);
}
