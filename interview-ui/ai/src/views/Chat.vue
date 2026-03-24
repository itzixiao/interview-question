<template>
  <div class="chat-page">
    <!-- 页头 -->
    <div class="chat-header">
      <div class="header-left">
        <div class="header-avatar">
          <el-icon :size="18"><ChatRound /></el-icon>
        </div>
        <div>
          <h2>AI 对话</h2>
          <p class="header-sub">支持普通对话、知识库问答、流式输出</p>
        </div>
      </div>
      <el-button class="new-session-btn" @click="createNewSession">
        <el-icon><Plus /></el-icon>
        新对话
      </el-button>
    </div>

    <!-- 聊天区 -->
    <div class="chat-container">
      <div class="chat-messages" ref="messagesRef">
        <!-- 空状态 -->
        <div v-if="chatStore.messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <el-icon :size="40"><ChatDotRound /></el-icon>
          </div>
          <h3>开始与 AI 助手对话</h3>
          <p>支持普通对话、知识库问答和流式输出</p>
          <div class="quick-starts">
            <div
              v-for="q in quickQuestions"
              :key="q"
              class="quick-card"
              @click="quickAsk(q)"
            >
              {{ q }}
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <ChatMessage
            v-for="msg in chatStore.messages"
            :key="msg.id"
            :message="msg"
          />
          <div v-if="loading" class="typing-indicator">
            <div class="typing-avatar">
              <el-icon :size="14"><Cpu /></el-icon>
            </div>
            <div class="typing-dots">
              <span></span><span></span><span></span>
            </div>
          </div>
        </template>
      </div>

      <ChatInput :loading="loading" @send="handleSend" />
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Plus, ChatDotRound, ChatRound, Cpu } from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'
import { chat, chatStream, ragAsk } from '@/api/ai'
import ChatMessage from '@/components/ChatMessage.vue'
import ChatInput from '@/components/ChatInput.vue'

const chatStore = useChatStore()
const loading = ref(false)
const messagesRef = ref(null)

const quickQuestions = [
  'Spring Boot 自动装配原理是什么？',
  'Redis 分布式锁如何实现？',
  'JVM 垃圾回收机制详解',
  'MySQL 索引优化最佳实践',
]

const createNewSession = () => chatStore.createSession()

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const quickAsk = (q) => {
  handleSend({ message: q, useKnowledge: false, useStream: true })
}

const handleSend = async ({ message, useKnowledge, useStream }) => {
  chatStore.addMessage('user', message)
  loading.value = true
  scrollToBottom()
  chatStore.addMessage('assistant', '')

  try {
    if (useKnowledge) {
      const res = await ragAsk(message, 3)
      chatStore.updateLastMessage(res.content || res.data?.content || '暂无回答')
    } else if (useStream) {
      const response = await chatStream({
        message,
        sessionId: chatStore.currentSessionId,
        history: chatStore.messages.slice(0, -2).map(m => ({ role: m.role, content: m.content }))
      })
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let fullContent = ''
      while (true) {
        const { done, value } = await reader.read()
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
            } catch {}
          }
        }
      }
    } else {
      const res = await chat({
        message,
        sessionId: chatStore.currentSessionId,
        history: chatStore.messages.slice(0, -2).map(m => ({ role: m.role, content: m.content }))
      })
      chatStore.updateLastMessage(res.content || res.data?.content || '暂无回答')
    }
  } catch {
    chatStore.updateLastMessage('抒歉，服务暂时不可用，请稍后重试。')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  if (!chatStore.currentSessionId) createNewSession()
})
</script>

<style scoped lang="scss">
.chat-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f8;
}

/* 页头 */
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: #fff;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 1px 8px rgba(0,0,0,0.04);

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;

    .header-avatar {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    h2 {
      margin: 0;
      font-size: 17px;
      font-weight: 600;
      color: #1a1a2e;
    }

    .header-sub {
      margin: 2px 0 0;
      font-size: 12px;
      color: #a0a3bd;
    }
  }

  .new-session-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 18px;
    border-radius: 10px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    border: none;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(102, 126, 234, 0.45);
    }
  }
}

/* 聊天区 */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 28px 32px;
  scroll-behavior: smooth;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.12); border-radius: 2px; }
}

/* 空状态 */
.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #a0a3bd;
  padding-bottom: 60px;

  .empty-icon {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, rgba(102,126,234,0.12), rgba(118,75,162,0.1));
    border-radius: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #667eea;
    margin-bottom: 20px;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.15);
  }

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 8px;
  }

  p {
    font-size: 14px;
    color: #909399;
    margin: 0 0 32px;
  }

  .quick-starts {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    max-width: 560px;
    width: 100%;

    .quick-card {
      padding: 14px 18px;
      background: #fff;
      border: 1px solid rgba(102, 126, 234, 0.15);
      border-radius: 12px;
      font-size: 13px;
      color: #606266;
      cursor: pointer;
      text-align: left;
      transition: all 0.2s;
      line-height: 1.5;
      box-shadow: 0 2px 8px rgba(0,0,0,0.04);

      &:hover {
        border-color: #667eea;
        color: #667eea;
        background: rgba(102, 126, 234, 0.04);
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(102, 126, 234, 0.12);
      }
    }
  }
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 8px 16px;

  .typing-avatar {
    width: 36px;
    height: 36px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
  }

  .typing-dots {
    background: #fff;
    border-radius: 18px;
    padding: 12px 18px;
    display: flex;
    align-items: center;
    gap: 5px;
    box-shadow: 0 2px 10px rgba(0,0,0,0.08);

    span {
      width: 7px;
      height: 7px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      border-radius: 50%;
      animation: bounce 1.4s infinite ease-in-out both;

      &:nth-child(1) { animation-delay: -0.32s; }
      &:nth-child(2) { animation-delay: -0.16s; }
    }
  }
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.5); opacity: 0.4; }
  40%            { transform: scale(1);   opacity: 1; }
}
</style>
