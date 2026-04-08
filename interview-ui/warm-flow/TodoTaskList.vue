<template>
  <div class="todo-task-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的待办任务</span>
          <el-badge :value="totalCount" :max="99" class="badge">
            <el-button @click="loadTasks" :icon="Refresh">
              刷新
            </el-button>
          </el-badge>
        </div>
      </template>

      <!-- 任务列表 -->
      <el-table :data="taskList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="任务ID" width="100" align="center" />
        <el-table-column prop="nodeName" label="节点名称" width="150" />
        <el-table-column prop="flowName" label="流程名称" min-width="150" />
        <el-table-column prop="businessId" label="业务ID" width="120" align="center" />
        <el-table-column prop="createBy" label="发起人" width="100" />
        <el-table-column prop="createTime" label="到达时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleApprove(row)">
              <el-icon><Select /></el-icon>
              审批
            </el-button>
            <el-button size="small" @click="viewFlowChart(row)">
              <el-icon><View /></el-icon>
              流程图
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadTasks"
        @current-change="loadTasks"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog 
      v-model="approveDialogVisible" 
      :title="`审批 - ${currentTask?.nodeName || ''}`" 
      width="600px"
    >
      <!-- 业务详情 -->
      <el-descriptions :column="2" border class="business-info">
        <el-descriptions-item label="流程名称">
          {{ currentTask?.flowName }}
        </el-descriptions-item>
        <el-descriptions-item label="发起人">
          {{ currentTask?.createBy }}
        </el-descriptions-item>
        <el-descriptions-item label="业务ID">
          {{ currentTask?.businessId }}
        </el-descriptions-item>
        <el-descriptions-item label="到达时间">
          {{ currentTask?.createTime }}
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <!-- 审批表单 -->
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="审批结果" required>
          <el-radio-group v-model="approveForm.approved">
            <el-radio :label="true">
              <el-icon color="#67c23a"><CircleCheck /></el-icon>
              通过
            </el-radio>
            <el-radio :label="false">
              <el-icon color="#f56c6c"><CircleClose /></el-icon>
              驳回
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见" required>
          <el-input 
            v-model="approveForm.comment" 
            type="textarea" 
            :rows="4"
            placeholder="请输入审批意见（必填）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval" :loading="submitting">
          提交审批
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Select, View, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import axios from 'axios'

// 状态
const loading = ref(false)
const submitting = ref(false)
const taskList = ref([])
const approveDialogVisible = ref(false)
const currentTask = ref(null)

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 审批表单
const approveForm = reactive({
  approved: true,
  comment: ''
})

// 总数
const totalCount = computed(() => pagination.total)

// 加载任务列表
onMounted(() => {
  loadTasks()
})

async function loadTasks() {
  loading.value = true
  try {
    const res = await axios.get('/warm-flow/task/todo', {
      params: {
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize
      }
    })
    
    taskList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (err) {
    ElMessage.error('加载任务失败: ' + err.message)
  } finally {
    loading.value = false
  }
}

// 审批
function handleApprove(row) {
  currentTask.value = row
  approveForm.approved = true
  approveForm.comment = ''
  approveDialogVisible.value = true
}

// 提交审批
async function submitApproval() {
  if (!approveForm.comment.trim()) {
    ElMessage.warning('请输入审批意见')
    return
  }

  submitting.value = true
  try {
    await axios.post(`/warm-flow/task/approve/${currentTask.value.instanceId}`, {
      approved: approveForm.approved,
      comment: approveForm.comment
    })
    
    ElMessage.success('审批成功')
    approveDialogVisible.value = false
    loadTasks()
  } catch (err) {
    ElMessage.error('审批失败: ' + err.message)
  } finally {
    submitting.value = false
  }
}

// 查看流程图
function viewFlowChart(row) {
  window.open(`/warm-flow-ui?instanceId=${row.instanceId}`, '_blank')
}
</script>

<style scoped>
.todo-task-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.badge {
  margin-left: 12px;
}

.business-info {
  margin-bottom: 16px;
}

:deep(.el-radio) {
  margin-right: 24px;
}

:deep(.el-radio__label) {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
