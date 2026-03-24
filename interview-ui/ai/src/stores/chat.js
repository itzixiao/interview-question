import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useChatStore = defineStore('chat', () => {
  // 会话列表
  const sessions = ref([])
  
  // 当前会话ID
  const currentSessionId = ref('')
  
  // 消息历史
  const messages = ref([])
  
  // 创建新会话
  const createSession = () => {
    const sessionId = Date.now().toString()
    const session = {
      id: sessionId,
      title: `会话 ${sessions.value.length + 1}`,
      createTime: new Date().toLocaleString()
    }
    sessions.value.unshift(session)
    currentSessionId.value = sessionId
    messages.value = []
    return sessionId
  }
  
  // 添加消息
  const addMessage = (role, content) => {
    messages.value.push({
      id: Date.now().toString(),
      role,
      content,
      timestamp: new Date().toLocaleTimeString()
    })
  }
  
  // 更新最后一条消息
  const updateLastMessage = (content) => {
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.role === 'assistant') {
      lastMsg.content = content
    }
  }
  
  // 清空当前会话
  const clearMessages = () => {
    messages.value = []
  }
  
  return {
    sessions,
    currentSessionId,
    messages,
    createSession,
    addMessage,
    updateLastMessage,
    clearMessages
  }
})
