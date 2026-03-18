<template>
  <el-dialog v-model="visible" title="审批流程轨迹" width="680px" destroy-on-close>
    <div v-loading="loading">
      <!-- 流程实例信息 -->
      <el-alert v-if="processInfo" :closable="false" type="info" style="margin-bottom: 16px;">
        <template #title>
          <span>流程：{{ processInfo.processDefinitionName }} &nbsp;|&nbsp; 业务单号：{{ businessLabel }}</span>
          <el-tag :type="processInfo.status === '已结束' ? 'info' : 'success'" size="small" style="margin-left: 8px;">
            {{ processInfo.status }}
          </el-tag>
        </template>
      </el-alert>

      <!-- 审批历史时间轴 -->
      <el-timeline v-if="historyList.length > 0">
        <el-timeline-item
            v-for="(item, idx) in historyList"
            :key="idx"
            :type="timelineType(item)"
            :timestamp="formatTime(item.startTime)"
            placement="top"
        >
          <el-card shadow="hover" style="padding: 0;">
            <div class="timeline-content">
              <div class="node-name">
                <el-icon>
                  <Check v-if="item.status === '已完成'"/>
                  <Clock v-else/>
                </el-icon>
                {{ item.taskName }}
                <el-tag :type="item.status === '已完成' ? 'success' : 'warning'" size="small" style="margin-left: 6px;">
                  {{ item.status }}
                </el-tag>
              </div>
              <div class="node-meta">
                <span><el-icon><User/></el-icon> 审批人：{{ item.assignee || '未分配' }}</span>
                <span v-if="item.endTime" style="margin-left: 16px;">
                  <el-icon><Timer/></el-icon> 耗时：{{ formatDuration(item.durationInMillis) }}
                </span>
              </div>
              <div v-if="item.comment" class="node-comment">
                <el-icon>
                  <ChatDotRound/>
                </el-icon>
                审批意见：{{ item.comment }}
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-else-if="!loading" description="暂无审批记录"/>
    </div>

    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import {ref, watch} from 'vue'
import {getProcessHistory, getProcessInstance} from '@/api/workflow'
import {ChatDotRound, Check, Clock, Timer, User} from "@element-plus/icons-vue";

const props = defineProps({
  modelValue: Boolean,
  processInstanceId: String,
  businessLabel: {type: String, default: ''}
})

const emit = defineEmits(['update:modelValue'])

const visible = ref(false)
const loading = ref(false)
const historyList = ref([])
const processInfo = ref(null)

watch(() => props.modelValue, val => {
  visible.value = val
  if (val && props.processInstanceId) {
    loadHistory()
  }
})

watch(visible, val => {
  emit('update:modelValue', val)
})

async function loadHistory() {
  if (!props.processInstanceId) return
  loading.value = true
  historyList.value = []
  processInfo.value = null
  try {
    const [histRes, piRes] = await Promise.all([
      getProcessHistory(props.processInstanceId),
      getProcessInstance(props.processInstanceId)
    ])
    historyList.value = histRes.data || []
    processInfo.value = piRes.data || null

    console.log('=== 流程审批历史 ===')
    console.log('流程实例ID:', props.processInstanceId)
    console.log('历史节点数:', historyList.value.length)
    historyList.value.forEach((item, i) => {
      console.log(`  [${i + 1}] ${item.taskName} | 审批人: ${item.assignee} | 状态: ${item.status} | 意见: ${item.comment || '无'}`)
    })
  } finally {
    loading.value = false
  }
}

function timelineType(item) {
  if (item.status === '已完成') return 'success'
  return 'warning'
}

function formatTime(dateStr) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

function formatDuration(ms) {
  if (!ms) return '-'
  const s = Math.floor(ms / 1000)
  if (s < 60) return s + '秒'
  const m = Math.floor(s / 60)
  if (m < 60) return m + '分' + (s % 60) + '秒'
  return Math.floor(m / 60) + '时' + (m % 60) + '分'
}
</script>

<style scoped>
.timeline-content {
  padding: 8px 12px;
}

.node-name {
  font-size: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 4px;
}

.node-meta {
  color: #666;
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.node-comment {
  color: #1890ff;
  font-size: 12px;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
