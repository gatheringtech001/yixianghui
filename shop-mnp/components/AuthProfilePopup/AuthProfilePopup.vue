<template>
	<view v-if="visible" class="auth-mask" @touchmove.stop.prevent>
		<view class="auth-profile-popup" @tap.stop>
			<view class="auth-topbar">
				<view class="back-action" :class="{ hidden: step === 1 }" @tap="onBackStep">
					<u-icon name="arrow-left" color="#222222" size="34" />
				</view>
				<view class="step-text">{{ topbarTitle }}</view>
				<view class="close-action" @tap="onCancel">关闭</view>
			</view>

			<view v-if="step === 1" class="auth-step login-step">
				<view class="brand-lockup">
					<image class="brand-logo" src="/static/home-design/brand-logo-transparent.png" mode="aspectFit" />
					<text class="brand-subtitle">康养旅居 · 活动 · 老年教育</text>
				</view>
				<image class="login-hero" src="/static/home-design/hero-banner.jpg" mode="aspectFill" />
				<view class="login-copy">
					<view class="title">欢迎使用逸享荟康养</view>
					<view class="desc login-desc">登录逸享荟，开启美好退休生活</view>
				</view>
				<view class="auth-actions">
					<view
						class="primary-btn"
						:class="{ disabled: !agreementChecked || loggingIn }"
						@tap="goProfileStep"
					>微信登录</view>
					<view class="agreement-row" @tap="toggleAgreement">
						<view class="agreement-checkbox" :class="{ checked: agreementChecked }">
							<text v-if="agreementChecked">✓</text>
						</view>
						<view class="agreement-copy">
							我已阅读并同意
							<text class="policy-link" @tap.stop="openPolicy('agreement')">《用户协议》</text>
							和
							<text class="policy-link" @tap.stop="openPolicy('privacy')">《隐私政策》</text>
						</view>
					</view>
				</view>
			</view>

			<view v-else-if="step === 2" class="auth-step profile-step">
				<view class="step-heading">
					<view class="title">申请获取您的头像、昵称</view>
					<view class="desc center">微信要求头像和昵称分别确认一次，完成后自动进入下一步。</view>
				</view>
				<view class="profile-form">
					<button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
						<image class="avatar-img" :src="avatarPreview" mode="aspectFill" />
						<view class="avatar-edit">
							<u-icon name="camera-fill" color="#ffffff" size="28" />
						</view>
					</button>
					<text class="avatar-tip">点击头像选择微信头像</text>
					<view class="nickname-row">
						<text class="label">昵称</text>
						<input
							class="nickname-input"
							type="nickname"
							v-model="nickName"
							maxlength="30"
							confirm-type="done"
							placeholder="请输入微信昵称"
							placeholder-class="nickname-placeholder"
							@blur="onNicknameBlur"
						/>
					</view>
				</view>
				<view class="auth-actions">
					<view class="primary-btn" :class="{ disabled: !profileReady }" @tap="goPhoneStep">继续</view>
				</view>
			</view>

			<view v-else class="auth-step phone-step">
				<view class="phone-brand">
					<image class="phone-logo" src="/static/home-design/brand-logo-transparent.png" mode="aspectFit" />
					<view class="phone-title">逸享荟</view>
					<view class="phone-subtitle">康养旅居 · 活动 · 老年教育</view>
				</view>
				<view class="auth-actions">
					<button
						class="one-tap-btn"
						open-type="getPhoneNumber"
						@getphonenumber="onGetPhoneNumber"
					>授权手机号</button>
					<view class="skip-btn" @tap="onSkipPhone">暂不授权</view>
				</view>
			</view>
		</view>
	</view>
</template>

<script>
	import { uploadUserAvatar } from '@/utils/uploadAvatar'

	export default {
		name: 'AuthProfilePopup',
		data() {
			return {
				visible: false,
				step: 1,
				agreementChecked: false,
				resolveFn: null,
				loginFn: null,
				loggingIn: false,
				avatarUrl: '',
				avatarLocalPreview: '',
				avatarUploading: false,
				nickName: ''
			}
		},
		computed: {
			topbarTitle() {
				return ['登录', '头像与昵称', '授权手机'][this.step - 1] || ''
			},
			profileReady() {
				const nickName = (this.nickName || '').trim()
				return !this.avatarUploading && !!this.avatarUrl && !!nickName && nickName.length <= 30
			},
			avatarPreview() {
				if (this.avatarLocalPreview) return this.avatarLocalPreview
				if (this.avatarUrl) {
					if (this.avatarUrl.startsWith('http') || this.avatarUrl.startsWith('wxfile://')) {
						return this.avatarUrl
					}
					if (this.avatarUrl.startsWith('/')) {
						return `${this.$host}${this.avatarUrl}`
					}
					return this.avatarUrl
				}
				return '/static/home-design/brand-logo-transparent.png'
			}
		},
		methods: {
			open(resolve, loginFn) {
				this.resolveFn = resolve
				this.loginFn = loginFn
				this.step = 1
				this.agreementChecked = false
				this.loggingIn = false
				this.avatarUrl = ''
				this.avatarLocalPreview = ''
				this.avatarUploading = false
				this.nickName = ''
				this.visible = true
			},
			close(result) {
				this.visible = false
				this.step = 1
				this.loginFn = null
				this.loggingIn = false
				if (this.resolveFn) {
					this.resolveFn(result)
					this.resolveFn = null
				}
			},
			toggleAgreement() {
				this.agreementChecked = !this.agreementChecked
			},
			async goProfileStep() {
				if (!this.agreementChecked) {
					uni.showToast({
						title: '请先阅读并同意用户协议和隐私政策',
						icon: 'none'
					})
					return
				}
				if (this.loggingIn) return
				this.loggingIn = true
				uni.showLoading({ title: '登录中...', mask: true })
				try {
					const result = this.loginFn ? await this.loginFn() : null
					if (result && result.profileComplete) {
						this.close({ loginOnly: true })
						return
					}
					const userInfo = (result && result.userInfo) || {}
					if (userInfo.avatar) this.avatarUrl = userInfo.avatar
					if (userInfo.nickName && userInfo.nickName !== '微信小程序用户') {
						this.nickName = userInfo.nickName
					}
					this.step = 2
				} catch (error) {
					uni.showToast({
						title: (error && error.message) || '微信登录失败，请重试',
						icon: 'none',
						duration: 3000
					})
				} finally {
					this.loggingIn = false
					uni.hideLoading({ noConflict: true })
				}
			},
			openPolicy(type) {
				const state = this.$store && this.$store.state
				const config = (state && state.config) || {}
				const articleId = type === 'privacy'
					? config.privacy_policy_id
					: config.user_agreement_id
				if (!articleId) {
					uni.showToast({ title: '协议内容加载中，请稍后重试', icon: 'none' })
					return
				}
				uni.navigateTo({
					url: `/packagesPublic/Article/index?id=${articleId}`
				})
			},
			onChooseAvatar(e) {
				const detail = e.detail || {}
				const tempPath = detail.avatarUrl
				if (!tempPath) {
					const errMsg = detail.errMsg || ''
					uni.showToast({
						title: errMsg.indexOf('deny') !== -1 ? '需要授权头像才能继续' : '头像获取失败，请重试',
						icon: 'none'
					})
					return
				}
				this.avatarLocalPreview = tempPath
				this.avatarUploading = true
				uploadUserAvatar(tempPath).then((serverAvatar) => {
					this.avatarUrl = serverAvatar
				}).catch((error) => {
					this.avatarUrl = ''
					this.avatarLocalPreview = ''
					uni.showToast({
						title: (error && error.message) || '头像上传失败',
						icon: 'none'
					})
				}).finally(() => {
					this.avatarUploading = false
					this.maybeAdvanceProfile()
				})
			},
			onNicknameBlur(e) {
				const value = (e.detail && e.detail.value) || ''
				if (value) {
					this.nickName = value.trim()
				}
				this.maybeAdvanceProfile()
			},
			maybeAdvanceProfile() {
				if (!this.profileReady) return false
				this.nickName = this.nickName.trim()
				this.step = 3
				return true
			},
			goPhoneStep() {
				if (this.maybeAdvanceProfile()) return
				if (this.avatarUploading) {
					uni.showToast({ title: '头像上传中，请稍候', icon: 'none' })
					return
				}
				const nickName = (this.nickName || '').trim()
				if (!this.avatarUrl) {
					uni.showToast({ title: '请先设置头像', icon: 'none' })
					return
				}
				if (!nickName) {
					uni.showToast({ title: '请先填写昵称', icon: 'none' })
					return
				}
				if (nickName.length > 30) {
					uni.showToast({ title: '昵称不能超过30个字符', icon: 'none' })
					return
				}
				this.nickName = nickName
				this.maybeAdvanceProfile()
			},
			onBackStep() {
				if (this.step > 1) {
					this.step -= 1
				}
			},
			onGetPhoneNumber(e) {
				const detail = e.detail || {}
				if (detail.code) {
					this.close({
						nickName: this.nickName,
						avatar: this.avatarUrl,
						phoneCode: detail.code
					})
					return
				}
				if (detail.errMsg && detail.errMsg.indexOf('deny') !== -1) {
					uni.showToast({ title: '未授权手机号，可点击跳过', icon: 'none' })
				}
			},
			onSkipPhone() {
				this.close({
					nickName: this.nickName,
					avatar: this.avatarUrl,
					phoneCode: ''
				})
			},
			onCancel() {
				this.close(null)
			}
		}
	}
</script>

<style scoped lang="scss">
	$accent: #701018;
	$ink: #17130f;
	$muted: #77716a;
	$line: #e9e2d8;

	.auth-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		z-index: 11000;
		display: flex;
		background: #fff;
	}

	.auth-profile-popup {
		width: 100%;
		height: 100%;
		display: flex;
		flex-direction: column;
		box-sizing: border-box;
		background: #fff;
	}

	.auth-topbar {
		display: grid;
		grid-template-columns: 100rpx 1fr 100rpx;
		align-items: center;
		min-height: 96rpx;
		padding: env(safe-area-inset-top) 32rpx 0;
		box-sizing: content-box;
		color: $ink;
	}

	.back-action,
	.close-action {
		display: flex;
		align-items: center;
		min-height: 76rpx;
		font-size: 26rpx;
	}

	.back-action.hidden {
		visibility: hidden;
	}

	.close-action {
		justify-content: flex-end;
		color: $muted;
	}

	.step-text {
		text-align: center;
		font-size: 24rpx;
		color: #9a948c;
	}

	.auth-step {
		flex: 1;
		min-height: 0;
		display: flex;
		flex-direction: column;
		padding: 18rpx 48rpx calc(40rpx + env(safe-area-inset-bottom));
		box-sizing: border-box;
	}

	.brand-lockup {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-top: 8rpx;
	}

	.brand-logo {
		width: 232rpx;
		height: 152rpx;
	}

	.brand-subtitle {
		margin-top: 8rpx;
		font-size: 24rpx;
		letter-spacing: 3rpx;
		color: $muted;
	}

	.login-hero {
		width: 100%;
		height: 300rpx;
		margin-top: 36rpx;
		border-radius: 28rpx;
	}

	.login-copy {
		margin-top: 42rpx;
	}

	.title {
		font-size: 42rpx;
		font-weight: 800;
		line-height: 1.3;
		color: $ink;
		text-align: center;
	}

	.eyebrow {
		margin-bottom: 12rpx;
		font-size: 24rpx;
		font-weight: 700;
		letter-spacing: 2rpx;
		color: $accent;
		text-align: center;
	}

	.desc {
		margin-top: 22rpx;
		font-size: 28rpx;
		line-height: 1.65;
		color: $muted;

		&.center {
			text-align: center;
		}
	}

	.login-desc {
		text-align: center;
		white-space: nowrap;
	}

	.step-heading {
		margin-top: 48rpx;
		text-align: center;
	}

	.profile-form {
		margin-top: 64rpx;
	}

	.avatar-btn {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 176rpx;
		height: 176rpx;
		padding: 0;
		margin: 0 auto;
		border-radius: 50%;
		background: #f4efe8;
		line-height: normal;
		overflow: visible;

		&::after {
			border: none;
		}
	}

	.avatar-img {
		width: 176rpx;
		height: 176rpx;
		border: 1rpx solid $line;
		border-radius: 50%;
		background: #f4efe8;
	}

	.avatar-edit {
		position: absolute;
		right: -4rpx;
		bottom: 2rpx;
		width: 52rpx;
		height: 52rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 0;
		border: 4rpx solid #fff;
		border-radius: 50%;
		box-sizing: border-box;
		background: $accent;
	}

	.avatar-tip {
		display: block;
		margin-top: 18rpx;
		font-size: 24rpx;
		color: #99928a;
		text-align: center;
	}

	.nickname-row {
		display: flex;
		align-items: center;
		margin-top: 48rpx;
		padding: 0 28rpx;
		border: 1rpx solid $line;
		border-radius: 18rpx;
		background: #fbfaf8;
	}

	.label {
		width: 88rpx;
		font-size: 28rpx;
		font-weight: 700;
		color: $ink;
		flex-shrink: 0;
	}

	.nickname-input {
		flex: 1;
		height: 96rpx;
		font-size: 28rpx;
		color: $ink;
	}

	.nickname-placeholder {
		color: #aaa39b;
	}

	.phone-brand {
		display: flex;
		flex-direction: column;
		align-items: center;
		margin-top: 76rpx;
	}

	.phone-logo {
		width: 168rpx;
		height: 168rpx;
		border-radius: 24rpx;
		box-shadow: 0 18rpx 44rpx rgba(112, 16, 24, 0.14);
	}

	.phone-title {
		margin-top: 34rpx;
		font-size: 42rpx;
		font-weight: 800;
		color: $ink;
	}

	.phone-subtitle {
		margin-top: 18rpx;
		font-size: 26rpx;
		letter-spacing: 2rpx;
		color: $muted;
	}

	.auth-actions {
		margin-top: auto;
	}

	.phone-step .auth-actions {
		margin-top: 92rpx;
	}

	.primary-btn,
	.one-tap-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 96rpx;
		padding: 0;
		border: none;
		border-radius: 48rpx;
		background: $accent;
		color: #fff;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 96rpx;
		text-align: center;
		box-shadow: 0 18rpx 36rpx rgba(112, 16, 24, 0.2);

		&::after {
			border: none;
		}

		&.disabled {
			opacity: 0.48;
			box-shadow: none;
		}
	}

	.agreement-row {
		display: flex;
		align-items: flex-start;
		justify-content: center;
		gap: 12rpx;
		margin-top: 24rpx;
		padding: 8rpx 0;
	}

	.agreement-checkbox {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 30rpx;
		height: 30rpx;
		margin-top: 2rpx;
		border: 2rpx solid #aaa39b;
		border-radius: 50%;
		box-sizing: border-box;
		color: #fff;
		font-size: 20rpx;
		line-height: 1;
		flex-shrink: 0;

		&.checked {
			border-color: $accent;
			background: $accent;
		}
	}

	.agreement-copy {
		font-size: 22rpx;
		line-height: 1.55;
		color: #8a837b;
	}

	.policy-link {
		color: $accent;
	}

	.skip-btn {
		min-height: 80rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-top: 18rpx;
		font-size: 27rpx;
		color: $muted;
	}
</style>
