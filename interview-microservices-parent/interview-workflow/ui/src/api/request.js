import axios from 'axios'
import {ElMessage} from 'element-plus'
import {useAuthStore} from '@/store/auth'

const request = axios.create({
    baseURL: '/api',
    timeout: 15000
})

// 调试：打印请求配置
request.interceptors.request.use(
    config => {
        console.log('[Request]', config.method?.toUpperCase(), config.url, config.data)
        const authStore = useAuthStore()
        if (authStore.token) {
            config.headers.Authorization = `Bearer ${authStore.token}`
        }
        return config
    },
    error => Promise.reject(error)
)


// 响应拦截 - 统一错误处理
request.interceptors.response.use(
    response => {
        const res = response.data
        console.log('[Response]', response.config.url, res)
        if (res.code !== 200) {
            ElMessage.error(res.message || '请求失败')
            return Promise.reject(new Error(res.message))
        }
        return res
    },
    error => {
        console.error('[Response Error]', error.config?.url, error.message, error.response?.status)
        if (error.response?.status === 401) {
            const authStore = useAuthStore()
            authStore.logout()
            window.location.href = '/login'
            ElMessage.error('登录已过期，请重新登录')
        } else if (error.response?.status === 403) {
            ElMessage.error('权限不足')
        } else if (error.code === 'ECONNREFUSED' || error.message?.includes('Network Error')) {
            ElMessage.error('无法连接到后端服务，请确认后端已启动')
        } else {
            ElMessage.error(error.response?.data?.message || error.message || '网络错误')
        }
        return Promise.reject(error)
    }
)

export default request
