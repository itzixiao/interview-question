package cn.itzixiao.interview.transaction.spi;

/**
 * SPI（Service Provider Interface）接口定义
 * 
 * SPI 是 Java 提供的一种服务发现机制，允许第三方实现接口并通过配置文件注册
 * 核心特点：
 * 1. 解耦：接口定义与实现分离
 * 2. 可扩展：无需修改源码即可添加新实现
 * 3. 动态加载：运行时通过 ServiceLoader 加载实现类
 * 
 * 应用场景：
 * - JDBC 驱动加载
 * - SLF4J 日志框架
 * - Dubbo SPI 扩展
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
public interface PaymentService {
    
    /**
     * 支付方式名称
     * @return 支付方式
     */
    String getPaymentName();
    
    /**
     * 支付处理
     * @param orderId 订单 ID
     * @param amount 金额
     * @return 处理结果
     */
    String pay(String orderId, double amount);
}
