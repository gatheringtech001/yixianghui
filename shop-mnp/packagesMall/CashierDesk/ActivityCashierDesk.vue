<template>
	<view class="page">
		<view class="price-count-down">
			<view class="price">
				<text class="min">{{ currencySymbol }}</text>
				<text class="max">{{ displayAmount }}</text>
				<text class="min">元</text>
			</view>
			<view class="count-down">
				<view class="title">{{ isExpired ? labels.expiredTitle : labels.countdownTitle }}</view>
				<view class="count" v-if="!isExpired">
					<text class="time">{{ hour < 10 ? `0${hour}` : hour }}</text>
					<text class="dot">:</text>
					<text class="time">{{ min < 10 ? `0${min}` : min }}</text>
					<text class="dot">:</text>
					<text class="time">{{ sec < 10 ? `0${sec}` : sec }}</text>
				</view>
				<view class="expired-tip" v-else>{{ labels.expiredTip }}</view>
			</view>
		</view>
		<view class="order-brief" v-if="orderId">
			<view class="brief-title">{{ labels.orderTitle }}</view>
			<view class="brief-row" v-if="activityName">
				<text class="label">{{ labels.activityName }}</text>
				<text class="value">{{ activityName }}</text>
			</view>
			<view class="brief-row">
				<text class="label">{{ labels.orderNo }}</text>
				<text class="value">{{ orderNo || '-' }}</text>
			</view>
		</view>
		<view class="pay-way">
			<view class="pay-list">
				<view class="list" v-for="(item, index) in PayList" :key="index" @click="onPayWay(item, index)">
					<view class="pay-type">
						<image :src="item.icon" mode=""></image>
						<text>{{ item.name }}</text>
					</view>
					<view class="check">
						<text class="iconfont" :class="PayWay === index ? 'icon-checked action' : 'icon-check'"></text>
					</view>
				</view>
			</view>
		</view>
		<view class="pay-submit">
			<view class="jump" @click="goOrder">{{ labels.skipPay }}</view>
			<view class="submit" :class="{ disabled: isExpired || paying }" @click="$u.throttle(onSubmit, 500)">{{ isExpired ? labels.expiredTitle : (displayAmount + '元') }}</view>
		</view>
	</view>
</template>

<script>
	import { payActivityOrder, getActivityOrderInfo, syncActivityOrderPay } from '@/api/activity/index'

	const PAY_TIMEOUT_SEC = 30 * 60

	export default {
		data() {
			return {
				currencySymbol: '\uFFE5',
				labels: {
					countdownTitle: '\u652f\u4ed8\u5269\u4f59\u65f6\u95f4',
					expiredTitle: '\u652f\u4ed8\u5df2\u8d85\u65f6',
					expiredTip: '\u8bf7\u8fd4\u56de\u91cd\u65b0\u62a5\u540d',
					orderTitle: '\u6d3b\u52a8\u62a5\u540d\u8ba2\u5355',
					activityName: '\u6d3b\u52a8\u540d\u79f0',
					orderNo: '\u8ba2\u5355\u53f7',
					skipPay: '\u6682\u4e0d\u652f\u4ed8',
					wxPay: '\u5fae\u4fe1\u652f\u4ed8',
					payTipTitle: '\u652f\u4ed8\u63d0\u793a',
					cancel: '\u53d6\u6d88',
					confirmPay: '\u7acb\u5373\u652f\u4ed8',
					paySuccess: '\u652f\u4ed8\u6210\u529f',
					payRetry: '\u672c\u6b21\u652f\u4ed8\u672a\u6210\u529f\uff0c\u7ee7\u7eed\u652f\u4ed8\uff1f',
					confirm: '\u786e\u5b9a',
					back: '\u8fd4\u56de',
					payFail: '\u53d1\u8d77\u652f\u4ed8\u5931\u8d25',
					orderPaid: '\u8ba2\u5355\u5df2\u652f\u4ed8',
					orderInvalid: '\u8ba2\u5355\u72b6\u6001\u5df2\u53d8\u66f4',
					orderLoadFail: '\u8ba2\u5355\u52a0\u8f7d\u5931\u8d25',
					expiredBlock: '\u652f\u4ed8\u5df2\u8d85\u65f6\uff0c\u8bf7\u91cd\u65b0\u62a5\u540d'
				},
				PayList: [{
					icon: '/static/wx_pay.png',
					name: '\u5fae\u4fe1\u652f\u4ed8'
				}],
				orderAmount: 0,
				orderId: null,
				orderNo: null,
				activityName: '',
				payDeadline: 0,
				countdownTimer: null,
				isExpired: false,
				paying: false,
				PayWay: 0,
				PayPirce: '\u5fae\u4fe1\u652f\u4ed8',
				hour: 0,
				min: 0,
				sec: 0
			}
		},
		computed: {
			displayAmount() {
				const num = Number(this.orderAmount)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			}
		},
		onLoad(option) {
			if (!option.orderId) {
				uni.showToast({ title: this.labels.orderLoadFail, icon: 'none' })
				setTimeout(() => uni.navigateBack(), 1500)
				return
			}
			this.orderId = option.orderId
			this.orderNo = option.orderNo || ''
			this.orderAmount = option.orderAmount || 0
			this.initPage()
		},
		onUnload() {
			this.clearCountdown()
		},
		methods: {
			deadlineStorageKey() {
				return `activity_pay_deadline_${this.orderId}`
			},
			async initPage() {
				try {
					const { data } = await getActivityOrderInfo(this.orderId)
					if (!data) {
						throw new Error(this.labels.orderLoadFail)
					}
					if (String(data.payStatus) !== '0') {
						uni.showToast({ title: this.labels.orderPaid, icon: 'none' })
						setTimeout(() => {
							uni.redirectTo({ url: '/packagesMember/MyActivity/index' })
						}, 1500)
						return
					}
					this.orderAmount = data.moneyPayable
					this.orderNo = data.orderNo || this.orderNo
					this.activityName = (data.activityInfo && data.activityInfo.activityName) || ''
					this.initPayDeadline(data.createTime)
					this.startCountdown()
				} catch (err) {
					uni.showToast({
						title: (err && err.message) || this.labels.orderLoadFail,
						icon: 'none'
					})
				}
			},
			initPayDeadline(createTime) {
				const storageKey = this.deadlineStorageKey()
				const createTs = createTime
					? new Date(String(createTime).replace(/-/g, '/')).getTime()
					: NaN
				// 优先用服务端下单时间，避免本地缓存与订单真实截止不一致
				if (Number.isFinite(createTs)) {
					this.payDeadline = createTs + PAY_TIMEOUT_SEC * 1000
					uni.setStorageSync(storageKey, this.payDeadline)
					return
				}
				let deadline = Number(uni.getStorageSync(storageKey))
				if (!deadline || Number.isNaN(deadline)) {
					deadline = Date.now() + PAY_TIMEOUT_SEC * 1000
					uni.setStorageSync(storageKey, deadline)
				}
				this.payDeadline = deadline
			},
			startCountdown() {
				this.clearCountdown()
				const tick = () => {
					const remain = Math.max(0, Math.floor((this.payDeadline - Date.now()) / 1000))
					this.hour = Math.floor(remain / 3600)
					this.min = Math.floor((remain % 3600) / 60)
					this.sec = remain % 60
					this.isExpired = remain <= 0
					if (remain > 0) {
						this.countdownTimer = setTimeout(tick, 1000)
					}
				}
				tick()
			},
			clearCountdown() {
				if (this.countdownTimer) {
					clearTimeout(this.countdownTimer)
					this.countdownTimer = null
				}
			},
			clearDeadlineStorage() {
				uni.removeStorageSync(this.deadlineStorageKey())
			},
			onPayWay(item, index) {
				this.PayWay = index
				this.PayPirce = item.name
			},
			onSubmit() {
				if (this.isExpired) {
					uni.showToast({ title: this.labels.expiredBlock, icon: 'none' })
					return
				}
				if (this.paying) return
				const _this = this
				uni.showModal({
					title: this.labels.payTipTitle,
					showCancel: true,
					cancelText: this.labels.cancel,
					cancelColor: '#000000',
					confirmText: this.labels.confirmPay,
					confirmColor: '#3CC51F',
					success(res) {
						if (res.confirm) {
							_this.pay()
						}
					}
				})
			},
			pay() {
				if (this.isExpired || this.paying) return
				this.paying = true
				const params = { orderId: this.orderId }
				payActivityOrder(params).then(res => {
					const order = res.data
					if (!order || !order.timeStamp) {
						throw new Error(this.labels.payFail)
					}
					const orderInfo = {
						timeStamp: String(order.timeStamp),
						nonceStr: order.nonceStr,
						package: order.packageVal,
						signType: order.signType,
						paySign: order.paySign
					}
					uni.requestPayment({
						provider: 'wxpay',
						...orderInfo,
						success: () => {
							this.clearDeadlineStorage()
							const amountNum = Number(this.displayAmount)
							const amount = Number.isFinite(amountNum) ? amountNum.toFixed(2) : String(this.displayAmount || '0')
							const gold = Number.isFinite(amountNum) && amountNum > 0 ? Math.floor(amountNum) : 0
							const finish = () => {
								uni.showModal({
									title: this.labels.paySuccess,
									content: gold > 0
										? `实付￥${amount}元，获得${gold}金币`
										: `实付￥${amount}元`,
									showCancel: false,
									confirmText: '知道了',
									success: () => {
										uni.redirectTo({
											url: `/packagesMall/PayResult/PayResult?orderAmount=${this.displayAmount}&type=activity&orderId=${this.orderId}`
										})
									}
								})
							}
							// 主动查单落库，避免回调延迟导致详情仍显示待支付
							syncActivityOrderPay(this.orderId).then(() => finish()).catch(() => finish())
						},
						fail: () => {
							uni.showModal({
								content: this.labels.payRetry,
								confirmText: this.labels.confirm,
								cancelText: this.labels.back
							})
						},
						complete: () => {
							this.paying = false
						}
					})
				}).catch((err) => {
					this.paying = false
					uni.showToast({
						title: (err && err.message) || this.labels.payFail,
						icon: 'none'
					})
				})
			},
			goOrder() {
				uni.redirectTo({
					url: '/packagesMember/MyActivity/index'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import '../CashierDesk/CashierDesk.scss';

	.order-brief {
		margin-top: 20rpx;
		padding: 24rpx 30rpx;
		background: #fff;

		.brief-title {
			font-size: 30rpx;
			font-weight: 700;
			color: #222;
			margin-bottom: 16rpx;
		}

		.brief-row {
			display: flex;
			align-items: flex-start;
			justify-content: space-between;
			gap: 20rpx;
			padding: 10rpx 0;
			font-size: 26rpx;

			.label {
				flex-shrink: 0;
				color: #888;
			}

			.value {
				flex: 1;
				text-align: right;
				color: #333;
				word-break: break-all;
			}
		}
	}

	.expired-tip {
		margin-top: 8rpx;
		font-size: 26rpx;
		color: #999;
		text-align: center;
	}

	.pay-submit .submit.disabled {
		opacity: 0.45;
	}
</style>
