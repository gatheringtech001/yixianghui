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
			<view class="list" v-for="(item,index) in recList" :key="index">
				<view class="list-data" :class="{'coupon-lose':TabShow!=0}">
					<view class="coupon-price">
						<view class="discounts">
							<text class="min">￥</text>
							<text class="max">{{item.couponInfo.discountPrice}}</text>
						</view>
						<view class="full-reduction"><text>{{item.couponInfo.couponName}}</text></view>
						<view class="jag"></view>
					</view>
					<view class="coupon-info">
						<view class="info-title">
							<view class="tag"><text>{{item.couponInfo.couponType == '1'?'满减券':'折扣券'}}</text></view>
							<view class="title"><text>{{item.couponInfo.couponName}}</text></view>
						</view>
						<view class="date-get">
							<view class="date"><text>{{item.couponInfo.enableStartTime}} 至 {{item.couponInfo.enableEndTime}}</text></view>
							<view class="get" @click="onCouponUse" v-if="TabShow===0">
								<text>立即使用</text>
							</view>
						</view>
						<view class="describe-title">
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
			<view class="empty" v-if="recList.length == 0">
				<u-empty text="暂无优惠券" mode="list"></u-empty>
			</view>
		</view>
	</view>
</template>

<script>
import { getMyCouponList } from '@/api/member/index'
export default {
	data() {
		return {
			TabShow: 0,
			isDes: false,
			total: 0,
			couponList: [],
			recList: []
		};
	},
	onShow() {
		this.getCoupons()
	},
	methods:{
		async getCoupons() {
			let {rows, total} = await getMyCouponList()
			this.total = total
			this.couponList = rows
			this.recList = rows.filter(v => v.isUsed == 0)
		},
		/**
		 * 优惠券tab点击
		 * @param {Number} type
		 */
		onCouponTab(type){
			this.TabShow = type
			if(type == 0) this.recList = this.couponList.filter(v => v.status == 1 && v.isUsed == 0)
			if(type == 1) this.recList = this.couponList.filter(v => v.status == 1 && v.isUsed == 1)
			if(type == 2) this.recList = this.couponList.filter(v => v.status == 0)
		},
		/**
		 * 去使用点击
		 */
		onCouponUse(){
			uni.navigateTo({
				url: '/packagesMall/SearchGoodsList/SearchGoodsList'
			})
		}
	}
};
</script>

<style scoped lang="scss">
@import 'MyCoupon.scss';
</style>
