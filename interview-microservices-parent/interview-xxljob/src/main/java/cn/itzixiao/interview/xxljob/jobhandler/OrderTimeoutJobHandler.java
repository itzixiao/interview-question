package cn.itzixiao.interview.xxljob.jobhandler;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时取消任务处理器
 * 演示业务场景：自动取消超时未支付的订单
 *
 * @author itzixiao
 * @date 2026-03-19
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderTimeoutJobHandler {

    // 模拟服务依赖
    // private final OrderService orderService;
    // private final PaymentService paymentService;

    /**
     * 取消超时未支付订单
     * 调度策略：每5分钟执行一次
     * Cron表达式：0 0/5 * * * ?
     */
    @XxlJob("cancelTimeoutOrderJob")
    public void cancelTimeoutOrderJob() {
        XxlJobHelper.log("开始执行订单超时取消任务");
        log.info("开始执行订单超时取消任务");

        try {
            // 1. 查询超时订单（创建时间超过15分钟且未支付）
            LocalDateTime timeoutTime = LocalDateTime.now().minusMinutes(15);
            XxlJobHelper.log("查询 {} 之前的超时订单", timeoutTime);

            // 模拟查询到的超时订单
            // List<Order> timeoutOrders = orderService.findTimeoutOrders(timeoutTime);
            int timeoutOrderCount = 10; // 模拟10个超时订单
            XxlJobHelper.log("查询到 {} 个超时订单", timeoutOrderCount);

            // 2. 批量取消订单
            int successCount = 0;
            int failCount = 0;

            for (int i = 0; i < timeoutOrderCount; i++) {
                try {
                    // 模拟取消订单
                    // orderService.cancelOrder(order.getId());
                    // 如果已预占库存，释放库存
                    // 如果已预占优惠券，释放优惠券

                    successCount++;
                    XxlJobHelper.log("订单 {} 取消成功", "ORDER_" + i);

                } catch (Exception e) {
                    failCount++;
                    XxlJobHelper.log("订单 {} 取消失败: {}", "ORDER_" + i, e.getMessage());
                    log.error("取消订单失败", e);
                }
            }

            XxlJobHelper.log("任务执行完成: 成功 {}, 失败 {}", successCount, failCount);
            log.info("订单超时取消任务完成: 成功 {}, 失败 {}", successCount, failCount);

        } catch (Exception e) {
            XxlJobHelper.log("任务执行异常: {}", e.getMessage());
            log.error("订单超时取消任务异常", e);
            // 返回失败，触发重试
            XxlJobHelper.handleFail("任务执行异常: " + e.getMessage());
        }
    }
}
