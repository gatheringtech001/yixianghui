import request from '@/utils/request'

// 查询康养顾问列表
export function listApp_consultant(query) {
  return request({
    url: '/system/app_consultant/list',
    method: 'get',
    params: query
  })
}

// 查询康养顾问详细
export function getApp_consultant(consultantId) {
  return request({
    url: '/system/app_consultant/' + consultantId,
    method: 'get'
  })
}

// 新增康养顾问
export function addApp_consultant(data) {
  return request({
    url: '/system/app_consultant',
    method: 'post',
    data: data
  })
}

// 修改康养顾问
export function updateApp_consultant(data) {
  return request({
    url: '/system/app_consultant',
    method: 'put',
    data: data
  })
}

// 删除康养顾问
export function delApp_consultant(consultantId) {
  return request({
    url: '/system/app_consultant/' + consultantId,
    method: 'delete'
  })
}
