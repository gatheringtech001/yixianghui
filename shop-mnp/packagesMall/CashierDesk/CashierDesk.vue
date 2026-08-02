<template>
	<view class="page">
		<view class="price-count-down">
			<view class="price">
				<text class="min">￥</text>
				<text class="max">{{orderAmount}}</text>
				<text class="min">元</text>
			</view>
			<view class="count-down">
				<view class="title">{{ isExpired ? '支付已超时' : '支付剩余时间' }}</view>
				<view class="count" v-if="!isExpired">
					<text class="time">{{hour < 10 ? `0${hour}`:hour}}</text>
					<text class="dot">:</text>
					<text class="time">{{min < 10 ? `0${min}`:min}}</text>
					<text class="dot">:</text>
					<text class="time">{{sec < 10 ? `0${sec}`:sec}}</text>
				</view>
				<view class="count" v-else>
					<text class="time">00</text><text class="dot">:</text><text class="time">00</text><text class="dot">:</text><text class="time">00</text>
				</view>
			</view>
		</view> 
		<!-- 支付方式列表 -->
		<view class="pay-way">
			<view class="pay-list">
				<view class="list" v-for="(item,index) in PayList" 
				@click="onPayWay(item,index)"
				:key="index">
					<view class="pay-type">
						<image :src="item.icon" mode=""></image>
						<text>{{item.name}}</text>
					</view>
					<view class="check">
						<text class="iconfont" :class="PayWay === index ? 'icon-checked action':'icon-check'"></text>
					</view>
				</view>
			</view>
		</view>
		<view class="pay-submit">
			<view class="jump" @click="goOrder">暂不支付</view>
			<view class="submit" :class="{ disabled: isExpired }" @click="$u.throttle(onSubmit, 500)">{{ isExpired ? '已超时' : (orderAmount + '元') }}</view>
		</view>
	</view>
</template>

<script>
	import { payOrder, syncGoodsOrderPay, getOrderDetail } from '@/api/member/index'

	// 与后端关单、微信 time_expire 对齐：下单起 30 分钟
	const PAY_TIMEOUT_SEC = 30 * 60

	export default {
		data() {
			return {
				PayList: [
					{
						icon: '/static/wx_pay.png',
						name: '微信支付',
					},
				],
				orderAmount: 0,
				orderId: null,
				orderNo: null,
				PayWay: 0,
				PayPirce: `微信支付`,
				payDeadline: 0,
				countdownTimer: null,
				hour: 0,
				min: 0,
				sec: 0,
				isExpired: false,
			};
		},
		onLoad(option){
			this.orderAmount = option.orderAmount
			this.orderId = option.orderId
			this.orderNo = option.orderNo
			this.initPage()
		},
		onUnload() {
			this.clearCountdown()
		},
		methods:{
			deadlineStorageKey() {
				return `goods_pay_deadline_${this.orderId}`
			},
			parseCreateTime(createTime) {
				if (!createTime) return NaN
				return new Date(String(createTime).replace(/-/g, '/')).getTime()
			},
			async initPage() {
				try {
					if (this.orderId) {
						const res = await getOrderDetail({ orderId: this.orderId })
						const data = res && res.data
						if (data) {
							if (data.moneyPayable != null && data.moneyPayable !== '') {
								this.orderAmount = data.moneyPayable
							}
							if (data.orderNo) {
								this.orderNo = data.orderNo
							}
							this.initPayDeadline(data.createTime)
						} else {
							this.initPayDeadline()
						}
					} else {
						this.initPayDeadline()
					}
				} catch (e) {
					this.initPayDeadline()
				}
				this.startCountdown()
			},
			/**
			 * 按下单时间计算截止时刻，避免重新进入页面倒计时重置
			 */
			initPayDeadline(createTime) {
				const storageKey = this.orderId ? this.deadlineStorageKey() : ''
				const createTs = this.parseCreateTime(createTime)
				if (Number.isFinite(createTs)) {
					this.payDeadline = createTs + PAY_TIMEOUT_SEC * 1000
					if (storageKey) {
						uni.setStorageSync(storageKey, this.payDeadline)
					}
					return
				}
				let deadline = storageKey ? Number(uni.getStorageSync(storageKey)) : NaN
				if (!deadline || Number.isNaN(deadline)) {
					deadline = Date.now() + PAY_TIMEOUT_SEC * 1000
					if (storageKey) {
						uni.setStorageSync(storageKey, deadline)
					}
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
				if (this.orderId) {
					uni.removeStorageSync(this.deadlineStorageKey())
				}
			},
			/**
			 * 支付方式切换点击
			 */
			onPayWay(item,index){
				this.PayWay = index;
				this.PayPirce = `${item.name}`
			},
			/**
			 * 支付点击
			 */
			onSubmit(){
				if (this.isExpired) {
					uni.showToast({ title: '支付已超时，请重新下单', icon: 'none' })
					return
				}
				let _this = this
				uni.showModal({
				  title: '支付提示',
				  showCancel: true,
				  cancelText: '取消',
				  cancelColor: '#000000',
				  confirmText: '立即支付',
				  confirmColor: '#3CC51F',
				  success: function(res) { // 成功回调
				    if (res.confirm) {
				      _this.pay()
				    }
				  }
				})
			},
			pay() {
				if (this.isExpired) {
					uni.showToast({ title: '支付已超时，请重新下单', icon: 'none' })
					return
				}
				let params = {
					orderNo: this.orderNo,
					orderId: this.orderId
				}
				payOrder(params).then(res => {
					console.log(res)
					let order = res.data
					let orderInfo = {
						"timeStamp": String(order.timeStamp),
						"nonceStr": order.nonceStr,  
						"package": order.packageVal,  
						"signType": order.signType,  
						"paySign":  order.paySign
					}
					console.log(orderInfo)
					uni.requestPayment({
						provider: 'wxpay',
						...orderInfo,
						success: (e) => {
							this.clearDeadlineStorage()
							const amountNum = Number(this.orderAmount)
							const amount = Number.isFinite(amountNum) ? amountNum.toFixed(2) : String(this.orderAmount || '0')
							const gold = Number.isFinite(amountNum) && amountNum > 0 ? Math.floor(amountNum) : 0
							const finish = () => {
								uni.showModal({
									title: '支付成功',
									content: gold > 0
										? `实付￥${amount}元，获得${gold}金币`
										: `实付￥${amount}元`,
									showCancel: false,
									confirmText: '知道了',
									success: () => {
										uni.redirectTo({
											url: `/packagesMall/PayResult/PayResult?orderAmount=${this.orderAmount}&orderId=${this.orderId || ''}`
										})
									}
								})
							}
							// 主动查单落库，避免回调延迟导致一直待付款
							syncGoodsOrderPay(this.orderId).then(() => {
								finish()
							}).catch(() => {
								finish()
							})
						},
						fail: (e) => {
							console.log(e)
							uni.showModal({
							  content: "本次支付未成功，继续支付？",
							  confirmText: "确定",
							  cancelText: "返回",
							  success: (res) => {
								if (res.confirm) {
								  console.log("用户点击确定");
								} else if (res.cancel) {
								}
							  },
							})
						}
					})
				})
			},
			goOrder() {
				uni.redirectTo({
					url: '/packagesMall/MyOrderList/MyOrderList'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'CashierDesk.scss';
</style>
