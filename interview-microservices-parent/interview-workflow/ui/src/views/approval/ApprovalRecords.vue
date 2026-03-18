<template>
  <div class="approval-records">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的审批记录</span>
          <el-button type="primary" :icon="Refresh" @click="fetchRecords">刷新</el-button>
        </div>
      </template>

      <el-table :data="records" v-loading="loading" stripe border>
        <el-table-column prop="applyNo" label="申请编号" width="180"/>
        <el-table-column prop="businessType" label="业务类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.businessType === '请假申请' ? 'success' : 'warning'">
              {{ row.businessType }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="100"/>
        <el-table-column prop="businessSummary" label="申请内容" min-width="150"/>
        <el-table-column prop="taskName" label="审批节点" width="120"/>
        <el-table-column prop="businessStatus" label="申请状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.businessStatus)">
              {{ row.businessStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="我的审批意见" min-width="150">
          <template #default="{ row }">
            <span>{{ row.comment || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="审批时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ formatDuration(row.durationInMillis) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewHistory(row)">
              审批轨迹
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
            v-model:current-page="pagination.pageNum"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next"
            @size-change="fetchRecords"
            @current-change="fetchRecords"
        />
      </div>
    </el-card>

    <!-- 审批轨迹弹窗 -->
    <ApprovalHistoryDialog
        v-model="historyDialogVisible"
        :process-instance-id="currentProcessInstanceId"
    />
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {Refresh} from '@element-plus/icons-vue'
import {getMyApprovalRecords} from '@/api/workflow'
import ApprovalHistoryDialog from '@/components/ApprovalHistoryDialog.vue'
import {ElMessage} from 'element-plus'

const loading = ref(false)
const records = ref([])
const pagination = ref({
  pageNum: 1,
  pageSize: 20,
  total: 0
})

// 审批轨迹弹窗
const historyDialogVisible = ref(false)
const currentProcessInstanceId = ref('')

// 获取审批记录
const fetchRecords = async () => {
  loading.value = true
  try {
    const res = await getMyApprovalRecords({
      pageNum: pagination.value.pageNum,
      pageSize: pagination.value.pageSize
    })
    // request.js 拦截器已解包，res 就是 {code, data, message} 结构
    records.value = res.data || []
    // 因为后端没返回total，先用records长度
    pagination.value.total = records.value.length
  } catch (error) {
    console.error('获取审批记录失败:', error)
    ElMessage.error('获取审批记录失败')
  } finally {
    loading.value = false
  }
}

// 查看审批轨迹
const viewHistory = (row) => {
  currentProcessInstanceId.value = row.processInstanceId
  historyDialogVisible.value = true
}

// 状态颜色映射
const getStatusType = (status) => {
  const map = {
    '审批中': 'warning',
    '已通过': 'success',
    '已拒绝': 'danger',
    '已撤回': 'info',
    '草稿': ''
  }
  return map[status] || ''
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化耗时
const formatDuration = (millis) => {
  if (!millis) return '-'
  const seconds = Math.floor(millis / 1000)
  if (seconds < 60) return `${seconds}秒`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时`
  const days = Math.floor(hours / 24)
  return `${days}天`
}

onMounted(() => {
  fetchRecords()
})
</script>

<style scoped>
.approval-records {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
