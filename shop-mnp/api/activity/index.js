import request from "../request.js";
const uri = 'mnp/index/'
const userUri = 'mnp/app_user/'


// 获取活动分类
export function getActivityCategoryList(params) {
	return request(`${uri}activity_category_list`, 'GET', params)
}
// 获取活动列表
export function getActivityList(params) {
	return request(`${uri}activity_list`, 'GET', params)
}
// 获取活动详情
export function getActivityInfo(activityId) {
	return request(`${uri}activity_info/${activityId}`, 'GET')
}
// 地址解析（用于地图导航）
export function geocodeAddress(address) {
	return request(`${uri}geocode_address`, 'GET', { address })
}


// 获取活动预约列表
export function getActivityOrderList() {
	return request(`${userUri}acticity_order/list`, 'GET')
}
// 获取活动预约详细信息
export function getActivityOrderInfo(orderId) {
	return request(`${userUri}acticity_order_info/${orderId}`, 'GET')
}
// 活动预约报名
export function addActivityOrder(data) {
	return request(`${userUri}acticity_order/add`, 'POST', data)
}
// 创建付费活动待支付订单
export function createActivityPendingOrder(data) {
	return request(`${userUri}acticity_order/create_pending`, 'POST', data)
}
// 活动订单支付
export function payActivityOrder(data) {
	return request(`${userUri}pay_activity_order`, 'POST', data)
}
// 客户端支付成功后主动同步活动订单支付结果
export function syncActivityOrderPay(orderId) {
	return request(`${userUri}sync_activity_order_pay?orderId=` + orderId, 'POST')
}
// 客户端主动同步活动订单退款结果
export function syncActivityOrderRefund(orderId) {
	return request(`${userUri}sync_activity_order_refund?orderId=` + orderId, 'POST')
}
// 活动预约取消
export function cancelActivityOrder(orderId) {
	return request(`${userUri}acticity_order/cancel?orderId=${orderId}`, 'POST')
}
// 活动预约修改
export function editActivityOrder(data) {
	return request(`${userUri}acticity_order/edit`, 'POST', data)
}