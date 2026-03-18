<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="header-bar">
          <span class="card-title"><el-icon><List/></el-icon> 我的请假记录</span>
          <el-radio-group v-model="statusFilter" @change="loadList">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="1">审批中</el-radio-button>
            <el-radio-button :label="2">已通过</el-radio-button>
            <el-radio-button :label="3">已拒绝</el-radio-button>
          </el-radio-group>
        </div>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="applyNo" label="申请编号" width="160"/>
        <el-table-column label="假期类型" width="80">
          <template #default="{ row }">{{ leaveTypeText(row.leaveType) }}</template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日期" width="110"/>
        <el-table-column prop="endDate" label="结束日期" width="110"/>
        <el-table-column prop="leaveDays" label="天数" width="60" align="center"/>
        <el-table-column label="审批路径" width="140">
          <template #default="{ row }">
            <el-tag size="small" :type="row.leaveDays > 3 ? 'warning' : 'info'">
              {{ row.leaveDays > 3 ? '总经理审批' : '部门经理审批' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentNode" label="当前节点"/>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" prop="applyTime" width="160"/>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="info" text @click="viewHistory(row)">轨迹</el-button>
            <!-- 当前节点是"申请人修改"时，显示修改重提和撤回按钮 -->
            <template v-if="row.currentNode === '申请人修改'">
              <el-button size="small" type="primary" text @click="openResubmit(row)">
                修改重提
              </el-button>
              <el-button size="small" type="danger" text @click="handleWithdraw(row)">
                撤回
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @change="loadList"
          class="pagination"
      />
    </el-card>

    <!-- 修改重提对话框 -->
    <el-dialog v-model="resubmitVisible" title="修改请假申请" width="500px">
      <el-form ref="resubmitFormRef" :model="resubmitForm" label-width="100px">
        <el-form-item label="假期类型">
          <el-select v-model="resubmitForm.leaveType" style="width: 100%">
            <el-option label="年假" :value="1"/>
            <el-option label="事假" :value="2"/>
            <el-option label="病假" :value="3"/>
            <el-option label="调休" :value="4"/>
          </el-select>
        </el-form-item>
        <el-form-item label="请假日期">
          <el-date-picker v-model="resubmitForm.dateRange" type="daterange" range-separator="至"
                          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
                          style="width: 100%"/>
        </el-form-item>
        <el-form-item label="请假原因">
          <el-input v-model="resubmitForm.reason" type="textarea" :rows="3"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resubmitVisible = false">取消</el-button>
        <el-button type="primary" :loading="resubmitLoading" @click="handleResubmit">重新提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批历史弹窗 -->
    <ApprovalHistoryDialog
        v-model="historyVisible"
        :processInstanceId="historyProcessId"
        :businessLabel="historyLabel"
    />
  </div>
</template>

<script setup>
import {onMounted, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {getMyLeaveList, resubmitLeave, withdrawLeave} from '@/api/workflow'
import ApprovalHistoryDialog from '@/components/ApprovalHistoryDialog.vue'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref(null)

const resubmitVisible = ref(false)
const resubmitLoading = ref(false)
const resubmitFormRef = ref(null)
const currentLeaveId = ref(null)
const resubmitForm = ref({leaveType: null, dateRange: [], reason: ''})

const historyVisible = ref(false)
const historyProcessId = ref('')
const historyLabel = ref('')

const leaveTypeText = t => ({1: '年假', 2: '事假', 3: '病假', 4: '调休'}[t] || '未知')
const statusText = s => ({0: '草稿', 1: '审批中', 2: '已通过', 3: '已拒绝', 4: '已撤回'}[s] || '未知')
const statusType = s => ({0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'info'}[s] || 'info')

async function loadList() {
  loading.value = true
  try {
    const res = await getMyLeaveList({pageNum: pageNum.value, pageSize: pageSize.value, status: statusFilter.value})
    list.value = res.data.records || []
    total.value = res.data.total || 0
    console.log('=== 我的请假列表 ===', '共', total.value, '条')
    list.value.forEach(item => {
      console.log(`  [${item.applyNo}] ${item.leaveDays}天 | 状态: ${statusText(item.status)} | 当前节点: ${item.currentNode}`)
    })
  } finally {
    loading.value = false
  }
}

function viewHistory(row) {
  historyProcessId.value = row.processInstanceId || ''
  historyLabel.value = row.applyNo
  historyVisible.value = true
  console.log('查看审批轨迹:', row.applyNo, '| 流程实例ID:', row.processInstanceId)
}

function openResubmit(row) {
  currentLeaveId.value = row.id
  resubmitForm.value = {
    leaveType: row.leaveType,
    dateRange: [row.startDate, row.endDate],
    reason: row.reason
  }
  resubmitVisible.value = true
  console.log('打开重提对话框:', row.applyNo, '| 原天数:', row.leaveDays)
}

async function handleResubmit() {
  resubmitLoading.value = true
  try {
    const payload = {
      leaveType: resubmitForm.value.leaveType,
      startDate: resubmitForm.value.dateRange[0],
      endDate: resubmitForm.value.dateRange[1],
      reason: resubmitForm.value.reason
    }
    console.log('重新提交请假:', payload)
    await resubmitLeave(currentLeaveId.value, payload)
    ElMessage.success('重新提交成功，等待审批')
    resubmitVisible.value = false
    loadList()
  } finally {
    resubmitLoading.value = false
  }
}

async function handleWithdraw(row) {
  await ElMessageBox.confirm(`确认撤回请假申请 ${row.applyNo}？`, '提示', {type: 'warning'})
  console.log('撤回请假申请:', row.applyNo, '| 申请ID:', row.id)
  await withdrawLeave(row.id)
  ElMessage.success('撤回成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

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

