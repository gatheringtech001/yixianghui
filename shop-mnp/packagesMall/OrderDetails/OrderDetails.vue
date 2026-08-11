<template>
	<view class="page">
		<!-- 订单状态 -->
		<view class="order-status" :class="{ 'is-pending': isPendingPayment }" v-if="orderDetail">
			<view class="status" v-if="!isPendingPayment">
				<text class="iconfont icon-zhuyi"></text>
				<text>{{ orderStatusText }}</text>
			</view>
			<view class="pending-header" v-if="isPendingPayment">
				<view class="status-line">
					<text class="iconfont icon-zhuyi"></text>
					<text class="status-text">{{ orderStatusText }}</text>
				</view>
				<view class="countdown-row" v-if="!isExpired">
					<text>请在</text>
					<text class="time">{{ countdownText }}</text>
					<text>内完成支付，超时订单将自动取消</text>
				</view>
				<view class="countdown-row expired" v-else>
					<text>支付已超时，请重新下单</text>
				</view>
			</view>
			<view class="reason" v-if="isAfterRejected && rejectReason">
				<text>{{ rejectReason }}</text>
			</view>
		</view>
		<!-- 订单商品（紧挨待付款状态，便于核对） -->
		<view class="order-goods order-goods-top" v-if="orderDetail && orderDetail.goodsList && orderDetail.goodsList.length>0">
			<view class="goods-list">
				<view class="list" v-for="(item, index) in orderDetail.goodsList" :key="index">
					<view class="thumb">
						<image :src="host + item.goodsCover" mode="aspectFill"></image>
					</view>
					<view class="item">
						<view class="product-name">
							<text class="two-omit">{{ getHotelProductName(item) }}</text>
						</view>
						<view class="product-spec" v-if="getHotelSpecName(item)">
							<text class="hotel-tag" v-if="isHotelOrder && isCustomNightOrder">自选</text>
							<text class="two-omit">{{ getHotelSpecName(item) }}</text>
						</view>
						<view class="title" v-if="!isHotelOrder">
							<text class="one-omit">{{ item.goodsName }}</text>
						</view>
						<view class="num-size">
							<text v-if="isHotelOrder">{{ getHotelGoodsMeta() }}</text>
							<text v-else>数量：{{ orderDetail.goodsCount }}</text>
							<text v-if="isEducationOrder && item.unit"> | {{ item.unit }}</text>
						</view>
						<view class="price">
							<text>￥{{ formatMoney(item.price) }}</text>
						</view>
            <view class="selected-dates-info" v-if="!isHotelOrder && orderDetail && orderDetail.checkInDate && orderDetail.checkOutDate">
              <text>入住: {{ formatDate(orderDetail.checkInDate) }} | 离店: {{ formatDate(orderDetail.checkOutDate) }} | 共 {{ getStayDays() }} 晚</text>
            </view>
            <view class="goods-spec" v-if="!isHotelOrder && (item.specifications || item.unit)">
              <text v-if="item.specifications">{{ item.specifications }}</text>
              <text v-if="item.unit">{{ item.unit }}</text>
            </view>
					</view>
				</view>
			</view>
		</view>
		<!-- 售后反馈 -->
		<view class="after-sale-box" v-if="latestAfter">
			<view class="box-title">售后进度</view>
			<view class="box-row">
				<text class="label">售后状态</text>
				<text class="value" :class="{ danger: isAfterRejected }">{{ afterStatusText }}</text>
			</view>
			<view class="box-row" v-if="latestAfter.remark">
				<text class="label">审核意见</text>
				<text class="value">{{ latestAfter.remark }}</text>
			</view>
			<view class="box-row" v-if="latestAfter.updateTime || latestAfter.createTime">
				<text class="label">更新时间</text>
				<text class="value">{{ latestAfter.updateTime || latestAfter.createTime }}</text>
			</view>
		</view>
		<!-- 旅居预订信息 -->
		<view class="booking-info-card" v-if="isHotelOrder && orderDetail">
			<view class="card-title">预订信息</view>
			<view class="date-row" v-if="hasHotelStayDates">
				<view class="date-item">
					<text class="date-label">入住</text>
					<text class="date-value">{{ formatDate(orderDetail.checkInDate) }}</text>
				</view>
				<view class="date-divider">
					<text class="nights">{{ getStayNights() }}晚</text>
				</view>
				<view class="date-item align-right">
					<text class="date-label">离店</text>
					<text class="date-value">{{ formatDate(orderDetail.checkOutDate) }}</text>
				</view>
			</view>
			<view class="info-rows">
				<view class="info-row" v-if="hotelBaseName">
					<text class="label">所在基地</text>
					<text class="value">{{ hotelBaseName }}</text>
				</view>
				<view class="info-row">
					<text class="label">预订房间</text>
					<text class="value">{{ orderDetail.goodsCount || 1 }} 间</text>
				</view>
				<view class="info-row" v-if="orderDetail.selfGoodsCount">
					<text class="label">用餐人数</text>
					<text class="value">{{ orderDetail.selfGoodsCount }} 人</text>
				</view>
				<view class="info-row" v-if="comboInfo">
					<text class="label">供餐套餐</text>
					<text class="value">{{ comboInfo.name }}（¥{{ comboInfo.price }}/人/天）</text>
				</view>
				<view class="info-row" v-if="orderDetail.contactName">
					<text class="label">联系姓名</text>
					<text class="value">{{ orderDetail.contactName }}</text>
				</view>
				<view class="info-row" v-if="orderDetail.contactPhone">
					<text class="label">联系电话</text>
					<text class="value">{{ orderDetail.contactPhone }}</text>
				</view>
				<view class="info-row" v-if="orderDetail.remark">
					<text class="label">备注</text>
					<text class="value">{{ orderDetail.remark }}</text>
				</view>
			</view>
		</view>
		<!-- 收货地址 -->
		<view class="shipping-address" v-if="orderDetail && orderDetail.addressId != 0 && address">
			<view class="name-phone">
				<text class="iconfont icon-dingwei"></text>
				<text>{{address.linkPerson}}</text>
				<text>{{address.linkMobile}}</text>
			</view>
			<view class="address">
				<text>{{address.provinceName}}{{address.cityName}}{{address.countyName}}{{address.streetName || ''}}</text>
			</view>
		</view>
		<!-- 报名联系信息（教育课程） -->
		<view class="education-contact" v-if="isEducationOrder && (orderDetail.contactName || orderDetail.contactPhone)">
			<view class="contact-list">
				<view class="list" v-if="orderDetail.contactName">
					<view class="title">{{ labels.contactName }}</view>
					<view class="content">{{ orderDetail.contactName }}</view>
				</view>
				<view class="list" v-if="orderDetail.contactPhone">
					<view class="title">{{ labels.contactPhone }}</view>
					<view class="content">{{ orderDetail.contactPhone }}</view>
				</view>
				<view class="list" v-if="orderDetail.remark">
					<view class="title">{{ labels.remark }}</view>
					<view class="content">{{ orderDetail.remark }}</view>
				</view>
			</view>
		</view>
		<!-- 课程信息（教育课程） -->
		<view class="education-course-info" v-if="isEducationOrder && hasEducationCourseInfo">
			<view class="info-title">{{ labels.courseInfo }}</view>
			<view class="info-grid">
				<view class="info-item" v-if="educationExt.courseTime">
					<text class="label">{{ labels.courseTime }}</text>
					<text class="value">{{ educationExt.courseTime }}</text>
				</view>
				<view class="info-item" v-if="educationExt.coursePlace">
					<text class="label">{{ labels.coursePlace }}</text>
					<text class="value">{{ educationExt.coursePlace }}</text>
				</view>
				<view class="info-item" v-if="educationExt.teacherName">
					<text class="label">{{ labels.courseTeacher }}</text>
					<text class="value">{{ educationExt.teacherName }}</text>
				</view>
				<view class="info-item" v-if="educationExt.startDate">
					<text class="label">{{ labels.startDate }}</text>
					<text class="value">{{ educationExt.startDate }}</text>
				</view>
				<view class="info-item" v-if="educationExt.lessonCount">
					<text class="label">{{ labels.lessonCount }}</text>
					<text class="value">{{ educationExt.lessonCount }}{{ labels.lessonUnit }}</text>
				</view>
			</view>
		</view>
		<!-- 订单信息 -->
		<view class="order-info" v-if="orderDetail">
			<view class="info-list">
				<view class="list">
					<view class="title">订单编号:</view>
					<view class="content">
						<text>{{orderDetail.orderNo}}</text>
						<text class="btn" @click="copyOrderNo">复制</text>
					</view>
				</view>
				<view class="list">
					<view class="title">下单时间:</view>
					<view class="content">
						<text>{{orderDetail.createTime}}</text>
					</view>
				</view>
				<view class="list" v-if="isPendingPayment">
					<view class="title">支付方式:</view>
					<view class="content">
						<text>微信支付</text>
					</view>
				</view>
				<view class="list" v-if="orderDetail.status == 1">
					<view class="title">支付方式:</view>
					<view class="content">
						<text>微信支付</text>
					</view>
				</view>
				<view class="list" v-if="isEducationOrder">
					<view class="title">服务方式:</view>
					<view class="content">
						<text>{{ labels.offlineCourse }}</text>
					</view>
				</view>
				<view class="list" v-if="orderDetail.addressId != 0">
					<view class="title">配送方式:</view>
					<view class="content">
						<text>普通快递</text>
					</view>
				</view>
			</view>
		</view>
		<!-- 订单明细 -->
		<view class="order-details" v-if="orderDetail">
			<view class="details-title">费用明细</view>
			<view class="details-list">
				<view class="list" v-if="showMoneyTotal">
					<view class="title">
						<text>商品总额</text>
					</view>
					<view class="price">
						<text>￥{{ formatMoney(orderDetail.moneyTotal) }}</text>
					</view>
				</view>
				<view class="list" v-if="hasDiscount">
					<view class="title">
						<text>优惠减免</text>
					</view>
					<view class="price discount">
						<text>-￥{{ formatMoney(orderDetail.moneyDiscount) }}</text>
					</view>
				</view>
				<view class="list" v-if="hasExpressFee">
					<view class="title">
						<text>运费</text>
					</view>
					<view class="price">
						<text>￥{{ formatMoney(orderDetail.moneyExpress) }}</text>
					</view>
				</view>
				<view class="list" v-if="!showMoneyTotal">
					<view class="title">
						<text>商品总额</text>
					</view>
					<view class="price">
						<text>￥{{ payAmountText }}</text>
					</view>
				</view>
				<view class="list action" v-if="isPendingPayment">
					<view class="title">
						<text>应付金额</text>
					</view>
					<view class="price">
						<text>￥{{ payAmountText }}</text>
					</view>
				</view>
				<view class="list action" v-if="orderDetail.status == 1">
					<view class="title">
						<text>实付款</text>
					</view>
					<view class="price">
						<text>￥{{ formatMoney(orderDetail.payMoney) }}</text>
					</view>
				</view>
			</view>
		</view>
		<!-- 底部按钮 -->
		<view class="footer-btn">
			<view class="del">
				<text class="action" @click="cancelOrder" v-if="showCancel">取消订单</text>
				<view class="footer-total" v-if="isPendingPayment">
					<text class="total-label">合计</text>
					<text class="total-symbol">¥</text>
					<text class="total-value">{{ payAmountText }}</text>
				</view>
			</view>
			<view class="btn">
				<text class="after" @click="onApplyAftersales" v-if="showSaleAfter">{{ isAfterRejected ? '重新申请售后' : '申请售后' }}</text>
				<text class="action" :class="{ disabled: isExpired }" @click="orderByPay" v-if="showCancel">{{ isExpired ? '已超时' : '确认付款' }}</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getOrderDetail, cancelOrder, syncGoodsOrderPay, syncGoodsOrderRefund } from '@/api/member/index'
	import { getGoodsInfo } from '@/api/shop/index'

	const PAY_TIMEOUT_SEC = 30 * 60

	export default {
		data() {
			return {
				host: this.$host,
				orderDetail: null,
				address: null,
				showCancel: false,
				showSaleAfter: false,
				pramOrderId: 0,
				goodsDetail: null,
				hotelProductTitle: '',
				skuDataList: [],
				payDeadline: 0,
				countdownTimer: null,
				countdownSec: 0,
				isExpired: false,
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
				],
				labels: {
					courseInfo: '\u8bfe\u7a0b\u4fe1\u606f',
					courseTime: '\u4e0a\u8bfe\u65f6\u95f4',
					coursePlace: '\u6388\u8bfe\u5730\u70b9',
					courseTeacher: '\u6388\u8bfe\u8001\u5e08',
					startDate: '\u5f00\u8bfe\u65e5\u671f',
					lessonCount: '\u8bfe\u6b21',
					lessonUnit: '\u6b21',
					contactName: '\u62a5\u540d\u4eba:',
					contactPhone: '\u8054\u7cfb\u7535\u8bdd:',
					remark: '\u7559\u8a00:',
					offlineCourse: '\u7ebf\u4e0b\u6388\u8bfe\uff08\u65e0\u9700\u914d\u9001\uff09'
				}
			};
		},
		computed: {
			isPendingPayment() {
				return !!(this.orderDetail && String(this.orderDetail.status) === '0')
			},
			isHotelOrder() {
				return !!(this.goodsDetail && this.goodsDetail.goodsType === 'hotel')
			},
			isCustomNightOrder() {
				if (!this.isHotelOrder || !this.orderDetail) return false
				const seq = Number(this.orderDetail.skuSeqNo)
				return !Number.isFinite(seq) || seq <= 0
			},
			comboInfo() {
				if (!this.isHotelOrder || !this.orderDetail) return null
				const index = Number(this.orderDetail.selComboIndex)
				if (!Number.isFinite(index) || index < 0 || index >= this.comboList.length) {
					return this.comboList[0]
				}
				return this.comboList[index]
			},
			hotelBaseName() {
				if (!this.isHotelOrder) return ''
				if (this.orderDetail && this.orderDetail.deptName) return this.orderDetail.deptName
				if (this.goodsDetail && this.goodsDetail.description) return this.goodsDetail.description
				return ''
			},
			hasHotelStayDates() {
				if (!this.isHotelOrder || !this.orderDetail) return false
				return !!(this.orderDetail.checkInDate && this.orderDetail.checkOutDate)
			},
			payAmountText() {
				if (!this.orderDetail) return '0.00'
				const amount = this.orderDetail.moneyPayable != null ? this.orderDetail.moneyPayable : this.orderDetail.payMoney
				return this.formatMoney(amount)
			},
			showMoneyTotal() {
				if (!this.orderDetail || this.orderDetail.moneyTotal == null) return false
				const total = Number(this.orderDetail.moneyTotal)
				const payable = Number(this.orderDetail.moneyPayable)
				return Number.isFinite(total) && total > 0 && (!Number.isFinite(payable) || Math.abs(total - payable) > 0.001 || this.hasDiscount || this.hasExpressFee)
			},
			hasDiscount() {
				if (!this.orderDetail || this.orderDetail.moneyDiscount == null) return false
				return Number(this.orderDetail.moneyDiscount) > 0
			},
			hasExpressFee() {
				if (!this.orderDetail || this.orderDetail.moneyExpress == null) return false
				return Number(this.orderDetail.moneyExpress) > 0
			},
			countdownText() {
				const remain = Math.max(0, this.countdownSec)
				const min = Math.floor(remain / 60)
				const sec = remain % 60
				const minText = min < 10 ? `0${min}` : `${min}`
				const secText = sec < 10 ? `0${sec}` : `${sec}`
				return `${minText}:${secText}`
			},
			isEducationOrder() {
				return !!(this.goodsDetail && this.goodsDetail.goodsType === 'education')
			},
			educationExt() {
				return (this.goodsDetail && this.goodsDetail.educationExt) || {}
			},
			hasEducationCourseInfo() {
				const ext = this.educationExt
				return !!(ext.courseTime || ext.coursePlace || ext.teacherName || ext.startDate || ext.lessonCount)
			},
			latestAfter() {
				const list = this.orderDetail && this.orderDetail.orderAfterList
				if (!list || !list.length) return null
				return list.reduce((prev, cur) => {
					const prevId = Number(prev && prev.afterId) || 0
					const curId = Number(cur && cur.afterId) || 0
					return curId >= prevId ? cur : prev
				})
			},
			isAfterRejected() {
				return !!(this.latestAfter && String(this.latestAfter.status) === '2')
			},
			rejectReason() {
				if (!this.isAfterRejected) return ''
				const remark = this.latestAfter && this.latestAfter.remark ? String(this.latestAfter.remark).trim() : ''
				return remark ? `拒绝原因：${remark}` : '您的售后申请未通过审核'
			},
			afterStatusText() {
				if (!this.latestAfter) return ''
				const map = {
					'0': '待审核',
					'1': '已同意退款',
					'2': '已拒绝',
					'6': '退款完成'
				}
				return map[String(this.latestAfter.status)] || ('状态' + this.latestAfter.status)
			},
			orderStatusText() {
				if (!this.orderDetail) return ''
				if (String(this.orderDetail.status) === '1' && this.isAfterRejected) {
					return '售后已拒绝'
				}
				const map = {
					'0': '待付款',
					'1': '已支付',
					'2': '已取消',
					'3': '退款中',
					'4': '退款完成'
				}
				return map[String(this.orderDetail.status)] || ''
			}
		},
		onLoad(option) {
			this.pramOrderId = option.orderId
			this.getOrderInfo(option.orderId)
		},
		onUnload() {
			this.clearCountdown()
		},
		methods:{
			formatMoney(value) {
				const num = Number(value)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			},
			parseCreateTime(createTime) {
				if (!createTime) return NaN
				return new Date(String(createTime).replace(/-/g, '/')).getTime()
			},
			deadlineStorageKey() {
				return `goods_pay_deadline_${this.pramOrderId || (this.orderDetail && this.orderDetail.orderId)}`
			},
			initPayDeadline(createTime) {
				const orderId = this.pramOrderId || (this.orderDetail && this.orderDetail.orderId)
				const storageKey = orderId ? this.deadlineStorageKey() : ''
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
					this.countdownSec = remain
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
			copyOrderNo() {
				if (!this.orderDetail || !this.orderDetail.orderNo) return
				uni.setClipboardData({
					data: String(this.orderDetail.orderNo),
					success: () => {
						uni.showToast({ title: '订单号已复制', icon: 'none' })
					}
				})
			},
			async getGoodsSkuInfoFn(id) {
							/* await getGoodsSkuInfo(id).then(res => {
								if (res.code != 200) return
								this.skuDataList = res.data
							}).catch(err => {
								console.log('getGoodsSkuInfo', err)
							}) */
						},
			async getOrderInfo(orderId) {
				let res = await getOrderDetail({ orderId: orderId })
				console.log(res)
				this.orderDetail = this.normalizeHotelOrder(res.data)
				// 待付款时主动向微信查一次，修复“已付款但仍显示待付款”
				if (this.orderDetail && String(this.orderDetail.status) === '0' && String(this.orderDetail.payStatus || '0') === '0') {
					try {
						await syncGoodsOrderPay(orderId)
						res = await getOrderDetail({ orderId: orderId })
						this.orderDetail = this.normalizeHotelOrder(res.data)
					} catch (e) {
						// 未支付成功时忽略
					}
				}
				// 退款中主动查退款，修复“已退款仍显示退款中”
				if (this.orderDetail && String(this.orderDetail.status) === '3') {
					try {
						await syncGoodsOrderRefund(orderId)
						res = await getOrderDetail({ orderId: orderId })
						this.orderDetail = this.normalizeHotelOrder(res.data)
					} catch (e) {
						// 退款未完成时忽略
					}
				}
				if(this.orderDetail && this.orderDetail.status=='0') {
					this.showCancel=true
					this.initPayDeadline(this.orderDetail.createTime)
					this.startCountdown()
				} else {
					this.showCancel=false
					this.clearCountdown()
				}
				if(this.orderDetail && this.orderDetail.status=='1') this.showSaleAfter=true
				else this.showSaleAfter=false
				if (this.orderDetail && this.orderDetail.addressInfo) {
					this.address = this.orderDetail.addressInfo
				}
				if(this.orderDetail && this.orderDetail.goodsList && this.orderDetail.goodsList.length>0) {
					this.goodsDetail = this.orderDetail.goodsList[0]
					if (this.goodsDetail && this.goodsDetail.goodsType === 'hotel') {
						const goodsId = this.orderDetail.goodsId || this.goodsDetail.goodsId
						if (goodsId) this.loadHotelProductTitle(goodsId)
					}
					if(this.goodsDetail.isSku == 1){
						//this.getGoodsSkuInfoFn(this.goodsDetail.goodsId)
					}
				}
				
				//if(data.addressId != 0) this.getAddressDetail(data.addressId)
			},
			// 获取地址详情
			async getAddressDetail(id) {
				let data = await getAddressInfo(id)
				this.address = data
			},
			/**
			 * 售后点击
			 */
			onApplyAftersales(){
				const orderId = this.pramOrderId || (this.orderDetail && this.orderDetail.orderId)
				if (!orderId) {
					uni.showToast({ icon: 'none', title: '订单信息缺失' })
					return
				}
				// 已有进行中售后时，直接提示，避免重复进入
				const after = this.latestAfter
				if (after && (String(after.status) === '0' || String(after.status) === '1')) {
					uni.showToast({ icon: 'none', title: '该订单已有进行中的售后申请' })
					return
				}
				uni.navigateTo({
					url: `/packagesMember/AfterSaleType/AfterSaleType?orderId=${orderId}`,
				})
			},
      formatDate(pardate){
        if (pardate == null || pardate === '' || pardate === 'null') return ''
        const text = String(pardate).trim()
        if (!text) return ''
        const date = new Date(text.replace(/-/g, '/'))
        if (Number.isNaN(date.getTime())) return text
        const month = date.getMonth() + 1;
        const day = date.getDate();
        return date.getFullYear() + '-' + month + '-' + day;
      },
    normalizeHotelOrder(order) {
      if (!order) return order
      const detail = order.orderDetailList && order.orderDetailList[0]
      if (detail) {
        if ((order.interCount == null || order.interCount === '') && detail.interCount != null) {
          order.interCount = detail.interCount
        }
        if (!order.checkInDate && detail.orderStartDate) {
          order.checkInDate = detail.orderStartDate
        }
        if (!order.checkOutDate && detail.orderEndDate) {
          order.checkOutDate = detail.orderEndDate
        }
        if ((order.skuSeqNo == null || order.skuSeqNo === '') && detail.skuSeqNo != null) {
          order.skuSeqNo = detail.skuSeqNo
        }
      }
      return order
    },
    getStayDays() {
      if (!this.orderDetail || !this.orderDetail.checkInDate || !this.orderDetail.checkOutDate) return 0;

      const checkIn = new Date(this.orderDetail.checkInDate);
      const checkOut = new Date(this.orderDetail.checkOutDate);
      const timeDiff = checkOut.getTime() - checkIn.getTime();
      return Math.ceil(timeDiff / (1000 * 3600 * 24));
    },
    getStayNights() {
      const interCount = Number(this.orderDetail && this.orderDetail.interCount)
      if (Number.isFinite(interCount) && interCount > 0) return interCount
      return this.getStayDays()
    },
    getStayDurationText() {
      const nights = this.getStayNights()
      if (!nights) return ''
      return `${nights + 1}天${nights}晚`
    },
    getGoodsDisplayName(item) {
      if (!this.isHotelOrder) return item.goodsName
      return this.getHotelProductName(item)
    },
    getHotelProductName(item) {
      if (!item) return ''
      if (this.hotelProductTitle) return this.hotelProductTitle
      return item.goodsName || ''
    },
    getHotelSpecName(item) {
      if (!this.isHotelOrder || !item) return ''
      let spec = item.specifications || item.skuDataValues || ''
      if (!spec && item.goodsName && item.goodsName !== this.getHotelProductName(item)) {
        spec = item.goodsName
      }
      if (this.isCustomNightOrder) {
        const duration = this.getStayDurationText()
        if (duration) {
          return spec ? `${spec} · 自选${duration}` : `自选${duration}`
        }
        return spec || '自选入住'
      }
      return spec
    },
    getHotelGoodsMeta() {
      if (!this.orderDetail) return ''
      const parts = []
      if (this.orderDetail.checkInDate && this.orderDetail.checkOutDate) {
        parts.push(`${this.formatDate(this.orderDetail.checkInDate)} 至 ${this.formatDate(this.orderDetail.checkOutDate)}`)
      }
      parts.push(`${this.orderDetail.goodsCount || 1} 间`)
      return parts.join(' · ')
    },
    async loadHotelProductTitle(goodsId) {
      try {
        const res = await getGoodsInfo(goodsId)
        if (res && res.data && res.data.goodsName) {
          this.hotelProductTitle = res.data.goodsName
        }
      } catch (e) {
        console.warn('[order-detail] failed to load product name', goodsId, e)
      }
    },
			orderByPay() {
				if (this.isExpired) {
					uni.showToast({ icon: 'none', title: '支付已超时，请重新下单' })
					return
				}
				if(this.goodsDetail && this.goodsDetail.goodsType == 'hotel'){
				uni.redirectTo({
					url: `/packagesMall/CashierDesk/SojournCashierDesk?id=${this.goodsDetail.goodsId}&price=${this.orderDetail.moneyPayable}&orderId=${this.orderDetail.orderId}&orderNo=${this.orderDetail.orderNo}&roomNumber=${this.orderDetail.goodsCount}&peopleNumber=${this.orderDetail.selfGoodsCount}&comboIndex=${this.orderDetail.selComboIndex}&checkInDate=${this.orderDetail.checkInDate}&checkOutDate=${this.orderDetail.checkOutDate}`
				})
				}else{
				uni.navigateTo({
					url: `/packagesMall/CashierDesk/CashierDesk?orderAmount=${this.orderDetail.moneyPayable}&orderId=${this.orderDetail.orderId}&orderNo=${this.orderDetail.orderNo}`,
				})
				}
				
			},
			/**
			 * 取消订单
			 */
			cancelOrder(){
				let that = this
				cancelOrder(that.orderDetail.orderId).then(res => {
					if (res.code == 200) {
            uni.showToast({
                title: res.msg,
                icon: 'none',
                duration: 500
            }) 
						uni.navigateTo({
							url: '/packagesMall/MyOrderList/MyOrderList?type=0'
						})
					} else {
            uni.showToast({
                title: res.msg,
                icon: 'none',
                duration: 2000
            })
					}
				})
			},
		}
	}
</script>

<style scoped lang="scss">
	@import 'OrderDetails.scss';
</style>
