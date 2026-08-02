import request from '@/utils/request'

// 查询快递公司列表
export function listApp_express(query) {
  return request({
    url: '/system/app_express/list',
    method: 'get',
    params: query
  })
}

// 查询快递公司详细
export function getApp_express(expressId) {
  return request({
    url: '/system/app_express/' + expressId,
    method: 'get'
  })
}

// 新增快递公司
export function addApp_express(data) {
  return request({
    url: '/system/app_express',
    method: 'post',
    data: data
  })
}

// 修改快递公司
export function updateApp_express(data) {
  return request({
    url: '/system/app_express',
    method: 'put',
    data: data
  })
}

// 删除快递公司
export function delApp_express(expressId) {
  return request({
    url: '/system/app_express/' + expressId,
    method: 'delete'
  })
}
