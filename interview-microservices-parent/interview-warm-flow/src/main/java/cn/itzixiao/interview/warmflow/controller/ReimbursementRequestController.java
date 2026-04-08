package cn.itzixiao.interview.warmflow.controller;

import cn.itzixiao.interview.warmflow.entity.ReimbursementRequest;
import cn.itzixiao.interview.warmflow.service.ReimbursementRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报销审批 Controller
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/api/workflow/reimbursement")
@RequiredArgsConstructor
@Tag(name = "报销审批", description = "报销申请与审批相关接口")
public class ReimbursementRequestController {

    private final ReimbursementRequestService reimbursementRequestService;

    @PostMapping("/submit")
    @Operation(summary = "提交报销申请")
    public Map<String, Object> submitReimbursementRequest(@RequestBody ReimbursementRequest reimbursementRequest) {
        String flowInstanceId = reimbursementRequestService.submitReimbursementRequest(reimbursementRequest);
        return Map.of(
            "code", 200,
            "message", "提交成功",
            "flowInstanceId", flowInstanceId
        );
    }

    @PostMapping("/approve")
    @Operation(summary = "审批报销申请")
    public Map<String, Object> approveReimbursementRequest(
            @RequestParam String flowInstanceId,
            @RequestParam Boolean approved,
            @RequestParam(required = false) String comment,
            @RequestParam Long approverId) {
        reimbursementRequestService.approveReimbursementRequest(flowInstanceId, approved, comment, approverId);
        return Map.of(
            "code", 200,
            "message", approved ? "审批通过" : "审批驳回"
        );
    }

    @PostMapping("/cancel")
    @Operation(summary = "撤销报销申请")
    public Map<String, Object> cancelReimbursementRequest(
            @RequestParam String flowInstanceId,
            @RequestParam Long userId) {
        reimbursementRequestService.cancelReimbursementRequest(flowInstanceId, userId);
        return Map.of(
            "code", 200,
            "message", "撤销成功"
        );
    }

    @GetMapping("/detail")
    @Operation(summary = "查询报销申请详情")
    public Map<String, Object> getDetail(@RequestParam String flowInstanceId) {
        Map<String, Object> detail = reimbursementRequestService.getReimbursementRequestDetail(flowInstanceId);
        return Map.of(
            "code", 200,
            "message", "查询成功",
            "data", detail
        );
    }
}
