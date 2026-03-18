package cn.itzixiao.interview.workflow.service.impl;

import cn.itzixiao.interview.workflow.dto.ApprovalDTO;
import cn.itzixiao.interview.workflow.dto.LeaveApplyDTO;
import cn.itzixiao.interview.workflow.entity.Leave;
import cn.itzixiao.interview.workflow.entity.User;
import cn.itzixiao.interview.workflow.mapper.LeaveMapper;
import cn.itzixiao.interview.workflow.mapper.UserMapper;
import cn.itzixiao.interview.workflow.service.LeaveService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 请假审批服务实现
 * 核心逻辑：启动流程时注入动态变量（申请人、部门ID、请假天数），
 * 流程监听器根据部门ID查库获取部门经理；天数 <= 3 只走部门经理，> 3 再上报总经理。
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(LeaveApplyDTO dto) {
        // 获取当前登录用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userMapper.selectByUsername(username);

        // 计算请假天数
        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        int leaveDays = (int) days;

        // 构建请假申请对象
        Leave leave = new Leave();
        leave.setApplyNo("LEAVE-" + System.currentTimeMillis());
        leave.setApplicantId(currentUser.getId());
        leave.setApplicantName(currentUser.getRealName());
        leave.setDeptId(currentUser.getDeptId());
        leave.setLeaveType(dto.getLeaveType());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setLeaveDays(leaveDays);
        leave.setReason(dto.getReason());
        leave.setStatus(1); // 审批中
        leave.setApplyTime(LocalDateTime.now());
        leave.setCreateTime(LocalDateTime.now());
        leave.setUpdateTime(LocalDateTime.now());
        leave.setDeleted(0);

        // 先保存获取 ID
        leaveMapper.insert(leave);

        // 构建流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantUsername", currentUser.getUsername());
        variables.put("deptId", currentUser.getDeptId());
        variables.put("leaveDays", leaveDays);

        // 启动流程实例，businessKey 绑定业务ID
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "leave_process",
                "LEAVE:" + leave.getId(),
                variables
        );

        // 更新流程实例ID到请假记录
        leave.setProcessInstanceId(processInstance.getId());
        leave.setCurrentNode("部门经理审批");
        leaveMapper.updateById(leave);

        log.info("=== 请假申请创建成功 ===");
        log.info("申请人: {}, 申请ID: {}, 流程实例ID: {}", username, leave.getId(), processInstance.getId());
        log.info("请假天数: {}, 审批路径: {}", leaveDays, leaveDays <= 3 ? "部门经理审批" : "部门经理 -> 总经理审批");

        return leave.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(ApprovalDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Task task = taskService.createTaskQuery()
                .taskId(dto.getTaskId())
                .taskAssignee(username)
                .singleResult();

        if (task == null) {
            throw new RuntimeException("任务不存在或您没有审批权限，任务ID: " + dto.getTaskId());
        }

        // 设置审批意见
        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), dto.getComment());

        // 根据任务名称设置审批变量
        Map<String, Object> variables = new HashMap<>();
        String taskName = task.getName();
        if ("部门经理审批".equals(taskName)) {
            variables.put("deptApproved", dto.getApproved());
        } else if ("财务经理审批".equals(taskName)) {
            variables.put("financeApproved", dto.getApproved());
        } else if ("总经理审批".equals(taskName)) {
            variables.put("gmApproved", dto.getApproved());
        }

        // 完成任务
        taskService.complete(dto.getTaskId(), variables);

        // 更新请假申请状态
        String businessKey = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult() != null
                ? runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult().getBusinessKey()
                : null;

        updateLeaveStatusAfterApproval(task.getProcessInstanceId(), businessKey, dto.getApproved());

        log.info("=== 审批操作完成 ===");
        log.info("审批人: {}, 任务: {}, 结果: {}, 意见: {}",
                username, taskName, dto.getApproved() ? "通过" : "驳回", dto.getComment());
    }

    /**
     * 审批后更新请假状态
     */
    private void updateLeaveStatusAfterApproval(String processInstanceId, String businessKey, boolean approved) {
        if (businessKey == null) return;
        String[] parts = businessKey.split(":");
        if (parts.length < 2) return;

        Long leaveId = Long.parseLong(parts[1]);
        Leave leave = leaveMapper.selectById(leaveId);
        if (leave == null) return;

        // 检查流程是否结束
        boolean isEnded = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .count() == 0;

        if (isEnded) {
            // 流程已结束，检查最终状态（通过 Flowable 历史数据）
            leave.setStatus(approved ? 2 : 3); // 2-通过，3-拒绝
            leave.setCurrentNode(approved ? "已通过" : "已拒绝");
        } else {
            // 流程还在进行
            Task nextTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult();
            if (nextTask != null) {
                if (!approved) {
                    leave.setCurrentNode("申请人修改");
                } else {
                    leave.setCurrentNode(nextTask.getName());
                }
            }
        }
        leave.setUpdateTime(LocalDateTime.now());
        leaveMapper.updateById(leave);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long leaveId, LeaveApplyDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Leave leave = leaveMapper.selectById(leaveId);

        if (leave == null || !leave.getApplicantId().equals(
                userMapper.selectByUsername(username).getId())) {
            throw new RuntimeException("申请不存在或无权操作");
        }

        // 更新申请信息
        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        leave.setLeaveType(dto.getLeaveType());
        leave.setStartDate(dto.getStartDate());
        leave.setEndDate(dto.getEndDate());
        leave.setLeaveDays((int) days);
        leave.setReason(dto.getReason());
        leave.setStatus(1);
        leave.setUpdateTime(LocalDateTime.now());
        leaveMapper.updateById(leave);

        // 在申请人修改任务中设置重新提交变量
        Task task = taskService.createTaskQuery()
                .processInstanceId(leave.getProcessInstanceId())
                .taskAssignee(username)
                .singleResult();

        if (task != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("resubmit", true);
            variables.put("leaveDays", (int) days);
            taskService.complete(task.getId(), variables);
        }

        log.info("=== 请假申请重新提交 ===");
        log.info("申请ID: {}, 申请人: {}, 新请假天数: {}", leaveId, username, days);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long leaveId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Leave leave = leaveMapper.selectById(leaveId);

        if (leave == null) {
            throw new RuntimeException("申请不存在");
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(leave.getProcessInstanceId())
                .taskAssignee(username)
                .singleResult();

        if (task != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("resubmit", false);
            taskService.complete(task.getId(), variables);
        }

        leave.setStatus(4); // 已撤回
        leave.setCurrentNode("已撤回");
        leave.setUpdateTime(LocalDateTime.now());
        leaveMapper.updateById(leave);

        log.info("=== 请假申请撤回 ===");
        log.info("申请ID: {}, 申请人: {}", leaveId, username);
    }

    @Override
    public IPage<Leave> myList(Page<Leave> page, Integer status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectByUsername(username);

        LambdaQueryWrapper<Leave> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Leave::getApplicantId, user.getId());
        if (status != null) {
            wrapper.eq(Leave::getStatus, status);
        }
        wrapper.orderByDesc(Leave::getCreateTime);
        return leaveMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<Leave> pendingList(Page<Leave> page) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 查询当前用户待审批的流程实例ID列表
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(username)
                .orderByTaskCreateTime().desc()
                .list();

        List<String> processInstanceIds = tasks.stream()
                .map(Task::getProcessInstanceId)
                .collect(Collectors.toList());

        if (processInstanceIds.isEmpty()) {
            return new Page<>();
        }

        LambdaQueryWrapper<Leave> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Leave::getProcessInstanceId, processInstanceIds);
        wrapper.orderByDesc(Leave::getCreateTime);
        return leaveMapper.selectPage(page, wrapper);
    }

    @Override
    public Leave detail(Long id) {
        return leaveMapper.selectById(id);
    }
}
