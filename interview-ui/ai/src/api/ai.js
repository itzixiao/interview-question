import request from '@/utils/request'

// AI 对话
export function chat(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data
  })
}

// 流式 AI 对话
export function chatStream(data) {
  return fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
}

// RAG 知识库问答
export function ragAsk(question, topK = 3) {
  return request({
    url: '/ai/rag/ask',
    method: 'post',
    params: { question, topK }
  })
}

// 加载文本到知识库
export function loadText(text, title) {
  return request({
    url: '/ai/rag/load-text',
    method: 'post',
    data: { text, title }
  })
}

// 健康检查
export function healthCheck() {
  return request({
    url: '/ai/health',
    method: 'get'
  })
}

// Agent 任务执行
export function executeTask(data) {
  return request({
    url: '/ai/agent/task',
    method: 'post',
    data
  })
}

// 上传文档到知识库
export function uploadDocument(file, title) {
  const formData = new FormData()
  formData.append('file', file)
  if (title) {
    formData.append('title', title)
  }
  return request({
    url: '/ai/knowledge/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 检索知识库
export function searchKnowledgeBase(query, topK = 5) {
  return request({
    url: '/ai/knowledge/search',
    method: 'get',
    params: { query, topK }
  })
}

// 获取知识库统计
export function getKnowledgeStats() {
  return request({
    url: '/ai/knowledge/stats',
    method: 'get'
  })
}

// 获取文档列表
export function getDocuments() {
  return request({
    url: '/ai/knowledge/documents',
    method: 'get'
  })
}

// 删除文档
export function deleteDocument(id) {
  return request({
    url: `/ai/knowledge/documents/${id}`,
    method: 'delete'
  })
}
