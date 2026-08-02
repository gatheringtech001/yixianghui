import request from "../request.js";
const uri = 'mnp/index/'

// 获取商品分类
export function getGoodsCatrgorys(params) {
	return request(`${uri}get_goods_category`, 'GET', params)
}
// 获取商品列表
export function getGoodsList(params = {}) {
	const currentSite = uni.getStorageSync('site')
	const queryParams = { ...params }
	const ignoreSite = queryParams.ignoreSite
	delete queryParams.ignoreSite
	if (!ignoreSite && currentSite && currentSite.deptId && !queryParams.deptId) {
		queryParams.deptId = currentSite.deptId
	}
	return request(`${uri}queryGoodsList`, 'POST', queryParams)
}

// 获取商品详情
export function getGoodsInfo(id) {
	return request(`${uri}get_goods_info/${id}`, 'GET')
}

// 获取商品型号信息
export function getGoodsSkuInfo(id) {
	return request(`${uri}goods_sku_data?goodsId=${id}`, 'GET')
}

// 根据ID获取商品型号信息
export function getGoodsSkuInfoById(id) {
	return request(`${uri}goods_sku_data_info?skuDataId=${id}`, 'GET')
}