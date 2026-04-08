package cn.itzixiao.interview.warmflow.service;

import cn.itzixiao.interview.warmflow.entity.ReimbursementRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 报销申请 Service 接口
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
public interface ReimbursementRequestService extends IService<ReimbursementRequest> {

    /**
     * 提交报销申请
     * 
     * @param reimbursementRequest 报销申请
     * @return 流程实例ID
     */
    String submitReimbursementRequest(ReimbursementRequest reimbursementRequest);

    /**
     * 审批报销申请
     * 
     * @param flowInstanceId 流程实例ID
     * @param approved 是否通过
     * @param comment 审批意见
     * @param approverId 审批人ID
     */
    void approveReimbursementRequest(String flowInstanceId, Boolean approved, String comment, Long approverId);

    /**
     * 撤销报销申请
     * 
     * @param flowInstanceId 流程实例ID
     * @param userId 申请人ID
     */
    void cancelReimbursementRequest(String flowInstanceId, Long userId);

    /**
     * 查询报销申请详情
     * 
     * @param flowInstanceId 流程实例ID
     * @return 报销申请详情
     */
    Map<String, Object> getReimbursementRequestDetail(String flowInstanceId);
}
