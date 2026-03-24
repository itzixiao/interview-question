<template>
  <div class="chat-message" :class="{ 'is-user': message.role === 'user' }">
    <div class="message-avatar">
      <div class="avatar-wrap" :class="message.role">
        <el-icon :size="16">
          <component :is="message.role === 'user' ? UserFilled : Cpu" />
        </el-icon>
      </div>
    </div>
    <div class="message-wrapper">
      <div class="message-meta">
        <span class="message-role">{{ message.role === 'user' ? '我' : 'AI 助手' }}</span>
        <span class="message-time">{{ message.timestamp }}</span>
      </div>
      <div class="message-body" v-html="renderedContent"></div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { UserFilled, Cpu } from '@element-plus/icons-vue'
import { marked } from 'marked'
import hljs from 'highlight.js'

const props = defineProps({
  message: { type: Object, required: true }
})

marked.setOptions({
  highlight: (code, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true
})

const renderedContent = computed(() => marked(props.message.content || ''))
</script>

<style scoped lang="scss">
.chat-message {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  animation: fadeUp 0.3s ease;

  &.is-user {
    flex-direction: row-reverse;

    .message-wrapper {
      align-items: flex-end;
    }

    .message-meta {
      flex-direction: row-reverse;
    }

    .message-body {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      border-radius: 18px 4px 18px 18px;
      box-shadow: 0 4px 16px rgba(102, 126, 234, 0.3);

      :deep(code) {
        background: rgba(255,255,255,0.2);
        color: #fff;
      }
    }
  }
}

.avatar-wrap {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;

  &.assistant {
    background: linear-gradient(135deg, #667eea, #764ba2);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.35);
  }

  &.user {
    background: linear-gradient(135deg, #f093fb, #f5576c);
    box-shadow: 0 4px 12px rgba(245, 87, 108, 0.35);
  }
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-width: 72%;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;

  .message-role {
    font-size: 12px;
    font-weight: 600;
    color: #606266;
  }

  .message-time {
    font-size: 11px;
    color: #c0c4cc;
  }
}

.message-body {
  background: #fff;
  padding: 13px 18px;
  border-radius: 4px 18px 18px 18px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  line-height: 1.7;
  color: #303133;
  font-size: 14px;

  :deep(pre) {
    background: #1a1a2e;
    border-radius: 10px;
    padding: 16px;
    overflow-x: auto;
    margin: 10px 0;
    border: 1px solid rgba(255,255,255,0.05);

    code {
      font-family: 'Fira Code', 'JetBrains Mono', monospace;
      font-size: 13px;
      color: #d4d4d4;
      background: none;
      padding: 0;
    }
  }

  :deep(code) {
    background: rgba(102, 126, 234, 0.08);
    color: #667eea;
    padding: 2px 7px;
    border-radius: 5px;
    font-family: 'Fira Code', monospace;
    font-size: 13px;
  }

  :deep(p) {
    margin: 8px 0;
    &:first-child { margin-top: 0; }
    &:last-child  { margin-bottom: 0; }
  }

  :deep(ul), :deep(ol) {
    margin: 8px 0;
    padding-left: 22px;
  }

  :deep(li) { margin: 5px 0; }

  :deep(h1), :deep(h2), :deep(h3) {
    margin: 14px 0 8px;
    color: #1a1a2e;
  }

  :deep(blockquote) {
    border-left: 3px solid #667eea;
    margin: 10px 0;
    padding: 4px 14px;
    color: #909399;
    background: rgba(102, 126, 234, 0.04);
    border-radius: 0 8px 8px 0;
  }

  :deep(table) {
    border-collapse: collapse;
    width: 100%;
    margin: 10px 0;
    font-size: 13px;

    th, td {
      border: 1px solid #e4e7ed;
      padding: 8px 12px;
      text-align: left;
    }

    th { background: #f5f7fa; font-weight: 600; }
    tr:nth-child(even) td { background: #fafafa; }
  }
}

@keyframes fadeUp {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}
</style>
