import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import {login} from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

    const isLoggedIn = computed(() => !!token.value)
    const roles = computed(() => userInfo.value?.roles?.map(r => r.authority) || [])

    /**
     * 登录 - 成功后打印 token 和用户角色信息
     */
    async function loginAction(username, password) {
        const res = await login({username, password})
        const data = res.data

        token.value = data.token
        userInfo.value = data
        localStorage.setItem('token', data.token)
        localStorage.setItem('userInfo', JSON.stringify(data))

        console.log('=== 用户登录成功 ===')
        console.log('用户名:', data.username, '| 真实姓名:', data.realName)
        console.log('角色列表:', data.roles?.map(r => r.authority).join(', '))
        console.log('Token:', data.token.substring(0, 30) + '...')
    }

    function logout() {
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    function hasRole(role) {
        return roles.value.some(r => r.includes(role))
    }

    return {
        token,
        userInfo,
        isLoggedIn,
        roles,
        loginAction,
        logout,
        hasRole
    }
})
