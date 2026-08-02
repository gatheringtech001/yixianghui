import request from '@/utils/request'

// 查询活动预约列表
export function listApp_activity_order(query) {
  return request({
    url: '/system/app_activity_order/list',
    method: 'get',
    params: query
  })
}

// 查询活动预约详细
export function getApp_activity_order(orderId) {
  return request({
    url: '/system/app_activity_order/' + orderId,
    method: 'get'
  })
}

// 新增活动预约
export function addApp_activity_order(data) {
  return request({
    url: '/system/app_activity_order',
    method: 'post',
    data: data
  })
}

// 修改活动预约
export function updateApp_activity_order(data) {
  return request({
    url: '/system/app_activity_order',
    method: 'put',
    data: data
  })
}

// 删除活动预约
export function delApp_activity_order(orderId) {
  return request({
    url: '/system/app_activity_order/' + orderId,
    method: 'delete'
  })
}
