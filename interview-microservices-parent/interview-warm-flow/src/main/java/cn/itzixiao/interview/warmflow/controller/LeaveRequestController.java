package cn.itzixiao.interview.warmflow.controller;

import cn.itzixiao.interview.warmflow.entity.LeaveRequest;
import cn.itzixiao.interview.warmflow.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 请假审批 Controller
 * 
 * @author itzixiao
 * @since 2026-04-08
 */
@RestController
@RequestMapping("/api/workflow/leave")
@RequiredArgsConstructor
@Tag(name = "请假审批", description = "请假申请与审批相关接口")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping("/submit")
    @Operation(summary = "提交请假申请")
    public Map<String, Object> submitLeaveRequest(@RequestBody LeaveRequest leaveRequest) {
        String flowInstanceId = leaveRequestService.submitLeaveRequest(leaveRequest);
        return Map.of(
            "code", 200,
            "message", "提交成功",
            "flowInstanceId", flowInstanceId
        );
    }

    @PostMapping("/approve")
    @Operation(summary = "审批请假申请")
    public Map<String, Object> approveLeaveRequest(
            @RequestParam String flowInstanceId,
            @RequestParam Boolean approved,
            @RequestParam(required = false) String comment,
            @RequestParam Long approverId) {
        leaveRequestService.approveLeaveRequest(flowInstanceId, approved, comment, approverId);
        return Map.of(
            "code", 200,
            "message", approved ? "审批通过" : "审批驳回"
        );
    }

    @PostMapping("/cancel")
    @Operation(summary = "撤销请假申请")
    public Map<String, Object> cancelLeaveRequest(
            @RequestParam String flowInstanceId,
            @RequestParam Long userId) {
        leaveRequestService.cancelLeaveRequest(flowInstanceId, userId);
        return Map.of(
            "code", 200,
            "message", "撤销成功"
        );
    }

    @GetMapping("/detail")
    @Operation(summary = "查询请假申请详情")
    public Map<String, Object> getDetail(@RequestParam String flowInstanceId) {
        Map<String, Object> detail = leaveRequestService.getLeaveRequestDetail(flowInstanceId);
        return Map.of(
            "code", 200,
            "message", "查询成功",
            "data", detail
        );
    }
}
