import request from '@/utils/request'

// 查询用户信息列表
export function listApp_user_info(query) {
  return request({
    url: '/system/app_user_info/list',
    method: 'get',
    params: query
  })
}

// 查询用户信息详细
export function getApp_user_info(userId) {
  return request({
    url: '/system/app_user_info/' + userId,
    method: 'get'
  })
}

// 新增用户信息
export function addApp_user_info(data) {
  return request({
    url: '/system/app_user_info',
    method: 'post',
    data: data
  })
}

// 修改用户信息
export function updateApp_user_info(data) {
  return request({
    url: '/system/app_user_info',
    method: 'put',
    data: data
  })
}

// 删除用户信息
export function delApp_user_info(userId) {
  return request({
    url: '/system/app_user_info/' + userId,
    method: 'delete'
  })
}
