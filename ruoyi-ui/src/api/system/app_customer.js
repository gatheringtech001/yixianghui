import request from '@/utils/request'

// 查询客户资料列表
export function listApp_customer(query) {
  return request({
    url: '/system/app_customer/list',
    method: 'get',
    params: query
  })
}

// 查询客户资料详细
export function getApp_customer(customerId) {
  return request({
    url: '/system/app_customer/' + customerId,
    method: 'get'
  })
}

// 新增客户资料
export function addApp_customer(data) {
  return request({
    url: '/system/app_customer',
    method: 'post',
    data: data
  })
}

// 修改客户资料
export function updateApp_customer(data) {
  return request({
    url: '/system/app_customer',
    method: 'put',
    data: data
  })
}

// 删除客户资料
export function delApp_customer(customerId) {
  return request({
    url: '/system/app_customer/' + customerId,
    method: 'delete'
  })
}
