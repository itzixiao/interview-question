<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <span class="card-title"><el-icon><Wallet/></el-icon> 提交报销申请</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 600px;">
        <el-form-item label="报销类型" prop="expenseType">
          <el-select v-model="form.expenseType" placeholder="请选择报销类型" style="width: 100%">
            <el-option label="差旅费" :value="1"/>
            <el-option label="招待费" :value="2"/>
            <el-option label="办公费" :value="3"/>
            <el-option label="培训费" :value="4"/>
            <el-option label="其他" :value="5"/>
          </el-select>
        </el-form-item>

        <el-form-item label="报销金额（元）" prop="amount">
          <el-input-number
              v-model="form.amount"
              :min="0.01" :precision="2" :step="100"
              style="width: 100%"
              @change="onAmountChange"
          />
        </el-form-item>

        <el-form-item label="审批路径">
          <el-tag v-if="form.amount" :type="approvalPathType" size="large">
            {{ approvalPathText }}
          </el-tag>
          <span v-else class="hint">填写金额后自动显示审批路径</span>
        </el-form-item>

        <el-form-item label="报销说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4"
                    placeholder="请详细描述报销事项..." maxlength="500" show-word-limit/>
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

      <el-divider/>
      <el-alert title="报销审批路径说明" type="info" :closable="false" show-icon>
        <template #default>
          <ul class="process-desc">
            <li>金额 <strong>&lt; 1,000 元</strong>：部门经理审批</li>
            <li>金额 <strong>1,000 ~ 5,000 元</strong>：财务经理审批</li>
            <li>金额 <strong>&gt; 5,000 元</strong>：总经理审批</li>
            <li>被驳回后可修改金额重新提交，审批路径将自动重新判断</li>
          </ul>
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import {computed, reactive, ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {applyExpense} from '@/api/workflow'
import {Check, Wallet} from "@element-plus/icons-vue";

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({expenseType: null, amount: null, description: ''})

const rules = {
  expenseType: [{required: true, message: '请选择报销类型'}],
  amount: [{required: true, message: '请输入报销金额'}],
  description: [{required: true, message: '请填写报销说明', trigger: 'blur'}]
}

const approvalPathText = computed(() => {
  if (!form.amount) return ''
  if (form.amount < 1000) return '部门经理审批'
  if (form.amount <= 5000) return '财务经理审批'
  return '总经理审批'
})

const approvalPathType = computed(() => {
  if (!form.amount) return 'info'
  if (form.amount < 1000) return 'success'
  if (form.amount <= 5000) return 'warning'
  return 'danger'
})

function onAmountChange(val) {
  console.log('报销金额:', val, '→ 审批路径:', approvalPathText.value)
}

async function handleSubmit() {
  await formRef.value.validate()
  loading.value = true

  // 构建请求数据
  const payload = {
    expenseType: form.expenseType,
    amount: form.amount,
    description: form.description
  }

  console.log('[ExpenseApply] 提交报销申请:', payload)
  console.log('[ExpenseApply] expenseType类型:', typeof payload.expenseType, '值:', payload.expenseType)
  console.log('[ExpenseApply] amount类型:', typeof payload.amount, '值:', payload.amount)
  console.log('[ExpenseApply] description类型:', typeof payload.description, '值:', payload.description)

  try {
    const res = await applyExpense(payload)
    ElMessage.success('报销申请提交成功！')
    console.log('=== 报销申请提交成功 ===')
    console.log('申请ID:', res.data, '| 金额:', form.amount, '元 | 审批路径:', approvalPathText.value)
    router.push('/expense/my-list')
  } catch (e) {
    console.error('[ExpenseApply] 提交失败:', e)
    console.error('[ExpenseApply] 错误响应:', e.response?.data)
    ElMessage.error(e.response?.data?.message || '提交失败，请检查表单')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  formRef.value.resetFields()
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
