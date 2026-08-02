import request from '@/utils/request'

// 查询用户会员卡列表
export function listApp_user_card(query) {
  return request({
    url: '/system/app_user_card/list',
    method: 'get',
    params: query
  })
}

// 查询用户会员卡详细
export function getApp_user_card(recordId) {
  return request({
    url: '/system/app_user_card/' + recordId,
    method: 'get'
  })
}

// 新增用户会员卡
export function addApp_user_card(data) {
  return request({
    url: '/system/app_user_card',
    method: 'post',
    data: data
  })
}

// 修改用户会员卡
export function updateApp_user_card(data) {
  return request({
    url: '/system/app_user_card',
    method: 'put',
    data: data
  })
}

// 删除用户会员卡
export function delApp_user_card(recordId) {
  return request({
    url: '/system/app_user_card/' + recordId,
    method: 'delete'
  })
}
