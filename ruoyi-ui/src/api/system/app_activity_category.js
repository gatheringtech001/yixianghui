import request from '@/utils/request'

// 查询活动分类列表
export function listApp_activity_category(query) {
  return request({
    url: '/system/app_activity_category/list',
    method: 'get',
    params: query
  })
}

// 查询活动分类详细
export function getApp_activity_category(categoryId) {
  return request({
    url: '/system/app_activity_category/' + categoryId,
    method: 'get'
  })
}

// 新增活动分类
export function addApp_activity_category(data) {
  return request({
    url: '/system/app_activity_category',
    method: 'post',
    data: data
  })
}

// 修改活动分类
export function updateApp_activity_category(data) {
  return request({
    url: '/system/app_activity_category',
    method: 'put',
    data: data
  })
}

// 删除活动分类
export function delApp_activity_category(categoryId) {
  return request({
    url: '/system/app_activity_category/' + categoryId,
    method: 'delete'
  })
}
