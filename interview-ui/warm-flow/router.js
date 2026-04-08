import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/workflow/designer',
    name: 'WorkflowDesigner',
    component: () => import('./WorkflowDesigner.vue'),
    meta: { title: '流程设计器' }
  },
  {
    path: '/workflow/instances',
    name: 'ProcessInstanceList',
    component: () => import('./ProcessInstanceList.vue'),
    meta: { title: '流程实例管理' }
  },
  {
    path: '/workflow/tasks',
    name: 'TodoTaskList',
    component: () => import('./TodoTaskList.vue'),
    meta: { title: '我的待办' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - Warm-Flow` : 'Warm-Flow'
  next()
})

export default router
