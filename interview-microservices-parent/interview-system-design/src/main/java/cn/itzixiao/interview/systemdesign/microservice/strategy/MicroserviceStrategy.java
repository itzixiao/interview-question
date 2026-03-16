package cn.itzixiao.interview.systemdesign.microservice.strategy;

/**
 * 微服务拆分策略接口
 * <p>
 * 微服务拆分原则：
 * 1. 单一职责原则 - 每个服务只负责一个业务领域
 * 2. 高内聚低耦合 - 相关功能放在一起
 * 3. 数据一致性边界 - 同一聚合的数据放在同一服务
 * 4. 可独立部署和扩展 - 服务可以独立部署和伸缩
 *
 * @author itzixiao
 * @date 2026-03-15
 */
public interface MicroserviceStrategy {

    /**
     * 获取服务名称
     */
    String getServiceName();

    /**
     * 获取服务职责描述
     */
    String getResponsibility();

    /**
     * 获取包含的领域对象
     */
    String[] getDomainObjects();
}
