package cn.itzixiao.interview.openfeign.fallback;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.openfeign.client.OrderClient;
import cn.itzixiao.interview.openfeign.dto.OrderDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * OrderClient 降级实现类
 * <p>
 * 使用 fallback 方式（与 UserClientFallbackFactory 对比）
 * 直接实现 FeignClient 接口，无法获取异常信息
 *
 * @author itzixiao
 * @since 1.0
 */
@Slf4j
@Component
public class OrderClientFallback implements OrderClient {

    @Override
    public Result<OrderDTO> getById(Long id) {
        log.warn("OrderClient.getById 降级处理, id={}", id);
        return Result.error(503, "订单服务暂不可用");
    }

    @Override
    public Result<OrderDTO> getByOrderNo(String orderNo) {
        log.warn("OrderClient.getByOrderNo 降级处理, orderNo={}", orderNo);
        return Result.error(503, "订单服务暂不可用");
    }

    @Override
    public Result<List<OrderDTO>> listByUserId(Long userId) {
        log.warn("OrderClient.listByUserId 降级处理, userId={}", userId);
        // 返回空列表
        return Result.success(Collections.emptyList());
    }

    @Override
    public Result<OrderDTO> create(OrderDTO order) {
        log.warn("OrderClient.create 降级处理");
        return Result.error(503, "订单服务暂不可用，请稍后重试");
    }

    @Override
    public Result<Void> cancel(Long id) {
        log.warn("OrderClient.cancel 降级处理, id={}", id);
        return Result.error(503, "订单服务暂不可用，请稍后重试");
    }
}
