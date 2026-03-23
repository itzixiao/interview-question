package cn.itzixiao.interview.springai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) 服务
 * 
 * <p>实现基于知识库的检索增强生成功能：</p>
 * <ul>
 *     <li>文档加载和切分</li>
 *     <li>向量化存储</li>
 *     <li>相似度检索</li>
 *     <li>上下文增强生成</li>
 * </ul>
 * 
 * @author itzixiao
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    /**
     * 加载 PDF 文档到知识库（简化版，实际应用中可添加 PDF 解析依赖）
     *
     * @param text 文本内容
     * @param metadata 文档元数据
     */
    public void loadDocument(String text, Map<String, Object> metadata) {
        log.info("加载文档到知识库");
        loadText(text, metadata);
    }

    /**
     * 加载文本到知识库
     *
     * @param text 文本内容
     * @param metadata 文档元数据
     */
    public void loadText(String text, Map<String, Object> metadata) {
        log.info("加载文本到知识库");
        
        // 简单切分：按段落切分
        String[] paragraphs = text.split("\n\n");
        List<Document> documents = new ArrayList<>();
        
        for (int i = 0; i < paragraphs.length; i++) {
            if (!paragraphs[i].trim().isEmpty()) {
                Document doc = new Document(paragraphs[i].trim(), metadata);
                documents.add(doc);
            }
        }
        
        // 存储到向量数据库
        vectorStore.add(documents);
        
        log.info("文本加载完成，共 {} 个片段", documents.size());
    }

    /**
     * 基于知识库回答问题
     *
     * @param question 用户问题
     * @param topK 检索相似文档数量
     * @return AI 回答
     */
    public String answerQuestion(String question, int topK) {
        log.info("RAG 问答: question={}, topK={}", question, topK);
        
        // 检索相似文档
        List<Document> similarDocuments = retrieveSimilarDocuments(question, topK);
        
        // 构建上下文
        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));
        
        // 构建提示词
        String systemPrompt = """
            你是一个专业的知识库助手。请基于以下知识库内容回答用户问题。
            如果知识库中没有相关信息，请明确告知用户。
            
            知识库内容：
            %s
            """.formatted(context);
        
        List<Message> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(question)
        );
        
        Prompt prompt = new Prompt(messages);
        
        // 调用 AI
        org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
        
        return response.getResult().getOutput().getContent();
    }

    /**
     * 基于知识库回答问题（默认 topK=3）
     *
     * @param question 用户问题
     * @return AI 回答
     */
    public String answerQuestion(String question) {
        return answerQuestion(question, 3);
    }

    /**
     * 检索相似文档
     *
     * @param query 查询文本
     * @param topK 返回数量
     * @return 相似文档列表
     */
    public List<Document> retrieveSimilarDocuments(String query, int topK) {
        SearchRequest searchRequest = SearchRequest.query(query)
                .withTopK(topK);
        
        return vectorStore.similaritySearch(searchRequest);
    }

    /**
     * 清空知识库
     */
    public void clearKnowledgeBase() {
        log.info("清空知识库");
        // 注意：SimpleVectorStore 不支持直接清空，需要重新创建
        // 实际应用中可以使用 Redis、PostgreSQL 等持久化向量存储
    }
}
