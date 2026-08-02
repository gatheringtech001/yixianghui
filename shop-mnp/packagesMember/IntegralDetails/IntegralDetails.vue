<template>
	<view class="page">
		<!-- 头部背景 -->
		<view class="head-bg">
			<view class="head-user">
				<view class="user">
					<image src="/static/img/user_pic.jpg" mode=""></image>
					<text>爱跳舞的汤姆</text>
				</view>
				<view class="exchange" @click="onExchangeRecord">
					<text>兑换记录</text>
					<text class="iconfont icon-more"></text>
				</view>
			</view>
			<view class="integral-balance">
				<view class="integral" @click="onIntegral">
					<text>当前可用金币</text>
					<text class="number">10000</text>
				</view>
			</view>
			<view class="bg">
				<image src="/static/integral_bg1.png" mode=""></image>
			</view>
		</view>
		<!-- 优惠券兑换专区 -->
		<view class="coupon-exchange">
			<view class="exchange-title">
				<view class="title">优惠券兑换</view>
			</view>
			<view class="coupon-list">
				<view class="list" v-for="(item,index) in 3"
					@click="onExchange(0)"
					:key="index">
					<view class="thumb">
						<image src="" mode=""></image>
					</view>
					<view class="title">50元直减优惠券</view>
					<view class="integral">1200金币</view>
				</view>
			</view>
		</view>
		<!-- 精选金币兑换 -->
		<view class="handpick-exchange">
			<view class="exchange-title">
				<view class="title">精选兑换</view>
			</view>
			<view class="handpick-list">
				<view class="list" @click="onExchange(1)">
					<view class="thumb">
						<image :src="'/static/img/goods_02.png'" mode=""></image>
					</view>
					<view class="title">
						<text class="two-omit">aa</text>
					</view>
					<view class="price-integral">
						<text class="integral">10000金币</text>
						<text class="price">￥1999.00</text>
					</view>
				</view>
			</view>
		</view>
		<!-- 超级会员专属兑换 -->
		<view class="vip-exchange">
			<view class="exchange-title">
				<view class="title">超级会员专属兑换</view>
			</view>
			<view class="goods-list">
				<view class="list" v-for="(item,index) in 1"
				@click="onExchange(2)"
				:key="index">
					<view class="thumb">
						<image :src="'/static/img/goods_03.png'" mode=""></image>
					</view>
					<view class="item">
						<view class="title">
							<text class="two-omit">老年运动休闲鞋，跑步鞋【足力健】</text>
						</view>
						<view class="price-integral-btn">
							<view class="price-integral">
								<text class="integral">10000金币</text>
								<text class="price">￥1999.00</text>
							</view>
							<view class="btn">
								<text>立即兑换</text>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		data() {
			return {

			};
		},
		methods:{
			/**
			 * 兑换点击
			 * @param {Number} type 0 优惠券 1 普通商品 2 会员商品
			 */
			onExchange(type){
				uni.navigateTo({
					url: '/packagesMember/ExchangeDetails/ExchangeDetails?type=' + type,
				})
			},
			/**
			 * 兑换记录
			 */
			onExchangeRecord(){
				uni.navigateTo({
					url: '/packagesMember/ExchangeRecord/ExchangeRecord',
				})
			},
			/**
			 * 金币点击
			 */
			onIntegral(){
				uni.navigateTo({
					url: '/packagesMember/IntegralRecord/IntegralRecord'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'IntegralDetails.scss';
</style>
