<template>
  <div class="dashboard">
    <div class="welcome-banner">
      <el-icon size="32" color="#1890ff">
        <User/>
      </el-icon>
      <div>
        <h2>欢迎回来，{{ authStore.userInfo?.realName }}</h2>
        <p>角色：{{ roleLabel }}</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <el-icon :size="36" :style="{ color: stat.color }">
              <component :is="stat.icon"/>
            </el-icon>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近申请 -->
    <el-row :gutter="16" class="recent-row">
      <el-col :span="12">
        <el-card header="最近请假申请" shadow="never">
          <el-table :data="recentLeaves" size="small" empty-text="暂无数据">
            <el-table-column prop="applyNo" label="申请编号" width="160"/>
            <el-table-column prop="leaveDays" label="天数" width="60"/>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前节点">
              <template #default="{ row }">
                <span class="node-text">{{ row.currentNode || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="more-link">
            <router-link to="/leave/my-list">查看全部 →</router-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="最近报销申请" shadow="never">
          <el-table :data="recentExpenses" size="small" empty-text="暂无数据">
            <el-table-column prop="applyNo" label="申请编号" width="160"/>
            <el-table-column prop="amount" label="金额(元)" width="90">
              <template #default="{ row }">
                <span class="amount">{{ row.amount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="当前节点">
              <template #default="{ row }">
                <span class="node-text">{{ row.currentNode || '-' }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="more-link">
            <router-link to="/expense/my-list">查看全部 →</router-link>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useAuthStore} from '@/store/auth'
import {getDashboardStats, getMyExpenseList, getMyLeaveList} from '@/api/workflow'
import {User} from "@element-plus/icons-vue";

const authStore = useAuthStore()
const recentLeaves = ref([])
const recentExpenses = ref([])
const statsLoading = ref(false)

const roleMap = {
  ROLE_ADMIN: '系统管理员',
  ROLE_GENERAL_MANAGER: '总经理',
  ROLE_DEPT_MANAGER: '部门经理',
  ROLE_FINANCE_MANAGER: '财务经理',
  ROLE_EMPLOYEE: '普通员工'
}
const roleLabel = computed(() =>
    authStore.roles?.map(r => roleMap[r] || r).join(' | ') || '未知'
)

const stats = ref([
  {label: '我的请假申请', value: 0, icon: 'Calendar', color: '#1890ff'},
  {label: '我的报销申请', value: 0, icon: 'Wallet', color: '#52c41a'},
  {label: '待我审批请假', value: 0, icon: 'Clock', color: '#fa8c16'},
  {label: '待我审批报销', value: 0, icon: 'Bell', color: '#f5222d'}
])

function statusText(status) {
  const map = {0: '草稿', 1: '审批中', 2: '已通过', 3: '已拒绝', 4: '已撤回'}
  return map[status] || '未知'
}

function statusType(status) {
  const map = {0: 'info', 1: 'warning', 2: 'success', 3: 'danger', 4: 'info'}
  return map[status] || 'info'
}

onMounted(async () => {
  statsLoading.value = true
  try {
    // 并行请求：仪表盘统计 + 最近5条记录
    const [statsRes, leaveRes, expenseRes] = await Promise.all([
      getDashboardStats(),
      getMyLeaveList({pageNum: 1, pageSize: 5}),
      getMyExpenseList({pageNum: 1, pageSize: 5})
    ])

    // 接入真实统计数据
    const s = statsRes.data
    stats.value[0].value = s.myLeaveTotal || 0
    stats.value[1].value = s.myExpenseTotal || 0
    stats.value[2].value = s.pendingLeaveCount || 0
    stats.value[3].value = s.pendingExpenseCount || 0

    recentLeaves.value = leaveRes.data?.records || []
    recentExpenses.value = expenseRes.data?.records || []

    console.log('=== 工作台统计数据（真实接口）===')
    console.log('我的请假总数:', s.myLeaveTotal, '| 审批中:', s.myLeaveInProgress)
    console.log('我的报销总数:', s.myExpenseTotal, '| 审批中:', s.myExpenseInProgress)
    console.log('待我审批请假:', s.pendingLeaveCount, '| 待我审批报销:', s.pendingExpenseCount)
    console.log('待审批总任务:', s.pendingApprovalCount)
  } catch (e) {
    console.error('工作台数据加载失败:', e)
  } finally {
    statsLoading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.welcome-banner {
  background: linear-gradient(135deg, #1890ff, #722ed1);
  color: white;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.welcome-banner h2 {
  margin: 0 0 4px;
  font-size: 20px;
}

.welcome-banner p {
  margin: 0;
  opacity: 0.8;
  font-size: 13px;
}

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-info .stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
}

.stat-info .stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.recent-row {
}

.more-link {
  text-align: right;
  margin-top: 12px;
}

.more-link a {
  color: #1890ff;
  text-decoration: none;
  font-size: 13px;
}

.node-text {
  color: #666;
  font-size: 12px;
}

.amount {
  color: #f5222d;
  font-weight: 500;
}
</style>
