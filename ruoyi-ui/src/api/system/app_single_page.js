import request from '@/utils/request'

// 查询单页文章列表
export function listApp_single_page(query) {
  return request({
    url: '/system/app_single_page/list',
    method: 'get',
    params: query
  })
}

// 查询单页文章详细
export function getApp_single_page(pageId) {
  return request({
    url: '/system/app_single_page/' + pageId,
    method: 'get'
  })
}

// 新增单页文章
export function addApp_single_page(data) {
  return request({
    url: '/system/app_single_page',
    method: 'post',
    data: data
  })
}

// 修改单页文章
export function updateApp_single_page(data) {
  return request({
    url: '/system/app_single_page',
    method: 'put',
    data: data
  })
}

// 删除单页文章
export function delApp_single_page(pageId) {
  return request({
    url: '/system/app_single_page/' + pageId,
    method: 'delete'
  })
}
