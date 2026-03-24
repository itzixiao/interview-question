# Spring AI 前端集成实战

## 一、项目架构

### 1.1 前端技术栈

| 技术           | 版本  | 用途          |
|--------------|-----|-------------|
| Vue 3        | 3.x | 前端框架        |
| Element Plus | 2.x | UI 组件库      |
| Pinia        | 2.x | 状态管理        |
| Axios        | 1.x | HTTP 请求     |
| Vite         | 5.x | 构建工具        |
| SCSS         | -   | CSS 预处理器    |
| marked       | -   | Markdown 渲染 |
| highlight.js | -   | 代码高亮        |

### 1.2 目录结构

```
interview-ui/ai/
├── src/
│   ├── api/
│   │   └── ai.js              # 所有后端接口封装
│   ├── components/
│   │   ├── ChatInput.vue      # 聊天输入框（模式切换 + 发送按钮）
│   │   └── ChatMessage.vue    # 消息气泡（Markdown + 代码高亮）
│   ├── router/
│   │   └── index.js           # 路由配置
│   ├── stores/
│   │   └── chat.js            # 聊天状态管理（Pinia）
│   ├── utils/
│   │   └── request.js         # Axios 实例（含代理前缀）
│   ├── views/
│   │   ├── Layout.vue         # 主布局（侧边栏导航）
│   │   ├── Chat.vue           # 聊天页
│   │   ├── Knowledge.vue      # 知识库管理页
│   │   ├── Search.vue         # 知识库检索页
│   │   └── Debug.vue          # 接口调试页
│   ├── App.vue                # 根组件（全局样式）
│   └── main.js
├── vite.config.js             # Vite 配置（含代理）
└── package.json
```

### 1.3 Vite 代理配置

前端运行在 `localhost:3000`，所有 `/api` 请求代理到后端 `localhost:8092`：

```js
// vite.config.js
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {'@': resolve(__dirname, 'src')}
    },
    server: {
        port: 3000,
        proxy: {
            '/api': {
                target: 'http://localhost:8092',
                changeOrigin: true   // 不修改路径，保留 /api 前缀
            }
        }
    }
})
```

> **后端接口前缀**：后端 Controller 使用 `/api/ai/**`，Vite 代理后无需重写路径。

---

## 二、API 封装层

### 2.1 Axios 请求实例

```js
// src/utils/request.js
import axios from 'axios'

const request = axios.create({
    baseURL: '/api',       // 配合 Vite 代理，实际转发到 localhost:8092/api
    timeout: 30000
})

// 统一响应处理
request.interceptors.response.use(
    res => res.data,
    err => Promise.reject(err)
)

export default request
```

### 2.2 AI 接口封装

```js
// src/api/ai.js

// ── 对话 ──────────────────────────────────────────────

/** 同步对话 */
export function chat(data) {
    return request({url: '/ai/chat', method: 'post', data})
}

/**
 * 流式 SSE 对话
 * 不使用 Axios，直接用原生 fetch（Axios 不支持流式读取）
 */
export function chatStream(data) {
    return fetch('/api/ai/chat/stream', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(data)
    })
}

/** RAG 知识库问答 */
export function ragAsk(question, topK = 3) {
    return request({url: '/ai/rag/ask', method: 'post', params: {question, topK}})
}

/**
 * 加载文本到知识库
 * ⚠️ 必须用 data（JSON Body），不能用 params（URL 参数），否则触发 HTTP 431
 */
export function loadText(text, title) {
    return request({url: '/ai/rag/load-text', method: 'post', data: {text, title}})
}

// ── 知识库管理 ──────────────────────────────────────────

/** 上传文件（multipart/form-data） */
export function uploadDocument(file, title) {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    return request({
        url: '/ai/knowledge/upload',
        method: 'post',
        data: formData,
        headers: {'Content-Type': 'multipart/form-data'}
    })
}

/** 向量相似度检索 */
export function searchKnowledgeBase(query, topK = 5) {
    return request({url: '/ai/knowledge/search', method: 'get', params: {query, topK}})
}

/** 获取统计信息 */
export function getKnowledgeStats() {
    return request({url: '/ai/knowledge/stats', method: 'get'})
}

/** 获取文档列表 */
export function getDocuments() {
    return request({url: '/ai/knowledge/documents', method: 'get'})
}

/** 删除文档 */
export function deleteDocument(id) {
    return request({url: `/ai/knowledge/documents/${id}`, method: 'delete'})
}

// ── Agent ──────────────────────────────────────────────

export function executeTask(data) {
    return request({url: '/ai/agent/task', method: 'post', data})
}

export function healthCheck() {
    return request({url: '/ai/health', method: 'get'})
}
```

---

## 三、SSE 流式聊天实现

### 3.1 核心逻辑（Chat.vue）

SSE 流式聊天使用原生 `fetch` + `ReadableStream` 读取，**不使用 Axios**（Axios 无法流式读取响应体）：

```js
// 发起流式请求
const response = await chatStream({
    message,
    sessionId: chatStore.currentSessionId,
    history: chatStore.messages
        .slice(0, -2)
        .map(m => ({role: m.role, content: m.content}))
})

// 流式读取
const reader = response.body.getReader()
const decoder = new TextDecoder()
let fullContent = ''

while (true) {
    const {done, value} = await reader.read()
    if (done) break

    const chunk = decoder.decode(value)
    for (const line of chunk.split('\n')) {
        if (line.startsWith('data:')) {
            try {
                const data = JSON.parse(line.slice(5).trim())
                if (data.content) {
                    fullContent += data.content
                    chatStore.updateLastMessage(fullContent)
                    scrollToBottom()
                }
            } catch { /* 忽略非 JSON 行 */
            }
        }
    }
}
```

**SSE 数据格式（后端推送）**：

```
data: {"content":"你","sessionId":"xxx","success":true}

data: {"content":"好","sessionId":"xxx","success":true}

data: {"content":"！","sessionId":"xxx","success":true}
```

### 3.2 多轮对话历史传递

```js
// 发送时携带历史消息（最近 N 条，排除当前正在生成的空消息）
history: chatStore.messages
    .slice(0, -2)
    .map(m => ({role: m.role, content: m.content}))
```

后端 `ChatRequest` 结构：

```java
public class ChatRequest {
    private String message;
    private String sessionId;
    private List<MessageHistory> history;

    @Data
    public static class MessageHistory {
        private String role;    // user / assistant / system
        private String content;
    }
}
```

### 3.3 聊天模式切换

`ChatInput.vue` 提供两个 toggle 开关：

| 开关    | 说明                                             |
|-------|------------------------------------------------|
| 知识库模式 | 开启后调用 `/api/ai/rag/ask`，关闭调用 `/api/ai/chat`    |
| 流式输出  | 开启后调用 `/api/ai/chat/stream`（SSE），关闭调用普通 `chat` |

```vue

<script setup>
  const emit = defineEmits(['send'])
  const inputValue = ref('')
  const useKnowledge = ref(false)
  const useStream = ref(true)

  const handleSend = () => {
    if (!inputValue.value.trim() || props.loading) return
    emit('send', {
      message: inputValue.value.trim(),
      useKnowledge: useKnowledge.value,
      useStream: useStream.value
    })
    inputValue.value = ''
  }
</script>
```

---

## 四、Pinia 状态管理

### 4.1 聊天状态 Store

```js
// src/stores/chat.js
import {defineStore} from 'pinia'

export const useChatStore = defineStore('chat', {
    state: () => ({
        messages: [],
        currentSessionId: null,
        sessions: []
    }),

    actions: {
        /** 创建新会话 */
        createSession() {
            this.currentSessionId = Date.now().toString()
            this.messages = []
        },

        /** 添加消息 */
        addMessage(role, content) {
            this.messages.push({
                id: Date.now(),
                role,        // 'user' | 'assistant'
                content,
                timestamp: new Date().toLocaleTimeString()
            })
        },

        /** 更新最后一条消息内容（用于流式追加） */
        updateLastMessage(content) {
            if (this.messages.length > 0) {
                this.messages[this.messages.length - 1].content = content
            }
        }
    }
})
```

---

## 五、知识库管理页（Knowledge.vue）

### 5.1 文档上传

```vue

<script setup>
  import {uploadDocument, getDocuments, deleteDocument, getKnowledgeStats} from '@/api/ai'

  // 从知识库文档列表加载并直接上传文本
  const handleLoadDocText = async (doc) => {
    // 读取文档 Markdown 内容后调用 loadText
    const response = await fetch(doc.path)
    const text = await response.text()
    await loadText(text, doc.title)
    ElMessage.success(`《${doc.title}》已加载到知识库`)
    await fetchDocuments()
  }

  // 文件上传
  const handleUpload = async (file) => {
    const res = await uploadDocument(file.raw, file.name)
    if (res.success) {
      ElMessage.success('文档上传成功')
      await fetchDocuments()
      await fetchStats()
    }
  }
</script>
```

### 5.2 统计卡片数据

```js
const stats = ref({documentCount: 0, totalChunks: 0, totalSize: 0})

const fetchStats = async () => {
    const res = await getKnowledgeStats()
    if (res.success) {
        stats.value = res.data
    }
}
```

---

## 六、知识库检索页（Search.vue）

```vue

<script setup>
  import {searchKnowledgeBase, ragAsk} from '@/api/ai'

  const query = ref('')
  const mode = ref('vector')   // 'vector' | 'ai'
  const results = ref([])
  const aiAnswer = ref('')

  const handleSearch = async () => {
    if (!query.value.trim()) return
    loading.value = true
    try {
      if (mode.value === 'vector') {
        // 向量相似度检索
        const res = await searchKnowledgeBase(query.value, 5)
        results.value = res.data || []
      } else {
        // RAG AI 回答
        const res = await ragAsk(query.value, 3)
        aiAnswer.value = res.content || res.data?.content || '暂无回答'
      }
    } finally {
      loading.value = false
    }
  }
</script>
```

**检索模式对比**：

| 模式    | 接口                                       | 返回内容             |
|-------|------------------------------------------|------------------|
| 向量检索  | `GET /api/ai/knowledge/search?query=...` | 相似文档片段列表 + 相似度分数 |
| AI 问答 | `POST /api/ai/rag/ask`                   | AI 基于知识库生成的回答    |

---

## 七、Markdown 渲染与代码高亮

`ChatMessage.vue` 使用 **marked** 解析 Markdown，**highlight.js** 高亮代码块：

```js
import {marked} from 'marked'
import hljs from 'highlight.js'

// 配置 marked 使用 highlight.js
marked.setOptions({
    highlight(code, lang) {
        if (lang && hljs.getLanguage(lang)) {
            return hljs.highlight(code, {language: lang}).value
        }
        return hljs.highlightAuto(code).value
    },
    breaks: true
})

// 渲染 Markdown
const renderedContent = computed(() =>
    marked(props.message.content || '')
)
```

---

## 八、常见问题

### 8.1 SSE 连接断开

**现象**：流式输出到一半突然停止，控制台报 `net::ERR_INCOMPLETE_CHUNKED_ENCODING`。

**原因**：后端 Flux 流异常终止，或 Nginx/代理超时。

**解决**：

- 后端：在 `onError` 回调中确保 `sink.complete()` 被调用
- Vite 代理：增加超时配置（开发环境默认无超时限制）

### 8.2 CORS 跨域问题

**现象**：浏览器报 CORS 错误。

**原因**：直接访问 `localhost:8092` 时没有代理，触发跨域。

**解决**：确保前端所有请求走 `/api` 路径（由 Vite 代理转发），不要直接写死 8092 端口。

### 8.3 大文本 HTTP 431

**现象**：`POST /api/ai/rag/load-text?text=...` 报 431 Request Header Fields Too Large。

**原因**：文本内容通过 URL query string 传递，被编码到请求 Header 中超出 Tomcat 默认限制（8KB）。

**解决**：

1. 前端改为 `data: { text, title }`（JSON Body），不用 `params`
2. 后端改为 `@RequestBody Map<String, String>`，不用 `@RequestParam`
3. 可选：`application.yml` 中增加 `server.max-http-header-size: 65536`

### 8.4 流式输出无响应（SSE 静默）

**现象**：发送消息后后端日志显示"收到流式聊天请求"，但前端一直 loading，无内容输出。

**原因**：使用了 thinking 模型（如 `qwen3-vl-235b-a22b-thinking`），正文在 `reasoning_content` 字段，Spring AI 0.8.x 的
`getContent()` 返回 null，全部被过滤。

**解决**：换用标准模型（`qwen-plus`、`qwen-max`）。

---

## 九、本地启动命令

```bash
# 安装依赖
cd interview-ui/ai
npm install

# 开发模式（代理到 localhost:8092）
npm run dev

# 生产构建
npm run build
```

访问地址：`http://localhost:3000`

---

**维护者：** itzixiao  
**最后更新：** 2026-03-24  
**问题反馈：** 欢迎提 Issue 或 PR
