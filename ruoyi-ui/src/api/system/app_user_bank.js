import request from '@/utils/request'

// 查询用户银行卡列表
export function listApp_user_bank(query) {
  return request({
    url: '/system/app_user_bank/list',
    method: 'get',
    params: query
  })
}

// 查询用户银行卡详细
export function getApp_user_bank(bankId) {
  return request({
    url: '/system/app_user_bank/' + bankId,
    method: 'get'
  })
}

// 新增用户银行卡
export function addApp_user_bank(data) {
  return request({
    url: '/system/app_user_bank',
    method: 'post',
    data: data
  })
}

// 修改用户银行卡
export function updateApp_user_bank(data) {
  return request({
    url: '/system/app_user_bank',
    method: 'put',
    data: data
  })
}

// 删除用户银行卡
export function delApp_user_bank(bankId) {
  return request({
    url: '/system/app_user_bank/' + bankId,
    method: 'delete'
  })
}
