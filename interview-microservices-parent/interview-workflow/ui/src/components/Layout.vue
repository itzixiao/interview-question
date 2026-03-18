<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <el-icon size="24" color="#fff">
          <Setting/>
        </el-icon>
        <span v-if="!isCollapse" class="logo-text">审批系统</span>
      </div>
      <el-menu
          :default-active="currentRoute"
          :collapse="isCollapse"
          background-color="#001529"
          text-color="#a6adb4"
          active-text-color="#1890ff"
          router
      >
        <el-menu-item index="/dashboard">
          <el-icon>
            <HomeFilled/>
          </el-icon>
          <template #title>工作台</template>
        </el-menu-item>

        <el-sub-menu index="leave">
          <template #title>
            <el-icon>
              <Calendar/>
            </el-icon>
            <span>请假管理</span>
          </template>
          <el-menu-item index="/leave/apply">提交请假申请</el-menu-item>
          <el-menu-item index="/leave/my-list">我的请假记录</el-menu-item>
          <el-menu-item index="/leave/pending">待我审批</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="expense">
          <template #title>
            <el-icon>
              <Wallet/>
            </el-icon>
            <span>报销管理</span>
          </template>
          <el-menu-item index="/expense/apply">提交报销申请</el-menu-item>
          <el-menu-item index="/expense/my-list">我的报销记录</el-menu-item>
          <el-menu-item index="/expense/pending">待我审批</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/approval/records">
          <el-icon>
            <Finished/>
          </el-icon>
          <template #title>审批记录</template>
        </el-menu-item>

        <el-menu-item index="/org">
          <el-icon>
            <OfficeBuilding/>
          </el-icon>
          <template #title>组织架构</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" size="18">
            <Fold v-if="!isCollapse"/>
            <Expand v-else/>
          </el-icon>
        </div>
        <div class="header-right">
          <el-badge :value="pendingCount" :hidden="pendingCount === 0" class="badge-item">
            <el-icon size="20">
              <Bell/>
            </el-icon>
          </el-badge>
          <el-dropdown @command="handleCommand" class="user-menu">
            <div class="user-info">
              <el-avatar size="small" :style="{ backgroundColor: '#1890ff' }">
                {{ authStore.userInfo?.realName?.substring(0, 1) }}
              </el-avatar>
              <span class="username">{{ authStore.userInfo?.realName }}</span>
              <el-icon>
                <ArrowDown/>
              </el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component"/>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import {computed, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '@/store/auth'
import {getPendingExpenseList, getPendingLeaveList} from '@/api/workflow'

import {
  ArrowDown,
  Bell,
  Calendar,
  Expand,
  Finished,
  Fold,
  HomeFilled,
  OfficeBuilding,
  Setting,
  Wallet
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const isCollapse = ref(false)
const pendingCount = ref(0)

const currentRoute = computed(() => route.path)

onMounted(async () => {
  // 加载待审批数量
  try {
    const [leaveRes, expenseRes] = await Promise.all([
      getPendingLeaveList({pageNum: 1, pageSize: 1}),
      getPendingExpenseList({pageNum: 1, pageSize: 1})
    ])
    pendingCount.value = (leaveRes.data?.total || 0) + (expenseRes.data?.total || 0)
    console.log('待审批总数:', pendingCount.value, '(请假:', leaveRes.data?.total, '| 报销:', expenseRes.data?.total, ')')
  } catch (e) {
    // 可能没有审批权限，忽略
  }
})

function handleCommand(command) {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #001529;
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: white;
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.collapse-btn {
  cursor: pointer;
  color: #666;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.badge-item {
  cursor: pointer;
  color: #666;
}

.user-menu {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #333;
}

.username {
  font-size: 14px;
}

.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
}

</style>
