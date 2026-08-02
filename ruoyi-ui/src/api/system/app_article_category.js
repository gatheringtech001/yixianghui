import request from '@/utils/request'

// 查询内容分类列表
export function listApp_article_category(query) {
  return request({
    url: '/system/app_article_category/list',
    method: 'get',
    params: query
  })
}

// 查询内容分类详细
export function getApp_article_category(categoryId) {
  return request({
    url: '/system/app_article_category/' + categoryId,
    method: 'get'
  })
}

// 新增内容分类
export function addApp_article_category(data) {
  return request({
    url: '/system/app_article_category',
    method: 'post',
    data: data
  })
}

// 修改内容分类
export function updateApp_article_category(data) {
  return request({
    url: '/system/app_article_category',
    method: 'put',
    data: data
  })
}

// 删除内容分类
export function delApp_article_category(categoryId) {
  return request({
    url: '/system/app_article_category/' + categoryId,
    method: 'delete'
  })
}
