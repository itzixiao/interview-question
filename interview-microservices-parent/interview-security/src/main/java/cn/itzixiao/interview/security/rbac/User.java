package cn.itzixiao.interview.security.rbac;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户实体
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户 ID
     */
    private Long id;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（加密存储）
     */
    private String password;
    
    /**
     * 邮箱
     */
    private String email;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 用户状态（NORMAL-正常，LOCKED-锁定，DISABLED-禁用）
     */
    private UserStatus status;
    
    /**
     * 用户拥有的角色列表
     */
    private List<Role> roles;
    
    public enum UserStatus {
        NORMAL,     // 正常
        LOCKED,     // 锁定
        DISABLED    // 禁用
    }
}
