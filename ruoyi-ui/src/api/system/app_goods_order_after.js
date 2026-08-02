import request from '@/utils/request'

// 查询订单商品售后列表
export function listApp_goods_order_after(query) {
  return request({
    url: '/system/app_goods_order_after/list',
    method: 'get',
    params: query
  })
}

// 查询订单商品售后详细
export function getApp_goods_order_after(afterId) {
  return request({
    url: '/system/app_goods_order_after/' + afterId,
    method: 'get'
  })
}

// 新增订单商品售后
export function addApp_goods_order_after(data) {
  return request({
    url: '/system/app_goods_order_after',
    method: 'post',
    data: data
  })
}

// 修改订单商品售后
export function updateApp_goods_order_after(data) {
  return request({
    url: '/system/app_goods_order_after',
    method: 'put',
    data: data
  })
}

// 删除订单商品售后
export function delApp_goods_order_after(afterId) {
  return request({
    url: '/system/app_goods_order_after/' + afterId,
    method: 'delete'
  })
}


// 修改订单商品售后审核
export function approvalApp_goods_order_after(data) {
  return request({
    url: '/system/app_goods_order_after/refundPrepay',
    method: 'post',
    data: data
  })
}
