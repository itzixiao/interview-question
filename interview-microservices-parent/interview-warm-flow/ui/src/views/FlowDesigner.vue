<template>
  <div class="designer-container">
    <el-page-header @back="goBack" content="Warm-Flow 流程设计器" />

    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>🎨 Warm-Flow 官方流程设计器</span>
          <div class="header-actions">
            <el-button type="primary" @click="openDesigner" size="small">
              打开设计器
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
          title="Warm-Flow 设计器已集成到后端，点击下方按钮直接访问"
          type="info"
          :closable="false"
          style="margin-bottom: 20px;"
      />

      <el-descriptions :column="2" border>
        <el-descriptions-item label="设计器地址">
          <el-link :href="designerUrl" target="_blank" type="primary">
            {{ designerUrl }}
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item label="访问方式">浏览器直接访问</el-descriptions-item>
        <el-descriptions-item label="功能特性" :span="2">
          <el-tag type="success" style="margin: 5px;">可视化设计</el-tag>
          <el-tag type="success" style="margin: 5px;">节点配置</el-tag>
          <el-tag type="success" style="margin: 5px;">流程发布</el-tag>
          <el-tag type="success" style="margin: 5px;">办理人设置</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <el-divider />

      <h3>📖 使用说明</h3>
      <el-steps :active="4" finish-status="success" simple style="margin-top: 20px">
        <el-step title="打开设计器" />
        <el-step title="创建流程定义" />
        <el-step title="绘制流程图" />
        <el-step title="发布流程" />
      </el-steps>

      <el-alert
          title="提示：设计器已集成到后端，访问 /warm-flow-ui/index.html 即可使用"
          type="warning"
          :closable="false"
          style="margin-top: 20px"
      />
    </el-card>
  </div>
</template>

<script setup>
import {ref} from 'vue'
import {useRouter} from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.push('/')
}

// 设计器地址
const backendUrl = 'http://localhost:8085'
const designerUrl = ref(`${backendUrl}/warm-flow-ui/index.html`)

const openDesigner = () => {
  window.open(designerUrl.value, '_blank')
}
</script>

<style scoped>
.designer-container {
  max-width: 1200px;
  margin: 20px auto;
  padding: 0 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

h3 {
  margin-top: 20px;
  color: #303133;
}
</style>
