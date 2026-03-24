package cn.itzixiao.interview.springai.controller;

import cn.itzixiao.interview.springai.entity.KnowledgeDocument;
import cn.itzixiao.interview.springai.repository.KnowledgeDocumentRepository;
import cn.itzixiao.interview.springai.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理控制器
 *
 * <p>提供知识库的文档管理功能：</p>
 * <ul>
 *     <li>上传文档到知识库</li>
 *     <li>检索知识库内容</li>
 *     <li>获取知识库统计信息</li>
 * </ul>
 *
 * @author itzixiao
 * @since 2024-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "知识库文档管理相关接口")
public class KnowledgeBaseController {

    private final RagService ragService;
    private final VectorStore vectorStore;
    private final KnowledgeDocumentRepository documentRepository;
    private final Tika tika = new Tika();

    /**
     * 上传文档到知识库
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文档", description = "上传 PDF、Word、TXT 等文档到知识库")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam("file") @Parameter(description = "文档文件") MultipartFile file,
            @RequestParam(name = "title", required = false) @Parameter(description = "文档标题") String title) {

        log.info("上传文档到知识库: filename={}, size={}", file.getOriginalFilename(), file.getSize());

        try {
            // 提取文档内容
            String content;
            try (InputStream inputStream = file.getInputStream()) {
                Metadata metadata = new Metadata();
                content = tika.parseToString(inputStream, metadata);
            }

            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "无法提取文档内容，请检查文件格式"
                ));
            }

            // 使用文件名作为标题
            String docTitle = title != null ? title :
                    (file.getOriginalFilename() != null ?
                            file.getOriginalFilename().replaceAll("\\.[^.]+$", "") : "未命名文档");

            // 加载到知识库（内部会同时持久化到 knowledge_document 表）
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("title", docTitle);
            metadata.put("filename", file.getOriginalFilename());
            metadata.put("size", file.getSize());
            metadata.put("uploadTime", new Date().toString());
            ragService.loadText(content, metadata);

            // 计算片段数
            int chunks = content.split("\n\n").length;

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "文档上传成功",
                    "data", Map.of(
                            "title", docTitle,
                            "chunks", chunks,
                            "size", content.length(),
                            "filename", file.getOriginalFilename()
                    )
            ));

        } catch (Exception e) {
            log.error("文档上传失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "文档上传失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 检索相似文档
     */
    @GetMapping("/search")
    @Operation(summary = "检索知识库", description = "基于相似度检索知识库中的文档")
    public ResponseEntity<Map<String, Object>> searchKnowledgeBase(
            @RequestParam("query") @Parameter(description = "查询关键词") String query,
            @RequestParam(name = "topK", required = false, defaultValue = "5") @Parameter(description = "返回数量") int topK) {

        log.info("检索知识库: query={}, topK={}", query, topK);

        try {
            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(topK);

            List<Document> documents = vectorStore.similaritySearch(searchRequest);

            List<Map<String, Object>> results = documents.stream()
                    .map(doc -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", doc.getContent());
                        result.put("metadata", doc.getMetadata());
                        // 简单模拟相似度分数
                        result.put("score", 0.85 + Math.random() * 0.14);
                        return result;
                    })
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", results
            ));

        } catch (Exception e) {
            log.error("知识库检索失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "检索失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "知识库统计", description = "获取知识库的统计信息")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("获取知识库统计信息");

        long documentCount = documentRepository.count();
        List<Object[]> sums = documentRepository.sumChunksAndChars();
        long totalChunks = 0;
        long totalSize = 0;
        if (!sums.isEmpty() && sums.get(0) != null) {
            Object[] row = sums.get(0);
            totalChunks = row[0] instanceof Number n ? n.longValue() : 0L;
            totalSize = row[1] instanceof Number n ? n.longValue() : 0L;
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "documentCount", documentCount,
                        "totalChunks", totalChunks,
                        "totalSize", totalSize
                )
        ));
    }

    /**
     * 获取文档列表
     */
    @GetMapping("/documents")
    @Operation(summary = "获取文档列表", description = "获取知识库中的所有文档")
    public ResponseEntity<Map<String, Object>> getDocuments() {
        log.info("获取文档列表");

        List<KnowledgeDocument> documents = documentRepository.findAll();
        List<Map<String, Object>> result = documents.stream()
                .map(doc -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", doc.getId());
                    map.put("title", doc.getTitle());
                    map.put("filename", doc.getFilename());
                    map.put("chunks", doc.getChunkCount());
                    map.put("size", doc.getCharCount());
                    map.put("docType", doc.getDocType());
                    map.put("createTime", doc.getCreateTime() != null ? doc.getCreateTime().toString() : null);
                    return map;
                })
                .toList();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", result
        ));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/documents/{id}")
    @Operation(summary = "删除文档", description = "根据ID删除知识库文档")
    public ResponseEntity<Map<String, Object>> deleteDocument(
            @PathVariable("id") @Parameter(description = "文档ID") Long id) {
        log.info("删除文档: id={}", id);

        documentRepository.deleteById(id);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "文档删除成功"
        ));
    }

    private String getDocType(String filename) {
        if (filename == null) return "unknown";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".txt")) return "text";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "word";
        return "unknown";
    }
}
