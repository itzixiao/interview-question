# Spring AI 智能体详解

## 一、概述

### 1.1 什么是 Spring AI

Spring AI 是 Spring 官方推出的 AI 集成框架，旨在简化 AI 能力在 Spring 应用中的集成。它提供了统一的抽象层，支持多种 AI
模型提供商：

| AI 提供商           | 模型类型           | 特点                  |
|------------------|----------------|---------------------|
| OpenAI           | Chat/Embedding | GPT 系列，功能最全面        |
| Azure OpenAI     | Chat/Embedding | 企业级，数据合规            |
| Ollama           | Chat/Embedding | 本地部署，隐私保护           |
| Anthropic Claude | Chat           | 长上下文，安全对齐           |
| Google Gemini    | Chat/Embedding | 多模态，Google 生态       |
| 阿里云 DashScope    | Chat/Embedding | 通义千问系列，兼容 OpenAI 接口 |
| 百度文心一言           | Chat           | 国产模型，企业应用           |

> **本项目实际接入**：使用阿里云 DashScope OpenAI 兼容接口，模型 `qwen-plus`，Embedding 模型 `text-embedding-v3`。

### 1.2 主流大语言模型对比

| 模型             | 厂商        | 特点            | 适用场景      |
|----------------|-----------|---------------|-----------|
| **GPT-4o**     | OpenAI    | 能力强，多模态       | 复杂推理、代码生成 |
| **Claude 3**   | Anthropic | 上下文长（200K）    | 长文档分析、写作  |
| **Gemini**     | Google    | 多模态，Google 生态 | 图文混合任务    |
| **通义千问（qwen）** | 阿里        | 开源可商用，中文优化    | 企业私有化部署   |
| **文心一言**       | 百度        | 中文优化          | 国内应用      |
| **Llama 3**    | Meta      | 完全开源免费        | 私有化本地部署   |
| **ChatGLM**    | 智谱AI      | 中英双语，可本地部署    | 私有化中文场景   |

**模型选择策略**：

```
是否需要私有化部署？
  ├── 是 → 选择开源模型（Llama / ChatGLM / Qwen 开源版）
  └── 否 → 预算充足？
        ├── 是 → GPT-4 / Claude 3
        └── 否 → 国产 API（通义千问 / 文心）
```

**LLM 与 Spring AI 的关系**：Spring AI 提供统一的 Java SDK 抽象层，无论底层使用哪家模型，代码逻辑完全一致，只需切换配置文件即可。

### 1.3 核心功能

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Spring AI 核心能力                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐ │
│  │   Chat 聊天   │   │  RAG 知识库   │   │ Function Call│   │   Agent 编排  │ │
│  │              │   │              │   │              │   │              │ │
│  │  多轮对话      │   │  文档向量化    │   │  工具调用      │   │  任务规划    │ │
│  │  角色设定      │   │  相似度检索    │   │  结构化输出    │   │  多步执行     │ │
│  │  流式响应      │   │  上下文增强    │   │  实时数据      │   │  自主决策     │ │
│  └──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘ │
│                                                                            │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐   ┌──────────────┐ │
│  │  Embedding   │   │ VectorStore  │   │   多模态      │   │   提示词模板   │ │
│  │              │   │              │   │              │   │              │ │
│  │  文本向量化    │   │  向量存储      │   │  图像理解      │   │  模板渲染     │ │
│  │  相似度计算    │   │  相似度搜索    │   │  图像生成      │   │  参数注入     │ │
│  │  语义检索      │   │  持久化存储    │   │  语音处理      │   │  动态组合     │ │
│  └──────────────┘   └──────────────┘   └──────────────┘   └──────────────┘ │
│                                                                            │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.4 版本说明

| Spring AI 版本 | Spring Boot 版本 | JDK 版本 | 状态    |
|--------------|----------------|--------|-------|
| 0.8.x        | 3.2.x          | 17+    | 稳定版   |
| 1.0.0-Mx     | 3.3.x          | 17+    | 里程碑版本 |

---

## 二、快速开始

### 2.1 添加依赖

#### 2.1.1 父 POM 配置

```xml

<properties>
    <spring-ai.version>0.8.1</spring-ai.version>
</properties>

<dependencyManagement>
<dependencies>
    <!-- Spring AI BOM -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-bom</artifactId>
        <version>${spring-ai.version}</version>
        <type>pom</type>
        <scope>import</scope>
    </dependency>
</dependencies>
</dependencyManagement>

        <!-- Spring AI Milestone 仓库 -->
<repositories>
<repository>
    <id>spring-milestones</id>
    <name>Spring Milestones</name>
    <url>https://repo.spring.io/milestone</url>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>
</repositories>
```

#### 2.1.2 模块 POM 配置

```xml

<dependencies>
    <!-- Spring AI OpenAI -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>

    <!-- AspectJ Weaver (Spring AOP 依赖) -->
    <dependency>
        <groupId>org.aspectj</groupId>
        <artifactId>aspectjweaver</artifactId>
    </dependency>
</dependencies>
```

### 2.2 配置文件

#### 2.2.1 接入 OpenAI（标准配置）

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key}
      base-url: https://api.openai.com  # 可选，自定义 API 地址
      chat:
        options:
          model: gpt-4o              # 模型选择
          temperature: 0.7           # 创造性程度 (0-2)
          max-tokens: 2048           # 最大输出长度
      embedding:
        options:
          model: text-embedding-3-small  # Embedding 模型
```

#### 2.2.2 接入阿里云 DashScope（本项目实际配置）

DashScope 提供了 OpenAI 兼容接口，无需更换任何依赖，只需修改 `base-url` 和 `api-key`：

```yaml
server:
  port: 8092
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB

spring:
  application:
    name: interview-spring-ai

  datasource:
    url: jdbc:mysql://localhost:3306/interview?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: ${MYSQL_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD:}

  # Spring AI 配置 —— 通过 OpenAI Starter 对接 DashScope 兼容接口
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY:}        # 阿里云 DashScope API Key（环境变量）
      base-url: https://dashscope.aliyuncs.com/compatible-mode  # ⚠️ 不含 /v1，框架自动追加
      chat:
        options:
          model: qwen-plus                  # 通义千问标准版
          temperature: 0.7
      embedding:
        options:
          model: text-embedding-v3          # DashScope 文本向量模型
    vectorstore:
      simple:
        embedding-dimension: 1536
```

**关键注意事项**：

| 配置项        | 正确值                                              | 错误值（常见坑）                      | 说明                                                                      |
|------------|--------------------------------------------------|-------------------------------|-------------------------------------------------------------------------|
| `base-url` | `https://dashscope.aliyuncs.com/compatible-mode` | `...compatible-mode/v1`       | Spring AI 0.8.x 会自动追加 `/v1`，手动加会导致路径变 `/v1/v1/...` 报 404                |
| `model`    | `qwen-plus`                                      | `qwen3-vl-235b-a22b-thinking` | thinking 模型的流式输出在 `reasoning_content` 字段，Spring AI 0.8.x 无法获取，导致 SSE 静默 |
| `api-key`  | `${DASHSCOPE_API_KEY:}`                          | 直接写明文                         | 必须通过环境变量注入，避免泄露                                                         |

### 2.3 基础使用

```java

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI 智能体", description = "Spring AI 智能体相关接口")
public class AiAgentController {

    private final AiAgentService aiAgentService;
    private final RagService ragService;

    /** 单轮/多轮对话 */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return aiAgentService.chat(request);
    }

    /** 流式对话（SSE） */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> chatStream(@RequestBody ChatRequest request) {
        return aiAgentService.chatStream(request);
    }

    /** RAG 知识库问答 */
    @PostMapping("/rag/ask")
    public ChatResponse ragAsk(
            @RequestParam("question") String question,
            @RequestParam(name = "topK", required = false, defaultValue = "3") int topK) {
        String answer = ragService.answerQuestion(question, topK);
        return ChatResponse.builder().content(answer).success(true).build();
    }

    /** 加载文本到知识库（JSON Body，避免 HTTP 431） */
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
}
```

> **HTTP 431 陷阱**：若把大段文本放在 `@RequestParam` 中（URL query string），超大 Header 会触发 431。
> 必须改用 `@RequestBody` + JSON 方式传输文本内容。

---

## 三、核心功能详解

### 3.1 Chat 聊天

#### 3.1.1 多轮对话

```java

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;

    // 会话历史存储（生产环境应使用 Redis 等）
    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();

    /**
     * 多轮对话
     */
    public String chat(String sessionId, String userMessage) {
        // 获取或创建会话历史
        List<Message> history = conversationHistory.computeIfAbsent(
                sessionId, k -> new ArrayList<>()
        );

        // 添加用户消息
        history.add(new UserMessage(userMessage));

        // 构建请求
        Prompt prompt = new Prompt(history);
        ChatResponse response = chatClient.call(prompt);

        // 保存助手回复
        String assistantReply = response.getResult().getOutput().getContent();
        history.add(new AssistantMessage(assistantReply));

        return assistantReply;
    }

    /**
     * 清除会话历史
     */
    public void clearHistory(String sessionId) {
        conversationHistory.remove(sessionId);
    }
}
```

#### 3.1.2 流式响应

本项目的流式实现使用 `StreamingChatClient` + `Flux.create`，并加入了 **thinking 模型兼容处理**：

```java

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
                        // thinking 模型：content 可能为 null（推理阶段），正文在 reasoning_content
                        // Spring AI 0.8.x 无法读取 reasoning_content，因此只处理非空 content
                        String text = chunk.getResult().getOutput().getContent();
                        if (text != null && !text.isEmpty()) {
                            fullContent.append(text);
                            sink.next(ChatResponse.builder()
                                    .content(text).sessionId(sessionId).success(true).build());
                        }
                    },
                    error -> {
                        log.error("Stream error: {}", error.getMessage(), error);
                        // 中途报错时，若已有内容则返回已有内容
                        if (!fullContent.isEmpty()) {
                            sink.next(ChatResponse.builder().content(fullContent.toString())
                                    .sessionId(sessionId).success(true).build());
                        } else {
                            sink.next(ChatResponse.failure("流式聊天异常: " + error.getMessage()));
                        }
                        sink.complete();
                    },
                    () -> {
                        // 流结束但无任何内容 → thinking 模型场景，推送提示
                        if (fullContent.isEmpty()) {
                            sink.next(ChatResponse.builder()
                                    .content("[模型已完成思考，但未输出正文内容，" +
                                            "请尝试非 thinking 模式或检查模型配置]")
                                    .sessionId(sessionId).success(true).build());
                        }
                        sink.complete();
                    }
            );
        } catch (Exception e) {
            sink.next(ChatResponse.failure("流式聊天异常: " + e.getMessage()));
            sink.complete();
        }
    });
}
```

**Thinking 模型问题说明**：

| 模型                 | content 字段 | reasoning_content 字段 | Spring AI 0.8.x 是否可读 |
|--------------------|------------|----------------------|----------------------|
| `qwen-plus`（标准）    | 正文 token   | 无                    | ✅ 正常                 |
| `qwen3-*-thinking` | null（推理阶段） | 推理过程                 | ❌ 无法读取               |

**结论**：Spring AI 0.8.x 不支持 thinking 模型的 `reasoning_content`，流式时全程 `content=null`，SSE 静默无响应。生产环境应使用
`qwen-plus`、`qwen-max` 等非 thinking 模型。

### 3.2 RAG (检索增强生成)

#### 3.2.1 RAG 架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           RAG 架构流程                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │   用户问题   │────▶│  Embedding  │────▶ │  向量检索    │                  │
│   └─────────────┘     └─────────────┘     └──────┬──────┘                  │
│                                                  │                         │
│                                                  ▼                         │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐                  │
│   │   AI 回答   │◀────│   LLM 生成   │◀────│  相关文档    │                   │
│   └─────────────┘     └─────────────┘     └─────────────┘                  │
│                                                                            │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                        向量数据库                                     │  │
│   │   ┌───────────┐   ┌───────────┐   ┌───────────┐   ┌───────────┐     │  │
│   │   │ 文档片段1  │   │ 文档片段2   │   │ 文档片段3   │   │    ...    │     │  │
│   │   │ 向量存储   │   │ 向量存储    │   │ 向量存储    │   │           │     │  │
│   │   └───────────┘   └───────────┘   └───────────┘   └───────────┘     │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 3.2.2 VectorStore 配置

```java

@Configuration
public class SpringAiConfig {

    /**
     * 配置简单的内存向量存储
     */
    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        return new SimpleVectorStore(embeddingClient);
    }
}
```

#### 3.2.3 RAG 服务实现（生产级）

本项目的 [RagService](../../../interview-microservices-parent/interview-spring-ai/src/main/java/cn/itzixiao/interview/springai/service/RagService.java)
实现了**向量检索 → 关键词降级**的双重保障：

```java

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final KnowledgeDocumentRepository documentRepository;

    /**
     * 加载文本到知识库
     * 1. 按 \n\n 段落切分
     * 2. 存向量库（失败时降级，不影响 DB 持久化）
     * 3. 持久化元数据到 knowledge_document 表（按 title 去重）
     */
    public void loadText(String text, Map<String, Object> metadata) {
        String[] paragraphs = text.split("\n\n");
        List<Document> documents = new ArrayList<>();
        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                documents.add(new Document(paragraph.trim(), metadata));
            }
        }

        // 向量存储（DashScope 不可用时降级，不抛异常）
        try {
            vectorStore.add(documents);
        } catch (Exception e) {
            log.warn("向量存储失败，内容已保存至数据库: {}", e.getMessage());
        }

        // 持久化到 knowledge_document 表（title 精确匹配去重）
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
            if (sizeObj instanceof Long size) doc.setFileSize(size);
            else if (sizeObj instanceof Number n) doc.setFileSize(n.longValue());
            documentRepository.save(doc);
        }
    }

    /**
     * 相似度检索（失败时降级为关键词检索）
     */
    public List<Document> retrieveSimilarDocuments(String query, int topK) {
        try {
            return vectorStore.similaritySearch(SearchRequest.query(query).withTopK(topK));
        } catch (Exception e) {
            log.warn("向量检索失败，降级为关键词检索: {}", e.getMessage());
            return fallbackKeywordSearch(query, topK);
        }
    }

    /** 关键词降级检索（从 DB 按标题+内容匹配） */
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

    /**
     * 基于知识库回答问题（带 AI 不可用降级）
     */
    public String answerQuestion(String question, int topK) {
        List<Document> similarDocuments = retrieveSimilarDocuments(question, topK);
        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));

        String systemPrompt = """
                你是一个专业的知识库助手。请基于以下知识库内容回答用户问题。
                如果知识库中没有相关信息，请明确告知用户。
                
                知识库内容：
                %s
                """.formatted(context);

        try {
            return chatClient.call(new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(question)
            ))).getResult().getOutput().getContent();
        } catch (Exception e) {
            // AI 不可用时返回原始检索结果
            if (!similarDocuments.isEmpty()) {
                return "《知识库检索结果》（AI 当前不可用）：\n\n" +
                        similarDocuments.stream()
                                .map(d -> "- " + d.getContent())
                                .collect(Collectors.joining("\n"));
            }
            return "当前 AI 服务不可用，请检查 DASHSCOPE_API_KEY 环境变量是否正确配置。";
        }
    }

    private String getDocType(String filename) {
        if (filename == null) return "unknown";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "pdf";
        if (lower.endsWith(".txt")) return "text";
        if (lower.endsWith(".md")) return "markdown";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "word";
        return "text";
    }
}
```

### 3.3 Function Calling (函数调用)

#### 3.3.1 定义工具函数

```java
/**
 * 天气查询工具
 */
@Component
public class WeatherFunction implements Supplier<WeatherService.WeatherData> {

    private final WeatherService weatherService;

    @Override
    public WeatherService.WeatherData get() {
        // 获取当前天气数据
        return weatherService.getCurrentWeather("北京");
    }
}

/**
 * 数据库查询工具
 */
@Component
public class DatabaseQueryFunction
        implements BiFunction<String, String, String> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String apply(String query, String params) {
        // 执行数据库查询
        return jdbcTemplate.queryForObject(query, String.class);
    }
}
```

#### 3.3.2 注册函数

```java

@Configuration
public class FunctionConfig {

    @Bean
    @Description("获取当前天气信息")
    public Supplier<WeatherData> currentWeather(WeatherService weatherService) {
        return weatherService::getCurrentWeather;
    }

    @Bean
    @Description("查询用户信息，参数为用户ID")
    public Function<UserQueryRequest, UserInfo> queryUser(
            UserService userService) {
        return userService::getUserById;
    }
}
```

#### 3.3.3 使用函数调用

```java

@GetMapping("/function-call")
public String functionCall(@RequestParam String question) {
    // 配置函数调用
    OpenAiChatOptions options = OpenAiChatOptions.builder()
            .withModel("gpt-4o")
            .withFunction("currentWeather")
            .withFunction("queryUser")
            .build();

    Prompt prompt = new Prompt(question, options);
    ChatResponse response = chatClient.call(prompt);

    return response.getResult().getOutput().getContent();
}
```

### 3.4 Agent 任务编排

#### 3.4.1 Agent 架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Agent 架构                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                          用户请求                                     │  │
│   └───────────────────────────────┬─────────────────────────────────────┘   │
│                                   │                                         │
│                                   ▼                                         │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                       Agent 核心流程                                  │  │
│   │                                                                     │  │
│   │   ┌──────────┐     ┌──────────┐    ┌──────────┐     ┌──────────┐    │  │
│   │   │  任务分析  │───▶│  任务规划  │───▶│  工具调用  │───▶│  结果整合  │    │  │
│   │   └──────────┘     └──────────┘    └──────────┘     └──────────┘    │  │
│   │        │               │               │               │            │  │
│   │        ▼               ▼               ▼               ▼            │  │
│   │   ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐      │  │
│   │   │ 意图识别  │     │ 步骤分解  │    │ 工具选择  │     │ 格式化输出 │     │  │
│   │   │ 实体抽取  │     │ 依赖分析  │    │ 参数填充  │     │ 答案生成  │      │  │
│   │   └──────────┘    └──────────┘    └──────────┘    └──────────┘      │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                   │                                        │
│                                   ▼                                        │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                          工具注册表                                   │  │
│   │                                                                     │  │
│   │   ┌────────┐   ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐       │  │
│   │   │ 天气查询 │  │ 数据库  │  │ API调用 │  │ 文件处理 │  │ 计算器  │       │  │
│   │   └────────┘   └────────┘  └────────┘  └────────┘  └────────┘       │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 3.4.2 Agent 服务实现

```java

@Service
@RequiredArgsConstructor
public class AgentService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * 执行复杂任务
     */
    public AgentResponse executeTask(String userTask) {
        // 1. 任务分析
        TaskAnalysis analysis = analyzeTask(userTask);

        // 2. 任务规划
        List<TaskStep> steps = planTask(analysis);

        // 3. 执行步骤
        List<StepResult> results = new ArrayList<>();
        for (TaskStep step : steps) {
            StepResult result = executeStep(step);
            results.add(result);
        }

        // 4. 整合结果
        return integrateResults(userTask, results);
    }

    /**
     * 任务分析
     */
    private TaskAnalysis analyzeTask(String userTask) {
        String analysisPrompt = """
                分析以下用户任务，提取关键信息：
                
                任务：%s
                
                请输出 JSON 格式：
                {
                    "intent": "任务意图",
                    "entities": ["实体1", "实体2"],
                    "complexity": "simple/medium/complex"
                }
                """.formatted(userTask);

        String response = chatClient.call(analysisPrompt);
        return parseAnalysis(response);
    }

    /**
     * 任务规划
     */
    private List<TaskStep> planTask(TaskAnalysis analysis) {
        String planningPrompt = """
                根据任务分析结果，制定执行计划：
                
                %s
                
                请输出执行步骤列表（JSON 数组）：
                [
                    {"step": 1, "action": "动作描述", "tool": "工具名称", "params": {}},
                    ...
                ]
                """.formatted(analysis);

        String response = chatClient.call(planningPrompt);
        return parseSteps(response);
    }

    /**
     * 执行单步任务
     */
    private StepResult executeStep(TaskStep step) {
        // 根据工具类型调用对应函数
        return switch (step.getTool()) {
            case "weather" -> callWeatherTool(step);
            case "database" -> callDatabaseTool(step);
            case "api" -> callApiTool(step);
            default -> StepResult.error("未知工具: " + step.getTool());
        };
    }
}
```

---

## 四、提示词工程

### 4.1 提示词模板

```java

@Service
public class PromptTemplateService {

    private final ChatClient chatClient;

    /**
     * 使用提示词模板
     */
    public String generateCode(String language, String requirement) {
        String template = """
                你是一个专业的 {language} 开发工程师。
                
                请根据以下需求生成代码：
                {requirement}
                
                要求：
                1. 代码规范、注释完整
                2. 考虑异常处理
                3. 遵循最佳实践
                """;

        PromptTemplate promptTemplate = new PromptTemplate(template);
        promptTemplate.add("language", language);
        promptTemplate.add("requirement", requirement);

        Prompt prompt = promptTemplate.create();
        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
}
```

### 4.2 系统提示词最佳实践

```java
/**
 * 面试官角色提示词
 */
public static final String INTERVIEWER_SYSTEM_PROMPT = """
                你是一位经验丰富的 Java 技术面试官，具有以下特点：
                
                ## 角色定位
                - 专业、严谨、但不失亲和力
                - 注重考察候选人的实际编码能力和系统设计思维
                - 善于引导候选人深入思考
                
                ## 面试风格
                - 从基础问题开始，逐步深入
                - 根据候选人回答调整问题难度
                - 关注候选人的思维过程而非标准答案
                
                ## 评价标准
                - 技术深度：是否理解原理
                - 实践经验：是否有实战经验
                - 表达能力：是否清晰有条理
                """;
```

---

## 五、向量数据库选型

### 5.1 向量数据库对比

| 向量数据库             | 特点            | 适用场景     | Spring AI 支持 |
|-------------------|---------------|----------|--------------|
| SimpleVectorStore | 内存存储，开箱即用     | 开发测试     | ✅            |
| PGVector          | PostgreSQL 扩展 | 已有 PG 环境 | ✅            |
| Milvus            | 高性能分布式        | 大规模生产    | ✅            |
| Pinecone          | 云原生托管         | 无运维需求    | ✅            |
| Chroma            | 轻量级本地         | 小规模应用    | ✅            |
| Redis             | 已有 Redis 环境   | 复用基础设施   | ✅            |
| Elasticsearch     | 已有 ES 环境      | 搜索增强     | ✅            |

### 5.2 向量存储配置示例

```yaml
# PGVector 配置
spring:
  ai:
    vectorstore:
      pgvector:
        index-type: hnsw           # 索引类型
        distance-type: cosine_distance  # 距离算法
        dimensions: 1536           # 向量维度
        initialize-schema: true    # 自动建表

# Milvus 配置
spring:
  ai:
    vectorstore:
      milvus:
        host: localhost
        port: 19530
        database-name: default
        collection-name: documents
```

---

## 六、高频面试题

**问题 1：Spring AI 的核心原理是什么？**

**参考答案：**

Spring AI 提供了统一的抽象层，核心设计理念：

1. **统一接口**：
    - `ChatClient`：同步聊天对话接口
    - `StreamingChatClient`：流式聊天接口，返回 `Flux<ChatResponse>`
    - `EmbeddingClient`：文本向量化接口
    - `VectorStore`：向量存储接口

2. **Provider 模式**：
    - 通过 SPI 机制支持多种 AI 提供商
    - 自动配置类：`OpenAiAutoConfiguration`、`OllamaAutoConfiguration` 等
    - **DashScope 兼容**：阿里云提供 OpenAI 兼容接口，使用 `spring-ai-openai-spring-boot-starter` 即可直接对接，只需修改
      `base-url` 和 `api-key`

3. **Prompt 抽象**：
    - `Message`：消息抽象（System/User/Assistant）
    - `Prompt`：提示词封装
    - `PromptTemplate`：模板化提示词

---

**问题 2：什么是 RAG？Spring AI 如何实现 RAG？**

**参考答案：**

**RAG (Retrieval-Augmented Generation)** 是检索增强生成技术：

1. **核心流程**：
    - 离线阶段：文档 → 切分 → Embedding → 向量存储
    - 在线阶段：问题 → Embedding → 向量检索 → 上下文增强 → LLM 生成

2. **Spring AI 实现**：
   ```java
   // 1. 文档向量化存储
   vectorStore.add(documents);
   
   // 2. 相似度检索
   List<Document> docs = vectorStore.similaritySearch(
       SearchRequest.query(question).withTopK(3)
   );
   
   // 3. 构建 RAG 提示词
   String context = docs.stream()
       .map(Document::getContent)
       .collect(Collectors.joining("\n"));
   ```

3. **优势**：
    - 无需微调模型
    - 知识可实时更新
    - 可追溯引用来源

---

**问题 3：Function Calling 是什么？有什么应用场景？**

**参考答案：**

**Function Calling** 允许 LLM 调用外部函数获取实时数据：

1. **工作原理**：
    - LLM 判断是否需要调用函数
    - 返回函数名和参数
    - 应用执行函数获取结果
    - 将结果返回给 LLM 继续推理

2. **应用场景**：
    - 实时数据查询（天气、股价）
    - 数据库操作
    - 外部 API 调用
    - 业务逻辑执行

3. **Spring AI 实现**：
   ```java
   @Bean
   @Description("查询天气信息")
   public Supplier<WeatherData> weatherFunction() {
       return () -> weatherService.getWeather();
   }
   ```

---

---

**问题 9：Spring AI 接入 DashScope 报 404 路径重复 `/v1/v1/` 如何解决？**

**参考答案：**

Spring AI 0.8.x 的 OpenAI 客户端会在 `base-url` 后**自动追加 `/v1`**，若配置时已包含 `/v1` 则路径重复：

```
# 错误配置（会生成 /v1/v1/chat/completions）
base-url: https://dashscope.aliyuncs.com/compatible-mode/v1

# 正确配置
base-url: https://dashscope.aliyuncs.com/compatible-mode
```

---

**问题 10：为什么使用 thinking 模型（如 qwen3-vl-235b-a22b-thinking）流式聊天没有任何响应？**

**参考答案：**

Thinking 模型在推理阶段将内容输出到 `reasoning_content` 字段，而非 `content` 字段。Spring AI 0.8.x 的
`ChatResponse.getResult().getOutput().getContent()` 只读 `content`，导致：

1. 推理阶段：`content = null` → 被过滤 → SSE 无事件
2. 正文阶段：部分 thinking 模型的正文也在 `reasoning_content` → 整个流静默

**解决方案**：

- 换用非 thinking 模型（`qwen-plus`、`qwen-max` 等）
- 升级 Spring AI 到支持 `reasoning_content` 的版本（1.0.x+）
- 在 `onComplete` 回调中增加兜底提示，避免客户端无限等待

---

**问题 11：知识库管理如何避免统计接口触发多次全表扫描？**

**参考答案：**

错误做法（3 次 SQL，2 次全表）：

```java
long documentCount = documentRepository.count();       // SQL 1
long totalChunks = documentRepository.findAll()      // SQL 2（全表）
        .stream().mapToLong(KnowledgeDocument::getChunkCount).sum();
long totalSize = documentRepository.findAll()      // SQL 3（全表）
        .stream().mapToLong(KnowledgeDocument::getCharCount).sum();
```

正确做法（2 次 SQL，0 次全表）：

```java
// Repository 中添加聚合查询
@Query("SELECT COALESCE(SUM(d.chunkCount), 0), COALESCE(SUM(d.charCount), 0) FROM KnowledgeDocument d")
List<Object[]> sumChunksAndChars();

// Controller 中使用
long documentCount = documentRepository.count();
List<Object[]> sums = documentRepository.sumChunksAndChars();
        long totalChunks = 0, totalSize = 0;
if(!sums.

isEmpty() &&sums.

get(0) !=null){
Object[] row = sums.get(0);
totalChunks =row[0]instanceof
Number n ?n.

longValue() :0L;
totalSize   =row[1]instanceof
Number n ?n.

longValue() :0L;
        }
```

---

**问题 4：如何解决 Spring AI 多 EmbeddingClient 冲突问题？**

**参考答案：**

当引入多个 Embedding 自动配置时，会出现 Bean 冲突：

**解决方案**：

1. **移除冲突依赖**：
   ```xml
   <!-- 只保留一个 Embedding 依赖 -->
   <dependency>
       <groupId>org.springframework.ai</groupId>
       <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
   </dependency>
   <!-- 移除 transformers 等其他 Embedding 依赖 -->
   ```

2. **使用 @Primary 注解**：
   ```java
   @Bean
   @Primary
   public EmbeddingClient primaryEmbeddingClient() {
       return new OpenAiEmbeddingClient();
   }
   ```

3. **使用 @Qualifier 注入**：
   ```java
   public RagService(@Qualifier("openAiEmbeddingClient") 
                      EmbeddingClient embeddingClient) {
       this.embeddingClient = embeddingClient;
   }
   ```

---

**问题 5：Spring AI 项目缺少 VectorStore Bean 如何解决？**

**参考答案：**

需要手动配置 VectorStore：

```java

@Configuration
public class SpringAiConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingClient embeddingClient) {
        // 简单内存向量存储
        return new SimpleVectorStore(embeddingClient);

        // 或使用 PGVector
        // return new PgVectorStore(jdbcTemplate, embeddingClient);
    }
}
```

**注意**：`SimpleVectorStore` 已包含在 `spring-ai-core` 中，无需额外依赖。

---

**问题 6：Spring AI 项目启动报 NoClassDefFoundError: Pointcut 如何解决？**

**参考答案：**

Spring AI 依赖 Spring AOP，需要添加 AspectJ 依赖：

```xml

<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
</dependency>
```

**原因**：Spring Boot 3.x + Spring AI 需要完整的 AOP 运行时支持。

---

**问题 7：如何选择合适的向量数据库？**

**参考答案：**

| 场景             | 推荐方案              | 理由            |
|----------------|-------------------|---------------|
| 开发测试           | SimpleVectorStore | 零配置，开箱即用      |
| 小规模生产（<100万向量） | PGVector          | 复用现有 PG 基础设施  |
| 中大规模生产         | Milvus            | 高性能分布式，支持水平扩展 |
| 云原生偏好          | Pinecone          | 全托管，无需运维      |
| 已有 Redis 集群    | Redis Vector      | 复用基础设施，降低运维成本 |

---

**问题 8：如何优化 RAG 系统的检索质量？**

**参考答案：**

1. **文档预处理**：
    - 合理切分（chunk size 500-1000）
    - 保持语义完整性
    - 添加元数据（标题、来源、时间）

2. **检索优化**：
    - 使用 Hybrid Search（向量 + 关键词）
    - 重排序（Reranking）
    - 多路召回

3. **提示词优化**：
    - 明确指令约束
    - 添加来源引用要求
    - 设置回答边界

---

**问题 12：AI Agent 的类型有哪些？和传统自动化有什么区别？**

**参考答案：**

**Agent 类型对比**：

| 类型                  | 特点               | 典型应用        |
|---------------------|------------------|-------------|
| **ReAct**           | 推理 + 行动迭代，边思考边执行 | 外部 API 工具调用 |
| **Plan-and-Solve**  | 先规划整体步骤，再逐步执行    | 复杂多步任务      |
| **Multi-Agent**     | 多 Agent 协作，角色分工  | 跨系统业务协同     |
| **Self-Reflection** | 自我反思与修正          | 高精度要求场景     |

**与传统自动化对比**：

| 特性   | 传统自动化             | AI Agent   |
|------|-------------------|------------|
| 决策能力 | 基于规则              | 基于 LLM 推理  |
| 适应性  | 固定流程              | 动态决策       |
| 工具使用 | 预设调用              | 自主选择和调用    |
| 异常处理 | 需上阶只车（hard-coded） | 可自主重试并调整策略 |

**Spring AI Agent 实现示例**：

```java

@Service
public class OrderAgent {

    private final ChatClient chatClient;

    // 定义工具函数（Function Calling）
    @Bean
    @Description("查询订单状态")
    public Function<String, String> queryOrderStatus() {
        return orderId -> orderService.getStatus(orderId);
    }

    @Bean
    @Description("取消订单")
    public Function<String, String> cancelOrder() {
        return orderId -> orderService.cancel(orderId);
    }

    public String handle(String userInput) {
        // Agent 根据用户输入自主决定调用哪个工具
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withFunction("queryOrderStatus")
                .withFunction("cancelOrder")
                .build();
        return chatClient.call(new Prompt(userInput, options))
                .getResult().getOutput().getContent();
    }
}
```

---

**问题 13：LLM 应用中有哪些常见安全风险？如何防护？**

**参考答案：**

| 风险                    | 描述               | 防护方案                        |
|-----------------------|------------------|-----------------------------|
| **Prompt 注入**         | 恶意指令覆盖系统提示词      | 输入过滤、输出校验、隔离 System/User 消息 |
| **数据泄露**              | 敏感信息通过 Prompt 泄露 | 数据脱敏、RAG 权限控制、最小权限原则        |
| **幻觉（Hallucination）** | 生成虚假内容           | RAG 增强、事实校验、设置低 temperature |
| **内容滥用**              | 生成有害内容           | 内容审核、关键词过滤、输出审核层            |
| **费用失控**              | Token 卖出导致费用暴跌   | API Key 限流、请求速率限制、密钥环境变量化   |

**实践建议**：

- API Key 必须通过环境变量注入，绝不写入代码或配置文件
- 对所有用户输入进行长度限制和内容过滤
- 启用 Token 消耗监控和预算告警
- 放宽与收紧策略分离：内部系统 vs 对外开放 API

---

**问题 14：如何评估 RAG 系统的效果？**

**参考答案：**

**检索阶段指标**：

- **召回率（Recall@K）**：相关文档是否被检索到
- **精确率（Precision@K）**：检索结果中相关文档的比例
- **MRR（Mean Reciprocal Rank）**：第一个相关结果的平均倒数排名

**生成阶段指标**：

- **回答准确性**：与事实的一致性
- **回答相关性**：与问题的匹配度
- **忠实性（Faithfulness）**：回答是否基于检索内容（键防幻觉）

**评估方法**：

- 主观评估：人工标注回答质量
- 自动评估：使用 LLM 对回答打分（LLM-as-Judge）
- A/B 测试：对比不同检索策略的效果
- 工具：**RAGAS**（开源 RAG 评估框架）

---

## 七、项目示例代码

本项目提供了完整的 Spring AI 示例代码：

### 7.1 模块结构

| 文件路径                                          | 说明                         |
|-----------------------------------------------|----------------------------|
| `interview-spring-ai/`                        | Spring AI 模块根目录            |
| `controller/AiAgentController.java`           | AI 聊天 + RAG 接口（端口 8092）    |
| `controller/KnowledgeBaseController.java`     | 知识库管理接口（上传、检索、统计）          |
| `service/impl/AiAgentServiceImpl.java`        | 聊天 + 流式实现（含 thinking 模型兼容） |
| `service/RagService.java`                     | RAG 服务（向量检索 + 关键词降级）       |
| `entity/KnowledgeDocument.java`               | 知识库文档实体（JPA）               |
| `repository/KnowledgeDocumentRepository.java` | 文档仓库（含聚合查询）                |
| `config/SpringAiConfig.java`                  | VectorStore Bean 配置        |
| `function/WeatherFunction.java`               | 天气查询函数示例                   |
| `resources/application.yml`                   | DashScope 接入配置             |

### 7.2 关联前端

| 前端目录                      | 技术栈                         | 端口   |
|---------------------------|-----------------------------|------|
| `interview-ui/ai/`        | Vue 3 + Element Plus + Vite | 3000 |
| `src/views/Chat.vue`      | SSE 流式对话页面                  | -    |
| `src/views/Knowledge.vue` | 知识库管理页面                     | -    |
| `src/views/Search.vue`    | 知识库检索页面                     | -    |
| `src/api/ai.js`           | Axios 封装的 API 调用            | -    |

### 7.3 启动方式

1. 配置环境变量（Windows PowerShell）：
   ```powershell
   $env:DASHSCOPE_API_KEY = "your-dashscope-api-key"
   $env:MYSQL_PASSWORD    = "your-mysql-password"
   ```

2. 启动后端（端口 8092）：
   ```bash
   mvn spring-boot:run -pl interview-microservices-parent/interview-spring-ai
   ```

3. 启动前端（端口 3000，自动代理到 8092）：
   ```bash
   cd interview-ui/ai && npm run dev
   ```

4. 访问 API 文档：
   ```
   http://localhost:8092/doc.html
   ```

---

## 八、总结

Spring AI 为 Java 开发者提供了便捷的 AI 集成能力：

| 能力    | 核心类/接口            | 应用场景       |
|-------|-------------------|------------|
| 对话聊天  | ChatClient        | 智能客服、问答系统  |
| 文本向量化 | EmbeddingClient   | 语义搜索、相似度计算 |
| 向量存储  | VectorStore       | 知识库、RAG 系统 |
| 函数调用  | Function Callback | 工具调用、实时数据  |
| 提示词模板 | PromptTemplate    | 提示词管理、复用   |

**最佳实践**：

1. 合理选择 AI 模型（GPT-4 vs GPT-3.5 vs 本地模型）
2. 设计良好的提示词模板
3. 使用 RAG 增强 LLM 知识能力
4. 通过 Function Calling 扩展 LLM 能力边界
5. 监控 AI 调用成本和响应时间

---

---

## 九、扩展文档

本模块已拆分为三个文档，按需查阅：

| 文档                                                     | 内容                                 |
|--------------------------------------------------------|------------------------------------|
| [05-Spring-AI智能体详解.md](./05-Spring-AI智能体详解.md)         | 框架基础、配置、Chat、RAG、Agent、面试题         |
| [06-Spring-AI知识库与RAG实战.md](./06-Spring-AI知识库与RAG实战.md) | 知识库实体设计、文件上传解析、聚合统计、向量降级检索         |
| [07-Spring-AI前端集成实战.md](./07-Spring-AI前端集成实战.md)       | Vue3 前端架构、SSE 流式对话、知识库管理、Vite 代理配置 |

---

**维护者：** itzixiao  
**最后更新：** 2026-03-24  
**问题反馈：** 欢迎提 Issue 或 PR
