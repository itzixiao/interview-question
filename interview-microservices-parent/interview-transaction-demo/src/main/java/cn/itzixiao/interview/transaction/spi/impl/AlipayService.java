package cn.itzixiao.interview.transaction.spi.impl;

import cn.itzixiao.interview.transaction.spi.PaymentService;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付实现
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
@Component
public class AlipayService implements PaymentService {
    
    @Override
    public String getPaymentName() {
        return "支付宝支付";
    }
    
    @Override
    public String pay(String orderId, double amount) {
        return String.format("[支付宝] 订单 %s 支付成功，金额：￥%.2f", orderId, amount);
    }
}
