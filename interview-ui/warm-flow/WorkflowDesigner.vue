<template>
  <div class="warm-flow-designer">
    <!-- 工具栏 -->
    <div class="designer-toolbar">
      <el-space>
        <el-button type="primary" @click="saveProcess" :loading="saving">
          <el-icon><Check /></el-icon>
          保存流程
        </el-button>
        <el-button type="success" @click="deployProcess" :loading="deploying">
          <el-icon><Upload /></el-icon>
          部署流程
        </el-button>
        <el-button @click="exportXML">
          <el-icon><Download /></el-icon>
          导出 XML
        </el-button>
        <el-button @click="importXML">
          <el-icon><Upload /></el-icon>
          导入 XML
        </el-button>
        <el-button @click="loadProcess">
          <el-icon><FolderOpened /></el-icon>
          加载流程
        </el-button>
      </el-space>
    </div>

    <!-- 设计器主体 -->
    <div class="designer-container">
      <!-- BPMN 画布 -->
      <div ref="bpmnCanvas" class="bpmn-canvas"></div>
      
      <!-- 属性面板 -->
      <div class="properties-panel">
        <h3>节点属性</h3>
        <el-form v-if="selectedElement" label-width="80px" size="small">
          <el-form-item label="节点ID">
            <el-input v-model="selectedElement.id" disabled />
          </el-form-item>
          <el-form-item label="节点名称">
            <el-input v-model="elementName" @change="updateElementName" />
          </el-form-item>
          <el-form-item label="节点类型">
            <el-tag>{{ elementType }}</el-tag>
          </el-form-item>
        </el-form>
        <el-empty v-else description="请选择一个节点" />
      </div>
    </div>

    <!-- 导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入 BPMN XML" width="600px">
      <el-input
        v-model="importXMLContent"
        type="textarea"
        :rows="15"
        placeholder="请粘贴 BPMN XML 内容"
      />
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportXML">导入</el-button>
      </template>
    </el-dialog>

    <!-- 加载流程对话框 -->
    <el-dialog v-model="loadDialogVisible" title="加载流程" width="800px">
      <el-table :data="processList" @row-click="handleSelectProcess">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="流程名称" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Check, Upload, Download, FolderOpened } from '@element-plus/icons-vue'
import BpmnModeler from 'bpmn-js/lib/Modeler'
import axios from 'axios'

// Refs
const bpmnCanvas = ref(null)
const importDialogVisible = ref(false)
const loadDialogVisible = ref(false)
const importXMLContent = ref('')
const processList = ref([])
const selectedElement = ref(null)
const elementName = ref('')
const elementType = ref('')
const saving = ref(false)
const deploying = ref(false)

let modeler = null

// 初始化设计器
onMounted(() => {
  initDesigner()
})

// 清理资源
onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy()
  }
})

// 初始化 BPMN 设计器
function initDesigner() {
  modeler = new BpmnModeler({
    container: bpmnCanvas.value,
    keyboard: {
      bindTo: document
    }
  })

  // 监听元素选择
  modeler.on('selection.changed', (event) => {
    const element = event.newSelection[0]
    if (element) {
      selectedElement.value = element
      elementName.value = element.businessObject.name || ''
      elementType.value = getElementType(element)
    } else {
      selectedElement.value = null
      elementName.value = ''
      elementType.value = ''
    }
  })

  // 创建默认流程图
  createDefaultDiagram()
}

// 创建默认流程图
async function createDefaultDiagram() {
  const defaultXML = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn2:definitions xmlns:bpmn2="http://www.omg.org/spec/BPMN/20100524/MODEL"
                   xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                   xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                   id="definitions"
                   targetNamespace="http://bpmn.io/schema/bpmn">
  <bpmn2:process id="leave_approval" name="请假审批流程" isExecutable="true">
    <bpmn2:startEvent id="start" name="开始">
      <bpmn2:outgoing>flow1</bpmn2:outgoing>
    </bpmn2:startEvent>
    <bpmn2:userTask id="manager_approve" name="经理审批">
      <bpmn2:incoming>flow1</bpmn2:incoming>
      <bpmn2:outgoing>flow2</bpmn2:outgoing>
    </bpmn2:userTask>
    <bpmn2:endEvent id="end" name="结束">
      <bpmn2:incoming>flow2</bpmn2:incoming>
    </bpmn2:endEvent>
    <bpmn2:sequenceFlow id="flow1" sourceRef="start" targetRef="manager_approve" />
    <bpmn2:sequenceFlow id="flow2" sourceRef="manager_approve" targetRef="end" />
  </bpmn2:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="leave_approval">
      <bpmndi:BPMNShape id="start_di" bpmnElement="start">
        <dc:Bounds x="150" y="200" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="155" y="243" width="26" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="manager_approve_di" bpmnElement="manager_approve">
        <dc:Bounds x="300" y="178" width="100" height="80" />
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="end_di" bpmnElement="end">
        <dc:Bounds x="500" y="200" width="36" height="36" />
        <bpmndi:BPMNLabel>
          <dc:Bounds x="505" y="243" width="26" height="14" />
        </bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="flow1_di" bpmnElement="flow1">
        <di:waypoint x="186" y="218" />
        <di:waypoint x="300" y="218" />
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="flow2_di" bpmnElement="flow2">
        <di:waypoint x="400" y="218" />
        <di:waypoint x="500" y="218" />
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn2:definitions>`

  try {
    await modeler.importXML(defaultXML)
    modeler.get('canvas').zoom('fit-viewport')
  } catch (err) {
    console.error('创建默认流程图失败:', err)
  }
}

// 获取元素类型
function getElementType(element) {
  const type = element.type
  if (type === 'bpmn:StartEvent') return '开始事件'
  if (type === 'bpmn:EndEvent') return '结束事件'
  if (type === 'bpmn:UserTask') return '用户任务'
  if (type === 'bpmn:ServiceTask') return '服务任务'
  if (type === 'bpmn:ExclusiveGateway') return '排他网关'
  if (type === 'bpmn:ParallelGateway') return '并行网关'
  if (type === 'bpmn:SequenceFlow') return '顺序流'
  return type
}

// 更新元素名称
function updateElementName() {
  if (!selectedElement.value) return

  const modeling = modeler.get('modeling')
  modeling.updateLabel(selectedElement.value, elementName.value)
}

// 保存流程
async function saveProcess() {
  saving.value = true
  try {
    const { xml } = await modeler.saveXML({ format: true })
    
    await axios.post('/warm-flow/def/save', {
      name: '请假审批流程',
      processJson: xml,
      category: 'leave'
    })
    
    ElMessage.success('流程保存成功')
  } catch (err) {
    ElMessage.error('保存失败: ' + err.message)
  } finally {
    saving.value = false
  }
}

// 部署流程
async function deployProcess() {
  deploying.value = true
  try {
    const { xml } = await modeler.saveXML({ format: true })
    
    // 先保存
    const saveRes = await axios.post('/warm-flow/def/save', {
      name: '请假审批流程',
      processJson: xml,
      category: 'leave'
    })
    
    // 再部署
    await axios.post(`/warm-flow/def/deploy/${saveRes.data}`)
    
    ElMessage.success('流程部署成功')
  } catch (err) {
    ElMessage.error('部署失败: ' + err.message)
  } finally {
    deploying.value = false
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
    a.download = `leave_approval_${Date.now()}.bpmn`
    a.click()
    URL.revokeObjectURL(url)
    
    ElMessage.success('导出成功')
  } catch (err) {
    ElMessage.error('导出失败: ' + err.message)
  }
}

// 导入 XML
function importXML() {
  importDialogVisible.value = true
  importXMLContent.value = ''
}

// 处理导入 XML
async function handleImportXML() {
  if (!importXMLContent.value.trim()) {
    ElMessage.warning('请输入 XML 内容')
    return
  }

  try {
    await modeler.importXML(importXMLContent.value)
    modeler.get('canvas').zoom('fit-viewport')
    ElMessage.success('导入成功')
    importDialogVisible.value = false
  } catch (err) {
    ElMessage.error('导入失败: ' + err.message)
  }
}

// 加载流程
async function loadProcess() {
  try {
    const res = await axios.get('/warm-flow/def/list', {
      params: { pageNum: 1, pageSize: 100 }
    })
    processList.value = res.data.records || []
    loadDialogVisible.value = true
  } catch (err) {
    ElMessage.error('获取流程列表失败: ' + err.message)
  }
}

// 选择流程
async function handleSelectProcess(row) {
  try {
    const res = await axios.get(`/warm-flow/def/${row.id}`)
    const xml = res.data.processJson
    
    await modeler.importXML(xml)
    modeler.get('canvas').zoom('fit-viewport')
    
    ElMessage.success('加载成功')
    loadDialogVisible.value = false
  } catch (err) {
    ElMessage.error('加载失败: ' + err.message)
  }
}
</script>

<style scoped>
.warm-flow-designer {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}

.designer-toolbar {
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.designer-container {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.bpmn-canvas {
  flex: 1;
  background: #fff;
}

.properties-panel {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  padding: 16px;
  overflow-y: auto;
}

.properties-panel h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

/* BPMN 设计器样式覆盖 */
:deep(.bjs-powered-by) {
  display: none;
}

:deep(.djs-palette) {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}

:deep(.djs-context-pad) {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
</style>
