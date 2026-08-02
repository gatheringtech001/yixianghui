import request from '@/utils/request'

// 查询用户地址列表
export function listApp_user_address(query) {
  return request({
    url: '/system/app_user_address/list',
    method: 'get',
    params: query
  })
}

// 查询用户地址详细
export function getApp_user_address(addressId) {
  return request({
    url: '/system/app_user_address/' + addressId,
    method: 'get'
  })
}

// 新增用户地址
export function addApp_user_address(data) {
  return request({
    url: '/system/app_user_address',
    method: 'post',
    data: data
  })
}

// 修改用户地址
export function updateApp_user_address(data) {
  return request({
    url: '/system/app_user_address',
    method: 'put',
    data: data
  })
}

// 删除用户地址
export function delApp_user_address(addressId) {
  return request({
    url: '/system/app_user_address/' + addressId,
    method: 'delete'
  })
}
