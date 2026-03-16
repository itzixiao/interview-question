package cn.itzixiao.interview.transaction.spi.impl;

import cn.itzixiao.interview.transaction.spi.PaymentService;
import org.springframework.stereotype.Component;

/**
 * 微信支付实现
 *
 * @author itzixiao
 * @since 2026-03-14
 */
@Component
public class WechatPayService implements PaymentService {

    @Override
    public String getPaymentName() {
        return "微信支付";
    }

    @Override
    public String pay(String orderId, double amount) {
        return String.format("[微信] 订单 %s 支付成功，金额：￥%.2f", orderId, amount);
    }
}
