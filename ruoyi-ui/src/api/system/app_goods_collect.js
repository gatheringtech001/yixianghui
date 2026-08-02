import request from '@/utils/request'

// 查询商品收藏列表
export function listApp_goods_collect(query) {
  return request({
    url: '/system/app_goods_collect/list',
    method: 'get',
    params: query
  })
}

// 查询商品收藏详细
export function getApp_goods_collect(collectId) {
  return request({
    url: '/system/app_goods_collect/' + collectId,
    method: 'get'
  })
}

// 新增商品收藏
export function addApp_goods_collect(data) {
  return request({
    url: '/system/app_goods_collect',
    method: 'post',
    data: data
  })
}

// 修改商品收藏
export function updateApp_goods_collect(data) {
  return request({
    url: '/system/app_goods_collect',
    method: 'put',
    data: data
  })
}

// 删除商品收藏
export function delApp_goods_collect(collectId) {
  return request({
    url: '/system/app_goods_collect/' + collectId,
    method: 'delete'
  })
}
