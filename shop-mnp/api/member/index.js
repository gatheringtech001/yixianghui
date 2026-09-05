import request from "../request.js";
const uri = 'mnp/app_user/'
const api = 'mnp/index/'

// 获取会员规则
export function getMemberRule(params) {
	return request('system/app_member_rule', 'GET')
}
// 获取会员卡列表
export function getMemberCardsList(params) {
	return request(`${api}card_list`, 'GET', params)
}
// 获取会员卡明细
export function getMemberCardInfo(cardId) {
	return request(`${api}card_info/${cardId}}`, 'GET')
}

// 购买会员卡
export function prepayCardOrder(data) {
	return request(`${uri}user_card/prepay`, 'POST', data)
}
// 支付会员卡
export function payCardOrder(data) {
	return request(`${uri}user_card/pay`, 'POST', data)
}
// 取消未支付会员卡
export function cancelUserCard(recordId) {
	return request(`${uri}user_card/cancel?recordId=` + recordId, 'POST')
}
// 会员卡退款
export function refundUserCard(recordId) {
	return request(`${uri}user_card/refund?recordId=` + recordId, 'POST')
}

// 商品收藏-新增
export function goodsCollect(data) {
	return request(`${uri}collect/add`, 'POST', data)
}
// 商品收藏-取消 {collectId }
export function deleteCollect(data) {
	return request(`${uri}collect/delete?collectId=${data.collectId}`, 'POST', data)
}
// 商品收藏-列表
export function goodsCollectList(params) {
	return request(`${uri}collect/list`, 'GET', params)
}
// 商品收藏-信息
export function goodsCollectInfo(params) {
	return request(`${uri}collect/${collectId}`, 'GET', params)
}

// 收货地址列表
export function getAddressList(params) {
	return request(`${uri}address/list`, 'GET', params)
}
// 收货地址
export function getAddressInfo(addressId) {
	return request(`${uri}address/${addressId}`, 'GET')
}
// 删除收货地址
export function deleteAddress(addressId) {
	return request(`${uri}address/delete/${addressId}`, 'POST')
}
// 新增收货地址
export function addAddress(data) {
	return request(`${uri}address/add`, 'POST', data)
}
// 修改收货地址
export function updateAddress(data) {
	return request(`${uri}address/edit`, 'POST', data)
}

// 获取优惠券
export function getCouponList(params) {
	return request(`system/app_goods_coupon_got/list`, 'GET', params)
}
// 获取订单可用优惠券
export function getEnableCouponList(params) {
	return request(`${uri}get_coupon_enable_list`, 'GET', params)
}
// 领取优惠券
export function gotCoupon(data) {
	return request(`system/app_goods_coupon_got`, 'POST', data)
}

export function getDistributionOffer(params) {
	return request(`${uri}distribution_offer`, 'GET', params)
}

export function claimDistributionCoupon(data) {
	return request(`${uri}claim_distribution_coupon`, 'POST', data)
}

// 获取我的优惠券
export function getMyCouponList(params) {
	return request(`${uri}get_coupon_list`, 'GET', params)
}

// 创建商品订单
export function createOrder(data) {
	return request(`${uri}add_goods_order`, 'POST', data)
}
export function quoteRetailOrder(data) {
	return request(`${uri}retail/quote`, 'POST', data)
}
export function submitRetailOrder(data) {
	return request(`${uri}retail/submit`, 'POST', data)
}

// 购物车
export function getCartList(params = {}) {
	return request(`${uri}cart/list`, 'GET', params)
}
export function addCart(data) {
	return request(`${uri}cart/add`, 'POST', data)
}
export function updateCart(data) {
	return request(`${uri}cart/edit`, 'POST', data)
}
export function deleteCart(cartId) {
	return request(`${uri}cart/delete?cartId=${cartId}`, 'POST')
}
export function clearCart() {
	return request(`${uri}cart/clear`, 'POST')
}
// 获取商品订单列表
export function getOrderList(params) {
	return request(`${uri}get_goods_order_list`, 'GET', params)
}
// 获取商品订单详情
export function getOrderDetail(params) {
	return request(`${uri}get_goods_order`, 'GET', params)
}
// 商品订单发起支付
export function payOrder(data) {
	return request(`${uri}pay_goods_order`, 'POST', data)
}
// 支付成功后主动同步订单状态
export function syncGoodsOrderPay(orderId) {
	return request(`${uri}sync_goods_order_pay?orderId=` + orderId, 'POST')
}
// 退款成功后主动同步订单状态（修复一直退款中）
export function syncGoodsOrderRefund(orderId) {
	return request(`${uri}sync_goods_order_refund?orderId=` + orderId, 'POST')
}

// 申请成为顾问
export function applyConsultant(data) {
	return request(`${uri}apply_consultant`, 'POST', data)
}
// 查询下级顾问列表
export function getConsultantChildren(params) {
	return request(`${uri}consultant/children`, 'GET', params)
}
// 查询上级信息
export function getConsultantParent(params) {
	return request(`${uri}consultant/parent`, 'GET', params)
}
// 查询客户列表
export function getCustomerList(params) {
	return request(`${uri}consultant/customer_list`, 'GET', params)
}
// 顾问中心统计数据
export function getConsultantStat(params) {
	return request(`${uri}consultant/stat`, 'GET', params)
}
// 顾问收支明细列表
export function getConsultantIncomeList(params) {
	return request(`${uri}consultant/income_list`, 'GET', params)
}
// 顾问提现记录列表
export function getConsultantCashList(params) {
	return request(`${uri}consultant/cash_list`, 'GET', params)
}
// 顾问邀请二维码
export function getConsultantInviteQrcode(params) {
	return request(`${uri}consultant/invite_qrcode`, 'GET', params)
}
// 绑定邀请人
export function bindInviter(data) {
	const parentUserId = data && data.parentUserId
	return request(`${uri}bind_inviter?parentUserId=${encodeURIComponent(parentUserId)}`, 'POST', data)
}

// 查询个人中心统计数据
export function getStatic(data) {
	return request(`${uri}user_statistic/all`, 'POST')
}

//取消订单
export function cancelOrder(data){
	 return request(`${uri}cacelOrder?orderId=`+data, 'POST', data);
}

//申请退款
export function refundOrder(data){
	return request(`${uri}appRefundOrder`, 'POST', data);
}
