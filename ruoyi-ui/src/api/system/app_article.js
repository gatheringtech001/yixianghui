import request from '@/utils/request'

// 查询图文内容列表
export function listApp_article(query) {
  return request({
    url: '/system/app_article/list',
    method: 'get',
    params: query
  })
}

// 查询图文内容详细
export function getApp_article(articleId) {
  return request({
    url: '/system/app_article/' + articleId,
    method: 'get'
  })
}

// 新增图文内容
export function addApp_article(data) {
  return request({
    url: '/system/app_article',
    method: 'post',
    data: data
  })
}

// 修改图文内容
export function updateApp_article(data) {
  return request({
    url: '/system/app_article',
    method: 'put',
    data: data
  })
}

// 删除图文内容
export function delApp_article(articleId) {
  return request({
    url: '/system/app_article/' + articleId,
    method: 'delete'
  })
}
