<template>
	<view class="page">
		<scroll-view class="settings-scroll" scroll-y :show-scrollbar="false">
			<view class="settings-shell">
				<view class="settings-hero" @click="onUserInfo">
					<image class="hero-brand" src="/static/home-design/brand-mark.png" mode="aspectFit" />
					<view class="hero-topline">
						<text class="hero-eyebrow">逸享荟账户中心</text>
						<text class="status-pill">已登录</text>
					</view>
					<view class="hero-profile">
						<image class="hero-avatar" :src="avatarDisplay" mode="aspectFill" />
						<view class="hero-copy">
							<text class="hero-name">{{ userInfo && userInfo.nickName || '逸享荟用户' }}</text>
							<text class="hero-description">查看并完善个人资料</text>
						</view>
						<u-icon name="arrow-right" color="rgba(255,255,255,0.76)" size="30" />
					</view>
				</view>

				<view class="settings-section">
					<view class="section-heading">
						<text class="section-title">账户与服务</text>
						<text class="section-caption">管理您的常用信息与权益</text>
					</view>
					<view class="settings-card">
						<view class="settings-row" @click="onAddress">
							<view class="settings-icon">
								<u-icon name="map" color="#701018" size="34" />
							</view>
							<view class="row-copy">
								<text class="row-title">地址管理</text>
								<text class="row-description">管理联系人与收货地址</text>
							</view>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
						<view class="settings-row" @click="onSetting('account')">
							<view class="settings-icon">
								<u-icon name="lock" color="#701018" size="34" />
							</view>
							<view class="row-copy">
								<text class="row-title">账户安全</text>
								<text class="row-description">更换当前登录账号</text>
							</view>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
						<view class="settings-row" @click="onSetting('vip')">
							<view class="settings-icon membership-icon">
								<u-icon name="integral" color="#8a5b18" size="34" />
							</view>
							<view class="row-copy">
								<text class="row-title">逸享荟会员</text>
								<text class="row-description">查看会员权益与专属服务</text>
							</view>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
					</view>
				</view>

				<view class="settings-section">
					<view class="section-heading">
						<text class="section-title">品牌与支持</text>
						<text class="section-caption">了解逸享荟康养服务</text>
					</view>
					<view class="settings-card">
						<view class="settings-row" @click="onSetting('about')">
							<view class="settings-icon">
								<u-icon name="info-circle" color="#701018" size="34" />
							</view>
							<view class="row-copy">
								<text class="row-title">关于我们</text>
								<text class="row-description">品牌介绍与服务说明</text>
							</view>
							<u-icon name="arrow-right" color="#a49c93" size="26" />
						</view>
					</view>
				</view>

				<view class="logout-button" @click="onQuitLogin">退出登录</view>
				<view class="brand-footer">
					<image src="/static/home-design/brand-mark.png" mode="aspectFit" />
					<text>逸享荟 · 让康养旅居更安心</text>
				</view>
			</view>
		</scroll-view>
		<DialogBox ref="DialogBox"></DialogBox>
		<AuthProfilePopup ref="authProfilePopup" />
	</view>
</template>

<script>
	import AuthProfilePopup from '@/components/AuthProfilePopup/AuthProfilePopup.vue'
	import { runWithAuth, bindPageAuthPopup } from '@/utils/login'
	export default {
		components: {
			AuthProfilePopup
		},
		data() {
			return {
				host: this.$host,
				userInfo: null
			}
		},
		computed: {
			avatarDisplay() {
				const avatar = this.userInfo && this.userInfo.avatar
				if (!avatar) return '/static/home-design/profile-avatar.png'
				if (avatar.startsWith('http') || avatar.startsWith('wxfile://')) return avatar
				if (avatar.startsWith('/')) return `${this.host}${avatar}`
				return avatar
			}
		},
		onShow() {
			bindPageAuthPopup(this)
			this.userInfo = uni.getStorageSync('userInfo')
		},
		methods: {
			/**
			 * 用户信息点击
			 */
			onUserInfo() {
				runWithAuth(this, (ok) => {
					if (!ok) return
					this.userInfo = uni.getStorageSync('userInfo')
					uni.navigateTo({
						url: '/packagesPublic/Information/Information'
					})
				})
			},
			/**
			 * 地址点击
			 */
			onAddress() {
				uni.navigateTo({
					url: '/packagesPublic/AddressList/AddressList',
				})
			},
			/**
			 * 设置列表点击
			 * @param {String} type
			 */
			onSetting(type) {
				switch (type) {
					case 'account':
						uni.navigateTo({
							url: '/packagesPublic/AccountSecurity/AccountSecurity'
						})
						break;
					case 'pay':
						uni.navigateTo({
							url: '/packagesPublic/PaymentPassword/PaymentPassword'
						})
						break;
					case 'invoice':
						uni.navigateTo({
							url: '/packagesPublic/InvoiceList/InvoiceList'
						})
						break;
					case 'vip':
						uni.switchTab({
							url: '/pages/MembersOpened/MembersOpened'
						})
						// uni.navigateTo({
						// 	url: '/packagesMember/MyMemberInterest/MyMemberInterest'
						// })
						break;
					case 'common':
						uni.navigateTo({
							url: '/packagesPublic/SettingCommon/SettingCommon'
						})
						break;
					case 'about':
						uni.navigateTo({
							url: '/packagesPublic/AboutUs/AboutUs?id=10'
						})
						break;
				}
			},
			/**
			 * 退出点击
			 */
			onQuitLogin() {
				this.$refs['DialogBox'].confirm({
					title: '提示',
					content: '是否要退出登录?',
					DialogType: 'inquiry',
					animation: 0
				}).then(() => {
					uni.removeStorageSync('token')
					uni.removeStorageSync('userInfo')
					uni.navigateBack()
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'Setting.scss';
</style>
