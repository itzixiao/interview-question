package cn.itzixiao.interview.workflow.controller;

import cn.itzixiao.interview.workflow.dto.ApprovalDTO;
import cn.itzixiao.interview.workflow.dto.ExpenseApplyDTO;
import cn.itzixiao.interview.workflow.entity.Expense;
import cn.itzixiao.interview.workflow.service.ExpenseService;
import cn.itzixiao.interview.workflow.util.Result;
import cn.itzixiao.interview.workflow.vo.PendingExpenseVO;
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
 * 报销审批控制器
 * <p>
 * 提供报销申请的全生命周期管理：提交、审批、重新提交、撤回、查询
 * <p>
 * 审批规则（根据金额分级）：
 * <ul>
 *   <li>金额 < 1000：部门经理审批</li>
 *   <li>1000 <= 金额 <= 5000：财务经理审批</li>
 *   <li>金额 > 5000：总经理审批</li>
 * </ul>
 *
 * @author itzixiao
 * @date 2026-03-17
 */
@Slf4j
@Api(tags = "报销审批")
@RestController
@RequestMapping("/api/expense")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private TaskService taskService;

    /**
     * 提交报销申请
     */
    @ApiOperation(value = "提交报销申请", notes = "提交报销申请并启动审批流程")
    @PostMapping("/apply")
    public Result<Long> apply(@Validated @RequestBody ExpenseApplyDTO dto) {
        return Result.success(expenseService.apply(dto));
    }

    /**
     * 审批（通过/驳回）
     */
    @ApiOperation(value = "审批报销", notes = "审批报销申请：通过或驳回")
    @PostMapping("/approve")
    public Result<Void> approve(@Validated @RequestBody ApprovalDTO dto) {
        expenseService.approve(dto);
        return Result.success();
    }

    /**
     * 重新提交（驳回后修改）
     */
    @ApiOperation(value = "重新提交", notes = "驳回后修改并重新提交")
    @PostMapping("/resubmit/{id}")
    public Result<Void> resubmit(
            @ApiParam(value = "报销申请ID", required = true) @PathVariable Long id,
            @Validated @RequestBody ExpenseApplyDTO dto) {
        expenseService.resubmit(id, dto);
        return Result.success();
    }

    /**
     * 撤回申请
     */
    @ApiOperation(value = "撤回申请", notes = "撤回待审批的报销申请")
    @PostMapping("/withdraw/{id}")
    public Result<Void> withdraw(
            @ApiParam(value = "报销申请ID", required = true) @PathVariable Long id) {
        expenseService.withdraw(id);
        return Result.success();
    }

    /**
     * 我的申请列表
     */
    @ApiOperation(value = "我的报销申请列表", notes = "查询当前用户发起的报销申请")
    @GetMapping("/my-list")
    public Result<IPage<Expense>> myList(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") long pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") long pageSize,
            @ApiParam(value = "状态：0草稿/1审批中/2已通过/3已拒绝/4已撤回") @RequestParam(required = false) Integer status) {
        return Result.success(expenseService.myList(new Page<>(pageNum, pageSize), status));
    }

    /**
     * 待我审批列表（带 taskId，供前端审批使用）
     */
    @ApiOperation(value = "待我审批列表", notes = "查询待当前用户审批的报销列表")
    @GetMapping("/pending-list")
    public Result<Map<String, Object>> pendingList(
            @ApiParam(value = "页码", defaultValue = "1") @RequestParam(defaultValue = "1") long pageNum,
            @ApiParam(value = "每页数量", defaultValue = "10") @RequestParam(defaultValue = "10") long pageSize) {
        IPage<Expense> page = expenseService.pendingList(new Page<>(pageNum, pageSize));

        List<Task> tasks = taskService.createTaskQuery()
                .orderByTaskCreateTime().desc()
                .list();
        Map<String, Task> processTaskMap = tasks.stream()
                .collect(Collectors.toMap(Task::getProcessInstanceId, t -> t, (t1, t2) -> t1));

        List<PendingExpenseVO> voList = new ArrayList<>();
        for (Expense expense : page.getRecords()) {
            PendingExpenseVO vo = new PendingExpenseVO();
            BeanUtils.copyProperties(expense, vo);
            Task task = processTaskMap.get(expense.getProcessInstanceId());
            if (task != null) {
                vo.setTaskId(task.getId());
                vo.setTaskName(task.getName());
                vo.setTaskCreateTime(task.getCreateTime() != null ? task.getCreateTime().toString() : null);
            }
            voList.add(vo);
        }

        // 封装为统一分页结构
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
    @ApiOperation(value = "报销详情", notes = "获取报销申请详细信息")
    @GetMapping("/{id}")
    public Result<Expense> detail(
            @ApiParam(value = "报销申请ID", required = true) @PathVariable Long id) {
        return Result.success(expenseService.detail(id));
    }
}
