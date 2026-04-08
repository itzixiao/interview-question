# Warm-Flow 流程设计器前端集成

本文档展示如何在前端项目中集成 Warm-Flow 流程设计器。

## 目录

- [1. 集成方式](#1-集成方式)
- [2. Vue 3 集成示例](#2-vue-3-集成示例)
- [3. Vue 2 集成示例](#3-vue-2-集成示例)
- [4. React 集成示例](#4-react-集成示例)
- [5. 核心功能](#5-核心功能)
- [6. API 接口](#6-api-接口)

---

## 1. 集成方式

Warm-Flow 提供了两种集成方式：

### 方式一：使用内置 UI 插件（推荐）

后端已集成 `warm-flow-plugin-ui-sb-web`，直接访问：

```
http://localhost:8092/warm-flow-ui
```

### 方式二：自定义前端集成

在前端项目中嵌入设计器，需要：
1. 引入 bpmn-js 库
2. 调用后端 API 接口
3. 自定义 UI 样式

---

## 2. Vue 3 集成示例

### 2.1 安装依赖

```bash
npm install bpmn-js bpmn-js-properties-panel
```

### 2.2 创建流程设计器组件

```vue
<template>
  <div class="workflow-designer">
    <div class="toolbar">
      <el-button @click="saveProcess" type="primary">保存流程</el-button>
      <el-button @click="deployProcess" type="success">部署流程</el-button>
      <el-button @click="exportXML">导出 XML</el-button>
    </div>
    
    <div class="canvas-container">
      <div ref="bpmnCanvas" class="bpmn-canvas"></div>
    </div>
    
    <!-- 流程属性面板 -->
    <div class="properties-panel" ref="propertiesPanel"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const bpmnCanvas = ref(null)
const propertiesPanel = ref(null)
let modeler = null

// 初始化设计器
onMounted(() => {
  modeler = new BpmnModeler({
    container: bpmnCanvas.value,
    keyboard: {
      bindTo: document
    }
  })

  // 创建新流程
  createNewDiagram()
})

// 创建新流程图
async function createNewDiagram() {
  const newDiagram = `<?xml version="1.0" encoding="UTF-8"?>
    <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                       id="definitions"
                       targetNamespace="http://bpmn.io/schema/bpmn">
      <bpmn2:process id="leave_approval" name="请假审批流程" isExecutable="true">
        <bpmn2:startEvent id="start" name="开始" />
      </bpmn2:process>
    </bpmn2:definitions>`

  try {
    await modeler.importXML(newDiagram)
    modeler.get('canvas').zoom('fit-viewport')
  } catch (err) {
    console.error('创建流程图失败:', err)
  }
}

// 保存流程
async function saveProcess() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    
    await axios.post('/api/warm-flow/def/save', {
      name: '请假审批流程',
      processJson: xml,
      category: 'leave'
    })
    
    ElMessage.success('流程保存成功')
  } catch (err) {
    ElMessage.error('保存失败: ' + err.message)
  }
}

// 部署流程
async function deployProcess() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    
    await axios.post('/api/warm-flow/def/deploy', {
      name: '请假审批流程',
      processJson: xml,
      category: 'leave'
    })
    
    ElMessage.success('流程部署成功')
  } catch (err) {
    ElMessage.error('部署失败: ' + err.message)
  }
}

// 导出 XML
async function exportXML() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    
    const blob = new Blob([xml], { type: 'application/xml' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'leave_approval.bpmn'
    a.click()
    URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error('导出失败: ' + err.message)
  }
}

// 清理资源
onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy()
  }
})
</script>

<style scoped>
.workflow-designer {
  display: flex;
  height: 100vh;
}

.toolbar {
  padding: 10px;
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
}

.canvas-container {
  flex: 1;
  position: relative;
}

.bpmn-canvas {
  width: 100%;
  height: 100%;
}

.properties-panel {
  width: 300px;
  border-left: 1px solid #ddd;
  background: #fff;
}
</style>
```

### 2.3 流程实例管理页面

```vue
<template>
  <div class="process-instance-list">
    <el-table :data="instanceList" border>
      <el-table-column prop="id" label="实例ID" width="100" />
      <el-table-column prop="businessId" label="业务ID" width="120" />
      <el-table-column prop="flowStatus" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.flowStatus)">
            {{ getStatusText(row.flowStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">详情</el-button>
          <el-button size="small" type="primary" @click="approve(row)">审批</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 审批对话框 -->
    <el-dialog v-model="dialogVisible" title="审批" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="form.approved">
            <el-radio :label="true">通过</el-radio>
            <el-radio :label="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="form.comment" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApproval">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const instanceList = ref([])
const dialogVisible = ref(false)
const form = ref({
  instanceId: '',
  approved: true,
  comment: ''
})

// 获取流程实例列表
onMounted(async () => {
  const res = await axios.get('/api/warm-flow/ins/list')
  instanceList.value = res.data
})

// 审批
function approve(row) {
  form.value.instanceId = row.id
  dialogVisible.value = true
}

// 提交审批
async function submitApproval() {
  try {
    await axios.post(`/api/warm-flow/task/approve/${form.value.instanceId}`, {
      approved: form.value.approved,
      comment: form.value.comment
    })
    
    ElMessage.success('审批成功')
    dialogVisible.value = false
    
    // 刷新列表
    const res = await axios.get('/api/warm-flow/ins/list')
    instanceList.value = res.data
  } catch (err) {
    ElMessage.error('审批失败: ' + err.message)
  }
}

// 查看详情
function viewDetail(row) {
  window.open(`/warm-flow-ui?instanceId=${row.id}`, '_blank')
}

// 状态映射
function getStatusType(status) {
  const map = {
    0: 'info',
    1: 'warning',
    2: 'success',
    3: 'danger'
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    0: '草稿',
    1: '审批中',
    2: '已通过',
    3: '已驳回'
  }
  return map[status] || '未知'
}
</script>
```

---

## 3. Vue 2 集成示例

### 3.1 安装依赖

```bash
npm install bpmn-js@8.9.0
```

### 3.2 创建流程设计器组件

```vue
<template>
  <div class="workflow-designer">
    <div class="toolbar">
      <el-button @click="saveProcess" type="primary">保存流程</el-button>
      <el-button @click="deployProcess" type="success">部署流程</el-button>
    </div>
    
    <div ref="bpmnCanvas" class="bpmn-canvas"></div>
  </div>
</template>

<script>
import BpmnModeler from 'bpmn-js/lib/Modeler'
import axios from 'axios'

export default {
  name: 'WorkflowDesigner',
  data() {
    return {
      modeler: null
    }
  },
  mounted() {
    this.initDesigner()
  },
  beforeDestroy() {
    if (this.modeler) {
      this.modeler.destroy()
    }
  },
  methods: {
    initDesigner() {
      this.modeler = new BpmnModeler({
        container: this.$refs.bpmnCanvas
      })
      this.createNewDiagram()
    },

    async createNewDiagram() {
      const newDiagram = `<?xml version="1.0" encoding="UTF-8"?>
        <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                           id="definitions">
          <bpmn2:process id="leave_approval" name="请假审批流程" isExecutable="true">
            <bpmn2:startEvent id="start" name="开始" />
          </bpmn2:process>
        </bpmn2:definitions>`

      try {
        await this.modeler.importXML(newDiagram)
        this.modeler.get('canvas').zoom('fit-viewport')
      } catch (err) {
        console.error('创建失败:', err)
      }
    },

    async saveProcess() {
      try {
        const { xml } = await this.modeler.saveXML({ format: true })
        await axios.post('/api/warm-flow/def/save', {
          name: '请假审批流程',
          processJson: xml
        })
        this.$message.success('保存成功')
      } catch (err) {
        this.$message.error('保存失败')
      }
    },

    async deployProcess() {
      try {
        const { xml } = await this.modeler.saveXML({ format: true })
        await axios.post('/api/warm-flow/def/deploy', {
          name: '请假审批流程',
          processJson: xml
        })
        this.$message.success('部署成功')
      } catch (err) {
        this.$message.error('部署失败')
      }
    }
  }
}
</script>

<style scoped>
.workflow-designer {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.toolbar {
  padding: 10px;
  background: #f5f5f5;
}

.bpmn-canvas {
  flex: 1;
}
</style>
```

---

## 4. React 集成示例

### 4.1 安装依赖

```bash
npm install bpmn-js
```

### 4.2 创建流程设计器组件

```jsx
import React, { useEffect, useRef, useState } from 'react'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { Button, message } from 'antd'
import axios from 'axios'

const WorkflowDesigner = () => {
  const canvasRef = useRef(null)
  const modelerRef = useRef(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    initDesigner()
    return () => {
      if (modelerRef.current) {
        modelerRef.current.destroy()
      }
    }
  }, [])

  const initDesigner = () => {
    modelerRef.current = new BpmnModeler({
      container: canvasRef.current
    })
    createNewDiagram()
  }

  const createNewDiagram = async () => {
    const newDiagram = `<?xml version="1.0" encoding="UTF-8"?>
      <bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         id="definitions">
        <bpmn2:process id="leave_approval" name="请假审批流程" isExecutable="true">
          <bpmn2:startEvent id="start" name="开始" />
        </bpmn2:process>
      </bpmn2:definitions>`

    try {
      await modelerRef.current.importXML(newDiagram)
      modelerRef.current.get('canvas').zoom('fit-viewport')
    } catch (err) {
      console.error('创建失败:', err)
    }
  }

  const saveProcess = async () => {
    setLoading(true)
    try {
      const { xml } = await modelerRef.current.saveXML({ format: true })
      await axios.post('/api/warm-flow/def/save', {
        name: '请假审批流程',
        processJson: xml
      })
      message.success('保存成功')
    } catch (err) {
      message.error('保存失败')
    } finally {
      setLoading(false)
    }
  }

  const deployProcess = async () => {
    setLoading(true)
    try {
      const { xml } = await modelerRef.current.saveXML({ format: true })
      await axios.post('/api/warm-flow/def/deploy', {
        name: '请假审批流程',
        processJson: xml
      })
      message.success('部署成功')
    } catch (err) {
      message.error('部署失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '10px', background: '#f5f5f5' }}>
        <Button type="primary" onClick={saveProcess} loading={loading}>
          保存流程
        </Button>
        <Button type="primary" onClick={deployProcess} loading={loading} style={{ marginLeft: 8 }}>
          部署流程
        </Button>
      </div>
      <div ref={canvasRef} style={{ flex: 1 }} />
    </div>
  )
}

export default WorkflowDesigner
```

---

## 5. 核心功能

### 5.1 流程设计

- ✅ 拖拽式流程设计
- ✅ 支持开始节点、审批节点、条件分支、并行网关
- ✅ 可视化配置审批人、条件表达式
- ✅ 实时保存和预览

### 5.2 流程管理

- ✅ 流程定义列表
- ✅ 流程部署/取消部署
- ✅ 流程版本管理
- ✅ 流程导入/导出

### 5.3 流程实例

- ✅ 发起流程
- ✅ 审批流程
- ✅ 查看流程图及进度
- ✅ 流程历史记录

---

## 6. API 接口

### 6.1 流程定义接口

```javascript
// 保存流程定义
POST /warm-flow/def/save
{
  "name": "流程名称",
  "processJson": "BPMN XML",
  "category": "分类"
}

// 部署流程
POST /warm-flow/def/deploy/{definitionId}

// 获取流程定义列表
GET /warm-flow/def/list?pageNum=1&pageSize=10
```

### 6.2 流程实例接口

```javascript
// 启动流程
POST /warm-flow/ins/start/{definitionId}
{
  "businessId": "业务ID",
  "variable": {}
}

// 获取流程实例列表
GET /warm-flow/ins/list?pageNum=1&pageSize=10

// 查看流程图
GET /warm-flow/ins/chart/{instanceId}
```

### 6.3 任务接口

```javascript
// 获取待办任务
GET /warm-flow/task/todo?pageNum=1&pageSize=10

// 审批通过
POST /warm-flow/task/skip/{taskId}
{
  "variable": {}
}

// 审批驳回
POST /warm-flow/task/termination/{taskId}
```

---

## 7. 访问内置设计器

Warm-Flow 提供了开箱即用的设计器 UI，直接访问：

```
http://localhost:8092/warm-flow-ui
```

### 设计器功能

- 流程定义管理
- 可视化流程设计
- 流程部署
- 流程实例监控
- 任务审批

### 设计器截图

![流程设计器](../docs/images/warm-flow-designer.png)

---

## 相关文档

- [后端 API 文档](http://localhost:8092/doc.html)
- [Warm-Flow 官方文档](https://warm-flow.com/)
- [bpmn-js 官方文档](https://github.com/bpmn-io/bpmn-js)
