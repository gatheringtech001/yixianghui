import request from '@/utils/request'

// 查询金币记录列表
export function listApp_user_gold_log(query) {
  return request({
    url: '/system/app_user_gold_log/list',
    method: 'get',
    params: query
  })
}

// 查询金币记录详细
export function getApp_user_gold_log(logId) {
  return request({
    url: '/system/app_user_gold_log/' + logId,
    method: 'get'
  })
}

// 新增金币记录
export function addApp_user_gold_log(data) {
  return request({
    url: '/system/app_user_gold_log',
    method: 'post',
    data: data
  })
}

// 修改金币记录
export function updateApp_user_gold_log(data) {
  return request({
    url: '/system/app_user_gold_log',
    method: 'put',
    data: data
  })
}

// 删除金币记录
export function delApp_user_gold_log(logId) {
  return request({
    url: '/system/app_user_gold_log/' + logId,
    method: 'delete'
  })
}
