package cn.itzixiao.interview.workflow.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 申请人修改任务监听器
 * <p>
 * 触发时机：审批被驳回后，申请人修改任务创建时
 * <p>
 * 核心职责：
 * <ul>
 *   <li>记录驳回后的修改任务信息</li>
 *   <li>申请人可选择「重新提交」或「撤回」</li>
 * </ul>
 *
 * @author itzixiao
 * @date 2026-03-18
 */
@Slf4j
@Component("applicantModifyTaskListener")
public class ApplicantModifyTaskListener implements TaskListener {

    private static final long serialVersionUID = 6712548257264351978L;

    /**
     * 任务创建时触发 - 记录申请人修改任务信息
     *
     * @param delegateTask 任务上下文
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        log.info(">>> 任务创建 [申请人修改] | 任务ID: {} | 分配给: {} | 流程实例: {}",
                delegateTask.getId(), delegateTask.getAssignee(), delegateTask.getProcessInstanceId());
    }
}
