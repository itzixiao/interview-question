package cn.itzixiao.interview.provider.controller.common;

import cn.itzixiao.interview.common.result.Result;
import cn.itzixiao.interview.provider.dto.ExportTaskDTO;
import cn.itzixiao.interview.provider.service.business.ExportTaskManager;
import cn.itzixiao.interview.provider.service.sharding.DeviceOperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private final ExportTaskManager exportTaskManager;

    public DeviceOperationLogController(DeviceOperationLogService deviceOperationLogService,
                                        @Autowired(required = false) ExportTaskManager exportTaskManager) {
        this.deviceOperationLogService = deviceOperationLogService;
        this.exportTaskManager = exportTaskManager;
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

    // ==================== 异步导出接口 ====================

    /**
     * 异步导出全表数据（多 Sheet 模式）
     * 
     * 特性：
     * 1. 立即返回任务 ID，不等待导出完成
     * 2. 后台线程池异步处理
     * 3. 可通过任务 ID 查询进度
     * 4. 完成后下载文件
     * 
     * 适用场景：大数据量导出（> 100 万条）
     * 
     * 访问方式：
     * POST http://localhost:8082/api/export/async/all
     * 
     * @return 任务信息（包含 taskId）
     */
    @PostMapping("/async/all")
    public Result<Map<String, Object>> asyncExportAll() {
        if (exportTaskManager == null) {
            return Result.error("异步导出功能未启用");
        }
        
        try {
            // 创建任务
            ExportTaskDTO task = exportTaskManager.createTask("all-multi-sheet");
            
            // 提交异步任务
            deviceOperationLogService.asyncExportAllWithMultiSheet(task.getTaskId());
            
            log.info("【异步导出】任务已提交：taskId={}", task.getTaskId());
            
            // 返回任务信息
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getTaskId());
            result.put("status", task.getStatus().name());
            result.put("message", "任务已提交，请使用 taskId 查询进度");
            result.put("queryUrl", "/api/export/async/status/" + task.getTaskId());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("异步导出任务提交失败", e);
            return Result.error("任务提交失败：" + e.getMessage());
        }
    }

    /**
     * 查询异步任务状态
     * 
     * 访问方式：
     * GET http://localhost:8082/api/export/async/status/{taskId}
     * 
     * @param taskId 任务 ID
     * @return 任务状态
     */
    @GetMapping("/async/status/{taskId}")
    public Result<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        if (exportTaskManager == null) {
            return Result.error("异步导出功能未启用");
        }
        
        try {
            ExportTaskDTO task = exportTaskManager.getTask(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
            
            Map<String, Object> status = new HashMap<>();
            status.put("taskId", task.getTaskId());
            status.put("exportType", task.getExportType());
            status.put("status", task.getStatus().name());
            status.put("statusDescription", task.getStatus().getDescription());
            status.put("progress", task.getProgress());
            status.put("totalRecords", task.getTotalRecords());
            status.put("createTime", task.getCreateTime());
            status.put("finishTime", task.getFinishTime());
            status.put("errorMessage", task.getErrorMessage());
            
            // 如果已完成，添加下载链接
            if (task.getStatus() == cn.itzixiao.interview.provider.enums.ExportTaskStatus.COMPLETED) {
                status.put("fileName", task.getFileName());
                status.put("downloadUrl", task.getDownloadUrl());
            }
            
            return Result.success(status);
        } catch (Exception e) {
            log.error("查询任务状态失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 下载已完成的导出文件
     * 
     * 访问方式：
     * GET http://localhost:8082/api/export/download/{taskId}
     * 
     * @param taskId 任务 ID
     * @param response HTTP 响应
     */
    @GetMapping("/download/{taskId}")
    public void downloadFile(@PathVariable String taskId, HttpServletResponse response) {
        if (exportTaskManager == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        try {
            ExportTaskDTO task = exportTaskManager.getTask(taskId)
                    .orElseThrow(() -> new IllegalArgumentException("任务不存在：" + taskId));
            
            // 检查任务状态
            if (task.getStatus() != cn.itzixiao.interview.provider.enums.ExportTaskStatus.COMPLETED) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("任务未完成，当前状态：" + task.getStatus().getDescription());
                return;
            }
            
            // 检查文件是否存在
            File file = new File(task.getFilePath());
            if (!file.exists()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("文件不存在或已过期删除");
                return;
            }
            
            // 设置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String encodedFileName = URLEncoder.encode(task.getFileName(), StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
            
            // 写入文件流
            try (InputStream inputStream = new FileInputStream(file);
                 OutputStream outputStream = response.getOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
            
            log.info("文件下载成功：taskId={}, filePath={}", taskId, task.getFilePath());
            
        } catch (Exception e) {
            log.error("文件下载失败：taskId={}", taskId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
