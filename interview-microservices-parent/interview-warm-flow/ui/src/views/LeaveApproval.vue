<template>
  <div class="leave-container">
    <el-page-header @back="goBack" content="请假审批测试" />

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 提交请假申请 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>📝 提交请假申请</span>
            </div>
          </template>

          <el-form :model="leaveForm" label-width="100px">
            <el-form-item label="申请人ID">
              <el-input-number v-model="leaveForm.userId" :min="1000" :max="9999" />
            </el-form-item>

            <el-form-item label="申请人姓名">
              <el-input v-model="leaveForm.userName" placeholder="请输入姓名" />
            </el-form-item>

            <el-form-item label="请假类型">
              <el-select v-model="leaveForm.leaveType" placeholder="请选择">
                <el-option label="事假" :value="1" />
                <el-option label="病假" :value="2" />
                <el-option label="年假" :value="3" />
                <el-option label="婚假" :value="4" />
                <el-option label="产假" :value="5" />
              </el-select>
            </el-form-item>

            <el-form-item label="开始时间">
              <el-date-picker
                  v-model="leaveForm.startTime"
                  type="datetime"
                  placeholder="选择开始时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>

            <el-form-item label="结束时间">
              <el-date-picker
                  v-model="leaveForm.endTime"
                  type="datetime"
                  placeholder="选择结束时间"
                  value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>

            <el-form-item label="请假天数">
              <el-input-number v-model="leaveForm.days" :min="0.5" :step="0.5" />
            </el-form-item>

            <el-form-item label="请假原因">
              <el-input v-model="leaveForm.reason" type="textarea" :rows="3" />
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
        <el-descriptions-item label="申请人">{{ detailData.leaveRequest?.userName }}</el-descriptions-item>
        <el-descriptions-item label="请假类型">{{ getLeaveTypeText(detailData.leaveRequest?.leaveType) }}</el-descriptions-item>
        <el-descriptions-item label="请假天数">{{ detailData.leaveRequest?.days }}天</el-descriptions-item>
        <el-descriptions-item label="审批状态">{{ getStatusText(detailData.leaveRequest?.status) }}</el-descriptions-item>
        <el-descriptions-item label="开始时间" :span="2">{{ detailData.leaveRequest?.startTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间" :span="2">{{ detailData.leaveRequest?.endTime }}</el-descriptions-item>
        <el-descriptions-item label="请假原因" :span="2">{{ detailData.leaveRequest?.reason }}</el-descriptions-item>
        <el-descriptions-item label="审批意见" :span="2">{{ detailData.leaveRequest?.approvalComment || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import {ref, reactive} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {submitLeave, approveLeave, cancelLeave, getLeaveDetail} from '@/api/leave'
import dayjs from 'dayjs'

const router = useRouter()

const goBack = () => {
  router.push('/')
}

// 提交表单
const leaveForm = reactive({
  userId: 1001,
  userName: '张三',
  leaveType: 3,
  startTime: dayjs().add(1, 'day').format('YYYY-MM-DD 09:00:00'),
  endTime: dayjs().add(3, 'day').format('YYYY-MM-DD 18:00:00'),
  days: 3,
  reason: '年假休息'
})

const submitLoading = ref(false)
const handleSubmit = async () => {
  try {
    submitLoading.value = true
    const res = await submitLeave(leaveForm)
    ElMessage.success('提交成功！流程实例ID: ' + res.flowInstanceId)
    // 自动填充到审批表单
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
    await approveLeave(approveForm)
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
    await cancelLeave(cancelForm)
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
    const res = await getLeaveDetail({flowInstanceId: detailFlowId.value})
    detailData.value = res.data
    ElMessage.success('查询成功！')
  } catch (error) {
    console.error('查询失败:', error)
  }
}

// 工具函数
const getLeaveTypeText = (type) => {
  const map = {1: '事假', 2: '病假', 3: '年假', 4: '婚假', 5: '产假'}
  return map[type] || '-'
}

const getStatusText = (status) => {
  const map = {0: '草稿', 1: '审批中', 2: '已通过', 3: '已驳回', 4: '已撤销'}
  return map[status] || '-'
}
</script>

<style scoped>
.leave-container {
  max-width: 1400px;
  margin: 20px auto;
  padding: 0 20px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
}
</style>
