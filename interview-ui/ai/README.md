# AI 知识库前端

基于 Vue 3 + Vite 构建的 AI 智能对话与知识库管理系统，配套后端模块 `interview-spring-ai`（端口 8092）。

---

## 目录

- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [页面功能](#页面功能)
- [接口说明](#接口说明)
- [状态管理](#状态管理)
- [代理配置](#代理配置)
- [常见问题](#常见问题)

---

## 技术栈

| 技术 | 版本 | 说明 |
|---|---|---|
| Vue 3 | ^3.4.21 | 核心框架，Composition API |
| Vite | ^5.2.8 | 构建工具，开发服务器 |
| Vue Router | ^4.3.0 | 客户端路由 |
| Pinia | ^2.1.7 | 状态管理 |
| Axios | ^1.6.8 | HTTP 请求封装 |
| Element Plus | ^2.6.3 | UI 组件库 |
| marked | ^12.0.1 | Markdown 渲染（AI 回复） |
| highlight.js | ^11.9.0 | 代码高亮 |
| Sass | ^1.74.1 | CSS 预处理器 |

---

## 项目结构

```
interview-ui/ai/
├── src/
│   ├── api/
│   │   └── ai.js            # 所有后端接口封装
│   ├── components/
│   │   ├── ChatInput.vue    # 聊天输入框组件
│   │   └── ChatMessage.vue  # 消息气泡组件（支持 Markdown）
│   ├── router/
│   │   └── index.js         # 路由配置（4 个页面）
│   ├── stores/
│   │   └── chat.js          # Pinia 聊天状态管理
│   ├── utils/
│   │   └── request.js       # Axios 实例（超时 60s，统一错误处理）
│   ├── views/
│   │   ├── Chat.vue         # AI 流式对话页面
│   │   ├── Knowledge.vue    # 知识库管理页面
│   │   ├── Search.vue       # 知识检索页面
│   │   ├── Debug.vue        # 接口调试页面
│   │   └── Layout.vue       # 整体布局（侧边栏 + 路由出口）
│   ├── App.vue              # 根组件
│   └── main.js              # 应用入口
├── vite.config.js           # Vite 配置（代理 /api → :8092）
├── package.json
└── index.html
```

---

## 快速启动

### 前置条件

- Node.js >= 18
- 后端服务 `interview-spring-ai` 已启动（端口 8092）
- 已配置 `DASHSCOPE_API_KEY` 环境变量

### 启动步骤

```bash
# 1. 进入前端目录
cd interview-ui/ai

# 2. 安装依赖（首次）
npm install

# 3. 启动开发服务器（端口 3000）
npm run dev
```

访问地址：`http://localhost:3000`

### 构建生产包

```bash
npm run build    # 产物输出到 dist/
npm run preview  # 本地预览构建结果
```

---

## 页面功能

### `/chat` — AI 对话

- **SSE 流式响应**：使用原生 `fetch` + `ReadableStream` 逐字输出 AI 回复
- **多轮会话**：左侧会话列表，支持新建、切换会话
- **Markdown 渲染**：AI 回复支持代码块、表格、列表等富文本格式
- **系统提示词**：可自定义对话角色设定

核心流程：

```
用户输入 → chatStream() → SSE 逐块接收 → updateLastMessage() → Markdown 渲染
```

### `/knowledge` — 知识库管理

- **文档上传**：支持 PDF、Word、TXT、Markdown 等格式（Apache Tika 解析）
- **文本直接录入**：粘贴文本内容即可入库
- **文档列表**：展示已入库文档（标题、分块数、字符数、入库时间）
- **文档删除**：逐条删除，同步清除向量库记录
- **统计面板**：文档总数、分块总数、总字符数

### `/search` — 知识检索

- **语义检索**：基于向量相似度搜索知识库
- **关键词降级**：向量检索无结果时自动切换关键词匹配
- **结果排序**：按相似度分数降序展示
- **TopK 配置**：可调节返回结果数量（默认 5）

### `/debug` — 接口调试

- 提供各接口的可视化调试入口
- 用于本地开发时验证后端连通性

---

## 接口说明

所有接口通过 `src/api/ai.js` 封装，Axios baseURL 为 `/api`，经 Vite 代理转发到 `http://localhost:8092`。

### AI 对话类

#### `chat(data)` — 普通聊天（同步）

```
POST /api/ai/chat
```

| 参数 | 类型 | 说明 |
|---|---|---|
| message | string | 用户输入内容 |
| sessionId | string | 会话 ID（可选） |

返回：`{ data: "AI 回复内容" }`

---

#### `chatStream(data)` — 流式聊天（SSE）

```
POST /api/ai/chat/stream
```

> **注意**：该接口使用原生 `fetch` 而非 Axios，因为 Axios 不支持 SSE 流式读取。

| 参数 | 类型 | 说明 |
|---|---|---|
| message | string | 用户输入内容 |
| sessionId | string | 会话 ID（可选） |

响应格式：`text/event-stream`，每个事件携带一个文本片段，`[DONE]` 表示流结束。

前端读取示例：

```javascript
const response = await chatStream({ message, sessionId })
const reader = response.body.getReader()
const decoder = new TextDecoder()

while (true) {
  const { done, value } = await reader.read()
  if (done) break
  const chunk = decoder.decode(value)
  // 逐块拼接到 AI 回复
}
```

---

#### `ragAsk(question, topK)` — RAG 知识库问答

```
POST /api/ai/rag/ask?question=xxx&topK=3
```

先检索知识库，将相关文档作为上下文，再调用 LLM 生成回答。

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| question | string | — | 用户问题 |
| topK | number | 3 | 检索返回的文档数 |

---

#### `executeTask(data)` — Agent 任务执行

```
POST /api/ai/agent/task
```

触发 AI Agent 执行工具调用（Function Calling）任务。

---

#### `healthCheck()` — 健康检查

```
GET /api/ai/health
```

用于检测后端服务与 AI 模型连通性。

---

### 知识库管理类

#### `loadText(text, title)` — 文本入库

```
POST /api/ai/rag/load-text
Body: { "text": "...", "title": "文档标题" }
```

> **重要**：参数通过 `@RequestBody` JSON 传入，不可改为 URL 参数（会触发 HTTP 431 错误）。

将纯文本切分为块，写入向量库，同时持久化到 MySQL `knowledge_document` 表。

---

#### `uploadDocument(file, title)` — 文件上传入库

```
POST /api/ai/knowledge/upload
Content-Type: multipart/form-data
```

| 参数 | 类型 | 说明 |
|---|---|---|
| file | File | 上传的文档文件 |
| title | string | 文档标题（可选） |

支持格式：PDF、DOCX、TXT、MD 等（后端使用 Apache Tika 解析）。

---

#### `searchKnowledgeBase(query, topK)` — 知识库检索

```
GET /api/ai/knowledge/search?query=xxx&topK=5
```

| 参数 | 类型 | 默认值 | 说明 |
|---|---|---|---|
| query | string | — | 检索关键词 |
| topK | number | 5 | 返回结果数量 |

返回：相似度排序的文档块列表，含相似度分数。

---

#### `getKnowledgeStats()` — 知识库统计

```
GET /api/ai/knowledge/stats
```

返回：

```json
{
  "documentCount": 10,
  "totalChunks": 128,
  "totalCharacters": 95000
}
```

> 后端通过聚合查询（`SUM`）实现，避免全表扫描。

---

#### `getDocuments()` — 文档列表

```
GET /api/ai/knowledge/documents
```

返回所有已入库文档的元数据列表。

---

#### `deleteDocument(id)` — 删除文档

```
DELETE /api/ai/knowledge/documents/{id}
```

删除指定文档，同步清除向量库中对应的向量记录。

---

## 状态管理

`src/stores/chat.js` 使用 Pinia 管理聊天状态：

| 状态 | 类型 | 说明 |
|---|---|---|
| `sessions` | `Ref<Array>` | 会话列表 |
| `currentSessionId` | `Ref<string>` | 当前激活会话 ID |
| `messages` | `Ref<Array>` | 当前会话消息列表 |

| 方法 | 说明 |
|---|---|
| `createSession()` | 创建新会话，重置消息列表 |
| `addMessage(role, content)` | 追加一条消息（role: `user` / `assistant`） |
| `updateLastMessage(content)` | 流式更新最后一条 assistant 消息 |
| `clearMessages()` | 清空当前会话消息 |

---

## 代理配置

`vite.config.js` 中配置了开发代理，将前端所有 `/api/*` 请求转发到后端：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8092',  // 后端地址
      changeOrigin: true
    }
  }
}
```

这样前端代码中统一使用 `/api/xxx` 路径，无需关心跨域问题。

---

## 常见问题

### Q1：启动后页面空白或路由 404

确认使用 `npm run dev` 启动开发服务器，不要直接打开 `index.html` 文件。

### Q2：AI 对话无响应（SSE 流式静默）

可能原因：
1. 后端未启动，检查 `http://localhost:8092/doc.html` 是否可访问
2. 使用了 thinking 模型（如 `qwen3-vl-235b-a22b-thinking`），该模型输出在 `reasoning_content` 字段，Spring AI 0.8.x 无法读取，请换用 `qwen-plus`
3. `DASHSCOPE_API_KEY` 未配置，后端启动时会报错

### Q3：上传文件报 413 错误

后端 `application.yml` 需配置：

```yaml
server:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB
```

### Q4：发送长文本报 HTTP 431 错误

文本内容必须通过 `@RequestBody` JSON 传输，不能拼接到 URL 参数（浏览器对请求头大小有限制）。`loadText` 接口已使用 `data: { text, title }` 方式传参。

### Q5：后端连接 DashScope 报 404（路径含 `/v1/v1/`）

检查 `application.yml` 中的 `base-url` 配置：

```yaml
# 正确（Spring AI 会自动追加 /v1）
base-url: https://dashscope.aliyuncs.com/compatible-mode

# 错误（会生成 /v1/v1/chat/completions）
base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

---

## 相关文档

| 文档 | 说明 |
|---|---|
| [05-Spring-AI智能体详解](../../docs/07-SpringCloud微服务/05-Spring-AI智能体详解.md) | Spring AI 框架基础、DashScope 接入、面试题 |
| [06-Spring-AI知识库与RAG实战](../../docs/07-SpringCloud微服务/06-Spring-AI知识库与RAG实战.md) | 知识库设计、文件上传、向量检索 |
| [07-Spring-AI前端集成实战](../../docs/07-SpringCloud微服务/07-Spring-AI前端集成实战.md) | 前端架构、SSE 流式对话、Vite 代理 |

---

**维护者：** itzixiao  
**后端模块：** `interview-microservices-parent/interview-spring-ai`  
**后端端口：** 8092 | **前端端口：** 3000
