# AI工程化与LLM应用开发

## 概述

AI工程化是将大语言模型（LLM）集成到企业应用中的实践，涉及RAG架构、AI Agent、模型部署等核心技术。

---

## 一、LLM基础概念

### 1.1 大语言模型对比

| 模型         | 厂商        | 特点         | 适用场景      |
|------------|-----------|------------|-----------|
| **GPT-4**  | OpenAI    | 能力强，价格高    | 复杂推理、代码生成 |
| **Claude** | Anthropic | 上下文长（200K） | 长文档分析     |
| **文心一言**   | 百度        | 中文优化       | 国内应用      |
| **通义千问**   | 阿里        | 开源可商用      | 企业私有化部署   |
| **Llama**  | Meta      | 开源免费       | 私有化部署     |

### 1.2 模型选择策略

```
┌─────────────────────────────────────────┐
│           LLM选择决策树                  │
├─────────────────────────────────────────┤
│                                          │
│  是否需要私有化部署？                      │
│       ├── 是 → 选择开源模型（Llama/ChatGLM）│
│       └── 否 → 预算充足？                 │
│              ├── 是 → GPT-4/Claude       │
│              └── 否 → 国产模型/API       │
│                                          │
└─────────────────────────────────────────┘
```

---

## 二、RAG架构（检索增强生成）

### 2.1 RAG流程

```
用户提问
   ↓
┌─────────────┐
│  查询理解    │ ← 意图识别、Query改写
└──────┬──────┘
       ↓
┌─────────────┐
│  向量检索    │ ← 从知识库检索相关文档
└──────┬──────┘
       ↓
┌─────────────┐
│  上下文组装  │ ← 将检索结果组装成Prompt
└──────┬──────┘
       ↓
┌─────────────┐
│  LLM生成    │ ← 大模型生成回答
└──────┬──────┘
       ↓
   返回结果
```

### 2.2 向量数据库选型

| 数据库          | 特点           | 适用场景    |
|--------------|--------------|---------|
| **Milvus**   | 开源、分布式       | 大规模向量检索 |
| **Pinecone** | 托管服务         | 快速上手    |
| **Qdrant**   | Rust实现、高性能   | 性能敏感场景  |
| **PGVector** | PostgreSQL插件 | 已有PG环境  |

### 2.3 Java实现RAG

```java

@Service
public class RAGService {

    @Autowired
    private EmbeddingClient embeddingClient;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private ChatClient chatClient;

    public String ask(String question) {
        // 1. 向量化查询
        float[] queryVector = embeddingClient.embed(question);

        // 2. 检索相关文档
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.query(question).withTopK(5)
        );

        // 3. 组装Prompt
        String context = relevantDocs.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n"));

        String prompt = """
                基于以下上下文回答问题：
                %s
                
                问题：%s
                """.formatted(context, question);

        // 4. 调用LLM生成回答
        return chatClient.call(prompt);
    }
}
```

---

## 三、AI Agent

### 3.1 Agent架构

```
┌──────────────────────────────────────────┐
│              AI Agent架构                 │
├──────────────────────────────────────────┤
│                                          │
│  ┌──────────┐    ┌─────────┐    ┌──────┐ │
│  │  感知     │───→│  决策   │───→│  执行 │ │
│  │(Perceive)│    │ (Decide)│    │(Act) │ │
│  └────┬─────┘    └────┬────┘    └──┬───┘ │
│       │              │             │     │
│       └──────────────┴─────────────┘     │
│                   ↑                      │
│              ┌────┴────┐                 │
│              │  记忆    │                 │
│              │(Memory) │                 │
│              └─────────┘                 │
│                                          │
└──────────────────────────────────────────┘
```

### 3.2 Agent类型

| 类型                 | 特点       | 示例     |
|--------------------|----------|--------|
| **ReAct**          | 推理+行动结合  | 先思考后行动 |
| **Plan-and-Solve** | 先规划再执行   | 复杂任务分解 |
| **Multi-Agent**    | 多Agent协作 | 角色分工   |

### 3.3 Spring AI实现Agent

```java

@Service
public class OrderAgent {

    private final ChatClient chatClient;

    @Tool("查询订单状态")
    public String queryOrderStatus(String orderId) {
        // 调用订单服务
        return orderService.getStatus(orderId);
    }

    @Tool("取消订单")
    public String cancelOrder(String orderId) {
        // 调用取消逻辑
        return orderService.cancel(orderId);
    }

    public String handle(String userInput) {
        // Agent根据用户输入决定调用哪个工具
        return chatClient.call(userInput);
    }
}
```

---

## 四、模型部署与优化

### 4.1 部署方式对比

| 方式        | 优点      | 缺点       |
|-----------|---------|----------|
| **API调用** | 简单、无需运维 | 成本高、依赖网络 |
| **私有化部署** | 数据安全、可控 | 硬件要求高    |
| **边缘部署**  | 低延迟     | 模型大小受限   |

### 4.2 模型量化

```python
# 使用GPTQ量化模型（减少显存占用）
from transformers import AutoModelForCausalLM, GPTQConfig

quantization_config = GPTQConfig(
    bits=4,  # 4bit量化
    group_size=128,
    desc_act=False,
)

model = AutoModelForCausalLM.from_pretrained(
    "model_name",
    quantization_config=quantization_config,
)
```

### 4.3 推理优化

| 技术           | 效果       | 适用场景  |
|--------------|----------|-------|
| **vLLM**     | 吞吐提升10x+ | 高并发服务 |
| **TensorRT** | 延迟降低50%  | 实时推理  |
| **批处理**      | 提高GPU利用率 | 非实时场景 |

---

## 五、高频面试题

**问题 1：什么是RAG？为什么需要RAG？**

**答：**

**RAG（Retrieval-Augmented Generation）**：检索增强生成，将外部知识检索与LLM生成结合。

**为什么需要：**

1. **解决知识时效性**：LLM训练数据有截止日期
2. **减少幻觉**：基于检索的事实生成更可靠
3. **企业知识集成**：接入私有知识库
4. **成本优化**：减少Prompt长度

---

**问题 2：AI Agent和传统自动化有什么区别？**

**答：**

| 特性       | 传统自动化 | AI Agent |
|----------|-------|----------|
| **决策能力** | 基于规则  | 基于LLM推理  |
| **适应性**  | 固定流程  | 动态决策     |
| **工具使用** | 预设调用  | 自主选择和调用  |
| **学习能力** | 需人工更新 | 可从交互中学习  |

---

**问题 3：如何评估RAG系统的效果？**

**答：**

**检索阶段指标：**

- 召回率（Recall）：相关文档是否被检索到
- 精确率（Precision）：检索结果的相关性

**生成阶段指标：**

- 回答准确性：与事实的一致性
- 回答相关性：与问题的匹配度

**端到端评估：**

- 人工评估：标注回答质量
- A/B测试：对比不同方案

---

**问题 4：LLM应用中的安全问题有哪些？**

**答：**

| 风险           | 描述             | 防护         |
|--------------|----------------|------------|
| **Prompt注入** | 恶意指令覆盖系统提示     | 输入过滤、输出校验  |
| **数据泄露**     | 敏感信息通过Prompt泄露 | 数据脱敏、权限控制  |
| **幻觉**       | 生成虚假内容         | RAG增强、事实校验 |
| **滥用**       | 生成有害内容         | 内容审核、关键词过滤 |

---

## 六、相关资源

### 6.1 开源框架

| 框架             | 语言        | 特点         |
|----------------|-----------|------------|
| **LangChain**  | Python/JS | 生态最完善      |
| **Spring AI**  | Java      | Spring生态集成 |
| **LlamaIndex** | Python    | 专注RAG      |

### 6.2 学习资源

- [Spring AI官方文档](https://spring.io/projects/spring-ai)
- [LangChain官方文档](https://python.langchain.com/)
- [Hugging Face模型库](https://huggingface.co/models)
