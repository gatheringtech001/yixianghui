import request from "./request.js";
const uri = 'mnp/index/'

// 获取banner广告位列表
export function getBannerPosList(params) {
	return request(`${uri}get_ad_position_list`, 'GET', params)
}
// 获取banner列表信息
export function getBannerList(params) {
	return request(`${uri}get_ad_content_list`, 'GET', params)
}

// 获取图文分类
export function getArticleCategorys(params) {
	return request(`${uri}get_article_category_list`, 'GET', params)
}
// 获取图文列表
export function getArticleList(params) {
	return request(`${uri}get_article_list`, 'GET', params)
}


// 获取单页文章详情
export function getSingleInfo(id) {
	return request(`${uri}get_single_page/${id}`, 'GET')
}

// 查询所在分站
export function getSite(params) {
	return request(`${uri}get_site`, 'GET', params)
}

export function  getSiteBydepId(deptId){
	return request(`${uri}get_site_bydepId/`+deptId, 'GET')
}

// 查询分站列表
export function getSiteList(params) {
	return request(`${uri}get_site_list`, 'GET', params)
}

// 查询省份列表
export function getProvinces() {
	return request(`${uri}get_provinces`, 'GET')
}

// 查询省份下的城市列表
export function getCities(params) {
	return request(`${uri}get_cities`, 'GET', params)
}

//查询对应广告位列表
export function getAdContentByPositionId(positionId){
	return request(`${uri}get_ad_content_list_by_position_id/${positionId}`, 'GET')
}


