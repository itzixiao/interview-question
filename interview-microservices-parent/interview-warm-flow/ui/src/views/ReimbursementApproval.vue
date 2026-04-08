<template>
  <div class="reimbursement-container">
    <el-page-header @back="goBack" content="报销审批测试" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 提交报销申请 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>💰 提交报销申请</span>
            </div>
          </template>

          <el-form :model="reimbursementForm" label-width="100px">
            <el-form-item label="申请人ID">
              <el-input-number v-model="reimbursementForm.userId" :min="1000" :max="9999" />
            </el-form-item>

            <el-form-item label="申请人姓名">
              <el-input v-model="reimbursementForm.userName" placeholder="请输入姓名" />
            </el-form-item>

            <el-form-item label="报销类型">
              <el-select v-model="reimbursementForm.reimbursementType" placeholder="请选择">
                <el-option label="差旅费" :value="1" />
                <el-option label="交通费" :value="2" />
                <el-option label="餐饮费" :value="3" />
                <el-option label="办公用品" :value="4" />
                <el-option label="其他" :value="5" />
              </el-select>
            </el-form-item>

            <el-form-item label="报销金额">
              <el-input-number v-model="reimbursementForm.amount" :min="0" :precision="2" :step="100" />
            </el-form-item>

            <el-form-item label="报销事由">
              <el-input v-model="reimbursementForm.reason" type="textarea" :rows="3" />
            </el-form-item>

            <el-form-item label="发票附件">
              <el-input v-model="reimbursementForm.attachmentUrls" placeholder="附件URL（可选）" />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
                提交申请
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 审批操作 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>✅ 审批操作</span>
            </div>
          </template>

          <el-form :model="approveForm" label-width="120px">
            <el-form-item label="流程实例ID">
              <el-input v-model="approveForm.flowInstanceId" placeholder="请输入流程实例ID" />
            </el-form-item>

            <el-form-item label="审批人ID">
              <el-input-number v-model="approveForm.approverId" :min="2000" :max="9999" />
            </el-form-item>

            <el-form-item label="审批结果">
              <el-radio-group v-model="approveForm.approved">
                <el-radio :label="true">通过</el-radio>
                <el-radio :label="false">驳回</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="审批意见">
              <el-input v-model="approveForm.comment" type="textarea" :rows="3" />
            </el-form-item>

            <el-form-item>
              <el-button type="success" @click="handleApprove" :loading="approveLoading">
                提交审批
              </el-button>
            </el-form-item>
          </el-form>

          <el-divider />

          <el-form :model="cancelForm" label-width="120px">
            <el-form-item label="流程实例ID">
              <el-input v-model="cancelForm.flowInstanceId" placeholder="请输入流程实例ID" />
            </el-form-item>

            <el-form-item label="申请人ID">
              <el-input-number v-model="cancelForm.userId" :min="1000" :max="9999" />
            </el-form-item>

            <el-form-item>
              <el-button type="warning" @click="handleCancel" :loading="cancelLoading">
                撤销申请
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 查询详情 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>🔍 查询申请详情</span>
        </div>
      </template>

      <el-form :inline="true">
        <el-form-item label="流程实例ID">
          <el-input v-model="detailFlowId" placeholder="请输入流程实例ID" style="width: 300px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQueryDetail">查询</el-button>
        </el-form-item>
      </el-form>

      <el-descriptions v-if="detailData" :column="2" border>
        <el-descriptions-item label="申请人">{{ detailData.reimbursementRequest?.userName }}</el-descriptions-item>
        <el-descriptions-item label="报销类型">{{ getReimbursementTypeText(detailData.reimbursementRequest?.reimbursementType) }}</el-descriptions-item>
        <el-descriptions-item label="报销金额">¥{{ detailData.reimbursementRequest?.amount }}</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ getStatusText(detailData.reimbursementRequest?.status) }}</el-descriptions-item>
        <el-descriptions-item label="报销事由" :span="2">{{ detailData.reimbursementRequest?.reason }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ detailData.reimbursementRequest?.approvalComment || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {submitReimbursement, approveReimbursement, cancelReimbursement, getReimbursementDetail} from '@/api/reimbursement'

const router = useRouter()

const goBack = () => {
  router.push('/')
}

// 提交表单
const reimbursementForm = reactive({
  userId: 1001,
  userName: '张三',
  reimbursementType: 1,
  amount: 2500.00,
  reason: '北京出差差旅费',
  attachmentUrls: ''
})

const submitLoading = ref(false)
const handleSubmit = async () => {
  try {
    submitLoading.value = true
    const res = await submitReimbursement(reimbursementForm)
    ElMessage.success('提交成功！流程实例ID: ' + res.flowInstanceId)
    approveForm.flowInstanceId = res.flowInstanceId
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 审批表单
const approveForm = reactive({
  flowInstanceId: '',
  approverId: 2001,
  approved: true,
  comment: '同意'
})

const approveLoading = ref(false)
const handleApprove = async () => {
  if (!approveForm.flowInstanceId) {
    ElMessage.warning('请输入流程实例ID')
    return
  }
  try {
    approveLoading.value = true
    await approveReimbursement(approveForm)
    ElMessage.success('审批成功！')
  } catch (error) {
    console.error('审批失败:', error)
  } finally {
    approveLoading.value = false
  }
}

// 撤销表单
const cancelForm = reactive({
  flowInstanceId: '',
  userId: 1001
})

const cancelLoading = ref(false)
const handleCancel = async () => {
  if (!cancelForm.flowInstanceId) {
    ElMessage.warning('请输入流程实例ID')
    return
  }
  try {
    cancelLoading.value = true
    await cancelReimbursement(cancelForm)
    ElMessage.success('撤销成功！')
  } catch (error) {
    console.error('撤销失败:', error)
  } finally {
    cancelLoading.value = false
  }
}

// 查询详情
const detailFlowId = ref('')
const detailData = ref(null)
const handleQueryDetail = async () => {
  if (!detailFlowId.value) {
    ElMessage.warning('请输入流程实例ID')
    return
  }
  try {
    const res = await getReimbursementDetail({flowInstanceId: detailFlowId.value})
    detailData.value = res.data
    ElMessage.success('查询成功！')
  } catch (error) {
    console.error('查询失败:', error)
  }
}

// 工具函数
const getReimbursementTypeText = (type) => {
  const map = {1: '差旅费', 2: '交通费', 3: '餐饮费', 4: '办公用品', 5: '其他'}
  return map[type] || '-'
}

const getStatusText = (status) => {
  const map = {0: '草稿', 1: '审批中', 2: '已通过', 3: '已驳回', 4: '已撤销'}
  return map[status] || '-'
}
</script>

<style scoped>
.reimbursement-container {
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 20px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}
</style>
