<template>
	<view class="page_view">
		<view class="page_price_card">
			<text class="price_label_view">支付金额</text>
			<text class="price_value_view">¥{{ formatMoney(reserveData.price) }}</text>
		</view>
		<view class="page_base_view">
			<view class="base_box_view">
				<view class="base_label_view">订单号</view>
				<view class="base_value_view">{{getOrderNoFn()}}</view>
			</view>
			<view class="base_box_view">
				<view class="base_label_view">房源名称</view>
				<view class="base_value_view">豪华双床房【城景，住满7晚含接或送机站1次】；{{reserveData.roomNumber}}间</view>
			</view>
			<view class="base_box_view">
				<view class="base_label_view">所在基地</view>
				<view class="base_value_view">{{hotelData.name}}</view>
			</view>
			<view class="base_box_view">
				<view class="base_label_view">入住日期</view>
				<view class="base_value_view">{{reserveData.checkInDate}}</view>
			</view>
			<view class="base_box_view">
				<view class="base_label_view">离店日期</view>
				<view class="base_value_view">{{reserveData.checkOutDate}}</view>
			</view>
			<view class="base_box_view">
				<view class="base_label_view">供餐需求</view>
				<view class="base_value_view">
					{{`${comboList[reserveData.comboIndex].name} ${comboList[reserveData.comboIndex].price}/人/天；${reserveData.peopleNumber}人`}}
				</view>
			</view>
			<view class="base_box_view is_amount">
				<view class="base_label_view">总金额</view>
				<view class="base_value_view">¥{{ formatMoney(reserveData.price) }}</view>
			</view>
			<view class="base_box_view is_amount">
				<view class="base_label_view">支付金额</view>
				<view class="base_value_view">¥{{ formatMoney(reserveData.price) }}（全款）</view>
			</view>
		</view>
		<view class="section_caption">支付方式</view>
		<view class="page_base_view">
			<view class="base_box_view method_value" v-for="(item, index) in payMthodList" :key="index"
				@click="reserveData.payMthod = item.name">
				<view class="base_label_view">
					<u-icon :name="item.icon" :color="item.color" size="50rpx"></u-icon>
					{{item.name}}
				</view>
				<view class="base_value_view">
					<u-icon name="checkmark-circle-fill" :color="reserveData.payMthod == item.name ? '#701018':'#909399'"
						size="50rpx"></u-icon>
				</view>
			</view>
		</view>
		<view class="page_foot_view">
			<view class="button_view button_secondary" @click="goOrder">暂不支付</view>
			<view class="button_view button_primary" @click="$u.throttle(onSubmit, 500)">¥{{ formatMoney(reserveData.price) }}</view>
		</view>
	</view>
</template>

<script>
	import {
		getGoodsInfo
	} from '@/api/shop/index'
	import { payOrder, syncGoodsOrderPay, getOrderDetail } from '@/api/member/index'

	// 与后端关单、微信 time_expire 对齐：下单起 30 分钟
	const PAY_TIMEOUT_SEC = 30 * 60

	export default {
		data() {
			return {
				reserveData: {
					price: '0', // 支付金额
					checkInDate: '', // 入住日期
					checkOutDate: '', // 离开日期
					roomNumber: 1, // 预定房间数
					peopleNumber: 2, // 入住人数
					comboIndex: 0, // 套餐选中的下标
					payMthod: '微信支付', // 支付方式
				}, // 预定信息
				payMthodList: [{
						name: '微信支付',
						icon: 'weixin-fill',
						color: '#55B746'
					},
				],
				comboList: [{
						name: '含早餐',
						price: 0,
					},
					{
						name: '一早一正【晚餐】',
						price: 25,
					},
					{
						name: '一日三餐',
						price: 50,
					}
				], // 套餐数据集合
				hotelData: {
					name: '',
				}, // 酒店信息
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
				isExpired: false
			}
		},
		onLoad(e) {
			if (e.price) {
				this.reserveData.price = e.price
			}
			if (e.roomNumber) {
				this.reserveData.roomNumber = e.roomNumber
			}
			if (e.peopleNumber) {
				this.reserveData.peopleNumber = e.peopleNumber
			}
			if (e.checkInDate) {
				this.reserveData.checkInDate = e.checkInDate
			}
			if (e.checkOutDate) {
				this.reserveData.checkOutDate = e.checkOutDate
			}
			if (e.comboIndex !== undefined && e.comboIndex !== '') {
				this.reserveData.comboIndex = Number(e.comboIndex)
			}
			if (e.id) {
				this.getGoodsDetailFn(e.id)
			}
			this.orderAmount = e.price || this.reserveData.price
			this.orderId = e.orderId
			this.orderNo = e.orderNo
			if (this.orderId) {
				this.syncOrderAmount()
			} else {
				this.initPayDeadline()
				this.startCountdown()
			}
		},
		onUnload() {
			this.clearCountdown()
		},
		methods: {
			formatMoney(value) {
				const num = Number(value)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			},
			calcGold(value) {
				const num = Number(value)
				return Number.isFinite(num) && num > 0 ? Math.floor(num) : 0
			},
			deadlineStorageKey() {
				return `goods_pay_deadline_${this.orderId}`
			},
			parseCreateTime(createTime) {
				if (!createTime) return NaN
				return new Date(String(createTime).replace(/-/g, '/')).getTime()
			},
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
			syncOrderAmount() {
				getOrderDetail({ orderId: this.orderId }).then(res => {
					const data = res && res.data
					if (data && String(data.payStatus) === '1') {
						this.clearDeadlineStorage()
						this.goOrder()
						return
					}
					if (!data) {
						this.initPayDeadline()
						this.startCountdown()
						return
					}
					const amount = data.moneyPayable != null ? data.moneyPayable : data.payMoney
					if (amount != null && amount !== '') {
						this.reserveData.price = amount
						this.orderAmount = amount
					}
					if (data.orderNo) {
						this.orderNo = data.orderNo
					}
					this.initPayDeadline(data.createTime)
					this.startCountdown()
				}).catch(() => {
					this.initPayDeadline()
					this.startCountdown()
				})
			},
			// 获取商品详情
			getGoodsDetailFn(id) {
				getGoodsInfo(id).then(res => {
					this.hotelData.name = res.data.goodsName
					this.hotelData.desc = res.data.description
					this.hotelData.goodsImages = res.data.goodsImages.split(',')
				}).catch(err => {
					console.log('getGoodsInfo', err)
				})
			},
			// 获取订单编号
			getOrderNoFn() {
				return this.orderNo || ''
			},
			goOrder() {
				uni.redirectTo({
					url: '/packagesMall/MyOrderList/MyOrderList'
				})
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
			},			/**
			 * 支付方式切换点击
			 */
			onPayWay(item,index){
				this.PayWay = index;
				this.PayPirce = `${item.name}`
			},
			pay() {
				if (this.isExpired) {
					uni.showToast({ title: '支付已超时，请重新下单', icon: 'none' })
					return
				}
				let _this = this
				let params = {
					orderNo: this.orderNo,
					orderId: this.orderId
				}
				payOrder(params).then(res => {
					if (!res || res.code !== 200 || !res.data) {
						uni.showToast({
							icon: 'none',
							title: (res && res.msg) || '发起支付失败'
						})
						return
					}
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
							const amount = this.formatMoney(this.reserveData.price)
							const gold = this.calcGold(this.reserveData.price)
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
											url: `/packagesMall/PayResult/PayResult?orderAmount=${this.reserveData.price}&orderId=${this.orderId || ''}`
										})
									}
								})
							}
							syncGoodsOrderPay(this.orderId).then(() => finish()).catch(() => finish())
						},
						fail: (e) => {
							console.log(e)
							uni.showModal({
							  content: "本次支付未成功，继续支付？",
							  confirmText: "继续支付",
							  cancelText: "返回",
							  success: (res) => {
								if (res.confirm) {
								  _this.pay()
								}
							  },
							})
						}
					})
				}).catch(err => {
					console.log('payOrder', err)
					uni.showToast({
						icon: 'none',
						title: (err && err.msg) || '发起支付失败，请稍后重试'
					})
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'SojournCashierDesk.scss';
</style>
