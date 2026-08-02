<template>
	<view class="page">
		<!-- 订单状态 -->
		<view class="order-status" v-if="orderDetail">
			<view class="status">
				<text class="iconfont icon-zhuyi"></text>
				<text>{{ orderStatusText }}</text>
			</view>
			<view class="reason" v-if="isAfterRejected && rejectReason">
				<text>{{ rejectReason }}</text>
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
		<!-- 订单商品 -->
		<view class="order-goods" v-if="orderDetail && orderDetail.goodsList && orderDetail.goodsList.length>0">
			<view class="goods-list">
				<view class="list" v-for="(item, index) in orderDetail.goodsList" :key="index">
					<view class="thumb">
						<image :src="host + item.goodsCover" mode=""></image>
					</view>
					<view class="item">
						<view class="title">
							<text class="one-omit">{{item.goodsName}}</text>
						</view>
						<view class="num-size">
							<text>数量：{{orderDetail.goodsCount}}</text>
							<text v-if="isEducationOrder && item.unit"> | {{ item.unit }}</text>
						</view>
						<view class="price">
							<text>￥{{item.price}}</text>
						</view>
            <view class="selected-dates-info" v-if="orderDetail && orderDetail.checkInDate && orderDetail.checkOutDate">
              <text>入住: {{ formatDate(orderDetail.checkInDate) }} | 离店: {{ formatDate(orderDetail.checkOutDate) }} | 共 {{ getStayDays() }} 晚</text>
            </view>
					</view>
				</view>
			</view>
			<!-- <view class="contact">
				<text class="iconfont icon-kefu"></text>
				<text>联系客服</text>
			</view> -->
		</view>
		<!-- 订单信息 -->
		<view class="order-info" v-if="orderDetail">
			<view class="info-list">
				<view class="list">
					<view class="title">订单编号:</view>
					<view class="content">
						<text>{{orderDetail.orderNo}}</text>
						<text class="btn">复制</text>
					</view>
				</view>
				<view class="list">
					<view class="title">下单时间:</view>
					<view class="content">
						<text>{{orderDetail.createTime}}</text>
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
			<view class="details-list">
				<view class="list">
					<view class="title">
						<text>商品总额</text>
					</view>
					<view class="price">
						<text>￥{{orderDetail.moneyPayable}}</text>
					</view>
				</view>
				<view class="list action" v-if="orderDetail.status == 1">
					<view class="title">
						<text>实付款：</text>
					</view>
					<view class="price">
						<text>￥{{orderDetail.payMoney}}</text>
					</view>
				</view>
			</view>
		</view>
		<!-- 底部按钮 -->
		<view class="footer-btn">
			<view class="del">
				<text class="action" @click="cancelOrder" v-if="showCancel">取消订单</text>
			</view>
			<view class="btn">
				<text class="after" @click="onApplyAftersales" v-if="showSaleAfter">{{ isAfterRejected ? '重新申请售后' : '申请售后' }}</text>
				<text class="action" @click="orderByPay" v-if="showCancel">确认付款</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getOrderDetail, cancelOrder, syncGoodsOrderPay, syncGoodsOrderRefund } from '@/api/member/index'
	import { getGoodsInfo,getGoodsSkuInfo } from '@/api/shop/index'
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
				skuDataList: [],
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
		methods:{
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
				this.orderDetail = res.data
				// 待付款时主动向微信查一次，修复“已付款但仍显示待付款”
				if (this.orderDetail && String(this.orderDetail.status) === '0' && String(this.orderDetail.payStatus || '0') === '0') {
					try {
						await syncGoodsOrderPay(orderId)
						res = await getOrderDetail({ orderId: orderId })
						this.orderDetail = res.data
					} catch (e) {
						// 未支付成功时忽略
					}
				}
				// 退款中主动查退款，修复“已退款仍显示退款中”
				if (this.orderDetail && String(this.orderDetail.status) === '3') {
					try {
						await syncGoodsOrderRefund(orderId)
						res = await getOrderDetail({ orderId: orderId })
						this.orderDetail = res.data
					} catch (e) {
						// 退款未完成时忽略
					}
				}
				if(this.orderDetail && this.orderDetail.status=='0') this.showCancel=true
				else this.showCancel=false
				if(this.orderDetail && this.orderDetail.status=='1') this.showSaleAfter=true
				else this.showSaleAfter=false
				if(this.orderDetail && this.orderDetail.goodsList && this.orderDetail.goodsList.length>0) {
					this.goodsDetail = this.orderDetail.goodsList[0]
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
        const checkIn = new Date(pardate);
        return checkIn.getFullYear() + '-' + (checkIn.getMonth() + 1) + '-' + checkIn.getDate();
      },
    getStayDays() {
      if (!this.orderDetail || !this.orderDetail.checkInDate || !this.orderDetail.checkOutDate) return 0;

      const checkIn = new Date(this.orderDetail.checkInDate);
      const checkOut = new Date(this.orderDetail.checkOutDate);
      const timeDiff = checkOut.getTime() - checkIn.getTime();
      return Math.ceil(timeDiff / (1000 * 3600 * 24));
    },
			orderByPay() {
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
