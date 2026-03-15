package cn.itzixiao.interview.security.rbac;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限实体
 * 
 * RBAC（Role-Based Access Control）权限模型：
 * - 用户（User）：系统使用者
 * - 角色（Role）：权限的集合
 * - 权限（Permission）：具体的操作权限
 * 
 * 关系：
 * - 用户可以拥有多个角色（1:N）
 * - 角色可以包含多个权限（1:N）
 * - 用户通过角色间接获得权限
 * 
 * @author itzixiao
 * @date 2026-03-15
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {
    
    /**
     * 权限 ID
     */
    private Long id;
    
    /**
     * 权限编码（如：user:create, order:delete）
     */
    private String code;
    
    /**
     * 权限名称
     */
    private String name;
    
    /**
     * 权限描述
     */
    private String description;
    
    /**
     * 资源类型（MENU-菜单，BUTTON-按钮，API-接口）
     */
    private ResourceType resourceType;
    
    /**
     * 资源路径（如：/api/users）
     */
    private String resourcePath;
    
    /**
     * 请求方法（GET, POST, PUT, DELETE）
     */
    private String httpMethod;
    
    public enum ResourceType {
        MENU,      // 菜单
        BUTTON,    // 按钮
        API        // 接口
    }
}
