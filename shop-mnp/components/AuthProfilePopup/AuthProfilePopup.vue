<template>
	<view v-if="visible" class="auth-mask" @touchmove.stop.prevent>
		<view class="auth-profile-popup" @tap.stop>
			<view class="title">{{ step === 1 ? '完善头像昵称' : '绑定手机号' }}</view>

			<block v-if="step === 1">
				<view class="desc">请先授权头像和昵称，用于展示个人资料与订单信息。</view>
				<view class="profile-form">
					<button class="avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
						<image class="avatar-img" :src="avatarPreview" mode="aspectFill" />
						<text class="avatar-tip">点击设置头像</text>
					</button>
					<view class="nickname-row">
						<text class="label">昵称</text>
						<input
							class="nickname-input"
							type="nickname"
							v-model="nickName"
							maxlength="30"
							placeholder="请输入昵称"
							placeholder-class="nickname-placeholder"
							@blur="onNicknameBlur"
						/>
					</view>
				</view>
				<view class="primary-btn" @tap="goPhoneStep">下一步</view>
				<view class="btn cancel" @tap="onCancel">取消</view>
			</block>

			<block v-else>
				<view class="desc">授权手机号便于为您提供更好的预订与服务体验，您也可以选择跳过，不影响正常使用。</view>
				<button
					class="one-tap-btn"
					open-type="getPhoneNumber"
					@getphonenumber="onGetPhoneNumber"
				>
					授权手机号
				</button>
				<view class="btn skip" @tap="onSkipPhone">暂不绑定，先进入</view>
				<view class="btn cancel" @tap="onBackStep">返回上一步</view>
			</block>
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
				resolveFn: null,
				avatarUrl: '',
				avatarLocalPreview: '',
				avatarUploading: false,
				nickName: ''
			}
		},
		computed: {
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
				return '/static/img/logo.jpg'
			}
		},
		methods: {
			open(resolve) {
				this.resolveFn = resolve
				this.step = 1
				this.avatarUrl = ''
				this.avatarLocalPreview = ''
				this.avatarUploading = false
				this.nickName = ''
				this.visible = true
			},
			close(result) {
				this.visible = false
				this.step = 1
				if (this.resolveFn) {
					this.resolveFn(result)
					this.resolveFn = null
				}
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
				})
			},
			onNicknameBlur(e) {
				const value = (e.detail && e.detail.value) || ''
				if (value) {
					this.nickName = value.trim()
				}
			},
			goPhoneStep() {
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
				this.step = 2
			},
			onBackStep() {
				this.step = 1
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
	.auth-mask {
		position: fixed;
		left: 0;
		top: 0;
		right: 0;
		bottom: 0;
		z-index: 11000;
		background: rgba(0, 0, 0, 0.5);
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 40rpx;
		box-sizing: border-box;
	}

	.auth-profile-popup {
		width: 100%;
		max-width: 640rpx;
		padding: 40rpx 36rpx 32rpx;
		background: #fff;
		border-radius: 16rpx;
	}

	.title {
		font-size: 34rpx;
		font-weight: 600;
		color: #333;
		text-align: center;
	}

	.desc {
		margin-top: 24rpx;
		padding: 0 8rpx;
		font-size: 28rpx;
		color: #666;
		line-height: 1.6;
		text-align: left;
	}

	.profile-form {
		margin-top: 32rpx;
	}

	.avatar-btn {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 0;
		margin: 0 auto;
		background: transparent;
		border: none;
		line-height: normal;

		&::after {
			border: none;
		}
	}

	.avatar-img {
		width: 140rpx;
		height: 140rpx;
		border-radius: 50%;
		background: #f5f5f5;
	}

	.avatar-tip {
		margin-top: 12rpx;
		font-size: 24rpx;
		color: #999;
	}

	.nickname-row {
		margin-top: 32rpx;
		display: flex;
		align-items: center;
		padding: 0 8rpx;
		border-bottom: 1rpx solid #eee;
	}

	.label {
		width: 90rpx;
		font-size: 28rpx;
		color: #333;
		flex-shrink: 0;
	}

	.nickname-input {
		flex: 1;
		height: 88rpx;
		font-size: 28rpx;
		color: #333;
	}

	.nickname-placeholder {
		color: #bbb;
	}

	.primary-btn,
	.one-tap-btn {
		margin-top: 40rpx;
		width: 100%;
		height: 88rpx;
		line-height: 88rpx;
		background: linear-gradient(90deg, #FF7906, #FE6F1B);
		color: #fff;
		font-size: 30rpx;
		border-radius: 44rpx;
		border: none;
		text-align: center;

		&::after {
			border: none;
		}
	}

	.btn {
		margin-top: 24rpx;
		height: 80rpx;
		line-height: 80rpx;
		text-align: center;
		border-radius: 40rpx;
		font-size: 28rpx;
	}

	.skip {
		background: #fff7ef;
		color: #ff7906;
	}

	.cancel {
		background: #f5f5f5;
		color: #666;
	}
</style>
