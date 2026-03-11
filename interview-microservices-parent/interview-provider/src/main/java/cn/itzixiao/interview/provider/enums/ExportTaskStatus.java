package cn.itzixiao.interview.provider.enums;

/**
 * 导出任务状态枚举
 */
public enum ExportTaskStatus {
    
    /**
     * 等待处理
     */
    PENDING("等待处理"),
    
    /**
     * 处理中
     */
    PROCESSING("处理中"),
    
    /**
     * 已完成
     */
    COMPLETED("已完成"),
    
    /**
     * 失败
     */
    FAILED("失败"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消");
    
    private final String description;
    
    ExportTaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
