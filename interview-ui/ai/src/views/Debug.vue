<template>
  <div class="debug-page">
    <div class="page-header">
      <h2>接口调试</h2>
      <el-tag :type="serverStatus.type">{{ serverStatus.text }}</el-tag>
    </div>
    
    <div class="page-content">
      <!-- 健康检查 -->
      <el-card class="debug-card">
        <template #header>
          <div class="card-header">
            <span>健康检查</span>
            <el-button type="primary" size="small" @click="checkHealth">测试</el-button>
          </div>
        </template>
        <div class="result-area">
          <pre v-if="healthResult">{{ JSON.stringify(healthResult, null, 2) }}</pre>
          <el-empty v-else description="点击测试按钮" />
        </div>
      </el-card>
      
      <!-- 文本上传测试 -->
      <el-card class="debug-card">
        <template #header>
          <div class="card-header">
            <span>文本上传测试</span>
            <el-button type="primary" size="small" @click="testTextUpload">测试</el-button>
          </div>
        </template>
        <el-form label-position="top">
          <el-form-item label="标题">
            <el-input v-model="testForm.title" placeholder="测试文档标题" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="testForm.content" type="textarea" :rows="3" placeholder="测试文档内容" />
          </el-form-item>
        </el-form>
        <div class="result-area">
          <pre v-if="textUploadResult">{{ JSON.stringify(textUploadResult, null, 2) }}</pre>
        </div>
      </el-card>
      
      <!-- RAG问答测试 -->
      <el-card class="debug-card">
        <template #header>
          <div class="card-header">
            <span>RAG 问答测试</span>
            <el-button type="primary" size="small" @click="testRagAsk">测试</el-button>
          </div>
        </template>
        <el-form label-position="top">
          <el-form-item label="问题">
            <el-input v-model="testForm.question" placeholder="输入问题" />
          </el-form-item>
        </el-form>
        <div class="result-area">
          <pre v-if="ragResult">{{ JSON.stringify(ragResult, null, 2) }}</pre>
        </div>
      </el-card>
      
      <!-- 文件上传测试 -->
      <el-card class="debug-card">
        <template #header>
          <div class="card-header">
            <span>文件上传测试</span>
          </div>
        </template>
        <el-upload
          drag
          action="#"
          :auto-upload="false"
          :on-change="handleTestFileChange"
          accept=".txt,.md,.pdf"
        >
          <el-icon :size="32"><Upload /></el-icon>
          <div>选择测试文件</div>
        </el-upload>
        <el-button 
          v-if="testFile" 
          type="primary" 
          class="mt-2" 
          @click="testFileUpload"
          :loading="fileUploading"
        >
          上传测试
        </el-button>
        <div class="result-area" v-if="fileUploadResult">
          <pre>{{ JSON.stringify(fileUploadResult, null, 2) }}</pre>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { healthCheck, loadText, ragAsk, uploadDocument } from '@/api/ai'
import { ElMessage } from 'element-plus'

const serverStatus = ref({ type: 'info', text: '未检测' })
const healthResult = ref(null)
const textUploadResult = ref(null)
const ragResult = ref(null)
const fileUploadResult = ref(null)
const fileUploading = ref(false)
const testFile = ref(null)

const testForm = ref({
  title: '测试文档',
  content: '这是一个测试文档内容。\n\n用于测试文本上传接口。',
  question: '什么是Spring Boot?'
})

// 健康检查
const checkHealth = async () => {
  try {
    const res = await healthCheck()
    healthResult.value = res
    serverStatus.value = { type: 'success', text: '服务正常' }
    ElMessage.success('连接成功')
  } catch (error) {
    healthResult.value = { error: error.message }
    serverStatus.value = { type: 'danger', text: '服务异常' }
    ElMessage.error('连接失败')
  }
}

// 测试文本上传
const testTextUpload = async () => {
  try {
    const res = await loadText(testForm.value.content, testForm.value.title)
    textUploadResult.value = res
    ElMessage.success('文本上传成功')
  } catch (error) {
    textUploadResult.value = { error: error.message }
    ElMessage.error('文本上传失败')
  }
}

// 测试RAG问答
const testRagAsk = async () => {
  try {
    const res = await ragAsk(testForm.value.question, 3)
    ragResult.value = res
    ElMessage.success('RAG问答成功')
  } catch (error) {
    ragResult.value = { error: error.message }
    ElMessage.error('RAG问答失败')
  }
}

// 测试文件选择
const handleTestFileChange = (uploadFile) => {
  testFile.value = uploadFile.raw
}

// 测试文件上传
const testFileUpload = async () => {
  if (!testFile.value) return
  
  fileUploading.value = true
  try {
    const res = await uploadDocument(testFile.value, '测试文件')
    fileUploadResult.value = res
    ElMessage.success('文件上传成功')
  } catch (error) {
    fileUploadResult.value = { error: error.message }
    ElMessage.error('文件上传失败')
  } finally {
    fileUploading.value = false
  }
}

onMounted(() => {
  checkHealth()
})
</script>

<style scoped lang="scss">
.debug-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  
  h2 {
    margin: 0;
    font-size: 18px;
    color: #303133;
  }
}

.page-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.debug-card {
  margin-bottom: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .result-area {
    margin-top: 16px;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 8px;
    
    pre {
      margin: 0;
      font-family: 'Fira Code', monospace;
      font-size: 13px;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
  }
}

.mt-2 {
  margin-top: 12px;
}
</style>
