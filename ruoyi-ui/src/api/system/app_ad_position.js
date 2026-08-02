import request from '@/utils/request'

// 查询广告管理列表
export function listApp_ad_position(query) {
  return request({
    url: '/system/app_ad_position/list',
    method: 'get',
    params: query
  })
}

// 查询广告管理详细
export function getApp_ad_position(positionId) {
  return request({
    url: '/system/app_ad_position/' + positionId,
    method: 'get'
  })
}

// 新增广告管理
export function addApp_ad_position(data) {
  return request({
    url: '/system/app_ad_position',
    method: 'post',
    data: data
  })
}

// 修改广告管理
export function updateApp_ad_position(data) {
  return request({
    url: '/system/app_ad_position',
    method: 'put',
    data: data
  })
}

// 删除广告管理
export function delApp_ad_position(positionId) {
  return request({
    url: '/system/app_ad_position/' + positionId,
    method: 'delete'
  })
}
