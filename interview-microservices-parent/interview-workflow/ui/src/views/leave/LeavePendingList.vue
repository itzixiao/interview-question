<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span class="card-title"><el-icon><Clock/></el-icon> 待我审批的请假</span>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="applyNo" label="申请编号" width="160"/>
        <el-table-column prop="applicantName" label="申请人" width="100"/>
        <el-table-column prop="deptName" label="部门" width="100"/>
        <el-table-column label="假期类型" width="80">
          <template #default="{ row }">{{ leaveTypeText(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="110"/>
        <el-table-column prop="endDate" label="结束日期" width="110"/>
        <el-table-column prop="leaveDays" label="天数" width="60" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.leaveDays > 3 ? 'warning' : 'info'">{{ row.leaveDays }}天</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="请假原因" show-overflow-tooltip/>
        <el-table-column prop="currentNode" label="当前节点" width="130"/>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openApproval(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="openApproval(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pageNum" :total="total" layout="total, prev, pager, next"
                     @current-change="loadList" class="pagination"/>
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog v-model="approvalVisible" :title="approvalAction ? '审批通过确认' : '审批驳回确认'" width="480px">
      <el-alert :type="approvalAction ? 'success' : 'warning'"
                :title="approvalAction ? '确认审批通过该请假申请？' : '确认驳回该请假申请？申请人可修改后重新提交'"
                :closable="false" show-icon style="margin-bottom: 16px;"/>
      <el-form>
        <el-form-item label="审批意见（选填）">
          <el-input v-model="approvalComment" type="textarea" :rows="3" placeholder="填写审批意见..."/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button :type="approvalAction ? 'success' : 'danger'" :loading="approvalLoading" @click="handleApprove">
          确认{{ approvalAction ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {Clock} from '@element-plus/icons-vue'
import {ElMessage} from 'element-plus'
import {approveLeave, getPendingLeaveList} from '@/api/workflow'
import {useAuthStore} from '@/store/auth'

const authStore = useAuthStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)

// 审批
const approvalVisible = ref(false)
const approvalLoading = ref(false)
const approvalComment = ref('')
const approvalAction = ref(true)
const currentTaskId = ref('')
const currentRow = ref(null)

const leaveTypeText = t => ({1: '年假', 2: '事假', 3: '病假', 4: '调休'}[t] || '未知')

async function loadList() {
  loading.value = true
  try {
    const res = await getPendingLeaveList({pageNum: pageNum.value, pageSize: 10})
    list.value = res.data.records || []
    total.value = res.data.total || 0
    console.log('=== 待审批请假列表 ===', '共', total.value, '条待处理')
  } finally {
    loading.value = false
  }
}

function openApproval(row, action) {
  currentRow.value = row
  approvalAction.value = action
  approvalComment.value = ''
  // 注意：taskId 需要从 Flowable 任务中获取
  // 实际场景中可在后端 pending-list 接口中同时返回 taskId
  currentTaskId.value = row.taskId || ''
  approvalVisible.value = true
  console.log('待审批任务:', row.applyNo, '| 操作:', action ? '通过' : '驳回', '| 当前节点:', row.currentNode)
}

async function handleApprove() {
  if (!currentTaskId.value) {
    ElMessage.warning('任务ID不存在，请刷新后重试')
    return
  }
  approvalLoading.value = true
  try {
    await approveLeave({
      taskId: currentTaskId.value,
      approved: approvalAction.value,
      comment: approvalComment.value
    })
    ElMessage.success(approvalAction.value ? '审批通过成功' : '已驳回申请')
    approvalVisible.value = false
    console.log('=== 审批完成 ===', '结果:', approvalAction.value ? '通过' : '驳回')
    loadList()
  } finally {
    approvalLoading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
