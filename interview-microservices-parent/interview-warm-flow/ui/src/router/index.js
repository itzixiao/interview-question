import {createRouter, createWebHistory} from 'vue-router'

const routes = [
    {
        path: '/',
        name: 'Home',
        component: () => import('@/views/Home.vue')
    },
    {
        path: '/leave',
        name: 'LeaveApproval',
        component: () => import('@/views/LeaveApproval.vue')
    },
    {
        path: '/reimbursement',
        name: 'ReimbursementApproval',
        component: () => import('@/views/ReimbursementApproval.vue')
    },
    {
        path: '/designer',
        name: 'FlowDesigner',
        component: () => import('@/views/FlowDesigner.vue')
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

export default router
