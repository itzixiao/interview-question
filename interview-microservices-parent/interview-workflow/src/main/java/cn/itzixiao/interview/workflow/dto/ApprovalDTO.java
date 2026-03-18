package cn.itzixiao.interview.workflow.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 审批操作 DTO
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Data
public class ApprovalDTO {

    /**
     * 任务ID
     */
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    /**
     * 审批结果：true-通过，false-驳回
     */
    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    /**
     * 审批意见
     */
    private String comment;
}
