import request from '@/utils/request'

// 查询商品列表
export function listApp_goods(query) {
  return request({
    url: '/system/app_goods/list',
    method: 'get',
    params: query
  })
}

// 查询商品详细
export function getApp_goods(goodsId) {
  return request({
    url: '/system/app_goods/' + goodsId,
    method: 'get'
  })
}

// 新增商品
export function addApp_goods(data) {
  return request({
    url: '/system/app_goods',
    method: 'post',
    data: data
  })
}

// 修改商品
export function updateApp_goods(data) {
  return request({
    url: '/system/app_goods',
    method: 'put',
    data: data
  })
}

// 删除商品
export function delApp_goods(goodsId) {
  return request({
    url: '/system/app_goods/' + goodsId,
    method: 'delete'
  })
}
