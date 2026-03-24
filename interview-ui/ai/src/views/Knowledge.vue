<template>
  <div class="knowledge-page">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon :size="18"><Collection /></el-icon>
        </div>
        <div>
          <h2>知识库管理</h2>
          <p class="header-sub">管理文档、文本片段与向量数据</p>
        </div>
      </div>
      <button class="add-btn" @click="showUploadDialog = true">
        <el-icon :size="14"><Plus /></el-icon>
        添加文档
      </button>
    </div>
    
    <div class="page-content">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
              <el-icon :size="32"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ documents.length }}</div>
              <div class="stat-label">文档数量</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
              <el-icon :size="32"><Collection /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalChunks }}</div>
              <div class="stat-label">文本片段</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
              <el-icon :size="32"><DataLine /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalSize }}</div>
              <div class="stat-label">总字符数</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      
      <!-- 文档列表 -->
      <el-card class="document-list">
        <template #header>
          <div class="card-header">
            <span>文档列表</span>
            <el-input
              v-model="searchQuery"
              placeholder="搜索文档..."
              prefix-icon="Search"
              clearable
              style="width: 240px"
            />
          </div>
        </template>
        
        <el-table :data="filteredDocuments" v-loading="loading" stripe>
          <el-table-column prop="title" label="文档标题" min-width="200">
            <template #default="{ row }">
              <div class="doc-title">
                <el-icon><Document /></el-icon>
                <span>{{ row.title }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="chunks" label="片段数" width="100" align="center" />
          <el-table-column prop="size" label="字符数" width="120" align="center" />
          <el-table-column prop="createTime" label="添加时间" width="180" />
          <el-table-column label="操作" width="150" align="center">
            <template #default="{ row }">
              <el-button type="primary" link :icon="View" @click="viewDocument(row)">
                查看
              </el-button>
              <el-button type="danger" link :icon="Delete" @click="deleteDocument(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <el-empty v-if="filteredDocuments.length === 0" description="暂无文档" />
      </el-card>
    </div>
    
    <!-- 上传对话框 -->
    <el-dialog
      v-model="showUploadDialog"
      title="添加文档到知识库"
      width="650px"
      destroy-on-close
    >
      <el-tabs v-model="uploadType" type="border-card">
        <!-- 文本输入 -->
        <el-tab-pane label="文本输入" name="text">
          <el-form :model="uploadForm" label-position="top">
            <el-form-item label="文档标题">
              <el-input v-model="uploadForm.title" placeholder="请输入文档标题" />
            </el-form-item>
            
            <el-form-item label="文档内容">
              <el-input
                v-model="uploadForm.content"
                type="textarea"
                :rows="10"
                placeholder="请输入或粘贴文档内容..."
              />
            </el-form-item>
          </el-form>
        </el-tab-pane>
        
        <!-- 文件上传 -->
        <el-tab-pane label="文件上传" name="file">
          <el-form label-position="top">
            <el-form-item label="文档标题（可选）">
              <el-input v-model="uploadForm.title" placeholder="留空将使用文件名作为标题" />
            </el-form-item>
            
            <el-form-item label="选择文件">
              <el-upload
                ref="uploadRef"
                v-model:file-list="fileList"
                drag
                action="#"
                :auto-upload="false"
                :limit="1"
                :on-change="handleFileChange"
                :on-remove="handleFileRemove"
                accept=".txt,.md,.pdf,.doc,.docx"
                class="upload-area"
              >
                <el-icon :size="48" color="#409eff"><Upload /></el-icon>
                <div class="el-upload__text">
                  拖拽文件到此处或 <em>点击上传</em>
                </div>
                <template #tip>
                  <div class="el-upload__tip">
                    支持格式：TXT、Markdown、PDF、Word（.doc/.docx），单个文件不超过 50MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>
            
            <!-- 文件信息展示 -->
            <el-form-item v-if="selectedFile">
              <el-card shadow="never" class="file-info-card">
                <div class="file-info">
                  <el-icon :size="24" color="#409eff"><Document /></el-icon>
                  <div class="file-detail">
                    <div class="file-name">{{ selectedFile.name }}</div>
                    <div class="file-size">{{ formatFileSize(selectedFile.size) }}</div>
                  </div>
                  <el-tag v-if="selectedFile.size > 50 * 1024 * 1024" type="danger" size="small">
                    超过50MB
                  </el-tag>
                  <el-tag v-else type="success" size="small">可上传</el-tag>
                </div>
              </el-card>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">
          添加到知识库
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 查看文档对话框 -->
    <el-dialog
      v-model="showViewDialog"
      title="查看文档"
      width="700px"
    >
      <div class="document-viewer">
        <h3>{{ currentDocument.title }}</h3>
        <div class="document-meta">
          <el-tag size="small">{{ currentDocument.chunks }} 个片段</el-tag>
          <el-tag size="small" type="info">{{ currentDocument.size }} 字符</el-tag>
          <span class="meta-time">{{ currentDocument.createTime }}</span>
        </div>
        <el-divider />
        <div class="document-content">
          <pre>{{ currentDocument.content }}</pre>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Plus, Document, Collection, DataLine, View, Delete, Search, Upload } from '@element-plus/icons-vue'
import { loadText, uploadDocument, getKnowledgeStats, getDocuments, deleteDocument as deleteDocApi } from '@/api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const uploading = ref(false)
const showUploadDialog = ref(false)
const showViewDialog = ref(false)
const searchQuery = ref('')

const uploadForm = ref({
  title: '',
  content: ''
})

// 文件上传相关
const fileList = ref([])
const uploadType = ref('text') // 'text' | 'file'
const uploadRef = ref(null)
const selectedFile = ref(null)

// 文档数据
const documents = ref([])

const currentDocument = ref({})

const filteredDocuments = computed(() => {
  if (!searchQuery.value) return documents.value
  const query = searchQuery.value.toLowerCase()
  return documents.value.filter(doc => 
    doc.title.toLowerCase().includes(query)
  )
})

const totalChunks = computed(() => {
  return documents.value.reduce((sum, doc) => sum + doc.chunks, 0)
})

const totalSize = computed(() => {
  const size = documents.value.reduce((sum, doc) => sum + doc.size, 0)
  return size > 1000 ? (size / 1000).toFixed(1) + 'k' : size
})

// 文件选择变化
const handleFileChange = (uploadFile) => {
  selectedFile.value = uploadFile.raw
  // 如果没有填写标题，自动使用文件名
  if (!uploadForm.value.title && uploadFile.name) {
    uploadForm.value.title = uploadFile.name.replace(/\.[^.]+$/, '')
  }
}

// 文件移除
const handleFileRemove = () => {
  selectedFile.value = null
  fileList.value = []
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 上传文档
const handleUpload = async () => {
  uploading.value = true
  
  try {
    if (uploadType.value === 'text') {
      // 文本上传
      if (!uploadForm.value.title.trim()) {
        ElMessage.warning('请输入文档标题')
        uploading.value = false
        return
      }
      if (!uploadForm.value.content.trim()) {
        ElMessage.warning('请输入文档内容')
        uploading.value = false
        return
      }
      
      await loadText(uploadForm.value.content, uploadForm.value.title)
      
      // 添加到本地列表
      const chunks = uploadForm.value.content.split('\n\n').filter(p => p.trim()).length
      documents.value.unshift({
        id: Date.now().toString(),
        title: uploadForm.value.title,
        content: uploadForm.value.content,
        chunks: chunks,
        size: uploadForm.value.content.length,
        createTime: new Date().toLocaleString()
      })
    } else {
      // 文件上传
      if (!selectedFile.value) {
        ElMessage.warning('请选择要上传的文件')
        uploading.value = false
        return
      }
      
      // 检查文件大小
      if (selectedFile.value.size > 50 * 1024 * 1024) {
        ElMessage.warning('文件大小超过50MB限制')
        uploading.value = false
        return
      }
      
      const file = selectedFile.value
      const res = await uploadDocument(file, uploadForm.value.title)
      
      if (res.success) {
        // 添加到本地列表
        documents.value.unshift({
          id: Date.now().toString(),
          title: res.data?.title || uploadForm.value.title || file.name,
          content: `[文件内容: ${file.name}]`,
          chunks: res.data?.chunks || 0,
          size: res.data?.size || file.size,
          createTime: new Date().toLocaleString()
        })
      }
    }
    
    ElMessage.success('文档已添加到知识库')
    showUploadDialog.value = false
    uploadForm.value = { title: '', content: '' }
    fileList.value = []
    selectedFile.value = null
    
    // 刷新列表和统计
    await loadDocuments()
    await loadStats()
  } catch (error) {
    ElMessage.error(error.message || '添加失败，请重试')
  } finally {
    uploading.value = false
  }
}

// 加载文档列表
const loadDocuments = async () => {
  loading.value = true
  try {
    const res = await getDocuments()
    if (res.success && res.data) {
      documents.value = res.data.map(doc => ({
        id: doc.id,
        title: doc.title,
        content: doc.filename || '',
        chunks: doc.chunks || 0,
        size: doc.size || 0,
        createTime: doc.createTime
      }))
    }
  } catch (error) {
    ElMessage.error('加载文档列表失败')
  } finally {
    loading.value = false
  }
}

// 加载统计数据
const loadStats = async () => {
  try {
    const res = await getKnowledgeStats()
    if (res.success && res.data) {
      // 统计会自动通过计算属性更新
    }
  } catch (error) {
    // 忽略统计错误
  }
}

onMounted(() => {
  loadDocuments()
  loadStats()
})

// 查看文档
const viewDocument = (doc) => {
  currentDocument.value = doc
  showViewDialog.value = true
}

// 删除文档
const deleteDocument = async (doc) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档 "${doc.title}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteDocApi(doc.id)
    await loadDocuments()
    await loadStats()
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}
</script>

<style scoped lang="scss">
.knowledge-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f8;
}

/* 页头 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: #fff;
  border-bottom: 1px solid rgba(0,0,0,0.06);
  box-shadow: 0 1px 8px rgba(0,0,0,0.04);

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;

    .header-icon {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #f093fb, #f5576c);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    h2 {
      margin: 0;
      font-size: 17px;
      font-weight: 600;
      color: #1a1a2e;
    }

    .header-sub {
      margin: 2px 0 0;
      font-size: 12px;
      color: #a0a3bd;
    }
  }

  .add-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 9px 20px;
    border-radius: 10px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    border: none;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(102, 126, 234, 0.45);
    }
  }
}

/* 主内容 */
.page-content {
  flex: 1;
  padding: 24px 28px;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.12); border-radius: 2px; }
}

/* 统计卡片 */
.stats-row {
  margin-bottom: 24px;
}

.stat-card {
  border: none;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 8px 28px rgba(0,0,0,0.1);
  }

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 18px;
    padding: 22px 24px;
  }

  .stat-icon {
    width: 60px;
    height: 60px;
    border-radius: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
    box-shadow: 0 6px 16px rgba(0,0,0,0.15);
  }

  .stat-info {
    flex: 1;

    .stat-value {
      font-size: 30px;
      font-weight: 700;
      color: #1a1a2e;
      line-height: 1.1;
      letter-spacing: -0.5px;
    }

    .stat-label {
      font-size: 13px;
      color: #a0a3bd;
      margin-top: 5px;
      font-weight: 500;
    }
  }
}

/* 文档列表 */
.document-list {
  border: none;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);

  :deep(.el-card__header) {
    padding: 16px 24px;
    border-bottom: 1px solid rgba(0,0,0,0.05);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    span {
      font-size: 15px;
      font-weight: 600;
      color: #1a1a2e;
    }

    :deep(.el-input__wrapper) {
      border-radius: 10px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
  }

  :deep(.el-table) {
    --el-table-header-bg-color: #fafbff;
    --el-table-row-hover-bg-color: rgba(102, 126, 234, 0.04);

    .el-table__header th {
      font-weight: 600;
      color: #606266;
      font-size: 13px;
    }

    .el-table__row {
      transition: background 0.15s;

      td {
        color: #303133;
        font-size: 13px;
      }
    }
  }

  .doc-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 500;

    .el-icon { color: #667eea; }
  }
}

/* 文档查看弹窗 */
.document-viewer {
  h3 {
    margin: 0 0 14px;
    color: #1a1a2e;
    font-size: 16px;
  }

  .document-meta {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 4px;

    .meta-time {
      color: #a0a3bd;
      font-size: 12px;
    }
  }

  .document-content {
    max-height: 400px;
    overflow-y: auto;
    margin-top: 4px;

    pre {
      margin: 0;
      padding: 18px;
      background: #f8f9ff;
      border-radius: 10px;
      border: 1px solid rgba(102, 126, 234, 0.1);
      font-family: 'Fira Code', 'JetBrains Mono', monospace;
      font-size: 13px;
      line-height: 1.7;
      white-space: pre-wrap;
      word-wrap: break-word;
      color: #303133;
    }
  }
}

/* 上传区域 */
.upload-area {
  width: 100%;

  :deep(.el-upload-dragger) {
    width: 100%;
    padding: 40px 20px;
    border-radius: 12px;
    border: 2px dashed #d0d5e8;
    background: #fafbff;
    transition: all 0.2s;

    &:hover {
      border-color: #667eea;
      background: rgba(102, 126, 234, 0.03);
    }
  }

  :deep(.el-upload__text) {
    font-size: 14px;
    color: #909399;

    em {
      color: #667eea;
      font-style: normal;
      font-weight: 500;
    }
  }

  :deep(.el-upload__tip) {
    text-align: center;
    color: #c0c4cc;
    margin-top: 12px;
    font-size: 12px;
  }
}

.file-info-card {
  width: 100%;
  background: #f8f9ff;
  border: 1px solid rgba(102, 126, 234, 0.12);
  border-radius: 10px;

  :deep(.el-card__body) { padding: 12px 16px; }

  .file-info {
    display: flex;
    align-items: center;
    gap: 12px;

    .file-detail {
      flex: 1;
      min-width: 0;

      .file-name {
        font-size: 14px;
        color: #303133;
        font-weight: 500;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .file-size {
        font-size: 12px;
        color: #a0a3bd;
        margin-top: 2px;
      }
    }
  }
}
</style>
