import request from '@/utils/request'

export function getCharData() {
  return request({
    url: '/monitor/online/fechCharData',
    method: 'get'
  })
}
