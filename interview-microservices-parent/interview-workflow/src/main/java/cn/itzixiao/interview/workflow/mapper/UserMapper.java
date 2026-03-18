package cn.itzixiao.interview.workflow.mapper;

import cn.itzixiao.interview.workflow.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据部门ID查询部门经理
     */
    @Select("SELECT u.* FROM sys_user u INNER JOIN sys_dept d ON d.manager_id = u.id " +
            "WHERE d.id = #{deptId} AND u.deleted = 0 AND u.status = 1")
    User selectDeptManagerByDeptId(@Param("deptId") Long deptId);

    /**
     * 查询总经理（角色编码为 GENERAL_MANAGER）
     */
    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON ur.user_id = u.id " +
            "INNER JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'GENERAL_MANAGER' AND u.deleted = 0 AND u.status = 1 LIMIT 1")
    User selectGeneralManager();

    /**
     * 查询财务经理（角色编码为 FINANCE_MANAGER）
     */
    @Select("SELECT u.* FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON ur.user_id = u.id " +
            "INNER JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'FINANCE_MANAGER' AND u.deleted = 0 AND u.status = 1 LIMIT 1")
    User selectFinanceManager();

    /**
     * 根据部门ID查询用户列表
     */
    @Select("SELECT * FROM sys_user WHERE dept_id = #{deptId} AND deleted = 0")
    List<User> selectByDeptId(@Param("deptId") Long deptId);

    /**
     * 查询所有用户
     */
    @Select("SELECT * FROM sys_user WHERE deleted = 0")
    List<User> selectAll();
}
