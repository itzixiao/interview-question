package cn.itzixiao.interview.workflow.listener;

import cn.itzixiao.interview.workflow.entity.Expense;
import cn.itzixiao.interview.workflow.entity.Leave;
import cn.itzixiao.interview.workflow.mapper.ExpenseMapper;
import cn.itzixiao.interview.workflow.mapper.LeaveMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;

/**
 * 审批通过结束监听器
 * <p>
 * 触发时机：流程达到 "approvedEnd" 结束节点时
 * <p>
 * 核心职责：更新业务表（biz_leave / biz_expense）状态为 "2-已通过"
 * <p>
 * 工作原理：
 * <ol>
 *   <li>从 execution 中获取 businessKey（格式为 "LEAVE:id" 或 "EXPENSE:id"）</li>
 *   <li>根据流程定义Key判断是请假还是报销流程</li>
 *   <li>查询并更新对应业务表的状态字段</li>
 * </ol>
 *
 * @author itzixiao
 * @date 2026-03-18
 */
@Slf4j
@Component("approvalEndListener")
public class ApprovalEndListener implements ExecutionListener {

    private static final long serialVersionUID = 1955007169873165332L;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 静态 Mapper - 解决 Flowable 监听器的 Bean 注入问题
     */
    private static LeaveMapper staticLeaveMapper;
    private static ExpenseMapper staticExpenseMapper;

    @PostConstruct
    public void init() {
        staticLeaveMapper = applicationContext.getBean(LeaveMapper.class);
        staticExpenseMapper = applicationContext.getBean(ExpenseMapper.class);
    }

    /**
     * 流程结束时执行 - 更新业务表状态为已通过
     */
    @Override
    public void notify(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        // 从流程定义ID中提取流程类型（leave_process / expense_process）
        String processDefinitionKey = execution.getProcessDefinitionId().split(":")[0];

        log.info("=== 流程结束 [审批通过] | 流程实例: {} | 业务Key: {} | 流程定义: {}",
                execution.getProcessInstanceId(), businessKey, processDefinitionKey);

        if (businessKey == null) {
            log.warn("业务Key为空，无法更新业务表状态");
            return;
        }

        try {
            if ("leave_process".equals(processDefinitionKey)) {
                // 解析 businessKey，格式为 "LEAVE:123"
                Long leaveId = parseBusinessKey(businessKey, "LEAVE:");
                if (leaveId == null) return;

                // 更新请假表状态为 "2-已通过"
                Leave leave = staticLeaveMapper.selectById(leaveId);
                if (leave != null) {
                    leave.setStatus(2);
                    leave.setCurrentNode("已通过");
                    leave.setUpdateTime(LocalDateTime.now());
                    staticLeaveMapper.updateById(leave);
                    log.info(">>> 请假申请 [{}] 状态已更新为: 已通过", leave.getApplyNo());
                }
            } else if ("expense_process".equals(processDefinitionKey)) {
                // 解析 businessKey，格式为 "EXPENSE:123"
                Long expenseId = parseBusinessKey(businessKey, "EXPENSE:");
                if (expenseId == null) return;

                // 更新报销表状态为 "2-已通过"
                Expense expense = staticExpenseMapper.selectById(expenseId);
                if (expense != null) {
                    expense.setStatus(2);
                    expense.setCurrentNode("已通过");
                    expense.setUpdateTime(LocalDateTime.now());
                    staticExpenseMapper.updateById(expense);
                    log.info(">>> 报销申请 [{}] 状态已更新为: 已通过", expense.getApplyNo());
                }
            }
        } catch (Exception e) {
            log.error("更新业务表状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析 businessKey，提取业务ID
     *
     * @param businessKey 格式为 "LEAVE:123" 或 "EXPENSE:456"
     * @param prefix      前缀，如 "LEAVE:" 或 "EXPENSE:"
     * @return 业务ID，解析失败返回 null
     */
    private Long parseBusinessKey(String businessKey, String prefix) {
        if (businessKey == null) return null;

        try {
            if (businessKey.startsWith(prefix)) {
                return Long.parseLong(businessKey.substring(prefix.length()));
            }
            // 兼容旧格式（纯数字）
            return Long.parseLong(businessKey);
        } catch (NumberFormatException e) {
            log.warn("解析 businessKey 失败: {}", businessKey);
            return null;
        }
    }
}
