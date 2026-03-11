package cn.itzixiao.interview.provider.service;

import cn.itzixiao.interview.provider.dto.ExportTaskDTO;
import cn.itzixiao.interview.provider.entity.DeviceOperationLog;
import cn.itzixiao.interview.provider.enums.ExportTaskStatus;
import cn.itzixiao.interview.provider.mapper.DeviceOperationLogMapper;
import com.alibaba.excel.EasyExcel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备运行日志 Service
 * 
 * 提供三种导出测试场景：
 * 1. 全表导出 - 测试是否 OOM
 * 2. 按时间范围导出 - 测试索引是否生效
 * 3. 分页导出 - 对比内存占用差异
 */
@Slf4j
@Service
public class DeviceOperationLogService {

    private final DeviceOperationLogMapper deviceOperationLogMapper;

    private final ExportTaskManager exportTaskManager;

    public DeviceOperationLogService(DeviceOperationLogMapper deviceOperationLogMapper,
                                     @Autowired(required = false) ExportTaskManager exportTaskManager) {
        this.deviceOperationLogMapper = deviceOperationLogMapper;
        this.exportTaskManager = exportTaskManager;
    }

    /**
     * 测试场景 1：直接查询全表导出
     * 
     * 风险：可能导致 OOM（OutOfMemoryError）
     * 适用场景：数据量较小（< 10 万条）
     * 
     * ⚠️ 注意：Excel (.xlsx) 最大支持 1,048,576 行
     * 
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    public void exportAll(HttpServletResponse response) throws IOException {
        log.warn("===========================================");
        log.warn("【警告】正在执行全表导出，请密切观察内存使用情况！");
        log.warn("===========================================");
        long startTime = System.currentTimeMillis();
        
        // 一次性查询所有数据
        List<DeviceOperationLog> allData = deviceOperationLogMapper.selectAll();
        
        log.info("【测试场景 1】查询完成，共 {} 条记录，耗时 {}ms", 
                allData.size(), System.currentTimeMillis() - startTime);
        
        // Excel (.xlsx) 最大支持 1,048,576 行
        final int MAX_EXCEL_ROWS = 1048576;
        
        if (allData.size() > MAX_EXCEL_ROWS) {
            log.error("【警告】数据量 {} 条超过 Excel 单文件最大行数限制 ({})", 
                    allData.size(), MAX_EXCEL_ROWS);
            log.error("【建议】请使用分页导出功能，或使用多 Sheet 模式导出");
            
            // 抛出友好提示异常
            throw new IllegalArgumentException(
                String.format("数据量过大 (%d 条),超出 Excel 单文件最大行数限制 (%d 行)。\n" +
                             "解决方案：\n" +
                             "  1. 使用分页导出：GET /api/export/page\n" +
                             "  2. 使用时间范围筛选：GET /api/export/by-time?startTime=xxx\n" +
                             "  3. 使用多 Sheet 模式：GET /api/export/all-multi-sheet", 
                             allData.size(), MAX_EXCEL_ROWS));
        }
        
        // 设置响应头
        setResponseHeaders(response, "device_log_all_export.xlsx");
        
        // 使用 EasyExcel 导出
        try {
            EasyExcel.write(response.getOutputStream(), DeviceOperationLog.class)
                    .sheet("设备运行日志")
                    .doWrite(allData);
            
            log.info("【测试场景 1】导出完成，总耗时 {}ms", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("【测试场景 1】Excel 写入失败", e);
            throw e;
        }
    }

    /**
     * 测试场景 1（增强版）：全表导出 - 支持多 Sheet 自动拆分
     * 
     * 风险：可能导致 OOM（OutOfMemoryError）
     * 优势：自动拆分为多个 Sheet，突破单文件行数限制
     * 性能优化：使用多线程准备数据，提升大数据量场景下的导出速度
     * 
     * @param response HTTP 响应
     * @throws IOException IO 异常
     */
    public void exportAllWithMultiSheet(HttpServletResponse response) throws IOException {
        log.warn("===========================================");
        log.warn("【测试场景 1-增强版】正在执行全表导出（多 Sheet 多线程模式）");
        log.warn("===========================================");
        long startTime = System.currentTimeMillis();
        
        // 一次性查询所有数据
        List<DeviceOperationLog> allData = deviceOperationLogMapper.selectAll();
        
        log.info("【测试场景 1-增强版】查询完成，共 {} 条记录，耗时 {}ms", 
                allData.size(), System.currentTimeMillis() - startTime);
        
        // Excel (.xlsx) 最大支持 1,048,576 行
        final int MAX_ROWS_PER_SHEET = 100000; // 每个 Sheet 10 万行，留余量
        
        // 设置响应头
        setResponseHeaders(response, "device_log_all_export_multi_sheet_threaded.xlsx");
        
        // 计算需要多少个 Sheet
        int totalSheets = (allData.size() + MAX_ROWS_PER_SHEET - 1) / MAX_ROWS_PER_SHEET;
        log.info("【测试场景 1-增强版】数据量 {} 条，将拆分为 {} 个 Sheet", allData.size(), totalSheets);
        
        // 创建线程池（CPU 核心数 + 1）
        int threadCount = Math.min(Runtime.getRuntime().availableProcessors() + 1, totalSheets);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        
        // 用于存储每个 Sheet 的数据（线程安全）
        java.util.List<java.util.concurrent.Future<SheetData>> futures = new java.util.ArrayList<>();
        
        try {
            // 多线程并行准备每个 Sheet 的数据
            for (int i = 0; i < totalSheets; i++) {
                final int sheetIndex = i;
                final int startRow = i * MAX_ROWS_PER_SHEET;
                final int endRow = Math.min(startRow + MAX_ROWS_PER_SHEET, allData.size());
                
                futures.add(executor.submit(() -> {
                    long dataStart = System.currentTimeMillis();
                    
                    // 准备当前 Sheet 的数据
                    List<DeviceOperationLog> subList = allData.subList(startRow, endRow);
                    
                    log.info("【线程 {}】Sheet {} 数据准备完成 ({}-{} 行), 耗时 {}ms",
                            Thread.currentThread().getName(), sheetIndex + 1, 
                            startRow + 1, endRow, System.currentTimeMillis() - dataStart);
                    
                    return new SheetData(sheetIndex, subList, startRow, endRow);
                }));
            }
            
            // 关闭线程池，等待所有任务完成
            executor.shutdown();
            boolean completed = executor.awaitTermination(10, java.util.concurrent.TimeUnit.MINUTES);
            
            if (!completed) {
                throw new RuntimeException("数据准备超时");
            }
            
            log.info("【测试场景 1-增强版】所有 Sheet 数据准备完成，开始写入 Excel...");
            
            // 创建 ExcelWriter（单线程写入，保证线程安全）
            com.alibaba.excel.ExcelWriter excelWriter = null;
            try {
                excelWriter = EasyExcel.write(
                        response.getOutputStream(), 
                        DeviceOperationLog.class
                ).build();
                
                // 按顺序写入每个 Sheet（保持 Sheet 顺序）
                for (java.util.concurrent.Future<SheetData> future : futures) {
                    SheetData sheetData;
                    try {
                        sheetData = future.get(); // 可能抛出 ExecutionException
                    } catch (java.util.concurrent.ExecutionException e) {
                        log.error("【测试场景 1-增强版】获取 Sheet 数据失败", e);
                        throw new RuntimeException("获取 Sheet 数据失败：" + e.getCause().getMessage(), e);
                    }
                    
                    long sheetStart = System.currentTimeMillis();
                    
                    // 创建 Sheet
                    com.alibaba.excel.write.metadata.WriteSheet writeSheet = EasyExcel
                            .writerSheet(sheetData.sheetIndex, "数据" + (sheetData.sheetIndex + 1))
                            .head(DeviceOperationLog.class)
                            .build();
                    
                    // 写入当前 Sheet 的数据
                    excelWriter.write(sheetData.data, writeSheet);
                    
                    log.info("【测试场景 1-增强版】Sheet {} 写入完成 ({}-{} 行), 耗时 {}ms", 
                            sheetData.sheetIndex + 1, sheetData.startRow + 1, sheetData.endRow,
                            System.currentTimeMillis() - sheetStart);
                }
                
                log.info("【测试场景 1-增强版】全部导出完成，总计 {}ms", 
                        System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("【测试场景 1-增强版】Excel 写入失败", e);
                throw e;
            } finally {
                // 务必关闭 ExcelWriter
                if (excelWriter != null) {
                    excelWriter.finish();
                }
            }
        } catch (InterruptedException e) {
            log.error("【测试场景 1-增强版】线程中断", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("导出过程中断");
        } catch (Exception e) {
            log.error("【测试场景 1-增强版】导出失败", e);
            throw e;
        } finally {
            // 确保线程池被关闭
            if (!executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }
    
    /**
     * 内部类：封装 Sheet 数据
     */
    private static class SheetData {
        final int sheetIndex;
        final List<DeviceOperationLog> data;
        final int startRow;
        final int endRow;
        
        SheetData(int sheetIndex, List<DeviceOperationLog> data, int startRow, int endRow) {
            this.sheetIndex = sheetIndex;
            this.data = data;
            this.startRow = startRow;
            this.endRow = endRow;
        }
    }

    /**
     * 测试场景 2：按时间范围导出
     * 
     * 优势：利用索引减少查询数据量
     * 注意点：观察执行计划确认索引是否生效
     * 
     * @param response HTTP 响应
     * @param startTime 开始时间
     * @throws IOException IO 异常
     */
    public void exportByTimeRange(HttpServletResponse response, LocalDateTime startTime) throws IOException {
        log.info("【测试场景 2】开始执行按时间范围导出，起始时间：{}", startTime);
        long queryStart = System.currentTimeMillis();
        
        // 按时间范围查询（应该走 idx_operation_time 索引）
        List<DeviceOperationLog> timeRangeData = deviceOperationLogMapper.selectByTimeRange(startTime);
        
        log.info("【测试场景 2】查询完成，共 {} 条记录，耗时 {}ms", 
                timeRangeData.size(), System.currentTimeMillis() - queryStart);
        
        // 设置响应头
        String fileName = "device_log_" + startTime.toString().replace(":", "-") + "_export.xlsx";
        setResponseHeaders(response, fileName);
        
        // 导出
        EasyExcel.write(response.getOutputStream(), DeviceOperationLog.class)
                .sheet("设备运行日志（按时间）")
                .doWrite(timeRangeData);
        
        log.info("【测试场景 2】导出完成，总耗时 {}ms", System.currentTimeMillis() - queryStart);
    }

    /**
     * 测试场景 3：分页导出（优化版）
     * 
     * 优势：控制内存占用，适合大数据量场景
     * 实现方式：分批查询 + 流式写入
     * 
     * @param response HTTP 响应
     * @param pageSize 每页大小（默认 1000）
     * @throws IOException IO 异常
     */
    public void exportPageByPage(HttpServletResponse response, Integer pageSize) throws IOException {
        if (pageSize == null || pageSize <= 0) {
            pageSize = 1000; // 默认每页 1000 条
        }
        
        log.info("【测试场景 3】开始执行分页导出，每页大小：{} 条", pageSize);
        long startTotal = System.currentTimeMillis();
        
        // 设置响应头
        setResponseHeaders(response, "device_log_page_export.xlsx");
        
        // 手动分页查询并写入（演示分页逻辑）
        int current = 0;
        int totalExported = 0;
        
        // 创建 Excel 写入器
        com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(
                response.getOutputStream(), 
                DeviceOperationLog.class
        ).build();
        
        try {
            com.alibaba.excel.write.metadata.WriteSheet writeSheet = EasyExcel.writerSheet("设备运行日志（分页）").build();
            
            while (true) {
                long pageStart = System.currentTimeMillis();
                
                // 分页查询
                List<DeviceOperationLog> pageData = deviceOperationLogMapper.selectPage(current, pageSize);
                
                if (pageData == null || pageData.isEmpty()) {
                    log.info("【测试场景 3】所有数据已导出完毕");
                    break;
                }
                
                // 写入当前页数据
                excelWriter.write(pageData, writeSheet);
                
                totalExported += pageData.size();
                log.info("【测试场景 3】第 {} 页导出完成，{} 条记录，本页耗时 {}ms，累计导出 {} 条",
                        (current / pageSize) + 1, pageData.size(), 
                        System.currentTimeMillis() - pageStart, totalExported);
                
                current += pageSize;
                
                // 防止无限循环（演示用，实际可以注释掉）
                if (totalExported >= 10000) {
                    log.warn("【测试场景 3】已达到演示上限 10000 条，停止导出");
                    break;
                }
            }
        } finally {
            // 务必关闭 ExcelWriter
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
        
        log.info("【测试场景 3】分页导出完成，总计 {}ms，导出 {} 条记录",
                System.currentTimeMillis() - startTotal, totalExported);
    }

    /**
     * 设置 Excel 导出响应头
     * 
     * @param response HTTP 响应
     * @param fileName 文件名
     */
    private void setResponseHeaders(HttpServletResponse response, String fileName) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            // URLEncoder.encode 处理中文文件名
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName);
        } catch (Exception e) {
            log.error("设置响应头失败", e);
        }
    }

    /**
     * 获取总记录数（用于分页计算）
     * 
     * @return 总记录数
     */
    public Long getTotalCount() {
        return deviceOperationLogMapper.selectCount(null);
    }

    /**
     * 获取指定时间范围的记录数
     * 
     * @param startTime 开始时间
     * @return 记录数
     */
    public Long getCountByTimeRange(LocalDateTime startTime) {
        return deviceOperationLogMapper.selectCount(null); // 简化实现，实际应该加条件
    }

    // ==================== 异步导出方法 ====================

    /**
     * 异步导出全表数据（多 Sheet 模式）- 内存优化版
     * 
     * @param taskId 任务 ID
     */
    @Async("exportTaskExecutor")
    public void asyncExportAllWithMultiSheet(String taskId) {
        log.info("【异步导出】开始执行异步导出任务：taskId={}", taskId);
        
        if (exportTaskManager == null) {
            log.error("【异步导出】ExportTaskManager 未启用，无法执行异步任务");
            return;
        }
        
        java.util.concurrent.ExecutorService executor = null;
        try {
            // 更新状态为处理中
            exportTaskManager.updateTaskProcessing(taskId, 10);
            
            // 创建临时目录
            String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "excel-export";
            File dir = new File(tempDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            // 生成文件名
            String fileName = "device_log_async_" + taskId + ".xlsx";
            String filePath = tempDir + File.separator + fileName;
            
            // Excel 参数
            final int MAX_ROWS_PER_SHEET = 100000;
            
            // 先查询总记录数（不加载数据）
            Long totalCount = deviceOperationLogMapper.selectCount(null);
            int totalSheets = (totalCount.intValue() + MAX_ROWS_PER_SHEET - 1) / MAX_ROWS_PER_SHEET;
            
            log.info("【异步导出】数据量 {} 条，将拆分为 {} 个 Sheet", totalCount, totalSheets);
            exportTaskManager.updateTaskProcessing(taskId, 30);
            
            // 创建 ExcelWriter（流式写入，不占用大量内存）
            com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(new File(filePath), DeviceOperationLog.class).build();
            
            try {
                // 多线程准备数据
                int threadCount = Math.min(Runtime.getRuntime().availableProcessors() + 1, totalSheets);
                executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
                
                // ✅ 优化：分批处理，每批处理完立即释放内存
                int batchSize = Math.min(5, totalSheets); // 每批处理 5 个 Sheet
                
                for (int batchStart = 0; batchStart < totalSheets; batchStart += batchSize) {
                    int batchEnd = Math.min(batchStart + batchSize, totalSheets);
                    log.info("【异步导出】处理批次：{}-{}", batchStart + 1, batchEnd);
                    
                    // 当前批次的 Future 列表（批次结束后清空）
                    java.util.List<java.util.concurrent.Future<SheetData>> futures = new java.util.ArrayList<>();
                    
                    try {
                        // 并行准备当前批次的 Sheet 数据
                        for (int i = batchStart; i < batchEnd; i++) {
                            final int sheetIndex = i;
                            final int startRow = i * MAX_ROWS_PER_SHEET;
                            final int endRow = Math.min(startRow + MAX_ROWS_PER_SHEET, totalCount.intValue());
                            final int progress = 30 + ((i * 60) / totalSheets); // 进度 30%-90%
                            
                            futures.add(executor.submit(() -> {
                                // ✅ 优化：分页查询，避免一次性加载
                                List<DeviceOperationLog> subList = deviceOperationLogMapper.selectPage(startRow, endRow - startRow);
                                exportTaskManager.updateTaskProcessing(taskId, progress);
                                return new SheetData(sheetIndex, subList, startRow, endRow);
                            }));
                        }
                        
                        // 等待当前批次所有任务完成
                        for (java.util.concurrent.Future<SheetData> future : futures) {
                            SheetData sheetData = future.get();
                            
                            // 创建 Sheet
                            com.alibaba.excel.write.metadata.WriteSheet writeSheet = EasyExcel
                                    .writerSheet(sheetData.sheetIndex, "数据" + (sheetData.sheetIndex + 1))
                                    .head(DeviceOperationLog.class)
                                    .build();
                            
                            // 写入当前 Sheet 的数据
                            excelWriter.write(sheetData.data, writeSheet);
                            
                            log.info("【异步导出】Sheet {} 写入完成 ({}-{} 行)", 
                                    sheetData.sheetIndex + 1, sheetData.startRow + 1, sheetData.endRow);
                        }
                        
                    } finally {
                        // ✅ 关键：清空当前批次的 Future，释放引用
                        futures.clear();
                        System.gc(); // 提示 JVM 进行 GC
                    }
                }
                
                // 标记任务完成
                exportTaskManager.completeTask(taskId, filePath, fileName, totalCount.intValue());
                log.info("【异步导出】任务完成：taskId={}, filePath={}, 总记录数={}", taskId, filePath, totalCount);
                
            } catch (Exception e) {
                log.error("【异步导出】Excel 写入失败", e);
                throw e;
            } finally {
                // 务必关闭 ExcelWriter
                if (excelWriter != null) {
                    excelWriter.finish();
                }
            }
            
        } catch (Exception e) {
            log.error("【异步导出】任务失败：taskId={}", taskId, e);
            exportTaskManager.failTask(taskId, "导出失败：" + e.getMessage());
        } finally {
            // ✅ 确保线程池被关闭
            if (executor != null && !executor.isShutdown()) {
                executor.shutdownNow();
            }
        }
    }
}
