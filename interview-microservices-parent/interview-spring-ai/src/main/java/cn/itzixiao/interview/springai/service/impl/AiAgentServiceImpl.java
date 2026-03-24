package cn.itzixiao.interview.springai.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.itzixiao.interview.springai.dto.AgentTaskRequest;
import cn.itzixiao.interview.springai.dto.AgentTaskResponse;
import cn.itzixiao.interview.springai.dto.ChatRequest;
import cn.itzixiao.interview.springai.dto.ChatResponse;
import cn.itzixiao.interview.springai.service.AiAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AI 智能体服务实现
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl implements AiAgentService {

    private final ChatClient chatClient;
    private final StreamingChatClient streamingChatClient;

    @Override
    public ChatResponse chat(ChatRequest request) {
        try {
            String sessionId = Objects.requireNonNullElse(request.getSessionId(), IdUtil.fastSimpleUUID());

            // 构建消息列表
            List<Message> messages = buildMessages(request);

            // 创建 Prompt
            Prompt prompt = new Prompt(messages);

            // 调用 AI
            org.springframework.ai.chat.ChatResponse aiResponse = chatClient.call(prompt);

            // 构建响应
            String content = aiResponse.getResult().getOutput().getContent();

            return ChatResponse.success(content, sessionId);

        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage(), e);
            return ChatResponse.failure("聊天服务异常: " + e.getMessage());
        }
    }

    @Override
    public Flux<ChatResponse> chatStream(ChatRequest request) {
        String sessionId = Objects.requireNonNullElse(request.getSessionId(), IdUtil.fastSimpleUUID());

        List<Message> messages = buildMessages(request);
        Prompt prompt = new Prompt(messages);

        StringBuilder fullContent = new StringBuilder();

        return Flux.create(sink -> {
            try {
                streamingChatClient.stream(prompt).subscribe(
                        chunk -> {
                            // 兼容 thinking 模型：content 可能为 null（thinking 阶段），
                            // 也可能是正文 token，累积后在 onComplete 一次性推送
                            String text = chunk.getResult().getOutput().getContent();
                            if (text != null && !text.isEmpty()) {
                                fullContent.append(text);
                                sink.next(ChatResponse.builder()
                                        .content(text)
                                        .sessionId(sessionId)
                                        .success(true)
                                        .build());
                            }
                        },
                        error -> {
                            log.error("Stream chat error: {}", error.getMessage(), error);
                            // 若累积了内容但中途报错，仍将已有内容返回
                            if (!fullContent.isEmpty()) {
                                sink.next(ChatResponse.builder()
                                        .content(fullContent.toString())
                                        .sessionId(sessionId)
                                        .success(true)
                                        .build());
                            } else {
                                sink.next(ChatResponse.failure("流式聊天异常: " + error.getMessage()));
                            }
                            sink.complete();
                        },
                        () -> {
                            // 若整个流结束后没有推送过任何 token（纯 thinking 模型场景），
                            // 说明模型回复内容全部在 reasoning_content，此时推送一个提示
                            if (fullContent.isEmpty()) {
                                log.warn("Stream completed but no content received, model may only return reasoning_content");
                                sink.next(ChatResponse.builder()
                                        .content("[模型已完成思考，但未输出正文内容，请尝试非 thinking 模式或检查模型配置]")
                                        .sessionId(sessionId)
                                        .success(true)
                                        .build());
                            }
                            sink.complete();
                        }
                );
            } catch (Exception e) {
                log.error("Stream chat error: {}", e.getMessage(), e);
                sink.next(ChatResponse.failure("流式聊天异常: " + e.getMessage()));
                sink.complete();
            }
        });
    }

    @Override
    public AgentTaskResponse executeTask(AgentTaskRequest request) {
        long startTime = System.currentTimeMillis();
        String taskId = IdUtil.fastSimpleUUID();

        try {
            // 构建系统提示词
            String systemPrompt = buildSystemPrompt(request);

            // 构建用户提示词
            String userPrompt = buildUserPrompt(request);

            // 创建 Prompt
            List<Message> messages = List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            );
            Prompt prompt = new Prompt(messages);

            // 调用 AI
            org.springframework.ai.chat.ChatResponse aiResponse = chatClient.call(prompt);
            String result = aiResponse.getResult().getOutput().getContent();

            // 解析执行步骤
            List<AgentTaskResponse.ExecutionStep> steps = parseExecutionSteps(result);

            long duration = System.currentTimeMillis() - startTime;

            return new AgentTaskResponse(
                    taskId,
                    AgentTaskResponse.ExecutionStatus.SUCCESS,
                    result,
                    steps,
                    request.getAvailableTools(),
                    duration,
                    null
            );

        } catch (Exception e) {
            log.error("Task execution error: {}", e.getMessage(), e);
            return new AgentTaskResponse(
                    taskId,
                    AgentTaskResponse.ExecutionStatus.FAILED,
                    null,
                    null,
                    null,
                    System.currentTimeMillis() - startTime,
                    "任务执行异常: " + e.getMessage()
            );
        }
    }

    @Override
    public Flux<AgentTaskResponse> executeTaskStream(AgentTaskRequest request) {
        String taskId = IdUtil.fastSimpleUUID();
        long startTime = System.currentTimeMillis();

        String systemPrompt = buildSystemPrompt(request);
        String userPrompt = buildUserPrompt(request);

        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        );
        Prompt prompt = new Prompt(messages);

        StringBuilder fullContent = new StringBuilder();

        return Flux.create(sink -> {
            try {
                streamingChatClient.stream(prompt).subscribe(
                        chunk -> {
                            String text = chunk.getResult().getOutput().getContent();
                            if (text != null && !text.isEmpty()) {
                                fullContent.append(text);
                                sink.next(new AgentTaskResponse(
                                        taskId,
                                        AgentTaskResponse.ExecutionStatus.IN_PROGRESS,
                                        text,
                                        null,
                                        null,
                                        System.currentTimeMillis() - startTime,
                                        null
                                ));
                            }
                        },
                        error -> {
                            log.error("Stream task error: {}", error.getMessage(), error);
                            sink.next(new AgentTaskResponse(
                                    taskId, AgentTaskResponse.ExecutionStatus.FAILED,
                                    null, null, null,
                                    System.currentTimeMillis() - startTime,
                                    "流式任务异常: " + error.getMessage()
                            ));
                            sink.complete();
                        },
                        sink::complete
                );
            } catch (Exception e) {
                log.error("Stream task error: {}", e.getMessage(), e);
                sink.next(new AgentTaskResponse(
                        taskId, AgentTaskResponse.ExecutionStatus.FAILED,
                        null, null, null,
                        System.currentTimeMillis() - startTime,
                        "流式任务异常: " + e.getMessage()
                ));
                sink.complete();
            }
        });
    }

    /**
     * 构建消息列表
     */
    private List<Message> buildMessages(ChatRequest request) {
        List<Message> messages = new ArrayList<>();

        // 添加系统消息
        messages.add(new SystemMessage("你是一个专业的 AI 助手，擅长回答技术问题。"));

        // 添加历史消息
        if (request.getHistory() != null) {
            for (ChatRequest.MessageHistory history : request.getHistory()) {
                switch (history.getRole()) {
                    case "user" -> messages.add(new UserMessage(history.getContent()));
                    case "assistant" ->
                            messages.add(new org.springframework.ai.chat.messages.AssistantMessage(history.getContent()));
                    case "system" -> messages.add(new SystemMessage(history.getContent()));
                }
            }
        }

        // 添加当前用户消息
        messages.add(new UserMessage(request.getMessage()));

        return messages;
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(AgentTaskRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能 Agent，可以执行复杂任务。");
        prompt.append("请按照以下步骤思考并解决问题：\n");
        prompt.append("1. 分析任务需求\n");
        prompt.append("2. 制定执行计划\n");
        prompt.append("3. 执行计划\n");
        prompt.append("4. 总结结果\n");

        if (request.getAvailableTools() != null && !request.getAvailableTools().isEmpty()) {
            prompt.append("\n可用工具：").append(String.join(", ", request.getAvailableTools()));
        }

        return prompt.toString();
    }

    /**
     * 构建用户提示词
     */
    private String buildUserPrompt(AgentTaskRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("任务类型：").append(request.getTaskType()).append("\n");
        prompt.append("任务描述：").append(request.getTask()).append("\n");

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            prompt.append("上下文：").append(request.getContext()).append("\n");
        }

        return prompt.toString();
    }

    /**
     * 解析执行步骤
     */
    private List<AgentTaskResponse.ExecutionStep> parseExecutionSteps(String result) {
        // 简化实现，实际应用中可以使用更复杂的解析逻辑
        List<AgentTaskResponse.ExecutionStep> steps = new ArrayList<>();

        steps.add(new AgentTaskResponse.ExecutionStep(
                1,
                "分析任务",
                "analyze",
                "任务分析完成",
                true
        ));

        steps.add(new AgentTaskResponse.ExecutionStep(
                2,
                "生成回答",
                "generate",
                "回答生成完成",
                true
        ));

        return steps;
    }
}
