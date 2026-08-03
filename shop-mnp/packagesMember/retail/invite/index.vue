<template>
	<view class="page">
		<view class="step">
			<u-steps :list="numList" current="-1" mode="number"></u-steps>
		</view>
		<view class="share-box-bg">
			<view class="share-box">
				<view class="share-main">
					<view class="dot l-t"></view>
					<view class="dot l-b"></view>
					<view class="dot r-t"></view>
					<view class="dot r-b"></view>
					<view class="title">邀请函</view>
					<view class="main">
						<view class="user">
							<view class="user-img">
								<image :src="avatar" mode="aspectFill"></image>
							</view>
							<text>{{ userName }}</text>
						</view>
						<view class="tips">和我一起，轻松推广赢奖励</view>
						<view class="code-img">
							<image :src="qrcodeUrl" mode="aspectFit"></image>
						</view>
					</view>
				</view>
			</view>
		</view>
		<canvas canvas-id="myCanvas" :style="{width: `${canvasW}px`, height: `${canvasH}px`}"></canvas>
		<view class="btn-bar">
			<u-button :loading="sharing" :disabled="sharing" @click="submit">分享给好友</u-button>
		</view>
	</view>
</template>

<script>
	import './index.scss'
	import { getInfo } from '@/api/public'
	import { getConsultantInviteQrcode } from '@/api/member/index'
	import { buildShareAppMessage } from '@/utils/invite'
	import { normalizeImageUrl } from '@/utils/consultant'

	export default {
		data() {
			return {
				host: this.$host,
				userName: '用户名称',
				avatar: '/static/img/user_pic.jpg',
				qrcodeUrl: '/static/code.png',
				inviteUserId: null,
				sharing: false,
				numList: [{
					name: '分享邀请\n码给好友'
				}, {
					name: '好友成为推广\n员并带来订单'
				}, {
					name: '获得\n邀请奖励'
				}],
				canvasW: 0,
				canvasH: 0
			}
		},
		onLoad() {
			this.loadPageData()
		},
		onShareAppMessage() {
			return buildShareAppMessage({
				title: `${this.userName}邀请你加入逸享荟`,
				path: '/pages/home/home'
			})
		},
		methods: {
			async loadPageData() {
				await Promise.all([this.loadUserInfo(), this.loadQrcode()])
			},
			async loadUserInfo() {
				try {
					const res = await getInfo()
					const user = res.data || {}
					const consultant = res.consultant || uni.getStorageSync('consultant')
					this.inviteUserId = user.userId || null
					this.userName = (consultant && consultant.consultantName) || user.nickName || '用户名称'
					this.avatar = normalizeImageUrl(this.host, user.avatar) || '/static/img/user_pic.jpg'
				} catch (e) {
					const userInfo = uni.getStorageSync('userInfo')
					const consultant = uni.getStorageSync('consultant')
					if (userInfo) {
						this.inviteUserId = userInfo.userId || null
						this.userName = (consultant && consultant.consultantName) || userInfo.nickName || '用户名称'
						this.avatar = normalizeImageUrl(this.host, userInfo.avatar) || '/static/img/user_pic.jpg'
					}
				}
			},
			async loadQrcode() {
				try {
					const res = await getConsultantInviteQrcode()
					if (res.code === 200 && res.data) {
						const url = res.data.qrcodeUrl || res.data.url || ''
						if (url) {
							this.qrcodeUrl = normalizeImageUrl(this.host, url) || '/static/code.png'
						}
						if (res.data.inviteUserId) {
							this.inviteUserId = res.data.inviteUserId
						}
					}
				} catch (e) {}
			},
			resolveImagePath(src) {
				return new Promise((resolve) => {
					if (!src) {
						resolve('')
						return
					}
					if (src.startsWith('/static') || src.startsWith('wxfile://')) {
						resolve(src)
						return
					}
					wx.getImageInfo({
						src,
						success: (res) => resolve(res.path),
						fail: () => resolve(src)
					})
				})
			},
			submit() {
				if (this.sharing) return
				this.sharing = true
				this.$nextTick(() => {
					wx.createSelectorQuery().in(this).select('.share-box-bg').boundingClientRect((rect) => {
						if (!rect || rect.width <= 0 || rect.height <= 0) {
							this.sharing = false
							uni.showToast({ title: '海报生成失败', icon: 'none' })
							return
						}
						this.canvasW = rect.width
						this.canvasH = rect.height
						this.$nextTick(() => {
							this.drawPoster(rect)
						})
					}).exec()
				})
			},
			async drawPoster(rect) {
				const canvasId = 'myCanvas'
				const ctx = wx.createCanvasContext(canvasId, this)
				const [avatarPath, qrcodePath] = await Promise.all([
					this.resolveImagePath(this.avatar),
					this.resolveImagePath(this.qrcodeUrl)
				])
				ctx.fillStyle = '#EEE2CE'
				ctx.fillRect(0, 0, rect.width, rect.height)
				ctx.fillStyle = '#fff'
				ctx.fillRect(15, 50, rect.width - 30, rect.height - 80)
				ctx.setLineWidth(1)
				ctx.setStrokeStyle('#C8A889')
				ctx.strokeRect(27, 62, rect.width - 54, rect.height - 104)
				ctx.fillStyle = '#C8A889'
				const cornerWidth = 10
				const cornerHeight = 10
				ctx.fillRect(27, 62, cornerWidth, cornerHeight)
				ctx.fillRect(rect.width - 36, 62, cornerWidth, cornerHeight)
				ctx.fillRect(27, rect.height - 52, cornerWidth, cornerHeight)
				ctx.fillRect(rect.width - 36, rect.height - 52, cornerWidth, cornerHeight)
				ctx.fillRect(136, 42, 80, 40)
				ctx.setFillStyle('#fff')
				ctx.setFontSize(18)
				ctx.textAlign = 'center'
				ctx.fillText('邀请函', rect.width / 2, 70)
				if (avatarPath) {
					ctx.drawImage(avatarPath, rect.width / 2 - 50, 100, 100, 100)
				}
				ctx.setFontSize(18)
				ctx.setFillStyle('#333')
				ctx.textAlign = 'center'
				ctx.fillText(this.userName, rect.width / 2, 230)
				ctx.setFontSize(16)
				ctx.fillText('和我一起，轻松推广赢奖励', rect.width / 2, 300)
				if (qrcodePath) {
					const qrSize = 120
					const qrX = rect.width / 2 - qrSize / 2
					const qrY = 350
					ctx.setStrokeStyle('#C8A889')
					ctx.setLineWidth(4)
					ctx.strokeRect(qrX - 4, qrY - 4, qrSize + 8, qrSize + 8)
					ctx.drawImage(qrcodePath, qrX, qrY, qrSize, qrSize)
				}
				ctx.draw(false, () => {
					setTimeout(() => {
						wx.canvasToTempFilePath({
							canvasId,
							success: (res) => {
								this.sharePoster(res.tempFilePath)
							},
							fail: () => {
								this.sharing = false
								uni.showToast({ title: '海报生成失败', icon: 'none' })
							}
						}, this)
					}, 300)
				})
			},
			sharePoster(tempFilePath) {
				if (typeof wx !== 'undefined' && wx.showShareImageMenu) {
					wx.showShareImageMenu({
						path: tempFilePath,
						fail: () => {
							this.showShareFallback()
						},
						complete: () => {
							this.sharing = false
						}
					})
					return
				}
				this.sharing = false
				this.showShareFallback()
			},
			showShareFallback() {
				uni.showModal({
					title: '分享提示',
					content: '请点击右上角「...」转发小程序给好友。',
					showCancel: false
				})
			}
		}
	}
</script>
