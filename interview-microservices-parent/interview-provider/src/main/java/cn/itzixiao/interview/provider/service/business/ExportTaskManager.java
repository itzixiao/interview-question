package cn.itzixiao.interview.provider.service.business;

import cn.itzixiao.interview.provider.dto.ExportTaskDTO;
import cn.itzixiao.interview.provider.enums.ExportTaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导出任务管理服务
 */
@Slf4j
@Service
public class ExportTaskManager {
    
    /**
     * 任务存储（内存存储，生产环境建议使用 Redis）
     * Key: taskId, Value: ExportTaskDTO
     */
    private final Map<String, ExportTaskDTO> taskMap = new ConcurrentHashMap<>();
    
    /**
     * 创建新的导出任务
     * 
     * @param exportType 导出类型
     * @return 任务 DTO
     */
    public ExportTaskDTO createTask(String exportType) {
        String taskId = generateTaskId();
        
        ExportTaskDTO task = ExportTaskDTO.builder()
                .taskId(taskId)
                .exportType(exportType)
                .status(ExportTaskStatus.PENDING)
                .progress(0)
                .createTime(LocalDateTime.now())
                .build();
        
        taskMap.put(taskId, task);
        log.info("创建导出任务：taskId={}, exportType={}", taskId, exportType);
        
        return task;
    }
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务 ID
     * @return 任务 DTO（Optional）
     */
    public Optional<ExportTaskDTO> getTask(String taskId) {
        return Optional.ofNullable(taskMap.get(taskId));
    }
    
    /**
     * 更新任务状态为处理中
     * 
     * @param taskId 任务 ID
     * @param progress 进度（0-100）
     */
    public void updateTaskProcessing(String taskId, int progress) {
        ExportTaskDTO task = taskMap.get(taskId);
        if (task != null) {
            task.setStatus(ExportTaskStatus.PROCESSING);
            task.setProgress(progress);
            log.debug("任务处理中：taskId={}, progress={}", taskId, progress);
        }
    }
    
    /**
     * 标记任务完成
     * 
     * @param taskId 任务 ID
     * @param filePath 文件路径
     * @param fileName 文件名
     * @param totalRecords 总记录数
     */
    public void completeTask(String taskId, String filePath, String fileName, int totalRecords) {
        ExportTaskDTO task = taskMap.get(taskId);
        if (task != null) {
            task.setStatus(ExportTaskStatus.COMPLETED);
            task.setProgress(100);
            task.setFilePath(filePath);
            task.setFileName(fileName);
            task.setTotalRecords(totalRecords);
            task.setFinishTime(LocalDateTime.now());
            task.setDownloadUrl("/api/export/download/" + taskId);
            log.info("任务完成：taskId={}, filePath={}, totalRecords={}", taskId, filePath, totalRecords);
            
            // ✅ 优化：完成后 24 小时自动清理（避免内存泄漏）
            scheduleCleanup(taskId, 24);
        }
    }
    
    /**
     * 标记任务失败
     * 
     * @param taskId 任务 ID
     * @param errorMessage 错误信息
     */
    public void failTask(String taskId, String errorMessage) {
        ExportTaskDTO task = taskMap.get(taskId);
        if (task != null) {
            task.setStatus(ExportTaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            task.setFinishTime(LocalDateTime.now());
            log.error("任务失败：taskId={}, error={}", taskId, errorMessage);
            
            // ✅ 优化：失败任务也自动清理（1 小时后）
            scheduleCleanup(taskId, 1);
        }
    }
    
    /**
     * 定时清理任务（避免内存泄漏）
     * 
     * @param taskId 任务 ID
     * @param delayHours 延迟小时数
     */
    private void scheduleCleanup(String taskId, int delayHours) {
        java.util.concurrent.ScheduledExecutorService scheduler = 
            java.util.concurrent.Executors.newScheduledThreadPool(1);
        
        scheduler.schedule(() -> {
            try {
                ExportTaskDTO removed = taskMap.remove(taskId);
                if (removed != null) {
                    log.info("自动清理任务：taskId={}, 状态={}", taskId, removed.getStatus());
                    
                    // 同时删除文件
                    if (removed.getFilePath() != null) {
                        File file = new File(removed.getFilePath());
                        if (file.exists()) {
                            file.delete();
                            log.info("删除过期文件：{}", removed.getFilePath());
                        }
                    }
                }
            } catch (Exception e) {
                log.error("清理任务失败：taskId={}", taskId, e);
            }
        }, delayHours, java.util.concurrent.TimeUnit.HOURS);
    }
    
    /**
     * 生成唯一任务 ID
     * 
     * @return 任务 ID
     */
    private String generateTaskId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * 清理已完成的任务（定期清理）
     * 
     * @param expireHours 过期时间（小时）
     * @return 清理的任务数量
     */
    public int cleanupExpiredTasks(int expireHours) {
        LocalDateTime expireTime = LocalDateTime.now().minusHours(expireHours);
        
        int cleaned = 0;
        for (Map.Entry<String, ExportTaskDTO> entry : taskMap.entrySet()) {
            ExportTaskDTO task = entry.getValue();
            if (task.getFinishTime() != null && task.getFinishTime().isBefore(expireTime)) {
                taskMap.remove(entry.getKey());
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            log.info("清理过期任务：清理了 {} 个任务", cleaned);
        }
        
        return cleaned;
    }
    
    /**
     * 获取所有任务（用于管理后台）
     * 
     * @return 所有任务
     */
    public Map<String, ExportTaskDTO> getAllTasks() {
        return new ConcurrentHashMap<>(taskMap);
    }
}
