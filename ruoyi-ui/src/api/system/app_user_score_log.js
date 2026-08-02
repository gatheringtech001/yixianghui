import request from '@/utils/request'

// 查询积分记录列表
export function listApp_user_score_log(query) {
  return request({
    url: '/system/app_user_score_log/list',
    method: 'get',
    params: query
  })
}

// 查询积分记录详细
export function getApp_user_score_log(logId) {
  return request({
    url: '/system/app_user_score_log/' + logId,
    method: 'get'
  })
}

// 新增积分记录
export function addApp_user_score_log(data) {
  return request({
    url: '/system/app_user_score_log',
    method: 'post',
    data: data
  })
}

// 修改积分记录
export function updateApp_user_score_log(data) {
  return request({
    url: '/system/app_user_score_log',
    method: 'put',
    data: data
  })
}

// 删除积分记录
export function delApp_user_score_log(logId) {
  return request({
    url: '/system/app_user_score_log/' + logId,
    method: 'delete'
  })
}
