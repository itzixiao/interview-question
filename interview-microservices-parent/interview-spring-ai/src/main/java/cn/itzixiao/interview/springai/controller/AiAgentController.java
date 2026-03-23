package cn.itzixiao.interview.springai.controller;

import cn.itzixiao.interview.springai.dto.AgentTaskRequest;
import cn.itzixiao.interview.springai.dto.AgentTaskResponse;
import cn.itzixiao.interview.springai.dto.ChatRequest;
import cn.itzixiao.interview.springai.dto.ChatResponse;
import cn.itzixiao.interview.springai.service.AiAgentService;
import cn.itzixiao.interview.springai.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * AI 智能体控制器
 * 
 * <p>提供 AI 智能体的 REST API 接口，包括：</p>
 * <ul>
 *     <li>对话聊天接口</li>
 *     <li>流式对话接口</li>
 *     <li>Agent 任务执行接口</li>
 *     <li>RAG 知识库问答接口</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 智能体", description = "Spring AI 智能体相关接口")
public class AiAgentController {

    private final AiAgentService aiAgentService;
    private final RagService ragService;

    /**
     * 简单对话聊天
     */
    @PostMapping("/chat")
    @Operation(summary = "简单对话聊天", description = "与 AI 进行单轮或多轮对话")
    public ChatResponse chat(
            @RequestBody @Parameter(description = "聊天请求") ChatRequest request) {
        log.info("收到聊天请求: {}", request.getMessage());
        return aiAgentService.chat(request);
    }

    /**
     * 流式对话聊天
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话聊天", description = "与 AI 进行流式对话，实时返回生成的内容")
    public Flux<ChatResponse> chatStream(
            @RequestBody @Parameter(description = "聊天请求") ChatRequest request) {
        log.info("收到流式聊天请求: {}", request.getMessage());
        return aiAgentService.chatStream(request);
    }

    /**
     * 执行 Agent 任务
     */
    @PostMapping("/agent/task")
    @Operation(summary = "执行 Agent 任务", description = "执行复杂的 AI Agent 任务，支持多种任务类型")
    public AgentTaskResponse executeTask(
            @RequestBody @Parameter(description = "任务请求") AgentTaskRequest request) {
        log.info("收到 Agent 任务请求: type={}, task={}", request.getTaskType(), request.getTask());
        return aiAgentService.executeTask(request);
    }

    /**
     * 流式执行 Agent 任务
     */
    @PostMapping(value = "/agent/task/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式执行 Agent 任务", description = "流式执行 AI Agent 任务")
    public Flux<AgentTaskResponse> executeTaskStream(
            @RequestBody @Parameter(description = "任务请求") AgentTaskRequest request) {
        log.info("收到流式 Agent 任务请求: type={}, task={}", request.getTaskType(), request.getTask());
        return aiAgentService.executeTaskStream(request);
    }

    /**
     * RAG 知识库问答
     */
    @PostMapping("/rag/ask")
    @Operation(summary = "RAG 知识库问答", description = "基于知识库进行检索增强问答")
    public ChatResponse ragAsk(
            @RequestParam @Parameter(description = "问题") String question,
            @RequestParam(required = false, defaultValue = "3") @Parameter(description = "检索文档数量") int topK) {
        log.info("收到 RAG 问答请求: question={}, topK={}", question, topK);
        
        String answer = ragService.answerQuestion(question, topK);
        
        return ChatResponse.builder()
                .content(answer)
                .success(true)
                .build();
    }

    /**
     * 加载文本到知识库
     */
    @PostMapping("/rag/load-text")
    @Operation(summary = "加载文本到知识库", description = "将文本内容加载到知识库中")
    public Map<String, Object> loadText(
            @RequestParam @Parameter(description = "文本内容") String text,
            @RequestParam(required = false) @Parameter(description = "文档标题") String title) {
        log.info("加载文本到知识库: title={}", title);
        
        Map<String, Object> metadata = Map.of("title", title != null ? title : "未命名文档");
        ragService.loadText(text, metadata);
        
        return Map.of(
                "success", true,
                "message", "文本已加载到知识库"
        );
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查 AI 服务状态")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "spring-ai",
                "message", "AI 智能体服务运行正常"
        );
    }
}
