import request from '../request.js'

export function getNoticeInfo(noticeId) {
	return request(`mnp/index/get_notice/${noticeId}`, 'GET')
}