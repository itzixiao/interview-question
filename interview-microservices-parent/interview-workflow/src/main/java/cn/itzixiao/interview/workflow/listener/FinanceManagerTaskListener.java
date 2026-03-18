package cn.itzixiao.interview.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 财务经理审批任务监听器
 * <p>
 * 触发时机：财务经理审批任务创建时（BPMN 中 userTask 的 taskListener event="create"）
 * <p>
 * 适用流程：报销流程，当报销金额在 1000-5000 元之间时需财务经理审批
 *
 * @author itzixiao
 * @date 2026-03-18
 * @see DeptManagerTaskListener 部门经理审批任务监听器
 */
@Slf4j
@Component("financeManagerTaskListener")
public class FinanceManagerTaskListener implements TaskListener {

    private static final long serialVersionUID = 6495295553656353851L;

    /**
     * 任务创建时触发 - 记录财务经理审批任务信息
     *
     * @param delegateTask 任务上下文
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        log.info(">>> 任务创建 [财务经理审批] | 任务ID: {} | 审批人: {} | 流程实例: {}",
                delegateTask.getId(), delegateTask.getAssignee(), delegateTask.getProcessInstanceId());
    }
}
