<template>
	<view class="page">
		<!-- 搜索与排序筛选栏 -->
		<block v-if="listHeaderVisible">
		<view class="search-index">
			<view class="icon-info" @click="">
				<text class="cuIcon-location"></text>
				<text class="city">昆明</text>
			</view>
			<view class="search">
				<text class="iconfont icon-fadajing"></text>
				<input type="text" v-model="keyword" placeholder="搜索商品" />
			</view>
			<view class="cut" @click="isList = !isList">
				<text class="iconfont" :class="isList?'icon-shitu01':'icon-shitu02'"></text>
			</view>
			<view class="cut" @click="goCart">
				<text class="iconfont icon-cart"></text>
			</view>
		</view>
		<!-- 筛选 -->
		<view class="screen-info">
			<view class="screen-list">
				<view class="list" :class="{'action':screenShow===0}" @click="onSynthesize(0)">
					<text>综合</text>
					<text class="iconfont icon-sanjiao icon_z"></text>
				</view>
				<view class="list">
					<text>销量</text>
					<text></text>
				</view>
				<view class="list">
					<text>价格</text>
					<view class="icon_j">
						<text class="iconfont icon-sanjiao up"></text>
						<text class="iconfont icon-sanjiao down"></text>
					</view>
				</view>
				<view class="list" @click="isDrawer = true">
					<text>筛选</text>
					<text class="iconfont icon-shaixuan icon_s"></text>
				</view>
			</view>
			<!-- 筛选弹窗 -->
			<view class="screen-popup" @click.stop="isScreen = false" v-show="isScreen">
				<view class="synthesize">
					<view class="list">
						<text class="check"></text>
						<text class="title">综合排序</text>
					</view>
					<view class="list">
						<text class="check"></text>
						<text class="title">评论数从高到低</text>
					</view>
				</view>
			</view>
		</view>
		</block>
		<!-- 商品列表 -->
		<view class="goods-data" :class="{'goods-data--no-header': !listHeaderVisible}">
			<mescroll-body ref="mescrollRef"
				@init="mescrollInit"
				@down="downCallback"
				@up="upCallback"
				:down="downOption"
				:up="upOption"
				:top="0">
				<view class="goods-list">
					<view :class="isList?'list-view':'list-li'" v-for="(item,index) in goodsList" @click="onGoodsList" :key="index">
						<view class="thumb">
							<image :src="item.img" mode="heightFix"></image>
						</view>
						<view class="item">
							<view class="title">
								<text class="two-omit">{{item.name}}</text>
							</view>
							<view class="price">
								<view class="retail-price">
									<text class="min">￥</text>
									<text class="max">{{item.price}}</text>
									<view class="tag" v-if="item.is_goods === 1">
										<text>抢购价</text>
									</view>
								</view>
								<view class="vip-price">
									<text class="min">￥</text>
									<text class="max">{{item.vip_price}}</text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</mescroll-body>
		</view>
		<!-- 抽屉 -->
		<view class="cu-modal drawer-modal justify-end" :class="{'show':isDrawer}" @click="isDrawer = false" style="position: absolute;">
			<view class="cu-dialog basis-lg" @click.stop="isDrawer = true">
				<view class="serve">
					<view class="title">
						<text>服务</text>
					</view>
					<view class="serve-list">
						<view class="list action">
							<text>会员专享</text>
						</view>
						<view class="list">
							<text>活动</text>
						</view>
						<view class="list">
							<text>活动</text>
						</view>
					</view>
				</view>
				<view class="price-screen">
					<view class="title">
						<text>价格区间</text>
					</view>
					<view class="price-section">
						<input type="number" placeholder="最低价">
						<text></text>
						<input type="number" placeholder="最高价">
					</view>
				</view>
				<view class="operation-btn">
					<view class="btn" @click.stop="isDrawer = false">
						<text>取消</text>
					</view>
					<view class="btn action">
						<text>确认</text>
					</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	// 引入mescroll-mixins.js
	import MescrollMixin from "@/components/mescroll-uni/mescroll-mixins.js";
	export default {
		components:{
		},
		mixins: [MescrollMixin], // 使用mixin
		data() {
			return {
				listHeaderVisible: false,
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
				keyword: '',
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
			}
		},
		onLoad(params) {
			this.keyword = decodeURIComponent(params.keyword||'');
		},
		methods: {
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
			onGoodsList(item,index){
				uni.navigateTo({
					url: '/packagesMall/GoodsDetails/GoodsDetails',
					animationType: 'zoom-fade-out',
					animationDuration: 200
				})
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
	@import  'list.scss';
</style>
