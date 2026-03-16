package cn.itzixiao.interview.systemdesign.microservice.strategy;

import org.springframework.stereotype.Component;

/**
 * 订单微服务拆分策略
 * <p>
 * 按业务领域拆分：
 * - 订单服务：负责订单管理
 * - 用户服务：负责用户管理
 * - 商品服务：负责商品管理
 * - 支付服务：负责支付处理
 *
 * @author itzixiao
 * @date 2026-03-15
 */
@Component
public class OrderServiceStrategy implements MicroserviceStrategy {

    @Override
    public String getServiceName() {
        return "order-service";
    }

    @Override
    public String getResponsibility() {
        return "订单全生命周期管理（创建、支付、发货、取消、完成）";
    }

    @Override
    public String[] getDomainObjects() {
        return new String[]{
                "Order（聚合根）",
                "OrderItem（实体）",
                "OrderAddress（值对象）",
                "OrderStatus（枚举）"
        };
    }
}
