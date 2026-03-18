package cn.itzixiao.interview.workflow.service;

import cn.itzixiao.interview.workflow.dto.ApprovalDTO;
import cn.itzixiao.interview.workflow.dto.LeaveApplyDTO;
import cn.itzixiao.interview.workflow.entity.Leave;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 请假审批服务接口
 *
 * @author itzixiao
 * @date 2026-03-17
 */
public interface LeaveService {

    /**
     * 提交请假申请
     *
     * @param dto 请假申请数据
     * @return 请假申请ID
     */
    Long apply(LeaveApplyDTO dto);

    /**
     * 执行审批（通过/驳回）
     *
     * @param dto 审批操作数据
     */
    void approve(ApprovalDTO dto);

    /**
     * 重新提交（驳回后修改重提）
     *
     * @param leaveId 请假申请ID
     * @param dto     修改后的申请数据
     */
    void resubmit(Long leaveId, LeaveApplyDTO dto);

    /**
     * 撤回申请
     *
     * @param leaveId 请假申请ID
     */
    void withdraw(Long leaveId);

    /**
     * 分页查询我的请假申请列表
     *
     * @param page   分页参数
     * @param status 状态过滤（可空）
     * @return 分页结果
     */
    IPage<Leave> myList(Page<Leave> page, Integer status);

    /**
     * 分页查询待我审批的请假任务
     *
     * @param page 分页参数
     * @return 分页结果
     */
    IPage<Leave> pendingList(Page<Leave> page);

    /**
     * 获取请假申请详情
     *
     * @param id 请假申请ID
     * @return 详情
     */
    Leave detail(Long id);
}
