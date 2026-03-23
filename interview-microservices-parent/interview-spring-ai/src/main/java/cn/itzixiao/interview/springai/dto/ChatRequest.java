package cn.itzixiao.interview.springai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 聊天请求 DTO
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户消息内容
     */
    private String message;

    /**
     * 会话ID（用于多轮对话）
     */
    private String sessionId;

    /**
     * 历史消息（可选）
     */
    private List<MessageHistory> history;

    /**
     * 模型参数
     */
    private ModelOptions options;

    /**
     * 消息历史记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageHistory implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 角色：user/assistant/system
         */
        private String role;

        /**
         * 消息内容
         */
        private String content;
    }

    /**
     * 模型参数选项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelOptions implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 温度参数 (0-2)
         */
        private Double temperature;

        /**
         * 最大输出 token 数
         */
        private Integer maxTokens;

        /**
         * 模型名称
         */
        private String model;
    }
}
