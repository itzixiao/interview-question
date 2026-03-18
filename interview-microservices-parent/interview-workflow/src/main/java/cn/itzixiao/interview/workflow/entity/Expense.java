package cn.itzixiao.interview.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 报销申请实体类
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Data
@TableName("biz_expense")
public class Expense {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请编号
     */
    private String applyNo;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 申请人部门ID
     */
    private Long deptId;

    /**
     * 申请人部门名称
     */
    private String deptName;

    /**
     * 报销类型：1-差旅，2-招待，3-办公，4-培训，5-其他
     */
    private Integer expenseType;

    /**
     * 报销金额
     */
    private BigDecimal amount;

    /**
     * 报销说明
     */
    private String description;

    /**
     * 附件URL（多个逗号分隔）
     */
    private String attachments;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 审批状态：0-草稿，1-审批中，2-已通过，3-已拒绝，4-已撤回
     */
    private Integer status;

    /**
     * 当前审批节点
     */
    private String currentNode;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Integer deleted;
}
