import request from "../request.js";
const uri = 'mnp/app_user/'

// 获取金币（签到）
export function signIn(data) {
	return request(`${uri}sign_got_golden`, 'POST', data)
}
// 查询金币获取记录列表
export function signInList(params) {
	return request(`${uri}get_gold_log`, 'GET', params)
}
// 查询金币获取记录列表
export function signInRule(params) {
	return request(`${uri}get_golden_rule`, 'GET', params)
}
