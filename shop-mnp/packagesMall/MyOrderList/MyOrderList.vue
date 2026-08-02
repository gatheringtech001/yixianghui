<template>
	<view class="page">
		
		<!-- 订单tab -->
		<view class="order-tab">
			<view class="tab" :class="{'action':OrderType == 0}" @click="onOrderTab(0)">
				<text>全部</text>
				<text class="line"></text>
			</view>
			<view class="tab" :class="{'action':OrderType == 1}" @click="onOrderTab(1)">
				<text>待付款</text>
				<text class="line"></text>
			</view>
			<view class="tab" :class="{'action':OrderType == 2}" @click="onOrderTab(2)">
				<text>已付款</text>
				<text class="line"></text>
			</view>
			<view class="tab" :class="{'action':OrderType == 3}" @click="onOrderTab(3)">
				<text>已取消</text>
				<text class="line"></text>
			</view>
			<view class="tab" :class="{'action':OrderType == 4}" @click="onOrderTab(4)">
				<text>退款中</text>
				<text class="line"></text>
			</view>
			<view class="tab" :class="{'action':OrderType == 5}" @click="onOrderTab(5)">
				<text>已退款</text>
				<text class="line"></text>
			</view>
		</view>
		<!-- 订单列表 -->
		<view class="order-list" v-if="orderList && orderList.length > 0">
			<view class="list" v-for="(item,index) in orderList" @click="onOrderList(item)" :key="index">
				<view class="title-status">
					<view class="title">
						<text v-if="isEducationOrder(item)" class="order-type-tag">课程报名</text>
						<text>下单时间：{{item.createTime}}</text>
					</view>
					<view class="status">
						<text>{{ getOrderStatusText(item) }}</text>
						<text class="iconfont icon-laji del" v-if="item.status == 0" @click.stop="cancelOrder(item)"></text>
					</view>
				</view>
				<view class="after-feedback" v-if="isAfterRejected(item)" @click.stop>
					<text class="after-title">售后已拒绝</text>
					<text class="after-reason">{{ getRejectReason(item) }}</text>
				</view>
				<view class="goods-list">
					<view class="goods" v-for="(goods, index) in item.goodsList" :key="index">
						<view class="thumb">
							<image :src="host + goods.goodsCover" mode=""></image>
						</view>
						<view class="item" :class="{ 'education-item': goods.goodsType === 'education' }">
							<view class="item-top">
								<view class="goods-name">
									<text class="two-omit">{{goods.goodsName}}</text>
								</view>
								<view class="content">
									<view class="goods-price">
										<text class="min">￥</text>
										<text class="max">{{ getGoodsLinePrice(item, goods) }}</text>
										<text class="unit">元</text>
									</view>
									<view class="goods-counts">× {{item.goodsCount}}</view>
								</view>
							</view>
							<view class="course-meta" v-if="goods.goodsType === 'education'">
								<view v-if="getEducationExt(goods).courseTime">
									<text>{{ labels.courseTime }}</text>{{ getEducationExt(goods).courseTime }}
								</view>
								<view v-if="getEducationExt(goods).coursePlace">
									<text>{{ labels.coursePlace }}</text>{{ getEducationExt(goods).coursePlace }}
								</view>
								<view v-if="getEducationExt(goods).teacherName">
									<text>{{ labels.courseTeacher }}</text>{{ getEducationExt(goods).teacherName }}
								</view>
							</view>
							<view class="signup-contact" v-if="goods.goodsType === 'education' && (item.contactName || item.contactPhone)">
								<text>{{ labels.signupContact }}</text>{{ item.contactName || '' }} {{ item.contactPhone || '' }}
							</view>
						</view>
						
					</view>
				</view>
				<view class="status-btn">
					<view class="price">
						总金额：<text>{{ formatMoney(getOrderAmount(item)) }}</text>元
					</view>
					<view class="btn" v-if="item.status == 0" @click="orderByPay(item)">
						<text>去支付</text>
					</view>
					<view class="btn" v-if="item.status == 1" @click.stop="onApplyAftersales(item)">
						<text>{{ isAfterRejected(item) ? '重新申请售后' : '申请售后' }}</text>
					</view>
					<!-- 评价能力尚未接后端；已取消/退款单不展示入口 -->
					<view class="btn action" v-if="canEvaluate(item)" @click.stop="onEvaluate(item)">
						<text>去评价</text>
					</view>
				</view>
			</view>
		</view>
		
		<view class="empty" v-else>
			<u-empty text="暂无订单" mode="list"></u-empty>
			<button @click="buyNow">立即下单</button>
		</view>
	</view>
</template>

<script>
	import { getOrderList, cancelOrder as cancelOrderApi, syncGoodsOrderPay, syncGoodsOrderRefund } from '@/api/member/index'
	export default {
		data() {
			return {
				host: this.$host,
				OrderType: 0,
				orderList: [],
				labels: {
					courseTime: '\u4e0a\u8bfe\u65f6\u95f4\uff1a',
					coursePlace: '\u6388\u8bfe\u5730\u70b9\uff1a',
					courseTeacher: '\u6388\u8bfe\u8001\u5e08\uff1a',
					signupContact: '\u62a5\u540d\u4eba\uff1a'
				}
			};
		},
		onLoad(params) {
			this.OrderType = params.type;
			this.getOrders()
		},
		methods:{
			formatMoney(value) {
				const num = Number(value)
				return Number.isFinite(num) ? num.toFixed(2) : '0.00'
			},
			getOrderAmount(item) {
				if (!item) return 0
				const paid = Number(item.payMoney)
				if (Number.isFinite(paid) && paid > 0) return paid
				const payable = Number(item.moneyPayable)
				return Number.isFinite(payable) ? payable : 0
			},
			getGoodsLinePrice(item, goods) {
				const total = this.getOrderAmount(item)
				const count = Number(item && item.goodsCount) || 1
				if (total > 0 && count > 0) {
					return this.formatMoney(total / count)
				}
				const catalog = Number(goods && goods.price)
				return Number.isFinite(catalog) ? catalog.toFixed(2) : '0.00'
			},
			isEducationOrder(item) {
				const goods = item && item.goodsList && item.goodsList[0]
				return !!(goods && goods.goodsType === 'education')
			},
			getEducationExt(goods) {
				return (goods && goods.educationExt) || {}
			},
			getOrderGoodsType(item) {
				const goods = item && item.goodsList && item.goodsList[0]
				return (goods && goods.goodsType) || item.goodsType || ''
			},
			getLatestAfter(item) {
				const list = item && item.orderAfterList
				if (!list || !list.length) return null
				return list.reduce((prev, cur) => {
					const prevId = Number(prev && prev.afterId) || 0
					const curId = Number(cur && cur.afterId) || 0
					return curId >= prevId ? cur : prev
				})
			},
			isAfterRejected(item) {
				const after = this.getLatestAfter(item)
				return !!(after && String(after.status) === '2')
			},
			getRejectReason(item) {
				const after = this.getLatestAfter(item)
				const remark = after && after.remark ? String(after.remark).trim() : ''
				return remark ? `拒绝原因：${remark}` : '您的售后申请未通过，可查看详情或重新申请'
			},
			getOrderStatusText(item) {
				if (!item) return ''
				if (String(item.status) === '1' && this.isAfterRejected(item)) {
					return '售后已拒绝'
				}
				const map = {
					'0': '待付款',
					'1': '已支付',
					'2': '已取消',
					'3': '退款中',
					'4': '退款完成'
				}
				return map[String(item.status)] || ''
			},
			async getOrders() {
				let params = {
					status: this.OrderType == 0 ? '' :
							this.OrderType == 1 ? '0' :
							this.OrderType == 2 ? '1' :
							this.OrderType == 3 ? '2' :
							this.OrderType == 4 ? '3' :
							this.OrderType == 5 ? '4' : ''
				}
				let {rows, total} = await getOrderList(params)
				this.orderList = rows
				let needRefresh = false
				// 待付款列表：尝试同步可能已支付成功但回调未落库的订单
				if (this.OrderType == 1 || this.OrderType == 0) {
					const pending = (rows || []).filter(item => String(item.status) === '0').slice(0, 5)
					for (const item of pending) {
						try {
							await syncGoodsOrderPay(item.orderId)
							needRefresh = true
						} catch (e) {}
					}
				}
				// 退款中：主动查退款结果，修复已退款仍显示退款中
				if (this.OrderType == 4 || this.OrderType == 0) {
					const refunding = (rows || []).filter(item => String(item.status) === '3').slice(0, 5)
					for (const item of refunding) {
						try {
							const beforeAmount = this.getOrderAmount(item)
							await syncGoodsOrderRefund(item.orderId)
							needRefresh = true
							const gold = Math.floor(Number(beforeAmount) || 0)
							uni.showToast({
								icon: 'none',
								title: gold > 0
									? `退款完成，已扣回${gold}金币`
									: '退款完成'
							})
						} catch (e) {}
					}
				}
				if (needRefresh) {
					let refreshed = await getOrderList(params)
					this.orderList = refreshed.rows
				}
			},
			/**
			 * 返回点击
			 */
			onBack(){
				uni.navigateBack();
			},
			/**
			 * 订单tab点击
			 */
			onOrderTab(type){
				this.OrderType = type
				this.getOrders()
			},
			/**
			 * 订单列表点击
			 */
			onOrderList(item){
				uni.navigateTo({
					url: `/packagesMall/OrderDetails/OrderDetails?orderId=${item.orderId}`,
				})
			},
			cancelOrder(item){
				if (!item || item.status != 0) return
				uni.showModal({
					title: '提示',
					content: '确认取消该订单？',
					success: (res) => {
						if (!res.confirm) return
						cancelOrderApi(item.orderId).then(() => {
							uni.showToast({ title: '取消成功', icon: 'none' })
							this.getOrders()
						}).catch(err => {
							uni.showToast({
								title: (err && err.message) || '取消失败',
								icon: 'none'
							})
						})
					}
				})
			},
			
			onApplyAftersales(item){
				
					uni.navigateTo({
						url: `/packagesMember/AfterSaleType/AfterSaleType?orderId=${item.orderId}`,
					})
			},
			orderByPay(item) {
				const goodsType = this.getOrderGoodsType(item)
				if(goodsType === 'hotel'){
					uni.redirectTo({
						url: `/packagesMall/CashierDesk/SojournCashierDesk?id=${item.goodsId}&price=${item.moneyPayable}&orderId=${item.orderId}&orderNo=${item.orderNo}&roomNumber=${item.goodsCount}&peopleNumber=${item.selfGoodsCount}&comboIndex=${item.selComboIndex}&checkInDate=${item.checkInDate}&checkOutDate=${item.checkOutDate}`
					})
				}else{
				uni.navigateTo({
					url: `/packagesMall/CashierDesk/CashierDesk?orderAmount=${item.moneyPayable}&orderId=${item.orderId}&orderNo=${item.orderNo}`
				})
				}
			},
			canEvaluate(item) {
				// 当前订单状态机无「已完成」态；评价页也未接接口，暂不开放入口
				return false
			},
			onEvaluate(item){
				uni.showToast({
					icon: 'none',
					title: '评价功能即将开放'
				})
			},
			buyNow() {
				uni.switchTab({
					url: '/pages/classify/classify'
				})
			}
		}
	}
</script>

<style scoped lang="scss">
	@import 'MyOrderList.scss';
</style>
