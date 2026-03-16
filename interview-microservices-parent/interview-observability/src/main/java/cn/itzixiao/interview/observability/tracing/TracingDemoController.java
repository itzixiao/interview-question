package cn.itzixiao.interview.observability.tracing;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 链路追踪演示 Controller - SkyWalking / Sleuth
 * <p>
 * 链路追踪原理：
 * 1. Trace ID - 整个请求链路的唯一标识
 * 2. Span ID - 单个操作的标识
 * 3. Parent Span ID - 父子操作关系
 * <p>
 * SkyWalking 功能：
 * 1. 自动探针 - 无侵入式采集
 * 2. 拓扑图生成 - 服务依赖可视化
 * 3. 性能分析 - 慢调用定位
 * 4. 指标监控 - QPS、响应时间等
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Slf4j
@RestController
@RequestMapping("/api/tracing")
public class TracingDemoController {

    /**
     * 模拟订单查询链路
     */
    @GetMapping("/order/{orderId}")
    public String queryOrder(@PathVariable String orderId) {
        log.info("开始查询订单：{}", orderId);

        // Span 1: 查询用户信息
        String userInfo = queryUserInfo(orderId);

        // Span 2: 查询订单详情
        String orderDetail = queryOrderDetail(orderId);

        // Span 3: 计算推荐商品
        String recommendation = calculateRecommendation(orderId);

        return String.format("订单 %s 查询完成：%s, %s, %s",
                orderId, userInfo, orderDetail, recommendation);
    }

    /**
     * 模拟用户服务调用
     */
    private String queryUserInfo(String orderId) {
        log.info("调用用户服务查询信息");
        try {
            Thread.sleep(50); // 模拟远程调用延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "用户信息";
    }

    /**
     * 模拟订单服务调用
     */
    private String queryOrderDetail(String orderId) {
        log.info("调用订单服务查询详情");
        try {
            Thread.sleep(80); // 模拟远程调用延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "订单详情";
    }

    /**
     * 模拟推荐服务调用
     */
    private String calculateRecommendation(String orderId) {
        log.info("调用推荐服务计算推荐");
        try {
            Thread.sleep(100); // 模拟复杂计算
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "推荐商品";
    }
}
