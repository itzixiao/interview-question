package cn.itzixiao.interview.springai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 任务响应 DTO
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 执行状态
     */
    private ExecutionStatus status;

    /**
     * 最终结果
     */
    private String result;

    /**
     * 执行步骤记录
     */
    private List<ExecutionStep> steps;

    /**
     * 使用的工具
     */
    private List<String> usedTools;

    /**
     * 总耗时（毫秒）
     */
    private Long durationMs;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行状态枚举
     */
    public enum ExecutionStatus {
        IN_PROGRESS,
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED,
        TIMEOUT
    }

    /**
     * 执行步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStep implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 步骤序号
         */
        private Integer stepNumber;

        /**
         * 步骤描述
         */
        private String description;

        /**
         * 执行动作
         */
        private String action;

        /**
         * 执行结果
         */
        private String result;

        /**
         * 是否成功
         */
        private Boolean success;
    }
}
