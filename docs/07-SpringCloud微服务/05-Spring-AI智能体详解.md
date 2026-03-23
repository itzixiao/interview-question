# Spring AI 智能体详解

## 一、概述

### 1.1 什么是 Spring AI

Spring AI 是 Spring 官方推出的 AI 集成框架，旨在简化 AI 能力在 Spring 应用中的集成。它提供了统一的抽象层，支持多种 AI
模型提供商：

| AI 提供商           | 模型类型           | 特点            |
|------------------|----------------|---------------|
| OpenAI           | Chat/Embedding | GPT 系列，功能最全面  |
| Azure OpenAI     | Chat/Embedding | 企业级，数据合规      |
| Ollama           | Chat/Embedding | 本地部署，隐私保护     |
| Anthropic Claude | Chat           | 长上下文，安全对齐     |
| Google Gemini    | Chat/Embedding | 多模态，Google 生态 |
| 阿里通义千问           | Chat/Embedding | 国产模型，中文优化     |
| 百度文心一言           | Chat           | 国产模型，企业应用     |

### 1.2 核心功能

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

### 1.3 版本说明

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

### 2.3 基础使用

```java

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ChatController {

    private final ChatClient chatClient;

    /**
     * 简单对话
     */
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.call(message);
    }

    /**
     * 带系统提示词的对话
     */
    @GetMapping("/chat-with-system")
    public String chatWithSystem(@RequestParam String message) {
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage("你是一个专业的 Java 面试官，请用简洁专业的方式回答问题。"),
                        new UserMessage(message)
                )
        );
        ChatResponse response = chatClient.call(prompt);
        return response.getResult().getOutput().getContent();
    }
}
```

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

```java

@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestParam String message) {
    Prompt prompt = new Prompt(message);
    return chatClient.stream(prompt)
            .map(response -> response.getResult().getOutput().getContent());
}
```

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

#### 3.2.3 RAG 服务实现

```java

@Service
@RequiredArgsConstructor
public class RagService {

    private final ChatClient chatClient;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    /**
     * 加载文档到知识库
     */
    public void loadDocument(String text, Map<String, Object> metadata) {
        // 按段落切分
        String[] paragraphs = text.split("\\n\\n");
        List<Document> documents = new ArrayList<>();

        for (String paragraph : paragraphs) {
            if (!paragraph.trim().isEmpty()) {
                documents.add(new Document(paragraph.trim(), metadata));
            }
        }

        // 存储到向量数据库
        vectorStore.add(documents);
    }

    /**
     * 基于知识库回答问题
     */
    public String answerQuestion(String question, int topK) {
        // 检索相似文档
        List<Document> similarDocs = vectorStore.similaritySearch(
                SearchRequest.query(question).withTopK(topK)
        );

        // 构建上下文
        String context = similarDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\\n\\n---\\n\\n"));

        // 构建 RAG 提示词
        String systemPrompt = """
                你是一个专业的知识库助手。请基于以下知识库内容回答用户问题。
                如果知识库中没有相关信息，请明确告知用户。
                
                知识库内容：
                %s
                """.formatted(context);

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(question)
                )
        );

        return chatClient.call(prompt).getResult().getOutput().getContent();
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
    - `ChatClient`：聊天对话接口
    - `EmbeddingClient`：向量化接口
    - `VectorStore`：向量存储接口

2. **Provider 模式**：
    - 通过 SPI 机制支持多种 AI 提供商
    - 自动配置类：`OpenAiAutoConfiguration`、`OllamaAutoConfiguration` 等

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

## 七、项目示例代码

本项目提供了完整的 Spring AI 示例代码：

### 7.1 模块结构

| 文件路径                                | 说明               |
|-------------------------------------|------------------|
| `interview-spring-ai/`              | Spring AI 模块根目录  |
| `controller/AiAgentController.java` | AI Agent REST 接口 |
| `service/ChatService.java`          | 对话聊天服务           |
| `service/RagService.java`           | RAG 知识库服务        |
| `service/AgentService.java`         | Agent 任务编排服务     |
| `function/WeatherFunction.java`     | 天气查询函数示例         |
| `config/SpringAiConfig.java`        | Spring AI 配置类    |

### 7.2 启动方式

1. 配置环境变量：
   ```bash
   export OPENAI_API_KEY=your-api-key
   ```

2. 启动应用：
   ```bash
   mvn spring-boot:run -pl interview-microservices-parent/interview-spring-ai
   ```

3. 访问 API 文档：
   ```
   http://localhost:8083/doc.html
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

**维护者：** itzixiao  
**最后更新：** 2026-03-23  
**问题反馈：** 欢迎提 Issue 或 PR
