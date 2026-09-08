<template>
	<view class="teacher-contact-card">
		<text class="teacher-title">扫码加齐齐老师</text>
		<text class="teacher-note">活动相关问题可联系老师</text>
		<image class="teacher-qr" :src="qrImage" mode="aspectFit" show-menu-by-longpress @click="previewQr" />
		<text class="teacher-hint">点击放大，长按保存后用微信扫一扫</text>
		<u-popup :value="value" mode="center" border-radius="24" :closeable="true" :mask-close-able="false" @input="$emit('input', $event)">
			<view class="teacher-success-popup">
				<text class="success-title">报名成功</text>
				<text class="teacher-title">扫码加齐齐老师</text>
				<image class="teacher-qr" :src="qrImage" mode="aspectFit" show-menu-by-longpress @click="previewQr" />
				<text class="teacher-hint">点击放大，长按保存后用微信扫一扫</text>
				<text class="teacher-note">之后可在“我的预约”详情中再次查看</text>
				<button class="teacher-confirm" @click="$emit('input', false)">我知道了</button>
			</view>
		</u-popup>
	</view>
</template>

<script>
	export default {
		props: {
			value: { type: Boolean, default: false }
		},
		data() {
			return { qrImage: '/packagesMember/static/qiqi-teacher-qr.png' }
		},
		methods: {
			previewQr() {
				const fail = () => uni.showToast({ title: '二维码打开失败，请重试', icon: 'none' })
				const path = `${uni.env.USER_DATA_PATH}/activity-qiqi-teacher.png`
				// 原生预览需要可读取的本地文件，不能直接使用分包资源路径。
				uni.getFileSystemManager().copyFile({
					srcPath: this.qrImage.slice(1),
					destPath: path,
					success: () => uni.previewImage({ current: path, urls: [path], fail }),
					fail
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
