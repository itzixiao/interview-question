<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div class="header-bar">
          <span class="card-title"><el-icon><Document/></el-icon> 我的报销记录</span>
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
        <el-table-column label="报销类型" width="90">
          <template #default="{ row }">{{ expenseTypeText(row.expenseType) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额（元）" width="110" align="right">
          <template #default="{ row }">
            <span class="amount">¥ {{ row.amount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批路径" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="amountPathType(row.amount)">{{ amountPathText(row.amount) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentNode" label="当前节点"/>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="applyTime" label="申请时间" width="160"/>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="info" text @click="viewHistory(row)">轨迹</el-button>
            <!-- 当前节点是"申请人修改"时，显示修改重提和撤回按钮 -->
            <template v-if="row.currentNode === '申请人修改'">
              <el-button size="small" type="primary" text @click="openResubmit(row)">修改重提</el-button>
              <el-button size="small" type="danger" text @click="handleWithdraw(row)">撤回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pageNum" :total="total"
                     layout="total, prev, pager, next" @current-change="loadList" class="pagination"/>
    </el-card>

    <el-dialog v-model="resubmitVisible" title="修改报销申请" width="480px">
      <el-form :model="resubmitForm" label-width="100px">
        <el-form-item label="报销类型">
          <el-select v-model="resubmitForm.expenseType" style="width: 100%">
            <el-option label="差旅费" :value="1"/>
            <el-option label="招待费" :value="2"/>
            <el-option label="办公费" :value="3"/>
            <el-option label="培训费" :value="4"/>
            <el-option label="其他" :value="5"/>
          </el-select>
        </el-form-item>
        <el-form-item label="报销金额（元）">
          <el-input-number v-model="resubmitForm.amount" :min="0.01" :precision="2" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="报销说明">
          <el-input v-model="resubmitForm.description" type="textarea" :rows="3"/>
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
import {getMyExpenseList, resubmitExpense, withdrawExpense} from '@/api/workflow'
import ApprovalHistoryDialog from '@/components/ApprovalHistoryDialog.vue'
import {Document} from "@element-plus/icons-vue";

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const statusFilter = ref(null)

const resubmitVisible = ref(false)
const resubmitLoading = ref(false)
const currentExpenseId = ref(null)
const resubmitForm = ref({expenseType: null, amount: null, description: ''})

const historyVisible = ref(false)
const historyProcessId = ref('')
const historyLabel = ref('')

const expenseTypeText = t => ({1: '差旅', 2: '招待', 3: '办公', 4: '培训', 5: '其他'}[t] || '未知')
const statusText = s => ({0: '草稿', 1: '审批中', 2: '已通过', 3: '已拒绝', 4: '已撤回'}[s] || '未知')
const statusType = s => ({0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'info'}[s] || 'info')

function amountPathText(amount) {
  if (!amount) return '-'
  if (amount < 1000) return '部门经理审批'
  if (amount <= 5000) return '财务经理审批'
  return '总经理审批'
}

function amountPathType(amount) {
  if (!amount) return 'info'
  if (amount < 1000) return 'success'
  if (amount <= 5000) return 'warning'
  return 'danger'
}

async function loadList() {
  loading.value = true
  try {
    const res = await getMyExpenseList({pageNum: pageNum.value, pageSize: 10, status: statusFilter.value})
    list.value = res.data.records || []
    total.value = res.data.total || 0
    console.log('=== 我的报销列表 ===', '共', total.value, '条')
    list.value.forEach(item => {
      console.log(`  [${item.applyNo}] ¥${item.amount} | 状态: ${statusText(item.status)} | 当前节点: ${item.currentNode}`)
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
  currentExpenseId.value = row.id
  resubmitForm.value = {expenseType: row.expenseType, amount: row.amount, description: row.description}
  resubmitVisible.value = true
  console.log('打开报销重提:', row.applyNo, '| 原金额:', row.amount)
}

async function handleResubmit() {
  resubmitLoading.value = true
  try {
    console.log('报销重新提交:', resubmitForm.value)
    await resubmitExpense(currentExpenseId.value, resubmitForm.value)
    ElMessage.success('重新提交成功，等待审批')
    resubmitVisible.value = false
    loadList()
  } finally {
    resubmitLoading.value = false
  }
}

async function handleWithdraw(row) {
  await ElMessageBox.confirm(`确认撤回报销申请 ${row.applyNo}？`, '提示', {type: 'warning'})
  console.log('撤回报销申请:', row.applyNo, '| 申请ID:', row.id)
  await withdrawExpense(row.id)
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

.amount {
  color: #f5222d;
  font-weight: 500;
}
</style>
