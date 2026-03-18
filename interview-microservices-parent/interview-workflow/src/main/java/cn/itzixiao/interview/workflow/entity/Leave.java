package cn.itzixiao.interview.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 请假申请实体类
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Data
@TableName("biz_leave")
public class Leave {

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
     * 假期类型：1-年假，2-事假，3-病假，4-调休
     */
    private Integer leaveType;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 请假天数
     */
    private Integer leaveDays;

    /**
     * 请假原因
     */
    private String reason;

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
