import request from '@/utils/request'

// 查询商品分类列表
export function listApp_goods_category(query) {
  return request({
    url: '/system/app_goods_category/list',
    method: 'get',
    params: query
  })
}

// 查询商品分类详细
export function getApp_goods_category(categoryId) {
  return request({
    url: '/system/app_goods_category/' + categoryId,
    method: 'get'
  })
}

// 新增商品分类
export function addApp_goods_category(data) {
  return request({
    url: '/system/app_goods_category',
    method: 'post',
    data: data
  })
}

// 修改商品分类
export function updateApp_goods_category(data) {
  return request({
    url: '/system/app_goods_category',
    method: 'put',
    data: data
  })
}

// 删除商品分类
export function delApp_goods_category(categoryId) {
  return request({
    url: '/system/app_goods_category/' + categoryId,
    method: 'delete'
  })
}
