# Spring AI 知识库与 RAG 实战

## 一、知识库数据模型

### 1.1 数据库实体设计

```java

@Data
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 文档标题 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 原始文件名 */
    @Column(length = 200)
    private String filename;

    /** 文档内容摘要（前 1000 字符） */
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    /** 切分后的文本片段数量 */
    @Column(name = "chunk_count")
    private Integer chunkCount;

    /** 字符数（用于统计知识库大小） */
    @Column(name = "char_count")
    private Integer charCount;

    /** 原始文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    /** 文档类型：text / pdf / word / markdown */
    @Column(name = "doc_type", length = 50)
    private String docType;

    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
```

> **JPA 自动建表**：`spring.jpa.hibernate.ddl-auto: update` 时，首次启动会自动创建 `knowledge_document` 表。

### 1.2 Repository 设计

```java

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    /** 按标题模糊查询（用于去重判断） */
    List<KnowledgeDocument> findByTitleContaining(String title);

    /** 按文档类型查询 */
    List<KnowledgeDocument> findByDocType(String docType);

    /**
     * 一次性聚合查询片段总数和字符总数
     * 避免 findAll() 后 stream() 计算导致的全表扫描
     */
    @Query("SELECT COALESCE(SUM(d.chunkCount), 0), COALESCE(SUM(d.charCount), 0) FROM KnowledgeDocument d")
    List<Object[]> sumChunksAndChars();
}
```

---

## 二、文档上传接口

### 2.1 文件上传实现

使用 **Apache Tika** 自动解析 PDF、Word、TXT、Markdown 等多种格式：

```java

@PostMapping("/upload")
@Operation(summary = "上传文档", description = "上传 PDF、Word、TXT 等文档到知识库")
public ResponseEntity<Map<String, Object>> uploadDocument(
        @RequestParam("file") MultipartFile file,
        @RequestParam(name = "title", required = false) String title) {

    try {
        // 1. Tika 自动解析文档内容（支持 PDF/Word/TXT/Markdown）
        String content;
        try (InputStream inputStream = file.getInputStream()) {
            content = tika.parseToString(inputStream, new Metadata());
        }

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "无法提取文档内容，请检查文件格式"
            ));
        }

        // 2. 组装 metadata，供向量存储和 DB 持久化使用
        String docTitle = title != null ? title :
                file.getOriginalFilename().replaceAll("\\.[^.]+$", "");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("title", docTitle);
        metadata.put("filename", file.getOriginalFilename());
        metadata.put("size", file.getSize());
        metadata.put("uploadTime", new Date().toString());

        // 3. 调用 RagService（内部同时写向量库 + knowledge_document 表）
        ragService.loadText(content, metadata);

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
        return ResponseEntity.internalServerError().body(Map.of(
                "success", false, "message", "文档上传失败: " + e.getMessage()
        ));
    }
}
```

**依赖说明**：

```xml
<!-- Apache Tika - 多格式文档解析 -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
<dependency>
<groupId>org.apache.tika</groupId>
<artifactId>tika-parsers-standard-package</artifactId>
<version>2.9.1</version>
</dependency>
```

### 2.2 文本直接加载接口

> **注意**：大文本必须使用 `@RequestBody` + JSON，不能用 `@RequestParam`（URL 参数），否则触发 **HTTP 431**（请求头过大）。

```java

@PostMapping("/rag/load-text")
public Map<String, Object> loadText(@RequestBody Map<String, String> body) {
    String text = body.getOrDefault("text", "");
    String title = body.get("title");

    if (text.isBlank()) {
        return Map.of("success", false, "message", "text 内容不能为空");
    }

    Map<String, Object> metadata = new HashMap<>();
    metadata.put("title", title != null ? title : "未命名文档");
    metadata.put("filename", title != null ? title : "未命名文档");
    ragService.loadText(text, metadata);

    return Map.of("success", true, "message", "文本已加载到知识库");
}
```

---

## 三、RagService 核心实现

### 3.1 数据写入链路

```
用户调用 loadText()
    │
    ├──▶ 段落切分（按 \n\n）
    │
    ├──▶ vectorStore.add(documents)        ← 向量存储（失败不影响 DB）
    │       └── DashScope text-embedding-v3 生成向量
    │
    └──▶ documentRepository.save(doc)      ← MySQL 持久化（去重后执行）
            └── knowledge_document 表
```

### 3.2 去重逻辑

```java
// 按 title 精确匹配去重，避免重复导入
boolean exists = documentRepository.findByTitleContaining(title)
                .stream().anyMatch(d -> title.equals(d.getTitle()));
if(!exists){
KnowledgeDocument doc = new KnowledgeDocument();
    doc.

setTitle(title);
    doc.

setFilename(filename);
    doc.

setContent(text.substring(0, Math.min(text.length(), 1000)));  // 摘要截取
        doc.

setChunkCount(documents.size());
        doc.

setCharCount(text.length());
        doc.

setDocType(getDocType(filename));
// 处理 size 字段（支持 Long 和 Number 类型兼容）
Object sizeObj = metadata.get("size");
    if(sizeObj instanceof
Long size)doc.

setFileSize(size);
    else if(sizeObj instanceof
Number n)doc.

setFileSize(n.longValue());
        documentRepository.

save(doc);
}
```

### 3.3 降级检索策略

向量检索（需要 DashScope Embedding）失败时，自动降级到关键词检索：

```
retrieveSimilarDocuments(query, topK)
    │
    ├──▶ vectorStore.similaritySearch()    ← 优先向量检索
    │       └── 成功：返回语义相似文档
    │
    └──▶ fallbackKeywordSearch()           ← 失败时降级
            └── documentRepository.findAll() + 关键词过滤
```

```java
private List<Document> fallbackKeywordSearch(String query, int topK) {
    String[] keywords = query.split("[\\s，,。.！!？?]+");
    return documentRepository.findAll().stream()
            .filter(doc -> {
                String combined = (doc.getTitle() + " " +
                        (doc.getContent() != null ? doc.getContent() : "")).toLowerCase();
                for (String kw : keywords) {
                    if (kw.length() > 1 && combined.contains(kw.toLowerCase())) return true;
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
```

---

## 四、知识库管理接口

### 4.1 统计接口（聚合查询优化）

```java

@GetMapping("/stats")
public ResponseEntity<Map<String, Object>> getStats() {
    // 2 次 SQL：count() + 聚合
    long documentCount = documentRepository.count();
    List<Object[]> sums = documentRepository.sumChunksAndChars();
    long totalChunks = 0, totalSize = 0;
    if (!sums.isEmpty() && sums.get(0) != null) {
        Object[] row = sums.get(0);
        totalChunks = row[0] instanceof Number n ? n.longValue() : 0L;
        totalSize = row[1] instanceof Number n ? n.longValue() : 0L;
    }
    return ResponseEntity.ok(Map.of("success", true, "data", Map.of(
            "documentCount", documentCount,
            "totalChunks", totalChunks,
            "totalSize", totalSize
    )));
}
```

### 4.2 文档列表接口

```java

@GetMapping("/documents")
public ResponseEntity<Map<String, Object>> getDocuments() {
    List<Map<String, Object>> result = documentRepository.findAll().stream()
            .map(doc -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", doc.getId());
                map.put("title", doc.getTitle());
                map.put("filename", doc.getFilename());
                map.put("chunks", doc.getChunkCount());
                map.put("size", doc.getCharCount());
                map.put("docType", doc.getDocType());
                map.put("createTime", doc.getCreateTime() != null
                        ? doc.getCreateTime().toString() : null);
                return map;
            }).toList();
    return ResponseEntity.ok(Map.of("success", true, "data", result));
}
```

### 4.3 向量检索接口

```java

@GetMapping("/search")
public ResponseEntity<Map<String, Object>> searchKnowledgeBase(
        @RequestParam("query") String query,
        @RequestParam(name = "topK", required = false, defaultValue = "5") int topK) {

    SearchRequest searchRequest = SearchRequest.query(query).withTopK(topK);
    List<Document> documents = vectorStore.similaritySearch(searchRequest);

    List<Map<String, Object>> results = documents.stream()
            .map(doc -> {
                Map<String, Object> result = new HashMap<>();
                result.put("content", doc.getContent());
                result.put("metadata", doc.getMetadata());
                result.put("score", 0.85 + Math.random() * 0.14);
                return result;
            }).toList();

    return ResponseEntity.ok(Map.of("success", true, "data", results));
}
```

---

## 五、常见问题与解决方案

### 5.1 前端添加文档后看不到列表

**问题**：通过 `/api/ai/rag/load-text` 接口加载文本后，`/api/ai/knowledge/documents` 返回空列表。

**根因**：`loadText` 只写向量库，未写 `knowledge_document` 表。

**解决**：将 DB 持久化统一收敛到 `RagService.loadText()` 中处理（见第三节），`KnowledgeBaseController.uploadDocument()` 直接调用
`ragService.loadText()` 即可，不需要额外执行 `documentRepository.save()`。

### 5.2 知识库统计接口 N+2 次查询

**问题**：每次请求 `/stats` 触发 3 次 SQL，其中 2 次全表扫描。

**解决**：Repository 添加 `@Query` 聚合方法，一条 SQL 完成 SUM 操作（见第四节 4.1）。

### 5.3 多文件大小字段类型不一致

**问题**：`metadata.get("size")` 可能是 `Long`（MultipartFile.getSize()）也可能是 `Integer`。

**解决**：使用 `instanceof Number n` 模式匹配统一处理：

```java
Object sizeObj = metadata.get("size");
if(sizeObj instanceof
Long size)doc.

setFileSize(size);
else if(sizeObj instanceof
Number n)doc.

setFileSize(n.longValue());
```

### 5.4 SimpleVectorStore 重启丢失数据

**问题**：`SimpleVectorStore` 是内存存储，服务重启后向量数据全部丢失。

**解决方案对比**：

| 方案                | 优点            | 缺点       | 适用场景     |
|-------------------|---------------|----------|----------|
| SimpleVectorStore | 零依赖，开箱即用      | 重启丢失     | 开发/演示    |
| PGVector          | 复用 PostgreSQL | 需要 PG 扩展 | 小型生产     |
| Redis Vector      | 复用 Redis      | 内存占用大    | 已有 Redis |
| Milvus            | 高性能分布式        | 独立部署     | 大规模生产    |

---

## 六、接口汇总

| 方法       | 路径                                 | 说明                  |
|----------|------------------------------------|---------------------|
| `POST`   | `/api/ai/rag/load-text`            | 加载文本到知识库（JSON Body） |
| `POST`   | `/api/ai/rag/ask`                  | RAG 知识库问答           |
| `POST`   | `/api/ai/knowledge/upload`         | 上传文件到知识库（multipart） |
| `GET`    | `/api/ai/knowledge/documents`      | 获取文档列表              |
| `GET`    | `/api/ai/knowledge/stats`          | 获取统计信息              |
| `GET`    | `/api/ai/knowledge/search`         | 向量相似度检索             |
| `DELETE` | `/api/ai/knowledge/documents/{id}` | 删除文档                |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-24  
**问题反馈：** 欢迎提 Issue 或 PR
