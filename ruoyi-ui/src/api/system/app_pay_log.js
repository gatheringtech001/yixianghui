import request from '@/utils/request'

// 查询支付记录列表
export function listApp_pay_log(query) {
  return request({
    url: '/system/app_pay_log/list',
    method: 'get',
    params: query
  })
}

// 查询支付记录详细
export function getApp_pay_log(logId) {
  return request({
    url: '/system/app_pay_log/' + logId,
    method: 'get'
  })
}

// 新增支付记录
export function addApp_pay_log(data) {
  return request({
    url: '/system/app_pay_log',
    method: 'post',
    data: data
  })
}

// 修改支付记录
export function updateApp_pay_log(data) {
  return request({
    url: '/system/app_pay_log',
    method: 'put',
    data: data
  })
}

// 删除支付记录
export function delApp_pay_log(logId) {
  return request({
    url: '/system/app_pay_log/' + logId,
    method: 'delete'
  })
}
