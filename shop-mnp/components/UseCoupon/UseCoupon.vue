<template>
	<view class="page-total">
		<view class="cu-modal bottom-modal" :class="{ show: isShow }" @click="hide">
			<view class="cu-dialog">
				<view class="coupon-title">
					<view class="title">优惠券</view>
					<view class="explain">使用说明</view>
				</view>
				<!-- <view class="coupon-tab">
					<view class="tab" :class="{'action':TabShow===0}" @click.stop="onTab(0)">
						<text>可用优惠券（1）</text>
						<text class="line"></text>
					</view>
					<view class="tab" :class="{'action':TabShow===1}" @click.stop="onTab(1)">
						<text>不可用优惠券（1）</text>
						<text class="line"></text>
					</view>
				</view> -->
				<!-- 优惠券数据 -->
				<view class="coupon-data">
					<view class="coupon-list" v-if="coupons.length > 0">
						<view class="list" :class="{'forbidden':TabShow === 1}" v-for="(item,index) in coupons" :key="index">
							<view class="coupon-price">
								<view class="discounts">
									<text class="min">￥</text>
									<text class="max">{{item.couponInfo.discountPrice}}</text>
								</view>
								<view class="full-reduction">
									<text>{{item.couponInfo.couponName}}</text>
								</view>
								<view class="jag"></view>
							</view>
							<view class="coupon-info">
								<view class="check" v-show="TabShow === 0">
									<view class="iconfont icon-check"></view>
								</view>
								<view class="info-title">
								<view class="tag"><text>{{item.couponInfo.couponType == '1'?'满减券':'折扣券'}}</text></view>
								<view class="title"><text>{{item.couponInfo.couponName}}</text></view>
								</view>
								<view class="date-get">
									<view class="date">
										<text>{{item.couponInfo.enableStartTime}} 至 {{item.couponInfo.enableEndTime}}</text>
									</view>
									<!-- <view class="get">
										<text>点击领取</text>
									</view> -->
								</view>
							</view>
						</view>
					</view>
					<view class="empty" v-else>
						<u-empty text="暂无可用优惠券" mode="list"></u-empty>
					</view>
				</view>
				<!--确认 -->
				<view class="cpupon-confirm">
					<view class="confirm" @click.stop="onConfirm">确定</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { getEnableCouponList } from '@/api/member/index'
	export default {
		data() {
			return {
				isShow: false,
				TabShow: 0,
				coupons: []
			};
		},
		methods:{
			show(goodsId){
				getEnableCouponList({goodsId: goodsId}).then(res => {
					this.isShow = true
					this.coupons = res.rows
				})
			},
			hide(){
				this.isShow = false;
			},
			/**
			 * tab 点击
			 */
			onTab(index){
				this.TabShow = index;
			},
			/**
			 * 确认点击
			 */
			onConfirm(){
				this.hide();
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'UseCoupon.scss';
</style>
