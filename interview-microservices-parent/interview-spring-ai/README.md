# Spring AI 智能体模块

基于 Spring AI 框架实现的 AI Agent 示例模块，展示现代 AI 应用开发的核心能力。

## 功能特性

### 1. 对话聊天
- 单轮/多轮对话
- 流式响应
- 会话管理

### 2. RAG (Retrieval-Augmented Generation)
- PDF 文档加载
- 文本向量化存储
- 相似度检索
- 上下文增强生成

### 3. Function Calling
- 天气查询
- 数学计算
- 可扩展的工具调用机制

### 4. Agent 任务编排
- 复杂任务分解
- 多步骤执行
- 执行过程追踪

## 技术栈

- **JDK**: 17+
- **Spring Boot**: 3.2.5
- **Spring AI**: 1.0.0-M4
- **OpenAI**: GPT-4 / GPT-3.5

## 快速开始

### 1. 配置 API Key

在 `application.yml` 中配置 OpenAI API Key：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:your-api-key-here}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
```

或通过环境变量设置：

```bash
export OPENAI_API_KEY=your-api-key-here
```

### 2. 启动服务

```bash
mvn spring-boot:run
```

服务启动后访问：
- API 文档：http://localhost:8090/swagger-ui.html
- 健康检查：http://localhost:8090/api/ai/health

## API 接口

### 对话聊天

```bash
curl -X POST http://localhost:8090/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下 Spring AI",
    "sessionId": "session-001"
  }'
```

### 流式对话

```bash
curl -X POST http://localhost:8090/api/ai/chat/stream \
  -H "Content-Type: application/json" \
  -d '{
    "message": "写一个快速排序算法",
    "sessionId": "session-002"
  }'
```

### Agent 任务

```bash
curl -X POST http://localhost:8090/api/ai/agent/task \
  -H "Content-Type: application/json" \
  -d '{
    "task": "查询北京今天的天气并计算 25 乘以 4",
    "taskType": "FUNCTION_CALL",
    "availableTools": ["getWeather", "calculate"]
  }'
```

### RAG 知识库问答

```bash
# 先加载知识
curl -X POST "http://localhost:8090/api/ai/rag/load-text" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "text=Spring AI 是 Spring 框架的 AI 集成项目" \
  -d "title=Spring AI 介绍"

# 然后提问
curl -X POST "http://localhost:8090/api/ai/rag/ask?question=什么是 Spring AI&topK=3"
```

## 项目结构

```
interview-spring-ai/
├── src/main/java/cn/itzixiao/interview/springai/
│   ├── SpringAiApplication.java      # 启动类
│   ├── controller/
│   │   └── AiAgentController.java    # REST API
│   ├── service/
│   │   ├── AiAgentService.java       # 智能体服务接口
│   │   ├── RagService.java           # RAG 服务
│   │   └── impl/
│   │       └── AiAgentServiceImpl.java
│   ├── function/
│   │   ├── WeatherFunction.java      # 天气查询函数
│   │   └── CalculatorFunction.java   # 计算器函数
│   └── dto/
│       ├── ChatRequest.java          # 聊天请求
│       ├── ChatResponse.java         # 聊天响应
│       ├── AgentTaskRequest.java     # 任务请求
│       └── AgentTaskResponse.java    # 任务响应
├── src/main/resources/
│   └── application.yml               # 配置文件
└── pom.xml                           # Maven 配置
```

## 扩展开发

### 添加自定义 Function

```java
@Configuration
public class MyFunction {

    public record Request(String param) {}
    public record Response(String result) {}

    @Bean
    @Description("功能描述")
    public Function<Request, Response> myFunction() {
        return request -> new Response("处理结果");
    }
}
```

### 使用向量数据库

Spring AI 支持多种向量数据库：
- Redis
- PostgreSQL (pgvector)
- Chroma
- Pinecone
- Weaviate

在 `pom.xml` 中添加对应依赖即可切换。

## 注意事项

1. **API Key 安全**：生产环境请使用环境变量或配置中心管理 API Key
2. **流式响应**：流式接口使用 SSE (Server-Sent Events) 协议
3. **Token 限制**：注意控制输入输出 token 数量，避免超出模型限制
