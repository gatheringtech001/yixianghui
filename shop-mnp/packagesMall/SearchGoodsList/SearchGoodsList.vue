<template>
	<view class="page">
		<!-- 搜索 -->
		<view class="search-head">
			<view class="icon-info" v-if="!couponScope" @click="getSite">
				<text class="cuIcon-location"></text>
				<text class="city">{{siteInfo.deptName || '昆明'}}</text>
			</view>
			<view class="search">
				<text class="iconfont icon-fadajing"></text>
				<input type="text" v-model="goodsName" placeholder="搜索商品" />
				<text class="search-text" @click="getGoods">搜索</text>
			</view>
			<view class="cut" @click="isList = !isList">
				<text class="iconfont" :class="isList?'icon-shitu01':'icon-shitu02'"></text>
			</view>
		</view>
		<!-- 商品列表 -->
		<view class="goods-data">
			<view v-if="couponScope" class="catalog-state">优惠券适用商品，使用门槛以结算页为准</view>
			<view v-if="error" class="catalog-state" @click="getGoods">{{ error }}，点击重试</view>
			<view v-if="loading" class="catalog-state">加载中…</view>
			<mescroll-body ref="mescrollRef"
				@init="mescrollInit"
				@down="downCallback"
				@up="upCallback"
				:down="downOption"
				:up="upOption"
				:top="0">
				<view class="goods-list" v-if="goodsList.length > 0">
					<view :class="isList?'list-view':'list-li'" v-for="(item,index) in goodsList" @click="onGoodsDetails(item)" :key="index">
						<view class="thumb">
							<image :src="/^https?:/.test(item.goodsCover) ? item.goodsCover : host + item.goodsCover" mode="widthFix"></image>
						</view>
						<view class="item">
							<view class="title">
								<text class="two-omit">{{item.goodsName}}</text>
							</view>
							<view class="tag" v-if="item.tags && item.tags != ''">
								<text v-for="(tag, j) in item.tags.split(/[\,|，]/)" :key="j">{{tag}}</text>
							</view>
							<view class="price">
								<view class="retail-price">
									<text class="min">￥</text>
									<text class="max">{{item.price || 0}}</text>
								</view>
								<!-- 会员价暂时隐藏 -->
								<!-- <view class="vip-price">会员价：￥{{item.vipPrice}}</view> -->
							</view>
						</view>
					</view>
				</view>
				<view class="empty" v-else-if="!loading && !error">
					<u-empty text="暂无商品" mode="list"></u-empty>
				</view>
			</mescroll-body>
		</view>
		
	</view>
</template>

<script>
	// 引入mescroll-mixins.js
	import MescrollMixin from "@/components/mescroll-uni/mescroll-mixins.js";
	import { getGoodsList } from '@/api/shop/index'
	import LocationService from '@/utils/location'
	import { getSite } from '@/api/index'
	
	export default {
		mixins: [MescrollMixin], // 使用mixin
		data() {
			return {
				host: this.$host,
				siteInfo: {},
				couponScope: false, loading: false, error: '',
				mescroll: null, // mescroll实例对象 (此行可删,mixins已默认)
				// 下拉刷新的配置(可选, 绝大部分情况无需配置)
				downOption: {},
				// 上拉加载的配置(可选, 绝大部分情况无需配置)
				upOption: {
				},
				// 列表视图切换
				isList: true,
				// 筛选弹窗
				isScreen: false,
				// 筛选切换
				screenShow: 0,
				// 抽屉
				isDrawer: false,
				goodsName: '',
				categoryId: '',
				goodsList:[],
			}
		},
		onLoad(option) {
			this.couponScope = option.couponScope === '1'
			if(option.keyword) this.goodsName = decodeURIComponent(option.keyword)
			if(option.categoryId) this.categoryId = option.categoryId
			this.getGoods()
		},
		onShow() {
			if (this.couponScope) return
			const prevDeptId = this.siteInfo && this.siteInfo.deptId
			let site = uni.getStorageSync('site')
			if (!site || site == undefined) {
				this.getCurrentLocation()
				return
			}
			this.siteInfo = site
			if (prevDeptId !== site.deptId) {
				this.getGoods()
			}
		},
		methods: {
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
					this.getGoods()
				} catch (error) {
					console.error('定位错误:', error)
				}
			},
			getSite() {
				uni.navigateTo({
					url: `/packagesPublic/site/index?id=${this.siteInfo.deptId}`
				})
			},
			async getGoods() {
				if (this.loading) return
				this.loading = true
				this.error = ''
				let params = {
					goodsName: this.goodsName,
					categoryId: this.categoryId,
					ignoreSite: this.couponScope
				}
				try {
					const res = await getGoodsList(params)
					this.goodsList = res.data
					if (this.mescroll) this.mescroll.endSuccess(this.goodsList.length, false)
				} catch (error) {
					this.error = error.message || '商品加载失败'
					if (this.mescroll) this.mescroll.endErr()
				} finally { this.loading = false }
			},
			/*下拉刷新的回调, 有三种处理方式:*/
			downCallback(){
				this.getGoods()
			},
			/*上拉加载的回调*/
			upCallback(page) {
				if (!this.loading) this.mescroll.endSuccess(this.goodsList.length, false)
			},
			/**
			 * 返回点击
			 */
			onBack(){
				uni.navigateBack();
			},
			/**
			 * 综合点击
			 * @param {Number} type
			 */
			onSynthesize(type){
				this.screenShow = type;
				this.isScreen = !this.isScreen;
			},
			/**
			 * 商品列表点击
			 */
			onGoodsDetails(item){
				/* uni.navigateTo({
					url: `/packagesMall/GoodsDetails/GoodsDetails?id=${item.goodsId}`
				}) */
				if (item.goodsType == 'hotel') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${item.goodsId}`
					})
				} else if (item.goodsType === 'education') {
					uni.navigateTo({url:`/packagesMall/GoodsDetails/EducationGoodsDetails?id=${item.goodsId}`})
				} else {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/GoodsDetails?id=${item.goodsId}`
					})
				}
			},
			
			goCart() {
				uni.navigateTo({
					url: `/packagesMall/cart/cart`
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import  'SearchGoodsList.scss';
	.catalog-state { padding: 24rpx; font-size: 28rpx; color: #701018; text-align: center; }
</style>
