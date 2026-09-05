<template>
	<view class="page">
		<!-- 优惠券tab -->
		<view class="coupon-tab">
			<view class="tab" :class="{'action':TabShow===0}" @click="onCouponTab(0)">
				<text>未使用</text>
				<text class="line"></text>
			</view>
			<view class="tab" :class="{'action':TabShow===1}" @click="onCouponTab(1)">
				<text>已使用</text>
				<text class="line"></text>
			</view>
			<view class="tab" :class="{'action':TabShow===2}" @click="onCouponTab(2)">
				<text>已失效</text>
				<text class="line"></text>
			</view>
		</view>
		<!-- 优惠券列表 -->
		<view class="coupon-list">
			<view v-if="error" class="wallet-state" @click="getCoupons()">{{ error }}，点击重试</view>
			<view class="list" v-for="(item,index) in recList" :key="index">
				<view class="list-data" :class="{'coupon-lose':TabShow!=0}">
					<view class="coupon-price">
						<view class="discounts">
							<text class="min" v-if="item.couponInfo.discountType !== '2'">￥</text>
							<text class="max">{{ couponValue(item.couponInfo) }}</text>
						</view>
						<view class="full-reduction"><text>{{item.couponInfo.couponName}}</text></view>
						<view class="jag"></view>
					</view>
					<view class="coupon-info">
						<view class="info-title">
							<view class="tag"><text>{{item.couponInfo.discountType === '2'?'折扣券':'满减券'}}</text></view>
							<view class="title"><text>{{item.couponInfo.couponName}}</text></view>
						</view>
						<view class="date-get">
							<view class="date"><text>{{item.couponInfo.enableStartTime}} 至 {{item.couponInfo.enableEndTime}}</text></view>
							<view class="get" @click="onCouponUse(item)" v-if="TabShow===0">
								<text>立即使用</text>
							</view>
						</view>
						<view class="describe-title">
							<text>满￥{{ item.couponInfo.minPrice || 0 }}可用 · {{ Number(item.couponInfo.goodsId) > 0 ? '仅限指定商品' : (Number(item.couponInfo.categoryId) > 0 ? '仅限指定分类' : '适用商品通用') }}</text>
							<u-parse :html="item.couponInfo.couponContent"></u-parse>
							<!-- <text>详细信息</text> -->
							<!-- <text class="iconfont icon-more more" :style="isDes?'transform: rotate(-90deg);':'transform: rotate(90deg);'"></text> -->
						</view>
					</view>
				</view>
				<view class="use-status" v-if="TabShow != 0">
					<text v-if="TabShow === 1">已使用</text>
					<text v-else-if="TabShow === 2">已失效</text>
				</view>
			</view>
			<view v-if="loading" class="wallet-state">加载中…</view>
			<button v-else-if="hasMore && !error" class="wallet-more" @click="getCoupons(false)">加载更多优惠券</button>
			<view class="empty" v-if="!loading && !error && !hasMore && recList.length == 0">
				<u-empty text="暂无优惠券" mode="list"></u-empty>
			</view>
		</view>
	</view>
</template>

<script>
import { getMyCouponList } from '@/api/member/index'
import { getGoodsInfo } from '@/api/shop/index'
export default {
	data() {
		return {
			TabShow: 0,
			isDes: false,
			total: 0,
			couponList: [],
			recList: [], loading: false, error: '', pageNum: 0, hasMore: true
		};
	},
	onShow() {
		this.getCoupons()
	},
	onReachBottom() { if (this.hasMore && !this.error) this.getCoupons(false) },
	methods:{
		async getCoupons(reset = true) {
			if (this.loading) return
			this.loading = true
			this.error = ''
			try {
				const pageNum = reset ? 1 : this.pageNum + 1
				const {rows, total} = await getMyCouponList({pageNum, pageSize:50})
				this.total = Number(total)
				this.pageNum = pageNum
				this.couponList = reset ? rows : this.couponList.concat(rows)
				this.hasMore = this.couponList.length < this.total
				this.onCouponTab(this.TabShow)
			} catch (error) { this.error = error.message || '优惠券加载失败' }
			finally { this.loading = false }
		},
		couponState(item) {
			if (Number(item.isUsed) === 1) return 1
			const coupon = item.couponInfo
			if (!coupon || String(item.status) !== '1' || (coupon.status && String(coupon.status) !== '1')) return 2
			const end = coupon.enableEndTime && new Date(String(coupon.enableEndTime).replace(/-/g, '/')).getTime()
			return end && end < Date.now() ? 2 : 0
		},
		couponValue(coupon) {
			return coupon.discountType === '2' ? `${Number(coupon.discountPrice) / 10}折` : coupon.discountPrice
		},
		/**
		 * 优惠券tab点击
		 * @param {Number} type
		 */
		onCouponTab(type){
			this.TabShow = type
			this.recList = this.couponList.filter(item => this.couponState(item) === type)
		},
		/**
		 * 去使用点击
		 */
		async onCouponUse(item){
			if (this.couponState(item) !== 0) return
			const coupon = item.couponInfo
			try {
				const start = coupon.enableStartTime && new Date(String(coupon.enableStartTime).replace(/-/g, '/')).getTime()
				if (start && start > Date.now()) throw new Error('优惠券尚未到可用时间')
				if (Number(coupon.goodsId) > 0) {
					const {data} = await getGoodsInfo(coupon.goodsId)
					const pages = {online:'GoodsDetails',hotel:'SojournGoodsDetails',education:'EducationGoodsDetails'}
					if (!data || !pages[data.goodsType]) throw new Error('适用商品暂不可用')
					uni.navigateTo({url:`/packagesMall/GoodsDetails/${pages[data.goodsType]}?id=${coupon.goodsId}`})
				} else {
					uni.navigateTo({url:`/packagesMall/SearchGoodsList/SearchGoodsList?couponScope=1&categoryId=${Number(coupon.categoryId) || ''}`})
				}
			} catch (error) { uni.showToast({title:error.message || '打开适用商品失败',icon:'none'}) }
		}
	}
};
</script>

<style scoped lang="scss">
@import 'MyCoupon.scss';
.wallet-state { padding: 28rpx; text-align: center; color: #701018; font-size: 28rpx; }
.wallet-more { margin: 24rpx auto; color: #701018; font-size: 28rpx; background: #f5f0e8; }
.coupon-list .list .list-data { height: auto; min-height: 240rpx; align-items: stretch; }
.coupon-list .list .list-data .coupon-price { height: auto; }
.coupon-list .list .list-data .coupon-info { height: auto; padding-top: 16rpx; padding-bottom: 16rpx; }
.coupon-list .list .list-data .coupon-info .date-get { height: auto; min-height: 80rpx; gap: 16rpx; }
.coupon-list .list .list-data .coupon-info .describe-title { height: auto; display: block; line-height: 1.5; }
</style>
