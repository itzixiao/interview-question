package cn.itzixiao.interview.systemdesign.domain.valueobject;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * 值对象 - 金额
 * 
 * 值对象特点：
 * 1. 无标识性（通过属性值判断相等）
 * 2. 不可变性（创建后不能修改）
 * 3. 自验证（包含业务规则校验）
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Value
@Builder
public class Money {
    
    /**
     * 币种
     */
    String currency;
    
    /**
     * 金额数值
     */
    BigDecimal amount;
    
    /**
     * 私有构造函数
     */
    private Money(String currency, BigDecimal amount) {
        // 值对象的自验证逻辑
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金额不能为负数");
        }
        
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("币种不能为空");
        }
        
        this.currency = currency;
        this.amount = amount;
    }
    
    /**
     * 值对象的业务行为
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalStateException("不同币种不能相加");
        }
        return Money.builder()
                .currency(this.currency)
                .amount(this.amount.add(other.amount))
                .build();
    }
    
    /**
     * 值对象的业务行为
     */
    public Money multiply(int multiplier) {
        return Money.builder()
                .currency(this.currency)
                .amount(this.amount.multiply(new BigDecimal(multiplier)))
                .build();
    }
}
