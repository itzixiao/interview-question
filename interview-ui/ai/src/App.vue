<template>
  <router-view />
</template>

<script setup>
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background-color: #f0f2f8;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #303133;
}

#app {
  height: 100vh;
}

/* 滚动条全局样式 */
::-webkit-scrollbar {
  width: 5px;
  height: 5px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(0,0,0,0.15);
  border-radius: 3px;
  &:hover { background: rgba(0,0,0,0.25); }
}

/* Element Plus 全局覆盖 */
.el-button {
  font-family: inherit;
}

.el-dialog {
  border-radius: 16px !important;
  overflow: hidden;

  .el-dialog__header {
    padding: 20px 24px 16px;
    font-weight: 600;
    border-bottom: 1px solid rgba(0,0,0,0.06);
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
    border-top: 1px solid rgba(0,0,0,0.06);
  }
}

.el-card {
  border-color: rgba(0,0,0,0.06) !important;
}

/* 加载动画 */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
  transform: translateY(6px);
}
</style>
