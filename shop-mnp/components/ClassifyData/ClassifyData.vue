<template>
	<view class="page-total">
		<!-- 分类列表 -->
		<view class="classify-list">
			<view class="list" @click="onSkip('classify')">
				<view class="thumb"><image src="/static/img/pin_01.png"></image></view>
				<view class="name"><text class="one-omit">小米</text></view>
			</view>
			<view class="list" @click="onSkip('classify')">
				<view class="thumb"><image src="/static/img/pin_02.png"></image></view>
				<view class="name"><text class="one-omit">华为</text></view>
			</view>
			<view class="list" @click="onSkip('classify')">
				<view class="thumb"><image src="/static/img/pin_03.png"></image></view>
				<view class="name"><text class="one-omit">荣耀</text></view>
			</view>
			<view class="list" @click="onSkip('classify')">
				<view class="thumb"><image src="/static/img/pin_04.png"></image></view>
				<view class="name"><text class="one-omit">iPhone</text></view>
			</view>
			<view class="list" @click="onSkip('classify')">
				<view class="thumb"><image src="/static/img/pin_05.png"></image></view>
				<view class="name"><text class="one-omit">vivo</text></view>
			</view>
		</view>
		<!-- 超值爆款 -->
		<view class="super-hot-style">
			<view class="hot-title">
				<view class="iconfont icon-zhizi"></view>
				<view class="title">超值爆款</view>
			</view>
			<view class="goods-list">
				<view class="list" v-for="(item, index) in classGoodsList" @click="onSkip('goods')" :key="index">
					<view class="thumb">
						<image :src="item.img" mode="widthFix"></image>
					</view>
					<view class="title"><text class="one-omit">{{item.name}}</text></view>
					<view class="price">
						<view class="retail-price">
							<text class="min">￥</text>
							<text class="max">{{item.price}}</text>
						</view>
						<view class="sales-volume">已售4件</view>
					</view>
				</view>
			</view>
		</view>
		<!-- 更多热卖 -->
		<view class="more-hot">
			<view class="hot-title">
				<view class="title">
					<text class="iconfont icon-xiedian"></text>
					<text class="icon">更多热卖</text>
					<text class="iconfont icon-xiedian"></text>
				</view>
			</view>
      <view class="goods-list">
        <view class="list" v-for="(item,index) in goodsList" @click="onSkip('goods')" :key="index">
          <view class="pictrue">
            <image :src="item.img" mode="heightFix"></image>
          </view>
          <view class="title-tag">
            <view class="tag">
              <text v-if="item.is_goods === 1">特价</text>
              {{item.name}}
            </view>
          </view>
          <view class="price-info">
            <view class="user-price">
              <text class="min">￥</text>
              <text class="max">{{item.price}}</text>
            </view>
            <view class="vip-price">
              <image src="/static/vip_ico.png"></image>
              <text>￥{{item.vip_price}}</text>
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
			goodsList:[
				{
					id: 1,
					name: 'BANDALY 2020夏季女装连衣裙韩版大码宽松显瘦套装裙子两件套 JX19301 上豆绿下米白 M ',
					price: '219.00',
					vip_price: '129.00',
					img: '/static/img/goods_thumb_01.png',
					is_goods: 0,
				}
			],
			classGoodsList: [
				{
					id: 1,
					name: '荣耀Play4T 全网通6GB+128GB大内存 幻夜黑 4000mAh大电池 4800万AI摄影  6.39英寸魅眼屏',
					price: '1190.00',
					vip_price: '1100.00',
					img: '/static/img/goods_thumb_11.png',
					is_goods: 0,
				}
			]
		};
	},
	methods:{
		/**
		 * 跳转点击
		 * @param {String} type 跳转类型
		 */
		onSkip(type){
			switch (type){
				case 'classify':
					uni.navigateTo({
						url: '/packagesMall/SearchGoodsList/SearchGoodsList',
					})
					break;
				case 'goods':
					uni.navigateTo({
						url: '/packagesMall/GoodsDetails/GoodsDetails',
						animationType: 'zoom-fade-out',
						animationDuration: 200
					})
					break;
			}
		}
	}
};
</script>

<style lang="scss">
@import 'ClassifyData.scss';
</style>
