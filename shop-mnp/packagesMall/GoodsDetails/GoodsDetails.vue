<template>
	<view>
		<view class="goods-head" v-if="PageScrollTop > 200"
			:style="'background:rgba(255,255,255,' + PageScrollTop / 100 + ')'">
			<!-- tab切换 -->
			<view class="head-tab" v-if="PageScrollTop > 200">
				<view class="tab" :class="{'action':TabShow===0}" @click="onTab(0)">
					<text>商品</text>
					<text class="line"></text>
				</view>
				<view class="tab" :class="{'action':TabShow===2}" @click="onTab(2)">
					<text>详情</text>
					<text class="line"></text>
				</view>
			</view>

		</view>
		<!-- banner，标题 -->
		<view class="banner-title" v-if="goodsDetail">
			<!-- banner -->
			<view class="banner" v-if="goodsDetail.goodsImages.length > 0">
				<swiper class="screen-swiper round-dot" indicator-dots="true" circular="true" autoplay="true" interval="5000"
					duration="500">
					<swiper-item v-for="(item, index) in goodsDetail.goodsImages" :key="index">
						<image :src="mediaUrl(item)" mode="aspectFill"></image>
					</swiper-item>
				</swiper>
			</view>
			<!-- 标题 -->
			<view class="goods-title">
				<text>{{goodsDetail.goodsName}}</text>
			</view>

			<view class="goods-tags" v-if="goodsDetail.tags && goodsDetail.tags != ''">
				<text v-for="(tag, j) in goodsDetail.tags" :key="j"
					:style="{color:tag.color,borderColor:tag.color,background:tag.bg}">{{tag.text}}</text>
			</view>
			<!-- 价格 -->
			<view class="price-info" v-show="type==0">
				<view class="price">
					<text class="min">￥</text>
					<text class="max">{{goodsDetail.price}}</text>
					<text class="min" v-if="goodsDetail.goodsType == 'hotel'">起</text>
				</view>
				<view class="info">
					<view class="list">
						<button open-type="share">
							<u-icon name="share" color="#333333" size="40" />
							<text>分享</text>
						</button>
					</view>
					<view class="list" @click="onAttention">
						<u-icon :name="AttentionShow == 0 ? 'heart' : 'heart-fill'" :color="AttentionShow == 0 ? '#333333' : '#701018'" size="40" />
						<text>{{ AttentionShow == 0 ? '收藏' : '已收藏' }}</text>
					</view>
				</view>
			</view>
		</view>
		<!-- 优惠金币 -->
		<view class="goods-discounts">
			<view class="list">
				<view class="title">金币</view>
				<view class="content">
					<text>下单实付1元赠送1金币，退款按退款金额扣回</text>
				</view>
				<view class="more">
					<text class="iconfont icon-more"></text>
				</view>
			</view>
		</view>
		<view class="goods-discounts" v-if="goodsDetail.goodsType == 'online'">
			<view class="list" @click="chooseAddress">
				<view class="title">送至</view>
				<view class="content">
					<view class="serve">
						<text class="iconfont icon-dingwei"></text>
						<text v-if="address">{{address.provinceName}}{{address.cityName}}{{address.countyName}}</text>
						<text v-else>请选择收货地址</text>
					</view>
				</view>
				<view class="more">
					<text class="iconfont icon-more"></text>
				</view>
			</view>
		</view>
		<view class="sku_box_view" id="skulist" v-if="goodsDetail && goodsDetail.goodsType == 'hotel'">
			<view class="sku_item_view" v-for="(item, index) in skuDataList" :key="index">
				<view class="item_left_view">
					<image :src="item.dataImage ? host + item.dataImage : host + goodsDetail.goodsImages[0]" mode="aspectFill">
					</image>
				</view>
				<view class="item_center_view">
					<view class="item_title_view">{{item.dataValues}}</view>
					<view class="item_subtitle_view">单间客房</view>
					<view class="item_price_view"><text>￥</text>{{item.dataPrice }}</view>
				</view>
				<view class="item_right_view">
					<view class="item_button_view" @click="$u.throttle(buyNow(item.dataId), 500)">订</view>
				</view>
			</view>
		</view>
		<!-- 商品介绍 -->
		<view class="products-introduction" ref="products" v-if="goodsDetail">
			<view class="title">
				<text>商品介绍</text>
			</view>
			<view class="content">
				<u-parse
					v-if="goodsDetail.content"
					:html="goodsDetail.content"
					:domain="host"
					:lazy-load="false"
					:show-with-animation="false"
					:tag-style="richTextTagStyle"
				></u-parse>
			</view>
		</view>
		<!-- 底部 -->
		<view class="page-footer" v-if="goodsDetail.goodsType == 'consultation'">
			<view class="footer-fn long">
				<view class="list" @click="showContact = true">
					<text class="iconfont icon-kefu"></text>
					<text>预约咨询</text>
				</view>
			</view>
		</view>
		<view class="page-footer" v-else>
			<view class="footer-fn">
				<view class="list" @click="showContact = true">
					<text class="iconfont icon-kefu"></text>
					<text>联系客服</text>
				</view>
				<view class="list" @click="openCart">
					<text class="iconfont icon-cart"></text>
					<text>购物车</text>
				</view>
			</view>
			<view class="footer-buy">
				<view v-if="goodsDetail.goodsType == 'hotel'" class="buy-at" @tap="scrollToSectionFn('skulist')">
					<text>查看房源</text>
				</view>
				<view v-else-if="goodsDetail.goodsType == 'online'" class="cart-add" @click="$u.throttle(addToCart, 500)">
					<text>加入购物车</text>
				</view>
				<view v-else class="buy-at" @click="$u.throttle(buyNow, 500)">
					<text>立即订购</text>
				</view>
				<view v-if="goodsDetail.goodsType == 'online'" class="buy-at compact" @click="$u.throttle(buyNow, 500)">
					<text>立即购买</text>
				</view>
			</view>
		</view>
		<u-popup class="butler-popup" v-model="showContact" @touchmove.stop.prevent mode="bottom" border-radius="16"
			:closeable="true">
			<view class="popup-title">添加逸享荟小管家</view>
			<view class="items">
				<view class="item">
					<u-icon name="server-man" size="40" color="#00C800" />
					<text>1对1专属服务</text>
				</view>
				<view class="item">
					<u-icon name="heart-fill" size="40" color="#00C800" />
					<text>全程管家式服务</text>
				</view>
				<view class="item">
					<u-icon name="file-text-fill" size="40" color="#00C800" />
					<text>优质路线推荐</text>
				</view>
				<view class="item">
					<u-icon name="coupon-fill" size="40" color="#00C800" />
					<text>优惠活动不错过</text>
				</view>
			</view>
			<view class="steps">
				<view class="title"><text></text>第一步</view>
				<view class="content">
					<img :src="host + contact[0].adImage" mode="" />
					<view class="tips">长按识别二维码添加</view>
				</view>
			</view>
			<view class="steps">
				<view class="title"><text></text>第二步</view>
				<view class="content else">
					<image :src="host + contact[1].adImage" mode="widthFix" />
				</view>
			</view>
			<view class="btns">
				<view class="btn"><u-icon name="phone-fill" size="40" color="#607CA9" />咨询预订</view>
				<view class="btn"><u-icon name="server-fill" size="40" color="#607CA9" />售后咨询</view>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'
	import {
		getBannerList
	} from '@/api/index'
	import {
		getGoodsInfo,
		getGoodsSkuInfo
	} from '@/api/shop/index'
	import { addCart } from '@/api/member/index'
	import { prepareRichTextHtml } from '@/utils/richText'
	import {
		goodsCollect,
		deleteCollect,
		goodsCollectList
	} from '@/api/member/index'
	import {
		getAddressList,
		getAddressInfo
	} from '@/api/member/index'
	import BaseUrl from '@/api/baseUrl'

	export default {
		mixins: [sharePageMixin],
		data() {
			return {
				host: this.$host,
				goodsDetail: null,
				goodsId: null,
				TabShow: 0,
				AttentionShow: 0,
				web_content: '',
				PageScrollTop: 0,
				type: 0,
				contact: [],
				showContact: false,
				address: null,
				collectId: null,
				skuDataList: [],
				richTextTagStyle: {
					img: 'width:100%;max-width:100%;display:block;',
					image: 'width:100%;max-width:100%;display:block;',
					table: 'width:100%;max-width:100%;table-layout:fixed;',
					p: 'max-width:100%;margin:0;'
				}
			};
		},
		onLoad(option) {
			parseInvitePageOptions(option)
			this.type = 0
			this.goodsId = option.id
			this.getGoodsDetail(option.id)
			this.getGoodsSkuInfoFn(option.id)
			this.getContactAdList()
		},
		onShow() {
			this.userInfo = uni.getStorageSync('userInfo')
			if (!this.userInfo || this.userInfo == '' || this.userInfo == undefined) return
			else {
				this.getAddress()
				this.getCollects()
			}
		},
		onPageScroll(e) {
			this.PageScrollTop = e.scrollTop
		},
		methods: {
			mediaUrl(path) {
				if (!path) return ''
				return /^https?:\/\//.test(path) ? path : this.host + path
			},
			requireLogin(action) {
				const userInfo = uni.getStorageSync('userInfo')
				if (userInfo) return true
				uni.showToast({ icon: 'none', title: `${action}请先登录~` })
				setTimeout(() => {
					uni.removeStorageSync('token')
					uni.removeStorageSync('userInfo')
					uni.navigateTo({ url: '/packagesPublic/login/login' })
				}, 1200)
				return false
			},
			openCart() {
				if (!this.requireLogin('查看购物车')) return
				uni.navigateTo({ url: '/packagesMall/cart/cart' })
			},
			addToCart() {
				if (!this.requireLogin('加入购物车')) return
				addCart({ goodsId: this.goodsDetail.goodsId, goodsCount: 1, isSku: 0, dataId: 0 })
					.then(() => uni.showToast({ title: '已加入购物车', icon: 'success' }))
					.catch(error => uni.showToast({ title: error.message || '加入失败', icon: 'none' }))
			},
			getShareConfig() {
				const cover = this.goodsDetail && this.goodsDetail.goodsCover
				return {
					title: (this.goodsDetail && this.goodsDetail.goodsName) || '逸享荟精选商品',
					path: '/packagesMall/GoodsDetails/GoodsDetails',
					query: { id: this.goodsId },
					imageUrl: cover ? (cover.startsWith('http') ? cover : this.host + cover) : ''
				}
			},
			async getAddress() {
				let {
					rows
				} = await getAddressList()
				this.address = rows.find(v => v.isDefault == 1)
			},
			// 获取地址详情
			async getAddressDetail(id) {
				let {
					data
				} = await getAddressInfo(id)
				this.address = data
			},
			async getContactAdList() {
				let params = {
					positionId: 2
				}
				let {
					data
				} = await getBannerList(params)
				this.contact = data
			},
			// 获取商品详情
			async getGoodsDetail(id) {
				let {
					data
				} = await getGoodsInfo(id)
				if(data.tags){
					data.tags = data.tags.split(/[\,|，]/)
						.map(tag => tag.trim())
						.filter(tag => tag && !['云野集', '云南好物'].includes(tag))
						.map(tag => ({ text: tag, color: '#701018', bg: '#f5e9e5' }))
				}
				this.goodsDetail = data
				if (this.goodsDetail.goodsImages && this.goodsDetail.goodsImages != '') this.goodsDetail.goodsImages = this
					.goodsDetail.goodsImages.split(',')
				else this.goodsDetail.goodsImages = []

				if (this.goodsDetail.content) {
					let content = this.goodsDetail.content
					if (content.includes('src="/api/')) {
						content = content.replace(/src="\/api\//g, `src="${BaseUrl.publicUrl}`)
					}
					this.goodsDetail.content = prepareRichTextHtml(content, this.host)
				}
			},

			async getCollects() {
				let {
					rows
				} = await goodsCollectList()
				if (rows && rows.length > 0) {
					let item = rows.find(v => v.goodsId == this.goodsId)
					if (item && item != undefined) {
						this.AttentionShow = 1
						this.collectId = item.collectId
					} else {
						this.AttentionShow = 0
						this.collectId = null
					}
				}
			},
			// 选择收货地址
			chooseAddress() {
				uni.navigateTo({
					url: '/packagesPublic/AddressList/AddressList?type=creatOrder',
				})
			},
			onTab(type) {
				this.TabShow = type;
				switch (type) {
					case 0:
						uni.pageScrollTo({
							scrollTop: 0,
							duration: 300
						});
						break;
					case 2:
						uni.createSelectorQuery().select(".products-introduction").boundingClientRect((data) => { //data - 各种参数
							uni.pageScrollTo({
								scrollTop: this.PageScrollTop + data.top - 50,
								duration: 300
							});
						}).exec()
						break;
				}
			},
			onAttention() {
				let userInfo = uni.getStorageSync('userInfo')
				if (!userInfo || userInfo == '' || userInfo == undefined) {
					uni.showToast({
						icon: 'none',
						title: '商品收藏请先登录~'
					})
					setTimeout(() => {
						uni.removeStorageSync('token')
						uni.removeStorageSync('userInfo')
						uni.navigateTo({
							url: '/packagesPublic/login/login'
						})
					}, 2000)
					return
				}
				if (this.AttentionShow == 0) {
					let params = {
						collectType: 'goods',
						goodsId: this.goodsDetail.goodsId
					}
					goodsCollect(params).then(res => {
						this.AttentionShow = 1
						uni.showToast({
							title: '收藏成功',
							icon: 'none'
						})
					})
				} else {
					deleteCollect({
						collectId: this.collectId
					}).then(res => {
						this.AttentionShow = 0
						uni.showToast({
							title: '取消成功',
							icon: 'none'
						})
					})
				}
			},
			buyNow(dataId) {
				if (!this.requireLogin('订购')) return
				if (this.goodsDetail && this.goodsDetail.goodsType === 'hotel') {
					uni.navigateTo({
						url: `/packagesMall/GoodsDetails/SojournGoodsDetails?id=${this.goodsDetail.goodsId}`
					})
					return
				}
				uni.navigateTo({
					url: `/packagesMall/ConfirmOrder/ConfirmOrder?id=${this.goodsDetail.goodsId}&dataId=${dataId}`
				})
			},
			/**获取商品sku信息
			 * @param {Object} id
			 */
			getGoodsSkuInfoFn(id) {
				getGoodsSkuInfo(id).then(res => {
					if (res.code != 200) return
					this.skuDataList = res.data
				}).catch(err => {
					console.log('getGoodsSkuInfo', err)
				})
			},
			// 锚点定位
			scrollToSectionFn(id) {
				// 获取需要定位的元素的坐标位置
				uni.createSelectorQuery().select(`#${id}`).boundingClientRect(data => {
					// 此处的定时器，非常的重要，等待页面渲染完，然后滚动页面。
					// 需要除去 标题栏高度 和 状态栏高度
					setTimeout(() => {
						uni.pageScrollTo({
							scrollTop: data.top - 50
						})
					}, 300)
				}).exec();
			}
		}
	};
</script>

<style scoped lang="scss">
	@import 'GoodsDetails.scss';
</style>
