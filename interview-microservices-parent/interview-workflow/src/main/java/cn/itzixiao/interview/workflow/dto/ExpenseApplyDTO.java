package cn.itzixiao.interview.workflow.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 报销申请 DTO
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Data
public class ExpenseApplyDTO {

    /**
     * 报销类型：1-差旅，2-招待，3-办公，4-培训，5-其他
     */
    @NotNull(message = "报销类型不能为空")
    private Integer expenseType;

    /**
     * 报销金额
     */
    @NotNull(message = "报销金额不能为空")
    @DecimalMin(value = "0.01", message = "报销金额必须大于0")
    private BigDecimal amount;

    /**
     * 报销说明
     */
    @NotBlank(message = "报销说明不能为空")
    private String description;

    /**
     * 附件URL（多个逗号分隔）
     */
    private String attachments;
}
