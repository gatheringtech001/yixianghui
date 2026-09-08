<template>
	<view class="teacher-contact-card">
		<text class="teacher-title">扫码加齐齐老师</text>
		<text class="teacher-note">活动相关问题可联系老师</text>
		<image v-if="qrImage" class="teacher-qr" :src="qrImage" mode="aspectFit" show-menu-by-longpress @click="previewQr" @error="onImageError" />
		<view v-else class="teacher-qr qr-placeholder" @click="loadQr">{{ loading ? '二维码加载中…' : qrError }}<text v-if="!loading">点击重试</text></view>
		<text class="teacher-hint">点击放大，长按保存后用微信扫一扫</text>
		<u-popup :value="value" mode="center" border-radius="24" :closeable="true" :mask-close-able="false" @input="$emit('input', $event)">
			<view class="teacher-success-popup">
				<text class="success-title">报名成功</text>
				<text class="teacher-title">扫码加齐齐老师</text>
				<image v-if="qrImage" class="teacher-qr" :src="qrImage" mode="aspectFit" show-menu-by-longpress @click="previewQr" @error="onImageError" />
				<view v-else class="teacher-qr qr-placeholder" @click="loadQr">{{ loading ? '二维码加载中…' : qrError }}<text v-if="!loading">点击重试</text></view>
				<text class="teacher-hint">点击放大，长按保存后用微信扫一扫</text>
				<text class="teacher-note">之后可在“我的预约”详情中再次查看</text>
				<button class="teacher-confirm" @click="$emit('input', false)">我知道了</button>
			</view>
		</u-popup>
	</view>
</template>

<script>
	import { getBannerPosList, getBannerList } from '@/api/index'
	import { AD_POSITION, resolveAdImageUrl } from '@/utils/adAsset'
	export default {
		props: {
			value: { type: Boolean, default: false }
		},
		data() {
			return { qrImage: '', qrError: '', loading: false }
		},
		created() {
			this.loadQr()
		},
		methods: {
			async loadQr() {
				if (this.loading) return
				this.loading = true
				this.qrImage = ''
				this.qrError = ''
				try {
					const positionCode = AD_POSITION.ACTIVITY_TEACHER_QR
					const { data: positions } = await getBannerPosList({ positionCode, status: '0', pageSize: 10 })
					const position = (positions || []).find(item => item.positionCode === positionCode && String(item.status) === '0')
					if (!position) throw new Error('活动老师二维码暂未配置')
					// 广告内容沿用后台 enable_status：1 启用，0 禁用。
					const { data: contents } = await getBannerList({ positionId: position.positionId, status: '1' })
					const item = (contents || []).filter(row => String(row.status) === '1' && row.adImage)
						.sort((a, b) => Number(a.orderNum || 0) - Number(b.orderNum || 0) || Number(b.contentId || 0) - Number(a.contentId || 0))[0]
					if (!item) throw new Error('活动老师二维码暂未配置')
					const url = resolveAdImageUrl(this.$host, item.adImage)
					if (!/^https:\/\/[^/\s]+\/\S+$/.test(url)) throw new Error('二维码地址无效，请联系工作人员')
					this.qrImage = url
				} catch (error) {
					this.qrError = (error && (error.message || error.errMsg)) || '二维码加载失败，请重试'
				} finally {
					this.loading = false
				}
			},
			onImageError() {
				this.qrImage = ''
				this.qrError = '二维码加载失败，请重试'
			},
			previewQr() {
				if (!this.qrImage) return
				uni.previewImage({
					current: this.qrImage, urls: [this.qrImage],
					fail: () => uni.showToast({ title: '二维码打开失败，请重试', icon: 'none' })
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	.teacher-contact-card {
		margin: 24rpx 0;
		padding: 28rpx 20rpx;
		border: 1rpx solid #e8e1d8;
		border-radius: 20rpx;
		background: #fff;
		text-align: center;
	}
	.teacher-success-popup {
		width: 650rpx;
		box-sizing: border-box;
		padding: 48rpx 28rpx 28rpx;
		background: #f7f7f5;
		text-align: center;
	}
	.success-title, .teacher-title, .teacher-note, .teacher-hint { display: block; }
	.success-title { margin-bottom: 20rpx; color: #701018; font-size: 38rpx; font-weight: 700; }
	.teacher-title { color: #111; font-size: 32rpx; font-weight: 700; line-height: 1.5; }
	.teacher-note { margin-top: 12rpx; color: #777; font-size: 26rpx; line-height: 1.6; }
	.teacher-qr { display: block; width: 400rpx; height: 414rpx; margin: 24rpx auto; background: #fff; }
	.qr-placeholder { display: flex; flex-direction: column; justify-content: center; gap: 20rpx; color: #777; font-size: 26rpx; line-height: 1.6; }
	.teacher-hint { color: #555; font-size: 26rpx; line-height: 1.6; }
	.teacher-confirm {
		margin-top: 28rpx;
		border-radius: 16rpx;
		background: #701018;
		color: #fff;
		font-size: 30rpx;
		font-weight: 700;
		line-height: 88rpx;
		&::after { border: none; }
	}
</style>
