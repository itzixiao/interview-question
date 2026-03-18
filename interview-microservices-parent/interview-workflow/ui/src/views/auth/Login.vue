<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <el-icon size="36" color="#1890ff">
          <Setting/>
        </el-icon>
        <h2>工作流审批系统</h2>
        <p>基于 Flowable + Spring Boot + Vue 3</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              clearable
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
              v-model="form.password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              type="password"
              show-password
              clearable
          />
        </el-form-item>
        <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-btn"
            block
        >
          登 录
        </el-button>
      </el-form>

      <!-- 演示账号提示 -->
      <el-divider>演示账号</el-divider>
      <el-divider>（密码均为 123456）</el-divider>
      <div class="demo-accounts">
        <el-tag
            v-for="account in demoAccounts"
            :key="account.username"
            class="demo-tag"
            @click="fillAccount(account)"
            style="cursor: pointer;"
            :type="account.type"
        >
          {{ account.label }}：{{ account.username }}
        </el-tag>
      </div>
    </div>
  </div>
</template>

<script setup>
import {reactive, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/store/auth'
import {ElMessage} from 'element-plus'
import {Setting} from "@element-plus/icons-vue";

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}]
}

const demoAccounts = [
  {username: 'emp_chen', label: '普通员工（技术部）', type: 'info'},
  {username: 'emp_liu', label: '普通员工（人事部）', type: 'info'},
  {username: 'tech_manager_li', label: '部门经理', type: 'warning'},
  {username: 'finance_manager_wang', label: '财务经理', type: 'warning'},
  {username: 'gm_zhang', label: '总经理', type: 'danger'},
  {username: 'admin', label: '管理员', type: 'success'}
]

function fillAccount(account) {
  form.username = account.username
  form.password = '123456'
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    console.log('[Login] 开始登录...', form.username)
    await authStore.loginAction(form.username, form.password)
    console.log('[Login] loginAction 完成, token:', authStore.token?.substring(0, 20) + '...')
    ElMessage.success('登录成功')

    // 延迟跳转，确保 pinia state 已更新
    setTimeout(() => {
      const redirect = route.query.redirect || '/dashboard'
      console.log('[Login] 准备跳转到:', redirect)
      router.push(redirect).then(() => {
        console.log('[Login] 跳转成功')
      }).catch(err => {
        console.error('[Login] 跳转失败:', err)
        router.push('/dashboard')
      })
    }, 100)
  } catch (e) {
    console.error('[Login] 登录失败:', e)
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #1890ff 0%, #722ed1 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-card {
  background: white;
  border-radius: 12px;
  padding: 40px;
  width: 420px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h2 {
  margin: 12px 0 6px;
  font-size: 22px;
  color: #333;
}

.login-header p {
  color: #999;
  font-size: 13px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.demo-tag {
  font-size: 12px;
}
</style>
