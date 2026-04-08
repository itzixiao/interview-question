package cn.itzixiao.interview.warmflow.service.impl;

import cn.itzixiao.interview.warmflow.entity.LeaveRequest;
import cn.itzixiao.interview.warmflow.mapper.LeaveRequestMapper;
import cn.itzixiao.interview.warmflow.service.LeaveRequestService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.warm.flow.core.entity.Instance;
import org.dromara.warm.flow.core.enums.FlowStatus;
import org.dromara.warm.flow.core.service.InsService;
import org.dromara.warm.flow.core.service.TaskService;
import org.dromara.warm.flow.core.dto.FlowParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 请假申请 Service 实现 (Warm-Flow 1.8.5)
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRequestServiceImpl extends ServiceImpl<LeaveRequestMapper, LeaveRequest> 
        implements LeaveRequestService {

    private final InsService insService;
    private final TaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitLeaveRequest(LeaveRequest leaveRequest) {
        // 保存请假申请
        leaveRequest.setStatus(1); // 审批中
        this.save(leaveRequest);

        // 启动工作流 - 使用 FlowParams 构建参数
        Map<String, Object> variable = new HashMap<>();
        variable.put("userId", leaveRequest.getUserId());
        variable.put("userName", leaveRequest.getUserName());
        variable.put("leaveType", leaveRequest.getLeaveType());
        variable.put("days", leaveRequest.getDays());

        FlowParams flowParams = FlowParams.build()
                .flowCode("leave_approval")
                .variable(variable);

        // 启动流程实例
        Instance instance = insService.start(String.valueOf(leaveRequest.getId()), flowParams);
        
        // 更新流程实例ID
        leaveRequest.setFlowInstanceId(String.valueOf(instance.getId()));
        this.updateById(leaveRequest);

        log.info("提交请假申请成功，流程实例ID: {}", instance.getId());
        return String.valueOf(instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveLeaveRequest(String flowInstanceId, Boolean approved, String comment, Long approverId) {
        // 构建审批参数
        Map<String, Object> variable = new HashMap<>();
        variable.put("approved", approved);
        variable.put("comment", comment);

        FlowParams flowParams = FlowParams.build()
                .variable(variable);

        Long instanceId = Long.valueOf(flowInstanceId);

        if (approved) {
            // 审批通过 - 使用 TaskService.skipByInsId
            taskService.skipByInsId(instanceId, flowParams);
            
            // 检查流程是否结束
            Instance instance = insService.getById(instanceId);
            if (instance.getFlowStatus().equals(FlowStatus.FINISHED.getValue())) {
                // 流程结束，更新请假申请状态为已通过
                LeaveRequest leaveRequest = this.lambdaQuery()
                        .eq(LeaveRequest::getFlowInstanceId, flowInstanceId)
                        .one();
                if (leaveRequest != null) {
                    leaveRequest.setStatus(2); // 已通过
                    leaveRequest.setApprovalComment(comment);
                    this.updateById(leaveRequest);
                }
            }
        } else {
            // 审批驳回 - 使用 TaskService.terminationByInsId
            taskService.terminationByInsId(instanceId, flowParams);
            
            // 更新请假申请状态为已驳回
            LeaveRequest leaveRequest = this.lambdaQuery()
                    .eq(LeaveRequest::getFlowInstanceId, flowInstanceId)
                    .one();
            if (leaveRequest != null) {
                leaveRequest.setStatus(3); // 已驳回
                leaveRequest.setApprovalComment(comment);
                this.updateById(leaveRequest);
            }
        }

        log.info("审批请假申请，流程实例ID: {}, 审批结果: {}", flowInstanceId, approved ? "通过" : "驳回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelLeaveRequest(String flowInstanceId, Long userId) {
        Long instanceId = Long.valueOf(flowInstanceId);
        
        // 查询流程实例
        Instance instance = insService.getById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        // 终止流程 - 使用 TaskService.terminationByInsId
        taskService.terminationByInsId(instanceId, FlowParams.build());

        // 更新请假申请状态为已撤销
        LeaveRequest leaveRequest = this.lambdaQuery()
                .eq(LeaveRequest::getFlowInstanceId, flowInstanceId)
                .one();
        if (leaveRequest != null) {
            leaveRequest.setStatus(4); // 已撤销
            this.updateById(leaveRequest);
        }

        log.info("撤销请假申请，流程实例ID: {}", flowInstanceId);
    }

    @Override
    public Map<String, Object> getLeaveRequestDetail(String flowInstanceId) {
        Map<String, Object> result = new HashMap<>();

        // 查询请假申请
        LeaveRequest leaveRequest = this.lambdaQuery()
                .eq(LeaveRequest::getFlowInstanceId, flowInstanceId)
                .one();
        result.put("leaveRequest", leaveRequest);

        // 查询流程实例
        Long instanceId = Long.valueOf(flowInstanceId);
        Instance instance = insService.getById(instanceId);
        result.put("instance", instance);

        return result;
    }
}
