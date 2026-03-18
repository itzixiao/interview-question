<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span class="card-title">
          <el-icon><Calendar/></el-icon> 提交请假申请
        </span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 600px;">
        <el-form-item label="假期类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="请选择假期类型" style="width: 100%">
            <el-option label="年假" :value="1"/>
            <el-option label="事假" :value="2"/>
            <el-option label="病假" :value="3"/>
            <el-option label="调休" :value="4"/>
          </el-select>
        </el-form-item>

        <el-form-item label="请假日期" prop="dateRange">
          <el-date-picker
              v-model="form.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              style="width: 100%"
              @change="onDateChange"
          />
        </el-form-item>

        <el-form-item label="请假天数">
          <el-tag v-if="form.leaveDays > 0" :type="form.leaveDays <= 3 ? 'success' : 'warning'" size="large">
            {{ form.leaveDays }} 天
            <span style="margin-left: 8px; font-size: 12px;">
              （{{ form.leaveDays <= 3 ? '仅需部门经理审批' : '需总经理审批' }}）
            </span>
          </el-tag>
          <span v-else class="hint">选择日期后自动计算</span>
        </el-form-item>

        <el-form-item label="请假原因" prop="reason">
          <el-input
              v-model="form.reason"
              type="textarea"
              :rows="4"
              placeholder="请详细描述请假原因..."
              maxlength="500"
              show-word-limit
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit" size="large">
            <el-icon>
              <Check/>
            </el-icon>
            提交申请
          </el-button>
          <el-button @click="resetForm" size="large">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 审批流程说明 -->
      <el-divider/>
      <el-alert title="审批流程说明" type="info" :closable="false" show-icon>
        <template #default>
          <ul class="process-desc">
            <li>请假 <strong>≤ 3天</strong>：提交 → 部门经理审批 → 完成</li>
            <li>请假 <strong>&gt; 3天</strong>：提交 → 部门经理审批 → 总经理审批 → 完成</li>
            <li>若被驳回，可修改后重新提交，或直接撤回申请</li>
          </ul>
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {applyLeave} from '@/api/workflow'
import dayjs from 'dayjs'
import {Calendar, Check} from "@element-plus/icons-vue";

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  leaveType: null,
  dateRange: [],
  leaveDays: 0,
  reason: ''
})

const rules = {
  leaveType: [{required: true, message: '请选择假期类型'}],
  dateRange: [{required: true, message: '请选择请假日期'}],
  reason: [{required: true, message: '请填写请假原因', trigger: 'blur'}]
}

function onDateChange(val) {
  if (val && val.length === 2) {
    const start = dayjs(val[0])
    const end = dayjs(val[1])
    form.leaveDays = end.diff(start, 'day') + 1
    console.log('请假天数计算:', form.leaveDays, '天', form.leaveDays > 3 ? '→ 需总经理审批' : '→ 部门经理审批即可')
  } else {
    form.leaveDays = 0
  }
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const payload = {
      leaveType: form.leaveType,
      startDate: form.dateRange[0],
      endDate: form.dateRange[1],
      reason: form.reason
    }
    const res = await applyLeave(payload)
    ElMessage.success('请假申请提交成功！')
    console.log('=== 请假申请提交成功 ===')
    console.log('申请ID:', res.data, '| 天数:', form.leaveDays, '| 审批路径:', form.leaveDays <= 3 ? '部门经理' : '部门经理→总经理')
    router.push('/leave/my-list')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formRef.value.resetFields()
  form.leaveDays = 0
}
</script>

<style scoped>
.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: 600;
}

.hint {
  color: #999;
  font-size: 13px;
}

.process-desc {
  margin: 0;
  padding-left: 20px;
  line-height: 1.8;
  font-size: 13px;
}
</style>
