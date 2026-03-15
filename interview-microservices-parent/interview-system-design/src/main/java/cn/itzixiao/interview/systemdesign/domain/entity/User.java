package cn.itzixiao.interview.systemdesign.domain.entity;

import cn.itzixiao.interview.systemdesign.domain.valueobject.Address;
import lombok.Getter;
import lombok.Setter;

/**
 * 实体 - 用户
 * 
 * 实体特点：
 * 1. 有唯一标识（ID）
 * 2. 可变性（状态可以改变）
 * 3. 有生命周期（创建、修改、删除）
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Getter
@Setter
public class User {
    
    /**
     * 唯一标识 ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 收货地址（值对象）
     */
    private Address address;
    
    public User(Long id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }
    
    /**
     * 实体的业务行为
     */
    public void updateAddress(Address newAddress) {
        if (newAddress == null) {
            throw new IllegalArgumentException("地址不能为空");
        }
        this.address = newAddress;
    }
    
    /**
     * 实体的业务行为
     */
    public void updateEmail(String newEmail) {
        if (newEmail == null || !newEmail.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
        this.email = newEmail;
    }
    
    @Override
    public boolean equals(Object obj) {
        // 实体通过 ID 判断相等
        if (this == obj) return true;
        if (!(obj instanceof User)) return false;
        User other = (User) obj;
        return this.id != null && this.id.equals(other.id);
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
