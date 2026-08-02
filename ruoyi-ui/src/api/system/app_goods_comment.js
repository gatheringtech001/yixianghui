import request from '@/utils/request'

// 查询商品评价列表
export function listApp_goods_comment(query) {
  return request({
    url: '/system/app_goods_comment/list',
    method: 'get',
    params: query
  })
}

// 查询商品评价详细
export function getApp_goods_comment(commentId) {
  return request({
    url: '/system/app_goods_comment/' + commentId,
    method: 'get'
  })
}

// 新增商品评价
export function addApp_goods_comment(data) {
  return request({
    url: '/system/app_goods_comment',
    method: 'post',
    data: data
  })
}

// 修改商品评价
export function updateApp_goods_comment(data) {
  return request({
    url: '/system/app_goods_comment',
    method: 'put',
    data: data
  })
}

// 删除商品评价
export function delApp_goods_comment(commentId) {
  return request({
    url: '/system/app_goods_comment/' + commentId,
    method: 'delete'
  })
}
