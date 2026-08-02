import request from '@/utils/request'

// 查询钱包记录列表
export function listApp_user_money_log(query) {
  return request({
    url: '/system/app_user_money_log/list',
    method: 'get',
    params: query
  })
}

// 查询钱包记录详细
export function getApp_user_money_log(logId) {
  return request({
    url: '/system/app_user_money_log/' + logId,
    method: 'get'
  })
}

// 新增钱包记录
export function addApp_user_money_log(data) {
  return request({
    url: '/system/app_user_money_log',
    method: 'post',
    data: data
  })
}

// 修改钱包记录
export function updateApp_user_money_log(data) {
  return request({
    url: '/system/app_user_money_log',
    method: 'put',
    data: data
  })
}

// 删除钱包记录
export function delApp_user_money_log(logId) {
  return request({
    url: '/system/app_user_money_log/' + logId,
    method: 'delete'
  })
}
