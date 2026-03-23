package cn.itzixiao.interview.springai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * Agent 任务请求 DTO
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务描述
     */
    private String task;

    /**
     * 任务类型
     */
    private TaskType taskType;

    /**
     * 上下文信息
     */
    private String context;

    /**
     * 可用工具列表（可选）
     */
    private List<String> availableTools;

    /**
     * 任务类型枚举
     */
    public enum TaskType {
        /**
         * 简单问答
         */
        QA,

        /**
         * 知识库检索
         */
        RAG,

        /**
         * 函数调用
         */
        FUNCTION_CALL,

        /**
         * 复杂任务编排
         */
        ORCHESTRATION,

        /**
         * 代码生成
         */
        CODE_GENERATION,

        /**
         * 数据分析
         */
        DATA_ANALYSIS
    }
}
