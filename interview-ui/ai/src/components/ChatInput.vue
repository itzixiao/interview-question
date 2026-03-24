<template>
  <div class="chat-input">
    <div class="input-toolbar">
      <div class="toolbar-switches">
        <div class="switch-item" :class="{ active: useKnowledge }" @click="useKnowledge = !useKnowledge">
          <el-icon :size="13"><Collection /></el-icon>
          <span>知识库</span>
        </div>
        <div class="switch-item" :class="{ active: useStream }" @click="useStream = !useStream">
          <el-icon :size="13"><Lightning /></el-icon>
          <span>流式</span>
        </div>
      </div>
      <span class="input-tip">Enter 发送 &nbsp;·&nbsp; Shift+Enter 换行</span>
    </div>
    <div class="input-area">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="3"
        placeholder="输入消息..."
        resize="none"
        @keydown.enter.prevent="handleEnter"
      />
      <button
        class="send-btn"
        :class="{ loading }"
        :disabled="loading || !inputMessage.trim()"
        @click="sendMessage"
      >
        <el-icon v-if="!loading" :size="18"><Promotion /></el-icon>
        <el-icon v-else :size="18" class="spin"><Loading /></el-icon>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Promotion, Collection, Lightning, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['send'])

const inputMessage = ref('')
const useKnowledge = ref(false)
const useStream = ref(true)

const handleEnter = (e) => {
  if (!e.shiftKey) sendMessage()
}

const sendMessage = () => {
  const message = inputMessage.value.trim()
  if (!message || props.loading) return
  emit('send', { message, useKnowledge: useKnowledge.value, useStream: useStream.value })
  inputMessage.value = ''
}
</script>

<style scoped lang="scss">
.chat-input {
  background: #fff;
  border-top: 1px solid rgba(0,0,0,0.06);
  padding: 14px 24px 18px;
  box-shadow: 0 -4px 20px rgba(0,0,0,0.04);
}

.input-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;

  .toolbar-switches {
    display: flex;
    gap: 8px;
  }

  .switch-item {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 5px 12px;
    border-radius: 20px;
    font-size: 12px;
    color: #909399;
    background: #f5f7fa;
    border: 1px solid transparent;
    cursor: pointer;
    transition: all 0.2s;
    user-select: none;

    &:hover {
      background: rgba(102, 126, 234, 0.06);
      color: #667eea;
    }

    &.active {
      background: rgba(102, 126, 234, 0.1);
      color: #667eea;
      border-color: rgba(102, 126, 234, 0.3);
      font-weight: 500;
    }
  }

  .input-tip {
    font-size: 11px;
    color: #c0c4cc;
  }
}

.input-area {
  display: flex;
  gap: 10px;
  align-items: flex-end;

  :deep(.el-textarea__inner) {
    border-radius: 14px;
    padding: 12px 16px;
    font-size: 14px;
    line-height: 1.6;
    border-color: #e8eaf0;
    transition: all 0.2s;
    resize: none;

    &:focus {
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.12);
    }
  }
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);

  &:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(102, 126, 234, 0.45);
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
    transform: none;
  }

  &.loading {
    opacity: 0.7;
  }
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
</style>
