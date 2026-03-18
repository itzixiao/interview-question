package cn.itzixiao.interview.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 总经理审批任务监听器
 * <p>
 * 触发时机：总经理审批任务创建时（BPMN 中 userTask 的 taskListener event="create"）
 * <p>
 * 适用流程：
 * <ul>
 *   <li>请假流程：请假天数 > 3 天时，部门经理审批后需总经理审批</li>
 *   <li>报销流程：报销金额 > 5000 元时需总经理审批</li>
 * </ul>
 *
 * @author itzixiao
 * @date 2026-03-18
 * @see DeptManagerTaskListener 部门经理审批任务监听器
 */
@Slf4j
@Component("gmTaskListener")
public class GmTaskListener implements TaskListener {

    private static final long serialVersionUID = 1120056835630986924L;

    /**
     * 任务创建时触发 - 记录总经理审批任务信息
     *
     * @param delegateTask 任务上下文
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        log.info(">>> 任务创建 [总经理审批] | 任务ID: {} | 审批人: {} | 流程实例: {}",
                delegateTask.getId(), delegateTask.getAssignee(), delegateTask.getProcessInstanceId());
    }
}
