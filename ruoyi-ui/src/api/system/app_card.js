import request from '@/utils/request'

// 查询会员卡列表
export function listApp_card(query) {
  return request({
    url: '/system/app_card/list',
    method: 'get',
    params: query
  })
}

// 查询会员卡详细
export function getApp_card(cardId) {
  return request({
    url: '/system/app_card/' + cardId,
    method: 'get'
  })
}

// 新增会员卡
export function addApp_card(data) {
  return request({
    url: '/system/app_card',
    method: 'post',
    data: data
  })
}

// 修改会员卡
export function updateApp_card(data) {
  return request({
    url: '/system/app_card',
    method: 'put',
    data: data
  })
}

// 删除会员卡
export function delApp_card(cardId) {
  return request({
    url: '/system/app_card/' + cardId,
    method: 'delete'
  })
}
