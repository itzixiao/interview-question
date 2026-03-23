package cn.itzixiao.interview.springai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 聊天响应 DTO
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应ID
     */
    private String id;

    /**
     * AI 回复内容
     */
    private String content;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 使用的 token 数量
     */
    private TokenUsage tokenUsage;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * Token 使用情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 提示词 token 数
         */
        private Integer promptTokens;

        /**
         * 生成 token 数
         */
        private Integer completionTokens;

        /**
         * 总 token 数
         */
        private Integer totalTokens;
    }

    /**
     * 创建成功响应
     */
    public static ChatResponse success(String content, String sessionId) {
        return ChatResponse.builder()
                .content(content)
                .sessionId(sessionId)
                .success(true)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static ChatResponse failure(String errorMessage) {
        return ChatResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
