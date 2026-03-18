package cn.itzixiao.interview.workflow.vo;

import cn.itzixiao.interview.workflow.entity.Expense;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 待审批报销 VO - 携带 Flowable taskId
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PendingExpenseVO extends Expense {

    /**
     * Flowable 任务ID（审批时需要）
     */
    private String taskId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务创建时间
     */
    private String taskCreateTime;
}
