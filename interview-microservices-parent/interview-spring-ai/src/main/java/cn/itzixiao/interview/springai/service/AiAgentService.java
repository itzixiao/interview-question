package cn.itzixiao.interview.springai.service;

import cn.itzixiao.interview.springai.dto.AgentTaskRequest;
import cn.itzixiao.interview.springai.dto.AgentTaskResponse;
import cn.itzixiao.interview.springai.dto.ChatRequest;
import cn.itzixiao.interview.springai.dto.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI 智能体服务接口
 * 
 * <p>定义智能体的核心能力，包括：</p>
 * <ul>
 *     <li>对话聊天：与 AI 进行多轮对话</li>
 *     <li>任务执行：执行复杂任务并返回结构化结果</li>
 *     <li>流式响应：实时返回 AI 生成的内容</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2024-01-01
 */
public interface AiAgentService {

    /**
     * 简单对话聊天
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式对话聊天
     *
     * @param request 聊天请求
     * @return 流式响应
     */
    Flux<ChatResponse> chatStream(ChatRequest request);

    /**
     * 执行 Agent 任务
     *
     * @param request 任务请求
     * @return 任务响应
     */
    AgentTaskResponse executeTask(AgentTaskRequest request);

    /**
     * 流式执行 Agent 任务
     *
     * @param request 任务请求
     * @return 流式任务响应
     */
    Flux<AgentTaskResponse> executeTaskStream(AgentTaskRequest request);
}
