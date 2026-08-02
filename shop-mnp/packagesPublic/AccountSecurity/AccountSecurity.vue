<template>
	<view class="page">
		<!-- 安全设置列表 -->
		<view class="security-list">
			<view class="list" @click="switchAccount">
				<view class="content">
					<view class="title">
						<text>更换账号</text>
					</view>
					<view class="describe">
						<text>切换账号需重新进行微信授权登录</text>
					</view>
				</view>
				<view class="more">
					<text class="iconfont icon-more"></text>
				</view>
			</view>
		</view>
		<DialogBox ref="DialogBox"></DialogBox>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				host: this.$host,
				userInfo: null
			};
		},
		
		onShow() {
			this.userInfo = uni.getStorageSync('userInfo')
		},
		methods: {
			clearAuthStorage() {
				uni.removeStorageSync('token')
				uni.removeStorageSync('userInfo')
				uni.removeStorageSync('userData')
				uni.removeStorageSync('userCard')
				uni.removeStorageSync('consultant')
				uni.removeStorageSync('setTokenTime')
			},
			switchAccount() {
				this.$refs.DialogBox.confirm({
					title: '提示',
					content: '更换账号需重新登录，是否继续？',
					DialogType: 'inquiry',
					animation: 0
				}).then(() => {
					this.clearAuthStorage()
					uni.reLaunch({
						url: '/packagesPublic/login/login'
					})
				})
			}
		}
	}
</script>

<style lang="scss" scoped>
	@import 'AccountSecurity.scss';
</style>
