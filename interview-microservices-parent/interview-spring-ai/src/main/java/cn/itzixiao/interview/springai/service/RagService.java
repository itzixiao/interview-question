package cn.itzixiao.interview.springai.service;

import cn.itzixiao.interview.springai.entity.KnowledgeDocument;
import cn.itzixiao.interview.springai.repository.KnowledgeDocumentRepository;
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
    private final KnowledgeDocumentRepository documentRepository;

    /**
     * 加载 PDF 文档到知识库（简化版，实际应用中可添加 PDF 解析依赖）
     *
     * @param text     文本内容
     * @param metadata 文档元数据
     */
    public void loadDocument(String text, Map<String, Object> metadata) {
        log.info("加载文档到知识库");
        loadText(text, metadata);
    }

    /**
     * 加载文本到知识库
     *
     * @param text     文本内容
     * @param metadata 文档元数据
     */
    public void loadText(String text, Map<String, Object> metadata) {
        log.info("加载文本到知识库");

        // 按段落切分
        String[] paragraphs = text.split("\n\n");
        List<Document> documents = new ArrayList<>();
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                documents.add(new Document(paragraph.trim(), metadata));
            }
        }

        // 存储到向量数据库并同步持久化到 knowledge_document 表
        try {
            vectorStore.add(documents);
        } catch (Exception e) {
            log.warn("向量存储失败（DashScope 不可用），内容已保存至数据库: {}", e.getMessage());
        }

        // 持久化元数据到 knowledge_document 表（如果外部尚未保存记录）
        String title = metadata.getOrDefault("title", "未命名文档").toString();
        String filename = metadata.getOrDefault("filename", title).toString();
        boolean exists = documentRepository.findByTitleContaining(title)
                .stream().anyMatch(d -> title.equals(d.getTitle()));
        if (!exists) {
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setTitle(title);
            doc.setFilename(filename);
            doc.setContent(text.substring(0, Math.min(text.length(), 1000)));
            doc.setChunkCount(documents.size());
            doc.setCharCount(text.length());
            doc.setDocType(getDocType(filename));
            Object sizeObj = metadata.get("size");
            if (sizeObj instanceof Long size) {
                doc.setFileSize(size);
            } else if (sizeObj instanceof Number n) {
                doc.setFileSize(n.longValue());
            }
            documentRepository.save(doc);
            log.info("知识库文档已持久化: title={}, chunks={}", title, documents.size());
        }

        log.info("文本加载完成，共 {} 个片段", documents.size());
    }

    /**
     * 根据文件名获取文档类型
     */
    private String getDocType(String filename) {
        if (filename == null) return "unknown";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".txt")) return "text";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "word";
        return "text";
    }

    /**
     * 基于知识库回答问题
     *
     * @param question 用户问题
     * @param topK     检索相似文档数量
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
        try {
            org.springframework.ai.chat.ChatResponse response = chatClient.call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            log.warn("AI 调用失败（DashScope 不可用），返回知识库检索结果: {}", e.getMessage());
            if (!similarDocuments.isEmpty()) {
                return "《知识库检索结果》（AI 当前不可用，以下是相关内容）：\n\n" +
                        similarDocuments.stream()
                                .map(d -> "- " + d.getContent())
                                .collect(Collectors.joining("\n"));
            }
            return "当前 AI 服务不可用（DashScope 接口无法连接），请检查 DASHSCOPE_API_KEY 环境变量是否正确配置后重试。";
        }
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
     * 检索相似文档（优先向量检索，失败时降级为关键词检索）
     *
     * @param query 查询文本
     * @param topK  返回数量
     * @return 相似文档列表
     */
    public List<Document> retrieveSimilarDocuments(String query, int topK) {
        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(topK);
            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.warn("向量检索失败（可能是 DashScope 网络不通），降级为关键词检索: {}", e.getMessage());
            return fallbackKeywordSearch(query, topK);
        }
    }

    /**
     * 关键词降级检索（从数据库中按标题和内容匹配）
     */
    private List<Document> fallbackKeywordSearch(String query, int topK) {
        log.info("执行关键词降级检索: query={}", query);
        String[] keywords = query.split("[\\s，,。.！!？?]+");

        List<KnowledgeDocument> docs = documentRepository.findAll();
        return docs.stream()
                .filter(doc -> {
                    String combined = (doc.getTitle() + " " + (doc.getContent() != null ? doc.getContent() : "")).toLowerCase();
                    for (String kw : keywords) {
                        if (kw.length() > 1 && combined.contains(kw.toLowerCase())) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(topK)
                .map(doc -> new Document(
                        doc.getTitle() + "\n" + (doc.getContent() != null ? doc.getContent() : ""),
                        Map.of("title", doc.getTitle(), "source", "keyword-search")
                ))
                .collect(Collectors.toList());
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
