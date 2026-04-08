package cn.itzixiao.interview.warmflow.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 请假申请实体
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@Data
@TableName("leave_request")
public class LeaveRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请人ID */
    private Long userId;

    /** 申请人姓名 */
    private String userName;

    /** 请假类型：1-事假 2-病假 3-年假 4-婚假 5-产假 */
    private Integer leaveType;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 请假天数 */
    private Double days;

    /** 请假原因 */
    private String reason;

    /** 流程实例ID */
    private String flowInstanceId;

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
