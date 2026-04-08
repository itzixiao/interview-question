package cn.itzixiao.interview.warmflow.service.impl;

import cn.itzixiao.interview.warmflow.entity.ReimbursementRequest;
import cn.itzixiao.interview.warmflow.mapper.ReimbursementRequestMapper;
import cn.itzixiao.interview.warmflow.service.ReimbursementRequestService;
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
 * 报销申请 Service 实现 (Warm-Flow 1.8.5)
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReimbursementRequestServiceImpl extends ServiceImpl<ReimbursementRequestMapper, ReimbursementRequest> 
        implements ReimbursementRequestService {

    private final InsService insService;
    private final TaskService taskService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitReimbursementRequest(ReimbursementRequest reimbursementRequest) {
        // 保存报销申请
        reimbursementRequest.setStatus(1); // 审批中
        this.save(reimbursementRequest);

        // 启动工作流 - 使用 FlowParams 构建参数
        Map<String, Object> variable = new HashMap<>();
        variable.put("userId", reimbursementRequest.getUserId());
        variable.put("userName", reimbursementRequest.getUserName());
        variable.put("reimbursementType", reimbursementRequest.getReimbursementType());
        variable.put("amount", reimbursementRequest.getAmount());

        FlowParams flowParams = FlowParams.build()
                .flowCode("reimbursement_approval")
                .variable(variable);

        // 启动流程实例
        Instance instance = insService.start(String.valueOf(reimbursementRequest.getId()), flowParams);
        
        // 更新流程实例ID
        reimbursementRequest.setFlowInstanceId(instance.getId());
        this.updateById(reimbursementRequest);

        log.info("提交报销申请成功，流程实例ID: {}", instance.getId());
        return String.valueOf(instance.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveReimbursementRequest(String flowInstanceId, Boolean approved, String comment, Long approverId) {
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
                // 流程结束，更新报销申请状态为已通过
                ReimbursementRequest reimbursementRequest = this.lambdaQuery()
                        .eq(ReimbursementRequest::getFlowInstanceId, instanceId)
                        .one();
                if (reimbursementRequest != null) {
                    reimbursementRequest.setStatus(2); // 已通过
                    reimbursementRequest.setApprovalComment(comment);
                    this.updateById(reimbursementRequest);
                }
            }
        } else {
            // 审批驳回 - 使用 TaskService.terminationByInsId
            taskService.terminationByInsId(instanceId, flowParams);
            
            // 更新报销申请状态为已驳回
            ReimbursementRequest reimbursementRequest = this.lambdaQuery()
                    .eq(ReimbursementRequest::getFlowInstanceId, instanceId)
                    .one();
            if (reimbursementRequest != null) {
                reimbursementRequest.setStatus(3); // 已驳回
                reimbursementRequest.setApprovalComment(comment);
                this.updateById(reimbursementRequest);
            }
        }

        log.info("审批报销申请，流程实例ID: {}, 审批结果: {}", flowInstanceId, approved ? "通过" : "驳回");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelReimbursementRequest(String flowInstanceId, Long userId) {
        Long instanceId = Long.valueOf(flowInstanceId);
        
        // 查询流程实例
        Instance instance = insService.getById(instanceId);
        if (instance == null) {
            throw new RuntimeException("流程实例不存在");
        }

        // 终止流程 - 使用 TaskService.terminationByInsId
        taskService.terminationByInsId(instanceId, FlowParams.build());

        // 更新报销申请状态为已撤销
        ReimbursementRequest reimbursementRequest = this.lambdaQuery()
                .eq(ReimbursementRequest::getFlowInstanceId, instanceId)
                .one();
        if (reimbursementRequest != null) {
            reimbursementRequest.setStatus(4); // 已撤销
            this.updateById(reimbursementRequest);
        }

        log.info("撤销报销申请，流程实例ID: {}", flowInstanceId);
    }

    @Override
    public Map<String, Object> getReimbursementRequestDetail(String flowInstanceId) {
        Map<String, Object> result = new HashMap<>();

        // 查询报销申请
        Long instanceId = Long.valueOf(flowInstanceId);
        ReimbursementRequest reimbursementRequest = this.lambdaQuery()
                .eq(ReimbursementRequest::getFlowInstanceId, instanceId)
                .one();
        result.put("reimbursementRequest", reimbursementRequest);

        // 查询流程实例
        Instance instance = insService.getById(instanceId);
        result.put("instance", instance);

        return result;
    }
}
