<template>
	<view class="page">
		<view class="head">
			<view class="edit" @click="isEdit = !isEdit">
				<text>{{isEdit?'完成':'编辑'}}</text>
			</view>
		</view>
		<!-- 购物车列表 -->
    <mescroll-body ref="mescrollRef"
                   @down="downCallback"
                   @up="upCallback"
                   :down="downOption"
                   :up="upOption"
                   :top="0">
		<view class="cart-list">
			<view class="list">
				<view class="check">
					<text class="iconfont icon-checked"></text>
				</view>
				<view class="goods">
					<view class="thumb">
						<image src="/static/img/goods_03.png" mode=""></image>
					</view>
					<view class="item">
						<view class="title">
							<text class="two-omit">aa</text>
						</view>
						<view class="attribute">
							<view class="attr">
								<text>cc</text>
								<text class="more"></text>
							</view>
						</view>
						<view class="price-num">
							<view class="price">
								<text class="min">￥</text>
								<text class="max">89.00</text>
							</view>
							<view class="num">
								<view class="add">
									<text class="iconfont icon-jian"></text>
								</view>
								<view class="number">
									<text>2</text>
								</view>
								<view class="add">
									<text class="iconfont icon-jia"></text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>
			<view class="list" v-for="(item,index) in 1" :key="index">
				<view class="check">
					<text class="iconfont icon-check"></text>
				</view>
				<view class="goods">
					<view class="thumb">
						<image :src="'/static/img/goods_02.png'" mode=""></image>
					</view>
					<view class="item">
						<view class="title">
							<text class="two-omit">aa</text>
						</view>
						<view class="attribute">
							<view class="attr">
								<text>bb</text>
								<text class="more"></text>
							</view>
						</view>
						<view class="price-num">
							<view class="price">
								<text class="min">￥</text>
								<text class="max">89.00</text>
							</view>
							<view class="num">
								<view class="add">
									<text class="iconfont icon-jian"></text>
								</view>
								<view class="number">
									<text>2</text>
								</view>
								<view class="add">
									<text class="iconfont icon-jia"></text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>
    <!-- 购物车失效商品列表 -->
    <view class="lose-efficacy-list">
      <view class="lose-efficacy-title">
        <view class="title">
          <text>失效商品1件</text>
        </view>
        <view class="empty">
          <text>清空失效商品</text>
        </view>
      </view>
      <view class="list" v-for="(item,index) in 1" :key="index">
        <view class="tag">
          <text>失效</text>
        </view>
        <view class="goods" @click="onSkip('goods')">
          <view class="pictrue">
            <image :src="'/static/img/goods_01.png'" mode=""></image>
          </view>
          <view class="item">
            <view class="title">
              <text class="two-omit">aa</text>
            </view>
            <view class="explain">
              <text>商品已不能购买，请联系客服进行沟通</text>
            </view>
          </view>
        </view>
      </view>
    </view>
    <!-- 为你推荐 -->
    <view class="recommend-info">
      <view class="recommend-title">
        <view class="title">
          <image src="/static/wntj_title.png" mode=""></image>
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
		<!-- 结算 -->
		<view class="close-account">
			<view class="check-total">
				<view class="check">
					<text class="iconfont icon-check"></text>
					<text class="all">全选</text>
				</view>
				<view class="total">
					<text>合计：</text>
					<text class="price">￥178.00</text>
				</view>
			</view>
			<view class="account">
				<view class="btn-calculate" v-if="!isEdit">
					<text>去结算(1)</text>
				</view>
				<view class="btn-del" v-else>
					<text class="attention">移入关注</text>
					<text class="del">删除</text>
				</view>
			</view>
		</view>
    </mescroll-body>
	</view>
</template>

<script>
	// 引入mescroll-mixins.js
	import MescrollMixin from "@/components/mescroll-uni/mescroll-mixins.js";
	export default {
		mixins: [MescrollMixin], // 使用mixin
		data() {
			return {
        mescroll: null, // mescroll实例对象 (此行可删,mixins已默认)
        // 下拉刷新的配置(可选, 绝大部分情况无需配置)
        downOption: {},
        // 上拉加载的配置(可选, 绝大部分情况无需配置)
        upOption: {
          use: false,
          toTop: {
            src: '',
          }
        },
		isEdit: false,
        goodsList:[
				// {
				// 	id: 1,
				// 	name: '老年运动休闲鞋，跑步鞋【足力健】',
				// 	price: '168.00',
				// 	vip_price: '158.00',
				// 	img: '/static/img/goods_03.png',
				// 	is_goods: 1,
				// },
			],
		};
	},
	onReady() {
      uni.hideTabBar()
	},
    methods:{
      /*下拉刷新的回调, 有三种处理方式:*/
      downCallback(){
        this.mescroll.endSuccess();
      },
      /*上拉加载的回调*/
      upCallback(page) {
        setTimeout(() =>{
          this.mescroll.endByPage(10, 20);
        },2000)
      },
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
	}
</script>

<style scoped lang="scss">
	@import 'cart.scss';
</style>
