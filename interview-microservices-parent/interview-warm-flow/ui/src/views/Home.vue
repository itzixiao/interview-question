<template>
  <div class="home-container">
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <h1>🔥 Warm-Flow 工作流审批系统</h1>
          <p>轻量级审批工作流引擎 - 前端测试与流程设计器</p>
        </div>
      </el-header>

      <el-main class="main-content">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="nav-card" shadow="hover" @click="navigateTo('/leave')">
              <el-icon :size="50" color="#409EFF"><Calendar /></el-icon>
              <h2>请假审批</h2>
              <p>测试请假申请、审批、撤销等完整流程</p>
              <el-button type="primary" plain>进入测试</el-button>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="nav-card" shadow="hover" @click="navigateTo('/reimbursement')">
              <el-icon :size="50" color="#67C23A"><Money /></el-icon>
              <h2>报销审批</h2>
              <p>测试报销申请、审批、撤销等完整流程</p>
              <el-button type="success" plain>进入测试</el-button>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="nav-card" shadow="hover" @click="navigateTo('/designer')">
              <el-icon :size="50" color="#E6A23C"><EditPen /></el-icon>
              <h2>流程设计器</h2>
              <p>可视化设计 BPMN 2.0 工作流程</p>
              <el-button type="warning" plain>开始设计</el-button>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="info-card" style="margin-top: 30px;">
          <template #header>
            <div class="card-header">
              <span>📖 系统说明</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工作流引擎">Warm-Flow 1.7.2</el-descriptions-item>
            <el-descriptions-item label="后端端口">8085</el-descriptions-item>
            <el-descriptions-item label="前端端口">5174</el-descriptions-item>
            <el-descriptions-item label="技术栈">Vue 3 + Element Plus + bpmn-js</el-descriptions-item>
          </el-descriptions>

          <el-divider />

          <h3>🚀 快速开始</h3>
          <el-steps :active="3" finish-status="success" simple style="margin-top: 20px">
            <el-step title="启动后端" />
            <el-step title="启动前端" />
            <el-step title="测试接口" />
          </el-steps>

          <el-alert
              title="提示：请先确保后端服务已启动在 8085 端口"
              type="info"
              :closable="false"
              style="margin-top: 20px"
          />
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import {useRouter} from 'vue-router'
import {Calendar, Money, EditPen} from '@element-plus/icons-vue'

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-content {
  text-align: center;
}

.header-content h1 {
  margin: 0;
  color: #303133;
  font-size: 32px;
}

.header-content p {
  margin: 10px 0 0;
  color: #606266;
  font-size: 16px;
}

.main-content {
  max-width: 1200px;
  margin: 40px auto;
  padding: 0 20px;
}

.nav-card {
  text-align: center;
  padding: 30px 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.nav-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.nav-card h2 {
  margin: 20px 0 10px;
  color: #303133;
}

.nav-card p {
  color: #606266;
  margin-bottom: 20px;
  min-height: 40px;
}

.info-card {
  backdrop-filter: blur(10px);
  background: rgba(255, 255, 255, 0.95);
}

.card-header {
  font-weight: bold;
  font-size: 18px;
}

h3 {
  margin-top: 20px;
  color: #303133;
}
</style>
