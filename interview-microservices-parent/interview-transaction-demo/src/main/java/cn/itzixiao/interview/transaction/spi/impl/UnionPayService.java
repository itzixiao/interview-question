package cn.itzixiao.interview.transaction.spi.impl;

import cn.itzixiao.interview.transaction.spi.PaymentService;

/**
 * 银联支付实现
 * 
 * @author itzixiao
 * @since 2026-03-14
 */
public class UnionPayService implements PaymentService {
    
    @Override
    public String getPaymentName() {
        return "银联支付";
    }
    
    @Override
    public String pay(String orderId, double amount) {
        return String.format("[银联] 订单 %s 支付成功，金额：￥%.2f", orderId, amount);
    }
}
