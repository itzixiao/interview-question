package cn.itzixiao.interview.warmflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销申请实体
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@Data
@TableName("reimbursement_request")
public class ReimbursementRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID */
    private Long userId;

    /** 申请人姓名 */
    private String userName;

    /** 报销类型：1-差旅费 2-交通费 3-餐饮费 4-办公用品 5-其他 */
    private Integer reimbursementType;

    /** 报销金额 */
    private BigDecimal amount;

    /** 报销事由 */
    private String reason;

    /** 发票附件URL */
    private String attachmentUrls;

    /** 流程实例ID */
    private Long flowInstanceId;

    /** 审批状态：0-草稿 1-审批中 2-已通过 3-已驳回 4-已撤销 */
    private Integer status;

    /** 当前审批节点 */
    private String currentNode;

    /** 审批意见 */
    private String approvalComment;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
