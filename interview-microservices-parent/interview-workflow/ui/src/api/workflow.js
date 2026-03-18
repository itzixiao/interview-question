import request from './request'

// ========== 请假接口 ==========
export function applyLeave(data) {
    return request.post('/leave/apply', data)
}

export function approveLeave(data) {
    return request.post('/leave/approve', data)
}

export function resubmitLeave(id, data) {
    return request.post(`/leave/resubmit/${id}`, data)
}

export function withdrawLeave(id) {
    return request.post(`/leave/withdraw/${id}`)
}

export function getMyLeaveList(params) {
    return request.get('/leave/my-list', {params})
}

export function getPendingLeaveList(params) {
    return request.get('/leave/pending-list', {params})
}

export function getLeaveDetail(id) {
    return request.get(`/leave/${id}`)
}

// ========== 报销接口 ==========
export function applyExpense(data) {
    return request.post('/expense/apply', data)
}

export function approveExpense(data) {
    return request.post('/expense/approve', data)
}

export function resubmitExpense(id, data) {
    return request.post(`/expense/resubmit/${id}`, data)
}

export function withdrawExpense(id) {
    return request.post(`/expense/withdraw/${id}`)
}

export function getMyExpenseList(params) {
    return request.get('/expense/my-list', {params})
}

export function getPendingExpenseList(params) {
    return request.get('/expense/pending-list', {params})
}

export function getExpenseDetail(id) {
    return request.get(`/expense/${id}`)
}

// ========== 流程历史接口 ==========
export function getProcessHistory(processInstanceId) {
    return request.get(`/process/history/${processInstanceId}`)
}

export function getProcessInstance(processInstanceId) {
    return request.get(`/process/instance/${processInstanceId}`)
}

export function getActivityTrace(processInstanceId) {
    return request.get(`/process/activity/${processInstanceId}`)
}

/**
 * 获取我的审批记录（我参与审批过的任务）
 */
export function getMyApprovalRecords(params) {
    return request.get('/process/my-approval-records', {params})
}

// ========== 系统接口 ==========
export function getDeptList() {
    return request.get('/system/dept/list')
}

export function getDashboardStats() {
    return request.get('/system/dashboard/stats')
}

// ========== 部门管理接口 ==========
export function createDept(data) {
    return request.post('/system/dept', data)
}

export function updateDept(id, data) {
    return request.put(`/system/dept/${id}`, data)
}

export function deleteDept(id) {
    return request.delete(`/system/dept/${id}`)
}

export function setDeptManager(deptId, userId) {
    return request.put(`/system/dept/${deptId}/manager/${userId}`)
}

// ========== 用户管理接口 ==========
export function getUserList(params) {
    return request.get('/auth/users', {params})
}

export function createUser(data) {
    return request.post('/auth/user', data)
}

export function updateUser(id, data) {
    return request.put(`/auth/user/${id}`, data)
}

export function deleteUser(id) {
    return request.delete(`/auth/user/${id}`)
}

export function toggleUserStatus(id, status) {
    return request.put(`/auth/user/${id}/status/${status}`)
}
