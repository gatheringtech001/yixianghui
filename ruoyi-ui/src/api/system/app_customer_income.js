import request from '@/utils/request'

// 查询收入明细列表
export function listApp_customer_income(query) {
  return request({
    url: '/system/app_customer_income/list',
    method: 'get',
    params: query
  })
}

// 统计收入明细
export function statApp_customer_income(query) {
  return request({
    url: '/system/app_customer_income/stat',
    method: 'get',
    params: query
  })
}

// 查询收入明细详细
export function getApp_customer_income(incomeId) {
  return request({
    url: '/system/app_customer_income/' + incomeId,
    method: 'get'
  })
}

// 新增收入明细
export function addApp_customer_income(data) {
  return request({
    url: '/system/app_customer_income',
    method: 'post',
    data: data
  })
}

// 修改收入明细
export function updateApp_customer_income(data) {
  return request({
    url: '/system/app_customer_income',
    method: 'put',
    data: data
  })
}

// 删除收入明细
export function delApp_customer_income(incomeId) {
  return request({
    url: '/system/app_customer_income/' + incomeId,
    method: 'delete'
  })
}
