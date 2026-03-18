package cn.itzixiao.interview.workflow.controller;

import cn.itzixiao.interview.workflow.entity.Expense;
import cn.itzixiao.interview.workflow.entity.Leave;
import cn.itzixiao.interview.workflow.mapper.ExpenseMapper;
import cn.itzixiao.interview.workflow.mapper.LeaveMapper;
import cn.itzixiao.interview.workflow.util.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 流程历史/审批轨迹控制器
 * 提供：审批历史记录、流程节点轨迹、审批意见查询
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@Api(tags = "流程历史")
@RestController
@RequestMapping("/api/process")
public class ProcessHistoryController {

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LeaveMapper leaveMapper;

    @Autowired
    private ExpenseMapper expenseMapper;

    /**
     * 查询当前登录用户的审批记录（我参与审批过的任务）
     * 包含请假和报销的审批记录
     */
    @ApiOperation(value = "我的审批记录", notes = "查询当前用户参与审批过的所有任务")
    @GetMapping("/my-approval-records")
    public Result<List<MyApprovalRecordVO>> getMyApprovalRecords(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "每页数量", defaultValue = "20") @RequestParam(defaultValue = "20") int pageSize) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // 查询当前用户已完成的历史任务
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(username)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage((pageNum - 1) * pageSize, pageSize);

        List<MyApprovalRecordVO> records = new ArrayList<>();
        for (HistoricTaskInstance task : historicTasks) {
            MyApprovalRecordVO vo = new MyApprovalRecordVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setProcessInstanceId(task.getProcessInstanceId());
            vo.setStartTime(task.getStartTime());
            vo.setEndTime(task.getEndTime());
            vo.setDurationInMillis(task.getDurationInMillis());

            // 获取审批意见
            List<org.flowable.engine.task.Comment> comments = taskService.getTaskComments(task.getId());
            if (!comments.isEmpty()) {
                vo.setComment(comments.get(comments.size() - 1).getFullMessage());
            }

            // 获取流程实例信息，判断是请假还是报销
            HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();

            if (pi != null) {
                String processDefKey = pi.getProcessDefinitionKey();
                String businessKey = pi.getBusinessKey();
                vo.setProcessDefinitionName(pi.getProcessDefinitionName());
                vo.setProcessStatus(pi.getEndTime() != null ? "已结束" : "进行中");

                // 根据流程类型查询业务详情
                if ("leave_process".equals(processDefKey) && businessKey != null) {
                    vo.setBusinessType("请假申请");
                    try {
                        Long leaveId = Long.parseLong(businessKey.replace("LEAVE:", ""));
                        Leave leave = leaveMapper.selectById(leaveId);
                        if (leave != null) {
                            vo.setApplyNo(leave.getApplyNo());
                            vo.setApplicantName(leave.getApplicantName());
                            vo.setBusinessStatus(getStatusText(leave.getStatus()));
                            vo.setBusinessSummary(leave.getLeaveType() + "类假期 " + leave.getLeaveDays() + "天");
                        }
                    } catch (Exception e) {
                        log.warn("解析请假业务ID失败: {}", businessKey);
                    }
                } else if ("expense_process".equals(processDefKey) && businessKey != null) {
                    vo.setBusinessType("报销申请");
                    try {
                        Long expenseId = Long.parseLong(businessKey.replace("EXPENSE:", ""));
                        Expense expense = expenseMapper.selectById(expenseId);
                        if (expense != null) {
                            vo.setApplyNo(expense.getApplyNo());
                            vo.setApplicantName(expense.getApplicantName());
                            vo.setBusinessStatus(getStatusText(expense.getStatus()));
                            vo.setBusinessSummary("报销金额 " + expense.getAmount() + " 元");
                        }
                    } catch (Exception e) {
                        log.warn("解析报销业务ID失败: {}", businessKey);
                    }
                }
            }

            records.add(vo);
        }

        log.info("查询用户[{}]的审批记录: {}条", username, records.size());
        return Result.success(records);
    }

    /**
     * 状态码转文本
     */
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0:
                return "草稿";
            case 1:
                return "审批中";
            case 2:
                return "已通过";
            case 3:
                return "已拒绝";
            case 4:
                return "已撤回";
            default:
                return "未知";
        }
    }

    /**
     * 查询流程审批历史（审批节点 + 审批人 + 审批意见 + 时间）
     *
     * @param processInstanceId Flowable 流程实例ID
     */
    @ApiOperation(value = "流程审批历史", notes = "查询流程的审批节点、审批人、意见等历史信息")
    @GetMapping("/history/{processInstanceId}")
    public Result<List<ApprovalHistoryVO>> getHistory(
            @ApiParam(value = "流程实例ID", required = true) @PathVariable String processInstanceId) {
        // 查询历史任务（已完成）
        List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricTaskInstanceStartTime().asc()
                .list();

        List<ApprovalHistoryVO> historyList = new ArrayList<>();
        for (HistoricTaskInstance task : historicTasks) {
            ApprovalHistoryVO vo = new ApprovalHistoryVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setAssignee(task.getAssignee());
            vo.setStartTime(task.getStartTime());
            vo.setEndTime(task.getEndTime());
            vo.setDurationInMillis(task.getDurationInMillis());

            // 查询审批意见（comment）
            List<org.flowable.engine.task.Comment> comments =
                    taskService.getTaskComments(task.getId());
            if (!comments.isEmpty()) {
                // 取最后一条意见
                vo.setComment(comments.get(comments.size() - 1).getFullMessage());
            }
            // 判断是否已完成（有结束时间）
            vo.setStatus(task.getEndTime() != null ? "已完成" : "审批中");

            historyList.add(vo);
        }

        log.info("查询流程历史: processInstanceId={}, 节点数={}", processInstanceId, historyList.size());
        return Result.success(historyList);
    }

    /**
     * 查询流程节点活动轨迹（所有节点，含网关）
     *
     * @param processInstanceId 流程实例ID
     */
    @ApiOperation(value = "流程活动轨迹", notes = "查询流程所有节点的活动轨迹（含网关）")
    @GetMapping("/activity/{processInstanceId}")
    public Result<List<ActivityVO>> getActivityTrace(
            @ApiParam(value = "流程实例ID", required = true) @PathVariable String processInstanceId) {
        List<HistoricActivityInstance> activities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<ActivityVO> activityVOs = new ArrayList<>();
        for (HistoricActivityInstance activity : activities) {
            ActivityVO vo = new ActivityVO();
            vo.setActivityId(activity.getActivityId());
            vo.setActivityName(activity.getActivityName());
            vo.setActivityType(activity.getActivityType());
            vo.setAssignee(activity.getAssignee());
            vo.setStartTime(activity.getStartTime());
            vo.setEndTime(activity.getEndTime());
            vo.setDurationInMillis(activity.getDurationInMillis());
            activityVOs.add(vo);
        }

        log.info("查询活动轨迹: processInstanceId={}, 活动数={}", processInstanceId, activityVOs.size());
        return Result.success(activityVOs);
    }

    /**
     * 查询流程实例信息（含最终状态）
     *
     * @param processInstanceId 流程实例ID
     */
    @ApiOperation(value = "流程实例信息", notes = "查询流程实例的详细信息（含最终状态）")
    @GetMapping("/instance/{processInstanceId}")
    public Result<ProcessInstanceVO> getProcessInstance(
            @ApiParam(value = "流程实例ID", required = true) @PathVariable String processInstanceId) {
        HistoricProcessInstance pi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (pi == null) {
            return Result.fail(404, "流程实例不存在");
        }

        ProcessInstanceVO vo = new ProcessInstanceVO();
        vo.setProcessInstanceId(pi.getId());
        vo.setProcessDefinitionName(pi.getProcessDefinitionName());
        vo.setBusinessKey(pi.getBusinessKey());
        vo.setStartTime(pi.getStartTime());
        vo.setEndTime(pi.getEndTime());
        vo.setDurationInMillis(pi.getDurationInMillis());
        vo.setStatus(pi.getEndTime() != null ? "已结束" : "进行中");

        return Result.success(vo);
    }

    /**
     * 仪表盘统计接口：待我审批 + 我发起（当前登录用户）
     * 通过 AuthController.info() 获取用户名，再按需展示数量
     */
    @ApiOperation(value = "统计数据", notes = "仪表盘统计数据（透传）")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats(
            @ApiParam(value = "待审批请假数") @RequestParam(required = false, defaultValue = "0") long myPendingLeave,
            @ApiParam(value = "待审批报销数") @RequestParam(required = false, defaultValue = "0") long myPendingExpense,
            @ApiParam(value = "我发起的请假数") @RequestParam(required = false, defaultValue = "0") long myAppliedLeave,
            @ApiParam(value = "我发起的报销数") @RequestParam(required = false, defaultValue = "0") long myAppliedExpense) {
        // 此接口从前端聚合各统计数据调用，服务端直接透传
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setMyPendingLeave(myPendingLeave);
        vo.setMyPendingExpense(myPendingExpense);
        vo.setMyAppliedLeave(myAppliedLeave);
        vo.setMyAppliedExpense(myAppliedExpense);
        return Result.success(vo);
    }

    // ==================== VO 类 ====================

    /**
     * 我的审批记录 VO
     */
    @Data
    public static class MyApprovalRecordVO {
        /**
         * 任务ID
         */
        private String taskId;
        /**
         * 任务名称（如"部门经理审批"）
         */
        private String taskName;
        /**
         * 流程实例ID
         */
        private String processInstanceId;
        /**
         * 流程定义名称
         */
        private String processDefinitionName;
        /**
         * 业务类型（请假申请/报销申请）
         */
        private String businessType;
        /**
         * 申请编号
         */
        private String applyNo;
        /**
         * 申请人姓名
         */
        private String applicantName;
        /**
         * 业务状态（审批中/已通过/已拒绝）
         */
        private String businessStatus;
        /**
         * 业务摘要（如"请假5天"或"报销500元"）
         */
        private String businessSummary;
        /**
         * 流程状态
         */
        private String processStatus;
        /**
         * 审批开始时间
         */
        private Date startTime;
        /**
         * 审批结束时间
         */
        private Date endTime;
        /**
         * 耗时（毫秒）
         */
        private Long durationInMillis;
        /**
         * 审批意见
         */
        private String comment;
    }

    @Data
    public static class ApprovalHistoryVO {
        private String taskId;
        private String taskName;
        private String assignee;
        private Date startTime;
        private Date endTime;
        private Long durationInMillis;
        private String comment;
        private String status;
    }

    @Data
    public static class ActivityVO {
        private String activityId;
        private String activityName;
        private String activityType;
        private String assignee;
        private Date startTime;
        private Date endTime;
        private Long durationInMillis;
    }

    @Data
    public static class ProcessInstanceVO {
        private String processInstanceId;
        private String processDefinitionName;
        private String businessKey;
        private Date startTime;
        private Date endTime;
        private Long durationInMillis;
        private String status;
    }

    @Data
    public static class DashboardStatsVO {
        private long myPendingLeave;
        private long myPendingExpense;
        private long myAppliedLeave;
        private long myAppliedExpense;
    }
}
