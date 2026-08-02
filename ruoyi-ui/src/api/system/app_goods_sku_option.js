import request from '@/utils/request'

// 查询属性选项列表
export function listApp_goods_sku_option(query) {
  return request({
    url: '/system/app_goods_sku_option/list',
    method: 'get',
    params: query
  })
}

// 查询属性选项详细
export function getApp_goods_sku_option(optionId) {
  return request({
    url: '/system/app_goods_sku_option/' + optionId,
    method: 'get'
  })
}

// 新增属性选项
export function addApp_goods_sku_option(data) {
  return request({
    url: '/system/app_goods_sku_option',
    method: 'post',
    data: data
  })
}

// 修改属性选项
export function updateApp_goods_sku_option(data) {
  return request({
    url: '/system/app_goods_sku_option',
    method: 'put',
    data: data
  })
}

// 删除属性选项
export function delApp_goods_sku_option(optionId) {
  return request({
    url: '/system/app_goods_sku_option/' + optionId,
    method: 'delete'
  })
}
