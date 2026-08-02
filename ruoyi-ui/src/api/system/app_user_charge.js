import request from '@/utils/request'

// 查询用户充值列表
export function listApp_user_charge(query) {
  return request({
    url: '/system/app_user_charge/list',
    method: 'get',
    params: query
  })
}

// 查询用户充值详细
export function getApp_user_charge(chargeId) {
  return request({
    url: '/system/app_user_charge/' + chargeId,
    method: 'get'
  })
}

// 新增用户充值
export function addApp_user_charge(data) {
  return request({
    url: '/system/app_user_charge',
    method: 'post',
    data: data
  })
}

// 修改用户充值
export function updateApp_user_charge(data) {
  return request({
    url: '/system/app_user_charge',
    method: 'put',
    data: data
  })
}

// 删除用户充值
export function delApp_user_charge(chargeId) {
  return request({
    url: '/system/app_user_charge/' + chargeId,
    method: 'delete'
  })
}
