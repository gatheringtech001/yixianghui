<template>
	<view class="page">
		<u-navbar class="weapp-nav-box home-nav" :is-back="false" :background="{ background: '#ffffff' }" :border-bottom="false">
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
				<view class="home-hero" @click="goToServiceTab">
					<view class="hero-copy">
						<text class="hero-chip">康养旅居</text>
						<view class="hero-title">康养旅居精选</view>
						<view class="hero-desc">精选目的地 · 管家陪同 · 适老友好</view>
						<view class="hero-btn">查看推荐</view>
					</view>
					<image class="hero-image" :src="heroImage" mode="aspectFill" />
				</view>

				<view class="housekeeper">
					<image class="housekeeper-avatar" :src="housekeeperAvatarDisplay" mode="aspectFill" />
					<view class="housekeeper-copy">
						<view class="title">添加逸享荟小管家</view>
						<view class="slogan">全程管家式服务，为您的生活保驾护航</view>
					</view>
					<view class="housekeeper-btn" @click.stop="showContact = true">添加管家</view>
				</view>

				<view class="entry-grid">
					<view class="entry-card" @click="goClassify('全国旅居')">
						<image class="entry-bg" src="/static/home-design/entry-stay.jpg" mode="aspectFill" />
						<view class="entry-icon">
							<u-icon name="bag-fill" color="#111111" size="36" />
						</view>
						<view class="entry-copy">
							<text class="entry-title">全国旅居</text>
							<text class="entry-desc">旅居好去处</text>
						</view>
					</view>
					<view class="entry-card" @click="goClassify('聚会活动')">
						<image class="entry-bg" src="/static/home-design/entry-activity.jpg" mode="aspectFill" />
						<view class="entry-icon">
							<u-icon name="account-fill" color="#111111" size="36" />
						</view>
						<view class="entry-copy">
							<text class="entry-title">聚会活动</text>
							<text class="entry-desc">精彩活动汇聚</text>
						</view>
					</view>
					<view class="entry-card" @click="goClassify('老年教育')">
						<image class="entry-bg" src="/static/home-design/entry-stay.jpg" mode="aspectFill" />
						<view class="entry-icon">
							<u-icon name="file-text" color="#111111" size="36" />
						</view>
						<view class="entry-copy">
							<text class="entry-title">老年教育</text>
							<text class="entry-desc">线下课程报名</text>
						</view>
					</view>
				</view>

				<view class="section-head">
					<text class="section-title">热门城市</text>
					<text class="section-more" @click="goToServiceTab">查看全部 &gt;</text>
				</view>
				<view class="city-grid">
					<view
						class="city-card"
						v-for="(item,index) in hotCardList"
						:key="index"
						:class="{ active: selectedCityIndex === index }"
						@click="changeSiteHandle(item, index)"
					>
						<image class="city-image" :src="host + item.adImage" mode="aspectFill" />
						<text class="city-name">{{ item.adName }}</text>
					</view>
				</view>

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
								<view class="product-tags" v-if="goods.tags && goods.tags != ''">
									<text
										class="product-tag"
										v-for="(tag, j) in goods.tags.split(/[\,|，]/).slice(0, 3)"
										:key="j"
									>{{ tag }}</text>
								</view>
								<view class="product-price-row">
									<view class="member-price">
										<text>会员价</text>
										<text class="price">¥{{ goods.vipPrice || goods.price }}</text>
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
					<u-icon name="bill-fill" size="40" color="#00C800" />
					<text>优惠活动不错过</text>
				</view>
			</view>
			<view class="steps" v-if="contact[0] && contact[0].adImage">
				<view class="title"><text></text>第一步</view>
				<view class="content">
					<image
						:src="getAdImageUrl(contact[0].adImage)"
						mode="aspectFit"
						show-menu-by-longpress
						@tap="previewQrImage(contact[0].adImage)"
					/>
					<view class="tips">长按识别二维码添加</view>
				</view>
			</view>
			<view class="steps" v-if="contact[1] && contact[1].adImage">
				<view class="title"><text></text>第二步</view>
				<view class="content else">
					<image :src="getAdImageUrl(contact[1].adImage)" mode="widthFix" />
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
	import { parseInvitePageOptions } from '@/utils/invite'
	import {
		getSite,
		getBannerPosList,
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
	export default {
		components: {
			TabBar
		},
		data() {
			return {
				host: this.$host,
				siteInfo: null,
				swiperList: [],
				hotCardList: [],
				slideNum: 0,
				navList: [],
				hotCategoryList: [],
				hotGoods: [],
				classifyShow: 0,
				showContact: false,
				contact: [],
				selectedCityIndex: -1,
				currentCategoryId: null,
				currentCategoryInfo: null,
				currentGoodsList: [],
				citySelectionReady: false,
				citySwitching: false,
				refreshing: false,
				refreshHintText: '下拉刷新',
				scrollViewHeight: 0,
				brandLogoUrl: '',
				housekeeperAvatarUrl: ''
			}
		},
		onReady() {
			uni.hideTabBar();
			this.setScrollViewHeight();
		},
		onLoad(options) {
			parseInvitePageOptions(options)
			this.loadMnpAdAssets()
			this.getAdPosList()
			this.gethotCardList()
			this.getClsList()
			this.getContactAdList()
		},
		async onShow() {
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
				if (this.swiperList && this.swiperList.length && this.swiperList[0].adImage) {
					const img = this.swiperList[0].adImage
					return img.startsWith('http') ? img : this.host + img
				}
				return '/static/home-design/hero-banner.jpg'
			}
		},
		methods: {
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
						this.getAdPosList(),
						this.gethotCardList(true),
						this.getClsList(true),
						this.getContactAdList()
					])
					const site = uni.getStorageSync('site')
					if (site) {
						this.siteInfo = site
					}
					if (prevIndex >= 0 && prevIndex < this.hotCardList.length) {
						this.selectedCityIndex = prevIndex
						const parsed = this.parseCityCardLink(this.hotCardList[prevIndex])
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
				const aliasMap = {
					'全国': ['全国旅居', '全国'],
					'大理': ['大理', '分类4'],
					'腾冲': ['腾冲', '分类2'],
					'曲靖': ['曲靖', '分类3'],
					'昆明': ['昆明']
				}
				const allCategories = uni.getStorageSync('cls') || this.hotCategoryList || []
				const candidates = aliasMap[name] || [name]
				for (let i = 0; i < candidates.length; i++) {
					const matched = allCategories.find(v => v.categoryName === candidates[i])
					if (matched) return matched.categoryId
				}
				const fuzzy = allCategories.find(v =>
					v.categoryName.includes(name) || name.includes(v.categoryName)
				)
				return fuzzy ? fuzzy.categoryId : null
			},
			// linkUrl 保存商品分类 ID
			parseCityCardLink(item) {
				if (!item) return null
				const link = String(item.linkUrl || '').trim()
				if (link && /^\d+$/.test(link)) {
					return { categoryId: link }
				}
				const categoryId = this.findCategoryIdByName(item.adName)
				if (categoryId) {
					return { categoryId: String(categoryId) }
				}
				return null
			},
			updateCurrentCategoryInfo() {
				if (!this.currentCategoryId) {
					this.currentCategoryInfo = null
					return
				}
				const allCategories = uni.getStorageSync('cls') || this.hotCategoryList || []
				this.currentCategoryInfo = allCategories.find(
					v => String(v.categoryId) === String(this.currentCategoryId)
				) || null
			},
			initCitySelection() {
				if (!this.hotCardList.length) return
				if (this.citySelectionReady && this.selectedCityIndex >= 0) {
					this.loadCurrentGoodsList()
					return
				}
				const index = this.hotCardList.findIndex(item => {
					if (this.isNationalCityCard(item)) return false
					const parsed = this.parseCityCardLink(item)
					return parsed && parsed.categoryId
				})
				if (index >= 0) {
					const parsed = this.parseCityCardLink(this.hotCardList[index])
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
					this.currentGoodsList = list.filter(v => v && v.goodsId)
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
			async getAdPosList() {
				let {
					data
				} = await getBannerPosList()
				this.getAdList(data[0].positionId)
			},
			async getAdList(positionId) {
				let params = {
					positionId: positionId
				}
				let {
					data
				} = await getBannerList(params)
				this.swiperList = data
				// console.log(data)
			},
			async gethotCardList(skipInit = false) {
				let {
					data
				} = await getBannerList({
					positionId: 6
				})
				this.hotCardList = data || []
				if (!skipInit) {
					this.initCitySelection()
				}
			},
			async getContactAdList() {
				let params = {
					positionId: 2
				}
				let {
					data
				} = await getBannerList(params)
				this.contact = data
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
			// 切换热门城市：linkUrl 为商品分类 ID，仅按分类加载推荐商品
			async changeSiteHandle(cardItem, index) {
				if (this.isNationalCityCard(cardItem)) {
					this.goToServiceTab()
					return
				}
				if (this.citySwitching) return
				let parsed = this.parseCityCardLink(cardItem)
				if ((!parsed || !parsed.categoryId) && !(uni.getStorageSync('cls') || []).length) {
					try {
						await this.getClsList()
					} catch (e) {}
					parsed = this.parseCityCardLink(cardItem)
				}
				if (!parsed || !parsed.categoryId) {
					uni.showToast({
						title: '未配置商品分类，请在广告链接填写分类ID',
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
				const allCategories = uni.getStorageSync('cls') || []
				const subCategoryId = this.currentCategoryId
				const subCat = allCategories.find(v => String(v.categoryId) === String(subCategoryId))
				const navList = allCategories.filter(v => v.parentId == 0)
				let navbarId = null
				if (subCat && subCat.parentId != 0) {
					navbarId = subCat.parentId
				} else {
					const matchedNav = navList.find(v => String(v.categoryId) === String(subCategoryId))
					if (matchedNav) navbarId = matchedNav.categoryId
				}
				this.navigateToServicePage(subCategoryId, navbarId || this.getTravelNavbarId())
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
