import request from '@/utils/request'

// 查询商品订单列表
export function listApp_goods_order(query) {
  return request({
    url: '/system/app_goods_order/list',
    method: 'get',
    params: query
  })
}

// 查询商品订单详细
export function getApp_goods_order(orderId) {
  return request({
    url: '/system/app_goods_order/' + orderId,
    method: 'get'
  })
}

// 新增商品订单
export function addApp_goods_order(data) {
  return request({
    url: '/system/app_goods_order',
    method: 'post',
    data: data
  })
}

// 修改商品订单
export function updateApp_goods_order(data) {
  return request({
    url: '/system/app_goods_order',
    method: 'put',
    data: data
  })
}

// 推进旅居订单履约状态
export function updateTravelStatus(orderId, travelStatus) {
  return request({
    url: '/system/app_goods_order/' + orderId + '/travel-status',
    method: 'patch',
    data: { travelStatus }
  })
}

// 删除商品订单
export function delApp_goods_order(orderId) {
  return request({
    url: '/system/app_goods_order/' + orderId,
    method: 'delete'
  })
}
