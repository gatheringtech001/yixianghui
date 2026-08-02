import request from '@/utils/request'

// 查询用户提现列表
export function listApp_user_cash(query) {
  return request({
    url: '/system/app_user_cash/list',
    method: 'get',
    params: query
  })
}

// 查询用户提现详细
export function getApp_user_cash(cashId) {
  return request({
    url: '/system/app_user_cash/' + cashId,
    method: 'get'
  })
}

// 新增用户提现
export function addApp_user_cash(data) {
  return request({
    url: '/system/app_user_cash',
    method: 'post',
    data: data
  })
}

// 修改用户提现
export function updateApp_user_cash(data) {
  return request({
    url: '/system/app_user_cash',
    method: 'put',
    data: data
  })
}

// 删除用户提现
export function delApp_user_cash(cashId) {
  return request({
    url: '/system/app_user_cash/' + cashId,
    method: 'delete'
  })
}
