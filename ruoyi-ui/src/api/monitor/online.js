import request from '@/utils/request'

// 查询在线用户列表
export function list(query) {
  return request({
    url: '/monitor/online/list',
    method: 'get',
    params: query
  })
}

// 强退用户
export function forceLogout(tokenId) {
  return request({
    url: '/monitor/online/' + tokenId,
    method: 'delete'
  })
}

export function getUserStaticCount(query) {
  return request({
    url: '/monitor/online/getUserStaticCount',
    method: 'get',
    params: query
  })
}

// 微信访问统计独立加载，微信不可用不影响订单统计。
export function getMiniProgramTraffic() {
  return request({ url: '/monitor/online/getMiniProgramTraffic', method: 'get', timeout: 20000 })
}
