<template>
	<view class="page">
		<!-- 用户信息 -->
		<view class="user-info">
			<view class="user-data" @click="onUserInfo">
				<view class="portrait-nickname">
					<view class="portrait">
						<image :src="userInfo && userInfo.avatar?host+userInfo.avatar : '/static/img/logo.jpg'"></image>
					</view>
					<view class="nickname">
						<text>{{userInfo.nickName || '用户昵称'}}</text>
					</view>
				</view>
				<view class="more">
					<text class="iconfont icon-more"></text>
				</view>
			</view>
			<view class="address" @click="onAddress">
				<view class="title">
					<text>地址管理</text>
				</view>
				<view class="more">
					<text class="iconfont icon-more"></text>
				</view>
			</view>
		</view>
		<!-- 设置列表 -->
		<view class="setting-list">
			<view class="list" @click="onSetting('account')">
				<view class="title">
					<text>账户安全</text>
				</view>
				<view class="more-content">
					<text class="content">更换账号</text>
					<text class="iconfont icon-more more"></text>
				</view>
			</view>
			<view class="list" @click="onSetting('vip')">
				<view class="title">
					<text>逸享荟会员</text>
				</view>
				<view class="more-content">
					<text class="content">会员专属</text>
					<text class="iconfont icon-more more"></text>
				</view>
			</view>
		</view>
		<!-- 设置列表 -->
		<view class="setting-list">
			<view class="list" @click="onSetting('about')">
				<view class="title">
					<text>关于我们</text>
				</view>
				<view class="more-content">
					<text class="content"></text>
					<text class="iconfont icon-more more"></text>
				</view>
			</view>
		</view>
		<!-- 退出 -->
		<view class="quit-login" @click="onQuitLogin">
			<text>退出登录</text>
		</view>
		<!-- 提示框 -->
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
			};
		},
		
		onShow() {
			bindPageAuthPopup(this)
			this.userInfo = uni.getStorageSync('userInfo')
		},
		methods:{
			/**
			 * 用户信息点击
			 */
			onUserInfo(){
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
			onAddress(){
				uni.navigateTo({
					url: '/packagesPublic/AddressList/AddressList',
				})
			},
			/**
			 * 设置列表点击
			 * @param {String} type
			 */
			onSetting(type){
				switch(type) {
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
			onQuitLogin(){
				this.$refs['DialogBox'].confirm({
					title: '提示',
					content: '是否要退出登录?',
					DialogType: 'inquiry',
					animation: 0
				}).then(()=>{
					uni.removeStorageSync('token')
					uni.removeStorageSync('userInfo')
					uni.navigateBack();
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'Setting.scss';
</style>
