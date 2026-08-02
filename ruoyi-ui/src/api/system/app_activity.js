import request from '@/utils/request'

// 查询活动列表
export function listApp_activity(query) {
  return request({
    url: '/system/app_activity/list',
    method: 'get',
    params: query
  })
}

// 查询活动详细
export function getApp_activity(activityId) {
  return request({
    url: '/system/app_activity/' + activityId,
    method: 'get'
  })
}

// 新增活动
export function addApp_activity(data) {
  return request({
    url: '/system/app_activity',
    method: 'post',
    data: data
  })
}

// 修改活动
export function updateApp_activity(data) {
  return request({
    url: '/system/app_activity',
    method: 'put',
    data: data
  })
}

// 删除活动
export function delApp_activity(activityId) {
  return request({
    url: '/system/app_activity/' + activityId,
    method: 'delete'
  })
}
