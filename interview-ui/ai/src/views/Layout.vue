<template>
  <el-container class="layout">
    <el-aside width="260px" class="sidebar">
      <!-- Logo 区域 -->
      <div class="logo">
        <div class="logo-icon">
          <el-icon :size="24"><Cpu /></el-icon>
        </div>
        <div class="logo-text">
          <span class="logo-title">AI 知识库</span>
          <span class="logo-sub">Powered by Qwen</span>
        </div>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <div class="nav-section-title">功能导航</div>
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <div class="nav-item-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <span class="nav-item-label">{{ item.label }}</span>
          <div v-if="$route.path === item.path" class="nav-item-indicator" />
        </router-link>
      </nav>

      <!-- 底部状态 -->
      <div class="sidebar-footer">
        <div class="service-status">
          <span class="status-dot"></span>
          <span class="status-text">服务运行中</span>
        </div>
        <div class="model-badge">qwen-plus</div>
      </div>
    </el-aside>

    <el-main class="main-content">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { ChatRound, Collection, Search, Tools, Cpu } from '@element-plus/icons-vue'

const menuItems = [
  { path: '/chat',      label: 'AI 对话',   icon: ChatRound },
  { path: '/knowledge', label: '知识库',   icon: Collection },
  { path: '/search',    label: '知识检索',   icon: Search },
  { path: '/debug',     label: '接口调试',   icon: Tools },
]
</script>

<style scoped lang="scss">
.layout {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏 */
.sidebar {
  background: linear-gradient(160deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  border-right: 1px solid rgba(255,255,255,0.06);

  &::before {
    content: '';
    position: absolute;
    top: -80px;
    right: -80px;
    width: 200px;
    height: 200px;
    background: radial-gradient(circle, rgba(102, 126, 234, 0.25) 0%, transparent 70%);
    pointer-events: none;
  }
  &::after {
    content: '';
    position: absolute;
    bottom: 60px;
    left: -60px;
    width: 180px;
    height: 180px;
    background: radial-gradient(circle, rgba(118, 75, 162, 0.2) 0%, transparent 70%);
    pointer-events: none;
  }
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 20px 20px;
  border-bottom: 1px solid rgba(255,255,255,0.08);

  .logo-icon {
    width: 42px;
    height: 42px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
    flex-shrink: 0;
  }

  .logo-text {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .logo-title {
      font-size: 16px;
      font-weight: 700;
      color: #fff;
      letter-spacing: 0.5px;
    }

    .logo-sub {
      font-size: 11px;
      color: rgba(255,255,255,0.4);
      letter-spacing: 0.5px;
    }
  }
}

/* 导航 */
.sidebar-nav {
  flex: 1;
  padding: 20px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.nav-section-title {
  font-size: 11px;
  color: rgba(255,255,255,0.3);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding: 0 8px 10px;
  font-weight: 600;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 14px;
  border-radius: 12px;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  color: rgba(255,255,255,0.55);

  .nav-item-icon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: rgba(255,255,255,0.06);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.25s ease;
    flex-shrink: 0;

    .el-icon {
      font-size: 16px;
    }
  }

  .nav-item-label {
    font-size: 14px;
    font-weight: 500;
    flex: 1;
  }

  .nav-item-indicator {
    width: 4px;
    height: 20px;
    background: linear-gradient(180deg, #667eea, #764ba2);
    border-radius: 2px;
    position: absolute;
    right: -12px;
  }

  &:hover {
    background: rgba(255,255,255,0.07);
    color: rgba(255,255,255,0.85);

    .nav-item-icon {
      background: rgba(102, 126, 234, 0.2);
    }
  }

  &.active {
    background: linear-gradient(135deg, rgba(102,126,234,0.25), rgba(118,75,162,0.2));
    color: #fff;
    box-shadow: 0 2px 12px rgba(102, 126, 234, 0.2);

    .nav-item-icon {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: #fff;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }
  }
}

/* 底部 */
.sidebar-footer {
  padding: 16px 20px;
  border-top: 1px solid rgba(255,255,255,0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 1;

  .service-status {
    display: flex;
    align-items: center;
    gap: 8px;

    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #67c23a;
      box-shadow: 0 0 8px rgba(103, 194, 58, 0.6);
      animation: pulse 2s infinite;
    }

    .status-text {
      font-size: 12px;
      color: rgba(255,255,255,0.5);
    }
  }

  .model-badge {
    font-size: 11px;
    color: rgba(255,255,255,0.35);
    background: rgba(255,255,255,0.07);
    padding: 3px 8px;
    border-radius: 20px;
    border: 1px solid rgba(255,255,255,0.1);
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.6; transform: scale(0.85); }
}

/* 主内容区 */
.main-content {
  padding: 0;
  background: #f0f2f8;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
</style>
