package cn.itzixiao.interview.systemdesign.domain.valueobject;

import lombok.Builder;
import lombok.Value;

/**
 * 值对象 - 地址
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Value
@Builder
public class Address {
    
    /**
     * 省/州
     */
    String province;
    
    /**
     * 城市
     */
    String city;
    
    /**
     * 区/县
     */
    String district;
    
    /**
     * 详细地址
     */
    String street;
    
    /**
     * 邮政编码
     */
    String zipCode;
    
    /**
     * 私有构造函数
     */
    private Address(String province, String city, String district, String street, String zipCode) {
        if (province == null || province.trim().isEmpty()) {
            throw new IllegalArgumentException("省份不能为空");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("城市不能为空");
        }
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("详细地址不能为空");
        }
        
        this.province = province;
        this.city = city;
        this.district = district;
        this.street = street;
        this.zipCode = zipCode;
    }
    
    /**
     * 格式化完整地址
     */
    public String format() {
        return String.format("%s%s%s%s", province, city, district, street);
    }
}
