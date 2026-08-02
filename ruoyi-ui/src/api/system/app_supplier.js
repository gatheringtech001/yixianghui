import request from '@/utils/request'

// 查询供应商列表
export function listApp_supplier(query) {
  return request({
    url: '/system/app_supplier/list',
    method: 'get',
    params: query
  })
}

// 查询供应商详细
export function getApp_supplier(supplierId) {
  return request({
    url: '/system/app_supplier/' + supplierId,
    method: 'get'
  })
}

// 新增供应商
export function addApp_supplier(data) {
  return request({
    url: '/system/app_supplier',
    method: 'post',
    data: data
  })
}

// 修改供应商
export function updateApp_supplier(data) {
  return request({
    url: '/system/app_supplier',
    method: 'put',
    data: data
  })
}

// 删除供应商
export function delApp_supplier(supplierId) {
  return request({
    url: '/system/app_supplier/' + supplierId,
    method: 'delete'
  })
}
