import request from '@/utils/request'

// 查询商品优惠券列表
export function listApp_goods_coupon(query) {
  return request({
    url: '/system/app_goods_coupon/list',
    method: 'get',
    params: query
  })
}

// 查询商品优惠券详细
export function getApp_goods_coupon(couponId) {
  return request({
    url: '/system/app_goods_coupon/' + couponId,
    method: 'get'
  })
}

// 新增商品优惠券
export function addApp_goods_coupon(data) {
  return request({
    url: '/system/app_goods_coupon',
    method: 'post',
    data: data
  })
}

// 修改商品优惠券
export function updateApp_goods_coupon(data) {
  return request({
    url: '/system/app_goods_coupon',
    method: 'put',
    data: data
  })
}

// 删除商品优惠券
export function delApp_goods_coupon(couponId) {
  return request({
    url: '/system/app_goods_coupon/' + couponId,
    method: 'delete'
  })
}
