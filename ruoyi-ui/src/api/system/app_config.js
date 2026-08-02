import request from '@/utils/request'

// 查询商城配置列表
export function listApp_config(query) {
  return request({
    url: '/system/app_config/list',
    method: 'get',
    params: query
  })
}

// 查询商城配置详细
export function getApp_config(configId) {
  return request({
    url: '/system/app_config/' + configId,
    method: 'get'
  })
}

// 新增商城配置
export function addApp_config(data) {
  return request({
    url: '/system/app_config',
    method: 'post',
    data: data
  })
}

// 修改商城配置
export function updateApp_config(data) {
  return request({
    url: '/system/app_config',
    method: 'put',
    data: data
  })
}

// 删除商城配置
export function delApp_config(configId) {
  return request({
    url: '/system/app_config/' + configId,
    method: 'delete'
  })
}
