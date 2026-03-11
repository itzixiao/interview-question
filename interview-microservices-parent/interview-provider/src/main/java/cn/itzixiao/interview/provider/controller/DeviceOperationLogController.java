package cn.itzixiao.interview.provider.controller;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.service.DeviceOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 设备运行日志导出测试接口
 * 
 * 三种测试场景：
 * 1. 全表导出 - 测试是否 OOM
 * 2. 按时间范围导出 - 测试索引是否生效
 * 3. 分页导出 - 对比内存占用差异
 */
@Slf4j
@RestController
@RequestMapping("/api/export")
public class DeviceOperationLogController {

    private final DeviceOperationLogService deviceOperationLogService;

    public DeviceOperationLogController(DeviceOperationLogService deviceOperationLogService) {
        this.deviceOperationLogService = deviceOperationLogService;
    }

    /**
     * 测试场景 1：直接查询全表导出
     * 
     * 风险提示：可能导致 OOM（OutOfMemoryError）
     * 建议：数据量超过 10 万条时谨慎使用
     * 
     * ⚠️ 重要提示：Excel (.xlsx) 最大支持 1,048,576 行
     * 如果数据量超出限制，请使用以下替代方案：
     * 1. 分页导出：/api/export/page
     * 2. 时间范围筛选：/api/export/by-time
     * 3. 多 Sheet 模式：/api/export/all-multi-sheet
     * 
     * 访问方式：
     * GET http://localhost:8082/api/export/all
     * 直接在浏览器打开或 Postman 下载
     */
    @GetMapping("/all")
    public void exportAll(HttpServletResponse response) {
        log.warn("===========================================");
        log.warn("【警告】正在执行全表导出，请密切观察内存使用情况！");
        log.warn("===========================================");
        
        try {
            deviceOperationLogService.exportAll(response);
        } catch (IllegalArgumentException e) {
            // 数据量超限的友好提示
            log.error("数据量超限：{}", e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("全表导出失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        } catch (OutOfMemoryError e) {
            log.error("发生 OOM！全表导出导致内存溢出", e);
            throw new RuntimeException("内存不足，请使用分页导出或多 Sheet 模式");
        }
    }

    /**
     * 测试场景 1（增强版）：全表导出 - 多 Sheet 自动拆分模式
     * 
     * 优势：自动将大数据量拆分为多个 Sheet，每个 Sheet 最多 10 万行
     * 适用场景：数据量超过 100 万行的超大表格
     * 
     * 访问方式：
     * GET http://localhost:8082/api/export/all-multi-sheet
     */
    @GetMapping("/all-multi-sheet")
    public void exportAllMultiSheet(HttpServletResponse response) {
        log.info("===========================================");
        log.info("【增强版】正在执行全表导出（多 Sheet 模式）");
        log.info("===========================================");
        
        try {
            deviceOperationLogService.exportAllWithMultiSheet(response);
        } catch (IOException e) {
            log.error("多 Sheet 导出失败", e);
            throw new RuntimeException("导出失败：" + e.getMessage());
        }
    }

    /**
     * 测试场景 2：按时间范围导出
     * 
     * 优势：利用索引减少查询数据量
     * 说明：operation_time 字段有索引，应该走 idx_operation_time
     * 
     * 访问方式：
     * GET http://localhost:8082/api/export/by-time?startTime=2025-01-01T00:00:00
     * 
     * @param startTime 开始时间（ISO-8601 格式）
     */
    @GetMapping("/by-time")
    public void exportByTimeRange(
            HttpServletResponse response,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime) {
        log.info("===========================================");
        log.info("【测试场景 2】按时间范围导出，起始时间：{}", startTime);
        log.info("===========================================");
        
        try {
            deviceOperationLogService.exportByTimeRange(response, startTime);
        } catch (IOException e) {
            log.error("按时间范围导出失败", e);
        }
    }

    /**
     * 测试场景 3：分页导出
     * 
     * 推荐方式：适合大数据量场景，内存占用可控
     * 默认每页 1000 条，可自定义
     * 
     * 访问方式：
     * GET http://localhost:8082/api/export/page
     * GET http://localhost:8082/api/export/page?pageSize=500
     * 
     * @param pageSize 每页大小（可选，默认 1000）
     */
    @GetMapping("/page")
    public void exportPageByPage(
            HttpServletResponse response,
            @RequestParam(required = false, defaultValue = "1000") Integer pageSize) {
        log.info("===========================================");
        log.info("【测试场景 3】分页导出，每页大小：{} 条", pageSize);
        log.info("===========================================");
        
        try {
            deviceOperationLogService.exportPageByPage(response, pageSize);
        } catch (IOException e) {
            log.error("分页导出失败", e);
        }
    }

    /**
     * 获取统计数据
     * 
     * 用于查看当前表中的数据总量
     * 
     * 访问方式：
     * GET http://localhost:8082/api/export/stats
     * 
     * @return 统计信息
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            Long totalCount = deviceOperationLogService.getTotalCount();
            stats.put("totalCount", totalCount);
            stats.put("description", "总记录数，用于评估导出测试的数据规模");
            
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return Result.error("获取统计数据失败：" + e.getMessage());
        }
    }
}
