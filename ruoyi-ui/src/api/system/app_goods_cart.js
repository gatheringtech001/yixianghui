import request from '@/utils/request'

// 查询用户购物车列表
export function listApp_goods_cart(query) {
  return request({
    url: '/system/app_goods_cart/list',
    method: 'get',
    params: query
  })
}

// 查询用户购物车详细
export function getApp_goods_cart(cartId) {
  return request({
    url: '/system/app_goods_cart/' + cartId,
    method: 'get'
  })
}

// 新增用户购物车
export function addApp_goods_cart(data) {
  return request({
    url: '/system/app_goods_cart',
    method: 'post',
    data: data
  })
}

// 修改用户购物车
export function updateApp_goods_cart(data) {
  return request({
    url: '/system/app_goods_cart',
    method: 'put',
    data: data
  })
}

// 删除用户购物车
export function delApp_goods_cart(cartId) {
  return request({
    url: '/system/app_goods_cart/' + cartId,
    method: 'delete'
  })
}
