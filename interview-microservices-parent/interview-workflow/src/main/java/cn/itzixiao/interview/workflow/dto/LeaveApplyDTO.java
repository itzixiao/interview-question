package cn.itzixiao.interview.workflow.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 请假申请 DTO
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Data
public class LeaveApplyDTO {

    /**
     * 假期类型：1-年假，2-事假，3-病假，4-调休
     */
    @NotNull(message = "假期类型不能为空")
    private Integer leaveType;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    /**
     * 请假原因
     */
    @NotBlank(message = "请假原因不能为空")
    private String reason;
}
