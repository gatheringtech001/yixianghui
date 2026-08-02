import request from '@/utils/request'

// 查询行政区域列表
export function listApp_area(query) {
  return request({
    url: '/system/app_area/list',
    method: 'get',
    params: query
  })
}

// 查询行政区域详细
export function getApp_area(areaId) {
  return request({
    url: '/system/app_area/' + areaId,
    method: 'get'
  })
}

// 新增行政区域
export function addApp_area(data) {
  return request({
    url: '/system/app_area',
    method: 'post',
    data: data
  })
}

// 修改行政区域
export function updateApp_area(data) {
  return request({
    url: '/system/app_area',
    method: 'put',
    data: data
  })
}

// 删除行政区域
export function delApp_area(areaId) {
  return request({
    url: '/system/app_area/' + areaId,
    method: 'delete'
  })
}
