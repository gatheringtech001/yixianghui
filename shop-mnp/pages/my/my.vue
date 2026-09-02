<template>
	<view class="page">
		<scroll-view class="profile-scroll" scroll-y :show-scrollbar="false">
			<view class="profile-head" :style="profileHeadPadding">
				<view class="profile-top">
					<image
						class="avatar"
						:src="userInfo && userInfo.avatar ? host + userInfo.avatar : '/static/home-design/profile-avatar.png'"
						mode="aspectFill"
						@click="onUserInfo"
					/>
					<view class="profile-info">
						<view class="profile-name" @click="onUserInfo">
							<text v-if="!userInfo">登录/注册</text>
							<text v-else>{{ userInfo.nickName }}</text>
						</view>
						<view class="settings" @click="onSetting">
							<image class="settings-icon" src="/static/profile-icons/settings.png" mode="aspectFit" />
							<text>设置</text>
						</view>
					</view>
				</view>
				<view class="stats">
					<view class="stat-item" @click="tapItemDetail('gold')">
						<text class="stat-value">{{ golds }}</text>
						<text class="stat-label">我的金币</text>
					</view>
					<view class="stat-item" @click="tapItemDetail('coupon')">
						<text class="stat-value">{{ coupons }}</text>
						<text class="stat-label">我的优惠券</text>
					</view>
					<view class="stat-item" @click="tapItemDetail('collect')">
						<text class="stat-value">{{ collects }}</text>
						<text class="stat-label">我的收藏</text>
					</view>
					<view class="stat-item" @click="tapItemDetail('activity')">
						<text class="stat-value">{{ activitys }}</text>
						<text class="stat-label">我的预约</text>
					</view>
				</view>
			</view>

			<view class="profile-housekeeper">
				<image
					v-if="stewardImageDisplay"
					class="steward-image"
					:src="stewardImageDisplay"
					mode="aspectFit"
				/>
				<view class="housekeeper-copy">
					<view class="title">添加逸享荟小管家</view>
					<view class="slogan">全程管家式服务，为您的生活保驾护航。</view>
				</view>
				<view class="housekeeper-btn" @click.stop="openHousekeeper">添加管家</view>
			</view>

			<view class="section-head">
				<text class="section-title">我的订单</text>
				<text class="section-more" @click="onSkipOrder(0)">全部订单 &gt;</text>
			</view>
			<view class="order-panel">
				<view class="order-grid">
					<view class="order-item" @click="onSkipOrder(1)">
						<image class="order-icon" src="/static/profile-icons/order-payment.png" mode="aspectFit" />
						<text class="order-label">待付款</text>
						<text class="order-badge" v-if="orders && orders > 0">{{ orders }}</text>
					</view>
					<view class="order-item" @click="onSkipOrder(2)">
						<image class="order-icon" src="/static/profile-icons/order-shipping.png" mode="aspectFit" />
						<text class="order-label">待发货</text>
					</view>
					<view class="order-item" @click="onSkipOrder(3)">
						<image class="order-icon" src="/static/profile-icons/order-receive.png" mode="aspectFit" />
						<text class="order-label">待收货</text>
					</view>
					<view class="order-item" @click="onSkipOrder(4)">
						<image class="order-icon" src="/static/profile-icons/order-refund.png" mode="aspectFit" />
						<text class="order-label">退款中</text>
					</view>
				</view>
			</view>

			<view class="section-head">
				<text class="section-title">我的服务</text>
			</view>
			<view class="service-list">
				<view class="service-row" @click="onServer('retail')">
					<view class="row-left">
						<image class="service-icon" src="/static/profile-icons/service-advisor.png" mode="aspectFit" />
						<text>康养顾问中心</text>
					</view>
					<text class="row-arrow">&gt;</text>
				</view>
				<view class="service-row" @click="onServer('address')">
					<view class="row-left">
						<image class="service-icon" src="/static/profile-icons/service-address.png" mode="aspectFit" />
						<text>收货地址</text>
					</view>
					<text class="row-arrow">&gt;</text>
				</view>
				<view class="service-row" @click="onServer('collect')">
					<view class="row-left">
						<image class="service-icon" src="/static/profile-icons/service-favorite.png" mode="aspectFit" />
						<text>我的收藏</text>
					</view>
					<text class="row-arrow">&gt;</text>
				</view>
			</view>
		</scroll-view>

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
			<view class="steps" v-if="selectedContact && selectedContact.adImage">
				<view class="content">
					<image
						:src="getAdImageUrl(selectedContact.adImage)"
						mode="aspectFit"
						show-menu-by-longpress
						@tap="previewQrImage(selectedContact.adImage)"
					/>
					<view class="tips">长按识别二维码添加</view>
				</view>
			</view>
		</u-popup>

		<TabBar :tabBarShow="4"></TabBar>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import TabBar from '@/components/TabBar/TabBar.vue'
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import { runWithAuth, bindPageAuthPopup, syncConsultantStorage } from '@/utils/login'
	import {
		getBannerList
	} from '@/api/index'
	import {
		AD_POSITION,
		loadAdImageUrl,
		resolveAdImageUrl
	} from '@/utils/adAsset'
	import {
		getMemberCardsList,
		getStatic
	} from '@/api/member/index'
	import {
		getInfo
	} from '@/api/public'
	import {
		CUSTOMER_SERVICE_POSITION_ID,
		HOUSEKEEPER_ROTATION_KEY,
		selectRotatingHousekeeper
	} from '@/utils/housekeeperRotation'
	export default {
		components: {
			TabBar,
			AuthProfilePopup,
		},
		data() {
			return {
				host: this.$host,
				userInfo: null,
				isOpen: false,
				defaultCard: null,
				golds: 0,
				coupons: 0,
				collects: 0,
				activitys: 0,
				orders: 0,
				userCard: undefined,
				showContact: false,
				contact: [],
				selectedContact: null,
				stewardImageUrl: '',
				profileHeadPadding: {
					paddingTop: '124rpx',
					paddingRight: '40rpx'
				}
			}
		},
		computed: {
			stewardImageDisplay() {
				return this.stewardImageUrl
			}
		},
		onReady() {
			uni.hideTabBar()
			this.setProfileHeadPadding()
		},
		onLoad() {
			this.loadStewardImage()
			this.getContactAdList()
			this.setProfileHeadPadding()
		},
		async onShow() {
			bindPageAuthPopup(this)
			this.setProfileHeadPadding()
			this.userInfo = uni.getStorageSync('userInfo')
			this.userCard = uni.getStorageSync('userCard')
			if (!this.userInfo || this.userInfo == '' || this.userInfo == undefined) return
			await this.getUserInfoFn()
			this.getStaticData()
			this.getCards()
			if (!this.userCard || this.userCard.cardKey == 'vip0') {
				this.isOpen = false
			} else {
				let endTime = new Date(this.userCard.enableEndTime).getTime()
				let now = new Date().getTime()
				this.isOpen = endTime < now ? false : true
			}
		},
		methods: {
			async loadStewardImage() {
				this.stewardImageUrl = await loadAdImageUrl(AD_POSITION.PROFILE_STEWARD, this.host)
			},
			setProfileHeadPadding() {
				const sys = uni.getSystemInfoSync()
				const baseTop = uni.upx2px(124)
				let paddingTop = (sys.statusBarHeight || 20) + uni.upx2px(72)
				let paddingRight = uni.upx2px(40)

				try {
					const menuButton = uni.getMenuButtonBoundingClientRect()
					if (menuButton && menuButton.bottom > 0) {
						paddingTop = Math.max(paddingTop, menuButton.bottom + uni.upx2px(48))
					}
					if (menuButton && menuButton.left > 0) {
						paddingRight = Math.max(
							uni.upx2px(40),
							sys.windowWidth - menuButton.left + uni.upx2px(12)
						)
					}
				} catch (e) {}

				paddingTop = Math.max(baseTop, paddingTop)

				this.profileHeadPadding = {
					paddingTop: `${paddingTop}px`,
					paddingRight: `${paddingRight}px`
				}
			},
			async getContactAdList() {
				let { data } = await getBannerList({
					positionId: CUSTOMER_SERVICE_POSITION_ID
				})
				this.contact = (Array.isArray(data) ? data : []).filter(item => (
					item && item.adImage
				))
			},
			openHousekeeper() {
				const cursor = uni.getStorageSync(HOUSEKEEPER_ROTATION_KEY)
				const selection = selectRotatingHousekeeper(this.contact, cursor)
				if (!selection.item) {
					uni.showToast({ title: '管家二维码加载中，请稍后重试', icon: 'none' })
					return
				}
				this.selectedContact = selection.item
				uni.setStorageSync(HOUSEKEEPER_ROTATION_KEY, selection.nextCursor)
				this.showContact = true
			},
			getAdImageUrl(adImage) {
				return resolveAdImageUrl(this.host, adImage)
			},
			previewQrImage(adImage) {
				const url = this.getAdImageUrl(adImage)
				if (!url) {
					uni.showToast({
						icon: 'none',
						title: '二维码未配置'
					})
					return
				}
				uni.previewImage({
					urls: [url],
					current: url
				})
			},
			async getStaticData() {
				let { data } = await getStatic()
				this.golds = (data && data.golden) || 0
				this.coupons = (data && data.couponGotCount) || 0
				this.collects = (data && data.collectCount) || 0
				this.activitys = (data && data.activityOrderCount) || 0
				this.orders = (data && data.goodsOrderCount) || 0
			},
			async getUserInfoFn() {
				const res = await getInfo()
				syncConsultantStorage(res.consultant)
				uni.setStorageSync('userCard', res.userCard)
				uni.setStorageSync('userInfo', res.data)
				uni.setStorageSync('userData', res.userInfo)
				this.userCard = res.userCard
			},
			async getCards() {
				let { rows } = await getMemberCardsList()
				if (!this.userCard) {
					this.defaultCard = rows.find(v => v.cardKey == 'vip0')
				} else {
					this.defaultCard = rows.find(v => v.cardId == this.userCard.cardId)
				}
			},
			onSkipOrder(type) {
				if (!this.userInfo) {
					this.userLogin()
					return
				}
				if (type === 5) {
					uni.navigateTo({
						url: '/packagesMember/AfterSalesOrder/AfterSalesOrder',
					})
					return
				}
				uni.navigateTo({
					url: '/packagesMall/MyOrderList/MyOrderList?type=' + type,
				})
			},
			onServer(type) {
				if (type === 'retail') {
					uni.navigateTo({ url: '/packagesPublic/TalentCenter/index' })
					return
				}
				if (!this.userInfo) {
					this.userLogin()
					return
				}
				switch (type) {
					case 'address':
						uni.navigateTo({ url: '/packagesPublic/AddressList/AddressList' })
						break
					case 'collect':
						uni.navigateTo({ url: '/packagesMember/GoodsOn/GoodsOn' })
						break
				}
			},
			tapItemDetail(type) {
				if (!this.userInfo) {
					this.userLogin()
					return
				}
				switch (type) {
					case 'gold':
						uni.navigateTo({ url: '/packagesMember/IntegralRecord/IntegralRecord' })
						break
					case 'coupon':
						uni.navigateTo({ url: '/packagesMember/MyCoupon/MyCoupon' })
						break
					case 'collect':
						uni.navigateTo({ url: '/packagesMember/GoodsOn/GoodsOn' })
						break
					case 'activity':
						uni.navigateTo({ url: '/packagesMember/MyActivity/index' })
						break
				}
			},
			onSetting() {
				runWithAuth(this, (ok) => {
					if (!ok) return
					this.userInfo = uni.getStorageSync('userInfo')
					uni.navigateTo({
						url: '/packagesPublic/Setting/Setting',
						fail: () => {
							uni.showToast({ title: '跳转失败，请重试', icon: 'none' })
						}
					})
				})
			},
			onUserInfo() {
				runWithAuth(this, (ok) => {
					if (!ok) return
					this.userInfo = uni.getStorageSync('userInfo')
					uni.navigateTo({
						url: '/packagesPublic/Setting/Setting',
						fail: () => {
							uni.showToast({ title: '跳转失败，请重试', icon: 'none' })
						}
					})
				})
			},
			userLogin() {
				uni.navigateTo({
					url: '/packagesPublic/login/login'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'my.scss';
</style>
