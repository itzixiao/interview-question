import {createRouter, createWebHistory} from 'vue-router'
import {useAuthStore} from '@/store/auth'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/auth/Login.vue'),
        meta: {public: true}
    },
    {
        path: '/',
        component: () => import('@/components/Layout.vue'),
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/dashboard/Dashboard.vue'),
                meta: {title: '工作台', icon: 'HomeFilled'}
            },
            {
                path: 'leave/apply',
                name: 'LeaveApply',
                component: () => import('@/views/leave/LeaveApply.vue'),
                meta: {title: '请假申请', icon: 'Calendar'}
            },
            {
                path: 'leave/my-list',
                name: 'LeaveMyList',
                component: () => import('@/views/leave/LeaveMyList.vue'),
                meta: {title: '我的请假', icon: 'List'}
            },
            {
                path: 'leave/pending',
                name: 'LeavePending',
                component: () => import('@/views/leave/LeavePendingList.vue'),
                meta: {title: '待审批请假', icon: 'Clock'}
            },
            {
                path: 'expense/apply',
                name: 'ExpenseApply',
                component: () => import('@/views/expense/ExpenseApply.vue'),
                meta: {title: '报销申请', icon: 'Wallet'}
            },
            {
                path: 'expense/my-list',
                name: 'ExpenseMyList',
                component: () => import('@/views/expense/ExpenseMyList.vue'),
                meta: {title: '我的报销', icon: 'Document'}
            },
            {
                path: 'expense/pending',
                name: 'ExpensePending',
                component: () => import('@/views/expense/ExpensePendingList.vue'),
                meta: {title: '待审批报销', icon: 'Bell'}
            },
            {
                path: 'approval/records',
                name: 'ApprovalRecords',
                component: () => import('@/views/approval/ApprovalRecords.vue'),
                meta: {title: '审批记录', icon: 'Finished'}
            },
            {
                path: 'org',
                name: 'Organization',
                component: () => import('@/views/org/Organization.vue'),
                meta: {title: '组织架构', icon: 'OfficeBuilding'}
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫 - 未登录跳转到登录页
router.beforeEach((to, from, next) => {
    const authStore = useAuthStore()
    const hasToken = authStore.token || localStorage.getItem('token')

    console.log('[Router Guard] to:', to.path, 'hasToken:', !!hasToken, 'isPublic:', !!to.meta.public)

    if (!to.meta.public && !hasToken) {
        console.log('[Router Guard] 未登录，跳转到登录页')
        next({name: 'Login', query: {redirect: to.fullPath}})
    } else if (to.path === '/login' && hasToken) {
        // 已登录用户访问登录页，跳转到首页
        console.log('[Router Guard] 已登录，跳转到首页')
        next('/dashboard')
    } else {
        next()
    }
})

export default router
