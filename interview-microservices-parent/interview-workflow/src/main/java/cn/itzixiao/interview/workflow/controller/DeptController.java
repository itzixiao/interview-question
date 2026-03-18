package cn.itzixiao.interview.workflow.controller;

import cn.itzixiao.interview.workflow.entity.Dept;
import cn.itzixiao.interview.workflow.entity.Expense;
import cn.itzixiao.interview.workflow.entity.Leave;
import cn.itzixiao.interview.workflow.entity.User;
import cn.itzixiao.interview.workflow.mapper.DeptMapper;
import cn.itzixiao.interview.workflow.mapper.ExpenseMapper;
import cn.itzixiao.interview.workflow.mapper.LeaveMapper;
import cn.itzixiao.interview.workflow.mapper.UserMapper;
import cn.itzixiao.interview.workflow.util.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门管理控制器 + 仪表盘统计控制器
 * 提供：部门CRUD、首页统计数据
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@Api(tags = "系统管理")
@RestController
@RequestMapping("/api/system")
public class DeptController {

    @Autowired
    private DeptMapper deptMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private TaskService taskService;

    /**
     * 获取所有启用部门列表（申请时下拉选择）
     */
    @ApiOperation(value = "获取部门列表", notes = "获取所有启用的部门列表，带经理信息")
    @GetMapping("/dept/list")
    public Result<List<Map<String, Object>>> deptList() {
        List<Dept> depts = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>()
                        .eq(Dept::getStatus, 1)
                        .eq(Dept::getDeleted, 0)
                        .orderByAsc(Dept::getSort));

        // 附加部门经理姓名
        List<Map<String, Object>> result = new ArrayList<>();
        for (Dept dept : depts) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", dept.getId());
            map.put("deptName", dept.getDeptName());
            map.put("parentId", dept.getParentId());
            map.put("managerId", dept.getManagerId());
            map.put("sort", dept.getSort());
            map.put("status", dept.getStatus());
            // 获取经理姓名
            if (dept.getManagerId() != null) {
                User manager = userMapper.selectById(dept.getManagerId());
                map.put("managerName", manager != null ? manager.getRealName() : null);
            } else {
                map.put("managerName", null);
            }
            result.add(map);
        }

        log.info("查询部门列表: {}个部门", result.size());
        return Result.success(result);
    }

    /**
     * 创建部门
     */
    @ApiOperation(value = "创建部门", notes = "创建新部门")
    @PostMapping("/dept")
    public Result<Dept> createDept(@RequestBody DeptRequest request) {
        // 检查同名部门
        Dept existing = deptMapper.selectOne(
                new LambdaQueryWrapper<Dept>()
                        .eq(Dept::getDeptName, request.getDeptName())
                        .eq(Dept::getDeleted, 0));
        if (existing != null) {
            return Result.fail("部门名称已存在");
        }

        Dept dept = new Dept();
        dept.setDeptName(request.getDeptName());
        dept.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        dept.setManagerId(request.getManagerId());
        dept.setSort(request.getSort() != null ? request.getSort() : 0);
        dept.setStatus(1);
        dept.setDeleted(0);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());

        deptMapper.insert(dept);
        log.info("创建部门: {} (ID: {})", dept.getDeptName(), dept.getId());
        return Result.success(dept);
    }

    /**
     * 更新部门
     */
    @ApiOperation(value = "更新部门", notes = "更新部门信息")
    @PutMapping("/dept/{id}")
    public Result<Dept> updateDept(
            @ApiParam(value = "部门ID", required = true) @PathVariable Long id,
            @RequestBody DeptRequest request) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null || dept.getDeleted() == 1) {
            return Result.fail("部门不存在");
        }

        // 检查同名部门（排除自己）
        Dept existing = deptMapper.selectOne(
                new LambdaQueryWrapper<Dept>()
                        .eq(Dept::getDeptName, request.getDeptName())
                        .ne(Dept::getId, id)
                        .eq(Dept::getDeleted, 0));
        if (existing != null) {
            return Result.fail("部门名称已存在");
        }

        dept.setDeptName(request.getDeptName());
        if (request.getParentId() != null) {
            dept.setParentId(request.getParentId());
        }
        if (request.getManagerId() != null) {
            dept.setManagerId(request.getManagerId());
        }
        if (request.getSort() != null) {
            dept.setSort(request.getSort());
        }
        dept.setUpdateTime(LocalDateTime.now());

        deptMapper.updateById(dept);
        log.info("更新部门: {} (ID: {})", dept.getDeptName(), dept.getId());
        return Result.success(dept);
    }

    /**
     * 删除部门（逻辑删除）
     */
    @ApiOperation(value = "删除部门", notes = "逻辑删除部门")
    @DeleteMapping("/dept/{id}")
    public Result<String> deleteDept(
            @ApiParam(value = "部门ID", required = true) @PathVariable Long id) {
        Dept dept = deptMapper.selectById(id);
        if (dept == null || dept.getDeleted() == 1) {
            return Result.fail("部门不存在");
        }

        // 检查是否有子部门
        long childCount = deptMapper.selectCount(
                new LambdaQueryWrapper<Dept>()
                        .eq(Dept::getParentId, id)
                        .eq(Dept::getDeleted, 0));
        if (childCount > 0) {
            return Result.fail("该部门存在子部门，无法删除");
        }

        // 检查是否有用户
        List<User> users = userMapper.selectByDeptId(id);
        if (!users.isEmpty()) {
            return Result.fail("该部门存在用户，无法删除");
        }

        dept.setDeleted(1);
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.updateById(dept);

        log.info("删除部门: {} (ID: {})", dept.getDeptName(), id);
        return Result.success("删除成功");
    }

    /**
     * 设置部门经理
     */
    @ApiOperation(value = "设置部门经理", notes = "为指定部门设置经理")
    @PutMapping("/dept/{deptId}/manager/{userId}")
    public Result<String> setDeptManager(
            @ApiParam(value = "部门ID", required = true) @PathVariable Long deptId,
            @ApiParam(value = "用户ID", required = true) @PathVariable Long userId) {
        Dept dept = deptMapper.selectById(deptId);
        if (dept == null || dept.getDeleted() == 1) {
            return Result.fail("部门不存在");
        }

        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            return Result.fail("用户不存在");
        }

        dept.setManagerId(userId);
        dept.setUpdateTime(LocalDateTime.now());
        deptMapper.updateById(dept);

        log.info("设置部门经理: 部门[{}] -> 经理[{}]", dept.getDeptName(), user.getRealName());
        return Result.success("设置成功");
    }

    // ===================== Request DTO ==========================

    @Data
    public static class DeptRequest {
        private String deptName;
        private Long parentId;
        private Long managerId;
        private Integer sort;
    }

    /**
     * 仪表盘统计数据（基于当前登录用户）
     * 返回：我的待审批数、我发起的申请数
     */
    @ApiOperation(value = "仪表盘统计", notes = "获取当前用户的待审批数、申请数等统计数据")
    @GetMapping("/dashboard/stats")
    public Result<DashboardStatsVO> dashboardStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectByUsername(username);

        DashboardStatsVO stats = new DashboardStatsVO();

        // 我发起的请假申请（总数）
        long myLeaveTotal = leaveMapper.selectCount(
                new LambdaQueryWrapper<Leave>()
                        .eq(Leave::getApplicantId, user.getId())
                        .eq(Leave::getDeleted, 0));

        // 我发起的报销申请（总数）
        long myExpenseTotal = expenseMapper.selectCount(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getApplicantId, user.getId())
                        .eq(Expense::getDeleted, 0));

        // 审批中（status=1）
        long myLeaveInProgress = leaveMapper.selectCount(
                new LambdaQueryWrapper<Leave>()
                        .eq(Leave::getApplicantId, user.getId())
                        .eq(Leave::getStatus, 1)
                        .eq(Leave::getDeleted, 0));

        long myExpenseInProgress = expenseMapper.selectCount(
                new LambdaQueryWrapper<Expense>()
                        .eq(Expense::getApplicantId, user.getId())
                        .eq(Expense::getStatus, 1)
                        .eq(Expense::getDeleted, 0));

        // 待我审批（Flowable 任务数）
        long pendingTaskCount = taskService.createTaskQuery()
                .taskAssignee(username)
                .count();

        // 待审批请假（任务名中含"请假"或来自 leave_process）
        List<Task> pendingLeaveTasks = taskService.createTaskQuery()
                .taskAssignee(username)
                .processDefinitionKey("leave_process")
                .list();

        List<Task> pendingExpenseTasks = taskService.createTaskQuery()
                .taskAssignee(username)
                .processDefinitionKey("expense_process")
                .list();

        stats.setMyLeaveTotal(myLeaveTotal);
        stats.setMyExpenseTotal(myExpenseTotal);
        stats.setMyLeaveInProgress(myLeaveInProgress);
        stats.setMyExpenseInProgress(myExpenseInProgress);
        stats.setPendingApprovalCount(pendingTaskCount);
        stats.setPendingLeaveCount(pendingLeaveTasks.size());
        stats.setPendingExpenseCount(pendingExpenseTasks.size());

        log.info("=== 仪表盘统计 [{}] === 我发起请假:{}, 报销:{}, 待审批:{} ===",
                username, myLeaveTotal, myExpenseTotal, pendingTaskCount);

        return Result.success(stats);
    }

    // ===================== VO ==========================

    @Data
    public static class DashboardStatsVO {
        /**
         * 我发起的请假总数
         */
        private long myLeaveTotal;
        /**
         * 我发起的报销总数
         */
        private long myExpenseTotal;
        /**
         * 我发起的请假-审批中数量
         */
        private long myLeaveInProgress;
        /**
         * 我发起的报销-审批中数量
         */
        private long myExpenseInProgress;
        /**
         * 待我审批总任务数
         */
        private long pendingApprovalCount;
        /**
         * 待我审批的请假数
         */
        private long pendingLeaveCount;
        /**
         * 待我审批的报销数
         */
        private long pendingExpenseCount;
    }
}
