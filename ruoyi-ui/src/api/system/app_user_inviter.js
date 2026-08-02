import request from '@/utils/request'

// 查询邀请记录列表
export function listApp_user_inviter(query) {
  return request({
    url: '/system/app_user_inviter/list',
    method: 'get',
    params: query
  })
}

// 查询邀请记录详细
export function getApp_user_inviter(inviterId) {
  return request({
    url: '/system/app_user_inviter/' + inviterId,
    method: 'get'
  })
}

// 新增邀请记录
export function addApp_user_inviter(data) {
  return request({
    url: '/system/app_user_inviter',
    method: 'post',
    data: data
  })
}

// 修改邀请记录
export function updateApp_user_inviter(data) {
  return request({
    url: '/system/app_user_inviter',
    method: 'put',
    data: data
  })
}

// 删除邀请记录
export function delApp_user_inviter(inviterId) {
  return request({
    url: '/system/app_user_inviter/' + inviterId,
    method: 'delete'
  })
}
