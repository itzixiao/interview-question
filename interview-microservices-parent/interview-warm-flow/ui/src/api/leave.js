import request from './request'

/**
 * 提交请假申请
 */
export function submitLeave(data) {
    return request({
        url: '/workflow/leave/submit',
        method: 'post',
        data
    })
}

/**
 * 审批请假申请
 */
export function approveLeave(params) {
    return request({
        url: '/workflow/leave/approve',
        method: 'post',
        params
    })
}

/**
 * 撤销请假申请
 */
export function cancelLeave(params) {
    return request({
        url: '/workflow/leave/cancel',
        method: 'post',
        params
    })
}

/**
 * 查询请假详情
 */
export function getLeaveDetail(params) {
    return request({
        url: '/workflow/leave/detail',
        method: 'get',
        params
    })
}
