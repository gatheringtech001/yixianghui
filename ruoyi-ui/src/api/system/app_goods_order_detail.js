import request from '@/utils/request'

// 查询订单详细列表
export function listApp_goods_order_detail(query) {
  return request({
    url: '/system/app_goods_order_detail/list',
    method: 'get',
    params: query
  })
}

// 查询订单详细详细
export function getApp_goods_order_detail(detailId) {
  return request({
    url: '/system/app_goods_order_detail/' + detailId,
    method: 'get'
  })
}

// 新增订单详细
export function addApp_goods_order_detail(data) {
  return request({
    url: '/system/app_goods_order_detail',
    method: 'post',
    data: data
  })
}

// 修改订单详细
export function updateApp_goods_order_detail(data) {
  return request({
    url: '/system/app_goods_order_detail',
    method: 'put',
    data: data
  })
}

// 删除订单详细
export function delApp_goods_order_detail(detailId) {
  return request({
    url: '/system/app_goods_order_detail/' + detailId,
    method: 'delete'
  })
}
