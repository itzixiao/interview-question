<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span class="card-title"><el-icon><Bell/></el-icon> 待我审批的报销</span>
      </template>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="applyNo" label="申请编号" width="160"/>
        <el-table-column prop="applicantName" label="申请人" width="90"/>
        <el-table-column prop="deptName" label="部门" width="100"/>
        <el-table-column label="报销类型" width="90">
          <template #default="{ row }">{{ expenseTypeText(row.expenseType) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额（元）" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">¥ {{ row.amount?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批级别" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="amountPathType(row.amount)">{{ amountPathText(row.amount) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="报销说明" show-overflow-tooltip/>
        <el-table-column prop="currentNode" label="当前节点" width="120"/>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="openApproval(row, true)">通过</el-button>
            <el-button size="small" type="danger" @click="openApproval(row, false)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination v-model:current-page="pageNum" :total="total"
                     layout="total, prev, pager, next" @current-change="loadList" class="pagination"/>
    </el-card>

    <el-dialog v-model="approvalVisible" :title="approvalAction ? '确认通过' : '确认驳回'" width="480px">
      <el-descriptions v-if="currentRow" :column="2" border size="small" style="margin-bottom: 16px;">
        <el-descriptions-item label="申请人">{{ currentRow.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="报销金额">
          <span class="amount">¥ {{ currentRow.amount?.toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="审批路径">{{ amountPathText(currentRow.amount) }}</el-descriptions-item>
        <el-descriptions-item label="当前节点">{{ currentRow.currentNode }}</el-descriptions-item>
      </el-descriptions>
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
import {ElMessage} from 'element-plus'
import {approveExpense, getPendingExpenseList} from '@/api/workflow'
import {Bell} from "@element-plus/icons-vue";

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)

const approvalVisible = ref(false)
const approvalLoading = ref(false)
const approvalComment = ref('')
const approvalAction = ref(true)
const currentTaskId = ref('')
const currentRow = ref(null)

const expenseTypeText = t => ({1: '差旅', 2: '招待', 3: '办公', 4: '培训', 5: '其他'}[t] || '未知')

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
    const res = await getPendingExpenseList({pageNum: pageNum.value, pageSize: 10})
    list.value = res.data.records || []
    total.value = res.data.total || 0
    console.log('=== 待审批报销列表 ===', '共', total.value, '条待处理')
  } finally {
    loading.value = false
  }
}

function openApproval(row, action) {
  currentRow.value = row
  approvalAction.value = action
  approvalComment.value = ''
  currentTaskId.value = row.taskId || ''
  approvalVisible.value = true
  console.log('报销待审批:', row.applyNo, '| 金额:', row.amount, '| 操作:', action ? '通过' : '驳回')
}

async function handleApprove() {
  if (!currentTaskId.value) {
    ElMessage.warning('任务ID不存在，请刷新后重试')
    return
  }
  approvalLoading.value = true
  try {
    await approveExpense({
      taskId: currentTaskId.value,
      approved: approvalAction.value,
      comment: approvalComment.value
    })
    ElMessage.success(approvalAction.value ? '审批通过成功' : '已驳回申请')
    approvalVisible.value = false
    console.log('=== 报销审批完成 ===', '金额:', currentRow.value?.amount, '| 结果:', approvalAction.value ? '通过' : '驳回')
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

.amount {
  color: #f5222d;
  font-weight: 500;
}
</style>
