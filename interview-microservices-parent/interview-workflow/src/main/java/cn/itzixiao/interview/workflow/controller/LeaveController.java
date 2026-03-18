package cn.itzixiao.interview.workflow.controller;

import cn.itzixiao.interview.workflow.dto.ApprovalDTO;
import cn.itzixiao.interview.workflow.dto.LeaveApplyDTO;
import cn.itzixiao.interview.workflow.entity.Leave;
import cn.itzixiao.interview.workflow.service.LeaveService;
import cn.itzixiao.interview.workflow.util.Result;
import cn.itzixiao.interview.workflow.vo.PendingLeaveVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 请假审批控制器
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@Api(tags = "请假审批")
@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private TaskService taskService;

    /**
     * 提交请假申请
     */
    @ApiOperation(value = "提交请假申请", notes = "提交请假申请并启动审批流程")
    @PostMapping("/apply")
    public Result<Long> apply(@Validated @RequestBody LeaveApplyDTO dto) {
        Long leaveId = leaveService.apply(dto);
        return Result.success(leaveId);
    }

    /**
     * 审批（通过/驳回）
     */
    @ApiOperation(value = "审批请假", notes = "审批请假申请：通过或驳回")
    @PostMapping("/approve")
    public Result<Void> approve(@Validated @RequestBody ApprovalDTO dto) {
        leaveService.approve(dto);
        return Result.success();
    }

    /**
     * 重新提交（驳回后修改）
     */
    @ApiOperation(value = "重新提交", notes = "驳回后修改并重新提交")
    @PostMapping("/resubmit/{id}")
    public Result<Void> resubmit(
            @ApiParam(value = "请假申请ID", required = true) @PathVariable Long id,
            @Validated @RequestBody LeaveApplyDTO dto) {
        leaveService.resubmit(id, dto);
        return Result.success();
    }

    /**
     * 撤回申请
     */
    @ApiOperation(value = "撤回申请", notes = "撤回待审批的请假申请")
    @PostMapping("/withdraw/{id}")
    public Result<Void> withdraw(
            @ApiParam(value = "请假申请ID", required = true) @PathVariable Long id) {
        leaveService.withdraw(id);
        return Result.success();
    }

    /**
     * 我的申请列表
     */
    @ApiOperation(value = "我的请假申请列表", notes = "查询当前用户发起的请假申请")
    @GetMapping("/my-list")
    public Result<IPage<Leave>> myList(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") long pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") long pageSize,
            @ApiParam(value = "状态：0草稿/1审批中/2已通过/3已拒绝/4已撤回") @RequestParam(required = false) Integer status) {
        return Result.success(leaveService.myList(new Page<>(pageNum, pageSize), status));
    }

    /**
     * 待我审批列表（带 taskId，供前端审批使用）
     * 返回统一分页结构 {records, total, current, size}
     */
    @ApiOperation(value = "待我审批列表", notes = "查询待当前用户审批的请假列表")
    @GetMapping("/pending-list")
    public Result<Map<String, Object>> pendingList(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") long pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") long pageSize) {
        IPage<Leave> page = leaveService.pendingList(new Page<>(pageNum, pageSize));

        // 查出当前用户所有待审批任务 processInstanceId -> taskId 映射
        List<Task> tasks = taskService.createTaskQuery()
                .orderByTaskCreateTime().desc()
                .list();
        Map<String, Task> processTaskMap = tasks.stream()
                .collect(Collectors.toMap(
                        Task::getProcessInstanceId, t -> t, (t1, t2) -> t1));

        List<PendingLeaveVO> voList = new ArrayList<>();
        for (Leave leave : page.getRecords()) {
            PendingLeaveVO vo = new PendingLeaveVO();
            BeanUtils.copyProperties(leave, vo);
            Task task = processTaskMap.get(leave.getProcessInstanceId());
            if (task != null) {
                vo.setTaskId(task.getId());
                vo.setTaskName(task.getName());
                vo.setTaskCreateTime(task.getCreateTime() != null ? task.getCreateTime().toString() : null);
            }
            voList.add(vo);
        }

        // 封装为统一分页结构，前端用 res.data.records / res.data.total
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("records", voList);
        result.put("total", page.getTotal());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        return Result.success(result);
    }

    /**
     * 申请详情
     */
    @ApiOperation(value = "请假详情", notes = "获取请假申请详细信息")
    @GetMapping("/{id}")
    public Result<Leave> detail(
            @ApiParam(value = "请假申请ID", required = true) @PathVariable Long id) {
        return Result.success(leaveService.detail(id));
    }
}
