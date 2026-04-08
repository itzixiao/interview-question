<template>
  <div class="process-instance-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>流程实例管理</span>
          <el-button type="primary" @click="showStartDialog">
            <el-icon><Plus /></el-icon>
            发起流程
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="流程状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="审批中" :value="1" />
            <el-option label="已通过" :value="2" />
            <el-option label="已驳回" :value="3" />
            <el-option label="已撤销" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadInstances">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table :data="instanceList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="实例ID" width="100" align="center" />
        <el-table-column prop="businessId" label="业务ID" width="120" align="center" />
        <el-table-column prop="flowName" label="流程名称" min-width="150" />
        <el-table-column prop="flowStatus" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.flowStatus)" size="small">
              {{ getStatusText(row.flowStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentNode" label="当前节点" width="120" />
        <el-table-column prop="createBy" label="发起人" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">
              <el-icon><View /></el-icon>
              详情
            </el-button>
            <el-button 
              size="small" 
              type="primary" 
              @click="showApproveDialog(row)"
              :disabled="row.flowStatus !== 1"
            >
              <el-icon><Edit /></el-icon>
              审批
            </el-button>
            <el-button 
              size="small" 
              type="danger" 
              @click="cancelInstance(row)"
              :disabled="row.flowStatus !== 1"
            >
              <el-icon><Close /></el-icon>
              撤销
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadInstances"
        @current-change="loadInstances"
        style="margin-top: 16px; justify-content: flex-end"
      />
    </el-card>

    <!-- 发起流程对话框 -->
    <el-dialog v-model="startDialogVisible" title="发起流程" width="600px">
      <el-form :model="startForm" label-width="100px">
        <el-form-item label="流程类型">
          <el-select v-model="startForm.type" placeholder="请选择">
            <el-option label="请假审批" value="leave" />
            <el-option label="报销审批" value="reimbursement" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务数据">
          <el-input 
            v-model="startForm.businessData" 
            type="textarea" 
            :rows="5"
            placeholder="请输入JSON格式的业务数据"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="startDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStart" :loading="starting">
          提交
        </el-button>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog v-model="approveDialogVisible" title="审批" width="500px">
      <el-form :model="approveForm" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="approveForm.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input 
            v-model="approveForm.comment" 
            type="textarea" 
            :rows="4"
            placeholder="请输入审批意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval" :loading="approving">
          提交
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, View, Edit, Close } from '@element-plus/icons-vue'
import axios from 'axios'

// 状态
const loading = ref(false)
const starting = ref(false)
const approving = ref(false)
const instanceList = ref([])
const startDialogVisible = ref(false)
const approveDialogVisible = ref(false)

// 搜索表单
const searchForm = reactive({
  status: undefined
})

// 分页
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})

// 发起流程表单
const startForm = reactive({
  type: 'leave',
  businessData: ''
})

// 审批表单
const approveForm = reactive({
  instanceId: '',
  approved: true,
  comment: ''
})

// 加载流程实例列表
onMounted(() => {
  loadInstances()
})

async function loadInstances() {
  loading.value = true
  try {
    const res = await axios.get('/warm-flow/ins/list', {
      params: {
        pageNum: pagination.pageNum,
        pageSize: pagination.pageSize,
        status: searchForm.status
      }
    })
    
    instanceList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (err) {
    ElMessage.error('加载失败: ' + err.message)
  } finally {
    loading.value = false
  }
}

// 重置搜索
function resetSearch() {
  searchForm.status = undefined
  pagination.pageNum = 1
  loadInstances()
}

// 显示发起流程对话框
function showStartDialog() {
  startDialogVisible.value = true
  startForm.type = 'leave'
  startForm.businessData = ''
}

// 提交发起流程
async function submitStart() {
  if (!startForm.businessData.trim()) {
    ElMessage.warning('请输入业务数据')
    return
  }

  starting.value = true
  try {
    const businessData = JSON.parse(startForm.businessData)
    
    const url = startForm.type === 'leave' 
      ? '/warm-flow/leave/submit' 
      : '/warm-flow/reimbursement/submit'
    
    await axios.post(url, businessData)
    
    ElMessage.success('流程发起成功')
    startDialogVisible.value = false
    loadInstances()
  } catch (err) {
    ElMessage.error('发起失败: ' + err.message)
  } finally {
    starting.value = false
  }
}

// 显示审批对话框
function showApproveDialog(row) {
  approveForm.instanceId = row.id
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

  approving.value = true
  try {
    await axios.post(`/warm-flow/task/approve/${approveForm.instanceId}`, {
      approved: approveForm.approved,
      comment: approveForm.comment
    })
    
    ElMessage.success('审批成功')
    approveDialogVisible.value = false
    loadInstances()
  } catch (err) {
    ElMessage.error('审批失败: ' + err.message)
  } finally {
    approving.value = false
  }
}

// 查看详情
function viewDetail(row) {
  // 在新窗口打开流程设计器查看流程图
  window.open(`/warm-flow-ui?instanceId=${row.id}`, '_blank')
}

// 撤销流程
async function cancelInstance(row) {
  try {
    await ElMessageBox.confirm('确定要撤销该流程吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const url = row.flowName.includes('请假') 
      ? `/warm-flow/leave/cancel/${row.id}` 
      : `/warm-flow/reimbursement/cancel/${row.id}`
    
    await axios.post(url)
    
    ElMessage.success('撤销成功')
    loadInstances()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('撤销失败: ' + err.message)
    }
  }
}

// 获取状态类型
function getStatusType(status) {
  const map = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger',
    4: 'info'
  }
  return map[status] || 'info'
}

// 获取状态文本
function getStatusText(status) {
  const map = {
    0: '草稿',
    1: '审批中',
    2: '已通过',
    3: '已驳回',
    4: '已撤销'
  }
  return map[status] || '未知'
}
</script>

<style scoped>
.process-instance-list {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 16px;
}
</style>
