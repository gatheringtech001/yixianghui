<template>
	<view class="page_view" :class="{ 'activity-mode': isActivityTab, 'education-mode': isEducationTab }">
		<u-navbar class="weapp-nav-box service-nav" :class="{ 'activity-mode': isActivityTab }" :is-back="false" :background="{ background: '#ffffff' }" :border-bottom="false">
			<view class="slot-wrap home-brand">
				<image v-if="brandLogoDisplay" class="brand-logo" :src="brandLogoDisplay" mode="aspectFit" />
				<text class="brand-title">逸享荟康养</text>
			</view>
		</u-navbar>
		<view class="head-info">
			<view class="head-search">
				<view class="search" @click="searchFn">
					<u-icon name="search" color="#8a8a8a" size="32" />
					<text class="search-text">{{ searchPlaceholder }}</text>
				</view>
			</view>
		</view>

		<view class="switcher">
			<view
				class="switcher-item"
				:class="{ active: isTravelTab }"
				@click="switchToTravel"
			>
				<u-icon name="map" :color="isTravelTab ? '#ffffff' : '#333333'" size="28" />
				<text>全国旅居</text>
			</view>
			<view
				class="switcher-item"
				:class="{ active: isActivityTab }"
				@click="switchToActivity"
			>
				<u-icon name="account-fill" :color="isActivityTab ? '#ffffff' : '#333333'" size="28" />
				<text>聚会活动</text>
			</view>
			<view
				class="switcher-item"
				:class="{ active: isEducationTab }"
				@click="switchToEducation"
			>
				<u-icon name="file-text" :color="isEducationTab ? '#ffffff' : '#333333'" size="28" />
				<text>老年教育</text>
			</view>
		</view>

		<view class="page_body_view">
			<scroll-view
				class="category-rail"
				scroll-y
				:show-scrollbar="false"
				:style="scrollViewStyle"
			>
				<view
					class="category-item"
					:class="{ active: goodsCatrgorySelect == item.categoryId }"
					v-for="(item, index) in goodsCatrgoryList"
					:key="index"
					@click="menuFn(item)"
				>
					{{ item.categoryName }}
				</view>
			</scroll-view>
			<scroll-view
				class="body_goods_view"
				scroll-y
				:show-scrollbar="false"
				:style="scrollViewStyle"
			>
				<block v-if="isEducationTab">
					<view class="course-hero" v-if="showEducationHero">
						<text class="eyebrow">张庙街道老年学校 · 2026秋季</text>
						<view class="hero-title">线下小班课程报名</view>
						<text class="hero-desc">每门课程每周一次，一学期共10次课。线下授课，小班授课（≤18人）。招满10人开班，2026年9月开课。报名时间为2026年06月23日起至课程开课。</text>
					</view>
					<view class="stay-card" v-for="(item, index) in goodsList" :key="index" @click="goodsFn(item)">
						<image :src="host + item.image" mode="aspectFill" />
						<view class="listing-body">
							<view class="card-title">{{ item.goodsName }}</view>
							<view class="meta" v-if="item.summary">{{ item.summary }}</view>
							<view class="tags" v-if="item.tagList && item.tagList.length">
								<text class="outline-chip" v-for="(tag, tagIndex) in item.tagList.slice(0, 3)" :key="tagIndex">{{ tag }}</text>
							</view>
							<view class="price-row">
								<text class="price">{{ currencySymbol }}{{ item.price }}</text>
								<view class="btn-primary">{{ labels.viewCourse }}</view>
							</view>
						</view>
					</view>
					<view class="goods_empty_view" v-if="!loading && isListEmpty">
						<text class="goods_empty_text">{{ emptyListText }}</text>
					</view>
				</block>
				<block v-else-if="!isActivityTab">
					<view class="goods_empty_view" v-if="!loading && isListEmpty">
						<text class="goods_empty_text">{{ emptyListText }}</text>
					</view>
					<view class="stay-card" v-for="(item, index) in goodsList" :key="index" @click="goodsFn(item)">
						<image :src="host + item.image" mode="aspectFill" />
						<view class="listing-body">
							<view class="card-title">{{ item.goodsName }}</view>
							<view class="meta" v-if="item.description">{{ item.description }}</view>
							<view class="tags" v-if="item.tagList && item.tagList.length">
								<text class="outline-chip" v-for="(tag, tagIndex) in item.tagList.slice(0, 3)" :key="tagIndex">{{ tag }}</text>
							</view>
							<view class="price-row">
								<text class="price">¥{{ item.price }}</text>
								<view class="btn-primary">查看详情</view>
							</view>
						</view>
					</view>
				</block>
				<block v-else>
					<view class="goods_empty_view" v-if="!loading && isListEmpty">
						<text class="goods_empty_text">{{ emptyListText }}</text>
					</view>
					<view class="listing-card" v-for="item in activityList" :key="item.activityId" @click.stop="activityDetailFn(item)">
						<image
							v-if="item.activityCover && !item._coverFailed"
							:src="host + item.activityCover"
							mode="aspectFill"
							lazy-load
							@error="onActivityCoverError(item)"
						/>
						<view v-else-if="item.activityCover" class="cover-placeholder">暂无封面</view>
						<view class="listing-body">
							<view class="card-title">{{ item.activityName }}</view>
							<view class="activity-meta">
								<view class="activity-meta-row">
									<view class="activity-label">
										<u-icon name="clock" color="#701018" size="24" />
										<text>时间</text>
									</view>
									<text class="activity-value">{{ item.activityTime }}</text>
								</view>
								<view class="activity-meta-row">
									<view class="activity-label">
										<u-icon name="map" color="#701018" size="24" />
										<text>地址</text>
									</view>
									<text class="activity-value">{{ item.address }}</text>
								</view>
								<view class="activity-meta-row">
									<view class="activity-label">
										<u-icon name="account" color="#701018" size="24" />
										<text>报名人数</text>
									</view>
									<text class="activity-value accent">{{ item.signcount || item.signCount || 0 }} / {{ item.maxCount || 0 }}</text>
								</view>
							</view>
							<view class="price-row">
								<view class="price-left">
									<text class="free-tag" v-if="isActivityFree(item)">免费</text>
									<template v-else>
										<text class="price">￥{{ getActivityPrice(item) }}</text>
										<text class="fee-note">/ 人</text>
									</template>
								</view>
								<view
									class="btn-primary"
									@click.stop="activityDetailFn(item)"
								>查看详情</view>
							</view>
						</view>
					</view>
				</block>
			</scroll-view>
		</view>
		<TabBar ref="tabsBar" class="tabs-bar" :tabBarShow="1"></TabBar>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import TabBar from '@/components/TabBar/TabBar.vue'
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import LocationService from '@/utils/location'
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'
	import { bindPageAuthPopup } from '@/utils/login'
	import { getSite } from '@/api/index'
	import {
		AD_POSITION,
		AD_FALLBACK,
		loadAdImageUrl
	} from '@/utils/adAsset'
	import {
		getGoodsCatrgorys,
		getGoodsList
	} from '@/api/shop/index'
	import {
		getActivityList,
		getActivityCategoryList
	} from '@/api/activity/index'
	import { parseCourseMeta } from '@/utils/courseMeta'
	import {
		getTravelCategoryRows,
		isVisibleCatalogGoods,
		isVisibleTravelGoods
	} from '@/utils/travelCatalog'
	export default {
		mixins: [sharePageMixin],
		components: {
			TabBar,
			AuthProfilePopup,
		},
		computed: {
			currentNavbarItem() {
				return this.navbarList.find(v => v.categoryId == this.navbarSelect) || null
			},
			isTravelTab() {
				const item = this.currentNavbarItem
				return !!(item && item.categoryName === '全国旅居')
			},
			isEducationTab() {
				const item = this.currentNavbarItem
				return !!(item && item.categoryName === '老年教育')
			},
			isActivityTab() {
				const item = this.currentNavbarItem
				return !!(item && item.linkType === 'activity')
			},
			isListEmpty() {
				return this.isActivityTab ? this.activityList.length === 0 : this.goodsList.length === 0
			},
			emptyListText() {
				if (this.isActivityTab) return '暂无活动'
				if (this.isEducationTab) return '暂无课程'
				return '暂无商品'
			},
			searchPlaceholder() {
				if (this.isActivityTab) return '搜索活动'
				if (this.isEducationTab) return '搜索课程'
				return '搜索热门商品'
			},
			scrollViewStyle() {
				const height = Number(this.bodyScrollHeight)
				const safeHeight = Number.isFinite(height) && height > 0 ? height : 400
				return { height: safeHeight + 'px' }
			},
			brandLogoDisplay() {
				return this.brandLogoUrl || AD_FALLBACK.BRAND_LOGO
			}
		},
		data() {
			return {
				host: this.$host,
				loading: false,
				siteData: null,
				navbarList: [],
				navbarSelect: 0,
				goodsCatrgoryList: [],
				goodsCatrgorySelect: 0,
				goodsList: [],
				activityList: [],
				pendingSubCls: null,
				searchKeyword: '',
				bodyScrollHeight: 0,
				brandLogoUrl: '',
				travelGoodsRows: [],
				travelGoodsLoaded: false,
				showEducationHero: false,
				currencySymbol: '\uFFE5',
				labels: {
					viewCourse: '\u67e5\u770b\u8bfe\u7a0b'
				}
			};
		},
		onLoad(options) {
			parseInvitePageOptions(options)
			if (options && options.cls) {
				uni.setStorageSync('currentCls', options.cls)
			}
			if (uni.getStorageSync('cls')) {
				this.navbarList = uni.getStorageSync('cls').filter(e => e.parentId == 0)
				if (this.navbarList.length > 0) {
					const cls = uni.getStorageSync('currentCls')
					const matched = cls ? this.navbarList.find(v => v.categoryId == cls) : null
					this.navbarSelect = matched ? matched.categoryId : this.navbarList[0].categoryId
				}
			}
		},
		onReady() {
			uni.hideTabBar()
			this.setBodyScrollHeight()
		},
		onShow() {
			bindPageAuthPopup(this)
			if (!this.brandLogoUrl) {
				this.loadBrandLogo()
			}
			this.syncNavbarListFromCache()
			this.syncSiteDisplay()
			this.$nextTick(() => {
				this.setBodyScrollHeight()
			})
			if (this.navbarList.length === 0) return

			const keyword = uni.getStorageSync('searchKeyword')
			const hasKeyword = keyword != null && keyword !== ''
			if (hasKeyword) {
				uni.removeStorageSync('searchKeyword')
			}

			const subCls = uni.getStorageSync('currentSubCls')
			const cls = uni.getStorageSync('currentCls')
			const hasCls = cls != null && cls !== ''
			const hasSubCls = subCls != null && subCls !== ''

			if (hasKeyword) {
				this.searchKeyword = keyword
				const travelNav = this.navbarList.find(v => v.categoryName === '全国旅居') || this.navbarList[0]
				if (travelNav) {
					if (hasCls) uni.removeStorageSync('currentCls')
					if (hasSubCls) uni.removeStorageSync('currentSubCls')
					this.pendingSubCls = 0
					this.applyNavbarItem(travelNav, true)
					return
				}
			}

			if (hasCls || hasSubCls) {
				if (hasCls) uni.removeStorageSync('currentCls')
				if (hasSubCls) uni.removeStorageSync('currentSubCls')

				if (hasCls) {
					const matched = this.navbarList.find(v => v.categoryId == cls)
					if (matched) {
						this.pendingSubCls = hasSubCls ? subCls : null
						this.applyNavbarItem(matched)
						return
					}
				}

				if (hasSubCls) {
					if (subCls === 0 || subCls === '0') {
						const travelNav = this.navbarList.find(v => v.categoryName === '全国旅居') || this.navbarList[0]
						if (travelNav) {
							this.pendingSubCls = 0
							this.applyNavbarItem(travelNav)
							return
						}
					}
					const allCategories = uni.getStorageSync('cls') || []
					const subCat = allCategories.find(v => String(v.categoryId) === String(subCls))
					if (subCat && subCat.parentId != 0) {
						const parentNav = this.navbarList.find(v => v.categoryId == subCat.parentId)
						if (parentNav) {
							this.pendingSubCls = subCls
							this.applyNavbarItem(parentNav)
							return
						}
					}
				}
			}

			if (!this.goodsCatrgoryList.length) {
				if (this.isActivityTab) {
					const item = this.navbarList.find(v => v.categoryId == this.navbarSelect)
					this.getActivityCategoryFn((item && item.linkId) || 0)
				} else {
					this.getGoodsCatrgoryFn(this.navbarSelect)
				}
			} else if (this.isListEmpty && !this.loading) {
				this.loadContentList()
			}
		},
		methods: {
			getShareConfig() {
				const item = this.currentNavbarItem
				return {
					title: item ? `逸享荟${item.categoryName}` : '逸享荟康养服务',
					path: '/pages/classify/classify',
					query: { cls: this.navbarSelect }
				}
			},
			async loadBrandLogo() {
				this.brandLogoUrl = await loadAdImageUrl(AD_POSITION.BRAND_LOGO, this.host)
			},
			syncNavbarListFromCache() {
				const cls = uni.getStorageSync('cls') || []
				if (!cls.length) return
				this.navbarList = cls.filter(e => e.parentId == 0)
				if (!this.navbarSelect && this.navbarList.length > 0) {
					this.navbarSelect = this.navbarList[0].categoryId
				}
			},
			refreshNavbarListFromApi() {
				return getGoodsCatrgorys({
					status: 1
				}).then(res => {
					const data = res.data || []
					uni.setStorageSync('cls', data)
					this.navbarList = data.filter(e => e.parentId == 0)
					return this.navbarList
				})
			},
			buildGoodsListItem(item) {
				let tagList = []
				let image = ''
				if (item.tags) {
					tagList = item.tags.split(/[,，|]/)
				}
				if (item.goodsImages) {
					image = item.goodsImages.split(',')[0]
				} else if (item.goodsCover) {
					image = item.goodsCover
				}
				const ext = item.educationExt || {}
				const courseMeta = parseCourseMeta(item.description)
				return {
					goodsId: item.goodsId,
					goodsName: item.goodsName,
					tagList,
					image,
					price: item.vipPrice || item.price,
					unit: item.unit,
					goodsType: item.goodsType,
					description: item.description,
					summary: courseMeta.summary,
					courseTime: ext.courseTime || courseMeta.time,
					coursePlace: ext.coursePlace || courseMeta.place,
					courseTeacher: ext.teacherName || courseMeta.teacher
				}
			},
			setBodyScrollHeight() {
				this.$nextTick(() => {
					const query = uni.createSelectorQuery().in(this)
					// 用 page_body_view 顶部位置算剩余视口，避免用自身 height 形成循环测量
					query.select('.page_body_view').boundingClientRect()
					query.exec((res) => {
						const bodyRect = res && res[0]
						const windowInfo = uni.getWindowInfo ? uni.getWindowInfo() : uni.getSystemInfoSync()
						const windowHeight = Number(windowInfo.windowHeight) || 0
						const tabBarH = uni.upx2px(118)
						let available = 0
						if (bodyRect && Number.isFinite(bodyRect.top)) {
							available = windowHeight - bodyRect.top - tabBarH
						} else {
							available = windowHeight - uni.upx2px(88) - uni.upx2px(92) - uni.upx2px(84) - tabBarH - uni.upx2px(24)
						}
						this.bodyScrollHeight = Math.max(Math.floor(available), 240)
					})
				})
			},
			buildActivitySideList(rows) {
				const list = [{
					categoryId: 0,
					categoryName: '全部'
				}]
				;(rows || []).forEach(item => {
					list.push({
						categoryId: item.categoryId,
						categoryName: item.categoryName
					})
				})
				list.push({
					categoryId: 'ended',
					categoryName: '已结束'
				})
				return list
			},
			onActivityCoverError(item) {
				if (!item) return
				this.$set(item, '_coverFailed', true)
			},
			switchToTravel() {
				if (this.isTravelTab) return
				const item = this.navbarList.find(v => v.categoryName === '全国旅居')
					|| this.navbarList.find(v => v.linkType !== 'activity' && v.categoryName !== '老年教育')
				if (item) this.applyNavbarItem(item)
			},
			switchToActivity() {
				if (this.isActivityTab) return
				const item = this.navbarList.find(v => v.linkType === 'activity')
					|| this.navbarList.find(v => v.categoryName === '聚会活动')
					|| this.navbarList.find(v => String(v.categoryName || '').trim() === '聚会活动')
				if (item) this.applyNavbarItem(item)
			},
			switchToEducation() {
				if (this.isEducationTab) return
				this.syncNavbarListFromCache()
				const applyEducation = () => {
					const item = this.navbarList.find(v => v.categoryName === '老年教育')
					if (item) {
						this.applyNavbarItem(item)
						return true
					}
					return false
				}
				if (applyEducation()) return
				this.refreshNavbarListFromApi().then(() => {
					if (!applyEducation()) {
						uni.showToast({
							title: '未找到老年教育分类',
							icon: 'none'
						})
					}
				}).catch(() => {
					uni.showToast({
						title: '未找到老年教育分类',
						icon: 'none'
					})
				})
			},
			async syncSiteDisplay() {
				let site = uni.getStorageSync('site')
				if (!site) {
					await this.getCurrentLocationFn()
				} else {
					this.siteData = site
				}
			},
			switchSiteFn() {
				const site = this.siteData || uni.getStorageSync('site')
				if (!site || !site.deptId) {
					uni.showToast({
						title: '正在获取定位，请稍后再试',
						icon: 'none'
					})
					return
				}
				uni.navigateTo({
					url: `/packagesPublic/site/index?id=${site.deptId}`
				})
			},
			async getCurrentLocationFn() {
				try {
					const { latitude, longitude } = await LocationService.getLocation()
					const { data } = await getSite({ lat: latitude, lng: longitude })
					this.siteData = data
					uni.setStorageSync('site', data)
				} catch (error) {
					console.error('定位错误:', error)
				}
			},
			applyNavbarItem(item, keepSearch = false) {
				this.navbarSelect = item.categoryId
				this.goodsCatrgorySelect = 0
				this.loading = false
				this.travelGoodsRows = []
				this.travelGoodsLoaded = false
				if (!keepSearch) {
					this.searchKeyword = ''
				}
				if (item.linkType === 'activity') {
					this.goodsList = []
					this.getActivityCategoryFn(item.linkId || 0)
				} else {
					this.activityList = []
					this.getGoodsCatrgoryFn(item.categoryId)
				}
				this.$nextTick(() => {
					this.setBodyScrollHeight()
				})
			},
			searchFn() {
				uni.navigateTo({
					url: '/packagesMall/search/search'
				})
			},
			navbarFn(data) {
				this.applyNavbarItem(data)
			},
			async getGoodsCatrgoryFn(id) {
				this.goodsCatrgoryList = []
				try {
					const categoryRequest = getGoodsCatrgorys({
						parentId: id,
						status: 1
					})
					const [categoryResponse, goodsResponse] = this.isTravelTab
						? await Promise.all([
							categoryRequest,
							getGoodsList({ categoryId: id, ignoreSite: true })
						])
						: [await categoryRequest, null]
					let categoryRows = categoryResponse.data || []
					if (this.isTravelTab) {
						this.travelGoodsRows = (goodsResponse.data || []).filter(isVisibleTravelGoods)
						this.travelGoodsLoaded = true
						const allCategories = uni.getStorageSync('cls') || []
						categoryRows = getTravelCategoryRows(
							allCategories,
							this.travelGoodsRows
						).map(row => row.category)
					}
					this.goodsCatrgoryList = [{
						categoryId: 0,
						categoryName: '全部'
					}]
					categoryRows.forEach(item => {
						this.goodsCatrgoryList.push({
							categoryId: item.categoryId,
							categoryName: item.categoryName
						})
					})
					const pending = this.pendingSubCls
					this.pendingSubCls = null
					if (pending != null && pending !== '') {
						if (pending === 0 || pending === '0') {
							this.goodsCatrgorySelect = 0
						} else {
							const matched = this.goodsCatrgoryList.find(
								v => String(v.categoryId) === String(pending)
							)
							if (matched) {
								this.goodsCatrgorySelect = matched.categoryId
							}
						}
					}
					this.getGoodsListFn()
				} catch (err) {
					console.log('getGoodsCatrgorys', err)
					this.goodsCatrgoryList = [{
						categoryId: 0,
						categoryName: '全部'
					}]
					this.getGoodsListFn()
				} finally {
					this.setBodyScrollHeight()
				}
			},
			getActivityCategoryFn(parentId) {
				this.goodsCatrgoryList = []
				getActivityCategoryList({
					parentId: parentId || 0
				}).then(res => {
					this.goodsCatrgoryList = this.buildActivitySideList(res.data)
					this.getActivityListFn()
				}).catch(err => {
					console.log('getActivityCategoryList', err)
					this.goodsCatrgoryList = this.buildActivitySideList([])
					this.getActivityListFn()
				}).finally(() => {
					this.setBodyScrollHeight()
				})
			},
			loadContentList() {
				if (this.isActivityTab) {
					this.getActivityListFn()
				} else {
					this.getGoodsListFn()
				}
			},
			getGoodsListFn() {
				if (!this.navbarSelect) return
				this.loading = true
				const categoryId = this.goodsCatrgorySelect == 0
					? this.navbarSelect
					: this.goodsCatrgorySelect
				const params = {
					categoryId,
					ignoreSite: true
				}
				const keyword = String(this.searchKeyword || '').trim()
				if (keyword) {
					params.goodsName = keyword
				}
				if (this.isTravelTab && this.travelGoodsLoaded && !keyword) {
					const rows = this.goodsCatrgorySelect == 0
						? this.travelGoodsRows
						: this.travelGoodsRows.filter(item => (
							String(item.categoryId) === String(this.goodsCatrgorySelect)
						))
					this.goodsList = rows.map(this.buildGoodsListItem)
					this.loading = false
					this.setBodyScrollHeight()
					return
				}
				getGoodsList(params).then(res => {
					this.goodsList = []
					;(res.data || []).filter(isVisibleCatalogGoods).forEach((item) => {
						this.goodsList.push(this.buildGoodsListItem(item))
					})
				}).catch(err => {
					console.log('getGoodsList', err)
					this.goodsList = []
				}).finally(() => {
					this.loading = false
					this.setBodyScrollHeight()
				})
			},
			getActivityListFn() {
				this.loading = true
				const params = {}
				const isEnded = this.goodsCatrgorySelect === 'ended'
				if (isEnded) {
					params.signFilter = 'ended'
				} else {
					params.signFilter = 'active'
					if (this.goodsCatrgorySelect && this.goodsCatrgorySelect !== 0) {
						params.categoryId = this.goodsCatrgorySelect
					}
				}
				const keyword = String(this.searchKeyword || '').trim()
				if (keyword) {
					params.activityName = keyword
				}
				getActivityList(params)
					.then(res => {
						this.activityList = res.rows || []
					}).catch(err => {
						console.log('getActivityList', err)
						this.activityList = []
					}).finally(() => {
						this.loading = false
						this.setBodyScrollHeight()
					})
			},
			menuFn(data) {
				this.searchKeyword = ''
				this.goodsCatrgorySelect = data.categoryId
				this.loadContentList()
				this.$nextTick(() => {
					this.setBodyScrollHeight()
				})
			},
			goodsFn(data) {
				if (data.goodsType == 'hotel') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${data.goodsId}`
					})
				} else if (data.goodsType == 'education') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/EducationGoodsDetails?id=${data.goodsId}`
					})
				} else {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/GoodsDetails?id=${data.goodsId}`
					})
				}
			},
			isActivityFree(item) {
				if (!item) return true
				const isFree = item.isFree
				return isFree === 1 || isFree === '1' || isFree === null || isFree === undefined
			},
			getActivityPrice(item) {
				if (this.isActivityFree(item)) return 0
				return item.vipPrice || item.price || 0
			},
			activityDetailFn(item) {
				uni.navigateTo({
					url: `/packagesMall/Activity/detail/index?id=${item.activityId}`
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'classify.scss';
</style>
