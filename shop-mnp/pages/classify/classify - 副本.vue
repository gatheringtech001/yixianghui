<template>
	<view class="page" ref="page">
		<u-navbar class="weapp-nav-box" :is-back="false" :background="{ background: '#EFEBDF' }" :border-bottom="false">
			<view class="slot-wrap top-box">
				<view class="top-search">
					<view class="icon-info" @click="getSite">
						<text class="cuIcon-location"></text>
						<text class="city">{{siteInfo.deptName || '昆明'}}</text>
					</view>
					<view class="search" @click="onSearch">
						<view class="icon">
							<u-icon name="search" color="#333" size="36" />
						</view>
						<view class="hint">
							<text class="max">搜索</text>
							<text class="min">热门商品</text>
						</view>
					</view>
				</view>
			</view>
		</u-navbar>
		<view class="cls-box">
			<view class="item" v-for="(item, index) in navList" :key="index" @click="getSub(item, index)">
				<view class="item-img">
					<image :src='host + item.categoryIcon' mode=""></image>
				</view>
				<view class="item-title" :class="[cut == index?'action':'']">{{item.categoryName}}</view>
			</view>
		</view>
		<!-- 分类数据 -->
		<view class="classify-data" :style="{height: `${height}px`}">
			<view class="classify-one">
				<scroll-view scroll-y class="classify-list">
					<view class="list" v-for="(sub, index) in leftList" :key="index" :class="[current == index?'action':'']" @click="getSubGoods(sub, index)">
						<text>{{sub.categoryName}}</text>
					</view>
				</scroll-view>
			</view>
			<view class="classify-two-three">
				<scroll-view scroll-y class="scroll">
					<view class="classify-two">
						<view class="classify-three" v-if="!loading && goodsList&&goodsList.length > 0">
							<view class="list" v-for="(goods, index) in goodsList" :key="index" @click="goProdDetail(goods)">
								<view class="img-box" v-if="goods.goodsCover">
									<image :src='host + goods.goodsCover' mode="widthFix" />
								</view>
								<view class="goods-content">
									<view class="name">{{goods.goodsName}}</view>
									<view class="tag" v-if="goods.tags && goods.tags != ''">
										<text v-for="(tag, j) in goods.tags.split(/[\,|，]/)" :key="j">{{tag}}</text>
									</view>
									<view class="price" :class="[!goods.goodsCover || goods.goodsCover == '' ? 'flex' : '']">
										<view class="num">
											<text class="price-num">{{goods.price || 0}}</text>
											<text class="unit">{{goods.unit}}</text>
										</view>
										<view class="bootom">
											<text class="price-vip">会员 ￥{{goods.vipPrice}}</text>
											<text class="btn">{{goods.goodsType == 'online' || goods.goodsType == 'o2o'?'购买':'预订'}}</text>
										</view>
									</view>
								</view>
							</view>
						</view>
						<view class="classify-three empty" v-else>
							<u-empty text="暂无数据" mode="list"></u-empty>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>

		<!-- tabbar -->
		<TabBar ref="tabsBar" class="tabs-bar" :tabBarShow="1"></TabBar>
	</view>
</template>

<script>
	import TabBar from '@/components/TabBar/TabBar.vue'
	import LocationService from '@/utils/location'
	import initAuthorization from '@/utils/login'
	import { isTokenExpired } from '@/utils/auth'
	import { getSite } from '@/api/index'
	import { getGoodsCatrgorys, getGoodsList } from '@/api/shop/index'
	export default {
		components:{
			TabBar,
		},
		data() {
			return {
				host: this.$host,
				siteInfo: null,
				loading: true,
				height: 0,
				navList: [],
				leftList: [],
				cut: 0,
				current: 0,
				goodsList: []
			};
		},
		onReady() {
			setTimeout(()=>{
			  uni.hideTabBar()  
			},100)
		},
		async onLoad() {
			let token = uni.getStorageSync('token'), userInfo = uni.getStorageSync('userInfo')
			if(!token || token == '' || token == undefined || !userInfo || userInfo == '' || userInfo == undefined) await initAuthorization()
			this.getClsList()
		},
		async onShow() {
			let token = uni.getStorageSync('token')
			if (token && isTokenExpired()) await initAuthorization()
			let site = uni.getStorageSync('site')
			if(!site || site == undefined) this.getCurrentLocation()
			else this.siteInfo = site
			
			if(this.navList.length > 0) {
				let cls = uni.getStorageSync('currentCls')
				if (cls && cls != '' && cls != undefined) {
					let index = this.navList.findIndex(v => v.categoryId == cls)
					this.cut = index
					this.getSubClsList(cls)
				} else {
					this.cut = 0
					this.getSubClsList(this.navList[0].categoryId)
				}
			}
		},
		methods:{
			async getCurrentLocation() {
				try {
					// 获取经纬度
					const { latitude, longitude } = await LocationService.getLocation()
					let params = {
						lat: latitude,
						lng: longitude
					}
					let { data } = await getSite(params)
					this.siteInfo = data
					uni.setStorageSync('site', data)
				} catch (error) {
					console.error('定位错误:', error)
				}
			},
			getSite() {
				uni.navigateTo({
					url: `/packagesPublic/site/index?id=${this.siteInfo.deptId}`
				})
			},
			async getClsList() {
				// let { data } = await getGoodsCatrgorys()
				let data = uni.getStorageSync('cls')
				this.current = 0
				this.navList = data.filter(v => v.parentId == 0)
				this.$nextTick(() => {
					const systemInfo = uni.getSystemInfoSync()
					let height = systemInfo.windowHeight - systemInfo.safeAreaInsets.bottom - systemInfo.statusBarHeight
					let bar = uni.upx2px(88)
					let cls = uni.createSelectorQuery().select(".cls-box")
					cls.boundingClientRect(res => {
						this.height = height - res.height - res.top - bar
		　　    		}).exec()
				})
				let cls = uni.getStorageSync('currentCls')
				if (cls && cls != '' && cls != undefined) {
					let index = this.navList.findIndex(v => v.categoryId == cls)
					this.cut = index
					this.getSubClsList(cls)
				} else {
					this.cut = 0
					this.getSubClsList(this.navList[0].categoryId)
				}
			},
			async getSubClsList(categoryId) {
				uni.showLoading({
				    title: '数据加载中...'
				})
				let params = {
					parentId: categoryId
				}
				let { data } = await getGoodsCatrgorys(params)
				this.leftList = data
				this.leftList.unshift({
					categoryName: '推荐'
				})
				this.getSubGoods(null, 0)
			},
			getSub(item, index) {
				if (item.linkType == 'toMNP') {
					uni.navigateToMiniProgram({
						appId: item.linkId,
						path: '/pages/index/index',
						envVersion: "release",
						success: res => {
							if(res && res.errMsg) {
								uni.showToast({
									icon: 'none',
									title: res.errMsg
								})
							}
						},
						fail: err => {
							 console.log('打开成功', err);
						}
					})
					
				} else if(item.linkType == 'activity') {
					uni.navigateTo({
						url: '/packagesMall/Activity/index'
					})
				} else {
					this.cut = index
					this.current = 0
					this.getSubClsList(item.categoryId)
				}
			},
			getSubGoods(item, index) {
				this.current = index
				if(index == 0) var categoryId = this.navList[this.cut]?.categoryId
				else var categoryId = item.categoryId
				getGoodsList({ categoryId: categoryId }).then(res => {
					this.goodsList = res.data
					if(index == 0) this.goodsList = res.data?.filter(v => v.isTop == '1')
					else this.goodsList = res.data
					this.loading = false
					uni.hideLoading()
				})
			},
			// 商品详情
			goProdDetail(item){
				if(item.goodsType == 'hotel') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${item.goodsId}`
					})
				} else {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/GoodsDetails?id=${item.goodsId}`
					})
				}
			},
			/**
			 * 搜索点击
			 */
			onSearch(){
				uni.navigateTo({url:'/packagesMall/search/search'})
			},
		}
	}
</script>

<style scoped lang="scss">
@import 'classify.scss'
</style>
