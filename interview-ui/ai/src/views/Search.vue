<template>
  <div class="search-page">
    <div class="page-header">
      <div class="header-left">
        <div class="header-icon">
          <el-icon :size="18"><Search /></el-icon>
        </div>
        <div>
          <h2>知识检索</h2>
          <p class="header-sub">智能问答 · 相似度检索</p>
        </div>
      </div>
    </div>

    <div class="search-container">
      <!-- 搜索框 -->
      <div class="search-hero">
        <div class="search-input-wrap">
          <el-icon class="search-prefix-icon" :size="20"><Search /></el-icon>
          <input
            v-model="searchQuery"
            class="search-input"
            placeholder="输入关键词搜索知识库..."
            @keyup.enter="handleSearch"
          />
          <button class="search-submit" :class="{ loading }" @click="handleSearch">
            <el-icon v-if="!loading" :size="16"><Search /></el-icon>
            <el-icon v-else :size="16" class="spin"><Loading /></el-icon>
            <span>{{ loading ? '搜索中' : '搜索' }}</span>
          </button>
        </div>

        <div class="search-options">
          <div class="mode-tabs">
            <div
              class="mode-tab"
              :class="{ active: searchMode === 'rag' }"
              @click="searchMode = 'rag'"
            >
              <el-icon :size="13"><ChatRound /></el-icon>
              智能问答
            </div>
            <div
              class="mode-tab"
              :class="{ active: searchMode === 'similar' }"
              @click="searchMode = 'similar'"
            >
              <el-icon :size="13"><Connection /></el-icon>
              相似度检索
            </div>
          </div>

          <el-select v-model="topK" size="small" class="topk-select">
            <el-option label="Top 3" :value="3" />
            <el-option label="Top 5" :value="5" />
            <el-option label="Top 10" :value="10" />
          </el-select>
        </div>
      </div>

      <!-- 搜索结果 -->
      <div class="search-results" v-loading="loading">
        <!-- 智能问答结果 -->
        <template v-if="searchMode === 'rag' && hasResult">
          <div class="answer-card">
            <div class="answer-header">
              <div class="answer-header-left">
                <div class="answer-icon">
                  <el-icon :size="16"><CircleCheck /></el-icon>
                </div>
                <span>AI 回答</span>
              </div>
              <el-tag v-if="answerTime" size="small" type="info" round>
                耗时 {{ answerTime }}ms
              </el-tag>
            </div>
            <div class="answer-content" v-html="renderedAnswer"></div>
          </div>

          <div class="reference-section" v-if="references.length > 0">
            <div class="reference-title">
              <el-icon :size="13"><Collection /></el-icon>
              参考来源
            </div>
            <el-collapse class="reference-collapse">
              <el-collapse-item
                v-for="(ref, index) in references"
                :key="index"
                :title="`参考 ${index + 1}：${ref.title || '未知来源'}`"
              >
                <div class="reference-content">{{ ref.content }}</div>
                <div class="reference-meta">
                  <el-tag size="small" type="success" round>
                    相似度 {{ (ref.score * 100).toFixed(1) }}%
                  </el-tag>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </template>

        <!-- 相似度检索结果 -->
        <template v-if="searchMode === 'similar' && similarResults.length > 0">
          <div
            v-for="(result, index) in similarResults"
            :key="index"
            class="result-card"
          >
            <div class="result-header">
              <span class="result-index">{{ index + 1 }}</span>
              <span class="result-title">{{ result.title || '未知来源' }}</span>
              <el-tag size="small" type="success" round>
                相似度 {{ (result.score * 100).toFixed(1) }}%
              </el-tag>
            </div>
            <div class="result-content">{{ result.content }}</div>
          </div>
        </template>

        <!-- 空状态 -->
        <el-empty v-if="!hasResult && !loading && searched" description="未找到相关内容" />

        <!-- 初始状态 -->
        <div v-if="!searched && !loading" class="initial-state">
          <div class="initial-icon">
            <el-icon :size="36"><Search /></el-icon>
          </div>
          <h3>搜索你的知识库</h3>
          <p>支持 AI 智能问答和向量相似度检索两种模式</p>
          <div class="hot-keywords">
            <span class="label">热门搜索</span>
            <div class="keywords-wrap">
              <div
                v-for="kw in hotKeywords"
                :key="kw"
                class="keyword-chip"
                @click="quickSearch(kw)"
              >
                {{ kw }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Search, CircleCheck, ChatRound, Collection, Loading, Connection } from '@element-plus/icons-vue'
import { ragAsk } from '@/api/ai'
import { marked } from 'marked'
import { ElMessage } from 'element-plus'

const searchQuery = ref('')
const searchMode = ref('rag')
const topK = ref(3)
const loading = ref(false)
const searched = ref(false)
const answer = ref('')
const answerTime = ref(0)
const references = ref([])
const similarResults = ref([])

const hotKeywords = ['Spring Boot', 'Redis', 'MySQL', 'JVM', '多线程', '分布式锁']

const hasResult = computed(() => answer.value || references.value.length > 0)
const renderedAnswer = computed(() => marked(answer.value || ''))

const handleSearch = async () => {
  const query = searchQuery.value.trim()
  if (!query) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  loading.value = true
  searched.value = true
  const startTime = Date.now()
  try {
    if (searchMode.value === 'rag') {
      const res = await ragAsk(query, topK.value)
      answerTime.value = Date.now() - startTime
      answer.value = res.content || res.data?.content || '暂无回答'
      references.value = [
        { title: '相关文档 1', content: '这是与问题相关的文档片段内容...', score: 0.95 },
        { title: '相关文档 2', content: '这是另一个相关文档片段...', score: 0.87 }
      ]
    } else {
      similarResults.value = [
        { title: 'Spring Boot 自动装配原理', content: 'Spring Boot 的自动装配机制是通过 @EnableAutoConfiguration 注解实现的...', score: 0.92 },
        { title: 'Spring Boot Starter 机制', content: 'Starter 是 Spring Boot 的核心特性之一，它简化了 Maven 配置...', score: 0.85 },
        { title: 'Spring Boot 配置加载顺序', content: 'Spring Boot 配置加载有特定的优先级顺序...', score: 0.78 }
      ]
    }
  } catch {
    ElMessage.error('搜索失败，请重试')
  } finally {
    loading.value = false
  }
}

const quickSearch = (keyword) => {
  searchQuery.value = keyword
  handleSearch()
}
</script>

<style scoped lang="scss">
.search-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f8;
}

/* 页头 */
.page-header {
  display: flex;
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
      background: linear-gradient(135deg, #4facfe, #00f2fe);
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
}

/* 搜索区域 */
.search-container {
  flex: 1;
  padding: 40px 48px;
  overflow-y: auto;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb { background: rgba(0,0,0,0.12); border-radius: 2px; }
}

.search-hero {
  max-width: 820px;
  margin: 0 auto 40px;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  padding: 6px 6px 6px 18px;
  border: 2px solid transparent;
  transition: all 0.25s;

  &:focus-within {
    border-color: #667eea;
    box-shadow: 0 4px 24px rgba(102, 126, 234, 0.18);
  }

  .search-prefix-icon {
    color: #a0a3bd;
    flex-shrink: 0;
  }

  .search-input {
    flex: 1;
    border: none;
    outline: none;
    font-size: 16px;
    padding: 10px 14px;
    color: #303133;
    background: transparent;
    font-family: inherit;

    &::placeholder { color: #c0c4cc; }
  }

  .search-submit {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 22px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: #fff;
    border: none;
    border-radius: 12px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    font-family: inherit;
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 16px rgba(102, 126, 234, 0.4);
    }

    &.loading { opacity: 0.75; }
  }
}

.search-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 0 4px;
}

.mode-tabs {
  display: flex;
  gap: 6px;
  background: rgba(255,255,255,0.7);
  padding: 4px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);

  .mode-tab {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 7px 16px;
    border-radius: 9px;
    font-size: 13px;
    color: #909399;
    cursor: pointer;
    transition: all 0.2s;
    font-weight: 500;

    &.active {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      box-shadow: 0 3px 10px rgba(102, 126, 234, 0.3);
    }

    &:not(.active):hover {
      color: #667eea;
      background: rgba(102, 126, 234, 0.06);
    }
  }
}

.topk-select {
  width: 110px;

  :deep(.el-input__wrapper) {
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  }
}

/* 结果区域 */
.search-results {
  max-width: 900px;
  margin: 0 auto;
}

/* AI 回答卡片 */
.answer-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  margin-bottom: 20px;
  overflow: hidden;

  .answer-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14px 20px;
    background: linear-gradient(135deg, rgba(103,194,58,0.08), rgba(103,194,58,0.04));
    border-bottom: 1px solid rgba(103, 194, 58, 0.15);

    .answer-header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .answer-icon {
        width: 28px;
        height: 28px;
        background: linear-gradient(135deg, #67c23a, #5daf34);
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }

      span {
        font-size: 14px;
        font-weight: 600;
        color: #3e7c31;
      }
    }
  }

  .answer-content {
    padding: 20px 24px;
    line-height: 1.8;
    color: #303133;
    font-size: 14px;

    :deep(p) { margin: 10px 0; &:first-child { margin-top: 0; } &:last-child { margin-bottom: 0; } }
    :deep(ul), :deep(ol) { margin: 10px 0; padding-left: 24px; }
    :deep(li) { margin: 6px 0; }
    :deep(code) { background: rgba(102, 126, 234, 0.08); color: #667eea; padding: 2px 7px; border-radius: 5px; font-family: 'Fira Code', monospace; font-size: 13px; }
    :deep(pre) { background: #1a1a2e; padding: 16px; border-radius: 10px; overflow-x: auto; code { background: none; color: #d4d4d4; } }
    :deep(h1), :deep(h2), :deep(h3) { color: #1a1a2e; margin: 14px 0 8px; }
  }
}

/* 参考来源 */
.reference-section {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  margin-bottom: 20px;
  overflow: hidden;

  .reference-title {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 14px 20px;
    font-size: 13px;
    font-weight: 600;
    color: #606266;
    border-bottom: 1px solid rgba(0,0,0,0.05);
    background: #fafbff;
  }

  .reference-collapse {
    :deep(.el-collapse-item__header) {
      padding: 0 20px;
      font-size: 13px;
      color: #606266;
    }
    :deep(.el-collapse-item__content) {
      padding: 0 20px 16px;
    }
    :deep(.el-collapse) { border: none; }
  }

  .reference-content {
    padding: 12px 16px;
    background: #f8f9ff;
    border-radius: 10px;
    color: #606266;
    font-size: 13px;
    line-height: 1.7;
    margin-bottom: 10px;
    border-left: 3px solid #667eea;
  }

  .reference-meta { display: flex; justify-content: flex-end; }
}

/* 相似度结果卡片 */
.result-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  padding: 20px 24px;
  margin-bottom: 14px;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 28px rgba(0,0,0,0.09);
    border-color: rgba(102, 126, 234, 0.15);
  }

  .result-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;

    .result-index {
      width: 26px;
      height: 26px;
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 13px;
    }

    .result-title {
      flex: 1;
      font-weight: 600;
      color: #1a1a2e;
      font-size: 14px;
    }
  }

  .result-content {
    color: #606266;
    line-height: 1.7;
    font-size: 13px;
    padding-left: 38px;
  }
}

/* 初始状态 */
.initial-state {
  text-align: center;
  padding: 80px 0 40px;

  .initial-icon {
    width: 88px;
    height: 88px;
    background: linear-gradient(135deg, rgba(102,126,234,0.1), rgba(118,75,162,0.08));
    border-radius: 28px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #667eea;
    margin: 0 auto 24px;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.12);
  }

  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a2e;
    margin: 0 0 10px;
  }

  p {
    font-size: 14px;
    color: #a0a3bd;
    margin: 0 0 32px;
  }

  .hot-keywords {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;

    .label {
      font-size: 12px;
      color: #c0c4cc;
      font-weight: 500;
    }

    .keywords-wrap {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      justify-content: center;
    }

    .keyword-chip {
      padding: 7px 16px;
      background: #fff;
      border: 1px solid rgba(102, 126, 234, 0.2);
      border-radius: 20px;
      font-size: 13px;
      color: #667eea;
      cursor: pointer;
      transition: all 0.2s;
      box-shadow: 0 2px 8px rgba(0,0,0,0.04);

      &:hover {
        background: rgba(102, 126, 234, 0.08);
        border-color: #667eea;
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(102, 126, 234, 0.15);
      }
    }
  }
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
</style>
