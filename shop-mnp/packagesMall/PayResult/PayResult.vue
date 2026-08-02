<template>
	<view class="page">
		<view class="pay-price">
			<view class="icon">
				<image src="/static/pay_success.png" mode=""></image>
			</view>
			<view class="price-data">
				<view class="list">
					<view class="title">支付方式</view>
					<view class="content">：微信支付</view>
				</view>
				<view class="list">
					<view class="title">支付金额</view>
					<view class="content">：￥{{orderAmount}}元</view>
				</view>
			</view>
		</view>
		<!-- 跳转按钮 -->
		<view class="skip-btn">
			<view class="btn" @click="onSkip(0)">{{ payType === 'activity' ? '我的活动' : '我的订单' }}</view>
			<view class="btn" @click="onSkip(1)">返回首页</view>
		</view>
    <!-- 为你推荐 -->
    <view class="recommend-info">
      <view class="recommend-title">
        <view class="title">
          <image src="/static/wntj_title.png" mode=""></image>
        </view>
      </view>
      <view class="goods-list">
        <view class="list" v-for="(item, index) in goodsList" @click="goDetail(item)" :key="index">
          <view class="pictrue">
            <image :src="host + item.goodsCover" mode="heightFix"></image>
          </view>
          <view class="title-tag">
            <view class="tag">
              {{item.goodsName}}
            </view>
          </view>
          <view class="price-info">
            <view class="user-price">
              <text class="min">￥</text>
              <text class="max">{{item.price}}</text>
            </view>
            <view class="vip-price">
              <image src="/static/vip_ico.png"></image>
              <text>￥{{item.vipPrice}}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
	</view>
</template>

<script>
	import { getGoodsList } from '@/api/shop/index'
	export default {
		data() {
			return {
				orderAmount: 0,
				payType: '',
				orderId: null,
				host: this.$host,
				goodsList: [],
			}
		},
		onLoad(option) {
			this.orderAmount = option.orderAmount
			this.payType = option.type || ''
			this.orderId = option.orderId || null
		},
		methods: {
			// 获取首页商品列表
			getGoodsLists() {
				getGoodsList().then(res => {
					this.goodsList = res.data.filter(v => v.isHot == 1)
				})
			},
			
			/**
			 * @param {Number} type 0 订单 1 首页
			 */
			onSkip(type){
				switch(type){
					case 0:
						if (this.payType === 'activity') {
							const url = this.orderId
								? `/packagesMember/MyActivity/detail/index?orderId=${this.orderId}`
								: '/packagesMember/MyActivity/index'
							uni.redirectTo({ url })
						} else {
							uni.redirectTo({
								url: '/packagesMall/MyOrderList/MyOrderList'
							})
						}
						break;
					case 1:
						uni.switchTab({
							url: '/pages/home/home'
						})
						break;
				}
			},
			goDetail(item) {
				uni.navigateTo({
					url: `/packagesMall/GoodsDetails/GoodsDetails?id=${item.goodsId}`
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'PayResult.scss';
</style>
