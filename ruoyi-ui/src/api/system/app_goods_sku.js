import request from '@/utils/request'

// 查询商品属性列表
export function listApp_goods_sku(query) {
  return request({
    url: '/system/app_goods_sku/list',
    method: 'get',
    params: query
  })
}

// 查询商品属性详细
export function getApp_goods_sku(skuId) {
  return request({
    url: '/system/app_goods_sku/' + skuId,
    method: 'get'
  })
}

// 新增商品属性
export function addApp_goods_sku(data) {
  return request({
    url: '/system/app_goods_sku',
    method: 'post',
    data: data
  })
}

// 修改商品属性
export function updateApp_goods_sku(data) {
  return request({
    url: '/system/app_goods_sku',
    method: 'put',
    data: data
  })
}

// 删除商品属性
export function delApp_goods_sku(skuId) {
  return request({
    url: '/system/app_goods_sku/' + skuId,
    method: 'delete'
  })
}
