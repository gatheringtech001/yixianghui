<template>
	<view class="page">
		<!-- 商品 -->
		<view class="goods-data" v-if="orderDetail">
			<view class="goods-list">
				<view class="list" v-for="(item, index) in orderDetail.goodsList" :key="index">
					<view class="thumb">
						<image :src="host + item.goodsCover" mode=""></image>
					</view>
					<view class="item">
						<view class="title">
							<text class="one-omit">{{item.goodsName}}</text>
						</view>
						<view class="num-size">
							<text>数量：{{orderDetail.goodsCount}}</text>
						</view>
						<view class="price">
							<text>￥{{ formatMoney(displayOrderAmount) }}元</text>
						</view>
					</view>
				</view>
			</view>
		</view>
		<!-- 售后类型选择 -->
		<view class="type-select">
			<view class="type-list">
				<view class="list" @click="onReturnType(0)">
					<view class="title">
						<image src="/static/sale_tk.png" mode=""></image>
						<text>退款</text>
					</view>
					<view class="content">
						<text>申请商品的退款</text>
						<text class="iconfont icon-more"></text>
					</view>
				</view>
				<view class="list" @click="onReturnType(1)" v-if="isBackBargon">
					<view class="title">
						<image src="/static/sale_th.png" mode=""></image>
						<text>退货</text>
					</view>
					<view class="content">
						<text>申请商品的退货</text>
						<text class="iconfont icon-more"></text>
					</view>
				</view>
				<view class="list" @click="onReturnType(2)" v-if="isTransferBargon">
					<view class="title">
						<image src="/static/sale_hh.png" mode=""></image>
						<text>换货</text>
					</view>
					<view class="content">
						<text>申请商品的换货</text>
						<text class="iconfont icon-more"></text>
					</view>
				</view>
			</view>
		</view>
		<!-- 联系客服 -->
		<view class="contact-service" @click="goCustomerService">
			<view class="btn">
				<text class="iconfont icon-kefu"></text>
				<text>联系客服</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getOrderDetail } from '@/api/member/index'
	export default {
		data() {
			return {
				host: this.$host,
				orderDetail: null,
				isBackBargon: false,
				isTransferBargon: false
			};
		},
	    onLoad(option) {
			this.getOrderInfo(option.orderId)
		},
		computed: {
			displayOrderAmount() {
				if (!this.orderDetail) return 0
				const paid = Number(this.orderDetail.payMoney)
				if (Number.isFinite(paid) && paid > 0) return paid
				const payable = Number(this.orderDetail.moneyPayable)
				return Number.isFinite(payable) ? payable : 0
			}
		},
		methods:{
		 formatMoney(value) {
				const num = Number(value)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			},
		 getOrderInfo(orderId) {
				getOrderDetail({ orderId: orderId }).then(res => {
					this.orderDetail = res.data
				})
			},
			/**
			 * 退款类型点击
			 */
			onReturnType(type){
				uni.navigateTo({
					url: '/packagesMall/ReturnDetails/ReturnDetails?type=' + type + '&orderId='+this.orderDetail.orderId,
				})
			},
			goCustomerService() {
				uni.switchTab({
					url: '/pages/MembersOpened/MembersOpened',
					fail: () => {
						uni.reLaunch({
							url: '/pages/MembersOpened/MembersOpened'
						})
					}
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'AfterSaleType.scss';
</style>
