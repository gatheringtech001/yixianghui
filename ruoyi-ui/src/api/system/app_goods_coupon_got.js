import request from '@/utils/request'

// 查询优惠券领取记录列表
export function listApp_goods_coupon_got(query) {
  return request({
    url: '/system/app_goods_coupon_got/list',
    method: 'get',
    params: query
  })
}

// 查询优惠券领取记录详细
export function getApp_goods_coupon_got(gotId) {
  return request({
    url: '/system/app_goods_coupon_got/' + gotId,
    method: 'get'
  })
}

// 新增优惠券领取记录
export function addApp_goods_coupon_got(data) {
  return request({
    url: '/system/app_goods_coupon_got',
    method: 'post',
    data: data
  })
}

// 修改优惠券领取记录
export function updateApp_goods_coupon_got(data) {
  return request({
    url: '/system/app_goods_coupon_got',
    method: 'put',
    data: data
  })
}

// 删除优惠券领取记录
export function delApp_goods_coupon_got(gotId) {
  return request({
    url: '/system/app_goods_coupon_got/' + gotId,
    method: 'delete'
  })
}
