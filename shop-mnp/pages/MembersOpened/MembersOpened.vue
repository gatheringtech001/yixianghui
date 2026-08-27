<template>
	<view class="page">
<!-- 		<view class="vip-card" v-if="defaultCard">
			<view class="card-bg">
				<image :src="host + defaultCard.cardImage" mode="" />
			</view>
			<view class="card-main">
				<view class="card-body">
					<view class="content">
						<view class="title">
							<image src="/static/icon/icon-vip.png" mode="" />
							<text>{{ defaultCard.cardName }}</text>
						</view>
						<view class="tips">{{ defaultCard.content }}</view>
						<view class="rule-btn" @click="goToArticle">规则说明</view>
					</view>
					<view class="price" v-if="!isOpen">
						<text>¥</text>
						<text class="num">{{ defaultCard.price }}</text>
						<text>/年</text>
					</view>
					<view class="price" v-else>
						VIP有效期：{{ userCard.enableEndTime ? userCard.enableEndTime:'--' }}
					</view>
				</view>
				<view class="open-btn">立即开卡</view>
				<view class="mark">VIP</view>
			</view>
		</view> -->
		<view v-if="showLegacyLayout">
		<view class="banner">
			<swiper class="screen-swiper square-dot" indicator-dots="true" circular="true" autoplay="true" interval="5000"
				duration="500">
				<swiper-item v-for="(item,index) in swiperList" :key="index" @click="goDetail(item)">
					<image :src='host + item.adImage' mode="aspectFill"></image>
				</swiper-item>
			</swiper>
		</view>
		<!-- 权益标题 -->
		<view class="interests-title">
			<!-- <view class="line line-left"></view> -->
			<view class="title-content">
				<image :src="host + vipInterestTitle" mode="widthFix" />
			</view>
		</view>
		<!-- 会员权益 -->
		<view class="vip-interests">
			<view class="interests-list">
				<view class="img" v-for="(img, i) in vipInterest" :key="i">
					<image :src="host + img.adImage" mode="" />
				</view>
			</view>
			<view class="vip-tips">
				<text>～会员开通后，权益立即到账，无法申请退款～</text>
			</view>
		</view>
		<!-- <view class="open-btn-bar">
			<view class="price">
				<text>¥</text>
				<text class="num">99</text>
				<text>/年</text>
			</view>
			<view class="btn" @click="$u.throttle(buyCard, 500)">
				{{ isOpen ? '立即续费':'立即开卡' }}
			</view>
		</view> -->
		<!-- 分享 -->
		<view class="share-btn" @click="showShareBox">
			<u-icon name="share-fill" size="48" color="#fff" />
			<text>分享</text>
		</view>

		<view class="share-content" ref="shareAdBox" id="shareAdBox" v-show="showShare">
			<view class="page-ad">
				<image src="/static/img/logo.jpg" mode="widthFix"></image>
			</view>
			<view class="info-content">
				<view class="main">
					<view class="user">
						<view class="img">
							<!-- <image src="/static/img/user_pic.jpg" mode="widthFix"></image> -->
							<image :src="userInfo && userInfo.avatar?host+userInfo.avatar : '/static/img/logo.jpg'" mode="widthFix">
							</image>
						</view>
						<text>{{userInfo.nickName}}</text>
					</view>
					<view class="tips">
						<text>{{ defaultCard.content }}</text>
					</view>
				</view>
				<view class="code">
					<image src="/static/code.png" mode=""></image>
				</view>
			</view>
		</view>
		<canvas canvas-id="myCanvas" :style="{width: `${canvasW}px`, height: `${canvasH}px`}"></canvas>
		<!-- 分享 -->
		<u-popup class="share-popup" v-model="showShare" :safe-area-inset-bottom="true" mode="bottom" border-radius="16"
			:closeable="true">
			<view class="btns">
				<button open-type="share">
					<view class="icon-box white">
						<image src="/static/wx_ico.png" mode="" />
					</view><br />
					好友或群
				</button>
				<button @click="downImg">
					<view class="icon-box">
						<u-icon name="download" size="80" color="#fff" />
					</view><br />
					下载海报
				</button>
			</view>
		</u-popup>
		</view>

		<view v-else class="customer-page">
			<u-navbar
				class="weapp-nav-box support-nav"
				:is-back="false"
				title="客服"
				title-color="#111111"
				:title-size="40"
				:title-bold="true"
				:title-width="320"
				:background="{ background: '#ffffff' }"
				:border-bottom="false"
			/>

			<scroll-view class="customer-scroll" scroll-y :show-scrollbar="false">
				<view class="support-info">
					<view class="support-qr-block">
						<image
							class="support-qr"
							:src="customerData.qrCode || '/static/home-design/support-qr.png'"
							mode="aspectFit"
							show-menu-by-longpress
							@tap="openGroupQr"
						/>
						<text>点击放大，长按识别加群</text>
					</view>
					<view class="support-time-block">
						<view class="card-title">客服在线时间</view>
						<view class="support-time">{{ customerData.onlineTime }}</view>
					</view>
				</view>

				<view class="support-list">
					<view class="support-card" v-for="(staff, index) in customerData.staffList" :key="index">
						<image class="staff-avatar" :src="getStaffAvatar(index, staff)" mode="aspectFill" />
						<view class="staff-copy">
							<view class="card-title">客服{{ index + 1 }} {{ staff.name }}</view>
							<view class="meta">手机/微信：{{ staff.wechat }}</view>
						</view>
						<view class="support-actions">
							<view class="btn secondary" @tap.stop="copyWechat(staff.wechat)">复制微信号</view>
							<view class="btn primary" @tap.stop="addWechat(staff)">
								<u-icon name="weixin-fill" color="#ffffff" size="28" />
								<text>添加微信</text>
							</view>
						</view>
					</view>
				</view>
			</scroll-view>
		</view>

		<!-- tabbar -->
		<TabBar :tabBarShow="3"></TabBar>
	</view>
</template>


<script>
	import TabBar from '@/components/TabBar/TabBar.vue'
	import { parseInvitePageOptions } from '@/utils/invite'
	import sharePageMixin from '@/utils/sharePageMixin'
	import {
		getBannerList
	} from '@/api/index'
	import { CUSTOMER_SERVICE_POSITION_ID } from '@/utils/housekeeperRotation'
	import {
		getMemberCardsList,
		prepayCardOrder,
		payCardOrder,
		cancelUserCard
	} from '@/api/member/index'
	export default {
		mixins: [sharePageMixin],
		components: {
			TabBar
		},
		data() {
			return {
				showLegacyLayout: false, // 保留旧版布局，后续可切回 true
				host: this.$host,
				userInfo: null,
				showShare: false,
				posterImg: '',
				cardList: [],
				defaultCard: null,
				isOpen: false,
				swiperList: [],
				vipInterest: [],
				vipInterestTitle: '',
				canvasW: 0,
				canvasH: 0,
				userCard: undefined,
				staffAvatar: '/static/img/customer-avatar.png',
				customerData: {
					headerBg: '',
					qrCode: '/static/code.png',
					onlineTime: '08:30~21:00',
					hotline: '400-800-1234',
					staffList: [{
							name: '媛媛',
							wechat: '15887297809'
						},
						{
							name: '曼曼',
							wechat: '18008890435'
						}
					]
				}
			};
		},

		onLoad(options) {
			parseInvitePageOptions(options)
			if (this.showLegacyLayout) {
				this.getAdList()
			} else {
				this.getCustomerRemoteData()
			}
		},
		onReady() {
			uni.hideTabBar()
		},

		async onShow() {
			if (!this.showLegacyLayout) {
				this.getCustomerRemoteData()
				return
			}
			this.userInfo = uni.getStorageSync('userInfo')
			if (this.userInfo && this.userInfo != '' && this.userInfo != undefined) this.getCards()
			this.userCard = uni.getStorageSync('userCard')
			if (!this.userCard || this.userCard.cardKey == 'vip0') {
				this.isOpen = false
			} else {
				let endTime = new Date(this.userCard.enableEndTime).getTime()
				let now = new Date().getTime()
				if (endTime < now) {
					this.isOpen = false
				} else {
					this.isOpen = true
				}
			}
		},
		methods: {
			getShareConfig() {
				return {
					title: '逸享荟康养，邀请您一起体验',
					path: '/pages/home/home'
				}
			},
			getStaffAvatar(index, staff) {
				if (staff && staff.avatar && staff.avatar !== '/static/img/customer-avatar.png') {
					return staff.avatar
				}
				const defaults = [
					'/static/home-design/support-avatar-1.png',
					'/static/home-design/support-avatar-2.png',
					'/static/home-design/support-avatar-3.png'
				]
				return defaults[index] || defaults[0]
			},
			async getCustomerRemoteData() {
				try {
					const [{
						data: qrData
					}, {
						data: staffData
					}] = await Promise.all([
						getBannerList({
							positionId: 3
						}),
						getBannerList({
							positionId: CUSTOMER_SERVICE_POSITION_ID
						})
					])

					const qrItem = (qrData || []).find(item => item.contentId == 19) ||
						(qrData || []).find(item => item.adImage)
					if (qrItem && qrItem.adImage) {
						this.customerData.qrCode = this.normalizeImage(qrItem.adImage)
					}
					if (qrItem && qrItem.description) {
						this.customerData.hotline = String(qrItem.description)
					}
					const headerItem = (qrData || []).find(item => item.contentId == 22)
					if (headerItem && headerItem.adImage) {
						this.customerData.headerBg = this.normalizeImage(headerItem.adImage)
					}

					const remoteStaffList = this.parseStaffListFromContent(staffData || [])
					if (remoteStaffList.length) {
						this.customerData.staffList = remoteStaffList
					}
				} catch (error) {
					console.error('加载客服配置失败:', error)
				}
			},
			parseStaffListFromContent(staffData) {
				// 兼容两种结构：
				// 1) positionId=7 返回多条记录，每条 content 是客服对象
				// 2) 仅一条记录，content 是客服数组
				const parsed = []
				staffData.forEach(item => {
					// 新接口字段（你提供的返回）：adName/description/adImage/adContent
					if (item && (item.adName || item.description || item.adImage || item.adContent)) {
						parsed.push(this.formatStaff(item))
						return
					}

					const content = this.safeJsonParse(item.content || item.adContent)
					if (Array.isArray(content)) {
						content.forEach(v => parsed.push(this.formatStaff(v)))
					} else if (content && typeof content === 'object') {
						parsed.push(this.formatStaff(content))
					}
				})
				return parsed.filter(v => v.name && v.wechat)
			},
			formatStaff(item) {
				const qrCode = this.normalizeImage(item.adImage || item.qrCode || item.image || '')
				return {
					name: item.adName || item.name || item.nickName || '',
					wechat: String(item.description || item.wechat || item.phone || item.mobile || item.adContent || ''),
					avatar: this.staffAvatar,
					qrCode
				}
			},
			safeJsonParse(content) {
				if (!content) return null
				if (typeof content === 'object') return content
				try {
					return JSON.parse(content)
				} catch (e) {
					return null
				}
			},
			normalizeImage(path) {
				if (!path) return ''
				if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('/static/')) {
					return path
				}
				if (path.startsWith('/')) {
					return `${this.host}${path}`
				}
				return `${this.host}/${path}`
			},
			previewQrImage(url) {
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
			openGroupQr() {
				this.previewQrImage(this.customerData.qrCode)
			},
			previewStaffQr(staff) {
				this.previewQrImage(staff.qrCode)
			},
			callHotline() {
				uni.makePhoneCall({
					phoneNumber: this.customerData.hotline
				})
			},
			copyWechat(wechat) {
				uni.setClipboardData({
					data: String(wechat),
					success: () => {
						uni.showToast({
							icon: 'none',
							title: '微信号已复制'
						})
					}
				})
			},
			addWechat(staff) {
				if (staff.qrCode) {
					this.previewStaffQr(staff)
					return
				}
				this.copyWechat(staff.wechat)
			},
			async getCards() {
				let {
					rows
				} = await getMemberCardsList()
				this.cardList = rows
				if (!this.userCard) {
					this.defaultCard = rows.find(v => v.cardKey == 'vip0')
				} else {
					this.defaultCard = rows.find(v => v.cardId == this.userCard.cardId)
				}

			},
			async getAdList() {
				let params = {
					positionId: 3
				}
				let {
					data
				} = await getBannerList(params)
				this.swiperList = data.filter(v => v.contentId == 6)
				this.getInterest()
			},
			async getInterest() {
				let params = {
					positionId: 3
				}
				let {
					data
				} = await getBannerList(params)
				this.vipInterest = data.filter(v => v.contentId == 10)
				this.vipInterestTitle = data.find(v => v.contentId == 9).adImage
			},

			showShareBox() {
				this.showShare = true
			},
			goToArticle() {
				uni.navigateTo({
					url: '/packagesPublic/Article/index?id=4'
				})
			},
			goDetail(item) {
				let id = item.linkUrl.split('=')[1]
				uni.navigateTo({
					url: `/packagesPublic/Article/index?id=${id}`
				})
			},
			async downImg() {
				const _this = this;
				this.$nextTick(() => {
					wx.createSelectorQuery().select('#shareAdBox').boundingClientRect(async rect => {
						if (rect && rect.width > 0 && rect.height > 0) {
							_this.canvasW = rect.width
							_this.canvasH = rect.height
							const canvasId = 'myCanvas'
							const ctx = wx.createCanvasContext(canvasId)
							// 绘制背景
							ctx.fillStyle = '#ffffff'
							ctx.fillRect(0, 0, rect.width, rect.height)
							// 绘制顶部图片
							let path_a = '../../static/img/logo.jpg'
							let imgInfo_a = await uni.getImageInfo({
								src: path_a
							})
							let rate = imgInfo_a[1].width / imgInfo_a[1].height
							ctx.drawImage(path_a, 0, 0, rect.width, rect.width / rate)

							// 绘制头像
							let path_b = this.userInfo && this.userInfo.avatar ? this.host + this.userInfo.avatar :
								'../../static/img/user_pic.jpg'
							let imgInfo_b = await uni.getImageInfo({
								src: path_b
							})
							let rate_b = imgInfo_b[1].width / imgInfo_b[1].height

							ctx.arc(40, rect.width / rate + 40, 25, 0, 2 * Math.PI)
							ctx.setFillStyle('#ffffff')
							ctx.fill() //保证图片无bug填充
							// ctx.clip();//画了圆 再剪切 原始画布中剪切任意形状和尺寸。一旦剪切了某个区域，则所有之后的绘图都会被限制在被剪切的区域内
							ctx.drawImage(path_b, 15, rect.width / rate + 12, 50, 50 / rate_b)

							ctx.font = 'bolder 16px sans-serif'; // 设置字体
							ctx.fillStyle = '#000'; // 字体颜色
							ctx.fillText(this.userInfo.nickName, 75, rect.width / rate + 45); // 绘制用户名

							// 绘制描述文字
							ctx.font = '13px sans-serif';
							ctx.fillStyle = '#666'; // 字体颜色
							ctx.fillText('邀请您开通逸享荟会员，尊享多项', 16, rect.width / rate + 85, rect.width - 150)
							ctx.fillText('权益，12000元权益等你来拿！', 16, rect.width / rate + 106, rect.width - 150)

							// 绘制二维码
							ctx.drawImage('../../static/code.png', rect.width - 100, rect.width / rate + 24, 88, 88)
							ctx.draw(true, () => {
								// 将 Canvas 转换为临时文件路径
								wx.canvasToTempFilePath({
									canvasId: canvasId,
									success(res) {
										// 获取 Base64 数据
										wx.getFileSystemManager().readFile({
											filePath: res.tempFilePath,
											encoding: 'base64',
											success(fileRes) {
												_this.saveImage(fileRes.data)
											},
											fail(err) {
												console.error('获取 Base64 失败:', err)
											}
										});
									},
									fail(err) {
										console.error('导出图片失败:', err)
									}
								});
							});
						} else {
							console.error('获取的尺寸为零或无效:', rect)
						}
					}).exec()
				})
			},
			saveImage(imageData) {
				const fs = wx.getFileSystemManager()
				const times = new Date().getTime()
				const codeimg = wx.env.USER_DATA_PATH + '/' + times + '.png'
				fs.writeFile({
					filePath: codeimg,
					data: imageData,
					encoding: 'base64',
					success: (res) => {
						uni.saveImageToPhotosAlbum({
							filePath: codeimg,
							success: function() {
								uni.showToast({
									title: "保存成功",
									icon: "success"
								});
							},
							fail: function() {
								uni.showToast({
									title: "保存失败，请稍后重试",
									icon: "none"
								});
							}
						})
					}
				})
			},
			// 开卡
			buyCard() {
				let thecard = this.cardList.find(v => v.cardKey == 'vip1')
				prepayCardOrder({
					cardId: thecard.cardId
				}).then(res => {
					let params = {
						cardId: thecard.cardId,
						recordId: res.data.recordId
					}
					const pendingRecordId = res.data.recordId
					payCardOrder(params).then(result => {
						let order = result.data
						let orderInfo = {
							"timeStamp": String(order.timeStamp),
							"nonceStr": order.nonceStr,
							"package": order.packageVal,
							"signType": order.signType,
							"paySign": order.paySign
						}
						uni.requestPayment({
							provider: 'wxpay',
							...orderInfo,
							success: (e) => {
								uni.showToast({
									icon: 'success',
									title: '开卡成功'
								})
								setTimeout(() => {
									uni.redirectTo({
										url: `/packagesMall/PayResult/PayResult?orderAmount=${this.orderAmount}`
									})
								}, 2000)
								this.getUserInfo()
							},
							fail: (e) => {
								console.log(e)
								uni.showModal({
									content: "本次支付未成功，继续支付？",
									confirmText: "继续支付",
									cancelText: "取消开通",
									success: (res) => {
										if (res.confirm) {
											this.buyCard()
										} else if (res.cancel && pendingRecordId) {
											cancelUserCard(pendingRecordId).catch(() => {})
										}
									},
								})
							}
						})
					})
				})
			},
			async getUserInfo() {
				const res = await getInfo()
				uni.setStorageSync('consultant', res.consultant)
				uni.setStorageSync('userCard', res.userCard)
				uni.setStorageSync('userInfo', res.data)
				uni.setStorageSync('userData', res.userInfo)
				let endTime = new Date(res.userCard.enableEndTime).getTime()
				let now = new Date().getTime()
				this.userCard = res.userCard
				if (endTime < now || !this.userCard || this.userCard.cardKey == 'vip0') {
					this.defaultCard = this.cardList.find(v => v.cardKey == 'vip1')
					this.isOpen = false
				} else {
					this.defaultCard = this.cardList.find(v => v.cardId == this.userCard.cardId)
					this.isOpen = true
				}
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'MembersOpened.scss';
</style>
