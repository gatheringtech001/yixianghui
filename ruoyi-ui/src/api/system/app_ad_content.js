import request from '@/utils/request'

// 查询广告内容列表
export function listApp_ad_content(query) {
  return request({
    url: '/system/app_ad_content/list',
    method: 'get',
    params: query
  })
}

// 查询广告内容详细
export function getApp_ad_content(contentId) {
  return request({
    url: '/system/app_ad_content/' + contentId,
    method: 'get'
  })
}

// 新增广告内容
export function addApp_ad_content(data) {
  return request({
    url: '/system/app_ad_content',
    method: 'post',
    data: data
  })
}

// 修改广告内容
export function updateApp_ad_content(data) {
  return request({
    url: '/system/app_ad_content',
    method: 'put',
    data: data
  })
}

// 删除广告内容
export function delApp_ad_content(contentId) {
  return request({
    url: '/system/app_ad_content/' + contentId,
    method: 'delete'
  })
}
