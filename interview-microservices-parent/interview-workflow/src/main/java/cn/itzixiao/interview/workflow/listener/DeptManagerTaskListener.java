package cn.itzixiao.interview.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 部门经理审批任务监听器
 * <p>
 * 触发时机：部门经理审批任务创建时（BPMN 中 userTask 的 taskListener event="create"）
 * <p>
 * 核心职责：记录任务创建日志，方便审计追踪
 *
 * @author itzixiao
 * @date 2026-03-18
 */
@Slf4j
@Component("deptManagerTaskListener")
public class DeptManagerTaskListener implements TaskListener {

    private static final long serialVersionUID = 3926724371187137600L;

    /**
     * 任务创建时触发 - 记录审批任务信息
     *
     * @param delegateTask 任务上下文，包含任务ID、审批人等信息
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        log.info(">>> 任务创建 [部门经理审批] | 任务ID: {} | 审批人: {} | 流程实例: {}",
                delegateTask.getId(), delegateTask.getAssignee(), delegateTask.getProcessInstanceId());
    }
}
