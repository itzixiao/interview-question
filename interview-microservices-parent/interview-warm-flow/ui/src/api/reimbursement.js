import request from './request'

/**
 * 提交报销申请
 */
export function submitReimbursement(data) {
    return request({
        url: '/workflow/reimbursement/submit',
        method: 'post',
        data
    })
}

/**
 * 审批报销申请
 */
export function approveReimbursement(params) {
    return request({
        url: '/workflow/reimbursement/approve',
        method: 'post',
        params
    })
}

/**
 * 撤销报销申请
 */
export function cancelReimbursement(params) {
    return request({
        url: '/workflow/reimbursement/cancel',
        method: 'post',
        params
    })
}

/**
 * 查询报销详情
 */
export function getReimbursementDetail(params) {
    return request({
        url: '/workflow/reimbursement/detail',
        method: 'get',
        params
    })
}
