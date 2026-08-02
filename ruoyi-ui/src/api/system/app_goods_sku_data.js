import request from '@/utils/request'

// 查询型号信息列表
export function listApp_goods_sku_data(query) {
  return request({
    url: '/system/app_goods_sku_data/list',
    method: 'get',
    params: query
  })
}

// 查询型号信息详细
export function getApp_goods_sku_data(dataId) {
  return request({
    url: '/system/app_goods_sku_data/' + dataId,
    method: 'get'
  })
}

// 新增型号信息
export function addApp_goods_sku_data(data) {
  return request({
    url: '/system/app_goods_sku_data',
    method: 'post',
    data: data
  })
}

// 修改型号信息
export function updateApp_goods_sku_data(data) {
  return request({
    url: '/system/app_goods_sku_data',
    method: 'put',
    data: data
  })
}

// 删除型号信息
export function delApp_goods_sku_data(dataId) {
  return request({
    url: '/system/app_goods_sku_data/' + dataId,
    method: 'delete'
  })
}
