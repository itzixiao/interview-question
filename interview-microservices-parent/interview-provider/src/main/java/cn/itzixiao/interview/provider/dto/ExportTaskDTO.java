package cn.itzixiao.interview.provider.dto;

import cn.itzixiao.interview.provider.enums.ExportTaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 导出任务 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportTaskDTO {
    
    /**
     * 任务 ID（唯一标识）
     */
    private String taskId;
    
    /**
     * 导出类型（all, by-time, page）
     */
    private String exportType;
    
    /**
     * 任务状态
     */
    private ExportTaskStatus status;
    
    /**
     * 文件路径（完成后才有）
     */
    private String filePath;
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 总记录数
     */
    private Integer totalRecords;
    
    /**
     * 错误信息（失败时）
     */
    private String errorMessage;
    
    /**
     * 进度百分比（0-100）
     */
    private Integer progress;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 完成时间
     */
    private LocalDateTime finishTime;
    
    /**
     * 下载 URL（完成后生成）
     */
    private transient String downloadUrl;
}
