package cn.itzixiao.interview.security.rbac;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 角色实体
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    
    /**
     * 角色 ID
     */
    private Long id;
    
    /**
     * 角色编码（如：ROLE_ADMIN, ROLE_USER）
     */
    private String code;
    
    /**
     * 角色名称
     */
    private String name;
    
    /**
     * 角色描述
     */
    private String description;
    
    /**
     * 角色拥有的权限列表
     */
    private List<Permission> permissions;
}
