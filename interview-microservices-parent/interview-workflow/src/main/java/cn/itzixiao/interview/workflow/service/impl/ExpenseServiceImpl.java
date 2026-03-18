package cn.itzixiao.interview.workflow.service.impl;

import cn.itzixiao.interview.workflow.dto.ApprovalDTO;
import cn.itzixiao.interview.workflow.dto.ExpenseApplyDTO;
import cn.itzixiao.interview.workflow.entity.Expense;
import cn.itzixiao.interview.workflow.entity.User;
import cn.itzixiao.interview.workflow.mapper.ExpenseMapper;
import cn.itzixiao.interview.workflow.mapper.UserMapper;
import cn.itzixiao.interview.workflow.service.ExpenseService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报销审批服务实现
 * 核心逻辑：
 * - amount < 1000：部门经理审批
 * - 1000 <= amount <= 5000：财务经理审批
 * - amount > 5000：总经理审批
 * 流程监听器根据部门ID动态查找部门经理；财务经理/总经理从角色表获取
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@Service
public class ExpenseServiceImpl implements ExpenseService {

    @Autowired
    private ExpenseMapper expenseMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long apply(ExpenseApplyDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userMapper.selectByUsername(username);

        // 构建报销申请
        Expense expense = new Expense();
        expense.setApplyNo("EXPENSE-" + System.currentTimeMillis());
        expense.setApplicantId(currentUser.getId());
        expense.setApplicantName(currentUser.getRealName());
        expense.setDeptId(currentUser.getDeptId());
        expense.setExpenseType(dto.getExpenseType());
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setAttachments(dto.getAttachments());
        expense.setStatus(1); // 审批中
        expense.setApplyTime(LocalDateTime.now());
        expense.setCreateTime(LocalDateTime.now());
        expense.setUpdateTime(LocalDateTime.now());
        expense.setDeleted(0);

        expenseMapper.insert(expense);

        // 构建流程变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("applicantUsername", currentUser.getUsername());
        variables.put("deptId", currentUser.getDeptId());
        variables.put("amount", dto.getAmount());

        // 确定当前审批路径（仅用于日志）
        String approvalPath = getApprovalPath(dto.getAmount());

        // 启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "expense_process",
                "EXPENSE:" + expense.getId(),
                variables
        );

        expense.setProcessInstanceId(processInstance.getId());
        expense.setCurrentNode(approvalPath);
        expenseMapper.updateById(expense);

        log.info("=== 报销申请创建成功 ===");
        log.info("申请人: {}, 申请ID: {}, 金额: {}, 审批路径: {}",
                username, expense.getId(), dto.getAmount(), approvalPath);

        return expense.getId();
    }

    /**
     * 根据金额确定审批路径描述
     */
    private String getApprovalPath(BigDecimal amount) {
        if (amount.compareTo(new BigDecimal("1000")) < 0) {
            return "部门经理审批";
        } else if (amount.compareTo(new BigDecimal("5000")) <= 0) {
            return "财务经理审批";
        } else {
            return "总经理审批";
        }
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

        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), dto.getComment());

        Map<String, Object> variables = new HashMap<>();
        String taskName = task.getName();
        if ("部门经理审批".equals(taskName)) {
            variables.put("deptApproved", dto.getApproved());
        } else if ("财务经理审批".equals(taskName)) {
            variables.put("financeApproved", dto.getApproved());
        } else if ("总经理审批".equals(taskName)) {
            variables.put("gmApproved", dto.getApproved());
        }

        taskService.complete(dto.getTaskId(), variables);
        updateExpenseStatusAfterApproval(task.getProcessInstanceId(), dto.getApproved());

        log.info("=== 报销审批操作完成 ===");
        log.info("审批人: {}, 任务: {}, 结果: {}", username, taskName, dto.getApproved() ? "通过" : "驳回");
    }

    private void updateExpenseStatusAfterApproval(String processInstanceId, boolean approved) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        String businessKey = pi != null ? pi.getBusinessKey() : null;
        if (businessKey == null) return;

        String[] parts = businessKey.split(":");
        if (parts.length < 2) return;
        Long expenseId = Long.parseLong(parts[1]);
        Expense expense = expenseMapper.selectById(expenseId);
        if (expense == null) return;

        boolean isEnded = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).count() == 0;

        if (isEnded) {
            expense.setStatus(approved ? 2 : 3);
            expense.setCurrentNode(approved ? "已通过" : "已拒绝");
        } else {
            Task nextTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId).singleResult();
            if (nextTask != null) {
                expense.setCurrentNode(!approved ? "申请人修改" : nextTask.getName());
            }
        }
        expense.setUpdateTime(LocalDateTime.now());
        expenseMapper.updateById(expense);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resubmit(Long expenseId, ExpenseApplyDTO dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Expense expense = expenseMapper.selectById(expenseId);
        if (expense == null) throw new RuntimeException("申请不存在");

        expense.setExpenseType(dto.getExpenseType());
        expense.setAmount(dto.getAmount());
        expense.setDescription(dto.getDescription());
        expense.setAttachments(dto.getAttachments());
        expense.setStatus(1);
        expense.setUpdateTime(LocalDateTime.now());
        expenseMapper.updateById(expense);

        Task task = taskService.createTaskQuery()
                .processInstanceId(expense.getProcessInstanceId())
                .taskAssignee(username)
                .singleResult();

        if (task != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("resubmit", true);
            variables.put("amount", dto.getAmount());
            taskService.complete(task.getId(), variables);
        }

        log.info("=== 报销申请重新提交 ===");
        log.info("申请ID: {}, 申请人: {}, 新金额: {}", expenseId, username, dto.getAmount());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long expenseId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Expense expense = expenseMapper.selectById(expenseId);
        if (expense == null) throw new RuntimeException("申请不存在");

        Task task = taskService.createTaskQuery()
                .processInstanceId(expense.getProcessInstanceId())
                .taskAssignee(username)
                .singleResult();

        if (task != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("resubmit", false);
            taskService.complete(task.getId(), variables);
        }

        expense.setStatus(4);
        expense.setCurrentNode("已撤回");
        expense.setUpdateTime(LocalDateTime.now());
        expenseMapper.updateById(expense);

        log.info("=== 报销申请撤回 ===");
        log.info("申请ID: {}, 申请人: {}", expenseId, username);
    }

    @Override
    public IPage<Expense> myList(Page<Expense> page, Integer status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userMapper.selectByUsername(username);

        LambdaQueryWrapper<Expense> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Expense::getApplicantId, user.getId());
        if (status != null) {
            wrapper.eq(Expense::getStatus, status);
        }
        wrapper.orderByDesc(Expense::getCreateTime);
        return expenseMapper.selectPage(page, wrapper);
    }

    @Override
    public IPage<Expense> pendingList(Page<Expense> page) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

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

        LambdaQueryWrapper<Expense> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Expense::getProcessInstanceId, processInstanceIds);
        wrapper.orderByDesc(Expense::getCreateTime);
        return expenseMapper.selectPage(page, wrapper);
    }

    @Override
    public Expense detail(Long id) {
        return expenseMapper.selectById(id);
    }
}
