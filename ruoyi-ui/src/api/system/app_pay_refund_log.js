import request from '@/utils/request'

// 查询退款记录列表
export function listApp_pay_refund_log(query) {
  return request({
    url: '/system/app_pay_refund_log/list',
    method: 'get',
    params: query
  })
}

// 查询退款记录详细
export function getApp_pay_refund_log(logId) {
  return request({
    url: '/system/app_pay_refund_log/' + logId,
    method: 'get'
  })
}

// 新增退款记录
export function addApp_pay_refund_log(data) {
  return request({
    url: '/system/app_pay_refund_log',
    method: 'post',
    data: data
  })
}

// 修改退款记录
export function updateApp_pay_refund_log(data) {
  return request({
    url: '/system/app_pay_refund_log',
    method: 'put',
    data: data
  })
}

// 删除退款记录
export function delApp_pay_refund_log(logId) {
  return request({
    url: '/system/app_pay_refund_log/' + logId,
    method: 'delete'
  })
}
