<template>
	<view class="page">
		<u-navbar class="weapp-nav-box home-nav" :is-back="false" :background="{ background: '#EFEBDF' }" :border-bottom="false">
			<view class="slot-wrap home-brand">
				<image v-if="brandLogoDisplay" class="brand-logo" :src="brandLogoDisplay" mode="aspectFit" />
				<text class="brand-title">逸享荟康养</text>
			</view>
		</u-navbar>
		<view class="head-info">
			<view class="head-search">
				<view class="search" @click="onSearch">
					<u-icon name="search" color="#8a8a8a" size="32" />
					<text class="search-text">搜索热门商品</text>
				</view>
			</view>
		</view>

		<scroll-view
			class="home-scroll"
			scroll-y
			:style="{ height: scrollViewHeight + 'px' }"
			refresher-enabled
			refresher-default-style="none"
			refresher-background="#f7f7f5"
			:refresher-triggered="refreshing"
			@refresherpulling="onRefresherPulling"
			@refresherrefresh="handleRefresherRefresh"
			@refresherrestore="onRefresherRestore"
		>
			<view slot="refresher" class="home-refresher">
				<text>{{ refreshHintText }}</text>
			</view>
			<view class="home-scroll-inner">
				<swiper
					v-if="recommendations.length"
					class="recommendation-swiper"
					indicator-dots
					circular
					autoplay
					interval="4200"
					duration="500"
				>
					<swiper-item v-for="item in recommendations" :key="item.key">
						<view class="recommendation-card" @click="openRecommendation(item)">
							<image class="recommendation-image" :src="mediaUrl(item.image)" mode="aspectFill" />
							<view class="recommendation-shade"></view>
							<view class="recommendation-copy">
								<text class="recommendation-section">{{ item.section }}</text>
								<view class="recommendation-title">{{ item.title }}</view>
								<text class="recommendation-action">查看详情</text>
							</view>
						</view>
					</swiper-item>
				</swiper>

				<view class="housekeeper">
					<image class="housekeeper-avatar" :src="housekeeperAvatarDisplay" mode="aspectFill" />
					<view class="housekeeper-copy">
						<view class="title">添加逸享荟小管家</view>
						<view class="slogan">全程管家式服务，为您的生活保驾护航</view>
					</view>
					<view class="housekeeper-btn" @click.stop="openHousekeeper">添加管家</view>
				</view>

				<view class="service-entry-stack">
					<view class="entry-card travel-entry-card" @click="goClassify('全国旅居')">
						<image class="entry-bg" src="/static/home-design/entry-stay-wide.jpg" mode="aspectFill" />
						<view class="entry-copy">
							<text class="entry-title">全国旅居</text>
							<text class="entry-desc">精选目的地 · 管家陪同 · 适老友好</text>
						</view>
					</view>
					<view class="secondary-entry-grid">
						<view class="entry-card" @click="goClassify('云南好物')">
							<image class="entry-bg" src="/static/home-design/entry-yunnan.jpg" mode="aspectFill" />
							<view class="entry-copy">
								<text class="entry-title">云南好物</text>
								<text class="entry-desc">山野风味甄选</text>
							</view>
						</view>
						<view class="entry-card" @click="goClassify('聚会活动')">
							<image class="entry-bg" src="/static/home-design/entry-activity.jpg" mode="aspectFill" />
							<view class="entry-copy">
								<text class="entry-title">聚会活动</text>
								<text class="entry-desc">精彩活动汇聚</text>
							</view>
						</view>
						<view class="entry-card" @click="goClassify('老年教育')">
							<image class="entry-bg" src="/static/home-design/entry-education.jpg" mode="aspectFill" />
							<view class="entry-copy">
								<text class="entry-title">芳华学院</text>
								<text class="entry-desc">线下课程报名</text>
							</view>
						</view>
						</view>
					</view>

				<view class="section-head">
					<text class="section-title">热门城市</text>
					<text class="section-more" @click="goToServiceTab">查看全部 &gt;</text>
				</view>
				<swiper
					v-if="hotCardList.length"
					class="city-swiper"
					:autoplay="hotCardList.length > 5"
					:circular="hotCardList.length > 5"
					:display-multiple-items="Math.min(5, hotCardList.length)"
					interval="2500"
					duration="600"
					@change="onHotCitySwiperChange"
				>
					<swiper-item
						class="city-slide"
						v-for="(item,index) in hotCardList"
						:key="item.categoryId"
					>
						<view
							class="city-card"
							:class="{ active: selectedCityIndex === index }"
							@click="changeSiteHandle(item, index)"
						>
							<image class="city-image" :src="getCityImageUrl(item)" mode="aspectFill" />
							<text class="city-name">{{ item.adName }}</text>
						</view>
					</swiper-item>
				</swiper>

				<view class="cls-goods" v-if="citySelectionReady">
					<view class="section-head recommend-head">
						<text class="section-title">精选推荐</text>
						<text class="section-more" v-if="currentCategoryId" @click="goMall">查看更多 &gt;</text>
					</view>
					<view class="product-list" v-if="currentGoodsList.length > 0">
						<view
							class="product-card"
							v-for="(goods, i) in currentGoodsList"
							:key="'home-goods-' + (goods.goodsId || i)"
							:data-goods-id="goods.goodsId"
							:data-goods-type="goods.goodsType || ''"
							@tap.stop="goProdDetail"
						>
							<image class="product-cover" :src="host + goods.goodsCover" mode="aspectFill" />
							<view class="product-body">
								<view class="product-title">{{ goods.goodsName }}</view>
								<view class="product-tags" v-if="visibleGoodsTags(goods.tags).length">
									<text
										class="product-tag"
										v-for="(tag, j) in visibleGoodsTags(goods.tags).slice(0, 3)"
										:key="j"
									>{{ tag }}</text>
								</view>
								<view class="product-price-row">
									<view class="product-price">
										<text class="price">¥{{ goods.price }}</text>
									</view>
									<view class="product-btn">
										<text>{{ goods.goodsType == 'online' || goods.goodsType == 'o2o' ? '购买' : '预订' }}</text>
									</view>
								</view>
							</view>
						</view>
					</view>
					<view class="goods-empty" v-else>
						<text>暂无商品，请稍后再试</text>
					</view>
				</view>
			</view>
		</scroll-view>

		<u-popup class="butler-popup" v-model="showContact" @touchmove.stop.prevent mode="bottom" border-radius="16"
			:closeable="true">
			<view class="popup-title">添加逸享荟小管家</view>
			<view class="items">
				<view class="item">
					<u-icon name="server-man" size="40" color="#00C800" />
					<text>1对1专属服务</text>
				</view>
				<view class="item">
					<u-icon name="heart-fill" size="40" color="#00C800" />
					<text>全程管家式服务</text>
				</view>
				<view class="item">
					<u-icon name="file-text-fill" size="40" color="#00C800" />
					<text>优质路线推荐</text>
				</view>
				<view class="item">
					<u-icon name="coupon-fill" size="40" color="#00C800" />
					<text>优惠活动不错过</text>
				</view>
			</view>
			<view class="steps" v-if="selectedContact && selectedContact.adImage">
				<view class="content">
					<image
						:src="getAdImageUrl(selectedContact.adImage)"
						mode="aspectFit"
						show-menu-by-longpress
						@tap="previewQrImage(selectedContact.adImage)"
					/>
					<view class="tips">长按识别二维码添加</view>
				</view>
			</view>
		</u-popup>

		<u-popup v-model="showDistributionCoupon" mode="center" border-radius="20" :closeable="true">
			<view class="distribution-coupon" v-if="distributionOffer">
				<view class="distribution-coupon-title">{{ distributionOffer.coupon.popupTitle || '专属优惠券' }}</view>
				<view class="distribution-coupon-value">
					<text v-if="distributionOffer.coupon.discountType === '2'">{{ formatCouponDiscount(distributionOffer.coupon.discountPrice) }}折</text>
					<text v-else>¥{{ distributionOffer.coupon.discountPrice }}</text>
				</view>
				<view class="distribution-coupon-name">{{ distributionOffer.coupon.couponName }}</view>
				<view class="distribution-coupon-limit">满¥{{ distributionOffer.coupon.minPrice }}可用</view>
				<view class="distribution-coupon-action" @click="claimChannelCoupon">
					{{ distributionOffer.claimed ? '已领取，去选购' : (claimingCoupon ? '领取中...' : '立即领取') }}
				</view>
			</view>
		</u-popup>

		<!-- tabbar -->
		<TabBar :tabBarShow="0"></TabBar>
	</view>
</template>

<script>
	import TabBar from '@/components/TabBar/TabBar.vue'
	import LocationService from '@/utils/location'
	import {
		parseInvitePageOptions,
		getDistributionLaunchSource,
		clearDistributionLaunchSource
	} from '@/utils/invite'
	import { getDistributionOffer, claimDistributionCoupon } from '@/api/member/index'
	import sharePageMixin from '@/utils/sharePageMixin'
	import {
		getSite,
		getBannerList
	} from '@/api/index'
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
		buildHotCityCards,
		isVisibleCatalogGoods,
		isVisibleTravelGoods
	} from '@/utils/travelCatalog'
	import { getActivityList } from '@/api/activity/index'
	import {
		CUSTOMER_SERVICE_POSITION_ID,
		HOUSEKEEPER_ROTATION_KEY,
		selectRotatingHousekeeper
	} from '@/utils/housekeeperRotation'
	export default {
		mixins: [sharePageMixin],
		components: {
			TabBar
		},
		data() {
			return {
				host: this.$host,
				siteInfo: null,
				recommendations: [],
				hotCardList: [],
				slideNum: 0,
				navList: [],
				hotCategoryList: [],
				hotGoods: [],
				classifyShow: 0,
				showContact: false,
				contact: [],
				selectedContact: null,
				selectedCityIndex: -1,
				currentCategoryId: null,
				currentCategoryInfo: null,
				currentGoodsList: [],
				citySelectionReady: false,
				citySwitching: false,
				citySwiperIndex: 0,
				refreshing: false,
				refreshHintText: '下拉刷新',
				scrollViewHeight: 0,
				brandLogoUrl: '',
				housekeeperAvatarUrl: '',
				showDistributionCoupon: false,
				distributionOffer: null,
				claimingCoupon: false,
				distributionOfferChecked: false
			}
		},
		onReady() {
			uni.hideTabBar();
			this.setScrollViewHeight();
		},
		onLoad(options) {
			parseInvitePageOptions(options)
			this.loadMnpAdAssets()
			this.gethotCardList()
			this.getContactAdList()
		},
		async onShow() {
			await this.loadDistributionOffer()
			let site = uni.getStorageSync('site')
			if (!site || site == undefined) {
				await this.getCurrentLocation()
			} else {
				this.siteInfo = site
			}
			this.initCitySelection()
		},
		computed: {
			brandLogoDisplay() {
				return this.brandLogoUrl || AD_FALLBACK.BRAND_LOGO
			},
			housekeeperAvatarDisplay() {
				return this.housekeeperAvatarUrl || AD_FALLBACK.HOME_HOUSEKEEPER
			},
			heroImage() {
				if (this.recommendations.length) return this.mediaUrl(this.recommendations[0].image)
				return '/static/home-design/hero-banner.jpg'
			}
		},
		methods: {
			async loadDistributionOffer() {
				const source = getDistributionLaunchSource()
				if (!source || !uni.getStorageSync('token') || this.distributionOfferChecked) return
				this.distributionOfferChecked = true
				try {
					const response = await getDistributionOffer(source)
					this.distributionOffer = response.data
					this.showDistributionCoupon = true
				} catch (error) {
					clearDistributionLaunchSource()
					console.warn('[distribution] offer unavailable:', error.message)
				}
			},
			formatCouponDiscount(percent) {
				const value = Number(percent) / 10
				return Number.isInteger(value) ? String(value) : value.toFixed(1)
			},
			async claimChannelCoupon() {
				if (!this.distributionOffer || this.claimingCoupon) return
				if (this.distributionOffer.claimed) {
					this.showDistributionCoupon = false
					this.goToServiceTab()
					return
				}
				this.claimingCoupon = true
				try {
					await claimDistributionCoupon(getDistributionLaunchSource())
					this.distributionOffer.claimed = true
					uni.showToast({ title: '领取成功', icon: 'success' })
				} catch (error) {
					uni.showToast({ title: error.message || '领取失败', icon: 'none' })
				} finally {
					this.claimingCoupon = false
				}
			},
			getShareConfig() {
				return {
					title: '逸享荟康养，旅居、活动、芳华学院一站式服务',
					path: '/pages/home/home',
					imageUrl: this.heroImage
				}
			},
			async loadMnpAdAssets() {
				const [brandLogoUrl, housekeeperAvatarUrl] = await Promise.all([
					loadAdImageUrl(AD_POSITION.BRAND_LOGO, this.host),
					loadAdImageUrl(AD_POSITION.HOME_HOUSEKEEPER, this.host)
				])
				this.brandLogoUrl = brandLogoUrl
				this.housekeeperAvatarUrl = housekeeperAvatarUrl
			},
			async handleRefresherRefresh() {
				if (this.refreshing) return
				this.refreshing = true
				this.refreshHintText = '正在刷新，请稍候...'
				try {
					await Promise.all([
						this.loadMnpAdAssets(),
						this.refreshHomeData()
					])
					uni.showToast({
						title: '刷新成功',
						icon: 'none',
						duration: 1500
					})
				} catch (error) {
					uni.showToast({
						title: '刷新失败，请稍后重试',
						icon: 'none'
					})
				} finally {
					this.refreshing = false
					this.refreshHintText = '下拉刷新'
				}
			},
			setScrollViewHeight() {
				this.$nextTick(() => {
					const query = uni.createSelectorQuery().in(this)
					query.select('.weapp-nav-box').boundingClientRect()
					query.select('.head-info').boundingClientRect()
					query.exec((res) => {
						const sys = uni.getSystemInfoSync()
						const navH = (res[0] && res[0].height) || (sys.statusBarHeight + 44)
						const headH = (res[1] && res[1].height) || uni.upx2px(88)
						this.scrollViewHeight = sys.windowHeight - navH - headH
					})
				})
			},
			onRefresherPulling(e) {
				if (this.refreshing) return
				const dy = (e.detail && e.detail.dy) || 0
				this.refreshHintText = dy > 45 ? '松开立即刷新' : '下拉刷新'
			},
			onRefresherRestore() {
				if (!this.refreshing) {
					this.refreshHintText = '下拉刷新'
				}
			},
			async refreshHomeData() {
				const prevIndex = this.selectedCityIndex
				const prevCategoryId = this.currentCategoryId
				try {
					await Promise.all([
						this.gethotCardList(true),
						this.getContactAdList()
					])
					const site = uni.getStorageSync('site')
					if (site) {
						this.siteInfo = site
					}
					const nextIndex = this.hotCardList.findIndex(item => {
						const parsed = this.parseCityCategoryLink(item)
						return parsed && String(parsed.categoryId) === String(prevCategoryId)
					})
					if (nextIndex >= 0) {
						this.selectedCityIndex = nextIndex
						const parsed = this.parseCityCategoryLink(this.hotCardList[nextIndex])
						this.currentCategoryId = (parsed && parsed.categoryId) || prevCategoryId
						this.updateCurrentCategoryInfo()
						this.citySelectionReady = true
						await this.loadCurrentGoodsList()
					} else {
						this.initCitySelection()
					}
				} catch (error) {
					console.error('刷新首页失败:', error)
					throw error
				}
			},
			async getCurrentLocation() {
				try {
					// 获取经纬度
					const {
						latitude,
						longitude
					} = await LocationService.getLocation()
					let params = {
						lat: latitude,
						lng: longitude
					}
					let {
						data
					} = await getSite(params)
					if (!data || !data.deptId) {
						console.error('定位返回站点无效:', data)
						return
					}
					this.siteInfo = data
					uni.setStorageSync('site', data)
					this.citySelectionReady = false
					this.initCitySelection()
				} catch (error) {
					console.error('定位错误:', error)
				}
			},
			findCategoryIdByName(cardName) {
				const name = String(cardName || '').trim()
				if (!name) return null
				const allCategories = uni.getStorageSync('cls') || this.hotCategoryList || []
				const aliases = name === '全国' ? ['全国旅居', '全国'] : [name]
				const matched = allCategories.find(category => (
					aliases.includes(String(category.categoryName || '').trim())
				))
				return matched ? Number(matched.categoryId) : null
			},
			// 生产广告 linkUrl 保存商品分类 ID；空链接按城市名匹配分类。
			parseCityCategoryLink(item) {
				if (!item) return null
				const link = String(item.linkUrl || '').trim()
				const categories = uni.getStorageSync('cls') || this.hotCategoryList || []
				if (link && /^\d+$/.test(link)) {
					const category = categories.find(entry => (
						String(entry.categoryId) === link
					))
					if (category) return { categoryId: Number(link) }
				}
				const categoryId = this.findCategoryIdByName(item.adName)
				return categoryId ? { categoryId } : null
			},
			updateCurrentCategoryInfo() {
				const categories = uni.getStorageSync('cls') || this.hotCategoryList || []
				this.currentCategoryInfo = categories.find(category => (
					String(category.categoryId) === String(this.currentCategoryId)
				)) || null
			},
			initCitySelection() {
				const categories = uni.getStorageSync('cls') || []
				if (!this.hotCardList.length || !categories.length) return
				if (this.citySelectionReady && this.selectedCityIndex >= 0) {
					this.loadCurrentGoodsList()
					return
				}
				const currentSiteName = String((this.siteInfo && this.siteInfo.deptName) || '').trim()
				let index = this.hotCardList.findIndex(item => {
					const parsed = this.parseCityCategoryLink(item)
					return parsed && String(item.adName || '').trim() === currentSiteName
				})
				if (index < 0) index = this.hotCardList.findIndex(item => {
					if (this.isNationalCityCard(item)) return false
					const parsed = this.parseCityCategoryLink(item)
					if (!parsed) return false
					const category = categories.find(entry => (
						String(entry.categoryId) === String(parsed.categoryId)
					))
					return category && String(category.isHot) === '1'
				})
				if (index < 0) index = this.hotCardList.findIndex(item => {
					if (this.isNationalCityCard(item)) return false
					return Boolean(this.parseCityCategoryLink(item))
				})
				if (index >= 0) {
					const parsed = this.parseCityCategoryLink(this.hotCardList[index])
					this.selectedCityIndex = index
					this.currentCategoryId = parsed.categoryId
				} else {
					this.selectedCityIndex = -1
					this.currentCategoryId = null
				}
				this.updateCurrentCategoryInfo()
				this.citySelectionReady = true
				this.loadCurrentGoodsList()
			},
			async loadCurrentGoodsList() {
				if (!this.currentCategoryId) {
					this.currentGoodsList = []
					return
				}
				try {
					const res = await getGoodsList({
						categoryId: this.currentCategoryId,
						ignoreSite: true
					})
					const list = res.data || []
					this.currentGoodsList = list.filter(isVisibleTravelGoods)
					this.hotGoods = this.currentGoodsList.filter(v => v.isHot == 1)
				} catch (error) {
					console.error('加载商品失败:', error)
					this.currentGoodsList = []
				}
			},
			getSite() {
				const deptId = this.siteInfo && this.siteInfo.deptId
				uni.navigateTo({
					url: deptId ? `/packagesPublic/site/index?id=${deptId}` : '/packagesPublic/site/index'
				})
			},
			async gethotCardList(skipInit = false) {
				const categories = await this.getClsList(true)
				await this.loadRecommendations(categories)
				const travel = categories.find(item => (
					String(item.categoryName || '').trim() === '全国旅居'
				))
				if (!travel) {
					this.hotCardList = []
					return []
				}
				const [adResponse, goodsResponse] = await Promise.all([
					getBannerList({ positionId: 6 }),
					getGoodsList({ categoryId: travel.categoryId, ignoreSite: true })
				])
				this.hotCardList = buildHotCityCards(
					categories,
					adResponse.data || [],
					goodsResponse.data || []
				)
				if (!skipInit) {
					this.initCitySelection()
				}
				return this.hotCardList
			},
			mediaUrl(path) {
				if (!path || String(path).startsWith('/static/')) return path || ''
				return /^https?:\/\//.test(path) ? path : this.host + path
			},
			pickRandom(rows) {
				if (!rows || !rows.length) return null
				return rows[Math.floor(Math.random() * rows.length)]
			},
			async loadRecommendations(categories) {
				const sectionConfig = [
					{ name: '全国旅居', kind: 'goods', filter: isVisibleTravelGoods },
					{ name: '云南好物', kind: 'goods', filter: isVisibleCatalogGoods },
					{ name: '老年教育', label: '芳华学院', kind: 'goods', filter: isVisibleCatalogGoods }
				]
				const goodsRequests = sectionConfig.map(async config => {
					try {
						const category = categories.find(item => String(item.categoryName || '').trim() === config.name)
						if (!category) return null
						const response = await getGoodsList({ categoryId: category.categoryId, ignoreSite: true })
						const item = this.pickRandom((response.data || []).filter(config.filter))
						return item && {
							key: `${config.name}-${item.goodsId}`,
							section: config.label || config.name,
							title: item.goodsName,
							image: item.goodsCover,
							kind: 'goods',
							goodsId: item.goodsId,
							goodsType: item.goodsType
						}
					} catch (error) {
						console.warn(`首页${config.label || config.name}推荐加载失败:`, error)
						return null
					}
				})
				const activityRequest = getActivityList({ status: 1, signFilter: 'active' })
					.then(response => {
						const item = this.pickRandom(response.rows || [])
						return item && {
							key: `activity-${item.activityId}`,
							section: '聚会活动',
							title: item.activityName,
							image: item.activityCover,
							kind: 'activity',
							activityId: item.activityId
						}
					})
					.catch(error => {
						console.warn('首页聚会活动推荐加载失败:', error)
						return null
					})
				const results = await Promise.all([...goodsRequests, activityRequest])
				this.recommendations = results.filter(item => item && item.image)
			},
			openRecommendation(item) {
				if (item.kind === 'activity') {
					uni.navigateTo({ url: `/packagesMall/Activity/detail/index?id=${item.activityId}` })
					return
				}
				this.goProdDetail(item)
			},
			visibleGoodsTags(tags) {
				return String(tags || '').split(/[\,|，]/)
					.map(tag => tag.trim())
					.filter(tag => tag && !['云野集', '云南好物'].includes(tag))
			},
			getCityImageUrl(item) {
				const image = String((item && item.adImage) || '').trim()
				if (!image || image.startsWith('/static/') || image.startsWith('http')) return image
				return this.host + image
			},
			onHotCitySwiperChange(event) {
				this.citySwiperIndex = Number(event && event.detail && event.detail.current) || 0
			},
			async getContactAdList() {
				let params = {
					positionId: CUSTOMER_SERVICE_POSITION_ID
				}
				let {
					data
				} = await getBannerList(params)
				this.contact = (Array.isArray(data) ? data : []).filter(item => (
					item && item.adImage
				))
			},
			openHousekeeper() {
				const cursor = uni.getStorageSync(HOUSEKEEPER_ROTATION_KEY)
				const selection = selectRotatingHousekeeper(this.contact, cursor)
				if (!selection.item) {
					uni.showToast({ title: '管家二维码加载中，请稍后重试', icon: 'none' })
					return
				}
				this.selectedContact = selection.item
				uni.setStorageSync(HOUSEKEEPER_ROTATION_KEY, selection.nextCursor)
				this.showContact = true
			},
			getAdImageUrl(adImage) {
				if (!adImage) return ''
				return adImage.startsWith('http') ? adImage : this.host + adImage
			},
			previewQrImage(adImage) {
				const url = this.getAdImageUrl(adImage)
				if (!url) {
					uni.showToast({
						icon: 'none',
						title: '二维码未配置'
					})
					return
				}
				uni.previewImage({
					urls: [url],
					current: url
				})
			},
			async getClsList(skipInit = false) {
				let {
					data
				} = await getGoodsCatrgorys({
					status: 1
				})
				uni.setStorageSync('cls', data)
				this.navList = data.filter(v => v.parentId == 0)
				this.hotCategoryList = data.filter(
					v => v.isHot == '1' && v.categoryName != ' ' && v.categoryName != ''
				)
				if (!skipInit) {
					this.initCitySelection()
				}
				return data || []
			},
			/**
			 * 搜索点击
			 */
			onSearch() {
				uni.navigateTo({
					url: '/packagesMall/search/search'
				})
			},
			isNationalCityCard(cardItem) {
				const name = String((cardItem && cardItem.adName) || '').trim()
				return name === '全国' || name === '更多'
			},
			getTravelNavbarId() {
				const navList = (uni.getStorageSync('cls') || []).filter(v => v.parentId == 0)
				const travelNav = navList.find(v => v.categoryName === '全国旅居')
				return travelNav ? travelNav.categoryId : (navList[0] && navList[0].categoryId)
			},
			navigateToServicePage(subCategoryId, navbarId) {
				const targetNavbarId = navbarId || this.getTravelNavbarId()
				if (targetNavbarId) {
					uni.setStorageSync('currentCls', targetNavbarId)
				}
				uni.setStorageSync('currentSubCls', subCategoryId)
				uni.switchTab({
					url: '/pages/classify/classify'
				})
			},
			goToServiceTab() {
				this.navigateToServicePage(0)
			},
			// 切换热门城市：按广告关联的商品分类加载推荐商品。
			async changeSiteHandle(cardItem, index) {
				if (this.isNationalCityCard(cardItem)) {
					this.goToServiceTab()
					return
				}
				if (this.citySwitching) return
				const parsed = this.parseCityCategoryLink(cardItem)
				if (!parsed || !parsed.categoryId) {
					uni.showToast({
						title: '未配置商品分类，请检查热门城市配置',
						icon: 'none'
					})
					return
				}
				this.citySwitching = true
				try {
					this.selectedCityIndex = index
					this.currentCategoryId = parsed.categoryId
					this.updateCurrentCategoryInfo()
					this.citySelectionReady = true
					await this.loadCurrentGoodsList()
				} catch (error) {
					console.error('切换热门城市失败:', error)
					uni.showToast({
						title: (error && error.message) || '加载商品失败',
						icon: 'none'
					})
				} finally {
					this.citySwitching = false
				}
			},
			goClassify(categoryName) {
				const navList = (uni.getStorageSync('cls') || []).filter(v => v.parentId == 0)
				const matched = navList.find(v => v.categoryName === categoryName)
					|| (categoryName === '聚会活动' ? navList.find(v => v.linkType === 'activity') : null)
				if (matched && matched.categoryId) {
					uni.setStorageSync('currentCls', matched.categoryId)
				}
				uni.setStorageSync('currentSubCls', 0)
				uni.switchTab({
					url: '/pages/classify/classify'
				})
			},
			/**
			 * 跳转点击
			 * @param {String} type 跳转类型
			 */
			onSkip(type, info) {
				switch (type) {
					case 'paycode':
						uni.navigateTo({
							url: '/packagesMember/PaymentCode/PaymentCode'
						})
						break;
					case 'menu':
						if (info.linkType == 'toMNP') {
							uni.navigateToMiniProgram({
								appId: info.linkId,
								path: '/pages/index/index',
								envVersion: "release",
								success: res => {
									if (res && res.errMsg) {
										uni.showToast({
											icon: 'none',
											title: res.errMsg
										})
									}
								},
								fail: err => {
									console.log('打开成功', err)
								}
							})
						} else if (info.linkType == 'activity') {
							uni.setStorageSync('currentCls', info.categoryId)
							uni.switchTab({
								url: '/pages/classify/classify'
							})
						} else {
							const currentClassify = info.categoryId || info.linkUrl;
							if (currentClassify) {
								uni.setStorageSync('currentCls', currentClassify)
							}
							uni.switchTab({
								url: '/pages/classify/classify'
							})
							// uni.navigateTo({
							// 	url: `/packagesMall/SearchGoodsList/SearchGoodsList?categoryId=${info.categoryId}`
							// })
						}
						break;
				}
			},
			goMall() {
				if (!this.currentCategoryId) return
				const categories = uni.getStorageSync('cls') || []
				const category = categories.find(item => (
					String(item.categoryId) === String(this.currentCategoryId)
				))
				const navbarId = category && category.parentId != 0
					? category.parentId
					: this.getTravelNavbarId()
				this.navigateToServicePage(this.currentCategoryId, navbarId)
			},
			// 商品详情（小程序用 dataset 传参，避免 v-for 闭包丢失）
			goProdDetail(e) {
				let goodsId = null
				let goodsType = ''
				if (e && e.currentTarget && e.currentTarget.dataset) {
					const ds = e.currentTarget.dataset
					goodsId = ds.goodsId
					goodsType = ds.goodsType || ''
				} else if (e && e.goodsId) {
					goodsId = e.goodsId
					goodsType = e.goodsType || ''
				}
				if (!goodsId) {
					uni.showToast({
						title: '商品信息无效',
						icon: 'none'
					})
					return
				}
				if (goodsType === 'hotel') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${goodsId}`
					})
				} else {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/GoodsDetails?id=${goodsId}`
					})
				}
			}
		}
	};
</script>

<style scoped lang="scss">
	@import 'home.scss';
</style>
