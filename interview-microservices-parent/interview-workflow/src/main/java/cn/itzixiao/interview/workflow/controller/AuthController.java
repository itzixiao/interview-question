package cn.itzixiao.interview.workflow.controller;

import cn.itzixiao.interview.workflow.entity.User;
import cn.itzixiao.interview.workflow.mapper.UserMapper;
import cn.itzixiao.interview.workflow.util.JwtUtil;
import cn.itzixiao.interview.workflow.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 认证控制器 - 登录/获取用户信息
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@Api(tags = "认证管理")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    @ApiOperation(value = "用户登录", notes = "用户名密码登录，返回JWT令牌")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        User user = userMapper.selectByUsername(request.getUsername());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("deptId", user.getDeptId());
        result.put("roles", userDetails.getAuthorities());

        log.info("用户登录成功: {}", request.getUsername());
        return Result.success(result);
    }

    /**
     * 获取当前登录用户信息
     */
    @ApiOperation(value = "获取当前用户信息", notes = "获取当前登录用户的详细信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectByUsername(username);

        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("email", user.getEmail());
        info.put("phone", user.getPhone());
        info.put("deptId", user.getDeptId());
        return Result.success(info);
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 调试接口：验证密码（仅开发环境使用）
     */
    @ApiOperation(value = "[调试]验证密码", notes = "仅开发环境使用，验证用户密码哈希")
    @GetMapping("/debug/password")
    public Result<Map<String, Object>> debugPassword(
            @ApiParam(value = "用户名", required = true) @RequestParam String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("passwordHash", user.getPassword());
        result.put("passwordLength", user.getPassword().length());
        result.put("testMatch", passwordEncoder.matches("123456", user.getPassword()));
        return Result.success(result);
    }

    /**
     * 生成新的BCrypt密码哈希（仅开发环境使用）
     */
    @ApiOperation(value = "[调试]生成密码哈希", notes = "仅开发环境使用，生成BCrypt密码哈希")
    @GetMapping("/debug/generate-password")
    public Result<Map<String, Object>> generatePassword(
            @ApiParam(value = "原始密码", required = true) @RequestParam String password) {
        String hash = passwordEncoder.encode(password);
        Map<String, Object> result = new HashMap<>();
        result.put("password", password);
        result.put("hash", hash);
        result.put("length", hash.length());
        result.put("verify", passwordEncoder.matches(password, hash));
        return Result.success(result);
    }

    /**
     * 修复数据库密码（仅开发环境使用）
     */
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @ApiOperation(value = "[调试]修复密码", notes = "仅开发环境使用，重置所有用户密码")
    @GetMapping("/debug/fix-password")
    public Result<String> fixPassword() {
        String newHash = "$2a$10$BiACCxsTlnbVij5lB9.l1erFvxFI9AMtUBjiTqRVUw6UGI0FWIc8i";
        String sql = "UPDATE sys_user SET password = ?";
        int updated = jdbcTemplate.update(sql, newHash);
        return Result.success("已修复 " + updated + " 个用户的密码");
    }

    /**
     * 修复已完成流程的业务表状态（仅开发环境使用）
     * 检查流程已结束但业务表状态仍为审批中的记录
     */
    @ApiOperation(value = "[调试]修复已完成流程状态", notes = "修复流程已结束但业务表状态仍为审批中的记录")
    @GetMapping("/debug/fix-completed-status")
    public Result<String> fixCompletedStatus() {
        // 修复请假表：流程已结束但状态仍为审批中(1)的记录
        String fixLeaveSql = "UPDATE biz_leave bl " +
                "SET bl.status = 2, bl.update_time = NOW() " +
                "WHERE bl.status = 1 " +
                "AND bl.process_instance_id IS NOT NULL " +
                "AND NOT EXISTS ( " +
                "    SELECT 1 FROM ACT_RU_EXECUTION e WHERE e.PROC_INST_ID_ = bl.process_instance_id " +
                ")";

        // 修复报销表
        String fixExpenseSql = "UPDATE biz_expense be " +
                "SET be.status = 2, be.update_time = NOW() " +
                "WHERE be.status = 1 " +
                "AND be.process_instance_id IS NOT NULL " +
                "AND NOT EXISTS ( " +
                "    SELECT 1 FROM ACT_RU_EXECUTION e WHERE e.PROC_INST_ID_ = be.process_instance_id " +
                ")";

        int leaveFixed = jdbcTemplate.update(fixLeaveSql);
        int expenseFixed = jdbcTemplate.update(fixExpenseSql);

        String msg = String.format("已修复 %d 条请假记录, %d 条报销记录", leaveFixed, expenseFixed);
        log.info(msg);
        return Result.success(msg);
    }

    /**
     * 修复部门经理数据（仅开发环境使用）
     */
    @ApiOperation(value = "[调试]修复部门经理", notes = "修复部门经理关联数据")
    @GetMapping("/debug/fix-dept-manager")
    public Result<String> fixDeptManager() {
        String[] sqls = {
                "UPDATE sys_dept SET manager_id = (SELECT id FROM sys_user WHERE username = 'admin') WHERE dept_name = '总公司'",
                "UPDATE sys_dept SET manager_id = (SELECT id FROM sys_user WHERE username = 'tech_manager_li') WHERE dept_name = '技术部'",
                "UPDATE sys_dept SET manager_id = (SELECT id FROM sys_user WHERE username = 'finance_manager_wang') WHERE dept_name = '财务部'",
                "UPDATE sys_dept SET manager_id = (SELECT id FROM sys_user WHERE username = 'admin') WHERE dept_name = '人事部'",
                "UPDATE sys_dept SET manager_id = (SELECT id FROM sys_user WHERE username = 'gm_zhang') WHERE dept_name = '销售部'"
        };
        int totalUpdated = 0;
        for (String sql : sqls) {
            totalUpdated += jdbcTemplate.update(sql);
        }
        return Result.success("已修复 " + totalUpdated + " 个部门的经理数据");
    }

    /**
     * 获取用户列表（支持按部门筛选）
     */
    @ApiOperation(value = "获取用户列表", notes = "获取用户列表，支持按部门筛选")
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsers(
            @ApiParam(value = "部门ID") @RequestParam(required = false) Long deptId) {
        List<User> users;
        if (deptId != null) {
            users = userMapper.selectByDeptId(deptId);
        } else {
            users = userMapper.selectAll();
        }

        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("realName", user.getRealName());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone());
            map.put("deptId", user.getDeptId());
            map.put("status", user.getStatus());
            return map;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 创建用户
     */
    @ApiOperation(value = "创建用户", notes = "创建新用户，默认密码123456")
    @PostMapping("/user")
    public Result<Map<String, Object>> createUser(@RequestBody UserRequest request) {
        // 检查用户名是否已存在
        User existing = userMapper.selectByUsername(request.getUsername());
        if (existing != null) {
            return Result.fail("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "123456"));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDeptId(request.getDeptId());
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreateTime(java.time.LocalDateTime.now());
        user.setUpdateTime(java.time.LocalDateTime.now());

        userMapper.insert(user);

        log.info("创建用户: {} (ID: {})", user.getUsername(), user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        return Result.success(result);
    }

    /**
     * 更新用户
     */
    @ApiOperation(value = "更新用户", notes = "更新用户信息")
    @PutMapping("/user/{id}")
    public Result<Map<String, Object>> updateUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id,
            @RequestBody UserRequest request) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 检查用户名是否重复（排除自己）
        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            User existing = userMapper.selectByUsername(request.getUsername());
            if (existing != null) {
                return Result.fail("用户名已存在");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getRealName() != null) {
            user.setRealName(request.getRealName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getDeptId() != null) {
            user.setDeptId(request.getDeptId());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdateTime(java.time.LocalDateTime.now());

        userMapper.updateById(user);

        log.info("更新用户: {} (ID: {})", user.getUsername(), user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        return Result.success(result);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @ApiOperation(value = "删除用户", notes = "逻辑删除用户")
    @DeleteMapping("/user/{id}")
    public Result<String> deleteUser(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        // 不能删除管理员
        if ("admin".equals(user.getUsername())) {
            return Result.fail("不能删除管理员账户");
        }

        user.setDeleted(1);
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        log.info("删除用户: {} (ID: {})", user.getUsername(), id);
        return Result.success("删除成功");
    }

    /**
     * 禁用/启用用户
     */
    @ApiOperation(value = "启用/禁用用户", notes = "切换用户启用状态")
    @PutMapping("/user/{id}/status/{status}")
    public Result<String> toggleUserStatus(
            @ApiParam(value = "用户ID", required = true) @PathVariable Long id,
            @ApiParam(value = "状态：1启用，0禁用", required = true) @PathVariable Integer status) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        user.setStatus(status);
        user.setUpdateTime(java.time.LocalDateTime.now());
        userMapper.updateById(user);

        log.info("修改用户状态: {} -> {}", user.getUsername(), status == 1 ? "启用" : "禁用");
        return Result.success(status == 1 ? "启用成功" : "禁用成功");
    }

    // ===================== Request DTO ==========================

    @Data
    public static class UserRequest {
        private String username;
        private String password;
        private String realName;
        private String email;
        private String phone;
        private Long deptId;
    }
}
