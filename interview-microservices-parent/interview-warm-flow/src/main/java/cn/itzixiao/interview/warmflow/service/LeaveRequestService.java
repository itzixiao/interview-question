package cn.itzixiao.interview.warmflow.service;

import cn.itzixiao.interview.warmflow.entity.LeaveRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 请假申请 Service 接口
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
public interface LeaveRequestService extends IService<LeaveRequest> {

    /**
     * 提交请假申请
     * 
     * @param leaveRequest 请假申请
     * @return 流程实例ID
     */
    String submitLeaveRequest(LeaveRequest leaveRequest);

    /**
     * 审批请假申请
     * 
     * @param flowInstanceId 流程实例ID
     * @param approved 是否通过
     * @param comment 审批意见
     * @param approverId 审批人ID
     */
    void approveLeaveRequest(String flowInstanceId, Boolean approved, String comment, Long approverId);

    /**
     * 撤销请假申请
     * 
     * @param flowInstanceId 流程实例ID
     * @param userId 申请人ID
     */
    void cancelLeaveRequest(String flowInstanceId, Long userId);

    /**
     * 查询请假申请详情
     * 
     * @param flowInstanceId 流程实例ID
     * @return 请假申请详情
     */
    Map<String, Object> getLeaveRequestDetail(String flowInstanceId);
}
